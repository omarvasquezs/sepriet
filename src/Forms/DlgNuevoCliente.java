package Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

/** Dialog to add a new Cliente. Required: nombres, dni, telefono. DNI must be unique. */
public class DlgNuevoCliente extends JDialog {
    private final JTextField txtNombres = new JTextField();
    private final JTextField txtDni = new JTextField();
    private final JTextField txtTelefono = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextArea txtDireccion = new JTextArea(5, 30);
    private final frmRegistrarComprobante parent; // may be null when opened standalone

    /** Main constructor used when launched from frmRegistrarComprobante */
    public DlgNuevoCliente(Window owner, frmRegistrarComprobante parent) {
        super(owner, "Añadir Cliente", ModalityType.APPLICATION_MODAL);
        this.parent = parent;
        buildAndInit();
    }

    /** Convenience constructor for standalone use (e.g., main menu) */
    public DlgNuevoCliente(Window owner) {
        this(owner, null);
    }

    private void buildAndInit() {
        buildUI();
    setSize(720, 480);
        setResizable(false);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(10, 12, 10, 12);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

    // Larger fonts and sizes
    Font labelFont = new Font("Segoe UI", Font.BOLD, 16);
    Font fieldFont = new Font("Segoe UI", Font.PLAIN, 16);
    Font btnFont = new Font("Segoe UI", Font.BOLD, 15);

        int row = 0;
    JLabel lblNombre = new JLabel("Nombre Completo *:"); lblNombre.setFont(labelFont);
    txtNombres.setFont(fieldFont); txtNombres.setPreferredSize(new Dimension(480, 34));
    addRow(form, c, row++, lblNombre, txtNombres);

    JLabel lblDni = new JLabel("DNI *:"); lblDni.setFont(labelFont);
    txtDni.setFont(fieldFont); txtDni.setPreferredSize(new Dimension(240, 34));
    addRow(form, c, row++, lblDni, txtDni);

    JLabel lblTelefono = new JLabel("Teléfono *:"); lblTelefono.setFont(labelFont);
    txtTelefono.setFont(fieldFont); txtTelefono.setPreferredSize(new Dimension(240, 34));
    addRow(form, c, row++, lblTelefono, txtTelefono);

    JLabel lblEmail = new JLabel("E-mail:"); lblEmail.setFont(labelFont);
    txtEmail.setFont(fieldFont); txtEmail.setPreferredSize(new Dimension(360, 34));
    addRow(form, c, row++, lblEmail, txtEmail);
    JLabel lblDireccion = new JLabel("Dirección:"); lblDireccion.setFont(labelFont);
    // place label
    c.gridy = row; c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE; form.add(lblDireccion, c);
    // place textarea inside a scrollpane and allow it to expand vertically
    c.gridx = 1; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
    JScrollPane sp = new JScrollPane(txtDireccion, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    txtDireccion.setFont(fieldFont);
    sp.setPreferredSize(new Dimension(520, 140));
    form.add(sp, c);
    // reset weighty so subsequent components (buttons) do not get extra space
    c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton btnGuardar = new JButton("Guardar"); btnGuardar.setFont(btnFont); btnGuardar.setPreferredSize(new Dimension(120, 38));
    btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
    JButton btnCancelar = new JButton("Cancelar"); btnCancelar.setFont(btnFont); btnCancelar.setPreferredSize(new Dimension(120, 38));
    btnCancelar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buttons.add(btnGuardar);
        buttons.add(btnCancelar);

        btnGuardar.addActionListener(this::guardarCliente);
        btnCancelar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, JComponent label, JComponent field) {
    c.gridy = row; c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE; form.add(label, c);
    c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; form.add(field, c);
    }

    private void guardarCliente(ActionEvent e) {
        String nombres = txtNombres.getText().trim();
        String dni = txtDni.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();
        String direccion = txtDireccion.getText().trim();

        if (nombres.isEmpty() || dni.isEmpty() || telefono.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete los campos obligatorios (Nombre, DNI, Teléfono).", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!dni.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "El DNI debe contener solo dígitos.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Insert into DB ensuring unique DNI
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Check uniqueness
            try (PreparedStatement chk = conn.prepareStatement("SELECT COUNT(*) FROM clientes WHERE dni = ?")) {
                chk.setString(1, dni);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this, "El DNI ya existe.", "Validación", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            String sql = "INSERT INTO clientes (nombres, dni, telefono, email, direccion) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nombres);
                ps.setString(2, dni);
                ps.setString(3, telefono);
                if (!email.isEmpty()) ps.setString(4, email); else ps.setNull(4, java.sql.Types.VARCHAR);
                if (!direccion.isEmpty()) ps.setString(5, direccion); else ps.setNull(5, java.sql.Types.CLOB);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Cliente guardado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            if (parent != null) { // refresh only if opened from registrar comprobante
                parent.loadClientes();
                parent.selectClienteByName(nombres);
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error guardando cliente:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
