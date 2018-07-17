package com.storytron.uber.operator;
import java.awt.Color;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;

/** 
 * This class represents operators that appear in scripts.
 * <p>
 * They contain a label, a return type and a list of arguments
 * they expect. Each arguments defines a prompt, an expected type,
 * and optionally a default value.
 * <p>
 * Operators also can introduce in scope for their children an iterator
 * like CandidateActor, CandidateStage, etc. In which case, the operator
 * is said to be an iteration.   
 * */
public class Operator implements Serializable, Comparable<Operator> {
		private static final long serialVersionUID = 1L;
		public static enum Type implements Serializable {
			UnType,
			Actor,
			Prop,
			Stage,
			Verb,
			ActorTrait,
			PropTrait,
			StageTrait,
			MoodTrait,
			Quantifier,
			Event,
			Number,
			Boolean,
			Procedure,
			BNumber,
			Text,
			
			ActorGroup,
			PropGroup,
			StageGroup,
			VerbGroup,
			ActorTraitGroup,
			PropTraitGroup,
			StageTraitGroup,
			MoodTraitGroup,
			QuantifierGroup
		}
		public static enum OpType {
			Constant,
			Read,
			Write,
			Procedure,
			Undefined
		}
//		private static final Color gray = new Color(120, 120, 120);
		private static final Color brown = new Color(128, 64, 0);
		private static final Color purple = new Color(64, 0, 128);
		private static final Color dullGreen1 = new Color(0, 128, 128);
		//private static final Color dullGreen2 = new Color(32, 128, 128);
		private static final Color dullGreen3 = new Color(0, 160, 128);
		private static final Color dullGreen4 = new Color(0, 128, 160);
		private static final Color dullGreen5 = new Color(0, 96, 128);
		private static final Color dullGreen6 = new Color(0, 128, 0);
		private static final Color darkRed = new Color(192, 0, 0);
		private static final Color frenchBlue = new Color(0, 192, 255);

		private static final Color ActorGroupColor = getColor(Type.Actor).darker(),
									PropGroupColor = getColor(Type.Prop).darker(),
									StageGroupColor = getColor(Type.Stage).darker(),
									VerbGroupColor = getColor(Type.Verb).darker(),
									ActorTraitGroupColor = getColor(Type.ActorTrait).darker(),
									PropTraitGroupColor = getColor(Type.PropTrait).darker(),
									StageTraitGroupColor = getColor(Type.StageTrait).darker(),
									MoodTraitGroupColor = getColor(Type.MoodTrait).darker(),
									QuantifierGroupColor = getColor(Type.Quantifier).darker();

