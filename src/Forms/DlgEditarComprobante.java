package Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

/**
 * Simple dialog to edit estado_comprobante, metodo_pago and add a new
 * monto_abonado (incremental)
 */
public class DlgEditarComprobante extends JDialog {
    private final int comprobanteId;
    private final Runnable onSaved;
    private final JComboBox<Item> cboEstado = new JComboBox<>();
    private final JComboBox<Item> cboEstadoRopa = new JComboBox<>();
    private final JComboBox<Item> cboMetodoPago = new JComboBox<>();
    private final JLabel lblMetodoPago = new JLabel("Método de pago:");
    private final JTextField txtMontoAbonar = new JTextField();
    private final JLabel lblInfo = new JLabel(" ");
    private float costoTotal = 0f;
    private float montoAbonadoPrevio = 0f;
    // original estado at dialog open (used to prevent backward transitions)
    private int originalEstadoComprobanteId = -1;
    private String originalEstadoComprobanteLabel = null;
    // remember original estado_ropa to restore on invalid attempts
    private int originalEstadoRopaId = -1;
    private boolean suppressEstadoListener = false;

    record Item(int id, String label) {
        public String toString() {
            return label;
        }
    }

    public DlgEditarComprobante(Window owner, int id, Runnable onSaved) {
        super(owner, "Editar Comprobante", ModalityType.APPLICATION_MODAL);
        this.comprobanteId = id;
        this.onSaved = onSaved;
        setSize(420, 300);
        setResizable(false);
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int r = 0;
        addRow(form, c, r++, new JLabel("ESTADO ROPA :"), cboEstadoRopa);
        addRow(form, c, r++, new JLabel("ESTADO COMPROBANTE :"), cboEstado);
        addRow(form, c, r++, lblMetodoPago, cboMetodoPago);
        addRow(form, c, r++, new JLabel("Monto a abonar (incremental):"), txtMontoAbonar);
        c.gridy = r++;
        c.gridx = 0;
        c.gridwidth = 2;
        form.add(lblInfo, c);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        buttons.add(btnGuardar);
        buttons.add(btnCancelar);
        btnGuardar.addActionListener(this::guardar);
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                dispose();
            }
        });
        getContentPane().setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, JComponent l, JComponent f) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        p.add(f, c);
    }

    private void loadData() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // load estado_ropa
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT id, nom_estado_ropa FROM estado_ropa ORDER BY nom_estado_ropa")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        cboEstadoRopa.addItem(new Item(rs.getInt(1), rs.getString(2)));
                }
            } catch (SQLException ignore) {
            }
            // load estados comprobante
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT id, nom_estado FROM estado_comprobantes WHERE habilitado=1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        cboEstado.addItem(new Item(rs.getInt(1), rs.getString(2)));
                }
            }
            // load metodos
            cboMetodoPago.addItem(new Item(-1, "(Ninguno/NULL)"));
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT id, nom_metodo_pago FROM metodo_pago WHERE habilitado=1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        cboMetodoPago.addItem(new Item(rs.getInt(1), rs.getString(2)));
                }
            }
            // load current comprobante
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT estado_comprobante_id, estado_ropa_id, metodo_pago_id, monto_abonado, costo_total FROM comprobantes WHERE id=?")) {
                ps.setInt(1, comprobanteId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int est = rs.getInt(1);
                        int estR = rs.getInt(2);
                        int mp = rs.getInt(3);
                        montoAbonadoPrevio = rs.getFloat(4);
                        costoTotal = rs.getFloat(5);
                        selectCombo(cboEstado, est);
                        selectCombo(cboEstadoRopa, estR);
                        selectCombo(cboMetodoPago, mp == 0 ? -1 : mp);
                    }
                }
            }
            // remember original estado_ropa so we can revert invalid attempts
            Object curR = cboEstadoRopa.getSelectedItem();
            if (curR instanceof Item)
                originalEstadoRopaId = ((Item) curR).id();
            // remember original estado to enforce allowed transitions
            Object cur = cboEstado.getSelectedItem();
            if (cur instanceof Item) {
                originalEstadoComprobanteId = ((Item) cur).id();
                originalEstadoComprobanteLabel = cur.toString().trim().toUpperCase();
            }
            // react to estado selection changes
            // add an ItemListener to prevent invalid backward transitions
            cboEstado.addItemListener(iEv -> {
                if (iEv.getStateChange() != java.awt.event.ItemEvent.SELECTED)
                    return;
                if (suppressEstadoListener)
                    return;
                Object it = iEv.getItem();
                if (!(it instanceof Item) || originalEstadoComprobanteLabel == null)
                    return;
                String selLabel = it.toString().trim().toUpperCase();
                // ABONO -> cannot go back to DEBE
                if ("ABONO".equals(originalEstadoComprobanteLabel) && "DEBE".equals(selLabel)) {
                    final Object prevMetodo = cboMetodoPago.getSelectedItem();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "No se puede regresar de ABONO a DEBE.", "Validación",
                                JOptionPane.WARNING_MESSAGE);
                        suppressEstadoListener = true;
                        selectCombo(cboEstado, originalEstadoComprobanteId);
                        // restore metodo selection if it still exists
                        try {
                            if (prevMetodo != null)
                                cboMetodoPago.setSelectedItem(prevMetodo);
                        } catch (Exception ignore) {
                        }
                        suppressEstadoListener = false;
                    });
                    return;
                }
                // CANCELADO -> cannot go back to DEBE, ABONO or ANULADO
                if ("CANCELADO".equals(originalEstadoComprobanteLabel)
                        && ("DEBE".equals(selLabel) || "ABONO".equals(selLabel) || "ANULADO".equals(selLabel))) {
                    final Object prevMetodo2 = cboMetodoPago.getSelectedItem();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "No se puede cambiar desde CANCELADO a DEBE, ABONO o ANULADO.", "Validación",
                                JOptionPane.WARNING_MESSAGE);
                        suppressEstadoListener = true;
                        selectCombo(cboEstado, originalEstadoComprobanteId);
                        try {
                            if (prevMetodo2 != null)
                                cboMetodoPago.setSelectedItem(prevMetodo2);
                        } catch (Exception ignore) {
                        }
                        suppressEstadoListener = false;
                    });
                    return;
                }
            });
            // prevent selecting "RECOGIDO" in estado_ropa unless comprobante estado is or
            // will be CANCELADO
            cboEstadoRopa.addItemListener(iEv -> {
                if (iEv.getStateChange() != java.awt.event.ItemEvent.SELECTED)
                    return;
                Object it = iEv.getItem();
                if (!(it instanceof Item))
                    return;
                String selLabel = it.toString().trim().toUpperCase();
                if (!"RECOGIDO".equals(selLabel))
                    return;
                // allow RECOGIDO if current estado is CANCELADO
                Object estSel = cboEstado.getSelectedItem();
                String estLabelLocal = estSel instanceof Item ? ((Item) estSel).label().toUpperCase() : "";
                if ("CANCELADO".equals(estLabelLocal))
                    return;
                // or allow if the current monto input would make the comprobante fully paid
                try {
                    float montoInput = 0f;
                    String txt = txtMontoAbonar.getText().trim();
                    if (!txt.isEmpty())
                        montoInput = Float.parseFloat(txt);
                    float wouldBe = montoAbonadoPrevio + montoInput;
                    if (wouldBe >= costoTotal - 0.001f)
                        return; // allowed because saving will force CANCELADO
                } catch (Exception ignore) {
                }
                // not allowed: revert and inform
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "No puede escoger 'RECOGIDO' a menos que el comprobante esté o vaya a ser CANCELADO.",
                            "Validación", JOptionPane.WARNING_MESSAGE);
                    selectCombo(cboEstadoRopa, originalEstadoRopaId);
                });
            });
            // regular action listener to update UI elements when selection changes
            cboEstado.addActionListener(this::applyEstadoRules);
            // apply rules once now so the UI matches the current estado when dialog opens
            applyEstadoRules(null);
            updateInfo();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando datos:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectCombo(JComboBox<Item> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void updateInfo() {
        lblInfo.setText(String.format("Abonado previo: %.2f  |  Total: %.2f  |  Deuda: %.2f", montoAbonadoPrevio,
                costoTotal, (costoTotal - montoAbonadoPrevio)));
    }

    private void applyEstadoRules(java.awt.event.ActionEvent ev) {
        Object sel = cboEstado.getSelectedItem();
        if (sel == null)
            return;
        String label = sel.toString().trim().toUpperCase();
        float deuda = costoTotal - montoAbonadoPrevio;
        switch (label) {
            case "ABONO":
                txtMontoAbonar.setEnabled(true);
                txtMontoAbonar.setText("");
                // show metodo
                lblMetodoPago.setVisible(true);
                cboMetodoPago.setVisible(true);
                cboMetodoPago.setEnabled(true);
                break;
            case "CANCELADO":
                // set monto to remaining and disable edits
                txtMontoAbonar.setEnabled(false);
                txtMontoAbonar.setText(String.format("%.2f", Math.max(0f, deuda)));
                // show metodo
                lblMetodoPago.setVisible(true);
                cboMetodoPago.setVisible(true);
                cboMetodoPago.setEnabled(true);
                break;
            case "DEBE":
            case "ANULADO":
                // hide metodo and reset selection
                txtMontoAbonar.setEnabled(false);
                txtMontoAbonar.setText("");
                lblMetodoPago.setVisible(false);
                cboMetodoPago.setVisible(false);
                cboMetodoPago.setSelectedIndex(0);
                break;
            default:
                txtMontoAbonar.setEnabled(true);
                lblMetodoPago.setVisible(true);
                cboMetodoPago.setVisible(true);
                cboMetodoPago.setEnabled(true);
                break;
        }
        // refresh UI to account for visibility changes
        SwingUtilities.invokeLater(() -> {
            lblMetodoPago.revalidate();
            cboMetodoPago.revalidate();
            this.revalidate();
            this.repaint();
        });
        updateInfo();
    }

    private void guardar(ActionEvent e) {
        String montoTxt = txtMontoAbonar.getText().trim();
        float montoNuevo = 0f;
        if (!montoTxt.isEmpty()) {
            try {
                montoNuevo = Float.parseFloat(montoTxt);
                if (montoNuevo < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Monto inválido", "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        Item est = (Item) cboEstado.getSelectedItem();
        Item mp = (Item) cboMetodoPago.getSelectedItem();
        float abonadoActualizado;
        String estLabel = est == null ? "" : est.label().toUpperCase();
        if ("CANCELADO".equals(estLabel)) {
            abonadoActualizado = costoTotal; // ensure full paid
            montoNuevo = Math.max(0f, costoTotal - montoAbonadoPrevio);
        } else if ("DEBE".equals(estLabel)) {
            abonadoActualizado = 0f;
            montoNuevo = 0f;
        } else {
            abonadoActualizado = montoAbonadoPrevio + montoNuevo;
        }
        if (abonadoActualizado > costoTotal + 0.001f) {
            JOptionPane.showMessageDialog(this, "El abono excede el total.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            // Decide final estado: if after applying the new payment the comprobante
            // is fully paid, force its estado to CANCELADO regardless of selection.
            int finalEstadoId = est.id();
            if (abonadoActualizado >= costoTotal - 0.001f) {
                // find CANCELADO id in the combo if present
                for (int i = 0; i < cboEstado.getItemCount(); i++) {
                    Item it = cboEstado.getItemAt(i);
                    if (it != null && "CANCELADO".equalsIgnoreCase(it.label())) {
                        finalEstadoId = it.id();
                        break;
                    }
                }
                // reflect change in UI while saving (avoid triggering listener side-effects)
                try {
                    suppressEstadoListener = true;
                    selectCombo(cboEstado, finalEstadoId);
                } finally {
                    suppressEstadoListener = false;
                }
            }
            // determine selected estado_ropa id
            int finalEstadoRopaId = -1;
            Object selR = cboEstadoRopa.getSelectedItem();
            if (selR instanceof Item) {
                finalEstadoRopaId = ((Item) selR).id();
            }
            // Validate: cannot save estado_ropa = RECOGIDO unless final estado is CANCELADO
            if (finalEstadoRopaId != -1) {
                // find label for finalEstadoRopaId
                String chosenRopaLabel = null;
                for (int i = 0; i < cboEstadoRopa.getItemCount(); i++) {
                    Item it = cboEstadoRopa.getItemAt(i);
                    if (it != null && it.id() == finalEstadoRopaId) {
                        chosenRopaLabel = it.label().toUpperCase();
                        break;
                    }
                }
                if ("RECOGIDO".equals(chosenRopaLabel)) {
                    // find label for the computed finalEstadoId (it may have been forced to
                    // CANCELADO above)
                    String finalEstadoLabel = null;
                    for (int i = 0; i < cboEstado.getItemCount(); i++) {
                        Item it = cboEstado.getItemAt(i);
                        if (it != null && it.id() == finalEstadoId) {
                            finalEstadoLabel = it.label().toUpperCase();
                            break;
                        }
                    }
                    if (!"CANCELADO".equals(finalEstadoLabel)) {
                        JOptionPane.showMessageDialog(this,
                                "No puede establecer ESTADO ROPA = RECOGIDO a menos que ESTADO COMPROBANTE sea CANCELADO.",
                                "Validación", JOptionPane.WARNING_MESSAGE);
                        return; // abort save
                    }
                }
            }
            // Update comprobante (now also persist estado_ropa_id)
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE comprobantes SET estado_comprobante_id=?, metodo_pago_id=?, monto_abonado=?, estado_ropa_id=? WHERE id=?")) {
                ps.setInt(1, finalEstadoId);
                if (mp == null || mp.id() == -1)
                    ps.setNull(2, java.sql.Types.INTEGER);
                else
                    ps.setInt(2, mp.id());
                ps.setFloat(3, abonadoActualizado);
                if (finalEstadoRopaId != -1)
                    ps.setInt(4, finalEstadoRopaId);
                else
                    ps.setNull(4, java.sql.Types.INTEGER);
                ps.setInt(5, comprobanteId);
                ps.executeUpdate();
            }
            if (montoNuevo > 0) {
                // Insert ingreso incremental
                String cod = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT cod_comprobante, cliente_id, costo_total FROM comprobantes WHERE id=?")) {
                    ps.setInt(1, comprobanteId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            cod = rs.getString(1);
                            int cliente = rs.getInt(2);
                            float costo = rs.getFloat(3);
                            try (PreparedStatement ins = conn.prepareStatement(
                                    "INSERT INTO reporte_ingresos (cod_comprobante, cliente_id, metodo_pago_id, fecha, monto_abonado, costo_total) VALUES (?,?,?,?,?,?)")) {
                                ins.setString(1, cod);
                                ins.setInt(2, cliente);
                                if (mp == null || mp.id() == -1)
                                    ins.setNull(3, java.sql.Types.INTEGER);
                                else
                                    ins.setInt(3, mp.id());
                                ins.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                                ins.setFloat(5, montoNuevo);
                                ins.setFloat(6, costo);
                                ins.executeUpdate();
                            }
                        }
                    }
                }
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Guardado.");
            // After successful save, if estado_ropa was set to LISTO PARA RECOGER,
            // prompt to confirm phone and send WhatsApp notification. We do this
            // after commit to avoid sending notifications for unsaved changes.
            try {
                // find label for finalEstadoRopaId
                String chosenRopaLabel = null;
                for (int i = 0; i < cboEstadoRopa.getItemCount(); i++) {
                    Item it = cboEstadoRopa.getItemAt(i);
                    if (it != null && it.id() == finalEstadoRopaId) {
                        chosenRopaLabel = it.label().toUpperCase();
                        break;
                    }
                }
                if ("LISTO PARA RECOGER".equals(chosenRopaLabel)) {
                    // lookup cod_comprobante and client phone (may be null)
                    String cod = null;
                    String telefonoCliente = null;
                    try (PreparedStatement ps3 = conn.prepareStatement(
                            "SELECT cod_comprobante, (SELECT telefono FROM clientes WHERE id = cliente_id) AS telefono FROM comprobantes WHERE id = ?")) {
                        ps3.setInt(1, comprobanteId);
                        try (ResultSet rs3 = ps3.executeQuery()) {
                            if (rs3.next()) {
                                cod = rs3.getString(1);
                                telefonoCliente = rs3.getString(2);
                            }
                        }
                    } catch (Exception ex) {
                    }

                    String prefill = telefonoCliente == null ? "" : telefonoCliente.replaceAll("\\D", "");
                    String input = (prefill != null && prefill.length() == 9) ? prefill : "";
                    String phone = (String) JOptionPane.showInputDialog(this,
                            "Número de celular (9 dígitos):\n(indique sin prefijo, por ejemplo 987654321)",
                            "Confirmar número WhatsApp", JOptionPane.PLAIN_MESSAGE, null, null, input);
                    if (phone != null) {
                        phone = phone.trim().replaceAll("\\D", "");
                        if (phone.length() == 9) {
                            String msgCode = cod == null ? "" : cod;
                            // Build a multi-line message with details, totals and debt
                            StringBuilder msgSb = new StringBuilder();
                            msgSb.append("Para informarle que sus prendas ya estan listas para recoger.")
                                    .append("\n\n");
                            if (msgCode != null && !msgCode.isBlank()) {
                                msgSb.append("Comprobante: ").append(msgCode).append("\n\n");
                            }

                            msgSb.append("DETALLES (Servicio: Peso(Kilos) x Precio al Kilo):\n");
                            StringBuilder detailsSb = new StringBuilder();
                            try (PreparedStatement psServices = conn.prepareStatement(
                                    "SELECT s.nom_servicio, d.peso_kg, d.costo_kilo FROM comprobantes_detalles d JOIN servicios s ON d.servicio_id = s.id WHERE d.comprobante_id = ?")) {
                                psServices.setInt(1, comprobanteId);
                                try (ResultSet rsS = psServices.executeQuery()) {
                                    boolean any = false;
                                    while (rsS.next()) {
                                        any = true;
                                        String srv = rsS.getString(1);
                                        double peso = rsS.getDouble(2);
                                        double costoKilo = rsS.getDouble(3);
                                        double totalLine = peso * costoKilo;
                                        if (peso > 0.001) {
                                            detailsSb.append(String.format("- %s: %.2f kg x %.2f = %.2f\n", srv, peso,
                                                    costoKilo, totalLine));
                                        } else {
                                            // if no peso, show service and price
                                            detailsSb.append(String.format("- %s: %.2f\n", srv, totalLine));
                                        }
                                    }
                                    if (!any) {
                                        detailsSb.append("- (sin detalles)\n");
                                    }
                                }
                            } catch (Exception ignore) {
                                detailsSb.append("- (error leyendo detalles)\n");
                            }
                            msgSb.append(detailsSb.toString()).append("\n");

                            // TOTAL (use costoTotal loaded earlier)
                            msgSb.append(String.format(java.util.Locale.US, "TOTAL: %.2f\n\n", (double) costoTotal));

                            // DEUDA: compute based on updated abonadoActualizado
                            try {
                                double deuda = Math.max(0d, (double) (costoTotal - abonadoActualizado));
                                if (deuda > 0.001d) {
                                    msgSb.append(String.format(java.util.Locale.US, "DEUDA: S/. %.2f\n\n", deuda));
                                } else {
                                    msgSb.append(
                                            "Este comprobante no registra ninguna deuda pendiente. Por lo tanto, solo queda proceder con la entrega de sus prendas.\n\n");
                                }
                            } catch (Exception ignore) {
                            }

                            // Footer (WhatsApp bold with single asterisks as requested)
                            msgSb.append("*El tiempo máximo para recoger su prenda es de 15 días.*\n");
                            msgSb.append("*Una vez retirada la prenda, no se aceptarán reclamos.*");

                            String message = msgSb.toString();
                            boolean ok = sendWhatsAppViaTextmebot(phone, message);
                            if (ok) {
                                JOptionPane.showMessageDialog(this, "Notificación enviada por WhatsApp.", "Enviado",
                                        JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                int opt = JOptionPane.showConfirmDialog(this,
                                        "Error enviando notificación. ¿Reintentar?", "Error", JOptionPane.YES_NO_OPTION,
                                        JOptionPane.ERROR_MESSAGE);
                                if (opt == JOptionPane.YES_OPTION) {
                                    boolean retryOk = sendWhatsAppViaTextmebot(phone, message);
                                    if (retryOk)
                                        JOptionPane.showMessageDialog(this, "Notificación enviada.", "Enviado",
                                                JOptionPane.INFORMATION_MESSAGE);
                                    else
                                        JOptionPane.showMessageDialog(this, "No se pudo enviar la notificación.",
                                                "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Número ingresado inválido. Omisión del envío.",
                                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            if (onSaved != null)
                onSaved.run();
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error guardando:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Send a WhatsApp message using textmebot API. Returns true on success.
     */
    private boolean sendWhatsAppViaTextmebot(String phone9, String message) {
        try {
            java.nio.file.Path apiPath = java.nio.file.Paths.get("textmebot_api.json");
            if (!java.nio.file.Files.exists(apiPath)) {
                return false;
            }
            String apiJson = new String(java.nio.file.Files.readAllBytes(apiPath),
                    java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\\"textmebot_api\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
                    .matcher(apiJson);
            String apikey = null;
            if (m.find())
                apikey = m.group(1);
            if (apikey == null || apikey.isBlank())
                return false;
            String recipient = "+51" + phone9;
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
            try (java.io.InputStream is = code >= 400 ? conn2.getErrorStream() : conn2.getInputStream()) {
                if (is != null) {
                    // consume response body to free connection resources
                    is.readAllBytes();
                }
            }
            return code >= 200 && code < 300;
        } catch (Exception ex) {
            return false;
        }
    }
}
