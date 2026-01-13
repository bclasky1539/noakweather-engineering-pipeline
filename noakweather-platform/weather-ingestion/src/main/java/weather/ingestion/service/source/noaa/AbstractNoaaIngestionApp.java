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

import weather.ingestion.config.NoaaConfiguration;
import weather.ingestion.service.S3UploadService;
import weather.model.WeatherData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ScheduledFuture;

/**
 * Abstract base class for NOAA weather data ingestion CLI applications.
 * <p>
 * Provides common CLI functionality for METAR, TAF, and any future NOAA data types.
 * Subclasses only need to provide the orchestrator and data type name.
 * <p>
 * This eliminates code duplication between MetarIngestionApp and TafIngestionApp.
 * <p>
 * Note: System.out is used intentionally for user-facing CLI output.
 * Logger is used for application lifecycle and error events.
 *
 * @author bclasky1539
 *
 */
@SuppressWarnings("java:S106") // System.out acceptable for CLI user interaction
public abstract class AbstractNoaaIngestionApp {

    private static final Logger logger = LogManager.getLogger(AbstractNoaaIngestionApp.class);
    private static final String USAGE_PREFIX = "  java ";

    protected static final String S3_BUCKET = System.getenv().getOrDefault("S3_BUCKET", "noakweather-data");
    protected static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    protected abstract AbstractNoaaIngestionOrchestrator createOrchestrator(
            NoaaAviationWeatherClient noaaClient,
            S3UploadService s3Service,
            int maxConcurrentFetches
    );

    protected abstract String getDataType();

    protected String getAdditionalUsageNotes() {
        return null;
    }

    protected void run(String[] args) {
        logger.info("=== {} Ingestion Application Started ===", getDataType());

        if (args.length == 0) {
            printUsage();
            return;
        }

        try {
            NoaaConfiguration config = new NoaaConfiguration();
            NoaaAviationWeatherClient noaaClient = new NoaaAviationWeatherClient(config);
            S3UploadService s3Service = new S3UploadService(S3_BUCKET, AWS_REGION);
            AbstractNoaaIngestionOrchestrator orchestrator = createOrchestrator(
                    noaaClient, s3Service, 10);

            if (!orchestrator.isHealthy()) {
                logger.error("System health check failed - S3 bucket not accessible");
                System.err.println("ERROR: Cannot access S3 bucket: " + S3_BUCKET);
                return;
            }

            logger.info("System health check passed");

            if (args[0].equals("--interactive")) {
                runInteractiveMode(orchestrator);
            } else if (args[0].equals("--schedule")) {
                runScheduledMode(orchestrator, args);
            } else {
                runBatchMode(orchestrator, args);
            }

            orchestrator.shutdown();
            logger.info("=== {} Ingestion Application Completed ===", getDataType());

        } catch (Exception e) {
            logger.error("Application error: {}", e.getMessage(), e);
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private void runBatchMode(AbstractNoaaIngestionOrchestrator orchestrator, String[] args) {
        List<String> stationIds = Arrays.asList(args);

        System.out.println("\n=== Ingesting " + getDataType() + " data for " +
                stationIds.size() + " stations ===");
        System.out.println("Stations: " + stationIds);
        System.out.println();

        AbstractNoaaIngestionOrchestrator.IngestionResult result =
                orchestrator.ingestStationsSequential(stationIds);

        displayIngestionResults(result);
    }

    private void displayIngestionResults(AbstractNoaaIngestionOrchestrator.IngestionResult result) {
        System.out.println("\n=== Ingestion Results ===");
        System.out.println("Total stations: " + (result.getSuccessCount() + result.getFailureCount()));
        System.out.println("Successful: " + result.getSuccessCount());
        System.out.println("Failed: " + result.getFailureCount());
        System.out.println("Success rate: " + String.format("%.1f%%", result.getSuccessRate() * 100));
        System.out.println("Duration: " + result.getDuration().toMillis() + "ms");

        if (!result.getSuccessfulStations().isEmpty()) {
            System.out.println("\nSuccessfully ingested:");
            for (WeatherData data : result.getSuccessfulData()) {
                String s3Key = (String) data.getMetadata().get("s3_key");
                System.out.println("  " + data.getStationId() + " -> s3://" + S3_BUCKET + "/" + s3Key);
            }
        }

        if (!result.getFailures().isEmpty()) {
            System.out.println("\nFailed stations:");
            result.getFailures().forEach((stationId, exception) ->
                    System.out.println("  " + stationId + ": " + exception.getMessage())
            );
        }
    }

    private void runScheduledMode(AbstractNoaaIngestionOrchestrator orchestrator, String[] args) {
        if (args.length < 3) {
            System.err.println("ERROR: --schedule requires interval (minutes) and station IDs");
            printUsage();
            return;
        }

        int intervalMinutes;
        try {
            intervalMinutes = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid interval: " + args[1]);
            return;
        }

        List<String> stationIds = Arrays.asList(args).subList(2, args.length);

        System.out.println("\n=== Starting Scheduled " + getDataType() + " Ingestion ===");
        System.out.println("Stations: " + stationIds);
        System.out.println("Interval: " + intervalMinutes + " minutes");

        String additionalNotes = getAdditionalUsageNotes();
        if (additionalNotes != null) {
            System.out.println(additionalNotes);
        }

        System.out.println("Press Ctrl+C to stop");
        System.out.println();

        ScheduledFuture<Void> scheduledTask = orchestrator.schedulePeriodicIngestion(
                stationIds, intervalMinutes);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping scheduled ingestion...");
            scheduledTask.cancel(false);
        }));

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs interactive mode with command loop.
     * Refactored to reduce cognitive complexity by extracting command handlers.
     */
    private void runInteractiveMode(AbstractNoaaIngestionOrchestrator orchestrator) {
        printInteractiveHeader();

        try (Scanner scanner = new Scanner(System.in)) {
            boolean continueRunning;
            do {
                continueRunning = processInteractiveCommand(scanner, orchestrator);
            } while (continueRunning);
        }
    }

