package com.rednorte.auth_service.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    void testRolEnum() {
        // Verifica que los valores existan y se puedan obtener por String
        Rol admin = Rol.valueOf("ADMIN");
        Rol doctor = Rol.valueOf("DOCTOR");
        Rol paciente = Rol.valueOf("PACIENTE");
        
        assertNotNull(admin);
        assertNotNull(doctor);
        assertNotNull(paciente);
        assertTrue(Rol.values().length >= 3);
    }

    @Test
    void testEspecialidadEnum() {
        // Verifica la especialidad por defecto y que existan valores
        Especialidad ninguna = Especialidad.valueOf("NINGUNA");
        
        assertNotNull(ninguna);
        assertTrue(Especialidad.values().length > 0);
    }
}