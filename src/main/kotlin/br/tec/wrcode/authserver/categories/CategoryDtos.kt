package br.tec.wrcode.authserver.categories

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CategoryRequest(
    @field:NotBlank(message = "o nome é obrigatório")
    @field:Size(max = 80, message = "o nome deve ter no máximo 80 caracteres")
    val name: String,

    @field:Size(max = 500, message = "a descrição deve ter no máximo 500 caracteres")
    val description: String = ""
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val description: String
) {
    companion object {
        fun from(category: Category): CategoryResponse = CategoryResponse(
            id = category.id ?: 0,
            name = category.name,
            description = category.description
        )
    }
}
