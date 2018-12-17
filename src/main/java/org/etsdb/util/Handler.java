package org.etsdb.util;

public interface Handler<T> {

    void handle(T event);

}