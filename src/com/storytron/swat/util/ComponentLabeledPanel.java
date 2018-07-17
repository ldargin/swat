package com.storytron.swat.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.storytron.swat.Swat;

/**
 * Looks almost like a panel with titled border, but can hold any component
 * in the title area. It is used for consequences, emotional reactions
 * and the verb panel in the middle.  
 * */
public class ComponentLabeledPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private Color color = Swat.shadow;
	private JComponent label;
	private JComponent contents=new JPanel(new BorderLayout());
	private Insets insets;
	
	public ComponentLabeledPanel(String buttonText){
		this(new JButton(buttonText),Swat.shadow,true);			
	}
	public ComponentLabeledPanel(JComponent labelComponent){
		this(labelComponent,Swat.shadow,true);			
	}
	public ComponentLabeledPanel(JComponent label,Color color,boolean packHeader){
		super(new BorderLayout());			
		this.label=label;
		this.color=color;
		JComponent top=Box.createHorizontalBox();
		top.add(Box.createRigidArea(new Dimension(10,10)));
		top.add(label);
		top.add(Box.createRigidArea(new Dimension(5,5)));
		if (packHeader)
			top.add(Box.createHorizontalGlue());
		super.add(top,BorderLayout.NORTH);
		contents.setOpaque(false);
		contents.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0,1,1,1,color),
				BorderFactory.createEmptyBorder(2, 1, 1, 1)
				));
		super.add(contents,BorderLayout.CENTER);
		
	}
	
	public ComponentLabeledPanel(){
		this(" ");
	}

	@Override
	public Component add(Component comp) {
		contents.removeAll();
		return contents.add(comp); 
	}

	@Override
	public Component add(Component comp,int pos){			
		return contents.add(comp,pos); 
	}
	
	@Override
	public void remove(Component comp){			
		contents.remove(comp); 
	}

	@Override
	protected void paintBorder(Graphics g){
		insets = getInsets(insets);
		g.setColor(color);
		g.drawLine(contents.getX(),1+label.getHeight()/2+insets.top,
					label.getX()-2,1+label.getHeight()/2+insets.top);
		g.drawLine(label.getX()+label.getWidth()+4,1+label.getHeight()/2+insets.top,
					contents.getX()+contents.getWidth()-1,1+label.getHeight()/2+insets.top);
		
		g.drawLine(contents.getX(),1+label.getHeight()/2+insets.top, 
					contents.getX(),contents.getY()-1);
		g.drawLine(contents.getX()+contents.getWidth()-1,1+label.getHeight()/2+insets.top, 
					contents.getX()+contents.getWidth()-1, contents.getY()-1);
		
		super.paintBorder(g);
	}
}
