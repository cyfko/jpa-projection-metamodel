package io.github.cyfko.jpametamodel.api;

import java.util.Objects;

/**
 * Runtime metadata for a queryable resource declaration, corresponding to the
 * {@code @Exposure} annotation placed on a {@code @Projection} interface.
 *
 * <p>
 * This record captures the abstract concepts of resource identity, logical grouping,
 * result cardinality strategy, transformation pipeline, and custom handler. It is
 * intentionally agnostic of transport protocol, delivery mechanism, and response
 * format — each implementation translates these concepts into whatever is meaningful
 * in its context.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * // From the annotation:
 * @Exposure(
 *     value = "products",
 *     namespace = "catalog",
 *     strategy = Strategy.WINDOWED,
 *     pipes = { @Method(type = TenantFilter.class, value = "enforceCurrentTenant") },
 *     handler = @Method(type = ReportService.class, value = "generate")
 * )
 *
 * // Produces:
 * new ExposureMetadata("products", "catalog", "WINDOWED",
 *     new MethodReference[]{ new MethodReference(TenantFilter.class, "enforceCurrentTenant") },
 *     new MethodReference(ReportService.class, "generate"))
 * }</pre>
 *
 * @param value     logical resource name (e.g. {@code "users"}, {@code "products"}).
 * @param namespace logical namespace grouping related resources. May be empty.
 * @param strategy  result cardinality strategy — one of {@code "WINDOWED"}, {@code "FULL"}, or {@code "CUSTOM"}.
 * @param pipes     ordered pipeline of query transformation method references. Never null, may be empty.
 * @param handler   custom handler method reference, or {@code null} if the implementation generates a default handler.
 * @author Frank KOSSI
 * @since 3.0.0
 */
public record ExposureMetadata(String value, String namespace, String strategy, MethodReference[] pipes, MethodReference handler) {

    /**
     * Compact constructor enforcing invariants.
     */
    public ExposureMetadata {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(strategy, "strategy cannot be null");
        Objects.requireNonNull(pipes, "pipes cannot be null");
        // handler may be null (no custom handler)
        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be blank");
        }
        if (strategy.isBlank()) {
            throw new IllegalArgumentException("strategy cannot be blank");
        }
    }

    /**
     * Compatibility constructor for projections without pipes or handler.
     */
    public ExposureMetadata(String value, String namespace, String strategy) {
        this(value, namespace, strategy, new MethodReference[0], null);
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
     * Checks if this exposure defines a transformation pipeline.
     *
     * @return {@code true} if at least one pipe is declared
     */
    public boolean hasPipes() {
        return pipes.length > 0;
    }

    /**
     * Returns the number of pipes in the transformation pipeline.
     *
     * @return pipe count
     */
    public int pipeCount() {
        return pipes.length;
    }

    /**
     * Checks if this exposure declares a custom handler.
     *
     * @return {@code true} if a handler method reference is present
     */
    public boolean hasHandler() {
        return handler != null;
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
