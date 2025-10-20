/*
 * Copyright 2025 bdeveloper.
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

/**
 * Represents the Lambda Architecture processing layer.
 * 
 * Lambda Architecture Pattern:
 * ┌─────────────┐
 * │ Speed Layer │ ──> Real-time, low-latency (S3, DynamoDB)
 * └─────────────┘
 *        │
 *        ▼
 * ┌─────────────┐
 * │ Batch Layer │ ──> Historical, high-volume (Snowflake)
 * └─────────────┘
 *        │
 *        ▼
 * ┌──────────────┐
 * │ Serving Layer│ ──> Unified queries (merge speed + batch)
 * └──────────────┘
 * 
 * @author bclasky1539
 *
 */
public enum ProcessingLayer {
    
    /**
     * Speed Layer: Real-time data processing
     * 
     * Characteristics:
     * - Low latency (seconds)
     * - Recent data only (last 24-48 hours)
     * - Stored in S3 and DynamoDB
     * - Accepts some data loss for speed
     * - Example: Current METAR observations
     */
    SPEED_LAYER("Speed Layer", "Real-time processing", 24),
    
    /**
     * Batch Layer: Historical data processing
     * 
     * Characteristics:
     * - High latency (hours/days)
     * - All historical data
     * - Stored in Snowflake data warehouse
     * - Complete accuracy, no data loss
     * - Example: 10 years of weather history
     */
    BATCH_LAYER("Batch Layer", "Historical processing", Integer.MAX_VALUE),
    
    /**
     * Serving Layer: Merged views
     * 
     * Characteristics:
     * - Combines speed and batch layers
     * - Provides unified query interface
     * - Resolves conflicts between layers
     * - Example: Query merging recent + historical data
     */
    SERVING_LAYER("Serving Layer", "Unified queries", Integer.MAX_VALUE),
    
    /**
     * Raw/Unprocessed: Data that hasn't been assigned to a layer yet
     */
    RAW("Raw Layer", "Unprocessed data", 0);
    
    private final String displayName;
    private final String description;
    private final int retentionHours;
    
    ProcessingLayer(String displayName, String description, int retentionHours) {
        this.displayName = displayName;
        this.description = description;
        this.retentionHours = retentionHours;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getRetentionHours() {
        return retentionHours;
    }
    
    /**
     * Check if this layer is real-time focused
     * @return 
     */
    public boolean isRealTime() {
        return this == SPEED_LAYER;
    }
    
    /**
     * Check if this layer stores historical data
     * @return 
     */
    public boolean isHistorical() {
        return this == BATCH_LAYER || this == SERVING_LAYER;
    }
}
