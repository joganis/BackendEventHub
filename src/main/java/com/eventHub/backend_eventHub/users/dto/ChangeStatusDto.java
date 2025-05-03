package com.eventHub.backend_eventHub.users.dto;

import com.eventHub.backend_eventHub.domain.entities.State;
import jakarta.validation.constraints.Pattern;

public class ChangeStatusDto {
    @Pattern(regexp = "Active|Blocked")
    private State state;
}
