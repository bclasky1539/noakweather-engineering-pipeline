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
package weather.processing.parser.noaa;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for NoaaAviationWeatherPatternHandler record.
 * 
 * @author bclasky1539
 *
 */
class NoaaAviationWeatherPatternHandlerTest {
    
    @Test
    void testSingleFactoryMethod() {
        NoaaAviationWeatherPatternHandler handler = NoaaAviationWeatherPatternHandler.single("testHandler");
        
        assertThat(handler.handlerName()).isEqualTo("testHandler");
        assertThat(handler.canRepeat()).isFalse();
    }
    
    @Test
    void testRepeatingFactoryMethod() {
        NoaaAviationWeatherPatternHandler handler = NoaaAviationWeatherPatternHandler.repeating("testHandler");
        
        assertThat(handler.handlerName()).isEqualTo("testHandler");
        assertThat(handler.canRepeat()).isTrue();
    }
    
    @Test
    void testRecordEquality() {
        NoaaAviationWeatherPatternHandler handler1 = NoaaAviationWeatherPatternHandler.single("test");
        NoaaAviationWeatherPatternHandler handler2 = NoaaAviationWeatherPatternHandler.single("test");
        NoaaAviationWeatherPatternHandler handler3 = NoaaAviationWeatherPatternHandler.repeating("test");
        
        assertThat(handler1)
                .isEqualTo(handler2)
                .isNotEqualTo(handler3);
    }
    
    @Test
    void testRecordToString() {
        NoaaAviationWeatherPatternHandler handler = NoaaAviationWeatherPatternHandler.single("windHandler");
        
        String str = handler.toString();
        assertThat(str)
                .contains("windHandler")
                .contains("false");
    }
}
