package com.resto.core.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent enqueue(UUID organizationId, UUID storeId, String aggregateType, UUID aggregateId,
                               String eventType, Map<String, Object> payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .organizationId(organizationId)
                    .storeId(storeId)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .published(false)
                    .build();
            return repository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize outbox payload", e);
        }
    }
}
