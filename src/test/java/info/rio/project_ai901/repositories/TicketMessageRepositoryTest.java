package info.rio.project_ai901.repositories;

import info.rio.project_ai901.models.Customer;
import info.rio.project_ai901.models.Ticket;
import info.rio.project_ai901.models.TicketMessage;
import info.rio.project_ai901.models.enums.CustomerType;
import info.rio.project_ai901.models.enums.HandledBy;
import info.rio.project_ai901.models.enums.MessageAction;
import info.rio.project_ai901.models.enums.MessageAuthor;
import info.rio.project_ai901.models.enums.TicketChannel;
import info.rio.project_ai901.models.enums.TicketStatus;
import info.rio.project_ai901.support.AbstractRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMessageRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TicketMessageRepository ticketMessageRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void cuentaMensajesPorTicket() {
        Ticket ticket1 = ticket("Ticket Uno");
        Ticket ticket2 = ticket("Ticket Dos");
        ticketMessageRepository.saveAndFlush(message(ticket1, MessageAuthor.CUSTOMER,
                "Mi pedido no llego", LocalDateTime.of(2026, 1, 1, 10, 0)));
        ticketMessageRepository.saveAndFlush(message(ticket1, MessageAuthor.EMPLOYEE,
                "Ya revisamos tu caso", LocalDateTime.of(2026, 1, 1, 11, 0)));
        ticketMessageRepository.saveAndFlush(message(ticket2, MessageAuthor.CUSTOMER,
                "Necesito facturas", LocalDateTime.of(2026, 1, 2, 10, 0)));

        assertThat(ticketMessageRepository.countByTicket_Id(ticket1.getId())).isEqualTo(2);
        assertThat(ticketMessageRepository.countByTicket_Id(ticket2.getId())).isEqualTo(1);
        assertThat(ticketMessageRepository.countByTicket_Id(UUID.randomUUID())).isZero();
    }

    @Test
    void encuentraMensajesDeUnTicketVacio() {
        Ticket ticket = ticket("Ticket Sin Mensajes");

        assertThat(ticketMessageRepository.findByTicket_IdOrderBySentAtAsc(ticket.getId())).isEmpty();
    }

    @Test
    void ordenaMensajesPorFechaDeEnvioAscendente() {
        Ticket ticket = ticket("Ticket Ordenado");
        ticketMessageRepository.saveAndFlush(message(ticket, MessageAuthor.EMPLOYEE,
                "Respuesta tardia", LocalDateTime.of(2026, 1, 1, 12, 0)));
        ticketMessageRepository.saveAndFlush(message(ticket, MessageAuthor.CUSTOMER,
                "Queja inicial", LocalDateTime.of(2026, 1, 1, 10, 0)));
        ticketMessageRepository.saveAndFlush(message(ticket, MessageAuthor.AGENT,
                "Seguimiento automatico", LocalDateTime.of(2026, 1, 1, 11, 0)));

        List<TicketMessage> result = ticketMessageRepository.findByTicket_IdOrderBySentAtAsc(ticket.getId());

        assertThat(result)
                .extracting(TicketMessage::getContent)
                .containsExactly("Queja inicial", "Seguimiento automatico", "Respuesta tardia");
    }

    @Test
    void eliminaSoloLosMensajesDelTicketIndicado() {
        Ticket ticket1 = ticket("Ticket Borrable");
        Ticket ticket2 = ticket("Ticket Conservado");
        ticketMessageRepository.saveAndFlush(message(ticket1, MessageAuthor.CUSTOMER,
                "Mensaje a borrar", LocalDateTime.of(2026, 1, 1, 10, 0)));
        ticketMessageRepository.saveAndFlush(message(ticket2, MessageAuthor.CUSTOMER,
                "Mensaje que permanece", LocalDateTime.of(2026, 1, 1, 10, 0)));

        ticketMessageRepository.deleteByTicket_Id(ticket1.getId());
        ticketMessageRepository.flush();

        assertThat(ticketMessageRepository.countByTicket_Id(ticket1.getId())).isZero();
        assertThat(ticketMessageRepository.countByTicket_Id(ticket2.getId())).isEqualTo(1);
        assertThat(ticketRepository.existsById(ticket1.getId())).isTrue();
    }

    private Ticket ticket(String subject) {
        Customer customer = new Customer();
        customer.setName("Cliente " + subject);
        customer.setType(CustomerType.HOTEL);
        customer.setEmail(subject.toLowerCase().replace(' ', '.') + "@test.local");
        customerRepository.saveAndFlush(customer);

        Ticket ticket = new Ticket();
        ticket.setCustomer(customer);
        ticket.setSubject(subject);
        ticket.setChannel(TicketChannel.EMAIL);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setHandledBy(HandledBy.HUMAN);
        ticket.setRequiresHumanReview(false);
        ticket.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        return ticketRepository.saveAndFlush(ticket);
    }

    private TicketMessage message(Ticket ticket, MessageAuthor author, String content, LocalDateTime sentAt) {
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setAuthor(author);
        message.setContent(content);
        message.setAction(MessageAction.CREATE_TICKET);
        message.setSources("fuente de prueba");
        message.setSentAt(sentAt);
        return message;
    }
}
