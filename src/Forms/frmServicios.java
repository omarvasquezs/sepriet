package Forms;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal frame for CRUD of servicios (nom_servicio, precio_kilo, habilitado).
 */
public class frmServicios extends JInternalFrame {
    private final JTextField txtSearch = new JTextField();
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
    private final JButton btnAdd = new JButton("Añadir");
    private final JButton btnEdit = new JButton("Editar");
    private final JButton btnDelete = new JButton("Eliminar");
    private final JTable table = new JTable();
    private final ServiciosTableModel model = new ServiciosTableModel();
    private int currentPage = 1;
    private int totalPages = 1;
    private int pageSize = 50;

    public frmServicios() {
        super("Servicios", true, true, true, true);
        buildUI();
        java.net.URL iconUrl = getClass().getResource("/Forms/icon.png");
        if (iconUrl != null) {
            setFrameIcon(new ImageIcon(iconUrl));
        }
        setSize(900, 520);
        loadPage(1);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            try {
                setMaximum(true);
            } catch (Exception ignored) {
            }
        });
    }

    private void buildUI() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtSearch.setPreferredSize(new Dimension(300, 24));
        top.add(new JLabel("Buscar: "));
        top.add(txtSearch);
        top.add(btnBuscar);
        top.add(btnReset);

        JPanel crudBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        crudBar.add(btnAdd);
        crudBar.add(btnEdit);
        crudBar.add(btnDelete);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);

        btnBuscar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadPage(1);
            }
        });
        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                txtSearch.setText("");
                loadPage(1);
            }
        });
        btnAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openDialog(null);
            }
        });
        btnEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Integer id = getSelectedId();
                if (id != null)
                    openDialog(id);
            }
        });
        btnDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteSelected();
            }
        });

        table.setModel(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
                boolean sel = table.getSelectedRow() >= 0;
                btnEdit.setEnabled(sel);
                btnDelete.setEnabled(sel);
            }
        });

        // Open edit dialog on double-click of any row
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Integer id = getSelectedId();
                    if (id != null) {
                        openDialog(id);
                    }
                }
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton btnPrev = new JButton("<");
        final JButton btnNext = new JButton(">");
        final JLabel lblPagina = new JLabel("Página 1 de 1");
        final JComboBox<Integer> pageSizeCombo = new JComboBox<>(new Integer[] { 10, 25, 50, 100 });
        pageSizeCombo.setSelectedItem(Integer.valueOf(pageSize));
        btnPrev.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 1)
                    loadPage(currentPage - 1);
            }
        });
        btnNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentPage < totalPages)
                    loadPage(currentPage + 1);
            }
        });
        pageSizeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Integer sel = (Integer) pageSizeCombo.getSelectedItem();
                if (sel != null && sel != pageSize) {
                    pageSize = sel;
                    loadPage(1);
                }
            }
        });

        bottom.add(new JLabel("Mostrar"));
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
        table.putClientProperty("pageLabel", lblPagina);
    }

    private Integer getSelectedId() {
        int r = table.getSelectedRow();
        if (r < 0)
            return null;
        int mr = table.convertRowIndexToModel(r);
        return model.rows.get(mr).id;
    }

    private void deleteSelected() {
        Integer id = getSelectedId();
        if (id == null)
            return;
        int c = JOptionPane.showConfirmDialog(this, "¿Eliminar el servicio?", "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (c != JOptionPane.YES_OPTION)
            return;
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM servicios WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            loadPage(currentPage);
            JOptionPane.showMessageDialog(this, "Eliminado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDialog(Integer id) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                id == null ? "Añadir SERVICIO" : "Editar SERVICIO", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Servicio:"), c);
        JTextField txtNom = new JTextField();
        txtNom.setColumns(30);
        // Force uppercase while typing
        try {
            ((AbstractDocument) txtNom.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    if (string != null)
                        string = string.toUpperCase(java.util.Locale.getDefault());
                    super.insertString(fb, offset, string, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (text != null)
                        text = text.toUpperCase(java.util.Locale.getDefault());
                    super.replace(fb, offset, length, text, attrs);
                }
            });
        } catch (Exception ignored) {
        }
        c.gridx = 1;
        p.add(txtNom, c);
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Precio por kilo (S/.):"), c);
        JTextField txtPrecio = new JTextField();
        txtPrecio.setColumns(12);
        c.gridx = 1;
        p.add(txtPrecio, c);
        c.gridx = 0;
        c.gridy++;
        // Tipo de servicio (k/s/p)
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Tipo de servicio:"), c);
        JComboBox<String> cboTipo = new JComboBox<>(new String[] { "AL KILO", "AL SECO", "POR PIEZA" });
        c.gridx = 1;
        p.add(cboTipo, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Estado:"), c);
        JCheckBox chk = new JCheckBox("Habilitado");
        chk.setSelected(true);
        c.gridx = 1;
        p.add(chk, c);

        if (id != null) {
            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT nom_servicio, tipo_servicio, precio_kilo, habilitado FROM servicios WHERE id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        txtNom.setText(rs.getString(1));
                        String tipo = rs.getString(2);
                        if (tipo != null) {
                            if ("k".equalsIgnoreCase(tipo))
                                cboTipo.setSelectedIndex(0);
                            else if ("s".equalsIgnoreCase(tipo))
                                cboTipo.setSelectedIndex(1);
                            else if ("p".equalsIgnoreCase(tipo))
                                cboTipo.setSelectedIndex(2);
                        }
                        txtPrecio.setText(rs.getString(3));
                        chk.setSelected(rs.getInt(4) != 0);
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error cargando:\n" + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        btns.add(btnSave);
        btns.add(btnCancel);
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String nom = txtNom.getText().trim();
                String precio = txtPrecio.getText().trim();
                int habil = chk.isSelected() ? 1 : 0;
                // map selected index to storage code: 0->k, 1->s, 2->p
                String tipoSel;
                int tipoIndex = cboTipo.getSelectedIndex();
                if (tipoIndex == 0)
                    tipoSel = "k";
                else if (tipoIndex == 1)
                    tipoSel = "s";
                else if (tipoIndex == 2)
                    tipoSel = "p";
                else
                    tipoSel = "k"; // fallback
                if (nom.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Servicio requerido.");
                    return;
                }
                try (Connection conn = DatabaseConfig.getConnection()) {
                    if (id == null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO servicios(nom_servicio,tipo_servicio,precio_kilo,habilitado) VALUES(?,?,?,?)")) {
                            ps.setString(1, nom);
                            ps.setString(2, tipoSel == null ? "k" : tipoSel);
                            if (precio.isEmpty())
                                ps.setObject(3, null);
                            else
                                ps.setFloat(3, Float.parseFloat(precio));
                            ps.setInt(4, habil);
                            ps.executeUpdate();
                        }
                    } else {
                        if (precio.isEmpty()) {
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "UPDATE servicios SET nom_servicio=?, tipo_servicio=?, precio_kilo=NULL, habilitado=? WHERE id=?")) {
                                ps.setString(1, nom);
                                ps.setString(2, tipoSel == null ? "k" : tipoSel);
                                ps.setInt(3, habil);
                                ps.setInt(4, id);
                                ps.executeUpdate();
                            }
                        } else {
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "UPDATE servicios SET nom_servicio=?, tipo_servicio=?, precio_kilo=?, habilitado=? WHERE id=?")) {
                                ps.setString(1, nom);
                                ps.setString(2, tipoSel == null ? "k" : tipoSel);
                                ps.setFloat(3, Float.parseFloat(precio));
                                ps.setInt(4, habil);
                                ps.setInt(5, id);
                                ps.executeUpdate();
                            }
                        }
                    }
                    dlg.dispose();
                    loadPage(currentPage);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Error guardando:\n" + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(p, BorderLayout.CENTER);
        dlg.getContentPane().add(btns, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private static class ServiceRow {
        int id;
        String nom;
        String tipo; // 'k','s','p'
        float precio;
        int habil;
    }

    private static class ServiciosTableModel extends AbstractTableModel {
        private final String[] cols = { "SERVICIO", "TIPO", "PRECIO POR KILO (S/.)", "ESTADO" };
        private java.util.List<ServiceRow> rows = new java.util.ArrayList<>();

        public void setRows(java.util.List<ServiceRow> data) {
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
            ServiceRow s = rows.get(r);
            if (c == 0)
                return s.nom;
            else if (c == 1) {
                // map tipo code to human label
                if (s.tipo == null)
                    return "";
                switch (s.tipo) {
                    case "k":
                        return "AL KILO";
                    case "s":
                        return "AL SECO";
                    case "p":
                        return "POR PIEZA";
                    default:
                        return s.tipo;
                }
            } else if (c == 2)
                return s.precio;
            else if (c == 3)
                return s.habil == 1 ? "HABILITADO" : "DESHABILITADO";
            else
                return null;
        }
    }

    private void loadPage(int page) {
        model.setRows(new ArrayList<>());
        String where = " WHERE 1=1 ";
        List<Object> params = new ArrayList<>();
        String q = txtSearch.getText().trim();
        if (!q.isEmpty()) {
            where += " AND nom_servicio LIKE ? ";
            params.add('%' + q + '%');
        }
        String countSql = "SELECT COUNT(*) FROM servicios" + where;
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement pc = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++)
                pc.setObject(i + 1, params.get(i));
            try (ResultSet rc = pc.executeQuery()) {
                int total = 0;
                if (rc.next())
                    total = rc.getInt(1);
                totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
            }
            currentPage = Math.min(page, totalPages);
            int offset = (currentPage - 1) * pageSize;
            String sql = "SELECT id, nom_servicio, tipo_servicio, precio_kilo, habilitado FROM servicios" + where
                    + " ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object p : params)
                    ps.setObject(idx++, p);
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    java.util.List<ServiceRow> list = new java.util.ArrayList<>();
                    while (rs.next()) {
                        ServiceRow s = new ServiceRow();
                        s.id = rs.getInt(1);
                        s.nom = rs.getString(2);
                        s.tipo = rs.getString(3);
                        s.precio = rs.getFloat(4);
                        s.habil = rs.getInt(5);
                        list.add(s);
                    }
                    model.setRows(list);
                }
            }
            JLabel lbl = (JLabel) table.getClientProperty("pageLabel");
            if (lbl != null)
                lbl.setText("Página " + currentPage + " de " + totalPages);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando servicios:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
