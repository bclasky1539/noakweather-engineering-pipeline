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
package weather.storage.repository.dynamodb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import weather.model.NoaaMetarData;
import weather.model.NoaaTafData;
import weather.model.NoaaWeatherData;
import weather.model.WeatherData;
import weather.storage.exception.WeatherDataMappingException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DynamoDbMapper.
 * <p>
 * Tests serialization/deserialization and utility methods for DynamoDB operations.
 *
 * @author bclasky1539
 *
 */
class DynamoDbMapperTest {

    private DynamoDbMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DynamoDbMapper();
    }

    // ========== CREATE PRIMARY KEY TESTS ==========

    @Test
    void shouldCreatePrimaryKeyWithValidInputs() {
        // Given
        String stationId = "KJFK";
        Instant observationTime = Instant.parse("2026-01-20T12:00:00Z");

        // When
        Map<String, AttributeValue> key = mapper.createPrimaryKey(stationId, observationTime);

        // Then
        assertThat(key)
                .isNotNull()
                .hasSize(2)
                .containsKey("station_id")
                .containsKey("observation_time");

        assertThat(key.get("station_id").s()).isEqualTo("KJFK");
        assertThat(key.get("observation_time").s()).isEqualTo("2026-01-20T12:00:00Z");
    }

    @Test
    void shouldCreatePrimaryKeyWithDifferentStations() {
        // Given
        String[] stationIds = {"KJFK", "KLGA", "KEWR", "KBOS"};
        Instant observationTime = Instant.now();

        // When/Then
        for (String stationId : stationIds) {
            Map<String, AttributeValue> key = mapper.createPrimaryKey(stationId, observationTime);

            assertThat(key.get("station_id").s()).isEqualTo(stationId);
        }
    }

    @Test
    void shouldCreatePrimaryKeyWithDifferentTimes() {
        // Given
        String stationId = "KJFK";
        Instant time1 = Instant.parse("2026-01-20T12:00:00Z");
        Instant time2 = Instant.parse("2026-01-20T13:00:00Z");

        // When
        Map<String, AttributeValue> key1 = mapper.createPrimaryKey(stationId, time1);
        Map<String, AttributeValue> key2 = mapper.createPrimaryKey(stationId, time2);

        // Then
        assertThat(key1.get("observation_time").s()).isEqualTo("2026-01-20T12:00:00Z");
        assertThat(key2.get("observation_time").s()).isEqualTo("2026-01-20T13:00:00Z");
        assertThat(key1.get("observation_time").s()).isNotEqualTo(key2.get("observation_time").s());
    }

    @Test
    void shouldCreatePrimaryKeyWithMillisecondPrecision() {
        // Given
        String stationId = "KJFK";
        Instant observationTime = Instant.parse("2026-01-20T12:34:56.789Z");

        // When
        Map<String, AttributeValue> key = mapper.createPrimaryKey(stationId, observationTime);

        // Then
        assertThat(key.get("observation_time").s()).isEqualTo("2026-01-20T12:34:56.789Z");
    }

    // ========== EXTRACT STATION ID TESTS ==========

    @Test
    void shouldExtractStationIdFromValidMap() {
        // Given
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("station_id", AttributeValue.builder().s("KJFK").build());

        // When
        String stationId = mapper.extractStationId(attributeMap);

        // Then
        assertThat(stationId).isEqualTo("KJFK");
    }

    @Test
    void shouldReturnNullWhenStationIdNotPresent() {
        // Given
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("other_field", AttributeValue.builder().s("value").build());

        // When
        String stationId = mapper.extractStationId(attributeMap);

        // Then
        assertThat(stationId).isNull();
    }

    @Test
    void shouldReturnNullWhenMapIsNull() {
        // When
        String stationId = mapper.extractStationId(null);

        // Then
        assertThat(stationId).isNull();
    }

    @Test
    void shouldReturnNullWhenMapIsEmpty() {
        // Given
        Map<String, AttributeValue> emptyMap = new HashMap<>();

        // When
        String stationId = mapper.extractStationId(emptyMap);

        // Then
        assertThat(stationId).isNull();
    }

    @Test
    void shouldExtractDifferentStationIds() {
        // Given
        String[] stationIds = {"KJFK", "KLGA", "KEWR", "KBOS", "KIAD"};

        for (String expectedId : stationIds) {
            Map<String, AttributeValue> attributeMap = new HashMap<>();
            attributeMap.put("station_id", AttributeValue.builder().s(expectedId).build());

            // When
            String actualId = mapper.extractStationId(attributeMap);

            // Then
            assertThat(actualId).isEqualTo(expectedId);
        }
    }

    // ========== EXTRACT OBSERVATION TIME TESTS ==========

    @Test
    void shouldExtractObservationTimeFromValidMap() {
        // Given
        Instant expectedTime = Instant.parse("2026-01-20T12:00:00Z");
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("observation_time", AttributeValue.builder().s(expectedTime.toString()).build());

        // When
        Instant actualTime = mapper.extractObservationTime(attributeMap);

        // Then
        assertThat(actualTime).isEqualTo(expectedTime);
    }

    @Test
    void shouldReturnNullWhenObservationTimeNotPresent() {
        // Given
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("other_field", AttributeValue.builder().s("value").build());

        // When
        Instant observationTime = mapper.extractObservationTime(attributeMap);

        // Then
        assertThat(observationTime).isNull();
    }

    @Test
    void shouldReturnNullWhenMapIsNullForObservationTime() {
        // When
        Instant observationTime = mapper.extractObservationTime(null);

        // Then
        assertThat(observationTime).isNull();
    }

    @Test
    void shouldReturnNullWhenMapIsEmptyForObservationTime() {
        // Given
        Map<String, AttributeValue> emptyMap = new HashMap<>();

        // When
        Instant observationTime = mapper.extractObservationTime(emptyMap);

        // Then
        assertThat(observationTime).isNull();
    }

    @Test
    void shouldExtractObservationTimeWithMilliseconds() {
        // Given
        Instant expectedTime = Instant.parse("2026-01-20T12:34:56.789Z");
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("observation_time", AttributeValue.builder().s(expectedTime.toString()).build());

        // When
        Instant actualTime = mapper.extractObservationTime(attributeMap);

        // Then
        assertThat(actualTime).isEqualTo(expectedTime);
    }

    @Test
    void shouldExtractDifferentObservationTimes() {
        // Given
        Instant[] times = {
                Instant.parse("2026-01-20T00:00:00Z"),
                Instant.parse("2026-01-20T06:00:00Z"),
                Instant.parse("2026-01-20T12:00:00Z"),
                Instant.parse("2026-01-20T18:00:00Z"),
                Instant.parse("2026-01-20T23:59:59Z")
        };

        for (Instant expectedTime : times) {
            Map<String, AttributeValue> attributeMap = new HashMap<>();
            attributeMap.put("observation_time", AttributeValue.builder().s(expectedTime.toString()).build());

            // When
            Instant actualTime = mapper.extractObservationTime(attributeMap);

            // Then
            assertThat(actualTime).isEqualTo(expectedTime);
        }
    }

    // ========== ROUND-TRIP TESTS (createPrimaryKey + extract) ==========

    @Test
    void shouldRoundTripStationIdAndTime() {
        // Given
        String originalStationId = "KJFK";
        Instant originalTime = Instant.parse("2026-01-20T12:00:00Z");

        // When - Create key
        Map<String, AttributeValue> key = mapper.createPrimaryKey(originalStationId, originalTime);

        // Then - Extract values
        String extractedStationId = mapper.extractStationId(key);
        Instant extractedTime = mapper.extractObservationTime(key);

        assertThat(extractedStationId).isEqualTo(originalStationId);
        assertThat(extractedTime).isEqualTo(originalTime);
    }

    // ========== TO ATTRIBUTE MAP TESTS ==========

    @Test
    void shouldSerializeNoaaMetarData() {
        // Given
        NoaaMetarData metar = new NoaaMetarData();
        metar.setStationId("KJFK");
        metar.setObservationTime(Instant.parse("2026-01-20T12:00:00Z"));
        metar.setReportType("METAR");

        // When
        Map<String, AttributeValue> attributeMap = mapper.toAttributeMap(metar);

        // Then
        assertThat(attributeMap).containsKeys("station_id", "observation_time", "dataType", "dataJson");
        assertThat(attributeMap.get("station_id").s()).isEqualTo("KJFK");
        assertThat(attributeMap.get("dataType").s()).isEqualTo("METAR");
    }

    @Test
    void shouldSerializeNoaaTafData() {
        // Given
        NoaaTafData taf = new NoaaTafData();
        taf.setStationId("KJFK");
        taf.setObservationTime(Instant.parse("2026-01-20T12:00:00Z"));
        taf.setReportType("TAF");

        // When
        Map<String, AttributeValue> attributeMap = mapper.toAttributeMap(taf);

        // Then
        assertThat(attributeMap).containsKeys("station_id", "observation_time", "dataType", "dataJson");
        assertThat(attributeMap.get("station_id").s()).isEqualTo("KJFK");
        assertThat(attributeMap.get("dataType").s()).isEqualTo("TAF");
    }

    @Test
    void shouldThrowExceptionWhenSerializingNull() {
        // When/Then
        assertThatThrownBy(() -> mapper.toAttributeMap(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Weather data cannot be null");
    }

    // ========== FROM ATTRIBUTE MAP TESTS ==========

    @Test
    void shouldDeserializeMetarData() {
        // Given
        NoaaMetarData original = new NoaaMetarData();
        original.setStationId("KJFK");
        original.setObservationTime(Instant.parse("2026-01-20T12:00:00Z"));
        original.setReportType("METAR");

        Map<String, AttributeValue> attributeMap = mapper.toAttributeMap(original);

        // When
        WeatherData deserialized = mapper.fromAttributeMap(attributeMap);

        // Then
        assertThat(deserialized).isInstanceOf(NoaaMetarData.class);
        assertThat(deserialized.getStationId()).isEqualTo("KJFK");
        assertThat(((NoaaWeatherData) deserialized).getReportType()).isEqualTo("METAR");
    }

    @Test
    void shouldDeserializeTafData() {
        // Given
        NoaaTafData original = new NoaaTafData();
        original.setStationId("KJFK");
        original.setObservationTime(Instant.parse("2026-01-20T12:00:00Z"));
        original.setReportType("TAF");

        Map<String, AttributeValue> attributeMap = mapper.toAttributeMap(original);

        // When
        WeatherData deserialized = mapper.fromAttributeMap(attributeMap);

        // Then
        assertThat(deserialized).isInstanceOf(NoaaTafData.class);
        assertThat(deserialized.getStationId()).isEqualTo("KJFK");
        assertThat(((NoaaWeatherData) deserialized).getReportType()).isEqualTo("TAF");
    }

    @Test
    void shouldReturnNullForEmptyMap() {
        // Given
        Map<String, AttributeValue> emptyMap = new HashMap<>();

        // When
        WeatherData result = mapper.fromAttributeMap(emptyMap);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForNullMap() {
        // When
        WeatherData result = mapper.fromAttributeMap(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenDataJsonIsMissing() {
        // Given
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("station_id", AttributeValue.builder().s("KJFK").build());
        attributeMap.put("observation_time", AttributeValue.builder().s(Instant.now().toString()).build());
        // No dataJson

        // When
        WeatherData result = mapper.fromAttributeMap(attributeMap);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowExceptionForInvalidJson() {
        // Given
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("dataJson", AttributeValue.builder().s("invalid json {{{").build());

        // When/Then
        assertThatThrownBy(() -> mapper.fromAttributeMap(attributeMap))
                .isInstanceOf(WeatherDataMappingException.class)
                .hasMessageContaining("Failed to deserialize weather data from JSON");
    }
}
