package ru.kevgen;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;

public class HttpServer implements Server {

    private final InetSocketAddress address;
    private final Router router;
    private ServerSocketChannel server;

    public HttpServer(int port, Router router) {
        this(new InetSocketAddress(port), router);
    }

    public HttpServer(InetSocketAddress address, Router router) {
        this.router = router;
        this.address = address;
    }

    @Override
    public void start() throws IOException {
        Selector selector = SelectorProvider.provider().openSelector();
        initializeServer(selector);
        runEventLoop(selector);
    }

    private void initializeServer(Selector selector) throws IOException {
        server = ServerSocketChannel.open().bind(address);
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void runEventLoop(Selector selector) throws IOException {
        while (true) {
            selector.select(key -> {
                if (!key.isValid()) {
                    return;
                }
                handleKey(selector, key);
            });
        }
    }

    private void handleKey(Selector selector, SelectionKey key) {
        try {
            if (key.isAcceptable()) {
                acceptConnection(selector, key);
            } else if (key.isReadable() && key.isWritable()) {
                handleRequest(key);
            }
        } catch (IOException e) {
            cancelKey(key);
        }
    }

    private void acceptConnection(Selector selector, SelectionKey key) throws IOException {
        SocketChannel clientChannel = server.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }

    private void handleRequest(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        try {
            HttpRequest request = new HttpRequest(clientChannel);
            request.parse();
            HttpResponse response = router.getHandler(request).handle(request);
            response.write(clientChannel);
        } catch (HttpRequest.BadRequestException e) {
            e.printStackTrace(); // Handle the exception appropriately
        } finally {
            clientChannel.close();
        }
    }

    private void cancelKey(SelectionKey key) {
        key.cancel();
        try {
            if (key.channel() != null) {
                key.channel().close();
            }
        } catch (IOException ex) {
            ex.printStackTrace(); // Handle the exception appropriately
        }
    }

    @Override
    public void close() throws IOException {
        if (server != null) {
            server.close();
        }
    }
}
