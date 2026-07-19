package com.duoc.gestionguiasdespacho.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private static final String NOMBRE_SERVICIO =
            "gestionguias-productor";

    private static final String DESCRIPCION_SERVICIO =
            "Microservicio productor de guías de despacho";

    private static final int PUERTO_SERVICIO = 8081;

    /**
     * Mantiene la ruta histórica /api/health para no romper
     * API Gateway, scripts, CI/CD ni pruebas existentes.
     *
     * También incorpora /api/productor/health para identificar
     * explícitamente al microservicio productor.
     */
    @GetMapping({
            "/api/health",
            "/api/productor/health"
    })
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put(
                "status",
                "UP"
        );

        respuesta.put(
                "service",
                NOMBRE_SERVICIO
        );

        respuesta.put(
                "description",
                DESCRIPCION_SERVICIO
        );

        respuesta.put(
                "timestamp",
                OffsetDateTime.now()
        );

        respuesta.put(
                "port",
                PUERTO_SERVICIO
        );

        return ResponseEntity.ok(
                respuesta
        );
    }
}