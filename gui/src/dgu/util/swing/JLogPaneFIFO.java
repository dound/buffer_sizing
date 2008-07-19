//Filename: JLogPaneFIFO.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/03/04 17:38:48 $

package dgu.util.swing;

import dgu.util.swing.LogSyntaxDocumentFIFO;
import javax.swing.JTextPane;
import javax.swing.text.*;


/**
 * JTextPane backed by a LogSyntaxDocumentFIFO restricted to the specified number of lines.  Coloring
 * will also be setup for certain keywords.
 *
 * @author David Underhill
 */
public class JLogPaneFIFO extends JTextPane implements TextFieldFIFO {
       
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    /* stores the text in this text area */
    private LogSyntaxDocumentFIFO document;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="    Logging Pane Setup   ">
    
    /** the style to show test name's with */
    public static final SimpleAttributeSet DEFAULT_TEST_NAME;
    
    static {
        DEFAULT_TEST_NAME = new SimpleAttributeSet();
		StyleConstants.setForeground( DEFAULT_TEST_NAME, new java.awt.Color( 0, 128, 0 ) );
        StyleConstants.setFontFamily( DEFAULT_TEST_NAME, LogSyntaxDocumentFIFO.DEFAULT_FONT_FAMILY );
        StyleConstants.setFontSize(   DEFAULT_TEST_NAME, LogSyntaxDocumentFIFO.DEFAULT_FONT_SIZE );
        StyleConstants.setBold(       DEFAULT_TEST_NAME, true );
    }
    
    /**
     * Registers a test name for highlighting
     * 
     * @param  testName  name of the test to highlight
     */
    public void addTestName( String testName ) {
        document.addKeyword( testName, DEFAULT_TEST_NAME );
    }
    
    /**
     * Removes all registered test names
     */
    public void clearTestNames() {
        document.clearKeywords();
    }    
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /**
     * create a JLogPaneFIFO which is limited to 10 lines 
     */
    public JLogPaneFIFO() {
        this( 10 );
    }
    
    /**
     * create a JLogPaneFIFO which is limited to the specified number of lines (no column length limit)
     *
     * @param numLines  maximum number of lines the document may contain (older lines are discarded)
     */
    public JLogPaneFIFO( int numLines ) {
        this( numLines, Integer.MAX_VALUE );
    }
    
    /**
     * create a JLogPaneFIFO which is limited to the specified number of lines
     *
     * @param numLines    maximum number of lines the document may contain (older lines are discarded)
     * @param numColumns  maximum number of columns any line may contain (longer lines are split up)
     */
    public JLogPaneFIFO( int numLines, int numColumns ) {
        super( new LogSyntaxDocumentFIFO(numLines, numColumns) );
        document = ((LogSyntaxDocumentFIFO)getStyledDocument()); //save a reference to the document which backs this
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">
    
    /** 
     * sets the text in this text area to the current text plus t (minus any truncated lines)
     * @param t  the text add to the text area
     */
    public void addText( String t ) {
        document.addText( t );
    }
    
    /** 
     * clears all text from this text area
     */
    public void clearText() {
        document.clearText();
    }
    
    /** 
     * gets the text in this text area
     * @return  the text in this text area
     */
    public String getText() {
        return document.getText();
    }
    
    /** 
     * sets the text in this text area to t (minus any truncated lines)
     * @param t  the text to put in the text area
     */
    public void setText( String t ) {
        document.setText( t );
    }
    
    //</editor-fold>
    
}
