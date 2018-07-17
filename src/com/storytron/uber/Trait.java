package com.storytron.uber;
import java.io.Serializable;

import com.storytron.enginecommon.Utils;

/** 
 * Used to represent traits for {@link Actor}s, {@link Prop}s and {@link Stage}s.
 * <p>
 * A Trait has a label, a description, and it has an index {@link #valuePosition}
 * which is used to get the trait values from the arrays that
 * are stored in each {@link Actor}, {@link Prop} or {@link Stage}.
 * */
public abstract class Trait implements Comparable<Trait>, Serializable {
	private static final long serialVersionUID = 1l;
	
	public String getLabel(){ return label; };
	public void setLabel(String s){ label=s; }
	
	private String label;
	private final int valuePosition;
	private String description = null;
	
	protected Trait(String label,int valuePosition,String description) {
		this.label = label;
		this.valuePosition = valuePosition;
		setDescription(description);
	}
	int getValuePosition(){ return valuePosition; }
	public int compareTo(Trait o) {
		return Integer.valueOf(valuePosition).compareTo(o.valuePosition);
	};	
	
	public String getDescription(){
		return description;
	}
	public void setDescription(String description){
		this.description = Utils.nullifyIfEmpty(description);
	}
	
	@Override
	public String toString(){
		return getLabel();
	}
}
