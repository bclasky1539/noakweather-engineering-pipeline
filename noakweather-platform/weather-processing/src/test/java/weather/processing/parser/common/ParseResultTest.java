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
package weather.processing.parser.common;

import org.junit.jupiter.api.DisplayName;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ParseResult wrapper class.
 * Tests success/failure handling, error propagation, and functional operations.
 * 
 * @author bclasky1539
 *
 */
class ParseResultTest {
    
    @Test
    @DisplayName("Should create successful result with data")
    void testSuccessWithData() {
        String testData = "test data";
        ParseResult<String> result = ParseResult.success(testData);
        
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertTrue(result.getData().isPresent());
        assertEquals(testData, result.getData().get());
        assertNull(result.getErrorMessage());
        assertFalse(result.getException().isPresent());
    }
    
    @Test
    @DisplayName("Should throw exception when creating success with null data")
    void testSuccessWithNullData() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ParseResult.success(null)
        );
        
        assertEquals("Success data cannot be null", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should create failed result with error message")
    void testFailureWithMessage() {
        String errorMessage = "Something went wrong";
        ParseResult<String> result = ParseResult.failure(errorMessage);
        
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertFalse(result.getData().isPresent());
        assertEquals(errorMessage, result.getErrorMessage());
        assertFalse(result.getException().isPresent());
    }
    
    @Test
    @DisplayName("Should use default message when error message is null")
    void testFailureWithNullMessage() {
        ParseResult<String> result = ParseResult.failure(null);
        
        assertTrue(result.isFailure());
        assertEquals("Unknown error", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should use default message when error message is empty")
    void testFailureWithEmptyMessage() {
        ParseResult<String> result = ParseResult.failure("   ");
        
        assertTrue(result.isFailure());
        assertEquals("Unknown error", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should create failed result with error message and exception")
    void testFailureWithMessageAndException() {
        String errorMessage = "Parse failed";
        Exception exception = new RuntimeException("Root cause");
        ParseResult<String> result = ParseResult.failure(errorMessage, exception);
        
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(errorMessage, result.getErrorMessage());
        assertTrue(result.getException().isPresent());
        assertEquals(exception, result.getException().get());
    }
    
    @Test
    @DisplayName("Should use exception message when error message is null")
    void testFailureWithNullMessageButException() {
        Exception exception = new RuntimeException("Exception message");
        ParseResult<String> result = ParseResult.failure(null, exception);
        
        assertTrue(result.isFailure());
        assertEquals("Exception message", result.getErrorMessage());
        assertTrue(result.getException().isPresent());
    }
    
    @Test
    @DisplayName("Should use default when both message and exception are null")
    void testFailureWithBothNull() {
        ParseResult<String> result = ParseResult.failure(null, null);
        
        assertTrue(result.isFailure());
        assertEquals("Unknown error", result.getErrorMessage());
        assertFalse(result.getException().isPresent());
    }
    
    @Test
    @DisplayName("Should execute action on success")
    void testIfSuccess() {
        ParseResult<String> result = ParseResult.success("test");
        
        StringBuilder builder = new StringBuilder();
        result.ifSuccess(builder::append);
        
        assertEquals("test", builder.toString());
    }
    
    @Test
    @DisplayName("Should not execute action on failure")
    void testIfSuccessNotCalled() {
        ParseResult<String> result = ParseResult.failure("error");
        
        StringBuilder builder = new StringBuilder();
        result.ifSuccess(builder::append);
        
        assertEquals("", builder.toString());
    }
    
    @Test
    @DisplayName("Should execute action on failure")
    void testIfFailure() {
        ParseResult<String> result = ParseResult.failure("error message");
        
        StringBuilder builder = new StringBuilder();
        result.ifFailure(builder::append);
        
        assertEquals("error message", builder.toString());
    }
    
    @Test
    @DisplayName("Should not execute action on success when checking failure")
    void testIfFailureNotCalled() {
        ParseResult<String> result = ParseResult.success("test");
        
        StringBuilder builder = new StringBuilder();
        result.ifFailure(builder::append);
        
        assertEquals("", builder.toString());
    }
    
    @Test
    @DisplayName("Should allow chaining ifSuccess and ifFailure")
    void testChaining() {
        StringBuilder successBuilder = new StringBuilder();
        StringBuilder failureBuilder = new StringBuilder();
        
        ParseResult<String> success = ParseResult.success("data");
        success.ifSuccess(successBuilder::append)
           .ifFailure(failureBuilder::append);
        
        assertEquals("data", successBuilder.toString());
        assertEquals("", failureBuilder.toString());
    }
    
    @Test
    @DisplayName("Should map successful result to new type")
    void testMapSuccess() {
        ParseResult<String> stringResult = ParseResult.success("123");
        ParseResult<Integer> intResult = stringResult.map(Integer::parseInt);
        
        assertTrue(intResult.isSuccess());
        assertEquals(123, intResult.getData().get());
    }
    
    @Test
    @DisplayName("Should propagate failure when mapping")
    void testMapFailure() {
        ParseResult<String> stringResult = ParseResult.failure("error");
        ParseResult<Integer> intResult = stringResult.map(Integer::parseInt);
        
        assertTrue(intResult.isFailure());
        assertEquals("error", intResult.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should handle exception during mapping")
    void testMapWithException() {
        ParseResult<String> stringResult = ParseResult.success("not a number");
        ParseResult<Integer> intResult = stringResult.map(Integer::parseInt);
        
        assertTrue(intResult.isFailure());
        assertTrue(intResult.getErrorMessage().contains("Mapping failed"));
    }
    
    @Test
    @DisplayName("Should return data when calling orElse on success")
    void testOrElseOnSuccess() {
        ParseResult<String> result = ParseResult.success("actual");
        String value = result.orElse("default");
        
        assertEquals("actual", value);
    }
    
    @Test
    @DisplayName("Should return default when calling orElse on failure")
    void testOrElseOnFailure() {
        ParseResult<String> result = ParseResult.failure("error");
        String value = result.orElse("default");
        
        assertEquals("default", value);
    }
    
    @Test
    @DisplayName("Should return data when calling orElseThrow on success")
    void testOrElseThrowOnSuccess() throws ParserException {
        ParseResult<String> result = ParseResult.success("data");
        String value = result.orElseThrow();
        
        assertEquals("data", value);
    }
    
    @Test
    @DisplayName("Should throw exception when calling orElseThrow on failure")
    void testOrElseThrowOnFailure() {
        ParseResult<String> result = ParseResult.failure("error message");
        
        ParserException exception = assertThrows(
            ParserException.class,
            result::orElseThrow
        );
        
        assertEquals("error message", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should have meaningful toString for success")
    void testToStringSuccess() {
        ParseResult<String> result = ParseResult.success("test data");
        String str = result.toString();
        
        assertTrue(str.contains("success=true"));
        assertTrue(str.contains("test data"));
    }
    
    @Test
    @DisplayName("Should have meaningful toString for failure")
    void testToStringFailure() {
        ParseResult<String> result = ParseResult.failure("error message");
        String str = result.toString();
        
        assertTrue(str.contains("success=false"));
        assertTrue(str.contains("error message"));
    }
    
    @Test
    @DisplayName("Should include exception type in toString")
    void testToStringWithException() {
        Exception exception = new RuntimeException("cause");
        ParseResult<String> result = ParseResult.failure("error", exception);
        String str = result.toString();
        
        assertTrue(str.contains("RuntimeException"));
    }
    
    @Test
    @DisplayName("Should handle getData returning empty Optional on failure")
    void testGetDataReturnsEmpty() {
        ParseResult<String> result = ParseResult.failure("error");
        Optional<String> data = result.getData();
        
        assertFalse(data.isPresent());
        assertEquals(Optional.empty(), data);
    }
}
