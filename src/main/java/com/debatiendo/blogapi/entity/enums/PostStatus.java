package com.debatiendo.blogapi.entity.enums;

/**
 * Ciclo de vida de un post.
 * DRAFT     -> solo visible para su autor y para ADMIN.
 * PUBLISHED -> visible para cualquiera con permiso POST_READ.
 * ARCHIVED  -> retirado de circulacion sin borrarse (el AUTHOR no tiene POST_DELETE).
 */
public enum PostStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
