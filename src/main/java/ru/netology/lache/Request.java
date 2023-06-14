package ru.netology.lache;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


public class Request {
    final public static String dir = "public";
    //String method;
    Methods method;
    String path;
    String body;
    Path filePath;
    String mimeType;
    List<NameValuePair> params;

    public Request(Methods method, String path, String body, List<NameValuePair> params) throws IOException {
        this.method = method;
        this.path = path;
        this.body = body;
        this.filePath = Path.of(".", dir, path);
        this.mimeType = Files.probeContentType(filePath);
        this.params = params;
    }

    public Request(Methods method, String path, String body, Path filePath, String mimeType, List<NameValuePair> params) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.filePath = filePath;
        this.mimeType = mimeType;
        this.params = params;
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

        final String[] pathAndParams = parts[1].split("\\?");
        final String path = pathAndParams[0];
        if (!Handlers.validPaths(path) || !path.startsWith("/")) {
            log.log("ERROR ", "Неправильный путь '" + path + "' в запросе: " + requestLine);
            result.put(null, StatusCode.S404);
            return result;
        }

        List<NameValuePair> paramsR = new ArrayList<>();
        if (pathAndParams.length > 1) {
            paramsR = URLEncodedUtils.parse(pathAndParams[1], StandardCharsets.UTF_8);
        }

        result.put(new Request(Methods.valueOf(parts[0]), path, parts[2], paramsR), StatusCode.S200);

        return result;
    }

    public Optional<NameValuePair> getQueryParam(String name) {
        return this.params.stream().filter(x -> x.getName().equals(name)).findFirst();
    }
    public List<NameValuePair> getQueryParams() {
        return this.params;
    }
}