    private void printInteractiveHeader() {
        System.out.println("\n=== Interactive " + getDataType() + " Ingestion Mode ===");
        System.out.println("Commands:");
        System.out.println("  ingest <STATION_ID> [STATION_ID...] - Ingest " + getDataType() + " for station(s)");
        System.out.println("  metrics                               - Show ingestion metrics");
        System.out.println("  health                                - Check system health");
        System.out.println("  quit                                  - Exit application");
        System.out.println();
    }

    private boolean processInteractiveCommand(Scanner scanner,
                                              AbstractNoaaIngestionOrchestrator orchestrator) {
        System.out.print("> ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return true;
        }

        String[] parts = input.split("\\s+");

        // Handle empty array or empty first element
        if (parts.length == 0 || parts[0].isEmpty()) {
            return true;
        }

        String command = parts[0].toLowerCase();

        return switch (command) {
            case "ingest" -> {
                handleIngestCommand(parts, orchestrator);
                yield true;
            }
            case "metrics" -> {
                handleMetricsCommand(orchestrator);
                yield true;
            }
            case "health" -> {
                handleHealthCommand(orchestrator);
                yield true;
            }
            case "quit", "exit" -> {
                System.out.println("Exiting...");
                yield false;
            }
            default -> {
                System.out.println("Unknown command: " + command);
                yield true;
            }
        };
    }

    private void handleIngestCommand(String[] parts, AbstractNoaaIngestionOrchestrator orchestrator) {
        if (parts.length < 2) {
            System.out.println("ERROR: Please specify station ID(s)");
            return;
        }

        List<String> stationIds = Arrays.asList(parts).subList(1, parts.length);
        AbstractNoaaIngestionOrchestrator.IngestionResult result =
                orchestrator.ingestStationsSequential(stationIds);

        System.out.println("Results: " + result.getSuccessCount() + " succeeded, " +
                result.getFailureCount() + " failed in " +
                result.getDuration().toMillis() + "ms");

        if (!result.getFailures().isEmpty()) {
            System.out.println("Failures:");
            result.getFailures().forEach((stationId, exception) ->
                    System.out.println("  " + stationId + ": " + exception.getMessage())
            );
        }
    }

    private void handleMetricsCommand(AbstractNoaaIngestionOrchestrator orchestrator) {
        Map<String, Object> metrics = orchestrator.getMetrics();
        System.out.println("Ingestion Metrics:");
        metrics.forEach((key, value) ->
                System.out.println("  " + key + ": " + value)
        );
    }

    private void handleHealthCommand(AbstractNoaaIngestionOrchestrator orchestrator) {
        boolean healthy = orchestrator.isHealthy();
        System.out.println("System health: " + (healthy ? "HEALTHY" : "UNHEALTHY"));
    }

    private void printUsage() {
        String dataType = getDataType();
        String className = getClass().getSimpleName();

        System.out.println("\n" + dataType + " Ingestion Application");
        System.out.println("=".repeat(dataType.length() + 23));
        System.out.println();
        System.out.println("Usage:");
        System.out.println(USAGE_PREFIX + className + " <STATION_ID> [STATION_ID...]");
        System.out.println(USAGE_PREFIX + className + " --schedule <INTERVAL_MINUTES> <STATION_ID> [STATION_ID...]");
        System.out.println(USAGE_PREFIX + className + " --interactive");
        System.out.println();
        System.out.println("Examples:");
        System.out.println(USAGE_PREFIX + className + " KCLT");
        System.out.println(USAGE_PREFIX + className + " KCLT KJFK KLAX KORD");
        System.out.println(USAGE_PREFIX + className + " --schedule 10 KCLT KJFK");
        System.out.println(USAGE_PREFIX + className + " --interactive");
        System.out.println();

        String additionalNotes = getAdditionalUsageNotes();
        if (additionalNotes != null) {
            System.out.println(additionalNotes);
            System.out.println();
        }

        System.out.println("Environment Variables:");
        System.out.println("  AWS_REGION - AWS region for S3 (default: us-east-1)");
        System.out.println("  S3_BUCKET  - S3 bucket name (default: noakweather-data)");
        System.out.println();
    }
}
