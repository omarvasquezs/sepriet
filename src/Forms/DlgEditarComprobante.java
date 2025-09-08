package Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

/** Simple dialog to edit estado_comprobante, metodo_pago and add a new monto_abonado (incremental) */
public class DlgEditarComprobante extends JDialog {
    private final int comprobanteId;
    private final Runnable onSaved;
    private final JComboBox<Item> cboEstado = new JComboBox<>();
    private final JComboBox<Item> cboEstadoRopa = new JComboBox<>();
    private final JComboBox<Item> cboMetodoPago = new JComboBox<>();
    private final JLabel lblMetodoPago = new JLabel("Método de pago:");
    private final JTextField txtMontoAbonar = new JTextField();
    private final JLabel lblInfo = new JLabel(" ");
    private float costoTotal = 0f;
    private float montoAbonadoPrevio = 0f;
    // original estado at dialog open (used to prevent backward transitions)
    private int originalEstadoComprobanteId = -1;
    private String originalEstadoComprobanteLabel = null;
    private boolean suppressEstadoListener = false;

    record Item(int id, String label){ public String toString(){ return label; }}

    public DlgEditarComprobante(Window owner, int id, Runnable onSaved) {
        super(owner, "Editar Comprobante", ModalityType.APPLICATION_MODAL);
        this.comprobanteId = id;
        this.onSaved = onSaved;
        setSize(420, 300); setResizable(false);
        buildUI();
        loadData();
    }

