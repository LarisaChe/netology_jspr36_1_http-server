package ru.netology.lache;

import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Handlers {
    //public static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    public static ConcurrentHashMap<String, ConcurrentHashMap<Methods, HandlerFunction>> handlers = new ConcurrentHashMap<>();

    public static boolean loadHandlers () {
        addHandler("/classic.html", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            final String template = Files.readString(r.filePath);
            final byte[] content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, content.length, content, null);
        });
        addHandler("/index.html", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/spring.svg", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/spring.png", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/resources.html", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/styles.css", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/app.js", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/links.html", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/forms.html", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/events.html", Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        addHandler("/events.js",    Methods.GET, (Request r, BufferedOutputStream bo) -> {
            ThreadForPool.sendRespond(bo, StatusCode.S200.getCommand(), r.mimeType, Files.size(r.filePath), null, r.filePath);
        });
        return true;
    }

    public static boolean validPaths(String path) {
        return handlers.keySet().contains(path);
    }

    public static void addHandler(String path, Methods method, HandlerFunction function) {
        if (!handlers.containsKey(path)) {
            handlers.put(path, new ConcurrentHashMap<>());
        }
        handlers.get(path).put(method, function);
    }
}
