package com.storytron.swat.loglizard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;

import Engine.enginePackage.TreeLogger;

import com.storytron.swat.util.BufferedArray;
import com.storytron.swat.util.CachedArray;
import com.storytron.swat.util.Compressed;
import com.storytron.swat.util.CompressedArray;
import com.storytron.swat.util.DiskArray;

/** 
 * Compact representation of a tree.
 * Each node is identified by an integer identifier, which is
 * used to retrieve every property of the node.
 * <p>
 * The tree is stored on a temporal file on disk. This is only
 * for keeping big trees out of the memory. 
 * */
public final class DiskIndexedTree implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final int CHUNK_SIZE = 16000;
	private static final int CHUNK_BYTES = CHUNK_SIZE*4;
	
	private DiskArray<Object[]> nda = new DiskArray<Object[]>(new DiskArray.ReadWriter<Object[]>(){
		private final ByteArrayOutputStream bos = new ByteArrayOutputStream(4+CHUNK_BYTES);
		private final DataOutputStream dos = new DataOutputStream(bos);
		public Object[] read(RandomAccessFile raf,int bytes) throws IOException {
			int length=raf.readInt();
			Object[] arr=new Object[length];
			byte[] buf = new byte[CHUNK_BYTES];
			int count=raf.read(buf);
			final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf,0,count));
			for(int i=0;i<length;i++)
				arr[i]=dis.readInt();
			return arr;
		}
		public int write(Object[] e, RandomAccessFile raf,int available) throws IOException {
			if (available<4+e.length*4)
				return 4+e.length*4;
			bos.reset();
			dos.writeInt(e.length);
			for(int i=0;i<e.length;i++)
				dos.writeInt((Integer)e[i]);
			raf.write(bos.toByteArray());
			return 4+e.length*4;
		}
	});
	private BufferedArray<Integer> nodes = new BufferedArray<Integer>(CHUNK_SIZE,new CachedArray<Object[]>(25,nda));
	
	private DiskArray<Compressed<Object[]>> pda = new DiskArray<Compressed<Object[]>>(); 
	private BufferedArray<Object> params = new BufferedArray<Object>(CHUNK_SIZE,new CachedArray<Object[]>(25,new CompressedArray<Object[]>(pda)));
	
	/** 
	 * Builds a compact tree representation for a 
	 * log tree. 
	 * */
	public DiskIndexedTree() throws IOException {
		addNode();
		addNode();
	}
	
	/** 
	 * <p>Adds a child with the given parameters to the given node.
	 * The root has index 0 and no parameters.</p>
	 * @return the identifier of the newly created node.
	 * */
	public int addChild(int parent,Object[] params) {
		return addChild(parent,-1,params);
	}
	
	/** 
	 * <p>Adds a child with the given parameters to the given node.
	 * The root has index 0 and no parameters.</p>
	 * <p>The parameter childIndex is the index of the first child.
	 * You can use whatever negative index you want to signal no
	 * children and other things you might need.</p>  
	 * @return the identifier of the newly created node.
	 * */
	public int addChild(int parent,int childIndex,Object[] params) {
		int newindex = getNextNodeIndex();
		int firstChild = nodes.get(parent);
		if (firstChild<0)
			nodes.set(parent, newindex);
		else
			nodes.set(getLastSibling(firstChild)+1, newindex);
		this.params.addAll(Arrays.asList(params));
		addNode();
		if (childIndex!=-1)
			nodes.set(newindex, childIndex);
		return newindex;
	}
	
	/**
	 * Sets the index to use as the first child.
	 * Negative indexes are considered as no children for the implementation,
	 * but you can use them to attach meaning to the node.
	 * */
	public void setFirstChild(int parent,int childIndex){
		nodes.set(parent, childIndex);
	}
	
	/** Returns the last sibling of the given node. */
	private int getLastSibling(int node){
		int sibling = nodes.get(node+1); 
		while(sibling>=0) {
			node = sibling;
			sibling = nodes.get(node+1);
		}
		return node;
	}

	/** Adds a node to the end of the node array. */
	private void addNode(){
		nodes.add(-1);
		nodes.add(-1);
		nodes.add(params.size());
	}
	
	/** Returns the amount of children of a given node. */
	public int getChildCount(int node){
		int sibling = nodes.get(node);
		int count=0;
		while(sibling>=0) {
			count++;
			sibling = nodes.get(sibling+1);
		}
		return count;
	}
	/** Returns a node child identifier. */
	public int getFirstChild(int node){
		return nodes.get(node);
	}
	/** Returns the next sibling of a node. */
	public int getNextSibling(int node){
		return nodes.get(node+1);
	}
	/** Returns the amount of parameters of a node . */
	public int getParamCount(int node){
		return nodes.get(node+2+3)-nodes.get(node+2);
	}
	/** Returns a node parameter. */
	public Object getParam(int node,int index){
		return params.get(nodes.get(node+2)+index);
	}
	/** Yields the index of the next added node will have. */
	public int getNextNodeIndex(){
		return nodes.size()-3;
	}		
	/** Erases disk storage. */
	public void dispose(){
		nda.dispose();
		pda.dispose();
	}
	
	/** Empties the tree leaving only the root. */
	public void clear(){
		nodes.clear();
		params.clear();
		addNode();
		addNode();
	}
	
	/** 
	 * Adds nodes to the disk tree.
	 * @return the index of the first inserted node. 
	 * */
	private static int addNodes(DiskIndexedTree c,int parent,Iterable<TreeLogger.Node> ns){
		int firstIndex = c.getNextNodeIndex();
		for(TreeLogger.Node n:ns){
			int newindex;
			if (n.children!=null && !n.children.isEmpty()) {
				if (n.children.getFirst()==null) {
					n.children=null;
					newindex = c.addChild(parent, -2, n.params);
				} else
					newindex = c.addChild(parent, n.params);
			} else {
				n.children = null;
				newindex = c.addChild(parent, n.params);
			}
				
			if (n.children!=null)
				addNodes(c,newindex,n.children);
 		}
		return firstIndex;
	}

	/** Test program for checking the IndexedTree representation. */
	public static void main(String[] args) throws IOException {
		
		TreeLogger.Node n0 = new TreeLogger.Node(new Object[]{"root"});
		n0.children = new LinkedList<TreeLogger.Node>();
		n0.children.add(new TreeLogger.Node(new Object[]{"child1"}));
		n0.children.add(new TreeLogger.Node(new Object[]{"child2"}));
		n0.children.add(new TreeLogger.Node(new Object[]{"child3"}));

		TreeLogger.Node n1 = new TreeLogger.Node(new Object[]{"root2"});
		n1.children = new LinkedList<TreeLogger.Node>();
		n1.children.add(new TreeLogger.Node(new Object[]{"child21"}));
		n1.children.add(new TreeLogger.Node(new Object[]{"child22"}));

		DiskIndexedTree c=new DiskIndexedTree();
		addNodes(c,0,Arrays.asList(n0,n1));
		
		System.out.println(c.getChildCount(0)==2);
		System.out.println(c.getChildCount(c.getFirstChild(0))==3);
		System.out.println(c.getChildCount(c.getNextSibling(c.getFirstChild(0)))==2);
		for(int i=1;i<8;i++)
			if (c.getParamCount(i*3)!=1)
				System.out.println(false);
		for(int i=2;i<8;i++)
			if (i!=5 && c.getChildCount(i*3)!=0)
				System.out.println(false);
		
		System.out.println(c.getParam(3,0).equals("root"));
		System.out.println(c.getParam(15,0).equals("root2"));

		System.out.println(c.getFirstChild(3)==6);
		System.out.println(c.getNextSibling(c.getFirstChild(3))==9);
		System.out.println(c.getNextSibling(c.getNextSibling(c.getFirstChild(3)))==12);
		System.out.println(c.getFirstChild(15)==18);
		System.out.println(c.getNextSibling(c.getFirstChild(15))==21);
		
		System.out.println(c.getParam(c.getFirstChild(3),0).equals("child1"));
		System.out.println(c.getParam(c.getNextSibling(c.getFirstChild(3)),0).equals("child2"));
		System.out.println(c.getParam(c.getNextSibling(c.getNextSibling(c.getFirstChild(3))),0).equals("child3"));
		System.out.println(c.getParam(c.getFirstChild(15),0).equals("child21"));
		System.out.println(c.getParam(c.getNextSibling(c.getFirstChild(15)),0).equals("child22"));
	}

}
