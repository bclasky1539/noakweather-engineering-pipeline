/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025-2026 bclasky1539
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package weather.ingestion.service.source.noaa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetarIngestionApp.
 * <p>
 * Since MetarIngestionApp is a simple concrete implementation of AbstractNoaaIngestionApp,
 * most functionality is tested in AbstractNoaaIngestionAppTest.
 * <p>
 * These tests focus on METAR-specific behavior.
 *
 * @author bclasky1539
 *
 */
class MetarIngestionAppTest {

    @Test
    void testGetDataType() {
        // Arrange
        MetarIngestionApp app = new MetarIngestionApp();

        // Act
        String dataType = app.getDataType();

        // Assert
        assertEquals("METAR", dataType);
    }

    @Test
    void testCreateOrchestrator_ReturnsMetarOrchestrator() {
        // Arrange
        MetarIngestionApp app = new MetarIngestionApp();

        // Act
        AbstractNoaaIngestionOrchestrator orchestrator =
                app.createOrchestrator(null, null, 10);

        // Assert
        assertNotNull(orchestrator);
        assertInstanceOf(MetarIngestionOrchestrator.class, orchestrator);
    }

    @Test
    void testGetAdditionalUsageNotes_ReturnsNull() {
        // Arrange
        MetarIngestionApp app = new MetarIngestionApp();

        // Act
        String notes = app.getAdditionalUsageNotes();

        // Assert
        assertNull(notes, "METAR app should not have additional usage notes");
    }

    @Test
    void testMainMethod_DoesNotThrowWithNoArgs() {
        // This is a smoke test to ensure main() can be called
        // It will print usage and exit gracefully

        // Act & Assert
        assertDoesNotThrow(() -> MetarIngestionApp.main(new String[]{}));
    }
}
