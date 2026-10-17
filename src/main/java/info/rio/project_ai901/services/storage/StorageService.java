package info.rio.project_ai901.services.storage;

import java.io.InputStream;

public interface StorageService {

    String upload(String folder, String filename, InputStream data, long size, String contentType);

    InputStream download(String path);

    void delete(String path);

    boolean exists(String path);
}
