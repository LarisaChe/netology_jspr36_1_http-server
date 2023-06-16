package ru.netology.lache;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThreadForPool extends Thread {
    private Socket socket;
    private static Log log = Log.getInstance();

    public ThreadForPool(Socket socket) {
        this.socket = socket;
        start();
    }

    @Override
    public void run() {

        try (BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());) {

            final int limit = 4096;
            in.mark(limit);

            Request request = Request.readInCheckAndCreatedRequest(in, limit);

            if (request.getStatusCode().equals(StatusCode.S400) || request.getStatusCode().equals(StatusCode.S404)) {
                sendBadRespond(out, request.getStatusCode().getMessage());
            } else {
                if (request.getParams().size() > 0) {  // ДЗ Query
                    log.log("INFO Все параметры: ", request.getQueryParams().toString());
                    log.log("INFO Параметр last: ", request.getQueryParam("last").toString());
                }

                Handlers.getHandlers().get(request.getPath()).get(request.getMethod()).handle(request, out);
            }
        } catch (IOException e) {
            //e.printStackTrace();
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


