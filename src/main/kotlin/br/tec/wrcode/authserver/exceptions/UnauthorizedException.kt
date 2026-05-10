package br.tec.wrcode.authserver.exceptions

class UnauthorizedException(message: String = "Credenciais ausentes ou inválidas") :
    RuntimeException(message)
