#!/usr/bin/env python
# -*- coding: utf-8 -*-
from unicodedata import normalize
import time

def __remove_acento(texto):
    return normalize('NFKD', texto.decode('utf-8')).encode('ASCII', 'ignore')

def capitaliza_nome_pessoa(nome_completo):
    excecao = ['dos','da','do','de']
    nome_capitalizado = ''
    nome_completo = __remove_acento(nome_completo)
    for n in nome_completo.lower().split(' '):        
        if n not in excecao:
            n = n.capitalize()
        nome_capitalizado += n + " "
    return nome_capitalizado.strip()

start_time = time.time()
print capitaliza_nome_pessoa("josé araújo")
print("--- %s seconds ---" % (time.time() - start_time))

start_time = time.time()
excecao = ['do']
lista = [x.capitalize() if x not in excecao else x for x in __remove_acento("josé araújo").split()]
print ' '.join(lista)
print("--- %s seconds ---" % (time.time() - start_time))
