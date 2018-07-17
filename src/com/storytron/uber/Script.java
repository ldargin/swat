package com.storytron.uber;
import java.awt.Color;
import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

import com.storytron.swat.tree.TNode;
import com.storytron.swat.verbeditor.VerbEditor;
import com.storytron.uber.Role.Option;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;

/**
 * <p>This class holds an expression tree plus some hooks to the place where 
 * the script fits in the storyworld.
 * The nodes of the expression tree are of type {@link DMTN}.
 * <p>
 * The tree can be of some fixed types listed in the enum {@link Script.Type}.
 * <ul>
 * <li>Scripts of type {@link Type#AssumeRoleIf} have an associated 
 * {@link Verb} and {@link Role}. The role being from the verb.</li>
 * <li>Scripts of type {@link Type#Emotion} also have an associated 
 * {@link Verb} and {@link Role}. The role being from the verb, also.</li>
 * <li>Scripts of type {@link Type#Consequence} have an associated {@link Verb}.</li>
 * <li>Scripts of type {@link Type#OptionAcceptable} and {@link Type#OptionDesirable}
 *  have an associated {@link Verb}, {@link Role} and {@link Option}.
 * The option being from the role, and the role being from the verb.</li>
 * <li>Scripts of type {@link Type#Desirable} and {@link Type#Acceptable} 
 * have an associated
 * {@link Verb}, {@link Role}, {@link Option} and index to a wordsocket.
 * The wordsocket being from the {@link Option}, the Option being from the 
 * {@link Role}, and the Role being from the {@link Verb}.</li>
 * <li>Scripts of type {@link Type#OperatorBody} 
 * have an associated
 * {@link CustomOperator}.
 * The custom operator contains the script as its body.</li>
 * </ul>
 * <p>
 * The method {@link #toString()} returns a String representing the associations
 * of this script. Each item is identified with its label. For instance, a 
 * script of type {@link Type#Acceptable} will return 
 * "verbCategoryLabel: verbLabel: roleLabel: optionLabel: wordSocketLabel: Acceptable".
 * At some point this was called the script path. You can also get the path with
 * {@link #getPath()} and the path components with {@link #getScriptLocators()}
 * which is handy if you don't want to parse a string.
 * <p>
 * The Script class also provides a method 
 * {@link #isValid(Operator, com.storytron.uber.Script.Node)},
 * which is used by {@link VerbEditor} to determine which operators are
 * valid to use in a given script node. This method is also used for
 * {@link #sniff()}ing.
 * <p>
 * The {@link #sniff()} method tells if all the nodes in a script have
 * valid operators. This is used when pasting scripts and also when loading a
 * storyworld.
 * <p>
 * There is a method {@link #traverse(com.storytron.uber.Script.NodeTraverser)}
 * which executes the method 
 * {@link NodeTraverser#traversing(Script, com.storytron.uber.Script.Node)}
 * for node in the script. This method is very useful to implement searches
 * or replacements of nodes. As it helps avoiding the hassle of implementing 
 * the explicit recursion. There is a similar method 
 * {@link Deikto#traverseScripts(com.storytron.uber.Script.NodeTraverser)}
 * which is even more powerful as it traverses all the nodes of every script in
 * a storyworld.  
 * */
public final class Script implements Cloneable, Serializable {
	private static final long serialVersionUID = 1l;
	private Node root, nodeUnderConstruction;
	private Script.Type type;
	private int iWordSocket;
	private CustomOperator customoperator;

