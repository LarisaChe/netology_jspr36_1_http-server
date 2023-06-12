package ru.netology.lache;

import java.io.IOException;
import java.time.LocalDateTime;

public class Log {
    private static String logFileName = "jspr36_1.log";
    private static Log INSTANCE = null;
    private Log() {}
    public static Log getInstance() {
        if (INSTANCE == null) {
            synchronized (Log.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Log();
                }
            }
        }
        return INSTANCE;
    }
    public void log(String level, String msg) throws IOException {

        String logMsg = "[" + LocalDateTime.now() + "] " + level + ": " + msg;
        boolean append = true;
        WorkWithFiles.saveToFile(logFileName, logMsg, append);
    }

}
