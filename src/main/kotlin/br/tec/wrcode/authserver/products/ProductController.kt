package br.tec.wrcode.authserver.products

import br.tec.wrcode.authserver.auth.AuthService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/products")
class ProductController(
    private val productService: ProductService,
    private val authService: AuthService
) {

    @Operation(
        summary = "Cadastra um novo produto",
        description = "Requer JWT. Pode receber categoryIds opcionais; todas as categorias informadas precisam estar previamente cadastradas.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ProductRequest
    ): ResponseEntity<ProductResponse> {
        val owner = authService.currentUser(jwt)
        val created = productService.create(request, owner)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @Operation(
        summary = "Lista produtos com filtros e ordenação",
        description = "Filtros opcionais: name (parcial, case-insensitive), status, categoryId, priceMin, priceMax. " +
            "Ordenação: sortBy=name|price|createdAt|sku|stock|status|id, sortDir=ASC|DESC."
    )
    @GetMapping
    fun search(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) status: ProductStatus?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) priceMin: BigDecimal?,
        @RequestParam(required = false) priceMax: BigDecimal?,
        @RequestParam(required = false, defaultValue = "createdAt") sortBy: String,
        @RequestParam(required = false, defaultValue = "DESC") sortDir: String
    ): List<ProductResponse> =
        productService.search(name, status, categoryId, priceMin, priceMax, sortBy, sortDir)

    @Operation(summary = "Busca um produto pelo id")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ProductResponse = productService.findById(id)

    @Operation(
        summary = "Atualiza um produto (apenas dono ou ADMIN)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductRequest
    ): ProductResponse {
        val requester = authService.currentUser(jwt)
        return productService.update(id, request, requester)
    }

    @Operation(
        summary = "Remove um produto (apenas dono ou ADMIN)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val requester = authService.currentUser(jwt)
        productService.delete(id, requester)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Associa uma categoria existente a um produto (apenas dono ou ADMIN)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/{id}/categories/{categoryId}")
    fun addCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
        @PathVariable categoryId: Long
    ): ProductResponse {
        val requester = authService.currentUser(jwt)
        return productService.addCategory(id, categoryId, requester)
    }

    @Operation(
        summary = "Desassocia uma categoria de um produto (apenas dono ou ADMIN)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/{id}/categories/{categoryId}")
    fun removeCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: Long,
        @PathVariable categoryId: Long
    ): ProductResponse {
        val requester = authService.currentUser(jwt)
        return productService.removeCategory(id, categoryId, requester)
    }
}
