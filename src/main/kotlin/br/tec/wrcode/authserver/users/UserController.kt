package br.tec.wrcode.authserver.users

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(val userService: UserService) {

    @Operation(summary = "Verifica se o serviço está respondendo")
    @GetMapping("/ping")
    fun ping() = mapOf("status" to "ok")

    @Operation(summary = "Cria um novo usuário")
    @PostMapping
    fun insert(@RequestBody user: User): ResponseEntity<User> =
        ResponseEntity.status(HttpStatus.CREATED).body(userService.insert(user))

    @Operation(summary = "Lista todos os usuários, opcionalmente ordenando pelo nome (ASC|DESC)")
    @GetMapping
    fun findAll(@RequestParam(required = false, defaultValue = "DESC") sortDir: String): List<User> =
        userService.findAll(sortDir)

    @Operation(summary = "Busca um usuário pelo id")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): User = userService.findById(id)

    @Operation(summary = "Remove um usuário pelo id")
    @DeleteMapping("/{id}")
    fun deleteById(@PathVariable id: Long): ResponseEntity<Void> {
        userService.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Concede uma role ao usuário")
    @PutMapping("/{id}/roles/{role}")
    fun grant(@PathVariable id: Long, @PathVariable role: String): ResponseEntity<Void> =
        if (userService.addRole(id, role.uppercase())) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.noContent().build()
        }
}
