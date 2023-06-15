package ru.netology.lache;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


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

    //public static HashMap<Request, StatusCode> checkAndCreated(String requestLine) throws IOException {
    public static HashMap<Request, StatusCode> readInCheckAndCreatedRequest(BufferedInputStream in, int limit) throws IOException {
        HashMap<Request, StatusCode> result =  new HashMap<>();
        Log log = Log.getInstance();

        final byte[] buffer = new byte[limit];
        final int read = in.read(buffer);

        // ищем request line
        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        if (requestLineEnd == -1) {
            log.log("ERROR ", "Не найден запрос.");
            result.put(null, StatusCode.S400);
            return result;
        }

        // читаем request line
        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        //final String[] parts = requestLine.split(" ");

        if (requestLine.length < 2) {
            log.log("ERROR ", "Запрос неполный: " + requestLine);
            result.put(null, StatusCode.S400);
            return result;
        }

        if (requestLine.length > 3) {
            log.log("WARNING ", "В запросе больше трех частей: " + requestLine);
        }

        if (!Methods.check(requestLine[0])) {
            log.log("ERROR ", "Неизвестный метод '" + requestLine[0] + "' в запросе: " + requestLine);
            result.put(null, StatusCode.S400);
            return result;
        }
        final Methods method = Methods.valueOf(requestLine[0]);

        final String[] pathAndParams = requestLine[1].split("\\?");
        final String path = pathAndParams[0];
        if (!Handlers.validPaths(path) || !path.startsWith("/")) {
            log.log("ERROR ", "Неправильный путь '" + path + "' в запросе: " + requestLine);
            result.put(null, StatusCode.S404);
            return result;
        }
        log.log("INFO ", "Разобран запрос. Метод: " + method + " в списке допустимых методов. Путь: '" + path + "' в списке допустимых путей.");

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
            result.put(null, StatusCode.S400);
            return result;
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

                /*final Optional<String> contentType = extractHeader(headers, "Content-Type");
                //final String charsetName = "------WebKitFormBoundaryD5kRmGTp9gyXGA0T";
                 if (contentType.toString().contains("multipart/form-data")) {
                   // paramsR = URLEncodedUtils.parse(body, StandardCharsets.UTF_8, Charset.forName(charsetName);
                }
                else {
                    paramsR.add(URLEncodedUtils.parse(body, StandardCharsets.UTF_8));
                }*/

            }
        }


        result.put(new Request(method, path, body, paramsR), StatusCode.S200);

        return result;
    }

   /* public static Map<Request, StatusCode> checkAndCreated1(BufferedInputStream in, byte[] buffer) {
    }*/
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

}
