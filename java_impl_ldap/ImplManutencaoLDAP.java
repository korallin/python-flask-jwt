/*
 * Created on 05/04/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package gov.trt.util.ldap;

/**
 * @author roliveira
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import gov.trt.util.Organograma;
import gov.trt.util.Pessoa;
import gov.trt.util.PropertiesLoader;
import gov.trt.util.SendMail;
import gov.trt.util.SendMailAntigo;
import gov.trt.util.clienteLDAP;
import gov.trt.util.jcrypt;
import gov.trt.util.fenix.Consultas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSet;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPModificationSet;
import netscape.ldap.LDAPSearchConstraints;
import netscape.ldap.LDAPSearchResults;

import javax.xml.ws.soap.SOAPFaultException;

public class ImplManutencaoLDAP
        extends UnicastRemoteObject
        implements IManutencaoLDAP {

    private static int POOL_MIN_MASTER;
    private static int POOL_MAX_MASTER;
    private static String MY_HOST_MASTER;
    private static int MY_PORT_MASTER;

    private static int POOL_MIN_SLAVE;
    private static int POOL_MAX_SLAVE;
    private static String MY_HOST_SLAVE;
    private static int MY_PORT_SLAVE;

    private static Properties properties;

    private static final String PROP_POOL_MIN_MASTER = "poolMinMaster";
    private static final String PROP_POOL_MAX_MASTER = "poolMaxMaster";
    private static final String PROP_HOST_LDAP_MASTER = "hostLDAPMaster";
    private static final String PROP_PORT_LDAP_MASTER = "portLDAPMaster";

    private static final String PROP_POOL_MIN_SLAVE = "poolMinSlave";
    private static final String PROP_POOL_MAX_SLAVE = "poolMaxSlave";
    private static final String PROP_HOST_LDAP_SLAVE = "hostLDAPSlave";
    private static final String PROP_PORT_LDAP_SLAVE = "portLDAPSlave";

    private LDAPConnectionPool poolMaster;
    private LDAPConnectionPool poolSlave;
    private LDAPConnectionPool pool;

    /**
     * **************************************
     */
    public static void main(String[] args) {

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        IManutencaoLDAP manutencaoLDAP = null;

        try {
            manutencaoLDAP = new ImplManutencaoLDAP();
        } catch (Exception e) {
            System.out.println(
                    "Server.main: uma exception ocorreu: " + e.getMessage());
            e.printStackTrace();

        }

        try {
            // Instantiate the remote object and register it
            Naming.rebind("//localhost:2005/manutencaoLDAP", manutencaoLDAP);
            System.out.println("Servidor de manutencao do LDAP Remoto esta no ar na porta 2005!");
        } catch (Exception e) {
            System.out.println(
                    "Erro no registro do Servidor de Manutencao do LDAP Remoto na porta 2005: " + e.getMessage());
            // e.printStackTrace();
        }

        try {
            // Instantiate the remote object and register it
            Naming.rebind("//localhost:2006/manutencaoLDAP", manutencaoLDAP);
            System.out.println("Servidor de manutencao do LDAP Remoto esta no ar na porta 2006!");
        } catch (Exception e) {
            System.out.println(
                    "Erro no registro do Servidor de Manutencao do LDAP Remoto na porta 2006: " + e.getMessage());
            // e.printStackTrace();
        }

    }

    /**
     * ********************************************************
     */
    /**
     * Construtor - deve ser criado obrigatoriamente!
     */
    public ImplManutencaoLDAP() throws ImplManutLDAPException, RemoteException {

        // Existe o construtor UnicastRemoteObject(int port): 
        // super(2007) ;
        boolean falhaMaster = false;
        boolean falhaSlave = false;
        String msg = "";

        load();

        try {
            poolMaster = new LDAPConnectionPool(POOL_MIN_MASTER, POOL_MAX_MASTER, MY_HOST_MASTER, MY_PORT_MASTER);
        } catch (LDAPException e) {
            falhaMaster = true;
            msg += e.toString();
            System.out.println("Problemas com o pool de conexoes do LDAP Master : " + e.toString());
        }

        try {
            poolSlave = new LDAPConnectionPool(POOL_MIN_SLAVE, POOL_MAX_SLAVE, MY_HOST_SLAVE, MY_PORT_SLAVE);
        } catch (LDAPException e) {
            falhaSlave = true;
            msg += "  " + e.toString();
            System.out.println("Problemas com o pool de conexoes do LDAP Master : " + e.toString());
        }

        if (falhaMaster && falhaSlave) {
            throw new ImplManutLDAPException(
                    "Problemas com o pool de conexoes tanto do LDAP Master quanto do LDAP Slave : " + msg);
        }

    }

    /**
     * ********************************************************
     */
    /**
     * Construtor - deve ser criado obrigatoriamente!
     */
    public ImplManutencaoLDAP(int poolMin, int poolMax, String host, int port) throws RemoteException, ImplManutLDAPException {

        try {
            poolMaster = new LDAPConnectionPool(poolMin, poolMax, host, port);
            poolSlave = poolMaster;
        } catch (LDAPException e) {
            throw new ImplManutLDAPException(
                    "Problemas com o pool de conexoes do LDAP: " + e.toString());
        }

    }

    /**
     * ******************************************
     */
    public void alteraNome(
            String idLDAP,
            String nomeCompleto,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        String novoNomeCompleto = "";
        String sobreNome = "";
        String nomeAtual = "";
        String login = "";
        String nomeArquivo = "";
        String primeirosNomes = "";

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: alteraNome";
        mensagemLOG += " idLDAP: " + idLDAP;
        mensagemLOG += " novoNome: " + nomeCompleto;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        nomeAtual = getAtributo(ld, "idLDAP", idLDAP, "cn", "pessoa");
        login = getAtributo(ld, "idLDAP", idLDAP, "uid", "pessoa");
        novoNomeCompleto = transformaNome(nomeCompleto);
        sobreNome = ultimoNome(novoNomeCompleto);
        primeirosNomes = primeirosNomes(novoNomeCompleto);

        alteraAtributo(ld, idLDAP, "cn", novoNomeCompleto);
        alteraAtributo(ld, idLDAP, "gecos", novoNomeCompleto);
        alteraAtributo(ld, idLDAP, "displayName", novoNomeCompleto);
        alteraAtributo(ld, idLDAP, "sn", sobreNome);
        alteraAtributo(ld, idLDAP, "pn", primeirosNomes);

        /* REMOVIDO POR TIAGO LEAL EM 21/05/2018
        
        nomeArquivo = "/home/" + login + "/.openwebmail/webmail/from.book";

        try {
            this.alteraFraseArquivo(nomeArquivo, nomeAtual, novoNomeCompleto);
        } catch (IOException ioex) {
            throw new ImplManutLDAPException(
                    "Problemas na abertura do arquivo: " + ioex.toString());
        }
        */

        pool.close(ld);

    }

    /**
     * ***********************************
     */
    private void alteraFraseArquivo(
            String nomeArquivo,
            String atual,
            String novo) throws IOException {

        String linha = "";
        String linhaNova = "";
        Vector linhas = new Vector();
        int qtd = 0;

        File arquivo = new File(nomeArquivo);

        if (!arquivo.exists()) {
            return;
        }

        Reader arquivoReader = new FileReader(arquivo);

        BufferedReader bufLinhas = new BufferedReader(arquivoReader);

        linha = bufLinhas.readLine().trim() + " ";
        linhaNova = linha;
        while (linha != null) {
            linhaNova = linha.replaceAll(atual, novo);
            linhas.add(linhaNova.trim());
            linha = bufLinhas.readLine();
        }

        bufLinhas.close();
        arquivoReader.close();

        Writer arquivoWriter = new FileWriter(arquivo);

        BufferedWriter bufNovasLinhas = new BufferedWriter(arquivoWriter);

        qtd = linhas.size();
        String aLinhas[] = new String[qtd];
        linhas.copyInto(aLinhas);
        for (int i = 0; i < qtd; i++) {
            linha = aLinhas[i];
            bufNovasLinhas.write(linha);
            bufNovasLinhas.newLine();
            bufNovasLinhas.flush();
        }

        bufNovasLinhas.close();
        arquivoWriter.close();

    }

    /**
     * ******************************************
     */
    private String ultimoNome(String nomeCompleto) {

        String ultimoNome = "";
        StringTokenizer st = new StringTokenizer(nomeCompleto);

        st.nextToken(); //descarta o primeiro nome

        while (st.hasMoreTokens()) {
            ultimoNome += st.nextToken() + " ";
        }
        
        return ultimoNome.trim();

    }

    /**
     * ******************************************
     */
    public String primeirosNomes(String nomeCompleto) {

        String primeiroNome = "";
        StringTokenizer st = new StringTokenizer(nomeCompleto);
        //int qtd = st.countTokens();

        primeiroNome = st.nextToken();

        //for (int i = 0; i < qtd - 1; i++) {
        //    primeirosNomes += st.nextToken() + " ";
        //}

        /*
    while (st.hasMoreTokens()) {
      ultimoNome = st.nextToken();
    }
         */
        return primeiroNome;

    }

    /**
     * ******************************************
     */
    public void incluiAtributo(
            String idLDAP,
            String nomeAtributo,
            String valorAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: incluiAtributo";
        mensagemLOG += " idLDAP: " + idLDAP;
        mensagemLOG += " nomeAtributo: " + nomeAtributo;
        mensagemLOG += " valorAtributo: " + valorAtributo;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        /*
        try {
    
          ld.connect(MY_HOST, MY_PORT);
         */
        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        incluiAtributo(ld, idLDAP, nomeAtributo, valorAtributo);

        pool.close(ld);

    }

    /**
     * ******************************************
     */
    private void incluiAtributo(
            LDAPConnection ld,
            String idLDAP,
            String nomeAtributo,
            String valorAtributo)
            throws ImplManutLDAPException {

        String filtro = "";
        String dn = "";
        String valorOldAtributo = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        LDAPModificationSet manyChanges = new LDAPModificationSet();

        try {

            if (idLDAP.equals("")
                    || nomeAtributo.equals("")
                    || valorAtributo.equals("")) {
                throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
            }

            filtro = "(idLDAP=" + idLDAP + ")";

            findEntry = getEntry(ld, filtro);
            if (findEntry == null) {
                throw (new ImplManutLDAPException(
                        "Nem Pessoa nem Lotacao cadastrada no Directory Server com este idLDAP : "
                        + idLDAP));
            }

            atributo = findEntry.getAttribute(nomeAtributo);

            if (atributo != null) {
                Enumeration e = atributo.getStringValues();
                valorOldAtributo = (String) e.nextElement();

                atributo = new LDAPAttribute(nomeAtributo, valorOldAtributo);
                manyChanges.add(LDAPModification.DELETE, atributo);
            }

            dn = findEntry.getDN();
            atributo = new LDAPAttribute(nomeAtributo, valorAtributo);
            manyChanges.add(LDAPModification.ADD, atributo);
            ld.modify(dn, manyChanges);

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        }

    }

    /**
     * ************************************
     */
    public void incluiAtributoVariosValores(
            String idLDAP,
            String nomeAtributo,
            String valorAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        LDAPConnection ld = null;

        if (idLDAP.equals("") || nomeAtributo.equals("") || valorAtributo.equals("") || loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        try {
            incluiAtributoVariosValores(ld, idLDAP, nomeAtributo, valorAtributo);
        } catch (Exception e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        } finally {
            pool.close(ld);
        }
    }

    /**
     * ******************************************
     */
    private void incluiAtributoVariosValores(
            LDAPConnection ld,
            String idLDAP,
            String nomeAtributo,
            String valorAtributo)
            throws ImplManutLDAPException {

        String filtro = "";
        String dn = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributoNew = null;
        LDAPAttribute atributoOld = null;
        LDAPModificationSet manyChanges = new LDAPModificationSet();
        String valorOldAtributo = "";
        boolean achou = false;

        try {

            if (idLDAP.equals("")
                    || nomeAtributo.equals("")
                    || valorAtributo.equals("")) {
                throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
            }

            filtro = "(idLDAP=" + idLDAP + ")";

            findEntry = getEntry(ld, filtro);
            if (findEntry == null) {
                throw (new ImplManutLDAPException(
                        "Nao ha cadastro no Directory Server com este idLDAP : "
                        + idLDAP));
            }

            atributoOld = findEntry.getAttribute(nomeAtributo);
            if (atributoOld != null) {
                Enumeration e = atributoOld.getStringValues();
                while (e.hasMoreElements()) {
                    valorOldAtributo = (String) e.nextElement();
                    if (valorOldAtributo.equals(valorAtributo)) {
                        achou = true;
                        // System.out.println("valorAtributo ja existe: " + valorAtributo) ;
                    }
                }
            }

            if (!achou) {
                // System.out.println("valorAtributo nao existe, sera incluido: " + valorAtributo) ;
                dn = findEntry.getDN();
                atributoNew = new LDAPAttribute(nomeAtributo, valorAtributo);
                manyChanges.add(LDAPModification.ADD, atributoNew);
                ld.modify(dn, manyChanges);
            }

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        }

    }

    /**
     * ************************************
     */
    public void removeAtributoVariosValores(
            String idLDAP,
            String nomeAtributo,
            String valorAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        LDAPConnection ld = null;

        if (idLDAP.equals("") || nomeAtributo.equals("") || valorAtributo.equals("") || loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        try {
            removeAtributoVariosValores(ld, idLDAP, nomeAtributo, valorAtributo);
        } catch (Exception e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        } finally {
            pool.close(ld);
        }
    }

    /**
     * ******************************************
     */
    private void removeAtributoVariosValores(
            LDAPConnection ld,
            String idLDAP,
            String nomeAtributo,
            String valorAtributo)
            throws ImplManutLDAPException {

        String filtro = "";
        String dn = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributoNew = null;
        LDAPAttribute atributoOld = null;
        LDAPModificationSet manyChanges = new LDAPModificationSet();
        String valorOldAtributo = "";
        boolean achou = false;

        try {

            if (idLDAP.equals("")
                    || nomeAtributo.equals("")
                    || valorAtributo.equals("")) {
                throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
            }

            filtro = "(idLDAP=" + idLDAP + ")";

            findEntry = getEntry(ld, filtro);
            if (findEntry == null) {
                throw (new ImplManutLDAPException(
                        "Nao ha cadastro no Directory Server com este idLDAP : "
                        + idLDAP));
            }

            atributoOld = findEntry.getAttribute(nomeAtributo);
            if (atributoOld != null) {
                Enumeration e = atributoOld.getStringValues();
                while (e.hasMoreElements()) {
                    valorOldAtributo = (String) e.nextElement();
                    if (valorOldAtributo.equals(valorAtributo)) {
                        achou = true;
                        // System.out.println("valorAtributo ja existe: " + valorAtributo) ;
                    }
                }
            }

            if (achou) {
                // System.out.println("valorAtributo existe, sera excluido: " + valorAtributo) ;
                dn = findEntry.getDN();
                atributoNew = new LDAPAttribute(nomeAtributo, valorAtributo);
                manyChanges.add(LDAPModification.DELETE, atributoNew);
                ld.modify(dn, manyChanges);
            }

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        }

    }

    /**
     * ************************************
     */
    public Vector getAtributosVariosValores(
            String idLDAP,
            String nomeAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        LDAPConnection ld = null;
        Vector vValores = new Vector();

        if (idLDAP.equals("") || nomeAtributo.equals("") || loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        try {
            vValores = getAtributosVariosValores(ld, idLDAP, nomeAtributo);
        } catch (Exception e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        } finally {
            pool.close(ld);
        }

        return vValores;

    }

    /**
     * ******************************************
     */
    private Vector getAtributosVariosValores(
            LDAPConnection ld,
            String idLDAP,
            String nomeAtributo)
            throws ImplManutLDAPException {

        String filtro = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributos = null;
        Enumeration eValores = null;
        Vector vValores = new Vector();
        String valorAtributo = "";

        if (idLDAP.equals("") || nomeAtributo.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        filtro = "(idLDAP=" + idLDAP + ")";
        findEntry = getEntry(ld, filtro);
        if (findEntry != null) {
            atributos = findEntry.getAttribute(nomeAtributo);
            if (atributos != null) {
                eValores = atributos.getStringValues();
                while (eValores.hasMoreElements()) {
                    valorAtributo = (String) eValores.nextElement();
                    vValores.addElement(valorAtributo);
                }
            }
        }

        return vValores;

    }

    /**
     * ******************************************
     */
    public void alteraAtributo(
            String idLDAP,
            String nomeAtributo,
            String valorAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: alteraAtributo";
        mensagemLOG += " idLDAP: " + idLDAP;
        mensagemLOG += " nomeAtributo: " + nomeAtributo;
        mensagemLOG += " valorAtributo: " + valorAtributo;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        /*
        try {
    
          ld.connect(MY_HOST, MY_PORT);
         */
        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        alteraAtributo(ld, idLDAP, nomeAtributo, valorAtributo);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
              
        } finally {
          try {
            ld.disconnect();
          } catch (LDAPException e) {
            new ImplManutLDAPException(
              "Alteracao realizada, mas nao foi possivel desconectar da LDAP : "
                + e.toString());
          }
        }
         */
    }

    /**
     * ******************************************
     */
    private void alteraAtributo(
            LDAPConnection ld,
            String idLDAP,
            String nomeAtributo,
            String valorAtributo)
            throws ImplManutLDAPException {

        String filtro = "";
        String dn = "";
        String valorOldAtributo = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        LDAPModificationSet manyChanges = new LDAPModificationSet();

        try {

            if (idLDAP.equals("")
                    || nomeAtributo.equals("")
                    || valorAtributo.equals("")) {
                throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
            }

            filtro = "(idLDAP=" + idLDAP + ")";

            findEntry = getEntry(ld, filtro);
            if (findEntry == null) {
                throw (new ImplManutLDAPException(
                        "Nem Pessoa nem Lotacao cadastrada no Directory Server com este idLDAP : "
                        + idLDAP));
            }

            dn = findEntry.getDN();

            atributo = findEntry.getAttribute(nomeAtributo);

            if (atributo == null) {
                throw (new ImplManutLDAPException(
                        "Pessoa ou Lotacao nao possui o atributo " + nomeAtributo));
            }

            Enumeration e = atributo.getStringValues();
            valorOldAtributo = (String) e.nextElement();

            atributo = new LDAPAttribute(nomeAtributo, valorOldAtributo);
            manyChanges.add(LDAPModification.DELETE, atributo);

            atributo = new LDAPAttribute(nomeAtributo, valorAtributo);
            manyChanges.add(LDAPModification.ADD, atributo);
            ld.modify(dn, manyChanges);

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        }

    }

    /**
     * ******************************************
     */
    public void removeAtributo(
            String idLDAP,
            String nomeAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: removeAtributo";
        mensagemLOG += " idLDAP: " + idLDAP;
        mensagemLOG += " nomeAtributo: " + nomeAtributo;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        if (idLDAP.equals("")
                || nomeAtributo.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        // LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        removeAtributo(ld, idLDAP, nomeAtributo);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
    
          try {
            ld.disconnect();
          } catch (LDAPException e) {
            new ImplManutLDAPException(
              "Remocao realizada, mas nao foi possivel desconectar da LDAP : "
                + e.toString());
          }
        }
         */
    }

    /**
     * ******************************************
     */
    private void removeAtributo(
            LDAPConnection ld,
            String idLDAP,
            String nomeAtributo)
            throws ImplManutLDAPException {

        String dnPessoa = "";
        String valorOldAtributo = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        LDAPModificationSet manyChanges = new LDAPModificationSet();

        try {

            if (idLDAP.equals("") || nomeAtributo.equals("")) {
                throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
            }

            findEntry = getEntryPessoa(ld, "idLDAP", idLDAP);
            if (findEntry == null) {
                throw (new ImplManutLDAPException(
                        "Pessoa nao cadastrada no Directory Server com este idLDAP : "
                        + idLDAP));
            }

            dnPessoa = findEntry.getDN();
            atributo = findEntry.getAttribute(nomeAtributo);

            if (atributo == null) {
                throw (new ImplManutLDAPException(
                        "Pessoa nao possui o atributo " + nomeAtributo));
            }

            Enumeration e = atributo.getStringValues();
            valorOldAtributo = (String) e.nextElement();
            atributo = new LDAPAttribute(nomeAtributo, valorOldAtributo);
            manyChanges.add(LDAPModification.DELETE, atributo);
            ld.modify(dnPessoa, manyChanges);

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        }

    }

    /**
     * ******************************************
     */
    public void movePessoa(
            String idLDAP,
            String idNovaLotacao,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));

        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: movePessoa";
        mensagemLOG += " idLDAP: " + idLDAP;
        mensagemLOG += " idNovaLotacao: " + idNovaLotacao;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        String dnAtual = "";
        String dnNovo = "";
        String dnLotacaoNova = "";
        String uid = "";
        String rdnNovo = "";
        String gidNumber = "";
        String ou = "";
        boolean deletaOldRDN = true;
        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        LDAPModificationSet manyChanges = null;
        String idLotacaoAtual = "";
        String dnLotacaoAtual = "";
        Enumeration en = null;

        if (idLDAP.equals("")
                || idNovaLotacao.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        // LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        findEntry = getEntryPessoa(ld, "idLDAP", idLDAP);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Pessoa nao cadastrada no Directory Server com este idLDAP : "
                    + idLDAP));
        }

        dnAtual = findEntry.getDN();

        atributo = findEntry.getAttribute("idLotacao");
        en = atributo.getStringValues();
        idLotacaoAtual = (String) en.nextElement();

        atributo = findEntry.getAttribute("uid");
        en = atributo.getStringValues();
        uid = (String) en.nextElement();

        findEntry = getEntryLotacao(ld, "idLotacao", idLotacaoAtual);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Lotacao nao cadastrada no Directory Server com este id : "
                    + idLotacaoAtual));
        }
        dnLotacaoAtual = findEntry.getDN();

        findEntry = getEntryLotacao(ld, "idLotacao", idNovaLotacao);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Lotacao nao cadastrada no Directory Server com este id : "
                    + idNovaLotacao));
        }
        dnLotacaoNova = findEntry.getDN();

        String descricaoLotacao = "";
        descricaoLotacao = getAtributo(ld, "idLotacao", idNovaLotacao, "description", "lotacao");
        if ((descricaoLotacao == null) || (descricaoLotacao.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Descricao da lotacao (identificador " + idNovaLotacao + " ), nao encontrada"));
        }

        // ou = getOU(dnLotacaoNova);
        ou = descricaoLotacao;
        atributo = findEntry.getAttribute("gidNumber");
        if (atributo != null) {
            en = atributo.getStringValues();
            gidNumber = (String) en.nextElement();
        }
        rdnNovo = "uid=" + uid;
        deletaOldRDN = true;
        dnNovo = rdnNovo + "," + dnLotacaoNova;

        try {
            ld.rename(dnAtual, rdnNovo, dnLotacaoNova, deletaOldRDN);
            alteraAtributo(ld, idLDAP, "gidNumber", gidNumber);
            alteraAtributo(ld, idLDAP, "idLotacao", idNovaLotacao);
            alteraAtributo(ld, idLDAP, "ou", ou);
            alteraAtributo(ld, idLDAP, "dnTRT15", dnNovo);

            // Remove o atributo memberUid na antiga lotacao mae
            manyChanges = new LDAPModificationSet();
            atributo = new LDAPAttribute("memberUid", uid);
            manyChanges.add(LDAPModification.DELETE, atributo);
            try {
                ld.modify(dnLotacaoAtual, manyChanges);
            } catch (LDAPException e) {

            }

            // Remove o atributo uniqueMember na antiga lotacao mae
            manyChanges = new LDAPModificationSet();
            atributo = new LDAPAttribute("uniqueMember", dnAtual);
            manyChanges.add(LDAPModification.DELETE, atributo);
            try {
                ld.modify(dnLotacaoAtual, manyChanges);
            } catch (LDAPException e) {

            }

            // Inclui o atributo memberUid na nova lotacao mae
            manyChanges = new LDAPModificationSet();
            atributo = new LDAPAttribute("memberUid", uid);
            manyChanges.add(LDAPModification.ADD, atributo);
            ld.modify(dnLotacaoNova, manyChanges);

            // Inclui o atributo uniqueMember na nova lotacao mae
            manyChanges = new LDAPModificationSet();
            atributo = new LDAPAttribute("uniqueMember", dnNovo);
            manyChanges.add(LDAPModification.ADD, atributo);
            ld.modify(dnLotacaoNova, manyChanges);

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        } finally {
            pool.close(ld);
        }

    }

    /**
     * ******************************************
     */
    public boolean validaSenha(String login, String senha)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: validaSenha";
        mensagemLOG += " login: " + login;
        vMensagensLog.add(mensagemLOG + " INICIO");

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        boolean valida = false;

        LDAPEntry findEntry = null;
        String DN = "";

        /*
        try {
    
          ld.connect(MY_HOST, MY_PORT);
         */
        if (login.equals("") || senha.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        try {
            ld.authenticate(3, "", "");
        } catch (LDAPException e) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Problema de autenticacao no Directory Server como anonimo: "
                    + e.toString()));
        }

        findEntry = getEntryPessoa(ld, "uid", login);

        if (findEntry == null) {
            // System.out.println("Pessoa nao cadastrada no Directory Server com este login: " + login));
            valida = false;
        } else {
            DN = findEntry.getDN();
            try {
                ld.authenticate(3, DN, senha);
                valida = true;
            } catch (LDAPException e) {
                valida = false;
            }
        }

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
         
          try {
            ld.disconnect();
          } catch (LDAPException e) {
            throw (
              new ImplManutLDAPException(
                "Senha valida, mas nao foi possivel desconectar da LDAP: "
                  + e.toString()));
          }
        }
         */
        vMensagensLog.clear();
        vMensagensLog.add(mensagemLOG + " FINAL");

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        return valida;

    }

    /**
     * ******************************************
     */
    private String getAtributo(
            LDAPConnection ld,
            String nomeAtributo,
            String valorAtributo,
            String nomeAtributoRetorno,
            String tipoEntry) {

        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        String valorAtributoRetorno = "";
        String cDN = "";

        try {

            if (nomeAtributo.equals("")
                    || valorAtributo.equals("")
                    || nomeAtributoRetorno.equals("")
                    || tipoEntry.equals("")) {
                return "";
            }

            if (tipoEntry.toUpperCase().equals("PESSOA")) {
                findEntry = getEntryPessoa(ld, nomeAtributo, valorAtributo);
            }

            if (tipoEntry.toUpperCase().equals("LOTACAO")) {
                findEntry = getEntryLotacao(ld, nomeAtributo, valorAtributo);
            }

            if (tipoEntry.toUpperCase().equals("GRUPOMAIL")) {
                findEntry = getEntryGrupoMail(ld, nomeAtributo, valorAtributo);
            }

            if (findEntry == null) {
                //throw (new ImplManutLDAPException("Lotacao nao cadastrada no Directory Server"));
                return "";
            }

            if (nomeAtributoRetorno.toUpperCase().equals("DN")) {
                cDN = findEntry.getDN();
                valorAtributoRetorno = cDN;
            } else {
                atributo = findEntry.getAttribute(nomeAtributoRetorno);
                if (atributo == null) {
                    //throw (new ImplManutLDAPException("Atributo " + nomeAtributoRetorno + " nao encontrado"));
                    return "";
                }
                Enumeration e = atributo.getStringValues();
                valorAtributoRetorno = (String) e.nextElement();
            }

        } catch (Exception e) {
            //throw (new ImplManutLDAPException("Problema com o Directory Server: " + e.toString()));
            return "";
        }

        return valorAtributoRetorno;

    }

    /**
     * ******************************************
     */
    public String getAtributo(
            String nomeAtributo,
            String valorAtributo,
            String nomeAtributoRetorno,
            String tipoEntry,
            String loginAdministrador,
            String senhaAdministrador)
            throws RemoteException, ImplManutLDAPException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: getAtributo";
        mensagemLOG += " nomeAtributo: " + nomeAtributo;
        mensagemLOG += " valorAtributo: " + valorAtributo;
        mensagemLOG += " nomeAtributoRetorno: " + nomeAtributoRetorno;
        mensagemLOG += " tipoEntry: " + tipoEntry;
        vMensagensLog.add(mensagemLOG);

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        LDAPConnection ld = null;

        String valorAtributoRetorno = "";

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        if (nomeAtributo.equals("")
                || valorAtributo.equals("")
                || nomeAtributoRetorno.equals("")
                || tipoEntry.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            //throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
            return "";
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        try {
            logar(ld, loginAdministrador, senhaAdministrador);
        } catch (ImplManutLDAPException e) {
            pool.close(ld);
            return "";
        }

        valorAtributoRetorno
                = getAtributo(
                        ld,
                        nomeAtributo,
                        valorAtributo,
                        nomeAtributoRetorno,
                        tipoEntry);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (new ImplManutLDAPException("Erro de conexao com a LDAP: " + e.toString()));
        } finally {
    
          try {
             ld.disconnect();
          } catch (LDAPException e) {
             new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
          }
        }
         */
        return valorAtributoRetorno;

    }

    /**
     * ******************************************
     */
    public void excluiEstacao(
            String uid,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/ldap/excluiEstacao.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));

        mensagemLOG += " metodo: excluiEstacao";
        mensagemLOG += " uid: " + uid;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (uid.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        try {

            logar(ld, loginAdministrador, senhaAdministrador);
            String dnOperador = "";
            String dnMae = "";
            String dnEstacao = "";
            boolean Vara = false;
            LDAPEntry findEntry = null;
            LDAPAttribute atributo = null;
            Consultas fenix = new Consultas();
            String cIdLDAPoperador = "";
            int categoriaOperadorCentralChamados = 0;

            if (!uid.endsWith("$")) {
                uid = uid + "$";
            }

            cIdLDAPoperador = getAtributo(ld, "uid", loginAdministrador, "idLDAP", "pessoa");

            categoriaOperadorCentralChamados = fenix.getCategoriaUsuarioCentralChamados(cIdLDAPoperador);

            /**
             * *************************************************************
             * Categorias existentes na Central de Chamados em outubro/2008: 1 -
             * Super-Administrador 2 - Administrador 3 - Administrador da
             * Lotacao Solicitante 4 - Operador 5 - Solicitante
             */
            if (categoriaOperadorCentralChamados <= 0) {
                throw (new ImplManutLDAPException("<br><br> " + loginAdministrador.toUpperCase()
                        + ", vocÃƒÂª nÃƒÂ£o tem direito de excluir estaÃƒÂ§ÃƒÂµes Windows "
                        + "no domÃƒÂ­nio da Rede Microsoft. Os mesmos operadores que podem abrir chamados no "
                        + "Sistema da Central de Chamados podem excluir estaÃƒÂ§ÃƒÂµes Windows. Geralmente o "
                        + "Diretor e/ou seu substituto podem dar esse direito, no sistema da "
                        + "Central de Chamados. <br>"));
            }

            dnOperador = getAtributo(ld, "uid", loginAdministrador, "dn", "pessoa");

            Vara = (dnOperador.indexOf("ou=1grau") > -1);

            if (Vara) {

                StringTokenizer st = new StringTokenizer(dnOperador, ",");

                int qtd = st.countTokens();
                String aComponentes[] = new String[qtd];
                for (int i = 0; i < qtd; i++) {
                    aComponentes[i] = st.nextToken();
                }

                dnMae = "ou=estacoes," + aComponentes[qtd - 3] + "," + aComponentes[qtd - 2] + "," + aComponentes[qtd - 1];

            } else {
                dnMae = "ou=estacoes, o=trt15";
            }

            dnEstacao = "uid=" + uid + "," + dnMae;

            ld.delete(dnEstacao);

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Erro de conexao com o LDAP: " + e.toString()));
        } catch (Exception ex) {
            throw (new ImplManutLDAPException(
                    "Erro generico: " + ex.toString()));
        } finally {
            if (ld != null) {
                pool.close(ld);
            }
        }

    }

    /**
     * ******************************************
     */
    public void incluiEstacao(
            String uid,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/ldap/incluiEstacao.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));

        mensagemLOG += " metodo: incluiEstacao";
        mensagemLOG += " uid: " + uid;
        vMensagensLog.add(mensagemLOG);

        try {
            //Pega o driver correto de acordo com o BD
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException cnfe) {
            throw (new ImplManutLDAPException(
                    "ClassNotFoundException: " + cnfe.getMessage()));
        }

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (uid.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("<br><br> Nenhum valor de entrada pode ser nulo <br>"));
        }

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                if (ld != null) {
                    pool.close(ld);
                }
                throw new ImplManutLDAPException(
                        "<br><br> Erro de conexao tanto com o LDAP Master quanto com o Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        try {

            logar(ld, loginAdministrador, senhaAdministrador);
            int uidNumber = 0;
            String dnOperador = "";
            String dnMae = "";
            String dnEstacao = "";
            String cGidNumber = "";
            int gIdNumber = 0;
            boolean Vara = false;
            LDAPEntry findEntry = null;
            LDAPAttribute atributo = null;
            String cIdLDAP = "";
            Consultas fenix = new Consultas();
            String cIdLDAPoperador = "";
            int categoriaOperadorCentralChamados = 0;

            if (!uid.endsWith("$")) {
                uid = uid + "$";
            }

            cIdLDAPoperador = getAtributo(ld, "uid", loginAdministrador, "idLDAP", "pessoa");

            categoriaOperadorCentralChamados = fenix.getCategoriaUsuarioCentralChamados(cIdLDAPoperador);

            /**
             * *************************************************************
             * Categorias existentes na Central de Chamados em outubro/2008: 1 -
             * Super-Administrador 2 - Administrador 3 - Administrador da
             * Lotacao Solicitante 4 - Operador 5 - Solicitante
             */
            if (categoriaOperadorCentralChamados <= 0) {
                throw (new ImplManutLDAPException("<br><br> " + loginAdministrador.toUpperCase()
                        + ", vocÃƒÂª nÃƒÂ£o tem direito de incluir estaÃƒÂ§ÃƒÂµes Windows "
                        + "no domÃƒÂ­nio da Rede Microsoft. Os mesmos operadores que podem abrir chamados no "
                        + "Sistema da Central de Chamados podem incluir estaÃƒÂ§ÃƒÂµes Windows. Geralmente o "
                        + "Diretor e/ou seu substituto podem dar esse direito, no sistema da "
                        + "Central de Chamados. <br>"));
            }

            dnOperador = getAtributo(ld, "uid", loginAdministrador, "dn", "pessoa");

            Vara = (dnOperador.indexOf("ou=1grau") > -1);

            if (Vara) {

                StringTokenizer st = new StringTokenizer(dnOperador, ",");

                int qtd = st.countTokens();
                String aComponentes[] = new String[qtd];
                for (int i = 0; i < qtd; i++) {
                    aComponentes[i] = st.nextToken();
                }

                dnMae = "ou=estacoes," + aComponentes[qtd - 3] + "," + aComponentes[qtd - 2] + "," + aComponentes[qtd - 1];

            } else {
                dnMae = "ou=estacoes, o=trt15";
            }

            dnEstacao = "uid=" + uid + "," + dnMae;

            if (Vara) {
                try {
                    findEntry = ld.read(dnMae);
                    if (findEntry == null) {
                        throw (new ImplManutLDAPException(
                                "<br><br> Nao cadastrada no ldap: " + dnMae + " <br>"));
                    }
                    atributo = findEntry.getAttribute("gidNumber");
                    if (atributo == null) {
                        throw (new ImplManutLDAPException(
                                "<br><br> Atributo gidNumber nao cadastrado no ldap para " + dnEstacao + " <br>"));
                    } else {
                        Enumeration e = atributo.getStringValues();
                        cGidNumber = (String) e.nextElement();
                        gIdNumber = (new Integer(cGidNumber)).intValue();
                    }

                } catch (LDAPException e) {
                    throw (new ImplManutLDAPException(
                            "<br><br> Erro de conexao com a LDAP: " + e.toString() + " <br>"));
                }

            } else {
                gIdNumber = 9999;
                cGidNumber = new Integer(gIdNumber).toString();
            }

            String acctFlags = "[W          ]";
            uidNumber = fenix.getUidNumber();
            String cUidNumber = new Integer(uidNumber).toString();
            String cn = uid;
            String loginShell = "/bin/false";

            int idLDAP = fenix.getGidLDAP();
            cIdLDAP = new Integer(idLDAP).toString();

            int rid = (2 * uidNumber) + 1000;
            String description = "estacao";
            String homeDirectory = "/dev/null";
            String logonTime = "0";
            String pwdCanChange = "0";
            int primaryGroupId = (2 * gIdNumber + 1001);
            String cPrimaryGroupId = new Integer(primaryGroupId).toString();
            String sambaSID = "S-1-5-21-2123005422-2009428996-684742794-" + cIdLDAP;
            String sambaAcctFlags = "[W          ]";
            
            // VariÃ¡veis de atributos adicionadas por Tiago Leal em 22/05/2018
            Long kickoffTime = 9223372036854775807L;            
            Long logoffTime =  kickoffTime;
            Long pwdMustChange = kickoffTime;
            String displayName = uid;
            Long pwdLastSet = System.currentTimeMillis();
            SambaPassword sambaPassword = new SambaPassword();
            String uidNochar = uid.replace("$","");
            String lmPassword = sambaPassword.calcLMHash(uidNochar.toUpperCase());
            String ntPassword = sambaPassword.calcNTLMHash(uidNochar.toLowerCase());
            // ---------------------------------            

            LDAPAttribute attr1 = new LDAPAttribute("acctFlags", acctFlags);
            LDAPAttribute attr2 = new LDAPAttribute("uid", uid);
            LDAPAttribute attr3 = new LDAPAttribute("cn", cn);
            LDAPAttribute attr4 = new LDAPAttribute("homeDirectory", homeDirectory);
            LDAPAttribute attr5 = new LDAPAttribute("uidNumber", cUidNumber);
            LDAPAttribute attr6 = new LDAPAttribute("loginShell", loginShell);
            LDAPAttribute attr7 = new LDAPAttribute("gidNumber", cGidNumber);
            LDAPAttribute attr8 = new LDAPAttribute("idLDAP", cIdLDAP);
            LDAPAttribute attr9 = new LDAPAttribute("rid", "" + rid);
            LDAPAttribute attr10 = new LDAPAttribute("description", description);
            LDAPAttribute attr11 = new LDAPAttribute("logonTime", logonTime);
            LDAPAttribute attr12 = new LDAPAttribute("pwdCanChange", pwdCanChange);
            LDAPAttribute attr13 = new LDAPAttribute("primaryGroupId", cPrimaryGroupId);
            LDAPAttribute attr14 = new LDAPAttribute("sambaSID", sambaSID);
            LDAPAttribute attr15 = new LDAPAttribute("sambaAcctFlags", sambaAcctFlags);

            String[] atributos
                    = {
                        "estacaoTRT15",
                        "posixAccount",
                        "sambaAccount",
                        "sambaSamAccount"
                    };

            LDAPAttribute attr16 = new LDAPAttribute("objectclass", atributos);
            
            LDAPAttribute attr17 = new LDAPAttribute("kickoffTime", kickoffTime.toString());
            LDAPAttribute attr18 = new LDAPAttribute("logoffTime", logoffTime.toString());
            LDAPAttribute attr19 = new LDAPAttribute("pwdMustChange", pwdMustChange.toString());
            LDAPAttribute attr20 = new LDAPAttribute("displayName", displayName);
            LDAPAttribute attr21 = new LDAPAttribute("pwdLastSet", pwdLastSet.toString());
            LDAPAttribute attr22 = new LDAPAttribute("lmPassword", lmPassword);
            LDAPAttribute attr23 = new LDAPAttribute("ntPassword", ntPassword);            

            LDAPAttributeSet myAttrs = new LDAPAttributeSet();
            myAttrs.add(attr1);
            myAttrs.add(attr2);
            myAttrs.add(attr3);
            myAttrs.add(attr4);
            myAttrs.add(attr5);
            myAttrs.add(attr6);
            myAttrs.add(attr7);
            myAttrs.add(attr8);
            myAttrs.add(attr9);
            myAttrs.add(attr10);
            myAttrs.add(attr11);
            myAttrs.add(attr12);
            myAttrs.add(attr13);
            myAttrs.add(attr14);
            myAttrs.add(attr15);
            myAttrs.add(attr16);
            myAttrs.add(attr17);
            myAttrs.add(attr18);
            myAttrs.add(attr19);
            myAttrs.add(attr20);
            myAttrs.add(attr21);
            myAttrs.add(attr22);
            myAttrs.add(attr23);

            LDAPEntry myEntry = new LDAPEntry(dnEstacao, myAttrs);

            ld.add(myEntry);

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "<br> Erro de conexao com o LDAP: " + e.toString() + " <br>"));

        } catch (Exception ex) {
            throw (new ImplManutLDAPException(
                    "<br><br> Erro generico: " + ex.toString() + " <br>"));
        } finally {
            if (ld != null) {
                pool.close(ld);
            }
        }

        // Desativado por Tiago Leal em 22/05/2018
        //alteraSenhaEstacao(uid);

    }

    /**
     * ******************************************
     */
    public void incluiPessoa(
            String nomeCompleto,
            String uid,
            String idLotacao,
            String categoria,
            String idLDAP,
            String idPessoa,
            String userPassword,
            String uidNumber,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        String comando = "";
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: incluiPessoa";
        mensagemLOG += " nomeCompleto: " + nomeCompleto;
        mensagemLOG += " uid: " + uid;
        mensagemLOG += " idLotacao: " + idLotacao;
        mensagemLOG += " idLDAP: " + idLDAP;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (nomeCompleto.equals("")
                || uid.equals("")
                || idLotacao.equals("")
                || categoria.equals("")
                || idLDAP.equals("")
                || userPassword.equals("")
                || uidNumber.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        String cn = transformaNome(nomeCompleto);

        String dn = "";

        dn = getAtributo(ld, "uid", uid, "dn", "pessoa");

        if (dn != null) {
            if (!dn.trim().equals("")) {
                pool.close(ld);
                throw (new ImplManutLDAPException("login " + uid + " ja cadastrado"));
            }
        }

        String dnLotacao = "";
        dnLotacao = getAtributo(ld, "idLotacao", idLotacao, "dn", "lotacao");
        if (dn != null) {
            if (!dn.trim().equals("")) {
                pool.close(ld);
                throw (new ImplManutLDAPException(
                        "A lotacao (identificador " + idLotacao + " ), nao encontrada"));
            }
        }

        String descricaoLotacao = "";
        descricaoLotacao = getAtributo(ld, "idLotacao", idLotacao, "description", "lotacao");
        if ((descricaoLotacao == null) || (descricaoLotacao.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Descricao da lotacao (identificador " + idLotacao + " ), nao encontrada"));
        }

        dn = getAtributo(ld, "idLDAP", idLDAP, "dn", "pessoa");
        if (dn != null) {
            if (!dn.trim().equals("")) {
                pool.close(ld);
                throw (new ImplManutLDAPException("idLDAP " + idLDAP + " ja cadastrado"));
            }
        }

        String sn = ultimoNome(cn);
        String pn = primeirosNomes(cn);

        /*
    String sn = "";
    StringTokenizer st = new StringTokenizer(cn);
    while (st.hasMoreTokens()) {
      sn = st.nextToken();
    }
         */
        String gecos = cn;
        String mailRoutingAddress = uid + "@mail.trt15.jus.br";
        String mail = uid + "@trt15.jus.br";

        String mailLocalAddress1 = uid + "@trt15.gov.br";
        String mailLocalAddress2 = uid + "@trt15.jus.br";
        String mailLocalAddress3 = uid + "@trtcamp.jus.br";
        String mailLocalAddress4 = uid + "@jtcamp.jus.br";
        String[] mailLocalAddress = {mailLocalAddress1, mailLocalAddress2, mailLocalAddress3, mailLocalAddress4};

        String homeDirectory = "/home/" + uid;
        String loginShell = "/bin/false";
        int rid = 0;
        String gidNumber
                = getAtributo(
                        "idLotacao",
                        idLotacao,
                        "gidNumber",
                        "lotacao",
                        loginAdministrador,
                        senhaAdministrador);
        // String ou = getOU(dnLotacao);
        String ou = descricaoLotacao;
        dn = "uid=" + uid + "," + dnLotacao;
        String sambaSid = "S-1-5-21-2123005422-2009428996-684742794-" + idLDAP;
        String sambaAcctFlags = "[U          ]";
        String sambaPasswordHistory = "0000000000000000000000000000000000000000000000000000000000000000";
        String sambaPwdLastSet = "1219935537";
        String sambaNTPassword = "X";
        String sambaLMPassword = "X";
        
        // VariÃ¡veis de atributos adicionadas por Tiago Leal em 24/05/2018    
        // Atributos necessÃ¡rios pelo Samba2 nÃ£o serÃ£o adicionados. SÃ£o Eles:
        // acctFlags, kickoffTime, logoffTime, logonTime, primaryGroupID, pwdCanChange, pwdLastSet, pwdMustChange,  
        String displayName = cn;
        String ntPassword = "X";
        String lmPassword = "X";        

        logar(ld, loginAdministrador, senhaAdministrador);

        LDAPAttribute attr1 = new LDAPAttribute("idLDAP", idLDAP);
        LDAPAttribute attr2 = new LDAPAttribute("idLotacao", idLotacao);
        LDAPAttribute attr3 = new LDAPAttribute("cn", cn);
        LDAPAttribute attr4 = new LDAPAttribute("sn", sn);
        LDAPAttribute attr5 = new LDAPAttribute("uid", uid);
        LDAPAttribute attr6 = new LDAPAttribute("userPassword", userPassword);
        LDAPAttribute attr7 = new LDAPAttribute("mailRoutingAddress", mailRoutingAddress);
        LDAPAttribute attr8 = new LDAPAttribute("mailLocalAddress", mailLocalAddress);
        LDAPAttribute attr9 = new LDAPAttribute("homeDirectory", homeDirectory);
        LDAPAttribute attr10 = new LDAPAttribute("uidNumber", uidNumber);
        LDAPAttribute attr11 = new LDAPAttribute("gecos", gecos);
        LDAPAttribute attr12 = new LDAPAttribute("loginShell", loginShell);
        LDAPAttribute attr13 = new LDAPAttribute("mail", mail);
        LDAPAttribute attr14 = new LDAPAttribute("rid", "" + rid);
        LDAPAttribute attr15 = new LDAPAttribute("gidNumber", gidNumber);
        LDAPAttribute attr16 = new LDAPAttribute("ou", ou);
        LDAPAttribute attr17 = new LDAPAttribute("categoria", categoria);

        String[] atributos
                = {
                    "top",
                    "person",
                    "inetOrgPerson",
                    "organizationalPerson",
                    "inetLocalMailRecipient",
                    "posixAccount",
                    "sambaAccount",
                    "SambaSamAccount",
                    "personTRT15"};

        LDAPAttribute attr18 = new LDAPAttribute("objectclass", atributos);

        LDAPAttribute attr19 = new LDAPAttribute("pn", pn);

        LDAPAttribute attr20 = new LDAPAttribute("sambaSid", sambaSid);
        LDAPAttribute attr21 = new LDAPAttribute("sambaAcctFlags", sambaAcctFlags);
        LDAPAttribute attr22 = new LDAPAttribute("sambaPasswordHistory", sambaPasswordHistory);
        LDAPAttribute attr23 = new LDAPAttribute("sambaPwdLastSet", sambaPwdLastSet);
        LDAPAttribute attr24 = new LDAPAttribute("sambaNTPassword", sambaNTPassword);
        LDAPAttribute attr25 = new LDAPAttribute("sambaLMPassword", sambaLMPassword);

        LDAPAttribute attr26 = new LDAPAttribute("dnTRT15", dn);

        LDAPAttribute attr27 = null;
        if (idPessoa != null && (!idPessoa.equals(""))) {
            attr27 = new LDAPAttribute("employeeNumber", idPessoa);
        }

        // Atributos adicionados por Tiago Leal em 24/05/2018
        LDAPAttribute attr28 = new LDAPAttribute("displayName", displayName);
        LDAPAttribute attr29 = new LDAPAttribute("ntPassword", ntPassword);
        LDAPAttribute attr30 = new LDAPAttribute("lmPassword", lmPassword);
        
        LDAPAttributeSet myAttrs = new LDAPAttributeSet();
        myAttrs.add(attr1);
        myAttrs.add(attr2);
        myAttrs.add(attr3);
        myAttrs.add(attr4);
        myAttrs.add(attr5);
        myAttrs.add(attr6);
        myAttrs.add(attr7);
        myAttrs.add(attr8);
        myAttrs.add(attr9);
        myAttrs.add(attr10);
        myAttrs.add(attr11);
        myAttrs.add(attr12);
        myAttrs.add(attr13);
        myAttrs.add(attr14);
        myAttrs.add(attr15);
        myAttrs.add(attr16);
        myAttrs.add(attr17);
        myAttrs.add(attr18);
        myAttrs.add(attr19);
        myAttrs.add(attr20);
        myAttrs.add(attr21);
        myAttrs.add(attr22);
        myAttrs.add(attr23);
        myAttrs.add(attr24);
        myAttrs.add(attr25);
        myAttrs.add(attr26);

        if (idPessoa != null && (!idPessoa.equals(""))) {
            myAttrs.add(attr27);
        }
        
        myAttrs.add(attr28);
        myAttrs.add(attr29);
        myAttrs.add(attr30);        

        LDAPEntry myEntry = new LDAPEntry(dn, myAttrs);

        try {
            // Inclui a pessoa
            ld.add(myEntry);
            alteraSenha(ld, uid, userPassword);

            // Inclui o atributo memberUid na lotacao mae
            LDAPModificationSet manyChanges = new LDAPModificationSet();
            attr1 = new LDAPAttribute("memberUid", uid);
            manyChanges.add(LDAPModification.ADD, attr1);
            try {
                ld.modify(dnLotacao, manyChanges);
            } catch (LDAPException ex) {
            }

            // Inclui o atributo uniqueMember na lotacao mae
            manyChanges = new LDAPModificationSet();
            attr1 = new LDAPAttribute("uniqueMember", dn);
            manyChanges.add(LDAPModification.ADD, attr1);
            try {
                ld.modify(dnLotacao, manyChanges);
            } catch (LDAPException ex) {
            }

            // Iguala as senhas do samba3 com as do samba2
            // Implementado na rotina de alteracao de senha!!!
/*      
      sambaNTPassword = this.getAtributo(ld, "idLDAP", idLDAP, "ntPassword", "PESSOA") ;
      sambaLMPassword = this.getAtributo(ld, "idLDAP", idLDAP, "lmPassword", "PESSOA") ;
      this.alteraAtributo(ld, idLDAP, "sambaNTPassword", sambaNTPassword) ;
      this.alteraAtributo(ld, idLDAP, "sambaLMPassword", sambaLMPassword) ;
             */
        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
            pool.close(ld);
        }

        /*    
    sambaNTPassword = this.getAtributo("idLDAP", idLDAP, "ntPassword", "PESSOA", loginAdministrador, senhaAdministrador) ;
    sambaLMPassword = this.getAtributo("idLDAP", idLDAP, "lmPassword", "PESSOA", loginAdministrador, senhaAdministrador) ;
    
    this.alteraAtributo(idLDAP, "sambaNTPassword", sambaNTPassword, loginAdministrador, senhaAdministrador) ;
    this.alteraAtributo(idLDAP, "sambaLMPassword", sambaLMPassword, loginAdministrador, senhaAdministrador) ;
         */
        
        /* REMOVIDO POR TIAGO LEAL EM 21/05/2018
        try {
            comando = "touch /var/spool/mail/" + uid;
            vMensagensLog = executaComando(comando);

            comando = "chmod 600 /var/spool/mail/" + uid;
            vMensagensLog = executaComando(comando);

            comando = "sleep 3";
            vMensagensLog = executaComando(comando);

            comando = "chown " + uid + ".users /var/spool/mail/" + uid;
            vMensagensLog = executaComando(comando);
            gravaMensagensLog(vMensagensLog, cArquivoLog);

        } catch (IOException e) {
            // ignorar erro de gravacao do log
        }
        
        */

    }

    /**
     * ************************************
     */
    public void removePessoa(
            String nomeAtributo,
            String valorAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: removePessoa";
        mensagemLOG += " nomeAtributo: " + nomeAtributo;
        mensagemLOG += " valorAtributo: " + valorAtributo;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (nomeAtributo.equals("")
                || valorAtributo.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        /*
        try {
    
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        String cDNpessoa = "";
        String loginPessoa = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;

        findEntry = getEntryPessoa(ld, nomeAtributo, valorAtributo);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException("Pessoa nao cadastrada no Directory Server"));
        }

        cDNpessoa = findEntry.getDN();
        atributo = findEntry.getAttribute("uid");
        Enumeration e = atributo.getStringValues();
        loginPessoa = (String) e.nextElement();
        try {
            // Remove a pessoa
            ld.delete(cDNpessoa);
            
            // REMOVIDO POR TIAGO LEAL EM 21/05/2018
            //moveMails(loginPessoa, senhaAdministrador);

            // Remove o atributo memberUid na lotacao mae da pessoa
            atributo = findEntry.getAttribute("idLotacao");
            e = atributo.getStringValues();
            String idLotacao = (String) e.nextElement();
            String dnLotacao = getAtributo(ld, "idLotacao", idLotacao, "dn", "lotacao");

            LDAPModificationSet manyChanges = new LDAPModificationSet();
            LDAPAttribute attr1 = new LDAPAttribute("memberUid", loginPessoa);
            manyChanges.add(LDAPModification.DELETE, attr1);
            try {
                ld.modify(dnLotacao, manyChanges);
            } catch (LDAPException ex) {

            }

            // Remove o atributo uniqueMember na lotacao mae da pessoa      
            manyChanges = new LDAPModificationSet();
            attr1 = new LDAPAttribute("uniqueMember", cDNpessoa);
            manyChanges.add(LDAPModification.DELETE, attr1);
            try {
                ld.modify(dnLotacao, manyChanges);
            } catch (LDAPException ex) {

            }

        } catch (LDAPException ex) {
            throw (new ImplManutLDAPException(
                    "Erro de conexao com a LDAP: " + ex.toString()));
        } finally {
            pool.close(ld);
        }

    }

    /**
     * ******************************************
     */
    public void alteraLogin(
            String nomeAtributo,
            String valorAtributo,
            String loginAdministrador,
            String senhaAdministrador,
            String novoLogin)
            throws ImplManutLDAPException, RemoteException {

        Enumeration en = null;
        String idLotacao = "";
        String dnLotacao = "";
        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        String nomeArquivo = "";
        String idLDAP = "";

        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: alteraLogin";
        mensagemLOG += " nomeAtributo: " + nomeAtributo;
        mensagemLOG += " valorAtributo: " + valorAtributo;
        mensagemLOG += " novoLogin: " + novoLogin;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (nomeAtributo.equals("")
                || valorAtributo.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")
                || novoLogin.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        String cDNpessoa = "";
        String loginPessoa = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        LDAPModificationSet manyChanges = new LDAPModificationSet();
        String newRDN = "";

        findEntry = getEntryPessoa(ld, "uid", novoLogin);
        if (findEntry != null) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Ja existe pessoa cadastrada com este login: " + novoLogin));
        }

        findEntry = getEntryPessoa(ld, nomeAtributo, valorAtributo);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException("Pessoa nao cadastrada no Directory Server"));
        }

        cDNpessoa = findEntry.getDN();

        atributo = findEntry.getAttribute("uid");
        en = atributo.getStringValues();
        loginPessoa = (String) en.nextElement();

        atributo = findEntry.getAttribute("idLotacao");
        en = atributo.getStringValues();
        idLotacao = (String) en.nextElement();

        atributo
                = new LDAPAttribute("mailLocalAddress");
        manyChanges.add(LDAPModification.DELETE, atributo);

        atributo
                = new LDAPAttribute("mailLocalAddress", novoLogin + "@trt15.gov.br");
        manyChanges.add(LDAPModification.ADD, atributo);

        atributo
                = new LDAPAttribute("mailLocalAddress", novoLogin + "@trt15.jus.br");
        manyChanges.add(LDAPModification.ADD, atributo);

        atributo
                = new LDAPAttribute("mailLocalAddress", novoLogin + "@trtcamp.jus.br");
        manyChanges.add(LDAPModification.ADD, atributo);

        atributo
                = new LDAPAttribute("mailLocalAddress", novoLogin + "@jtcamp.jus.br");
        manyChanges.add(LDAPModification.ADD, atributo);

        atributo
                = new LDAPAttribute(
                        "mailRoutingAddress",
                        loginPessoa + "@mail.trt15.jus.br");
        manyChanges.add(LDAPModification.DELETE, atributo);

        atributo
                = new LDAPAttribute(
                        "mailRoutingAddress",
                        novoLogin + "@mail.trt15.jus.br");
        manyChanges.add(LDAPModification.ADD, atributo);

        atributo = new LDAPAttribute("mail", loginPessoa + "@trt15.jus.br");
        manyChanges.add(LDAPModification.DELETE, atributo);

        atributo = new LDAPAttribute("mail", novoLogin + "@trt15.jus.br");
        manyChanges.add(LDAPModification.ADD, atributo);

        atributo = new LDAPAttribute("homeDirectory", "/home/" + loginPessoa);
        manyChanges.add(LDAPModification.DELETE, atributo);

        atributo = new LDAPAttribute("homeDirectory", "/home/" + novoLogin);
        manyChanges.add(LDAPModification.ADD, atributo);

        findEntry = getEntryLotacao(ld, "idLotacao", idLotacao);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Lotacao nao cadastrada no Directory Server com este id : "
                    + idLotacao));
        }
        dnLotacao = findEntry.getDN();

        //        Obs.: O atributo uid e alterado atraves do comando rename, executado abaixo depois do modify
        try {
            ld.modify(cDNpessoa, manyChanges);
            newRDN = "uid=" + novoLogin;
            ld.rename(cDNpessoa, newRDN, true);
            // REMOVIDO POR TIAGO LEAL EM 21/05/2018
            // renomeiaMails(loginPessoa, novoLogin, senhaAdministrador);

            // Remove o atributo memberUid na lotacao mae
            manyChanges = new LDAPModificationSet();
            atributo = new LDAPAttribute("memberUid", loginPessoa);
            manyChanges.add(LDAPModification.DELETE, atributo);

            // Inclui o atributo memberUid na lotacao mae
            atributo = new LDAPAttribute("memberUid", novoLogin);
            manyChanges.add(LDAPModification.ADD, atributo);

            // Remove o atributo uniqueMember na lotacao mae
            atributo = new LDAPAttribute("uniqueMember", cDNpessoa);
            manyChanges.add(LDAPModification.DELETE, atributo);

            // Inclui o atributo uniqueMember na lotacao mae
            String dnNovo = newRDN + "," + dnLotacao;
            atributo = new LDAPAttribute("uniqueMember", dnNovo);
            manyChanges.add(LDAPModification.ADD, atributo);

            ld.modify(dnLotacao, manyChanges);

            idLDAP = this.getAtributo(ld, nomeAtributo, valorAtributo, "idLDAP", "pessoa");
            alteraAtributo(ld, idLDAP, "dnTRT15", dnNovo);

        } catch (LDAPException ex) {
            throw (new ImplManutLDAPException(
                    "Erro de conexao com a LDAP: " + ex.toString()));
        } finally {
            pool.close(ld);
        }

        /* REMOVIDO POR TIAGO LEAL EM 21/05/2018
        
        nomeArquivo = "/home/" + novoLogin + "/.openwebmail/webmail/from.book";

        try {
            this.alteraFraseArquivo(nomeArquivo, loginPessoa, novoLogin);
        } catch (IOException ioex) {
            throw new ImplManutLDAPException(
                    "Problemas na abertura do arquivo: " + ioex.toString());
        }

        nomeArquivo = "/home/" + novoLogin + "/.openwebmail/openwebmailrc";

        try {
            this.alteraFraseArquivo(nomeArquivo, loginPessoa, novoLogin);
        } catch (IOException ioex) {
            throw new ImplManutLDAPException(
                    "Problemas na abertura do arquivo: " + ioex.toString());
        }

        nomeArquivo = "/home/" + novoLogin + "/.openwebmail/webmail/search.cache";

        try {
            this.alteraFraseArquivo(nomeArquivo, loginPessoa, novoLogin);
        } catch (IOException ioex) {
            throw new ImplManutLDAPException(
                    "Problemas na abertura do arquivo: " + ioex.toString());
        }
        */

    }

    /**
     * ******************************************
     */
    public void alteraSenha(
            String login,
            String senhaNew,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: alteraSenha (administrador)";
        mensagemLOG += " login: " + login;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (login.equals("")
                || senhaNew.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        if (login.equals("")
                || senhaNew.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")
                || login == null
                || senhaNew == null
                || loginAdministrador == null
                || senhaAdministrador == null) {
            throw (new ImplManutLDAPException("Falha! Nenhum valor de entrada pode ser nulo"));
        }

        if (senhaNew.length() < 5) {
            throw (new ImplManutLDAPException("Falha! A senha deve ter pelo menos 5 caracteres"));
        }

        if (senhaNew.indexOf("#") > -1) {
            throw (new ImplManutLDAPException("Falha! O caracter # nao ÃƒÂ© permitido na senha!"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }


        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        alteraSenha(ld, login, senhaNew);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
                try {
                  ld.disconnect();
                } catch (LDAPException e) {
                  throw (
                    new ImplManutLDAPException(
                      "Senha alterada, mas nao foi possivel desconectar da LDAP: "
                        + e.toString()));
                }
        }
         */
    }

    /**
     * ******************************************
     */
    public void alteraSenha(String login, String senhaOld, String senhaNew)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: alteraSenha (usuario)";
        mensagemLOG += " login: " + login;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (login.equals("") || senhaOld.equals("") || senhaNew.equals("")
                || login == null || senhaOld == null || senhaNew == null) {
            throw (new ImplManutLDAPException("Falha! Nenhum valor de entrada pode ser nulo"));
        }

        if (senhaNew.length() < 5) {
            throw (new ImplManutLDAPException("Falha! A senha deve ter pelo menos 5 caracteres"));
        }

        if (senhaNew.indexOf("#") > -1) {
            throw (new ImplManutLDAPException("Falha! O caracter # nao ÃƒÂ© permitido na senha!"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        String cDNpessoa = "";
        LDAPEntry findEntry = null;
        String senhaAtual = "";
        String senhaOldCrypt = "";
        LDAPAttribute atributo = null;

        try {
            ld.authenticate(3, "", "");
        } catch (LDAPException e) {
            pool.close(ld);
            throw (new ImplManutLDAPException("Falha de autenticacao como anonymous"));
        }

        findEntry = getEntryPessoa(ld, "uid", login);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException("Login ou senha nao confere"));
        }

        cDNpessoa = findEntry.getDN();

        /*      
          try {
            ld.authenticate(3, "cn=admin,o=trt15", "admin-1");
          } catch (LDAPException e) {
            throw (
              new ImplManutLDAPException("login ou senha do super administrador nao confere para autenticacao no Directory Server"));
          }
    
          findEntry = getEntryPessoa(ld, "uid", login);
          atributo = findEntry.getAttribute("userPassword");
          Enumeration enum = atributo.getStringValues();
          senhaAtual = (String) enum.nextElement();
          if (senhaAtual.toUpperCase().startsWith("{CRYPT}")) {
            senhaOldCrypt = criptografaSenhaCrypt(senhaOld, senhaAtual);
         */

 /*
              System.out.println("senha Atual: " + senhaAtual) ;
              System.out.println("senha OldCrypt: " + senhaOldCrypt) ;
         */

 /*
            if (!senhaOldCrypt
              .toUpperCase()
              .equals(senhaAtual.toUpperCase())) {
              throw (
                new ImplManutLDAPException("Login ou senha nao confere"));
            }
            alteraSenha(ld, login, senhaOld);
          }
         */

 /*        
      if ( (senhaAtual.toUpperCase().startsWith("{SSHA}") && senhaOld.equals(senhaNew) ) {
        throw (new ImplManutLDAPException("senha nao alterada pois nova senha identica a atual"));
      }
         */
        try {
            ld.authenticate(3, cDNpessoa, senhaOld);
            alteraSenha(ld, login, senhaNew);
        } catch (LDAPException e) {
            throw (new ImplManutLDAPException("Login ou senha nao confere"));
        } finally {
            pool.close(ld);
        }

        /*
          try {
            ld.authenticate(3, "cn=admin,o=trt15", "admin-1");
          } catch (LDAPException e) {
            throw (
              new ImplManutLDAPException("login ou senha do super administrador nao confere para autenticacao no Directory Server"));
          }
         */

 /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
                try {
                  ld.disconnect();
                } catch (LDAPException e) {
                  throw (
                    new ImplManutLDAPException(
                      "Senha alterada, mas nao foi possivel desconectar da LDAP: "
                        + e.toString()));
                }
        }
         */
    }

    /**
     * ******************************************
     */
    private void alteraSenhaEstacao(String login)
            throws ImplManutLDAPException {

        String comando = "";
        String cArquivoLog = "/var/log/ldap/incluiEstacao.log";
        Vector vMensagensLog = new Vector();

        comando = "sleep 6";
        vMensagensLog = executaComando(comando);

        comando = "/usr/local/samba/bin/smbpasswd -a -m " + login;

        vMensagensLog = executaComando(comando);

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (IOException e) {
            // ignorar erro de gravacao do log
        }

    }

    /**
     * ******************************************
     */
    private void alteraSenha(LDAPConnection ld, String login, String senhaNew)
            throws ImplManutLDAPException {

        String cDNpessoa = "";
        LDAPEntry findEntry = null;
        LDAPAttribute atributoSenha = null;
        LDAPModification mod = null;
        String comando = "";
        String cArquivoLog = "";
        String senhaCriptografada = "";
        Vector vMensagensLog = new Vector();
        String idLDAP = "";
        String sambaNTPassword = "";
        String sambaLMPassword = "";
        Long sambaPwdLastSet;
        
        // Criptografa as senhas em NTLM e NT
        SambaPassword sambaPassword = new SambaPassword();

        try {

            findEntry = getEntryPessoa(ld, "uid", login);
            if (findEntry == null) {
                throw (new ImplManutLDAPException(
                        "Pessoa nao cadastrada no Directory Server com este login: "
                        + login));
            }

            cDNpessoa = findEntry.getDN();

            // Alterado por Tiago Leal em 19/05/2018
            //senhaCriptografada = criptografaSenha(senhaNew);
            senhaCriptografada = criptografaSenhaSHA(senhaNew);
            atributoSenha = new LDAPAttribute("userPassword", senhaCriptografada);
            mod = new LDAPModification(LDAPModification.REPLACE, atributoSenha);

            ld.modify(cDNpessoa, mod);

            cArquivoLog = "/var/log/samba/alterasenha.log";

            comando = "echo ---------- portal ----------";
            vMensagensLog = executaComando(comando);
            gravaMensagensLog(vMensagensLog, cArquivoLog);

            comando = "/bin/date";
            vMensagensLog = executaComando(comando);
            gravaMensagensLog(vMensagensLog, cArquivoLog);

            // Alterado por Tiago Leal em 19/05/2018            
            // A gravacao e' feita no ldap master e o sistema operacional provavelmente acessa alguma slave.
            // Ai a necessidade de um tempo para garantir que ja houve a replicacao entre a master e as slaves,
            // para que o usuario tambem exista nas slaves.
            //comando = "sleep 6";
            //vMensagensLog = executaComando(comando);

            //comando = "/usr/local/samba/bin/smbpasswd -a " + login + " " + senhaNew;
            //vMensagensLog = executaComando(comando);
            //gravaMensagensLog(vMensagensLog, cArquivoLog);
            
            //sambaNTPassword = this.getAtributo(ld, "uid", login, "ntPassword", "PESSOA");
            //sambaLMPassword = this.getAtributo(ld, "uid", login, "lmPassword", "PESSOA");
            sambaNTPassword = sambaPassword.calcNTLMHash(senhaNew);
            sambaLMPassword = sambaPassword.calcLMHash(senhaNew);
            sambaPwdLastSet = System.currentTimeMillis();
            
            idLDAP = this.getAtributo(ld, "uid", login, "idLDAP", "PESSOA");

            this.alteraAtributo(ld, idLDAP, "sambaNTPassword", sambaNTPassword);
            this.alteraAtributo(ld, idLDAP, "ntPassword", sambaNTPassword);
            this.alteraAtributo(ld, idLDAP, "sambaLMPassword", sambaLMPassword);            
            this.alteraAtributo(ld, idLDAP, "lmPassword", sambaLMPassword);
            this.alteraAtributo(ld, idLDAP, "sambaPwdLastSet", sambaPwdLastSet.toString());

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema com o Directory Server: " + e.toString()));
        } catch (IOException e) {
            // ignorar erro de gravacao do log
        }
        catch (NoSuchAlgorithmException e) {
            throw (new ImplManutLDAPException(
                    "Problema alterando senha: " + e.toString()));       	
        }
        catch (Exception e) { 
        	// Ignora demais erros 
        }
    }

    /**
     * *************************************************
     */
    private LDAPEntry getEntryPessoa(
            LDAPConnection ld,
            String nomeAtributo,
            String valorAtributo) {

        String filtro
                = "(&(objectClass=person)(" + nomeAtributo + "=" + valorAtributo + "))";

        return getEntry(ld, filtro);

    }

    /**
     * *************************************************
     */
    private LDAPEntry getEntryLotacao(
            LDAPConnection ld,
            String nomeAtributo,
            String valorAtributo) {

        /*    
          String filtro = "(&(objectClass=organizationalUnitTRT15)("+nomeAtributo+"="+valorAtributo+"))";
         */
        String filtro
                = "(|(&(objectClass=organizationalUnitTRT15)("
                + nomeAtributo
                + "="
                + valorAtributo
                + "))(&(objectClass=organizationTRT15)("
                + nomeAtributo
                + "="
                + valorAtributo
                + ")))";

        return getEntry(ld, filtro);

    }

    /**
     * *************************************************
     */
    private LDAPEntry getEntryGrupoMail(
            LDAPConnection ld,
            String nomeAtributo,
            String valorAtributo) {

        /*    
          String filtro = "(&(objectClass=organizationalUnitTRT15)("+nomeAtributo+"="+valorAtributo+"))";
         */
        String filtro
                = "(&(objectClass=grupoMailTRT15)(" + nomeAtributo + "=" + valorAtributo + "))";

        return getEntry(ld, filtro);

    }

    /**
     * ***************************************************
     */
    private LDAPEntry getEntry(LDAPConnection ld, String filtro) {

        LDAPEntry findEntry = null;
        LDAPSearchConstraints cons = null;
        String basePesquisa = "";
        int escopo = 0;
        LDAPSearchResults res;
        String[] atributos = {};

        try {

            cons = ld.getSearchConstraints();
            cons.setBatchSize(1);

            basePesquisa = "o=trt15";
            escopo = LDAPConnection.SCOPE_SUB;

            res = ld.search(basePesquisa, escopo, filtro, atributos, false, cons);

            if (res.hasMoreElements()) {
                findEntry = res.next();
            } else {
                return null;
            }

        } catch (LDAPException e) {
            //          System.out.println( "Error: " + e.toString() );
            return null;
        }

        return findEntry;

    }

    /**
     * ************************************
     */
    private void moveMails(String loginPessoa, String senhaAdministrador) throws ImplManutLDAPException {

        String cMailOrigem = "";
        String cMailDestino = "";

        cMailOrigem = "/var/spool/mail/" + loginPessoa;
        cMailDestino = "/home/cemiterioMails/mailsEntrada/" + loginPessoa;

        move(cMailOrigem, cMailDestino);

        cMailOrigem = "/home/" + loginPessoa;
        cMailDestino = "/home/cemiterioMails/pastas/" + loginPessoa;

        move(cMailOrigem, cMailDestino);
    }

    /**
     * ************************************
     */
    private void renomeiaMails(String loginPessoa, String novoLogin, String senhaAdministrador) throws ImplManutLDAPException {

        String cMailOrigem = "";
        String cMailDestino = "";

        cMailOrigem = "/var/spool/mail/" + loginPessoa;
        cMailDestino = "/var/spool/mail/" + novoLogin;

        move(cMailOrigem, cMailDestino);

        cMailOrigem = "/home/" + loginPessoa;
        cMailDestino = "/home/" + novoLogin;

        move(cMailOrigem, cMailDestino);
    }

    /**
     * ************************************
     */
    private void move(String cMailOrigem, String cMailDestino) {

        File fArquivoMailOrigem = new File(cMailOrigem);
        File fArquivoMailDestino = new File(cMailDestino);
        String comando = "";

        /*
          File fDiretorioMailOrigem = new File(cDiretorioMailOrigem);      
          File fDiretorioMailDestino = new File(cDiretorioMailDestino);
         */
        File fAuxiliar1 = null;
        int contador = 0;
        String cAux1 = "";
        String cAux2 = "";

        if (fArquivoMailOrigem.isFile() || fArquivoMailOrigem.isDirectory()) {

            fAuxiliar1 = fArquivoMailDestino;
            while (fAuxiliar1.isFile() || fAuxiliar1.isDirectory()) {

                if (contador == 0) {
                    cAux1 = cMailDestino;
                } else {
                    cAux1 = cMailDestino + "." + contador;
                }

                fAuxiliar1 = new File(cAux1);

                contador++;
            }

            contador--;

            while (contador > 0) {

                if (contador == 1) {
                    cAux1 = cMailDestino;
                } else {
                    cAux1 = cMailDestino + "." + (contador - 1);
                }

                cAux2 = cMailDestino + "." + contador;

                comando = "mv " + cAux1 + " " + cAux2;

                try {
                    Runtime runtime = Runtime.getRuntime();
                    runtime.exec(comando);
                } catch (Exception e) {
                    System.out.println("Error: " + e.toString());
                }

                contador--;

            }

            comando = "mv " + cMailOrigem + " " + cMailDestino;

            try {
                Runtime runtime = Runtime.getRuntime();
                runtime.exec(comando);
            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
            }
        }
    }

    /**
     * ******************************************************
     */
    private Vector executaComando(String comando) {

        String mensagem = "";
        InputStream istr, istr2 = null;
        BufferedReader br, br2 = null;
        Process process = null;
        Runtime run = null;
        Vector vMensagensLog = new Vector();

        try {

            run = Runtime.getRuntime();

            process = run.exec(comando);

            istr = process.getInputStream();
            br = new BufferedReader(new InputStreamReader(istr));

            istr2 = process.getErrorStream();
            br2 = new BufferedReader(new InputStreamReader(istr2));

            // read output lines from command
            while ((mensagem = br.readLine()) != null) {
                vMensagensLog.addElement(mensagem);
            }

            while ((mensagem = br2.readLine()) != null) {
                vMensagensLog.addElement(mensagem);
            }

            // close stream
            br.close();

        } catch (Exception e) {
            vMensagensLog.addElement("Error: " + e.toString());
            return vMensagensLog;
        }

        return vMensagensLog;

    }

    /**
     * ******************************************************
     */
    private void gravaMensagensLog(Vector vMensagensLog, String cArquivoLog)
            throws IOException {

        String mensagem = "";
        int qtdElementos = 0;

        File outputFile = new File(cArquivoLog);
        FileWriter out = new FileWriter(outputFile, true);
        BufferedWriter bufSaida = new BufferedWriter(out);

        qtdElementos = vMensagensLog.size();
        String aMensagensLog[] = new String[qtdElementos];
        vMensagensLog.copyInto(aMensagensLog);
        for (int i = 0; i < qtdElementos; i++) {
            mensagem = aMensagensLog[i];
            bufSaida.write(mensagem, 0, mensagem.length());
            bufSaida.newLine();
        }
        bufSaida.flush();
        bufSaida.close();
        out.close();
    }

    /**
     * **************************************************
     */
    private String criptografaSenhaCrypt(String senha) {

        String caracteres
                = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int qtdcaracteres = caracteres.length();
        Random random = new Random();
        int posicao1 = random.nextInt(qtdcaracteres);
        int posicao2 = random.nextInt(qtdcaracteres);

        String salt
                = caracteres.substring(posicao1, posicao1 + 1)
                + caracteres.substring(posicao2, posicao2 + 1);

        String senhaCriptografada = "{CRYPT}" + jcrypt.crypt(salt, senha);

        return senhaCriptografada;
    }

    /**
     * **************************************************
     */    
	private String criptografaSenhaSHA(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest digest = MessageDigest.getInstance("SHA");
		digest.update(password.getBytes("UTF8"));              
		String md5Password = new String(Base64.getEncoder().encode(digest.digest()));
		return "{SHA}" + md5Password;
	}    
    
    /**
     * **************************************************
     */
	/*
	private String criptografaSenhaNT(String password) throws UnsupportedEncodingException {        
        MD4 md4 = new MD4();
        byte[] bpass = password.getBytes("UnicodeLittleUnmarked");
        md4.engineUpdate(bpass, 0, bpass.length);        
        byte[] hashbytes = md4.engineDigest();
        String ntHash = Hexdump.toHexString(hashbytes, 0, hashbytes.length * 2);        
        return ntHash;
    }
    */	
	
    /**
     * **************************************************
     */
    private String criptografaSenhaCrypt(
            String senhaNaoCrypto,
            String senhaCrypto) {

        String salt = senhaCrypto.substring(7, 9);

        String senhaCriptografada = "{CRYPT}" + jcrypt.crypt(salt, senhaNaoCrypto);

        return senhaCriptografada;
    }

    /**
     * **************************************************
     */    
    
/*    SUBSTITUÃ�DO PELO MÃ‰TODO criptografaSenhaSHA por Tiago Leal em 17/05/2018
    private String criptografaSenha(String senha) {

        String senhaCriptografada = "";
        Vector vSenha = new Vector();
        int qtdElementos = 0;
        String comando = "";
        //alterar a linha seguinte
        comando = "/usr/sbin/slappasswd -v -h {SHA} -s " + senha;
        vSenha = executaComando(comando);

        qtdElementos = vSenha.size();
        String aSenha[] = new String[qtdElementos];
        vSenha.copyInto(aSenha);
        for (int i = 0; i < qtdElementos; i++) {
            senhaCriptografada = aSenha[i];
        }

        return senhaCriptografada;

    }
*/    

    /**
     * **************************************************
     */
    private void logar(
            LDAPConnection ld,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException {

        String cDNadministrador = "";
        LDAPEntry findEntry = null;

        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        try {
            ld.authenticate(3, "", "");
        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Problema de autenticacao no Directory Server como anonimo: "
                    + e.toString()));
        }

        findEntry = getEntryPessoa(ld, "uid", loginAdministrador);
        if (findEntry == null) {
            throw (new ImplManutLDAPException("login ou senha do administrador nao confere para autenticacao no Directory Server"));
        }

        cDNadministrador = findEntry.getDN();

        try {
            ld.authenticate(3, cDNadministrador, senhaAdministrador);
        } catch (LDAPException e) {
            throw (new ImplManutLDAPException("login ou senha do administrador nao confere para autenticacao no Directory Server"));
        }

        /*
        try {
          ld.authenticate(3, "cn=admin,o=trt15", "admin-1");
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException("login ou senha do super administrador nao confere para autenticacao no Directory Server"));
        }
         */
    }

    /**
     * ************************
     */
    public int getIdLotacaoTRT15(
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: getIdLotacaoTRT15";
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }


        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
          poolMaster.close(ld);
                  try {
                    ld.disconnect();
                  } catch (LDAPException e) {
                    new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
                  }
        }
         */
        return getIdLotacaoTRT15();

    }

    /**
     * **************************************************************
     */
    private int getIdLotacaoTRT15() throws ImplManutLDAPException {

        return 2038;

    }

    /**
     * ****************************
     */
    public LotacaoLDAP getLotacao(
            int idLDAP,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: getLotacao";
        mensagemLOG += " idLDAP: " + idLDAP;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        /*
            if (loginAdministrador.equals("")
              || senhaAdministrador.equals("")) {
              throw (
                new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
            }
         */
        LotacaoLDAP lotacao = null;

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }


        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        lotacao = getLotacao(ld, idLDAP);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
          poolMaster.close(ld);
                  try {
                    ld.disconnect();
                  } catch (LDAPException e) {
                    new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
                  }
        }
         */
        return lotacao;

    }

    /**
     * **********************************************************************
     */
    private LotacaoLDAP getLotacao(LDAPConnection ld, int idLDAP)
            throws ImplManutLDAPException {

        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        Enumeration enume = null;

        findEntry = getEntryLotacao(ld, "idLDAP", String.valueOf(idLDAP));
        if (findEntry == null) {
            throw (new ImplManutLDAPException(
                    "Lotacao nao cadastrada no Directory Server com este idLDAP : "
                    + idLDAP));
        }

        int idLotacaoFenix = 0;
        String descLotacao = "";
        String sigla = "";
        int gidNumber = 0;
        String dnLotacao = "";
        String identOrigem = "";
        String description = "";

        atributo = findEntry.getAttribute("idLotacao");
        if (atributo != null) {
            enume = atributo.getStringValues();
            idLotacaoFenix = (new Integer((String) enume.nextElement())).intValue();
        }

        atributo = findEntry.getAttribute("description");
        if (atributo != null) {
            enume = atributo.getStringValues();
            descLotacao = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("cn");
        if (atributo != null) {
            enume = atributo.getStringValues();
            sigla = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("gidNumber");
        if (atributo != null) {
            enume = atributo.getStringValues();
            gidNumber = (new Integer((String) enume.nextElement())).intValue();
        }

        atributo = findEntry.getAttribute("identOrigem");
        if (atributo != null) {
            enume = atributo.getStringValues();
            identOrigem = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("description");
        if (atributo != null) {
            enume = atributo.getStringValues();
            description = (String) enume.nextElement();
        }

        dnLotacao = findEntry.getDN();

        LotacaoLDAP lotacao = new LotacaoLDAP(idLDAP, idLotacaoFenix, descLotacao, sigla, gidNumber, dnLotacao, identOrigem, description);

        return lotacao;

    }

    /**
     * ****************************
     */
    public Pessoa getPessoa(
            String uid,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: getPessoa (UID)";
        mensagemLOG += " uid: " + uid;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if ((uid == null) || (uid.equals(""))) {
            throw (new ImplManutLDAPException("uid (login) deve ser informado"));
        }

        Pessoa pessoa = null;

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }


        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        int idLDAP
                = Integer.parseInt(this.getAtributo(ld, "uid", uid, "idLDAP", "PESSOA"));

        pessoa = getPessoa(ld, idLDAP);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
          poolMaster.close(ld);
                  try {
                    ld.disconnect();
                  } catch (LDAPException e) {
                    new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
                  }
        }
         */
        return pessoa;

    }

    /**
     * ****************************
     */
    public Pessoa getPessoa(
            int idLDAP,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: getPessoa (idLDAP)";
        mensagemLOG += " idLDAP: " + idLDAP;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        Pessoa pessoa = null;

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        pessoa = getPessoa(ld, idLDAP);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
          poolMaster.close(ld);
          /*        
                  try {
                    ld.disconnect();
                  } catch (LDAPException e) {
                    new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
                  }
        }
         */
        return pessoa;

    }

    /**
     * *********************************************************************
     */
    private Pessoa getPessoa(LDAPConnection ld, int idLDAP)
            throws ImplManutLDAPException {

        Pessoa pessoa = null;

        int matricula = 0;
        String nomeCompleto = "";
        String login = "";
        int idLotacaoFenix = 0;
        String categoria = "";
        String mail = "";
        String senha = "";
        String dnPessoa = "";
        int gIdNumber = 0;
        String ou = "";
        int cargo = 0;

        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        Enumeration enume = null;

        findEntry = getEntryPessoa(ld, "idLDAP", String.valueOf(idLDAP));
        if (findEntry == null) {
            throw (new ImplManutLDAPException(
                    "Pessoa nao cadastrada no Directory Server com este idLDAP : "
                    + idLDAP));
        }

        atributo = findEntry.getAttribute("employeeNumber");
        if (atributo != null) {
            enume = atributo.getStringValues();
            matricula = new Integer((String) enume.nextElement()).intValue();
        }

        atributo = findEntry.getAttribute("cn");
        if (atributo != null) {
            enume = atributo.getStringValues();
            nomeCompleto = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("uid");
        if (atributo != null) {
            enume = atributo.getStringValues();
            login = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("idLotacao");
        if (atributo != null) {
            enume = atributo.getStringValues();
            idLotacaoFenix = new Integer((String) enume.nextElement()).intValue();
        }

        atributo = findEntry.getAttribute("categoria");
        if (atributo != null) {
            enume = atributo.getStringValues();
            categoria = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("mail");
        if (atributo != null) {
            enume = atributo.getStringValues();
            mail = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("userPassword");
        if (atributo != null) {
            enume = atributo.getStringValues();
            senha = (String) enume.nextElement();
        }

        atributo = findEntry.getAttribute("gidNumber");
        if (atributo != null) {
            enume = atributo.getStringValues();
            gIdNumber = new Integer((String) enume.nextElement()).intValue();
        }

        atributo = findEntry.getAttribute("ou");
        if (atributo != null) {
            enume = atributo.getStringValues();
            ou = (String) enume.nextElement();;
        }

        atributo = findEntry.getAttribute("cargo");
        if (atributo != null) {
            enume = atributo.getStringValues();
            cargo = new Integer((String) enume.nextElement()).intValue();
        }

        dnPessoa = findEntry.getDN();

        pessoa
                = new Pessoa(
                        idLDAP,
                        matricula,
                        nomeCompleto,
                        login,
                        idLotacaoFenix,
                        categoria,
                        mail,
                        dnPessoa,
                        senha,
                        gIdNumber,
                        ou,
                        cargo);

        return pessoa;

    }

    /**
     * ********************************************
     */
    private Vector getIds(LDAPConnection ld, int idLDAP, String filtro) {

        LDAPEntry findEntry = null;
        LDAPSearchConstraints cons = null;
        int escopo = 0;
        LDAPSearchResults res;
        String[] atributos = {};
        Vector vIds = new Vector();
        LDAPAttribute atributo = null;
        Integer idLDAPfilho = null;
        String basePesquisa = "";

        try {

            cons = ld.getSearchConstraints();
            cons.setBatchSize(1);
            escopo = LDAPConnection.SCOPE_ONE;
            findEntry = getEntryLotacao(ld, "idLDAP", String.valueOf(idLDAP));
            basePesquisa = findEntry.getDN();
            res = ld.search(basePesquisa, escopo, filtro, atributos, false, cons);

            while (res.hasMoreElements()) {
                findEntry = res.next();
                atributo = findEntry.getAttribute("idLDAP");
                Enumeration enume = atributo.getStringValues();
                idLDAPfilho = new Integer((String) enume.nextElement());
                vIds.addElement(idLDAPfilho);
            }

        } catch (LDAPException e) {
            System.out.println(
                    "Erro: "
                    + e.getMessage()
                    + " "
                    + e.getLDAPErrorMessage()
                    + " "
                    + e.errorCodeToString()
                    + " "
                    + e.getLDAPResultCode());
            return null;
        }

        return vIds;

    }

    /**
     * *****************************************
     */
    public Vector getIdsPessoas(
            int idLotacao,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        LDAPConnection ld = null;

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: getIdsPessoas";
        mensagemLOG += " idLotacao: " + idLotacao;
        vMensagensLog.add(mensagemLOG);

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        Vector idsPessoas = null;

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        idsPessoas = getIdsPessoas(ld, idLotacao);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
          poolMaster.close(ld);
                  try {
                    ld.disconnect();
                  } catch (LDAPException e) {
                    new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
                  }
        }
         */
        return idsPessoas;

    }

    /**
     * **************************************************************************
     */
    private Vector getIdsPessoas(LDAPConnection ld, int idLotacao) {

        Vector vIdsPessoas = new Vector();
        String filtro = "";

        filtro = "(&(objectClass=personTRT15))";

        vIdsPessoas = getIds(ld, idLotacao, filtro);

        return vIdsPessoas;

    }

    /**
     * *****************************************
     */
    public Vector getIdsLotacoes(
            int idLotacao,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: getIdsLotacoes";
        mensagemLOG += " idLotacao: " + idLotacao;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        Vector idsLotacoes = null;

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        idsLotacoes = getIdsLotacoes(ld, idLotacao);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
          poolMaster.close(ld);
                    try {
                      ld.disconnect();
                    } catch (LDAPException e) {
                      new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
                    }
        }
         */
        return idsLotacoes;

    }

    /**
     * **************************************************************************
     */
    private Vector getIdsLotacoes(LDAPConnection ld, int idLotacao) {

        Vector vIdsLotacoes = new Vector();
        String filtro = "";

        filtro = "(&(objectClass=organizationalUnitTRT15))";

        vIdsLotacoes = getIds(ld, idLotacao, filtro);

        return vIdsLotacoes;

    }

    /**
     * *********************************************************************************
     */
    public Organograma getOrganograma(
            int idLotacaoRaiz,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: getOrganograma";
        mensagemLOG += " idLotacaoRaiz: " + idLotacaoRaiz;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (loginAdministrador.equals("") || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        Organograma org = null;

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);

        org = getOrganograma(ld, idLotacaoRaiz);

        pool.close(ld);

        /*
        } catch (LDAPException e) {
          throw (
            new ImplManutLDAPException(
              "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
          poolMaster.close(ld);
                    try {
                      ld.disconnect();
                    } catch (LDAPException e) {
                      new ImplManutLDAPException("Valor obtido, mas nao foi possivel desconectar da LDAP : " + e.toString());
                    }
        }
         */
        return org;

    }

    /**
     * *******************************************************************************************
     */
    private Organograma getOrganograma(LDAPConnection ld, int idLotacaoRaiz)
            throws ImplManutLDAPException {

        Organograma organograma = new Organograma();

        if (idLotacaoRaiz == 0) {
            idLotacaoRaiz = getIdLotacaoTRT15();
        }

        preencheRecursivo(ld, idLotacaoRaiz, organograma);

        return organograma;

    }

    /**
     * ***********************************************************************************************
     */
    private void preencheRecursivo(
            LDAPConnection ld,
            int idLotacao,
            Organograma organograma)
            throws ImplManutLDAPException {

        LotacaoLDAP lotacao = null;
        Pessoa pessoa = null;
        int idPessoa = 0;
        Vector vIdsLotacoes = new Vector();
        Vector vIdsPessoas = new Vector();
        int qtdElementos = 0;

        lotacao = getLotacao(ld, idLotacao);

        if (lotacao != null) {
            organograma.addLotacao(lotacao);
        }

        vIdsPessoas = getIdsPessoas(ld, idLotacao);

        if (vIdsPessoas != null) {

            qtdElementos = vIdsPessoas.size();

            Object aIdPessoas[] = new Object[qtdElementos];
            vIdsPessoas.copyInto(aIdPessoas);
            for (int i = 0; i < qtdElementos; i++) {
                idPessoa = ((Integer) aIdPessoas[i]).intValue();
                pessoa = getPessoa(ld, idPessoa);
                organograma.addPessoa(pessoa);
            }

        }

        vIdsLotacoes = getIdsLotacoes(ld, idLotacao);

        if (vIdsLotacoes != null) {

            qtdElementos = vIdsLotacoes.size();

            Object aIdLotacoes[] = new Object[qtdElementos];
            vIdsLotacoes.copyInto(aIdLotacoes);
            for (int i = 0; i < qtdElementos; i++) {
                idLotacao = ((Integer) aIdLotacoes[i]).intValue();
                preencheRecursivo(ld, idLotacao, organograma);
            }

        }

    }

    /**
     * *******************************************************************************************
     */
    private void load() throws RemoteException, ImplManutLDAPException {

        try {
            properties = PropertiesLoader.load(this.getClass());
        } catch (Exception e) {
            throw new ImplManutLDAPException(
                    "Arquivo ImplManutencaoLDAP.properites nao encontrado: " + e.toString());
        }

        MY_HOST_MASTER = properties.getProperty(PROP_HOST_LDAP_MASTER);
        MY_PORT_MASTER
                = Integer.valueOf(properties.getProperty(PROP_PORT_LDAP_MASTER)).intValue();
        POOL_MIN_MASTER
                = Integer.valueOf(properties.getProperty(PROP_POOL_MIN_MASTER)).intValue();
        POOL_MAX_MASTER
                = Integer.valueOf(properties.getProperty(PROP_POOL_MAX_MASTER)).intValue();

        MY_HOST_SLAVE = properties.getProperty(PROP_HOST_LDAP_SLAVE);
        MY_PORT_SLAVE
                = Integer.valueOf(properties.getProperty(PROP_PORT_LDAP_SLAVE)).intValue();
        POOL_MIN_SLAVE
                = Integer.valueOf(properties.getProperty(PROP_POOL_MIN_SLAVE)).intValue();
        POOL_MAX_SLAVE
                = Integer.valueOf(properties.getProperty(PROP_POOL_MAX_SLAVE)).intValue();

        System.out.println(" ");
        System.out.println("MY_HOST_MASTER: " + MY_HOST_MASTER);
        System.out.println("MY_PORT_MASTER: " + MY_PORT_MASTER);
        System.out.println("POOL_MIN_MASTER: " + POOL_MIN_MASTER);
        System.out.println("POOL_MAX_MASTER: " + POOL_MAX_MASTER);

        System.out.println(" ");
        System.out.println("MY_HOST_SLAVE: " + MY_HOST_SLAVE);
        System.out.println("MY_PORT_SLAVE: " + MY_PORT_SLAVE);
        System.out.println("POOL_MIN_SLAVE: " + POOL_MIN_SLAVE);
        System.out.println("POOL_MAX_SLAVE: " + POOL_MAX_SLAVE);

    }

    /**
     * *******************************************************************************************
     */
    private String tiraAcento(String texto) {

        int posicao = 0;
        int posicaoV1 = 0;

        String V1 = "ÃƒÆ’Ã¯Â¿Â½?Ãƒâ€šÃƒâ‚¬Ãƒâ€¡Ãƒâ€°ÃƒÅ Ãƒï¿½Ãƒâ€œÃƒâ€�Ãƒâ€¢ÃƒÅ¡ÃƒÂ§ÃƒÂ¡ÃƒÂ¢ÃƒÂ ÃƒÂ£ÃƒÂ©ÃƒÂªÃƒÂ­ÃƒÂ³ÃƒÂ´ÃƒÂµÃƒÂºÃ‚ÂºÃ‚ÂªÃƒÅ“ÃƒÂ¼";
        String V2 = "AAAAACEEIOOOUcaaaaeeiooouoaUu";

        String textoTransformado = "";
        int tamanhoTexto = 0;

        if (texto != null) {
            tamanhoTexto = texto.length();
            if (tamanhoTexto > 0) {
                for (posicao = 0; posicao < tamanhoTexto; posicao++) {
                    posicaoV1 = V1.indexOf(texto.substring(posicao, posicao + 1));

                    if (posicaoV1 > -1) {
                        textoTransformado
                                = textoTransformado + V2.substring(posicaoV1, posicaoV1 + 1);
                    } else {
                        textoTransformado
                                = textoTransformado + texto.substring(posicao, posicao + 1);
                    }

                }
            }
        }

        return textoTransformado;

    }

    /**
     * *******************************************************************************************
     */
    public String transformaNome(String nomeCompleto) {

        String aux = "";
        String nomeTransformado = "";
        String nome = "";

        aux = tiraAcento(nomeCompleto.trim());

        StringTokenizer st = new StringTokenizer(aux, " ");
        while (st.hasMoreTokens()) {
            nome = st.nextToken();
            nome = nome.toLowerCase();
            if (nome.equals("de")
                    || nome.equals("ao")
                    || nome.equals("a")
                    || nome.equals("as")
                    || nome.equals("ou")
                    || nome.equals("outros")
                    || nome.equals("para")
                    || nome.equals("da")
                    || nome.equals("das")
                    || nome.equals("do")
                    || nome.equals("dos")
                    || nome.equals("em")
                    || nome.equals("sem")
                    || nome.equals("com")
                    || nome.equals("e")) {
                nomeTransformado = nomeTransformado + nome + " ";
            } else {
                nomeTransformado
                        = nomeTransformado
                        + nome.substring(0, 1).toUpperCase()
                        + nome.substring(1)
                        + " ";
            }

        }

        return nomeTransformado;

    }

    /**
     * ********************************
     */
    private String getOU(String dn) {

        String ou = "";
        int posicaoInicial = -1;
        int posicaoFinal = -1;

        posicaoInicial = dn.indexOf("ou=") + 3;
        posicaoFinal = dn.indexOf(",");
        if (posicaoFinal < 0) {
            ou = dn.substring(posicaoInicial).trim();
        } else {
            ou = dn.substring(posicaoInicial, posicaoFinal).trim();
        }

        return ou;

    }

    /**
     * ******************************************
     */
    public void incluiLotacao(
            String idLDAPMae,
            String idLDAP,
            String idLotacao,
            String descricao,
            String sigla,
            String gidNumber,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*    
    mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
    mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: incluiLotacao";
        mensagemLOG += " idLDAPMae: " + idLDAPMae;
        mensagemLOG += " idLDAP: " + idLDAP;
        mensagemLOG += " sigla: " + sigla;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        String cn = "";
        String dnLotacao = "";
        String dnLotacaoMae = "";
        String ouMae = "";
        String sambaSID = "";

        if (idLDAPMae.equals("")
                || idLDAP.equals("")
                || idLotacao.equals("")
                || descricao.equals("")
                || sigla.equals("")
                || gidNumber.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        /*
        try {
          ld.connect(MY_HOST, MY_PORT);
         */
        logar(ld, loginAdministrador, senhaAdministrador);
        dnLotacaoMae = getAtributo(ld, "idLDAP", idLDAPMae, "dn", "lotacao");

        if ((dnLotacaoMae == null) || (dnLotacaoMae.equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Nao existe lotacao mae com este idLDAP: " + idLDAPMae));
        }

        String dnPessoa = getAtributo(ld, "idLDAP", idLDAP, "dn", "pessoa");

        if ((dnPessoa != null) && (!dnPessoa.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "ja existe uma pessoa com este idLDAP : " + idLDAP));
        }

        dnLotacao = getAtributo(ld, "idLDAP", idLDAP, "dn", "lotacao");
        if ((dnLotacao != null) && (!dnLotacao.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "ja existe uma lotacao com este idLDAP : " + idLDAP));
        }

        dnLotacao = getAtributo(ld, "idLotacao", idLotacao, "dn", "lotacao");
        if ((dnLotacao != null) && (!dnLotacao.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Ja existe uma lotacao com este idLotacao:  " + idLotacao));
        }

        dnLotacao = getAtributo(ld, "gidNumber", gidNumber, "dn", "lotacao");
        if ((dnLotacao != null) && (!dnLotacao.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "Ja existe uma lotacao com este gidNumber:  " + gidNumber));
        }

        dnLotacao = "ou=" + sigla + "," + dnLotacaoMae;
        ouMae = getOU(dnLotacaoMae);
        cn = sigla.trim() + "." + ouMae;
        sambaSID = "S-1-5-21-2123005422-2009428996-684742794-" + idLDAP;

        LDAPAttribute attr1 = new LDAPAttribute("idLDAP", idLDAP);
        LDAPAttribute attr2 = new LDAPAttribute("idLotacao", idLotacao);
        LDAPAttribute attr3 = new LDAPAttribute("description", descricao);
        LDAPAttribute attr4 = new LDAPAttribute("cn", cn);
        LDAPAttribute attr5 = new LDAPAttribute("gidNumber", gidNumber);
        LDAPAttribute attr6 = new LDAPAttribute("ou", sigla);
        LDAPAttribute attr7 = new LDAPAttribute("sambaSID", sambaSID);
        LDAPAttribute attr8 = new LDAPAttribute("sambaGroupType", "2");
        LDAPAttribute attr9 = new LDAPAttribute("uniqueMember", dnLotacao);

        String[] atributos = {"top", "organizationalUnitTRT15", "sambaGroupMapping", "groupOfUniqueNames"};

        LDAPAttribute attr10 = new LDAPAttribute("objectclass", atributos);

        LDAPAttributeSet myAttrs = new LDAPAttributeSet();
        myAttrs.add(attr1);
        myAttrs.add(attr2);
        myAttrs.add(attr3);
        myAttrs.add(attr4);
        myAttrs.add(attr5);
        myAttrs.add(attr6);
        myAttrs.add(attr7);
        myAttrs.add(attr8);
        myAttrs.add(attr9);
        myAttrs.add(attr10);

        LDAPEntry myEntry = new LDAPEntry(dnLotacao, myAttrs);

        try {

            ld.add(myEntry);

        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
            pool.close(ld);
        }

        /*
  } catch (LDAPException e) {
    throw (
      new ImplManutLDAPException(
        "Erro de conexao com a LDAP: " + e.toString()));
  } finally {
    poolMaster.close(ld);
            try {
              ld.disconnect();
            } catch (LDAPException e) {
              new ImplManutLDAPException("Lotacao incluida, mas nao foi possivel desconectar da LDAP : " + e.toString());
            }
  }
         */
    }

    /**
     * ******************************************
     */
    public void incluiGrupoMail(
            String grupo,
            String idLDAP,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));

        mensagemLOG += " metodo: incluiGrupoMail";
        mensagemLOG += " grupo: " + grupo;
        mensagemLOG += " idLDAP: " + idLDAP;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;
        String dnGrupoMail = "";
        String mail = "";
        String mailRoutingAddress = "";

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (idLDAP.equals("")
                || grupo.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        dnGrupoMail = getAtributo(ld, "idLDAP", idLDAP, "dn", "grupoMail");

        if ((dnGrupoMail != null) && (!dnGrupoMail.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "ja existe um grupo de mail com esse idLDAP: " + idLDAP));
        }

        dnGrupoMail = getAtributo(ld, "cn", grupo, "dn", "grupoMail");

        if ((dnGrupoMail != null) && (!dnGrupoMail.trim().equals(""))) {
            pool.close(ld);
            throw (new ImplManutLDAPException(
                    "ja existe um grupo de mail com esse nome: " + grupo));
        }

        dnGrupoMail = "cn= " + grupo + ", ou=gruposMail, ou=grupos, o=trt15";
        mail = grupo + "@trt15.jus.br";
        mailRoutingAddress = grupo + "@mail.trt15.jus.br";

        String[] mailLocalAddress = {grupo + "@trt15.gov.br", grupo + "@trt15.jus.br",
            grupo + "@trtcamp.jus.br", grupo + "@jtcamp.jus.br"};
        String[] objectClass = {"grupoMailTRT15", "inetLocalMailRecipient"};

        LDAPAttribute attr1 = new LDAPAttribute("mail", mail);
        LDAPAttribute attr2 = new LDAPAttribute("mailRoutingAddress", mailRoutingAddress);
        LDAPAttribute attr3 = new LDAPAttribute("objectclass", objectClass);
        LDAPAttribute attr4 = new LDAPAttribute("idLDAP", idLDAP);
        LDAPAttribute attr5 = new LDAPAttribute("mailLocalAddress", mailLocalAddress);
        LDAPAttribute attr6 = new LDAPAttribute("cn", grupo);

        LDAPAttributeSet myAttrs = new LDAPAttributeSet();
        myAttrs.add(attr1);
        myAttrs.add(attr2);
        myAttrs.add(attr3);
        myAttrs.add(attr4);
        myAttrs.add(attr5);
        myAttrs.add(attr6);

        LDAPEntry myEntry = new LDAPEntry(dnGrupoMail, myAttrs);

        try {
            ld.add(myEntry);
        } catch (LDAPException e) {
            throw (new ImplManutLDAPException(
                    "Erro de conexao com a LDAP: " + e.toString()));
        } finally {
            pool.close(ld);
        }

    }

    /**
     * ******************************************
     */
    public void removeGrupoMail(
            String nomeAtributo,
            String valorAtributo,
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: removeGrupoMail";
        mensagemLOG += " nomeAtributo: " + nomeAtributo;
        mensagemLOG += " valorAtributo: " + valorAtributo;
        vMensagensLog.add(mensagemLOG);

        LDAPConnection ld = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        if (nomeAtributo.equals("")
                || valorAtributo.equals("")
                || loginAdministrador.equals("")
                || senhaAdministrador.equals("")) {
            throw (new ImplManutLDAPException("Nenhum valor de entrada pode ser nulo"));
        }

        //LDAPConnection ld = new LDAPConnection();
        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        String dnGrupoMail = "";
        LDAPEntry findEntry = null;

        findEntry = getEntryGrupoMail(ld, nomeAtributo, valorAtributo);
        if (findEntry == null) {
            pool.close(ld);
            throw (new ImplManutLDAPException("Grupo de Mail nao cadastrado no Directory Server, " + nomeAtributo + ": " + valorAtributo));
        }

        dnGrupoMail = findEntry.getDN();
        try {
            // Remove a pessoa
            ld.delete(dnGrupoMail);

        } catch (LDAPException ex) {
            throw (new ImplManutLDAPException(
                    "Erro de conexao com a LDAP: " + ex.toString()));
        } finally {
            pool.close(ld);
        }

    }

    /**
     * ****************************
     */
    public Hashtable getTodasLotacoes(
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: getTodasLotacoes";
        vMensagensLog.add(mensagemLOG);
        Hashtable htTodasLotacoes = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        LDAPConnection ld = null;

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        htTodasLotacoes = getTodasLotacoes(ld);

        pool.close(ld);

        return htTodasLotacoes;

    }

    /**
     * **********************************************************************
     */
    private Hashtable getTodasLotacoes(LDAPConnection ld)
            throws ImplManutLDAPException {

        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        Enumeration enume = null;

        LDAPSearchConstraints cons = null;
        String basePesquisa = "";
        int escopo = 0;
        LDAPSearchResults res;
        String[] atributos = {};

        int idLDAP = 0;
        int idLotacaoFenix = 0;
        String descLotacao = "";
        String sigla = "";
        int gidNumber = 0;
        String dnLotacao = "";
        String identOrigem = "";

        Hashtable htTodasLotacoes = new Hashtable();

        String filtro = "(|(objectclass=organizationTRT15)(objectclass=organizationalUnitTRT15))";

        try {

            cons = ld.getSearchConstraints();
            cons.setBatchSize(1);

            basePesquisa = "o=trt15";
            escopo = LDAPConnection.SCOPE_SUB;

            res = ld.search(basePesquisa, escopo, filtro, atributos, false, cons);

            while (res.hasMoreElements()) {

                idLDAP = 0;
                idLotacaoFenix = 0;
                descLotacao = "";
                sigla = "";
                gidNumber = 0;
                dnLotacao = "";
                identOrigem = "";

                findEntry = res.next();
                if (findEntry != null) {

                    atributo = findEntry.getAttribute("idLDAP");
                    if (atributo != null) {
                        enume = atributo.getStringValues();
                        idLDAP = (new Integer((String) enume.nextElement())).intValue();
                    }

                    atributo = findEntry.getAttribute("idLotacao");
                    if (atributo != null) {
                        enume = atributo.getStringValues();
                        idLotacaoFenix = (new Integer((String) enume.nextElement())).intValue();
                    }

                    atributo = findEntry.getAttribute("description");
                    if (atributo != null) {
                        enume = atributo.getStringValues();
                        descLotacao = (String) enume.nextElement();
                    }

                    atributo = findEntry.getAttribute("cn");
                    if (atributo != null) {
                        enume = atributo.getStringValues();
                        sigla = (String) enume.nextElement();
                    }

                    atributo = findEntry.getAttribute("gidNumber");
                    if (atributo != null) {
                        enume = atributo.getStringValues();
                        gidNumber = (new Integer((String) enume.nextElement())).intValue();
                    }

                    atributo = findEntry.getAttribute("identOrigem");
                    if (atributo != null) {
                        enume = atributo.getStringValues();
                        identOrigem = (String) enume.nextElement();
                    }

                    dnLotacao = findEntry.getDN();

                    LotacaoLDAP lotacao = new LotacaoLDAP(idLDAP, idLotacaoFenix, descLotacao, sigla, gidNumber, dnLotacao, identOrigem);

                    htTodasLotacoes.put(new Integer(idLDAP), lotacao);

                }
            }

        } catch (LDAPException e) {
            //          System.out.println( "Error: " + e.toString() );
            return null;
        }

        return htTodasLotacoes;

    }

    /**
     * ****************************
     */
    public Vector getGruposMails(
            String loginAdministrador,
            String senhaAdministrador)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        mensagemLOG += " metodo: getGruposMails";
        vMensagensLog.add(mensagemLOG);
        Vector gruposMails = null;

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        LDAPConnection ld = null;

        try {
            ld = poolMaster.getConnection();
            pool = poolMaster;
        } catch (Exception e) {
            try {
                ld = poolSlave.getConnection();
                pool = poolSlave;
            } catch (Exception ex) {
                throw new ImplManutLDAPException(
                        "Erro de conexao tanto com a LDAP Master quanto a Slave : "
                        + e.toString() + "  " + ex.toString());
            }
        }

        logar(ld, loginAdministrador, senhaAdministrador);

        gruposMails = getGruposMails(ld);

        pool.close(ld);

        return gruposMails;

    }

    /**
     * **********************************************************************
     */
    private Vector getGruposMails(LDAPConnection ld)
            throws ImplManutLDAPException {

        LDAPEntry findEntry = null;
        LDAPAttribute atributo = null;
        Enumeration enume = null;

        LDAPSearchConstraints cons = null;
        String basePesquisa = "";
        int escopo = 0;
        LDAPSearchResults res;
        String[] atributos = {};

        String grupo = "";

        Vector gruposMails = new Vector();

        String filtro = "(objectclass=grupoMailTRT15)";

        try {

            cons = ld.getSearchConstraints();
            cons.setBatchSize(1);

            basePesquisa = "o=trt15";
            escopo = LDAPConnection.SCOPE_SUB;

            res = ld.search(basePesquisa, escopo, filtro, atributos, false, cons);

            while (res.hasMoreElements()) {

                grupo = "";

                findEntry = res.next();
                if (findEntry != null) {

                    atributo = findEntry.getAttribute("cn");
                    if (atributo != null) {
                        enume = atributo.getStringValues();
                        grupo = (String) enume.nextElement();
                        gruposMails.add(grupo.toLowerCase());
                    }
                }
            }

        } catch (LDAPException e) {
            //          System.out.println( "Error: " + e.toString() );
            return null;
        }

        return gruposMails;

    }

    /**
     * *************************************************
     */
    public void enviaMail(
            String SMTPServer,
            String Sender,
            String Recipient,
            String CcRecipient,
            String BccRecipient,
            String Subject,
            String Body,
            String Attachments)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));
        /*  
  mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
  mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();
         */
        mensagemLOG += " metodo: enviaMail";
        mensagemLOG += " Sender: " + Sender;
        mensagemLOG += " Recipient: " + Recipient;
        vMensagensLog.add(mensagemLOG);

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        try {
            SendMailAntigo.Send(
                    SMTPServer,
                    Sender,
                    Recipient,
                    CcRecipient,
                    BccRecipient,
                    Subject,
                    Body,
                    Attachments);
        } catch (Exception e) {
            throw (new ImplManutLDAPException(e.toString()));
        }

    }

    /**
     * ******************
     */
    public void enviaMail(
            String SMTPServer,
            String Sender,
            String Recipient,
            String CcRecipient,
            String BccRecipient,
            String Subject,
            String Body)
            throws ImplManutLDAPException, RemoteException {

        Vector vMensagensLog = new Vector();
        String cArquivoLog = "/var/log/rmi/acoes.log";
        String mensagemLOG = dataFormatada(new Date(System.currentTimeMillis()));

        mensagemLOG += " conexoes ldap master: " + poolMaster.getTotalConAtivas();
        mensagemLOG += " conexoes ldap slave: " + poolSlave.getTotalConAtivas();

        mensagemLOG += " metodo: enviaMail";
        mensagemLOG += " Sender: " + Sender;
        mensagemLOG += " Recipient: " + Recipient;
        vMensagensLog.add(mensagemLOG);

        try {
            gravaMensagensLog(vMensagensLog, cArquivoLog);
        } catch (Exception e) {
            System.out.println("Erro na gravacao do log: " + e.toString());
        }

        try {
            SendMail.Send(
                    SMTPServer,
                    Sender,
                    Recipient,
                    CcRecipient,
                    BccRecipient,
                    Subject,
                    Body);
        } catch (Exception e) {
            throw (new ImplManutLDAPException(e.toString()));
        }

    }

    private String dataFormatada(Date data) {

        if (data == null) {
            return "";
        }

        Calendar dataAbertura = Calendar.getInstance();
        dataAbertura.setTime(data);
        int dia = dataAbertura.get(Calendar.DAY_OF_MONTH);
        int mes = dataAbertura.get(Calendar.MONTH) + 1;
        int ano = dataAbertura.get(Calendar.YEAR);
        int hora = dataAbertura.get(Calendar.HOUR_OF_DAY);
        int minuto = dataAbertura.get(Calendar.MINUTE);
        int segundo = dataAbertura.get(Calendar.SECOND);

        String dataFormatada = ((dia < 10) ? "0" : "") + dia + "/";
        dataFormatada += ((mes < 10) ? "0" : "") + mes + "/";
        dataFormatada += ano + " ";
        dataFormatada += ((hora < 10) ? "0" : "") + hora + ":";
        dataFormatada += ((minuto < 10) ? "0" : "") + minuto + ":";
        dataFormatada += ((segundo < 10) ? "0" : "") + segundo;

        return dataFormatada;
    }

}

