package br.tec.wrcode.authserver.categories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByNameIgnoreCase(name: String): Category?
}
