package com.storytron.uber;

import java.util.LinkedList;

import com.storytron.uber.Role.Option;
import com.storytron.uber.Script.Node;
import com.storytron.uber.Script.Type;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;

/** A class for identifying and validating scripts. */
public final class ScriptPath {
	public static final int NESTING_LIMIT = 8; 

	private Verb verb;
	private Role.Link role;
	private Option option;

	public ScriptPath(Verb tVerb,Role.Link tRole,Option tOption) {
		verb = tVerb;
		role = tRole;
		option = tOption;
	}

	public Verb getVerb() { return(verb); }
	public Role.Link getRole() { return(role); }
	public Option getOption() { return(option); }
	
	/** 
	 * Returns the path for the script.
	 * <p>
	 * The path describes where the script fits in the storyworld.
	 * If you need to work with the path decomposition use
	 * {@link #getScriptLocators()} instead. This method is just
	 * the concatenation of path component names with the separator ": ".  
	 * */
	public String getPath(Script script) {
		final String sep = ": ";
		String s = verb!=null ? verb.getCategory()+sep : "";
		switch(script.getType()){
		case AssumeRoleIf:
			return s+verb.getLabel()+sep+role.getLabel()+sep+"AssumeRoleIf";
		case AbortIf:
			return s+verb.getLabel()+sep+"AbortIf";
		case Emotion:
			return s+verb.getLabel()+sep+role.getLabel()+sep + "Emotion"+sep+script.getLabel();
		case Consequence:
			return s+verb.getLabel()+sep+ "Consequence" + sep +script.getLabel();
		case OptionDesirable:
			return s+verb.getLabel()+sep+role.getLabel()+sep+option.getLabel()+sep+"Desirable";
		case OptionAcceptable:
			return s+verb.getLabel()+sep+role.getLabel()+sep+option.getLabel()+sep+"Acceptable";
		case Desirable:
			return s+verb.getLabel()+sep+role.getLabel()+sep+option.getLabel()+sep+option.getPointedVerb().getWordSocketFullLabel(script.getIWordSocket())+sep+"Desirable";
		case Acceptable:
			return s+verb.getLabel()+sep+role.getLabel()+sep+option.getLabel()+sep+option.getPointedVerb().getWordSocketFullLabel(script.getIWordSocket())+sep+"Acceptable";
		case OperatorBody:
			return script.getCustomOperator().getLabel();
		case WordsocketLabel:
			return s+verb.getLabel()+sep+verb.getWordSocketFullLabel(script.getIWordSocket());
		case WordsocketSuffix:
			return s+verb.getLabel()+sep+verb.getWordSocketFullLabel(script.getIWordSocket())+sep+"Suffix";
		default:
			return "unknown script type: "+script.getType().name();
		}
	}
	
	/** 
	 * Returns the script strings that indicate where the script is placed in the
	 * storyworld. The strings alone do not suffice for that, but together with 
	 * the script type they do.
	 * <p>
	 * This method is a convenient to avoid parsing the script path when the locators
	 * are needed.  
	 * */
	public String[] getScriptLocators(Script script) {		 
		switch(script.getType()){
		case AssumeRoleIf:
			return new String[]{ verb.getLabel(), role.getLabel()};
		case AbortIf:
			return new String[]{ verb.getLabel()};
		case Emotion:
			return new String[]{ verb.getLabel(), role.getLabel(), script.getLabel()};
		case Consequence:
			return new String[]{ verb.getLabel(), script.getLabel()};
		case OptionDesirable:
		case OptionAcceptable:
			return new String[]{ verb.getLabel(), role.getLabel(), option.getLabel()};
		case Desirable:
		case Acceptable:
			return new String[]{ verb.getLabel(), role.getLabel(), option.getLabel(), option.getPointedVerb().getWordSocketFullLabel(script.getIWordSocket())};
		case WordsocketLabel:
			return new String[]{ verb.getLabel(), verb.getWordSocketFullLabel(script.getIWordSocket())};
		case WordsocketSuffix:
			return new String[]{ verb.getLabel(), verb.getWordSocketFullLabel(script.getIWordSocket())};
		case OperatorBody:
			return new String[]{ script.getCustomOperator().getLabel() };
		default:
			return null;
		}
	}

