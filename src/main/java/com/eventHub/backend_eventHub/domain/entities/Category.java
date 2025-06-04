// Entidad Category
package com.eventHub.backend_eventHub.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "categorias")
public class Category {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("nombre_categoria")
    private String nombreCategoria;

    private String descripcion;
    private boolean activa = true;
}