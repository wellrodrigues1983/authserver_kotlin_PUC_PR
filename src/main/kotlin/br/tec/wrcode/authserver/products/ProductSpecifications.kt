package br.tec.wrcode.authserver.products

import br.tec.wrcode.authserver.categories.Category
import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal

object ProductSpecifications {

    fun nameContains(fragment: String?): Specification<Product>? =
        fragment?.takeIf { it.isNotBlank() }?.let {
            Specification { root, _, cb ->
                cb.like(cb.lower(root.get("name")), "%${it.lowercase()}%")
            }
        }

    fun statusEquals(status: ProductStatus?): Specification<Product>? =
        status?.let {
            Specification { root, _, cb -> cb.equal(root.get<ProductStatus>("status"), it) }
        }

    fun priceAtLeast(min: BigDecimal?): Specification<Product>? =
        min?.let {
            Specification { root, _, cb -> cb.greaterThanOrEqualTo(root.get("price"), it) }
        }

    fun priceAtMost(max: BigDecimal?): Specification<Product>? =
        max?.let {
            Specification { root, _, cb -> cb.lessThanOrEqualTo(root.get("price"), it) }
        }

    fun hasCategory(categoryId: Long?): Specification<Product>? =
        categoryId?.let {
            Specification { root, query, cb ->
                query?.distinct(true)
                val join = root.join<Product, Category>("categories", JoinType.INNER)
                cb.equal(join.get<Long>("id"), it)
            }
        }
}
