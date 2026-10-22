package info.rio.project_ai901.e2e;

import com.jayway.jsonpath.JsonPath;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.support.AbstractIntegrationTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketE2ETest extends AbstractIntegrationTest {

    @Test
    void creaTicketConEstadoOpenYClasificacion() throws Exception {
        UUID branchId = createBranch("SUC-TCK-1");
        UUID customerId = createCustomer("Hotel Ticket", CustomerType.HOTEL, branchId);

        String response = postJson("/ticket",
                "{\"customerId\":\"" + customerId + "\",\"subject\":\"Paquete retrasado\","
                        + "\"channel\":\"EMAIL\",\"topic\":\"DELAY\",\"sentiment\":\"NEGATIVE\","
                        + "\"initialMessage\":{\"author\":\"CUSTOMER\",\"content\":\"Mi pedido no llego\"}}");

        UUID ticketId = UUID.fromString(dataField(response, "id"));
        Assertions.assertThat(response)
                .contains("\"status\":\"OPEN\"")
                .contains("\"handledBy\":\"HUMAN\"")
                .contains("\"requiresHumanReview\":false")
                .contains("\"topic\":\"DELAY\"")
                .contains("\"sentiment\":\"NEGATIVE\"")
                .contains("\"messageCount\":1")
                .contains("\"customerName\":\"Hotel Ticket\"");

        String detail = getJson("/ticket/" + ticketId);
        Assertions.assertThat(detail)
                .contains("\"status\":\"OPEN\"")
                .contains("\"customerName\":\"Hotel Ticket\"")
                .contains("\"messageCount\":1");
    }

    @Test
    void creaTicketAgenteListoParaLaIa() throws Exception {
        UUID branchId = createBranch("SUC-TCK-2");
        UUID customerId = createCustomer("Tienda IA", CustomerType.STORE, branchId);

        String response = postJson("/ticket",
                "{\"customerId\":\"" + customerId + "\",\"subject\":\"Factura con error\","
                        + "\"channel\":\"PORTAL\",\"topic\":\"INVOICE\",\"sentiment\":\"NEGATIVE\","
                        + "\"handledBy\":\"AGENT\",\"requiresHumanReview\":true,"
                        + "\"initialMessage\":{\"author\":\"AGENT\","
                        + "\"content\":\"El cliente reporta error en la factura\","
                        + "\"action\":\"CONSULT_ORDER\",\"sources\":\"politica-facturacion#12\"}}");

        UUID ticketId = UUID.fromString(dataField(response, "id"));
        Assertions.assertThat(response)
                .contains("\"handledBy\":\"AGENT\"")
                .contains("\"requiresHumanReview\":true")
                .contains("\"topic\":\"INVOICE\"")
                .contains("\"messageCount\":1");

        mockMvc.perform(get("/ticket/{id}/message", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].author").value("AGENT"))
                .andExpect(jsonPath("$.data[0].action").value("CONSULT_ORDER"))
                .andExpect(jsonPath("$.data[0].sources").value("politica-facturacion#12"));
    }

    @Test
    void devuelve404AlConsultarTicketInexistente() throws Exception {
        mockMvc.perform(get("/ticket/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail", containsString("Ticket no encontrado")));
    }

    @Test
    void devuelve404AlCrearTicketConClienteInexistente() throws Exception {
        mockMvc.perform(post("/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + UUID.randomUUID() + "\",\"subject\":\"Asunto\","
                                + "\"channel\":\"EMAIL\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Cliente no encontrado")));
    }

    @Test
    void devuelve404AlCrearTicketConOrdenInexistente() throws Exception {
        UUID branchId = createBranch("SUC-TCK-3");
        UUID customerId = createCustomer("Hotel Orden 404", CustomerType.HOTEL, branchId);

        mockMvc.perform(post("/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + customerId + "\",\"orderId\":\"" + UUID.randomUUID()
                                + "\",\"subject\":\"Asunto\",\"channel\":\"EMAIL\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Orden no encontrada")));
    }

    @Test
    void actualizaElTicketSinCambiarSuEstado() throws Exception {
        UUID branchId = createBranch("SUC-TCK-4");
        UUID customerId = createCustomer("Hotel Edicion", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Asunto original", "EMAIL");

        mockMvc.perform(put("/ticket/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Asunto actualizado\",\"channel\":\"PORTAL\","
                                + "\"topic\":\"BILLING\",\"sentiment\":\"POSITIVE\","
                                + "\"requiresHumanReview\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Asunto actualizado"))
                .andExpect(jsonPath("$.data.channel").value("PORTAL"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.requiresHumanReview").value(true));

        String detail = getJson("/ticket/" + ticketId);
        Assertions.assertThat(detail)
                .contains("\"subject\":\"Asunto actualizado\"")
                .contains("\"status\":\"OPEN\"");
    }

    @Test
    void aplicaTransicionesDeEstadoValidas() throws Exception {
        UUID branchId = createBranch("SUC-TCK-5");
        UUID customerId = createCustomer("Hotel Estados", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Flujo completo", "EMAIL");

        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    void rechazaTransicionInvalidaCon400() throws Exception {
        UUID branchId = createBranch("SUC-TCK-6");
        UUID customerId = createCustomer("Hotel Transicion", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Transicion invalida", "EMAIL");

        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud invalida"))
                .andExpect(jsonPath("$.detail")
                        .value(containsString("Transicion de estado no permitida: IN_PROGRESS -> OPEN")));
    }

    @Test
    void rechazaTransicionAlMismoEstadoCon400() throws Exception {
        UUID branchId = createBranch("SUC-TCK-7");
        UUID customerId = createCustomer("Hotel Mismo Estado", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Mismo estado", "EMAIL");

        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El ticket ya se encuentra en el estado OPEN"));
    }

    @Test
    void soloPermiteCalificarTicketsResueltos() throws Exception {
        UUID branchId = createBranch("SUC-TCK-8");
        UUID customerId = createCustomer("Hotel Calificacion", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Calificacion", "EMAIL");

        mockMvc.perform(put("/ticket/{id}/rating", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Solo se puede calificar un ticket RESUELTO")));

        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/ticket/{id}/rating", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("rating: La calificacion debe estar entre 1 y 5"));

        mockMvc.perform(put("/ticket/{id}/rating", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    void soloPermiteEliminarTicketsResueltos() throws Exception {
        UUID branchId = createBranch("SUC-TCK-9");
        UUID customerId = createCustomer("Hotel Eliminar", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Eliminable", "EMAIL");

        mockMvc.perform(delete("/ticket/{id}", ticketId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail",
                        containsString("Solo se puede eliminar un ticket RESUELTO")));

        mockMvc.perform(put("/ticket/{id}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/ticket/{id}", ticketId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ticket/{id}", ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaTicketsFiltrandoPorEstadoClienteTopicoYHandledBy() throws Exception {
        UUID branchId = createBranch("SUC-TCK-A");
        UUID customerA = createCustomer("Hotel Filtro A", CustomerType.HOTEL, branchId);
        UUID customerB = createCustomer("Tienda Filtro B", CustomerType.STORE, branchId);

        String ticketA = dataField(postJson("/ticket",
                "{\"customerId\":\"" + customerA + "\",\"subject\":\"Queja A\",\"channel\":\"EMAIL\","
                        + "\"topic\":\"DELAY\"}"), "id");
        String ticketB = dataField(postJson("/ticket",
                "{\"customerId\":\"" + customerB + "\",\"subject\":\"Queja B\",\"channel\":\"PORTAL\","
                        + "\"topic\":\"BILLING\"}"), "id");
        String ticketC = dataField(postJson("/ticket",
                "{\"customerId\":\"" + customerA + "\",\"subject\":\"Queja C\",\"channel\":\"VOICE\","
                        + "\"topic\":\"OTHER\",\"handledBy\":\"AGENT\"}"), "id");

        mockMvc.perform(put("/ticket/{id}/status", UUID.fromString(ticketB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ticket?status=OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(ticketA)))
                .andExpect(jsonPath("$.data.items[*].id", hasItem(ticketC)))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(ticketB))));

        mockMvc.perform(get("/ticket?customerId=" + customerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(ticketA)))
                .andExpect(jsonPath("$.data.items[*].id", hasItem(ticketC)))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(ticketB))));

        mockMvc.perform(get("/ticket?topic=BILLING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(ticketB)))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(ticketA))));

        mockMvc.perform(get("/ticket?handledBy=AGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(ticketC)))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(ticketA))));
    }

    @Test
    void acotaElLimiteA100() throws Exception {
        mockMvc.perform(get("/ticket?limit=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(100));
    }

    @Test
    void gestionaLosMensajesDelTicket() throws Exception {
        UUID branchId = createBranch("SUC-TCK-B");
        UUID customerId = createCustomer("Hotel Mensajes", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Conversacion", "EMAIL");

        mockMvc.perform(get("/ticket/{id}/message", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        addTicketMessage(ticketId, "CUSTOMER", "Mi pedido no llego");
        addTicketMessage(ticketId, "EMPLOYEE", "Ya revisamos tu caso");

        mockMvc.perform(get("/ticket/{id}/message", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].content", hasItem("Mi pedido no llego")))
                .andExpect(jsonPath("$.data[*].content", hasItem("Ya revisamos tu caso")))
                .andExpect(jsonPath("$.data[*].author", hasItem("CUSTOMER")))
                .andExpect(jsonPath("$.data[*].author", hasItem("EMPLOYEE")));
    }

    @Test
    void rechazaMensajeSinContenidoCon400() throws Exception {
        UUID branchId = createBranch("SUC-TCK-C");
        UUID customerId = createCustomer("Hotel Validacion", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Validado", "EMAIL");

        mockMvc.perform(post("/ticket/{id}/message", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"CUSTOMER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validacion fallida"))
                .andExpect(jsonPath("$.errors[0]").value("content: El contenido es obligatorio"));
    }

    @Test
    void devuelve409AlEliminarClienteConTickets() throws Exception {
        UUID branchId = createBranch("SUC-TCK-D");
        UUID customerId = createCustomer("Hotel Con Tickets", CustomerType.HOTEL, branchId);
        createTicket(customerId, null, "Impide eliminar", "EMAIL");

        mockMvc.perform(delete("/customer/{id}", customerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail", containsString("tickets asociados")));
    }

    @Test
    void devuelve409AlEliminarOrdenConTickets() throws Exception {
        UUID branchId = createBranch("SUC-TCK-E");
        UUID categoryId = createCategory("Categoria Ticket Orden");
        UUID productId = createProduct("SKU-TCK-ORD", categoryId, "15.00");
        UUID customerId = createCustomer("Hotel Orden Con Ticket", CustomerType.HOTEL, branchId);
        UUID orderId = createOrder(customerId, branchId, productId, 1);
        createTicket(customerId, orderId, "Ticket de la orden", "EMAIL");

        mockMvc.perform(put("/order/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/order/{id}", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Recurso en uso"))
                .andExpect(jsonPath("$.detail", containsString("tickets asociados")));
    }

    @Test
    void devuelveElIdDelTicketCreadoEnElResumen() throws Exception {
        UUID branchId = createBranch("SUC-TCK-F");
        UUID customerId = createCustomer("Hotel Listado", CustomerType.HOTEL, branchId);
        UUID ticketId = createTicket(customerId, null, "Visible en listado", "EMAIL");

        String response = getJson("/ticket/" + ticketId);
        Assertions.assertThat(JsonPath.parse(response).read("$.data.id", String.class))
                .isEqualTo(ticketId.toString());
    }
}
