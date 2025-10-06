package Forms;

import javax.swing.JCheckBox;

/**
 * Consulta de comprobantes con filtro específico:
 * - Estado Ropa: RECOGIDO (id=4) - SIEMPRE SELECCIONADO Y BLOQUEADO
 * - Estado Comprobante: CANCELADO (id=4) - SIEMPRE SELECCIONADO Y BLOQUEADO
 * Los demás checkboxes se ocultan completamente
 */
public class frmConsultarComprobantesRecogidosCancelados extends frmConsultarComprobantes {

    public frmConsultarComprobantesRecogidosCancelados() {
        // Usar Mode.TODOS para evitar los pre-filtros del modo DEFAULT
        // (DEFAULT excluye RECOGIDO id=4 y por eso no devolvía resultados)
        super(Mode.TODOS);
        setTitle("Comprobantes RECOGIDOS y CANCELADOS");

        // La búsqueda automática ahora se ejecuta desde postPopulateEstadoItems()
        // después de que los checkboxes estén correctamente configurados
    }

    @Override
    protected void postPopulateEstadoItems() {
        // Usar try-catch para evitar errores si los checkboxes no están listos
        try {
            // Primero desmarcar TODOS los checkboxes
            for (JCheckBox cb : getEstadoRopaItems()) {
                cb.setSelected(false);
            }
            for (JCheckBox cb : getEstadoComprobanteItems()) {
                cb.setSelected(false);
            }

            // Marcar y bloquear SOLO RECOGIDO, ocultar los demás
            for (JCheckBox cb : getEstadoRopaItems()) {
                String text = cb.getText().toUpperCase();
                if (text.contains("RECOGIDO")) {
                    cb.setSelected(true);
                    cb.setEnabled(false); // Bloqueado, no se puede desmarcar
                    cb.setVisible(true);
                } else {
                    // Ocultar todos los demás checkboxes de estado_ropa
                    cb.setVisible(false);
                }
            }

            // Marcar y bloquear SOLO CANCELADO, ocultar los demás
            for (JCheckBox cb : getEstadoComprobanteItems()) {
                String text = cb.getText().toUpperCase();
                if (text.contains("CANCELADO")) {
                    cb.setSelected(true);
                    cb.setEnabled(false); // Bloqueado, no se puede desmarcar
                    cb.setVisible(true);
                } else {
                    // Ocultar todos los demás checkboxes de estado_comprobante
                    cb.setVisible(false);
                }
            }
        } catch (Exception ignore) {
        }

        // Después de configurar los checkboxes, ejecutar la búsqueda automáticamente
        javax.swing.SwingUtilities.invokeLater(() -> {
            loadPage(1);
        });
    }

    @Override
    protected void resetFilters() {
        // Llamar al reseteo normal primero
        super.resetFilters();

        // Usar try-catch para evitar errores
        try {
            // Desmarcar TODOS excepto RECOGIDO y CANCELADO
            for (JCheckBox cb : getEstadoRopaItems()) {
                String text = cb.getText().toUpperCase();
                if (!text.contains("RECOGIDO")) {
                    cb.setSelected(false);
                } else {
                    cb.setSelected(true);
                }
            }

            for (JCheckBox cb : getEstadoComprobanteItems()) {
                String text = cb.getText().toUpperCase();
                if (!text.contains("CANCELADO")) {
                    cb.setSelected(false);
                } else {
                    cb.setSelected(true);
                }
            }
        } catch (Exception ignore) {
        }
    }
}
