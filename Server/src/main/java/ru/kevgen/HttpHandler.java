package ru.kevgen;

public interface HttpHandler {
    HttpResponse handle(HttpRequest request);
}

