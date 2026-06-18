package com.rednorte.waitlist_service.client;

import com.rednorte.waitlist_service.config.FeignClientConfig;
import com.rednorte.waitlist_service.model.UsuarioDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "auth-service", 
    url = "http://localhost:8081", 
    configuration = FeignClientConfig.class
)
public interface AuthClient {
    
    @GetMapping("/api/auth/usuarios/{id}")
    UsuarioDto obtenerUsuarioPorId(@PathVariable("id") Long id); 

    @GetMapping("/api/auth/usuarios/email/{email}")
    UsuarioDto obtenerUsuarioPorEmail(@PathVariable("email") String email); 
}