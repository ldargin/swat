package com.storytron.uber.operator;

import java.lang.reflect.Method;

import Engine.enginePackage.Interpreter;

import com.storytron.uber.Actor;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.TextTrait;
import com.storytron.uber.Actor.PTraitType;
import com.storytron.uber.Deikto.TraitType;
import com.storytron.uber.operator.OperatorDictionary.Menu;

/**
 * <p>In this class methods we group the nuances of instantiating different
 * kind of operators. No other code should instantiate operators other than
 * through the methods of this class. </p> 
 * */
public final class OperatorFactory {
	
	/**
	 * Creates an operator with all its fields to be filled.
	 * */
	public static Operator createOperator(String label,int cArgs,Method m){
		return new Operator(label,cArgs,m);
	}

	/**
	 * <p>Creates an operator for setting BNumber values for a trait
	 * of the given type <code>tt</code>. It will have two arguments:
	 * <ul>
	 * <li>The object which type depends on the trait type.</li>
	 * <li>The BNumber to set.</li>
	 * </ul>
	 * </p>
	 * */
	public static TraitOperator<FloatTrait> createSetTraitOperator(TraitType tt,FloatTrait t){				
		TraitOperator<FloatTrait> operator;
		Method m = Interpreter.getMethod(Operator.OpType.Write,tt.name()+"Trait");
		if (tt==TraitType.Actor)
			operator = new ActorTraitOperator(Actor.TraitType.Normal,t,m){
				private static final long serialVersionUID = 1L;
				@Override
				public String getLabel() {	return "Set"+getTrait().getLabel();	}
			};
		else
			operator = new TraitOperator<FloatTrait>(t,m){
				private static final long serialVersionUID = 1L;
				@Override
				public String getLabel() {	return "Set"+getTrait().getLabel();	}
			};
		operator.setOperatorType(Operator.OpType.Write);
		operator.setDataType(Operator.Type.Procedure);
		operator.addArgument(Operator.Type.valueOf(tt.name()),tt.name(),"");
		operator.addArgument(Operator.Type.BNumber,"BNumber","");			
		operator.setMenu(OperatorDictionary.Menu.valueOf("Set"+tt.name()));
		
		return operator;
	}

	/**
	 * <p>Creates an operator for getting BNumber values of a trait
	 * of the given type <code>tt</code>. It will have one argument,
	 * which type depends on the trait type.
	 * </p>
	 * */
	public static TraitOperator<FloatTrait> createGetTraitOperator(TraitType tt,FloatTrait t){				
		TraitOperator<FloatTrait> operator;
		if (tt==TraitType.Actor)
			operator = new ActorTraitOperator(Actor.TraitType.Normal,t,Interpreter.getMethod(Operator.OpType.Read,"ActorTrait"));
		else
			operator = new TraitOperator<FloatTrait>(t,Interpreter.getMethod(Operator.OpType.Read,tt.name()+"Trait"));
		operator.setOperatorType(Operator.OpType.Read);
		operator.setDataType(Operator.Type.BNumber);
		operator.addArgument(Operator.Type.valueOf(tt.name()),tt.name(),"");
		operator.setMenu(OperatorDictionary.Menu.valueOf(tt.name()));
		return operator;
	}

	/**
	 * <p>Creates an operator for getting the Text values of a text trait
	 * of the given type <code>tt</code>. It will have one argument,
	 * which type depends on the trait type.
	 * </p>
	 * */
	public static TraitOperator<TextTrait> createGetTextTraitOperator(TraitType tt,TextTrait t){				
		TraitOperator<TextTrait> operator = new TraitOperator<TextTrait>(t,Interpreter.getMethod(Operator.OpType.Read,tt.name()+"TextTrait"));
		operator.setOperatorType(Operator.OpType.Read);
		operator.setDataType(Operator.Type.Text);
		operator.addArgument(Operator.Type.valueOf(tt.name()),tt.name(),"");
		operator.setMenu(OperatorDictionary.Menu.valueOf(tt.name()));
		return operator;
	}

	/**
	 * <p>Creates an operator for getting BNumber values for a non-visible actor trait.
	 * </p>
	 * */
	public static TraitOperator<FloatTrait> createActorTraitOperator(final Actor.TraitType tt,FloatTrait t){				
		TraitOperator<FloatTrait> operator = new ActorTraitOperator(tt,t,Interpreter.getMethod(Operator.OpType.Read,"ActorTrait"));
		
		operator.setOperatorType(Operator.OpType.Read);
		operator.setDataType(Operator.Type.BNumber);
		operator.addArgument(Operator.Type.Actor,"Who","");
		operator.setMenu(tt==Actor.TraitType.Weight?OperatorDictionary.Menu.Weight:OperatorDictionary.Menu.Actor);
		operator.setToolTipText("produces the "+operator.getLabel()+" value for an Actor");
		
		return operator;
	}

