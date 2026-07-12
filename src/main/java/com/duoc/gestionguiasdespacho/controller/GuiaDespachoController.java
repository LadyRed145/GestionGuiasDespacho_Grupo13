package com.duoc.gestionguiasdespacho.controller;

import com.duoc.gestionguiasdespacho.dto.ConsumoMensajeResponse;
import com.duoc.gestionguiasdespacho.dto.EstadoColasResponse;
import com.duoc.gestionguiasdespacho.dto.GuiaDespachoRequest;
import com.duoc.gestionguiasdespacho.dto.GuiaDespachoResponse;
import com.duoc.gestionguiasdespacho.dto.PublicacionGuiaResponse;
import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import com.duoc.gestionguiasdespacho.service.GuiaDespachoConsumer;
import com.duoc.gestionguiasdespacho.service.GuiaDespachoMqService;
import com.duoc.gestionguiasdespacho.service.GuiaDespachoService;
import com.duoc.gestionguiasdespacho.service.RabbitMqMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaDespachoController {

    private final GuiaDespachoService guiaService;
    private final GuiaDespachoMqService guiaMqService;
    private final GuiaDespachoConsumer guiaConsumer;
    private final RabbitMqMonitorService rabbitMonitorService;

    @PostMapping
    public ResponseEntity<PublicacionGuiaResponse> crear(
            @Valid
            @RequestBody
            GuiaDespachoRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        guiaMqService.publicar(request)
                );
    }

    @PostMapping("/cola/consumir")
    public ResponseEntity<ConsumoMensajeResponse>
    consumirMensaje() {
        Optional<GuiaDespachoMq> resultado =
                guiaConsumer.consumirUnMensaje();

        if (resultado.isEmpty()) {
            return ResponseEntity.ok(
                    ConsumoMensajeResponse.builder()
                            .mensaje(
                                    "La cola principal "
                                            + "no contiene mensajes."
                            )
                            .consumido(false)
                            .guia(null)
                            .fechaConsumo(
                                    LocalDateTime.now()
                            )
                            .build()
            );
        }

        return ResponseEntity.ok(
                ConsumoMensajeResponse.builder()
                        .mensaje(
                                "Mensaje consumido y "
                                        + "guardado en Oracle."
                        )
                        .consumido(true)
                        .guia(resultado.get())
                        .fechaConsumo(
                                LocalDateTime.now()
                        )
                        .build()
        );
    }

    @GetMapping("/cola/estado")
    public ResponseEntity<EstadoColasResponse>
    consultarEstadoColas() {
        return ResponseEntity.ok(
                rabbitMonitorService.obtenerEstado()
        );
    }

    @GetMapping("/cola/procesados")
    public ResponseEntity<List<GuiaDespachoMq>>
    listarMensajesProcesados(
            @RequestParam(required = false)
            String estado
    ) {
        return ResponseEntity.ok(
                guiaMqService.listarPorEstado(estado)
        );
    }

    @GetMapping
    public ResponseEntity<List<GuiaDespachoResponse>> listar(
            @RequestParam(required = false)
            String transportista,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fecha
    ) {
        return ResponseEntity.ok(
                guiaService.listar(
                        transportista,
                        fecha
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaDespachoResponse>
    buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                guiaService.buscarPorId(id)
        );
    }

    @PostMapping("/{id}/subir")
    public ResponseEntity<GuiaDespachoResponse>
    generarYSubir(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                guiaService.generarYSubirAS3(id)
        );
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<ByteArrayResource>
    descargar(
            @PathVariable Long id
    ) {
        ByteArrayResource archivo =
                guiaService.descargar(id);

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "text/plain; charset=UTF-8"
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename="
                                + "\"guia-despacho.txt\""
                )
                .body(archivo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespachoResponse>
    actualizar(
            @PathVariable Long id,

            @Valid
            @RequestBody
            GuiaDespachoRequest request
    ) {
        return ResponseEntity.ok(
                guiaService.actualizar(
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        guiaService.eliminar(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}