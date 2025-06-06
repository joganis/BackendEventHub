package com.eventHub.backend_eventHub.domain.entities;


import com.eventHub.backend_eventHub.domain.enums.StateList;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
@Document(collection = "estados")
public class State {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("nombre")
    private StateList nameState;
}
