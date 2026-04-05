package io.github.cyfko.tests;

import io.github.cyfko.jpametamodel.api.DirectMapping;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DirectMappingTest {

    @Test
    void testConstructorValidation() {
        assertThrows(NullPointerException.class, () ->
            new DirectMapping(null, "entityField", null, Optional.empty())
        );

        assertThrows(NullPointerException.class, () ->
            new DirectMapping("dtoField", null, String.class, Optional.empty())
        );

        assertThrows(IllegalArgumentException.class, () ->
            new DirectMapping("", "entityField", String.class, Optional.empty())
        );

        assertThrows(IllegalArgumentException.class, () ->
            new DirectMapping("dtoField", "  ",  String.class, Optional.empty())
        );

        assertDoesNotThrow(() ->
                new DirectMapping("xx", "entityField", String.class, Optional.empty())
        );

        assertThrows(NullPointerException.class, () ->
                new DirectMapping("xx", "entityField", String.class, null)
        );
    }

    @Test
    void testSimpleMapping() {
        DirectMapping mapping = new DirectMapping("userEmail", "email", String.class, Optional.empty());

        assertEquals("userEmail", mapping.dtoField());
        assertEquals("email", mapping.entityField());
        assertFalse(mapping.isNested());
        assertEquals(0, mapping.nestingDepth());
        assertEquals("email", mapping.getRootField());
    }

    @Test
    void testNestedMapping() {
        DirectMapping mapping = new DirectMapping("city", "address.city", String.class, Optional.empty());

        assertTrue(mapping.isNested());
        assertEquals(1, mapping.nestingDepth());
        assertEquals("address", mapping.getRootField());
    }

    @Test
    void testDeeplyNestedMapping() {
        DirectMapping mapping = new DirectMapping("street", "user.address.street", String.class, Optional.empty());

        assertTrue(mapping.isNested());
        assertEquals(2, mapping.nestingDepth());
        assertEquals("user", mapping.getRootField());
    }

    // ==================== logicalPrefix and cycleBreak tests ====================

    @Test
    void testConvenienceConstructorDefaults() {
        DirectMapping mapping = new DirectMapping("email", "email", String.class, Optional.empty());

        // Convenience constructor should set defaults
        assertEquals(Optional.empty(), mapping.logicalPrefix());
        assertFalse(mapping.cycleBreak());
        assertFalse(mapping.isProjectionType());
    }

    @Test
    void testComposedProjectionMapping() {
        DirectMapping mapping = new DirectMapping(
                "sourceSite", "sourceSite", Object.class, Optional.empty(),
                Optional.of("SOURCE_SITE"), false
        );

        assertTrue(mapping.isProjectionType());
        assertEquals("SOURCE_SITE", mapping.logicalPrefix().orElseThrow());
        assertFalse(mapping.cycleBreak());
    }

    @Test
    void testCycleBreakMapping() {
        DirectMapping mapping = new DirectMapping(
                "department", "department", Object.class, Optional.empty(),
                Optional.of("DEPT"), true
        );

        assertTrue(mapping.isProjectionType());
        assertTrue(mapping.cycleBreak());
    }

    @Test
    void testLogicalPrefixCannotContainDoubleUnderscore() {
        assertThrows(IllegalArgumentException.class, () ->
            new DirectMapping("site", "site", Object.class, Optional.empty(),
                    Optional.of("SOURCE__SITE"), false)
        );
    }

    @Test
    void testLogicalPrefixCannotStartWithUnderscore() {
        assertThrows(IllegalArgumentException.class, () ->
            new DirectMapping("site", "site", Object.class, Optional.empty(),
                    Optional.of("_SOURCE_SITE"), false)
        );
    }

    @Test
    void testLogicalPrefixCannotEndWithUnderscore() {
        assertThrows(IllegalArgumentException.class, () ->
            new DirectMapping("site", "site", Object.class, Optional.empty(),
                    Optional.of("SOURCE_SITE_"), false)
        );
    }

    @Test
    void testLogicalPrefixCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
            new DirectMapping("site", "site", Object.class, Optional.empty(),
                    Optional.of("   "), false)
        );
    }

    @Test
    void testLogicalPrefixWithSingleUnderscoreIsValid() {
        assertDoesNotThrow(() ->
            new DirectMapping("site", "site", Object.class, Optional.empty(),
                    Optional.of("SOURCE_SITE"), false)
        );
    }

    @Test
    void testLogicalPrefixNullOptionalRequiresWrapper() {
        assertThrows(NullPointerException.class, () ->
            new DirectMapping("site", "site", Object.class, Optional.empty(),
                    null, false)
        );
    }
}