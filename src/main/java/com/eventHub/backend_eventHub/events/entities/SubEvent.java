// Entidad SubEvent
package com.eventHub.backend_eventHub.events.entities;

import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "subeventos")
public class SubEvent {
    @Id
    private String id;

    private String title;
    private String description;

    // Referencia al evento principal
    @DBRef
    private Event eventoPrincipal;

    private Location location;
    private Instant start;
    private Instant end;

    private String type;
    private String privacy;
    private String ticketType;
    private Price price;

    private Integer maxAttendees;
    private Integer currentAttendees = 0;

    @Field("mainImages")
    private List<Media> mainImages;

    private OtherData otherData;

    @DBRef
    private State status;

    @DBRef
    private Users creator;

    private List<HistoryRecord> history;

    private Instant createdAt;
    private Instant updatedAt;
}