package Forms;

import javax.swing.JCheckBox;

/**
 * Specialized internal frame for "Comprobantes YA Cancelados".
 * It hides all other estado comprobante checkboxes and leaves only CANCELADO
 * visible and locked (not editable).
 */
public class frmConsultarComprobantesCancelados extends frmConsultarComprobantes {

    public frmConsultarComprobantesCancelados() {
        super(Mode.CANCELADOS);
        setTitle("Comprobantes YA Cancelados");
    }

    @Override
    protected void postPopulateEstadoItems() {
        for (JCheckBox cb : getEstadoComprobanteItems()) {
            try {
                if (cb.getText() != null && cb.getText().trim().equalsIgnoreCase("CANCELADO")) {
                    cb.setSelected(true);
                    cb.setEnabled(false); // lock it
                    cb.setVisible(true);
                } else {
                    cb.setSelected(false);
                    cb.setEnabled(false);
                    cb.setVisible(false);
                }
            } catch (Exception ignore) {
            }
        }
    }
}
