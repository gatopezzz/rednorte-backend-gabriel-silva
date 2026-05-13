package com.rednorte.doctor_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/mi-lista/{especialidad}")
    public ResponseEntity<?> obtenerMiLista(@PathVariable String especialidad) {
        String url = "http://localhost:8083/api/waitlist/especialidad/" + especialidad;
        Object[] pacientes = restTemplate.getForObject(url, Object[].class);
        return ResponseEntity.ok(pacientes);
    }

    @PatchMapping("/atender/{id}")
    public ResponseEntity<?> atenderPaciente(@PathVariable Long id) {
        String url = "http://localhost:8083/api/waitlist/" + id + "/atender";
        restTemplate.patchForObject(url, null, String.class);
        return ResponseEntity.ok().body("{\"mensaje\": \"Paciente atendido\"}");
    }
}