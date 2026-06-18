package com.rednorte.auth_service.service;

import com.rednorte.auth_service.model.Usuario;
import com.rednorte.auth_service.model.Rol;
import com.rednorte.auth_service.model.SolicitudTutoria;
import com.rednorte.auth_service.repository.UsuarioRepository;
import com.rednorte.auth_service.repository.SolicitudTutoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudTutoriaRepository solicitudTutoriaRepository;

    @Transactional
    public Usuario registrarUsuario(Usuario usuario) {
        // Verificamos si ya existe un usuario con este email
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya existe");
        }
        
        // Si no existe, procedemos a guardar
        return usuarioRepository.save(usuario);
    }

    // Método para la vista de gestión: Excluye a los administradores de la lista general
    public List<Usuario> obtenerUsuariosParaGestion() {
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        
        // Filtramos la lista para que devuelva todos MENOS los que tengan el rol de ADMIN
        return todosLosUsuarios.stream()
                .filter(usuario -> usuario.getRol() != Rol.ADMIN)
                .collect(Collectors.toList());
    }

    // --- ENVIAR SOLICITUD DE TUTORÍA ---
    @Transactional
    public void enviarSolicitudTutoria(Long idSolicitante, String emailPaciente) {
        // 1. Verificar que el tutor solicitante exista
        Usuario solicitante = usuarioRepository.findById(idSolicitante)
                .orElseThrow(() -> new RuntimeException("El usuario solicitante no existe."));

        if (solicitante.getEdad() == null || solicitante.getEdad() < 18) {
            throw new IllegalArgumentException("Debes ser mayor de edad (18+) para ser tutor.");
        }

        // 2. Buscar al paciente de la tercera edad por su email
        Usuario pacienteReceptor = usuarioRepository.findByEmail(emailPaciente)
                .orElseThrow(() -> new RuntimeException("No se encontró ningún paciente con el email ingresado."));

        if (pacienteReceptor.getEdad() == null || pacienteReceptor.getEdad() < 60) {
            throw new IllegalArgumentException("El paciente ingresado no pertenece a la tercera edad (menor de 60 años).");
        }

        // 3. Evitar solicitudes duplicadas que ya estén pendientes
        solicitudTutoriaRepository.findByIdSolicitanteAndIdPacienteReceptorAndEstado(idSolicitante, pacienteReceptor.getId(), "PENDIENTE")
                .ifPresent(s -> {
                    throw new IllegalStateException("Ya le enviaste una solicitud de tutoría a este paciente y está pendiente.");
                });

        // 4. Crear y guardar la solicitud en la BD
        SolicitudTutoria nuevaSolicitud = new SolicitudTutoria();
        nuevaSolicitud.setIdSolicitante(idSolicitante);
        nuevaSolicitud.setIdPacienteReceptor(pacienteReceptor.getId());
        solicitudTutoriaRepository.save(nuevaSolicitud);

        // 5. Dejar el mensaje de notificación listo para que React lo pinte en la vista del abuelo
        pacienteReceptor.setMensajeNotificacion("El usuario " + solicitante.getNombre() + " desea administrar tu perfil como tutor. ¿Aceptas?");
        usuarioRepository.save(pacienteReceptor);
    }

    // --- RESPONDER SOLICITUD (ACEPTAR / RECHAZAR) ---
    @Transactional
    public void responderSolicitudTutoria(Long idSolicitud, boolean aceptada) {
        SolicitudTutoria solicitud = solicitudTutoriaRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("La solicitud no existe."));

        Usuario paciente = usuarioRepository.findById(solicitud.getIdPacienteReceptor())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado."));
                
        Usuario tutor = usuarioRepository.findById(solicitud.getIdSolicitante())
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado."));

        if (aceptada) {
            // Cambio de estados en BD
            solicitud.setEstado("ACEPTADA");
            paciente.setTutorId(tutor.getId()); // ¡Vínculo oficial establecido!
            paciente.setMensajeNotificacion("Has aceptado a " + tutor.getNombre() + " como tu tutor.");
            
            // Notificamos también al tutor
            tutor.setMensajeNotificacion("¡Buenas noticias! El paciente " + paciente.getNombre() + " aceptó tu tutoría.");
        } else {
            solicitud.setEstado("RECHAZADA");
            paciente.setMensajeNotificacion(null); // Limpiamos la alerta de la pantalla del paciente
            tutor.setMensajeNotificacion("El paciente " + paciente.getNombre() + " rechazó tu solicitud de tutoría.");
        }

        // Guardamos todos los cambios de forma permanente
        solicitudTutoriaRepository.save(solicitud);
        usuarioRepository.save(paciente);
        usuarioRepository.save(tutor);
    }
}