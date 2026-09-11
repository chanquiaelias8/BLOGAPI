package com.debatiendo.blogapi.common;

/**
 * Catalogo de permisos. Estas constantes son la fuente de verdad para el seeder;
 * en las anotaciones @PreAuthorize hay que repetir el literal porque SpEL dentro de
 * una anotacion no acepta referencias a constantes sin el operador T(), que empeora
 * la legibilidad. Tener el catalogo centralizado evita que se cuelen typos en el seed.
 */
public final class Permissions {

    public static final String POST_CREATE = "POST_CREATE";
    public static final String POST_READ = "POST_READ";
    public static final String POST_UPDATE = "POST_UPDATE";
    public static final String POST_DELETE = "POST_DELETE";

    public static final String AUTHOR_CREATE = "AUTHOR_CREATE";
    public static final String AUTHOR_READ = "AUTHOR_READ";
    public static final String AUTHOR_UPDATE = "AUTHOR_UPDATE";
    public static final String AUTHOR_DELETE = "AUTHOR_DELETE";

    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_READ = "USER_READ";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";

    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String PERMISSION_MANAGE = "PERMISSION_MANAGE";

    private Permissions() {
    }
}
