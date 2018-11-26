import re
import datetime
import functools
import jwt
from flask import Flask, request, jsonify
from flask_restful import Resource, Api, abort
from werkzeug.security import generate_password_hash, check_password_hash
import config

from acessoLDAP import AcessoLDAP

app = Flask(__name__)

app.config.from_object('config')
api = Api(app)

ldap = AcessoLDAP(app.config['SERV_LDAP'],app.config['PORT_LDAP'],app.config['BASE_DN_LDAP'],app.config['USER_DN_LDAP'],app.config['PASS_LDAP'])

def auth_required(method):
   @functools.wraps(method)
   def wrapper(*args, **kwargs):
      try:
         header = request.headers.get('Authorization')
         if header is None:
            abort(400, message='Empty or null authorization header.')
         decoded = jwt.decode(str(header), app.config['KEY'], algorithms='HS256')
      except jwt.DecodeError as e:
         abort(400, message=str(e))
      except jwt.ExpiredSignatureError as e:
         abort(400, message=str(e))
      return method(*args, **kwargs)
   return wrapper

class Pessoa(Resource):
   @auth_required
   def get(self,uid,attr=None):
     res = ldap.get_pessoa_by_uid(uid,attr)
     return jsonify(res)

class Senha(Resource):
   @auth_required
   def get(self):
      return "Funcionou a senha!"

api.add_resource(Pessoa, '/v1/pessoas', '/v1/pessoas/<string:uid>','/v1/pessoas/<string:uid>/<string:attr>')
api.add_resource(Senha, '/v1/senhas',)

if __name__ == '__main__':
   app.run(host='0.0.0.0',port=5000,debug=app.config['DEBUG'])
