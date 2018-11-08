import re
import datetime
import functools
import jwt
from flask import Flask, request
from flask_restful import Resource, Api, abort
from werkzeug.security import generate_password_hash, check_password_hash
import config

app = Flask(__name__)

app.config.from_object('config')
api = Api(app)

def auth_required(method):
   @functools.wraps(method)
   def wrapper(self):
      try:
         header = request.headers.get('Authorization')
         if header is None:
            abort(400, message='Empty or null authorization header.')
         decoded = jwt.decode(str(header), app.config['KEY'], algorithms='HS256')
      except jwt.DecodeError as e: 
         abort(400, message=str(e))
      except jwt.ExpiredSignatureError as e:
         abort(400, message=str(e))
      return method(self)
   return wrapper

class Pessoa(Resource):
   @auth_required
   def get(self):
      return "Funcionou!"

class Senha(Resource):
   @auth_required
   def get(self):
      return "Funcionou a senha!"

api.add_resource(Pessoa, '/v1/pessoa')
api.add_resource(Senha, '/v1/senha')

if __name__ == '__main__':
   app.run(host='0.0.0.0',port=5000,debug=True)
