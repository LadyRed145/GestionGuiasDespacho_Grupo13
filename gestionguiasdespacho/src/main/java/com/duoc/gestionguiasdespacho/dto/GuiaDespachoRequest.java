package com.duoc.gestionguiasdespacho.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class GuiaDespachoRequest {
    private String numeroGuia;
    private String transportista;
    private String destinatario;
    private String direccionDestino;
    private LocalDate fechaGeneracion;
    private String estado;
}
