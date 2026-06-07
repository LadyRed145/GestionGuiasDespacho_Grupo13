package com.duoc.gestionguiasdespacho.controller;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoRequest;
import com.duoc.gestionguiasdespacho.dto.GuiaDespachoResponse;
import com.duoc.gestionguiasdespacho.service.GuiaDespachoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaDespachoController {

    private final GuiaDespachoService guiaService;

    @PostMapping
    public ResponseEntity<GuiaDespachoResponse> crear(@RequestBody GuiaDespachoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guiaService.crear(request));
    }

    @GetMapping
    public ResponseEntity<List<GuiaDespachoResponse>> listar(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(guiaService.listar(transportista, fecha));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaDespachoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.buscarPorId(id));
    }

    @PostMapping("/{id}/subir")
    public ResponseEntity<GuiaDespachoResponse> generarYSubir(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.generarYSubirAS3(id));
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<ByteArrayResource> descargar(@PathVariable Long id) {
        ByteArrayResource archivo = guiaService.descargar(id);

        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(MediaType.TEXT_PLAIN))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guia-despacho.txt")
                .body(archivo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespachoResponse> actualizar(
            @PathVariable Long id,
            @RequestBody GuiaDespachoRequest request
    ) {
        return ResponseEntity.ok(guiaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        guiaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}