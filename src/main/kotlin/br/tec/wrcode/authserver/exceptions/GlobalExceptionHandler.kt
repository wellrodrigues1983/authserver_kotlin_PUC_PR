package br.tec.wrcode.authserver.exceptions

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

data class ApiError(
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val details: Map<String, String>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiError> {
        log.warn("Recurso não encontrado: {}", ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError(status = 404, error = "Não encontrado", message = ex.message ?: ""))
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<ApiError> {
        log.warn("Requisição não autenticada: {}", ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header("WWW-Authenticate", "Basic realm=\"authserver\"")
            .body(ApiError(status = 401, error = "Não autorizado", message = ex.message ?: ""))
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ResponseEntity<ApiError> {
        log.warn("Requisição proibida: {}", ex.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError(status = 403, error = "Proibido", message = ex.message ?: ""))
    }

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(ex: DuplicateResourceException): ResponseEntity<ApiError> {
        log.warn("Recurso duplicado: {}", ex.message)
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError(status = 409, error = "Conflito", message = ex.message ?: ""))
    }

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(ex: BusinessRuleException): ResponseEntity<ApiError> {
        log.warn("Violação de regra de negócio: {}", ex.message)
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiError(status = 422, error = "Entidade não processável", message = ex.message ?: ""))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "inválido")
        }
        log.warn("Falha de validação: {}", fieldErrors)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                status = 400,
                error = "Requisição inválida",
                message = "Falha de validação",
                details = fieldErrors
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        log.warn("Corpo da requisição inválido: {}", ex.mostSpecificCause.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                status = 400,
                error = "Requisição inválida",
                message = "Corpo da requisição malformado ou faltando campos obrigatórios"
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        log.warn("Argumento inválido: {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(status = 400, error = "Requisição inválida", message = ex.message ?: "Argumento inválido"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiError> {
        log.error("Exceção não tratada", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError(
                status = 500,
                error = "Erro interno do servidor",
                message = ex.message ?: "Erro inesperado"
            )
        )
    }
}