	/**
	 * <p>Creates an operator for setting BNumber values for a perceived trait
	 * of the given type <code>tt</code>. It will have three arguments:
	 * <ul>
	 * <li>The actor perceiving the trait.</li>
	 * <li>The object having the perceived trait which type depends on the trait type.</li>
	 * <li>The BNumber to set.</li>
	 * </ul>
	 * </p>
	 * */
	public static TraitOperator<FloatTrait> createSetPTraitOperator(final Actor.PTraitType att,TraitType tt,FloatTrait t){				
		TraitOperator<FloatTrait> operator = new PTraitOperator(att,t,Interpreter.getMethod(Operator.OpType.Write,"P"+tt.name()+"Trait")) {
			private static final long serialVersionUID = 1L;
			@Override
			public String getLabel() {	return "Set"+(att==Actor.PTraitType.Perception?"P":"C")
												+getTrait().getLabel();	}
		};
		operator.setOperatorType(Operator.OpType.Write);
		operator.setDataType(Operator.Type.Procedure);

		operator.addArgument(Operator.Type.Actor,"FromWhom","");
		if (tt==TraitType.Actor)
			operator.addArgument(Operator.Type.valueOf(tt.name()),"TowardsWhom","");
		else
			operator.addArgument(Operator.Type.valueOf(tt.name()),tt.name(),"");
		operator.addArgument(Operator.Type.BNumber,"BNumber","");			
		if (tt==TraitType.Actor)
			operator.setMenu(OperatorDictionary.Menu.valueOf("Set"+tt.name()+(att==Actor.PTraitType.Perception?"P":"C")));
		else
			operator.setMenu(OperatorDictionary.Menu.valueOf("Set"+tt.name()));
		operator.setToolTipText("Sets the first actor's "+
				Actor.traitName(att,operator.getTrait())+" for the second "+tt.name());
		
		return operator;
	}

	/**
	 * <p>Creates an operator for getting BNumber values of a perceived trait
	 * of the given type <code>tt</code>. It will have two arguments:
	 * <ul>
	 * <li>The actor perceiving the trait.</li>
	 * <li>The object having the perceived trait which type depends on the trait type.</li>
	 * </ul>
	 * </p>
	 * */
	public static TraitOperator<FloatTrait> createGetPTraitOperator(final Actor.PTraitType att,TraitType tt,FloatTrait t){				
		TraitOperator<FloatTrait> operator = new PTraitOperator(att,t,Interpreter.getMethod(Operator.OpType.Read,"P"+tt.name()+"Trait"));
		operator.setOperatorType(Operator.OpType.Read);
		operator.setDataType(Operator.Type.BNumber);

		operator.addArgument(Operator.Type.Actor,"Perceiver","");
		if (tt==TraitType.Actor)
			operator.addArgument(Operator.Type.valueOf(tt.name()),"TowardsWhom","ThisSubject");
		else
			operator.addArgument(Operator.Type.valueOf(tt.name()),tt.name(),"");
		operator.setMenu(att==PTraitType.Perception?OperatorDictionary.Menu.Perception:OperatorDictionary.Menu.Confidence);

		if (tt==TraitType.Actor)
			operator.setToolTipText("produces the "+operator.getLabel()+" for one "+tt.name()+" as perceived by another.");
		else
			operator.setToolTipText("produces the "+operator.getLabel()+" for one "+tt.name()+" as perceived by an actor.");
		
		return operator;
	}

	/**
	 * <p>Creates an operator for getting BNumber values of a perceived weight trait. 
	 * It will have two arguments:
	 * <ul>
	 * <li>The actor perceiving the trait.</li>
	 * <li>The actor having the perceived trait.</li>
	 * </ul>
	 * </p>
	 * */
	public static TraitOperator<FloatTrait> createActorGetPWeightTraitOperator(FloatTrait t){				
		TraitOperator<FloatTrait> operator = new TraitOperator<FloatTrait>(t,Interpreter.getMethod(Operator.OpType.Read,"PActorTraitWeight")) {
			private static final long serialVersionUID = 1L;
			@Override
			public String getLabel() {	return "P"+getTrait().getLabel()+"Weight";	}
		};
		operator.setOperatorType(Operator.OpType.Read);
		operator.setDataType(Operator.Type.BNumber);
		operator.addArgument(Operator.Type.Actor,"Of Whom","");
		operator.addArgument(Operator.Type.Actor,"For Whom","ThisSubject");
		operator.setMenu(OperatorDictionary.Menu.PWeight);
		operator.setToolTipText("produces the "+operator.getLabel()+" of the first Actor for the second Actor.");
		
		return operator;
	}
	
