/*
 * NoakWeather Engineering Pipeline(TM) is a multi-source weather data engineering platform
 * Copyright (C) 2025 bclasky1539
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
package weather.processing.parser.noaa;

import java.util.regex.Matcher;

/**
 * Wrapper for lightning pattern matching that provides individual type captures.
 * 
 * The underlying regex captures all lightning types (IC, CC, CG, CA, CW) as a single
 * string for stack-safety. This wrapper provides virtual capture groups for each type,
 * making it compatible with code expecting individual captures.
 * 
 * Usage:
 * <pre>
 * LightningMatcher matcher = new LightningMatcher("OCNL LTGICCCCG VC N-NE");
 * if (matcher.find()) {
 *     String typeIC = matcher.group("typeic");  // "IC" if present, null otherwise
 *     String freq = matcher.group("freq");      // "OCNL"
 *     String loc = matcher.group("loc");        // "VC"
 * }
 * </pre>
 * 
 * @author bclasky1539
 * 
 */
public class LightningMatcher {
    
    private final Matcher matcher;
    private String types;
    
    /**
     * Create a lightning matcher for the given input.
     * 
     * @param input The text to match against
     */
    public LightningMatcher(String input) {
        this.matcher = RegExprConst.LIGHTNING_PATTERN.matcher(input);
    }
    
    /**
     * Attempts to find the next match in the input.
     * 
     * @return true if a match was found
     */
    public boolean find() {
        boolean found = matcher.find();
        if (found) {
            this.types = matcher.group("types");
        }
        return found;
    }
    
    /**
     * Returns the captured group for the given name.
     * 
     * For lightning type groups (typeic, typecc, typecg, typeca, typecw),
     * returns the type code if present in the match, null otherwise.
     * 
     * For other groups (freq, loc, dir, dir2), delegates to the underlying matcher.
     * 
     * @param name The capture group name
     * @return The captured text, or null if not present
     */
    public String group(String name) {
        // Handle virtual lightning type capture groups
        if ("typeic".equals(name)) {
            return hasType("IC") ? "IC" : null;
        }
        if ("typecc".equals(name)) {
            return hasType("CC") ? "CC" : null;
        }
        if ("typecg".equals(name)) {
            return hasType("CG") ? "CG" : null;
        }
        if ("typeca".equals(name)) {
            return hasType("CA") ? "CA" : null;
        }
        if ("typecw".equals(name)) {
            return hasType("CW") ? "CW" : null;
        }
        
        // Delegate to actual matcher for real capture groups
        return matcher.group(name);
    }
    
    /**
     * Returns the entire matched substring.
     * 
     * @param group Group number (0 for entire match)
     * @return The matched substring
     */
    public String group(int group) {
        return matcher.group(group);
    }
    
    /**
     * Returns the entire matched substring.
     * 
     * @return The matched substring
     */
    public String group() {
        return matcher.group();
    }
    
    /**
     * Replaces the first occurrence of the matched pattern.
     * 
     * @param replacement The replacement string
     * @return The input string with the first match replaced
     */
    public String replaceFirst(String replacement) {
        return matcher.replaceFirst(replacement);
    }
    
    /**
     * Check if a specific lightning type is present.
     * 
     * @param type The two-letter type code (IC, CC, CG, CA, CW)
     * @return true if the type is present in the match
     */
    public boolean hasType(String type) {
        return types != null && types.contains(type);
    }
    
    /**
     * Get all lightning types as a single string.
     * 
     * @return The types string (e.g., "ICCCCG"), or null if no types
     */
    public String getTypesString() {
        return types;
    }
    
    /**
     * Check if any lightning types were captured.
     * 
     * @return true if at least one type is present
     */
    public boolean hasAnyTypes() {
        return types != null && !types.isEmpty();
    }
}
