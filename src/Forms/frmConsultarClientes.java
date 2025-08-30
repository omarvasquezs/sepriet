package Forms;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class frmConsultarClientes extends JInternalFrame {
    private final JTable table = new JTable();
    private final ClientesTableModel model = new ClientesTableModel();
    private final JButton btnAdd = new JButton("Añadir");
    private final JButton btnEdit = new JButton("Editar");
    private final JButton btnDelete = new JButton("Eliminar");
    // filter and pagination
    private final JTextField txtFilter = new JTextField();
    private int pageSize = 50;
    private final JComboBox<Integer> pageSizeCombo = new JComboBox<>(new Integer[] { 10, 25, 50, 100, 200 });
    private final JButton btnPrev = new JButton("<");
    private final JButton btnNext = new JButton(">");
    private final JLabel lblPagina = new JLabel("Página 1 de 1");
    private int currentPage = 1;
    private int totalPages = 1;

    public frmConsultarClientes() {
        super("Clientes", true, true, true, true);
        // Set internal frame icon
        java.net.URL iconUrl = getClass().getResource("/Forms/icon.png");
        if (iconUrl != null) {
            setFrameIcon(new ImageIcon(iconUrl));
        }
        buildUI();
        setSize(800, 420);
        loadClients();
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
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        tools.add(btnAdd);
        tools.add(btnEdit);
        tools.add(btnDelete);
        top.add(tools);

        // filter row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        filterRow.add(new JLabel("Filtro:"));
        txtFilter.setPreferredSize(new Dimension(260, 24));
        filterRow.add(txtFilter);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnReset = new JButton("Resetear");
        filterRow.add(btnBuscar);
        filterRow.add(btnReset);
        top.add(filterRow);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);

        table.setModel(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnAdd.addActionListener(this::onAdd);
        btnEdit.addActionListener(this::onEdit);
        btnDelete.addActionListener(this::onDelete);

        // search actions
        btnBuscar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadClients(1);
            }
        });
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFilter.setText("");
                loadClients(1);
            }
        });

        table.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent ev) {
                boolean sel = table.getSelectedRow() >= 0;
                btnEdit.setEnabled(sel);
                btnDelete.setEnabled(sel);
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        // bottom pagination bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pageSizeCombo.setSelectedItem(Integer.valueOf(pageSize));
        pageSizeCombo.setMaximumRowCount(6);
        pageSizeCombo.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Integer sel = (Integer) pageSizeCombo.getSelectedItem();
                if (sel != null && sel != pageSize) {
                    pageSize = sel;
                    loadClients(1);
                }
            }
        });
        bottom.add(new JLabel("Registros por página:"));
        bottom.add(pageSizeCombo);
        bottom.add(btnPrev);
        bottom.add(btnNext);
        bottom.add(lblPagina);
        getContentPane().add(bottom, BorderLayout.SOUTH);

        // pagination listeners
        btnPrev.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (currentPage > 1)
                    loadClients(currentPage - 1);
            }
        });
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (currentPage < totalPages)
                    loadClients(currentPage + 1);
            }
        });
    }

    private void onAdd(ActionEvent ev) {
        Window w = SwingUtilities.getWindowAncestor(this);
        DlgNuevoCliente dlg = new DlgNuevoCliente(w);
        dlg.setLocationRelativeTo(w);
        dlg.setVisible(true);
        // reload after dialog closes
        loadClients(currentPage);
    }

    private Integer getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        int modelRow = table.convertRowIndexToModel(row);
        return model.rows.get(modelRow).id;
    }

    private void onEdit(ActionEvent ev) {
        Integer id = getSelectedId();
        if (id == null)
            return;
        Window w = SwingUtilities.getWindowAncestor(this);
        DlgEditarCliente dlg = new DlgEditarCliente(w, id, this::loadClients);
        dlg.setLocationRelativeTo(w);
        dlg.setVisible(true);
    }

    private void onDelete(ActionEvent ev) {
        Integer id = getSelectedId();
        if (id == null)
            return;
        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar el cliente seleccionado?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION)
            return;
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM clientes WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            loadClients(currentPage);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error eliminando cliente:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // paged load with optional filter
    public void loadClients(int page) {
        List<ClienteRow> rows = new ArrayList<>();
        String filter = txtFilter.getText().trim();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String where = " WHERE 1=1 ";
            List<Object> params = new ArrayList<>();
            if (!filter.isEmpty()) {
                where += " AND (nombres LIKE ? OR dni LIKE ?) ";
                params.add('%' + filter + '%');
                params.add('%' + filter + '%');
            }
            String sqlCount = "SELECT COUNT(*) FROM clientes" + where;
            try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                for (int i = 0; i < params.size(); i++)
                    ps.setObject(i + 1, params.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt(1);
                        totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
                    }
                }
            }
            currentPage = Math.min(Math.max(1, page), totalPages);
            int offset = (currentPage - 1) * pageSize;
            String sql = "SELECT id,nombres,dni,telefono,email FROM clientes " + where
                    + " ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object p : params)
                    ps.setObject(idx++, p);
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ClienteRow r = new ClienteRow();
                        r.id = rs.getInt(1);
                        r.nombres = rs.getString(2);
                        r.dni = rs.getString(3);
                        r.telefono = rs.getString(4);
                        r.email = rs.getString(5);
                        rows.add(r);
                    }
                }
            }
        } catch (Exception ex) {
            // non-fatal
        }
        model.setRows(rows);
        lblPagina.setText("Página " + currentPage + " de " + totalPages);
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }

    // convenience wrapper
    public void loadClients() {
        loadClients(1);
    }

    private static class ClienteRow {
        int id;
        String nombres;
        String dni;
        String telefono;
        String email;
    }

    private static class ClientesTableModel extends AbstractTableModel {
        private final String[] cols = { "ID", "NOMBRES", "DNI", "TELÉFONO", "EMAIL" };
        private List<ClienteRow> rows = new ArrayList<>();

        public void setRows(List<ClienteRow> data) {
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
            ClienteRow row = rows.get(r);
            return switch (c) {
                case 0 -> row.id;
                case 1 -> row.nombres;
                case 2 -> row.dni;
                case 3 -> row.telefono;
                case 4 -> row.email;
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return c == 0 ? Integer.class : String.class;
        }
    }
}
