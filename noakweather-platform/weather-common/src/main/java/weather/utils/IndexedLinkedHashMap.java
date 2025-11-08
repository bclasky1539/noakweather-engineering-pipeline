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
package weather.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
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
    private static final long serialVersionUID = 1L;

    private transient ArrayList<K> indexList = new ArrayList<>();

    public IndexedLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public IndexedLinkedHashMap() {
    }

    /**
     * Equals method override.
     *
     * @param obj
     * @return value
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
     * Sets the hash.
     *
     * @return value
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), indexList);
    }

    /**
     * Sets the locale of the bundle.
     *
     * @param key
     * @param val
     * @return value
     */
    @Override
    public V put(K key, V val) {
        if (!super.containsKey(key)) {
            indexList.add(key);
        }
        return super.put(key, val);
    }

    /**
     * Remove the specified key and update the index list.
     * Only remove from indexList if key was actually in the map.
     *
     * @param key
     * @return the previous value associated with key, or null
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
     * Clear all entries and reset the index list.
     */
    @Override
    public void clear() {
        super.clear();
        indexList.clear();
    }

    /**
     * Get the value at the specified index position.
     *
     * @param i
     * @return index
     */
    public V getValueAtIndex(int i) {
        return super.get(indexList.get(i));
    }

    /**
     * Get the key at the specified index position.
     *
     * @param i
     * @return index
     */
    public K getKeyAtIndex(int i) {
        return indexList.get(i);
    }

    /**
     * Get the value at index of key.
     *
     * @param key
     * @return index of key
     */
    public int getIndexOf(K key) {
        return indexList.indexOf(key);
    }

    /**
     * Called automatically during de-serialization - no impact on calling code.
     *
     * @param in
     * @return
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Rebuild the index from the deserialized map contents
        indexList = new ArrayList<>();
        for (K key : this.keySet()) {
            indexList.add(key);
        }
    }
}
