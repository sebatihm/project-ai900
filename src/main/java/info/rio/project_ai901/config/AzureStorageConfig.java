package info.rio.project_ai901.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "azure")
public class AzureStorageConfig {

    @Bean
    public BlobServiceClient blobServiceClient(
            @Value("${app.storage.azure.connection-string:}") String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException(
                    "app.storage.azure.connection-string no esta configurado (AZURE_STORAGE_CONNECTION_STRING)");
        }
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}
