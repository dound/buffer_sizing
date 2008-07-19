//Filename: JRadioButtonBound.java
//Revision: $Revision: 1.1 $
//Rev Date: $Date: 2007/02/20 15:25:22 $

package dgu.util.swing.binding;

//<editor-fold defaultstate="collapsed" desc="         Imports         ">

import dgu.util.swing.binding.delegate.BoundComponent;
import dgu.util.swing.binding.delegate.JToggleButtonComponentDelegate;
import dgu.util.translator.TypeTranslator;
import javax.swing.JRadioButton;

//</editor-fold>


/**
 * Describes a radio button field which holds a value of type TO if selected and null otherwise.  To 
 * be used in conjunction with JButtonGroupBound. 
 *
 * @author David Underhill
 */
public class JRadioButtonBound<TO> extends JRadioButton {
    
    //<editor-fold defaultstate="collapsed" desc="         Fields          ">
    
    private TO selectedValue   = null;
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="      Constructors       ">
    
    /** Instantiates a JRadioButtonBound with no selected or unselected value (both null) */
    public JRadioButtonBound() {
        super();
    }
    
    /** 
     * Instantiates a JRadioButtonBound with no unselected value (null) 
     *
     * @param selectedValue  the value this radio button returns for getValue() when it is selected
     */
    public JRadioButtonBound( TO selectedValue ) {
        super();
        this.selectedValue = selectedValue;
    }
    
    //</editor-fold>
            
            
    //<editor-fold defaultstate="collapsed" desc="   Accessors/Mutators    ">
    
    /** gets the current value of this radio button (null if unselected) */
    public TO getValue() {
        if( this.isSelected() )
            return selectedValue;
        else
            return null;
    }
    
    /** gets the value of this radio button when it is selected */
    public TO getSelectedValue() {
        return selectedValue;
    }

    /** sets the value of this radio button when it is selected */
    public void setSelectedValue(TO selectedValue) {
        this.selectedValue = selectedValue;
    }
    
    //</editor-fold>
    
}
