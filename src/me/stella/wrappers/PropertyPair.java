package me.stella.wrappers;

public class PropertyPair<K, V> {

    private final K key;
    private final V value;

    protected PropertyPair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public static <K, V> PropertyPair<K, V> parse(K key, V value) {
        return new PropertyPair<>(key, value);
    }

}
