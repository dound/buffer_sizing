//Filename: JEditorPaneMultiSyntax.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:23:38 $

package dgu.util.swing;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyledEditorKit;
import java.util.HashMap;

//</editor-fold>


/**
 * A JEditorPane which highlights syntax with a MultiSyntaxDocument.  Allows any number of keywords to 
 * be formatted in any number of user-defined styles.
 *
 * @author David Underhill
 */
public class JEditorPaneMultiSyntax extends JEditorPane {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiate a MultiSyntaxJEditorPane with no keywords */
    public JEditorPaneMultiSyntax() {
        this( new HashMap<String, MutableAttributeSet>() );
    }
    
    /** 
     * Instantiate a MultiSyntaxJEditorPane with the specified keywords 
     *
     * @param keywords  a list of keywords and the style in which to display them
     */
    public JEditorPaneMultiSyntax( final HashMap<String, MutableAttributeSet> keywords ) {
        this( keywords, 4 );
    }
    
    /** 
     * Instantiate a MultiSyntaxJEditorPane with the specified keywords and tab width
     *
     * @param keywords  a list of keywords and the style in which to display them
     * @param tabSize   the number of spaces to make a tab equivalent to
     */
    public JEditorPaneMultiSyntax( final HashMap<String, MutableAttributeSet> keywords, final int tabSize ) {
        setEditorKitForContentType( "text/java", makeEditorKit(new HashMap<String, MutableAttributeSet>()) );
		setContentType( "text/java" );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc=" Make Styled Editor Kits ">
    
    /** 
     * Return a StyledEditorKit with the specified keywords to be highlighted
     *
     * @param keywords  a list of keywords and the style in which to display them
     */
    public static StyledEditorKit makeEditorKit( final HashMap<String, MutableAttributeSet> keywords ) {
        return makeEditorKit( keywords, 4 );
    }
    
    /** 
     * Return a StyledEditorKit with the specified keywords to be highlighted
     *
     * @param keywords  a list of keywords and the style in which to display them
     * @param tabSize   the number of spaces to make a tab equivalent to
     */
    public static StyledEditorKit makeEditorKit( final HashMap<String, MutableAttributeSet> keywords, final int tabSize ) {
        StyledEditorKit editorKit = new StyledEditorKit()
		{
			public Document createDefaultDocument()
			{
				MultiSyntaxDocument doc = new MultiSyntaxDocument( keywords );
                doc.setTabs( tabSize );
                return doc;
			}
		};

        return editorKit;
    }
    
    //</editor-fold>
    
}
