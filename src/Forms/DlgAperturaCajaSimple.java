package Forms;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Diálogo simple para apertura de caja
 * Solo pide el monto inicial
 */
public class DlgAperturaCajaSimple extends JDialog {

    private final JTextField txtMontoInicial;
    private final JButton btnAceptar;
    private final JButton btnCancelar;
    private final int usuarioId;
    private boolean aperturaExitosa = false;

    public DlgAperturaCajaSimple(Frame parent, int usuarioId) {
        super(parent, "Apertura de Caja", true);
        this.usuarioId = usuarioId;

        // Configurar diálogo
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel de información
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Apertura de Caja"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Mensaje
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel lblMensaje = new JLabel(
                "<html><b>No se ha aperturado la caja el día de hoy.</b><br>Por favor ingrese el monto inicial:</html>");
        infoPanel.add(lblMensaje, gbc);

        // Monto inicial
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        infoPanel.add(new JLabel("Monto Inicial (S/.):"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        txtMontoInicial = new JTextField(15);
        txtMontoInicial.setText("0.00");
        infoPanel.add(txtMontoInicial, gbc);

        mainPanel.add(infoPanel, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnCancelar = new JButton("Cancelar");
        btnAceptar = new JButton("Aperturar Caja");

        btnCancelar.addActionListener(_ -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Si no apertura la caja, no podrá registrar comprobantes.\n¿Desea salir del sistema?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        btnAceptar.addActionListener(_ -> aperturarCaja());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnAceptar);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);

        // Focus en el campo de texto
        txtMontoInicial.selectAll();
        txtMontoInicial.requestFocusInWindow();
    }

    private void aperturarCaja() {
        // Validar monto
        String montoStr = txtMontoInicial.getText().trim();
        if (montoStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe ingresar el monto inicial.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            txtMontoInicial.requestFocus();
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
            txtMontoInicial.selectAll();
            txtMontoInicial.requestFocus();
            return;
        }

        // Guardar en base de datos
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "INSERT INTO caja_apertura_cierre " +
                    "(datetime_apertura, monto_apertura, id_usuario_apertura) " +
                    "VALUES (NOW(), ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, monto);
                ps.setInt(2, usuarioId);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    aperturaExitosa = true;
                    JOptionPane.showMessageDialog(this,
                            String.format("Caja aperturada exitosamente con S/. %.2f", monto),
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "No se pudo aperturar la caja. Intente nuevamente.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al aperturar la caja:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public boolean isAperturaExitosa() {
        return aperturaExitosa;
    }

    /**
     * Verifica si hay apertura de caja para HOY
     *
     * @return true si ya hay apertura hoy, false si no
     */
    public static boolean hayAperturaHoy() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT id FROM caja_apertura_cierre " +
                    "WHERE DATE(datetime_apertura) = CURDATE()";

            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
