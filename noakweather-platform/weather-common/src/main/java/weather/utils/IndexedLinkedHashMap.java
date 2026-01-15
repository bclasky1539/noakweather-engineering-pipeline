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
package weather.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Class representing the indexed linked hash map. It inherits from the
 * LinkedHashMap class
 * 
 * @author bclasky1539
 *
 * @param <K>
 * @param <V>
 */
public class IndexedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 1L;

    private transient ArrayList<K> indexList = new ArrayList<>();

    public IndexedLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public IndexedLinkedHashMap() {
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns true if the given object is also an IndexedLinkedHashMap with the same
     * mappings and insertion order.
     *
     * @param obj the object to be compared for equality with this map
     * @return true if the specified object is equal to this map
     */
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        
        if (obj instanceof IndexedLinkedHashMap<?, ?> other) {
            return Objects.equals(this.indexList, other.indexList);
        }
        
        return false;
    }

    /**
     * Returns the hash code value for this map.
     *
     * @return the hash code value for this map
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), indexList);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the key is new, it is added to the index list to maintain insertion order.
     *
     * @param key the key with which the specified value is to be associated
     * @param val the value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @Override
    public V put(K key, V val) {
        if (!super.containsKey(key)) {
            indexList.add(key);
        }
        return super.put(key, val);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     * Also removes the key from the index list to maintain consistency.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @Override
    public V remove(Object key) {
        V value = super.remove(key);
        if (value != null) {
            indexList.removeIf(k -> Objects.equals(k, key));
        }
        return value;
    }

    /**
     * Removes all mappings from this map and clears the index list.
     */
    @Override
    public void clear() {
        super.clear();
        indexList.clear();
    }

    /**
     * Returns the value at the specified index position in insertion order.
     *
     * @param i the index of the value to return (0-based)
     * @return the value at the specified index position
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public V getValueAtIndex(int i) {
        return super.get(indexList.get(i));
    }

    /**
     * Returns the key at the specified index position in insertion order.
     *
     * @param i the index of the key to return (0-based)
     * @return the key at the specified index position
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public K getKeyAtIndex(int i) {
        return indexList.get(i);
    }

    /**
     * Returns the index position of the specified key in insertion order.
     *
     * @param key the key whose index is to be returned
     * @return the index of the key, or -1 if the key is not present
     */
    public int getIndexOf(K key) {
        return indexList.indexOf(key);
    }

    /**
     * Reconstitutes the IndexedLinkedHashMap instance from a stream (deserializes it).
     * Rebuilds the index list from the deserialized map contents to maintain insertion order.
     *
     * @param in the ObjectInputStream from which to read the object
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Rebuild the index from the deserialized map contents
        indexList = new ArrayList<>(this.keySet());
    }
}
