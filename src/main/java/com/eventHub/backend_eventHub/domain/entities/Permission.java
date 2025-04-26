package com.eventHub.backend_eventHub.domain.entities;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entidad que representa un permiso en el sistema.
 *
 * Se mapea a la colección "permissions" en MongoDB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permissions")
public class Permission {
    @Id
    private String id;

    /**
     * Nombre del permiso (por ejemplo, "CREAR_EVENTO").
     */
    private String name;

    /**
     * Descripción del permiso.
     */
    private String description;
}
