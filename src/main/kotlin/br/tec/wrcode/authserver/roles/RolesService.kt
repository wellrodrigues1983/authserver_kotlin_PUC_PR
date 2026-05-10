package br.tec.wrcode.authserver.roles

import br.tec.wrcode.authserver.exceptions.DuplicateResourceException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RoleService(val repository: RoleRepository) {

    private val log = LoggerFactory.getLogger(RoleService::class.java)

    @Transactional
    fun insert(role: Roles): Roles {
        val normalized = role.name.uppercase()
        if (repository.findByName(normalized) != null) {
            log.info("Role duplicada rejeitada: {}", normalized)
            throw DuplicateResourceException("A role '$normalized' já existe")
        }
        role.name = normalized
        log.info("Criando role nome={}", normalized)
        return repository.save(role)
    }

    @Transactional(readOnly = true)
    fun findAll() = repository.findAll()
}
