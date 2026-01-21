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
package weather.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.*;

/**
 * Universal base class for all weather data in the platform.
 * <p>
 * Design Philosophy:
 * - Source-agnostic: Works with NOAA, OpenWeatherMap, WeatherAPI, etc.
 * - Lambda Architecture ready: Tracks which processing layer it belongs to
 * - Immutable core fields: ID and ingestion time never change
 * - Extensible: Subclasses add source-specific fields
 * - Metadata-focused: Contains WHO, WHAT, WHEN, WHERE - not the weather conditions themselves
 * <p>
 * This is analogous to a universal "fact table header" in a data warehouse that can
 * accept data from any source system. The actual weather conditions are stored in
 * subclass-specific structures (e.g., WeatherConditions for observations/forecasts).
 * <p>
 * Sealed class: Only permits known weather data types
 *
 * @author bclasky1539
 *
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "dataType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoaaWeatherData.class, name = "NOAA"),
        @JsonSubTypes.Type(value = NoaaMetarData.class, name = "METAR"),
        @JsonSubTypes.Type(value = NoaaTafData.class, name = "TAF"),
        @JsonSubTypes.Type(value = TestWeatherData.class, name = "TEST")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract sealed class WeatherData
        permits NoaaWeatherData, TestWeatherData {

    /**
     * Unique identifier for this weather data record.
     * Generated at ingestion time, never changes.
     */
    private final String id;

    /**
     * When this data was ingested into our platform (not observation time).
     * Uses Instant for precise timestamp with timezone.
     */
    private final Instant ingestionTime;

    /**
     * Source of this weather data (NOAA, OpenWeatherMap, etc.)
     */
    private WeatherDataSource source;

    /**
     * Which layer of Lambda Architecture this data is in
     */
    private ProcessingLayer processingLayer;

    /**
     * ICAO station identifier or location identifier.
     * Examples: "KJFK", "KLGA" for aviation, or city IDs for other sources
     */
    private String stationId;

    /**
     * When the weather was actually observed (UTC).
     * This is the meteorological observation time.
     */
    private Instant observationTime;

    /**
     * Geographic location of the observation
     */
    private GeoLocation location;

    /**
     * Raw data as received from the source API.
     * Useful for debugging, auditing, and re-processing.
     */
    private String rawData;

    /**
     * Quality control flags or validation status
     */
    private String qualityFlags;

    /**
     * Flexible metadata storage for source-specific attributes
     * that don't warrant dedicated fields.
     */
    private Map<String, Object> metadata;

    /**
     * Protected constructor for subclasses.
     * Automatically generates ID and sets ingestion time.
     */
    protected WeatherData() {
        this.id = UUID.randomUUID().toString();
        this.ingestionTime = Instant.now();
        this.metadata = new HashMap<>();
    }

    /**
     * Constructor with required fields
     * @param source The WeatherDataSource
     * @param stationId The Station ID
     * @param observationTime The observation time
     */
    protected WeatherData(WeatherDataSource source, String stationId, Instant observationTime) {
        this();
        this.source = source;
        this.stationId = stationId;
        this.observationTime = observationTime;
        this.processingLayer = ProcessingLayer.SPEED_LAYER; // Default to speed layer
    }

    // ==================== Getters and Setters ====================

    // ID and ingestionTime are immutable (no setters)

    public String getId() {
        return id;
    }

    public Instant getIngestionTime() {
        return ingestionTime;
    }

    public WeatherDataSource getSource() {
        return source;
    }

    public void setSource(WeatherDataSource source) {
        this.source = source;
    }

    public ProcessingLayer getProcessingLayer() {
        return processingLayer;
    }

    public void setProcessingLayer(ProcessingLayer processingLayer) {
        this.processingLayer = processingLayer;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public Instant getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(Instant observationTime) {
        this.observationTime = observationTime;
    }

    public GeoLocation getLocation() {
        return location;
    }

    public void setLocation(GeoLocation location) {
        this.location = location;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String getQualityFlags() {
        return qualityFlags;
    }

    public void setQualityFlags(String qualityFlags) {
        this.qualityFlags = qualityFlags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Add a single metadata entry
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    // ==================== Abstract Methods ====================

    /**
     * Determines if this weather data is considered current/valid.
     * Subclasses implement based on their specific validity rules.
     *
     * @return true if the data is current and usable
     */
    public abstract boolean isCurrent();

    /**
     * Returns the specific type of weather data.
     * Examples: "METAR", "TAF", "CURRENT", "FORECAST"
     *
     * @return the data type identifier
     */
    public abstract String getDataType();

    /**
     * Returns a human-readable summary of this weather data.
     * Useful for logging and debugging.
     *
     * @return summary string
     */
    public abstract String getSummary();

    // ==================== Object Methods ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WeatherData that)) {
            return false;
        }
        return Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "%s{id='%s', source=%s, stationId='%s', observationTime=%s, layer=%s}".formatted(
                getClass().getSimpleName(), id, source, stationId, observationTime, processingLayer);
    }
}
