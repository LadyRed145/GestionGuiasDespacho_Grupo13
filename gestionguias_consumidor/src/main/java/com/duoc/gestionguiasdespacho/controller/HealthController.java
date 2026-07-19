package com.duoc.gestionguiasdespacho.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {

    private static final String NOMBRE_SERVICIO =
            "gestionguias-consumidor";

    private static final int PUERTO_SERVICIO =
            8082;

    @GetMapping("/api/consumidor/health")
    public Map<String, Object> health() {
        return Map.of(
                "status",
                "UP",

                "service",
                NOMBRE_SERVICIO,

                "description",
                "Microservicio consumidor de guías de despacho",

                "port",
                PUERTO_SERVICIO,

                "timestamp",
                LocalDateTime.now()
        );
    }
}