	public static enum Type {
		/** No type was assigned to this script yet. */
		None,
		/** Scripts executed to decide if an actor plays a given role. */
		AssumeRoleIf,
		/** Scripts executed to decide if a plan must be aborted. */
		AbortIf,
		/** Scripts executed to change the emotional state of an actor playing a role. */
		Emotion,
		/** Scripts executed to change the world state. */
		Consequence,
		/** Scripts executed to decide if an option is acceptable. */
		OptionAcceptable,
		/** Scripts executed to decide how desirable an reaction is. */
		OptionDesirable,
		/** Scripts executed to decide how desirable a candidate is. */
		Desirable,
		/** Scripts executed to decide if an object is eligible. */
		Acceptable,
		/** Scripts returning a label for a wordsocket. */
		WordsocketLabel,
		/** Scripts returning a text to place after a wordsocket. */
		WordsocketSuffix,
		/** Scripts executed for a given custom operator. */
		OperatorBody
	}

//***********************************************************************
	/** 
	 * Nodes for scripts. They have an operator, a description, a boolean
	 * telling if they are expanded, and if the operator is a constant operator there
	 * is also the constant value.
	 * */
	public static final class Node extends DefaultMutableTreeNode implements Serializable, Cloneable, TNode {
		private static final long serialVersionUID = 1l;
		private Operator operator;
		private Object constant;
		private String description="";

		//-----------------------------------------------------------------------
		private Node(Operator tOperator, Object tObject) {
			operator = tOperator;
			constant = tObject;
		}
		//-----------------------------------------------------------------------			
		public String toString() {
			if (operator.getOperatorType()==Operator.OpType.Constant) {
				switch (operator.getDataType()) {
					case Actor: { return ((Actor)getConstant()).getLabel(); }
					case Prop: { return ((Prop)getConstant()).getLabel(); }
					case Stage: { return ((Stage)getConstant()).getLabel(); }
					case Verb: { return ((Verb)getConstant()).getLabel(); }
					case Number: { return String.valueOf(getConstant()); }
					case Boolean: { return String.valueOf((Boolean)getConstant()); }
					case BNumber: { return String.valueOf(getConstant()); }
					case Text: { return (String)getConstant(); }
					case Quantifier: return ((Word)getConstant()).getLabel();
					case PropTrait: 
					case ActorTrait:
					case StageTrait: return ((FloatTrait)getConstant()).getLabel();
					case MoodTrait: return ((Actor.MoodTrait)getConstant()).name();
					default: { System.out.println("Script.Token.toString bad Constant type "); return "-1"; }
				}
			}
			else return(operator.getLabel());
		}
		/** Clones the node and all of its descendants. */
		public Node cloneTree() {
			Node newNode = (Node)clone();
			for (int i = 0; (i < getChildCount()); ++i)
				newNode.add(((Node)getChildAt(i)).cloneTree());
			
			return newNode;
		}

		//-----------------------------------------------------------------------			
		public float getNumericValue(Deikto dk) {
			if (operator.getLabel().endsWith("Constant")) {
				switch (operator.getDataType()) {
					case Actor: { return (float)((Word)getConstant()).getReference().getIndex(); }
					case Prop: { return (float)((Word)getConstant()).getReference().getIndex(); }
					case Stage: { return (float)((Word)getConstant()).getReference().getIndex(); }
					case Verb: { return (float)((Word)getConstant()).getReference().getIndex(); }
					case Number: { return (Float)getConstant(); }
					case Boolean: { return (Float)getConstant(); }
					case Quantifier: { return (float)dk.quantifiers.indexOf(constant); }
					case BNumber: { return (Float)getConstant(); }
					case ActorTrait: { return (float)dk.getActorTraits().indexOf((FloatTrait)getConstant()); }
					case PropTrait: { return (float)dk.getPropTraits().indexOf((FloatTrait)getConstant()); }
					case StageTrait: { return (float)dk.getStageTraits().indexOf((FloatTrait)getConstant()); }
					case MoodTrait: { return (float)((Actor.MoodTrait)getConstant()).ordinal(); }
					default: { return -1.0f; }
				}
			}
			else return -1.0f;
		}
		//-----------------------------------------------------------------------	
		public Object getConstant() {			
			return constant;
		}
		//-----------------------------------------------------------------------
		public void setConstant(Object newObject) { constant = newObject; }
		public void setOperator(Operator op) { operator = op; }
		//-----------------------------------------------------------------------	
		public Operator getOperator() { return operator; }

