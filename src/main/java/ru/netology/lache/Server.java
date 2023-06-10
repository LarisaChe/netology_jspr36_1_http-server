package ru.netology.lache;

import java.io.IOException;
import java.net.ServerSocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Server {
    public void start(int port) {

        final ExecutorService threadPool = Executors.newFixedThreadPool(64);

        try (final ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {

                Future future = threadPool.submit(new ThreadForPool(serverSocket));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadPool.shutdown();
    }
}
