package com.duoc.gestionguiasdespacho.service;

import com.duoc.gestionguiasdespacho.dto.GuiaDespachoRequest;
import com.duoc.gestionguiasdespacho.dto.GuiaDespachoResponse;
import com.duoc.gestionguiasdespacho.exception.RecursoNoEncontradoException;
import com.duoc.gestionguiasdespacho.model.GuiaDespacho;
import com.duoc.gestionguiasdespacho.repository.GuiaDespachoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

    private final GuiaDespachoRepository guiaRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.efs.base-path:/mnt/efs/guias}")
    private String efsBasePath;

    public GuiaDespachoResponse crear(GuiaDespachoRequest request) {
        Objects.requireNonNull(request, "La solicitud de guía no puede ser nula.");

        String numeroGuia = validarTexto(request.getNumeroGuia(), "El número de guía es obligatorio.");
        String transportista = validarTexto(request.getTransportista(), "El transportista es obligatorio.");
        String destinatario = validarTexto(request.getDestinatario(), "El destinatario es obligatorio.");
        String direccionDestino = validarTexto(request.getDireccionDestino(), "La dirección de destino es obligatoria.");

        if (guiaRepository.existsByNumeroGuia(numeroGuia)) {
            throw new IllegalArgumentException("Ya existe una guía con ese número.");
        }

        GuiaDespacho guia = GuiaDespacho.builder()
                .numeroGuia(numeroGuia)
                .transportista(transportista)
                .destinatario(destinatario)
                .direccionDestino(direccionDestino)
                .fechaGeneracion(request.getFechaGeneracion() != null ? request.getFechaGeneracion() : LocalDate.now())
                .estado(normalizarEstado(request.getEstado(), "CREADA"))
                .build();

        return toResponse(Objects.requireNonNull(guiaRepository.save(Objects.requireNonNull(guia))));
    }

    public GuiaDespachoResponse generarYSubirAS3(Long id) {
        Long guiaId = Objects.requireNonNull(id, "El ID de la guía es obligatorio.");
        GuiaDespacho guia = Objects.requireNonNull(buscarEntidad(guiaId));

        try {
            Path carpetaEfs = Path.of(efsBasePath);
            Files.createDirectories(carpetaEfs);

            String numeroGuia = validarTexto(guia.getNumeroGuia(), "La guía debe tener número.");
            String nombreArchivo = "guia-" + numeroGuia + ".txt";
            Path archivoEfs = carpetaEfs.resolve(nombreArchivo);

            Files.writeString(archivoEfs, construirContenidoGuia(guia));

            String s3Key = construirS3Key(guia, nombreArchivo);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(archivoEfs));

            guia.setNombreArchivo(nombreArchivo);
            guia.setRutaEfs(archivoEfs.toString());
            guia.setS3Key(s3Key);
            guia.setEstado("SUBIDA_S3");

            return toResponse(Objects.requireNonNull(guiaRepository.save(Objects.requireNonNull(guia))));

        } catch (IOException e) {
            throw new RuntimeException("Error al generar la guía en EFS.", e);
        }
    }

    public ByteArrayResource descargar(Long id) {
        Long guiaId = Objects.requireNonNull(id, "El ID de la guía es obligatorio.");
        GuiaDespacho guia = Objects.requireNonNull(buscarEntidad(guiaId));
        validarS3Key(guia);

        String s3Key = validarTexto(guia.getS3Key(), "La guía no tiene ruta S3 asociada.");

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> archivo = s3Client.getObjectAsBytes(request);
        byte[] contenido = Objects.requireNonNull(archivo.asByteArray());

        return new ByteArrayResource(contenido);
    }

    public GuiaDespachoResponse actualizar(Long id, GuiaDespachoRequest request) {
        Long guiaId = Objects.requireNonNull(id, "El ID de la guía es obligatorio.");
        Objects.requireNonNull(request, "La solicitud de actualización no puede ser nula.");

        GuiaDespacho guia = Objects.requireNonNull(buscarEntidad(guiaId));

        guia.setNumeroGuia(validarTexto(request.getNumeroGuia(), "El número de guía es obligatorio."));
        guia.setTransportista(validarTexto(request.getTransportista(), "El transportista es obligatorio."));
        guia.setDestinatario(validarTexto(request.getDestinatario(), "El destinatario es obligatorio."));
        guia.setDireccionDestino(validarTexto(request.getDireccionDestino(), "La dirección de destino es obligatoria."));
        guia.setFechaGeneracion(request.getFechaGeneracion() != null ? request.getFechaGeneracion() : LocalDate.now());
        guia.setEstado(normalizarEstado(request.getEstado(), "ACTUALIZADA"));

        GuiaDespacho actualizada = Objects.requireNonNull(
                guiaRepository.save(Objects.requireNonNull(guia))
        );

        if (actualizada.getS3Key() != null && !actualizada.getS3Key().isBlank()) {
            return generarYSubirAS3(Objects.requireNonNull(actualizada.getId()));
        }

        return toResponse(actualizada);
    }

    public void eliminar(Long id) {
        Long guiaId = Objects.requireNonNull(id, "El ID de la guía es obligatorio.");
        GuiaDespacho guia = Objects.requireNonNull(buscarEntidad(guiaId));

        if (guia.getS3Key() != null && !guia.getS3Key().isBlank()) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(guia.getS3Key())
                    .build();

            s3Client.deleteObject(request);
        }

        guiaRepository.delete(guia);
    }

    public List<GuiaDespachoResponse> listar(String transportista, LocalDate fecha) {
        List<GuiaDespacho> guias;

        if (transportista != null && !transportista.isBlank() && fecha != null) {
            guias = guiaRepository.findByTransportistaIgnoreCaseAndFechaGeneracion(transportista, fecha);
        } else if (transportista != null && !transportista.isBlank()) {
            guias = guiaRepository.findByTransportistaIgnoreCase(transportista);
        } else if (fecha != null) {
            guias = guiaRepository.findByFechaGeneracion(fecha);
        } else {
            guias = guiaRepository.findAll();
        }

        return guias.stream()
                .map(this::toResponse)
                .toList();
    }

    private GuiaDespacho buscarEntidad(Long id) {
    Long guiaId = Objects.requireNonNull(id, "El ID de la guía es obligatorio.");

    return guiaRepository.findById(guiaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Guía no encontrada con ID: " + guiaId));
}

    private String construirContenidoGuia(GuiaDespacho guia) {
        String numeroGuia = validarTexto(guia.getNumeroGuia(), "La guía debe tener número.");
        String transportista = validarTexto(guia.getTransportista(), "La guía debe tener transportista.");
        String destinatario = validarTexto(guia.getDestinatario(), "La guía debe tener destinatario.");
        String direccionDestino = validarTexto(guia.getDireccionDestino(), "La guía debe tener dirección de destino.");
        LocalDate fecha = Objects.requireNonNullElse(guia.getFechaGeneracion(), LocalDate.now());
        String estado = normalizarEstado(guia.getEstado(), "GENERADA");

        return """
                GUÍA DE DESPACHO
                -----------------
                Número: %s
                Transportista: %s
                Destinatario: %s
                Dirección destino: %s
                Fecha generación: %s
                Estado: %s
                """.formatted(
                numeroGuia,
                transportista,
                destinatario,
                direccionDestino,
                fecha,
                estado
        );
    }

    private String construirS3Key(GuiaDespacho guia, String nombreArchivo) {
        LocalDate fecha = Objects.requireNonNullElse(guia.getFechaGeneracion(), LocalDate.now());
        String transportista = validarTexto(
                guia.getTransportista(),
                "La guía debe tener transportista para construir la ruta S3."
        );

        return fecha
                + "/"
                + limpiarNombreCarpeta(transportista)
                + "/"
                + nombreArchivo;
    }

    private String limpiarNombreCarpeta(String texto) {
        return validarTexto(texto, "El nombre de carpeta no puede estar vacío.")
                .replaceAll("\\s+", "_");
    }

    private void validarS3Key(GuiaDespacho guia) {
        if (guia.getS3Key() == null || guia.getS3Key().isBlank()) {
            throw new RecursoNoEncontradoException("La guía aún no tiene archivo asociado en S3.");
        }
    }

    private String validarTexto(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensajeError);
        }

        return valor.trim();
    }

    private String normalizarEstado(String estado, String estadoPorDefecto) {
        if (estado == null || estado.isBlank()) {
            return estadoPorDefecto;
        }

        return estado.trim().toUpperCase();
    }

    private GuiaDespachoResponse toResponse(GuiaDespacho guia) {
        Objects.requireNonNull(guia, "La guía no puede ser nula.");

        return GuiaDespachoResponse.builder()
                .id(guia.getId())
                .numeroGuia(guia.getNumeroGuia())
                .transportista(guia.getTransportista())
                .destinatario(guia.getDestinatario())
                .direccionDestino(guia.getDireccionDestino())
                .fechaGeneracion(guia.getFechaGeneracion())
                .estado(guia.getEstado())
                .nombreArchivo(guia.getNombreArchivo())
                .rutaEfs(guia.getRutaEfs())
                .s3Key(guia.getS3Key())
                .build();
    }
}
