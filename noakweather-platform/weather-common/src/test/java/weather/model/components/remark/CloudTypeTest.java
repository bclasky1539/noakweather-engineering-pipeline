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
package weather.model.components.remark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for CloudType value object.
 *
 * @author bclasky1539
 *
 */
class CloudTypeTest {

    // ==================== Constructor and Validation Tests ====================

    @ParameterizedTest
    @ValueSource(strings = {"CU", "TCU", "CF", "ST", "SC", "SF", "NS", "AS", "AC", "CS", "CC", "CI"})
    void testValidCloudTypes(String cloudType) {
        CloudType cloud = new CloudType(cloudType, null, null, null, null);

        assertThat(cloud.cloudType()).isEqualTo(cloudType);
        assertThat(cloud.oktas()).isNull();
        assertThat(cloud.intensity()).isNull();
        assertThat(cloud.location()).isNull();
        assertThat(cloud.movementDirection()).isNull();
    }

    @Test
    void testCloudTypeNormalization() {
        // Lowercase should be normalized to uppercase
        CloudType cloud1 = new CloudType("cu", null, null, null, null);
        assertThat(cloud1.cloudType()).isEqualTo("CU");

        // Mixed case should be normalized to uppercase
        CloudType cloud2 = new CloudType("Sc", null, null, null, null);
        assertThat(cloud2.cloudType()).isEqualTo("SC");

        // With whitespace should be trimmed and normalized
        CloudType cloud3 = new CloudType("  ac  ", null, null, null, null);
        assertThat(cloud3.cloudType()).isEqualTo("AC");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void testCloudTypeRequired(String invalidCloudType) {
        assertThatThrownBy(() -> new CloudType(invalidCloudType, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cloud type cannot be null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "FOG", "ABC", "C", "CUCU", "12"})
    void testInvalidCloudType(String invalidCloudType) {
        assertThatThrownBy(() -> new CloudType(invalidCloudType, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cloud type");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    void testValidOktas(int oktas) {
        CloudType cloud = new CloudType("SC", oktas, null, null, null);

        assertThat(cloud.oktas()).isEqualTo(oktas);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 9, 10, 100})
    void testInvalidOktas(int invalidOktas) {
        assertThatThrownBy(() -> new CloudType("SC", invalidOktas, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Oktas must be between 1 and 8");
    }

    @Test
    void testNullOktasAllowed() {
        CloudType cloud = new CloudType("CI", null, null, null, null);
        assertThat(cloud.oktas()).isNull();
    }

    @Test
    void testValidIntensity() {
        CloudType cloud = new CloudType("CU", null, "MDT", null, null);

        assertThat(cloud.intensity()).isEqualTo("MDT");
    }

    @Test
    void testIntensityNormalization() {
        // Lowercase should be normalized to uppercase
        CloudType cloud1 = new CloudType("CU", null, "mdt", null, null);
        assertThat(cloud1.intensity()).isEqualTo("MDT");

        // With whitespace should be trimmed and normalized
        CloudType cloud2 = new CloudType("CU", null, "  MDT  ", null, null);
        assertThat(cloud2.intensity()).isEqualTo("MDT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIGHT", "HEAVY", "XXX", "MODERATE"})
    void testInvalidIntensity(String invalidIntensity) {
        assertThatThrownBy(() -> new CloudType("CU", null, invalidIntensity, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid intensity");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void testBlankIntensityBecomesNull(String blankIntensity) {
        CloudType cloud = new CloudType("CU", null, blankIntensity, null, null);
        assertThat(cloud.intensity()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"OHD", "OHD-ALQDS", "ALQDS", "TR"})
    void testValidLocation(String location) {
        CloudType cloud = new CloudType("AC", null, null, location, null);

        assertThat(cloud.location()).isEqualTo(location);
    }

    @Test
    void testLocationNormalization() {
        // Lowercase should be normalized to uppercase
        CloudType cloud1 = new CloudType("AC", null, null, "ohd", null);
        assertThat(cloud1.location()).isEqualTo("OHD");

        // With whitespace should be trimmed and normalized
        CloudType cloud2 = new CloudType("AC", null, null, "  TR  ", null);
        assertThat(cloud2.location()).isEqualTo("TR");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DSNT", "VC", "XXX", "OVERHEAD"})
    void testInvalidLocation(String invalidLocation) {
        assertThatThrownBy(() -> new CloudType("AC", null, null, invalidLocation, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid location");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void testBlankLocationBecomesNull(String blankLocation) {
        CloudType cloud = new CloudType("AC", null, null, blankLocation, null);
        assertThat(cloud.location()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"N", "S", "E", "W", "NE", "NW", "SE", "SW"})
    void testValidMovementDirection(String direction) {
        CloudType cloud = new CloudType("CI", null, null, null, direction);

        assertThat(cloud.movementDirection()).isEqualTo(direction);
    }

    @Test
    void testMovementDirectionNormalization() {
        // Lowercase should be normalized to uppercase
        CloudType cloud1 = new CloudType("CI", null, null, null, "ne");
        assertThat(cloud1.movementDirection()).isEqualTo("NE");

        // With whitespace should be trimmed and normalized
        CloudType cloud2 = new CloudType("CI", null, null, null, "  SW  ");
        assertThat(cloud2.movementDirection()).isEqualTo("SW");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NORTH", "NORTHEAST", "XXX", "NNE", "SSW"})
    void testInvalidMovementDirection(String invalidDirection) {
        assertThatThrownBy(() -> new CloudType("CI", null, null, null, invalidDirection))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid movement direction");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void testBlankMovementDirectionBecomesNull(String blankDirection) {
        CloudType cloud = new CloudType("CI", null, null, null, blankDirection);
        assertThat(cloud.movementDirection()).isNull();
    }

    // ==================== Query Methods Tests ====================

    @Test
    void testHasOktaCoverage() {
        CloudType withOktas = new CloudType("SC", 3, null, null, null);
        CloudType withoutOktas = new CloudType("SC", null, null, null, null);

        assertThat(withOktas.hasOktaCoverage()).isTrue();
        assertThat(withoutOktas.hasOktaCoverage()).isFalse();
    }

    @Test
    void testHasIntensity() {
        CloudType withIntensity = new CloudType("CU", null, "MDT", null, null);
        CloudType withoutIntensity = new CloudType("CU", null, null, null, null);

        assertThat(withIntensity.hasIntensity()).isTrue();
        assertThat(withoutIntensity.hasIntensity()).isFalse();
    }

    @Test
    void testHasLocation() {
        CloudType withLocation = new CloudType("AC", null, null, "OHD", null);
        CloudType withoutLocation = new CloudType("AC", null, null, null, null);

        assertThat(withLocation.hasLocation()).isTrue();
        assertThat(withoutLocation.hasLocation()).isFalse();
    }

    @Test
    void testHasMovement() {
        CloudType withMovement = new CloudType("CI", null, null, null, "NE");
        CloudType withoutMovement = new CloudType("CI", null, null, null, null);

        assertThat(withMovement.hasMovement()).isTrue();
        assertThat(withoutMovement.hasMovement()).isFalse();
    }

    @Test
    void testIsTrace() {
        CloudType trace = new CloudType("SC", null, null, "TR", null);
        CloudType notTrace = new CloudType("SC", null, null, "OHD", null);
        CloudType noLocation = new CloudType("SC", null, null, null, null);

        assertThat(trace.isTrace()).isTrue();
        assertThat(notTrace.isTrace()).isFalse();
        assertThat(noLocation.isTrace()).isFalse();
    }

    @Test
    void testIsOverhead() {
        CloudType ohd = new CloudType("AC", null, null, "OHD", null);
        CloudType ohdAlqds = new CloudType("AC", null, null, "OHD-ALQDS", null);
        CloudType notOverhead = new CloudType("AC", null, null, "TR", null);
        CloudType noLocation = new CloudType("AC", null, null, null, null);

        assertThat(ohd.isOverhead()).isTrue();
        assertThat(ohdAlqds.isOverhead()).isTrue();
        assertThat(notOverhead.isOverhead()).isFalse();
        assertThat(noLocation.isOverhead()).isFalse();
    }

    @Test
    void testIsAllQuadrants() {
        CloudType alqds = new CloudType("AC", null, null, "ALQDS", null);
        CloudType ohdAlqds = new CloudType("AC", null, null, "OHD-ALQDS", null);
        CloudType notAllQuadrants = new CloudType("AC", null, null, "OHD", null);
        CloudType noLocation = new CloudType("AC", null, null, null, null);

        assertThat(alqds.isAllQuadrants()).isTrue();
        assertThat(ohdAlqds.isAllQuadrants()).isTrue();
        assertThat(notAllQuadrants.isAllQuadrants()).isFalse();
        assertThat(noLocation.isAllQuadrants()).isFalse();
    }

    // ==================== Conversion Methods Tests ====================

    @ParameterizedTest
    @CsvSource({
            "CU, Cumulus",
            "CF, Cumuliform",
            "ST, Stratus",
            "SC, Stratocumulus",
            "SF, Stratiform",
            "NS, Nimbostratus",
            "AS, Altostratus",
            "AC, Altocumulus",
            "CS, Cirrostratus",
            "CC, Cirrocumulus",
            "CI, Cirrus"
    })
    void testGetCloudTypeDescription(String code, String description) {
        CloudType cloud = new CloudType(code, null, null, null, null);

        assertThat(cloud.getCloudTypeDescription()).isEqualTo(description);
    }

    @Test
    void testGetOktasFraction() {
        assertThat(new CloudType("SC", 1, null, null, null).getOktasFraction()).isEqualTo(0.125);
        assertThat(new CloudType("SC", 2, null, null, null).getOktasFraction()).isEqualTo(0.25);
        assertThat(new CloudType("SC", 3, null, null, null).getOktasFraction()).isEqualTo(0.375);
        assertThat(new CloudType("SC", 4, null, null, null).getOktasFraction()).isEqualTo(0.5);
        assertThat(new CloudType("SC", 5, null, null, null).getOktasFraction()).isEqualTo(0.625);
        assertThat(new CloudType("SC", 6, null, null, null).getOktasFraction()).isEqualTo(0.75);
        assertThat(new CloudType("SC", 7, null, null, null).getOktasFraction()).isEqualTo(0.875);
        assertThat(new CloudType("SC", 8, null, null, null).getOktasFraction()).isEqualTo(1.0);

        assertThat(new CloudType("SC", null, null, null, null).getOktasFraction()).isNull();
    }

    @Test
    void testGetSummary_SimpleCloudType() {
        CloudType cloud = new CloudType("CI", null, null, null, null);

        assertThat(cloud.getSummary()).isEqualTo("Cirrus");
    }

    @Test
    void testGetSummary_WithOktas() {
        CloudType cloud = new CloudType("SC", 1, null, null, null);

        assertThat(cloud.getSummary()).isEqualTo("Stratocumulus (1/8)");
    }

    @Test
    void testGetSummary_WithTrace() {
        CloudType cloud = new CloudType("SC", null, null, "TR", null);

        assertThat(cloud.getSummary()).isEqualTo("Stratocumulus (tr)");
    }

    @Test
    void testGetSummary_WithIntensityAndLocation() {
        CloudType cloud = new CloudType("CU", null, "MDT", "OHD", null);

        assertThat(cloud.getSummary()).isEqualTo("mdt Cumulus (ohd)");
    }

    @Test
    void testGetSummary_WithOktasAndLocation() {
        CloudType cloud = new CloudType("AC", 8, null, "ALQDS", null);

        assertThat(cloud.getSummary()).isEqualTo("Altocumulus (8/8, alqds)");
    }

    @Test
    void testGetSummary_WithMovement() {
        CloudType cloud = new CloudType("CI", null, null, null, "NE");

        assertThat(cloud.getSummary()).isEqualTo("Cirrus (moving NE)");
    }

    @Test
    void testGetSummary_WithAllFields() {
        CloudType cloud = new CloudType("CU", 4, "MDT", "OHD", "E");

        assertThat(cloud.getSummary()).isEqualTo("mdt Cumulus (4/8, ohd, moving E)");
    }

    @Test
    void testGetSummary_WithOktasAndMovement() {
        CloudType cloud = new CloudType("AC", 3, null, null, "SW");

        assertThat(cloud.getSummary()).isEqualTo("Altocumulus (3/8, moving SW)");
    }

    // ==================== Factory Methods Tests ====================

    @Test
    void testFactoryOf_CloudTypeOnly() {
        CloudType cloud = CloudType.of("SC");

        assertThat(cloud.cloudType()).isEqualTo("SC");
        assertThat(cloud.oktas()).isNull();
        assertThat(cloud.intensity()).isNull();
        assertThat(cloud.location()).isNull();
        assertThat(cloud.movementDirection()).isNull();
    }

    @Test
    void testFactoryOf_WithOktas() {
        CloudType cloud = CloudType.of("AC", 5);

        assertThat(cloud.cloudType()).isEqualTo("AC");
        assertThat(cloud.oktas()).isEqualTo(5);
        assertThat(cloud.intensity()).isNull();
        assertThat(cloud.location()).isNull();
        assertThat(cloud.movementDirection()).isNull();
    }

    @Test
    void testFactoryWithLocation() {
        CloudType cloud = CloudType.withLocation("CI", "TR");

        assertThat(cloud.cloudType()).isEqualTo("CI");
        assertThat(cloud.oktas()).isNull();
        assertThat(cloud.intensity()).isNull();
        assertThat(cloud.location()).isEqualTo("TR");
        assertThat(cloud.movementDirection()).isNull();
    }

    // ==================== Real-World Examples Tests ====================

    @Test
    void testRealWorldExample_SC1() {
        // From METAR: RMK SC1
        CloudType cloud = new CloudType("SC", 1, null, null, null);

        assertThat(cloud.cloudType()).isEqualTo("SC");
        assertThat(cloud.oktas()).isEqualTo(1);
        assertThat(cloud.hasOktaCoverage()).isTrue();
        assertThat(cloud.getSummary()).isEqualTo("Stratocumulus (1/8)");
    }

    @Test
    void testRealWorldExample_SC_TR() {
        // From METAR: RMK SC TR
        CloudType cloud = new CloudType("SC", null, null, "TR", null);

        assertThat(cloud.cloudType()).isEqualTo("SC");
        assertThat(cloud.isTrace()).isTrue();
        assertThat(cloud.getSummary()).isEqualTo("Stratocumulus (tr)");
    }

    @Test
    void testRealWorldExample_TCU4() {
        // From METAR: RMK TCU4AC1AC2
        CloudType cloud = new CloudType("CU", 4, null, null, null);

        assertThat(cloud.cloudType()).isEqualTo("CU");
        assertThat(cloud.oktas()).isEqualTo(4);
        assertThat(cloud.getOktasFraction()).isEqualTo(0.5);
    }

    @Test
    void testRealWorldExample_MDT_CU_OHD() {
        // From METAR: RMK MDT CU OHD
        CloudType cloud = new CloudType("CU", null, "MDT", "OHD", null);

        assertThat(cloud.cloudType()).isEqualTo("CU");
        assertThat(cloud.hasIntensity()).isTrue();
        assertThat(cloud.isOverhead()).isTrue();
        assertThat(cloud.getSummary()).isEqualTo("mdt Cumulus (ohd)");
    }

    @Test
    void testRealWorldExample_AC8() {
        // Full coverage altocumulus
        CloudType cloud = new CloudType("AC", 8, null, null, null);

        assertThat(cloud.cloudType()).isEqualTo("AC");
        assertThat(cloud.oktas()).isEqualTo(8);
        assertThat(cloud.getOktasFraction()).isEqualTo(1.0);
        assertThat(cloud.getSummary()).isEqualTo("Altocumulus (8/8)");
    }

    // ==================== Record Equality Tests ====================

    @Test
    void testEquality() {
        CloudType cloud1 = new CloudType("SC", 1, null, null, null);
        CloudType cloud2 = new CloudType("SC", 1, null, null, null);
        CloudType cloud3 = new CloudType("SC", 2, null, null, null);

        assertThat(cloud1)
                .isEqualTo(cloud2)
                .isNotEqualTo(cloud3);
        assertThat(cloud1.hashCode()).hasSameHashCodeAs(cloud2.hashCode());
    }

    @Test
    void testToString() {
        CloudType cloud = new CloudType("SC", 1, null, null, null);

        String result = cloud.toString();
        assertThat(result)
                .contains("SC")
                .contains("1");
    }

    @Test
    @DisplayName("Should accept TCU as valid cloud type")
    void testTCU_Valid() {
        CloudType tcu = CloudType.of("TCU");

        assertThat(tcu.cloudType()).isEqualTo("TCU");
        assertThat(tcu.oktas()).isNull();
        assertThat(tcu.intensity()).isNull();
        assertThat(tcu.location()).isNull();
        assertThat(tcu.movementDirection()).isNull();
    }

    @Test
    @DisplayName("Should create TCU with oktas")
    void testTCU_WithOktas() {
        CloudType tcu4 = CloudType.of("TCU", 4);

        assertThat(tcu4.cloudType()).isEqualTo("TCU");
        assertThat(tcu4.oktas()).isEqualTo(4);
        assertThat(tcu4.hasOktaCoverage()).isTrue();
        assertThat(tcu4.getOktasFraction()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Should normalize TCU case")
    void testTCU_CaseInsensitive() {
        CloudType lower = CloudType.of("tcu");
        CloudType mixed = CloudType.of("Tcu");
        CloudType upper = CloudType.of("TCU");

        assertThat(lower.cloudType()).isEqualTo("TCU");
        assertThat(mixed.cloudType()).isEqualTo("TCU");
        assertThat(upper.cloudType()).isEqualTo("TCU");
    }

    @Test
    @DisplayName("Should return correct description for TCU")
    void testTCU_Description() {
        CloudType tcu = CloudType.of("TCU");

        assertThat(tcu.getCloudTypeDescription()).isEqualTo("Towering Cumulus");
    }

    @Test
    @DisplayName("Should format TCU summary correctly")
    void testTCU_Summary() {
        // TCU with oktas
        CloudType tcu4 = CloudType.of("TCU", 4);
        assertThat(tcu4.getSummary()).isEqualTo("Towering Cumulus (4/8)");

        // TCU with location
        CloudType tcuOhd = CloudType.withLocation("TCU", "OHD");
        assertThat(tcuOhd.getSummary()).isEqualTo("Towering Cumulus (ohd)");

        // TCU with oktas and location
        CloudType tcuComplete = new CloudType("TCU", 2, null, "TR", null);
        assertThat(tcuComplete.getSummary()).isEqualTo("Towering Cumulus (2/8, tr)");

        // TCU with movement
        CloudType tcuMoving = new CloudType("TCU", null, null, null, "NE");
        assertThat(tcuMoving.getSummary()).isEqualTo("Towering Cumulus (moving NE)");

        // TCU with intensity
        CloudType tcuIntense = new CloudType("TCU", 6, "MDT", null, null);
        assertThat(tcuIntense.getSummary()).isEqualTo("mdt Towering Cumulus (6/8)");
    }

    @Test
    @DisplayName("Should handle real-world TCU examples")
    void testTCU_RealWorldExamples() {
        // Example from CYYZ: "TCU4"
        CloudType tcu4 = CloudType.of("TCU", 4);
        assertThat(tcu4.cloudType()).isEqualTo("TCU");
        assertThat(tcu4.oktas()).isEqualTo(4);
        assertThat(tcu4.getSummary()).isEqualTo("Towering Cumulus (4/8)");

        // Example: "TCU DSNT S" (would go to thunderstorm location, not cloud types)
        // But we can still create it
        CloudType tcuDistant = new CloudType("TCU", null, null, null, "S");
        assertThat(tcuDistant.cloudType()).isEqualTo("TCU");
        assertThat(tcuDistant.hasMovement()).isTrue();
    }

    @Test
    @DisplayName("Should handle TCU edge cases")
    void testTCU_EdgeCases() {
        // TCU with all fields
        CloudType complete = new CloudType("TCU", 8, "MDT", "OHD", "NW");
        assertThat(complete.cloudType()).isEqualTo("TCU");
        assertThat(complete.oktas()).isEqualTo(8);
        assertThat(complete.intensity()).isEqualTo("MDT");
        assertThat(complete.location()).isEqualTo("OHD");
        assertThat(complete.movementDirection()).isEqualTo("NW");
        assertThat(complete.hasOktaCoverage()).isTrue();
        assertThat(complete.hasIntensity()).isTrue();
        assertThat(complete.hasLocation()).isTrue();
        assertThat(complete.hasMovement()).isTrue();
        assertThat(complete.isOverhead()).isTrue();

        // TCU with minimum valid oktas
        CloudType min = CloudType.of("TCU", 1);
        assertThat(min.oktas()).isEqualTo(1);

        // TCU with maximum valid oktas
        CloudType max = CloudType.of("TCU", 8);
        assertThat(max.oktas()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should handle TCU equality correctly")
    void testTCU_Equality() {
        CloudType tcu1 = CloudType.of("TCU", 4);
        CloudType tcu2 = CloudType.of("TCU", 4);
        CloudType tcu3 = CloudType.of("TCU", 5);

        assertThat(tcu1)
                .isEqualTo(tcu2)
                .isNotEqualTo(tcu3);
        assertThat(tcu1.hashCode()).hasSameHashCodeAs(tcu2.hashCode());
    }

    @ParameterizedTest
    @CsvSource({
            "CU, Cumulus",
            "TCU, Towering Cumulus",
            "CF, Cumuliform",
            "ST, Stratus",
            "SC, Stratocumulus",
            "SF, Stratiform",
            "NS, Nimbostratus",
            "AS, Altostratus",
            "AC, Altocumulus",
            "CS, Cirrostratus",
            "CC, Cirrocumulus",
            "CI, Cirrus"
    })
    @DisplayName("Should return correct descriptions for all cloud types")
    void testCloudTypeDescriptions(String code, String expectedDescription) {
        CloudType ct = CloudType.of(code);
        assertThat(ct.getCloudTypeDescription()).isEqualTo(expectedDescription);
    }
}
