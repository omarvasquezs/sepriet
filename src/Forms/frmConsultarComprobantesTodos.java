package Forms;

import javax.swing.JCheckBox;

/**
 * Specialized internal frame for "Comprobantes TODOS" (no prefilters).
 * Ensures all estado checkboxes are visible, enabled and selected by default.
 */
public class frmConsultarComprobantesTodos extends frmConsultarComprobantes {

    public frmConsultarComprobantesTodos() {
        super(Mode.TODOS);
        setTitle("Comprobantes - Todos");
    }

    @Override
    protected void postPopulateEstadoItems() {
        // Ensure estado_ropa checkboxes are visible, enabled and selected
        try {
            for (JCheckBox cb : getEstadoRopaItems()) {
                cb.setVisible(true);
                cb.setEnabled(true);
                cb.setSelected(true);
            }
        } catch (Exception ignore) {
        }
        // Ensure estado_comprobante checkboxes are visible, enabled and selected
        try {
            for (JCheckBox cb : getEstadoComprobanteItems()) {
                cb.setVisible(true);
                cb.setEnabled(true);
                cb.setSelected(true);
            }
        } catch (Exception ignore) {
        }
    }

}
