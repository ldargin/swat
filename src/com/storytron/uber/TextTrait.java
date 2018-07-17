package com.storytron.uber;

/** A subtype representing traits with text values. */
public final class TextTrait extends Trait {
	private static final long serialVersionUID = 1L;

	public TextTrait(String label,int valuePosition,String description){
		super(label,valuePosition,description);
	}
}
