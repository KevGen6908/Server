package ru.kevgen;

import java.net.URI;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Router {
    private final Map<String, EnumMap<Methods, HttpHandler>> routeMap = new TreeMap<>();
    private final EnumMap<Methods, HttpHandler> defaultHandlers = new EnumMap<>(Methods.class);

    public Router addRoute(URI uri, EnumSet<Methods> methods, HttpHandler handler) {
        String normalizedPath = uri.normalize().getPath();
        EnumMap<Methods, HttpHandler> methodHandlerMap = methods.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        method -> handler,
                        (existing, replacement) -> replacement,
                        () -> new EnumMap<>(Methods.class)
                ));

        routeMap.merge(normalizedPath, methodHandlerMap, (existingMap, newMap) -> {
            existingMap.putAll(newMap);
            return existingMap;
        });

        return this;
    }

    public Router addDefaultHandler(EnumSet<Methods> methods, HttpHandler handler) {
        for (Methods method : methods) {
            defaultHandlers.put(method, handler);
        }
        return this;
    }

    public Router addDefaultHandler(HttpHandler handler) {
        return addDefaultHandler(EnumSet.allOf(Methods.class), handler);
    }

    public HttpHandler getHandler(URI uri, Methods method) {
        String normalizedPath = uri.normalize().getPath();
        HttpHandler handler = routeMap.getOrDefault(normalizedPath, defaultHandlers)
                .getOrDefault(method, defaultHandlers.get(method));

        if (handler == null) {
            throw new NoSuchElementException("No handler found for " + method + " " + normalizedPath);
        }

        return handler;
    }

    public HttpHandler getHandler(HttpRequest request) {
        return getHandler(request.getTarget(), request.getMethod());
    }
}
