package com.rednorte.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rednorte.auth_service.model.Especialidad;
import com.rednorte.auth_service.model.Rol;
import com.rednorte.auth_service.model.Usuario;
import com.rednorte.auth_service.repository.UsuarioRepository;
import com.rednorte.auth_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class })
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private AuthService authService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // --- TESTS DE REGISTRO ---

    @Test
    void registrarUsuario_DebeRetornar200_CuandoDatosSonCorrectos() throws Exception {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"nombre\":\"Juan\", \"password\":\"123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void registrarUsuario_DebeRetornar409_CuandoEmailYaExiste() throws Exception {
        Usuario usuarioExistente = new Usuario();
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuarioExistente));

        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"nombre\":\"Juan\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Error: El correo ya está en uso"));
    }

    @Test
    void registrarUsuario_DebeRetornar409_CuandoRutYaExiste() throws Exception {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.existsByRut("12345678-9")).thenReturn(true);

        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nuevo@test.com\", \"rut\":\"12345678-9\", \"nombre\":\"Juan\", \"password\":\"123\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Error: El RUT ya está registrado"));
    }

    // --- TESTS DE LOGIN Y SESIÓN ---

    @Test
    void login_DebeRetornarToken_CuandoCredencialesSonCorrectas() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("medico@test.com");
        usuario.setPassword("hash123");
        usuario.setRol(Rol.DOCTOR);
        usuario.setEspecialidad(Especialidad.CARDIOLOGIA);

        when(usuarioRepository.findByEmail("medico@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave123", "hash123")).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"medico@test.com\", \"password\":\"clave123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.rol").value("DOCTOR"))
                .andExpect(jsonPath("$.especialidad").value("CARDIOLOGIA"));
    }

    @Test
    void login_DebeRetornar401_CuandoCredencialesSonInvalidas() throws Exception {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"noexiste@test.com\", \"password\":\"123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Credenciales inválidas"));
    }

    @Test
    void obtenerDatosSesion_DebeRetornar200_ConDatosDelUsuario() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("sesion@test.com");
        usuario.setRol(Rol.PACIENTE);
        usuario.setEspecialidad(Especialidad.NINGUNA);

        when(usuarioRepository.findByEmail("sesion@test.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/api/auth/me/sesion@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.especialidad").value("NINGUNA"));
    }

    // --- TESTS DE OBTENCIÓN DE USUARIOS Y ESPECIALIDADES ---

    @Test
    void obtenerUsuarios_DebeRetornarListaVacia_CuandoNoHayUsuarios() throws Exception {
        when(usuarioRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void obtenerTodosLosUsuarios_DebeExcluirAlAdmin() throws Exception {
        Usuario admin = new Usuario();
        admin.setRol(Rol.ADMIN);
        
        Usuario paciente = new Usuario();
        paciente.setRol(Rol.PACIENTE);
        
        when(usuarioRepository.findAll()).thenReturn(List.of(admin, paciente));

        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rol").value("PACIENTE"));
    }

    @Test
    void obtenerEspecialidades_DebeRetornarListaDeValores() throws Exception {
        mockMvc.perform(get("/api/auth/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    // --- TESTS DE ACTUALIZACIÓN DE PERFIL ---

    @Test
    void actualizarPerfil_DebeRetornar200_CuandoDatosSonValidos() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("actual@test.com");
        when(usuarioRepository.findByEmail("actual@test.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/auth/update-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentEmail\":\"actual@test.com\", \"newEmail\":\"nuevo@test.com\", \"nombre\":\"Nuevo Nombre\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Perfil actualizado correctamente"));
    }

    @Test
    void actualizarPerfil_DebeRetornar404_SiUsuarioNoExiste() throws Exception {
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/auth/update-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentEmail\":\"nadie@test.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Usuario no encontrado"));
    }

    @Test
    void actualizarPerfil_DebeRetornar409_CuandoNuevoEmailYaExiste() throws Exception {
        Usuario usuarioActual = new Usuario();
        usuarioActual.setEmail("actual@test.com");
        
        when(usuarioRepository.findByEmail("actual@test.com")).thenReturn(Optional.of(usuarioActual));
        when(usuarioRepository.findByEmail("ocupado@test.com")).thenReturn(Optional.of(new Usuario()));

        mockMvc.perform(patch("/api/auth/update-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentEmail\":\"actual@test.com\", \"newEmail\":\"ocupado@test.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("El nuevo correo ya está en uso."));
    }

    // --- TESTS DE GESTIÓN DE ROLES ---

    @Test
    void cambiarRolUsuario_DebeRetornar200_CuandoEsExitoso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@test.com");
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(put("/api/auth/users/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"rol\":\"DOCTOR\", \"especialidad\":\"CARDIOLOGIA\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario actualizado correctamente"));
    }

    @Test
    void cambiarRolUsuario_DebeRetornar400_SiRolEsInvalido() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("test@test.com");
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(put("/api/auth/users/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"rol\":\"ROL_FALSO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Rol o Especialidad no válidos"));
    }

    @Test
    void cambiarRolUsuario_DebeRetornar404_SiUsuarioNoExiste() throws Exception {
        when(usuarioRepository.findByEmail("fantasma@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/auth/users/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"fantasma@test.com\", \"rol\":\"DOCTOR\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Usuario no encontrado"));
    }

    // --- TESTS DE ACTUALIZACIÓN ADMIN ---

    @Test
    void adminUpdateUser_DebeRetornar200_CuandoActualizaExitosamente() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail("original@test.com");
        when(usuarioRepository.findByEmail("original@test.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(put("/api/auth/users/update-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalEmail\":\"original@test.com\", \"nombre\":\"Nombre Admin\", \"rol\":\"PACIENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario actualizado por administrador"));
    }

    @Test
    void adminUpdateUser_DebeRetornar404_CuandoUsuarioNoExiste() throws Exception {
        when(usuarioRepository.findByEmail("fantasma@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/auth/users/update-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalEmail\":\"fantasma@test.com\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminUpdateUser_DebeRetornar409_CuandoNuevoEmailYaEstaOcupado() throws Exception {
        Usuario usuarioActual = new Usuario();
        usuarioActual.setEmail("admin@test.com");

        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(usuarioActual));
        when(usuarioRepository.findByEmail("ocupado@test.com")).thenReturn(Optional.of(new Usuario()));

        mockMvc.perform(put("/api/auth/users/update-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalEmail\":\"admin@test.com\", \"email\":\"ocupado@test.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("El nuevo correo ya está asignado a otro usuario."));
    }

    // --- TESTS DE ELIMINACIÓN ---

    @Test
    void eliminarUsuarioCompletamente_DebeLlamarAlWaitlistYBorrar() throws Exception {
        String email = "borrar@test.com";
        Usuario usuario = new Usuario();
        usuario.setEmail(email);

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));

        mockMvc.perform(delete("/api/auth/users/" + email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario eliminado de todo el sistema"));
        
        org.mockito.Mockito.verify(restTemplate).delete("http://localhost:8083/api/waitlist/eliminar-por-correo/" + email);
    }

    @Test
    void eliminarUsuarioCompletamente_DebeRetornar404_CuandoUsuarioNoExiste() throws Exception {
        String email = "fantasma@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/auth/users/" + email))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUsuarioCompletamente_DebeRetornar500_CuandoFallaMicroservicio() throws Exception {
        String email = "error@test.com";
        org.mockito.Mockito.doThrow(new RuntimeException("Servicio caído"))
            .when(restTemplate).delete(anyString());

        mockMvc.perform(delete("/api/auth/users/" + email))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error en la eliminación en cascada"));
    }

    // --- TESTS DE TUTORÍAS ---

    @Test
    void enviarSolicitudTutoria_ExitoTest() throws Exception {
        mockMvc.perform(post("/api/auth/tutoria/solicitar")
                .param("idSolicitante", "1")
                .param("emailPaciente", "abuelo@correo.com"))
                .andExpect(status().isOk());
    }

    @Test
    void solicitarTutoria_DebeRetornar400_CuandoFalla() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("Error simulado"))
                .when(authService).enviarSolicitudTutoria(anyLong(), anyString());

        mockMvc.perform(post("/api/auth/tutoria/solicitar")
                .param("idSolicitante", "1")
                .param("emailPaciente", "error@test.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Error simulado"));
    }

    @Test
    void responderTutoria_DebeRetornar200_CuandoSeResponde() throws Exception {
        mockMvc.perform(post("/api/auth/tutoria/responder")
                .param("idSolicitud", "1")
                .param("aceptada", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("Tutoría aceptada exitosamente."));
    }

    @Test
    void responderTutoria_DebeRetornar400_CuandoFalla() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("Error al procesar la tutoría"))
                .when(authService).responderSolicitudTutoria(anyLong(), anyBoolean());

        mockMvc.perform(post("/api/auth/tutoria/responder")
                .param("idSolicitud", "99")
                .param("aceptada", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Error al procesar la tutoría"));
    }

    // --- TESTS PARA CUBRIR LAS RAMAS RESTANTES (EL 1% FALTANTE) ---

    @Test
    void actualizarPerfil_DebeActualizarPassword_CuandoSeEnvia() throws Exception {
        // Cubre el 'if (password != null && !password.trim().isEmpty())' en update-profile
        Usuario usuario = new Usuario();
        usuario.setEmail("actual@test.com");
        when(usuarioRepository.findByEmail("actual@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("hashGenerado");

        mockMvc.perform(patch("/api/auth/update-profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentEmail\":\"actual@test.com\", \"password\":\"nuevaClaveSegura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Perfil actualizado correctamente"));
    }

    @Test
    void adminUpdateUser_DebeActualizarPasswordYRolDoctor() throws Exception {
        // Cubre el 'if (password != null)' y el 'if (u.getRol() == Rol.DOCTOR)' en update-admin
        Usuario usuario = new Usuario();
        usuario.setEmail("target@test.com");
        when(usuarioRepository.findByEmail("target@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("hashAdmin");

        mockMvc.perform(put("/api/auth/users/update-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalEmail\":\"target@test.com\", \"password\":\"clave123\", \"rol\":\"DOCTOR\", \"especialidad\":\"PEDIATRIA\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario actualizado por administrador"));
    }

    @Test
    void cambiarRolUsuario_DebeAsignarNingunaEspecialidad_SiRolNoEsDoctor() throws Exception {
        // Cubre el 'else' de especialidad en cambiarRolUsuario
        Usuario usuario = new Usuario();
        usuario.setEmail("paciente@test.com");
        when(usuarioRepository.findByEmail("paciente@test.com")).thenReturn(Optional.of(usuario));

        mockMvc.perform(put("/api/auth/users/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"paciente@test.com\", \"rol\":\"PACIENTE\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario actualizado correctamente"));
    }
}