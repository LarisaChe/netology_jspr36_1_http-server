package ru.netology;

import ru.netology.lache.HandlerFunction;
import ru.netology.lache.Handlers;
import ru.netology.lache.Request;
import ru.netology.lache.Server;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Handler;

public class Main {
  public static void main(String[] args) throws IOException {
    int port = 9999;
    Server server = new Server();

    // добавление хендлеров (обработчиков)
    server.addHandler("GET", "/messages", new HandlerFunction() {
      @Override
      public void handle(Request request, BufferedOutputStream responseStream) {
        // TODO: handlers code
      }
    });
    server.addHandler("POST", "/messages",new HandlerFunction()  {
      public void handle(Request request, BufferedOutputStream responseStream) {
        // TODO: handlers code
      }
    });

    System.out.println(Handlers.getHandlers());

    server.start(port);
  }
}


