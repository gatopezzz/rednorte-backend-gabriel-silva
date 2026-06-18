package com.rednorte.waitlist_service.controller;

import com.rednorte.waitlist_service.service.WaitlistService;
import com.rednorte.waitlist_service.model.ListaEspera;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WaitlistController.class)
public class WaitlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WaitlistService waitlistService;

    @Test
    void testUnirseFallaCuandoAuthServiceFalla_ControllerTest() throws Exception {
        // Configuramos el mock para que lance la excepción
        when(waitlistService.unirseALista(any(ListaEspera.class)))
                .thenThrow(new IllegalArgumentException("Usuario no encontrado"));

        // Verificamos que el controlador responde con un error 400
        mockMvc.perform(post("/api/waitlist/unirse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"test@test.com\"}"))
                .andExpect(status().isBadRequest()); 
    }

    @Test
    void testUnirseALista() throws Exception {
        mockMvc.perform(post("/api/waitlist/unirse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"test@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerTodosTest() throws Exception {
        when(waitlistService.obtenerTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/waitlist/todas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void unirseTest() throws Exception {
        String jsonRequest = "{\"email\":\"test@correo.com\"}";

        ListaEspera paciente = new ListaEspera();
        paciente.setEmail("test@correo.com");
        when(waitlistService.unirseALista(any(ListaEspera.class))).thenReturn(paciente);

        mockMvc.perform(post("/api/waitlist/unirse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());
    }

    @Test
    void marcarComoAtendidoTest() throws Exception {
        mockMvc.perform(patch("/api/waitlist/1/atender"))
                .andExpect(status().isOk());
    }
}