	/** Instantiates the root operator used for scripts of custom operators. */
	public static Operator getReturnedValueOperator(Operator.Type argType,String argName){
		Operator root = new Operator("Returned value",1,null);
		root.setDataType(argType);
		root.addArgument(argType,argName,"");
		root.setOperatorType(Operator.OpType.Read);
		return root;
	}

	/** Creates the Acceptable... operator for the given group type. */
	public static Operator getAcceptableOperator(Operator.Type elemType){
		Operator.Type groupType = Operator.getGroupType(elemType);
		Operator op = new Operator("Acceptable"+elemType.name()+"s",1,Interpreter.getMethod(Operator.OpType.Write,"Acceptable"));
		op.setDataType(groupType);
		op.setOperatorType(Operator.OpType.Write);
		op.addArgument(groupType,"Acceptable"+elemType.name()+"s",null);
		return op;
	}
	
	/** Creates the All...Who/Which operator for the given group type. */
	public static Operator getAllWordsWhichOperator(Operator.Type elemType){
		Operator op;
		Operator.Type groupType = Operator.getGroupType(elemType);
		if (elemType==Operator.Type.Actor) {
			op = new Operator("AllActorsWho",1,null);
			op.setToolTipText("picks all Actors who meet the specifications");
		} else {
			op = new Operator("All"+elemType.name()+"sWhich",1,null);
			op.setToolTipText("picks all "+elemType.name()+"s which meet the specifications");
		}
		op.setIteratorType(elemType);
		op.setDataType(groupType);
		op.setOperatorType(Operator.OpType.Read);
		op.setMenu(Menu.Picking);
		op.addArgument(Operator.Type.Boolean,"Condition", null);
		
		return op;
	}
	
	/** Creates the ...GroupN operator for the given group type. */
	public static Operator getGroupNOperator(Operator.Type elemType,int n){
		Operator.Type groupType = Operator.getGroupType(elemType);
		Operator op = new Operator(elemType.name()+"Group"+n,n,Interpreter.getMethod(Operator.OpType.Read,"Group"+n));
		op.setToolTipText("Produces the union of the provided "+groupType.name()+"s.");
		op.setDataType(groupType);
		op.setOperatorType(Operator.OpType.Read);
		for(int i=1;i<=n;i++)
			op.addArgument(groupType,elemType.name()+"Group"+i,null);
		op.setMenu(Menu.Picking);
		return op;
	}

	/** Creates the ...2Group operator for the given group type. */
	public static Operator getWord2GroupOperator(Operator.Type elemType){
		Operator.Type groupType = Operator.getGroupType(elemType);
		Operator op = new Operator(elemType.name()+"2Group",1,Interpreter.getMethod(Operator.OpType.Read,"Word2Group"));
		op.setToolTipText("creates a group holding the given "+elemType.name());
		
		op.setDataType(groupType);
		op.setOperatorType(Operator.OpType.Read);
		op.setMenu(Menu.Picking);
		op.addArgument(elemType,elemType.name(), null);
		
		return op;
	}

	/** Creates the GroupN...s operator for the given group type. */
	public static Operator getGroupNElemsOperator(Operator.Type elemType,int n){
		Operator.Type groupType = Operator.getGroupType(elemType);
		Operator op = new Operator("Group"+n+elemType.name()+"s",n,Interpreter.getMethod(Operator.OpType.Read,"Group"+n+"Elems"));
		op.setToolTipText("Produces a group with the "+n+" provided "+elemType.name()+"s.");
		op.setDataType(groupType);
		op.setOperatorType(Operator.OpType.Read);
		for(int i=1;i<=n;i++)
			op.addArgument(elemType,elemType.name()+i,null);
		op.setMenu(Menu.Picking);
		return op;
	}

	public static Operator getWordNameOperator(Operator.Type wType){
		Operator op = new Operator(wType.name()+"Name",1,Interpreter.getMethod(Operator.OpType.Read,"WordName"));
		op.setToolTipText("yields the name of the given "+wType.name());
		
		op.setDataType(Operator.Type.Text);
		op.setOperatorType(Operator.OpType.Read);
		switch(wType){
		case Verb:
			op.setMenu(Menu.Verb);
			break;
		case Actor:
		case ActorTrait:
		case MoodTrait:
			op.setMenu(Menu.Actor);
			break;
		case Stage:
		case StageTrait:
			op.setMenu(Menu.Stage);
			break;
		case Prop:
		case PropTrait:
			op.setMenu(Menu.Prop);
			break;
		case Quantifier:
			op.setMenu(Menu.Text);
			break;
		}
		op.addArgument(wType,wType.name(), null);
		
		return op;
	}
}
