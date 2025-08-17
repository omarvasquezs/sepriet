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
    // which comprobante column to filter for estados (either estado_ropa_id or estado_comprobante_id)
    private String estadoFilterColumn = "c.estado_ropa_id";
    private final JButton filterEstadoBtn = new JButton("Estados (Todos)");
    private final JDateChooser filterFecha = new JDateChooser();
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
    private final JPopupMenu estadoPopup = new JPopupMenu();
    private final List<Integer> estadoIds = new ArrayList<>();
    private final List<JCheckBoxMenuItem> estadoItems = new ArrayList<>();
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
    private static final int PAGE_SIZE = 10;
    private Mode mode;

    public enum Mode { TODOS, RECIBIDOS, CANCELADOS, DEFAULT }

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
            try { setMaximum(true); } catch (Exception ignored) {}
        });
    }

    private void buildUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Filtros:"));
        filterCod.setPreferredSize(new Dimension(150,24));
        filterCliente.setPreferredSize(new Dimension(180,24));
        filterCliente.setEditable(true);
        filterFecha.setPreferredSize(new Dimension(130,24));
        top.add(new JLabel("Cod:")); top.add(filterCod);
        top.add(new JLabel("Cliente:")); top.add(filterCliente);
        top.add(filterEstadoBtn);
        top.add(new JLabel("Fecha:")); top.add(filterFecha);
        top.add(btnBuscar); top.add(btnReset);
        btnBuscar.addActionListener(this::onBuscar);
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent evt) { resetFilters(); loadPage(1); }
        });

        // CRUD toolbar
        JPanel crudBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        crudBar.add(btnAdd); crudBar.add(btnEdit); crudBar.add(btnDelete);
        btnEdit.setEnabled(false); btnDelete.setEnabled(false);
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { openAddComprobante(); }
        });
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { openEditDialog(); }
        });
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { deleteSelected(); }
        });

        table.setModel(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        // Populate cliente names and estados for filters
        SwingUtilities.invokeLater(() -> {
            populateClienteNames();
            populateEstadoItems();
            try { org.jdesktop.swingx.autocomplete.AutoCompleteDecorator.decorate(filterCliente); } catch (Exception ignore) {}
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPrev.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent evt) { if(currentPage>1) loadPage(currentPage-1); }
        });
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent evt) { if(currentPage<totalPages) loadPage(currentPage+1); }
        });
        bottom.add(btnPrev); bottom.add(btnNext); bottom.add(lblPagina);

        getContentPane().setLayout(new BorderLayout());
    JPanel north = new JPanel(new BorderLayout());
    north.add(top, BorderLayout.NORTH);
    north.add(crudBar, BorderLayout.SOUTH);
    add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override public void valueChanged(javax.swing.event.ListSelectionEvent ev) {
                boolean sel = table.getSelectedRow() >= 0;
                btnEdit.setEnabled(sel); btnDelete.setEnabled(sel);
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount()==2) {
                    int row = table.getSelectedRow();
                    if (row>=0) {
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

    private void onBuscar(ActionEvent e) { loadPage(1); }

    private Integer getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        return model.rows.get(modelRow).id;
    }

    private void openAddComprobante() {
        // Attempt to reuse existing registrar frame if open
        JDesktopPane dp = getDesktopPane();
        if (dp != null) {
            for (JInternalFrame f : dp.getAllFrames()) {
                if (f instanceof frmRegistrarComprobante) {
                    try { f.setIcon(false); f.setSelected(true); } catch (Exception ignored) {}
                    f.toFront();
                    return;
                }
            }
            frmRegistrarComprobante reg = new frmRegistrarComprobante();
            dp.add(reg); reg.setVisible(true);
        }
    }

    private void openEditDialog() {
        Integer id = getSelectedId();
        if (id == null) return;
        DlgEditarComprobante dlg = new DlgEditarComprobante(SwingUtilities.getWindowAncestor(this), id, this::onEdited);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // Populate cliente names into filterCliente combobox
    private void populateClienteNames() {
        filterCliente.removeAllItems(); allClientes.clear();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nombres FROM clientes ORDER BY nombres LIMIT 1000");
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
        for (String c : allClientes) model.addElement(c);
        filterCliente.setModel(model);
        filterCliente.setSelectedIndex(0);
        // add simple live-filtering to mimic autocomplete if swingx decorator isn't available
        try {
            JTextField editor = (JTextField) filterCliente.getEditor().getEditorComponent();
            editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void update() {
                    String text = editor.getText();
                    DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
                    m.addElement(text);
                    String lower = text.toLowerCase();
                    for (String it : allClientes) {
                        if (it.toLowerCase().contains(lower) && !it.equals(text)) m.addElement(it);
                    }
                    SwingUtilities.invokeLater(() -> {
                        filterCliente.setModel(m);
                        editor.setText(text);
                        filterCliente.setPopupVisible(m.getSize()>1);
                    });
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            });
        } catch (Exception ex) {
            // ignore if editor not available
        }
    }

    // Build estado popup with checkbox menu items (all checked by default)
    private void populateEstadoItems() {
        estadoPopup.removeAll(); estadoItems.clear(); estadoIds.clear();
        // Try estado_ropa first (common in this project). If not present, try estado_comprobantes
        boolean loaded = false;
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id,nom_estado_ropa FROM estado_ropa ORDER BY nom_estado_ropa"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1); String nom = rs.getString(2);
                    JCheckBoxMenuItem it = new JCheckBoxMenuItem(nom, true);
                    it.setActionCommand(String.valueOf(id)); estadoItems.add(it); estadoIds.add(id); estadoPopup.add(it);
                    it.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent ae) { updateEstadoButtonLabel(); } });
                }
                if (!estadoItems.isEmpty()) { estadoFilterColumn = "c.estado_ropa_id"; loaded = true; }
            } catch (SQLException ignored) {}
            if (!loaded) {
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT id,nom_estado FROM estado_comprobantes ORDER BY nom_estado"); ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        int id = rs2.getInt(1); String nom = rs2.getString(2);
                        JCheckBoxMenuItem it = new JCheckBoxMenuItem(nom, true);
                        it.setActionCommand(String.valueOf(id)); estadoItems.add(it); estadoIds.add(id); estadoPopup.add(it);
                        it.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent ae) { updateEstadoButtonLabel(); } });
                    }
                    if (!estadoItems.isEmpty()) { estadoFilterColumn = "c.estado_comprobante_id"; loaded = true; }
                } catch (SQLException ignored) {}
            }
        } catch (Exception ex) {
            // ignore
        }
        if (!loaded) {
            JCheckBoxMenuItem it = new JCheckBoxMenuItem("Todos", true); it.setActionCommand("0"); estadoItems.add(it); estadoPopup.add(it);
        }
        filterEstadoBtn.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent ae) { estadoPopup.show(filterEstadoBtn, 0, filterEstadoBtn.getHeight()); } });
        updateEstadoButtonLabel();
    }

    private void updateEstadoButtonLabel() {
        int checked = 0; for (JCheckBoxMenuItem it : estadoItems) if (it.isSelected()) checked++;
        filterEstadoBtn.setText("Estados ("+checked+")");
    }

    private void resetFilters() {
        filterCod.setText("");
        filterCliente.setSelectedItem("");
        for (JCheckBoxMenuItem it : estadoItems) it.setSelected(true);
        filterFecha.setDate(null);
        updateEstadoButtonLabel();
    }

    private void onEdited() { loadPage(currentPage); }

    private void deleteSelected() {
        Integer id = getSelectedId();
        if (id == null) return;
        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar el comprobante seleccionado?", "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            String cod = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT cod_comprobante FROM comprobantes WHERE id=?")) {
                ps.setInt(1, id); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) cod = rs.getString(1); }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comprobantes_detalles WHERE comprobante_id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
            if (cod != null) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM reporte_ingresos WHERE cod_comprobante=?")) { ps.setString(1, cod); ps.executeUpdate(); }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM comprobantes WHERE id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
            conn.commit();
            JOptionPane.showMessageDialog(this, "Eliminado.");
            loadPage(currentPage);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error eliminando:\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPage(int page) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            switch (mode) {
                case RECIBIDOS -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id IN (1,2) ");
                case CANCELADOS -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id=4 ");
                case DEFAULT -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id IN (1,2,4) ");
                case TODOS -> {}
            }
            List<Object> params = new ArrayList<>();

            // Filter: COD
            String cod = filterCod.getText().trim();
            if (!cod.isEmpty()) { where.append(" AND c.cod_comprobante LIKE ? "); params.add('%'+cod+'%'); }

            // Filter: CLIENTE
            String clienteVal = (filterCliente.getEditor().getItem()!=null? filterCliente.getEditor().getItem().toString().trim(): "");
            if (!clienteVal.isEmpty()) { where.append(" AND cl.nombres LIKE ? "); params.add('%'+clienteVal+'%'); }

            // Filter: ESTADO ROPA (multi-select checkboxes)
            List<Integer> selEstadoIds = new ArrayList<>();
            for (JCheckBoxMenuItem it : estadoItems) {
                if (it.isSelected()) {
                    try { selEstadoIds.add(Integer.parseInt(it.getActionCommand())); } catch (Exception ignore) {}
                }
            }
            if (!selEstadoIds.isEmpty()) {
                where.append(" AND ").append(estadoFilterColumn).append(" IN (");
                for (int i=0;i<selEstadoIds.size();i++) { where.append(i==0?"?":",?"); params.add(selEstadoIds.get(i)); }
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
                for (int i=0;i<params.size();i++) {
                    Object p = params.get(i);
                    if (p instanceof java.sql.Date) ps.setDate(i+1, (java.sql.Date)p);
                    else ps.setObject(i+1, p);
                }
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { int total = rs.getInt(1); totalPages = Math.max(1, (int)Math.ceil(total/(double)PAGE_SIZE)); } }
            }
            currentPage = Math.min(page, totalPages); int offset=(currentPage-1)*PAGE_SIZE;
            String sql = "SELECT c.id,c.cod_comprobante,cl.nombres cliente,er.nom_estado_ropa estado_ropa,c.costo_total,(c.costo_total-IFNULL(c.monto_abonado,0)) deuda,c.fecha " +
                    "FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id=cl.id LEFT JOIN estado_ropa er ON c.estado_ropa_id=er.id" + where +
                    " ORDER BY c.fecha DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx=1; for(Object p:params) { if (p instanceof java.sql.Date) ps.setDate(idx++, (java.sql.Date)p); else ps.setObject(idx++, p); }
                ps.setInt(idx++, PAGE_SIZE); ps.setInt(idx, offset);
                List<ComprobanteRow> rows = new ArrayList<>();
                try(ResultSet rs=ps.executeQuery()) { while(rs.next()){ ComprobanteRow r=new ComprobanteRow(); r.id=rs.getInt("id"); r.codComprobante=rs.getString("cod_comprobante"); r.cliente=rs.getString("cliente"); r.estadoRopa=rs.getString("estado_ropa"); r.costoTotal=rs.getFloat("costo_total"); r.deuda=rs.getFloat("deuda"); Timestamp ts=rs.getTimestamp("fecha"); r.fecha= ts!=null? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")) : ""; rows.add(r);} }
                model.setRows(rows);
            }
            lblPagina.setText("Página "+currentPage+" de "+totalPages);
            btnPrev.setEnabled(currentPage>1); btnNext.setEnabled(currentPage<totalPages);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando comprobantes:\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    

    @SuppressWarnings("unused")
    private static class ComprobanteRow { int id; String codComprobante; String cliente; String estadoRopa; float costoTotal; float deuda; String fecha; }
    private static class ComprobantesTableModel extends AbstractTableModel {
        private final String[] cols = {"COD COMPROBANTE","CLIENTE","ESTADO ROPA","COSTO TOTAL (S/.)","DEUDA (S/.)","FECHA DE REGISTRO"};
        private List<ComprobanteRow> rows = new ArrayList<>();
        public void setRows(List<ComprobanteRow> data){ rows=data; fireTableDataChanged(); }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r,int c){ ComprobanteRow row=rows.get(r); return switch(c){ case 0->row.codComprobante; case 1->row.cliente; case 2->row.estadoRopa; case 3->row.costoTotal; case 4->row.deuda; case 5->row.fecha; default->null; }; }
        @Override public Class<?> getColumnClass(int c){ return switch(c){ case 3,4->Float.class; default->String.class; }; }
    }
}