		/** Sets description for this token. */
		public void setDescription(String description) { this.description = description;}
		/** Gets the description for this token. */
		public String getDescription() {return description;	}

		/** Implementation for interface {@link com.storytron.swat.tree.TNode}. */
		private boolean expanded=true;
		public void setExpanded(boolean expanded) {	this.expanded = expanded; }
		public boolean isExpanded() { return expanded;	}
		/** Sets operator and value for this node. */
		public void setOperatorValue(Operator op,Object value){
			operator=op;
			constant=value;
		}
	}
//***********************************************************************
	/**
	 * Creates a script instance.
	 * <p>
	 * The arguments describe the script location inside a storyworld.
	 * They can be set to null if they are irrelevant for the script location.
	 * For instance, an emotional reaction script does not need an option to be located,
	 * so the tOption argument my be null.
	 * @param tfFillArgs tells if default children must be added to the root node. 
	 * */
	public Script(Script.Type t,int tiWordSocket,CustomOperator op,Operator tOperator, boolean tfFillArgs) {
		root = new Node(tOperator, null);
		nodeUnderConstruction = root;
		type = t;
		iWordSocket = tiWordSocket;
		customoperator = op;

		if (tfFillArgs) { // must add unknown values for the tokens)
			Operator zOperator = root.operator;
			for (int i = 0; i < zOperator.getCArguments(); ++i) {
				String argLabel = zOperator.getArgumentDefaultValue(i);
				if (argLabel==null || argLabel.length()<2)
					argLabel = "?"+zOperator.getArgumentDataType(i)+"?";
				root.add(createNode(OperatorDictionary.getGlobalOperator(argLabel), 0.0f));
			}
		}
	}
	
	public Script.Type getType() { return type; }
	public CustomOperator getCustomOperator() {	return customoperator;	}
	public int getIWordSocket() { return iWordSocket; }

//***********************************************************************
	public String getLabel() {
		if (getType()==Script.Type.OptionAcceptable || getType()==Script.Type.Acceptable)
			return "Acceptable";
		else
			return root.operator.getLabel();
	}
//***********************************************************************
	public static Node createNode(Operator op, Object newConstant) {
		return new Node(op, newConstant);
	}
//***********************************************************************
	public void clear() {
		root = new Node(root.getOperator(),null);
		nodeUnderConstruction = root;
	}
//***********************************************************************
	public Node getRoot() { return root; }
//***********************************************************************
	public void setRoot(Node newRoot) { root = newRoot; }
//***********************************************************************
	public void addNode(boolean isRoot, Operator tOperator, String description,Object constantObject, int tNodeShift) {
		Node zToken;
		
		if (tOperator != null) {
			zToken = new Node(tOperator, constantObject);
			zToken.setDescription(description);
			if (isRoot) {
				root = zToken;
				nodeUnderConstruction = root;
			} else 
				nodeUnderConstruction.add(zToken);
			zToken.setExpanded(isRoot || root==zToken.getParent()
								|| description.length()==0);
		}
		if (!isRoot) {
			switch (tNodeShift) {
				case -1: { nodeUnderConstruction = (Node)nodeUnderConstruction.getParent(); break; }
				case 0: { break; }
				case +1: { nodeUnderConstruction = (Node)nodeUnderConstruction.getLastChild(); break; }
			}
		}		
	}
	public Node getNodeUnderConstruction(){ return nodeUnderConstruction; }
	public void setNodeUnderConstruction(Node n){ nodeUnderConstruction=n; }
//***********************************************************************
	public Color getBaseColor() {	return root.operator.getColor();	}