	/** 
	 * Returns the path components.
	 * <p>
	 * This method is a convenient to avoid parsing the script path when it need to be
	 * edited in some special way.  
	 * */
	public String[] getPathComponents(Script script) {		 
		switch(script.getType()){
		case AssumeRoleIf:
			return new String[]{ verb.getLabel(), role.getLabel(),"AssumeRoleIf"};
		case AbortIf:
			return new String[]{ verb.getLabel(), "AbortIf"};
		case Emotion:
			return new String[]{ verb.getLabel(), role.getLabel(), script.getLabel()};
		case Consequence:
			return new String[]{ verb.getLabel(), script.getLabel()};
		case OptionDesirable:
			return new String[]{ verb.getLabel(), role.getLabel(), option.getLabel(), "Desirable"};
		case OptionAcceptable:
			return new String[]{ verb.getLabel(), role.getLabel(), option.getLabel(), "Acceptable"};
		case Desirable:
			return new String[]{ verb.getLabel(), role.getLabel(), option.getLabel(), option.getPointedVerb().getWordSocketFullLabel(script.getIWordSocket()), "Desirable"};
		case Acceptable:
			return new String[]{ verb.getLabel(), role.getLabel(), option.getLabel(), option.getPointedVerb().getWordSocketFullLabel(script.getIWordSocket()), "Acceptable"};
		case WordsocketLabel:
			return new String[]{ verb.getLabel(), verb.getWordSocketFullLabel(script.getIWordSocket())};
		case WordsocketSuffix:
			return new String[]{ verb.getLabel(), verb.getWordSocketFullLabel(script.getIWordSocket()), "Suffix"};
		case OperatorBody:
			return new String[]{ script.getCustomOperator().getLabel() };
		default:
			return null;
		}
	}

	/** @return true iff the script contains undefined operators. */
	public static boolean containsUndefinedOperators(Script s) {
		return containsUndefinedOperators(s.getRoot());
	}
	private static boolean containsUndefinedOperators(Node n) {
		if (n.getOperator()!=null && OperatorDictionary.getUndefinedOperator(n.getOperator().getLabel())!=null)
			return true;
		for (int i=0; i < n.getChildCount(); ++i){
			if (containsUndefinedOperators((Node)n.getChildAt(i)))
				return true;
		}
		return false;
	} 
	
