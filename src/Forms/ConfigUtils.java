package Forms;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class ConfigUtils {
    private static int maxDiasRecojo = -1;

    public static int getMaxDiasRecojo() {
        if (maxDiasRecojo > 0)
            return maxDiasRecojo;
        try {
            Path dbJsonPath = Path.of("db_settings.json");
            if (Files.exists(dbJsonPath)) {
                String json = new String(Files.readAllBytes(dbJsonPath), StandardCharsets.UTF_8);
                String pattern = "\\\"max_dias_recojo\\\"\\s*:\\s*\\\"(\\d+)\\\"";
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
                if (m.find()) {
                    maxDiasRecojo = Integer.parseInt(m.group(1));
                    return maxDiasRecojo;
                }
            }
        } catch (Exception ignored) {
        }
        maxDiasRecojo = 15;
        return maxDiasRecojo;
    }
}
