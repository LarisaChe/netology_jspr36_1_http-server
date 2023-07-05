package ru.netology.lache;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import javax.servlet.http.Part;
import java.io.*;
import java.net.URISyntaxException;
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
    private List<NameValuePair> queryParams;
    private List<NameValuePair> postParams;
    private List<FileItem> parts;
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

   /* public List<NameValuePair> getQueryParams() {
        return queryParams;
    }*/

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public Request(Methods method, String path, String body, List<NameValuePair> queryParams, List<NameValuePair> postParams, List<FileItem> parts, StatusCode statusCode) throws IOException {
        this.method = method;
        this.path = path;
        this.body = body;
        this.filePath = Path.of(".", dir, path);
        this.mimeType = Files.probeContentType(filePath);
        this.queryParams = queryParams;
        this.postParams = postParams;
        this.parts = parts;
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
                ", params=" + queryParams +
                '}';
    }

    public static Request readInCheckAndCreatedRequest(BufferedInputStream in, int limit) throws Exception {

        Log log = Log.getInstance();

        final byte[] buffer = new byte[limit];
        final int read = in.read(buffer);
        final String s = new String(buffer, StandardCharsets.UTF_8);
        log.log("BUFFER ", s);
        log.log("BUFFER end ", " ------------------------------------------------------------------- ");
        // ищем request line
        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        if (requestLineEnd == -1) {
            log.log("ERROR ", "Не найден запрос.");
            return new Request(null, "", null, null, null, null, StatusCode.S400);
        }

        // читаем request line
        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");

        if (requestLine.length < 2) {
            log.log("ERROR ", "Запрос неполный: " + requestLine);
            return new Request(null, "", null, null, null, null, StatusCode.S400);
        }

        if (requestLine.length > 3) {
            log.log("WARNING ", "В запросе больше трех частей: " + requestLine);
        }

        if (!Methods.isMethodExisted(requestLine[0])) {
            log.log("ERROR ", "Неизвестный метод '" + requestLine[0] + "' в запросе: " + requestLine);
            return new Request(null, "", null, null, null, null, StatusCode.S400);
        }
        final Methods method = Methods.valueOf(requestLine[0]);

        final String[] pathAndQuery = requestLine[1].split("\\?");
        final String path = pathAndQuery[0];
        if (!Handlers.validPaths(path) || !path.startsWith("/")) {
            log.log("ERROR ", "Неправильный путь '" + path + "' в запросе: " + requestLine);
            return new Request(null, "", null, null, null, null, StatusCode.S404);
        }
        log.log("INFO ", "Разобран запрос. Метод: " + method + " есть в списке допустимых методов. Путь: '" + path + "' есть в списке допустимых путей.");

        List<NameValuePair> queryParams = new ArrayList<>();
        if (pathAndQuery.length > 1) {
            queryParams = URLEncodedUtils.parse(pathAndQuery[1], StandardCharsets.UTF_8);
        }

        // ищем заголовки
        final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimiter.length;
        final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            log.log("ERROR ", "Не найдены заголовки.");
            return new Request(null, "", null, null, null, null, StatusCode.S400);
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

        List<NameValuePair> postParams = new ArrayList<>();
        List<FileItem> parts = new ArrayList<>();
        //List<NameValuePair> multyParams = new ArrayList<>();

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

                final Optional<String> contentType = extractHeader(headers, "Content-Type");
                System.out.println("contentType: " + contentType.toString());

                if (body != null && body.length() > 0) {
                    if (contentType.toString().indexOf("multipart/form-data") > -1) {
                        System.out.println("multipart/form-data");


                        RequestContext requestContext = new RequestContext() {
                            @Override
                            public String getCharacterEncoding() {
                                return StandardCharsets.UTF_8.displayName();
                            }

                            @Override
                            public String getContentType() {
                                System.out.println("ss: " + contentType.toString().substring(9,contentType.toString().length() - 1));
                                return  contentType.toString().substring(9,contentType.toString().length() - 1);// contentType.toString() // "multipart/form-data";
                            }
                            public String getBoundary() {
                             return contentType.toString().substring(contentType.toString().indexOf("=") + 1);
                            }

                            @Override
                            public int getContentLength() {
                                return length;
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                return in;//new ByteArrayInputStream(buffer);
                            }
                        };

                        System.out.println("uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu");
                        //System.out.println(requestContext.

                        //String ss = new String(requestContext., StandardCharsets.UTF_8);

                        DiskFileItemFactory factory = new DiskFileItemFactory();

                        // Set factory constraints
                        factory.setSizeThreshold(1000000);
                        factory.setRepository(new File("D:\\temp"));

                        // Create a new file upload handler
                        ServletFileUpload upload = new ServletFileUpload(factory);
                        // Set overall request size constraint
                        upload.setSizeMax(1000000);

                        //Part filePart = requestContext. .getPart(fieldname);
                        // Parse the request    List<FileItem>
                        parts = upload.parseRequest(requestContext);
                        System.out.println("parts: " + parts.toString());
                        log.log("INFO ", "parts: " + parts);

                        // Process the uploaded items
                        Iterator<FileItem> iter = parts.iterator();
                        while (iter.hasNext()) {
                            FileItem item = iter.next();
                            if (item.isFormField()) {
                                //processFormField(item);
                                /*String name = item.getFieldName();
                                String value = item.getString();*/
                                NameValuePair nvp = new BasicNameValuePair(item.getFieldName(), item.getString());
                                postParams.add(postParams.size() == 0 ? 1 : postParams.size(), nvp);
                                System.out.println("postParams: " + postParams);
                            } else {
                                // Process a file upload
                                System.out.println("item.getFieldName(): " + item.getFieldName());
                                File uploadedFile = new File(item.getFieldName());
                                item.write(uploadedFile);
                            }
                        }
                    } else {
                        postParams = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
                        log.log("INFO ", "postParams: " + postParams);
                    }
                }
            }

        }

        return new Request(method, path, body, queryParams, postParams, parts, StatusCode.S200);
    }

    /* private static void qwe() {
         // Create a factory for disk-based file items
         DiskFileItemFactory factory = new DiskFileItemFactory();

 // Set factory constraints
         factory.setSizeThreshold(yourMaxMemorySize);
         factory.setRepository(yourTempDirectory);

 // Create a new file upload handler
         ServletFileUpload upload = new ServletFileUpload(factory);

 // Set overall request size constraint
         upload.setSizeMax(yourMaxRequestSize);

 // Parse the request
         List<FileItem> items = upload.parseRequest(request);
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
        return this.queryParams.stream().filter(x -> x.getName().equals(name)).findFirst();
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public Map<String, List<String>> getPostParam(String name) {
        Map<String, List<String>> result = new HashMap<>();
        for (NameValuePair item : this.queryParams) {
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
        for (NameValuePair item : this.queryParams) {
            if (!result.containsKey(item.getName())) {
                result.put(item.getName(), new ArrayList<>());
            }
            result.get(item.getName()).add(item.getValue());
        }
        // System.out.println("result из getPostParams(): " + result);
        return result;
    }

    public void getPart(String name) {  // FileItem

    }

    public void getParts() {  // FileItem

    }

}
