package com.debatiendo.blogapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Perfil publico del que escribe. Separado de User a proposito: User es "como entras"
 * (credenciales, roles, estado de la cuenta) y Author es "quien sos de cara al lector"
 * (nombre para mostrar, bio). Cambiar la bio no deberia tocar la tabla de credenciales.
 *
 * Relacion 1:1 UNIDIRECCIONAL hacia User. No hay Author en User porque un @OneToOne
 * en el lado NO propietario ignora el FetchType.LAZY: Hibernate necesita saber si el
 * otro extremo es null para decidir entre proxy y null, asi que siempre dispara un
 * SELECT extra. Con la direccion unica evitamos ese query fantasma en cada carga de User.
 */
@Entity
@Table(
        name = "authors",
        uniqueConstraints = @UniqueConstraint(name = "uk_authors_user", columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author extends Auditable {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Size(max = 2000)
    @Column(name = "bio", length = 2000)
    private String bio;

    /**
     * Nullable: permite que un ADMIN (AUTHOR_CREATE) de de alta autores invitados o
     * historicos que no tienen cuenta en la app. El UNIQUE garantiza que una cuenta
     * no pueda tener dos perfiles de autor.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_authors_user"))
    private User user;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    /** Mantiene sincronizados los dos lados de la relacion. */
    public void addPost(Post post) {
        this.posts.add(post);
        post.setAuthor(this);
    }

    public void removePost(Post post) {
        this.posts.remove(post);
        post.setAuthor(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Author author = (Author) o;
        return id != null && Objects.equals(id, author.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
