package com.resto.core.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Drains unpublished outbox rows and fans them out over STOMP.
 * Keeps transactional write path free of broker I/O failures.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository repository,
                           SimpMessagingTemplate messagingTemplate,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${restoos.outbox.poll-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = repository.findByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                Map<String, Object> payload = objectMapper.readValue(
                        event.getPayload(), new TypeReference<>() {});
                String topic = payload.containsKey("topic")
                        ? String.valueOf(payload.get("topic"))
                        : "/topic/store/" + event.getStoreId() + "/kitchen";
                messagingTemplate.convertAndSend(topic, payload);
                event.setPublished(true);
                repository.save(event);
            } catch (Exception ex) {
                log.warn("Outbox publish failed for {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}
