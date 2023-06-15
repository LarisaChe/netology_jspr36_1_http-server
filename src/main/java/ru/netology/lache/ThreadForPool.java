package ru.netology.lache;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ThreadForPool extends Thread {

    private static ServerSocket serverSocketThread;

    private static Log log = Log.getInstance();
    public ThreadForPool(ServerSocket serverSocket) {
        serverSocketThread = serverSocket;
        start();
    }

    @Override
    public void run() {

        while (true) {

            try (Socket socket = serverSocketThread.accept();
                 //BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                 BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());) {

                final int limit = 4096;
                in.mark(limit);

                Map<Request, StatusCode> requestStringMap = Request.readInCheckAndCreatedRequest(in, limit);
                /* String requestLine;
                requestLine = in.readLine();

                if (requestLine == null) continue;

                Map<Request, StatusCode> requestStringMap = Request.checkAndCreated(requestLine);*/
                Request request = requestStringMap.entrySet().iterator().next().getKey();
                if (request == null) {
                    sendBadRespond(out, requestStringMap.get(request).getCommand());
                    continue;
                }

                synchronized (request) {
                    System.out.println(request);
                    if (request.params.size() > 0) {  // ДЗ Query
                        log.log("INFO Все параметры: ", request.getQueryParams().toString());
                        log.log("INFO Параметр last: ", request.getQueryParam("last").toString());
                    }
                }

                synchronized (request) {
                    System.out.println(request);
                    if (request.params.size() > 0) {  // ДЗ x-www-form-urlencoded* (задача со звёздочкой)
                        log.log("INFO Все параметры: ", request.getPostParams().toString());
                        log.log("INFO Параметр last: ", request.getPostParam("value").toString());
                    }
                }

                Handlers.handlers.get(request.path).get(request.method).handle(request, out);

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    log.log("IOException ", String.valueOf(e));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
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


