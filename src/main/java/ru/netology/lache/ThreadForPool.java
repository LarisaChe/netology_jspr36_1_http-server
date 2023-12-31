package ru.netology.lache;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ThreadForPool extends Thread {

    private static ServerSocket serverSocketThread;

    public ThreadForPool(ServerSocket serverSocket) {
        serverSocketThread = serverSocket;
        start();
    }

    @Override
    public void run() {

        while (true) {

            try (Socket socket = serverSocketThread.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());) {

                // read only request line for simplicity
                // must be in form GET /path HTTP/1.1
                String requestLine;
                requestLine = in.readLine();

                if (requestLine == null) continue;

                Map<Request, StatusCode> requestStringMap = Request.checkAndCreated(requestLine);
                Request request = requestStringMap.entrySet().iterator().next().getKey();
                if (request == null) {
                    sendBadRespond(out, requestStringMap.get(request).getCommand());
                    continue;
                }

                Handlers.handlers.get(request.path).get(request.method).handle(request, out);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendRespond(BufferedOutputStream out, String status, String mimeType, long length, byte[] content, Path filePath) {
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

    public static void sendBadRespond(BufferedOutputStream out, String status) {
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


