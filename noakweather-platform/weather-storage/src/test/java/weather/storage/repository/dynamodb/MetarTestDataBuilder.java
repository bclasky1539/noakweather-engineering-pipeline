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

import weather.model.NoaaMetarData;
import weather.model.WeatherConditions;
import weather.model.WeatherDataSource;
import weather.model.components.*;
import weather.model.enums.SkyCoverage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builder for creating test METAR data with realistic values.
 * <p>
 * This builder provides:
 * - Fluent API for customization
 * - Sensible defaults for all fields
 * - Realistic weather data combinations
 * - Support for edge cases (extreme temperatures, low visibility, etc.)
 * <p>
 * Philosophy:
 * - Default values create valid, realistic METAR data
 * - Each setter returns 'this' for method chaining
 * - Can create both simple and complex test scenarios
 * <p>
 * Usage Examples:
 *
 * <pre>
 * // Simple METAR with defaults
 * NoaaMetarData metar = MetarTestDataBuilder.create("KJFK").build();
 *
 * // Customized METAR
 * NoaaMetarData metar = MetarTestDataBuilder.create("KLGA")
 *     .withTemperature(25.0, 15.0)
 *     .withWind(270, 15, 25)
 *     .withVisibility(5.0, "SM")
 *     .build();
 *
 * // IMC conditions
 * NoaaMetarData metar = MetarTestDataBuilder.createIMC("KEWR");
 *
 * // VFR conditions
 * NoaaMetarData metar = MetarTestDataBuilder.createVFR("KTEB");
 * </pre>
 *
 * @author bclasky1539
 *
 */
public class MetarTestDataBuilder {

    private final String stationId;
    private Instant observationTime;
    private String rawText;

    // Weather conditions components
    private Wind wind;
    private Visibility visibility;
    private final List<PresentWeather> presentWeather;
    private final List<SkyCondition> skyConditions;
    private Temperature temperature;
    private Pressure pressure;

    // Metadata
    private Double latitude;
    private Double longitude;
    private Integer elevationFeet;
    private WeatherDataSource source;

    /**
     * Private constructor - use static factory methods.
     */
    private MetarTestDataBuilder(String stationId) {
        this.stationId = stationId;

        // Set sensible defaults
        this.observationTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        this.source = WeatherDataSource.NOAA;
        this.presentWeather = new ArrayList<>();
        this.skyConditions = new ArrayList<>();

        // Default VFR conditions
        this.wind = Wind.of(360, 10, "KT");
        this.visibility = Visibility.of(10.0, "SM", false, false);
        this.temperature = Temperature.of(20.0, 10.0);
        this.pressure = Pressure.ofInchesHg(29.92);
        this.skyConditions.add(SkyCondition.of(SkyCoverage.FEW, 25000, null));
    }

    /**
     * Create a builder with default VFR conditions.
     *
     * @param stationId ICAO station identifier (e.g., "KJFK")
     * @return builder instance
     */
    public static MetarTestDataBuilder create(String stationId) {
        return new MetarTestDataBuilder(stationId);
    }

    /**
     * Create METAR with IMC (Instrument Meteorological Conditions).
     * - Low ceiling (800 feet BKN)
     * - Reduced visibility (2 SM)
     * - Light rain
     *
     * @param stationId ICAO station identifier
     * @return METAR with IMC conditions
     */
    public static NoaaMetarData createIMC(String stationId) {
        return new MetarTestDataBuilder(stationId)
                .withVisibility(2.0, "SM")
                .withSkyCondition(SkyCoverage.BROKEN, 800, null)
                .withPresentWeather("-RA")  // Light rain
                .withRawText("METAR " + stationId + " IMC 08008KT 2SM -RA BKN008 15/13 A2990")
                .build();
    }

    /**
     * Create METAR with VFR (Visual Flight Rules) conditions.
     * - High ceiling (25000 feet FEW)
     * - Good visibility (10 SM)
     * - No precipitation
     *
     * @param stationId ICAO station identifier
     * @return METAR with VFR conditions
     */
    public static NoaaMetarData createVFR(String stationId) {
        return new MetarTestDataBuilder(stationId)
                .withVisibility(10.0, "SM")
                .withSkyCondition(SkyCoverage.FEW, 25000, null)
                .withRawText("METAR " + stationId + " VFR 36010KT 10SM FEW250 20/10 A2992")
                .build();
    }

    /**
     * Create METAR with LIFR (Low Instrument Flight Rules) conditions.
     * - Very low ceiling (200 feet OVC)
     * - Poor visibility (1/2 SM)
     * - Fog
     *
     * @param stationId ICAO station identifier
     * @return METAR with LIFR conditions
     */
    public static NoaaMetarData createLIFR(String stationId) {
        return new MetarTestDataBuilder(stationId)
                .withVisibility(0.5, "SM")
                .withSkyCondition(SkyCoverage.OVERCAST, 200, null)
                .withPresentWeather("FG")  // Fog
                .withRawText("METAR " + stationId + " LIFR 00000KT 1/2SM FG OVC002 10/09 A2985")
                .build();
    }

