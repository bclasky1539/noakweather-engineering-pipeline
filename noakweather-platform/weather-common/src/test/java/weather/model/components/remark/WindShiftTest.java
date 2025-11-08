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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for WindShift record.
 * 
 * @author bclasky1539
 *
 */
class WindShiftTest {
    
    @Test
    void testValidWindShift() {
        WindShift windShift = new WindShift(15, 30, false);
        
        assertThat(windShift.hour()).isEqualTo(15);
        assertThat(windShift.minute()).isEqualTo(30);
        assertThat(windShift.frontalPassage()).isFalse();
    }
    
    @Test
    void testValidWindShift_WithFrontalPassage() {
        WindShift windShift = new WindShift(15, 30, true);
        
        assertThat(windShift.hour()).isEqualTo(15);
        assertThat(windShift.minute()).isEqualTo(30);
        assertThat(windShift.frontalPassage()).isTrue();
    }
    
    @Test
    void testValidWindShift_NullHour() {
        WindShift windShift = new WindShift(null, 30, false);
        
        assertThat(windShift.hour()).isNull();
        assertThat(windShift.minute()).isEqualTo(30);
        assertThat(windShift.frontalPassage()).isFalse();
    }
    
    @Test
    void testValidWindShift_NullMinute() {
        WindShift windShift = new WindShift(15, null, false);
        
        assertThat(windShift.hour()).isEqualTo(15);
        assertThat(windShift.minute()).isNull();
        assertThat(windShift.frontalPassage()).isFalse();
    }
    
    @Test
    void testValidWindShift_AllNull() {
        WindShift windShift = new WindShift(null, null, false);
        
        assertThat(windShift.hour()).isNull();
        assertThat(windShift.minute()).isNull();
        assertThat(windShift.frontalPassage()).isFalse();
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, 24, 25, 30})
    void testInvalidWindShift_HourOutOfRange(int invalidHour) {
        assertThatThrownBy(() -> new WindShift(invalidHour, 30, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hour must be between 0 and 23");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, 60, 65})
    void testInvalidWindShift_MinuteOutOfRange(int invalidMinute) {
        assertThatThrownBy(() -> new WindShift(15, invalidMinute, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Minute must be between 0 and 59");
    }
    
    @Test
    void testValidWindShift_BoundaryHour_Zero() {
        WindShift windShift = new WindShift(0, 30, false);
        
        assertThat(windShift.hour()).isEqualTo(0);
    }
    
    @Test
    void testValidWindShift_BoundaryHour_23() {
        WindShift windShift = new WindShift(23, 30, false);
        
        assertThat(windShift.hour()).isEqualTo(23);
    }
    
    @Test
    void testValidWindShift_BoundaryMinute_Zero() {
        WindShift windShift = new WindShift(15, 0, false);
        
        assertThat(windShift.minute()).isEqualTo(0);
    }
    
    @Test
    void testValidWindShift_BoundaryMinute_59() {
        WindShift windShift = new WindShift(15, 59, false);
        
        assertThat(windShift.minute()).isEqualTo(59);
    }
    
    @Test
    void testFrontalPassage_True() {
        WindShift windShift = new WindShift(15, 30, true);
        
        assertThat(windShift.frontalPassage()).isTrue();
    }
    
    @Test
    void testFrontalPassage_False() {
        WindShift windShift = new WindShift(15, 30, false);
        
        assertThat(windShift.frontalPassage()).isFalse();
    }
    
    @Test
    void testWindShift_Midnight() {
        WindShift windShift = new WindShift(0, 0, false);
        
        assertThat(windShift.hour()).isEqualTo(0);
        assertThat(windShift.minute()).isEqualTo(0);
    }
    
    @Test
    void testWindShift_EndOfDay() {
        WindShift windShift = new WindShift(23, 59, false);
        
        assertThat(windShift.hour()).isEqualTo(23);
        assertThat(windShift.minute()).isEqualTo(59);
    }
    
    @Test
    void testWindShift_EqualityWithSameValues() {
        WindShift ws1 = new WindShift(15, 30, true);
        WindShift ws2 = new WindShift(15, 30, true);
        
        assertThat(ws1).isEqualTo(ws2);
        assertThat(ws1.hashCode()).isEqualTo(ws2.hashCode());
    }
    
    @Test
    void testWindShift_InequalityWithDifferentHour() {
        WindShift ws1 = new WindShift(15, 30, true);
        WindShift ws2 = new WindShift(16, 30, true);
        
        assertThat(ws1).isNotEqualTo(ws2);
    }
    
    @Test
    void testWindShift_InequalityWithDifferentMinute() {
        WindShift ws1 = new WindShift(15, 30, true);
        WindShift ws2 = new WindShift(15, 31, true);
        
        assertThat(ws1).isNotEqualTo(ws2);
    }
    
    @Test
    void testWindShift_InequalityWithDifferentFrontalPassage() {
        WindShift ws1 = new WindShift(15, 30, true);
        WindShift ws2 = new WindShift(15, 30, false);
        
        assertThat(ws1).isNotEqualTo(ws2);
    }
}
