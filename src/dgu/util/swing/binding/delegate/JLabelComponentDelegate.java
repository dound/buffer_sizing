//Filename: JLabelComponentDelegate.java
//Revision: $Revision: 1.8 $
//Rev Date: $Date: 2007/04/02 06:40:10 $
//Author's Note: This class is IDENTICAL to TextComponentDelegate except LabelComponentDelegate is 
//               extended vice JTextField.  Instaces of JTextField in 
//               javadoc comments have also been replaced.

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.swing.binding.delegate.BoundDelegateComponent;
import dgu.util.translator.TypeTranslator;
import dgu.util.swing.binding.JLabelBound;
import javax.swing.JLabel;

//</editor-fold>


/**
 * A delegate for a JLabel which is bound to a value of type FROM which can be represented as a String.
 *
 * @author David Underhill
 */
public class JLabelComponentDelegate<FROM> extends GenericJComponentDelegate<FROM, String> implements BoundDelegateComponent {   
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound JLabelComponent Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon  the component that this works for
     */
    public JLabelComponentDelegate( BoundComponent owner, JLabelBound compon ) {
        super( owner, compon );
    }
    
    /** 
     * Instantiates a bound JLabelComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JLabelComponentDelegate( BoundComponent owner, JLabelBound compon, 
                                  TypeTranslator<FROM, String> translator, String varName ) {
        super( false, owner, compon, translator, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JLabelComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public JLabelComponentDelegate( BoundComponent owner, JLabelBound compon, 
                                  TypeTranslator<FROM, String> translator, Object boundItem, String varName ) {
        super( false, owner, compon, translator, boundItem, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JLabelComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public JLabelComponentDelegate( BoundComponent owner, JLabelBound compon, 
                                  TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName ) {
        super( false, owner, compon, translator, boundItem, getterName, setterName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound JLabelComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public JLabelComponentDelegate( BoundComponent owner, JLabelBound compon, 
                                  TypeTranslator<FROM, String> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super( false, owner, compon, translator, boundItem, getterName, setterName, indexAt );
        completeInit();
    }
    
    /** adds a focus listener which binds any changes in the component to the object it is bound to when focus is lost */
    protected void completeInit() {
        load();
        
        //on focus lost, update the data structure
        me().addFocusListener( new java.awt.event.FocusAdapter() {
                public void focusLost(java.awt.event.FocusEvent evt) {
                    save();
                }
            });
    }
    
    //</editor-fold>
        
        
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">
    
    /** load the current text for this component from the bound object */
    public void load() {
        if( binding() == null ) return;
        
        try {
            me().setText( binding().getTranslatedValue() );
            setValidationError( false );
        } catch( Exception e ) { //occurs when the boundItem is null
            me().setText( "" );
            if( binding().getBoundItem() != null) //only flag an error if the boundItem isn't null (shouldn't happen)
                setValidationError( true, e.getMessage() );
            else
                setValidationError( false );
        }
    }
    
    /** save the current text in this component to the bound object */
    public void save() {
        if( binding() == null ) return;
        
        setText( me().getText() );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
  
    /** get the JLabel this is bound to */
    private JLabel me() { return (JLabel)super.getMe(); }
    
    /** get the binding this is bound to */
    private Binding<FROM, String> binding() { 
        return super.getBinding(); 
    }
    
    /**
     * Sets the bound item with the translation of the specified
     * text.  The text which appears in this component will be the 
     * translation back from the bound item.  If the text cannot be
     * translated, this a validation error will be flagged and the 
     * text will be set but the bound object's value will not change!
     *
     * @param t the new text to be set
     */
    public void setText( String t ) {
        if( binding() == null ) return;
        
        try {
            if( !binding().canSetFromTranslatedValue( t ) ) {
                me().setText( t );
                setValidationError( true, binding().getTranslator().getLastException().getMessage() );
                return;
            }
            setValidationError( false );
        
            binding().setFromTranslatedValue( t );
            me().setText( binding().getTranslatedValue() );
            if( getBoundParent() != null ) getBoundParent().notifyBoundComponentChange( getOwner() );
        } catch( Exception e ) {
            //can't happen
            setValidationError( true, e.getMessage() ); //apparently something went wrong
        }
    }

    /** same as parent but no border if validated = ok */
    protected void handleValidationState() {
        if( !highlightOnError() ) return;
        
        super.handleValidationState();
        if( !this.validationError() )
            me().setBorder( null );
    }
    
    //</editor-fold>
    
}
