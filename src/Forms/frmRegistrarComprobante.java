/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JInternalFrame.java to edit this template
 */
package Forms;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import javax.swing.ImageIcon;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.text.JTextComponent;
import javax.swing.event.TableModelEvent;
import java.util.Vector;
import javax.swing.ComboBoxModel;

/**
 *
 * @author omarv
 */
public class frmRegistrarComprobante extends javax.swing.JInternalFrame {

    /**
     * Creates new form frmRegistrarComprobante
     */
    public frmRegistrarComprobante() {
        initComponents();
        loadClientes();
        loadMetodosPago();
        loadServicios();
        loadEstadoComprobante();
        // make all combo-boxes filter as you type
        AutoCompleteDecorator.decorate(cbxCliente);
        AutoCompleteDecorator.decorate(cbxServicio);
        // Make combos editable and install placeholder behavior so the placeholder
        // disappears on click/focus and when the user erases the text it stays empty
        setupComboPlaceholderBehavior(cbxCliente, "-- Seleccione un cliente --");
        setupComboPlaceholderBehavior(cbxServicio, "-- Seleccione un servicio --");
        this.setSize(1100, 600);
        this.setTitle("REGISTRAR COMPROBANTE");
        // Set the DateTimePicker to the current date and time
        dateTimePicker1.setDateTimePermissive(LocalDateTime.now());
        // Add listeners to radio buttons to toggle RUC and Razon Social fields
        radioFactura.addActionListener(_ -> toggleRucFields());
        radioNotaVenta.addActionListener(_ -> toggleRucFields());
        radioBoleta.addActionListener(_ -> toggleRucFields());
        // Add listener for estado combo box
        cbxEstadoComprobante.addActionListener(_ -> toggleMontoAbonado());
        // Load and set the internal‐frame icon
        ImageIcon icon = new ImageIcon(getClass().getResource("/Forms/icon.png"));
        this.setFrameIcon(icon);
        // Restrict RUC field to digits only
        AbstractDocument rucDoc = (AbstractDocument) txtRUC.getDocument();
        rucDoc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb,
                    int offset,
                    String string,
                    AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb,
                    int offset,
                    int length,
                    String text,
                    AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        // Restrict MONTO ABONADO field to integers or decimal numbers (allows e.g., 10,
        // 10.5, 10.50)
        AbstractDocument montoDoc = (AbstractDocument) txtMontoAbonado.getDocument();
        montoDoc.setDocumentFilter(new DocumentFilter() {
            private boolean isValid(String text) {
                if (text.isEmpty())
                    return true; // allow empty while typing
                return text.matches("\\d+(\\.\\d*)?"); // digits, optional decimal part
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string == null)
                    return;
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.insert(offset, string);
                if (isValid(sb.toString())) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text == null)
                    text = "";
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.replace(offset, offset + length, text);
                if (isValid(sb.toString())) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        btnGenerarComprobante.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnAgregarNuevoCliente.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnAgregarServicioComprobante.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        // Initialize the state of controls
        toggleRucFields();
        toggleMontoAbonado();
        // Initialize table cell editors
        setupTableCellEditors();
        // Change cursor to hand when hovering over ACCIONES buttons
        jTable1.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = jTable1.columnAtPoint(e.getPoint());
                int row = jTable1.rowAtPoint(e.getPoint());
                if (col == 4 && row >= 0) {
                    Object servicioValue = jTable1.getValueAt(row, 0);
                    if (servicioValue != null && !servicioValue.toString().trim().isEmpty()) {
                        jTable1.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                        return;
                    }
                }
                jTable1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        });

