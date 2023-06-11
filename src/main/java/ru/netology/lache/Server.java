package ru.netology.lache;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    public Server() {
        if (!Handlers.loadHandlers()) {
            System.out.println("Не загрузились хендлеры!!!!");
        }
    }

    public void start(int port) {

        final ExecutorService threadPool = Executors.newFixedThreadPool(64);

        try (final ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                threadPool.submit(new ThreadForPool(serverSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadPool.shutdown();
    }

    public void addHandler(String methodStr, String path, HandlerFunction function) {
        Handlers.addHandler(path, Methods.valueOf(methodStr), function);
    }
}

