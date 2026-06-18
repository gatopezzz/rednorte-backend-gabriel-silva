package com.rednorte.auth_service.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    void testGettersYSettersDeUsuario() {
        // 1. Instanciamos el modelo
        Usuario usuario = new Usuario();

        // 2. Usamos los setters
        usuario.setId(1L);
        usuario.setNombre("Pedro");
        usuario.setEmail("pedro@correo.com");
        usuario.setPassword("secreta");
        usuario.setRut("11111111-1");
        usuario.setEdad(65);
        usuario.setMensajeNotificacion("Hola");
        usuario.setTutorId(2L);
        usuario.setRol(Rol.PACIENTE);
        usuario.setEspecialidad(Especialidad.NINGUNA);

        // 3. Validamos con los getters
        assertEquals(1L, usuario.getId());
        assertEquals("Pedro", usuario.getNombre());
        assertEquals("pedro@correo.com", usuario.getEmail());
        assertEquals("secreta", usuario.getPassword());
        assertEquals("11111111-1", usuario.getRut());
        assertEquals(65, usuario.getEdad());
        assertEquals("Hola", usuario.getMensajeNotificacion());
        assertEquals(2L, usuario.getTutorId());
        assertEquals(Rol.PACIENTE, usuario.getRol());
        assertEquals(Especialidad.NINGUNA, usuario.getEspecialidad());
    }
}