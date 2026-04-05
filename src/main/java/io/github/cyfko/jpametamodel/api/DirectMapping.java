package io.github.cyfko.jpametamodel.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a direct mapping between a Data Transfer Object (DTO) field and its corresponding entity field.
 * <p>
 * This mapping defines how a field exposed in a DTO projection correlates with a field path in the underlying entity model,
 * including nested paths (e.g., "address.city") to support complex entity structures.
 * </p>
 * <p>
 * The {@code dtoFieldType} specifies the {@link Class} type of the DTO field, which is used especially
 * for type-safe query construction and further projection metadata resolution.
 * </p>
 * <p>
 * If the DTO field represents a collection, additional metadata about the collection kind and type may be provided.
 * </p>
 * <p>
 * When the DTO field returns a type that is itself annotated with {@code @Projection}, the {@code logicalPrefix}
 * captures the value of {@code @Projected(as)} (or a name derived from the method name). This prefix is used
 * for composed criterion inheritance, where queryable properties of the nested projection are inherited under
 * this prefix joined by the {@code __} separator.
 * </p>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * {@code
 * // Simple scalar mapping:
 * DirectMapping mapping = new DirectMapping(
 *     "name",
 *     "username",
 *     java.lang.String.class,
 *     Optional.empty()
 * );
 *
 * // Nested entity field mapping:
 * DirectMapping nestedMapping = new DirectMapping(
 *     "city",
 *     "address.cityName",
 *     java.lang.String.class,
 *     Optional.empty()
 * );
 *
 * // Composed projection mapping (returns another @Projection type):
 * DirectMapping composedMapping = new DirectMapping(
 *     "sourceSite",
 *     "sourceSite",
 *     SiteDTO.class,
 *     Optional.empty(),
 *     Optional.of("SOURCE_SITE"),
 *     false
 * );
 * }
 * </pre>
 *
 * @param dtoField       the name of the field in the DTO projection
 * @param entityField    the path to the corresponding field in the entity, supporting nested paths via dot notation
 * @param dtoFieldType   the {@link Class} type of the DTO field (or collection element type if the field is a collection)
 * @param collection     optional metadata describing collection properties of the DTO field, if applicable
 * @param logicalPrefix  optional logical prefix for composed criterion inheritance, from {@code @Projected(as)}.
 *                       Present only when {@code dtoFieldType} is itself a {@code @Projection} type.
 *                       Must not contain {@code __}, nor start or end with {@code _}.
 * @param cycleBreak     if {@code true}, this mapping is excluded from composed criterion inheritance
 *                       to break bidirectional cycles. Corresponds to {@code @Projected(cycleBreak = true)}.
 * @author Frank KOSSI
 * @since 1.0.0
 */
public record DirectMapping(String dtoField, String entityField, Class<?> dtoFieldType,
                            Optional<CollectionMetadata> collection,
                            Optional<String> logicalPrefix,
                            boolean cycleBreak) {

    /**
     * Convenience constructor for scalar mappings without composition metadata.
     * <p>
     * Sets {@code logicalPrefix} to {@link Optional#empty()} and {@code cycleBreak} to {@code false}.
     * </p>
     *
     * @param dtoField     the name of the field in the DTO projection
     * @param entityField  the path to the corresponding field in the entity
     * @param dtoFieldType the {@link Class} type of the DTO field
     * @param collection   optional collection metadata
     */
    public DirectMapping(String dtoField, String entityField, Class<?> dtoFieldType, Optional<CollectionMetadata> collection) {
        this(dtoField, entityField, dtoFieldType, collection, Optional.empty(), false);
    }

    public DirectMapping {
        Objects.requireNonNull(dtoField, "dtoField cannot be null");
        Objects.requireNonNull(entityField, "entityField cannot be null");
        Objects.requireNonNull(dtoFieldType, "dtoFieldType cannot be null");
        Objects.requireNonNull(collection, "collection cannot be null");
        Objects.requireNonNull(logicalPrefix, "logicalPrefix cannot be null (use Optional.empty())");

        if (dtoField.isBlank()) {
            throw new IllegalArgumentException("dtoField cannot be blank");
        }
        if (entityField.isBlank()) {
            throw new IllegalArgumentException("entityField cannot be blank");
        }

        logicalPrefix.ifPresent(prefix -> {
            if (prefix.isBlank()) {
                throw new IllegalArgumentException("logicalPrefix cannot be blank when present");
            }
            if (prefix.contains("__")) {
                throw new IllegalArgumentException(
                        "logicalPrefix \"" + prefix + "\" must not contain \"__\" — reserved as composition level separator");
            }
            if (prefix.startsWith("_")) {
                throw new IllegalArgumentException(
                        "logicalPrefix \"" + prefix + "\" must not start with \"_\"");
            }
            if (prefix.endsWith("_")) {
                throw new IllegalArgumentException(
                        "logicalPrefix \"" + prefix + "\" must not end with \"_\"");
            }
        });
    }

    /**
     * Indicates whether this mapping targets a composed projection type
     * (i.e., the DTO field returns another {@code @Projection} type).
     *
     * @return {@code true} if a logical prefix is present, indicating a composed projection
     */
    public boolean isProjectionType() {
        return logicalPrefix.isPresent();
    }

    /**
     * Indicates whether the entity field path describes a nested property.
     *
     * @return {@code true} if the {@code entityField} contains one or more dot separators representing nesting,
     *         {@code false} if it is a simple field without nesting
     */
    public boolean isNested() {
        return entityField.contains(".");
    }

    /**
     * Returns the depth of nesting in the entity field path.
     * <p>
     * For example, an {@code entityField} of {@code "address.city"} has a nesting depth of 1,
     * while {@code "order.customer.address.city"} has a nesting depth of 3.
     * </p>
     *
     * @return the number of nesting levels; zero indicates a non-nested field
     */
    public int nestingDepth() {
        return entityField.split("\\.").length - 1;
    }

    /**
     * Returns the root (first segment) of the entity field path.
     * <p>
     * For instance, the root of {@code "address.city"} is {@code "address"}.
     * </p>
     *
     * @return the root entity field name
     */
    public String getRootField() {
        return entityField.split("\\.")[0];
    }

    /**
     * Encapsulates metadata related to collection DTO fields.
     * <p>
     * This metadata specifies characteristics of the collection such as its kind (e.g., SET, LIST)
     * and the underlying collection type implementation (e.g., PERSISTENT, TRANSIENT).
     * </p>
     * <p>
     * This nested record is designed to be extendable with further collection-specific attributes if needed.
     * </p>
     *
     * @param kind           the kind of collection (an enum defining collection semantics)
     * @param collectionType the specific collection type implementation
     * @author Frank KOSSI
     * @since 4.0.0
     */
    public record CollectionMetadata(
            CollectionKind kind,
            CollectionType collectionType
    ) {
        /**
         * Factory method to create a new instance of {@link CollectionMetadata} with specified kind and collection type.
         *
         * @param kind           the kind of collection
         * @param collectionType the type of the collection implementation
         * @return a new {@link CollectionMetadata} instance
         */
        public static CollectionMetadata of(CollectionKind kind, CollectionType collectionType) {
            return new CollectionMetadata(kind, collectionType);
        }

        /**
         * Returns a string representing the Java code to instantiate this {@link CollectionMetadata}.
         * <p>
         * Useful for code generation or logging purposes.
         * </p>
         *
         * @return a String containing the Java constructor invocation for this metadata instance
         */
        public String asInstance(){
            return "new DirectMapping.CollectionMetadata(CollectionKind." + kind.name() + ", CollectionType." + collectionType.name() + ")";
        }
    }
}
