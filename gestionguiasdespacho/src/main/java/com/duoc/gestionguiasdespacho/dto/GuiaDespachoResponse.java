package com.duoc.gestionguiasdespacho.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class GuiaDespachoResponse {
    private Long id;
    private String numeroGuia;
    private String transportista;
    private String destinatario;
    private String direccionDestino;
    private LocalDate fechaGeneracion;
    private String estado;
    private String nombreArchivo;
    private String rutaEfs;
    private String s3Key;
}