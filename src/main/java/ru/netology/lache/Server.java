package ru.netology.lache;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    public Server() {
        if (!Handlers.loadHandlers()) {
            System.out.println("Не загрузились хендлеры!!!!");
        }
    }

    public void start(int port) throws IOException {

        final ExecutorService threadPool = Executors.newFixedThreadPool(64);

        final ServerSocket serverSocket = new ServerSocket(port);
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(new ThreadForPool(socket));
            }

        } finally {
            threadPool.shutdown();

            serverSocket.close();
        }

    }

    public void addHandler(String methodStr, String path, HandlerFunction function) {
        Handlers.addHandler(path, Methods.valueOf(methodStr), function);
    }
}

