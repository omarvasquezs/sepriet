package Forms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.toedter.calendar.JDateChooser;

/**
 * Internal frame to show workload report (comprobantes) with date filters,
 * pagination and CSV export. Mirrors frmReporteFinanciero but with different
 * columns.
 */
public class frmReporteTrabajo extends JInternalFrame {
    private final JDateChooser startDate = new JDateChooser();
    private final JDateChooser endDate = new JDateChooser();
    private final JCheckBox chkHoy = new JCheckBox("FECHA HOY DÍA");
    private final JCheckBox chkMes = new JCheckBox("MES ACTUAL");
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
    private final JButton btnExportCsv = new JButton("Exportar CSV");
    private final JTable table = new JTable();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] { "COMPROBANTE", "CLIENTE", "FECHA", "COSTO TOTAL" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // pagination state
    private int currentPage = 1;
    private int totalPages = 1;
    private int pageSize = 50;

    public frmReporteTrabajo() {
        super("Reporte Trabajo", true, true, true, true);
        initUI();
        setSize(900, 480);
        loadPage(1);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            try { setMaximum(true); } catch (Exception ignored) {}
        });
    }

    private void initUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        startDate.setPreferredSize(new Dimension(140, 24));
        endDate.setPreferredSize(new Dimension(140, 24));
        top.add(new JLabel("FECHA INICIO:"));
        top.add(startDate);
        top.add(new JLabel("FECHA FIN:"));
        top.add(endDate);
        top.add(chkHoy);
        top.add(chkMes);
        btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportCsv.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        top.add(btnBuscar);
        top.add(btnReset);
        top.add(btnExportCsv);

        btnBuscar.addActionListener(this::onBuscar);
        btnExportCsv.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                exportCsv();
            }
        });
        btnReset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                resetFilters();
            }
        });

        table.setModel(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        // pagination controls
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        final JButton btnPrev = new JButton("<");
        final JButton btnNext = new JButton(">");
        final JLabel lblPagina = new JLabel("Página 1 de 1");
        final JComboBox<Integer> pageSizeCombo = new JComboBox<>(new Integer[] { 10, 25, 50, 100 });
        pageSizeCombo.setSelectedItem(Integer.valueOf(pageSize));
        pageSizeCombo.setMaximumRowCount(6);
        pageSizeCombo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPrev.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        bottom.add(new JLabel("Registros por página:"));
        bottom.add(pageSizeCombo);
        bottom.add(btnPrev);
        bottom.add(btnNext);
        bottom.add(lblPagina);

        btnPrev.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (currentPage > 1) loadPage(currentPage - 1);
            }
        });
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (currentPage < totalPages) loadPage(currentPage + 1);
            }
        });
        pageSizeCombo.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Integer sel = (Integer) pageSizeCombo.getSelectedItem();
                if (sel != null && sel != pageSize) { pageSize = sel; loadPage(1); }
            }
        });

        getContentPane().setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void onBuscar(java.awt.event.ActionEvent ev) {
        if (chkHoy.isSelected()) {
            Date today = new Date();
            startDate.setDate(today);
            endDate.setDate(today);
        } else if (chkMes.isSelected()) {
            Date now = new Date();
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(now);
            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
            startDate.setDate(c.getTime());
            c.add(java.util.Calendar.MONTH, 1);
            c.set(java.util.Calendar.DAY_OF_MONTH, 1);
            c.add(java.util.Calendar.DATE, -1);
            endDate.setDate(c.getTime());
        }
        loadPage(1);
    }

    private void loadPage(int page) {
        model.setRowCount(0);
        String baseWhere = " WHERE 1=1 ";
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (startDate.getDate() != null && endDate.getDate() != null) {
            baseWhere += " AND DATE(c.fecha) >= ? AND DATE(c.fecha) <= ? ";
            params.add(new java.sql.Date(startDate.getDate().getTime()));
            params.add(new java.sql.Date(endDate.getDate().getTime()));
        }

        String countSql = "SELECT COUNT(*) FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id = cl.id " + baseWhere;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pc = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) pc.setObject(i + 1, params.get(i));
            try (ResultSet rc = pc.executeQuery()) {
                int total = 0;
                if (rc.next()) total = rc.getInt(1);
                totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            }

            currentPage = Math.min(page, totalPages);
            int offset = (currentPage - 1) * pageSize;

            String sql = "SELECT c.cod_comprobante, cl.nombres, DATE(c.fecha) as fecha, c.costo_total "
                    + "FROM comprobantes c LEFT JOIN clientes cl ON c.cliente_id = cl.id "
                    + baseWhere
                    + " ORDER BY c.fecha DESC LIMIT ? OFFSET ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object p : params) ps.setObject(idx++, p);
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    while (rs.next()) {
                        String cod = rs.getString("cod_comprobante");
                        String cliente = rs.getString("nombres");
                        Date f = null;
                        try { f = rs.getDate("fecha"); } catch (Exception ignored) {}
                        String fecha = f != null ? df.format(f) : "";
                        double monto = rs.getDouble("costo_total");
                        model.addRow(new Object[] { cod, cliente, fecha, formatNumber(monto) });
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando reporte:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("registro_trabajo_comprobantes_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv"));
        int sel = fc.showSaveDialog(this);
        if (sel != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("COMPROBANTE,CLIENTE,FECHA,COSTO TOTAL\n");
            for (int r = 0; r < model.getRowCount(); r++) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < model.getColumnCount(); c++) {
                    Object v = model.getValueAt(r, c);
                    String cell = v == null ? "" : v.toString().replace("\"", "\"\"");
                    if (cell.contains(",") || cell.contains("\n")) cell = '"' + cell + '"';
                    if (c > 0) line.append(',');
                    line.append(cell);
                }
                line.append('\n');
                fw.write(line.toString());
            }
            fw.flush();
            JOptionPane.showMessageDialog(this, "CSV exportado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exportando CSV:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetFilters() {
        startDate.setDate(null);
        endDate.setDate(null);
        chkHoy.setSelected(false);
        chkMes.setSelected(false);
        loadPage(1);
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
