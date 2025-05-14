package com.eventHub.backend_eventHub.events.entities;

import lombok.*;

/** Precio para eventos de tipo "paid" */
@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class Price {
    private Double amount;
    private String currency;
}