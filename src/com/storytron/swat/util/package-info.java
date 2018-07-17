/** 
 This package contains various classes that are used by swat.
 
 <h2>Undo support</h2>
 <p> Swat uses {@link com.storytron.swat.Swat.UndoManager} a subclass of 
     {@link javax.swing.undo.AbstractUndoManager} to handle undo of operations.
 The class representing undoable edits is {@link com.storytron.swat.util.UndoableAction} 
 which subclasses  {@link javax.swing.undo.AbstractUndoableEdit}.  
{@link com.storytron.swat.util.UndoableAction} is designed for being anonimously 
subclassed in the place in the code where the undoable action should be executed. 
</p>

<p> The special class {@link com.storytron.swat.undo.UndoableSlider} was implemented for 
    undoing changes in {@link javax.swing.JSlider}s.
    Sliders would register many little actions when dragging or using the 
    keyboard to move the slider. This class get rid of the spurious actions 
    registering only the last one.    
</p> <h2>Special GUI controls</h2>   
 <p>
 Throughout the editors we are using the class {@link com.storytron.swat.DropDown}
 which allows reordering its elements by dragging them up and down in the popup list.  
 </p>
 <p> There two classes that we use to implement popups. We have our own lightweight popup
 {@link com.storytron.swat.util.LightweightPopup} which can hold text editors.
 And we have {@link com.storytron.swat.util.ErrorPopup} which behaves almost as a tooltip,
 and it is used to show errors when the input of the user is not valid.
 </p>
*/
package com.storytron.swat.util;