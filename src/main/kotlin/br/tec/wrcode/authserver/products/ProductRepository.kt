package br.tec.wrcode.authserver.products

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository :
    JpaRepository<Product, Long>,
    JpaSpecificationExecutor<Product> {

}
