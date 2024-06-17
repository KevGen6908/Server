# Server

This project implements a simple Http server model. Implemented part of the HTTP 1.1 protocol.

  Namely:
  GET
  POST
  PUT
  DELETE
  PATCH

The headers are also stored as a map. The server supports creation on the specified host and port. Implemented listening by a specific path and method and creating and sending an http response back to the client.

By default, the server is running on port 8080. To run it, you need to run Main and go to http://localhost:8080/.

Or use the  command in the terminal  "curl 'http://localhost:8080 /' (other parameters)"
