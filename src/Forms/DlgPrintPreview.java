package Forms;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Simple print preview dialog rendering HTML in a JEditorPane and allowing A4 /
 * 58mm
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
        showForHtml(html, telefono, null);
    }

    public void showForHtml(String html, String telefono, String codigoPais) {
        editor.setText(html);
        editor.setCaretPosition(0);
        // if telefono provided, normalize digits. Accept values that include
        // country code by taking the last 9 digits when more are present.
        if (telefono != null) {
            String digits = telefono.replaceAll("\\D", "");
            // Accept numbers between 7 and 15 digits for international compatibility
            if (digits.length() >= 7 && digits.length() <= 15) {
                this.getRootPane().putClientProperty("telefonoCliente", digits);
                // Debug log: record the value placed into rootPane property
                Forms.DebugLogger.log("DlgPrintPreview", "setRoot telefonoCliente='" + digits + "'");
            }
        }
        // Store country code (default to +51 if not provided)
        String countryCode = (codigoPais != null && !codigoPais.trim().isEmpty()) ? codigoPais.trim() : "+51";
        this.getRootPane().putClientProperty("codigoPaisCliente", countryCode);
        // Try to resolve the receipt id or code from the HTML and, if the
        // comprobante is in ABONO or DEBE, append the remaining debt to the preview
        try {
            String receiptId = resolveReceiptIdFromHtml(html);
            if (receiptId != null && !receiptId.isBlank()) {
                this.getRootPane().putClientProperty("receiptId", receiptId);
                DebtInfo info = fetchDebtInfo(receiptId);
                if (info != null) {
                    String estado = info.estadoLabel == null ? "" : info.estadoLabel.toUpperCase();
                    if ("ABONO".equals(estado) || "DEBE".equals(estado) || "DEUDA".equals(estado)
                            || "CANCELADO".equals(estado)) {
                        String deudaStr = String.format(java.util.Locale.US, "%.2f", info.deuda);
                        String debtHtml = "<span style=\"font-weight:bold;display:block;margin-top:4px\">DEUDA: S/. "
                                + deudaStr + "</span>";
                        // avoid inserting twice
                        if (!html.toUpperCase().contains("DEUDA:")) {
                            String newHtml;
                            // Always show the total amount paid so far under the DEUDA line
                            String montoStr = String.format(java.util.Locale.US, "%.2f", info.montoAbonado);
                            String abonHtml = "<span style=\"display:block;margin-top:2px\">TOTAL ABONADO: S/. "
                                    + montoStr + "</span>";
                            try {
                                // try to inject right after the first occurrence of 'ESTADO:'
                                // (case-insensitive)
                                if (html.toUpperCase().contains("ESTADO:")) {
                                    newHtml = html.replaceFirst("(?i)ESTADO\\s*:", "ESTADO:" + debtHtml + abonHtml);
                                } else {
                                    // fallback: append near top of receipt (after first header) if possible
                                    newHtml = html + "<hr/>" + debtHtml + abonHtml;
                                }
                            } catch (Exception ex) {
                                newHtml = html + "<hr/>" + debtHtml + abonHtml;
                            }
                            editor.setText(newHtml);
                            editor.setCaretPosition(0);
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        // Append fixed footer notice to HTML preview if not already present
        try {
            // Both lines wrapped inside <b>...</b> so the entire notice appears bold
            int dias = Forms.ConfigUtils.getMaxDiasRecojo();
            String footerHtml = "<div style=\"margin-top:12px;font-size:11px;line-height:1.2;\"><b>El tiempo máximo para recoger su prenda es de "
                    + dias + " días.<br/>Una vez retirada la prenda o reparación, no se aceptarán reclamos.</b></div>";
            String current = editor.getText();
            if (current != null && !current.toUpperCase().contains("TIEMPO MÁXIMO PARA RECOGER")
                    && !current.contains("no se aceptarán reclamos")) {
                // try to insert before closing body if present
                if (current.toLowerCase().contains("</body>")) {
                    current = current.replaceFirst("(?i)</body>", footerHtml + "</body>");
                } else {
                    current = current + footerHtml;
                }
                editor.setText(current);
                editor.setCaretPosition(0);
            }
        } catch (Exception ignore) {
        }
        setVisible(true);
    }

    private static class DebtInfo {
        final String estadoLabel;
        final float deuda;
        final float montoAbonado;

        DebtInfo(int id, String estadoLabel, float deuda, float montoAbonado) {
            this.estadoLabel = estadoLabel;
            this.deuda = deuda;
            this.montoAbonado = montoAbonado;
        }
    }

    /**
     * Try to find a receipt id embedded in the HTML or resolve cod_comprobante ->
     * id.
     */
    private String resolveReceiptIdFromHtml(String html) {
        if (html == null)
            return null;
        // First try embedded RECEIPT_ID comment
        try {
            java.util.regex.Matcher cmt = java.util.regex.Pattern
                    .compile("RECEIPT_ID:\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
            if (cmt.find())
                return cmt.group(1);
        } catch (Exception ignore) {
        }
        // Try to find a code-like token then lookup id by cod_comprobante
        try (java.sql.Connection conn = DatabaseConfig.getConnection()) {
            if (conn == null)
                return null;
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile(
                    ">([A-Z0-9\\-]{3,50})<",
                    java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
            while (mm.find()) {
                String c = mm.group(1).trim();
                if (c.matches(".*\\d.*") && c.contains("-")) {
                    try (java.sql.PreparedStatement ps = conn
                            .prepareStatement("SELECT id FROM comprobantes WHERE cod_comprobante = ? LIMIT 1")) {
                        ps.setString(1, c);
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next())
                                return Integer.toString(rs.getInt(1));
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /** Fetch estado label and compute debt for a comprobante id. */
    private DebtInfo fetchDebtInfo(String receiptId) {
        try (java.sql.Connection conn = DatabaseConfig.getConnection()) {
            if (conn == null)
                return null;
            // try by numeric id
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.id, c.costo_total, IFNULL(c.monto_abonado,0) monto_abonado, ec.nom_estado, c.cod_comprobante FROM comprobantes c LEFT JOIN estado_comprobantes ec ON c.estado_comprobante_id = ec.id WHERE c.id = ?")) {
                ps.setInt(1, Integer.parseInt(receiptId));
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        float costo = rs.getFloat(2);
                        float abon = rs.getFloat(3);
                        String estado = rs.getString(4);
                        String cod = null;
                        try {
                            cod = rs.getString(5);
                        } catch (Exception _e) {
                            cod = null;
                        }
                        // If comprobante.monto_abonado is zero, try summing reporte_ingresos for
                        // cod_comprobante
                        if ((abon == 0.0f || abon < 0.0001f) && cod != null && !cod.isBlank()) {
                            try (java.sql.PreparedStatement psSum = conn.prepareStatement(
                                    "SELECT IFNULL(SUM(r.monto_abonado),0) FROM reporte_ingresos r WHERE r.cod_comprobante = ?")) {
                                psSum.setString(1, cod);
                                try (java.sql.ResultSet rsSum = psSum.executeQuery()) {
                                    if (rsSum.next()) {
                                        float s = rsSum.getFloat(1);
                                        if (s > 0.0f) {
                                            abon = s;
                                        }
                                    }
                                }
                            } catch (Exception __) {
                                // ignore
                            }
                        }
                        return new DebtInfo(id, estado, Math.max(0f, costo - abon), abon);
                    }
                }
            } catch (NumberFormatException ignore) {
                // maybe receiptId is actually a cod string, fallthrough
            }
            // fallback: try by cod_comprobante
            try (java.sql.PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT c.id, c.costo_total, IFNULL(c.monto_abonado,0) monto_abonado, ec.nom_estado, c.cod_comprobante FROM comprobantes c LEFT JOIN estado_comprobantes ec ON c.estado_comprobante_id = ec.id WHERE c.cod_comprobante = ? LIMIT 1")) {
                ps2.setString(1, receiptId);
                try (java.sql.ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        float costo = rs.getFloat(2);
                        float abon = rs.getFloat(3);
                        String estado = rs.getString(4);
                        String cod = null;
                        try {
                            cod = rs.getString(5);
                        } catch (Exception _e) {
                            cod = null;
                        }
                        if ((abon == 0.0f || abon < 0.0001f) && cod != null && !cod.isBlank()) {
                            try (java.sql.PreparedStatement psSum = conn.prepareStatement(
                                    "SELECT IFNULL(SUM(r.monto_abonado),0) FROM reporte_ingresos r WHERE r.cod_comprobante = ?")) {
                                psSum.setString(1, cod);
                                try (java.sql.ResultSet rsSum = psSum.executeQuery()) {
                                    if (rsSum.next()) {
                                        float s = rsSum.getFloat(1);
                                        if (s > 0.0f) {
                                            abon = s;
                                        }
                                    }
                                }
                            } catch (Exception __) {
                                // ignore
                            }
                        }
                        return new DebtInfo(id, estado, Math.max(0f, costo - abon), abon);
                    }
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    private void onPrint(ActionEvent e) {
        try {
            boolean done = editor.print();
            if (!done) {
                JOptionPane.showMessageDialog(this, "Impresión cancelada o fallida.", "Imprimir",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error imprimiendo: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSend(ActionEvent e) {
        // retrieve telefono and codigo_pais from stored properties
        String tel = (String) this.getRootPane().getClientProperty("telefonoCliente");
        String codigoPais = (String) this.getRootPane().getClientProperty("codigoPaisCliente");
        if (codigoPais == null || codigoPais.trim().isEmpty()) {
            codigoPais = "+51"; // default to Peru
        }

        // Debug log: record what the preview send path is using as prefill
        Forms.DebugLogger.log("DlgPrintPreview",
                "prefillRoot telefonoCliente='" + (tel == null ? "" : tel) + "', codigoPais='" + codigoPais + "'");
        String input = tel != null ? tel : "";

        // Show custom dialog to edit both phone and country code
        WhatsAppDialog whatsAppDialog = new WhatsAppDialog(this, input, codigoPais);
        whatsAppDialog.setVisible(true);

        if (!whatsAppDialog.isConfirmed()) {
            return; // user cancelled
        }

        String phone = whatsAppDialog.getPhone();
        String selectedCountryCode = whatsAppDialog.getCountryCode();

        if (phone.length() < 7 || phone.length() > 15) {
            JOptionPane.showMessageDialog(this, "El número debe tener entre 7 y 15 dígitos (sin prefijo de país).",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String recipient = selectedCountryCode + phone;

        // read textmebot_api.json to get apikey
        java.nio.file.Path apiPath = java.nio.file.Paths.get("textmebot_api.json");
        if (!java.nio.file.Files.exists(apiPath)) {
            JOptionPane.showMessageDialog(this,
                    "textmebot_api.json no encontrado. Configure la API key en el formulario DB.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String apiJson;
        try {
            apiJson = new String(java.nio.file.Files.readAllBytes(apiPath), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error leyendo textmebot_api.json:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String apikey = null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\"textmebot_api\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
                .matcher(apiJson);
        if (m.find())
            apikey = m.group(1);
        if (apikey == null || apikey.isBlank()) {
            JOptionPane.showMessageDialog(this, "API key no encontrada en textmebot_api.json.", "Error",
                    JOptionPane.ERROR_MESSAGE);
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
                    // crude filter: codes usually contain a dash and digits, e.g. NV001-1030 or
                    // B-000005
                    if (c.matches(".*\\d.*") && c.contains("-")) {
                        codCandidate = c;
                        break;
                    }
                }
                if (codCandidate != null) {
                    System.out.println("Attempting to lookup comprobante id for code: " + codCandidate);
                    try (java.sql.Connection conn = DatabaseConfig.getConnection();
                            java.sql.PreparedStatement ps = conn.prepareStatement(
                                    "SELECT id FROM comprobantes WHERE cod_comprobante = ? LIMIT 1")) {
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
                java.util.regex.Matcher cmt = java.util.regex.Pattern
                        .compile("RECEIPT_ID:\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(htmlText);
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
            JOptionPane.showMessageDialog(this, "No se encontró el ID del comprobante.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String receiptDetails = fetchReceiptDetailsFromDatabase(receiptId);

        // Build a plain-text message from the HTML preview.
        // Remove any previously-injected footer HTML so htmlToPlainText doesn't turn it
        // into list bullets.
        String html = editor.getText();
        // Remove only the exact injected footer block (make regex specific to avoid
        // removing other parts)
        String cleanedHtml = html == null ? ""
                : html.replaceAll(
                        "(?is)<div[^>]*>\\s*<b>\\s*El\\s+tiempo\\s+m[aá]ximo\\s+para\\s+recoger[\\s\\S]*?no\\s+se\\s+aceptar[aá]n\\s+reclamos\\.?\\s*</b>\\s*</div>",
                        "");
        String message = htmlToPlainText(cleanedHtml);

        // Remove any duplicate footer text that may have been included in the
        // HTML-to-text conversion
        message = message.replaceAll("(?i)- El tiempo m[aá]ximo para recoger su prenda es de 15 d[ií]as\\.", "");
        message = message.replaceAll("(?i)- Una vez retirada la prenda o reparaci[oó]n, no se aceptar[aá]n reclamos\\.",
                "");
        message = message.replaceAll("(?i)El tiempo m[aá]ximo para recoger su prenda es de 15 d[ií]as\\.", "");
        message = message.replaceAll("(?i)Una vez retirada la prenda o reparaci[oó]n, no se aceptar[aá]n reclamos\\.",
                "");
        // Remove standalone "- Una" and "- vez retirada..." fragments
        message = message.replaceAll("(?i)- Una\\s*\n", "");
        message = message.replaceAll("(?i)- vez retirada[^\\n]*", "");
        message = message.replaceAll("(?i)vez retirada[^\\n]*", "");
        message = message.replaceAll("(?i)- Una\\s*$", "");
        // Clean up extra whitespace and newlines that may result from removal
        message = message.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n").trim();

        // Append database details to the message, then add closing footer so it is last
        message += "\n" + receiptDetails;
        // If this comprobante has a remaining debt and its estado is ABONO/DEBE,
        // append a DEUDA line to the WhatsApp message so the recipient sees outstanding
        // balance.
        try {
            String resolved = (String) this.getRootPane().getClientProperty("receiptId");
            if (resolved == null || resolved.isBlank()) {
                resolved = resolveReceiptIdFromHtml(html);
            }
            if (resolved != null && !resolved.isBlank()) {
                DebtInfo di = fetchDebtInfo(resolved);
                if (di != null) {
                    String est = di.estadoLabel == null ? "" : di.estadoLabel.toUpperCase();
                    if ("ABONO".equals(est) || "DEBE".equals(est) || "DEUDA".equals(est) || "CANCELADO".equals(est)) {
                        String deudaStr = String.format(java.util.Locale.US, "%.2f", di.deuda);
                        message += "\nDEUDA: S/. " + deudaStr;
                        // Always append the total already paid (may be 0.00)
                        String montoStr = String.format(java.util.Locale.US, "%.2f", di.montoAbonado);
                        message += "\nTOTAL ABONADO: S/. " + montoStr;
                        // Append footer immediately after DEUDA line with double asterisks for WhatsApp
                        // bold
                        int dias = Forms.ConfigUtils.getMaxDiasRecojo();
                        message += "\n\n**El tiempo máximo para recoger su prenda es de " + dias
                                + " días.**\n**Una vez retirada la prenda o reparación, no se aceptarán reclamos.**";
                    }
                }
            }
        } catch (Exception ignore) {
        }
        // For comprobantes without DEUDA, append footer at the end if not already
        // present
        if (!message.contains("tiempo máximo para recoger")) {
            int dias = Forms.ConfigUtils.getMaxDiasRecojo();
            message += "\n\n**El tiempo máximo para recoger su prenda es de " + dias
                    + " días.**\n**Una vez retirada la prenda o reparación, no se aceptarán reclamos.**";
        }
        message = message.trim();

        try {
            String req = "https://api.textmebot.com/send.php?recipient="
                    + java.net.URLEncoder.encode(recipient, "UTF-8")
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
                JOptionPane.showMessageDialog(this, "Mensaje enviado correctamente.", "Enviado",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                log("Textmebot error: code=" + code + " body=" + resp);
                JOptionPane.showMessageDialog(this, "Error enviando (codigo=" + code + "):\n" + resp, "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error realizando solicitud:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Convert a fragment of receipt HTML into a compact plain-text message
    private String htmlToPlainText(String html) {
        if (html == null)
            return "";
        // Normalize and preserve structural breaks so table rows/cells and list items
        // become separate lines when tags are stripped.
        String t = html;
        // Common line breaks
        t = t.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)<\\s*/p\\s*>", "\n");
        // End of table rows and tables -> newline
        t = t.replaceAll("(?i)</tr>|</tbody>|</table>", "\n");
        // Table cells -> separator
        t = t.replaceAll("(?i)</td>|</th>", " | ");
        // Remove opening cell/th tags
        t = t.replaceAll("(?i)<td[^>]*>", "").replaceAll("(?i)<th[^>]*>", "");
        // List items
        t = t.replaceAll("(?i)<li[^>]*>", "- ").replaceAll("(?i)</li>", "\n");
        // Remove remaining tags but keep inner text
        t = t.replaceAll("<[^>]+>", "");
        // Additionally, try to extract table rows directly from the raw HTML so
        // we can recover item detail rows even if the simple tag-stripping
        // logic split them incorrectly.
        java.util.List<String> tableRows = new java.util.ArrayList<>();
        try {
            java.util.regex.Pattern tableP = java.util.regex.Pattern.compile("(?is)<table[^>]*>(.*?)</table>");
            java.util.regex.Matcher tableM = tableP.matcher(html);
            while (tableM.find()) {
                String tableHtml = tableM.group(1);
                java.util.regex.Pattern trP = java.util.regex.Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>");
                java.util.regex.Matcher trM = trP.matcher(tableHtml);
                while (trM.find()) {
                    String trHtml = trM.group(1);
                    java.util.regex.Pattern cellP = java.util.regex.Pattern.compile("(?is)<t[dh][^>]*>(.*?)</t[dh]>");
                    java.util.regex.Matcher cellM = cellP.matcher(trHtml);
                    java.util.List<String> cells = new java.util.ArrayList<>();
                    while (cellM.find()) {
                        String cellHtml = cellM.group(1);
                        String cellText = cellHtml.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
                        if (cellText.isEmpty())
                            continue;
                        // basic entity decode for common entities and numeric entities
                        cellText = cellText.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("&lt;", "<")
                                .replaceAll("&gt;", ">").replaceAll("&quot;", "\"");
                        java.util.regex.Pattern entP2 = java.util.regex.Pattern.compile("&#(\\d+);");
                        java.util.regex.Matcher entM2 = entP2.matcher(cellText);
                        StringBuffer sb2 = new StringBuffer();
                        while (entM2.find()) {
                            try {
                                int code = Integer.parseInt(entM2.group(1));
                                entM2.appendReplacement(sb2,
                                        java.util.regex.Matcher.quoteReplacement(Character.toString((char) code)));
                            } catch (Exception ex) {
                                entM2.appendReplacement(sb2, "");
                            }
                        }
                        entM2.appendTail(sb2);
                        cellText = sb2.toString();
                        cells.add(cellText);
                    }
                    if (!cells.isEmpty()) {
                        tableRows.add(String.join(" | ", cells));
                    }
                }
            }
        } catch (Exception ignore) {
        }
        // Decode common HTML entities
        t = t.replaceAll("&nbsp;", " ").replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"");
        // Decode numeric entities like &#225;
        java.util.regex.Pattern entP = java.util.regex.Pattern.compile("&#(\\d+);");
        java.util.regex.Matcher entM = entP.matcher(t);
        StringBuffer sbEnt = new StringBuffer();
        while (entM.find()) {
            try {
                int code = Integer.parseInt(entM.group(1));
                entM.appendReplacement(sbEnt,
                        java.util.regex.Matcher.quoteReplacement(Character.toString((char) code)));
            } catch (Exception ex) {
                entM.appendReplacement(sbEnt, "");
            }
        }
        entM.appendTail(sbEnt);
        t = sbEnt.toString();

        // Split lines and trim
        String[] lines = t.split("\\r?\\n");
        java.util.List<String> raw = new java.util.ArrayList<>();
        for (String line : lines) {
            String s2 = line.replaceAll("\\s+", " ").trim();
            if (!s2.isEmpty())
                raw.add(s2);
        }

        // Re-group fragments that were split into separate entries by cell separators
        // e.g. ["SERVICIO","|","PESO","|","C/ KG","|"] -> ["SERVICIO | PESO | C/ KG"]
        java.util.List<String> L = new java.util.ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            String cur = raw.get(i);
            if (cur.matches("^\\|\\s*$")) {
                // stray separator without left-hand text: skip
                continue;
            }
            StringBuilder sb = new StringBuilder(cur);
            int j = i + 1;
            while (j + 0 < raw.size()) {
                String maybeSep = raw.get(j);
                if (!maybeSep.matches("^\\|\\s*$"))
                    break;
                if (j + 1 >= raw.size())
                    break;
                String nextText = raw.get(j + 1);
                sb.append(" | ").append(nextText);
                j += 2;
            }
            L.add(sb.toString());
            i = Math.max(i, j - 1);
        }

        // Heuristics: build a friendlier message
        StringBuilder out = new StringBuilder();
        // Title/company (first lines)
        int idx = 0;
        if (L.size() > 0) {
            out.append(L.get(0)).append("\n");
            idx = 1;
        }
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
                // Header removed per client request: do not append the DETALLES header
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
        for (String it : itemLines)
            out.append(it).append("\n");
        out.append("\n");
        // Totals
        for (String totalLine : totalLines) {
            out.append(totalLine).append("\n");
        }
        out.append("\n");

        String msg = out.toString().trim();
        if (msg.length() > 3000)
            msg = msg.substring(0, 3000) + "...";
        return msg;
    }

    // Simple append-only logger for send events
    private void log(String msg) {
        try {
            java.nio.file.Path logPath = java.nio.file.Paths.get("sepriet_send.log");
            String line = java.time.LocalDateTime.now().toString() + " - " + msg + System.lineSeparator();
            java.nio.file.Files.writeString(logPath, line, java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
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
            String query = "SELECT s.nom_servicio AS servicio, d.peso_kg, d.costo_kilo, (d.peso_kg * d.costo_kilo) AS total "
                    +
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
                        details.append(
                                String.format("- %s: %.2f kg x %.2f = %.2f\n", servicio, peso, costoKilo, total));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                details.append("Error al ejecutar la consulta de detalles.\n");
            }

            // Query to fetch totals and descuento
            query = "SELECT c.costo_total AS total, IFNULL(c.descuento,0) AS descuento, c.tipo_comprobante FROM comprobantes c WHERE c.id = ?";
            try (java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, receiptId);
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        double total = rs.getDouble("total");
                        double descuentoPerc = 0.0;
                        String tipoComprobante = rs.getString("tipo_comprobante");
                        try {
                            descuentoPerc = rs.getDouble("descuento");
                            if (rs.wasNull())
                                descuentoPerc = 0.0;
                        } catch (Exception ex) {
                            descuentoPerc = 0.0;
                        }
                        // total in DB is stored as the final total (after discount)
                        // compute total before discount (if descuento present)
                        double totalConDescuento = total;
                        double totalSinDescuento = total;
                        if (descuentoPerc > 0.0) {
                            // derive pre-discount total
                            totalSinDescuento = totalConDescuento / (1.0 - (descuentoPerc / 100.0));
                        }
                        // IGV should appear above total lines; compute igv from the post-discount
                        // amount (maintaining current business logic where IGV is applied on
                        // the total after discount) - but only show for FACTURA
                        if ("F".equals(tipoComprobante)) {
                            double igv = totalConDescuento * 0.18;
                            details.append(String.format("\nIGV 18%%: S/. %.2f\n", igv));
                        }

                        if (descuentoPerc > 0.0) {
                            details.append(String.format("TOTAL SIN DESCUENTO: %.2f\n", totalSinDescuento));
                            double descuentoAmount = totalSinDescuento - totalConDescuento;
                            details.append(String.format("DESCUENTO: %s%% (S/. %.2f)\n", formatNumber(descuentoPerc),
                                    descuentoAmount));
                            details.append(String.format("TOTAL CON DESCUENTO: %.2f\n", totalConDescuento));
                        } else {
                            // no discount -> show single total (which equals totalConDescuento)
                            details.append(String.format("TOTAL: %.2f\n", totalConDescuento));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                details.append("Error al ejecutar la consulta de totales.\n");
            }
            // Append payment history (abonos) from reporte_ingresos (if any)
            try {
                // resolve cod_comprobante for this receipt id
                String cod = null;
                try (java.sql.PreparedStatement psCod = conn
                        .prepareStatement("SELECT cod_comprobante FROM comprobantes WHERE id = ?")) {
                    psCod.setString(1, receiptId);
                    try (java.sql.ResultSet rsCod = psCod.executeQuery()) {
                        if (rsCod.next())
                            cod = rsCod.getString("cod_comprobante");
                    }
                }
                if (cod != null && !cod.isBlank()) {
                    try (java.sql.PreparedStatement psPay = conn.prepareStatement(
                            "SELECT r.fecha, r.monto_abonado, COALESCE(m.nom_metodo_pago,'') AS metodo "
                                    + "FROM reporte_ingresos r LEFT JOIN metodo_pago m ON r.metodo_pago_id = m.id "
                                    + "WHERE r.cod_comprobante = ? ORDER BY r.fecha ASC")) {
                        psPay.setString(1, cod);
                        try (java.sql.ResultSet rsPay = psPay.executeQuery()) {
                            boolean any = false;
                            StringBuilder sb = new StringBuilder();
                            while (rsPay.next()) {
                                any = true;
                                java.sql.Timestamp ts = rsPay.getTimestamp("fecha");
                                String when = ts == null ? ""
                                        : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(ts);
                                double monto = rsPay.getDouble("monto_abonado");
                                String metodo = rsPay.getString("metodo");
                                sb.append(String.format("- %s: S/. %.2f %s\n", when, monto,
                                        (metodo == null || metodo.isBlank()) ? "" : "(" + metodo + ")"));
                            }
                            if (any) {
                                details.append("\nABONOS:\n");
                                details.append(sb.toString());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Do not fail if payments can't be fetched
            }
        } catch (Exception e) {
            e.printStackTrace();
            details.append("Error general al obtener detalles del comprobante.\n");
        }
        return details.toString();
    }

    // Format a number with up to two decimal places, dropping trailing .00 when
    // possible
    private String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return Double.toString(value);
        }
        java.text.DecimalFormatSymbols symbols = java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US);
        java.text.DecimalFormat df;
        // If value is effectively an integer, show no decimals; otherwise show up to
        // two decimals
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            df = new java.text.DecimalFormat("#", symbols);
        } else {
            df = new java.text.DecimalFormat("#.##", symbols);
        }
        return df.format(value);
    }
}

/**
 * Custom dialog for WhatsApp sending that allows editing both phone number and
 * country code
 */
class WhatsAppDialog extends JDialog {
    private JTextField txtPhone;
    private JComboBox<String> cbxCountryCode;
    private boolean confirmed = false;

    public WhatsAppDialog(Window parent, String initialPhone, String initialCountryCode) {
        super(parent instanceof Dialog ? (Dialog) parent : null, "Enviar por WhatsApp", true);
        initComponents(initialPhone, initialCountryCode);
        setLocationRelativeTo(parent);
    }

    private void initComponents(String initialPhone, String initialCountryCode) {
        setLayout(new BorderLayout());

        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Country code label and combo
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 5);
        mainPanel.add(new JLabel("Código de país:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        cbxCountryCode = new JComboBox<>(new String[] {
                "+51", "+593", "+1", "+52", "+57", "+58", "+56", "+55", "+54", "+34", "+49", "+33", "+44", "+39", "+81",
                "+86"
        });
        cbxCountryCode.setEditable(true);
        cbxCountryCode.setSelectedItem(initialCountryCode);
        cbxCountryCode.setPreferredSize(new Dimension(120, 25));
        mainPanel.add(cbxCountryCode, gbc);

        // Phone label and field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(8, 0, 15, 5);
        mainPanel.add(new JLabel("Número de celular:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtPhone = new JTextField(initialPhone);
        txtPhone.setPreferredSize(new Dimension(150, 25));
        // Add document filter to allow only digits
        ((AbstractDocument) txtPhone.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        mainPanel.add(txtPhone, gbc);

        // Help text
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel helpLabel = new JLabel(
                "<html><small><i>Ingrese entre 7 y 15 dígitos (sin prefijo de país)</i></small></html>");
        helpLabel.setForeground(Color.GRAY);
        mainPanel.add(helpLabel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        btnOK.setPreferredSize(new Dimension(80, 30));
        btnCancel.setPreferredSize(new Dimension(80, 30));

        btnOK.addActionListener(_ -> {
            String phone = txtPhone.getText().trim().replaceAll("\\D", "");
            if (phone.length() < 7 || phone.length() > 15) {
                JOptionPane.showMessageDialog(this,
                        "El número debe tener entre 7 y 15 dígitos (sin prefijo de país).",
                        "Validación", JOptionPane.WARNING_MESSAGE);
                txtPhone.requestFocus();
                return;
            }
            confirmed = true;
            dispose();
        });

        btnCancel.addActionListener(_ -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);

        // Set default button and escape key
        getRootPane().setDefaultButton(btnOK);

        // ESC key to cancel
        ActionMap actionMap = getRootPane().getActionMap();
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        actionMap.put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        pack();

        // Focus on phone field and select all text
        SwingUtilities.invokeLater(() -> {
            txtPhone.requestFocus();
            txtPhone.selectAll();
        });
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getPhone() {
        return txtPhone.getText().trim().replaceAll("\\D", "");
    }

    public String getCountryCode() {
        return (String) cbxCountryCode.getSelectedItem();
    }
}
