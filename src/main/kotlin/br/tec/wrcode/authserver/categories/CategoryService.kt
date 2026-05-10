package br.tec.wrcode.authserver.categories

import br.tec.wrcode.authserver.exceptions.BusinessRuleException
import br.tec.wrcode.authserver.exceptions.DuplicateResourceException
import br.tec.wrcode.authserver.exceptions.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {

    private val log = LoggerFactory.getLogger(CategoryService::class.java)

    @Transactional
    fun create(request: CategoryRequest): CategoryResponse {
        val name = request.name.trim()
        if (categoryRepository.findByNameIgnoreCase(name) != null) {
            log.info("Categoria duplicada rejeitada: {}", name)
            throw DuplicateResourceException("Já existe uma categoria com o nome '$name'")
        }
        log.info("Criando categoria nome={}", name)
        val saved = categoryRepository.save(
            Category(name = name, description = request.description.trim())
        )
        log.info("Categoria criada id={}", saved.id)
        return CategoryResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<CategoryResponse> =
        categoryRepository.findAll().map(CategoryResponse::from)

    @Transactional(readOnly = true)
    fun findById(id: Long): CategoryResponse =
        CategoryResponse.from(loadById(id))

    fun loadById(id: Long): Category =
        categoryRepository.findById(id).orElseThrow {
            log.info("Categoria não encontrada id={}", id)
            ResourceNotFoundException("Categoria", id)
        }

    fun loadAllByIds(ids: Collection<Long>): List<Category> {
        if (ids.isEmpty()) return emptyList()
        val found = categoryRepository.findAllById(ids)
        if (found.size != ids.toSet().size) {
            val foundIds = found.mapNotNull { it.id }.toSet()
            val missing = ids.toSet() - foundIds
            log.info("Categorias não cadastradas referenciadas: {}", missing)
            throw BusinessRuleException(
                "Categoria(s) não cadastrada(s): ${missing.joinToString()}"
            )
        }
        return found
    }

    @Transactional
    fun update(id: Long, request: CategoryRequest): CategoryResponse {
        val category = loadById(id)
        val newName = request.name.trim()
        val existing = categoryRepository.findByNameIgnoreCase(newName)
        if (existing != null && existing.id != id) {
            throw DuplicateResourceException("Já existe uma categoria com o nome '$newName'")
        }
        log.info("Atualizando categoria id={}", id)
        category.name = newName
        category.description = request.description.trim()
        return CategoryResponse.from(categoryRepository.save(category))
    }

    @Transactional
    fun delete(id: Long) {
        val category = loadById(id)
        log.warn("Removendo categoria id={}", id)
        try {
            categoryRepository.delete(category)
            categoryRepository.flush()
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            log.warn("Tentativa de remover categoria id={} com produtos associados", id)
            throw BusinessRuleException(
                "Não é possível remover a categoria '$id' pois há produtos associados"
            )
        }
    }
}
