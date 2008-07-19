//Filename: TextScrollupListener.java
//Revision: $Revision: 1.4 $
//Rev Date: $Date: 2007/04/08 03:42:40 $

package dgu.util.swing;

import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


/**
 * Listens for changes in a document backing a JTextComponent and if the typing isn't
 * the cause of the changes, then the JTextComponent is scrolled to the top.
 *
 * @author David Underhill
 */
public class TextScrollupListener implements DocumentListener {
    
    /** adds a TextScrollupListener to the specified JTextComponent */
    public static void listenTo( JTextComponent txtCompon ) {
        txtCompon.getDocument().addDocumentListener( new TextScrollupListener( txtCompon ) );
    }
    
    private boolean typing = false;
    private final JTextComponent txtCompon;
    private final ScrollupKeyHelper keyHelper;

    /** lets a ScrollupListener know when keys are being pressed */
    private static class ScrollupKeyHelper implements KeyListener {
        private final TextScrollupListener sl;

        public ScrollupKeyHelper( TextScrollupListener sl ) {
            this.sl = sl;
        }

        public void keyPressed(KeyEvent e) {
            sl.typing = true;
        }
        public void keyReleased(KeyEvent e) {
            sl.typing = false;
        }
        public void keyTyped(KeyEvent e) {
            /* Intentionally Blank */
        }
    }

    public TextScrollupListener( final JTextComponent txtCompon ) {
        this.txtCompon = txtCompon;

        keyHelper = new ScrollupKeyHelper(this);
        this.txtCompon.addKeyListener( keyHelper );
    }

    public void changedUpdate( DocumentEvent e ) { 
        javax.swing.SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                if( !typing )
                    txtCompon.scrollRectToVisible( new java.awt.Rectangle( 0, 0, 0, 0 ) );
            }
        });
    }
    public void insertUpdate( DocumentEvent e ) { changedUpdate(null); }
    public void removeUpdate( DocumentEvent e ) { /* intentionally blank */ }
};
