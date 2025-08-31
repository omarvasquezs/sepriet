package Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Simple print preview dialog rendering HTML in a JEditorPane and allowing A4 / 58mm
 * view toggles.
 */
public class DlgPrintPreview extends JDialog {

    private final JEditorPane editor = new JEditorPane();
    private final JButton btnPrint = new JButton("Imprimir");

    public DlgPrintPreview(Window owner) {
        super(owner, "Vista de Impresión (58MM)", ModalityType.APPLICATION_MODAL);
        initUI();
        setSize(900, 700);
        setLocationRelativeTo(owner);
    }

    /** Create preview dialog with target content width (pixels). */
    public DlgPrintPreview(Window owner, int contentWidthPx) {
        super(owner, "Vista de Impresión (58MM)", ModalityType.APPLICATION_MODAL);
        initUI();
        // make dialog slightly wider than content to include controls and padding
        int w = Math.max(400, contentWidthPx + 200);
        int h = 700;
        setSize(w, h);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        editor.setContentType("text/html");
        editor.setEditable(false);

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(btnPrint);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(editor), BorderLayout.CENTER);

        btnPrint.addActionListener(this::onPrint);
    }

    public void showForHtml(String html) {
        editor.setText(html);
        editor.setCaretPosition(0);
        setVisible(true);
    }


    

    private void onPrint(ActionEvent e) {
        try {
            boolean done = editor.print();
            if (!done) {
                JOptionPane.showMessageDialog(this, "Impresión cancelada o fallida.", "Imprimir", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error imprimiendo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
