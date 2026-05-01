package br.tec.wrcode.authserver.roles

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Roles (

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String,
    @Column(nullable = false)
    var description: String

){
}