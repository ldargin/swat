package com.storytron.uber.operator;

import java.lang.reflect.Method;

import com.storytron.uber.Trait;

/**
 * Represents an operator related to a trait.
 * Use {@link #getTrait()} to get the associated trait.
 */
public class TraitOperator <T extends Trait> extends Operator {
	private static final long serialVersionUID = 1L;
	protected final T t;
	public TraitOperator(T t,Method m) {
		super(null,-1,m);
		this.t=t;
	}
	public T getTrait(){ return t; };
	@Override
	public String getLabel(){
		return t.getLabel();
	}
	@Override
	public void setLabel(String s){
		throw new RuntimeException("Cannot set the name of a trait operator: "+getLabel());
	}
}
