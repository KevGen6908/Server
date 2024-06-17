package ru.kevgen;

public enum Methods {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE;

    public static Methods of(String method) {
        return switch (method) {
            case "GET" -> GET;
            case "POST" -> POST;
            case "PUT" -> PUT;
            case "PATCH" -> PATCH;
            case "DELETE" -> DELETE;
            default -> throw new IllegalArgumentException("HTTP method " + method + " is not implemented");
        };
    }
}
