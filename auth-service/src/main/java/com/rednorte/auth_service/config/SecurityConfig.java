package com.rednorte.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Apaga la protección por defecto para que podamos enviar datos desde React
            .csrf(csrf -> csrf.disable()) 
            // Le decimos que permita todas las peticiones a nuestra API sin mostrar la ventana verde
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() 
            );
            
        return http.build();
    }
}