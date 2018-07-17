package com.storytron.uber.operator;

import java.lang.reflect.Method;

import com.storytron.uber.FloatTrait;
import com.storytron.uber.Actor.PTraitType;

public class PTraitOperator extends TraitOperator<FloatTrait> {
	private static final long serialVersionUID = 1L;
	private PTraitType tt;

	public PTraitOperator(PTraitType tt,FloatTrait t,Method m) {
		super(t,m);
		this.tt = tt;
	}
	@Override
	public String getLabel(){
		return (tt==PTraitType.Perception?"P":"C")+getTrait().getLabel();
	}
	public PTraitType getPTraitType(){ return tt; }
	
	@Override
	public void setLabel(String s){
		throw new RuntimeException("Cannot set the name of a C trait operator: "+t.getLabel());
	}
}
