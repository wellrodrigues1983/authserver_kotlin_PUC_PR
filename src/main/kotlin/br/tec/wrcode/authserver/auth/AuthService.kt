package br.tec.wrcode.authserver.auth

import br.tec.wrcode.authserver.exceptions.ForbiddenException
import br.tec.wrcode.authserver.exceptions.UnauthorizedException
import br.tec.wrcode.authserver.users.User
import br.tec.wrcode.authserver.users.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtEncoder: JwtEncoder,
    @Value("\${app.security.jwt.expiration-seconds:3600}") private val expirationSeconds: Long,
    @Value("\${app.security.jwt.issuer:authserver}") private val issuer: String
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional(readOnly = true)
    fun login(email: String, rawPassword: String): TokenResponse {
        val user = userRepository.findByEmail(email)
        if (user == null || user.password.isNullOrBlank()) {
            log.warn("Falha de login para email={} (usuário inexistente)", email)
            throw UnauthorizedException("Email ou senha inválidos")
        }
        if (!passwordEncoder.matches(rawPassword, user.password)) {
            log.warn("Falha de login para email={} (senha incorreta)", email)
            throw UnauthorizedException("Email ou senha inválidos")
        }
        user.role.size
        log.info("Login bem-sucedido para userId={} email={}", user.id, user.email)
        return issueToken(user)
    }

    fun issueToken(user: User): TokenResponse {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(expirationSeconds)
        val roles = user.role.map { it.name }
        val claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.email)
            .claim("userId", user.id)
            .claim("roles", roles)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return TokenResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = expirationSeconds,
            email = user.email ?: "",
            roles = roles
        )
    }

    @Transactional(readOnly = true)
    fun currentUser(jwt: Jwt): User {
        val email = jwt.subject
            ?: throw UnauthorizedException("Token sem subject")
        val user = userRepository.findByEmail(email)
            ?: throw UnauthorizedException("Usuário do token não encontrado")
        user.role.size
        return user
    }

    fun requireRole(user: User, role: String) {
        val normalized = role.uppercase()
        if (user.role.none { it.name.equals(normalized, ignoreCase = true) }) {
            log.warn("Usuário id={} não possui a role obrigatória {}", user.id, normalized)
            throw ForbiddenException("A role '$normalized' é obrigatória")
        }
    }
}
