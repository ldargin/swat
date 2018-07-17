package com.storytron.swat;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.tree.TNode;
import com.storytron.swat.tree.Tree;
import com.storytron.swat.tree.TreeCellRenderer;
import com.storytron.uber.Script;
import com.storytron.uber.operator.Operator;

/** 
 * A class for drawing script tree nodes.
 * <p>
 * Displays operator labels and node descriptions.  
 * */
public final class ScriptTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer 
									implements TreeCellRenderer {

	private static final long serialVersionUID = 1L;
	private JLabel tokenLabel;		
	private JLabel descriptionLabel = new JLabel();
	private FontMetrics fm;
	private JComponent renderer;
	private Tree tree;
	private Insets insets=new Insets(3,3,3,3);
	private Border focusBorder;
	private Font plainFont;
	private Font boldFont;

	public ScriptTreeCellRenderer(Tree tree) {
		// Here we initialize the label for showing operators
		// the label for showing the node labels (if any)
		// and the field for editing the node labels.
		this.tree=tree;
		renderer = Box.createVerticalBox();
		tokenLabel = new JLabel();
		tokenLabel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		descriptionLabel.setBorder(BorderFactory.createEmptyBorder(2,3,4,3));
		descriptionLabel.setVerticalAlignment(JLabel.TOP);
		descriptionLabel.setBackground(backgroundSelectionColor);
		descriptionLabel.setOpaque(true);
		fm=tokenLabel.getFontMetrics(tokenLabel.getFont());
		tokenLabel.setBackground(backgroundSelectionColor);
		focusBorder=BorderFactory.createLineBorder(borderSelectionColor);
		boldFont = tokenLabel.getFont();
		plainFont = new Font(boldFont.getName(),Font.PLAIN,boldFont.getSize());
	}


	/**
	 * We want to hide node labels in visiscript.  
	 * */
	private boolean showDescriptions=true;
	public void setShowDescriptions(boolean showDescriptions) {
		this.showDescriptions = showDescriptions;
	}
	public boolean getShowDescriptions() {
		return showDescriptions;
	}

 	/**
 	 * This method tells if the node would show both the token label and its
 	 * description label if it had a description. 
 	 * */
 	private boolean fullDisplaying(TNode n){
 		return n.isExpanded() || n.isLeaf();
 	}
 	
	/**
	 * The height of the node is the one of a JLabel
	 * if the node does not have a description label or 
	 * has but it is collapsed.
	 * Otherwise it is twice as high.
	 * */
	public int getHeight(TNode n){
		if (tree.getRowHeight()>0)
			return tree.getRowHeight();

		if (fullDisplaying(n) && getShowDescriptions() && 
				((Script.Node)n).getDescription().length()!=0)
			return 2*(getFontHeight()+insets.top+insets.bottom);
		else return getFontHeight()+insets.top+insets.bottom;
	}
	
	/**
	 * The width of the node is the one of the label being displayed.
	 * If there is more than one label to show, the width is the maximum
	 * of the label widths. 
	 * */
	public int getWidth(TNode n){
		Script.Node t = (Script.Node)n;
		if (!getShowDescriptions())
			return 30+fm.stringWidth(getTokenLabel(n));

		if (fullDisplaying(n) && t.getDescription().length()!=0)
			return 30+Math.max(fm.stringWidth(getTokenLabel(n)), 
					Math.min(fm.stringWidth(t.getDescription()),250));
		else if (fullDisplaying(n) || t.getDescription().length()==0)
			return 30+fm.stringWidth(getTokenLabel(n));
		else return 30+Math.min(fm.stringWidth(t.getDescription()),250);
	}

	public int getTokenLabelWidth(TNode n){
		return fm.stringWidth(getTokenLabel(n));
	}

	/**
	 * Utility method to configure height of the labels.
	 * */
	private int getFontHeight(){
		return fm.getAscent();
	}

	/**
	 * Construct the label for a given node.
	 * */
	public static String getTokenLabel(TNode n){
		Script.Node t=(Script.Node)n;
		Operator zOperator=t.getOperator();
		String zString=t.toString();
		if (zOperator.getCArguments() > 0) {
			if (zOperator.getDataType() != Operator.Type.Boolean) {
				if (zOperator.getLabel().startsWith("AdjustP2", 0))
					return zString.concat(" for ThisSubject by:");
				else	
					return zString.concat(" of:");
			}
		} else if (zOperator.getOperatorType()==Operator.OpType.Undefined) {
			Script.Node zMyDMTN = (Script.Node)n.getParent();
			if (zMyDMTN != null) {
				int ziArgument = zMyDMTN.getIndex(n);
				Operator ziParent = zMyDMTN.getOperator();
				return ziParent.getArgumentLabel(ziArgument)+"?";
			}
		} if (isEmptyText(t))
			return "<empty text>";
		return zString;
	}

	private static boolean isEmptyText(Script.Node n){
		return n.getOperator()!=null && n.getOperator().getDataType()==Operator.Type.Text && n.toString().length()==0;
	};
	
	/**
	 * Constructs the Component to draw for a given node.
	 * This comes from the interface {@link TreeCellRenderer}.
	 * */
	public Component getTreeCellRendererComponent(com.storytron.swat.tree.Tree tree, TNode value, boolean hasFocus) {
		Component returnValue = null;
		int w=getWidth(value);
		int h=getHeight(value);

		renderer.removeAll();
		Script.Node token = (Script.Node) value;
		JComponent lastComponent = null;
		if (token.getDescription().length()!=0 && getShowDescriptions()){
			// add the description label
			descriptionLabel.setText(token.getDescription());
			descriptionLabel.setSize(w,fullDisplaying(value)?h/2:h);
			descriptionLabel.setPreferredSize(descriptionLabel.getSize());
			descriptionLabel.setBackground(Utils.lightGrayBackground);
			descriptionLabel.setBorder(null);
			renderer.add(descriptionLabel); 	  
			lastComponent = descriptionLabel;
		}
		if (fullDisplaying(value) || token.getDescription().length()==0) {
			Operator zOperator = token.getOperator();
			tokenLabel.setText(getTokenLabel(value));
			if (isEmptyText(token))
				tokenLabel.setFont(plainFont);
			else
				tokenLabel.setFont(boldFont);
			tokenLabel.setForeground(zOperator.getColor());

			tokenLabel.setSize(w,token.getDescription().length()==0 || !getShowDescriptions()?h:h/2);
			tokenLabel.setPreferredSize(tokenLabel.getSize());
			tokenLabel.setOpaque(value==tree.getSelectedNode());
			tokenLabel.setBorder(null);

			renderer.add(tokenLabel);
			lastComponent = tokenLabel;
		} 	    		
		if (lastComponent!=null && tree.hasFocus() && value==tree.getSelectedNode()){
			lastComponent.setBorder(focusBorder);
			if (lastComponent==descriptionLabel)
				descriptionLabel.setBackground(backgroundSelectionColor);
		}

		returnValue = renderer;
		returnValue.setSize(getWidth(value),getHeight(value));
		return returnValue;
	}

	public Component getComponent(){ return renderer; }
}
