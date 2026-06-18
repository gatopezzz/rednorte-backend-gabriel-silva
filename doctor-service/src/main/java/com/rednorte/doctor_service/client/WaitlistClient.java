package com.rednorte.doctor_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

// Le decimos a Feign a qué URL base debe conectarse
@FeignClient(name = "waitlist-service", url = "http://localhost:8083/api/waitlist")
public interface WaitlistClient {

    @GetMapping("/especialidad/{especialidad}")
    List<Object> obtenerPorEspecialidad(@PathVariable("especialidad") String especialidad);

    @PatchMapping("/{id}/atender")
    String atenderPaciente(@PathVariable("id") Long id);
}