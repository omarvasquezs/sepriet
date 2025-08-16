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
import javax.swing.text.DocumentFilter.FilterBypass;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.UIManager;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
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
            return;
        }
        boolean enable = "ABONO".equals(selectedItem.toString().trim());
        txtMontoAbonado.setEnabled(enable);
    }

    private void loadClientes() {
        final String sql = "SELECT id, nombres FROM clientes ORDER BY nombres";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
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

    /**
     * Load enabled payment methods from MariaDB and populate cbxMetodoPago.
     */
    private void loadMetodosPago() {
        final String sql = "SELECT nom_metodo_pago FROM metodo_pago WHERE habilitado = 1 ORDER BY nom_metodo_pago";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

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
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

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
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "SERVICIO", "PESO EN KG", "PRECIO POR KG (S/.)", "TOTAL (S/.)", "ACCIONES"
            }
        ));
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
                        .addComponent(dateTimePicker1, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxMetodoPago, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(cbxEstadoComprobante, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(radioNotaVenta)
                                        .addGap(19, 19, 19)
                                        .addComponent(radioBoleta)
                                        .addGap(18, 18, 18)
                                        .addComponent(radioFactura))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(cbxCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(btnAgregarNuevoCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRUC, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRazonSocial))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(cbxServicio, javax.swing.GroupLayout.PREFERRED_SIZE, 410, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAgregarServicioComprobante, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jLabel15)
                            .addComponent(jScrollPane2))
                        .addGap(43, 43, 43)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnGenerarComprobante, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel11)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel10)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(txtMontoAbonado, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel9))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel14, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING))))))
                .addGap(15, 15, 15))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(lblTitulo)
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel2)
                    .addComponent(btnAgregarNuevoCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(dateTimePicker1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbxCliente, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(txtMontoAbonado, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                .addComponent(jLabel3)
                                .addComponent(cbxEstadoComprobante, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                .addComponent(radioNotaVenta)
                                .addComponent(radioBoleta)
                                .addComponent(radioFactura)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cbxMetodoPago, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(txtRUC, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRazonSocial, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(28, 28, 28)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnGenerarComprobante, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(cbxServicio, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnAgregarServicioComprobante, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(194, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 1070, 680));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgregarServicioComprobanteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarServicioComprobanteActionPerformed
        // Get the selected service
        Object selectedService = cbxServicio.getSelectedItem();
        // Check if a valid service is selected (not the placeholder)
        if (selectedService == null || selectedService.toString().startsWith("--")) {
            JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un servicio válido.",
                    "Servicio no seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
    // Extra guard: avoid adding a service that is already present in the table (in case it wasn't removed from combo por algún motivo)
    if (isServiceAlreadyInTable(selectedService.toString())) {
        JOptionPane.showMessageDialog(this,
            "El servicio ya fue añadido. Primero elimínelo de la tabla si desea volver a agregarlo.",
            "Servicio duplicado", JOptionPane.WARNING_MESSAGE);
        return;
    }
        // Get the price per kg for this service from database
        double precioPorKg = getPrecioServicio(selectedService.toString());
        // Add to table with empty PESO field and editable cells
        addServiceToTable(selectedService.toString(), "", String.format("%.2f", precioPorKg), "0.00", "X");
    // Remove the added service from the combo to prevent duplicates
    removeServiceFromCombo(selectedService.toString());
        // Reset the combo box to the placeholder
        cbxServicio.setSelectedIndex(0);
        // Set up cell editors for PESO and PRECIO columns if not already set
        setupTableCellEditors();
        // Set up delete button renderer/editor
        setupTableButtons();
    }
//GEN-LAST:event_btnAgregarServicioComprobanteActionPerformed

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
     * @param servicio The service name
     * @param peso The weight in kg (as String, initially empty)
     * @param precioPorKg The price per kg (as String)
     * @param total The total price (as String, initially "0.00")
     * @param acciones The value for the ACCIONES column ("X")
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
            model.addRow(new Object[]{servicio, peso, precioPorKg, total, acciones});
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
        // Custom editor for PESO column
        jTable1.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new javax.swing.JTextField()) {
            @Override
            public boolean stopCellEditing() {
                try {
                    String value = getCellEditorValue().toString();
                    if (!value.isEmpty()) {
                        Double.parseDouble(value);
                    }
                    return super.stopCellEditing();
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(jTable1,
                            "Por favor ingrese un valor numérico válido.",
                            "Error de formato", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        });
        // Custom editor for PRECIO column
        jTable1.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new javax.swing.JTextField()) {
            @Override
            public boolean stopCellEditing() {
                try {
                    String value = getCellEditorValue().toString();
                    if (!value.isEmpty()) {
                        Double.parseDouble(value);
                    }
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
     * Sets up the delete button renderer and editor for the table
     */
    private void setupTableButtons() {
        jTable1.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        jTable1.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    /**
     * Updates the total for a specific row based on weight and price
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

        // Calculate IGV (18%)
        double igv = subtotal * 0.18;
        double total = subtotal + igv;

        // Update the labels
        jLabel12.setText("S/. " + String.format("%.2f", subtotal));
        jLabel13.setText("S/. " + String.format("%.2f", igv));
        jLabel14.setText("S/. " + String.format("%.2f", total));
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
     * Adds a service back into the combo box (after deletion) if it's not already there.
     * Inserts after the placeholder and keeps alphabetical order relative to existing items (optional improvement).
     */
    private void addServiceBackToCombo(String serviceName) {
        ComboBoxModel<String> m = cbxServicio.getModel();
        if (!(m instanceof DefaultComboBoxModel)) return;
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
}
