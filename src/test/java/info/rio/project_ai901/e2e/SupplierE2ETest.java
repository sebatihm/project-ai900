package info.rio.project_ai901.e2e;

import info.rio.project_ai901.models.Invoice;
import info.rio.project_ai901.models.Supplier;
import info.rio.project_ai901.models.enums.InvoiceStatus;
import info.rio.project_ai901.repositories.InvoiceRepository;
import info.rio.project_ai901.repositories.SupplierRepository;
import info.rio.project_ai901.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupplierE2ETest extends AbstractIntegrationTest {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void creaProveedorYLoObtienePorId() throws Exception {
        UUID id = createSupplier("Distribuidora Delta", "tst850101ab1", "ventas@delta.test");

        mockMvc.perform(get("/supplier/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("Distribuidora Delta"))
                .andExpect(jsonPath("$.data.rfc").value("TST850101AB1"))
                .andExpect(jsonPath("$.data.email").value("ventas@delta.test"))
                .andExpect(jsonPath("$.data.invoiceAnalyzerUrl")
                        .value("http://frontend.test/invoice-analyzer/" + id));
    }

    @Test
    void listaProveedores() throws Exception {
        UUID id = createSupplier("Proveedor Listado", "TST010101AA1", "lista@test.com");

        mockMvc.perform(get("/supplier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(id.toString())));
    }

    @Test
    void actualizaProveedor() throws Exception {
        UUID id = createSupplier("Proveedor Original", "TST010101AA2", "original@test.com");

        mockMvc.perform(put("/supplier/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Renombrado\",\"rfc\":\"tst010101zz2\","
                                + "\"email\":\"nuevo@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Proveedor Renombrado"))
                .andExpect(jsonPath("$.data.rfc").value("TST010101ZZ2"))
                .andExpect(jsonPath("$.data.email").value("nuevo@test.com"));

        mockMvc.perform(get("/supplier/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Proveedor Renombrado"));
    }

    @Test
    void eliminaProveedorSinFacturas() throws Exception {
        UUID id = createSupplier("Proveedor Borrable", "TST010101AA3", "borrar@test.com");

        mockMvc.perform(delete("/supplier/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/supplier/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaRfcDuplicadoCon409() throws Exception {
        createSupplier("Proveedor Uno", "TST010101AA4", "uno@test.com");

        mockMvc.perform(post("/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Proveedor Dos\",\"rfc\":\"tst010101aa4\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso duplicado"));
    }

    @Test
    void noEliminaProveedorConFacturas() throws Exception {
        UUID id = createSupplier("Proveedor Con Factura", "TST010101AA5", "factura@test.com");
        Supplier supplier = supplierRepository.findById(id).orElseThrow();
        invoiceRepository.save(invoice(supplier));

        mockMvc.perform(delete("/supplier/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail")
                        .value("No se puede eliminar el proveedor porque tiene facturas asociadas"));

        mockMvc.perform(get("/supplier/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/supplier?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }

    private UUID createSupplier(String name, String rfc, String email) throws Exception {
        String response = postJson("/supplier",
                "{\"name\":\"" + name + "\",\"rfc\":\"" + rfc + "\",\"email\":\"" + email + "\"}");
        return UUID.fromString(dataField(response, "id"));
    }

    private Invoice invoice(Supplier supplier) {
        Invoice invoice = new Invoice();
        invoice.setSupplier(supplier);
        invoice.setOriginalFileName("factura.pdf");
        invoice.setFileHash(UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", ""));
        invoice.setContainer("facturas-validas");
        invoice.setBlobPath("2026/factura.pdf");
        invoice.setStatus(InvoiceStatus.SUSPICIOUS);
        invoice.setSuspicionScore(3);
        invoice.setCreatedAt(LocalDateTime.now());
        return invoice;
    }
}
