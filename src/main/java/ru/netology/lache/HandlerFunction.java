package ru.netology.lache;

import java.io.BufferedOutputStream;
import java.io.IOException;

//@FunctionalInterface
public interface HandlerFunction {
    public void handle(Request r, BufferedOutputStream responseStream) throws IOException;
}