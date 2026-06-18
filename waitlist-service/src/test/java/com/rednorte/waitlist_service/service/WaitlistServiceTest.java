package com.rednorte.waitlist_service.service;

import com.rednorte.waitlist_service.client.AuthClient;
import com.rednorte.waitlist_service.model.ListaEspera;
import com.rednorte.waitlist_service.model.UsuarioDto;
import com.rednorte.waitlist_service.repository.WaitlistRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
public class WaitlistServiceTest {

    @Mock
    private WaitlistRepository repository;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private WaitlistService waitlistService;

    @Test
    @DisplayName("Debería agregar un paciente a la lista si existe en auth-service")
    void unirseAListaExitosamenteTest() {
        ListaEspera nuevoPaciente = new ListaEspera();
        nuevoPaciente.setEmail("juan@correo.com");

        UsuarioDto usuarioMock = new UsuarioDto(1L, "juan@correo.com", "PACIENTE");

        Mockito.when(repository.existsByEmailAndEstado(anyString(), anyString())).thenReturn(false);
        Mockito.when(authClient.obtenerUsuarioPorEmail("juan@correo.com")).thenReturn(usuarioMock);

        ListaEspera pacienteGuardado = new ListaEspera();
        pacienteGuardado.setEmail("juan@correo.com");
        pacienteGuardado.setEstado("PENDIENTE");
        Mockito.when(repository.save(any(ListaEspera.class))).thenReturn(pacienteGuardado);

        ListaEspera resultado = waitlistService.unirseALista(nuevoPaciente);

        assertNotNull(resultado);
        assertEquals("PENDIENTE", resultado.getEstado());
        Mockito.verify(repository, Mockito.times(1)).save(any(ListaEspera.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción si el paciente ya está en la lista")
    void unirseAListaFallaPorDuplicadoTest() {
        ListaEspera pacienteDuplicado = new ListaEspera();
        pacienteDuplicado.setEmail("maria@correo.com");

        Mockito.when(repository.existsByEmailAndEstado("maria@correo.com", "PENDIENTE")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            waitlistService.unirseALista(pacienteDuplicado);
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si el usuario es ADMIN")
    void unirseAListaFallaSiEsAdminTest() {
        ListaEspera admin = new ListaEspera();
        admin.setEmail("admin@correo.com");

        UsuarioDto adminUser = new UsuarioDto(99L, "admin@correo.com", "ADMIN");

        Mockito.when(repository.existsByEmailAndEstado(anyString(), anyString())).thenReturn(false);
        Mockito.when(authClient.obtenerUsuarioPorEmail("admin@correo.com")).thenReturn(adminUser);

        assertThrows(IllegalArgumentException.class, () -> {
            waitlistService.unirseALista(admin);
        });
    }

    @Test
    @DisplayName("Debería lanzar excepción si el Auth Service falla")
    void unirseAListaFallaCuandoAuthServiceFallaTest() {
        ListaEspera entrada = new ListaEspera();
        entrada.setEmail("test@test.com");

        Mockito.when(repository.existsByEmailAndEstado(anyString(), anyString())).thenReturn(false);

        // Creamos un Request dummy necesario para el constructor de FeignException
        Request dummyRequest = Request.create(Request.HttpMethod.GET, "url", Collections.emptyMap(), null, null, null);

        // Corregimos la instanciación de FeignException
        Mockito.when(authClient.obtenerUsuarioPorEmail("test@test.com"))
               .thenThrow(new FeignException.NotFound("Usuario no encontrado", dummyRequest, null, Collections.emptyMap()));

        assertThrows(IllegalArgumentException.class, () -> {
            waitlistService.unirseALista(entrada);
        });
    }

    @Test
    @DisplayName("Debería cambiar el estado de un paciente a ATENDIDO")
    void marcarComoAtendidoExitosamenteTest() {
        Long idPaciente = 1L;
        ListaEspera paciente = new ListaEspera();
        paciente.setId(idPaciente);
        paciente.setEstado("PENDIENTE");

        Mockito.when(repository.findById(idPaciente)).thenReturn(Optional.of(paciente));

        waitlistService.marcarComoAtendido(idPaciente);

        assertEquals("ATENDIDO", paciente.getEstado());
        Mockito.verify(repository, Mockito.times(1)).save(paciente);
    }
}