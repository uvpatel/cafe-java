package com.cafepos.activitylog.application;

import com.cafepos.activitylog.domain.ActivityLog;
import com.cafepos.activitylog.domain.ActivityLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ActivityLogApplicationService {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "pass", "pwd", "pin", "cashierpin", "cvv", "cvc",
            "cardnumber", "card_number", "creditcard", "cc_number",
            "secret", "token", "accesstoken", "refreshtoken", "auth", "authorization"
    );

    private static final Pattern FALLBACK_SENSITIVE_PATTERN = Pattern.compile(
            "(?i)\"(password|pin|cvv|cvc|cardNumber|card_number|secret|token|auth|authorization|creditCard)\"\\s*:\\s*\"[^\"]*\""
    );

    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    public ActivityLogApplicationService(ActivityLogRepository activityLogRepository, ObjectMapper objectMapper) {
        this.activityLogRepository = Objects.requireNonNull(activityLogRepository, "activityLogRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Transactional
    public ActivityLog recordActivity(UUID cafeId,
                                      UUID actorEmployeeId,
                                      String actorEmail,
                                      String action,
                                      String entityType,
                                      String entityId,
                                      String detailsJson,
                                      String ipAddress) {
        String sanitizedDetails = sanitizeDetails(detailsJson);
        ActivityLog log = new ActivityLog(
                cafeId,
                actorEmployeeId,
                actorEmail,
                action,
                entityType,
                entityId,
                sanitizedDetails,
                ipAddress
        );
        return activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> searchActivityLogs(UUID cafeId, Pageable pageable) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        return activityLogRepository.findByCafeIdOrderByCreatedAtDesc(cafeId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLog> searchActivityLogs(UUID cafeId, String action, String entityType, Pageable pageable) {
        Objects.requireNonNull(cafeId, "cafeId must not be null");
        if (action != null && !action.isBlank()) {
            return activityLogRepository.findByCafeIdAndAction(cafeId, action.trim(), pageable);
        }
        if (entityType != null && !entityType.isBlank()) {
            return activityLogRepository.findByCafeIdAndEntityType(cafeId, entityType.trim(), pageable);
        }
        return searchActivityLogs(cafeId, pageable);
    }

    public String sanitizeDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return detailsJson;
        }

        try {
            JsonNode tree = objectMapper.readTree(detailsJson);
            sanitizeNode(tree);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            // Fallback regex sanitization if not standard JSON tree
            return FALLBACK_SENSITIVE_PATTERN.matcher(detailsJson)
                    .replaceAll("\"$1\": \"[REDACTED]\"");
        }
    }

    private void sanitizeNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String normalizedKey = key.toLowerCase().replaceAll("[_-]", "");

                if (SENSITIVE_KEYS.contains(normalizedKey)) {
                    objectNode.put(key, "[REDACTED]");
                } else {
                    sanitizeNode(entry.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                sanitizeNode(element);
            }
        }
    }
}
