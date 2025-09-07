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
            // estado_comprobante: only CANCELADO (4) and lock it
            for (JCheckBox cb : getEstadoComprobanteItems()) {
                try {
                    int id = Integer.parseInt(cb.getActionCommand());
                    if (id == 4) {
                        cb.setVisible(true);
                        cb.setEnabled(false);
                        cb.setSelected(true);
                    } else {
                        cb.setVisible(false);
                        cb.setSelected(false);
                        cb.setEnabled(false);
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }
    }
}
