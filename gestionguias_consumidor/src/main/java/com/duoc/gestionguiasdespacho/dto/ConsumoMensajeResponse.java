package com.duoc.gestionguiasdespacho.dto;

import com.duoc.gestionguiasdespacho.model.GuiaDespachoMq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumoMensajeResponse {

    private String mensaje;
    private boolean consumido;
    private GuiaDespachoMq guia;
    private LocalDateTime fechaConsumo;
}
