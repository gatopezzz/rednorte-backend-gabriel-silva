package com.rednorte.doctor_service.controller;

import com.rednorte.doctor_service.client.WaitlistClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    @Autowired
    private WaitlistClient waitlistClient;

    @GetMapping("/mi-lista/{especialidad}")
    public ResponseEntity<?> obtenerMiLista(@PathVariable String especialidad) {
        // Feign se encarga de hacer la petición HTTP por debajo
        return ResponseEntity.ok(waitlistClient.obtenerPorEspecialidad(especialidad));
    }

    @PatchMapping("/atender/{id}")
    public ResponseEntity<?> atenderPaciente(@PathVariable Long id) {
        // Llamamos al otro microservicio limpiamente
        waitlistClient.atenderPaciente(id);
        return ResponseEntity.ok().body("{\"mensaje\": \"Paciente atendido\"}");
    }
}