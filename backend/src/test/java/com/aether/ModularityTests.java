package com.aether;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Enforces ADR-001: every top-level package is a module, and a module may only
 * use another module's public API — never its {@code internal} package.
 * A violation or a dependency cycle fails the build here.
 */
class ModularityTests {

    @Test
    void modulesRespectBoundaries() {
        ApplicationModules.of(AetherApplication.class).verify();
    }
}
