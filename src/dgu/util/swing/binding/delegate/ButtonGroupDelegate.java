//Filename: JToggleButtonComponentDelegate.java
//Revision: $Revision: 1.6 $
//Rev Date: $Date: 2007/03/29 23:59:05 $

package dgu.util.swing.binding.delegate;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.Binding;
import dgu.util.swing.binding.JRadioButtonBound;
import dgu.util.swing.binding.delegate.BoundDelegateComponent;
import dgu.util.translator.TypeTranslator;
import dgu.util.swing.binding.ButtonGroupBound;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import java.util.Enumeration;
import java.util.Vector;

//</editor-fold>


/**
 * A delegate for a ButtonGroup which is bound to a value of type FROM which can be represented as some 
 * type TO.  This ButtonGroup may ONLY contain JRadioButtonBound<TO>.
 *
 * @author David Underhill
 */
public class ButtonGroupDelegate<FROM, TO> extends GenericDelegate<FROM, TO> implements BoundDelegateComponent { 
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** 
     * Instantiates an unbound ButtonGroup Delegate 
     *
     * @param  owner   the BoundComponent which this belongs to
     * @param  compon  the component that this works for
     */
    public ButtonGroupDelegate( BoundComponent owner, ButtonGroupBound compon ) {
        super( owner, compon );
    }
    
    /** 
     * Instantiates an unbound ButtonGroup Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ButtonGroupDelegate( BoundComponent owner, ButtonGroupBound compon, 
                                  TypeTranslator<FROM, TO> translator, String varName ) {
        super( false, owner, compon, translator, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound ButtonGroup Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public ButtonGroupDelegate( BoundComponent owner, ButtonGroupBound compon, 
                                  TypeTranslator<FROM, TO> translator, Object boundItem, String varName ) {
        super( false, owner, compon, translator, boundItem, varName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound ButtonGroup Delegate
     *
     * @param  owner       the BoundComponent which this belongs to
     * @param  compon      the component that this works for
     * @param  translator  how to translate between the value in the boundItem (type FROM) a another value of type TO
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public ButtonGroupDelegate( BoundComponent owner, ButtonGroupBound compon, 
                                  TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName ) {
        super( false, owner, compon, translator, boundItem, getterName, setterName );
        completeInit();
    }
    
    /** 
     * Instantiates a bound ButtonGroup Delegate
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
    public ButtonGroupDelegate( BoundComponent owner, ButtonGroupBound compon, 
                                  TypeTranslator<FROM, TO> translator, Object boundItem, String getterName, String setterName, int indexAt ) {
        super( false, owner, compon, translator, boundItem, getterName, setterName, indexAt );
        completeInit();
    }
    
    /** adds a focus listener which binds any changes in the component to the object it is bound to when focus is lost */
    protected void completeInit() {
        load();
        
        //whenever a button is clicked, save and update
        for( JRadioButtonBound b : getButtons() ) {
            b.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    save(); //update the subcomponents AND refresh the text of the menu
                }
            });
        }
    }
    
    //</editor-fold>
            
        
    //<editor-fold defaultstate="collapsed" desc="   Update the Binding    ">
    
    /** load the current text for this component from the bound object */
    public void load() {
        if( binding() == null ) return;
        
        try {
            setSelected( binding().getTranslatedValue() );
            setValidationError( false );
        } catch( Exception e ) { //occurs when the boundItem is null
            setSelected( null ); //deselect all
            if( binding().getBoundItem() != null) //only flag an error if the boundItem isn't null (shouldn't happen)
                setValidationError( true, e.getMessage() );
            else
                setValidationError( false );
        }
    }
    
    /** save the current text in this component to the bound object */
    public void save() {
        if( binding() == null ) return;
        
        //see if value matches any of the radio button's selected value flags
        for( JRadioButtonBound<TO> b : getButtons() ) { 
            if( b.isSelected() ) {
                //found the selected value
                TO curValue = b.getValue();
                
                //translate to FROM
                try {
                    if( binding().canSetFromTranslatedValue( curValue ) ) {
                        binding().setFromTranslatedValue( curValue );
                        setValidationError( false );
                        return;
                    } else 
                        setValidationError( true, "Unable to translate the current value: " + 
                                            binding().getTranslator().getLastException().getMessage() );
                } catch( Exception e ) {
                    //shouldn't happen
                    setValidationError( true, e.getMessage() );
                }
            }
        }
        
        //no radio button was selected => set to null
        binding().setValue( null );
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
  
    /** get the ButtonGroup this is bound to */
    private ButtonGroup me() { return (ButtonGroup)super.getMe(); }
    
    /** get the binding this is bound to */
    private Binding<FROM, TO> binding() { 
        return super.getBinding(); 
    }
    
    /**
     * Adds the button to the group.
     * @param b the button to be added
     */ 
    public void add( JRadioButtonBound<TO> b) {
        me().add( b );
        
        b.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    save(); //update the subcomponents AND refresh the text of the menu
                }
            });
    }
    
    /** gets the buttons in this button group */
    private Vector<JRadioButtonBound<TO>> getButtons() {
        Vector<JRadioButtonBound<TO>> ret = new Vector<JRadioButtonBound<TO>>();
        
        Enumeration<AbstractButton> buttons = me().getElements();
        while( buttons.hasMoreElements() )
            ret.add( (JRadioButtonBound<TO>)buttons.nextElement() );

        return ret;
    }
    
    /** draw the border to reflect the specified validation state */
    protected void handleValidationState() {
        if( !highlightOnError() ) return;
        
        if( validationError() ) {
            for( AbstractButton b : getButtons() )
                b.setBorder( new javax.swing.border.LineBorder( java.awt.Color.RED, 2 ) );
            
            try { 
                Object obj = binding().getTranslatedValue();
                if( obj != null ) {
                    for( AbstractButton b : getButtons() )
                        b.setToolTipText( "<html>Saved Value: " + obj.toString() + getFormattedValidationErrorMsg() + "</html>" );
                }
            } catch( Exception e ) { /* shouldn't happen */ }
        } else {
            for( AbstractButton b : getButtons() ) {
                b.setBorder( new javax.swing.border.LineBorder( java.awt.Color.BLACK, 1 ) );
                b.setToolTipText( null );
            }
        }
    }
    
    /**
     * Sets the bound item with the the specified value
     *
     * @param value the new value to be set
     */
    public void setSelected( TO value ) {
        if( binding() == null ) return;
        
        //if value is null, deselect all
        if( value == null ) {
            for( JRadioButtonBound b : getButtons() )
                b.setSelected( false );
            
            setValidationError( false );
            return;
        }
        
        //see if value matches any of the radio button's selected value flags
        for( JRadioButtonBound<TO> b : getButtons() ) {
            if( b.getSelectedValue().equals( value ) ) {
                b.setSelected( true );
                setValidationError( false );
                return;
            }
        }

        //no matching radio button found => validation error (deselect all)
        for( JRadioButtonBound b : getButtons() )
                b.setSelected( false );
        
        setValidationError( true, "No radio button matches the component's value (programmer error!)" );
    }

    //</editor-fold>
    
}
