package com.duoc.gestionguiasdespacho.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class GuiaDespachoMessage implements Serializable {

    private String messageId;
    private String numeroGuia;
    private String transportista;
    private String destinatario;
    private String direccionDestino;
    private LocalDate fechaGeneracion;
    private String estado;
    private LocalDateTime fechaEnvio;
    private String mensajeError;
}
