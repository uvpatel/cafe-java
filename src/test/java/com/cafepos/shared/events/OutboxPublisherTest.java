package com.cafepos.shared.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID cafeId = UUID.randomUUID();
    private final UUID ticketId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, messagingTemplate, objectMapper);
    }

    @Test
    @DisplayName("publishPendingEvents should read pending event, send STOMP message to /topic/kitchen/{cafeId}, and mark event PROCESSED")
    void shouldPublishPendingEventSuccessfully() {
        String payload = """
                {
                  "ticketId": "%s",
                  "cafeId": "%s",
                  "orderId": "%s",
                  "orderNumber": "ORD-001"
                }
                """.formatted(ticketId, cafeId, UUID.randomUUID());

        OutboxEvent event = new OutboxEvent("KitchenTicket", ticketId.toString(), "KitchenTicketDispatchedEvent", payload);

        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));

        outboxPublisher.publishPendingEvents();

        verify(messagingTemplate).convertAndSend(eq("/topic/kitchen/" + cafeId), eq(payload));

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("publishPendingEvents should mark event FAILED if publishing throws exception")
    void shouldMarkEventFailedOnException() {
        String payload = """
                {
                  "cafeId": "%s"
                }
                """.formatted(cafeId);

        OutboxEvent event = new OutboxEvent("KitchenTicket", ticketId.toString(), "KitchenTicketDispatchedEvent", payload);

        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));
        doThrow(new RuntimeException("Messaging error"))
                .when(messagingTemplate).convertAndSend(eq("/topic/kitchen/" + cafeId), eq(payload));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("publishPendingEvents should route to both tenant and station-specific STOMP topics when stationId is present")
    void shouldPublishToStationSpecificTopicWhenStationIdPresent() {
        UUID stationId = UUID.randomUUID();
        String payload = """
                {
                  "ticketId": "%s",
                  "cafeId": "%s",
                  "stationId": "%s"
                }
                """.formatted(ticketId, cafeId, stationId);

        OutboxEvent event = new OutboxEvent("KitchenTicket", ticketId.toString(), "KitchenTicketDispatchedEvent", payload);
        when(outboxEventRepository.findByStatus(OutboxEventStatus.PENDING)).thenReturn(List.of(event));

        outboxPublisher.publishPendingEvents();

        verify(messagingTemplate).convertAndSend(eq("/topic/kitchen/" + cafeId), eq(payload));
        verify(messagingTemplate).convertAndSend(eq("/topic/kitchen/" + cafeId + "/station/" + stationId), eq(payload));
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
    }
}
