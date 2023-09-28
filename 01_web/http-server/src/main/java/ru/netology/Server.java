package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int THREAD_POOL_SIZE = 64;
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    public Server() {
        try {
            serverSocket = new ServerSocket(9999);
            threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Server started.");

        while (true) {
            try {
                var socket = serverSocket.accept();
                System.out.println("New connection accepted.");

                // Обработка подключения в потоке из пула
                threadPool.execute(() -> handleConnection(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            var requestLine = in.readLine();
            var parts = requestLine.split(" ");

            if (parts.length != 3) {
                return;
            }

            var path = parts[1];

            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            var filePath = Path.of(".", "public", path);
            var mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                var template = Files.readString(filePath);
                var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                );
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length() + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content.getBytes());
                out.flush();
                return;
            }

            var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

