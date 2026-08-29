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
 * Custom dialog for WhatsApp sending that allows editing both phone number and
 * country code
 */
public class WhatsAppDialog extends JDialog {
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
                if (string != null && string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null && text.matches("\\d*")) {
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
