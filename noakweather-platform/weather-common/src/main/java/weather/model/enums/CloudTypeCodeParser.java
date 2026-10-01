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
package weather.model.enums;

/**
 * Shared digit-to-constant parsing logic for the three WMO predominant
 * cloud type enums ({@link LowCloudType}, {@link MiddleCloudType},
 * {@link HighCloudType}). Each of those enums transmits as a single digit
 * "0"-"9", or "/" for an obscured layer, and relies on its own declaration
 * order matching the WMO digit order (its first constant at ordinal 0
 * through its last numbered constant at ordinal 9); the obscured constant
 * is handled separately since "/" has no corresponding digit.
 * <p>
 * Package-private: this is implementation detail for the three cloud-type
 * enums, not a general-purpose utility.
 *
 * @author bclasky1539
 */
final class CloudTypeCodeParser {

    private CloudTypeCodeParser() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Parses a transmitted digit (or "/" for obscured) into the matching
     * enum constant, using the enum's declaration order as the digit
     * mapping.
     *
     * @param code          a single digit "0"-"9", or "/" for obscured-above-overcast
     * @param enumType      the enum class to resolve against
     * @param obscuredValue the constant to return for "/"
     * @param label         a human-readable label for this enum, used in error messages
     * @param <T>           the enum type
     * @return the corresponding enum constant
     * @throws IllegalArgumentException if code is not a recognized value
     */
    static <T extends Enum<T>> T fromCode(String code, Class<T> enumType, T obscuredValue, String label) {
        if (code == null) {
            throw new IllegalArgumentException(label + " code cannot be null");
        }
        if ("/".equals(code)) {
            return obscuredValue;
        }
        if (code.length() == 1 && Character.isDigit(code.charAt(0))) {
            int digit = code.charAt(0) - '0';
            T[] values = enumType.getEnumConstants();
            if (digit < values.length) {
                return values[digit];
            }
        }
        throw new IllegalArgumentException("Invalid " + label + " code: " + code);
    }
}
