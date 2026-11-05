package de.idiotischer.bob.time;

import java.util.function.Consumer;

public interface TimeResolver {
    void addTimeHook(String id, long days, Consumer<Long> callback);

    void removeTimeHook(String s);
}