	/** 
	 * @return null if all the script nodes have valid operators,
	 *         otherwise it returns an error string.
	 * */
	public String sniff(Script script) {
		return sniffNode(script,script.getRoot());
	}
	/** 
	 * @return null if all the nodes in the subtree rooted at a given 
	 *         node have valid operators, otherwise it returns an error string.
	 * */
	public String sniffNode(Script script,Node zNode) {
		Operator zOperator=zNode.getOperator();
		// Here follow the various tests for correctness
		// First: is argument count correct?
		if (zNode.getChildCount() != zOperator.getCArguments())
			return "Invalid argument count for "+zOperator.getLabel();

		String result=isValid(script,zOperator,zNode);
		if (result!=null)
			return result;
		
		for (int i=0; i < zNode.getChildCount(); ++i){
			result = sniffNode(script,(Node)zNode.getChildAt(i));
			if (result!=null) return result;
		}

		return null;
	}

	
	/**
	 * Returns an error string if the operator is not valid for a given node 
	 * in the script. Returns null if the operator is valid.
	 * <p>
	 * This code is intended to be used for sniffing as well
	 * as for selecting the correct operators to offer when editing
	 * a script.
	 **/
	public String isValid(Script script,Operator op,Node zNode){
		if (zNode==null || zNode.getParent()==null) return null;
		/* Here's where we get hairy. There are lots of factors to consider when including
	 	or excluding an Operator in an OperatorMenu. Here's my list:
	 	1. The dataType of the Operator must match the dataType of the selected Operator
	 	2. Special case: if this OperatorMenu is the "Chosen" OperatorMenu, then we have
	 	     a constraint on the Operators: the WordSocket to which the Operator refers
	 	     must have been filled in previous to this Script. Example: the Operator
	 	     "ChosenSubject1" is included only if a) the WordSocket "ChosenSubject1"
	 	     is active for the Option verb; b) the WordSocket that we are filling in
	 	     is greater than the ChosenSubject1 WordSocket (meaning that it is executed
	 	     AFTER the ChosenSubject1 WordSocket); c) the Script being edited is part
	 	     of an Option WordSocket script -- this condition is specified by the value
	 	     of Script.getIWordSocket() for the Script being edited. If that value is
	 	     -1, then the Script is a Consequence, AssumeRoleIf, or EmotionalReaction
	 	     Script. If it is equal to Sentence.MaxSockets, then it is an Inclination
	 	     Script. I think that you should replace this coding system with something a
	 	     little more comprehensible?
		3. Special case: if the operator is a "Candidate" operator (right now specified
		     by the fact that its label begins with "Candidate"), then it can only be
		     included when a Candidate is legal for inclusion in this Script. This
		     calculation is so complicated that I broke it off into its own method,
		     isCandidateLegal(), which I have annotated separately.
		4. Special case: if the menu in question is the "ThisEvent" menu, then the
		     operator should be included only if the WordSocket for the Verb being edited 
		     corresponds to the the WordSocket referred to in the operator. For example,
		     the Operator "ThisSubject1" can only be included if, for the Verb being
		     edited, Verb.isWordSocketActive[wordSocketIndex] is true.
		5. Special case: if the Operator is a "Box"-type Operator (currently specified
		     by the label ending in "Box"), then it can be included only if that box
		     has already been filled in a Script above it in the Verb-Role-Option
		     hierarchy. For example, the Operator "RoleVerbBox" can only be included
		     if the Role containing this Script includes a Script beginning with
		     "FillRoleVerbBox". The goal here is to guarantee that the storybuilder can
		     use the RoleVerbBox Operator only AFTER the RoleVerbBox has been filled.
	*/

		int childIndex=zNode.getParent().getIndex(zNode);
		Operator nOp = ((Node)zNode.getParent()).getOperator();
		if (op.getDataType()!=nOp.getArgumentDataType(childIndex)) 
			return "Argument "+childIndex+" ("+nOp.getArgumentDataType(childIndex)+") of "+nOp.getLabel()+
				" does not match the type of "+op.getLabel()+" ("+op.getDataType()+")";

		// Next test: if this is an Operator from the "Chosen" OperatorMenu, is the WordSocket
		// to which it refers already filled in?
		else if (op instanceof CustomOperator) {
			if (script.getType()==Script.Type.OperatorBody && op.getLabel().equals(script.getCustomOperator().getLabel()))
				return "The custom operator "+op.getLabel()+" calls itself.";
			else 
				return null;
		} else if (op.getMenu()==OperatorDictionary.Menu.Chosen) 
			if (script.getType()!=Script.Type.Acceptable && script.getType()!=Script.Type.Desirable && script.getType()!=Script.Type.OptionDesirable)
				return "Chosen operators are valid only in Desirable scripts and wordsocket Acceptable scripts.";
			else {
				Verb v = getOption().getPointedVerb();
				int i=Verb.getWordSocketIndex(op.getLabel().substring(6)); // because "Chosen" is 6 chars long
				if (i>=script.getIWordSocket())
					return op.getLabel()+" is premature";
				else if (!v.isWordSocketActive(i))
					return "WordSocket "+((Integer)i).toString()+" is not active in verb "+v.getLabel();
 			} 
		
		// Next test: if this is a Candidate Operator, is it legal?
		else if (op.getLabel().startsWith("Candidate") && 
				!isCandidateLegal(script,zNode, op))
			return "Orphan "+op.getLabel();

		// Next test: verify that iterations are not too much nested
		else if (op.isIteration() && amountOfIterations((Node)zNode.getParent())>=NESTING_LIMIT)
			return "Too much nested iterations at operator "+op.getLabel();
		
		// Next test: if this is an operator from the ThisEvent menu, does thisEvent actually
		// contain the socket to which this operator refers?
		else if (op.getMenu()==OperatorDictionary.Menu.ThisEvent) {
			// we use a 4 here to skip the string 'This' and isolate the socket label
			String label = op.getLabel();
			if (script.getType()==Script.Type.OperatorBody)
				return "Use of "+label+" when there is no event in scope.";
			else if (label.startsWith("This") && Character.isDigit(label.charAt(4))) {
				int end = 5;
				while (Character.isDigit(label.charAt(end))) 
					end++;
				Integer iSocket = Integer.parseInt(label.substring(4,end))-1;
				if (!verb.isWordSocketActive(iSocket))
					return "Use of "+op.getLabel()+" when verb '"+getVerb().getLabel()+"' doesn't use that socket";
				else if (!verb.getWordSocketBaseLabel(iSocket).equals(label.substring(end)))
					return "Use of "+op.getLabel()+" when verb '"+getVerb().getLabel()+"' has the socket with a different type";
				else if ((script.getType()==Script.Type.WordsocketSuffix || script.getType()==Script.Type.WordsocketLabel) &&
							iSocket>script.getIWordSocket())
					return label+" is premature";
				else
					return null;
			} else if (label.equals("ThisDirObject"))
				if (!verb.isWordSocketActive(Sentence.DefDirObject))
					return "Use of "+label+" when verb '"+getVerb().getLabel()+"' doesn't use that socket";
				else if (verb.getWordSocketType(Sentence.DefDirObject)!=Operator.Type.Actor)
					return "Use of "+label+" when verb '"+getVerb().getLabel()+"' has the socket with a different type";
				else if ((script.getType()==Script.Type.WordsocketSuffix || script.getType()==Script.Type.WordsocketLabel) &&
						Sentence.DefDirObject>script.getIWordSocket())
					return label+" is premature";
				else 
					return null;
			else
				return null;
		}
		else if (op.getMenu()==OperatorDictionary.Menu.Boxes) {
			// determine if this Box has been assigned a value earlier
			// First, is it a VerbBox, a GlobalBox or a RoleBox?
			String boxType = op.getLabel().substring(0,4);
			if (boxType.equals("Glob"))
				return null;
			else if (boxType.equals("Verb")) {
				if (script.getType()==Script.Type.OperatorBody)
					return "Use of "+op.getLabel()+" when there is no event in scope.";
				
				for (Script sc: getVerb().getConsequences()) {
					if (sc == script) break;
					if (sc.getLabel().endsWith(op.getLabel()))
						return null;
				}
			} else if (boxType.equals("Role")) {
				if (script.getType()==Script.Type.OperatorBody)
					return "Use of "+op.getLabel()+" when there is no event in scope.";
				
				if (getRole()==null)
					return "Use of "+op.getLabel()+" outside the scope of a role.";
				for (Script sc: getRole().getRole().getEmotions()) {
					if (sc == script) break;
					if (sc.getLabel().endsWith(op.getLabel()))
						return null;
				}
			}
			return "Use of "+op.getLabel()+" when that Box has not been filled.";
		}
		// ReactingActor makes sense in role scripts.
		else if (op.getLabel().equals("ReactingActor"))
			switch (script.getType()) {
			case Emotion:
			case AssumeRoleIf:
			case OptionAcceptable:
			case OptionDesirable:
			case Acceptable:
			case Desirable:
				break;
			default:
				return "Use of ReactingActor makes sense only in scripts related to roles and options.";
			}
		else if (script.getType()==Script.Type.AbortIf && (
					op.getLabel().equals("ReactingActor")
					// Forbid history book lookups in abort scripts.
					|| op.getLabel().equals("LookUpCausalEvent")
					|| op.getLabel().equals("CausalEventHappened")))
			return "Use of "+op.getLabel()+" does not make sense in an abort script."; 
		else if (script.getType()!=Script.Type.OptionDesirable 
					&& script.getType()!=Script.Type.Acceptable 
					&& script.getType()!=Script.Type.Desirable 
					&& op.getLabel().startsWith("IHaventDoneThis"))
				return "Use of "+op.getLabel()+" is allowed only in Desirable scripts or in wordsocket Acceptable scripts.";
		else if (op==OperatorDictionary.getSameAsThisOneOperator()) {
			if (script.getType()!=Script.Type.Acceptable)
				return "The operator SameAsThisOne is allowed only in wordsocket Acceptable scripts.";
			else if (!getVerb().isWordSocketActive(script.getIWordSocket()))
				return "Wordsocket This"+((Integer)(1+script.getIWordSocket())).toString()+"SomeType is not active,"
						+" making use of SameAsThisOne illegal in this script.";
			else if (getVerb().getWordSocketType(script.getIWordSocket())!=getOption().getPointedVerb().getWordSocketType(script.getIWordSocket()))
				return "Type of This"+ getVerb().getWordSocketFullLabel(script.getIWordSocket())
						+" does not match type of "+getOption().getPointedVerb().getWordSocketFullLabel(script.getIWordSocket())
						+" making use of SameAsThisOne illegal.";
		} else if (op.getDataType()==Operator.Type.Text) { 
			if (op==OperatorDictionary.getNominativePronounOperator() 
				|| op==OperatorDictionary.getGenitivePronounOperator()
				|| op==OperatorDictionary.getAccusativePronounOperator()
				|| op==OperatorDictionary.getReflexivePronounOperator()) {
					if (script.getType()!=Script.Type.WordsocketLabel && script.getType()!=Script.Type.WordsocketSuffix)
						return op.getLabel()+" is allowed only in wordsocket text scripts.";
//					else if (getVerb().getWordSocketType(script.getIWordSocket())!=Operator.Type.Actor)
//						return op.getLabel()+" is allowed only in actor wordsocket text scripts.";
			} else if (op==OperatorDictionary.getTheNameOperator() 
						&& script.getType()!=Script.Type.WordsocketLabel && script.getType()!=Script.Type.WordsocketSuffix)
				return op.getLabel()+" is allowed only in wordsocket text scripts.";
		} else if (script.getType()!=Script.Type.Acceptable && script.getType()!=Script.Type.Acceptable 
					&& op.getLabel().length()==15 
					&& op.getLabel().startsWith("IsAMemberOf")
					&& (op.getLabel().endsWith("9Set")
						|| op.getLabel().endsWith("7Set")
						|| op.getLabel().endsWith("5Set")
						|| op.getLabel().endsWith("3Set")))
			return "IsAMemberOfNSet is allowed only in wordsocket Acceptable scripts.";
		else if (script.getType()!=Type.AssumeRoleIf && op.getLabel().equals("FatesRole"))
			return "The operator FatesRole is allowed only in AssumeRoleIf scripts.";

		return null;
	}

	
	/** Returns the valid operators of a menu for a given node of the script.  
	 * @see #isValid(Operator, com.storytron.uber.Script.Node) 
	 * */
	public LinkedList<Operator> getValidOperators(Iterable<Operator> ops,Script script,Script.Node tNode) {
		LinkedList<Operator> l = new LinkedList<Operator>();

		if (tNode==null) 
			return l;
		for (Operator op: ops)
			if (isValid(script,op,tNode)==null)
				l.add(op);
		return l;
	}

