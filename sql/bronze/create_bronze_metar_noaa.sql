--DROP TABLE IF EXISTS noakweather.bronze_metar_noaa;

CREATE TABLE IF NOT EXISTS noakweather.bronze_metar_noaa (
    id STRING COMMENT 'Unique observation ID (UUID)',
    dataType STRING COMMENT 'Data type identifier',
    source STRING COMMENT 'Data source',
    processingLayer STRING COMMENT 'Processing layer identifier',
    stationId STRING COMMENT 'ICAO station identifier',
    observationTime BIGINT COMMENT 'Unix timestamp (seconds)',
    ingestionTime DOUBLE COMMENT 'Pipeline ingestion timestamp',
    rawData STRING COMMENT 'Original METAR text from source',
    rawText STRING COMMENT 'Parsed METAR text',
    unparsedMainBody STRING COMMENT 'Tokens not recognized by parser',
    metadata STRUCT<
        fetch_timestamp:STRING,
        validated:STRING,
        processor_version:STRING,
        parser_version:STRING,
        full_response:STRING,
        storage_format:STRING,
        parsed:STRING,
        processor:STRING,
        validation_timestamp:STRING
    >,
    reportType STRING,
    reportModifier STRING,
    conditions STRUCT<
        wind:STRUCT<
            directionDegrees:INT,
            speedValue:INT,
            gustValue:INT,
            variabilityFrom:INT,
            variabilityTo:INT,
            unit:STRING,
            speedKnots:INT,
            cardinalDirection:STRING,
            summary:STRING,
            calm:BOOLEAN,
            strongWind:BOOLEAN,
            speedMps:INT,
            speedKmh:DOUBLE,
            beaufortScale:INT,
            gale:BOOLEAN,
            variable:BOOLEAN
        >,
        visibility:STRUCT<
            distanceValue:DOUBLE,
            unit:STRING,
            lessThan:BOOLEAN,
            greaterThan:BOOLEAN,
            specialCondition:BOOLEAN,
            summary:STRING,
            cavok:BOOLEAN,
            lowVisibility:BOOLEAN,
            vfr:BOOLEAN,
            ifr:BOOLEAN,
            unlimited:BOOLEAN
        >,
        temperature:STRUCT<
            celsius:DOUBLE,
            dewpointCelsius:DOUBLE,
            relativeHumidity:DOUBLE,
            heatIndex:DOUBLE,
            spread:DOUBLE,
            freezing:BOOLEAN,
            summary:STRING,
            maxCelsius:DOUBLE,
            minCelsius:DOUBLE,
            belowFreezing:BOOLEAN,
            aboveFreezing:BOOLEAN,
            fogLikely:BOOLEAN,
            icingLikely:BOOLEAN,
            veryCold:BOOLEAN,
            veryHot:BOOLEAN,
            currentObservation:BOOLEAN,
            forecast:BOOLEAN
        >,
        pressure:STRUCT<
            value:DOUBLE,
            unit:STRING,
            pressureAltitudeFeet:DOUBLE,
            formattedValue:STRING,
            deviationFromStandard:DOUBLE,
            summary:STRING,
            belowStandard:BOOLEAN,
            aboveStandard:BOOLEAN,
            lowPressure:BOOLEAN,
            highPressure:BOOLEAN,
            extremelyLow:BOOLEAN,
            extremelyHigh:BOOLEAN
        >,
        skyConditions:ARRAY<STRUCT<
            coverage:STRING,
            heightFeet:INT,
            heightMeters:INT,
            cloudType:STRING,
            ceiling:BOOLEAN,
            cumulonimbus:BOOLEAN,
            toweringCumulus:BOOLEAN,
            convective:BOOLEAN,
            summary:STRING,
            clear:BOOLEAN
        >>,
        presentWeather:ARRAY<STRUCT<
            intensity:STRING,
            descriptor:STRING,
            precipitation:STRING,
            obscuration:STRING,
            other:STRING,
            rawCode:STRING,
            description:STRING,
            thunderstorm:BOOLEAN,
            freezing:BOOLEAN,
            light:BOOLEAN,
            heavy:BOOLEAN,
            vicinity:BOOLEAN,
            showers:BOOLEAN,
            noSignificantWeather:BOOLEAN,
            intensityDescription:STRING
        >>,
        ceilingFeet:INT,
        likelyVMC:BOOLEAN,
        likelyIMC:BOOLEAN,
        clearAndCalm:BOOLEAN
    >,
    runwayVisualRange ARRAY<STRUCT<
        runwayId:STRING,
        visualRangeFeet:INT,
        variableLow:INT,
        variableHigh:INT,
        trend:STRING
    >>,
    remarks STRUCT<
        automatedStationType:STRING,
        seaLevelPressure:STRUCT<
            value:DOUBLE,
            unit:STRING,
            summary:STRING
        >,
        preciseTemperature:STRUCT<
            celsius:DOUBLE,
            dewpointCelsius:DOUBLE
        >,
        peakWind:STRUCT<
            directionDegrees:INT,
            speedKnots:INT,
            hour:INT,
            minute:INT
        >,
        windShift:STRUCT<
            hour:INT,
            minute:INT,
            frontalPassage:BOOLEAN
        >,
        maintenanceRequired:BOOLEAN,
        freeText:STRING
    >,
    latitude DOUBLE,
    longitude DOUBLE,
    elevationFeet INT,
    peakWind STRUCT<
        directionDegrees:INT,
        speedKnots:INT,
        hour:INT,
        minute:INT
    >,
    seaLevelPressure DOUBLE,
    automatedStation STRING,
    noSignificantChange BOOLEAN,
    automated BOOLEAN,
    summary STRING,
    `current` BOOLEAN,
    minimumRvrFeet INT,
    temperature STRUCT<
        celsius:DOUBLE,
        dewpointCelsius:DOUBLE,
        relativeHumidity:DOUBLE,
        summary:STRING
    >,
    visibility STRUCT<
        distanceValue:DOUBLE,
        unit:STRING,
        vfr:BOOLEAN,
        summary:STRING
    >,
    pressure STRUCT<
        value:DOUBLE,
        unit:STRING,
        summary:STRING
    >,
    wind STRUCT<
        directionDegrees:INT,
        speedKnots:INT,
        cardinalDirection:STRING,
        summary:STRING
    >,
    skyConditions ARRAY<STRUCT<
        coverage:STRING,
        heightFeet:INT,
        cloudType:STRING,
        ceiling:BOOLEAN
    >>,
    presentWeather ARRAY<STRUCT<
        rawCode:STRING,
        description:STRING
    >>,
    ceilingFeet INT
)
COMMENT 'Bronze: Raw NOAA METAR observations (immutable source of truth)'
PARTITIONED BY (
    year STRING,
    month STRING,
    day STRING
)
ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'
WITH SERDEPROPERTIES (
    'ignore.malformed.json' = 'true',
    'case.insensitive' = 'true'
)
STORED AS INPUTFORMAT 'org.apache.hadoop.mapred.TextInputFormat'
OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION 's3://noakweather-data/bronze/speed-layer/noaa/metar/'
TBLPROPERTIES (
    'projection.enabled' = 'true',
    'projection.year.type' = 'integer',
    'projection.year.range' = '2020,2030',
    'projection.year.digits' = '4',
    'projection.month.type' = 'integer',
    'projection.month.range' = '01,12',
    'projection.month.digits' = '2',
    'projection.day.type' = 'integer',
    'projection.day.range' = '01,31',
    'projection.day.digits' = '2',
    'storage.location.template' = 's3://noakweather-data/bronze/speed-layer/noaa/metar/${year}/${month}/${day}',
    'classification' = 'json'
);
