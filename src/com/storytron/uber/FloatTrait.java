package com.storytron.uber;

/** 
 * A subtype representing traits with float values.
 * It has a boolean attribute to tell whether it is visible to actors on the same stage
 * than the actor owning the trait.
 *  */
public final class FloatTrait extends Trait {
	private static final long serialVersionUID = 1L;
	private boolean visible;

	public FloatTrait(String label,int valuePosition,boolean visible,String description){
		super(label,valuePosition,description);
		this.visible = visible;
	}
	
	/** Tells if the trait is visible. */
	public boolean isVisible() {
		return visible;
	}

	/** Sets visibility of the trait. */
	void setVisible(boolean visible) {
		this.visible = visible;
	}
	
}
