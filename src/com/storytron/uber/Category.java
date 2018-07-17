package com.storytron.uber;
import java.util.ArrayList;

/** This is a tree class designed to hold verb categories.
 * Note: this class doesn't contain the verbs; it links 
 * to verbs through the category name
 * */
public final class Category {
	String name;
	Category parent = null;
	Boolean isLeaf = true;
	
	ArrayList<Category> children = new ArrayList<Category>();

	//	 Generate a root category
	public Category(Deikto tdk) {
		super();
		this.name = "root";
	}

	//	 Generate a root category
	public Category(String name) {
		super();
		this.name = name;
	}

	// Generate a child category
	public Category(String name, Category parent) {
		super();

		this.name = name;
		this.parent = parent;
		if (parent != null) {
			parent.isLeaf = false;
		}
	}

	public Boolean getIsLeaf() {
		return isLeaf;
	}
	public void setIsLeaf(Boolean isLeaf) {
		this.isLeaf = isLeaf;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Category getParent() {
		return parent;
	}
	public void setParent(Deikto dk,int index,Category newParent) {
		// verify that the new parent has no verbs, and is not a child of this category
		if ((newParent.hasNumVerbs(dk) == 0) && !this.hasChild(newParent))
		{
			int i=this.parent.getIndex(this);
			if (i!=-1 && i<index) index--;
			this.parent.removeChild(this);
			this.parent = newParent;
			newParent.addChild(index,this);
		}
	}
	
	public void setParent(Deikto dk,Category newParent) {
		setParent(dk,newParent,newParent.children.size());
	}
	public void setParent(Deikto dk,Category newParent, int pos) {
		if (newParent.hasNumVerbs(dk) == 0 && !this.hasChild(newParent))
		{
			this.parent.removeChild(this);
			this.parent = newParent;
			newParent.addChild(pos>=newParent.children.size()?pos-1:pos, this);
		}
	}
	
	public int getIndex(Category cat){
		return getChildren().indexOf(cat);
	}
	
	public void addChild(Category child) {
		children.add(child);
	}
	public void addChild(int index,Category child) {
		children.add(index,child);
	}
	
	public void removeChild(Category child) {
		children.remove(child);
	}
	
	// traverse all child nodes for a specific child category
	private Boolean hasChild(Category child) {
		Boolean found = false;
		for (Category search: children) {
			if (search == child) {
				found = true;
				break;
			} else if (search.hasChild(child)) {
				found = true;
				break;
			} else
				found = false;
		}
		
		return found;
	}
	public Category findChild(String childName) {
		Category result = null;
		for (Category search: children) {
			if (search.name.equals(childName)) {
				result = search;
				break;
			} else {
				result = search.findChild(childName);
				if (result != null)
					break;
			}
		}
		
		return result;
		
	}
	
	// Return the number of verbs in this category
	public Integer hasNumVerbs(Deikto dk) {

		Integer verbsFound = 0;
		for (int i=0; i< dk.getVerbCount(); ++i) {
			if (dk.getVerb(i).getCategory().equals(name)) {
				++verbsFound;
				break;
			}
		}
		return verbsFound;
	}
	
	/** Get an array of verbs that use this category */
	public ArrayList<String> getVerbNames(Deikto dk) {
		ArrayList<String> verbList = new ArrayList<String>();

		for (int i=0; i< dk.getVerbCount(); ++i) {
			if (dk.getVerb(i).getCategory().equals(name)) {
				verbList.add(dk.getVerb(i).getLabel());
			}
		}
		return verbList;
	}

	// Get the list of this category nodes immediate children
	public ArrayList<Category> getChildren() {
		return children;
	}

	// Determine whether any verbs are under this category or any sub-categories
	public  boolean hasAnyVerbs(Deikto dk) {
		boolean result = false;

		if(hasNumVerbs(dk) ==0 && children.isEmpty() == true) {
			// this category is a dead end
			result = false;
		} else if (hasNumVerbs(dk) > 0) {
			// this category has verbs directly under it
			result = true;
		} else {
			// Search the sub-categories
			for (Category search: children) {
				if (search.hasAnyVerbs(dk) == true) {
					result = true;
					break;
				}
			}
		}
		
		return result;
		
	}
}
