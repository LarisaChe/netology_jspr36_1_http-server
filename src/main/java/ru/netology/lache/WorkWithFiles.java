package ru.netology.lache;

import java.io.FileWriter;
import java.io.IOException;

public class WorkWithFiles {
    public static void saveToFile(String fileName, String str, boolean append) throws IOException {
        try (FileWriter writer = new FileWriter(fileName, append)) {
            writer.write(str);
            writer.append('\n');
            writer.flush();
        }
    }
}
