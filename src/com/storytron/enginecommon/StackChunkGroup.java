package com.storytron.enginecommon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;



public final class StackChunkGroup implements Serializable {
	private static final long serialVersionUID = 1l;
	public ArrayList<StackChunk> stackChunks = new ArrayList<StackChunk>();
	@SuppressWarnings("unchecked")
	public transient Enumeration enumeration;
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
}
