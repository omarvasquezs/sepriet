package Forms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

public class DlgEstadoCaja extends JDialog {
    private final int usuarioId;
    private int cajaId = -1;
    private java.sql.Timestamp aperturaDate;

    private JLabel lblStatus;
    private JLabel lblMontoInicial;
    private JLabel lblVentas;
    private JLabel lblEgresos;
    private JLabel lblTotalTeorico;

    private JTable tablaEgresos;
    private DefaultTableModel modEgresos;

    public DlgEstadoCaja(Frame parent, int usuarioId) {
        super(parent, "Estado de Caja y Egresos", true);
        this.usuarioId = usuarioId;

        setSize(550, 450);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel superior (Resumen)
        JPanel summaryPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Resumen de Caja en Efectivo"));
        lblStatus = new JLabel("Cargando...");
        lblMontoInicial = new JLabel();
        lblVentas = new JLabel();
        lblEgresos = new JLabel();
        lblTotalTeorico = new JLabel();
        lblTotalTeorico.setFont(lblTotalTeorico.getFont().deriveFont(Font.BOLD, 14f));
        
        summaryPanel.add(lblStatus);
        summaryPanel.add(lblMontoInicial);
        summaryPanel.add(lblVentas);
        summaryPanel.add(lblEgresos);
        summaryPanel.add(lblTotalTeorico);

        mainPanel.add(summaryPanel, BorderLayout.NORTH);

        // Panel central (Tabla y añadir egreso)
        JPanel egresoPanel = new JPanel(new BorderLayout(5, 5));
        egresoPanel.setBorder(BorderFactory.createTitledBorder("Egresos (Gastos Diarios)"));
        
        modEgresos = new DefaultTableModel(new Object[]{"Hora", "Descripción", "Monto (S/.)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaEgresos = new JTable(modEgresos);
        egresoPanel.add(new JScrollPane(tablaEgresos), BorderLayout.CENTER);

        JPanel addEgresoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddEgreso = new JButton("Registrar Nuevo Egreso");
        btnAddEgreso.addActionListener(_ -> registrarEgreso());
        addEgresoPanel.add(btnAddEgreso);

        egresoPanel.add(addEgresoPanel, BorderLayout.SOUTH);
        mainPanel.add(egresoPanel, BorderLayout.CENTER);

        // Boton de cerrar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(_ -> dispose());
        bottomPanel.add(btnCerrar);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        
        cargarEstado();
    }

    private void cargarEstado() {
        lblMontoInicial.setText(""); lblVentas.setText(""); lblEgresos.setText(""); lblTotalTeorico.setText("");
        modEgresos.setRowCount(0);
        cajaId = -1;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Retrieve current open box
            String sqlCaja = "SELECT id, datetime_apertura, monto_apertura FROM caja_apertura_cierre " +
                             "WHERE DATE(datetime_apertura) = CURDATE() AND datetime_cierre IS NULL " +
                             "ORDER BY datetime_apertura DESC LIMIT 1";
            double montoInicial = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlCaja);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cajaId = rs.getInt("id");
                    aperturaDate = rs.getTimestamp("datetime_apertura");
                    montoInicial = rs.getDouble("monto_apertura");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    lblStatus.setText("Caja Abierta desde: " + sdf.format(aperturaDate));
                    lblMontoInicial.setText(String.format("1. Monto Inicial (Apertura): S/. %.2f", montoInicial));
                } else {
                    lblStatus.setText("No hay caja abierta para el día de hoy.");
                    return; // Cannot continue without an open box
                }
            }

            // Retrieve sales specifically in Cash since opening
            double totalVentasEfectivo = 0;
            String sqlVentas = "SELECT SUM(monto_abonado) as total FROM reporte_ingresos r " +
                               "JOIN metodo_pago m ON r.metodo_pago_id = m.id " +
                               "WHERE r.fecha >= ? AND m.nom_metodo_pago LIKE '%EFECTIVO%'";
            try (PreparedStatement psV = conn.prepareStatement(sqlVentas)) {
                psV.setTimestamp(1, aperturaDate);
                try (ResultSet rsV = psV.executeQuery()) {
                    if (rsV.next()) {
                        totalVentasEfectivo = rsV.getDouble("total");
                    }
                }
            }
            lblVentas.setText(String.format("2. Total Ingresos en Efectivo: S/. %.2f", totalVentasEfectivo));

            // Retrieve egresos for this caja
            double totalEgresos = 0;
            String sqlEgresos = "SELECT fecha, descripcion, monto FROM caja_egresos WHERE id_caja = ? ORDER BY fecha";
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
            try (PreparedStatement psE = conn.prepareStatement(sqlEgresos)) {
                psE.setInt(1, cajaId);
                try (ResultSet rsE = psE.executeQuery()) {
                    while (rsE.next()) {
                        String hora = timeFmt.format(rsE.getTimestamp("fecha"));
                        String desc = rsE.getString("descripcion");
                        double val = rsE.getDouble("monto");
                        totalEgresos += val;
                        modEgresos.addRow(new Object[]{hora, desc, String.format("%.2f", val)});
                    }
                }
            }
            lblEgresos.setText(String.format("3. Total Gastos / Egresos: S/. %.2f", totalEgresos));

            double totalTeorico = montoInicial + totalVentasEfectivo - totalEgresos;
            lblTotalTeorico.setText(String.format("Monto Teórico Actual en Caja: S/. %.2f", totalTeorico));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando estado: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarEgreso() {
        if (cajaId == -1) {
            JOptionPane.showMessageDialog(this, "No puede registrar un gasto sin una caja abierta.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel p = new JPanel(new GridLayout(2, 2, 10, 5));
        p.add(new JLabel("Descripción del Gasto:"));
        JTextField txtDesc = new JTextField(15);
        p.add(txtDesc);
        p.add(new JLabel("Monto a retirar (S/.):"));
        JTextField txtMonto = new JTextField(10);
        p.add(txtMonto);

        int res = JOptionPane.showConfirmDialog(this, p, "Registrar Egreso / Gasto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String desc = txtDesc.getText().trim();
            if (desc.isEmpty()) {
                JOptionPane.showMessageDialog(this, "La descripción es requerida.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            double monto = 0;
            try {
                monto = Double.parseDouble(txtMonto.getText().trim());
                if (monto <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "El monto debe ser un número válido mayor a cero.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO caja_egresos (id_caja, fecha, descripcion, monto, id_usuario) VALUES (?, NOW(), ?, ?, ?)")) {
                ps.setInt(1, cajaId);
                ps.setString(2, desc);
                ps.setDouble(3, monto);
                ps.setInt(4, usuarioId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Egreso registrado correctamente.");
                cargarEstado(); // Refresh info
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar el egreso: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