		private String label, myToolTipText;
		private OpType operatorType;
		private OperatorDictionary.Menu menu=OperatorDictionary.Menu.NONE;
		private ArrayList<String> argumentLabels = new ArrayList<String>();
		private ArrayList<Type> argumentDataType = new ArrayList<Type>();
		private ArrayList<String> defaultValues = new ArrayList<String>();
		private Type dataType; // the index of this data type, using the constants above
		private transient Method method;
		public static final EnumSet<Type> WORDSOCKET_TYPES = EnumSet.range(Type.Actor, Type.Quantifier); 
		
//**********************************************************************	
	public Operator(String tLabel, int tArguments,Method m) {
		label = tLabel;
		method = m;
	}
//**********************************************************************	
	public void setLabel(String tLabel) {
		label = tLabel;
	}
//**********************************************************************	
	public String getLabel() { return label; }
//**********************************************************************
	private Type mIteratorType; 
	void setIteratorType(Type t) {	this.mIteratorType = t; }
	public Type getIteratorType() { return mIteratorType; } 
	public boolean isIteration() {	return mIteratorType!=null; }
//**********************************************************************	
	public void setOperatorType(OpType tTokenType) { operatorType = tTokenType; }
//**********************************************************************	 
	public OpType getOperatorType() { return(operatorType); }
//**********************************************************************	
	public void setDataType(Type t) { dataType = t; }
//**********************************************************************	 
	public Type getDataType() { return dataType; }
//**********************************************************************	 
	public OperatorDictionary.Menu getMenu() { return menu; }
//**********************************************************************	
	public void setMenu(OperatorDictionary.Menu tMenu) { menu = tMenu; }
//**********************************************************************	 
	public int getCArguments() { return argumentLabels.size(); }
//**********************************************************************	 
	public void addArgument(Type t,String targumentLabel,String defaultValue) {
		addArgument(argumentLabels.size(),t,targumentLabel,defaultValue);
	}
	public void addArgument(int iArg,Type t,String targumentLabel,String defaultValue) {
		argumentLabels.add(iArg,targumentLabel);
		defaultValues.add(iArg,defaultValue);
		argumentDataType.add(iArg,t);
	}
//**********************************************************************	 
	public void setArgumentLabel(int tIndex, String targumentLabel) {
		argumentLabels.set(tIndex, targumentLabel);
	}
//**********************************************************************	 
	public String getArgumentLabel(int tIndex) {
		return(argumentLabels.get(tIndex));
	}
//	**********************************************************************	 
	public Type getArgumentDataType(int tIndex) {
		return argumentDataType.get(tIndex);
	}
//**********************************************************************	 
	public String getArgumentDefaultValue(int tIndex) {
		return defaultValues.get(tIndex);
	}
	public void removeArgument(int i){
		if (i<defaultValues.size())
			defaultValues.remove(i);
		if (i<argumentDataType.size())
			argumentDataType.remove(i);
		if (i<argumentLabels.size())
			argumentLabels.remove(i);
	}
//**********************************************************************	 
	public void setToolTipText(String tToolTipText) {
		myToolTipText = tToolTipText;
	}
//**********************************************************************	 
	public String getToolTipText() { return (myToolTipText); }
//**********************************************************************	 
	public Color getColor() { return getColor(getDataType()); }
//**********************************************************************
	public static Type getElementType(Type t){
		switch (t) {
		case ActorGroup: return Type.Actor;
		case PropGroup: return Type.Prop;
		case StageGroup: return Type.Stage;
		case VerbGroup: return Type.Verb;
		case ActorTraitGroup: return Type.ActorTrait;
		case PropTraitGroup: return Type.PropTrait;
		case StageTraitGroup: return Type.StageTrait;
		case MoodTraitGroup: return Type.MoodTrait;
		case QuantifierGroup: return Type.Quantifier;
		default: return Type.UnType;
		}
	}
	public static Type getGroupType(Type t){
		switch (t) {
		case Actor: return Type.ActorGroup;
		case Prop: return Type.PropGroup;
		case Stage: return Type.StageGroup;
		case Verb: return Type.VerbGroup;
		case ActorTrait: return Type.ActorTraitGroup;
		case PropTrait: return Type.PropTraitGroup;
		case StageTrait: return Type.StageTraitGroup;
		case MoodTrait: return Type.MoodTraitGroup;
		case Quantifier: return Type.QuantifierGroup;
		default: return Type.UnType;
		}
	}
	public static Color getColor(Type t) {
		switch (t) {
		case UnType: return Color.gray;
		case Actor: return Color.blue;
		case Prop: return Color.magenta;
		case Stage: return Color.orange;
		case Verb: return dullGreen6;
		case ActorTrait: return dullGreen1;
		case PropTrait: return dullGreen3;
		case StageTrait: return dullGreen4;
		case MoodTrait: return dullGreen5;
		case Quantifier: return purple;
		case ActorGroup: return ActorGroupColor;
		case PropGroup: return PropGroupColor;
		case StageGroup: return StageGroupColor;
		case VerbGroup: return VerbGroupColor;
		case ActorTraitGroup: return ActorTraitGroupColor;
		case PropTraitGroup: return PropTraitGroupColor;
		case StageTraitGroup: return StageTraitGroupColor;
		case MoodTraitGroup: return MoodTraitGroupColor;
		case QuantifierGroup: return QuantifierGroupColor;
		case Event: return frenchBlue;
		case Number: return darkRed;
		case Boolean: return Color.black;
		case Procedure: return Color.black;
		case BNumber: return Color.red;
		case Text: return brown;
		default: return Color.black;
		}
	}
//**********************************************************************	
	public String getMyToolTipText() { return myToolTipText; }
//**********************************************************************	
	public void setMyToolTipText(String newToolTipText) { myToolTipText = newToolTipText; }
//	**********************************************************************	
	public Method getMethod() { return method; }
//**********************************************************************	
	@Override
	public String toString(){ return getLabel(); }
	//**********************************************************************
	public int compareTo(Operator o) {
		return getLabel().compareToIgnoreCase(o.getLabel());
	}
}
