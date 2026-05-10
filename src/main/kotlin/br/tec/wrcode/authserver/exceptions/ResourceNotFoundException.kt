package br.tec.wrcode.authserver.exceptions

class ResourceNotFoundException(
    resource: String,
    identifier: Any
) : RuntimeException("$resource não encontrado(a): $identifier")
