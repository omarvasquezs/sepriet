package Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Simple print preview dialog rendering HTML in a JEditorPane and allowing A4 / 58mm
 * view toggles.
 */
public class DlgPrintPreview extends JDialog {

    private final JEditorPane editor = new JEditorPane();
    private final JButton btnPrint = new JButton("Imprimir");
    private final JButton btnSend = new JButton("Enviar por WhatsApp");
    private final JTextField txtPublicUrl = new JTextField(40);

    public DlgPrintPreview(Window owner) {
        super(owner, "Vista de Impresión (58MM)", ModalityType.APPLICATION_MODAL);
        initUI();
        setSize(400, 700);
        setLocationRelativeTo(owner);
    }

    /** Create preview dialog with target content width (pixels). */
    public DlgPrintPreview(Window owner, int contentWidthPx) {
        super(owner, "Vista de Impresión (58MM)", ModalityType.APPLICATION_MODAL);
        initUI();
        // make dialog slightly wider than content to include controls and padding
        int w = Math.max(400, contentWidthPx + 200);
        int h = 700;
        setSize(w, h);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        editor.setContentType("text/html");
        editor.setEditable(false);

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(btnPrint);
    top.add(btnSend);
    // small URL panel (hidden by default) - label removed, keep only the text field
    JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    txtPublicUrl.setEditable(false);
    txtPublicUrl.setVisible(false);
    urlPanel.add(txtPublicUrl);

    getContentPane().setLayout(new BorderLayout());
    // place top buttons and url panel
    JPanel north = new JPanel(new BorderLayout());
    north.add(top, BorderLayout.NORTH);
    north.add(urlPanel, BorderLayout.SOUTH);
    getContentPane().add(north, BorderLayout.NORTH);
    getContentPane().add(new JScrollPane(editor), BorderLayout.CENTER);

    btnPrint.addActionListener(this::onPrint);
    btnSend.addActionListener(this::onSend);
    }

    public void showForHtml(String html) {
        showForHtml(html, null);
    }

    public void showForHtml(String html, String telefono) {
        editor.setText(html);
        editor.setCaretPosition(0);
        // if telefono looks valid (9 digits) prefill it in root property
        if (telefono != null) {
            String digits = telefono.replaceAll("\\D", "");
            if (digits.length() == 9) this.getRootPane().putClientProperty("telefonoCliente", digits);
        }
        setVisible(true);
    }


    

