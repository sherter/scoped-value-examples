package io.herter.scopedvalue.guice;

import com.google.inject.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopedValueScopeTest {

    @Test
    void test() {
        ScopedValue<String> scopedValue = ScopedValue.newInstance();

        ScopedValueScope scope = new ScopedValueScope(scopedValue);

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Object.class).in(scope);
            }
        });

        ProvisionException e1 = assertThrows(ProvisionException.class, () -> injector.getInstance(Object.class));
        assertInstanceOf(OutOfScopeException.class, e1.getCause());

        Object scopedObject = ScopedValue.getWhere(scopedValue, "key", () -> {
            ProvisionException e2 = assertThrows(ProvisionException.class, () -> injector.getInstance(Object.class));
            assertInstanceOf(IllegalStateException.class, e2.getCause());

            Object o;
            try(var ignored = scope.open()) {
                o = injector.getInstance(Object.class);
                assertNotNull(o);
                assertSame(o, injector.getInstance(Object.class));
            }
            scope.open();
            Object o2 = injector.getInstance(Object.class);
            assertNotSame(o, o2);
            return o2;
        });

        ProvisionException e3 = assertThrows(ProvisionException.class, () -> injector.getInstance(Object.class));
        assertInstanceOf(OutOfScopeException.class, e3.getCause());

        ScopedValue.runWhere(scopedValue, "key", () -> {
            assertSame(scopedObject, injector.getInstance(Object.class));
            scope.close();
            ProvisionException e4 = assertThrows(ProvisionException.class, () -> injector.getInstance(Object.class));
            assertInstanceOf(IllegalStateException.class, e4.getCause());
        });
    }
}