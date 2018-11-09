package gov.trt.util;

import gov.trt.util.ldap.*;
import gov.trt.util.PropertiesLoader;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.rmi.*;

public class clienteLDAP {

	private static final String PROP_SERV_RMI = "servidorRMI";
	private static final String PROP_PORT_RMI_MASTER = "portRMImaster";
  private static final String PROP_PORT_RMI_SLAVE = "portRMIslave";
	private static final String PROP_CLASS_RMI = "classRMI";

	private static Properties properties;
  

	  /*********************************************/
	  public static void incluiAtributo(
	      String idLDAP,
	      String nomeAtributo,
	      String valorAtributo,
	      String loginAdministrador,
	      String senhaAdministrador)
	    throws ImplManutLDAPException {

	    String enderecoRMI = getEnderecoRMI();

	    try {
	      IManutencaoLDAP manutencao =
	        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
	      manutencao.incluiAtributo(
	        idLDAP,
	        nomeAtributo,
	        valorAtributo,
	        loginAdministrador,
	        senhaAdministrador);
	    } catch (Exception e) {
	      throw (new ImplManutLDAPException(e.toString()));
	    }
	  }

	
	
	/*********************************************/
  public static void alteraNome(
    String idLDAP,
    String nomeCompleto,
    String loginAdministrador,
    String senhaAdministrador)
    throws ImplManutLDAPException {

    String enderecoRMI = getEnderecoRMI();

    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      manutencao.alteraNome(
        idLDAP,
        nomeCompleto,
        loginAdministrador,
        senhaAdministrador);
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
  }
  
