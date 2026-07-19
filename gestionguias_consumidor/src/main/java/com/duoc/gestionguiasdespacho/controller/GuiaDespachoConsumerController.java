package com.duoc.gestionguiasdespacho.controller;

import com.duoc.gestionguiasdespacho.dto.ConsumoMensajeResponse;
import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import com.duoc.gestionguiasdespacho.service.GuiaDespachoConsultaService;
import com.duoc.gestionguiasdespacho.service.GuiaDespachoConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaDespachoConsumerController {

    private final GuiaDespachoConsumer guiaConsumer;

    private final GuiaDespachoConsultaService
            guiaConsultaService;

    @PostMapping("/cola/consumir")
    public ResponseEntity<ConsumoMensajeResponse>
    consumirMensaje() {

        Optional<GuiaDespachoMq> resultado =
                guiaConsumer.consumirUnMensaje();

        if (resultado.isEmpty()) {
            ConsumoMensajeResponse respuesta =
                    ConsumoMensajeResponse
                            .builder()
                            .mensaje(
                                    "La cola principal "
                                            + "no contiene mensajes."
                            )
                            .consumido(false)
                            .guia(null)
                            .fechaConsumo(
                                    LocalDateTime.now()
                            )
                            .build();

            return ResponseEntity.ok(
                    respuesta
            );
        }

        ConsumoMensajeResponse respuesta =
                ConsumoMensajeResponse
                        .builder()
                        .mensaje(
                                "Mensaje consumido y "
                                        + "guardado en Oracle."
                        )
                        .consumido(true)
                        .guia(
                                resultado.get()
                        )
                        .fechaConsumo(
                                LocalDateTime.now()
                        )
                        .build();

        return ResponseEntity.ok(
                respuesta
        );
    }

    @GetMapping("/cola/procesados")
    public ResponseEntity<List<GuiaDespachoMq>>
    listarMensajesProcesados(
            @RequestParam(required = false)
            String estado
    ) {
        return ResponseEntity.ok(
                guiaConsultaService.listarPorEstado(
                        estado
                )
        );
    }
}