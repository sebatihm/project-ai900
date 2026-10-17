package info.rio.project_ai901.services.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "azure")
public class AzureBlobStorageService implements StorageService {

    private final String connectionString;
    private final String containerName;
    private BlobContainerClient container;

    public AzureBlobStorageService(
            @Value("${app.storage.azure.connection-string:}") String connectionString,
            @Value("${app.storage.azure.container:uploads}") String containerName) {
        this.connectionString = connectionString;
        this.containerName = containerName;
    }

    @PostConstruct
    void init() {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException(
                    "app.storage.azure.connection-string no esta configurado (AZURE_STORAGE_CONNECTION_STRING)");
        }
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.container = serviceClient.getBlobContainerClient(containerName);
        if (!container.exists()) {
            container.create();
        }
    }

    @Override
    public String upload(String folder, String filename, InputStream data, long size, String contentType) {
        String path = join(folder, filename);
        BlobHttpHeaders headers = new BlobHttpHeaders();
        if (contentType != null && !contentType.isBlank()) {
            headers.setContentType(contentType);
        }
        container.getBlobClient(path).getBlockBlobClient()
                .uploadWithResponse(data, size, headers, null, null, null, null, null, null);
        return path;
    }

    @Override
    public InputStream download(String path) {
        return container.getBlobClient(path).openInputStream();
    }

    @Override
    public void delete(String path) {
        container.getBlobClient(path).delete();
    }

    @Override
    public boolean exists(String path) {
        return container.getBlobClient(path).exists();
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
