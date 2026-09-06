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
import weather.model.components.Wind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WindAtLocation.
 *
 * @author bclasky1539
 *
 */
class WindAtLocationTest {

    @Test
    @DisplayName("Should create wind at altitude via canonical constructor")
    void testConstructor_AtAltitude() {
        Wind wind = Wind.of(230, 10, "KT");
        WindAtLocation windAtLocation = new WindAtLocation(1400, null, wind);

        assertThat(windAtLocation.heightFeet()).isEqualTo(1400);
        assertThat(windAtLocation.runway()).isNull();
        assertThat(windAtLocation.wind()).isEqualTo(wind);
    }

    @Test
    @DisplayName("Should create wind at runway via canonical constructor")
    void testConstructor_AtRunway() {
        Wind wind = Wind.of(0, 0, "KT");
        WindAtLocation windAtLocation = new WindAtLocation(null, "26", wind);

        assertThat(windAtLocation.heightFeet()).isNull();
        assertThat(windAtLocation.runway()).isEqualTo("26");
        assertThat(windAtLocation.wind()).isEqualTo(wind);
    }

    @Test
    @DisplayName("Should throw when neither heightFeet nor runway is provided")
    void testConstructor_NeitherHeightNorRunway_Throws() {
        Wind wind = Wind.of(230, 10, "KT");

        assertThatThrownBy(() -> new WindAtLocation(null, null, wind))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either heightFeet or runway must be provided");
    }

    @Test
    @DisplayName("Should throw when runway is blank")
    void testConstructor_BlankRunway_Throws() {
        Wind wind = Wind.of(230, 10, "KT");

        assertThatThrownBy(() -> new WindAtLocation(null, "   ", wind))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either heightFeet or runway must be provided");
    }

    @Test
    @DisplayName("Should throw when both heightFeet and runway are provided")
    void testConstructor_BothHeightAndRunway_Throws() {
        Wind wind = Wind.of(230, 10, "KT");

        assertThatThrownBy(() -> new WindAtLocation(1400, "26", wind))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot specify both heightFeet and runway");
    }

    @Test
    @DisplayName("Should throw when wind is null")
    void testConstructor_NullWind_Throws() {
        assertThatThrownBy(() -> new WindAtLocation(1400, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wind cannot be null");
    }

    @Test
    @DisplayName("isAtAltitude should return true for altitude-based reading")
    void testIsAtAltitude_True() {
        WindAtLocation windAtLocation = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));

        assertThat(windAtLocation.isAtAltitude()).isTrue();
        assertThat(windAtLocation.isAtRunway()).isFalse();
    }

    @Test
    @DisplayName("isAtRunway should return true for runway-based reading")
    void testIsAtRunway_True() {
        WindAtLocation windAtLocation = WindAtLocation.atRunway("26", Wind.of(0, 0, "KT"));

        assertThat(windAtLocation.isAtRunway()).isTrue();
        assertThat(windAtLocation.isAtAltitude()).isFalse();
    }

    @Test
    @DisplayName("Should generate correct summary for altitude reading")
    void testGetSummary_AtAltitude() {
        WindAtLocation windAtLocation = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));

        assertThat(windAtLocation.getSummary())
                .isEqualTo("Wind at 1400ft: 230° at 10 KT");
    }

    @Test
    @DisplayName("Should generate correct summary for runway reading")
    void testGetSummary_AtRunway() {
        WindAtLocation windAtLocation = WindAtLocation.atRunway("26", Wind.of(0, 0, "KT"));

        assertThat(windAtLocation.getSummary())
                .isEqualTo("Wind at RWY 26: 0° at 0 KT");
    }

    @Test
    @DisplayName("Should generate correct summary for variable direction wind")
    void testGetSummary_VariableDirection() {
        WindAtLocation windAtLocation = WindAtLocation.atRunway("32", Wind.variable(1, "KT"));

        assertThat(windAtLocation.getSummary())
                .contains("Wind at RWY 32:")
                .contains("VRB");
    }

    @Test
    @DisplayName("Factory atAltitude should create altitude-based reading")
    void testFactory_AtAltitude() {
        Wind wind = Wind.of(230, 10, "KT");
        WindAtLocation windAtLocation = WindAtLocation.atAltitude(1400, wind);

        assertThat(windAtLocation.heightFeet()).isEqualTo(1400);
        assertThat(windAtLocation.runway()).isNull();
        assertThat(windAtLocation.wind()).isEqualTo(wind);
    }

    @Test
    @DisplayName("Factory atRunway should create runway-based reading")
    void testFactory_AtRunway() {
        Wind wind = Wind.of(0, 0, "KT");
        WindAtLocation windAtLocation = WindAtLocation.atRunway("26", wind);

        assertThat(windAtLocation.heightFeet()).isNull();
        assertThat(windAtLocation.runway()).isEqualTo("26");
        assertThat(windAtLocation.wind()).isEqualTo(wind);
    }

    @Test
    @DisplayName("Should be equal when heightFeet, runway, and wind match")
    void testEquality() {
        WindAtLocation windAtLocation1 = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));
        WindAtLocation windAtLocation2 = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));

        assertThat(windAtLocation1).isEqualTo(windAtLocation2);
        assertThat(windAtLocation1.hashCode()).isEqualTo(windAtLocation2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when heightFeet differs")
    void testInequality_DifferentHeight() {
        WindAtLocation windAtLocation1 = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));
        WindAtLocation windAtLocation2 = WindAtLocation.atAltitude(1126, Wind.of(230, 10, "KT"));

        assertThat(windAtLocation1).isNotEqualTo(windAtLocation2);
    }

    @Test
    @DisplayName("Should not be equal when runway differs")
    void testInequality_DifferentRunway() {
        WindAtLocation windAtLocation1 = WindAtLocation.atRunway("26", Wind.of(0, 0, "KT"));
        WindAtLocation windAtLocation2 = WindAtLocation.atRunway("32", Wind.of(0, 0, "KT"));

        assertThat(windAtLocation1).isNotEqualTo(windAtLocation2);
    }

    @Test
    @DisplayName("Should not be equal when wind differs")
    void testInequality_DifferentWind() {
        WindAtLocation windAtLocation1 = WindAtLocation.atAltitude(1400, Wind.of(230, 10, "KT"));
        WindAtLocation windAtLocation2 = WindAtLocation.atAltitude(1400, Wind.of(180, 5, "KT"));

        assertThat(windAtLocation1).isNotEqualTo(windAtLocation2);
    }
}
