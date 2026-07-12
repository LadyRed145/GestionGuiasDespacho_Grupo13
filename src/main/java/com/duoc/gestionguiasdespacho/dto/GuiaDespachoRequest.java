package com.duoc.gestionguiasdespacho.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuiaDespachoRequest {

    @NotBlank(
            message = "El número de guía es obligatorio."
    )
    @Size(
            max = 50,
            message = "El número de guía no puede superar "
                    + "los 50 caracteres."
    )
    private String numeroGuia;

    @NotBlank(
            message = "El transportista es obligatorio."
    )
    @Size(
            max = 120,
            message = "El transportista no puede superar "
                    + "los 120 caracteres."
    )
    private String transportista;

    @NotBlank(
            message = "El destinatario es obligatorio."
    )
    @Size(
            max = 120,
            message = "El destinatario no puede superar "
                    + "los 120 caracteres."
    )
    private String destinatario;

    @NotBlank(
            message = "La dirección de destino es obligatoria."
    )
    @Size(
            max = 200,
            message = "La dirección de destino no puede "
                    + "superar los 200 caracteres."
    )
    private String direccionDestino;

    @PastOrPresent(
            message = "La fecha de generación no puede "
                    + "ser futura."
    )
    private LocalDate fechaGeneracion;

    @Size(
            max = 30,
            message = "El estado no puede superar "
                    + "los 30 caracteres."
    )
    private String estado;
}
