package br.tec.wrcode.authserver.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @Operation(
        summary = "Autentica um usuário e retorna um token JWT",
        description = "Recebe email e senha. A senha é validada com BCrypt contra o hash armazenado."
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        authService.login(request.email, request.password)

    @Operation(
        summary = "Retorna os dados do usuário autenticado",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): Map<String, Any?> {
        val user = authService.currentUser(jwt)
        return mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "roles" to user.role.map { it.name }
        )
    }
}
