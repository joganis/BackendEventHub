package com.eventHub.backend_eventHub.events.entities;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/** Imágenes, videos o documentos asociados */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Media {
    private String url;
    private String description;
    private Instant uploadedAt;
    @Field("type")   // opcional, p.ej. "pdf", "mp4", "jpg"
    private String mediaType;
}