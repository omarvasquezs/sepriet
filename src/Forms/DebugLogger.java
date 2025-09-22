package Forms;

import java.nio.file.*;
import java.time.*;

/**
 * Small append-only logger used to debug WhatsApp phone prefill issues in
 * production. Writes to sepriet_send.log in the application working directory.
 */
public final class DebugLogger {
    private DebugLogger() {
    }

    public static void log(String tag, String message) {
        try {
            Path p = Paths.get("sepriet_send.log");
            String line = LocalDateTime.now().toString() + " [" + tag + "] " + message + System.lineSeparator();
            Files.writeString(p, line, java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception ignore) {
        }
    }
}
