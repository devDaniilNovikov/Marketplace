package dn.marketplace.core.exception;

/**
 * Запрошенный ресурс не найден или недоступен вызывающему.
 * Намеренно не различает "нет строки" и "нет прав видеть строку" — иначе
 * эндпоинт превращается в оракул существования чужих сущностей.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s %s не найден".formatted(resource, id));
    }
}
