package com.rednorte.waitlist_service.controller;

import com.rednorte.waitlist_service.model.Especialidad;
import com.rednorte.waitlist_service.model.ListaEspera;
import com.rednorte.waitlist_service.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {

    @Autowired
    private WaitlistRepository repository;

    @GetMapping("/todas")
    public ResponseEntity<List<ListaEspera>> obtenerTodos() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/unirse")
    public ResponseEntity<?> unirse(@RequestBody ListaEspera entrada) {
        if (repository.existsByEmailAndEstado(entrada.getEmail(), "PENDIENTE")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya te encuentras en la lista de espera.");
        }

        try {
            entrada.setEstado("PENDIENTE");
            repository.save(entrada);
            return ResponseEntity.ok().body("{\"mensaje\": \"Unido a la lista exitosamente\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar la solicitud");
        }
    }

    @PatchMapping("/{id}/atender")
    public ResponseEntity<?> marcarComoAtendido(@PathVariable Long id) {
        return repository.findById(id)
                .map(paciente -> {
                    paciente.setEstado("ATENDIDO");
                    repository.save(paciente);
                    return ResponseEntity.ok().body("{\"mensaje\": \"Paciente marcado como atendido\"}");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/especialidad/{especialidad}")
    public ResponseEntity<List<ListaEspera>> obtenerPorEspecialidad(@PathVariable Especialidad especialidad) {
        return ResponseEntity.ok(repository.findByEspecialidadAndEstado(especialidad, "PENDIENTE"));
    }

    @Transactional
    @DeleteMapping("/eliminar-por-correo/{email}")
    public ResponseEntity<?> eliminarPorCorreo(@PathVariable String email) {
        repository.deleteByEmail(email);
        return ResponseEntity.ok().body("{\"mensaje\": \"Registros en lista de espera eliminados\"}");
    }
}