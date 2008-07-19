package dgu.util.swing;


/**
 * A special text container which limits the number of lines which it displays.
 *
 * @author David Underhill
 */
public interface TextFieldFIFO {
    
    /** 
     * sets the text in this text field to the current text plus t (minus any truncated lines)
     * @param t  the text add to the text field
     */
    public void addText( String t );
    
    /** 
     * clears all text from this text field
     */
    public void clearText();
    
    /** 
     * gets the text in this text field
     * @return  the text in this text field
     */
    public String getText();
    
    /** 
     * sets the text in this text field to t (minus any truncated lines)
     * @param t  the text to put in the text field
     */
    public void setText( String t );
    
}
