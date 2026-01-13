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
 * Unit tests for TafIngestionApp.
 * <p>
 * Since TafIngestionApp is a simple concrete implementation of AbstractNoaaIngestionApp,
 * most functionality is tested in AbstractNoaaIngestionAppTest.
 * <p>
 * These tests focus on TAF-specific behavior.
 *
 * @author bclasky1539
 *
 */
class TafIngestionAppTest {

    @Test
    void testGetDataType() {
        // Arrange
        TafIngestionApp app = new TafIngestionApp();

        // Act
        String dataType = app.getDataType();

        // Assert
        assertEquals("TAF", dataType);
    }

    @Test
    void testCreateOrchestrator_ReturnsTafOrchestrator() {
        // Arrange
        TafIngestionApp app = new TafIngestionApp();

        // Act
        AbstractNoaaIngestionOrchestrator orchestrator =
                app.createOrchestrator(null, null, 10);

        // Assert
        assertNotNull(orchestrator);
        assertInstanceOf(TafIngestionOrchestrator.class, orchestrator);
    }

    @Test
    void testGetAdditionalUsageNotes_ReturnsNotes() {
        // Arrange
        TafIngestionApp app = new TafIngestionApp();

        // Act
        String notes = app.getAdditionalUsageNotes();

        // Assert
        assertNotNull(notes, "TAF app should have additional usage notes");
        assertTrue(notes.contains("TAF updates every 6 hours"));
        assertTrue(notes.contains("30-60 minute"));
    }

    @Test
    void testAdditionalUsageNotes_ContainsNewline() {
        // Arrange
        TafIngestionApp app = new TafIngestionApp();

        // Act
        String notes = app.getAdditionalUsageNotes();

        // Assert
        assertTrue(notes.contains("\n"), "Usage notes should be multi-line");
    }

    @Test
    void testMainMethod_DoesNotThrowWithNoArgs() {
        // This is a smoke test to ensure main() can be called
        // It will print usage and exit gracefully

        // Act & Assert
        assertDoesNotThrow(() -> TafIngestionApp.main(new String[]{}));
    }
}
