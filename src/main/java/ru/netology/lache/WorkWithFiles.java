package ru.netology.lache;

import java.io.FileWriter;
import java.io.IOException;

public class WorkWithFiles {
    public static void saveToFile(String fn, String str, boolean append) throws IOException {
        try (FileWriter writer = new FileWriter(fn, append)) {
            writer.write(str);
            writer.append('\n');
            writer.flush();
        }
    }
}