    private void buildUI(){
    JPanel form = new JPanel(new GridBagLayout()); GridBagConstraints c=new GridBagConstraints(); c.insets=new Insets(6,6,6,6); c.fill=GridBagConstraints.HORIZONTAL; c.weightx=1; int r=0;
    addRow(form,c,r++,new JLabel("ESTADO ROPA :"), cboEstadoRopa);
    addRow(form,c,r++,new JLabel("ESTADO COMPROBANTE :"), cboEstado);
        addRow(form,c,r++, lblMetodoPago, cboMetodoPago);
        addRow(form,c,r++,new JLabel("Monto a abonar (incremental):"), txtMontoAbonar);
        c.gridy=r++; c.gridx=0; c.gridwidth=2; form.add(lblInfo,c);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnGuardar=new JButton("Guardar"); JButton btnCancelar=new JButton("Cancelar");
        buttons.add(btnGuardar); buttons.add(btnCancelar);
    btnGuardar.addActionListener(this::guardar); btnCancelar.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent ev){ dispose(); } });
        getContentPane().setLayout(new BorderLayout()); add(form,BorderLayout.CENTER); add(buttons,BorderLayout.SOUTH);
    }
    private void addRow(JPanel p, GridBagConstraints c, int row, JComponent l, JComponent f){ c.gridy=row; c.gridx=0; c.gridwidth=1; c.weightx=0; p.add(l,c); c.gridx=1; c.weightx=1; p.add(f,c);}    

    private void loadData(){
        try (Connection conn = DatabaseConfig.getConnection()) {
            // load estado_ropa
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, nom_estado_ropa FROM estado_ropa ORDER BY nom_estado_ropa")){
                try(ResultSet rs=ps.executeQuery()){ while(rs.next()) cboEstadoRopa.addItem(new Item(rs.getInt(1), rs.getString(2))); }
            } catch (SQLException ignore) {}
            // load estados comprobante
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, nom_estado FROM estado_comprobantes WHERE habilitado=1")){
                try(ResultSet rs=ps.executeQuery()){ while(rs.next()) cboEstado.addItem(new Item(rs.getInt(1), rs.getString(2))); }
            }
            // load metodos
            cboMetodoPago.addItem(new Item(-1, "(Ninguno/NULL)"));
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, nom_metodo_pago FROM metodo_pago WHERE habilitado=1")){
                try(ResultSet rs=ps.executeQuery()){ while(rs.next()) cboMetodoPago.addItem(new Item(rs.getInt(1), rs.getString(2))); }
            }
            // load current comprobante
            try (PreparedStatement ps = conn.prepareStatement("SELECT estado_comprobante_id, estado_ropa_id, metodo_pago_id, monto_abonado, costo_total FROM comprobantes WHERE id=?")){
                ps.setInt(1, comprobanteId);
                try(ResultSet rs=ps.executeQuery()){ if(rs.next()){ int est=rs.getInt(1); int estR=rs.getInt(2); int mp=rs.getInt(3); montoAbonadoPrevio=rs.getFloat(4); costoTotal=rs.getFloat(5); selectCombo(cboEstado, est); selectCombo(cboEstadoRopa, estR); selectCombo(cboMetodoPago, mp==0? -1: mp); }}
            }
            // remember original estado to enforce allowed transitions
            Object cur = cboEstado.getSelectedItem();
            if (cur instanceof Item) {
                originalEstadoComprobanteId = ((Item) cur).id();
                originalEstadoComprobanteLabel = cur.toString().trim().toUpperCase();
            }
            // react to estado selection changes
            // add an ItemListener to prevent invalid backward transitions
            cboEstado.addItemListener(iEv -> {
                if (iEv.getStateChange() != java.awt.event.ItemEvent.SELECTED)
                    return;
                if (suppressEstadoListener)
                    return;
                Object it = iEv.getItem();
                if (!(it instanceof Item) || originalEstadoComprobanteLabel == null)
                    return;
                String selLabel = it.toString().trim().toUpperCase();
                // ABONO -> cannot go back to DEBE
                if ("ABONO".equals(originalEstadoComprobanteLabel) && "DEBE".equals(selLabel)) {
                    final Object prevMetodo = cboMetodoPago.getSelectedItem();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "No se puede regresar de ABONO a DEBE.", "Validación",
                                JOptionPane.WARNING_MESSAGE);
                        suppressEstadoListener = true;
                        selectCombo(cboEstado, originalEstadoComprobanteId);
                        // restore metodo selection if it still exists
                        try {
                            if (prevMetodo != null) cboMetodoPago.setSelectedItem(prevMetodo);
                        } catch (Exception ignore) {}
                        suppressEstadoListener = false;
                    });
                    return;
                }
        // CANCELADO -> cannot go back to DEBE, ABONO or ANULADO
                if ("CANCELADO".equals(originalEstadoComprobanteLabel)
                        && ("DEBE".equals(selLabel) || "ABONO".equals(selLabel) || "ANULADO".equals(selLabel))) {
                    final Object prevMetodo2 = cboMetodoPago.getSelectedItem();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "No se puede cambiar desde CANCELADO a DEBE, ABONO o ANULADO.", "Validación",
                                JOptionPane.WARNING_MESSAGE);
                        suppressEstadoListener = true;
                        selectCombo(cboEstado, originalEstadoComprobanteId);
                        try {
                            if (prevMetodo2 != null) cboMetodoPago.setSelectedItem(prevMetodo2);
                        } catch (Exception ignore) {}
                        suppressEstadoListener = false;
                    });
                    return;
                }
            });
            // regular action listener to update UI elements when selection changes
            cboEstado.addActionListener(this::applyEstadoRules);
            // apply rules once now so the UI matches the current estado when dialog opens
            applyEstadoRules(null);
            updateInfo();
        } catch (Exception ex){ JOptionPane.showMessageDialog(this,"Error cargando datos:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}    
    }

    private void selectCombo(JComboBox<Item> combo, int id){ for(int i=0;i<combo.getItemCount();i++){ if(combo.getItemAt(i).id()==id){ combo.setSelectedIndex(i); return; }} }
    private void updateInfo(){ lblInfo.setText(String.format("Abonado previo: %.2f  |  Total: %.2f  |  Deuda: %.2f", montoAbonadoPrevio, costoTotal, (costoTotal - montoAbonadoPrevio))); }

    private void applyEstadoRules(java.awt.event.ActionEvent ev){
        Object sel = cboEstado.getSelectedItem(); if (sel==null) return;
        String label = sel.toString().trim().toUpperCase();
        float deuda = costoTotal - montoAbonadoPrevio;
        switch(label){
            case "ABONO":
                txtMontoAbonar.setEnabled(true);
                txtMontoAbonar.setText("");
                // show metodo
                lblMetodoPago.setVisible(true);
                cboMetodoPago.setVisible(true);
                cboMetodoPago.setEnabled(true);
                break;
            case "CANCELADO":
                // set monto to remaining and disable edits
                txtMontoAbonar.setEnabled(false);
                txtMontoAbonar.setText(String.format("%.2f", Math.max(0f, deuda)));
                // show metodo
                lblMetodoPago.setVisible(true);
                cboMetodoPago.setVisible(true);
                cboMetodoPago.setEnabled(true);
                break;
            case "DEBE":
            case "ANULADO":
                // hide metodo and reset selection
                txtMontoAbonar.setEnabled(false);
                txtMontoAbonar.setText("");
                lblMetodoPago.setVisible(false);
                cboMetodoPago.setVisible(false);
                cboMetodoPago.setSelectedIndex(0);
                break;
            default:
                txtMontoAbonar.setEnabled(true);
                lblMetodoPago.setVisible(true);
                cboMetodoPago.setVisible(true);
                cboMetodoPago.setEnabled(true);
                break;
        }
        // refresh UI to account for visibility changes
        SwingUtilities.invokeLater(() -> { lblMetodoPago.revalidate(); cboMetodoPago.revalidate(); this.revalidate(); this.repaint(); });
        updateInfo();
    }

    private void guardar(ActionEvent e){
        String montoTxt = txtMontoAbonar.getText().trim();
        float montoNuevo = 0f;
        if(!montoTxt.isEmpty()){
            try { montoNuevo = Float.parseFloat(montoTxt); if (montoNuevo < 0) throw new NumberFormatException(); } catch(NumberFormatException ex){ JOptionPane.showMessageDialog(this,"Monto inválido","Validación",JOptionPane.WARNING_MESSAGE); return; }
        }
        Item est = (Item) cboEstado.getSelectedItem(); Item mp = (Item) cboMetodoPago.getSelectedItem();
        float abonadoActualizado;
        String estLabel = est==null?"":est.label().toUpperCase();
        if ("CANCELADO".equals(estLabel)) {
            abonadoActualizado = costoTotal; // ensure full paid
            montoNuevo = Math.max(0f, costoTotal - montoAbonadoPrevio);
        } else if ("DEBE".equals(estLabel)) {
            abonadoActualizado = 0f;
            montoNuevo = 0f;
        } else {
            abonadoActualizado = montoAbonadoPrevio + montoNuevo;
        }
        if (abonadoActualizado > costoTotal + 0.001f) { JOptionPane.showMessageDialog(this,"El abono excede el total.","Validación",JOptionPane.WARNING_MESSAGE); return; }
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            // Update comprobante
            try (PreparedStatement ps = conn.prepareStatement("UPDATE comprobantes SET estado_comprobante_id=?, metodo_pago_id=?, monto_abonado=? WHERE id=?")){
                ps.setInt(1, est.id());
                if (mp == null || mp.id() == -1) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, mp.id());
                ps.setFloat(3, abonadoActualizado);
                ps.setInt(4, comprobanteId);
                ps.executeUpdate();
            }
            if (montoNuevo > 0) {
                // Insert ingreso incremental
                String cod = null;
                try (PreparedStatement ps = conn.prepareStatement("SELECT cod_comprobante, cliente_id, costo_total FROM comprobantes WHERE id=?")){
                    ps.setInt(1, comprobanteId); try(ResultSet rs=ps.executeQuery()){ if(rs.next()){ cod=rs.getString(1); int cliente=rs.getInt(2); float costo=rs.getFloat(3);
                        try (PreparedStatement ins = conn.prepareStatement("INSERT INTO reporte_ingresos (cod_comprobante, cliente_id, metodo_pago_id, fecha, monto_abonado, costo_total) VALUES (?,?,?,?,?,?)")){
                            ins.setString(1, cod); ins.setInt(2, cliente); if(mp==null || mp.id()==-1) ins.setNull(3, java.sql.Types.INTEGER); else ins.setInt(3, mp.id());
                            ins.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                            ins.setFloat(5, montoNuevo); ins.setFloat(6, costo); ins.executeUpdate(); }
                    }} }
            }
            conn.commit();
            JOptionPane.showMessageDialog(this,"Guardado.");
            if (onSaved != null) onSaved.run();
            dispose();
        } catch (Exception ex){ JOptionPane.showMessageDialog(this,"Error guardando:\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}    
    }
}
