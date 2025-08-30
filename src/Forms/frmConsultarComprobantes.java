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
        // "Estados:" label removed per request; estadoPanel will be placed on its own line
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
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openEditDialog();
            }
        });
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
                // filter where either columna matches selected ids
                where.append(" AND ( ").append("c.estado_ropa_id IN (");
                for (int i = 0; i < selRopa.size(); i++) {
                    where.append(i == 0 ? "?" : ",?");
                    params.add(selRopa.get(i));
                }
                where.append(") OR c.estado_comprobante_id IN (");
                for (int i = 0; i < selComp.size(); i++) {
                    where.append(i == 0 ? "?" : ",?");
                    params.add(selComp.get(i));
                }
                where.append(") ) ");
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
}
