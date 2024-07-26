package io.herter.scopedvalue;

public interface NonThrowingCloseable extends AutoCloseable {

    @Override
    void close();
}
