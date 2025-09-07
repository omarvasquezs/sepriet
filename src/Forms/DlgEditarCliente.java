package Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DlgEditarCliente extends JDialog {
    private final JTextField txtNombres = new JTextField();
    private final JTextField txtDni = new JTextField();
    private final JTextField txtTelefono = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextArea txtDireccion = new JTextArea(5, 30);
    private final int clienteId;
    private final Runnable onSaved;

    public DlgEditarCliente(Window owner, int id, Runnable onSaved) {
        super(owner, "Editar Cliente", ModalityType.APPLICATION_MODAL);
        this.clienteId = id;
        this.onSaved = onSaved;
        buildUI();
        loadCliente();
        setSize(640, 420);
        setResizable(false);
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;

        addRow(form, c, 0, new JLabel("Nombres:"), txtNombres);
        TextCaseUtils.applyUppercase(txtNombres);
        addRow(form, c, 1, new JLabel("DNI:"), txtDni);
        // DNI digit-only
        try {
            ((AbstractDocument) txtDni.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    if (string == null)
                        return;
                    String filtered = string.replaceAll("[^0-9]", "");
                    super.insertString(fb, offset, filtered, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (text == null) {
                        super.replace(fb, offset, length, text, attrs);
                        return;
                    }
                    String filtered = text.replaceAll("[^0-9]", "");
                    super.replace(fb, offset, length, filtered, attrs);
                }
            });
        } catch (Exception ignore) {
        }
        addRow(form, c, 2, new JLabel("Teléfono:"), txtTelefono);
        // Telefono digit-only
        try {
            ((AbstractDocument) txtTelefono.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    if (string == null)
                        return;
                    String filtered = string.replaceAll("[^0-9]", "");
                    super.insertString(fb, offset, filtered, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (text == null) {
                        super.replace(fb, offset, length, text, attrs);
                        return;
                    }
                    String filtered = text.replaceAll("[^0-9]", "");
                    super.replace(fb, offset, length, filtered, attrs);
                }
            });
        } catch (Exception ignore) {
        }
        addRow(form, c, 3, new JLabel("E-mail:"), txtEmail);
        TextCaseUtils.applyUppercase(txtEmail);

        c.gridy = 4;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Dirección:"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        JScrollPane sp = new JScrollPane(txtDireccion);
        TextCaseUtils.applyUppercase(txtDireccion);
        sp.setPreferredSize(new Dimension(420, 120));
        form.add(sp, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        btns.add(btnSave);
        btns.add(btnCancel);

        btnSave.addActionListener(this::onSave);
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(btns, BorderLayout.SOUTH);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, JComponent label, JComponent field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        p.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(field, c);
    }

    private void loadCliente() {
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT nombres,dni,telefono,email,direccion FROM clientes WHERE id=?")) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtNombres.setText(rs.getString(1));
                    txtDni.setText(rs.getString(2));
                    txtTelefono.setText(rs.getString(3));
                    txtEmail.setText(rs.getString(4));
                    txtDireccion.setText(rs.getString(5));
                }
            }
        } catch (Exception ex) {
            // ignore
        }
    }

    private void onSave(ActionEvent ev) {
        String nombres = txtNombres.getText().trim();
        String dni = txtDni.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();
        String direccion = txtDireccion.getText().trim();
        if (nombres.isEmpty() || dni.isEmpty() || telefono.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Complete los campos obligatorios.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE clientes SET nombres=?,dni=?,telefono=?,email=?,direccion=? WHERE id=?")) {
                ps.setString(1, nombres);
                ps.setString(2, dni);
                ps.setString(3, telefono);
                if (!email.isEmpty())
                    ps.setString(4, email);
                else
                    ps.setNull(4, java.sql.Types.VARCHAR);
                if (!direccion.isEmpty())
                    ps.setString(5, direccion);
                else
                    ps.setNull(5, java.sql.Types.CLOB);
                ps.setInt(6, clienteId);
                ps.executeUpdate();
            }
            if (onSaved != null)
                onSaved.run();
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error guardando cliente:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
