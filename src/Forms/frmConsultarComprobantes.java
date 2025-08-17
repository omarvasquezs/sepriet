package Forms;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Internal frame to list comprobantes (read only) with search & pagination. */
public class frmConsultarComprobantes extends JInternalFrame {
    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> cboCampo = new JComboBox<>(new String[]{
        "COD COMPROBANTE",
        "CLIENTE",
        "ESTADO ROPA",
        "COSTO TOTAL (S/.)",
        "DEUDA (S/.)",
        "FECHA DE REGISTRO"
    });
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
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
        top.add(new JLabel("Buscar:"));
        txtBuscar.setPreferredSize(new Dimension(170,24));
        top.add(txtBuscar); top.add(cboCampo); top.add(btnBuscar); top.add(btnReset);
        btnBuscar.addActionListener(this::onBuscar);
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent evt) { txtBuscar.setText(""); loadPage(1); }
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
        String search = txtBuscar.getText().trim();
        try (Connection conn = DatabaseConfig.getConnection()) {
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            switch (mode) {
                case RECIBIDOS -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id IN (1,2) ");
                case CANCELADOS -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id=4 ");
                case DEFAULT -> where.append(" AND c.estado_ropa_id IN (1,3) AND c.estado_comprobante_id IN (1,2,4) ");
                case TODOS -> {}
            }
            List<Object> params = new ArrayList<>();
            if (!search.isEmpty()) {
                int fieldIndex = cboCampo.getSelectedIndex();
                switch (fieldIndex) {
                    case 0 -> { // COD COMPROBANTE
                        where.append(" AND c.cod_comprobante LIKE ? "); params.add('%'+search+'%');
                    }
                    case 1 -> { // CLIENTE
                        where.append(" AND cl.nombres LIKE ? "); params.add('%'+search+'%');
                    }
                    case 2 -> { // ESTADO ROPA
                        where.append(" AND er.nom_estado_ropa LIKE ? "); params.add('%'+search+'%');
                    }
                    case 3 -> { // COSTO TOTAL numeric equality
                        Float v = parseFloat(search);
                        if (v != null) { where.append(" AND c.costo_total = ? "); params.add(v); }
                        else { JOptionPane.showMessageDialog(this, "Ingrese un número válido para COSTO TOTAL."); }
                    }
                    case 4 -> { // DEUDA expression (c.costo_total - IFNULL(c.monto_abonado,0))
                        Float v = parseFloat(search);
                        if (v != null) { where.append(" AND (c.costo_total - IFNULL(c.monto_abonado,0)) = ? "); params.add(v); }
                        else { JOptionPane.showMessageDialog(this, "Ingrese un número válido para DEUDA."); }
                    }
                    case 5 -> { // FECHA DE REGISTRO (date part) expect yyyy-MM-dd
                        where.append(" AND DATE(c.fecha) = ? "); params.add(search);
                    }
                }
            }
            String sqlCount = "SELECT COUNT(*) FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id=cl.id" + where;
            try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                for (int i=0;i<params.size();i++) ps.setObject(i+1, params.get(i));
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { int total = rs.getInt(1); totalPages = Math.max(1, (int)Math.ceil(total/(double)PAGE_SIZE)); } }
            }
            currentPage = Math.min(page, totalPages); int offset=(currentPage-1)*PAGE_SIZE;
            String sql = "SELECT c.id,c.cod_comprobante,cl.nombres cliente,er.nom_estado_ropa estado_ropa,c.costo_total,(c.costo_total-IFNULL(c.monto_abonado,0)) deuda,c.fecha " +
                    "FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id=cl.id LEFT JOIN estado_ropa er ON c.estado_ropa_id=er.id" + where +
                    " ORDER BY c.fecha DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx=1; for(Object p:params) ps.setObject(idx++, p); ps.setInt(idx++, PAGE_SIZE); ps.setInt(idx, offset);
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

    private Float parseFloat(String s) {
        try { return Float.parseFloat(s); } catch (Exception ex) { return null; }
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
