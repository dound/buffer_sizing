//Filename: IntermediateField.java
//Revision: $Revision: 1.3 $
//Rev Date: $Date: 2007/02/26 15:13:38 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundContainer;
import dgu.util.swing.binding.delegate.IntermediateFieldDelegate;
import dgu.util.translator.SelfTranslator;
import dgu.util.translator.TypeTranslator;

//</editor-fold>


/**
 * Describes a text field which holds a value of type TYPE
 * @author David Underhill
 */
public class IntermediateField<TYPE> implements BoundContainer {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private final IntermediateFieldDelegate<TYPE> delegate;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates an unbound intermediate holderBound */
    public IntermediateField() {
        super();
        delegate = new IntermediateFieldDelegate<TYPE>( this, this );
    }
        
    /** 
     * Instantiates an unbound intermediate holder; generic type MUST be String
     *
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public IntermediateField( String varName ) {
        super();
        delegate = new IntermediateFieldDelegate<TYPE>( 
                this, this, varName );
    }
    
    /** 
     * Instantiates a bound intermediate holder; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by varName)
     * @param  varName     the name of the variable; assumes the getter and setter name will
     *                     be get and set followed by varName with its first letter capitalized 
     *                     (ex: if varName is "value" then the getter will be getValue).
     */
    public IntermediateField( Object boundItem, String varName ) {
        super();
        delegate = new IntermediateFieldDelegate<TYPE>( 
                this, this, boundItem, varName );
    }
    
    /** 
     * Instantiates a bound intermediate holder; generic type MUST be String
     *
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public IntermediateField( String getterName, String setterName ) {
        super();
        delegate = new IntermediateFieldDelegate<TYPE>( 
                this, this, null, getterName, setterName );
    }
            
    /** 
     * Instantiates a bound intermediate holder; generic type MUST be String
     *
     * @param  boundItem   the object this is bound to (contains the value to be modified as specified by the getter 
and setter name)
     * @param  getterName  how to get the bound value from the bound object
     * @param  setterName  how to set the bound value from the bound object
     */
    public IntermediateField( Object boundItem, String getterName, String setterName ) {
        super();
        delegate = new IntermediateFieldDelegate<TYPE>( 
                this, this, boundItem, getterName, setterName );
    }
    
    //</editor-fold>
    
        
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the delete which controls the binding */
    public IntermediateFieldDelegate<TYPE> getBindingDelegate() {
        return delegate;
    }
    
    //</editor-fold>
    
}
