package Forms;

import javax.swing.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility to export all comprobantes to CSV (similar to the PHP controller).
 */
public class ExportComprobantes {
    public static void exportAllToCsv(java.awt.Window parent) {
        String sql = "SELECT c.cod_comprobante, cl.nombres, c.num_ruc, c.razon_social, c.fecha, c.fecha_actualizacion, "
                + "mp.nom_metodo_pago, er.nom_estado_ropa, ec.nom_estado, u1.username as registrado_por, "
                + "u2.username as actualizado_por, l.nombre as local, c.observaciones, c.monto_abonado, c.costo_total "
                + "FROM comprobantes c "
                + "LEFT JOIN metodo_pago mp ON c.metodo_pago_id = mp.id "
                + "LEFT JOIN estado_ropa er ON c.estado_ropa_id = er.id "
                + "LEFT JOIN estado_comprobantes ec ON c.estado_comprobante_id = ec.id "
                + "LEFT JOIN clientes cl ON c.cliente_id = cl.id "
                + "LEFT JOIN users u1 ON c.user_id = u1.id "
                + "LEFT JOIN users u2 ON c.last_updated_by = u2.id "
                + "LEFT JOIN locales l ON c.local_id = l.id ";

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("comprobantes_todos_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv"));
        int sel = fc.showSaveDialog(parent);
        if (sel != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery();
             FileWriter fw = new FileWriter(f)) {

            String[] headers = new String[] {"COMPROBANTE", "CLIENTE", "N RUC", "RAZON SOCIAL", "FECHA", "FECHA ACTUALIZACION", "METODO DE PAGO", "ESTADO ROPA", "ESTADO COMPROBANTE", "REGISTRADO POR", "ACTUALIZADO POR", "LOCAL", "OBSERVACIONES", "ABONADO", "COSTO TOTAL"};
            // write header
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) fw.write(',');
                fw.write(escapeCsv(headers[i]));
            }
            fw.write('\n');

            while (rs.next()) {
                Object[] vals = new Object[] {
                        rs.getString("cod_comprobante"),
                        rs.getString("nombres"),
                        rs.getString("num_ruc"),
                        rs.getString("razon_social"),
                        rs.getTimestamp("fecha"),
                        rs.getTimestamp("fecha_actualizacion"),
                        rs.getString("nom_metodo_pago"),
                        rs.getString("nom_estado_ropa"),
                        rs.getString("nom_estado"),
                        rs.getString("registrado_por"),
                        rs.getString("actualizado_por"),
                        rs.getString("local"),
                        rs.getString("observaciones"),
                        rs.getObject("monto_abonado"),
                        rs.getObject("costo_total")
                };
                for (int i = 0; i < vals.length; i++) {
                    if (i > 0) fw.write(',');
                    Object v = vals[i];
                    String cell;
                    if (v == null) cell = "";
                    else if (v instanceof java.sql.Timestamp) cell = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(((java.sql.Timestamp) v));
                    else cell = v.toString();
                    fw.write(escapeCsv(cell));
                }
                fw.write('\n');
            }
            fw.flush();
            JOptionPane.showMessageDialog(parent, "Exportado: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Error exportando:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"");
        String out = s.replace("\"", "\"\"");
        if (needQuote) return '"' + out + '"';
        return out;
    }
}
