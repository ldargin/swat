package com.storytron.uber.operator;

import java.lang.reflect.Method;

import com.storytron.uber.Actor;
import com.storytron.uber.FloatTrait;

/** Operator for float actor traits. */
public class ActorTraitOperator extends TraitOperator<FloatTrait> {
	private static final long serialVersionUID = 1L;
	private Actor.TraitType tt;
	public ActorTraitOperator(Actor.TraitType tt,FloatTrait t,Method m) {
		super(t,m);
		this.tt = tt;
	}
	@Override
	public String getLabel(){
		return getTrait().getLabel()+(tt==Actor.TraitType.Weight?"Weight":"");
	}
	public Actor.TraitType getTraitType() { return tt; }
}
