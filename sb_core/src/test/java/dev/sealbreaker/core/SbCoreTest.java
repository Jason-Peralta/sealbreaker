package dev.sealbreaker.core;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Milestone 0 smoke test for the JUnit side of the toolchain: pure-logic tests that may touch
 * Minecraft classes without booting a server. Real tests (rolling, Seal rules, hit-shape math)
 * arrive with the systems in Milestone 1.
 */
class SbCoreTest {
    @Test
    void modIdIsAValidNamespace() {
        assertTrue(Identifier.isValidNamespace(SbCore.MOD_ID), "mod id must be a valid resource namespace");
        assertEquals("sb_core:hello_world", Identifier.fromNamespaceAndPath(SbCore.MOD_ID, "hello_world").toString());
    }
}
