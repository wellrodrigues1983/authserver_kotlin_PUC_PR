package br.tec.wrcode.authserver.categories

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
class CategoryController(private val categoryService: CategoryService) {

    @Operation(
        summary = "Cadastra uma nova categoria",
        description = "Requer JWT. As categorias devem estar previamente cadastradas para poderem ser associadas a produtos.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping
    fun create(
        @Suppress("UNUSED_PARAMETER") @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CategoryRequest
    ): ResponseEntity<CategoryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request))

    @Operation(summary = "Lista todas as categorias")
    @GetMapping
    fun findAll(): List<CategoryResponse> = categoryService.findAll()

    @Operation(summary = "Busca uma categoria pelo id")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): CategoryResponse = categoryService.findById(id)

    @Operation(
        summary = "Atualiza uma categoria (requer JWT)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/{id}")
    fun update(
        @Suppress("UNUSED_PARAMETER") @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
        @Valid @RequestBody request: CategoryRequest
    ): CategoryResponse = categoryService.update(id, request)

    @Operation(
        summary = "Remove uma categoria (requer JWT). Falha se houver produtos associados.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/{id}")
    fun delete(
        @Suppress("UNUSED_PARAMETER") @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        categoryService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
