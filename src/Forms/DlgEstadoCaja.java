package Forms;

import com.github.lgooddatepicker.components.DatePicker;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

public class DlgEstadoCaja extends JDialog {
    private final int usuarioId;
    private boolean isAdmin;
    private int cajaId = -1;
    private java.sql.Timestamp aperturaDate;

    private JLabel lblStatus;
    private JLabel lblMontoInicial;
    private JLabel lblVentas;
    private JLabel lblEgresos;
    private JLabel lblVentasYape;
    private JLabel lblEgresosYape;
    private JLabel lblTotalTeorico;

    private DatePicker dpFiltroDesde;
    private DatePicker dpFiltroHasta;

    private JTable tablaEgresos;
    private DefaultTableModel modEgresos;

    public DlgEstadoCaja(Frame parent, int usuarioId) {
        super(parent, "Estado de Caja y Egresos", true);
        this.usuarioId = usuarioId;
        checkAdminRole();

        setSize(750, 550);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel superior (Resumen)
        JPanel summaryPanel = new JPanel(new GridLayout(7, 1, 5, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Resumen Consolidado (Efectivo y YAPE/PLIN)"));
        lblStatus = new JLabel("Cargando...");
        lblMontoInicial = new JLabel();
        lblVentas = new JLabel();
        lblEgresos = new JLabel();
        lblVentasYape = new JLabel();
        lblEgresosYape = new JLabel();
        lblTotalTeorico = new JLabel();
        lblTotalTeorico.setFont(lblTotalTeorico.getFont().deriveFont(Font.BOLD, 14f));
        
        summaryPanel.add(lblStatus);
        summaryPanel.add(lblMontoInicial);
        summaryPanel.add(lblVentas);
        summaryPanel.add(lblEgresos);
        summaryPanel.add(lblVentasYape);
        summaryPanel.add(lblEgresosYape);
        summaryPanel.add(lblTotalTeorico);

        mainPanel.add(summaryPanel, BorderLayout.NORTH);

        // Panel central (Tabla y añadir egreso)
        JPanel egresoPanel = new JPanel(new BorderLayout(5, 5));
        egresoPanel.setBorder(BorderFactory.createTitledBorder("Detalle de Gastos / Egresos"));
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Desde:"));
        dpFiltroDesde = new DatePicker();
        dpFiltroDesde.setDateToToday();
        filterPanel.add(dpFiltroDesde);
        filterPanel.add(new JLabel("Hasta:"));
        dpFiltroHasta = new DatePicker();
        dpFiltroHasta.setDateToToday();
        filterPanel.add(dpFiltroHasta);
        JButton btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(_ -> cargarEstado());
        filterPanel.add(btnFiltrar);
        egresoPanel.add(filterPanel, BorderLayout.NORTH);

        modEgresos = new DefaultTableModel(new Object[]{"ID", "Fecha/Hora", "Descripción", "Monto (S/.)", "Método", "Imagen"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaEgresos = new JTable(modEgresos);
        tablaEgresos.removeColumn(tablaEgresos.getColumnModel().getColumn(0)); // Oculta el ID visualmente
        
        tablaEgresos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = tablaEgresos.rowAtPoint(e.getPoint());
                int col = tablaEgresos.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    int modelCol = tablaEgresos.convertColumnIndexToModel(col);
                    if (modelCol == 5) {
                        String val = (String) modEgresos.getValueAt(tablaEgresos.convertRowIndexToModel(row), 5);
                        if ("Ver Imagen".equals(val)) {
                            int id = (int) modEgresos.getValueAt(tablaEgresos.convertRowIndexToModel(row), 0);
                            verImagen(id);
                        }
                    }
                }
            }
        });
        
        egresoPanel.add(new JScrollPane(tablaEgresos), BorderLayout.CENTER);

        JPanel addEgresoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAddEgreso = new JButton("Registrar Nuevo Egreso");
        btnAddEgreso.addActionListener(_ -> registrarEgreso());
        addEgresoPanel.add(btnAddEgreso);

        if (isAdmin) {
            JButton btnEditEgreso = new JButton("Editar Seleccionado");
            btnEditEgreso.addActionListener(_ -> editarEgreso());
            addEgresoPanel.add(btnEditEgreso);
        }

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

    private void checkAdminRole() {
        isAdmin = false;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT role_id FROM users WHERE id = ?")) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("role_id") == 1) {
                    isAdmin = true;
                }
            }
        } catch (Exception ex) {
            System.err.println("Error checking role: " + ex.getMessage());
        }
    }

    private void cargarEstado() {
        lblMontoInicial.setText(""); lblVentas.setText(""); lblEgresos.setText(""); lblVentasYape.setText(""); lblEgresosYape.setText(""); lblTotalTeorico.setText("");
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
                } else {
                    lblStatus.setText("No hay caja abierta para el día de hoy.");
                }
            }
            
            java.time.LocalDate desde = dpFiltroDesde.getDate();
            java.time.LocalDate hasta = dpFiltroHasta.getDate();
            if (desde == null) desde = java.time.LocalDate.now();
            if (hasta == null) hasta = desde;

            java.sql.Date sqlDesde = java.sql.Date.valueOf(desde);
            java.sql.Date sqlHasta = java.sql.Date.valueOf(hasta);

            // Retrieve sales specifically in Cash for the period
            double totalVentasEfectivo = 0;
            String sqlVentas = "SELECT SUM(monto_abonado) as total FROM reporte_ingresos r " +
                               "JOIN metodo_pago m ON r.metodo_pago_id = m.id " +
                               "WHERE DATE(r.fecha) >= ? AND DATE(r.fecha) <= ? AND m.nom_metodo_pago LIKE '%EFECTIVO%'";
            try (PreparedStatement psV = conn.prepareStatement(sqlVentas)) {
                psV.setDate(1, sqlDesde);
                psV.setDate(2, sqlHasta);
                try (ResultSet rsV = psV.executeQuery()) {
                    if (rsV.next()) {
                        totalVentasEfectivo = rsV.getDouble("total");
                    }
                }
            }
            lblVentas.setText(String.format("1. Total Ingresos Efectivo (Filtrados): S/. %.2f", totalVentasEfectivo));

            // Retrieve sales specifically in YAPE / PLIN for the period
            double totalVentasYape = 0;
            String sqlVentasYape = "SELECT SUM(monto_abonado) as total FROM reporte_ingresos r " +
                               "JOIN metodo_pago m ON r.metodo_pago_id = m.id " +
                               "WHERE DATE(r.fecha) >= ? AND DATE(r.fecha) <= ? AND m.nom_metodo_pago LIKE '%YAPE%'";
            try (PreparedStatement psV = conn.prepareStatement(sqlVentasYape)) {
                psV.setDate(1, sqlDesde);
                psV.setDate(2, sqlHasta);
                try (ResultSet rsV = psV.executeQuery()) {
                    if (rsV.next()) {
                        totalVentasYape = rsV.getDouble("total");
                    }
                }
            }
            lblVentasYape.setText(String.format("2. Total Ingresos YAPE/PLIN (Filtrados): S/. %.2f", totalVentasYape));

            // Retrieve egresos
            double totalEgresosEfectivo = 0;
            double totalEgresosYape = 0;
            String sqlEgresos = "SELECT c.id, c.fecha, c.descripcion, c.monto, c.imagen_path, m.nom_metodo_pago, c.id_metodo_pago " +
                                "FROM caja_egresos c " +
                                "LEFT JOIN metodo_pago m ON c.id_metodo_pago = m.id " +
                                "WHERE DATE(c.fecha) >= ? AND DATE(c.fecha) <= ? ORDER BY c.fecha";
            SimpleDateFormat timeFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            try (PreparedStatement psE = conn.prepareStatement(sqlEgresos)) {
                psE.setDate(1, sqlDesde);
                psE.setDate(2, sqlHasta);
                try (ResultSet rsE = psE.executeQuery()) {
                    while (rsE.next()) {
                        int egId = rsE.getInt("id");
                        String hora = timeFmt.format(rsE.getTimestamp("fecha"));
                        String desc = rsE.getString("descripcion");
                        double val = rsE.getDouble("monto");
                        String imgPath = rsE.getString("imagen_path");
                        String metodo = rsE.getString("nom_metodo_pago");
                        int idMetodo = rsE.getInt("id_metodo_pago");
                        
                        if (idMetodo == 1) { // YAPE / PLIN
                            totalEgresosYape += val;
                        } else {
                            totalEgresosEfectivo += val;
                        }
                        
                        modEgresos.addRow(new Object[]{egId, hora, desc, String.format("%.2f", val), metodo != null ? metodo : "N/D", (imgPath != null && !imgPath.isBlank()) ? "Ver Imagen" : "---"});
                    }
                }
            }
            lblEgresos.setText(String.format("3. Total Gastos / Egresos en Efectivo (Filtrados): S/. %.2f", totalEgresosEfectivo));
            lblEgresosYape.setText(String.format("4. Total Gastos / Egresos YAPE / PLIN (Filtrados): S/. %.2f", totalEgresosYape));

            if (desde.equals(java.time.LocalDate.now()) && hasta.equals(java.time.LocalDate.now()) && cajaId != -1) {
                lblMontoInicial.setText(String.format("Monto Inicial (Apertura de Hoy): S/. %.2f", montoInicial));
                double totalTeorico = montoInicial + totalVentasEfectivo - totalEgresosEfectivo;
                lblTotalTeorico.setText(String.format("Monto Teórico Actual en Caja (Solo Efectivo): S/. %.2f", totalTeorico));
            } else {
                lblMontoInicial.setText("Monto Inicial: N/D (Filtro por fechas)");
                lblTotalTeorico.setText("Consolidado: Diferencia de días");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando estado: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarEgreso() {
        if (cajaId == -1 && !isAdmin) {
            JOptionPane.showMessageDialog(this, "No puede registrar un gasto sin una caja abierta.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel p = new JPanel(new GridLayout(isAdmin ? 5 : 4, 2, 10, 5));
        
        DatePicker dpFechaEgreso = null;
        if (isAdmin) {
            p.add(new JLabel("Fecha del Gasto:"));
            dpFechaEgreso = new DatePicker();
            dpFechaEgreso.setDateToToday();
            p.add(dpFechaEgreso);
        }

        p.add(new JLabel("Descripción del Gasto:"));
        JTextField txtDesc = new JTextField(15);
        p.add(txtDesc);
        
        p.add(new JLabel("Monto a retirar (S/.):"));
        JTextField txtMonto = new JTextField(10);
        p.add(txtMonto);

        p.add(new JLabel("Método de Pago:"));
        JComboBox<String> cmbMetodo = new JComboBox<>(new String[]{"EFECTIVO", "YAPE / PLIN"});
        p.add(cmbMetodo);

        JButton btnImage = new JButton("Adjuntar Imagen...");
        JLabel lblImage = new JLabel("Ninguna");
        p.add(btnImage);
        p.add(lblImage);

        final java.io.File[] selectedFile = {null};
        btnImage.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png"));
            if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = jfc.getSelectedFile();
                lblImage.setText(selectedFile[0].getName());
            }
        });

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
            
            int idMetodoPago = cmbMetodo.getSelectedIndex() == 0 ? 4 : 1; // 4 = Efectivo, 1 = Yape/Plin

            java.sql.Timestamp fechaGuardar = new java.sql.Timestamp(System.currentTimeMillis());
            if (isAdmin && dpFechaEgreso != null && dpFechaEgreso.getDate() != null) {
                if (!dpFechaEgreso.getDate().equals(java.time.LocalDate.now())) {
                    fechaGuardar = java.sql.Timestamp.valueOf(dpFechaEgreso.getDate().atStartOfDay());
                }
            }

            int cajaIdTemp = cajaId;
            if (cajaId == -1 && isAdmin) cajaIdTemp = 1; // Fallback or logic needed if saving past without box. We assume admin can bypass.

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO caja_egresos (id_caja, fecha, descripcion, monto, id_metodo_pago, id_usuario, imagen_path) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, cajaIdTemp);
                ps.setTimestamp(2, fechaGuardar);
                ps.setString(3, desc);
                ps.setDouble(4, monto);
                ps.setInt(5, idMetodoPago);
                ps.setInt(6, usuarioId);

                String relativePath = null;
                if (selectedFile[0] != null) {
                    try {
                        String rootFolderName = "imagenes";
                        java.io.File rootDir = new java.io.File(rootFolderName);
                        if (!rootDir.exists()) rootDir.mkdirs();

                        String yearMonth = new SimpleDateFormat("yyyyMM").format(new java.util.Date());
                        java.io.File monthDir = new java.io.File(rootDir, yearMonth);
                        if (!monthDir.exists()) monthDir.mkdirs();

                        // Generar un nombre unico seguro para evitar reemplazos de archivos con el mismo nombre
                        String originalName = selectedFile[0].getName();
                        String extension = originalName.substring(originalName.lastIndexOf('.'));
                        String safeName = System.currentTimeMillis() + extension;

                        java.io.File destinationFile = new java.io.File(monthDir, safeName);
                        // Copiar fisicamente el archivo
                        java.nio.file.Files.copy(selectedFile[0].toPath(), destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                        // La ruta relativa que se guarda en BD
                        relativePath = rootFolderName + "/" + yearMonth + "/" + safeName;
                    } catch (Exception copyEx) {
                        JOptionPane.showMessageDialog(this, "No se pudo copiar la imagen al directorio, el registro continuará sin imagen.\n" + copyEx.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
                    }
                }

                if (relativePath != null) {
                    ps.setString(7, relativePath);
                } else {
                    ps.setNull(7, java.sql.Types.VARCHAR);
                }

                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Egreso registrado correctamente.");
                cargarEstado(); // Refresh info
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar el egreso: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editarEgreso() {
        int row = tablaEgresos.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione un egreso para editar.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) modEgresos.getValueAt(tablaEgresos.convertRowIndexToModel(row), 0);

        JPanel p = new JPanel(new GridLayout(5, 2, 10, 5));
        
        p.add(new JLabel("Fecha del Gasto:"));
        DatePicker dpFechaEgreso = new DatePicker();
        p.add(dpFechaEgreso);

        p.add(new JLabel("Descripción del Gasto:"));
        JTextField txtDesc = new JTextField(15);
        p.add(txtDesc);
        
        p.add(new JLabel("Monto a retirar (S/.):"));
        JTextField txtMonto = new JTextField(10);
        p.add(txtMonto);

        p.add(new JLabel("Método de Pago:"));
        JComboBox<String> cmbMetodo = new JComboBox<>(new String[]{"EFECTIVO", "YAPE / PLIN"});
        p.add(cmbMetodo);

        JButton btnImage = new JButton("Cambiar Imagen...");
        JLabel lblImage = new JLabel("Misma Imagen");
        p.add(btnImage);
        p.add(lblImage);

        final java.io.File[] selectedFile = {null};
        btnImage.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imágenes", "jpg", "jpeg", "png"));
            if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile[0] = jfc.getSelectedFile();
                lblImage.setText(selectedFile[0].getName());
            }
        });

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT fecha, descripcion, monto, id_metodo_pago, imagen_path FROM caja_egresos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dpFechaEgreso.setDate(rs.getDate("fecha").toLocalDate());
                    txtDesc.setText(rs.getString("descripcion"));
                    txtMonto.setText(String.valueOf(rs.getDouble("monto")));
                    cmbMetodo.setSelectedIndex(rs.getInt("id_metodo_pago") == 4 ? 0 : 1);
                    String curImg = rs.getString("imagen_path");
                    if (curImg == null || curImg.isBlank()) {
                        lblImage.setText("Sin imagen. Adjuntar...");
                    } else {
                        lblImage.setText("Ya tiene imagen.");
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int res = JOptionPane.showConfirmDialog(this, p, "Editar Egreso / Gasto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String desc = txtDesc.getText().trim();
            if (desc.isEmpty()) return;
            double monto = 0;
            try {
                monto = Double.parseDouble(txtMonto.getText().trim());
            } catch (Exception ex) { return; }
            int idMetodoPago = cmbMetodo.getSelectedIndex() == 0 ? 4 : 1;
            
            java.sql.Timestamp fechaGuardar = java.sql.Timestamp.valueOf(dpFechaEgreso.getDate().atStartOfDay());

            String extraSql = "";
            String relativePath = null;
            if (selectedFile[0] != null) {
                try {
                    String rootFolderName = "imagenes";
                    java.io.File rootDir = new java.io.File(rootFolderName);
                    if (!rootDir.exists()) rootDir.mkdirs();

                    String yearMonth = new java.text.SimpleDateFormat("yyyyMM").format(new java.util.Date());
                    java.io.File monthDir = new java.io.File(rootDir, yearMonth);
                    if (!monthDir.exists()) monthDir.mkdirs();

                    String originalName = selectedFile[0].getName();
                    String extension = originalName.substring(originalName.lastIndexOf('.'));
                    String safeName = System.currentTimeMillis() + extension;

                    java.io.File destinationFile = new java.io.File(monthDir, safeName);
                    java.nio.file.Files.copy(selectedFile[0].toPath(), destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    relativePath = rootFolderName + "/" + yearMonth + "/" + safeName;
                    extraSql = ", imagen_path = ?";
                } catch (Exception copyEx) {
                    JOptionPane.showMessageDialog(this, "No se pudo copiar la nueva imagen, conservará la anterior.\n" + copyEx.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
                }
            }

            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE caja_egresos SET fecha = ?, descripcion = ?, monto = ?, id_metodo_pago = ?" + extraSql + " WHERE id = ?")) {
                ps.setTimestamp(1, fechaGuardar);
                ps.setString(2, desc);
                ps.setDouble(3, monto);
                ps.setInt(4, idMetodoPago);
                if (relativePath != null) {
                    ps.setString(5, relativePath);
                    ps.setInt(6, id);
                } else {
                    ps.setInt(5, id);
                }
                
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Egreso actualizado correctamente.");
                cargarEstado();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar el egreso: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void verImagen(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT imagen_path FROM caja_egresos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String path = rs.getString("imagen_path");
                    if (path != null && !path.isBlank()) {
                        java.io.File imgFile = new java.io.File(path);
                        if (!imgFile.exists()) {
                            JOptionPane.showMessageDialog(this, "El archivo local no existe o fue movido: " + path, "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
                        if (icon.getIconWidth() > 800 || icon.getIconHeight() > 600) {
                            Image img = icon.getImage();
                            int newWidth = 800;
                            int newHeight = (newWidth * icon.getIconHeight()) / icon.getIconWidth();
                            if (newHeight > 600) {
                                newHeight = 600;
                                newWidth = (newHeight * icon.getIconWidth()) / icon.getIconHeight();
                            }
                            icon = new ImageIcon(img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH));
                        }
                        JLabel lbl = new JLabel(icon);
                        JOptionPane.showMessageDialog(this, new JScrollPane(lbl), "Imagen del Egreso", JOptionPane.PLAIN_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Este egreso no contiene ninguna imagen registrada.", "Error", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar la imagen: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
