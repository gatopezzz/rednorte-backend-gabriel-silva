package com.rednorte.auth_service.service;

import com.rednorte.auth_service.model.Rol;
import com.rednorte.auth_service.model.Usuario;
import com.rednorte.auth_service.model.SolicitudTutoria;
import com.rednorte.auth_service.repository.UsuarioRepository;
import com.rednorte.auth_service.repository.SolicitudTutoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SolicitudTutoriaRepository solicitudTutoriaRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void responderSolicitudTutoria_FallaSiSolicitudNoExiste() {
        when(solicitudTutoriaRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.responderSolicitudTutoria(1L, true));
    }

    @Test
    void responderSolicitudTutoria_FallaSiPacienteNoExiste() {
        // Necesitas mockear la solicitud para que pase la primera validación, pero no
        // el paciente
        when(solicitudTutoriaRepository.findById(1L)).thenReturn(Optional.of(new SolicitudTutoria()));
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.empty()); // Paciente no encontrado

        assertThrows(RuntimeException.class, () -> authService.responderSolicitudTutoria(1L, true));
    }

    @Test
    @DisplayName("Debería lanzar excepción si el tutor asociado a la solicitud no existe")
    void responderSolicitudTutoria_FallaSiTutorNoExiste() {
        // 1. Arrange
        Long idSolicitud = 1L;
        SolicitudTutoria solicitud = new SolicitudTutoria();
        solicitud.setIdPacienteReceptor(2L); // ID del paciente
        solicitud.setIdSolicitante(3L);      // ID del tutor

        Usuario paciente = new Usuario();
        paciente.setId(2L);

        // Mockeamos la solicitud y el paciente encontrados, pero el tutor NO encontrado
        when(solicitudTutoriaRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.empty()); // ¡Aquí está el fallo!

        // 2. Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.responderSolicitudTutoria(idSolicitud, true);
        });
        
        assertEquals("Tutor no encontrado.", exception.getMessage());
    }

    @Test
    @DisplayName("Debería fallar si el paciente no existe")
    void enviarSolicitudTutoria_FallaSiPacienteNoExiste() {
        Usuario solicitante = new Usuario();
        solicitante.setId(1L);
        solicitante.setEdad(25);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.findByEmail("no@existe.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.enviarSolicitudTutoria(1L, "no@existe.com"));
    }

    @Test
    @DisplayName("Debería rechazar la solicitud de tutoría correctamente")
    void responderSolicitudTutoria_RechazarExitosamente() {
        // Arrange
        Long idSolicitud = 100L;
        SolicitudTutoria solicitud = new SolicitudTutoria();
        solicitud.setIdPacienteReceptor(2L);
        solicitud.setIdSolicitante(1L);

        Usuario paciente = new Usuario();
        paciente.setId(2L);

        Usuario tutor = new Usuario();
        tutor.setId(1L);

        when(solicitudTutoriaRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(tutor));

        // Act
        authService.responderSolicitudTutoria(idSolicitud, false); // false = rechazar

        // Assert
        assertEquals("RECHAZADA", solicitud.getEstado());
        assertNull(paciente.getMensajeNotificacion()); // Debería limpiarse
        verify(solicitudTutoriaRepository).save(solicitud);
        verify(usuarioRepository).save(paciente);
        verify(usuarioRepository).save(tutor);
    }

    @Test
    @DisplayName("Debería filtrar correctamente: manejar lista vacía y lista mixta")
    void obtenerUsuariosParaGestion_CoberturaCompleta() {
        // Caso 1: Lista vacía
        when(usuarioRepository.findAll()).thenReturn(List.of());
        assertTrue(authService.obtenerUsuariosParaGestion().isEmpty());

        // Caso 2: Solo admins (debería devolver lista vacía)
        Usuario admin = new Usuario();
        admin.setRol(Rol.ADMIN);
        when(usuarioRepository.findAll()).thenReturn(List.of(admin));
        assertTrue(authService.obtenerUsuariosParaGestion().isEmpty());
    }

    @Test
    @DisplayName("Debería aceptar la solicitud y vincular al tutor")
    void responderSolicitudTutoria_AceptarExitosamente() {
        Long idSolicitud = 100L;
        SolicitudTutoria solicitud = new SolicitudTutoria();
        solicitud.setIdPacienteReceptor(2L);
        solicitud.setIdSolicitante(1L);

        Usuario paciente = new Usuario();
        paciente.setId(2L);
        Usuario tutor = new Usuario();
        tutor.setId(1L);

        when(solicitudTutoriaRepository.findById(idSolicitud)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(tutor));

        // Act - Aceptamos
        authService.responderSolicitudTutoria(idSolicitud, true);

        // Assert
        assertEquals("ACEPTADA", solicitud.getEstado());
        verify(usuarioRepository, Mockito.times(2)).save(any()); // Verifica que guardó paciente y tutor
    }

    @Test
    @DisplayName("Debería fallar si el paciente es menor de 60 años")
    void enviarSolicitudTutoria_FallaSiPacienteEsMenorDe60() {
        Usuario solicitante = new Usuario();
        solicitante.setId(1L);
        solicitante.setEdad(25);

        Usuario pacienteJoven = new Usuario();
        pacienteJoven.setEdad(59);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.findByEmail("joven@correo.com")).thenReturn(Optional.of(pacienteJoven));

        assertThrows(IllegalArgumentException.class, () -> authService.enviarSolicitudTutoria(1L, "joven@correo.com"));
    }

    @Test
    void registrarUsuario_DebeLanzarExcepcion_CuandoEmailYaExiste() {
        // 1. Arrange: Preparar datos
        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setEmail("test@correo.com");

        // Simulamos que el repositorio encuentra a alguien
        when(usuarioRepository.findByEmail("test@correo.com"))
                .thenReturn(Optional.of(usuarioExistente));

        // 2. Act & Assert: Ejecutar y verificar
        // Verificamos que se lance la excepción correcta
        assertThrows(RuntimeException.class, () -> {
            authService.registrarUsuario(usuarioExistente);
        });

        // Verificamos que NUNCA se intentó guardar nada (opcional pero recomendado)
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería retornar usuarios excluyendo las cuentas de administración")
    void obtenerUsuariosParaGestionExcluyeAdminsTest() {
        // ARRANGE
        Usuario paciente = new Usuario();
        paciente.setNombre("Juan Pérez");
        paciente.setRol(Rol.PACIENTE);

        Usuario admin = new Usuario();
        admin.setNombre("Super Admin");
        admin.setRol(Rol.ADMIN);

        Usuario doctor = new Usuario();
        doctor.setNombre("Dra. Gómez");
        doctor.setRol(Rol.DOCTOR);

        List<Usuario> usuariosSimulados = Arrays.asList(paciente, admin, doctor);
        Mockito.when(usuarioRepository.findAll()).thenReturn(usuariosSimulados);

        // ACT
        List<Usuario> resultado = authService.obtenerUsuariosParaGestion();

        // ASSERT
        assertNotNull(resultado);
        assertEquals(2, resultado.size());

        boolean contieneAdmin = resultado.stream().anyMatch(u -> u.getRol() == Rol.ADMIN);
        assertFalse(contieneAdmin, "La lista general no debe incluir administradores");

        Mockito.verify(usuarioRepository, Mockito.times(1)).findAll();
    }

    @Test
    @DisplayName("Debería registrar un nuevo usuario exitosamente")
    void registrarUsuarioExitosamenteTest() {
        // ARRANGE
        Usuario nuevoDoctor = new Usuario();
        nuevoDoctor.setNombre("Dr. House");
        nuevoDoctor.setRol(Rol.DOCTOR);

        Mockito.when(usuarioRepository.save(any(Usuario.class))).thenReturn(nuevoDoctor);

        // ACT
        Usuario resultado = authService.registrarUsuario(nuevoDoctor);

        // ASSERT
        assertNotNull(resultado);
        assertEquals("Dr. House", resultado.getNombre());
        assertEquals(Rol.DOCTOR, resultado.getRol());

        Mockito.verify(usuarioRepository, Mockito.times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción si el solicitante de tutoría es menor de edad")
    void enviarSolicitudTutoria_FallaPorMenorDeEdadTest() {
        // ARRANGE
        Long idSolicitante = 1L;
        String emailPaciente = "abuelo@correo.com";

        Usuario solicitanteMenor = new Usuario();
        solicitanteMenor.setId(idSolicitante);
        solicitanteMenor.setEdad(17); // Menor de 18 años

        Mockito.when(usuarioRepository.findById(idSolicitante)).thenReturn(Optional.of(solicitanteMenor));

        // ACT & ASSERT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.enviarSolicitudTutoria(idSolicitante, emailPaciente);
        });

        assertEquals("Debes ser mayor de edad (18+) para ser tutor.", exception.getMessage());

        // Verificamos que NUNCA se guardó la solicitud porque falló antes
        Mockito.verify(solicitudTutoriaRepository, Mockito.never()).save(any(SolicitudTutoria.class));
    }

    @Test
    @DisplayName("Debería enviar solicitud de tutoría exitosamente")
    void enviarSolicitudTutoria_ExitoTest() {
        // ARRANGE
        Long idSolicitante = 1L;
        String emailPaciente = "abuelo@correo.com";

        Usuario solicitante = new Usuario();
        solicitante.setId(idSolicitante);
        solicitante.setNombre("Carlos");
        solicitante.setEdad(25); // Mayor de edad

        Usuario pacienteReceptor = new Usuario();
        pacienteReceptor.setId(2L);
        pacienteReceptor.setEmail(emailPaciente);
        pacienteReceptor.setEdad(65); // Tercera edad

        Mockito.when(usuarioRepository.findById(idSolicitante)).thenReturn(Optional.of(solicitante));
        Mockito.when(usuarioRepository.findByEmail(emailPaciente)).thenReturn(Optional.of(pacienteReceptor));

        // Simulamos que NO hay solicitudes pendientes previas
        Mockito.when(solicitudTutoriaRepository.findByIdSolicitanteAndIdPacienteReceptorAndEstado(idSolicitante, 2L,
                "PENDIENTE"))
                .thenReturn(Optional.empty());

        // ACT
        authService.enviarSolicitudTutoria(idSolicitante, emailPaciente);

        // ASSERT
        // 1. Verificamos que se guardó la nueva solicitud
        Mockito.verify(solicitudTutoriaRepository, Mockito.times(1)).save(any(SolicitudTutoria.class));

        // 2. Verificamos que se actualizó el mensaje del paciente
        assertEquals("El usuario Carlos desea administrar tu perfil como tutor. ¿Aceptas?",
                pacienteReceptor.getMensajeNotificacion());
        Mockito.verify(usuarioRepository, Mockito.times(1)).save(pacienteReceptor);
    }

    @Test
    @DisplayName("Debería lanzar excepción si ya existe una solicitud pendiente (Duplicada)")
    void enviarSolicitudTutoria_FallaSiYaExistePendiente() {
        Long idSolicitante = 1L;
        String emailPaciente = "abuelo@correo.com";

        Usuario solicitante = new Usuario();
        solicitante.setId(idSolicitante);
        solicitante.setEdad(25);

        Usuario pacienteReceptor = new Usuario();
        pacienteReceptor.setId(2L);
        pacienteReceptor.setEdad(65);

        when(usuarioRepository.findById(idSolicitante)).thenReturn(Optional.of(solicitante));
        when(usuarioRepository.findByEmail(emailPaciente)).thenReturn(Optional.of(pacienteReceptor));

        // Simulamos que YA existe una solicitud pendiente en la base de datos
        when(solicitudTutoriaRepository.findByIdSolicitanteAndIdPacienteReceptorAndEstado(idSolicitante, 2L, "PENDIENTE"))
                .thenReturn(Optional.of(new SolicitudTutoria()));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            authService.enviarSolicitudTutoria(idSolicitante, emailPaciente);
        });

        assertEquals("Ya le enviaste una solicitud de tutoría a este paciente y está pendiente.", exception.getMessage());
    }

    @Test
    @DisplayName("Debería lanzar excepción si el tutor no existe al crear solicitud")
    void enviarSolicitudTutoria_FallaSiTutorNoExiste() {
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.enviarSolicitudTutoria(99L, "paciente@correo.com");
        });

        assertEquals("El usuario solicitante no existe.", exception.getMessage());
    }

    @Test
    @DisplayName("Debería fallar si la edad del solicitante es nula")
    void enviarSolicitudTutoria_FallaSiEdadEsNull() {
        Usuario solicitanteSinEdad = new Usuario();
        solicitanteSinEdad.setId(1L);
        solicitanteSinEdad.setEdad(null); // Forzamos el null que revisa tu validación

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitanteSinEdad));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.enviarSolicitudTutoria(1L, "paciente@correo.com");
        });
        
        assertEquals("Debes ser mayor de edad (18+) para ser tutor.", exception.getMessage());
    }
}