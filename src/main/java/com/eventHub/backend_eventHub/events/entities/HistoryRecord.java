package com.eventHub.backend_eventHub.events.entities;


import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/** Registro de cambios en campos del evento */
@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class HistoryRecord {
    private String field;
    private String oldValue;
    private String newValue;
    private Instant changedAt;
}