package com.storytron.swat.verbeditor;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Shape;

import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

import com.storytron.swat.Swat;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;

/**
 * Buttons with a little decoration in its top right corner.
 * It is used for representing script display buttons.
 * */
public class ScriptDisplayButton extends JToggleButton {
	private static final long serialVersionUID = 1L;
	private ScriptPath mScriptPath=null;
	private Script mScript=null;
	private ButtonGroup scriptButtons;
	
	public ScriptDisplayButton(ScriptPath sp,Script s,ButtonGroup scriptButtons){
		this(s.getLabel(),scriptButtons);
		setScriptPath(sp,s);
		reformat();
	}
	public ScriptDisplayButton(String label,ButtonGroup scriptButtons){
		super(label);
		this.scriptButtons=scriptButtons;
		setOpaque(false);
		setAlignmentX(Component.CENTER_ALIGNMENT);
		setMargin(new Insets(2,3,0,5));
	}
	public ScriptPath getScriptPath(){ return mScriptPath; }
	public Script getScript(){ return mScript; }
	public void setScriptPath(ScriptPath sp,Script s){ 
		mScriptPath=sp;
		mScript=s;
	}

	/** Takes button foreground color and label from the script. */
	public void reformat(){
		if (mScript!=null){
			setForeground(mScript.getBaseColor());
			setText(mScript.getLabel());
		}
	}
	@Override
	public void removeNotify(){	
		scriptButtons.remove(this);
		super.removeNotify();
	}
	@Override
	public void addNotify(){ 
		scriptButtons.add(this);
		super.addNotify();
	}
	
	@Override
	public void paint(Graphics g) {
		Graphics2D g2d=(Graphics2D)g;
		Shape oldclip=g.getClip();
		int cornersize=getHeight()/3;
		Polygon p = new Polygon();
		p.addPoint(0, 0);
		p.addPoint(getWidth()-cornersize, 0);
		p.addPoint(getWidth(), cornersize);
		p.addPoint(getWidth(), getHeight());
		p.addPoint(0, getHeight());
		
		g2d.clip(p);
		if (!getModel().isArmed() && getModel().isPressed() || !isEnabled()){
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paint(g);
		g.setColor(Swat.darkShadow);
		g.drawLine(getWidth()-cornersize-1, 1, getWidth()-cornersize-1, cornersize-1);
		g.drawLine(getWidth()-cornersize, cornersize, getWidth()-1, cornersize);
		g.drawLine(getWidth()-cornersize, 1, getWidth()-1, cornersize);
		g.setClip(oldclip);
	}
}