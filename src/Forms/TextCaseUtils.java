package Forms;

import java.util.Locale;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Small helper to apply an uppercase DocumentFilter to text components.
 */
public final class TextCaseUtils {
    private TextCaseUtils() {
    }

    public static void applyUppercase(JTextComponent comp) {
        if (comp == null)
            return;
        try {
            ((AbstractDocument) comp.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                        throws BadLocationException {
                    if (string != null)
                        string = string.toUpperCase(Locale.getDefault());
                    super.insertString(fb, offset, string, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                        throws BadLocationException {
                    if (text != null)
                        text = text.toUpperCase(Locale.getDefault());
                    super.replace(fb, offset, length, text, attrs);
                }
            });
        } catch (Exception ignore) {
        }
    }
}
