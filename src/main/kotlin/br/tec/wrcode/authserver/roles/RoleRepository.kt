package br.tec.wrcode.authserver.roles

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository: JpaRepository<Roles, Long> {
    fun findByName(name: String): Roles?
}