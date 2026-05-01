package br.tec.wrcode.authserver.users

import br.tec.wrcode.authserver.roles.Roles
import jakarta.persistence.*

@Entity
@Table(name = "UserTable")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(unique = true, nullable = false)
    var email: String? = null,

    @Column(nullable = false)
    var password: String? = null,

    @ManyToMany
    @JoinTable(name = "UserRole",
        joinColumns = [JoinColumn(name = "idUser")],
        inverseJoinColumns = [JoinColumn(name = "idRole")])
    var role: MutableSet<Roles> = mutableSetOf(),
)
