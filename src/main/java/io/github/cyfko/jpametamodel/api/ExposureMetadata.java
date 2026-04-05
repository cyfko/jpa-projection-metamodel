package io.github.cyfko.jpametamodel.api;

import java.util.Objects;

/**
 * Runtime metadata for a queryable resource declaration, corresponding to the
 * {@code @Exposure} annotation placed on a {@code @Projection} interface.
 *
 * <p>
 * This record captures the abstract concepts of resource identity, logical grouping,
 * and result cardinality strategy. It is intentionally agnostic of transport protocol,
 * delivery mechanism, and response format — each implementation translates these
 * concepts into whatever is meaningful in its context.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * // From the annotation:
 * @Exposure(value = "products", namespace = "catalog", strategy = Strategy.WINDOWED)
 *
 * // Produces:
 * new ExposureMetadata("products", "catalog", "WINDOWED")
 * }</pre>
 *
 * @param value     logical resource name (e.g. {@code "users"}, {@code "products"}).
 *                  Implementations derive a concrete identifier from this value
 *                  (REST path, GraphQL query type, messaging topic, etc.)
 * @param namespace logical namespace grouping related resources (e.g. {@code "api"}, {@code "admin"}).
 *                  May be empty if no namespace is specified.
 * @param strategy  result cardinality strategy — one of {@code "WINDOWED"}, {@code "FULL"},
 *                  or {@code "CUSTOM"}. Defines the intent regarding how much of the matching
 *                  result set is returned.
 * @author Frank KOSSI
 * @since 3.0.0
 */
public record ExposureMetadata(
        String value,
        String namespace,
        String strategy
) {

    /**
     * Compact constructor enforcing invariants.
     */
    public ExposureMetadata {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(strategy, "strategy cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be blank");
        }
        if (strategy.isBlank()) {
            throw new IllegalArgumentException("strategy cannot be blank");
        }
    }

    /**
     * Checks if this exposure has a non-empty namespace.
     *
     * @return {@code true} if the namespace is specified and non-empty
     */
    public boolean hasNamespace() {
        return !namespace.isBlank();
    }

    /**
     * Checks if this exposure uses the windowed result strategy.
     *
     * @return {@code true} if the strategy is {@code "WINDOWED"}
     */
    public boolean isWindowed() {
        return "WINDOWED".equals(strategy);
    }

    /**
     * Checks if this exposure uses the full result strategy.
     *
     * @return {@code true} if the strategy is {@code "FULL"}
     */
    public boolean isFull() {
        return "FULL".equals(strategy);
    }

    /**
     * Checks if this exposure uses a custom result strategy.
     *
     * @return {@code true} if the strategy is {@code "CUSTOM"}
     */
    public boolean isCustom() {
        return "CUSTOM".equals(strategy);
    }
}
