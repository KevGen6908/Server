package ru.kevgen;

public enum Status {
    NotImplemented(501, "Not implemented"),
    HttpVersionNotSupported(505, "HTTP Version Not Supported"),
    BadRequest(400, "Bad Request"),

    NotFound(404, "Not Found"),

    LengthRequired(411, "Length Required"),
    InternalServerError(500, "Internal Server Error"),
    Ok(200, "OK"), Created(201, "Created");

    public final int statusCode;
    public final String statusText;

    Status(int statusCode, String statusText) {
        this.statusCode = statusCode;
        this.statusText = statusText;
    }
}