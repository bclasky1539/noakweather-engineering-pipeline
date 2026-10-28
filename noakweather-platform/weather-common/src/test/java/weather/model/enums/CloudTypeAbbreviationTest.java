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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudTypeAbbreviation}: the shared cloud type vocabulary (#140).
 */
class CloudTypeAbbreviationTest {

    @Test
    void testConstantsPinnedInOrder() {
        assertThat(Arrays.stream(CloudTypeAbbreviation.values()).map(Enum::name))
                .containsExactly("CU", "TCU", "CB", "CF", "ST", "SC", "SF", "NS",
                        "AS", "AC", "CS", "CC", "CI", "ACC", "ACSL");
    }

    @Test
    void testValidCodesDisplay() {
        assertThat(CloudTypeAbbreviation.validCodesDisplay())
                .isEqualTo("CU, TCU, CB, CF, ST, SC, SF, NS, AS, AC, CS, CC, CI, ACC, ACSL");
    }

    @ParameterizedTest
    @EnumSource(CloudTypeAbbreviation.class)
    void testIsValid_AllConstants(CloudTypeAbbreviation type) {
        assertThat(CloudTypeAbbreviation.isValid(type.name())).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "XX", "cb", "Acc", "CBX", "C", "ACCSL", " CB"})
    void testIsValid_RejectsUnknownOrUnnormalized(String code) {
        assertThat(CloudTypeAbbreviation.isValid(code)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "CU, Cumulus",
            "TCU, Towering Cumulus",
            "CB, Cumulonimbus",
            "CF, Cumuliform",
            "ST, Stratus",
            "SC, Stratocumulus",
            "SF, Stratiform",
            "NS, Nimbostratus",
            "AS, Altostratus",
            "AC, Altocumulus",
            "CS, Cirrostratus",
            "CC, Cirrocumulus",
            "CI, Cirrus",
            "ACC, Altocumulus Castellanus",
            "ACSL, Altocumulus Standing Lenticular Cloud"
    })
    void testDescriptions(String code, String description) {
        assertThat(CloudTypeAbbreviation.valueOf(code).getDescription()).isEqualTo(description);
    }

    @Test
    void testFind() {
        assertThat(CloudTypeAbbreviation.find("ACC")).contains(CloudTypeAbbreviation.ACC);
        assertThat(CloudTypeAbbreviation.find("XX")).isEmpty();
        assertThat(CloudTypeAbbreviation.find(null)).isEmpty();
    }
}
