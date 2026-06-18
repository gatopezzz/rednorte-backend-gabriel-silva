package com.rednorte.auth_service.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolicitudTutoriaTest {

    @Test
    void testGettersYSettersDeSolicitud() {
        SolicitudTutoria solicitud = new SolicitudTutoria();
        
        solicitud.setId(1L);
        solicitud.setIdSolicitante(10L);
        solicitud.setIdPacienteReceptor(20L);
        solicitud.setEstado("PENDIENTE");

        assertEquals(1L, solicitud.getId());
        assertEquals(10L, solicitud.getIdSolicitante());
        assertEquals(20L, solicitud.getIdPacienteReceptor());
        assertEquals("PENDIENTE", solicitud.getEstado());
    }
}