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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuiaDespachoService {

    private static final String CONTENT_TYPE_TEXT_UTF8 = "text/plain; charset=UTF-8";

    private final GuiaDespachoRepository guiaRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.efs.base-path:/mnt/efs/guias}")
    private String efsBasePath;

    public GuiaDespachoResponse crear(GuiaDespachoRequest request) {
        validarRequest(request);

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

        return toResponse(guardarGuia(guia));
    }

    public GuiaDespachoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidad(id));
    }

    public GuiaDespachoResponse generarYSubirAS3(Long id) {
        GuiaDespacho guia = buscarEntidad(id);

        try {
            Path carpetaEfs = Path.of(efsBasePath).toAbsolutePath().normalize();
            Files.createDirectories(carpetaEfs);

            String numeroGuia = validarTexto(guia.getNumeroGuia(), "La guía debe tener número.");
            String nombreArchivo = construirNombreArchivo(numeroGuia);
            Path archivoEfs = carpetaEfs.resolve(nombreArchivo).normalize();

            String contenido = construirContenidoGuia(guia);
            byte[] contenidoUtf8 = contenido.getBytes(StandardCharsets.UTF_8);

            Files.write(
                    archivoEfs,
                    contenidoUtf8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            String s3Key = construirS3Key(guia, nombreArchivo);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(CONTENT_TYPE_TEXT_UTF8)
                    .contentEncoding("UTF-8")
                    .build();

            s3Client.putObject(
                    putRequest,
                    RequestBody.fromBytes(contenidoUtf8)
            );

            guia.setNombreArchivo(nombreArchivo);
            guia.setRutaEfs(archivoEfs.toString());
            guia.setS3Key(s3Key);
            guia.setEstado("SUBIDA_S3");

            return toResponse(guardarGuia(guia));

        } catch (IOException e) {
            throw new RuntimeException("Error al generar la guía en EFS.", e);
        }
    }

    public ByteArrayResource descargar(Long id) {
        GuiaDespacho guia = buscarEntidad(id);
        validarS3Key(guia);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(guia.getS3Key())
                .build();

        ResponseBytes<GetObjectResponse> archivo = s3Client.getObjectAsBytes(request);
        byte[] contenido = archivo.asByteArray();

        if (contenido == null || contenido.length == 0) {
            throw new RecursoNoEncontradoException("El archivo descargado desde S3 está vacío o no existe.");
        }

        return new ByteArrayResource(contenido);
    }

    public GuiaDespachoResponse actualizar(Long id, GuiaDespachoRequest request) {
        validarRequest(request);

        GuiaDespacho guia = buscarEntidad(id);

        guia.setNumeroGuia(validarTexto(request.getNumeroGuia(), "El número de guía es obligatorio."));
        guia.setTransportista(validarTexto(request.getTransportista(), "El transportista es obligatorio."));
        guia.setDestinatario(validarTexto(request.getDestinatario(), "El destinatario es obligatorio."));
        guia.setDireccionDestino(validarTexto(request.getDireccionDestino(), "La dirección de destino es obligatoria."));
        guia.setFechaGeneracion(request.getFechaGeneracion() != null ? request.getFechaGeneracion() : LocalDate.now());
        guia.setEstado(normalizarEstado(request.getEstado(), "ACTUALIZADA"));

        GuiaDespacho actualizada = guardarGuia(guia);

        if (actualizada.getS3Key() != null && !actualizada.getS3Key().isBlank()) {
            return generarYSubirAS3(actualizada.getId());
        }

        return toResponse(actualizada);
    }

    public void eliminar(Long id) {
        GuiaDespacho guia = buscarEntidad(id);

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
        if (id == null) {
            throw new IllegalArgumentException("El ID de la guía es obligatorio.");
        }

        return guiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Guía no encontrada con ID: " + id));
    }

    private GuiaDespacho guardarGuia(GuiaDespacho guia) {
        if (guia == null) {
            throw new IllegalArgumentException("La guía no puede ser nula.");
        }

        return guiaRepository.save(guia);
    }

    private String construirContenidoGuia(GuiaDespacho guia) {
        String numeroGuia = validarTexto(guia.getNumeroGuia(), "La guía debe tener número.");
        String transportista = validarTexto(guia.getTransportista(), "La guía debe tener transportista.");
        String destinatario = validarTexto(guia.getDestinatario(), "La guía debe tener destinatario.");
        String direccionDestino = validarTexto(guia.getDireccionDestino(), "La guía debe tener dirección de destino.");
        LocalDate fecha = guia.getFechaGeneracion() != null ? guia.getFechaGeneracion() : LocalDate.now();
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

    private String construirNombreArchivo(String numeroGuia) {
        String numeroLimpio = limpiarNombreArchivo(numeroGuia);
        return "guia-" + numeroLimpio + ".txt";
    }

    private String construirS3Key(GuiaDespacho guia, String nombreArchivo) {
        LocalDate fecha = guia.getFechaGeneracion() != null ? guia.getFechaGeneracion() : LocalDate.now();
        String transportista = validarTexto(
                guia.getTransportista(),
                "La guía debe tener transportista para construir la ruta S3."
        );

        return fecha + "/" + limpiarNombreCarpeta(transportista) + "/" + nombreArchivo;
    }

    private String limpiarNombreCarpeta(String texto) {
        return limpiarTextoParaRuta(validarTexto(texto, "El nombre de carpeta no puede estar vacío."));
    }

    private String limpiarNombreArchivo(String texto) {
        return limpiarTextoParaRuta(validarTexto(texto, "El nombre de archivo no puede estar vacío."));
    }

    private String limpiarTextoParaRuta(String texto) {
        String normalizado = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalizado
                .replaceAll("[^a-zA-Z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private void validarS3Key(GuiaDespacho guia) {
        if (guia.getS3Key() == null || guia.getS3Key().isBlank()) {
            throw new RecursoNoEncontradoException("La guía aún no tiene archivo asociado en S3.");
        }
    }

    private void validarRequest(GuiaDespachoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula.");
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
        if (guia == null) {
            throw new IllegalArgumentException("La guía no puede ser nula.");
        }

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