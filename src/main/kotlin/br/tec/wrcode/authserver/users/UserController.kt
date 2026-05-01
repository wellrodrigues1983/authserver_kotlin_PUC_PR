package br.tec.wrcode.authserver.users

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(val userService: UserService) {

    @GetMapping("/ping")
    fun ping() = mapOf("status" to "ok")

    @PostMapping
    fun insert(@RequestBody user: User): ResponseEntity<User> {
        val created = userService.insert(user)

        return if (created?.id != null) {
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping
    fun findAll(@RequestParam(required = false, defaultValue = "DESC") sortDir: String): List<User> {
        return userService.findAll(sortDir)
    }

    @GetMapping("/{id}")
    fun findByIdOrNull(@PathVariable id: Long) = userService.findById(id).let { ResponseEntity.ok(it) }

    @DeleteMapping("/{id}")
    fun deleteById(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            userService.deleteById(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}/roles/{role}")
    fun grant(@PathVariable id: Long, @PathVariable role: String): ResponseEntity<Void> =
        userService.addRole(id, role.uppercase())
            ?.let { if (it) ResponseEntity.ok().build() else ResponseEntity.noContent().build() }
            ?: ResponseEntity.badRequest().build()


}