	/*
	 * <p>Use this interface to pass a method to the script traversing routine.
	 * This method is to be called for each node in the script visited in preorder
	 * (see {@link Script#traverse(com.storytron.uber.Script.NodeTraverser)}).
	 * </p>
	 */
	public static abstract class NodeTraverser {
		public Verb verb;
		public Role.Link role;
		public Option option;
		
		/** Called for each visited node in preorder.
		 * @return false iff the traversal must be interrupted 
		 */
		public abstract boolean traversing(Script s,Node n);
		
		public final void setPath(Verb verb,Role.Link role,Option option) {
			this.verb = verb;
			this.role = role;
			this.option = option;
		}
	}

	/**
	 * <p>Traverses the script nodes using the traverser <code>t</code>.
	 * Use it like this:
	 * <pre>
	s.traverse(new Script.Traverser(){
		public boolean traversing(Script s, DMTN n) {
          do something with s and n
          return true;
      	}
	});
	  </pre>
	 * Here "do something with s and n" will be executed for each node in the
	 * script s visited in preorder (see {@link NodeTraverser}).  
	 * </p>
	 * */
	public void traverse(NodeTraverser t){
		traverse(t,root);
	}
	private boolean traverse(NodeTraverser t,Node n){
		if (t.traversing(this,n)) {
			if (n==root || n.getParent()!=null) {
				for(int i=0; i < n.getChildCount(); ++i) 
					if (!traverse(t,(Node)n.getChildAt(i))) return false;
			}
			return true;
		} else
			return false;
	}
//	***********************************************************************
	@Override
	public Script clone() {
		Script newScript;
		try {
			newScript = (Script)super.clone();
			newScript.root = root.cloneTree();
			return(newScript);
		} catch (CloneNotSupportedException e) {
				throw new Error("This should never happen!");
		}
	}

	/** 
	 * Recursive routine to convert a script to nice html.
	 * Very inefficient, but is used sparingly and scripts are small. 
	 * */
	public String toHtml() {
		return toHtml(getRoot(),"", "&nbsp;");
	}
	private static String toHtml(Node zNode, String tOut, String tabStop) {
		int cNodes = zNode.getChildCount();
		Color baseColor = zNode.getOperator().getColor();
		int colorValue = 65536 * baseColor.getRed() + 256 * baseColor.getGreen() + baseColor.getBlue();
		String colorString = Integer.toHexString(colorValue);
		int j = colorString.length();
		while (j<6) {
			colorString = "0"+colorString;
			++j;
		}
		String zOut = tOut;
		if (zNode.getDescription().length()>0)
			zOut = zOut.concat(tabStop+"<font style=\"background-color: #dddddd\">"+zNode.getDescription()+"</font><br>");
		String colorSetting = "<font color=\"#"+colorString+"\">";
		zOut = zOut.concat(tabStop+colorSetting+zNode.toString()+"</font><br>");
		tabStop = tabStop.concat("&nbsp;&nbsp;&nbsp;");
		for (int i=0; (i < cNodes); ++i)
			zOut = toHtml((Node)zNode.getChildAt(i), zOut, tabStop);
		tabStop = tabStop.substring(0, tabStop.length()-3);
		return zOut;
	}

	/** 
	 * Deletes a node from the script.
	 * @return the new undefined node placed in the script. 
	 * */
	public Node deleteNode(Deikto dk,Node n){
 		Node parent = (Node)n.getParent();
		int iChild = parent.getIndex(n);
		Operator zOperator = n.getOperator();
		Node child = Script.createNode(OperatorDictionary.getUndefinedOperator("?"+zOperator.getDataType()+"?"), 0.0f);
		parent.remove(iChild);
		parent.insert(child, iChild);
		child.setDescription(n.getDescription());
		
		if (dk.usageGraph!=null && getType()==Script.Type.OperatorBody 
				&& n.getOperator() instanceof CustomOperator)
			dk.usageGraph.removeUse((CustomOperator)n.getOperator(),getCustomOperator());
		
		return child;
	}
	
}
