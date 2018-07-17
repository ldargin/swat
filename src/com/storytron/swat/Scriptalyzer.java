package com.storytron.swat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeNode;

import Engine.enginePackage.Interpreter;

import com.storytron.enginecommon.StackChunk;
import com.storytron.enginecommon.StackChunkGroup;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.tree.Tree;
import com.storytron.swat.tree.TreeCellRenderer;
import com.storytron.uber.Deikto;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;
import com.storytron.uber.operator.ParameterOperator;
import com.storytron.uber.operator.OperatorDictionary.Menu;

/**
 * GUI program to analyze the behavior of scripts.
 * <p>
 * It has histograms showing the most frequent values in a set of random
 * tests, and it also enables the user to see the script values in a single
 * point.
 * <p> 
 * There are two basic structures: the tree structure of the script
 * being displayed, and the linear top-down structure of the operator display.
 * These should be linked by a simple preorderEnumeration -- and in fact they
 * are so linked in Interpreter.executeScript. However, the situation is made
 * messy by the fact that the actual result data -- the StackChunks in the
 * StackChunkGroup -- are NOT in the same order as the enumeration. The mapping
 * from enumeration to StackChunk ArrayList is done through node identifiers,
 * assigned prior to execution of the script. 
 * <p>
 * The crucial data is in results.stackChunks, which is stored in the order
 * of computation (messy: first the children in reversed order, then the root). 
 * However, for the purposes of this display, we need to present everything in 
 * preorder sequence, which is how the nodes are presented from top to bottom.
 * <p>
 * Here's an example of my meaning. For the script:
 * <pre>
 * Inclination
 *   Blend
 *     P1Nasty_Nice
 *       ReactingActor
 *     P2Faithless_Honest
 *       ThisDirObject
 *       ThisSubject
 *     0.5
 * </pre>
 * The order of display is as above, but the order of calculation is:
 * <pre>
 *   ReactingActor
 *   P1NastyNice
 *   ThisDirObject
 *   ThisSubject
 *   P2Faithless_Honest
 *   0.5
 *   Blend
 *   Inclination
 * </pre>
 * This is solved by specifying a map to the interpreter when requesting StackChunks.
 * The map tells for each node which row it has. The generated StackChunks carry this
 * row number when returned.
 *   <p>
 *   The next complexities arise from the display differences. First, we never
 *   display the zeroth element (in this case, Inclination), because it is invisible
 *   to the storybuilder. In the middle panel we display histograms only of BNumber
 *   values (in this case, Blend, P1Nasty_Nice, P2Faithless_Honest, and 0.5). So
 *   there will be only four histograms to display.
 *   Next, there's the complexity that we put sliders in the right panel where there
 *   are BNumbers. All this is taken care of in {@link #instantiateSlidersAndHistograms(Script)}.
 *  <p> 
 *   However, there's another, even messier factor in all this: in order to make
 *   the sliders work, we must replace all the declarative operators with simple
 *   BNumber constants. Thus, the script above ends up looking like this:
 * <pre>   
 * Inclination
 *   Blend
 *     BNumberConstant
 *     BNumberConstant
 *     BNumberConstant
 * </pre>
 * Extra care should be taken to avoid screwing the original map from node to rows due 
 * to this modification.   
 */
public final class Scriptalyzer extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final int 	barWidth = 200;
	private Histogram[] histograms;
	private Tree dTree;
	private JScrollPane rightScroll;
	private JComponent sliderBox;
	private Script alteredScript;
	private ScriptPath sp;
	private HashMap<String,ArrayList<JComponent>> parameterComponents = new HashMap<String,ArrayList<JComponent>>();
	private ArrayList<JComponent> sliders = new ArrayList<JComponent>();
	private ResultPanel resultPanel;
	private Color peachColor = new Color(253, 218, 164);
	private boolean userInput=true;
	private Deikto dk;
	private Interpreter interpreter;
	private boolean poisoned = false;
	
