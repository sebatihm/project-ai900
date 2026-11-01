package info.rio.project_ai901.services.ai;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import info.rio.project_ai901.exceptions.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SpeechTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SpeechTranscriptionService.class);

    private static final String SCOPE = "https://cognitiveservices.azure.com/.default";
    private static final String API_VERSION = "2025-10-15";
    private static final int MAX_ATTEMPTS = 3;
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "audio/wav", "audio/x-wav", "audio/wave",
            "audio/webm", "audio/ogg", "audio/opus",
            "audio/mpeg", "audio/mp3",
            "audio/flac", "audio/aac", "audio/amr",
            "audio/mp4", "audio/x-m4a");

    /** Resultado de una transcripcion. */
    public record Transcription(String text, long durationMs) {}

    private final RestClient client;
    private final TokenCredential credential;
    private final JsonMapper jsonMapper;
    private final String locale;
    private final long maxDurationMs;
    private final String apiKey;

    public SpeechTranscriptionService(@Value("${speech.endpoint}") String endpoint,
                                      @Value("${speech.locale:es-MX}") String locale,
                                      @Value ("${speech.key}") String apiKey,
                                      @Value("${speech.max-duration-seconds:60}") int maxSeconds,
                                      JsonMapper jsonMapper) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(30));

        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.client = RestClient.builder().baseUrl(base).requestFactory(factory).build();
        this.credential = new DefaultAzureCredentialBuilder().build();   // el mismo mecanismo que Foundry
        this.jsonMapper = jsonMapper;
        this.locale = locale;
        this.apiKey = apiKey;
        this.maxDurationMs = maxSeconds * 1000L;
    }

    public Transcription transcribe(MultipartFile audio) {
        byte[] bytes = validate(audio);
        String contentType = normalize(audio.getContentType());
        String response = callWithRetry(bytes, contentType);
        return parse(response);
    }

    // ---------- Validacion ----------

    private byte[] validate(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new InvalidRequestException("El audio esta vacio.");
        }
        if (audio.getSize() > MAX_BYTES) {
            throw new InvalidRequestException("El audio es demasiado grande (maximo 10 MB).");
        }
        if (!ALLOWED_TYPES.contains(normalize(audio.getContentType()))) {
            throw new InvalidRequestException(
                    "Formato de audio no permitido. Usa WAV, MP3, OGG, WebM, FLAC, AAC o AMR.");
        }
        byte[] bytes;
        try {
            bytes = audio.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("No se pudo leer el audio.");
        }
        // No basta con confiar en el tipo que declara el cliente: se revisan los primeros bytes
        if (!looksLikeAudio(bytes)) {
            throw new InvalidRequestException("El archivo no parece un audio valido.");
        }
        return bytes;
    }

    private static String normalize(String contentType) {
        return contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
    }

    private static boolean looksLikeAudio(byte[] b) {
        if (b.length < 12) {
            return false;
        }
        String head = new String(b, 0, 4, StandardCharsets.ISO_8859_1);
        if (head.equals("RIFF") && new String(b, 8, 4, StandardCharsets.ISO_8859_1).equals("WAVE")) {
            return true;                                                          // WAV
        }
        if (head.equals("OggS") || head.equals("fLaC") || head.equals("#!AM")) {
            return true;                                                          // OGG, FLAC, AMR
        }
        if ((b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45
                && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) {
            return true;                                                          // WebM / Matroska
        }
        if (head.startsWith("ID3")) {
            return true;                                                          // MP3 con etiqueta
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0) {
            return true;                                                          // MP3 / AAC (ADTS)
        }
        return new String(b, 4, 4, StandardCharsets.ISO_8859_1).equals("ftyp");   // MP4 / M4A
    }

    // ---------- Llamada a Azure ----------

    private String callWithRetry(byte[] bytes, String contentType) {
        String definition = jsonMapper.writeValueAsString(Map.of("locales", List.of(locale)));

        for (int attempt = 1; ; attempt++) {
            try {
                return call(bytes, contentType, definition);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status == 500 || status == 502
                        || status == 503 || status == 504;
                if (retryable && attempt < MAX_ATTEMPTS) {
                    pause(attempt * 1000L);
                    continue;
                }
                throw translate(e, status);
            } catch (ResourceAccessException e) {          // tiempo de espera o red
                if (attempt < MAX_ATTEMPTS) {
                    pause(attempt * 1000L);
                    continue;
                }
                log.warn("Speech no respondio: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "El servicio de voz no respondio. Intenta de nuevo.");
            }
        }
    }

    private String call(byte[] bytes, String contentType, String definition) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("audio", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "audio";
            }
        }, MediaType.parseMediaType(contentType));
        body.part("definition", definition, MediaType.APPLICATION_JSON);

        return client.post()
                .uri("/speechtotext/transcriptions:transcribe?api-version=" + API_VERSION)
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(String.class);
    }

    private String bearerToken() {
        AccessToken token;
        try {
            token = credential.getToken(new TokenRequestContext().addScopes(SCOPE)).block();
        } catch (RuntimeException e) {
            log.error("No se pudo obtener el token para Speech: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El servicio de voz no esta disponible en este momento.");
        }
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El servicio de voz no esta disponible en este momento.");
        }
        return token.getToken();
    }

    private RuntimeException translate(RestClientResponseException e, int status) {
        String detail = e.getResponseBodyAsString();
        log.warn("Speech respondio {}: {}", status, detail.length() > 300 ? detail.substring(0, 300) : detail);
        if (status == 400 || status == 415 || status == 422) {
            return new InvalidRequestException(
                    "No se pudo procesar el audio: formato o contenido no compatible.");
        }
        if (status == 401 || status == 403) {
            log.error("Speech sin autorizacion: revisa el rol 'Cognitive Services User' y el endpoint con subdominio.");
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "El servicio de voz no esta disponible en este momento.");
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------- Respuesta ----------

    private Transcription parse(String response) {
        JsonNode root = jsonMapper.readTree(response);
        long durationMs = root.path("durationMilliseconds").asLong(0);
        JsonNode phrases = root.path("combinedPhrases");
        String text = phrases.size() > 0 ? phrases.get(0).path("text").asString() : "";

        if (durationMs > maxDurationMs) {
            throw new InvalidRequestException(
                    "El audio dura mas de " + (maxDurationMs / 1000) + " segundos. Envia uno mas corto.");
        }
        if (text == null || text.isBlank()) {
            throw new InvalidRequestException(
                    "No pude entender el audio. Intenta grabarlo de nuevo, hablando con claridad.");
        }
        // No se registra el texto: puede contener datos personales
        return new Transcription(text.trim(), durationMs);
    }
}