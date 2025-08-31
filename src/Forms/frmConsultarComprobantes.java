package Forms;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import com.toedter.calendar.JDateChooser;

/** Internal frame to list comprobantes (read only) with search & pagination. */
public class frmConsultarComprobantes extends JInternalFrame {
    // Filters replacing the previous global search
    private final JTextField filterCod = new JTextField();
    private final JComboBox<String> filterCliente = new JComboBox<>();
    private final List<String> allClientes = new ArrayList<>();
    // estados will be shown as visible checkboxes
    private final JPanel estadoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    // sub-panels for separation (will use vertical BoxLayout so each checkbox
    // appears on its own line)
    private final JPanel estadoRopaPanel = new JPanel();
    private final JPanel estadoComprobantePanel = new JPanel();
    // label for selected count removed per request
    // Separate lists for possible two kinds of estados
    private final List<JCheckBox> estadoRopaItems = new ArrayList<>();
    private final List<JCheckBox> estadoComprobanteItems = new ArrayList<>();
    private boolean hasEstadoRopa = false;
    private boolean hasEstadoComprobante = false;
    private final JDateChooser filterFecha = new JDateChooser();
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
    private final List<Integer> estadoIds = new ArrayList<>();
    private final JButton btnAdd = new JButton("Añadir");
    private final JButton btnEdit = new JButton("Editar");
    private final JButton btnDelete = new JButton("Eliminar");
    private final JButton btnPrev = new JButton("<");
    private final JButton btnNext = new JButton(">");
    private final JLabel lblPagina = new JLabel("Página 1 de 1");
    private final JTable table = new JTable();
    private final ComprobantesTableModel model = new ComprobantesTableModel();
    private int currentPage = 1;
    private int totalPages = 1;
    // page size is configurable by the user; default to 50
    private int pageSize = 50;
    private final JComboBox<Integer> pageSizeCombo = new JComboBox<>(new Integer[] { 10, 25, 50, 100, 200 });
    private Mode mode;

    public enum Mode {
        TODOS, RECIBIDOS, CANCELADOS, DEFAULT
    }

    public frmConsultarComprobantes(Mode mode) {
        super("Comprobantes", true, true, true, true);
        this.mode = mode;
        // Set internal frame icon
        java.net.URL iconUrl = getClass().getResource("/Forms/icon.png");
        if (iconUrl != null) {
            setFrameIcon(new ImageIcon(iconUrl));
        }
        buildUI();
        setSize(1000, 520);
        loadPage(1);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Maximize after added to desktop
        SwingUtilities.invokeLater(() -> {
            try {
                setMaximum(true);
            } catch (Exception ignored) {
            }
        });
    }