        // Action for generating (saving) the comprobante
        btnGenerarComprobante.addActionListener(_ -> saveComprobante());
        btnAgregarNuevoCliente.addActionListener(_ -> openNuevoClienteDialog());
    }

    /**
     * Make a combo editable and ensure placeholder handling:
     * - when focused or clicked, if the editor text equals the placeholder it is
     * cleared
     * - if user deletes all text, the editor stays empty (and placeholder is not
     * re-selected)
     */
    private void setupComboPlaceholderBehavior(javax.swing.JComboBox<String> combo, String placeholder) {
        combo.setEditable(true);
        // Ensure the model still contains the placeholder as first element
        ComboBoxModel<String> model = combo.getModel();
        if (model.getSize() == 0 || model.getElementAt(0) == null || !model.getElementAt(0).equals(placeholder)) {
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            m.addElement(placeholder);
            // copy existing items if any
            for (int i = 0; i < model.getSize(); i++) {
                String it = model.getElementAt(i);
                if (it != null && !it.equals(placeholder))
                    m.addElement(it);
            }
            combo.setModel(m);
        }

        // Access the editor component (a JTextComponent) for listening
        java.awt.Component editorComp = combo.getEditor().getEditorComponent();
        if (editorComp instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) editorComp;
            // Do NOT prefill the editor here. Prefill only when the editor is not focused
            // so we don't interfere with autocomplete or user typing.

            // When focus gained, if the editor currently shows the placeholder, clear it so
            // user types
            tc.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    try {
                        if (tc.getText() != null && tc.getText().equals(placeholder)) {
                            tc.setText("");
                        }
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    // If the editor is empty when losing focus and the model still has the
                    // placeholder as first element, show the placeholder text for visual cue.
                    try {
                        String t = tc.getText();
                        if (t == null || t.trim().isEmpty()) {
                            if (combo.getModel().getSize() > 0
                                    && combo.getModel().getElementAt(0).equals(placeholder)) {
                                tc.setText(placeholder);
                            } else {
                                tc.setText("");
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            });

            // Mouse press: request focus but don't forcibly clear or change selection here.
            // This avoids clashing with the dropdown selection and the autocomplete
            // decorator.
            tc.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    tc.requestFocusInWindow();
                }
            });

            // Avoid modifying combo selection from document changes. The autocomplete
            // and selection model should be the source of truth. We only keep the
            // editor placeholder visual in focusLost.
            // However, keep an action listener so when the selected item changes and the
            // editor is not focused we reflect the selection in the editor for clarity.
            combo.addActionListener((ActionEvent e) -> {
                try {
                    if (!tc.isFocusOwner()) {
                        Object sel = combo.getSelectedItem();
                        if (sel == null) {
                            // if model has placeholder, show it; otherwise clear
                            if (combo.getModel().getSize() > 0
                                    && combo.getModel().getElementAt(0).equals(placeholder)) {
                                tc.setText(placeholder);
                            } else {
                                tc.setText("");
                            }
                        } else {
                            tc.setText(sel.toString());
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void toggleRucFields() {
        boolean enable = radioFactura.isSelected();
        txtRUC.setEnabled(enable);
        txtRazonSocial.setEnabled(enable);
    }

    private void toggleMontoAbonado() {
        Object selectedItem = cbxEstadoComprobante.getSelectedItem();
        if (selectedItem == null || selectedItem.toString().startsWith("--")) {
            txtMontoAbonado.setEnabled(false); // Disable if placeholder is selected
            txtMontoAbonado.setText("");
            cbxMetodoPago.setEnabled(true); // default
            return;
        }
        String estado = selectedItem.toString().trim().toUpperCase();
        switch (estado) {
            case "ABONO":
                txtMontoAbonado.setEnabled(true);
                // Do not overwrite existing manual input
                cbxMetodoPago.setEnabled(true);
                break;
            case "CANCELADO":
                // Auto fill with total and disable editing
                txtMontoAbonado.setText(extractNumeric(jLabel14.getText()));
                txtMontoAbonado.setEnabled(false);
                cbxMetodoPago.setEnabled(true);
                break;
            case "DEBE":
                // No payment yet, disable monto + metodo de pago
                txtMontoAbonado.setText("");
                txtMontoAbonado.setEnabled(false);
                cbxMetodoPago.setEnabled(false);
                cbxMetodoPago.setSelectedIndex(0); // placeholder
                break;
            default:
                txtMontoAbonado.setText("");
                txtMontoAbonado.setEnabled(false);
                cbxMetodoPago.setEnabled(true);
                break;
        }
    }

    public void loadClientes() {
        final String sql = "SELECT id, nombres FROM clientes ORDER BY nombres";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("-- Seleccione un cliente --"); // Add placeholder
            while (rs.next()) {
                model.addElement(rs.getString("nombres"));
            }
            cbxCliente.setModel(model);
            cbxCliente.setSelectedIndex(0); // Select the placeholder
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando clientes:\n" + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void selectClienteByName(String nombre) {
        if (nombre == null)
            return;
        for (int i = 0; i < cbxCliente.getItemCount(); i++) {
            String item = cbxCliente.getItemAt(i);
            if (item != null && item.equalsIgnoreCase(nombre)) {
                cbxCliente.setSelectedIndex(i);
                break;
            }
        }
    }

    private void openNuevoClienteDialog() {
        DlgNuevoCliente dlg = new DlgNuevoCliente(javax.swing.SwingUtilities.getWindowAncestor(this), this);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    /**
     * Load enabled payment methods from MariaDB and populate cbxMetodoPago.
     */
    private void loadMetodosPago() {
        final String sql = "SELECT nom_metodo_pago FROM metodo_pago WHERE habilitado = 1 ORDER BY nom_metodo_pago";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("-- Seleccione método de pago --"); // Add placeholder
            while (rs.next()) {
                model.addElement(rs.getString("nom_metodo_pago"));
            }
            cbxMetodoPago.setModel(model);
            cbxMetodoPago.setSelectedIndex(0); // Select the placeholder

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando métodos de pago:\n" + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Load enabled servicios from MariaDB and populate cbxServicio.
     */
    private void loadServicios() {
        final String sql = "SELECT nom_servicio FROM servicios WHERE habilitado = 1 ORDER BY nom_servicio";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("-- Seleccione un servicio --"); // Add placeholder
            while (rs.next()) {
                model.addElement(rs.getString("nom_servicio"));
            }
            cbxServicio.setModel(model);
            cbxServicio.setSelectedIndex(0); // Select the placeholder

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando servicios:\n" + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadEstadoComprobante() {
        final String sql = "SELECT nom_estado FROM estado_comprobantes WHERE habilitado = 1 AND nom_estado <> 'ANULADO' ORDER BY nom_estado";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("-- Seleccione un estado --"); // Add placeholder
            while (rs.next()) {
                model.addElement(rs.getString("nom_estado").trim());
            }
            cbxEstadoComprobante.setModel(model);
            cbxEstadoComprobante.setSelectedIndex(0); // Select the placeholder
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error cargando estados de comprobante:\n" + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        btnAgregarNuevoCliente = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        cbxEstadoComprobante = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        cbxMetodoPago = new javax.swing.JComboBox<>();
        radioFactura = new javax.swing.JRadioButton();
        radioNotaVenta = new javax.swing.JRadioButton();
        radioBoleta = new javax.swing.JRadioButton();
        lblTitulo = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtRUC = new javax.swing.JTextField();
        txtRazonSocial = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        dateTimePicker1 = new com.github.lgooddatepicker.components.DateTimePicker();
        cbxCliente = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        txtMontoAbonado = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        btnGenerarComprobante = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        cbxServicio = new javax.swing.JComboBox<>();
        btnAgregarServicioComprobante = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel2.setText("CLIENTE:");

        btnAgregarNuevoCliente.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        btnAgregarNuevoCliente.setText("AÑADIR NUEVO CLIENTE");

        jLabel3.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel3.setText("ESTADO:");

        cbxEstadoComprobante.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N

        jLabel1.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel1.setText("CONDICIÓN DE PAGO:");

        cbxMetodoPago.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N

        buttonGroup1.add(radioFactura);
        radioFactura.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        radioFactura.setText("FACTURA");

        buttonGroup1.add(radioNotaVenta);
        radioNotaVenta.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        radioNotaVenta.setText("NOTA DE VENTA");

        buttonGroup1.add(radioBoleta);
        radioBoleta.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        radioBoleta.setText("BOLETA");

        lblTitulo.setFont(new java.awt.Font("sansserif", 1, 24)); // NOI18N
        lblTitulo.setText("REGISTRO DE COMPROBANTE");

        jLabel4.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel4.setText("RUC:");

        jLabel5.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel5.setText("RAZON SOCIAL:");

        txtRUC.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        txtRUC.setEnabled(false);

        txtRazonSocial.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        txtRazonSocial.setEnabled(false);

        jLabel6.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel6.setText("CREACIÓN:");

        dateTimePicker1.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N

        cbxCliente.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N

        jLabel7.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel7.setText("OP. GRAVADAS:");

        jLabel8.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel8.setText("IGV 18%:");

        jLabel9.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel9.setText("TOTAL A PAGAR:");

        jLabel10.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel10.setText("MONTO ABONADO:");

        txtMontoAbonado.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        txtMontoAbonado.setEnabled(false);

        jLabel11.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel11.setText("OBSERVACIONES:");

        txtObservaciones.setColumns(20);
        txtObservaciones.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        txtObservaciones.setRows(5);
        jScrollPane1.setViewportView(txtObservaciones);

        btnGenerarComprobante.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnGenerarComprobante.setText("GENERAR");

        jLabel12.setText("S/. 0.00");
        jLabel12.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jLabel13.setText("S/. 0.00");
        jLabel13.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jLabel14.setText("S/. 0.00");
        jLabel14.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        cbxServicio.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N

        btnAgregarServicioComprobante.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        btnAgregarServicioComprobante.setText("AÑADIR AL COMPROBANTE");
        btnAgregarServicioComprobante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarServicioComprobanteActionPerformed(evt);
            }
        });

        jLabel15.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jLabel15.setText("SELECCIONAR SERVICIO:");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null },
                        { null, null, null, null, null }
                },
                new String[] {
                        "SERVICIO", "PESO EN KG", "PRECIO POR KG (S/.)", "TOTAL (S/.)", "ACCIONES"
                }));
        jTable1.setRowHeight(22);
        jScrollPane2.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(4).setMaxWidth(80);
        }

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblTitulo)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(704, 704, 704)
                                                .addComponent(jLabel6)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(dateTimePicker1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        256, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addComponent(jLabel1)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cbxMetodoPago, 0,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        Short.MAX_VALUE))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                jPanel1Layout.createSequentialGroup()
                                                                        .addGroup(jPanel1Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel3)
                                                                                .addComponent(jLabel2))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                        .addGroup(jPanel1Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                        jPanel1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(
                                                                                                        cbxEstadoComprobante,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        227,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(18, 18, 18)
                                                                                                .addComponent(
                                                                                                        radioNotaVenta)
                                                                                                .addGap(19, 19, 19)
                                                                                                .addComponent(
                                                                                                        radioBoleta)
                                                                                                .addGap(18, 18, 18)
                                                                                                .addComponent(
                                                                                                        radioFactura))
                                                                                .addGroup(
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                        jPanel1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(
                                                                                                        cbxCliente,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        333,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(
                                                                                                        btnAgregarNuevoCliente,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                        234,
                                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                jPanel1Layout.createSequentialGroup()
                                                                        .addComponent(jLabel4)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(txtRUC,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                200,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                        .addComponent(jLabel5)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(txtRazonSocial))
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addComponent(cbxServicio,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 410,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnAgregarServicioComprobante,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        Short.MAX_VALUE))
                                                        .addComponent(jLabel15)
                                                        .addComponent(jScrollPane2))
                                                .addGap(43, 43, 43)
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(btnGenerarComprobante,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jScrollPane1)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                jPanel1Layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel11)
                                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                                .addComponent(jLabel10)
                                                                                .addPreferredGap(
                                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                .addComponent(txtMontoAbonado,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        202,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                jPanel1Layout.createSequentialGroup()
                                                                        .addGroup(jPanel1Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel7)
                                                                                .addComponent(jLabel8)
                                                                                .addComponent(jLabel9))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                        .addGroup(jPanel1Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel14,
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                .addComponent(jLabel13,
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                .addComponent(jLabel12,
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING))))))
                                .addGap(15, 15, 15)));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(lblTitulo)
                                .addGap(27, 27, 27)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                        .addComponent(jLabel2)
                                        .addComponent(btnAgregarNuevoCliente, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel6)
                                        .addComponent(dateTimePicker1, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cbxCliente, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel7)
                                                        .addComponent(jLabel12))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel8)
                                                        .addComponent(jLabel13))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel9)
                                                        .addComponent(jLabel14))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel10)
                                                        .addComponent(txtMontoAbonado,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel1Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.CENTER)
                                                                .addComponent(jLabel3)
                                                                .addComponent(cbxEstadoComprobante,
                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(jPanel1Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.CENTER)
                                                                .addComponent(radioNotaVenta)
                                                                .addComponent(radioBoleta)
                                                                .addComponent(radioFactura)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(cbxMetodoPago,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel1))
                                                .addGap(18, 18, 18)
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel4)
                                                        .addComponent(jLabel5)
                                                        .addComponent(txtRUC, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtRazonSocial,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(28, 28, 28)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel15)
                                        .addComponent(jLabel11))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                jPanel1Layout.createSequentialGroup()
                                                        .addComponent(jScrollPane1)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                        .addComponent(btnGenerarComprobante,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                                        .addComponent(cbxServicio,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnAgregarServicioComprobante,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 138,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(194, Short.MAX_VALUE)));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 1070, 680));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgregarServicioComprobanteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAgregarServicioComprobanteActionPerformed
        // Resolve the selected service robustly. For editable/autocompleted combos
        // require an exact match against the combo model (case-insensitive).
        Object selectedService = cbxServicio.getSelectedItem();
        String serviceName = null;
        if (selectedService != null)
            serviceName = selectedService.toString().trim();
        // If selected is invalid, try the editor's current text and match it to the
        // model
        if (serviceName == null || serviceName.isEmpty() || serviceName.startsWith("--")) {
            try {
                Object editorItem = cbxServicio.getEditor().getItem();
                if (editorItem != null) {
                    String ed = editorItem.toString().trim();
                    if (!ed.isEmpty() && !ed.startsWith("--")) {
                        // search model for a case-insensitive match; do NOT accept free-typed values
                        ComboBoxModel<String> m = cbxServicio.getModel();
                        for (int i = 0; i < m.getSize(); i++) {
                            String it = m.getElementAt(i);
                            if (it != null && it.equalsIgnoreCase(ed)) {
                                serviceName = it; // use the canonical model value
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
        // Require an exact match (no free-typed services)
        if (serviceName == null || serviceName.isEmpty() || serviceName.startsWith("--")) {
            JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un servicio válido.",
                    "Servicio no seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Extra guard: avoid adding a service that is already present in the table (in
        // case it wasn't removed from combo por algún motivo)
        if (isServiceAlreadyInTable(serviceName)) {
            JOptionPane.showMessageDialog(this,
                    "El servicio ya fue añadido. Primero elimínelo de la tabla si desea volver a agregarlo.",
                    "Servicio duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Get the price per kg for this service from database
        double precioPorKg = getPrecioServicio(serviceName);
        // Add to table with empty PESO field and editable cells
        addServiceToTable(serviceName, "", String.format("%.2f", precioPorKg), "0.00", "X");
        // Remove the added service from the combo to prevent duplicates
        removeServiceFromCombo(serviceName);
        // Reset the combo box to the placeholder
        cbxServicio.setSelectedIndex(0);
        // Set up cell editors for PESO and PRECIO columns if not already set
        setupTableCellEditors();
        // Set up delete button renderer/editor
        setupTableButtons();
    }
    // GEN-LAST:event_btnAgregarServicioComprobanteActionPerformed

    /**
     * Gets the price per kg for a given service from the database
     *
     * @param serviceName The name of the service
     * @return The price per kg
     */
    private double getPrecioServicio(String serviceName) {
        // Query to get the price from the database
        final String sql = "SELECT precio_kilo FROM servicios WHERE nom_servicio = ? AND habilitado = 1";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serviceName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("precio_kilo");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error obteniendo precio del servicio:\n" + ex.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }
        return 0.0; // Default price if not found
    }

    /**
     * Adds a service to the table with editable PESO and PRECIO fields
     *
     * @param servicio    The service name
     * @param peso        The weight in kg (as String, initially empty)
     * @param precioPorKg The price per kg (as String)
     * @param total       The total price (as String, initially "0.00")
     * @param acciones    The value for the ACCIONES column ("X")
     */
    private void addServiceToTable(String servicio, String peso, String precioPorKg, String total, String acciones) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();

        // Find the first empty row to insert the data
        int firstEmptyRow = -1;
        for (int i = 0; i < model.getRowCount(); i++) {
            Object servicioValue = model.getValueAt(i, 0);
            if (servicioValue == null || servicioValue.toString().trim().isEmpty()) {
                firstEmptyRow = i;
                break;
            }
        }

        if (firstEmptyRow != -1) {
            // Insert into the first empty row
            model.setValueAt(servicio, firstEmptyRow, 0);
            model.setValueAt(peso, firstEmptyRow, 1);
            model.setValueAt(precioPorKg, firstEmptyRow, 2);
            model.setValueAt(total, firstEmptyRow, 3);
            model.setValueAt(acciones, firstEmptyRow, 4);
        } else {
            // If no empty row found, add a new row
            model.addRow(new Object[] { servicio, peso, precioPorKg, total, acciones });
        }

        updateTotals();
    }

    /**
     * Sets up the table with custom cell editors for numeric columns
     */
    private void setupTableCellEditors() {
        // Ensure TOTAL column (index 3) is not editable by using a custom table model
        DefaultTableModel oldModel = (DefaultTableModel) jTable1.getModel();
        @SuppressWarnings("unchecked")
        Vector<Vector<Object>> data = (Vector<Vector<Object>>) (Vector<?>) oldModel.getDataVector();
        Vector<String> cols = new Vector<>();
        for (int i = 0; i < oldModel.getColumnCount(); i++) {
            cols.add(oldModel.getColumnName(i));
        }
        DefaultTableModel newModel = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // If the SERVICIO cell is empty for this row, make the entire row non-editable
                Object servicioVal = null;
                if (row >= 0 && row < getRowCount()) {
                    servicioVal = getValueAt(row, 0);
                }
                if (servicioVal == null || servicioVal.toString().trim().isEmpty()) {
                    return false;
                }
                // Allow editing only for PESO (1), PRECIO (2) and ACCIONES (4)
                return column == 1 || column == 2 || column == 4;
            }
        };
        jTable1.setModel(newModel);
        // Shared numeric document filter
        NumericDocumentFilter numericFilter = new NumericDocumentFilter();

        // Custom editor for PESO column (only integers or decimals while typing)
        javax.swing.JTextField pesoField = new javax.swing.JTextField();
        ((AbstractDocument) pesoField.getDocument()).setDocumentFilter(numericFilter);
        jTable1.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(pesoField) {
            @Override
            public boolean stopCellEditing() {
                try {
                    String value = getCellEditorValue().toString();
                    if (!value.isEmpty())
                        Double.parseDouble(value);
                    return super.stopCellEditing();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(jTable1,
                            "Por favor ingrese un valor numérico válido.",
                            "Error de formato", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });

        // Custom editor for PRECIO column (only integers or decimals while typing)
        javax.swing.JTextField precioField = new javax.swing.JTextField();
        ((AbstractDocument) precioField.getDocument()).setDocumentFilter(numericFilter);
        jTable1.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(precioField) {
            @Override
            public boolean stopCellEditing() {
                try {
                    String value = getCellEditorValue().toString();
                    if (!value.isEmpty())
                        Double.parseDouble(value);
                    return super.stopCellEditing();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(jTable1,
                            "Por favor ingrese un valor numérico válido.",
                            "Error de formato", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });
        // Add a listener to recalculate totals when cell editing stops
        jTable1.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                // Only recalculate if weight or price columns were edited
                if (column == 1 || column == 2) {
                    updateRowTotal(row);
                    updateTotals();
                }
            }
        });
    }

    /**
     * Reusable DocumentFilter allowing only integer or decimal numbers (e.g. 10,
     * 10., 10.5, 10.50).
     * Empty string is allowed while user is typing.
     */
    private static class NumericDocumentFilter extends DocumentFilter {
        private boolean isValid(String text) {
            if (text.isEmpty())
                return true;
            return text.matches("\\d+(\\.\\d*)?");
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string == null)
                return;
            StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            sb.insert(offset, string);
            if (isValid(sb.toString())) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null)
                text = "";
            StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            sb.replace(offset, offset + length, text);
            if (isValid(sb.toString())) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    /**
     * Sets up the delete button renderer and editor for the table
     */
    private void setupTableButtons() {
        jTable1.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        jTable1.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    /**
     * Updates the total for a specific row based on weight and price
     *
     * @param row The row to update
     */
    private void updateRowTotal(int row) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        try {
            String pesoStr = model.getValueAt(row, 1).toString().trim();
            String precioStr = model.getValueAt(row, 2).toString().trim();
            if (!pesoStr.isEmpty() && !precioStr.isEmpty()) {
                double peso = Double.parseDouble(pesoStr);
                double precio = Double.parseDouble(precioStr);
                double total = peso * precio;
                model.setValueAt(String.format("%.2f", total), row, 3);
            }
        } catch (Exception ex) {
            // If there's an error parsing numbers, leave the total as is
        }
    }

    // Class to render the delete button in the table
    class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
            setText("X");
            setBackground(Color.RED);
            setForeground(Color.WHITE);
            setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            setBorder(javax.swing.BorderFactory.createRaisedBevelBorder());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            Object servicioValue = table.getValueAt(row, 0);
            if (servicioValue != null && !servicioValue.toString().trim().isEmpty()) {
                // Return a standard JButton labeled "Borrar"
                JButton btn = new JButton("Borrar");
                btn.setOpaque(true);
                btn.setFocusable(true);
                btn.setFocusPainted(true);
                btn.setBorderPainted(true);
                btn.setContentAreaFilled(true);
                // Use LAF default colors (do not force red)
                btn.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
                btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                return btn;
            } else {
                // Return an empty label for empty rows
                return new javax.swing.JLabel("");
            }
        }
    }

    // Class to handle the delete button click
    class ButtonEditor extends DefaultCellEditor {

        protected JButton button;
        private String label;
        private boolean isPushed;
        private int targetRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setText("X");
            button.setBackground(Color.RED);
            button.setForeground(Color.WHITE);
            button.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            button.setBorder(javax.swing.BorderFactory.createRaisedBevelBorder());

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            Object servicioValue = table.getValueAt(row, 0);
            if (servicioValue != null && !servicioValue.toString().trim().isEmpty()) {
                label = "Borrar";
                button.setText(label);
                // Reset to LAF defaults
                button.setBackground(null);
                button.setForeground(null);
                button.setFocusable(true);
                button.setFocusPainted(true);
                button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                targetRow = row;
                isPushed = true;
                return button;
            } else {
                // If row empty, return an empty label to avoid showing an editor
                isPushed = false;
                return new javax.swing.JLabel("");
            }
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Clear the row instead of removing it to maintain table structure
                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                String servicioEliminado = "";
                Object servicioObj = model.getValueAt(targetRow, 0);
                if (servicioObj != null) {
                    servicioEliminado = servicioObj.toString();
                }
                model.setValueAt("", targetRow, 0); // SERVICIO
                model.setValueAt("", targetRow, 1); // PESO EN KG
                model.setValueAt("", targetRow, 2); // PRECIO POR KG
                model.setValueAt("", targetRow, 3); // TOTAL
                model.setValueAt("", targetRow, 4); // ACCIONES
                // Re-add service to combo (if any) so it becomes selectable again
                if (servicioEliminado != null && !servicioEliminado.isBlank()) {
                    addServiceBackToCombo(servicioEliminado);
                }
                // Update totals
                updateTotals();
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    /**
     * Updates the total amounts displayed in the UI
     */
    private void updateTotals() {
        double subtotal = 0.0;
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();

        // Sum up all totals from the table
        for (int i = 0; i < model.getRowCount(); i++) {
            Object totalObj = model.getValueAt(i, 3);
            if (totalObj != null && !totalObj.toString().isEmpty()) {
                try {
                    subtotal += Double.parseDouble(totalObj.toString().replace("S/.", "").trim());
                } catch (NumberFormatException e) {
                    // Skip this value if it can't be parsed
                }
            }
        }

        // Following the web behaviour: total = sum of service totals; IGV = total *
        // 0.18;
        // subtotal (OP. GRAVADAS) = total - IGV
        double total = subtotal;
        double igv = total * 0.18;
        double subtotalVisible = total - igv;

        // Update the labels
        jLabel12.setText("S/. " + String.format("%.2f", subtotalVisible));
        jLabel13.setText("S/. " + String.format("%.2f", igv));
        jLabel14.setText("S/. " + String.format("%.2f", total));
        // If estado is CANCELADO keep monto abonado synced with total
        Object estadoSel = cbxEstadoComprobante.getSelectedItem();
        if (estadoSel != null && "CANCELADO".equalsIgnoreCase(estadoSel.toString().trim())) {
            txtMontoAbonado.setText(String.format("%.2f", total));
        }
    }

    private String extractNumeric(String labelText) {
        if (labelText == null)
            return "";
        // Expect format "S/. 123.45" -> return 123.45
        String cleaned = labelText.replace("S/.", "").replace("S/", "").trim();
        return cleaned;
    }

    /**
     * Checks if a service name is already present (non-empty) in the table.
     */
    private boolean isServiceAlreadyInTable(String serviceName) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object val = model.getValueAt(i, 0);
            if (val != null && !val.toString().isBlank() && val.toString().equalsIgnoreCase(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a service (by its display name) from the combo box model.
     */
    private void removeServiceFromCombo(String serviceName) {
        ComboBoxModel<String> m = cbxServicio.getModel();
        if (m instanceof DefaultComboBoxModel) {
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) m;
            for (int i = 0; i < model.getSize(); i++) {
                String item = model.getElementAt(i);
                if (item != null && item.equalsIgnoreCase(serviceName)) {
                    model.removeElementAt(i);
                    break;
                }
            }
        }
    }

    /**
     * Adds a service back into the combo box (after deletion) if it's not already
     * there.
     * Inserts after the placeholder and keeps alphabetical order relative to
     * existing items (optional improvement).
     */
    private void addServiceBackToCombo(String serviceName) {
        ComboBoxModel<String> m = cbxServicio.getModel();
        if (!(m instanceof DefaultComboBoxModel))
            return;
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) m;
        // Check if already present
        for (int i = 0; i < model.getSize(); i++) {
            String item = model.getElementAt(i);
            if (item != null && item.equalsIgnoreCase(serviceName)) {
                return; // already present
            }
        }
        // Find insertion index (keep first element as placeholder)
        int insertIndex = model.getSize(); // default append
        // Attempt alphabetical insertion starting from index 1
        for (int i = 1; i < model.getSize(); i++) {
            String item = model.getElementAt(i);
            if (item != null && serviceName.compareToIgnoreCase(item) < 0) {
                insertIndex = i;
                break;
            }
        }
        model.insertElementAt(serviceName, insertIndex);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregarNuevoCliente;
    private javax.swing.JButton btnAgregarServicioComprobante;
    private javax.swing.JButton btnGenerarComprobante;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox<String> cbxCliente;
    private javax.swing.JComboBox<String> cbxEstadoComprobante;
    private javax.swing.JComboBox<String> cbxMetodoPago;
    private javax.swing.JComboBox<String> cbxServicio;
    private com.github.lgooddatepicker.components.DateTimePicker dateTimePicker1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JRadioButton radioBoleta;
    private javax.swing.JRadioButton radioFactura;
    private javax.swing.JRadioButton radioNotaVenta;
    private javax.swing.JTextField txtMontoAbonado;
    private javax.swing.JTextArea txtObservaciones;
    private javax.swing.JTextField txtRUC;
    private javax.swing.JTextField txtRazonSocial;
    // End of variables declaration//GEN-END:variables

    // ===================== PERSISTENCE LOGIC =====================
    // Assumptions (adjust as needed):
    // - local_id fixed to 5
    // - estado_ropa_id fixed to 1
    // - user_id & last_updated_by temporarily fixed to 1 (no auth system in Swing
    // client yet)
    private static final int DEFAULT_LOCAL_ID = 5;
    private static final int DEFAULT_ESTADO_ROPA_ID = 1;
    private static final int DEFAULT_USER_ID = 1;

    private void saveComprobante() {
        // VALIDATIONS
        String clienteNombre = (String) cbxCliente.getSelectedItem();
        if (clienteNombre == null || clienteNombre.startsWith("--")) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente válido.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String estadoNombre = (String) cbxEstadoComprobante.getSelectedItem();
        if (estadoNombre == null || estadoNombre.startsWith("--")) {
            JOptionPane.showMessageDialog(this, "Seleccione un estado de comprobante.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String metodoPagoNombre = (String) cbxMetodoPago.getSelectedItem();
        String estadoUpperTmp = estadoNombre.trim().toUpperCase();
        boolean estadoEsDebe = "DEBE".equals(estadoUpperTmp);
        if (!estadoEsDebe) { // For non-DEBE states require a payment method
            if (metodoPagoNombre == null || metodoPagoNombre.startsWith("--")) {
                JOptionPane.showMessageDialog(this, "Seleccione un método de pago.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            // Ignore metodo de pago (should be disabled)
            metodoPagoNombre = null;
        }
        char tipoComprobante;
        if (radioNotaVenta.isSelected())
            tipoComprobante = 'N';
        else if (radioBoleta.isSelected())
            tipoComprobante = 'B';
        else if (radioFactura.isSelected())
            tipoComprobante = 'F';
        else {
            JOptionPane.showMessageDialog(this, "Seleccione un tipo de comprobante.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String numRuc = txtRUC.getText().trim();
        String razonSocial = txtRazonSocial.getText().trim();
        if (tipoComprobante == 'F') { // factura requires RUC & razon social
            if (numRuc.isEmpty() || razonSocial.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese RUC y Razón Social para FACTURA.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else { // clear if not factura
            numRuc = null;
            razonSocial = null;
        }
        // Collect table details
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        java.util.List<ServiceDetail> details = new java.util.ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object servicioObj = model.getValueAt(i, 0);
            if (servicioObj == null)
                continue;
            String servicio = servicioObj.toString().trim();
            if (servicio.isEmpty())
                continue;
            String pesoStr = safeStr(model.getValueAt(i, 1));
            String precioStr = safeStr(model.getValueAt(i, 2));
            if (pesoStr.isEmpty() || precioStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fila " + (i + 1) + ": complete PESO y PRECIO.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double peso = Double.parseDouble(pesoStr);
                double precio = Double.parseDouble(precioStr);
                if (peso <= 0 || precio < 0) {
                    JOptionPane.showMessageDialog(this, "Fila " + (i + 1) + ": valores numéricos inválidos.",
                            "Validación", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                details.add(new ServiceDetail(servicio, peso, precio));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Fila " + (i + 1) + ": formato numérico inválido.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        if (details.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agregue al menos un servicio.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Totals (labels contain e.g. "S/. 123.45")
        double total = parseMoneyLabel(jLabel14.getText());
        double montoAbonado;
        String estadoUpper = estadoNombre.toUpperCase();
        if ("ABONO".equals(estadoUpper)) {
            String montoAbonadoStr = txtMontoAbonado.getText().trim();
            if (montoAbonadoStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese el monto abonado.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                montoAbonado = Double.parseDouble(montoAbonadoStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Monto abonado inválido.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (montoAbonado < 0 || montoAbonado > total) {
                JOptionPane.showMessageDialog(this, "Monto abonado fuera de rango.", "Validación",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else if ("CANCELADO".equals(estadoUpper) || "PAGADO".equals(estadoUpper)) {
            montoAbonado = total; // fully paid
        } else {
            montoAbonado = 0.0; // pending / other states
        }

        LocalDateTime fecha = dateTimePicker1.getDateTimePermissive();
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una fecha válida.", "Validación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String observaciones = txtObservaciones.getText().trim();

        // DB operations
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            int clienteId = fetchId(conn, "SELECT id FROM clientes WHERE nombres = ?", clienteNombre);
            Integer metodoPagoId = null;
            if (metodoPagoNombre != null && !metodoPagoNombre.startsWith("--")) {
                int tmp = fetchId(conn, "SELECT id FROM metodo_pago WHERE nom_metodo_pago = ?", metodoPagoNombre);
                if (tmp != -1)
                    metodoPagoId = tmp;
                else {
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "Método de pago no encontrado.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            int estadoComprobanteId = fetchId(conn, "SELECT id FROM estado_comprobantes WHERE nom_estado = ?",
                    estadoNombre);
            if (clienteId == -1 || estadoComprobanteId == -1) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "No se pudo resolver IDs requeridos.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String codComprobante = generateComprobanteCode(conn, tipoComprobante);
            // Insert comprobante
            String insertComprobanteSql = "INSERT INTO comprobantes (tipo_comprobante, cliente_id, user_id, fecha, metodo_pago_id, num_ruc, razon_social, estado_comprobante_id, estado_ropa_id, local_id, observaciones, monto_abonado, last_updated_by, cod_comprobante, costo_total) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertComprobanteSql,
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, String.valueOf(tipoComprobante));
                ps.setInt(2, clienteId);
                ps.setInt(3, DEFAULT_USER_ID);
                ps.setTimestamp(4, java.sql.Timestamp.valueOf(fecha));
                if (metodoPagoId != null)
                    ps.setInt(5, metodoPagoId);
                else
                    ps.setNull(5, java.sql.Types.INTEGER);
                if (numRuc != null)
                    ps.setLong(6, Long.parseLong(numRuc));
                else
                    ps.setNull(6, java.sql.Types.BIGINT);
                if (razonSocial != null)
                    ps.setString(7, razonSocial);
                else
                    ps.setNull(7, java.sql.Types.VARCHAR);
                ps.setInt(8, estadoComprobanteId);
                ps.setInt(9, DEFAULT_ESTADO_ROPA_ID);
                ps.setInt(10, DEFAULT_LOCAL_ID);
                if (!observaciones.isEmpty())
                    ps.setString(11, observaciones);
                else
                    ps.setNull(11, java.sql.Types.CLOB);
                ps.setDouble(12, montoAbonado);
                ps.setInt(13, DEFAULT_USER_ID);
                ps.setString(14, codComprobante);
                ps.setDouble(15, total);
                ps.executeUpdate();
                int comprobanteId;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next())
                        comprobanteId = rs.getInt(1);
                    else
                        throw new SQLException("No se obtuvo ID de comprobante");
                }
                // Insert details
                String detailSql = "INSERT INTO comprobantes_detalles (comprobante_id, servicio_id, peso_kg, costo_kilo) VALUES (?,?,?,?)";
                try (PreparedStatement psDet = conn.prepareStatement(detailSql)) {
                    for (ServiceDetail d : details) {
                        int servicioId = fetchId(conn, "SELECT id FROM servicios WHERE nom_servicio = ?",
                                d.nombreServicio);
                        if (servicioId == -1)
                            throw new SQLException("Servicio no encontrado: " + d.nombreServicio);
                        psDet.setInt(1, comprobanteId);
                        psDet.setInt(2, servicioId);
                        psDet.setDouble(3, d.pesoKg);
                        psDet.setDouble(4, d.costoKilo);
                        psDet.addBatch();
                    }
                    psDet.executeBatch();
                }
                // Insert ingreso
                String ingresoSql = "INSERT INTO reporte_ingresos (cod_comprobante, cliente_id, metodo_pago_id, fecha, monto_abonado, costo_total) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement psIng = conn.prepareStatement(ingresoSql)) {
                    psIng.setString(1, codComprobante);
                    psIng.setInt(2, clienteId);
                    if (metodoPagoId != null)
                        psIng.setInt(3, metodoPagoId);
                    else
                        psIng.setNull(3, java.sql.Types.INTEGER);
                    psIng.setTimestamp(4, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    psIng.setDouble(5, montoAbonado);
                    psIng.setDouble(6, total);
                    psIng.executeUpdate();
                }
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Comprobante registrado. Código: " + codComprobante, "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
            resetFormAfterSave();

            // After successful save, ask for phone (prefilled if exists) and attempt to
            // send WhatsApp.
            try {
                String phoneDigits = null;
                // Try to read client's phone from clientes row by inspecting likely columns
                try (PreparedStatement psPhone = conn.prepareStatement("SELECT * FROM clientes WHERE id = ?")) {
                    psPhone.setInt(1, clienteId);
                    try (ResultSet rsPhone = psPhone.executeQuery()) {
                        if (rsPhone.next()) {
                            java.sql.ResultSetMetaData md = rsPhone.getMetaData();
                            for (int ci = 1; ci <= md.getColumnCount(); ci++) {
                                String col = md.getColumnName(ci).toLowerCase();
                                if (col.contains("cel") || col.contains("tel") || col.contains("phone")
                                        || col.contains("movil")) {
                                    String v = rsPhone.getString(ci);
                                    if (v != null) {
                                        String digits = v.replaceAll("\\D", "");
                                        if (digits.length() == 9) {
                                            phoneDigits = digits;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Prompt user for number (prefilled with stored number if any)
                String prefill = phoneDigits != null ? phoneDigits : "";
                String input = (String) JOptionPane.showInputDialog(this, "Número de celular (9 dígitos):",
                        "Enviar WhatsApp", JOptionPane.PLAIN_MESSAGE, null, null, prefill);
                if (input == null) {
                    // user cancelled - do nothing
                } else {
                    String phone = input.trim().replaceAll("\\D", "");
                    if (phone.length() != 9) {
                        JOptionPane.showMessageDialog(this, "El número debe tener exactamente 9 dígitos (sin prefijo).",
                                "Validación", JOptionPane.WARNING_MESSAGE);
                    } else {
                        // Read API key
                        java.nio.file.Path apiPath = java.nio.file.Paths.get("textmebot_api.json");
                        if (!java.nio.file.Files.exists(apiPath)) {
                            // no API configured - skip
                        } else {
                            String apiJson = new String(java.nio.file.Files.readAllBytes(apiPath),
                                    java.nio.charset.StandardCharsets.UTF_8);
                            java.util.regex.Matcher m = java.util.regex.Pattern
                                    .compile("\\\"textmebot_api\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(apiJson);
                            String apikey = null;
                            if (m.find())
                                apikey = m.group(1);
                            if (apikey == null || apikey.isBlank()) {
                                // no apikey - skip
                            } else {
                                String recipient = "+51" + phone;
                                // Build detailed receipt-like plain-text message similar to
                                // DlgPrintPreview.htmlToPlainText
                                StringBuilder sbMsg = new StringBuilder();
                                sbMsg.append("LAVANDERIA SEPRIET\n");
                                sbMsg.append("Enrique Nerini 995, San Luis 15021\n");

                                // Tipo de comprobante label
                                String tipoLabel = "COMPROBANTE";
                                try (PreparedStatement psTipo = conn.prepareStatement(
                                        "SELECT tipo_comprobante, fecha, num_ruc, razon_social, costo_total FROM comprobantes WHERE cod_comprobante = ?")) {
                                    psTipo.setString(1, codComprobante);
                                    try (ResultSet rsTipo = psTipo.executeQuery()) {
                                        if (rsTipo.next()) {
                                            String tipo = rsTipo.getString("tipo_comprobante");
                                            if ("N".equalsIgnoreCase(tipo))
                                                tipoLabel = "NOTA DE VENTA ELECTRÓNICA";
                                            else if ("B".equalsIgnoreCase(tipo))
                                                tipoLabel = "BOLETA";
                                            else if ("F".equalsIgnoreCase(tipo))
                                                tipoLabel = "FACTURA";
                                            java.sql.Timestamp ts = rsTipo.getTimestamp("fecha");
                                            if (ts != null) {
                                                java.time.LocalDateTime ldt = ts.toLocalDateTime();
                                                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                                                        .ofPattern("dd/MM/yyyy - HH:mm");
                                                sbMsg.append(tipoLabel).append("\n");
                                                // cod comprobante
                                                sbMsg.append(codComprobante).append("\n");
                                                sbMsg.append("FECHA Y HORA: ").append(ldt.format(fmt)).append("\n");
                                            } else {
                                                sbMsg.append(tipoLabel).append("\n");
                                                sbMsg.append(codComprobante).append("\n");
                                            }
                                            // consume num_ruc / razon_social if needed (not required here)
                                            // append razon social if present later from cliente
                                            // leave numRuc/razon if needed
                                            // keep costo_total for totals
                                        }
                                    }
                                } catch (Exception ignore) {
                                }

                                // Cliente info
                                try (PreparedStatement psCli = conn.prepareStatement(
                                        "SELECT nombres, dni, direccion FROM clientes WHERE id = ?")) {
                                    psCli.setInt(1, clienteId);
                                    try (ResultSet rsCli = psCli.executeQuery()) {
                                        if (rsCli.next()) {
                                            String nombres = rsCli.getString("nombres");
                                            if (nombres != null && !nombres.isBlank())
                                                sbMsg.append("CLIENTE: ").append(nombres).append("\n");
                                            String dni = null;
                                            try {
                                                dni = rsCli.getString("dni");
                                            } catch (Exception ex) {
                                                /* ignore */ }
                                            if (dni == null || dni.isBlank()) {
                                                // try alternate column names
                                                try {
                                                    dni = rsCli.getString("num_documento");
                                                } catch (Exception ex) {
                                                }
                                            }
                                            if (dni != null && !dni.isBlank())
                                                sbMsg.append("DNI: ").append(dni).append("\n");
                                            String dir = null;
                                            try {
                                                dir = rsCli.getString("direccion");
                                            } catch (Exception ex) {
                                            }
                                            if (dir != null && !dir.isBlank())
                                                sbMsg.append(dir).append("\n");
                                        }
                                    }
                                } catch (Exception ignore) {
                                }

                                sbMsg.append("\n");
                                sbMsg.append("DETALLES (Servicio: Peso(Kilos) x Precio al Kilo):\n");

                                // Details rows
                                try (PreparedStatement psDet = conn.prepareStatement(
                                        "SELECT s.nom_servicio AS servicio, d.peso_kg, d.costo_kilo, (d.peso_kg * d.costo_kilo) AS total, d.comprobante_id "
                                                +
                                                "FROM comprobantes_detalles d JOIN servicios s ON d.servicio_id = s.id WHERE d.comprobante_id = (SELECT id FROM comprobantes WHERE cod_comprobante = ?)")) {
                                    psDet.setString(1, codComprobante);
                                    try (ResultSet rsDet = psDet.executeQuery()) {
                                        while (rsDet.next()) {
                                            String serv = rsDet.getString("servicio");
                                            double peso = rsDet.getDouble("peso_kg");
                                            double precioK = rsDet.getDouble("costo_kilo");
                                            double tot = rsDet.getDouble("total");
                                            sbMsg.append("- ").append(serv == null ? "" : serv).append(": ")
                                                    .append(String.format("%.2f", peso)).append(" kg x ")
                                                    .append(String.format("%.2f", precioK)).append(" = ")
                                                    .append(String.format("%.2f", tot)).append("\n");
                                        }
                                    }
                                } catch (Exception ignore) {
                                }

                                // Total
                                try (PreparedStatement psTot = conn.prepareStatement(
                                        "SELECT costo_total FROM comprobantes WHERE cod_comprobante = ?")) {
                                    psTot.setString(1, codComprobante);
                                    try (ResultSet rsTot = psTot.executeQuery()) {
                                        if (rsTot.next()) {
                                            double grand = rsTot.getDouble("costo_total");
                                            sbMsg.append("\nTOTAL: ").append(String.format("%.2f", grand)).append("\n");
                                        }
                                    }
                                } catch (Exception ignore) {
                                }

                                String text = sbMsg.toString().trim();
                                if (text.length() > 3000)
                                    text = text.substring(0, 3000);
                                String req = "https://api.textmebot.com/send.php?recipient="
                                        + java.net.URLEncoder.encode(recipient, "UTF-8")
                                        + "&apikey=" + java.net.URLEncoder.encode(apikey, "UTF-8")
                                        + "&text=" + java.net.URLEncoder.encode(text, "UTF-8")
                                        + "&json=yes";
                                java.net.URL u = java.net.URI.create(req).toURL();
                                java.net.HttpURLConnection conn2 = (java.net.HttpURLConnection) u.openConnection();
                                conn2.setRequestMethod("GET");
                                conn2.setConnectTimeout(10000);
                                conn2.setReadTimeout(15000);
                                int code2 = conn2.getResponseCode();
                                String resp;
                                try (java.io.InputStream is = code2 >= 400 ? conn2.getErrorStream()
                                        : conn2.getInputStream()) {
                                    resp = is == null ? ""
                                            : new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                                }

                                boolean ok = false;
                                String r = resp == null ? "" : resp.trim();
                                // If JSON, look for status: success
                                if (r.startsWith("{")) {
                                    java.util.regex.Matcher jm = java.util.regex.Pattern
                                            .compile("\"status\"\\s*:\\s*\"(success|ok)\"",
                                                    java.util.regex.Pattern.CASE_INSENSITIVE)
                                            .matcher(r);
                                    if (jm.find())
                                        ok = true;
                                } else {
                                    // HTML or plain: look for words Result and Success, or the pattern Result:
                                    // <b>Success!</b>
                                    if (r.toLowerCase().contains("\"status\"") || r.toLowerCase().contains("status")) {
                                        java.util.regex.Matcher jm2 = java.util.regex.Pattern
                                                .compile("\"status\"\\s*:\\s*\"(success|ok)\"",
                                                        java.util.regex.Pattern.CASE_INSENSITIVE)
                                                .matcher(r);
                                        if (jm2.find())
                                            ok = true;
                                    }
                                    if (!ok) {
                                        if (r.toLowerCase().contains("result") && r.toLowerCase().contains("success"))
                                            ok = true;
                                        java.util.regex.Matcher hm = java.util.regex.Pattern
                                                .compile("(?i)Result\\s*:\\s*.*?Success").matcher(r);
                                        if (hm.find())
                                            ok = true;
                                    }
                                }

                                if (ok) {
                                    JOptionPane.showMessageDialog(this, "WhatsApp enviado correctamente a " + recipient,
                                            "Enviado", JOptionPane.INFORMATION_MESSAGE);
                                    System.out.println("Auto-send WhatsApp OK to " + recipient + " for comprobante "
                                            + codComprobante + " resp=" + r);
                                } else {
                                    JOptionPane.showMessageDialog(this,
                                            "No se pudo enviar WhatsApp al número indicado.\nRespuesta API:\n" + r,
                                            "Aviso", JOptionPane.WARNING_MESSAGE);
                                }
                            }
                        }
                    }
                }
            } catch (Exception exSend) {
                JOptionPane.showMessageDialog(this,
                        "Error intentando enviar WhatsApp automático:\n" + exSend.getMessage(), "Aviso",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error guardando comprobante:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String safeStr(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private double parseMoneyLabel(String lbl) {
        if (lbl == null)
            return 0.0;
        // Expected formats: "S/. 123.45" or "S/.0.00"; remove currency part and
        // spaces/commas
        String cleaned = lbl.replace("S/.", "").replace("S/", "").trim();
        cleaned = cleaned.replace(",", "");
        if (cleaned.isEmpty())
            return 0.0;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int fetchId(Connection conn, String sql, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        }
        return -1;
    }

    private String generateComprobanteCode(Connection conn, char tipo) throws SQLException {
        // Manage counter inside transaction
        int newValue = 1;
        // Try select for update
        try (PreparedStatement ps = conn
                .prepareStatement("SELECT last_value FROM comprobante_counter WHERE tipo_comprobante = ? FOR UPDATE")) {
            ps.setString(1, String.valueOf(tipo));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    newValue = rs.getInt(1) + 1;
                    try (PreparedStatement upd = conn.prepareStatement(
                            "UPDATE comprobante_counter SET last_value = ? WHERE tipo_comprobante = ?")) {
                        upd.setInt(1, newValue);
                        upd.setString(2, String.valueOf(tipo));
                        upd.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO comprobante_counter (tipo_comprobante, last_value) VALUES (?,?)")) {
                        ins.setString(1, String.valueOf(tipo));
                        ins.setInt(2, newValue);
                        ins.executeUpdate();
                    }
                }
            }
        }
        return tipo + String.format("-%06d", newValue); // e.g. N-001234
    }

    private void resetFormAfterSave() {
        cbxCliente.setSelectedIndex(0);
        cbxEstadoComprobante.setSelectedIndex(0);
        cbxMetodoPago.setSelectedIndex(0);
        radioNotaVenta.setSelected(true);
        txtRUC.setText("");
        txtRazonSocial.setText("");
        txtMontoAbonado.setText("");
        txtObservaciones.setText("");
        dateTimePicker1.setDateTimePermissive(LocalDateTime.now());
        // Clear table
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                model.setValueAt("", i, c);
            }
        }
        updateTotals();
        toggleMontoAbonado();
        // Reload servicios so previously-removed items become selectable again
        try {
            loadServicios();
            if (cbxServicio.getItemCount() > 0)
                cbxServicio.setSelectedIndex(0);
            cbxServicio.setEnabled(true);
            // re-setup table editors/buttons in case model changed
            setupTableCellEditors();
            setupTableButtons();
        } catch (Exception ignore) {
        }
    }

    private static class ServiceDetail {
        String nombreServicio;
        double pesoKg;
        double costoKilo;

        ServiceDetail(String n, double p, double c) {
            nombreServicio = n;
            pesoKg = p;
            costoKilo = c;
        }
    }
}
