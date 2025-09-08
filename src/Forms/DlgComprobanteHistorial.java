package Forms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Vector;

/**
 * Dialog to show creation date and incremental payments (reporte_ingresos)
 * for a comprobante.
 */
public class DlgComprobanteHistorial extends JDialog {
    private final int comprobanteId;
    private final JLabel lblCreated = new JLabel("Creación: -");
    private final JLabel lblTotal = new JLabel("Total: -");
    private final JLabel lblAbonado = new JLabel("Abonado: -");
    private final JLabel lblDeuda = new JLabel("Deuda: -");
    private final JTable tbl = new JTable();

    public DlgComprobanteHistorial(Window owner, int comprobanteId) {
        super(owner, "Historial de Comprobante", ModalityType.APPLICATION_MODAL);
        this.comprobanteId = comprobanteId;
        setSize(560, 380);
        setResizable(false);
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel top = new JPanel(new GridLayout(2, 2, 8, 6));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(lblCreated);
        top.add(lblTotal);
        top.add(lblAbonado);
        top.add(lblDeuda);

    tbl.setModel(new DefaultTableModel(new Object[] { "Fecha", "Método", "Monto" }, 0));
        JScrollPane sp = new JScrollPane(tbl);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        bottom.add(btnClose);

        getContentPane().setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadData() {
        DefaultTableModel tm = (DefaultTableModel) tbl.getModel();
        tm.setRowCount(0);
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Get comprobante creation date, total and total abonos (from comprobantes table)
            String cod = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT fecha, costo_total, IFNULL(monto_abonado,0) as monto_abonado, cod_comprobante FROM comprobantes WHERE id=?")) {
                ps.setInt(1, comprobanteId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Timestamp ts = rs.getTimestamp("fecha");
                        String fecha = ts == null ? "-" : ts.toString();
                        lblCreated.setText("Creación: " + fecha);
                        double total = rs.getDouble("costo_total");
                        double abonado = rs.getDouble("monto_abonado");
                        lblTotal.setText("Total: S/. " + formatNumber(total));
                        lblAbonado.setText("Abonado: S/. " + formatNumber(abonado));
                        double deuda = Math.max(0.0, total - abonado);
                        lblDeuda.setText("Deuda: S/. " + formatNumber(deuda));
                        cod = rs.getString("cod_comprobante");
                    }
                }
            }

            // Load reporte_ingresos rows for this comprobante (ordered by fecha asc)
            // reporte_ingresos stores payments by cod_comprobante (string), not comprobante_id
            try (PreparedStatement ps = conn.prepareStatement("SELECT fecha, metodo_pago_id, monto_abonado FROM reporte_ingresos WHERE cod_comprobante=? ORDER BY fecha ASC")) {
                ps.setString(1, cod == null ? "" : cod);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("fecha");
                        String fecha = ts == null ? "-" : ts.toString();
                        int metodoId = rs.getInt("metodo_pago_id");
                        String metodo = "";
                        if (metodoId > 0) {
                            try (PreparedStatement p2 = conn.prepareStatement("SELECT nom_metodo_pago FROM metodo_pago WHERE id=?")) {
                                p2.setInt(1, metodoId);
                                try (ResultSet r2 = p2.executeQuery()) {
                                    if (r2.next()) metodo = r2.getString(1);
                                }
                            }
                        }
                        double monto = rs.getDouble("monto_abonado");
                        Vector<Object> row = new Vector<>();
                        row.add(fecha);
                        row.add(metodo == null ? "" : metodo);
                        row.add(formatNumber(monto));
                        tm.addRow(row);
                    }
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando historial:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String formatNumber(double v) {
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");
            return df.format(v);
        } catch (Exception ex) {
            return String.format(java.util.Locale.US, "%.2f", v);
        }
    }
}
