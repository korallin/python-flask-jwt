#!/usr/bin/env python
# -*- coding: utf-8 -*-
from unicodedata import normalize
import time

def __remove_acento(texto):
    return normalize('NFKD', texto.decode('utf-8')).encode('ASCII', 'ignore')

def capitaliza_nome_pessoa_antigo(nome_completo):
    excecao = ['dos','da','do','de']
    nome_capitalizado = ''
    nome_completo = __remove_acento(nome_completo)
    for n in nome_completo.lower().split(' '):
        if n not in excecao:
            n = n.capitalize()
        nome_capitalizado += n + " "
    return nome_capitalizado.strip()

def normaliza_nome_pessoa(nome_completo):
    nome_completo = normalize('NFKD', nome_completo.decode('utf-8')).encode('ASCII', 'ignore')
    excecao = ['dos','da','do','de']
    lista = [x.capitalize() if x not in excecao else x for x in nome_completo.split()]
    return ' '.join(lista)

print normaliza_nome_pessoa("josé de araújo mota")
