package info.rio.project_ai901.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${app.storage.local.base-path:./data/storage}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de storage: " + this.basePath, e);
        }
    }

    @Override
    public String upload(String folder, String filename, InputStream data, long size, String contentType) {
        Path target = resolve(folder, filename);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Error al guardar el archivo: " + target, e);
        }
        return basePath.relativize(target).toString().replace('\\', '/');
    }

    @Override
    public InputStream download(String path) {
        Path target = resolveExisting(path);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer el archivo: " + target, e);
        }
    }

    @Override
    public void delete(String path) {
        Path target = resolveExisting(path);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("Error al eliminar el archivo: " + target, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(resolve(path));
    }

    private Path resolveExisting(String path) {
        Path target = resolve(path);
        if (!Files.exists(target)) {
            throw new IllegalStateException("El archivo no existe: " + path);
        }
        return target;
    }

    private Path resolve(String folder, String filename) {
        return resolve(join(folder, filename));
    }

    private Path resolve(String path) {
        String safe = sanitize(path);
        Path target = basePath.resolve(safe).normalize();
        if (!target.startsWith(basePath)) {
            throw new IllegalArgumentException("La ruta se sale del directorio de storage: " + path);
        }
        return target;
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

    private String sanitize(String path) {
        String safe = (path == null ? "" : path).replace('\\', '/').trim();
        if (safe.startsWith("/") || safe.contains("..")) {
            throw new IllegalArgumentException("Ruta de archivo inválida: " + path);
        }
        return safe;
    }
}
