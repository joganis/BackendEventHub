package com.eventHub.backend_eventHub.events.dto;

import lombok.Data;

import java.time.Instant;
import java.util.*;

@Data
public class UpdateEventDto {
    private String title;
    private String description;
    private Instant start;
    private Instant end;
    private PriceDto price;
    private Integer maxAttendees;
    private List<String> categories;

    private List<MediaDto> mainImages;
    private List<MediaDto> galleryImages;
    private List<MediaDto> videos;
    private List<MediaDto> documents;

    private OtherDataDto otherData;
}