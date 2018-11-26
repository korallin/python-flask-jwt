import ldap
import sys

class AcessoLDAP:
  def __init__(self, servidor, porta, base_dn, usuario, senha):
    self.servidor = servidor
    self.porta = porta
    self.base_dn = base_dn
    self.usuario = usuario
    self.senha = senha

  def get_pessoa_by_uid(self, uid, attr=None):
      conn = ldap.initialize(self.servidor)
      conn.bind_s(self.usuario,self.senha)
      if attr is not None:
        attr = [attr]
      result = conn.search_s(self.base_dn,ldap.SCOPE_SUBTREE,"uid="+uid,attr)
      conn.unbind_s()
      return result