//**********************************************************************
	// touch
	public Scriptalyzer(ScriptPath sp,Script s, Deikto tdk) {
		super("Scriptalyzer for: "+sp.getPath(s));
		// clone the script because it will be fully expanded
		// and we don't want to loose the expanded state of the script in verb editor.
		Script testScript = s.clone();
		dk = tdk.cloneWorldShareLanguage();
		this.sp = sp;
		
		dTree = new TreeWithLines(testScript);
		
		sliderBox = new JPanel();
		sliderBox.setLayout(new BoxLayout(sliderBox,BoxLayout.Y_AXIS));
		sliderBox.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(Color.black),
						BorderFactory.createEmptyBorder(8,5,0,5)
				)
			);
		sliderBox.setBackground(peachColor);
		sliderBox.setMinimumSize(new Dimension(100,10));
		sliderBox.setMaximumSize(new Dimension(300,Integer.MAX_VALUE));
		sliderBox.setPreferredSize(new Dimension(200,10));

		resultPanel = new ResultPanel(50,30);
		resultPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0,0,0,1),
				BorderFactory.createLineBorder(Color.black))
			);

		dTree.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0,1,0,1),
				BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(Color.black),
						BorderFactory.createEmptyBorder(10,0,0,0)
				))
			);

		JPanel rightPanel=new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel,BoxLayout.X_AXIS));
		rightPanel.add(resultPanel);		
		rightPanel.add(sliderBox);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.X_AXIS));
		mainPanel.setOpaque(false);
		mainPanel.add(BorderLayout.CENTER, dTree);
		mainPanel.add(BorderLayout.EAST, rightPanel);

		rightScroll=new JScrollPane(mainPanel,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		rightScroll.getVerticalScrollBar().setUnitIncrement(20);

		add(rightScroll);

		setSize(900, 600);
		setLocation(200,200);
		setSize(768,600);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// this next line carries out the 1000 runs of the Script
		ArrayList<StackChunkGroup> results = scriptTest(sp,testScript);

		histograms = new Histogram[countNodes(testScript.getRoot())];

		if (!poisoned) {
			instantiateSlidersAndHistograms(testScript);

			fillHistograms(results);
		
			initializeSliders(testScript);
			
			sliderBox.add(Box.createVerticalGlue());
		}

		interpreter = new Interpreter(new DummyEngine(dk.getActorCount()),dk);
		interpreter.node2id = createAlteredScript(testScript);
			
		recalculate();
		validate();
		setVisible(true);
	}

	/** 
	 * Modifies the original script to manipulate values of internal nodes.
	 * @return the map of nodes to identifiers for the altered script. 
	 * */ 
	@SuppressWarnings("unchecked")
	private Map<Node, Short> createAlteredScript(Script testScript) {
		alteredScript = (Script) testScript.clone();
		Map<Node, Short> node2id = new HashMap<Node, Short>();

		ArrayList<Node> removeChildren = new ArrayList<Node>(getNodeCount(alteredScript.getRoot()));
		Enumeration alteredScriptEnumeration = alteredScript.getRoot().preorderEnumeration();
		alteredScriptEnumeration.nextElement(); // Skip root
		for(int i=0;i<sliders.size() && alteredScriptEnumeration.hasMoreElements();i++) {
			Node zDMTN = (Node)alteredScriptEnumeration.nextElement();
			node2id.put(zDMTN,displayed2NodeId(i));
			if (sliders.get(i)!=null) {
				if (zDMTN.getOperator().getDataType()==Operator.Type.Boolean)
					zDMTN.setOperatorValue(OperatorDictionary.getTrueOperator(), 1.0f);
				else if (zDMTN.getOperator().getDataType()==Operator.Type.BNumber)
					zDMTN.setOperatorValue(OperatorDictionary.getBNumberConstantOperator(), 0.0f);
				else if (zDMTN.getOperator().getDataType()==Operator.Type.Number)
					zDMTN.setOperatorValue(OperatorDictionary.getNumberConstantOperator(), 0.0f);
				removeChildren.add(zDMTN);
			}
		}
		for(Node n:removeChildren)
			n.removeAllChildren();
		return node2id;
	}

	private static int getNodeCount(TreeNode n){
		int count=1;
		for(int i=0;i<n.getChildCount();i++)
			count+=getNodeCount(n.getChildAt(i));
		return count;
	}
	
	/** Maps a node id to the row where it is shown. */
	private int nodeId2displayed(int id) {	return id; };

	/** Maps a row to the node id of the node being shown. */
	private short displayed2NodeId(int row) { return (short)row; };

	/** Returns the amount of nodes in the script. */
	private int countNodes(TreeNode n) {
		int sum=0;
		for(int i=0;i<n.getChildCount();i++)
			sum+=countNodes(n.getChildAt(i));
		return sum+1;
	};
	
	/** 
	 * Creates the mapping which tells which node has which id.
	 * Ids are assigned in preorder. 
	 * */
	public Map<Node, Short> createNodeMap(Node n) {
		Map<Node, Short> node2id = new HashMap<Node, Short>();
		initializeNodeMap(node2id, n,(short)-1);
		return node2id;
	}
	/** Initializes the mapping which tells which node has which id. */
	private short initializeNodeMap(Map<Node, Short> m,Node n,short count) {
		if (count<0)
			count++; // skip root
		else
			m.put(n,count++);
		for(int i=0;i<n.getChildCount();i++)
			count=initializeNodeMap(m,(Node)n.getChildAt(i),count);
		return count;
	};

	/** 
	 * Fills histograms with the given results. 
	 * Called after instantiating the histograms and the sliders with {@link #instantiateSlidersAndHistograms(Script)},
	 * so we know which data we are interested in. 
	 * */
	private void fillHistograms(ArrayList<StackChunkGroup> results){
		// Create structure to collect histogram data
		float[][] histogramData = new float[histograms.length][];
		for (int i=0; i<histograms.length; ++i) {
			if (histograms[i]!=null)
				histogramData[i] = new float[results.size()];
		}

		// Collect histogram data
		for(int i=0; i<results.size(); ++i)
			for(int j=0;j<results.get(i).stackChunks.size();j++) {
				StackChunk zStackChunk = results.get(i).stackChunks.get(j);
				int k = nodeId2displayed(zStackChunk.getId());
				if (histogramData[k]!=null)
					histogramData[k][i] = zStackChunk.getValue();
			}
		
		// Set histogram data
		for(int i=0;i<histograms.length;i++)
			if (histograms[i]!=null)
				histograms[i].setData(histogramData[i]);
	}

	/** 
	 * Creates sliders and histograms for the given script.
	 * Actually, decides which nodes have histograms, sliders and checkboxes. 
	 * */
	@SuppressWarnings("unchecked")
	private void instantiateSlidersAndHistograms(Script testScript){
		Dimension rigidAreaSize=new Dimension(20,25);
		// Here we set up the order of display of the sliders.
		int row = 0;
		Enumeration enumeration=testScript.getRoot().preorderEnumeration();
		enumeration.nextElement(); // discard the root.
		while (enumeration.hasMoreElements()) {
			Node zDMTN = (Node)enumeration.nextElement();
			Operator zOperator = zDMTN.getOperator();
			
			// if it's not a BNumber and is not a Number HistoryBook operator, 
			// we'll skip displaying anything and just put in an empty strut
			if (zOperator.getDataType()!=Operator.Type.Boolean &&
					zOperator.getDataType()!=Operator.Type.BNumber &&
					zOperator.getDataType()!=Operator.Type.Number
					|| isUnderIterationOrHistoryOperatorExceptAcceptableOrDesirable(zDMTN)) {
				sliderBox.add(Box.createRigidArea(rigidAreaSize));
				sliders.add(null); // keep the spacing right
			}
			else if (containsManipulableChildren(zDMTN) && !zOperator.isIteration()
					 && zOperator.getMenu()!=Menu.History) {
				// If it contains manipulable children and it is not an iteration
				// and is not a history lookup,
				// the operator does not have sliders, but it does have histograms
				sliderBox.add(Box.createRigidArea(rigidAreaSize));
				sliders.add(null);
				histograms[row] = createHistogram(zOperator.getDataType());
			}
			else {
				// declarative operators get sliders AND histograms
				histograms[row] = createHistogram(zOperator.getDataType());

				JComponent xSlider;
				JComponent sliderHolder = Box.createHorizontalBox();										
				if (zOperator.getDataType()==Operator.Type.Boolean){
					xSlider = new JCheckBox();
					((JCheckBox)xSlider).setSelected(zOperator==OperatorDictionary.getTrueOperator());
					if (zOperator instanceof ParameterOperator) {
						final String opLabel = zOperator.getLabel();
						((JCheckBox)xSlider).addChangeListener(new ChangeListener() {
							public void stateChanged(ChangeEvent e) {
								if (!userInput) return;
								userInput=false;
								for (JComponent c:parameterComponents.get(opLabel))
									((JCheckBox)c).setSelected(((JCheckBox)e.getSource()).isSelected());
								userInput=true;
								recalculate();
							};
						});
						addToParameterComponents(opLabel,xSlider);
					} else {
						((JCheckBox)xSlider).addChangeListener(new ChangeListener() {
							public void stateChanged(ChangeEvent e) {
								if (!userInput) return;
								recalculate();
							};
						});
					}
					
					xSlider.setMaximumSize(new Dimension(25, 25));
					xSlider.setPreferredSize(xSlider.getMaximumSize());
					sliderHolder.add(Box.createHorizontalGlue());
					sliderHolder.add(xSlider);
					sliderHolder.add(Box.createHorizontalGlue());
				} else {

					xSlider= new JSlider(0, 198);
					xSlider.setMaximumSize(new Dimension(120, 25));
					xSlider.setPreferredSize(xSlider.getMaximumSize());
					if (zOperator instanceof ParameterOperator) {
						final String opLabel = zOperator.getLabel();
						((JSlider)xSlider).addChangeListener(new ChangeListener() {
							public void stateChanged(ChangeEvent e) {
								if (!userInput) 
									return;
								userInput=false;
								for (JComponent c:parameterComponents.get(opLabel))
									((JSlider)c).setValue(((JSlider)e.getSource()).getValue());
								userInput=true;
								recalculate();
							};
						});
						addToParameterComponents(opLabel,xSlider);
					} else {
						((JSlider)xSlider).addChangeListener(new ChangeListener() {
							public void stateChanged(ChangeEvent e) {
								if (!userInput) 
									return;
								recalculate();
							};
						});
					}
					sliderHolder.setMaximumSize(new Dimension(300, 25));					
					xSlider.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
					xSlider.setAlignmentY(0.5f);
					sliderHolder.add(xSlider);
				}
				xSlider.setOpaque(false);
				sliders.add(xSlider);
				
				sliderHolder.setAlignmentX(0.5f);
				sliderBox.add(sliderHolder);
			}
			++row;
		}
	}
	
	private void addToParameterComponents(String opLabel,JComponent c){
		ArrayList<JComponent> cmps = parameterComponents.get(opLabel);
		if (cmps==null) {
			cmps = new ArrayList<JComponent>();
			parameterComponents.put(opLabel,cmps);
		}
		cmps.add(c);
	};
	
	/** 
	 * Initializes labels and sliders with test data.
	 * Slider and histograms must be already instantiated with {@link #instantiateSlidersAndHistograms(Script)}.
	 * Call after filling the histograms with {@link #fillHistograms(ArrayList)}, 
	 * so the histogram data can be used.
	 * */
	@SuppressWarnings("unchecked")
	private void initializeSliders(Script testScript) {
		Enumeration enumeration=testScript.getRoot().preorderEnumeration();
		enumeration.nextElement(); // discard the root.
		for(int i=0;i<sliders.size();i++) {
			JComponent c = sliders.get(i);
			Node zDMTN = (Node)enumeration.nextElement();
			if (c!=null && c instanceof JSlider) {
				// Surround slider with appropriate labels
				NumericHistogram nh = (NumericHistogram)histograms[i];
				JLabel sliderLowerLabel = new JLabel(nh.prefixLabel);
				JLabel sliderUpperLabel = new JLabel(nh.postfixLabel);
				sliderLowerLabel.setAlignmentY(0.5f);
				c.getParent().add(sliderLowerLabel,0);
				sliderUpperLabel.setAlignmentY(0.5f);
				c.getParent().add(sliderUpperLabel);
				
				// find initial value for the slider
				int initialValue=99;
				if (zDMTN.getOperator()==OperatorDictionary.getBNumberConstantOperator())
					initialValue=Math.max(0,Math.min(198,(int)(zDMTN.getNumericValue(dk)*100+99)));
				else if (zDMTN.getOperator().getLabel().equals("Maxi"))
					initialValue=Math.max(0,Math.min(198,(int)(Utils.MAXI_VALUE*100+99)));
				else if (zDMTN.getOperator().getLabel().equals("Mini"))
					initialValue=Math.max(0,Math.min(198,(int)(Utils.MINI_VALUE*100+99)));
				else if (zDMTN.getOperator()==OperatorDictionary.getNumberConstantOperator())
					initialValue=Math.max(0,Math.min(198,(int)(((zDMTN.getNumericValue(dk)-nh.lowerBound)/(nh.upperBound-nh.lowerBound)*198))));
				
				userInput = false;
				((JSlider)c).setValue(initialValue);
				userInput = true;
			}
		}
	}
	
	/** 
	 * Tells if an node is under an iteration operator, but
	 * ignores Acceptable and Desirable operators.
	 * */ 
	private boolean isUnderIterationOrHistoryOperatorExceptAcceptableOrDesirable(Node n) {
		while (n.getParent()!=null) {
			n=(Node)n.getParent();
			if (n.getOperator().getMenu()==Menu.History 
				|| n.getOperator().isIteration() 
					&& !n.getOperator().getLabel().equals("Acceptable")
					&& !n.getOperator().getLabel().equals("Desirable"))
				return true;
		}
		return false;
	}
	
	private Histogram createHistogram(Operator.Type t){
		switch (t){
		case Boolean:
			return new BooleanHistogram();
		case BNumber:
			return new BNumberHistogram();
		case Number:
			return new NumberHistogram();
		default:
			return null;
		}
	}
	
	/** Tells if any of the children of the node will have slider or checkboxes. */
	private static boolean containsManipulableChildren(Node n){
		for(int i=0;i<n.getChildCount();i++) {
			switch(((Node)n.getChildAt(i)).getOperator().getDataType()) {
			case BNumber:
			case Number:
			case Boolean:
				return true;
			default:
				if (!((Node)n.getChildAt(i)).getOperator().isIteration()
						&& containsManipulableChildren((Node)n.getChildAt(i)))
					return true;
			}
		}
		return false;
	}
	
	private final class TreeWithLines extends Tree {
		private static final long serialVersionUID = 1L;
		TreeWithLines(Script s){
			setRoot(s.getRoot());
			setRootVisible(false);
			setReactToUserInput(false);
			unfold();
			setRowHeight(25);
		}

		@Override
		public TreeCellRenderer createTreeCellRenderer() {
			ScriptTreeCellRenderer cellRenderer = new ScriptTreeCellRenderer(this);
			cellRenderer.setShowDescriptions(false);
			return cellRenderer;
		}
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			int baseX=0;
			int baseY=30;
			
			g.setColor(Color.black);
			for (int i=0; i<histograms.length; ++i) {
				if (histograms[i]!=null) {
					int stepY = baseY+i*25;
					g.drawLine(baseX,stepY,getWidth(),stepY);
				}
			}
		}
		
	}
	
