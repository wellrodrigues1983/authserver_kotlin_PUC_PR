package br.tec.wrcode.authserver.exceptions

class ForbiddenException(message: String = "Acesso negado") :
    RuntimeException(message)
