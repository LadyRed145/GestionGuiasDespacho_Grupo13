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
public class EstadoColasResponse {

    private String colaPrincipal;
    private Integer mensajesColaPrincipal;
    private String colaErrores;
    private Integer mensajesColaErrores;
    private String estado;
    private LocalDateTime fechaConsulta;
}
