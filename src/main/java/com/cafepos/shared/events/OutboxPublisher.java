package com.cafepos.shared.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           SimpMessagingTemplate messagingTemplate,
                           ObjectMapper objectMapper) {
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository, "outboxEventRepository must not be null");
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null").copy().findAndRegisterModules();
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING);
        for (OutboxEvent event : pendingEvents) {
            try {
                processEvent(event);
                event.markProcessed();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event id {}", event.getId(), e);
                event.markFailed();
                outboxEventRepository.save(event);
            }
        }
    }

    private void processEvent(OutboxEvent event) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(event.getPayload());
        String tenantId = null;
        if (jsonNode.has("cafeId")) {
            tenantId = jsonNode.get("cafeId").asText();
        } else if (jsonNode.has("tenantId")) {
            tenantId = jsonNode.get("tenantId").asText();
        }

        if (tenantId != null && !tenantId.isBlank()) {
            String destination = "/topic/kitchen/" + tenantId;
            messagingTemplate.convertAndSend(destination, event.getPayload());
        } else {
            log.warn("Outbox event id {} has no cafeId/tenantId in payload, skipping WS broadcast", event.getId());
        }
    }
}
