package ru.kevgen;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


public class Main {
    public static final int PORT = 8080;


    private static class PersonHandler implements HttpHandler {

        private static final Map<String, String> persons = new HashMap<>();

        @Override
        public HttpResponse handle(HttpRequest request) {
            Supplier<IllegalArgumentException> nameError = () ->
                    new IllegalArgumentException("Should specify name in the query\n");

            String query = Optional.ofNullable(request.getTarget().getQuery())
                    .orElseThrow(nameError);

            String name = Arrays.stream(query.split("&"))
                    .filter(s -> s.startsWith("name="))
                    .findAny()
                    .orElseThrow(nameError)
                    .substring("name=".length());

            if (Methods.PUT.equals(request.getMethod())) {
                String put = persons.put(name, request.getBody());
                return HttpResponse.of(put == null ? Status.Created : Status.Ok, "");
            }
            if (Methods.GET.equals(request.getMethod())) {
                String data = persons.get(name);
                if (data == null)
                    return HttpResponse.of(Status.NotFound, "");
                return HttpResponse.of(Status.Ok, data);
            }
            throw new IllegalStateException("Unsupported HTTP method");
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        Router router = new Router()
                .addDefaultHandler(request -> {
                    return HttpResponse.of(Status.NotFound, "Not Found");
                })
                .addRoute(new URI("/"), EnumSet.of(Methods.GET), request -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Your fields are:\n");
                    for (var entry : request.headers.entrySet()) {
                        sb.append(entry.getKey()).append(":\t").append(entry.getValue()).append('\n');
                    }
                    return HttpResponse.of(Status.Ok, sb.toString());
                })
                .addRoute(new URI("/person"), EnumSet.of(Methods.GET, Methods.PUT), new PersonHandler());

        try (HttpServer server = new HttpServer(PORT, router)) {
            CompletableFuture.runAsync(() -> {
                try {
                    server.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).exceptionally(e -> {
                return null;
            });

            try (Scanner scanner = new Scanner(System.in)) {
                String input;
                do {
                    input = scanner.next();
                } while (!"stop".equals(input));
            }

        }
    }
}
