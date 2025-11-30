package Forms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.toedter.calendar.JDateChooser;

/**
 * Internal frame to show financial report (reporte_ingresos) with date filters
 * and CSV export. Styled similarly to other listing frames.
 */
public class frmReporteFinanciero extends JInternalFrame {
    private final JDateChooser startDate = new JDateChooser();
    private final JDateChooser endDate = new JDateChooser();
    private final JCheckBox chkHoy = new JCheckBox("FECHA HOY DÍA");
    private final JCheckBox chkMes = new JCheckBox("MES ACTUAL");
    // When we programmatically update the date fields we temporarily suppress
    // the property-change listeners so they don't uncheck the quick-range boxes.
    private boolean suppressDateChange = false;
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
    private final JButton btnExportCsv = new JButton("Exportar CSV");
    private final JButton btnExportExcel = new JButton("Exportar Excel");
    private final JTable table = new JTable();
    // summary labels shown above the table (similar to web app)
    private final JLabel lblFilas = new JLabel("FILAS ENCONTRADAS: 0");
    private final JLabel lblEfectivo = new JLabel("EFECTIVO TOTAL: 0.00");
    private final JLabel lblYape = new JLabel("YAPE / PLIN TOTAL: 0.00");
    private final JLabel lblTotalIngresos = new JLabel("INGRESOS TOTALES EN SOLES: 0.00");
    private final JLabel lblApertura = new JLabel("APERTURA CAJA: -");
    private final JLabel lblCierre = new JLabel("CIERRE CAJA: -");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[] { "COMPROBANTE", "CLIENTE", "FECHA DE ABONO", "METODO DE PAGO", "MONTO ABONADO" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // Pagination state (defaults similar to other frames)
    private int currentPage = 1;
    private int totalPages = 1;
    private int pageSize = 50;
    private JLabel lblPagina; // Reference to pagination label

    public frmReporteFinanciero() {
        super("Reporte Ingresos", true, true, true, true);
        java.net.URL iconUrl = getClass().getResource("/Forms/icon.png");
        if (iconUrl != null) {
            setFrameIcon(new ImageIcon(iconUrl));
        }
        initUI();
        setSize(900, 480);
        loadPage(1);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Maximize after added to desktop (run on EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                setMaximum(true);
            } catch (Exception ignored) {
            }
        });
    }

    private void initUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        startDate.setPreferredSize(new Dimension(140, 24));
        endDate.setPreferredSize(new Dimension(140, 24));
        // If the user manually edits either date, clear the quick-range checkboxes.
        // When we programmatically set the dates we set suppressDateChange=true so
        // these listeners won't fire.
        startDate.addPropertyChangeListener("date", evt -> {
            if (suppressDateChange)
                return;
            if (evt.getNewValue() != null || evt.getOldValue() != null) {
                chkHoy.setSelected(false);
                chkMes.setSelected(false);
            }
        });
        endDate.addPropertyChangeListener("date", evt -> {
            if (suppressDateChange)
                return;
            if (evt.getNewValue() != null || evt.getOldValue() != null) {
                chkHoy.setSelected(false);
                chkMes.setSelected(false);
            }
        });

        // Mutually exclusive checkboxes: selecting one deselects the other and
        // autofills the date range accordingly.
        chkHoy.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                if (chkHoy.isSelected()) {
                    // deselect other
                    if (chkMes.isSelected())
                        chkMes.setSelected(false);
                    // fill both dates with today
                    try {
                        suppressDateChange = true;
                        Date today = new Date();
                        startDate.setDate(today);
                        endDate.setDate(today);
                    } finally {
                        suppressDateChange = false;
                    }
                    loadPage(1);
                }
            }
        });

        chkMes.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                if (chkMes.isSelected()) {
                    if (chkHoy.isSelected())
                        chkHoy.setSelected(false);
                    try {
                        suppressDateChange = true;
                        Date now = new Date();
                        java.util.Calendar c = java.util.Calendar.getInstance();
                        c.setTime(now);
                        c.set(java.util.Calendar.DAY_OF_MONTH, 1);
                        startDate.setDate(c.getTime());
                        c.add(java.util.Calendar.MONTH, 1);
                        c.set(java.util.Calendar.DAY_OF_MONTH, 1);
                        c.add(java.util.Calendar.DATE, -1);
                        endDate.setDate(c.getTime());
                    } finally {
                        suppressDateChange = false;
                    }
                    loadPage(1);
                }
            }
        });
        top.add(new JLabel("FECHA INICIO:"));
        top.add(startDate);
        top.add(new JLabel("FECHA FIN:"));
        top.add(endDate);
        top.add(chkHoy);
        top.add(chkMes);
        btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportCsv.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnExportExcel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        top.add(btnBuscar);
        top.add(btnReset);
        top.add(btnExportCsv);
        top.add(btnExportExcel);

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
        btnExportCsv.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                exportCsv();
            }
        });
        btnExportExcel.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                exportExcel();
            }
        });

        table.setModel(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        // pagination controls (bottom)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        final JButton btnPrev = new JButton("<");
        final JButton btnNext = new JButton(">");
        lblPagina = new JLabel("Página 1 de 1");
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

        // wire pagination actions
        btnPrev.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (currentPage > 1)
                    loadPage(currentPage - 1);
            }
        });
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (currentPage < totalPages)
                    loadPage(currentPage + 1);
            }
        });
        pageSizeCombo.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Integer sel = (Integer) pageSizeCombo.getSelectedItem();
                if (sel != null && sel != pageSize) {
                    pageSize = sel;
                    loadPage(1);
                }
            }
        });

        // summary panel under filters
        JPanel summary = new JPanel(new GridLayout(6, 1, 4, 4));
        summary.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        summary.add(lblApertura);
        summary.add(lblCierre);
        summary.add(lblFilas);
        summary.add(lblEfectivo);
        summary.add(lblYape);
        summary.add(lblTotalIngresos);

        JPanel center = new JPanel(new BorderLayout());
        center.add(summary, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void exportExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(
                "reporte_ingresos_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx"));
        int sel = fc.showSaveDialog(this);
        if (sel != JFileChooser.APPROVE_OPTION)
            return;
        java.io.File f = fc.getSelectedFile();

        // Get all data, not just current page
        java.util.List<Object[]> allData = getAllFilteredData();
        if (allData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(f); ZipOutputStream zos = new ZipOutputStream(fos)) {
            // [Content_Types].xml
            String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    +
                    "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                    +
                    "</Types>";
            putEntry(zos, "[Content_Types].xml", contentTypes);

            // _rels/.rels
            String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    +
                    "</Relationships>";
            putEntry(zos, "_rels/.rels", rels);

            // xl/workbook.xml
            String workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                    +
                    "<sheets><sheet name=\"Reporte\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                    "</workbook>";
            putEntry(zos, "xl/workbook.xml", workbook);

            // xl/_rels/workbook.xml.rels
            String wbRels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    +
                    "</Relationships>";
            putEntry(zos, "xl/_rels/workbook.xml.rels", wbRels);

            // xl/worksheets/sheet1.xml
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
            sb.append("<sheetData>\n");
            // header row r=1
            sb.append("<row r=\"1\">\n");
            for (int c = 0; c < model.getColumnCount(); c++) {
                String val = escapeXml(model.getColumnName(c));
                sb.append("<c t=\"inlineStr\"><is><t>").append(val).append("</t></is></c>");
            }
            sb.append("</row>\n");
            // data rows starting r=2 - use all filtered data
            for (int r = 0; r < allData.size(); r++) {
                sb.append("<row r=\"" + (r + 2) + "\">\n");
                Object[] rowData = allData.get(r);
                for (int c = 0; c < rowData.length; c++) {
                    Object v = rowData[c];
                    String cell = v == null ? "" : escapeXml(v.toString());
                    // write everything as inlineStr to avoid num/date formatting complexity
                    sb.append("<c t=\"inlineStr\"><is><t>").append(cell).append("</t></is></c>");
                }
                sb.append("</row>\n");
            }
            sb.append("</sheetData>\n");
            sb.append("</worksheet>");
            putEntry(zos, "xl/worksheets/sheet1.xml", sb.toString());

            // xl/styles.xml minimal
            String styles = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                    "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
                    "<fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills>" +
                    "<borders count=\"1\"><border/></borders>" +
                    "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                    +
                    "<cellXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs>"
                    +
                    "</styleSheet>";
            putEntry(zos, "xl/styles.xml", styles);

            JOptionPane.showMessageDialog(this, "Excel exportado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exportando Excel:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void putEntry(ZipOutputStream zos, String name, String content) throws java.io.IOException {
        ZipEntry ze = new ZipEntry(name);
        zos.putNextEntry(ze);
        byte[] b = content.getBytes(StandardCharsets.UTF_8);
        zos.write(b);
        zos.closeEntry();
    }

    private static String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeXml(String s) {
        return escapeHtml(s);
    }

    private java.util.List<Object[]> getAllFilteredData() {
        java.util.List<Object[]> result = new java.util.ArrayList<>();
        String baseWhere = " WHERE r.monto_abonado > 0 ";
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (startDate.getDate() != null && endDate.getDate() != null) {
            baseWhere += " AND DATE(r.fecha) >= ? AND DATE(r.fecha) <= ? ";
            params.add(new java.sql.Date(startDate.getDate().getTime()));
            params.add(new java.sql.Date(endDate.getDate().getTime()));
        }

        String sql = "SELECT r.cod_comprobante, c.nombres, DATE(r.fecha) as fecha, "
                + "COALESCE(NULLIF(m.nom_metodo_pago, ''), 'NINGUNO') as nom_metodo_pago, r.monto_abonado "
                + "FROM reporte_ingresos r "
                + "LEFT JOIN metodo_pago m ON r.metodo_pago_id = m.id "
                + "LEFT JOIN clientes c ON r.cliente_id = c.id "
                + baseWhere
                + " ORDER BY r.fecha DESC";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Object p : params)
                ps.setObject(idx++, p);
            try (ResultSet rs = ps.executeQuery()) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                while (rs.next()) {
                    String cod = rs.getString("cod_comprobante");
                    String cliente = rs.getString("nombres");
                    Date f = null;
                    try {
                        f = rs.getDate("fecha");
                    } catch (Exception ignored) {
                    }
                    String fecha = f != null ? df.format(f) : "";
                    String metodo = rs.getString("nom_metodo_pago");
                    double monto = rs.getDouble("monto_abonado");
                    result.add(new Object[] { cod, cliente, fecha, metodo, formatNumber(monto) });
                }
            }
        } catch (Exception ex) {
            // Log error but return empty list
            ex.printStackTrace();
        }
        return result;
    }

    private void onBuscar(ActionEvent ev) {
        // quick toggles
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
        String baseWhere = " WHERE r.monto_abonado > 0 ";
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (startDate.getDate() != null && endDate.getDate() != null) {
            baseWhere += " AND DATE(r.fecha) >= ? AND DATE(r.fecha) <= ? ";
            params.add(new java.sql.Date(startDate.getDate().getTime()));
            params.add(new java.sql.Date(endDate.getDate().getTime()));
        }
        // compute aggregate totals (EFECTIVO, YAPE/PLIN, TOTAL) for the current filters
        String aggSql = "SELECT "
                + "SUM(CASE WHEN COALESCE(NULLIF(m.nom_metodo_pago, ''), 'NINGUNO') = 'EFECTIVO' THEN r.monto_abonado ELSE 0 END) AS efectivo_total, "
                + "SUM(CASE WHEN COALESCE(NULLIF(m.nom_metodo_pago, ''), 'NINGUNO') = 'YAPE / PLIN' THEN r.monto_abonado ELSE 0 END) AS yape_total, "
                + "SUM(r.monto_abonado) AS ingresos_total, "
                + "COUNT(*) AS filas "
                + "FROM reporte_ingresos r "
                + "LEFT JOIN metodo_pago m ON r.metodo_pago_id = m.id "
                + baseWhere;

        String countSql = "SELECT COUNT(*) FROM reporte_ingresos r " + baseWhere;
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement aggPs = conn.prepareStatement(aggSql)) {
            // set params for aggregate query
            for (int i = 0; i < params.size(); i++)
                aggPs.setObject(i + 1, params.get(i));
            try (ResultSet aggRs = aggPs.executeQuery()) {
                if (aggRs.next()) {
                    double efectivo = aggRs.getDouble("efectivo_total");
                    double yape = aggRs.getDouble("yape_total");
                    double ingresos = aggRs.getDouble("ingresos_total");
                    int filas = aggRs.getInt("filas");
                    lblFilas.setText("FILAS ENCONTRADAS: " + filas);
                    lblEfectivo.setText("EFECTIVO TOTAL: " + formatNumber(efectivo));
                    lblYape.setText("YAPE / PLIN TOTAL: " + formatNumber(yape));
                    lblTotalIngresos.setText("INGRESOS TOTALES EN SOLES: " + formatNumber(ingresos));
                } else {
                    lblFilas.setText("FILAS ENCONTRADAS: 0");
                    lblEfectivo.setText("EFECTIVO TOTAL: 0.00");
                    lblYape.setText("YAPE / PLIN TOTAL: 0.00");
                    lblTotalIngresos.setText("INGRESOS TOTALES EN SOLES: 0.00");
                }
            }

            try (PreparedStatement pc = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++)
                    pc.setObject(i + 1, params.get(i));
                try (ResultSet rc = pc.executeQuery()) {
                    int total = 0;
                    if (rc.next())
                        total = rc.getInt(1);
                    totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
                }
            }

            currentPage = Math.min(page, totalPages);
            int offset = (currentPage - 1) * pageSize;

            String sql = "SELECT r.cod_comprobante, c.nombres, DATE(r.fecha) as fecha, "
                    + "COALESCE(NULLIF(m.nom_metodo_pago, ''), 'NINGUNO') as nom_metodo_pago, r.monto_abonado "
                    + "FROM reporte_ingresos r "
                    + "LEFT JOIN metodo_pago m ON r.metodo_pago_id = m.id "
                    + "LEFT JOIN clientes c ON r.cliente_id = c.id "
                    + baseWhere
                    + " ORDER BY r.fecha DESC LIMIT ? OFFSET ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object p : params)
                    ps.setObject(idx++, p);
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    while (rs.next()) {
                        String cod = rs.getString("cod_comprobante");
                        String cliente = rs.getString("nombres");
                        Date f = null;
                        try {
                            f = rs.getDate("fecha");
                        } catch (Exception ignored) {
                        }
                        String fecha = f != null ? df.format(f) : "";
                        String metodo = rs.getString("nom_metodo_pago");
                        double monto = rs.getDouble("monto_abonado");
                        model.addRow(new Object[] { cod, cliente, fecha, metodo, formatNumber(monto) });
                    }
                }
            }
            // Update pagination label
            lblPagina.setText("Página " + currentPage + " de " + totalPages);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando reporte:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        updateCajaInfo();
    }

    private void updateCajaInfo() {
        // Logic:
        // 1. If chkMes is selected -> Hide
        // 2. If range selected (start != end) -> Hide
        // 3. If single day selected (start == end) -> Show for that day
        // 4. If no filter (start/end null) -> Show for Today (Default)

        Date targetDate = null;

        if (chkMes.isSelected()) {
            targetDate = null;
        } else if (startDate.getDate() != null && endDate.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String s = sdf.format(startDate.getDate());
            String e = sdf.format(endDate.getDate());
            if (s.equals(e)) {
                targetDate = startDate.getDate();
            } else {
                targetDate = null;
            }
        } else if (startDate.getDate() == null && endDate.getDate() == null) {
            // Default to today
            targetDate = new Date();
        }

        if (targetDate == null) {
            lblApertura.setText("APERTURA CAJA: -");
            lblCierre.setText("CIERRE CAJA: -");
            return;
        }

        // Query DB
        String sql = "SELECT monto_apertura, monto_cierre FROM caja_apertura_cierre " +
                "WHERE DATE(datetime_apertura) = ? ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, new java.sql.Date(targetDate.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double ap = rs.getDouble("monto_apertura");
                    double ci = rs.getDouble("monto_cierre");
                    // Check if cierre is null (it might be 0.00 if primitive, but in DB it is NULL)
                    // rs.getDouble returns 0.0 if NULL. We should check wasNull.

                    lblApertura.setText("APERTURA CAJA: " + formatNumber(ap));

                    // Check if monto_cierre was actually null in DB
                    // However, getDouble returns 0 if null.
                    // Let's check object to be sure or just assume 0 means not closed if we rely on
                    // logic?
                    // The DB has NULL for datetime_cierre and monto_cierre.
                    // Let's retrieve as Object to check null.
                    Object ciObj = rs.getObject("monto_cierre");
                    if (ciObj == null) {
                        lblCierre.setText("CIERRE CAJA: -");
                    } else {
                        lblCierre.setText("CIERRE CAJA: " + formatNumber(ci));
                    }
                } else {
                    lblApertura.setText("APERTURA CAJA: 0.00");
                    lblCierre.setText("CIERRE CAJA: -");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            lblApertura.setText("APERTURA CAJA: Error");
            lblCierre.setText("CIERRE CAJA: Error");
        }
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("registro_ingresos_comprobantes_"
                + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv"));
        int sel = fc.showSaveDialog(this);
        if (sel != JFileChooser.APPROVE_OPTION)
            return;
        java.io.File f = fc.getSelectedFile();

        // Get all data, not just current page
        java.util.List<Object[]> allData = getAllFilteredData();
        if (allData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try (FileWriter fw = new FileWriter(f)) {
            // header
            fw.write("COMPROBANTE,CLIENTE,FECHA DE ABONO,METODO DE PAGO,MONTO ABONADO\n");
            for (Object[] rowData : allData) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < rowData.length; c++) {
                    Object v = rowData[c];
                    String cell = v == null ? "" : v.toString().replace("\"", "\"\"");
                    // simple CSV quoting
                    if (cell.contains(",") || cell.contains("\n"))
                        cell = '"' + cell + '"';
                    if (c > 0)
                        line.append(',');
                    line.append(cell);
                }
                line.append('\n');
                fw.write(line.toString());
            }
            fw.flush();
            JOptionPane.showMessageDialog(this, "CSV exportado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exportando CSV:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
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
