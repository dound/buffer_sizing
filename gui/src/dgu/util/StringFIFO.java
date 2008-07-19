//Filename: StringFIFO.java
//Revision: $Revision: 1.7 $
//Rev Date: $Date: 2007/03/04 18:09:01 $

package dgu.util;


/**
 * This class contains a String which is limited to a certain number lines.
 *
 * @author David Underhill
 */
public class StringFIFO {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    /** the string */
    private String text = "";
    
    /** maximum number of lines this string may contain */
    private int maxLines;
    
    /** maximum number of columns this string may contain */
    private int maxColumns;
    
    /** whether or not to hide blank white lines */
    private final boolean hideEndingBlankLines;
    
    //internal helpers
    /** number of lines currently in the string */
    private int numLines = 1;
    
    /** number of lines currently in the string */
    private int numCols = 0;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * initialize the FIFO string (will hide a trailing blank line if one exists)
     *
     * @param maxLines  the maximum number of lines the string may contain (if 
     *                  there are too many lines, those at the top of the string 
     *                  are removed)
     */
    public StringFIFO( final int maxLines ) {
        this( maxLines, Integer.MAX_VALUE );
    }
    
    /** 
     * initialize the FIFO string (will hide a trailing blank line if one exists)
     *
     * @param maxLines  the maximum number of lines the string may contain (if 
     *                  there are too many lines, those at the top of the string 
     *                  are removed)
     *
     * @param maxCharsPerLine  the maximum number of columns any line may contain (if
     *                         there are too many columns, the line will be split up
     *                         into multiple lines)
     */
    public StringFIFO( final int maxLines, final int maxCharsPerLine ) {
        this( maxLines, maxCharsPerLine, true );
    }
    
    /** 
     * initialize the FIFO string 
     *
     * @param maxLines  the maximum number of lines the string may contain (if 
     *                  there are too many lines, those at the top of the string 
     *                  are removed)
     *
     * @param maxCharsPerLine  the maximum number of columns any line may contain (if
     *                         there are too many columns, the line will be split up
     *                         into multiple lines)
     *
     * @param hideEndingBlankLines  whether or not a trailing, blank line should 
     *                              count against the limit and be shown in the 
     *                              string.  If true, if there is a trailing blank
     *                              line at the end of a string being added, it 
     *                              will be queued (not in the string) until text
     *                              is added to that line.  Only the last blank 
     *                              white line is treated in this manner; if there
     *                              are multiple blank white lines, all of them 
     *                              except the last one will be part of the string.
     */
    public StringFIFO( final int maxLines, final int maxCharsPerLine, final boolean hideEndingBlankLines ) {
        this.maxLines = maxLines;
        this.maxColumns = maxCharsPerLine;
        this.hideEndingBlankLines = hideEndingBlankLines;
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="     Worker Methods      ">
    
    /** 
     * adds the specified string plus a newline to this object
     * @param s  the string to add
     */
    public void addLine( String s ) {
        addText( s + "\n" );
    }
    
    /** 
     * adds the specified string to this object
     * @param s  the string to add
     */
    public void addText( String s ) {
        s = StringOps.splitIntoLines( s, maxColumns - numCols,  maxColumns, "\n", false );
        
        //multiple lines, so get a count of the number of them first then add the text
        if( s.indexOf( '\n' ) == -1 ) {
            numCols += s.length();
            if( numCols == maxColumns ) {
                s = s.concat( "\n" );
                numLines += 1;
                numCols = 0;
            }
            text = text.concat( s );
        } else {
            String[] lines = s.split( "\n" );
            numLines += lines.length - 1;
            if( s.endsWith( "\n" ) ) {
                numLines += 1;
                numCols = 0;
            } else {
                numCols += lines[lines.length-1].length();
                if( numCols == maxColumns ) {
                    s = s.concat( "\n" );
                    numLines += 1;
                    numCols = 0;
                }
            }
            text = text.concat( s );
            truncate();
        }
    }
    
    /**
     * Clear the text from the string
     */
    public void clear() {
        numLines = 1;
        numCols = 0;
        text = "";
    }
    
    /**
     * Trim the string contained by this object so it contains no more than 
     * the maximum number of lines.
     */
    private void truncate() {
        //find the position of first line at the new beginning of the string (remove extra lines from the top)
        int firstEndline = 0;
        
        int mod = 0;
        if( hideEndingBlankLines && text.endsWith( "\n" ) ) mod = 1;
        
        while( numLines - mod > maxLines ) {
            firstEndline = text.indexOf( '\n', firstEndline ) + 1;
            numLines -= 1;
        }
        
        //truncate the string
        if( firstEndline > 0 )
            text = text.substring( firstEndline );
    }

    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the length string (number of characters) */
    public int length() {
        return text.length();
    }
    
    /** gets the number of lines in this string */
    public int size() {
        return numLines;
    }
    
    /** 
     * gets the current max number of columns a line may have 
     * @return current maximum characters each line in the string is limited to
     */
    public int getMaxColumns() {
        return maxColumns;
    }
    
    /** 
     * gets the current max number of lines 
     * @return current maximum number of lines the string is limited to
     */
    public int getMaxLines() {
        return maxLines;
    }

    /** 
     * sets the current max number of lines
     * @param maxLines  maximum number of lines to limit this string to
     */
    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
        truncate();
    }
    
    /** gets the current value of the string */
    public String text() {
        if( this.hideEndingBlankLines && text.endsWith( "\n" ) )
            return text.substring( 0, text.length()-1 );
        else
            return text;
    }
 
    //</editor-fold>

}
