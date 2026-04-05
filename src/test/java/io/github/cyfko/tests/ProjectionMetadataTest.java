package io.github.cyfko.tests;

import io.github.cyfko.jpametamodel.api.ComputationProvider;
import io.github.cyfko.jpametamodel.api.ComputedField;
import io.github.cyfko.jpametamodel.api.DirectMapping;
import io.github.cyfko.jpametamodel.api.ExposedCriterion;
import io.github.cyfko.jpametamodel.api.ExposureMetadata;
import io.github.cyfko.jpametamodel.api.ProjectionMetadata;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionMetadataTest {

    @Test
    void testConstructorValidation() {
        assertThrows(NullPointerException.class, () ->
            new ProjectionMetadata(null, new DirectMapping[]{}, new ComputedField[]{}, new ComputationProvider[]{})
        );
    }

    @Test
    void testGetAllRequiredEntityFields() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{
                    new DirectMapping("email", "email", String.class, Optional.empty()),
                    new DirectMapping("city", "address.city", String.class, Optional.empty())
            },
            new ComputedField[]{
                    new ComputedField("fullName", new String[]{"firstName", "lastName"}),
                    new ComputedField("age", new String[]{"birthDate"})
            },
            new ComputationProvider[]{}
        );

        List<String> required = metadata.getAllRequiredEntityFields();

        assertEquals(5, required.size());
        assertTrue(required.contains("email"));
        assertTrue(required.contains("address.city"));
        assertTrue(required.contains("firstName"));
        assertTrue(required.contains("lastName"));
        assertTrue(required.contains("birthDate"));
    }

    @Test
    void testGetAllRequiredEntityFieldsDeduplicates() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{
                    new DirectMapping("name", "firstName", String.class, Optional.empty())
            },
            new ComputedField[]{
                    new ComputedField("fullName", new String[]{"firstName", "lastName"})
            },
            new ComputationProvider[]{}
        );

        List<String> required = metadata.getAllRequiredEntityFields();

        // firstName should appear only once
        assertEquals(2, required.size());
        assertTrue(required.contains("firstName"));
        assertTrue(required.contains("lastName"));
    }

    @Test
    void testGetDirectMapping() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{
                    new DirectMapping("userEmail", "email", String.class, Optional.empty())
            },
            new ComputedField[]{},
            new ComputationProvider[]{}
        );

        assertTrue(metadata.getDirectMapping("userEmail", false).isPresent());
        assertEquals("email", metadata.getDirectMapping("userEmail", false).get().entityField());
        assertFalse(metadata.getDirectMapping("nonExistent", false).isPresent());
    }

    @Test
    void testGetComputedField() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{},
            new ComputedField[]{
                    new ComputedField("fullName", new String[]{"firstName", "lastName"})
            },
            new ComputationProvider[]{}
        );

        assertTrue(metadata.getComputedField("fullName",false).isPresent());
        assertEquals(2, metadata.getComputedField("fullName",false).get().dependencyCount());
        assertFalse(metadata.getComputedField("nonExistent",false).isPresent());
    }

    @Test
    void testIsComputedField() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{
                    new DirectMapping("email", "email", String.class, Optional.empty())
            },
            new ComputedField[]{
                    new ComputedField("fullName", new String[]{"firstName", "lastName"})
            },
            new ComputationProvider[]{}
        );

        assertTrue(metadata.isComputedField("fullName",false));
        assertFalse(metadata.isComputedField("email",false));
    }

    @Test
    void testIsDirectMapping() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{
                    new DirectMapping("email", "email", String.class, Optional.empty())
            },
            new ComputedField[]{
                    new ComputedField("fullName", new String[]{"firstName", "lastName"})
            },
            new ComputationProvider[]{}
        );

        assertTrue(metadata.isDirectMapping("email",false));
        assertFalse(metadata.isDirectMapping("fullName",false));
    }

    // ==================== Exposure Layer tests ====================

    @Test
    void testConvenienceConstructorSetsDefaults() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{},
            new ComputedField[]{},
            new ComputationProvider[]{}
        );

        assertNotNull(metadata.exposedCriteria());
        assertEquals(0, metadata.exposedCriteria().length);
        assertNull(metadata.exposure());
        assertFalse(metadata.isExposed());
    }

    @Test
    void testIsExposedWithExposureMetadata() {
        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{},
            new ComputedField[]{},
            new ComputationProvider[]{},
            new ExposedCriterion[]{},
            new ExposureMetadata("users", "api", "WINDOWED")
        );

        assertTrue(metadata.isExposed());
        assertEquals("users", metadata.exposure().value());
    }

    @Test
    void testGetCriterion() {
        ExposedCriterion c1 = new ExposedCriterion("NAME", "name", new String[]{"EQ"}, true, false);
        ExposedCriterion c2 = new ExposedCriterion("SOURCE_SITE__SITE_NAME", "sourceSite.name", new String[]{"MATCHES"}, true, true);

        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{},
            new ComputedField[]{},
            new ComputationProvider[]{},
            new ExposedCriterion[]{c1, c2},
            null
        );

        assertTrue(metadata.getCriterion("NAME", false).isPresent());
        assertEquals("name", metadata.getCriterion("NAME", false).get().sourcePath());

        assertTrue(metadata.getCriterion("SOURCE_SITE__SITE_NAME", false).isPresent());
        assertEquals("sourceSite.name", metadata.getCriterion("SOURCE_SITE__SITE_NAME", false).get().sourcePath());

        assertFalse(metadata.getCriterion("NONEXISTENT", false).isPresent());
    }

    @Test
    void testGetCriterionIgnoreCase() {
        ExposedCriterion c1 = new ExposedCriterion("NAME", "name", new String[]{"EQ"}, true, false);

        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{},
            new ComputedField[]{},
            new ComputationProvider[]{},
            new ExposedCriterion[]{c1},
            null
        );

        assertTrue(metadata.getCriterion("name", true).isPresent());
        assertFalse(metadata.getCriterion("name", false).isPresent());
    }

    @Test
    void testGetComposedCriteria() {
        ExposedCriterion direct = new ExposedCriterion("NAME", "name", new String[]{"EQ"}, true, false);
        ExposedCriterion composed1 = new ExposedCriterion("ADDR__CITY", "address.city", new String[]{"EQ"}, true, true);
        ExposedCriterion composed2 = new ExposedCriterion("ADDR__COUNTRY", "address.country", new String[]{"EQ"}, true, true);

        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{},
            new ComputedField[]{},
            new ComputationProvider[]{},
            new ExposedCriterion[]{direct, composed1, composed2},
            null
        );

        List<ExposedCriterion> composed = metadata.getComposedCriteria();
        assertEquals(2, composed.size());
        assertTrue(composed.stream().allMatch(ExposedCriterion::isComposed));
    }

    @Test
    void testGetProjectionMappings() {
        DirectMapping scalar = new DirectMapping("email", "email", String.class, Optional.empty());
        DirectMapping projection = new DirectMapping(
                "sourceSite", "sourceSite", Object.class, Optional.empty(),
                Optional.of("SOURCE_SITE"), false
        );

        ProjectionMetadata metadata = new ProjectionMetadata(
            Object.class,
            new DirectMapping[]{scalar, projection},
            new ComputedField[]{},
            new ComputationProvider[]{}
        );

        List<DirectMapping> projections = metadata.getProjectionMappings();
        assertEquals(1, projections.size());
        assertEquals("sourceSite", projections.get(0).dtoField());
        assertTrue(projections.get(0).isProjectionType());
    }
}