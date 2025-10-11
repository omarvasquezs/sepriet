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

        // Override comportamiento de "FECHA HOY DÍA" para excluir ANULADO
        setupFechaHoyListener();
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

    /**
     * Configura listener personalizado para "FECHA HOY DÍA" que excluye ANULADO.
     */
    private void setupFechaHoyListener() {
        try {
            // Remover todos los ActionListeners existentes del checkbox
            java.awt.event.ActionListener[] listeners = getChkFechaHoy().getActionListeners();
            for (java.awt.event.ActionListener listener : listeners) {
                getChkFechaHoy().removeActionListener(listener);
            }

            // Agregar nuevo listener que excluye ANULADO
            getChkFechaHoy().addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    if (getChkFechaHoy().isSelected()) {
                        // Establecer fecha de hoy
                        getFilterFecha().setDate(new java.util.Date());

                        // Desmarcar ANULADO (id=3) en estado_comprobante
                        try {
                            for (JCheckBox cb : getEstadoComprobanteItems()) {
                                try {
                                    int id = Integer.parseInt(cb.getActionCommand());
                                    if (id == 3) { // ANULADO
                                        cb.setSelected(false);
                                    }
                                } catch (Exception ignore) {
                                }
                            }
                        } catch (Exception ignore) {
                        }

                        // Ejecutar búsqueda y actualizar estadísticas
                        loadPage(1);
                        updateDateStats();
                    } else {
                        // Si se desmarca, limpiar la fecha y volver a marcar todo
                        getFilterFecha().setDate(null);
                        try {
                            for (JCheckBox cb : getEstadoComprobanteItems()) {
                                cb.setSelected(true);
                            }
                        } catch (Exception ignore) {
                        }
                        // Ejecutar búsqueda y actualizar estadísticas
                        loadPage(1);
                        updateDateStats();
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    // Métodos helper para acceder a componentes protegidos de la clase padre
    private javax.swing.JCheckBox getChkFechaHoy() {
        try {
            java.lang.reflect.Field field = frmConsultarComprobantes.class.getDeclaredField("chkFechaHoy");
            field.setAccessible(true);
            return (javax.swing.JCheckBox) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    private com.toedter.calendar.JDateChooser getFilterFecha() {
        try {
            java.lang.reflect.Field field = frmConsultarComprobantes.class.getDeclaredField("filterFecha");
            field.setAccessible(true);
            return (com.toedter.calendar.JDateChooser) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }

}
