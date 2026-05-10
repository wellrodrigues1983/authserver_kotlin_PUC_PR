package br.tec.wrcode.authserver.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "o email é obrigatório")
    @field:Email(message = "email inválido")
    val email: String,

    @field:NotBlank(message = "a senha é obrigatória")
    val password: String
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val email: String,
    val roles: List<String>
)
