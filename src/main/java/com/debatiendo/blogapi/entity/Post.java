package com.debatiendo.blogapi.entity;

import com.debatiendo.blogapi.entity.enums.PostStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "posts",
        uniqueConstraints = @UniqueConstraint(name = "uk_posts_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_posts_author", columnList = "author_id"),
                @Index(name = "idx_posts_status_published", columnList = "status, published_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends Auditable {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    /** Identificador legible para URLs publicas, derivado del titulo en el service. */
    @NotBlank
    @Size(max = 220)
    @Column(name = "slug", nullable = false, length = 220)
    private String slug;

    @NotBlank
    @Lob
    @Column(name = "contenido", nullable = false)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    /** Se setea al pasar a PUBLISHED. Null mientras el post es DRAFT. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * LAZY obligatorio: el default de @ManyToOne es EAGER, y con EAGER cada listado
     * de posts dispara un SELECT de author por fila (N+1). Los listados resuelven el
     * author con JOIN FETCH explicito cuando lo necesitan.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_posts_author"))
    private Author author;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Post post = (Post) o;
        return id != null && Objects.equals(id, post.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
