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
        try {
            // estado_ropa: keep only ids 1 and 3
            for (JCheckBox cb : getEstadoRopaItems()) {
                try {
                    int id = Integer.parseInt(cb.getActionCommand());
                    if (id == 1 || id == 3) {
                        cb.setVisible(true);
                        cb.setEnabled(true);
                        cb.setSelected(true);
                    } else {
                        cb.setVisible(false);
                        cb.setSelected(false);
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }
        try {
            // estado_comprobante: keep only DEBE(1) and ABONO(2)
            for (JCheckBox cb : getEstadoComprobanteItems()) {
                try {
                    int id = Integer.parseInt(cb.getActionCommand());
                    if (id == 1 || id == 2) {
                        cb.setVisible(true);
                        cb.setEnabled(true);
                        cb.setSelected(true);
                    } else {
                        cb.setVisible(false);
                        cb.setSelected(false);
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }
    }
}