	/** @return the amount of iterations in the path to the root. */
	private int amountOfIterations(Node n) {
		int count=0;
		while(n!=null) {
			if (n.getOperator()!=null && n.getOperator().isIteration())
				count++;
			n = (Node)n.getParent();
		}
		return count;
	}
	
	/** 
	 * Determines if a Candidate operator is legal.
	 * <p>
	 * Candidates operators are legal only under an iteration node.
	 * This requires us to examine all the ancestors of the operator in the script 
	 * to see if any one of them is an iteration. PickBest operators, Group operators
	 * and history lookup operators are iterations. 
	 */ 
	private boolean isCandidateLegal(Script script,Node tNode, Operator tOperator) {
		if (tNode != null) {
			Node zNode = (Node)tNode.getParent();
			while (zNode != null) {
				Operator zOperator = zNode.getOperator();
				String zLabel = zOperator.getLabel();
				if (zOperator.isIteration() && zOperator.getIteratorType() == tOperator.getDataType())
					return true;
				if ((script.getType()==Script.Type.Acceptable && zLabel.equals("Acceptable") || 
						script.getType()==Script.Type.Desirable && zLabel.equals("Desirable")) 
					&& script.getIWordSocket()>=0 
					&& tOperator.getDataType() == getOption().getPointedVerb().getWordSocketType(script.getIWordSocket()))
					return true;
				zNode = (Node)zNode.getParent();
			}
		}
		return false;
	}

}
