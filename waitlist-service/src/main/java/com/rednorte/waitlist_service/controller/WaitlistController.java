package com.rednorte.waitlist_service.controller;

import com.rednorte.waitlist_service.model.Especialidad;
import com.rednorte.waitlist_service.model.ListaEspera;
import com.rednorte.waitlist_service.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@Tag(name = "Lista de Espera", description = "Endpoints para la gestión de pacientes y turnos en la lista de espera médica")
public class WaitlistController {

    @Autowired
    private WaitlistService waitlistService;

    @GetMapping("/todas")
    @Operation(summary = "Obtener lista completa", description = "Devuelve todos los registros activos actualmente en la lista de espera.")
    public ResponseEntity<List<ListaEspera>> obtenerTodos() {
        return ResponseEntity.ok(waitlistService.obtenerTodos());
    }

    @PostMapping("/unirse")
    @Operation(summary = "Unirse a la lista", description = "Añade un nuevo paciente a la lista de espera validando que no se encuentre ya registrado en la misma especialidad.")
    public ResponseEntity<?> unirse(@RequestBody ListaEspera entrada) {
        try {
            waitlistService.unirseALista(entrada);
            return ResponseEntity.ok().body("{\"mensaje\": \"Unido a la lista exitosamente\"}");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar la solicitud: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/atender")
    @Operation(summary = "Marcar como atendido", description = "Cambia el estado de un registro de lista de espera a ATENDIDO.")
    public ResponseEntity<?> marcarComoAtendido(@PathVariable Long id) {
        try {
            waitlistService.marcarComoAtendido(id);
            return ResponseEntity.ok().body("{\"mensaje\": \"Paciente marcado como atendido\"}");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/especialidad/{especialidad}")
    @Operation(summary = "Filtrar por especialidad", description = "Devuelve únicamente los registros de lista de espera que coincidan con la especialidad indicada.")
    public ResponseEntity<List<ListaEspera>> obtenerPorEspecialidad(@PathVariable Especialidad especialidad) {
        return ResponseEntity.ok(waitlistService.obtenerPorEspecialidad(especialidad));
    }

    @DeleteMapping("/eliminar-por-correo/{email}")
    @Operation(summary = "Eliminar en cascada", description = "Endpoint interno utilizado para eliminar todos los registros de un paciente en base a su correo electrónico.")
    public ResponseEntity<?> eliminarPorCorreo(@PathVariable String email) {
        waitlistService.eliminarPorCorreo(email);
        return ResponseEntity.ok().body("{\"mensaje\": \"Registros en lista de espera eliminados\"}");
    }
}