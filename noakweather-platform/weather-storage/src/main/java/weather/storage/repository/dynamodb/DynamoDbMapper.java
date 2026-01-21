package weather.storage.repository.dynamodb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import weather.model.WeatherData;
import weather.storage.exception.WeatherDataMappingException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps between WeatherData domain objects and DynamoDB AttributeValue maps.
 * <p>
 * Think of this as a custom ORM (Object-Relational Mapper) but for DynamoDB.
 * It handles the translation between your Java objects and DynamoDB's attribute format,
 * similar to how JPA/Hibernate maps Java objects to database rows.
 * <p>
 * Key responsibilities:
 * 1. Serialize Java objects to DynamoDB attribute maps (toAttributeMap)
 * 2. Deserialize DynamoDB attribute maps back to Java objects (fromAttributeMap)
 * 3. Handle null/empty cases gracefully
 * 4. Support polymorphic types through Jackson's type system
 * <p>
 * CRITICAL FIXES APPLIED:
 * 1. fromAttributeMap() now returns null for empty maps instead of throwing exception
 * 2. This allows repository layer to return Optional.empty() for "not found" cases
 * 3. Jackson configured with JavaTimeModule for Instant serialization
 */
public class DynamoDbMapper {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbMapper.class);

    private final ObjectMapper objectMapper;

    /**
     * Attribute name for the JSON representation of the weather data
     */
    private static final String ATTR_DATA_JSON = "dataJson";

    /**
     * Attribute name for the partition key (station ID)
     */
    private static final String ATTR_STATION_ID = "station_id";

    /**
     * Attribute name for the sort key (observation time)
     */
    private static final String ATTR_OBSERVATION_TIME = "observation_time";

    /**
     * Attribute name for the data type discriminator
     */
    private static final String ATTR_DATA_TYPE = "dataType";

    public DynamoDbMapper() {
        this.objectMapper = new ObjectMapper();
        // CRITICAL: Register JavaTimeModule to handle Instant serialization
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Converts a WeatherData object to a DynamoDB attribute map.
     * <p>
     * This is like a custom SQL INSERT statement generator - it takes your Java object
     * and creates the key-value pairs that DynamoDB needs to store it.
     *
     * @param weatherData the weather data to convert
     * @return a map of DynamoDB attributes
     * @throws RuntimeException if serialization fails
     */
    public Map<String, AttributeValue> toAttributeMap(WeatherData weatherData) {
        if (weatherData == null) {
            throw new IllegalArgumentException("Weather data cannot be null");
        }

        Map<String, AttributeValue> attributeMap = new HashMap<>();

        try {
            // Store the partition key (station ID)
            if (weatherData.getStationId() != null) {
                attributeMap.put(ATTR_STATION_ID,
                        AttributeValue.builder().s(weatherData.getStationId()).build());
            }

            // Store the sort key (observation time as ISO-8601 string)
            if (weatherData.getObservationTime() != null) {
                attributeMap.put(ATTR_OBSERVATION_TIME,
                        AttributeValue.builder()
                                .s(weatherData.getObservationTime().toString())
                                .build());
            }

            // Store the data type for easier querying (even though it's in the JSON too)
            attributeMap.put(ATTR_DATA_TYPE,
                    AttributeValue.builder().s(weatherData.getDataType()).build());

            // Serialize the entire object to JSON and store it
            // This is similar to storing a JSONB column in PostgreSQL
            // Jackson will automatically include the "dataType" discriminator
            String json = objectMapper.writeValueAsString(weatherData);
            attributeMap.put(ATTR_DATA_JSON,
                    AttributeValue.builder().s(json).build());

            logger.debug("Serialized {} to DynamoDB attributes", weatherData.getClass().getSimpleName());
            return attributeMap;

        } catch (JsonProcessingException e) {
            throw new WeatherDataMappingException("Failed to serialize weather data to JSON", e);
        }
    }

    /**
     * Converts a DynamoDB attribute map back to a WeatherData object.
     * <p>
     * This is like a custom SQL SELECT result mapper - it takes the raw data from
     * DynamoDB and reconstructs your Java object, including determining which
     * concrete subclass to instantiate based on the type discriminator.
     * <p>
     * CRITICAL FIX: Now handles null and empty attribute maps gracefully by returning null
     * instead of throwing an exception. This allows the repository layer to return
     * Optional.empty() when items aren't found.
     * <p>
     * How Jackson Polymorphism Works:
     * 1. JSON contains "dataType": "METAR" or "TAF" or "NOAA"
     * 2. Jackson reads WeatherData's @JsonTypeInfo annotation
     * 3. Jackson checks @JsonSubTypes for matching name
     * 4. Jackson instantiates the correct class (NoaaMetarData, NoaaTafData, etc.)
     * 5. Jackson populates all fields from JSON
     *
     * @param attributeMap the DynamoDB attribute map
     * @return the deserialized WeatherData object, or null if attributeMap is null/empty
     * @throws RuntimeException if deserialization fails for a non-empty map
     */
    public WeatherData fromAttributeMap(Map<String, AttributeValue> attributeMap) {
        // CRITICAL FIX: Handle null and empty maps gracefully
        // This is the fix for: shouldReturnEmptyWhenItemNotFound test
        if (attributeMap == null || attributeMap.isEmpty()) {
            logger.debug("Received null or empty attribute map, returning null");
            return null;
        }

        try {
            // Extract the JSON representation
            AttributeValue dataJsonAttr = attributeMap.get(ATTR_DATA_JSON);
            if (dataJsonAttr == null || dataJsonAttr.s() == null) {
                logger.warn("Missing or null dataJson attribute in map");
                return null;
            }

            String json = dataJsonAttr.s();

            // Jackson will use the "dataType" property in the JSON to determine
            // which concrete class to instantiate (NoaaMetarData, NoaaTafData, etc.)
            // This is similar to discriminator-based polymorphism in JPA/Hibernate
            //
            // Example JSON flow:
            // {"dataType":"METAR",...} → Jackson sees @JsonSubTypes → Creates NoaaMetarData
            // {"dataType":"TAF",...} → Jackson sees @JsonSubTypes → Creates NoaaTafData
            // {"dataType":"NOAA",...} → Jackson sees @JsonSubTypes → Creates NoaaWeatherData
            WeatherData weatherData = objectMapper.readValue(json, WeatherData.class);

            logger.debug("Deserialized {} from DynamoDB attributes",
                    weatherData.getClass().getSimpleName());
            return weatherData;

        } catch (JsonProcessingException e) {
            throw new WeatherDataMappingException("Failed to deserialize weather data from JSON", e);
        }
    }

    /**
     * Creates a primary key map for querying DynamoDB.
     * <p>
     * This is like building the WHERE clause for a SQL query that uses a composite key.
     *
     * @param stationId the partition key value
     * @param observationTime the sort key value
     * @return a map containing the primary key attributes
     */
    public Map<String, AttributeValue> createPrimaryKey(String stationId, Instant observationTime) {
        Map<String, AttributeValue> key = new HashMap<>();

        key.put(ATTR_STATION_ID,
                AttributeValue.builder().s(stationId).build());
        key.put(ATTR_OBSERVATION_TIME,
                AttributeValue.builder().s(observationTime.toString()).build());

        return key;
    }

    /**
     * Extracts the station ID from an attribute map.
     *
     * @param attributeMap the attribute map
     * @return the station ID, or null if not present
     */
    public String extractStationId(Map<String, AttributeValue> attributeMap) {
        if (attributeMap == null) {
            return null;
        }
        AttributeValue value = attributeMap.get(ATTR_STATION_ID);
        return value != null ? value.s() : null;
    }

    /**
     * Extracts the observation time from an attribute map.
     *
     * @param attributeMap the attribute map
     * @return the observation time, or null if not present
     */
    public Instant extractObservationTime(Map<String, AttributeValue> attributeMap) {
        if (attributeMap == null) {
            return null;
        }
        AttributeValue value = attributeMap.get(ATTR_OBSERVATION_TIME);
        return value != null ? Instant.parse(value.s()) : null;
    }
}
