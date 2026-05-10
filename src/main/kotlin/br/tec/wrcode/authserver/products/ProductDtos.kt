package br.tec.wrcode.authserver.products

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductRequest(
    @field:NotBlank(message = "o nome é obrigatório")
    @field:Size(max = 200, message = "o nome deve ter no máximo 200 caracteres")
    val name: String,

    @field:Size(max = 1000, message = "a descrição deve ter no máximo 1000 caracteres")
    val description: String = "",

    @field:DecimalMin(value = "0.00", inclusive = true, message = "o preço deve ser maior ou igual a 0")
    val price: BigDecimal,

    @field:Min(value = 0, message = "o estoque não pode ser negativo")
    val stock: Int = 0,

    val status: ProductStatus = ProductStatus.ACTIVE,

    val categoryIds: Set<Long> = emptySet()
)

data class CategoryRef(
    val id: Long,
    val name: String
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val stock: Int,
    val status: ProductStatus,
    val createdAt: LocalDateTime,
    val ownerId: Long?,
    val ownerName: String?,
    val categories: List<CategoryRef>
) {
    companion object {
        fun from(product: Product): ProductResponse = ProductResponse(
            id = product.id ?: 0,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            status = product.status,
            createdAt = product.createdAt,
            ownerId = product.owner?.id,
            ownerName = product.owner?.name,
            categories = product.categories
                .sortedBy { it.id }
                .map { CategoryRef(it.id ?: 0, it.name) }
        )
    }
}
