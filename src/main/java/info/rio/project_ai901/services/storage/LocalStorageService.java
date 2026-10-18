package info.rio.project_ai901.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${app.storage.local.base-path:./data/storage}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new StorageException("No se pudo crear el directorio de storage: " + this.basePath, e);
        }
    }

    @Override
    public String upload(String container, String folder, String filename, InputStream data, long size,
            String contentType, Map<String, String> metadata) {
        String path = join(folder, filename);
        Path target = resolve(container, path);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Error al guardar el archivo: " + target, e);
        }
        return path;
    }

    @Override
    public InputStream download(String container, String path) {
        Path target = requireExisting(container, path);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new StorageException("Error al leer el archivo: " + target, e);
        }
    }

    @Override
    public void delete(String container, String path) {
        Path target = requireExisting(container, path);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new StorageException("Error al eliminar el archivo: " + target, e);
        }
    }

    @Override
    public boolean exists(String container, String path) {
        return Files.exists(resolve(container, path));
    }

    @Override
    public String readUrl(String container, String path, Duration ttl) {
        return "/local-storage/" + sanitize(container) + "/" + sanitize(path);
    }

    private Path requireExisting(String container, String path) {
        Path target = resolve(container, path);
        if (!Files.exists(target)) {
            throw new StorageException("El archivo no existe: " + container + "/" + path);
        }
        return target;
    }

    private Path resolve(String container, String path) {
        Path target = basePath.resolve(sanitize(container)).resolve(sanitize(path)).normalize();
        if (!target.startsWith(basePath)) {
            throw new StorageException("La ruta se sale del directorio de storage: " + container + "/" + path);
        }
        return target;
    }

    private String sanitize(String value) {
        String clean = value == null ? "" : value.replace('\\', '/').trim();
        if (clean.isBlank() || clean.startsWith("/") || clean.contains("..")) {
            throw new StorageException("Ruta de storage invalida: " + value);
        }
        return clean;
    }

    private String join(String folder, String filename) {
        if (folder == null || folder.isBlank()) {
            return filename == null ? "" : filename;
        }
        if (filename == null || filename.isBlank()) {
            return folder;
        }
        return folder + "/" + filename;
    }
}
