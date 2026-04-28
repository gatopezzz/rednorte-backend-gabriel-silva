package com.rednorte.waitlist_service.controller;

import com.rednorte.waitlist_service.model.ListaEspera;
import com.rednorte.waitlist_service.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/waitlist")
@CrossOrigin(origins = "*")
public class WaitlistController {

    @Autowired
    private WaitlistRepository repository;

    @GetMapping
    public List<ListaEspera> obtenerTodos() {
        return repository.findAll();
    }

    @PostMapping
    public ListaEspera unirse(@RequestBody ListaEspera entrada) {
        entrada.setEstado("Pendiente");
        return repository.save(entrada);
    }
}