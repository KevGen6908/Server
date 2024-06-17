package ru.kevgen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    final Status status;

    final Map<String, String> fields;

    final ByteBuffer body;

    public HttpResponse(Status status, Map<String, String> fields, ByteBuffer body) {
        this.status = status;
        this.fields = fields;
        this.body = body;
    }

    public HttpResponse(Status status, ByteBuffer body) {
        this(status,new HashMap<>(), body);
    }

    private void writeHead(PrintWriter writer) {
        fields.put("Content-Length", String.valueOf(body.limit() - body.position()));
        fields.putIfAbsent("Content-Type", "text/plain");

        writer.printf("HTTP/1.1 %1d %2s\r\n", status.statusCode, status.statusText);
        for (var entry : fields.entrySet()) {
            writer.printf("%1s: %2s\r\n", entry.getKey(), entry.getValue());
        }
        writer.print("\r\n");
        writer.close();
    }

    void write(WritableByteChannel channel) throws IOException {
        StringWriter sw = new StringWriter();
        writeHead(new PrintWriter(sw));
        channel.write(ByteBuffer.wrap(sw.toString().getBytes()));
        while (body.hasRemaining()) {
            channel.write(body);
        }
    }

    public static HttpResponse of(Status status, String body) {
        return new HttpResponse(status, ByteBuffer.wrap(body.getBytes()));
    }
}