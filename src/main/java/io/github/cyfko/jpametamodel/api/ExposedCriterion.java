package io.github.cyfko.jpametamodel.api;

import java.util.Arrays;
import java.util.Objects;

/**
 * Runtime metadata for a queryable criterion declared via {@code @ExposedAs},
 * either directly on a scalar field or inherited through composed criterion inheritance
 * from a nested {@code @Projection} type.
 *
 * <p><b>Direct criterion example:</b></p>
 * <pre>{@code
 * // Declared on a scalar field:
 * @ExposedAs(value = "NAME", operators = {StandardOp.EQ, StandardOp.MATCHES})
 * String getName();
 * // → ExposedCriterion("NAME", "name", {"EQ", "MATCHES"}, true, false)
 * }</pre>
 *
 * <p><b>Composed criterion example:</b></p>
 * <pre>{@code
 * // Inherited from a nested @Projection via @Projected(as = "SOURCE_SITE"):
 * // SiteDTO exposes @ExposedAs(value = "SITE_NAME", operators = {StandardOp.EQ})
 * // → ExposedCriterion("SOURCE_SITE__SITE_NAME", "sourceSite.name", {"EQ"}, true, true)
 * }</pre>
 *
 * @param ref        symbolic criterion name used in queries (e.g. {@code "NAME"} or {@code "SOURCE_SITE__SITE_NAME"})
 * @param sourcePath the entity field path this criterion resolves to (e.g. {@code "name"} or {@code "sourceSite.name"})
 * @param operators  supported operator identifiers (e.g. {@code {"EQ", "MATCHES"}})
 * @param exposed    if {@code false}, the criterion is internal-only and not visible to external consumers
 * @param composed   {@code true} if this criterion was inherited from a nested {@code @Projection}
 *                   via composed criterion inheritance; {@code false} if declared directly
 * @author Frank KOSSI
 * @since 3.0.0
 */
public record ExposedCriterion(
        String ref,
        String sourcePath,
        String[] operators,
        boolean exposed,
        boolean composed
) {

    /**
     * Compact constructor enforcing invariants.
     */
    public ExposedCriterion {
        Objects.requireNonNull(ref, "ref cannot be null");
        Objects.requireNonNull(sourcePath, "sourcePath cannot be null");
        Objects.requireNonNull(operators, "operators cannot be null");

        if (ref.isBlank()) {
            throw new IllegalArgumentException("ref cannot be blank");
        }
        if (sourcePath.isBlank()) {
            throw new IllegalArgumentException("sourcePath cannot be blank");
        }
    }

    /**
     * Returns whether this criterion was inherited via composed criterion inheritance.
     * <p>Alias for {@link #composed()}.</p>
     *
     * @return {@code true} if composed, {@code false} if declared directly
     */
    public boolean isComposed() {
        return composed;
    }

    /**
     * Returns the composition depth of this criterion.
     * <p>
     * A depth of 0 means the criterion is declared directly (no {@code __} in the ref).
     * Each {@code __} separator adds one level of depth.
     * </p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "NAME"} → depth 0</li>
     *   <li>{@code "SOURCE_SITE__SITE_NAME"} → depth 1</li>
     *   <li>{@code "SOURCE_SITE__LOCALITY__NAME"} → depth 2</li>
     * </ul>
     *
     * @return the number of composition levels (count of {@code __} separators)
     */
    public int compositionDepth() {
        int depth = 0;
        int idx = 0;
        while ((idx = ref.indexOf("__", idx)) != -1) {
            depth++;
            idx += 2;
        }
        return depth;
    }

    /**
     * Splits the criterion ref into its composition segments.
     * <p>
     * Each segment corresponds to one level in the composition hierarchy.
     * </p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "NAME"} → {@code ["NAME"]}</li>
     *   <li>{@code "SOURCE_SITE__SITE_NAME"} → {@code ["SOURCE_SITE", "SITE_NAME"]}</li>
     *   <li>{@code "SOURCE_SITE__LOCALITY__NAME"} → {@code ["SOURCE_SITE", "LOCALITY", "NAME"]}</li>
     * </ul>
     *
     * @return array of ref segments split on {@code __}
     */
    public String[] refSegments() {
        return ref.split("__");
    }

    /**
     * Checks if a specific operator is supported by this criterion.
     *
     * @param operator the operator identifier to check
     * @return {@code true} if the operator is in the supported list
     */
    public boolean supportsOperator(String operator) {
        return Arrays.asList(operators).contains(operator);
    }

    /**
     * Returns the number of supported operators.
     *
     * @return operator count
     */
    public int operatorCount() {
        return operators.length;
    }
}
