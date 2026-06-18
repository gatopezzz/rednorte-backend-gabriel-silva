package com.rednorte.waitlist_service.service;

import com.rednorte.waitlist_service.client.AuthClient;
import com.rednorte.waitlist_service.model.UsuarioDto;
import com.rednorte.waitlist_service.model.Especialidad;
import com.rednorte.waitlist_service.model.ListaEspera;
import com.rednorte.waitlist_service.repository.WaitlistRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WaitlistService {

    @Autowired
    private WaitlistRepository repository;

    @Autowired
    private AuthClient authClient; // ¡Ya conectado!

    public List<ListaEspera> obtenerTodos() {
        return repository.findAll();
    }

    @Transactional
    public ListaEspera unirseALista(ListaEspera entrada) {
        // 1. Verificación local
        if (repository.existsByEmailAndEstado(entrada.getEmail(), "PENDIENTE")) {
            throw new IllegalStateException("Ya te encuentras en la lista de espera.");
        }

        // 2. Validación vía Feign
        try {
            // Llamada al otro microservicio
            UsuarioDto paciente = authClient.obtenerUsuarioPorEmail(entrada.getEmail());
            
            // Lógica de seguridad: evitar que admins se registren como pacientes
            if ("ADMIN".equalsIgnoreCase(paciente.getRol())) {
                throw new IllegalArgumentException("Las cuentas administrativas no pueden registrarse en la lista.");
            }
            
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("No se encontró un usuario registrado con ese correo.");
        } catch (FeignException e) {
            throw new RuntimeException("Error al conectar con el servicio de autenticación.");
        }

        entrada.setEstado("PENDIENTE");
        return repository.save(entrada);
    }

    @Transactional
    public void marcarComoAtendido(Long id) {
        ListaEspera paciente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
        
        paciente.setEstado("ATENDIDO");
        repository.save(paciente);
    }

    public List<ListaEspera> obtenerPorEspecialidad(Especialidad especialidad) {
        return repository.findByEspecialidadAndEstado(especialidad, "PENDIENTE");
    }

    @Transactional
    public void eliminarPorCorreo(String email) {
        repository.deleteByEmail(email);
    }
}