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
package weather.model.components.remark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import weather.model.components.Pressure;
import weather.model.components.Temperature;
import weather.model.enums.AutomatedStationType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON serialization tests for NoaaMetarRemarks.
 * <p>
 * The ObjectMapper is configured the same way as the one in S3UploadService,
 * which writes the Bronze speed-layer JSON.
 *
 * @author bclasky1539
 *
 */
class NoaaMetarRemarksSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Should serialize the precise temperature once, with the dewpoint inside it - KATL (Issue #217)")
    void testPreciseTemperatureSerializedOnce() {
        // KATL 081952Z ... RMK AO2 SLP174 T02060072
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .automatedStationType(AutomatedStationType.AO2)
                .seaLevelPressure(Pressure.hectopascals(1017.4))
                .preciseTemperature(Temperature.of(20.6, 7.2))
                .build();

        JsonNode json = objectMapper.valueToTree(remarks);

        assertThat(json.has("preciseDewpoint")).isFalse();
        assertThat(json.path("preciseTemperature").path("celsius").asDouble()).isEqualTo(20.6);
        assertThat(json.path("preciseTemperature").path("dewpointCelsius").asDouble()).isEqualTo(7.2);
    }

    @Test
    @DisplayName("Should serialize a precise temperature without a dewpoint - Issue #217")
    void testPreciseTemperatureWithoutDewpoint() {
        // T-group with the dewpoint omitted, e.g. T0206
        NoaaMetarRemarks remarks = NoaaMetarRemarks.builder()
                .preciseTemperature(Temperature.of(20.6))
                .build();

        JsonNode json = objectMapper.valueToTree(remarks);

        assertThat(json.has("preciseDewpoint")).isFalse();
        assertThat(json.path("preciseTemperature").path("celsius").asDouble()).isEqualTo(20.6);
        assertThat(json.path("preciseTemperature").path("dewpointCelsius").isNull()).isTrue();
    }
}
