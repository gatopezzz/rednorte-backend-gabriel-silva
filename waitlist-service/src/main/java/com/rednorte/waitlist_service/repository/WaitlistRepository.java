package com.rednorte.waitlist_service.repository;

import com.rednorte.waitlist_service.model.ListaEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitlistRepository extends JpaRepository<ListaEspera, Long> {

}
