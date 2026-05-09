package com.rednorte.waitlist_service.repository;

import com.rednorte.waitlist_service.model.Especialidad;
import com.rednorte.waitlist_service.model.ListaEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaitlistRepository extends JpaRepository<ListaEspera, Long> {
    List<ListaEspera> findByEspecialidad(Especialidad especialidad);

    List<ListaEspera> findByEspecialidadAndEstado(Especialidad especialidad, String estado);

    boolean existsByEmailAndEstado(String email, String estado);

    void deleteByEmail(String email);
}