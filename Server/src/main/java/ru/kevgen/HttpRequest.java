package ru.kevgen;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HttpRequest {

    static class ByteCharSequence implements CharSequence {
        private final ByteBuffer internal;

        private ByteCharSequence(ByteBuffer internal) {
            this.internal = internal;
        }

        @Override
        public int length() {
            return internal.remaining();
        }

        @Override
        public char charAt(int index) {
            return (char)internal.get(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new ByteCharSequence(internal.slice(start, end - start));
        }

        public static CharSequence of(ByteBuffer buffer) {
            return new ByteCharSequence(buffer);
        }
    }

    private static final Pattern CRLN = Pattern.compile("\r\n");
    private static final Pattern CRLNCRLN = Pattern.compile("\r\n\r\n");
    private static final Pattern SP = Pattern.compile(" ");
    public static final Pattern VERSION = Pattern.compile("HTTP/1\\.\\d");
    public static final Pattern TOKEN = Pattern.compile("[!#$%&'*+-.^`|~\\w]+");
    public static final Pattern STRIP_REGEX = Pattern.compile("[ \t]*(.*)[ \t]*");
    public static final Pattern OBSOLETE_FOLD = Pattern.compile("^[ \t].*");

    public static final int BUFFER_CAPACITY_BYTES = 8192;

    private final ReadableByteChannel byteChannel;
    private Methods method;
    private URI target;
    private List<ByteBuffer> body;
    final Map<String, String> headers = new HashMap<>();

    public HttpRequest(ReadableByteChannel channel) {
        this.byteChannel = channel;
    }

    public String getBody() {
        return body.stream().map(b -> StandardCharsets.UTF_8.decode(b).toString()).collect(Collectors.joining());
    }

    public Methods getMethod() {
        return method;
    }

    public URI getTarget() {
        return target;
    }

    public void parse() throws IOException, BadRequestException {
        ByteBuffer buf = ByteBuffer.allocate(BUFFER_CAPACITY_BYTES);
        Matcher matcher = CRLNCRLN.matcher(ByteCharSequence.of(buf));
        while (true) {
            int bytesRead = byteChannel.read(buf);
            matcher.reset();
            if (matcher.find()) {
                break;
            }
            if (bytesRead == -1) {
                throw new BadRequestException("No header separator found");
            }
        }
        ByteBuffer header = buf.slice(0, matcher.end());
        buf.flip();
        buf.position(matcher.end()); // rest of buffer content is the body
        Scanner scanner = new Scanner(StandardCharsets.UTF_8.decode(header).toString());
        parseRequestLine(scanner);
        parseHeaders(scanner);
        scanner.skip(CRLN);
        switch (method) {
            case PATCH:
            case PUT:
            case POST:
                body = readBody(buf);
                break;
            default:
                /* nothing to do */
        }
    }

    private void parseRequestLine(Scanner scanner) throws BadRequestException {
        scanner.useDelimiter(SP);
        try {
            method = Methods.of(scanner.next());
        } catch (NoSuchElementException e) {
            throw new BadRequestException("Invalid HTTP method");
        }
        try {
            target = new URI(scanner.next());
        } catch (URISyntaxException e) {
            throw new BadRequestException("Invalid URI");
        }
        scanner.skip(SP);
        scanner.useDelimiter(CRLN);
        try {
            scanner.next(VERSION);
        } catch (NoSuchElementException e) {
            throw new BadRequestException("Unsupported HTTP version");
        }
    }

    private void parseHeaders(Scanner scanner) throws BadRequestException {
        scanner.useDelimiter(CRLN);
        String line;
        try {
            while (!(line = scanner.next()).isEmpty()) {
                if (OBSOLETE_FOLD.matcher(line).matches()) {
                    throw new BadRequestException("Obsolete line folding in header");
                }
                var keyValue = line.split(":", 2);
                if (keyValue.length < 2) {
                    throw new BadRequestException("Invalid header format");
                }
                if (!TOKEN.matcher(keyValue[0]).matches()) {
                    throw new BadRequestException("Invalid header field name");
                }
                Matcher m = STRIP_REGEX.matcher(keyValue[1]);
                boolean matches = m.matches();
                assert matches;
                headers.put(keyValue[0], m.group(1));
            }
        } catch (NoSuchElementException e) {
            throw new BadRequestException("Incomplete headers");
        }
    }

    private List<ByteBuffer> readBody(ByteBuffer part) throws IOException, BadRequestException {
        String contentLength = headers.get("Content-Length");
        if (contentLength == null) {
            throw new BadRequestException("Content-Length header required");
        }
        int length = Integer.parseInt(contentLength);
        if (length <= part.remaining()) {
            part.limit(part.position() + length);
            return List.of(part);
        }
        ByteBuffer buffer2 = ByteBuffer.allocate(length - part.remaining());
        while (byteChannel.read(buffer2) > 0) {
            // Read the remaining part of the body
        }
        if (buffer2.hasRemaining()) {
            throw new BadRequestException("Content-Length is larger than the body size");
        }
        return List.of(part, buffer2);
    }

    public static class BadRequestException extends Exception {
        public BadRequestException(String message) {
            super(message);
        }
    }
}
