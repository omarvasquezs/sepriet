package Forms;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Simple updater that checks GitHub releases for updates and installs into a
 * portable layout (app/ and runtime/ present). Designed for basic portable
 * deployments. It downloads the release asset (zip or jar) and replaces the
 * files under the portable "app" folder.
 */
public class Updater {
    private static final String REPO_API_LATEST = "https://api.github.com/repos/omarvasquezs/sepriet/releases/latest";

    public static void checkAndInstallIfPortable(JFrame parent) {
        try {
            File portableRoot = findPortableRoot();
            if (portableRoot == null) {
                return; // not running from portable layout
            }

            // Query GitHub latest release
            String json = httpGet(REPO_API_LATEST);
            if (json == null || json.isEmpty())
                return;

            // extract release tag/name (simple key lookup)
            String tag = extractJsonString(json, "tag_name", 0);
            if (tag == null)
                tag = extractJsonString(json, "name", 0);
            if (tag == null)
                tag = "(unknown)";

            // find asset pairs (name + browser_download_url) inside assets array
            String bestUrl = null;
            String bestName = null;
            int assetsIdx = json.indexOf("\"assets\"");
            if (assetsIdx >= 0) {
                int arrStart = json.indexOf('[', assetsIdx);
                int arrEnd = json.indexOf(']', arrStart);
                if (arrStart >= 0 && arrEnd > arrStart) {
                    String assetsBlock = json.substring(arrStart, arrEnd + 1);
                    int pos = 0;
                    while (true) {
                        String name = extractJsonString(assetsBlock, "name", pos);
                        String url = extractJsonString(assetsBlock, "browser_download_url", pos);
                        if (name == null || url == null)
                            break;
                        // prefer zip, then jar
                        if (bestUrl == null) {
                            bestUrl = url;
                            bestName = name;
                        } else if (name.toLowerCase().endsWith(".zip") && !bestName.toLowerCase().endsWith(".zip")) {
                            bestUrl = url;
                            bestName = name;
                        } else if (name.toLowerCase().endsWith(".jar") && !bestName.toLowerCase().endsWith(".zip")
                                && !bestName.toLowerCase().endsWith(".jar")) {
                            bestUrl = url;
                            bestName = name;
                        }
                        pos = Math.max(assetsBlock.indexOf(name, pos) + name.length(),
                                assetsBlock.indexOf(url, pos) + url.length());
                        if (pos < 0)
                            break;
                    }
                }
            }

            if (bestUrl == null) {
                // No downloadable asset found
                return;
            }

            // read local installed version if present
            File appDir = new File(portableRoot, "app");
            File verFile = new File(appDir, "version.txt");
            String localVer = null;
            if (verFile.exists()) {
                try {
                    localVer = new String(Files.readAllBytes(verFile.toPath())).trim();
                } catch (Exception ignored) {
                }
            }

            boolean shouldInstall = false;
            if (localVer == null || !localVer.equals(tag)) {
                // prompt user
                final String title = "Actualización disponible";
                final String message = "Versión disponible: " + tag
                        + "\n\nReemplazar instalación portable actual?\n(versión local: "
                        + (localVer == null ? "desconocida" : localVer) + ")";
                int resp = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                shouldInstall = resp == JOptionPane.YES_OPTION;
            }

            if (!shouldInstall)
                return;

            // download asset
            File tmp = File.createTempFile("sepriet_update_", "");
            tmp.deleteOnExit();
            boolean ok = downloadToFile(bestUrl, tmp);
            if (!ok) {
                JOptionPane.showMessageDialog(parent, "Error descargando actualización desde: " + bestUrl, "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Install: if zip, extract into appDir; if jar, replace app/Sepriet.jar
            if (bestName.toLowerCase().endsWith(".zip")) {
                unzipTo(tmp, appDir);
            } else if (bestName.toLowerCase().endsWith(".jar")) {
                File destJar = new File(appDir, "Sepriet.jar");
                Files.copy(tmp.toPath(), destJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                // unknown type: save as-is into appDir
                File dest = new File(appDir, bestName);
                Files.copy(tmp.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // record installed version
            try {
                Files.write(verFile.toPath(), tag.getBytes());
            } catch (Exception ignored) {
            }

            JOptionPane.showMessageDialog(parent,
                    "Actualización instalada: " + tag + "\nReinicie la aplicación para aplicar los cambios.",
                    "Actualizado", JOptionPane.INFORMATION_MESSAGE);
            // exit app so user restarts
            System.exit(0);

        } catch (Exception ex) {
            // Fail silently but log
            java.util.logging.Logger.getLogger(Updater.class.getName()).log(java.util.logging.Level.WARNING,
                    "Updater error", ex);
        }
    }

    private static File findPortableRoot() {
        try {
            File cur = new File(System.getProperty("user.dir"));
            for (int i = 0; i < 6 && cur != null; i++) {
                File app = new File(cur, "app");
                File runtime = new File(cur, "runtime");
                if (app.exists() && runtime.exists() && app.isDirectory() && runtime.isDirectory()) {
                    return cur;
                }
                cur = cur.getParentFile();
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    private static String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(urlStr).toURL().openConnection();
        conn.setRequestProperty("User-Agent", "Sepriet-Updater");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static boolean downloadToFile(String urlStr, File dest) {
        try {
            HttpURLConnection conn = (HttpURLConnection) java.net.URI.create(urlStr).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "Sepriet-Updater");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(0);
            try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1)
                    out.write(buf, 0, r);
            }
            return true;
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Updater.class.getName()).log(java.util.logging.Level.WARNING,
                    "Download failed", ex);
            return false;
        }
    }

    private static void unzipTo(File zipFile, File targetDir) throws IOException {
        if (!targetDir.exists())
            targetDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                File out = new File(targetDir, ze.getName());
                if (ze.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (!parent.exists())
                        parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) > 0)
                            fos.write(buf, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Very small helper to extract a JSON string value by key starting from an
     * index. It does not handle escapes robustly but is adequate for simple
     * GitHub API responses where keys/values are plain.
     */
    private static String extractJsonString(String text, String key, int fromIndex) {
        try {
            int k = text.indexOf('"' + key + '"', fromIndex);
            if (k < 0)
                return null;
            int colon = text.indexOf(':', k);
            if (colon < 0)
                return null;
            int q1 = text.indexOf('"', colon);
            if (q1 < 0)
                return null;
            int q2 = text.indexOf('"', q1 + 1);
            if (q2 < 0)
                return null;
            return text.substring(q1 + 1, q2);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean isAutoUpdateEnabled() {
        try {
            File portableRoot = findPortableRoot();
            File cfg;
            if (portableRoot != null)
                cfg = new File(portableRoot, "updater.properties");
            else
                cfg = new File(System.getProperty("user.dir"), "updater.properties");
            if (!cfg.exists()) {
                // Create a default updater.properties with auto_update=true so users
                // can see and change the setting. If creation fails, fall back to
                // default enabled behavior.
                try {
                    java.util.Properties def = new java.util.Properties();
                    def.setProperty("auto_update", "true");
                    // ensure parent exists
                    File parent = cfg.getParentFile();
                    if (parent != null && !parent.exists())
                        parent.mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(cfg)) {
                        def.store(fos, "Sepriet updater settings (auto-generated)");
                    }
                } catch (Exception ignored) {
                }
                return true; // default enabled
            }
            java.util.Properties p = new java.util.Properties();
            try (FileInputStream fis = new FileInputStream(cfg)) {
                p.load(fis);
            }
            String v = p.getProperty("auto_update", "true");
            return Boolean.parseBoolean(v.trim());
        } catch (Exception ex) {
            return true;
        }
    }
}
