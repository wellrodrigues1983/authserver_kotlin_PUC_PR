package br.tec.wrcode.authserver.users

import br.tec.wrcode.authserver.roles.RoleRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    val userRepository: UserRepository,
    val roleRepository: RoleRepository
) {

    fun insert(user: User): User? {
        val usuario = userRepository.findByEmail(user.email)
        if (usuario != null) {
            return null
        }
        return userRepository.save(user)
    }

    fun findAll(sortDir: String): List<User> {
        val users = userRepository.findAll()
        return if (sortDir.equals(other = "ASC", ignoreCase = true)) {
            users.sortedBy { it.name }
        } else {
            users.toList()
        }
    }

    fun findById(id: Long) = userRepository.findById(id)

    fun deleteById(id: Long): Unit = userRepository.deleteById(id)

    fun addRole(id: Long, roleName: String): Boolean? {
        val user = findByIdOrNull(id) ?: return null
        if (user.role.any { it.name == roleName }) return false

        val role = roleRepository.findByName(roleName) ?: return null

        user.role.add(role)
        userRepository.save(user)
        return true
    }

    fun findByIdOrNull(id: Long) = userRepository.findByIdOrNull(id)

}
