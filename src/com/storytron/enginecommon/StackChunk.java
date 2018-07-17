package com.storytron.enginecommon;

import com.storytron.uber.Script;
import com.storytron.uber.operator.Operator;

import java.io.Serializable;

//***********************************************************************
/*
 * About StackChunks and StackChunkGroups
 * These exist solely for the benefit of VisiScript; ideally they should be 
 * removed from the version of Interpreter that runs on the player's machine.
 * (But I can't figure out how to do conditional compilation cleanly.)
 * 
 * A stack chunk is a record of the execution of a single operator by this
 * Interpreter. It records:
 *   1. the value on the top of the stack
 *   2. the dataType of that value
 *   3. the node from the Script that owns the operator
 *   
 *  The first two fields are fairly obvious in their use, but the third field
 *  is another matter. It is used in just one place: in VisiScript, in a short
 *  while-loop whose function is to map StackChunks to the histogram ArrayList.
 *  This is the result of the clumsy (and confusing) relationship between the
 *  sequence order of operators in a Script and the sequence order of the
 *  StackChunks.
 *  
 *  To summarize: a StackChunk gives us a snapshot of the results of the
 *  execution of a single Operator.
 *  
 *  A StackChunkGroup is a sequence of StackChunks recording the step-by-step
 *  results of the execution of a Script (which is a sequence of Operators).
 *  
 *  The boolean variable accumulateChunks enables the accumulation of StackChunks
 *  when true, and disables it when false. It is true only for operations
 *  undertaken by VisiScript.
 */
 
public final class StackChunk implements Serializable {
	private static final long serialVersionUID = 1l;
	float value;
	Operator.Type dataType;
	int nodeId;
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public StackChunk(float stackValue, Operator.Type zDataType, short nodeId) {
		value = stackValue;
		dataType = zDataType;
		this.nodeId = nodeId;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public float getValue() {
		return value;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public Operator.Type getDataType() {
		return dataType;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public Script.Node getNode() {
		//return node;
		return null;
	}
	
	public int getId() {
		return nodeId;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
}
