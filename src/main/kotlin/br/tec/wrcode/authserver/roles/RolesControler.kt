package br.tec.wrcode.authserver.roles

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/roles")
class RoleController(val roleService: RoleService) {

    @Operation(summary = "Cria uma nova role")
    @PostMapping
    @ApiResponse(responseCode = "201")
    fun insert(@RequestBody role: Roles): ResponseEntity<Roles> =
        ResponseEntity.status(HttpStatus.CREATED).body(roleService.insert(role))

    @Operation(summary = "Lista todas as roles")
    @GetMapping
    fun list() = roleService.findAll()
}
