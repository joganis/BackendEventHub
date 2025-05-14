package com.eventHub.backend_eventHub.events.entities;

import lombok.*;

/** Cualquier dato extra como organizador y notas */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OtherData {
    private String organizer;
    private String contact;
    private String notes;
}