package br.tec.wrcode.authserver.products

import br.tec.wrcode.authserver.categories.CategoryService
import br.tec.wrcode.authserver.exceptions.BusinessRuleException
import br.tec.wrcode.authserver.exceptions.ForbiddenException
import br.tec.wrcode.authserver.exceptions.ResourceNotFoundException
import br.tec.wrcode.authserver.users.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryService: CategoryService
) {

    private val log = LoggerFactory.getLogger(ProductService::class.java)

    private val sortableFields = setOf("name", "price", "createdAt", "stock", "status", "id")

    @Transactional
    fun create(request: ProductRequest, owner: User): ProductResponse {

        val categories = categoryService.loadAllByIds(request.categoryIds)
        log.info(
            "Criando produto nome='{}' donoId={} categorias={}",
            request.name, owner.id, request.categoryIds
        )
        val product = Product(
            name = request.name.trim(),
            description = request.description.trim(),
            price = request.price,
            stock = request.stock,
            status = request.status,
            owner = owner,
            categories = categories.toMutableSet()
        )
        val saved = productRepository.save(product)
        log.info("Produto criado id={}", saved.id)
        return ProductResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): ProductResponse = ProductResponse.from(loadById(id))

    private fun loadById(id: Long): Product =
        productRepository.findById(id).orElseThrow {
            log.info("Produto não encontrado id={}", id)
            ResourceNotFoundException("Produto", id)
        }

    @Transactional(readOnly = true)
    fun search(
        name: String?,
        status: ProductStatus?,
        categoryId: Long?,
        priceMin: BigDecimal?,
        priceMax: BigDecimal?,
        sortBy: String,
        sortDir: String
    ): List<ProductResponse> {
        if (sortBy !in sortableFields) {
            throw BusinessRuleException(
                "Campo de ordenação inválido '$sortBy'. Valores permitidos: ${sortableFields.joinToString()}"
            )
        }
        val direction = runCatching { Sort.Direction.fromString(sortDir) }
            .getOrElse {
                throw BusinessRuleException("Direção de ordenação inválida '$sortDir'. Use ASC ou DESC")
            }
        if (priceMin != null && priceMax != null && priceMin > priceMax) {
            throw BusinessRuleException("priceMin não pode ser maior que priceMax")
        }

        val specs = listOfNotNull(
            ProductSpecifications.nameContains(name),
            ProductSpecifications.statusEquals(status),
            ProductSpecifications.hasCategory(categoryId),
            ProductSpecifications.priceAtLeast(priceMin),
            ProductSpecifications.priceAtMost(priceMax)
        )
        val spec: Specification<Product> = if (specs.isEmpty()) {
            Specification { _, _, cb -> cb.conjunction() }
        } else {
            Specification.allOf(specs)
        }

        log.debug(
            "Buscando produtos name='{}' status={} categoryId={} priceMin={} priceMax={} sortBy={} sortDir={}",
            name, status, categoryId, priceMin, priceMax, sortBy, direction
        )
        return productRepository.findAll(spec, Sort.by(direction, sortBy))
            .map(ProductResponse::from)
    }

    @Transactional
    fun update(id: Long, request: ProductRequest, requester: User): ProductResponse {
        val product = loadById(id)
        ensureCanModify(product, requester)

        log.info("Atualizando produto id={} pelo usuárioId={}", id, requester.id)
        product.name = request.name.trim()
        product.description = request.description.trim()
        product.price = request.price
        product.stock = request.stock
        product.status = request.status
        if (request.categoryIds.isNotEmpty()) {
            product.categories = categoryService.loadAllByIds(request.categoryIds).toMutableSet()
        }
        return ProductResponse.from(productRepository.save(product))
    }

    @Transactional
    fun delete(id: Long, requester: User) {
        val product = loadById(id)
        ensureCanModify(product, requester)
        log.warn("Removendo produto id={} pelo usuárioId={}", id, requester.id)
        productRepository.delete(product)
    }

    @Transactional
    fun addCategory(productId: Long, categoryId: Long, requester: User): ProductResponse {
        val product = loadById(productId)
        ensureCanModify(product, requester)
        val category = categoryService.loadById(categoryId)
        if (product.categories.any { it.id == category.id }) {
            log.info("Produto id={} já possui a categoria id={}", productId, categoryId)
            throw BusinessRuleException(
                "O produto $productId já está associado à categoria $categoryId"
            )
        }
        log.info(
            "Associando categoria id={} ao produto id={} pelo usuárioId={}",
            categoryId, productId, requester.id
        )
        product.categories.add(category)
        return ProductResponse.from(productRepository.save(product))
    }

    @Transactional
    fun removeCategory(productId: Long, categoryId: Long, requester: User): ProductResponse {
        val product = loadById(productId)
        ensureCanModify(product, requester)
        val present = product.categories.firstOrNull { it.id == categoryId }
            ?: throw BusinessRuleException(
                "O produto $productId não está associado à categoria $categoryId"
            )
        log.info(
            "Desassociando categoria id={} do produto id={} pelo usuárioId={}",
            categoryId, productId, requester.id
        )
        product.categories.remove(present)
        return ProductResponse.from(productRepository.save(product))
    }

    private fun ensureCanModify(product: Product, requester: User) {
        val isOwner = product.owner?.id == requester.id
        val isAdmin = requester.role.any { it.name.equals("ADMIN", ignoreCase = true) }
        if (!isOwner && !isAdmin) {
            log.warn(
                "Usuário id={} tentou modificar produto id={} pertencente ao usuárioId={}",
                requester.id, product.id, product.owner?.id
            )
            throw ForbiddenException("Apenas o dono do produto ou um ADMIN pode modificá-lo")
        }
    }
}