//**********************************************************************
	public class ResultPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		int baseX, baseY;
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		public ResultPanel(int x, int y) {
			super(null);
			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			add(Box.createRigidArea(new Dimension(300,20)));
			add(Box.createVerticalGlue());
			baseX = x;
			baseY = y;
			setBackground(Color.white);		
		}
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D)g.create();
			g2.setBackground(Color.WHITE);
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, getWidth(), getHeight());

			if (poisoned) {
				g2.setFont(new Font(g2.getFont().getName(), Font.PLAIN, 36));
				g2.drawString("POISONED!!!", baseX, baseY+histograms.length*25+40);
				sliders.clear();
			}
			else { // the normal display
				g2.setFont(new Font(g2.getFont().getName(), Font.PLAIN, 10));
				g2.translate(baseX, baseY);
				for (int i=0; i<histograms.length; ++i) {
					if (histograms[i]!=null) {
						histograms[i].draw(g2);
						g2.drawLine(-baseX,0,-baseX+10,0);
					}
					g2.translate(0, 25);
				}
			}
		}
	}
//**********************************************************************
	private void recalculate() {
		HashMap<Short, Float> constantNodeMap = new HashMap<Short, Float>();
	
		for(int i=0;i<sliders.size();i++) {
			JComponent c = sliders.get(i);
			if (c==null)
				continue;
			
			if (c instanceof JSlider) {
				if (histograms[i]==null)
					continue;
				
				NumericHistogram nh = (NumericHistogram)histograms[i]; 
				float newConstant = (nh.lowerBound+((JSlider)c).getValue()/198.0f*(nh.upperBound-nh.lowerBound));
				if (nh.lowerBound!=-0.99f || nh.upperBound!=0.99f) {
					userInput=false;
					((JSlider)c).setValue((int)((newConstant-nh.lowerBound)/(nh.upperBound-nh.lowerBound)*198));
					userInput=true;
				}
				constantNodeMap.put(displayed2NodeId(i), newConstant);
					
			} else if (c instanceof JCheckBox) {
				constantNodeMap.put(displayed2NodeId(i), ((JCheckBox)c).isSelected()?1.0f:0.0f);
			}
		}
		
		StackChunkGroup newResult = singleScriptTest(sp,constantNodeMap);

		for (int i=0;i<newResult.stackChunks.size();i++) {
			StackChunk zChunk = newResult.stackChunks.get(i);
			int k=(int)nodeId2displayed(zChunk.getId());
			if (histograms[k]!=null)
				histograms[k].setMarker(zChunk.getValue());
		}

		resultPanel.repaint();
	}
	
	/** 
	 * Runs many tests of a script with random values.
	 * @return the collected script node values.
	 * */
	private ArrayList<StackChunkGroup> scriptTest(ScriptPath sp,Script testScript) {
		ArrayList<StackChunkGroup> results = new ArrayList<StackChunkGroup>();
		
		Interpreter interpreter = new Interpreter(new DummyEngine(dk.getActorCount()),dk);
		interpreter.node2id = createNodeMap(testScript.getRoot());
		
		for (int cTests=0; (cTests<1000); ++cTests) {
			interpreter.setScriptalyzer(true);
			interpreter.setAccumulateChunks(true);
			interpreter.executeScript(sp,testScript);
			results.add(interpreter.getStackChunkGroup());
		}
		interpreter.setScriptalyzer(false);
		interpreter.setAccumulateChunks(false);
		poisoned = interpreter.getPoison();
		return results;
	}

	/** 
	 * Runs a test of a script with values specified for some nodes in
	 * a given map.
	 * */
	@SuppressWarnings("unchecked")
	private StackChunkGroup singleScriptTest(ScriptPath sp,HashMap<Short, Float> constantNodeMap) {
		
		Enumeration alteredScriptEnumeration = alteredScript.getRoot().preorderEnumeration();
		while (alteredScriptEnumeration.hasMoreElements()) {
			Node zDMTN = (Node)alteredScriptEnumeration.nextElement();
			if (constantNodeMap.containsKey(interpreter.node2id.get(zDMTN)))
				if (zDMTN.getOperator().getDataType()==Operator.Type.Boolean)
					if (constantNodeMap.get(interpreter.node2id.get(zDMTN))==1.0f)
						zDMTN.setOperatorValue(OperatorDictionary.getTrueOperator(), 1.0f);
					else
						zDMTN.setOperatorValue(OperatorDictionary.getFalseOperator(), 0.0f);
				else
					zDMTN.setConstant(constantNodeMap.get(interpreter.node2id.get(zDMTN)));
		} 

		StackChunkGroup result;
		interpreter.setScriptalyzer(true);
		interpreter.setAccumulateChunks(true);
		interpreter.executeScript(sp,alteredScript);
		result = interpreter.getStackChunkGroup();
		interpreter.setScriptalyzer(false);
		interpreter.setAccumulateChunks(false);
		poisoned = interpreter.getPoison();
		return result;
	}

	/** 
	 * Class to process and display histogram data.
	 * <p>
	 * An histogram is a set of frequencies of occurrence of values
	 * in certain interval. The interval may be BNumbers, Numbers or
	 * Booleans.
	 * <p>
	 * The histogram also holds a red marker that is displayed on top of
	 * the rest of the data.
	 * <p>
	 * To use a histogram, the data of various tests has to be passed
	 * to it {@link #setData(float[])}. Then it can be drawn with {@link #draw(Graphics2D)},
	 * and the red marker can be set with {@link #setMarker(float)}.  
	 * */
	public static abstract class Histogram {
		protected float[] histogram = new float[100];
		protected int redMarkX;
		protected float markValue;
		protected String prefixLabel;
		protected String postfixLabel;

		/** Specifies histogram values. */
		abstract void setData(float[] values);
		/** Sets the histogram red marker. */
		abstract void setMarker(float value);

		/** Draws the histogram. */
		abstract void draw(Graphics2D g2);
	} 
	
	/** Histogram for boolean data. */
	public static final class BooleanHistogram extends Histogram {
		void setData(float[] values){
			for (float x:values)
				histogram[x<0?0:x>99?99:(int)x]++;
		}
		
		/** Sets the histogram red marker. */
		void setMarker(float value){
			markValue = value;
			redMarkX = value<0.5?0:99;  
		}
		
		/** Draws the histogram. */
		void draw(Graphics2D g2){
			g2.setColor(Color.black);

			int a = (int)histogram[0];
			int b = (int)histogram[1];
			int x = (barWidth * a)/(a+b);
			g2.drawRect(0,-15,barWidth,15);
			g2.fillRect(0,-15,x,15);

			g2.setColor(Color.red);
			if (redMarkX==0)
				g2.fillRect(0,-15,2,15);
			else 
				g2.fillRect(barWidth-1,-15,2,15);
			g2.setColor(Color.black);

			g2.drawLine(0,0,barWidth,0);
		}
	}

	/** Histogram for Number and BNumber data. */
	public static abstract class NumericHistogram extends Histogram {
		public float lowerBound;
		public float upperBound;
		
		/** Draws the histogram. */
		void draw(Graphics2D g2){
			g2.setFont(new Font(g2.getFont().getName(), Font.PLAIN, 10));

			for (int i=0; i<histogram.length; ++i) {
				float brightness = 1.0f-histogram[i];
				if (brightness > 1.0f)
					brightness = 1.0f;
				if (brightness < 0.0f)
					brightness = 0.0f;
				g2.setColor(Color.getHSBColor(0.0f, 0.0f, brightness));
				if (i==redMarkX)
					g2.setColor(Color.red);
				g2.fillRect(2*i,-15,2,15);
				g2.setColor(Color.black);
				if (i % 10 == 0) {
					g2.drawLine(2*i,0,2*i,3);
					if (i == 50)
						g2.drawLine(2*i,0,2*i,6);
				}
			}
			if (redMarkX!=-1){
				g2.setColor(Color.red);
				g2.drawString(String.format("%1.2g",markValue), 2*redMarkX+4, -15);
				g2.setColor(Color.black);
			}
			g2.drawLine(0,0,barWidth,0);
			g2.drawLine(barWidth,0,barWidth,3);
			g2.drawString(postfixLabel, barWidth+5, -3);
			g2.drawString(prefixLabel, -g2.getFontMetrics().stringWidth(prefixLabel)-5, -3);
		}
	}
	
	/** Histogram for BNumber data. */
	private static final class BNumberHistogram extends NumericHistogram {
		public BNumberHistogram(){
			lowerBound=Utils.MINI_VALUE;
			upperBound=Utils.MAXI_VALUE;
		}
		void setData(float[] values){
			for (float x:values) {
				float v = x*50.0f +50.0f;
				histogram[v<0?0:v>99?99:(int)v]++;
			}
			prefixLabel = "-0.99";
			postfixLabel = "+0.99";
			int hsum=0;
			for (int i=0; i<histogram.length; ++i)
				hsum+=histogram[i];
			
			for (int i=0; i<histogram.length; ++i)
				histogram[i]/=hsum/20;
		}
		/** Sets the histogram red marker. */
		void setMarker(float value){
			markValue = value;
			redMarkX = (int)((value+1f)*50f);  
			redMarkX=redMarkX<0?0:redMarkX>99?99:redMarkX;						
		}
	}
	
	/** Histogram for Number data. */
	private static final class NumberHistogram extends NumericHistogram {

		public NumberHistogram(){}
		
		void setData(float[] values){

			// Filter values far from the mean
			float sumX = 0.0f;
			float sumX2 = 0.0f;
			for (float x:values) {
				sumX += x;
				sumX2 += x*x;
			}
			float mean=sumX / values.length;
			double stdD=2.0 * Math.sqrt((sumX2 - values.length*mean*mean)/(values.length-1));

			// Calculate range bounds
			float min=Float.MAX_VALUE;
			float max=Float.MIN_VALUE;
			for (float x:values) {
				if (x<mean-stdD || x>mean+stdD)
					continue;

				if (min>x) min=x;
				if (max<x) max=x;
			}
			if (min==max){
				min-=5*min;
				max+=5*max;
			}
			
			// Build histogram
			for (float x:values) {
				if (x<min || x>max)
					continue;
				
				histogram[(int)(99*(x-min)/(max-min))]++;
			}
			
			for (int i=0; i<histogram.length; ++i)
				histogram[i]/=200;

			prefixLabel = String.format("%1.2g",min);
			postfixLabel = String.format("%1.2g",max);
			
			lowerBound=min;
			upperBound=max;
		}
		
		/** Sets the histogram red marker. */
		void setMarker(float value){
			markValue = value;
			redMarkX=(int)(99*(value-lowerBound)/(upperBound-lowerBound));
			redMarkX=redMarkX<0?0:redMarkX>99?99:redMarkX;						
		}

	}
	
	/** Class for testing Scriptalyzer. */
	public static class Test {
	
		/** @return the sliders (and checkboxes) of Scriptalyzer. */
		public static ArrayList<JComponent> getSliders(Scriptalyzer s) {
			return s.sliders;
		}
		
		/** @return the histograms of Scriptalyzer. */
		public static Histogram[] getHistograms(Scriptalyzer s) {
			return s.histograms;
		}
	}
}
