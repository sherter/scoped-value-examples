package io.herter.scopedvalue.guice;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import io.herter.scopedvalue.NonThrowingCloseable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class ScopedValueScope implements Scope {

    private final ScopedValue<?> scopedValue;
    private final Map<Object, Map<Key<?>, Object>> scopedObjects = new ConcurrentHashMap<>();

    public ScopedValueScope(ScopedValue<?> scopedValue) {
        this.scopedValue = requireNonNull(scopedValue);
    }

    @Override
    public <T> Provider<T> scope(Key<T> key, Provider<T> creator) {
        return () -> {
            Object scopeKey = scopedValue.orElseThrow(() -> new OutOfScopeException("Scoped value is not bound"));
            Map<Key<?>, Object> scope = scopedObjects.get(scopeKey);
            if (scope == null) {
                throw new IllegalStateException("Scope for key '%s' was not opened or has already been closed".formatted(scopeKey));
            }
            return (T) scope.computeIfAbsent(key, k -> creator.get());
        };
    }

    public NonThrowingCloseable open() {
        return open(scopedValue.orElseThrow(() -> new IllegalStateException()));
    }

    public NonThrowingCloseable open(Object key) {
        return open(key, Map.of());
    }

    public NonThrowingCloseable open(Object key, Map<Key<?>, Object> seed) {
        Map<Key<?>, Object> scope = new ConcurrentHashMap<>(seed);
        Map<Key<?>, Object> previousValue = scopedObjects.putIfAbsent(key, scope);
        if (previousValue != null) {
            throw new IllegalArgumentException("Scope for key '%s' already exists".formatted(key));
        }
        return () -> scopedObjects.remove(key);
    }

    public void close() {
        close(scopedValue.orElseThrow(() -> new IllegalStateException()));
    }

    public void close(Object key) {
        if (scopedObjects.remove(key) == null) {
            throw new IllegalArgumentException("Scope for key '%s' does not exist".formatted(key));
        }
    }
}