//Filename: GenericJComponentDelegate.java
//Revision: $Revision: 1.6 $
//Rev Date: $Date: 2007/03/29 23:59:05 $

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.translator.TypeTranslator;
import javax.swing.JComponent;

//</editor-fold>


/**
 * Binds some Object of type FROM to a component which requires type TO
 *
 * @author David Underhill
 */
public abstract class GenericJComponentDelegate<FROM, TO> extends GenericDelegate<FROM, TO> {
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound JComponent Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon  the component that this works for
     */
    public GenericJComponentDelegate( BoundComponent owner, BoundComponent compon ) {
        super( owner, compon );
    }
    
    /** 
     * Instantiates a bound JComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public GenericJComponentDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon,
                                     TypeTranslator<FROM, TO> translator, String varName ) {
        super( isContainer, owner, compon, translator, varName );
    }
    
    /** 
     * Instantiates a bound JComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public GenericJComponentDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon,
                                     TypeTranslator<FROM, TO> translator, Object boundItem, String varName ) {
        super( isContainer, owner, compon, translator, boundItem, varName );
    }
    
    /** 
     * Instantiates a bound JComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public GenericJComponentDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon, 
                                     TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName ) {
        super( isContainer, owner, compon, translator, boundItem, getterName, setterName );
    }
    
    /** 
     * Instantiates a bound JComponent Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  isContainer whether or not this component can contain others
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     * @param  indexAt     if bound value is stored within a container with get(index) and 
     *                     set(index, Object) methods, then indexAt should be the index to 
     *                     use; otherwise, indexAt should be -1
     */
    public GenericJComponentDelegate( boolean isContainer, BoundComponent owner, BoundComponent compon, 
                                     TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super( isContainer, owner, compon, translator, boundItem, getterName, setterName, indexAt );
    }
    
    /** suggested that this is used to load the component with its initial values after the constructor is done */
    protected abstract void completeInit();
    
    //</editor-fold>
            
      
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** get the JComboBox this is bound to */
    private BoundComponent me() { return (BoundComponent)super.getMe(); }
    
    /** get the binding this is bound to */
    private Binding<FROM, TO> binding() { 
        return super.getBinding(); 
    }
    
    /** 
     * draw the border to reflect the specified validation state if me() returns a 
     * JComponent, and does nothing otherwise
     */
    protected void handleValidationState() {
        if( !highlightOnError() ) return;
        
        JComponent jc;
        if( me() instanceof JComponent )
            jc = (JComponent)me();
        else
            return;
            
        if( validationError() ) {
            jc.setBorder( new javax.swing.border.LineBorder( java.awt.Color.RED, 2 ) );
            
            try { 
                Object obj = binding().getTranslatedValue();
                if( obj != null )
                    jc.setToolTipText( "<html>Saved Value: " + obj.toString() + getFormattedValidationErrorMsg() + "</html>" );
            } catch( Exception e ) { /* shouldn't happen */ }
        } else {
            jc.setBorder( new javax.swing.border.LineBorder( java.awt.Color.BLACK, 1 ) );
            jc.setToolTipText( null );
        }
    }
    
    //</editor-fold>
    
}
