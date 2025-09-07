package Forms;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal frame for CRUD of users (username, password, role, habilitado).
 */
public class frmUsuarios extends JInternalFrame {
    private final JTextField txtSearch = new JTextField();
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnReset = new JButton("Resetear");
    private final JButton btnAdd = new JButton("Añadir");
    private final JButton btnEdit = new JButton("Editar");
    private final JButton btnDelete = new JButton("Eliminar");
    private final JTable table = new JTable();
    private final UsuariosTableModel model = new UsuariosTableModel();
    private int currentPage = 1;
    private int totalPages = 1;
    private int pageSize = 50;

    // helper types
    private static class RoleItem {
        int id;
        String name;

        RoleItem(int i, String n) {
            id = i;
            name = n;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class UserRow {
        int id;
        String username;
        String role;
        int habilitado;
    }

    private static class UsuariosTableModel extends AbstractTableModel {
        private final String[] cols = { "USUARIO", "ROL", "ESTADO" };
        private java.util.List<UserRow> rows = new java.util.ArrayList<>();

        public void setRows(java.util.List<UserRow> data) {
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
            UserRow row = rows.get(r);
            if (c == 0)
                return row.username == null ? null : row.username.toUpperCase();
            else if (c == 1)
                return row.role;
            else if (c == 2)
                return row.habilitado == 1 ? "HABILITADO" : "DESHABILITADO";
            else
                return null;
        }
    }

    public frmUsuarios() {
        super("Usuarios", true, true, true, true);
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
        txtSearch.setPreferredSize(new Dimension(200, 24));
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
                onBuscar(e);
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
                openUserDialog(null);
            }
        });
        btnEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Integer id = getSelectedId();
                if (id != null)
                    openUserDialog(id);
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
                        openUserDialog(id);
                    }
                }
            }
        });

        // bottom pagination
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton btnPrev = new JButton("<");
        final JButton btnNext = new JButton(">");
        final JLabel lblPagina = new JLabel("Página 1 de 1");
        final JComboBox<Integer> pageSizeCombo = new JComboBox<>(new Integer[] { 10, 25, 50, 100 });
        pageSizeCombo.setSelectedItem(Integer.valueOf(pageSize));

        btnPrev.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (currentPage > 1)
                    loadPage(currentPage - 1);
            }
        });
        btnNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (currentPage < totalPages)
                    loadPage(currentPage + 1);
            }
        });
        pageSizeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Integer sel = (Integer) pageSizeCombo.getSelectedItem();
                if (sel != null && sel != pageSize) {
                    pageSize = sel;
                    loadPage(1);
                }
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

        // store label update via property
        table.putClientProperty("pageLabel", lblPagina);
    }

    private void onBuscar(ActionEvent ev) {
        loadPage(1);
    }

    private Integer getSelectedId() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        int modelRow = table.convertRowIndexToModel(row);
        return model.rows.get(modelRow).id;
    }

    private void deleteSelected() {
        Integer id = getSelectedId();
        if (id == null)
            return;
        int conf = JOptionPane.showConfirmDialog(this, "¿Eliminar el usuario seleccionado?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION)
            return;
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            loadPage(currentPage);
            JOptionPane.showMessageDialog(this, "Eliminado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error eliminando:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openUserDialog(Integer id) {
        // id == null -> add, otherwise edit
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                id == null ? "Añadir usuario" : "Editar usuario", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Usuario:"), c);
        JTextField txtUser = new JTextField();
        txtUser.setColumns(30);
        // Force uppercase while typing
        try {
            ((AbstractDocument) txtUser.getDocument()).setDocumentFilter(new DocumentFilter() {
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
        p.add(txtUser, c);
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Contraseña:"), c);
        JPasswordField txtPass = new JPasswordField();
        txtPass.setColumns(30);
        c.gridx = 1;
        p.add(txtPass, c);
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Rol:"), c);
        JComboBox<RoleItem> cbRoles = new JComboBox<>();
        c.gridx = 1;
        p.add(cbRoles, c);
        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Estado:"), c);
        JCheckBox chk = new JCheckBox("Habilitado");
        chk.setSelected(true);
        c.gridx = 1;
        p.add(chk, c);

        // load roles
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT id, role_name FROM roles WHERE habilitado=1 ORDER BY role_name");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cbRoles.addItem(new RoleItem(rs.getInt(1), rs.getString(2)));
            }
        } catch (Exception ignore) {
        }

        if (id != null) {
            // load values
            try (Connection conn = DatabaseConfig.getConnection();
                    PreparedStatement ps = conn
                            .prepareStatement("SELECT username, role_id, habilitado FROM users WHERE id=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        txtUser.setText(rs.getString("username"));
                        txtUser.setText(txtUser.getText().toUpperCase());
                        int rid = rs.getInt("role_id");
                        for (int i = 0; i < cbRoles.getItemCount(); i++)
                            if (cbRoles.getItemAt(i).id == rid)
                                cbRoles.setSelectedIndex(i);
                        chk.setSelected(rs.getInt("habilitado") != 0);
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error cargando usuario:\n" + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        buttons.add(btnSave);
        buttons.add(btnCancel);

        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String user = txtUser.getText().trim().toUpperCase();
                String pass = new String(txtPass.getPassword());
                RoleItem sel = (RoleItem) cbRoles.getSelectedItem();
                int habil = chk.isSelected() ? 1 : 0;
                if (user.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Usuario requerido.");
                    return;
                }
                try (Connection conn = DatabaseConfig.getConnection()) {
                    if (id == null) {
                        if (pass.isEmpty()) {
                            JOptionPane.showMessageDialog(dlg, "Contraseña requerida.");
                            return;
                        }
                        String hashed = hashPassword(pass);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO users(username,password,role_id,habilitado) VALUES(?,?,?,?)")) {
                            ps.setString(1, user);
                            ps.setString(2, hashed);
                            ps.setObject(3, sel == null ? null : sel.id);
                            ps.setInt(4, habil);
                            ps.executeUpdate();
                        }
                    } else {
                        if (pass.isEmpty()) {
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "UPDATE users SET username=?, role_id=?, habilitado=? WHERE id=?")) {
                                ps.setString(1, user);
                                ps.setObject(2, sel == null ? null : sel.id);
                                ps.setInt(3, habil);
                                ps.setInt(4, id);
                                ps.executeUpdate();
                            }
                        } else {
                            String hashed = hashPassword(pass);
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "UPDATE users SET username=?, password=?, role_id=?, habilitado=? WHERE id=?")) {
                                ps.setString(1, user);
                                ps.setString(2, hashed);
                                ps.setObject(3, sel == null ? null : sel.id);
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
        dlg.getContentPane().add(buttons, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void loadPage(int page) {
        model.setRows(new ArrayList<>());
        String where = " WHERE 1=1 ";
        List<Object> params = new ArrayList<>();
        String q = txtSearch.getText().trim();
        if (!q.isEmpty()) {
            where += " AND u.username LIKE ? ";
            params.add('%' + q + '%');
        }
        String countSql = "SELECT COUNT(*) FROM users u LEFT JOIN roles r ON u.role_id=r.id" + where;
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
            String sql = "SELECT u.id,u.username,r.role_name, u.habilitado FROM users u LEFT JOIN roles r ON u.role_id=r.id"
                    + where + " ORDER BY u.id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Object p : params)
                    ps.setObject(idx++, p);
                ps.setInt(idx++, pageSize);
                ps.setInt(idx, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    List<UserRow> rows = new ArrayList<>();
                    while (rs.next()) {
                        UserRow ur = new UserRow();
                        ur.id = rs.getInt(1);
                        ur.username = rs.getString(2);
                        ur.role = rs.getString(3);
                        ur.habilitado = rs.getInt(4);
                        rows.add(ur);
                    }
                    model.setRows(rows);
                }
            }
            JLabel lbl = (JLabel) table.getClientProperty("pageLabel");
            if (lbl != null)
                lbl.setText("Página " + currentPage + " de " + totalPages);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando usuarios:\n" + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String hashPassword(String plain) throws Exception {
        // md5(sha1(password))
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha = sha1.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : sha)
            sb.append(String.format("%02x", b));
        String shaHex = sb.toString();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] md = md5.digest(shaHex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb2 = new StringBuilder();
        for (byte b : md)
            sb2.append(String.format("%02x", b));
        return sb2.toString();
    }
}
