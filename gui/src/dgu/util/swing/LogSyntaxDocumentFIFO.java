//Filename: DefaultStyledDocumentFIFO.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/03/04 18:09:02 $

package dgu.util.swing;

import dgu.util.StringFIFO;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;


/**
 * A special DefaultStyledDocument which limits the number of lines which it displays.  Only 
 * the addText(), clearText(), and setText() methods may be used to alter the text 
 * in this text area or line limiting may not function properly.
 *
 * @author David Underhill
 */
public class LogSyntaxDocumentFIFO extends LogSyntaxDocument implements TextFieldFIFO {
    
    /* stores the text in this text area */
    private StringFIFO text;
    
    /**
     * create a LogSyntaxDocumentFIFO which is limited to 10 lines 
     */
    public LogSyntaxDocumentFIFO() {
        this( 10 );
    }
    
    /**
     * create a LogSyntaxDocumentFIFO which is limited to the specified number of lines (no column length limit)
     *
     * @param numLines  maximum number of lines the document may contain (older lines are discarded)
     */
    public LogSyntaxDocumentFIFO( int numLines ) {
        this( numLines, Integer.MAX_VALUE );
    }
    
    /**
     * create a LogSyntaxDocumentFIFO which is limited to the specified number of lines 
     *
     * @param numLines    maximum number of lines the document may contain (older lines are discarded)
     * @param numColumns  maximum number of columns any line may contain (longer lines are split up)
     */
    public LogSyntaxDocumentFIFO( int numLines, int numColumns ) {
        super( new java.util.HashMap<String, javax.swing.text.MutableAttributeSet>() );
        text = new StringFIFO( numLines, numColumns );
    }
    
    /** 
     * sets the text in this text area to the current text plus t (minus any truncated lines)
     * @param t  the text add to the text area
     */
    public void addText( String t ) {
        text.addText( t );
        
        try {
            super.replace( 0, super.getLength(), text.text(), null );
        } catch( BadLocationException e ) { /* shouldn't be able to happen; range is hard-coded */ }
    }
    
    /** 
     * clears all text from this text area
     */
    public void clearText() {
        text.clear();
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
        try {
            super.replace( 0, super.getLength(), text.text(), null );
        } catch( BadLocationException e ) { /* shouldn't be able to happen; range is hard-coded */ }
    }
    
}