    private void onPrint(ActionEvent e) {
        try {
            boolean done = editor.print();
            if (!done) {
                JOptionPane.showMessageDialog(this, "Impresión cancelada o fallida.", "Imprimir", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error imprimiendo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSend(ActionEvent e) {
        // retrieve telefono (may be null)
        String tel = (String) this.getRootPane().getClientProperty("telefonoCliente");
        String input = tel != null ? tel : "";
        String phone = (String) JOptionPane.showInputDialog(this, "Número de celular (9 dígitos):", "Enviar por WhatsApp", JOptionPane.PLAIN_MESSAGE, null, null, input);
        if (phone == null) return; // user cancelled
        phone = phone.trim();
        phone = phone.replaceAll("\\D", "");
        if (phone.length() != 9) {
            JOptionPane.showMessageDialog(this, "El número debe tener exactamente 9 dígitos (sin prefijo).", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String recipient = "+51" + phone;

        // read textmebot_api.json to get apikey
        java.nio.file.Path apiPath = java.nio.file.Paths.get("textmebot_api.json");
        if (!java.nio.file.Files.exists(apiPath)) {
            JOptionPane.showMessageDialog(this, "textmebot_api.json no encontrado. Configure la API key en el formulario DB.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String apiJson;
        try {
            apiJson = new String(java.nio.file.Files.readAllBytes(apiPath), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error leyendo textmebot_api.json:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String apikey = null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\"textmebot_api\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(apiJson);
        if (m.find()) apikey = m.group(1);
        if (apikey == null || apikey.isBlank()) {
            JOptionPane.showMessageDialog(this, "API key no encontrada en textmebot_api.json.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Fetch receipt details from the database
        String receiptId = (String) this.getRootPane().getClientProperty("receiptId");
        if (receiptId == null || receiptId.isBlank()) {
            // Try to infer cod_comprobante from the HTML and lookup the id as fallback
            try {
                String htmlForParse = editor.getText();
                String codCandidate = null;
                java.util.regex.Matcher mm = java.util.regex.Pattern.compile(
                        ">([A-Z0-9\\-]{3,50})<",
                        java.util.regex.Pattern.CASE_INSENSITIVE).matcher(htmlForParse);
                while (mm.find()) {
                    String c = mm.group(1).trim();
                    // crude filter: codes usually contain a dash and digits, e.g. NV001-1030 or B-000005
                    if (c.matches(".*\\d.*") && c.contains("-")) {
                        codCandidate = c;
                        break;
                    }
                }
                if (codCandidate != null) {
                    System.out.println("Attempting to lookup comprobante id for code: " + codCandidate);
                    try (java.sql.Connection conn = DatabaseConfig.getConnection();
                         java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM comprobantes WHERE cod_comprobante = ? LIMIT 1")) {
                        ps.setString(1, codCandidate);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                receiptId = Integer.toString(rs.getInt("id"));
                                // cache it so subsequent sends don't need lookup
                                this.getRootPane().putClientProperty("receiptId", receiptId);
                                System.out.println("Resolved comprobante id: " + receiptId);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        // Try reading embedded RECEIPT_ID comment in HTML as highest-priority source
        if (receiptId == null || receiptId.isBlank()) {
            try {
                String htmlText = editor.getText();
                java.util.regex.Matcher cmt = java.util.regex.Pattern.compile("RECEIPT_ID:\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(htmlText);
                if (cmt.find()) {
                    receiptId = cmt.group(1);
                    this.getRootPane().putClientProperty("receiptId", receiptId);
                    System.out.println("Found embedded RECEIPT_ID in HTML: " + receiptId);
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        if (receiptId == null || receiptId.isBlank()) {
            JOptionPane.showMessageDialog(this, "No se encontró el ID del comprobante.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String receiptDetails = fetchReceiptDetailsFromDatabase(receiptId);

        // Build a plain-text message from the HTML preview
        String html = editor.getText();
        String message = htmlToPlainText(html);

        // Append database details to the message
        message += "\n" + receiptDetails;

        try {
            String req = "https://api.textmebot.com/send.php?recipient=" + java.net.URLEncoder.encode(recipient, "UTF-8")
                    + "&apikey=" + java.net.URLEncoder.encode(apikey, "UTF-8")
                    + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
            java.net.URL u = java.net.URI.create(req).toURL();
            java.net.HttpURLConnection conn2 = (java.net.HttpURLConnection) u.openConnection();
            conn2.setRequestMethod("GET");
            conn2.setConnectTimeout(10000);
            conn2.setReadTimeout(15000);
            int code = conn2.getResponseCode();
            String resp;
            try (java.io.InputStream is = code >= 400 ? conn2.getErrorStream() : conn2.getInputStream()) {
                resp = is == null ? "" : new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            if (code >= 200 && code < 300) {
                log("Textmebot response OK: code=" + code + " body=" + resp);
                txtPublicUrl.setVisible(false);
                JOptionPane.showMessageDialog(this, "Mensaje enviado correctamente.", "Enviado", JOptionPane.INFORMATION_MESSAGE);
            } else {
                log("Textmebot error: code=" + code + " body=" + resp);
                JOptionPane.showMessageDialog(this, "Error enviando (codigo=" + code + "):\n" + resp, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error realizando solicitud:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Convert a fragment of receipt HTML into a compact plain-text message
    private String htmlToPlainText(String html) {
        if (html == null) return "";
        // Replace <br> and </p> tags with newlines
        String t = html.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<\\s*/p\\s*>", "\n");
        // Remove all other HTML tags but keep their inner text
        t = t.replaceAll("<[^>]+>", "");
        // Decode common HTML entities
        t = t.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&quot;", "\"");
        // Decode numeric entities like &#225;
        java.util.regex.Pattern entP = java.util.regex.Pattern.compile("&#(\\d+);");
        java.util.regex.Matcher entM = entP.matcher(t);
        StringBuffer sbEnt = new StringBuffer();
        while (entM.find()) {
            try {
                int code = Integer.parseInt(entM.group(1));
                entM.appendReplacement(sbEnt, java.util.regex.Matcher.quoteReplacement(Character.toString((char) code)));
            } catch (Exception ex) {
                entM.appendReplacement(sbEnt, "");
            }
        }
        entM.appendTail(sbEnt);
        t = sbEnt.toString();

        // Split lines and trim
        String[] lines = t.split("\\r?\\n");
        java.util.List<String> L = new java.util.ArrayList<>();
        for (String line : lines) {
            String s2 = line.replaceAll("\\s+", " ").trim();
            if (!s2.isEmpty()) L.add(s2);
        }

        // Heuristics: build a friendlier message
        StringBuilder out = new StringBuilder();
        // Title/company (first lines)
        int idx = 0;
        if (L.size() > 0) { out.append(L.get(0)).append("\n"); idx = 1; }
        // Collect up to next 6 lines as header info
        for (int i = idx; i < Math.min(L.size(), idx + 6); i++) {
            out.append(L.get(i)).append("\n");
        }
        out.append("\n");

        // Extract details and totals using simple line-based parsing
        java.util.List<String> itemLines = new java.util.ArrayList<>();
        java.util.List<String> totalLines = new java.util.ArrayList<>();

        // Find details section and extract item data
        boolean inDetails = false;
        for (int i = 0; i < L.size(); i++) {
            String line = L.get(i);
            String upper = line.toUpperCase();

            if (upper.contains("DETALLES")) {
                inDetails = true;
                out.append("DETALLES:\n");
                continue;
            }

            if (inDetails) {
                // Stop when we hit totals
                if (upper.startsWith("SUBTOTAL") || upper.startsWith("IGV") || upper.startsWith("TOTAL")) {
                    inDetails = false;
                    totalLines.add(line);
                    continue;
                }
                // Skip table headers (SERVICIO PESO C/ KG TOTAL)
                if (upper.contains("SERVICIO") && upper.contains("PESO") && upper.contains("TOTAL")) {
                    continue;
                }
                // This should be an item line
                if (!line.trim().isEmpty()) {
                    itemLines.add("- " + line);
                }
            }
        }

        // Attach details
        for (String it : itemLines) out.append(it).append("\n");
        out.append("\n");

        // Totals
        for (String totalLine : totalLines) {
            out.append(totalLine).append("\n");
        }
        out.append("\nGracias por su preferencia!");

        String msg = out.toString().trim();
        if (msg.length() > 3000) msg = msg.substring(0, 3000) + "...";
        return msg;
    }

    // Simple append-only logger for send events
    private void log(String msg) {
        try {
            java.nio.file.Path logPath = java.nio.file.Paths.get("sepriet_send.log");
            String line = java.time.LocalDateTime.now().toString() + " - " + msg + System.lineSeparator();
            java.nio.file.Files.writeString(logPath, line, java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignore) {
        }
    }

    private String fetchReceiptDetailsFromDatabase(String receiptId) {
        StringBuilder details = new StringBuilder();
        try (java.sql.Connection conn = DatabaseConfig.getConnection()) {
            if (conn == null) {
                details.append("Error: No se pudo establecer la conexión a la base de datos.\n");
                return details.toString();
            }

            // Log the receipt ID for debugging
            System.out.println("Fetching details for receipt ID: " + receiptId);

            // Query to fetch receipt details
            String query = "SELECT s.nom_servicio AS servicio, d.peso_kg, d.costo_kilo, (d.peso_kg * d.costo_kilo) AS total " +
                           "FROM comprobantes_detalles d " +
                           "JOIN servicios s ON d.servicio_id = s.id " +
                           "WHERE d.comprobante_id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, receiptId);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String servicio = rs.getString("servicio");
                        double peso = rs.getDouble("peso_kg");
                        double costoKilo = rs.getDouble("costo_kilo");
                        double total = rs.getDouble("total");
                        details.append(String.format("- %s: %.2f kg x %.2f = %.2f\n", servicio, peso, costoKilo, total));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                details.append("Error al ejecutar la consulta de detalles.\n");
            }

            // Query to fetch totals
            query = "SELECT c.costo_total AS total FROM comprobantes c WHERE c.id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, receiptId);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double total = rs.getDouble("total");
                        details.append(String.format("\nTOTAL: %.2f\n", total));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                details.append("Error al ejecutar la consulta de totales.\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            details.append("Error general al obtener detalles del comprobante.\n");
        }
        return details.toString();
    }
}
