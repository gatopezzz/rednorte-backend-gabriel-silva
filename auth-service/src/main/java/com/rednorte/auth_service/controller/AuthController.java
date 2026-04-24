package com.rednorte.auth_service.controller;

import com.rednorte.auth_service.model.Usuario;
import com.rednorte.auth_service.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository repository;
    
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostMapping("/registro")
    public String registro(@RequestBody Usuario user) {
        user.setPassword(encoder.encode(user.getPassword()));
        repository.save(user);
        return "Usuario registrado con éxito";
    }

    @PostMapping("/login")
    public String login(@RequestBody Usuario user) {
        return repository.findByEmail(user.getEmail())
            .filter(u -> encoder.matches(user.getPassword(), u.getPassword()))
            .map(u -> "Login exitoso. Bienvenido, " + u.getNombre())
            .orElse("Error en credenciales");
    }
}