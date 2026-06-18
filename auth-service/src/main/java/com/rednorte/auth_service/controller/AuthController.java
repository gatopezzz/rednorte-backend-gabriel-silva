package com.rednorte.auth_service.controller;

import com.rednorte.auth_service.client.WaitlistClient;
import com.rednorte.auth_service.model.Especialidad;
import com.rednorte.auth_service.model.Rol;
import com.rednorte.auth_service.model.SolicitudTutoria;
import com.rednorte.auth_service.model.Usuario;
import com.rednorte.auth_service.repository.SolicitudTutoriaRepository;
import com.rednorte.auth_service.repository.UsuarioRepository;
import com.rednorte.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación y Gestión de Usuarios", description = "Endpoints para login, registro, gestión de roles y tutorías")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final SolicitudTutoriaRepository solicitudTutoriaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final WaitlistClient waitlistClient;

    @Autowired
    public AuthController(UsuarioRepository usuarioRepository,
                          SolicitudTutoriaRepository solicitudTutoriaRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService,
                          WaitlistClient waitlistClient) {
        this.usuarioRepository = usuarioRepository;
        this.solicitudTutoriaRepository = solicitudTutoriaRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.waitlistClient = waitlistClient;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un token JWT junto con su rol y especialidad.")
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
                    .signWith(io.jsonwebtoken.security.Keys
                            .hmacShaKeyFor("clave-secreta-de-al-menos-32-caracteres-rednorte".getBytes()))
                    .compact();

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "id", String.valueOf(u.getId()),
                    "rol", u.getRol().name(),
                    "especialidad", u.getEspecialidad().name()));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea un nuevo usuario paciente en el sistema validando que el correo y RUT no existan previamente.")
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
    @Operation(summary = "Actualizar perfil de usuario", description = "Permite a un usuario modificar su correo, nombre, contraseña o edad.")
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

            // Validación de Edad para el Perfil
            String edadStr = body.get("edad");
            if (edadStr != null && !edadStr.trim().isEmpty()) {
                try {
                    usuario.setEdad(Integer.parseInt(edadStr));
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body("El formato de la edad no es válido");
                }
            }

            usuarioRepository.save(usuario);
            return ResponseEntity.ok().body("{\"mensaje\": \"Perfil actualizado correctamente\"}");
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
    }

    @GetMapping("/users")
    @Operation(summary = "Obtener lista de usuarios", description = "Devuelve todos los usuarios del sistema excluyendo a los administradores.")
    public ResponseEntity<?> obtenerTodosLosUsuarios() {
        List<Usuario> usuariosGenerales = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != Rol.ADMIN)
                .collect(Collectors.toList());

        return ResponseEntity.ok(usuariosGenerales);
    }

    @PutMapping("/users/rol")
    @Operation(summary = "Cambiar rol y especialidad", description = "Actualiza el rol de un usuario y le asigna una especialidad si el nuevo rol es DOCTOR.")
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
    @Operation(summary = "Actualización completa por administrador", description = "Permite a un administrador forzar la actualización de los datos de cualquier usuario.")
    public ResponseEntity<?> adminUpdateUser(@RequestBody Map<String, String> body) {
        String originalEmail = body.get("originalEmail");
        String newEmail = body.get("email");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(originalEmail);

        if (usuarioOpt.isPresent()) {
            Usuario u = usuarioOpt.get();

            if (newEmail != null && !newEmail.trim().isEmpty() && !newEmail.equals(originalEmail)) {
                if (usuarioRepository.findByEmail(newEmail).isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("El nuevo correo ya está asignado a otro usuario.");
                }
                u.setEmail(newEmail);
            }

            u.setNombre(body.get("nombre"));
            u.setRut(body.get("rut"));

            // Validación de Edad para el Admin
            String edadStr = body.get("edad");
            if (edadStr != null && !edadStr.trim().isEmpty()) {
                try {
                    u.setEdad(Integer.parseInt(edadStr));
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body("El formato de la edad no es válido");
                }
            }

            String password = body.get("password");
            if (password != null && !password.trim().isEmpty()) {
                u.setPassword(passwordEncoder.encode(password));
            }

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
    @Operation(summary = "Eliminar usuario", description = "Elimina permanentemente a un usuario del sistema y envía una petición para borrarlo de la lista de espera.")
    public ResponseEntity<?> eliminarUsuarioCompletamente(@PathVariable String email) {
        try {
            waitlistClient.eliminarPorCorreo(email);

            return usuarioRepository.findByEmail(email)
                    .map(u -> {
                        usuarioRepository.delete(u);
                        return ResponseEntity.ok().body("{\"mensaje\": \"Usuario eliminado de todo el sistema\"}");
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en la eliminación en cascada: " + e.getMessage());
        }
    }

    @GetMapping("/me/{email}")
    @Operation(summary = "Obtener datos de sesión", description = "Devuelve información rápida del usuario (rol y especialidad) basándose en su correo electrónico.")
    public ResponseEntity<?> obtenerDatosSesion(@PathVariable String email) {
        return usuarioRepository.findByEmail(email)
                .map(u -> ResponseEntity.ok(Map.of(
                        "rol", u.getRol().name(),
                        "especialidad", u.getEspecialidad().name())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuarios/email/{email}")
    public ResponseEntity<Usuario> obtenerUsuarioPorEmail(@PathVariable("email") String email) {
        return usuarioRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/especialidades")
    @Operation(summary = "Obtener especialidades", description = "Devuelve una lista con todas las especialidades médicas disponibles en la plataforma.")
    public ResponseEntity<?> obtenerEspecialidades() {
        return ResponseEntity.ok(Especialidad.values());
    }

    @PostMapping("/tutoria/solicitar")
    @Operation(summary = "Enviar solicitud de tutoría", description = "Permite a un usuario mayor de edad enviar una solicitud a un paciente de la tercera edad usando su email.")
    public ResponseEntity<?> solicitarTutoria(@RequestParam Long idSolicitante, @RequestParam String emailPaciente) {
        try {
            authService.enviarSolicitudTutoria(idSolicitante, emailPaciente);
            return ResponseEntity.ok("Solicitud enviada correctamente.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/tutoria/responder")
    @Operation(summary = "Responder solicitud de tutoría", description = "Permite a un paciente de la tercera edad aceptar o rechazar la solicitud de administración.")
    public ResponseEntity<?> responderTutoria(@RequestParam Long idSolicitud, @RequestParam boolean aceptada) {
        try {
            authService.responderSolicitudTutoria(idSolicitud, aceptada);
            return ResponseEntity.ok(aceptada ? "Tutoría aceptada exitosamente." : "Tutoría rechazada.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tutoria/pendientes/{idPaciente}")
    @Operation(summary = "Obtener solicitudes pendientes", description = "Devuelve la lista de solicitudes de tutoría pendientes para un paciente específico.")
    public ResponseEntity<List<SolicitudTutoria>> obtenerPendientes(@PathVariable Long idPaciente) {
        List<SolicitudTutoria> pendientes = solicitudTutoriaRepository
                .findByIdPacienteReceptorAndEstado(idPaciente, "PENDIENTE");
        return ResponseEntity.ok(pendientes);
    }

    @GetMapping("/tutoria/enviadas/{idSolicitante}")
    @Operation(summary = "Obtener solicitudes enviadas", description = "Devuelve la lista de solicitudes que un usuario ha enviado a otros para ser su tutor.")
    public ResponseEntity<List<SolicitudTutoria>> obtenerEnviadas(@PathVariable Long idSolicitante) {
        List<SolicitudTutoria> enviadas = solicitudTutoriaRepository.findAll().stream()
                .filter(s -> s.getIdSolicitante().equals(idSolicitante))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(enviadas);
    }
}