package com.rednorte.auth_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Declaramos el cliente Feign apuntando al puerto del waitlist-service
@FeignClient(name = "waitlist-service", url = "http://localhost:8083/api/waitlist")
public interface WaitlistClient {

    // Mapeamos el endpoint de eliminación en cascada que ya tiene el WaitlistController
    @DeleteMapping("/eliminar-por-correo/{email}")
    void eliminarPorCorreo(@PathVariable("email") String email);
}