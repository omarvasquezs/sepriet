package Forms;

import javax.swing.JCheckBox;

/**
 * Specialized internal frame for "Comprobantes NO Cancelados".
 * It hides/unchecks the "CANCELADO" checkbox in Estado Comprobante group.
 */
public class frmConsultarComprobantesNoCancelados extends frmConsultarComprobantes {

    public frmConsultarComprobantesNoCancelados() {
        super(Mode.RECIBIDOS);
        setTitle("Comprobantes NO Cancelados");
    }

    @Override
    protected void postPopulateEstadoItems() {
        // After the base populates the checkbox lists, remove or uncheck CANCELADO
        for (JCheckBox cb : getEstadoComprobanteItems()) {
            try {
                if (cb.getText() != null && cb.getText().trim().equalsIgnoreCase("CANCELADO")) {
                    // uncheck and remove from UI
                    cb.setSelected(false);
                    cb.setVisible(false);
                } else {
                    cb.setSelected(true);
                    cb.setVisible(true);
                }
            } catch (Exception ignore) {
            }
        }
    }
}
