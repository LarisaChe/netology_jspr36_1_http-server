package ru.netology.lache;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ThreadForPool extends Thread {

    public static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private static ServerSocket serverSocketThread;

    public ThreadForPool(ServerSocket serverSocket) {  //(Socket socket) throws IOException {
        serverSocketThread = serverSocket;
        start();
    }

    @Override
    public void run() {

        while (true) {

            try (final var socket = serverSocketThread.accept();
                 final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final var out = new BufferedOutputStream(socket.getOutputStream());) {

                // read only request line for simplicity
                // must be in form GET /path HTTP/1.1
                final String requestLine;

                requestLine = in.readLine();


                if (requestLine == null) continue;

                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    // just close socket
                    sendBadRespond(out, "400 Bad Request");
                    continue;
                }

                final var path = parts[1];
                if (!validPaths.contains(path)) {
                    sendBadRespond(out, "404 Not Found");
                    continue;
                }

                final var filePath = Path.of(".", "public", path);
                final var mimeType = Files.probeContentType(filePath);

                // special case for classic
                if (path.equals("/classic.html")) {
                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    sendRespond(out, "200 OK", mimeType, content.length, content, null);
                    continue;
                }

                sendRespond(out, "200 OK", mimeType, Files.size(filePath), null, filePath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendRespond(BufferedOutputStream out, String status, String mimeType, long length, byte[] content, Path filePath) {
        try {
            out.write((
                    "HTTP/1.1 " + status + "\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            if (content != null) {
                out.write(content);
            }
            if (filePath != null) {
                Files.copy(filePath, out);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBadRespond(BufferedOutputStream out, String status) {
        try {
            out.write((
                    "HTTP/1.1 " + status + "\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
