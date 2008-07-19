//Filename: LogSyntaxDocument.java
//Revision: $Revision: 1.6 $
//Rev Date: $Date: 2007/03/25 20:48:21 $

package dgu.util.swing;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Highlights syntax in a DefaultStyledDocument.  Allows any number of keywords to 
 * be formatted in any number of user-defined styles.  "Comments" or "Timestamps" 
 * are formatted specially and are defined as text which appears between *% and %*.
 *
 * @author David Underhill
 */
public class LogSyntaxDocument extends MultiSyntaxDocument
{
    
    //<editor-fold defaultstate="collapsed" desc="        Defaults         ">
    
    /** black and bold in the default font */
    public static final SimpleAttributeSet DEFAULT_BOLD;
    
    static {
        DEFAULT_BOLD = new SimpleAttributeSet();
		StyleConstants.setForeground( DEFAULT_BOLD, new Color( 0, 0, 0 ) );
        StyleConstants.setFontFamily( DEFAULT_BOLD, DEFAULT_FONT_FAMILY );
        StyleConstants.setFontSize(   DEFAULT_BOLD, DEFAULT_FONT_SIZE );
        StyleConstants.setBold(       DEFAULT_BOLD, true );
    }
    
    /** dark red and bold in the default font */
    public static final SimpleAttributeSet DEFAULT_TITLE;
    
    static {
        DEFAULT_TITLE = new SimpleAttributeSet();
		StyleConstants.setForeground( DEFAULT_TITLE, new Color( 128, 0, 0 ) );
        StyleConstants.setFontFamily( DEFAULT_TITLE, DEFAULT_FONT_FAMILY );
        StyleConstants.setFontSize(   DEFAULT_TITLE, DEFAULT_FONT_SIZE );
        StyleConstants.setBold(       DEFAULT_TITLE, true );
    }
    
    /** dark blue and bold in the default font */
    public static final SimpleAttributeSet DEFAULT_EVENT;
    
    static {
        DEFAULT_EVENT = new SimpleAttributeSet();
		StyleConstants.setForeground( DEFAULT_EVENT, new Color( 0, 0, 128 ) );
        StyleConstants.setFontFamily( DEFAULT_EVENT, DEFAULT_FONT_FAMILY );
        StyleConstants.setFontSize(   DEFAULT_EVENT, DEFAULT_FONT_SIZE );
        StyleConstants.setBold(       DEFAULT_EVENT, true );
    }
    
    /** bright red and bold and underlined in the default font */
    public static final SimpleAttributeSet DEFAULT_ERROR;
    
    static {
        DEFAULT_ERROR = new SimpleAttributeSet();
		StyleConstants.setForeground( DEFAULT_ERROR, new Color( 255, 0, 0 ) );
        StyleConstants.setFontFamily( DEFAULT_ERROR, DEFAULT_FONT_FAMILY );
        StyleConstants.setFontSize(   DEFAULT_ERROR, DEFAULT_FONT_SIZE );
        StyleConstants.setBold(       DEFAULT_ERROR, true );
        StyleConstants.setUnderline(  DEFAULT_ERROR, true );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
	public LogSyntaxDocument( final HashMap<String, MutableAttributeSet> keywords ) {
        super( keywords );
        super.setAttributeStyle( ATTR_TYPE.Comment, DEFAULT_TITLE );
	}
    
    //</editor-fold>

        
    //<editor-fold defaultstate="collapsed" desc="   Syntax Highlighting   ">
   
	/*
	 *  *-* marks the beginning of a timestamp (processed as a multiline comment)
	 */
	protected String getStartDelimiter()
	{
		return "*-*";
	}

	/*
	 *  *~* marks the end of a timestamp (processed as a multiline comment)
	 */
	protected String getEndDelimiter()
	{
		return "*~*";
	}

    //</editor-fold>
    
}
