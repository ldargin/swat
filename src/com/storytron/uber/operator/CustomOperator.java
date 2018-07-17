package com.storytron.uber.operator;

import java.util.ArrayList;

import Engine.enginePackage.Interpreter;

import com.storytron.uber.Script;

/** 
 * A custom operator has an associated script.
 * <p>
 * Custom operators and their attributes are supposed to be edited
 * by authors.
 * */
public class CustomOperator extends Operator {
	private static final long serialVersionUID = 1L;
	private Script body;
	private ArrayList<ParameterOperator> parameterOperators;
	
	public CustomOperator(String label,int cargs){
		super(label,cargs,Interpreter.getMethod(Operator.OpType.Read,"CustomOperator"));
		setMenu(OperatorDictionary.Menu.Custom);
		setOperatorType(OpType.Read);
		parameterOperators = new ArrayList<ParameterOperator>(cargs);
		for(int i=0;i<cargs;i++)
			parameterOperators.add(new ParameterOperator(this,i));
	}

	@Override
	public void removeArgument(int arg) {
		parameterOperators.remove(arg);
		for(int i=arg;i<parameterOperators.size();i++)
			parameterOperators.get(i).setParameterIndex(i);
		super.removeArgument(arg);
	}

	@Override
	public void addArgument(int arg, Type t, String targumentLabel, String defaultValue) {
		addArgument(new ParameterOperator(this,arg), arg, t, targumentLabel, defaultValue);
	}
	
	/** 
	 * Adds an argument to the custom operator. Specifies a parameter operator to use
	 * instead of instantiating a new one.
	 * */
	public void addArgument(ParameterOperator r,int arg, Type t, String targumentLabel, String defaultValue){
		parameterOperators.add(arg, r);
		for(int i=arg;i<parameterOperators.size();i++)
			parameterOperators.get(i).setParameterIndex(i);
		super.addArgument(arg, t, targumentLabel, defaultValue);
	}

	/** Yields the operator for a given parameter. */
	public ParameterOperator getParameterOperator(int i){
		return parameterOperators.get(i);
	}
	
	public void setBody(Script body) {
		this.body = body;
	}

	public Script getBody() {
		return body;
	}
}
