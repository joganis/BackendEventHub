package com.eventHub.backend_eventHub.domain.entities;




import com.eventHub.backend_eventHub.domain.enums.RoleList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * Entidad que representa un rol global en el sistema.
 *
 * Se mapea a la colección "roles" en MongoDB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roles")
public class Role {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("nombre")
    private RoleList nombreRol;

    /**
     * Lista de permisos asociados a este rol.
     */
    private List<Permission> permissions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RoleList getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(RoleList nombreRol) {
        this.nombreRol = nombreRol;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
}
