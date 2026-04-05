package io.github.cyfko.jpametamodel.api;

import java.util.*;

/**
 * Metadata about a DTO projection mapping to an entity.
 * <p>
 * This record captures the complete metadata for a projected DTO, including:
 * <ul>
 *   <li>Direct field mappings ({@code @Projected})</li>
 *   <li>Computed fields ({@code @Computed})</li>
 *   <li>Computation providers ({@code @Provider})</li>
 *   <li>Exposed queryable criteria ({@code @ExposedAs}), including composed criteria inherited
 *       from nested {@code @Projection} types</li>
 *   <li>Resource exposure declaration ({@code @Exposure})</li>
 * </ul>
 *
 * @param entityClass     the source entity class being projected
 * @param directMappings  array of direct field mappings (DTO field → Entity field)
 * @param computedFields  array of computed fields with their dependencies
 * @param computers       array of computation providers
 * @param exposedCriteria array of queryable criteria (direct and composed), may be empty
 * @param exposure        resource exposure metadata, or {@code null} if this projection is not exposed
 * @since 1.0.0
 */
public record ProjectionMetadata(
        Class<?> entityClass,
        DirectMapping[] directMappings,
        ComputedField[] computedFields,
        ComputationProvider[] computers,
        ExposedCriterion[] exposedCriteria,
        ExposureMetadata exposure
) {

    /**
     * Convenience constructor for projections without exposure metadata.
     * <p>
     * Sets {@code exposedCriteria} to an empty array and {@code exposure} to {@code null}.
     * </p>
     */
    public ProjectionMetadata(Class<?> entityClass, DirectMapping[] directMappings,
                              ComputedField[] computedFields, ComputationProvider[] computers) {
        this(entityClass, directMappings, computedFields, computers, new ExposedCriterion[]{}, null);
    }

    public ProjectionMetadata {
        Objects.requireNonNull(entityClass, "entityClass cannot be null");
        Objects.requireNonNull(directMappings, "directMappings cannot be null");
        Objects.requireNonNull(computedFields, "computedFields cannot be null");
        Objects.requireNonNull(computers, "computers cannot be null");
        Objects.requireNonNull(exposedCriteria, "exposedCriteria cannot be null");
        // exposure may be null (no @Exposure annotation)
    }

    /**
     * Gets all entity fields required for this projection.
     * Includes both direct mappings and computed field dependencies.
     *
     * @return deduplicated list of entity field paths
     */
    public List<String> getAllRequiredEntityFields() {
        Set<String> fields = new LinkedHashSet<>();

        // Add direct mappings
        for (var dm: directMappings){
            fields.add(dm.entityField());
        }

        // Add computed dependencies
        for (var cf: computedFields){
            fields.addAll(List.of(cf.dependencies()));
        }

        return List.copyOf(fields);
    }

    /**
     * Gets the direct mapping for a DTO field, if it exists.
     *
     * @param dtoField the DTO field name
     * @param ignoreCase whether field name should be compared equals ignoring case
     * @return Optional containing the mapping, or empty if not found
     */
    public Optional<DirectMapping> getDirectMapping(String dtoField, boolean ignoreCase) {
        return Arrays.stream(directMappings)
                .filter(m -> ignoreCase ? m.dtoField().equalsIgnoreCase(dtoField) : m.dtoField().equals(dtoField))
                .findFirst();
    }

    /**
     * Gets the computed field metadata for a DTO field, if it exists.
     *
     * @param dtoField the DTO field name
     * @param ignoreCase whether field name should be compared equals ignoring case
     * @return Optional containing the computed field, or empty if not found
     */
    public Optional<ComputedField> getComputedField(String dtoField, boolean ignoreCase) {
        return Arrays.stream(computedFields)
                .filter(c -> ignoreCase ? c.dtoField().equalsIgnoreCase(dtoField) : c.dtoField().equals(dtoField))
                .findFirst();
    }

    /**
     * Checks if a DTO field is a computed field.
     *
     * @param dtoField the DTO field name
     * @param ignoreCase whether field name should be compared equals ignoring case
     * @return true if computed, false otherwise
     */
    public boolean isComputedField(String dtoField, boolean ignoreCase) {
        return Arrays.stream(computedFields)
                .anyMatch(c -> ignoreCase ? c.dtoField().equalsIgnoreCase(dtoField) : c.dtoField().equals(dtoField));
    }

    /**
     * Checks if a DTO field is a direct mapping.
     *
     * @param dtoField the DTO field name
     * @param ignoreCase whether field name should be compared equals ignoring case
     * @return true if direct mapping, false otherwise
     */
    public boolean isDirectMapping(String dtoField, boolean ignoreCase) {
        return Arrays.stream(directMappings)
                .anyMatch(m -> ignoreCase ? m.dtoField().equalsIgnoreCase(dtoField) : m.dtoField().equals(dtoField));
    }

    // ==================== Exposure Layer Methods ====================

    /**
     * Returns only the composed (inherited) criteria — those inherited from nested
     * {@code @Projection} types via composed criterion inheritance.
     *
     * @return list of composed criteria, empty if none
     */
    public List<ExposedCriterion> getComposedCriteria() {
        return Arrays.stream(exposedCriteria)
                .filter(ExposedCriterion::isComposed)
                .toList();
    }

    /**
     * Returns the direct mappings whose {@code dtoFieldType} is itself a {@code @Projection}
     * type (non-scalar), identified by having a {@code logicalPrefix} present.
     *
     * @return list of projection-type direct mappings
     */
    public List<DirectMapping> getProjectionMappings() {
        return Arrays.stream(directMappings)
                .filter(DirectMapping::isProjectionType)
                .toList();
    }

    /**
     * Finds a criterion by its symbolic ref.
     *
     * @param ref        the criterion ref (e.g. {@code "NAME"} or {@code "SOURCE_SITE__SITE_NAME"})
     * @param ignoreCase whether to match ignoring case
     * @return the matching criterion, or empty if not found
     */
    public Optional<ExposedCriterion> getCriterion(String ref, boolean ignoreCase) {
        return Arrays.stream(exposedCriteria)
                .filter(c -> ignoreCase ? c.ref().equalsIgnoreCase(ref) : c.ref().equals(ref))
                .findFirst();
    }

    /**
     * Returns whether this projection is exposed as a queryable resource
     * (i.e., has an {@code @Exposure} annotation).
     *
     * @return {@code true} if exposure metadata is present
     */
    public boolean isExposed() {
        return exposure != null;
    }
}
