package dgu.util.swing;

import dgu.util.StringFIFO;
import javax.swing.JTextArea;
import java.util.HashSet;


/**
 * A special JTextArea which limits the number of lines which it displays.  Only 
 * the addText(), clearText(), and setText() methods may be used to alter the text 
 * in this text area or line limiting may not function properly.
 *
 * @author David Underhill
 */
public class JTextAreaFIFO extends JTextArea implements TextFieldFIFO {
    
    /* stores the text in this text area */
    private StringFIFO text;
    
    /** stores listeners awaiting notification of text changes */
    private HashSet<ChangeListener> listeners = new HashSet<ChangeListener>();
    
    /**
     * create a JTextAreaFIFO which is limited to 10 lines 
     */
    public JTextAreaFIFO() {
        this( 10 );
    }
    
    /**
     * create a JTextAreaFIFO which is limited to the specified number of lines (no column length limit)
     *
     * @param numLines  maximum number of lines the document may contain (older lines are discarded)
     */
    public JTextAreaFIFO( int numLines ) {
        this( numLines, Integer.MAX_VALUE );
    }
    
    /**
     * create a JTextAreaFIFO which is limited to the specified number of lines 
     *
     * @param numLines    maximum number of lines the document may contain (older lines are discarded)
     * @param numColumns  maximum number of columns any line may contain (longer lines are split up)
     */
    public JTextAreaFIFO( int numLines, int numColumns ) {
        super();
        text = new StringFIFO( numLines, numColumns );
    }
    
    /** 
     * sets the text in this text area to the current text plus t (minus any truncated lines)
     * @param t  the text add to the text area
     */
    public void addText( String t ) {
        text.addText( t );
        super.setText( text.text() );
        notifyTextChangeListeners();
    }
    
    /** 
     * clears all text from this text area
     */
    public void clearText() {
        text.clear();
        notifyTextChangeListeners();
    }
    
    /** 
     * gets the text in this text area
     * @return  the text in this text area
     */
    public String getText() {
        return text.text();
    }
    
    /** 
     * sets the text in this text area to t (minus any truncated lines)
     * @param t  the text to put in the text area
     */
    public void setText( String t ) {
        text = new StringFIFO( text.getMaxLines(), text.getMaxColumns() );
        super.setText( text.text() );
        notifyTextChangeListeners();
    }
    
    /** 
     * Adds a new text change listener
     * @param l  the listener to notify when text changes
     */
    public void addTextChangeListener( ChangeListener l ) {
      listeners.add( l );
    }
    
    /** 
     * Gets an array of the text change listeners
     * @return array of the text change listeners
     */
    public ChangeListener[] getTextChangeListeners() {
      return (ChangeListener[])listeners.toArray();
    }
    
    /** 
     * Notifies all text change listeners that the text has changed
     */
    private void notifyTextChangeListeners() {
      for( ChangeListener l : listeners )
        l.changed();
    }
    
    /** 
     * Removes text change listener
     * @param l  the listener to stop notifying
     */
    public void removeTextChangeListener( ChangeListener l ) {
      listeners.remove( l );
    }
    
    
}
