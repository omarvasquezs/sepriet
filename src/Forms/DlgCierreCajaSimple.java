package Forms;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

/**
 * Diálogo simple para cierre de caja
 * Solo pide el monto final
 */
public class DlgCierreCajaSimple extends JDialog {

    private final JLabel lblInfoApertura;
    private final JTextField txtMontoCierre;
    private final JButton btnAceptar;
    private final JButton btnCancelar;
    private final int usuarioId;
    private int cajaId = -1;

    public DlgCierreCajaSimple(Frame parent, int usuarioId) {
        super(parent, "Cierre de Caja", true);
        this.usuarioId = usuarioId;

        setResizable(false);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de información
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Información de Apertura"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        lblInfoApertura = new JLabel("Cargando...");
        lblInfoApertura.setFont(lblInfoApertura.getFont().deriveFont(Font.PLAIN, 12f));
        infoPanel.add(lblInfoApertura, gbc);

        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // Panel de cierre
        JPanel cierrePanel = new JPanel(new GridBagLayout());
        cierrePanel.setBorder(BorderFactory.createTitledBorder("Cierre de Caja"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        cierrePanel.add(new JLabel("Monto de Cierre (S/.):"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        txtMontoCierre = new JTextField(15);
        txtMontoCierre.setText("0.00");
        cierrePanel.add(txtMontoCierre, gbc);

        mainPanel.add(cierrePanel, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnCancelar = new JButton("Cancelar");
        btnAceptar = new JButton("Cerrar Caja");

        btnCancelar.addActionListener(_ -> dispose());
        btnAceptar.addActionListener(_ -> cerrarCaja());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnAceptar);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Cargar información de apertura
        cargarInfoApertura();

        pack();
        setLocationRelativeTo(parent);

        txtMontoCierre.selectAll();
        txtMontoCierre.requestFocusInWindow();
    }

    private void cargarInfoApertura() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id, datetime_apertura, monto_apertura, id_usuario_apertura " +
                    "FROM caja_apertura_cierre " +
                    "WHERE DATE(datetime_apertura) = CURDATE() " +
                    "AND datetime_cierre IS NULL " +
                    "ORDER BY datetime_apertura DESC LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    cajaId = rs.getInt("id");
                    java.sql.Timestamp apertura = rs.getTimestamp("datetime_apertura");
                    double montoApertura = rs.getDouble("monto_apertura");

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    String fechaApertura = sdf.format(apertura);

                    lblInfoApertura.setText(String.format(
                            "<html>Apertura: %s<br>Monto Inicial: S/. %.2f</html>",
                            fechaApertura,
                            montoApertura));
                } else {
                    lblInfoApertura.setText("<html><font color='red'>No hay caja abierta hoy</font></html>");
                    btnAceptar.setEnabled(false);
                    txtMontoCierre.setEnabled(false);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar información de apertura:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void cerrarCaja() {
        if (cajaId == -1) {
            JOptionPane.showMessageDialog(this,
                    "No hay caja abierta para cerrar.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validar monto
        String montoStr = txtMontoCierre.getText().trim();
        if (montoStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe ingresar el monto de cierre.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            txtMontoCierre.requestFocus();
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoStr);
            if (monto < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "El monto debe ser un número válido mayor o igual a 0.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            txtMontoCierre.selectAll();
            txtMontoCierre.requestFocus();
            return;
        }

        // Confirmar cierre
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("¿Está seguro que desea cerrar la caja con S/. %.2f?", monto),
                "Confirmar Cierre",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Guardar en base de datos
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "UPDATE caja_apertura_cierre SET " +
                    "datetime_cierre = NOW(), " +
                    "monto_cierre = ?, " +
                    "id_usuario_cierre = ? " +
                    "WHERE id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, monto);
                ps.setInt(2, usuarioId);
                ps.setInt(3, cajaId);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this,
                            String.format("Caja cerrada exitosamente con S/. %.2f", monto),
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "No se pudo cerrar la caja. Intente nuevamente.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cerrar la caja:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
