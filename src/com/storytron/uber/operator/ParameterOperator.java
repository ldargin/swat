package com.storytron.uber.operator;

import Engine.enginePackage.Interpreter;

/** 
 * A ParamenterOperator is a reference to a parameter in a custom operator body.
 * Therefore, it can appear only inside an operator body script. 
 * */
public class ParameterOperator extends Operator {
	private static final long serialVersionUID = 1L;

	private int paramIndex;
	private CustomOperator parentOperator;
	
	/** Creates a parameter operator. */
	public ParameterOperator(CustomOperator parentOperator,int paramIndex){
		super(null,0,Interpreter.getMethod(Operator.OpType.Read,"ParameterOperator"));
		this.parentOperator = parentOperator;
		setOperatorType(Operator.OpType.Read);
		setMenu(OperatorDictionary.Menu.Parameter);
		setParameterIndex(paramIndex);
	}

	/** 
	 * Tells the value of the parameter index. The parameter index
	 * is the position that the parameter has in the list of parameters
	 * of the containing custom operator.
	 * */
	public void setParameterIndex(int paramIndex) {
		this.paramIndex = paramIndex;
	}

	/** Returns the paramenter index. {@see #setParameterIndex(int)} */
	public int getParameterIndex() {
		return paramIndex;
	}
	
	@Override
	public String getLabel() {
		return parentOperator.getArgumentLabel(paramIndex);
	}
	
	@Override
	public Operator.Type getDataType(){
		return parentOperator.getArgumentDataType(paramIndex);
	}
}