	/*********************************************/
	public static void alteraAtributo(
		String idLDAP,
		String nomeAtributo,
		String valorAtributo,
		String loginAdministrador,
		String senhaAdministrador)
		throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();

		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.alteraAtributo(
				idLDAP,
				nomeAtributo,
				valorAtributo,
				loginAdministrador,
				senhaAdministrador);
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}
	}

	/*********************************************/
	public static void movePessoa(
		String idLDAP,
		String idNovaLotacao,
		String loginAdministrador,
		String senhaAdministrador)
		throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();

		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.movePessoa(
				idLDAP,
				idNovaLotacao,
				loginAdministrador,
				senhaAdministrador);
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}

	/*********************************************/
	public static void validaSenha(String login, String senha)
		throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();
		boolean senhaValida = false;
    
		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			senhaValida = manutencao.validaSenha(login, senha);
			if (!senhaValida) {
				throw (new ImplManutLDAPException("Senha invalida"));
			}
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}

	/*********************************************/
	public static String getAtributo(
		String nomeAtributo,
		String valorAtributo,
		String nomeAtributoRetorno,
		String tipoEntry,
		String loginAdministrador,
		String senhaAdministrador)
		throws ImplManutLDAPException {

		String cMensagem = "";
		String enderecoRMI = getEnderecoRMI();

		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			cMensagem =
				manutencao.getAtributo(
					nomeAtributo,
					valorAtributo,
					nomeAtributoRetorno,
					tipoEntry,
					loginAdministrador,
					senhaAdministrador);
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

		return cMensagem;

	}

  /*********************************************/
  public static void incluiEstacao(
    String uid,
    String loginAdministrador,
    String senhaAdministrador) throws ImplManutLDAPException {
    
    String enderecoRMI = getEnderecoRMI();

    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      manutencao.incluiEstacao(
        uid,
        loginAdministrador,
        senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
  }

  /*********************************************/
  public static void excluiEstacao(
    String uid,
    String loginAdministrador,
    String senhaAdministrador) throws ImplManutLDAPException {
    
    String enderecoRMI = getEnderecoRMI();

    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      manutencao.excluiEstacao(
        uid,
        loginAdministrador,
        senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
  }
  
  /*********************************************/
  public static void incluiGrupoMail(
    String grupo,
    String idLDAP,
    String loginAdministrador,
    String senhaAdministrador) throws ImplManutLDAPException {
    
    String enderecoRMI = getEnderecoRMI();

    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      manutencao.incluiGrupoMail(
        grupo,
        idLDAP,
        loginAdministrador,
        senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
  }
  
  /*********************************************/
  public static void removeGrupoMail(
      String nomeAtributo,
      String valorAtributo,
      String loginAdministrador,
      String senhaAdministrador) throws ImplManutLDAPException {
    
    String enderecoRMI = getEnderecoRMI();

    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      manutencao.removeGrupoMail(
        nomeAtributo,
        valorAtributo,
        loginAdministrador,
        senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
  }

	/*********************************************/
	public static void incluiPessoa(
		String nomeCompleto,
		String uid,
		String idLotacao,
		String categoria,
		String idLDAP,
		String idPessoa,
		String userPassword,
		String uidNumber,
		String loginAdministrador,
		String senhaAdministrador) throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();

		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.incluiPessoa(
				nomeCompleto,
				uid,
				idLotacao,
				categoria,
				idLDAP,
				idPessoa,
				userPassword,
				uidNumber,
				loginAdministrador,
				senhaAdministrador) ;
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}


	/*********************************************/
	public static void incluiLotacao(
		String idLDAPMae, 
		String idLDAP, 
		String idLotacao, 
		String descricao, 
		String sigla, 
		String gidNumber, 
		String loginAdministrador,
		String senhaAdministrador) throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();

		try {
			IManutencaoLDAP manutencao = (IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.incluiLotacao(idLDAPMae, idLDAP, idLotacao, descricao, sigla, gidNumber, loginAdministrador, senhaAdministrador) ;
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}

	/***************************************************/
	public static void removePessoa(
		String nomeAtributo,
		String valorAtributo,
		String loginAdministrador,
		String senhaAdministrador)
		throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();

		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.removePessoa(nomeAtributo, valorAtributo, loginAdministrador, senhaAdministrador);
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}

	/***************************************************/
	public static void alteraLogin(
		String nomeAtributo,
		String valorAtributo,
		String loginAdministrador,
		String senhaAdministrador,
		String novoLogin) throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();
		
		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.alteraLogin(nomeAtributo, valorAtributo, loginAdministrador, senhaAdministrador, novoLogin) ;
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}

	/*****************************************/
	public static void alteraSenha(
		String login,
		String senhaOld,
		String senhaNew) throws ImplManutLDAPException {

		String enderecoRMI = getEnderecoRMI();
		
		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.alteraSenha(login,senhaOld,senhaNew) ;
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}

	/****************************************************/
	public static void alteraSenha(
		String login,
		String senhaNew,
		String loginAdministrador,
		String senhaAdministrador) throws ImplManutLDAPException {

		String cMensagem = "";
		String enderecoRMI = getEnderecoRMI();
		
		try {
			IManutencaoLDAP manutencao =
				(IManutencaoLDAP) Naming.lookup(enderecoRMI);
			manutencao.alteraSenha(login, senhaNew, loginAdministrador, senhaAdministrador) ;
		} catch (Exception e) {
			throw (new ImplManutLDAPException(e.toString()));
		}

	}
  
  /****************************************************/
  public static void enviaMail(String SMTPServer,
    String Sender,
    String Recipient,
    String CcRecipient,
    String BccRecipient,
    String Subject,
    String Body,
    String Attachments) throws ImplManutLDAPException {
      
    String enderecoRMI = getEnderecoRMI();

    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      manutencao.enviaMail(
         SMTPServer, 
         Sender, 
         Recipient, 
         CcRecipient, 
         BccRecipient, 
         Subject, 
         Body, 
         Attachments) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }

  }
  
  /****************************************************/
  public static void enviaMail(String SMTPServer,
    String Sender,
    String Recipient,
    String CcRecipient,
    String BccRecipient,
    String Subject,
    String Body) throws ImplManutLDAPException {
      
    String enderecoRMI = getEnderecoRMI();

    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      manutencao.enviaMail(
         SMTPServer, 
         Sender, 
         Recipient, 
         CcRecipient, 
         BccRecipient, 
         Subject, 
         Body) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }

  }


  /***************************************************/
  public static Pessoa getPessoa(
    String uid,
    String loginAdministrador,
    String senhaAdministrador) throws ImplManutLDAPException {

    String enderecoRMI = getEnderecoRMI();
    Pessoa pessoa = null ;
    
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      pessoa = manutencao.getPessoa(uid, loginAdministrador, senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
    return pessoa ;

  }
  
  /***************************************************/
  public static LotacaoLDAP getLotacao(
    int idLDAP,
    String loginAdministrador,
    String senhaAdministrador) throws ImplManutLDAPException {

    String enderecoRMI = getEnderecoRMI();
    LotacaoLDAP lotacao = null ;
    
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      lotacao = manutencao.getLotacao(idLDAP, loginAdministrador, senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
    return lotacao ;

  }
  
  /****************************/
  public static int getIdLotacaoTRT15(
      String loginAdministrador,
      String senhaAdministrador)
      throws ImplManutLDAPException, RemoteException {
    
    String enderecoRMI = getEnderecoRMI();
    int idLotacaoTRT15 = 0 ;
    
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      idLotacaoTRT15 = manutencao.getIdLotacaoTRT15(loginAdministrador, senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
    return idLotacaoTRT15 ;

  }
  
  /***************************/
  public static Vector getIdsLotacoes(
      int idLotacao,
      String loginAdministrador,
      String senhaAdministrador)
      throws ImplManutLDAPException, RemoteException {
    
    String enderecoRMI = getEnderecoRMI();
    Vector vIdsLotacoes = new Vector() ;
    
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
        vIdsLotacoes = manutencao.getIdsLotacoes(idLotacao, loginAdministrador, senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
    return vIdsLotacoes ;
  }
  
  /***************************/
  public static Vector getIdsPessoas(
      int idLotacao,
      String loginAdministrador,
      String senhaAdministrador)
      throws ImplManutLDAPException, RemoteException {
    
    String enderecoRMI = getEnderecoRMI();
    Vector vIdsPessoas = new Vector() ;
    
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
        vIdsPessoas = manutencao.getIdsPessoas(idLotacao, loginAdministrador, senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
    return vIdsPessoas ;
  }
  
  /*******************************/
  public static Hashtable getTodasLotacoes(
    String loginAdministrador,
    String senhaAdministrador)
    throws ImplManutLDAPException, RemoteException {

    
    String enderecoRMI = getEnderecoRMI();
    Hashtable htTodasLotacoes = null ;
    
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
        htTodasLotacoes = manutencao.getTodasLotacoes(loginAdministrador, senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
    return htTodasLotacoes ;
  }

  /*******************************/
  public static Vector getGruposMails(
    String loginAdministrador,
    String senhaAdministrador)
    throws ImplManutLDAPException {

    
    String enderecoRMI = getEnderecoRMI();
    Vector gruposMails = null ;
    
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
        gruposMails = manutencao.getGruposMails(loginAdministrador, senhaAdministrador) ;
    } catch (Exception e) {
      throw (new ImplManutLDAPException(e.toString()));
    }
    
    return gruposMails ;
  }
  
  /**************************************/
  private static String getEnderecoRMI() throws ImplManutLDAPException {
    
    String resultado = "" ;
    boolean erro = false ;
    String portRMI = "" ;
    String servidorRMI = "" ;
    String classRMI = "" ;
    String enderecoRMI = "" ;
    String msg = "" ;

    try {
      properties = PropertiesLoader.load(Class.forName("gov.trt.util.clienteLDAP"));
    } catch (Exception e) {
      return "";
    }

    portRMI = properties.getProperty(PROP_PORT_RMI_MASTER);

    servidorRMI = properties.getProperty(PROP_SERV_RMI);
    classRMI = properties.getProperty(PROP_CLASS_RMI);

    enderecoRMI = "//" + servidorRMI + ":" + portRMI + "/" + classRMI;
      
    try {
      IManutencaoLDAP manutencao =
        (IManutencaoLDAP) Naming.lookup(enderecoRMI);
      resultado =
        manutencao.getAtributo("idLDAP","2038","cn","lotacao","administrador","E2p7Qa3T");
    } catch (Exception e) {
      msg += e.toString() ;
      erro = true ;
    }
    
    if ( (!erro) && resultado.trim().equals("trt15")) {
      
      return enderecoRMI;
      
    } else {
      erro = false ;
      portRMI = properties.getProperty(PROP_PORT_RMI_SLAVE);
      enderecoRMI = "//" + servidorRMI + ":" + portRMI + "/" + classRMI;
      
      try {
        IManutencaoLDAP manutencao =
          (IManutencaoLDAP) Naming.lookup(enderecoRMI);
        resultado =
          manutencao.getAtributo("idLDAP","2038","cn","lotacao","administrador","E2p7Qa3T");        
      } catch (Exception e) {
        msg += " - " + e.toString() ;
        erro = true ;
      }

      if ( (!erro) && resultado.trim().equals("trt15")) {
        return enderecoRMI;
      } else {
        throw (new ImplManutLDAPException("Problemas tanto com o RMI master quanto como RMI slave: " + msg));
      }
    }
    
  }


}

