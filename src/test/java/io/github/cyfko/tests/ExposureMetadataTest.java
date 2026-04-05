package io.github.cyfko.tests;

import io.github.cyfko.jpametamodel.api.ExposureMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExposureMetadataTest {

    @Test
    void testConstructorValidation() {
        assertThrows(NullPointerException.class, () ->
            new ExposureMetadata(null, "api", "WINDOWED")
        );

        assertThrows(NullPointerException.class, () ->
            new ExposureMetadata("users", null, "WINDOWED")
        );

        assertThrows(NullPointerException.class, () ->
            new ExposureMetadata("users", "api", null)
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ExposureMetadata("", "api", "WINDOWED")
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ExposureMetadata("users", "api", "   ")
        );

        // Empty namespace is valid
        assertDoesNotThrow(() ->
            new ExposureMetadata("users", "", "WINDOWED")
        );
    }

    @Test
    void testWindowedStrategy() {
        ExposureMetadata metadata = new ExposureMetadata("users", "api", "WINDOWED");

        assertEquals("users", metadata.value());
        assertEquals("api", metadata.namespace());
        assertEquals("WINDOWED", metadata.strategy());
        assertTrue(metadata.isWindowed());
        assertFalse(metadata.isFull());
        assertFalse(metadata.isCustom());
        assertTrue(metadata.hasNamespace());
    }

    @Test
    void testFullStrategy() {
        ExposureMetadata metadata = new ExposureMetadata("config", "admin", "FULL");

        assertTrue(metadata.isFull());
        assertFalse(metadata.isWindowed());
        assertFalse(metadata.isCustom());
    }

    @Test
    void testCustomStrategy() {
        ExposureMetadata metadata = new ExposureMetadata("dashboard", "", "CUSTOM");

        assertTrue(metadata.isCustom());
        assertFalse(metadata.isWindowed());
        assertFalse(metadata.isFull());
        assertFalse(metadata.hasNamespace());
    }

    @Test
    void testEmptyNamespace() {
        ExposureMetadata metadata = new ExposureMetadata("products", "", "WINDOWED");

        assertFalse(metadata.hasNamespace());
        assertEquals("", metadata.namespace());
    }
}
