package com.storytron.enginecommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/** 
 * A generic graph implemented with adjacency lists.
 * <p>
 * This implementation contains some algorithm for finding paths
 * between nodes and detecting cycles. 
 * */
public final class Graph<T> {
	
	/** For a node t, usersGraph.get(t) is the set of adjacent nodes. */
	private HashMap<T,Set<T>> adjacentNodes = new HashMap<T,Set<T>>(); 

	/** Adds an edge from {@code from} to {@code to}. */
	public void addEdge(T from,T to){
		Set<T> users = adjacentNodes.get(from);
		if (users==null) {
			users = new HashSet<T>();
			adjacentNodes.put(from, users);
		}
		users.add(to);
	}

	/** Removes an edge from {@code from} to {@code to}. */
	public void removeEdge(T from,T to){
		Set<T> users = adjacentNodes.get(from);
		if (users!=null)
			users.remove(to);
	}

	/** 
	 * Gets the path from {@code from} to {@code to}. Returns an empty list if no 
	 * such path exists.
	 * */
	public List<T> getPath(T from,T to){
		final ArrayList<T> path = new ArrayList<T>(adjacentNodes.size());
		final Set<T> visited = new HashSet<T>((int)(adjacentNodes.size()*1.3));
		getPath(visited,from,to,path);
		return path;
	}

	private boolean getPath(Set<T> visited,T from,T to,ArrayList<T> path){
		if (!visited.add(from))
			return false;
		
		path.add(from);
		Set<T> users = adjacentNodes.get(from);
		if (users!=null) {
			if (users.contains(to)) 
				return true;
			
			for(T userOfUsed:users) {
				if (getPath(visited,userOfUsed,to,path))
					return true;
			}
		}
		path.remove(path.size()-1);
		return false;
	}

	/** 
	 * Clears a given set and adds to it all the nodes that are reachable
	 * from a given one.
	 * */
	public void getReachableNodes(T from,Set<T> reachableNodes){
		reachableNodes.clear();
		mGetReachableNodes(from, reachableNodes);
	}
	
	private void mGetReachableNodes(T from,Set<T> reachableNodes){
		Set<T> adjacents = adjacentNodes.get(from);
		if (adjacents!=null) {
			for(T adjacent:adjacents)
				if (reachableNodes.add(adjacent))
					mGetReachableNodes(adjacent,reachableNodes);
		}
	}

	/** 
	 * Tells if the usage graph has any cycle.
	 * If there is no cycle an empty list is returned.  
	 * */
	public List<T> getCycle(){
		final ArrayList<T> path = new ArrayList<T>(adjacentNodes.size());
		final Set<T> visited = new HashSet<T>((int)(adjacentNodes.size()*1.3));
		for(T op:adjacentNodes.keySet())
			if (hasCycles(visited,op,path))
				break;
		if (path.isEmpty())
			return path;
		else {
			int i= path.indexOf(path.get(path.size()-1));
			return path.subList(i+1, path.size());
		}
	}
	private boolean hasCycles(Set<T> visited,T n,ArrayList<T> path){
		if (!visited.add(n)) {
			if (path.contains(n)) {
				path.add(n);
				return true;
			} else
				return false;
		}

		path.add(n);
		Set<T> adjacents = adjacentNodes.get(n);
		if (adjacents!=null) {
			for(T adjacent:adjacents) {
				if (hasCycles(visited,adjacent,path))
					return true;
			}
		}
		path.remove(path.size()-1);
		return false;
	}

}
