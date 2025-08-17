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
    private final JComboBox<String> cboCampo = new JComboBox<>(new String[]{"COD COMPROBANTE","CLIENTE"});
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
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

    private void buildUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Buscar:"));
        txtBuscar.setPreferredSize(new Dimension(170,24));
        top.add(txtBuscar); top.add(cboCampo); top.add(btnBuscar); top.add(btnReset);
        btnBuscar.addActionListener(this::onBuscar);
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent evt) { txtBuscar.setText(""); loadPage(1); }
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
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

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
                if (cboCampo.getSelectedIndex()==0) { where.append(" AND c.cod_comprobante LIKE ? "); params.add('%'+search+'%'); }
                else { where.append(" AND cl.nombres LIKE ? "); params.add('%'+search+'%'); }
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
