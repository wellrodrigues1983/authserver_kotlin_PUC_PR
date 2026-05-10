package br.tec.wrcode.authserver.init

import br.tec.wrcode.authserver.roles.RoleRepository
import br.tec.wrcode.authserver.roles.Roles
import br.tec.wrcode.authserver.users.User
import br.tec.wrcode.authserver.users.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DataSeed(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.security.admin.email}") private val adminEmail: String,
    @Value("\${app.security.admin.password}") private val adminPassword: String,
    @Value("\${app.security.admin.name:Administrador}") private val adminName: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataSeed::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val adminRole = ensureRole("ADMIN", "Administrador do sistema")
        ensureRole("USER", "Usuário comum")

        val existing = userRepository.findByEmail(adminEmail)
        if (existing == null) {
            val admin = User(
                name = adminName,
                email = adminEmail,
                password = passwordEncoder.encode(adminPassword),
                role = mutableSetOf(adminRole)
            )
            userRepository.save(admin)
            log.info("Usuário admin criado no startup: email={} (senha definida via app.security.admin.password)", adminEmail)
        } else {
            if (existing.role.none { it.name.equals("ADMIN", ignoreCase = true) }) {
                existing.role.add(adminRole)
                userRepository.save(existing)
                log.info("Role ADMIN garantida para usuário existente email={}", adminEmail)
            } else {
                log.info("Usuário admin já existe e possui a role ADMIN: email={}", adminEmail)
            }
        }
    }

    private fun ensureRole(name: String, description: String): Roles =
        roleRepository.findByName(name) ?: roleRepository.save(
            Roles(name = name, description = description)
        ).also { log.info("Role criada no startup: {}", name) }
}
