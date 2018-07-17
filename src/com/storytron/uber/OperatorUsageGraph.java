package com.storytron.uber;

import java.util.List;
import java.util.Set;

import com.storytron.enginecommon.Graph;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.CustomOperator;

/** A class for analyzing usage relations among custom operators. */
public final class OperatorUsageGraph {

	/** There is an edge from op1 to op2 in this graph if op1 is used by op2. */
	private final Graph<CustomOperator> usersGraph = new Graph<CustomOperator>(); 
	
	/** Adds a custom operator so its usage relations can be analyzed. */
	public void add(final CustomOperator op){
		op.getBody().traverse(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (n.getOperator() instanceof CustomOperator) {
					addUse((CustomOperator)n.getOperator(),op);
				}
				return true;
			}
		});
	}

	/** Removes a custom operator from the graph. */
	public void remove(final CustomOperator op){
		op.getBody().traverse(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (n.getOperator() instanceof CustomOperator) {
					removeUse((CustomOperator)n.getOperator(),op);
				}
				return true;
			}
		});
	}

	/** Adds a usage relation between to operators (i.e. an edge on the graph). */
	public void addUse(CustomOperator used,CustomOperator user){
		usersGraph.addEdge(used, user);
	}

	/** Removes usage relation between to operators (i.e. an edge on the graph). */
	public void removeUse(CustomOperator used,CustomOperator user){
		usersGraph.removeEdge(used,user);
	}
	
	/** 
	 * Gets the path that describes how execution of user 
	 * implies execution of used. Returns an empty list if execution of
	 * user does not imply execution of used.
	 * */
	public List<CustomOperator> getPath(CustomOperator used,CustomOperator user){
		return usersGraph.getPath(used,user);
	}

	/** 
	 * Clears a given set and adds to it all the operators that used directly or indirectly
	 * a given operator.
	 * */
	public void addUsers(CustomOperator used,Set<CustomOperator> users){
		usersGraph.getReachableNodes(used,users);
	}
	
	/** 
	 * Tells if the usage graph has any cycle.
	 * If there is no cycle an empty list is returned.  
	 * */
	public List<CustomOperator> getCycle(){
		return usersGraph.getCycle();
	}

}
