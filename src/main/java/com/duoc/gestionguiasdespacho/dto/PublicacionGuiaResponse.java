package com.duoc.gestionguiasdespacho.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionGuiaResponse {

    private String mensaje;
    private String messageId;
    private String numeroGuia;
    private String estado;
    private LocalDateTime fechaPublicacion;
}