    private void buildUI() {
        // Top area: a header line for "Filtros" and a filter row below it.
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        // Add left padding for the filters area so controls don't stick to the border
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
        JLabel lblFiltros = new JLabel("Filtros");
        lblFiltros.setFont(lblFiltros.getFont().deriveFont(Font.BOLD, 16f));
        lblFiltros.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(lblFiltros);

        JPanel filtersRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterCod.setPreferredSize(new Dimension(150, 24));
        filterCliente.setPreferredSize(new Dimension(180, 24));
        filterCliente.setEditable(true);
        filterFecha.setPreferredSize(new Dimension(130, 24));
        filtersRow.add(new JLabel("Código:"));
        filtersRow.add(filterCod);
        filtersRow.add(new JLabel("Cliente:"));
        filtersRow.add(filterCliente);
        // "Estados:" label removed per request; estadoPanel will be placed on its own
        // line
        filtersRow.add(new JLabel("Fecha:"));
        filtersRow.add(filterFecha);
        filtersRow.add(btnBuscar);
        filtersRow.add(btnReset);
        filtersRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Place the estado groups side-by-side: the container is horizontal while
        // each group keeps a vertical layout so their checkboxes remain one-per-line.
        estadoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 4));
        estadoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(filtersRow);
        top.add(estadoPanel);
        btnBuscar.addActionListener(this::onBuscar);
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetFilters();
                loadPage(1);
            }
        });

        // show pointer cursor for buttons
        Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        btnBuscar.setCursor(hand);
        btnReset.setCursor(hand);

        // CRUD toolbar
        JPanel crudBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        crudBar.add(btnAdd);
        crudBar.add(btnEdit);
        crudBar.add(btnDelete);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openAddComprobante();
            }
        });
    // Print preview button
    JButton btnPrintPreview = new JButton("Vista Impresión");
    crudBar.add(btnPrintPreview);
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openEditDialog();
            }
        });
    btnPrintPreview.setCursor(hand);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deleteSelected();
            }
        });

        // Hand cursor for CRUD/pagination buttons (reuse hand variable)
        btnAdd.setCursor(hand);
        btnEdit.setCursor(hand);
        btnDelete.setCursor(hand);
        btnPrev.setCursor(hand);
        btnNext.setCursor(hand);
        btnPrintPreview.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openPrintPreview();
            }
        });

        table.setModel(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        // allow only one row to be selected at a time
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        // Populate cliente names and estados for filters
        SwingUtilities.invokeLater(() -> {
            populateClienteNames();
            populateEstadoItems();
            try {
                org.jdesktop.swingx.autocomplete.AutoCompleteDecorator.decorate(filterCliente);
            } catch (Exception ignore) {
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // page size selector (records per page)
        pageSizeCombo.setSelectedItem(Integer.valueOf(pageSize));
        pageSizeCombo.setMaximumRowCount(6);
        pageSizeCombo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pageSizeCombo.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Integer sel = (Integer) pageSizeCombo.getSelectedItem();
                if (sel != null && sel != pageSize) {
                    pageSize = sel;
                    loadPage(1);
                }
            }
        });
        btnPrev.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (currentPage > 1)
                    loadPage(currentPage - 1);
            }
        });
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (currentPage < totalPages)
                    loadPage(currentPage + 1);
            }
        });
        bottom.add(new JLabel("Registros por página:"));
        bottom.add(pageSizeCombo);
        bottom.add(btnPrev);
        bottom.add(btnNext);
        bottom.add(lblPagina);

        getContentPane().setLayout(new BorderLayout());
        JPanel north = new JPanel(new BorderLayout());
        north.add(top, BorderLayout.NORTH);
        north.add(crudBar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent ev) {
                boolean sel = table.getSelectedRow() >= 0;
                btnEdit.setEnabled(sel);
                btnDelete.setEnabled(sel);
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = table.convertRowIndexToModel(row);
                        ComprobanteRow cr = model.rows.get(modelRow);
                        JOptionPane.showMessageDialog(frmConsultarComprobantes.this,
                                "Comprobante: " + cr.codComprobante + "\nCliente: " + cr.cliente +
                                        "\nCosto Total: " + cr.costoTotal + "\nDeuda: " + cr.deuda +
                                        "\nFecha: " + cr.fecha,
                                "Detalle", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
    }

    private void onBuscar(ActionEvent e) {
        loadPage(1);
    }

    private void openPrintPreview() {
        Integer id = getSelectedId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un comprobante para vista previa.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    // Build HTML from DB (header + details) following the app's 58mm template
    // Compute pixel width that corresponds to 164pt (58mm) on this screen: px = points * DPI / 72
    int pxWidth = Math.round(164f * Toolkit.getDefaultToolkit().getScreenResolution() / 72f);
    // no logo for 58mm preview
    StringBuilder sb = new StringBuilder();
    sb.append("<html><head><meta charset=\"utf-8\"><style>");
    sb.append("body{font-family:'Courier New',Consolas,monospace;color:#111;font-size:12px;margin:0;padding:0;} ");
    sb.append(".container{width:100%;margin:6px 0;padding:0;} ");
    sb.append(".receipt{display:block;width:300px;max-width:300px;margin-left:auto;margin-right:auto;padding:16px 8px 12px 8px;background:#fff;font-family:monospace;} ");
    sb.append("img.logo{display:block;margin:0 auto 6px auto;max-width:120px;height:auto;} ");
    sb.append(".company{font-size:16px;font-weight:700;letter-spacing:1px;text-align:center;margin-bottom:4px;} ");
    sb.append(".company-sub{font-size:11px;text-align:center;margin-bottom:6px;} ");
    sb.append(".tipo{font-weight:700;text-align:center;margin:6px 0 4px 0;} .cod{font-weight:700;text-align:center;margin-bottom:8px;} ");
    sb.append("table{width:100%;border-collapse:collapse;font-size:11px;} th,td{padding:4px 6px;} ");
    sb.append("th.service{width:52%;text-align:left;} th.num{width:16%;text-align:right;} td.service{white-space:normal;} td.num{text-align:right;} ");
    sb.append(".totals{width:100%;margin-top:8px;font-size:11px;} .totals td{padding:2px 6px;} .totals .label{text-align:right;padding-right:10px;} .totals .val{text-align:right;font-weight:700;} ");
    sb.append(".footer{margin-top:10px;text-align:center;font-weight:700;} ");
    sb.append("</style></head><body>");

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Header
            String hdrSql = "SELECT c.tipo_comprobante,c.cod_comprobante,c.fecha,c.num_ruc,c.razon_social,c.costo_total, "
                    + "cl.nombres,cl.dni,cl.direccion,ec.nom_estado as estado_comprobante "
                    + "FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id=cl.id LEFT JOIN estado_comprobantes ec ON c.estado_comprobante_id=ec.id WHERE c.id=?";
            try (PreparedStatement ph = conn.prepareStatement(hdrSql)) {
                ph.setInt(1, id);
                try (ResultSet rh = ph.executeQuery()) {
                    if (!rh.next()) {
                        JOptionPane.showMessageDialog(this, "Comprobante no encontrado.", "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    String tipo = rh.getString("tipo_comprobante");
                    String cod = rh.getString("cod_comprobante");
                    Timestamp ts = rh.getTimestamp("fecha");
                    String numRuc = rh.getString("num_ruc");
                    String razon = rh.getString("razon_social");
                    String cliente = rh.getString("nombres");
                    String dni = rh.getString("dni");
                    String direccion = rh.getString("direccion");
                    String estado = rh.getString("estado_comprobante");

                    sb.append("<div class=\"container\"><div class=\"receipt\">");
                    sb.append("<div class=\"company\">VJS LAUNDRY S.A.C.</div>");
                    sb.append("<div class=\"company-sub\">Av. Agustín de la Rosa Toro 318, San Luis 15021<br>R.U.C. N° 20602340466</div>");
                    // Tipo label and code
                    String tipoLabel = switch (tipo == null ? "" : tipo) {
                        case "B" -> "BOLETA DE VENTA ELECTRÓNICA";
                        case "F" -> "FACTURA DE VENTA ELECTRÓNICA";
                        case "N" -> "NOTA DE VENTA ELECTRÓNICA";
                        default -> "";
                    };
                    if (!tipoLabel.isEmpty()) {
                        sb.append("<div style=\"text-align:center;margin-top:6px;font-weight:700;\">" + tipoLabel + "</div>");
                    }
                    sb.append("<div style=\"text-align:center;font-weight:700;margin-bottom:6px;\">" + escapeHtml(cod) + "</div>");
                    // format fecha consistently with other screens
                    String fechaFmt = "";
                    try {
                        if (ts != null) fechaFmt = ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"));
                    } catch (Exception ignore) {
                        fechaFmt = ts != null ? ts.toString() : "";
                    }
                    sb.append("<div style=\"font-size:11px;\">FECHA Y HORA: " + escapeHtml(fechaFmt) + "</div>");
                    if ("F".equals(tipo)) {
                        sb.append("<div style=\"font-size:12px;\">N° DE RUC: " + escapeHtml(numRuc == null ? "" : numRuc) + "</div>");
                        sb.append("<div style=\"font-size:12px;\">RAZON SOCIAL: " + escapeHtml(razon == null ? "" : razon) + "</div>");
                    }
                    sb.append("<div style=\"font-size:12px;\">CLIENTE: " + escapeHtml(cliente == null ? "" : cliente) + "</div>");
                    sb.append("<div style=\"font-size:12px;\">DNI: " + escapeHtml(dni == null ? "" : dni) + "</div>");
                    sb.append("<div style=\"font-size:12px;\">DIRECCIÓN: " + escapeHtml(direccion == null ? "" : direccion) + "</div>");
                    sb.append("<div style=\"font-size:12px;\">ESTADO: " + escapeHtml(estado == null ? "" : estado) + "</div>");
                    sb.append("<div style=\"text-align:center;margin-top:8px;font-weight:700;\">DETALLES</div>");

                    // Details
                    String detSql = "SELECT d.peso_kg,d.costo_kilo,s.nom_servicio FROM comprobantes_detalles d LEFT JOIN servicios s ON d.servicio_id=s.id WHERE d.comprobante_id=?";
                    try (PreparedStatement pd = conn.prepareStatement(detSql)) {
                        pd.setInt(1, id);
                        try (ResultSet rd = pd.executeQuery()) {
                            sb.append("<table>");
                            sb.append("<thead><tr><th class=\"service\">SERVICIO</th><th class=\"num\">PESO</th><th class=\"num\">C/ KG</th><th class=\"num\">TOTAL</th></tr></thead>");
                            sb.append("<tbody>");
                            double calcTotal = 0.0;
                            while (rd.next()) {
                                String nomServ = rd.getString("nom_servicio");
                                double peso = rd.getDouble("peso_kg");
                                double costoK = rd.getDouble("costo_kilo");
                                double rowTotal = peso * costoK;
                                calcTotal += rowTotal;
                                sb.append("<tr>");
                                sb.append("<td class=\"service\">" + escapeHtml(nomServ == null ? "" : nomServ) + "</td>");
                                sb.append("<td class=\"num\">" + formatNumber(peso) + "</td>");
                                sb.append("<td class=\"num\">" + formatNumber(costoK) + "</td>");
                                sb.append("<td class=\"num\">" + formatNumber(rowTotal) + "</td>");
                                sb.append("</tr>");
                            }
                            sb.append("</tbody></table>");

                            // Totals block similar to PHP template
                            double igv = calcTotal * 0.18;
                            double subtotal = calcTotal - igv;
                            sb.append("<table class=\"totals\">\n<tbody>\n");
                            sb.append("<tr><td class=\"label\">SUBTOTAL</td><td class=\"val\">S/. " + formatNumber(subtotal) + "</td></tr>");
                            sb.append("<tr><td class=\"label\">IGV 18%</td><td class=\"val\">S/. " + formatNumber(igv) + "</td></tr>");
                            sb.append("<tr><td class=\"label\" style=\"font-weight:700;\">TOTAL</td><td class=\"val\" style=\"font-weight:700;\">S/. " + formatNumber(calcTotal) + "</td></tr>");
                            sb.append("</tbody></table>\n");

                            sb.append("<div class=\"footer\">¡Gracias por su preferencia!</div>");
                        }
                    }
                    sb.append("</div></div>"); // receipt + container
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generando vista previa:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sb.append("</body></html>");

    DlgPrintPreview dlg = new DlgPrintPreview(SwingUtilities.getWindowAncestor(this), pxWidth);
    dlg.showForHtml(sb.toString());
    }

    private Integer getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        int modelRow = table.convertRowIndexToModel(row);
        return model.rows.get(modelRow).id;
    }

    private void openAddComprobante() {
        // Attempt to reuse existing registrar frame if open
        JDesktopPane dp = getDesktopPane();
        if (dp != null) {
            for (JInternalFrame f : dp.getAllFrames()) {
                if (f instanceof frmRegistrarComprobante) {
                    try {
                        f.setIcon(false);
                        f.setSelected(true);
                    } catch (Exception ignored) {
                    }
                    f.toFront();
                    return;
                }
            }
            frmRegistrarComprobante reg = new frmRegistrarComprobante();
            dp.add(reg);
            reg.setVisible(true);
        }
    }

    private void openEditDialog() {
        Integer id = getSelectedId();
        if (id == null)
            return;
        DlgEditarComprobante dlg = new DlgEditarComprobante(SwingUtilities.getWindowAncestor(this), id, this::onEdited);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // Populate cliente names into filterCliente combobox
    private void populateClienteNames() {
        filterCliente.removeAllItems();
        allClientes.clear();
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT nombres FROM clientes ORDER BY nombres");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nombre = rs.getString(1);
                allClientes.add(nombre);
            }
        } catch (Exception ex) {
            // non-fatal
        }
        // initial model: include empty selection
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("");
        for (String c : allClientes)
            model.addElement(c);
        filterCliente.setModel(model);
        filterCliente.setSelectedIndex(0);
        // Allow larger visible row count so long lists are scrollable
        filterCliente.setMaximumRowCount(20);
        // Prefer SwingX AutoCompleteDecorator for stable behavior (same as
        // frmRegistrarComprobante)
        try {
            org.jdesktop.swingx.autocomplete.AutoCompleteDecorator.decorate(filterCliente);
        } catch (Exception ignore) {
        }
    }

    // Build estado panel with JCheckBox items (all checked by default).
    // Load both estado_ropa and estado_comprobantes if present.
    private void populateEstadoItems() {
        estadoPanel.removeAll();
        estadoRopaPanel.removeAll();
        estadoComprobantePanel.removeAll();
        estadoRopaItems.clear();
        estadoComprobanteItems.clear();
        estadoIds.clear();
        hasEstadoRopa = false;
        hasEstadoComprobante = false;
        // set vertical layout for subpanels so items appear one per line
        estadoRopaPanel.setLayout(new BoxLayout(estadoRopaPanel, BoxLayout.Y_AXIS));
        estadoComprobantePanel.setLayout(new BoxLayout(estadoComprobantePanel, BoxLayout.Y_AXIS));
        try (Connection conn = DatabaseConfig.getConnection()) {
            // load estado_ropa
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT id,nom_estado_ropa FROM estado_ropa ORDER BY nom_estado_ropa");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String nom = rs.getString(2);
                    JCheckBox it = new JCheckBox(nom, true);
                    it.setActionCommand(String.valueOf(id));
                    it.setAlignmentX(Component.LEFT_ALIGNMENT);
                    estadoRopaItems.add(it);
                    estadoIds.add(id);
                    estadoRopaPanel.add(it);
                    it.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent ae) {
                            updateEstadoButtonLabel();
                        }
                    });
                }
                if (!estadoRopaItems.isEmpty()) {
                    hasEstadoRopa = true;
                }
            } catch (SQLException ignored) {
            }
            // load estado_comprobantes
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT id,nom_estado FROM estado_comprobantes WHERE habilitado=1 ORDER BY nom_estado");
                    ResultSet rs2 = ps2.executeQuery()) {
                while (rs2.next()) {
                    int id = rs2.getInt(1);
                    String nom = rs2.getString(2);
                    JCheckBox it = new JCheckBox(nom, true);
                    it.setActionCommand(String.valueOf(id));
                    it.setAlignmentX(Component.LEFT_ALIGNMENT);
                    estadoComprobanteItems.add(it);
                    estadoIds.add(id);
                    estadoComprobantePanel.add(it);
                    it.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent ae) {
                            updateEstadoButtonLabel();
                        }
                    });
                }
                if (!estadoComprobanteItems.isEmpty()) {
                    hasEstadoComprobante = true;
                }
            } catch (SQLException ignored) {
            }
        } catch (Exception ex) {
            // ignore
        }
        if (!hasEstadoRopa && !hasEstadoComprobante) {
            JCheckBox it = new JCheckBox("Todos", true);
            it.setActionCommand("0");
            it.setAlignmentX(Component.LEFT_ALIGNMENT);
            estadoRopaItems.add(it);
            estadoRopaPanel.add(it);
        }
        // add labeled groups to the container panel
        if (hasEstadoRopa || !estadoRopaItems.isEmpty()) {
            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
            JLabel l = new JLabel("Estado Ropa:");
            l.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(l);
            estadoRopaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(estadoRopaPanel);
            estadoPanel.add(p);
        }
        if (hasEstadoComprobante || !estadoComprobanteItems.isEmpty()) {
            JPanel p2 = new JPanel();
            p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
            p2.setAlignmentX(Component.LEFT_ALIGNMENT);
            p2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
            JLabel l2 = new JLabel("Estado Comprobante:");
            l2.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
            l2.setAlignmentX(Component.LEFT_ALIGNMENT);
            p2.add(l2);
            estadoComprobantePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            p2.add(estadoComprobantePanel);
            estadoPanel.add(p2);
        }
        // small label showing how many selected (no visual count now)
        updateEstadoButtonLabel();
        estadoPanel.revalidate();
        estadoPanel.repaint();
        // hook for subclasses to modify the estado checkbox lists/visibility
        try {
            postPopulateEstadoItems();
        } catch (Exception ignore) {
        }
    }

    /**
     * Hook called after estado items are populated. Subclasses may override to
     * remove or lock specific checkboxes (e.g., remove CANCELADO).
     */
    protected void postPopulateEstadoItems() {
        // default: no-op
    }

    // Protected accessors for subclasses
    protected java.util.List<JCheckBox> getEstadoComprobanteItems() {
        return estadoComprobanteItems;
    }

    protected java.util.List<JCheckBox> getEstadoRopaItems() {
        return estadoRopaItems;
    }

    protected JPanel getEstadoComprobantePanel() {
        return estadoComprobantePanel;
    }

    protected JPanel getEstadoRopaPanel() {
        return estadoRopaPanel;
    }

    private void updateEstadoButtonLabel() {
        // counter removed - no UI update needed
    }

    private void resetFilters() {
        filterCod.setText("");
        filterCliente.setSelectedItem("");
        for (JCheckBox it : estadoRopaItems)
            it.setSelected(true);
        for (JCheckBox it : estadoComprobanteItems)
            it.setSelected(true);
        filterFecha.setDate(null);
        updateEstadoButtonLabel();
    }

    private void onEdited() {
        loadPage(currentPage);
    }

    private void deleteSelected() {
        Integer id = getSelectedId();
        if (id == null)
            return;
        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar el comprobante seleccionado?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION)
            return;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            String cod = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT cod_comprobante FROM comprobantes WHERE id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        cod = rs.getString(1);
                }
            }
            try (PreparedStatement ps = conn
                    .prepareStatement("DELETE FROM comprobantes_detalles WHERE comprobante_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            if (cod != null) {
                try (PreparedStatement ps = conn
                        .prepareStatement("DELETE FROM reporte_ingresos WHERE cod_comprobante=?")) {
                    ps.setString(1, cod);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comprobantes WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Eliminado.");
            loadPage(currentPage);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error eliminando:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPage(int page) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            switch (mode) {
                case RECIBIDOS -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id IN (1,2) ");
                case CANCELADOS -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id=4 ");
                case DEFAULT -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id IN (1,2,4) ");
                case TODOS -> {
                }
            }
            List<Object> params = new ArrayList<>();

            // Filter: COD
            String cod = filterCod.getText().trim();
            if (!cod.isEmpty()) {
                where.append(" AND c.cod_comprobante LIKE ? ");
                params.add('%' + cod + '%');
            }

            // Filter: CLIENTE
            String clienteVal = (filterCliente.getEditor().getItem() != null
                    ? filterCliente.getEditor().getItem().toString().trim()
                    : "");
            if (!clienteVal.isEmpty()) {
                where.append(" AND cl.nombres LIKE ? ");
                params.add('%' + clienteVal + '%');
            }

            // Filter: ESTADO ROPA (multi-select checkboxes)
            List<Integer> selRopa = new ArrayList<>();
            for (JCheckBox it : estadoRopaItems)
                if (it.isSelected())
                    try {
                        selRopa.add(Integer.parseInt(it.getActionCommand()));
                    } catch (Exception ignore) {
                    }
            List<Integer> selComp = new ArrayList<>();
            for (JCheckBox it : estadoComprobanteItems)
                if (it.isSelected())
                    try {
                        selComp.add(Integer.parseInt(it.getActionCommand()));
                    } catch (Exception ignore) {
                    }
            if (!selRopa.isEmpty() && !selComp.isEmpty()) {
                // require both estado_ropa and estado_comprobante to match selected ids
                // (intersection)
                where.append(" AND c.estado_ropa_id IN (");
                for (int i = 0; i < selRopa.size(); i++) {
                    where.append(i == 0 ? "?" : ",?");
                    params.add(selRopa.get(i));
                }
                where.append(") ");
                where.append(" AND c.estado_comprobante_id IN (");
                for (int i = 0; i < selComp.size(); i++) {
                    where.append(i == 0 ? "?" : ",?");
                    params.add(selComp.get(i));
                }
                where.append(") ");
            } else if (!selRopa.isEmpty()) {
                where.append(" AND c.estado_ropa_id IN (");
                for (int i = 0; i < selRopa.size(); i++) {
                    where.append(i == 0 ? "?" : ",?");
                    params.add(selRopa.get(i));
                }
                where.append(") ");
            } else if (!selComp.isEmpty()) {
                where.append(" AND c.estado_comprobante_id IN (");
                for (int i = 0; i < selComp.size(); i++) {
                    where.append(i == 0 ? "?" : ",?");
                    params.add(selComp.get(i));
                }
                where.append(") ");
            }

            // Filter: FECHA (date part)
            Date d = filterFecha.getDate();
            if (d != null) {
                where.append(" AND DATE(c.fecha) = ? ");
                params.add(new java.sql.Date(d.getTime()));
            }
            String sqlCount = "SELECT COUNT(*) FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id=cl.id" + where;
            try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                for (int i = 0; i < params.size(); i++) {
                    Object p = params.get(i);
                    if (p instanceof java.sql.Date)
                        ps.setDate(i + 1, (java.sql.Date) p);
                    else
                        ps.setObject(i + 1, p);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt(1);
                        totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
                    }
                }
            }
            currentPage = Math.min(page, totalPages);
            int offset = (currentPage - 1) * pageSize;
            String sql = "SELECT c.id,c.cod_comprobante,cl.nombres cliente,er.nom_estado_ropa estado_ropa,ec.nom_estado estado_comprobante,c.costo_total,(c.costo_total-IFNULL(c.monto_abonado,0)) deuda,c.fecha "
                    +
                    "FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id=cl.id LEFT JOIN estado_ropa er ON c.estado_ropa_id=er.id LEFT JOIN estado_comprobantes ec ON c.estado_comprobante_id=ec.id"
                    + where +
                    " ORDER BY c.fecha DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object p : params) {
                    if (p instanceof java.sql.Date)
                        ps.setDate(idx++, (java.sql.Date) p);
                    else
                        ps.setObject(idx++, p);
                }
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, offset);
                List<ComprobanteRow> rows = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ComprobanteRow r = new ComprobanteRow();
                        r.id = rs.getInt("id");
                        r.codComprobante = rs.getString("cod_comprobante");
                        r.cliente = rs.getString("cliente");
                        r.estadoRopa = rs.getString("estado_ropa");
                        r.estadoComprobante = rs.getString("estado_comprobante");
                        r.costoTotal = rs.getFloat("costo_total");
                        r.deuda = rs.getFloat("deuda");
                        Timestamp ts = rs.getTimestamp("fecha");
                        r.fecha = ts != null
                                ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm"))
                                : "";
                        rows.add(r);
                    }
                }
                model.setRows(rows);
            }
            lblPagina.setText("Página " + currentPage + " de " + totalPages);
            btnPrev.setEnabled(currentPage > 1);
            btnNext.setEnabled(currentPage < totalPages);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando comprobantes:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unused")
    private static class ComprobanteRow {
        int id;
        String codComprobante;
        String cliente;
        String estadoRopa;
        String estadoComprobante;
        float costoTotal;
        float deuda;
        String fecha;
    }

    private static class ComprobantesTableModel extends AbstractTableModel {
        private final String[] cols = { "COD COMPROBANTE", "CLIENTE", "ESTADO ROPA", "ESTADO COMPROBANTE",
                "COSTO TOTAL (S/.)", "DEUDA (S/.)", "FECHA DE REGISTRO" };
        private List<ComprobanteRow> rows = new ArrayList<>();

        public void setRows(List<ComprobanteRow> data) {
            rows = data;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Object getValueAt(int r, int c) {
            ComprobanteRow row = rows.get(r);
            return switch (c) {
                case 0 -> row.codComprobante;
                case 1 -> row.cliente;
                case 2 -> row.estadoRopa;
                case 3 -> row.estadoComprobante;
                case 4 -> row.costoTotal;
                case 5 -> row.deuda;
                case 6 -> row.fecha;
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return switch (c) {
                case 4, 5 -> Float.class;
                default -> String.class;
            };
        }
    }

    // Simple escaping for HTML content
    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
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
