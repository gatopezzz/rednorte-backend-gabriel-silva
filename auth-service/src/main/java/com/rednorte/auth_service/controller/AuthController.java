package com.rednorte.auth_service.controller;

import com.rednorte.auth_service.model.Especialidad;
import com.rednorte.auth_service.model.Rol;
import com.rednorte.auth_service.model.Usuario;
import com.rednorte.auth_service.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        String email = credenciales.get("email");
        String passwordPlana = credenciales.get("password");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent() && passwordEncoder.matches(passwordPlana, usuarioOpt.get().getPassword())) {
            Usuario u = usuarioOpt.get();
            
            String token = io.jsonwebtoken.Jwts.builder()
                .setSubject(u.getEmail())
                .claim("rol", u.getRol().name())
                .setIssuedAt(new java.util.Date())
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("clave-secreta-de-al-menos-32-caracteres-rednorte".getBytes()))
                .compact();

            return ResponseEntity.ok(Map.of(
                "token", token,
                "rol", u.getRol().name(),
                "especialidad", u.getEspecialidad().name()
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String rut = body.get("rut");
        String nombre = body.get("nombre");
        
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: El correo ya está en uso");
        }

        if (rut != null && usuarioRepository.existsByRut(rut)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: El RUT ya está registrado");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(email);
        nuevoUsuario.setRut(rut);
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setPassword(passwordEncoder.encode(body.get("password")));
        nuevoUsuario.setRol(Rol.PACIENTE); 
        nuevoUsuario.setEspecialidad(Especialidad.NINGUNA);
        usuarioRepository.save(nuevoUsuario);
        
        return ResponseEntity.ok("Usuario registrado con éxito");
    }

    @PatchMapping("/update-profile")
    public ResponseEntity<?> actualizarPerfil(@RequestBody Map<String, String> body) {
        String currentEmail = body.get("currentEmail");
        String newEmail = body.get("newEmail");
        String nombre = body.get("nombre");
        String password = body.get("password");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(currentEmail);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equals(currentEmail)) {
                if (usuarioRepository.findByEmail(newEmail).isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("El nuevo correo ya está en uso.");
                }
                usuario.setEmail(newEmail);
            }
            
            if (nombre != null && !nombre.trim().isEmpty()) {
                usuario.setNombre(nombre);
            }
            
            if (password != null && !password.trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(password));
            }
            
            usuarioRepository.save(usuario);
            return ResponseEntity.ok().body("{\"mensaje\": \"Perfil actualizado correctamente\"}");
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
    }

    @GetMapping("/users")
    public ResponseEntity<?> obtenerTodosLosUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    @PutMapping("/users/rol")
    public ResponseEntity<?> cambiarRolUsuario(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String nuevoRol = body.get("rol");
        String nuevaEspecialidad = body.getOrDefault("especialidad", "NINGUNA");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            try {
                usuario.setRol(Rol.valueOf(nuevoRol.toUpperCase()));
                
                if (usuario.getRol() == Rol.DOCTOR) {
                    usuario.setEspecialidad(Especialidad.valueOf(nuevaEspecialidad.toUpperCase()));
                } else {
                    usuario.setEspecialidad(Especialidad.NINGUNA);
                }
                
                usuarioRepository.save(usuario);
                return ResponseEntity.ok("Usuario actualizado correctamente");
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Rol o Especialidad no válidos");
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
    }

    @PutMapping("/users/update-admin")
    public ResponseEntity<?> adminUpdateUser(@RequestBody Map<String, String> body) {
        String originalEmail = body.get("originalEmail");
        String newEmail = body.get("email");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(originalEmail);

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();
            
            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equals(originalEmail)) {
                if (usuarioRepository.findByEmail(newEmail).isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("El nuevo correo ya está asignado a otro usuario.");
                }
                u.setEmail(newEmail);
            }

            u.setNombre(body.get("nombre"));
            u.setRut(body.get("rut"));
            
            String password = body.get("password");
            if (password != null && !password.trim().isEmpty()) {
                u.setPassword(passwordEncoder.encode(password));
            }

            // NUEVO: Atrapamos y guardamos el rol y especialidad en el mismo paso
            String rol = body.get("rol");
            if (rol != null) {
                u.setRol(Rol.valueOf(rol.toUpperCase()));
                if (u.getRol() == Rol.DOCTOR) {
                    String esp = body.getOrDefault("especialidad", "NINGUNA");
                    u.setEspecialidad(Especialidad.valueOf(esp.toUpperCase()));
                } else {
                    u.setEspecialidad(Especialidad.NINGUNA);
                }
            }
            
            usuarioRepository.save(u);
            return ResponseEntity.ok("Usuario actualizado por administrador");
        }
        return ResponseEntity.notFound().build();
    }

    @Transactional
    @DeleteMapping("/users/{email}")
    public ResponseEntity<?> eliminarUsuarioCompletamente(@PathVariable String email) {
        try {
            String urlWaitlist = "http://localhost:8083/api/waitlist/eliminar-por-correo/" + email;
            restTemplate.delete(urlWaitlist);

            return usuarioRepository.findByEmail(email)
                    .map(u -> {
                        usuarioRepository.delete(u);
                        return ResponseEntity.ok().body("{\"mensaje\": \"Usuario eliminado de todo el sistema\"}");
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error en la eliminación en cascada");
        }
    }

    @GetMapping("/me/{email}")
    public ResponseEntity<?> obtenerDatosSesion(@PathVariable String email) {
        return usuarioRepository.findByEmail(email)
                .map(u -> ResponseEntity.ok(Map.of(
                        "rol", u.getRol().name(),
                        "especialidad", u.getEspecialidad().name()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}