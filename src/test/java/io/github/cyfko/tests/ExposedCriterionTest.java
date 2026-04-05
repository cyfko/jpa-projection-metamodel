package io.github.cyfko.tests;

import io.github.cyfko.jpametamodel.api.ExposedCriterion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExposedCriterionTest {

    @Test
    void testConstructorValidation() {
        assertThrows(NullPointerException.class, () ->
            new ExposedCriterion(null, "name", new String[]{"EQ"}, true, false)
        );

        assertThrows(NullPointerException.class, () ->
            new ExposedCriterion("NAME", null, new String[]{"EQ"}, true, false)
        );

        assertThrows(NullPointerException.class, () ->
            new ExposedCriterion("NAME", "name", null, true, false)
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ExposedCriterion("", "name", new String[]{"EQ"}, true, false)
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ExposedCriterion("NAME", "  ", new String[]{"EQ"}, true, false)
        );

        assertDoesNotThrow(() ->
            new ExposedCriterion("NAME", "name", new String[]{"EQ"}, true, false)
        );

        // Empty operators array is valid
        assertDoesNotThrow(() ->
            new ExposedCriterion("NAME", "name", new String[]{}, true, false)
        );
    }

    @Test
    void testDirectCriterion() {
        ExposedCriterion criterion = new ExposedCriterion(
                "NAME", "name", new String[]{"EQ", "MATCHES"}, true, false
        );

        assertEquals("NAME", criterion.ref());
        assertEquals("name", criterion.sourcePath());
        assertEquals(2, criterion.operatorCount());
        assertTrue(criterion.exposed());
        assertFalse(criterion.composed());
        assertFalse(criterion.isComposed());
        assertEquals(0, criterion.compositionDepth());
        assertArrayEquals(new String[]{"NAME"}, criterion.refSegments());
    }

    @Test
    void testComposedCriterionOneLevel() {
        ExposedCriterion criterion = new ExposedCriterion(
                "SOURCE_SITE__SITE_NAME", "sourceSite.name",
                new String[]{"EQ", "MATCHES"}, true, true
        );

        assertTrue(criterion.isComposed());
        assertEquals(1, criterion.compositionDepth());
        assertArrayEquals(new String[]{"SOURCE_SITE", "SITE_NAME"}, criterion.refSegments());
    }

    @Test
    void testComposedCriterionTwoLevels() {
        ExposedCriterion criterion = new ExposedCriterion(
                "SOURCE_SITE__LOCALITY__NAME", "sourceSite.locality.name",
                new String[]{"EQ"}, true, true
        );

        assertTrue(criterion.isComposed());
        assertEquals(2, criterion.compositionDepth());
        assertArrayEquals(new String[]{"SOURCE_SITE", "LOCALITY", "NAME"}, criterion.refSegments());
    }

    @Test
    void testSupportsOperator() {
        ExposedCriterion criterion = new ExposedCriterion(
                "NAME", "name", new String[]{"EQ", "MATCHES", "IN"}, true, false
        );

        assertTrue(criterion.supportsOperator("EQ"));
        assertTrue(criterion.supportsOperator("MATCHES"));
        assertTrue(criterion.supportsOperator("IN"));
        assertFalse(criterion.supportsOperator("GT"));
        assertFalse(criterion.supportsOperator("eq")); // case-sensitive
    }

    @Test
    void testInternalOnlyCriterion() {
        ExposedCriterion criterion = new ExposedCriterion(
                "TENANT_ID", "tenantId", new String[]{"EQ"}, false, false
        );

        assertFalse(criterion.exposed());
        assertFalse(criterion.isComposed());
    }
}
