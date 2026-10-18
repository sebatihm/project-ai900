package info.rio.project_ai901.services.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

public interface StorageService {

    String upload(String container, String folder, String filename, InputStream data, long size, String contentType,
            Map<String, String> metadata);

    InputStream download(String container, String path);

    void delete(String container, String path);

    boolean exists(String container, String path);

    String readUrl(String container, String path, Duration ttl);
}
