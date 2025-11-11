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
package weather.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for IndexedLinkedHashMap.
 * 
 * Tests the indexed access functionality on top of LinkedHashMap behavior.
 */
class IndexedLinkedHashMapTest {
    
    private IndexedLinkedHashMap<String, String> map;
    
    @BeforeEach
    void setUp() {
        map = new IndexedLinkedHashMap<>();
    }
    
    // ========== CONSTRUCTOR TESTS ==========
    
    @Test
    void testDefaultConstructor() {
        IndexedLinkedHashMap<String, String> newMap = new IndexedLinkedHashMap<>();
        
        assertThat(newMap).isEmpty();
    }
    
    @Test
    void testConstructorWithInitialCapacity() {
        IndexedLinkedHashMap<String, String> newMap = new IndexedLinkedHashMap<>(20);
        
        assertThat(newMap).isEmpty();
    }
    
    // ========== PUT OPERATION TESTS ==========
    
    @Test
    void testPut_SingleEntry() {
        map.put("key1", "value1");
        
        assertThat(map).hasSize(1);
        assertThat(map).containsEntry("key1", "value1");
        assertThat(map.getValueAtIndex(0)).isEqualTo("value1");
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key1");
    }
    
    @Test
    void testPut_MultipleEntries() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        assertThat(map).hasSize(3);
        assertThat(map.getValueAtIndex(0)).isEqualTo("value1");
        assertThat(map.getValueAtIndex(1)).isEqualTo("value2");
        assertThat(map.getValueAtIndex(2)).isEqualTo("value3");
    }
    
    @Test
    void testPut_UpdateExistingKey() {
        map.put("key1", "value1");
        map.put("key1", "value2");
        
        // Should still have only one entry
        assertThat(map).hasSize(1);
        assertThat(map).containsEntry("key1", "value2");
        assertThat(map.getValueAtIndex(0)).isEqualTo("value2");
        
        // Index should not change when updating existing key
        assertThat(map.getIndexOf("key1")).isZero();
    }
    
    @Test
    void testPut_NullKey() {
        map.put(null, "value1");
        
        assertThat(map).hasSize(1);
        assertThat(map).containsEntry(null, "value1");
        assertThat(map.getValueAtIndex(0)).isEqualTo("value1");
        assertThat(map.getKeyAtIndex(0)).isNull();
    }
    
    @Test
    void testPut_NullValue() {
        map.put("key1", null);
        
        assertThat(map).hasSize(1);
        assertThat(map.get("key1")).isNull();
        assertThat(map.getValueAtIndex(0)).isNull();
    }
    
    @Test
    void testPut_ReturnsOldValue() {
        String oldValue = map.put("key1", "value1");
        assertThat(oldValue).isNull();
        
        oldValue = map.put("key1", "value2");
        assertThat(oldValue).isEqualTo("value1");
    }
    
    // ========== INDEX-BASED ACCESS TESTS ==========
    
    @Test
    void testGetValueAtIndex() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        assertThat(map.getValueAtIndex(0)).isEqualTo("value1");
        assertThat(map.getValueAtIndex(1)).isEqualTo("value2");
        assertThat(map.getValueAtIndex(2)).isEqualTo("value3");
    }
    
    @Test
    void testGetValueAtIndex_OutOfBounds() {
        map.put("key1", "value1");
        
        assertThatThrownBy(() -> map.getValueAtIndex(1))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }
    
    @Test
    void testGetValueAtIndex_NegativeIndex() {
        map.put("key1", "value1");
        
        assertThatThrownBy(() -> map.getValueAtIndex(-1))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }
    
    @Test
    void testGetKeyAtIndex() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key1");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("key2");
        assertThat(map.getKeyAtIndex(2)).isEqualTo("key3");
    }
    
    @Test
    void testGetKeyAtIndex_OutOfBounds() {
        map.put("key1", "value1");
        
        assertThatThrownBy(() -> map.getKeyAtIndex(1))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }
    
    @Test
    void testGetKeyAtIndex_NegativeIndex() {
        map.put("key1", "value1");
        
        assertThatThrownBy(() -> map.getKeyAtIndex(-1))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }
    
    @Test
    void testGetIndexOf() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        assertThat(map.getIndexOf("key1")).isZero();
        assertThat(map.getIndexOf("key2")).isEqualTo(1);
        assertThat(map.getIndexOf("key3")).isEqualTo(2);
    }
    
    @Test
    void testGetIndexOf_NonExistentKey() {
        map.put("key1", "value1");
        
        assertThat(map.getIndexOf("nonexistent")).isEqualTo(-1);
    }
    
    @Test
    void testGetIndexOf_NullKey() {
        map.put(null, "value1");
        map.put("key1", "value2");
        
        assertThat(map.getIndexOf(null)).isZero();
    }
    
    // ========== INSERTION ORDER PRESERVATION TESTS ==========
    
    @Test
    void testInsertionOrderPreserved() {
        map.put("zebra", "z");
        map.put("apple", "a");
        map.put("banana", "b");
        
        // Should maintain insertion order, not alphabetical
        assertThat(map.getKeyAtIndex(0)).isEqualTo("zebra");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("apple");
        assertThat(map.getKeyAtIndex(2)).isEqualTo("banana");
    }
    
    @Test
    void testOrderPreservedAfterUpdate() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        // Update middle entry
        map.put("key2", "updated");
        
        // Order should remain the same
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key1");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("key2");
        assertThat(map.getKeyAtIndex(2)).isEqualTo("key3");
        assertThat(map.getValueAtIndex(1)).isEqualTo("updated");
    }
    
    // ========== REMOVE OPERATION TESTS ==========
    
    @Test
    void testRemove_UpdatesIndices() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        map.remove("key2");
        
        assertThat(map).hasSize(2);
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key1");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("key3");
        assertThat(map.getIndexOf("key2")).isEqualTo(-1);
    }
    
    @Test
    void testRemove_FirstElement() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        map.remove("key1");
        
        assertThat(map).hasSize(2);
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key2");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("key3");
    }
    
    @Test
    void testRemove_LastElement() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        map.remove("key3");
        
        assertThat(map).hasSize(2);
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key1");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("key2");
    }

    @Test
    void testRemove_NonExistentKey() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        
        // Remove a key that doesn't exist
        String removed = map.remove("nonexistent");
        
        assertThat(removed).isNull();
        assertThat(map).hasSize(2);
        
        // Verify existing keys still have correct indices
        assertThat(map.getIndexOf("key1")).isZero();
        assertThat(map.getIndexOf("key2")).isEqualTo(1);
    }
    
    @Test
    void testRemove_NullKey() {
        map.put("key1", "value1");
        map.put(null, "value2");
        map.put("key3", "value3");
        
        // Remove the null key
        String removed = map.remove(null);
        
        assertThat(removed).isEqualTo("value2");
        assertThat(map).hasSize(2);
        assertThat(map.getIndexOf(null)).isEqualTo(-1);
        
        // Verify other indices shifted
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key1");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("key3");
    }

    @Test
    void testRemove_AllElements() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        // Remove all elements one by one
        map.remove("key1");
        map.remove("key2");
        map.remove("key3");
        
        assertThat(map).isEmpty();
        
        // Try to remove from empty map - this tests the null branch
        String removed = map.remove("key1");
        assertThat(removed).isNull();
    }
    
    @Test
    void testRemove_ThenReAdd() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        
        // Remove and re-add
        map.remove("key1");
        map.put("key1", "value1_new");
        
        assertThat(map).hasSize(2);
        // key1 should now be at the end (index 1)
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key2");
        assertThat(map.getKeyAtIndex(1)).isEqualTo("key1");
        assertThat(map).containsEntry("key1", "value1_new");
    }
    
    @Test
    void testRemove_FromEmptyMap() {
        // Remove from empty map - tests null branch
        String removed = map.remove("anyKey");
        
        assertThat(removed).isNull();
        assertThat(map).isEmpty();
    }
    
    // ========== CLEAR OPERATION TESTS ==========
    
    @Test
    void testClear() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        map.clear();
        
        assertThat(map).isEmpty();
    }
    
    @Test
    void testClear_ThenAdd() {
        map.put("key1", "value1");
        map.clear();
        map.put("key2", "value2");
        
        assertThat(map).hasSize(1);
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key2");
        assertThat(map.getIndexOf("key2")).isZero();
    }
    
    // ========== EQUALS AND HASHCODE TESTS ==========
    
    @Test
    void testEquals_SameObject() {
        assertThat(map).isEqualTo(map);
    }
    
    @Test
    void testEquals_EqualMaps() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        
        map2.put("key1", "value1");
        map2.put("key2", "value2");
        
        assertThat(map1).isEqualTo(map2);
    }
    
    @Test
    void testEquals_DifferentOrder() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        
        map2.put("key2", "value2");
        map2.put("key1", "value1");
        
        // Should NOT be equal - different insertion order
        assertThat(map1).isNotEqualTo(map2);
    }
    
    @Test
    void testEquals_DifferentValues() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put("key1", "value1");
        map2.put("key1", "value2");
        
        assertThat(map1).isNotEqualTo(map2);
    }
    
    @Test
    void testEquals_Null() {
        assertThat(map).isNotEqualTo(null);
    }

    @Test
    void testEquals_DifferentType() {
        assertThat(map).isNotEqualTo("not a map");
    }
        
    @Test
    void testEquals_EmptyMaps() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        assertThat(map1).isEqualTo(map2);
    }
    
    @Test
    void testEquals_DifferentSizes() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        
        map2.put("key1", "value1");
        
        assertThat(map1).isNotEqualTo(map2);
    }

    @Test
    void testEquals_DifferentKeys() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put("key1", "value1");
        map2.put("key2", "value1");
        
        assertThat(map1).isNotEqualTo(map2);
    }
    
    @Test
    void testEquals_SameContentsDifferentInsertionPattern() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        // Map 1: Add, remove, re-add
        map1.put("key1", "value1");
        map1.put("key2", "value2");
        map1.remove("key1");
        map1.put("key1", "value1");
        
        // Map 2: Add in different order
        map2.put("key2", "value2");
        map2.put("key1", "value1");
        
        // Both have same final state and order
        assertThat(map1).isEqualTo(map2);
    }

    @Test
    void testEquals_WithNullValues() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put("key1", null);
        map1.put("key2", "value2");
        
        map2.put("key1", null);
        map2.put("key2", "value2");
        
        assertThat(map1).isEqualTo(map2);
    }
    
    @Test
    void testEquals_WithNullKeys() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put(null, "value1");
        map1.put("key2", "value2");
        
        map2.put(null, "value1");
        map2.put("key2", "value2");
        
        assertThat(map1).isEqualTo(map2);
    }
    
    @Test
    void testHashCode_Consistency() {
        map.put("key1", "value1");
        
        int hash1 = map.hashCode();
        int hash2 = map.hashCode();
        
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void testHashCode_EqualMaps() {
        IndexedLinkedHashMap<String, String> map1 = new IndexedLinkedHashMap<>();
        IndexedLinkedHashMap<String, String> map2 = new IndexedLinkedHashMap<>();
        
        map1.put("key1", "value1");
        map2.put("key1", "value1");
        
        assertThat(map1.hashCode()).hasSameHashCodeAs(map2.hashCode());
    }
    
    // ========== SERIALIZATION TESTS ==========
    
    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        
        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        oos.close();
        
        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        @SuppressWarnings("unchecked")
        IndexedLinkedHashMap<String, String> deserializedMap = 
            (IndexedLinkedHashMap<String, String>) ois.readObject();
        ois.close();
        
        // Verify
        assertThat(deserializedMap).hasSize(3);
        assertThat(deserializedMap).containsEntry("key1", "value1");
        assertThat(deserializedMap).containsEntry("key2", "value2");
        assertThat(deserializedMap).containsEntry("key3", "value3");
        
        // Verify index-based access still works
        assertThat(deserializedMap.getKeyAtIndex(0)).isEqualTo("key1");
        assertThat(deserializedMap.getKeyAtIndex(1)).isEqualTo("key2");
        assertThat(deserializedMap.getKeyAtIndex(2)).isEqualTo("key3");
        assertThat(deserializedMap.getIndexOf("key2")).isEqualTo(1);
    }
    
    @Test
    void testSerialization_EmptyMap() throws IOException, ClassNotFoundException {
        // Serialize empty map
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        oos.close();
        
        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        @SuppressWarnings("unchecked")
        IndexedLinkedHashMap<String, String> deserializedMap = 
            (IndexedLinkedHashMap<String, String>) ois.readObject();
        ois.close();
        
        // Verify
        assertThat(deserializedMap).isEmpty();
    }
    
    @Test
    void testSerialization_PreservesOrder() throws IOException, ClassNotFoundException {
        map.put("zebra", "z");
        map.put("apple", "a");
        map.put("banana", "b");
        
        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(map);
        oos.close();
        
        // Deserialize
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        @SuppressWarnings("unchecked")
        IndexedLinkedHashMap<String, String> deserializedMap = 
            (IndexedLinkedHashMap<String, String>) ois.readObject();
        ois.close();
        
        // Verify order preserved
        assertThat(deserializedMap.getKeyAtIndex(0)).isEqualTo("zebra");
        assertThat(deserializedMap.getKeyAtIndex(1)).isEqualTo("apple");
        assertThat(deserializedMap.getKeyAtIndex(2)).isEqualTo("banana");
    }
    
    // ========== EDGE CASE TESTS ==========
    
    @Test
    void testEmptyMap_GetValueAtIndex() {
        assertThatThrownBy(() -> map.getValueAtIndex(0))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }
    
    @Test
    void testEmptyMap_GetKeyAtIndex() {
        assertThatThrownBy(() -> map.getKeyAtIndex(0))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }
    
    @Test
    void testEmptyMap_GetIndexOf() {
        assertThat(map.getIndexOf("anyKey")).isEqualTo(-1);
    }
    
    @Test
    void testLargeMap() {
        // Test with many entries
        for (int i = 0; i < 1000; i++) {
            map.put("key" + i, "value" + i);
        }
        
        assertThat(map).hasSize(1000);
        assertThat(map.getKeyAtIndex(0)).isEqualTo("key0");
        assertThat(map.getKeyAtIndex(999)).isEqualTo("key999");
        assertThat(map.getIndexOf("key500")).isEqualTo(500);
    }
    
    @Test
    void testIntegerKeys() {
        IndexedLinkedHashMap<Integer, String> intMap = new IndexedLinkedHashMap<>();
        
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        
        assertThat(intMap.getKeyAtIndex(0)).isEqualTo(1);
        assertThat(intMap.getValueAtIndex(1)).isEqualTo("two");
        assertThat(intMap.getIndexOf(3)).isEqualTo(2);
    }
    
    @Test
    void testCustomObjectKeys() {
        record Person(String name, int age) {}
        
        IndexedLinkedHashMap<Person, String> personMap = new IndexedLinkedHashMap<>();
        Person person1 = new Person("Alice", 30);
        Person person2 = new Person("Bob", 25);
        
        personMap.put(person1, "Engineer");
        personMap.put(person2, "Designer");
        
        assertThat(personMap.getKeyAtIndex(0)).isEqualTo(person1);
        assertThat(personMap.getValueAtIndex(1)).isEqualTo("Designer");
        assertThat(personMap.getIndexOf(person1)).isZero();
    }
}
