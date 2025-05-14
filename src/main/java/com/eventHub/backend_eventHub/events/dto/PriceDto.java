package com.eventHub.backend_eventHub.events.dto;

import lombok.Data;

@Data
public class PriceDto {
    private Double amount;
    private String currency;
}