    /**
     * Create METAR with random realistic values.
     * Useful for stress testing and bulk operations.
     *
     * @param stationId ICAO station identifier
     * @return METAR with random conditions
     */
    public static NoaaMetarData createRandom(String stationId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return new MetarTestDataBuilder(stationId)
                .withWind(
                        random.nextInt(0, 360),
                        random.nextInt(5, 30),
                        null
                )
                .withVisibility(random.nextDouble(0.5, 10.0), "SM")
                .withTemperature(
                        random.nextDouble(-20.0, 40.0),
                        random.nextDouble(-30.0, 25.0)
                )
                .build();
    }

    // ========== Builder Methods ==========

    public MetarTestDataBuilder withObservationTime(Instant time) {
        this.observationTime = time;
        return this;
    }

    public MetarTestDataBuilder withRawText(String rawText) {
        this.rawText = rawText;
        return this;
    }

    public MetarTestDataBuilder withWind(Integer direction, Integer speed, Integer gust) {
        if (direction == null || speed == null) {
            this.wind = Wind.calm();
        } else if (direction == 0 && speed == 0) {
            this.wind = Wind.calm();
        } else if (gust != null) {
            this.wind = Wind.ofWithGusts(direction, speed, gust, "KT");
        } else {
            this.wind = Wind.of(direction, speed, "KT");
        }
        return this;
    }

    public MetarTestDataBuilder withVisibility(Double distance, String unit) {
        this.visibility = Visibility.of(distance, unit, false, false);
        return this;
    }

    public MetarTestDataBuilder withPresentWeather(String weatherCode) {
        this.presentWeather.add(PresentWeather.of(weatherCode));
        return this;
    }

    public MetarTestDataBuilder withSkyCondition(SkyCoverage coverage, Integer heightFeet, String cloudType) {
        this.skyConditions.clear();  // Replace default
        this.skyConditions.add(SkyCondition.of(coverage, heightFeet, cloudType));
        return this;
    }

    public MetarTestDataBuilder addSkyCondition(SkyCoverage coverage, Integer heightFeet, String cloudType) {
        this.skyConditions.add(SkyCondition.of(coverage, heightFeet, cloudType));
        return this;
    }

    public MetarTestDataBuilder withTemperature(Double tempC, Double dewpointC) {
        this.temperature = Temperature.of(tempC, dewpointC);
        return this;
    }

    public MetarTestDataBuilder withPressure(Double inchesHg) {
        this.pressure = Pressure.ofInchesHg(inchesHg);
        return this;
    }

    public MetarTestDataBuilder withCoordinates(Double latitude, Double longitude, Integer elevationFeet) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevationFeet = elevationFeet;
        return this;
    }

    public MetarTestDataBuilder withSource(WeatherDataSource source) {
        this.source = source;
        return this;
    }

    /**
     * Build the NoaaMetarData instance.
     *
     * @return fully constructed METAR data
     */
    public NoaaMetarData build() {
        // Create weather conditions
        WeatherConditions conditions = WeatherConditions.of(
                wind,
                visibility,
                presentWeather,
                skyConditions,
                temperature,
                pressure
        );

        // Create METAR data
        NoaaMetarData metar = new NoaaMetarData(
                stationId,
                observationTime
        );

        // Set conditions
        metar.setConditions(conditions);

        // Set raw text (auto-generate if not provided)
        metar.setRawText(Objects.requireNonNullElseGet(rawText, this::generateRawText));

        // Set location if provided
        if (latitude != null) {
            metar.setLatitude(latitude);
        }
        if (longitude != null) {
            metar.setLongitude(longitude);
        }
        if (elevationFeet != null) {
            metar.setElevationFeet(elevationFeet);
        }

        // Set source
        metar.setSource(source);

        return metar;
    }

    /**
     * Auto-generate a realistic raw METAR text from the builder's values.
     * This is a simplified version - real METARs are more complex.
     */
    private String generateRawText() {
        StringBuilder sb = new StringBuilder();

        sb.append("METAR ").append(stationId).append(" ");

        // Wind
        if (wind != null && !wind.isCalm()) {
            sb.append(String.format("%03d%02dKT ",
                    wind.directionDegrees(),
                    wind.getSpeedKnots()));
        } else {
            sb.append("00000KT ");
        }

        // Visibility
        if (visibility != null) {
            sb.append(String.format("%.0f%s ",
                    visibility.distanceValue(),
                    visibility.unit()));
        }

        // Present weather
        if (!presentWeather.isEmpty()) {
            presentWeather.forEach(pw -> sb.append(pw.rawCode()).append(" "));
        }

        // Sky conditions
        if (!skyConditions.isEmpty()) {
            skyConditions.forEach(sky ->
                    sb.append(sky.coverage()).append(String.format("%03d ", sky.heightFeet() / 100)));
        }

        // Temperature/Dewpoint
        if (temperature != null) {
            sb.append(String.format("%02d/%02d ",
                    temperature.celsius().intValue(),
                    temperature.dewpointCelsius().intValue()));
        }

        // Altimeter
        if (pressure != null) {
            sb.append(pressure.toMetarAltimeter()).append(" ");
        }

        return sb.toString().trim();
    }
}
