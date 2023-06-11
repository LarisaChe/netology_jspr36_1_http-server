package ru.netology.lache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;



public class Request {
    final public static String dir = "public";
    //String method;
    Methods method;
    String path;
    String body;
    Path filePath;
    String mimeType;
    public Request(Methods method, String path, String body) throws IOException {
        this.method = method;
        this.path = path;
        this.body = body;
        this.filePath = Path.of(".", dir, path);
        this.mimeType = Files.probeContentType(filePath);
    }

    public static HashMap<Request, StatusCode> checkAndCreated(String requestLine) throws IOException {
        HashMap<Request, StatusCode> result =  new HashMap<>();
        Log log = Log.getInstance();

        final String[] parts = requestLine.split(" ");

        if (parts.length < 2) {
            log.log("ERROR ", "Запрос неполный: " + requestLine);
            result.put(null, StatusCode.S400);
            return result;
        }
        if (parts.length == 2) {
            log.log("WARNING ", "В запросе нет body: " + requestLine);
        }

        if (parts.length > 3) {
            log.log("WARNING ", "В запросе больше трех частей: " + requestLine);
        }

        if (!Methods.check(parts[0])) {
            log.log("ERROR ", "Неизвестный метод '" + parts[0] + "' в запросе: " + requestLine);
            result.put(null, StatusCode.S400);
            return result;
        }

        final String path = parts[1];
        if (!Handlers.validPaths(path)) {
            log.log("ERROR ", "Неправильный путь '" + path + "' в запросе: " + requestLine);
            result.put(null, StatusCode.S404);
            return result;
        }
        result.put(new Request(Methods.valueOf(parts[0]), parts[1], parts[2]), StatusCode.S200);

        return result;
    }
}
