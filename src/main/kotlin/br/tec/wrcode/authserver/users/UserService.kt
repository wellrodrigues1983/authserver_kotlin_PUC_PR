package br.tec.wrcode.authserver.users

import br.tec.wrcode.authserver.exceptions.BusinessRuleException
import br.tec.wrcode.authserver.exceptions.DuplicateResourceException
import br.tec.wrcode.authserver.exceptions.ResourceNotFoundException
import br.tec.wrcode.authserver.roles.RoleRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    val userRepository: UserRepository,
    val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    @Transactional
    fun insert(user: User): User {
        val rawPassword = user.password
        if (rawPassword.isNullOrBlank()) {
            throw BusinessRuleException("A senha é obrigatória")
        }
        if (userRepository.findByEmail(user.email) != null) {
            log.info("Email de usuário duplicado rejeitado: {}", user.email)
            throw DuplicateResourceException("Já existe um usuário com o email '${user.email}'")
        }
        user.password = passwordEncoder.encode(rawPassword)
        log.info("Criando usuário email={}", user.email)
        val saved = userRepository.save(user)
        saved.role.size
        return saved
    }

    @Transactional(readOnly = true)
    fun findAll(sortDir: String): List<User> {
        val users = userRepository.findAll()
        users.forEach { it.role.size }
        return if (sortDir.equals("ASC", ignoreCase = true)) {
            users.sortedBy { it.name }
        } else {
            users
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): User {
        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Usuário", id)
        user.role.size
        return user
    }

    @Transactional
    fun deleteById(id: Long) {
        if (!userRepository.existsById(id)) {
            throw ResourceNotFoundException("Usuário", id)
        }
        log.warn("Removendo usuário id={}", id)
        userRepository.deleteById(id)
    }

    @Transactional
    fun addRole(id: Long, roleName: String): Boolean {
        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Usuário", id)
        val normalized = roleName.uppercase()
        if (user.role.any { it.name.equals(normalized, ignoreCase = true) }) {
            log.info("Usuário id={} já possui a role {}", id, normalized)
            return false
        }
        val role = roleRepository.findByName(normalized)
            ?: throw ResourceNotFoundException("Role", normalized)
        user.role.add(role)
        userRepository.save(user)
        log.info("Role {} concedida ao usuárioId={}", normalized, id)
        return true
    }
}
