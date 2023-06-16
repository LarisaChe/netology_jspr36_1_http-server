package ru.netology.lache;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class Request {
    final public static String dir = "public";
    //String method;
    private Methods method;
    private String path;
    private String body;
    private Path filePath;
    private String mimeType;
    private List<NameValuePair> params;
    private StatusCode statusCode;

    public Methods getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public List<NameValuePair> getParams() {
        return params;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public Request(Methods method, String path, String body, List<NameValuePair> params, StatusCode statusCode) throws IOException {
        this.method = method;
        this.path = path;
        this.body = body;
        this.filePath = Path.of(".", dir, path);
        this.mimeType = Files.probeContentType(filePath);
        this.params = params;
        this.statusCode = statusCode;
    }


    @Override
    public String toString() {
        return "Request{" +
                "method=" + method +
                ", path='" + path + '\'' +
                ", body='" + body + '\'' +
                ", filePath=" + filePath +
                ", mimeType='" + mimeType + '\'' +
                ", params=" + params +
                '}';
    }

    public static Request readInCheckAndCreatedRequest(BufferedInputStream in, int limit) throws IOException {

        Log log = Log.getInstance();

        final byte[] buffer = new byte[limit];
        final int read = in.read(buffer);

        // ищем request line
        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        if (requestLineEnd == -1) {
            log.log("ERROR ", "Не найден запрос.");
            return new Request(null,"",null,null, StatusCode.S400);
        }

        // читаем request line
        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

        if (requestLine.length < 2) {
            log.log("ERROR ", "Запрос неполный: " + requestLine);
            return new Request(null,"",null,null, StatusCode.S400);
        }

        if (requestLine.length > 3) {
            log.log("WARNING ", "В запросе больше трех частей: " + requestLine);
        }

        if (!Methods.isMethodExisted(requestLine[0])) {
            log.log("ERROR ", "Неизвестный метод '" + requestLine[0] + "' в запросе: " + requestLine);
            return new Request(null,"",null,null, StatusCode.S400);
        }
        final Methods method = Methods.valueOf(requestLine[0]);

        final String[] pathAndParams = requestLine[1].split("\\?");
        final String path = pathAndParams[0];
        if (!Handlers.validPaths(path) || !path.startsWith("/")) {
            log.log("ERROR ", "Неправильный путь '" + path + "' в запросе: " + requestLine);
            return new Request(null,"",null,null, StatusCode.S404);
        }
        log.log("INFO ", "Разобран запрос. Метод: " + method + " есть в списке допустимых методов. Путь: '" + path + "' есть в списке допустимых путей.");

        List<NameValuePair> paramsR = new ArrayList<>();
        if (pathAndParams.length > 1) {
            paramsR = URLEncodedUtils.parse(pathAndParams[1], StandardCharsets.UTF_8);
        }

        // ищем заголовки
        final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimiter.length;
        final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            log.log("ERROR ", "Не найдены заголовки.");
            return new Request(null,"",null,null, StatusCode.S400);
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        log.log("INFO ", "Разобраны заголовки: " + headers);
        //System.out.println(headers);

        String body = null;

        // для GET тела нет
        if (!method.equals(Methods.GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final Optional<String> contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final int length = Integer.parseInt(contentLength.get());
                final byte[] bodyBytes = in.readNBytes(length);

                body = new String(bodyBytes);
                log.log("INFO ", "Тело запроса: " + body);
                paramsR.addAll(URLEncodedUtils.parse(body, StandardCharsets.UTF_8));

                final Optional<String> contentType = extractHeader(headers, "Content-Type");
                System.out.println("contentType: " + contentType);
                final Optional<String> boundary = extractHeader(headers, "boundary");
                System.out.println("boundary: " + boundary);
            }
        }

        return new Request(method, path, body, paramsR, StatusCode.S200);
    }

   // from google guava with modifications
   private static int indexOf(byte[] array, byte[] target, int start, int max) {
       outer:
       for (int i = start; i < max - target.length + 1; i++) {
           for (int j = 0; j < target.length; j++) {
               if (array[i + j] != target[j]) {
                   continue outer;
               }
           }
           return i;
       }
       return -1;
   }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
    
    public Optional<NameValuePair> getQueryParam(String name) {
        return this.params.stream().filter(x -> x.getName().equals(name)).findFirst();
    }
    public List<NameValuePair> getQueryParams() {
        return this.params;
    }
    public Map<String, List<String>> getPostParam(String name) {
        Map<String, List<String>> result = new HashMap<>();
        for (NameValuePair item : this.params) {
            if (item.getName().equals(name)) {
                if (!result.containsKey(item.getName())) {
                    result.put(item.getName(), new ArrayList<>());
                }
                result.get(item.getName()).add(item.getValue());
            }
        }
        // System.out.println("result из getPostParam(" + name + "): " + result);
        return result;
    }
    public Map<String, List<String>> getPostParams() {
        Map<String, List<String>> result = new HashMap<>();
        for (NameValuePair item : this.params) {
            if (!result.containsKey(item.getName())) {
                result.put(item.getName(), new ArrayList<>());
            }
            result.get(item.getName()).add(item.getValue());
        }
        // System.out.println("result из getPostParams(): " + result);
        return result;
    }
}
