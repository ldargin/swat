package com.storytron.swat;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.text.JTextComponent;

import com.storytron.enginecommon.ScaledImage;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.EditorListener;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Quantifier;

/**
 * A class for editing and displaying background information about things.
 * It displays an image, a text description and a list of traits.
 * */
public abstract class BackgroundInformationPanel extends JPanel {
	static final long serialVersionUID=0;
	
	/** Called whenever the description text is edited. */
	public abstract void onDescriptionChange(String newDescription);
	
	/** Called whenever the image is edited. */
	public abstract void onImageChange(ScaledImage newImage);

	/** Height of the main panel. */
	private static final int PANEL_HEIGHT = 450;
	/** Width of the panels for text and traits. */
	private static final int LEFT_PANEL_WIDTH = 400;
	/** Width of the image panel. */
	public static final int IMAGE_PANEL_WIDTH = 290;
	/** 
	 * Height of the image panel portion where the image is 
	 * vertically centered. 
	 * */
	private static final int UPPER_IMAGE_PANEL_HEIGHT = 210;

	private ScaledImage image;
	private JLabel imageLabel=null;
	private JComponent messageLabel;
	private JComponent labelPanel = new JPanel(null);
	private JTextComponent textArea;
	private JComponent traitNamesPanel, traitValuesPanel;
	private boolean editable;
	

	/** Sets the description text. */
	public void setDescription(String desc){
		if (!editable)
			desc = Utils.insertHtmlBreaks(desc);
		textArea.setText(desc);
		textArea.setCaretPosition(0);
	}
	
	/** 
	 * Sets an image. If the filename is null the current image is deleted.
	 * The image is set to have height of 200 pixels.
	 * @return true if the image was set. It is possible to return false when
	 * the given filename is wrong or is not a loadable image.
	 *  */
	boolean setImage(String filename){
		if (filename==null)
			deleteImage();
		else {
			ScaledImage tmp;
			try {
				tmp = new ScaledImage(javax.imageio.ImageIO.read(new File(filename)));
			} catch(IOException ex){
				tmp= null;
			}
			if (tmp==null)
				return false;
			if (tmp==null)
				deleteImage();
			else {
				setImage(tmp,1.0);
				this.image.applyScaling();
			}
		}
		return true;
	}

	/** Returns the image being displayed. */
	public ScaledImage getImage(){
		return image;
	}
	
	/** Returns the description being displayed. */
	public String getDescription(){
		return textArea.getText();
	}
	
	/** Sets the traits values. Call first set traits. */
	public void setTraitValues(float[] values){
		if (values==null){
			for(int i=0;i<traitValuesPanel.getComponentCount();i++)
				((JLabel)traitValuesPanel.getComponent(i)).setText("");
		} else {
			int i=0;
			for(float f:values){
				((JLabel)traitValuesPanel.getComponent(i)).setText(
						float2Quantifier(((JLabel)traitNamesPanel.getComponent(i)).getText(),f));
				i++;
			}
		}
		traitValuesPanel.validate();
	};

	/** Sets the traits. */
	public void setTraits(Iterable<FloatTrait> traits){
		traitNamesPanel.removeAll();
		traitValuesPanel.removeAll();
		for(final FloatTrait t:traits){
			JLabel l = new JLabel(t.getLabel()){
				private static final long serialVersionUID = 1L;
				@Override
				public String getToolTipText() { return t.getDescription();	}
			};
			ToolTipManager.sharedInstance().registerComponent(l);
			l.setAlignmentX(1.0f);
			traitNamesPanel.add(l);
			traitValuesPanel.add(new JLabel());
		}
		traitValuesPanel.revalidate();
	};

	/** Returns the panel where the image is displayed. */
	JComponent getLabelPanel(){ return labelPanel; }

	/** Returns the image label. */
	JComponent getImageLabel(){ return imageLabel; }

	public void setTraitNames(Iterable<String> traitNames,Iterable<String> traitDescriptions){
		traitNamesPanel.removeAll();
		traitValuesPanel.removeAll();
		Iterator<String> descIt = traitDescriptions.iterator();
		for(String name:traitNames){
			JLabel l = new JLabel(name);
			if (descIt.hasNext())
				l.setToolTipText(descIt.next());
			l.setAlignmentX(1.0f);
			traitNamesPanel.add(l);
			traitValuesPanel.add(new JLabel());
		}
		traitValuesPanel.revalidate();
	};

	private String float2Quantifier(String traitName,float value){
		 int quantifierIndex = (int)((value * 5.0f) + 5.5f);
		 if (quantifierIndex < 0)
			 quantifierIndex = 0;
		 if (quantifierIndex > 10)
			 quantifierIndex = 10;
		return Quantifier.getQuantifierLabel(traitName,quantifierIndex,true);
	}
	
	public BackgroundInformationPanel(boolean editable){
		super(null);
		setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
		this.editable = editable;
		
		if (editable)
			messageLabel=new JLabel("<html><center>Drag an image file here.</center></html>");
		else
			messageLabel=new JLabel(" ");
		
		setPanels();
		
		textArea.requestFocusInWindow();
		if (editable)
			textArea.selectAll();
		else 
			textArea.setEditable(false);
	}

	private void setPanels(){
		if (editable) {
			Swat.TextArea text = new Swat.TextArea("Write a description here.");
			//		textArea.setColumns(25);
			Font f=text.getFont();
			text.setFont(new Font(f.getName(),f.getStyle(),15));
			text.setMargin(new Insets(0,0,0,5));
			text.setLineWrap(true);
			text.setWrapStyleWord(true);
			text.setOpaque(false);
			new EditorListener(text){
				private static final long serialVersionUID = 1L;
				@Override
				public boolean timedActionPerformed(ActionEvent e) {
					onDescriptionChange(textArea.getText());
					return true;
				}
				@Override
				public String getText() { return null; }
			};
			
			textArea = text;
		} else {
			JEditorPane textPane = new JEditorPane();
			Font f=textPane.getFont();
			textPane.setFont(new Font(f.getName(),f.getStyle(),15));
			textPane.setMargin(new Insets(0,0,0,5));
			textPane.setOpaque(false);
			textPane.setContentType("text/html");
			textArea = textPane;
		}
		
		JScrollPane textScroll = new JScrollPane(textArea);
		textScroll.getViewport().setOpaque(false);
		textScroll.setOpaque(false);
		textScroll.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 10));
		textScroll.setPreferredSize(new Dimension(350,UPPER_IMAGE_PANEL_HEIGHT));
		textScroll.setMinimumSize(textScroll.getPreferredSize());

		traitNamesPanel = Box.createVerticalBox();
		for(int i=0;i<traitNamesPanel.getComponentCount();i++)
			((JComponent)traitNamesPanel.getComponent(i)).setAlignmentX(1.0f);
		traitNamesPanel.setMinimumSize(new Dimension(150,100));

		traitValuesPanel = Box.createVerticalBox();
		traitValuesPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
		for(int i=0;i<traitValuesPanel.getComponentCount();i++)
			((JComponent)traitValuesPanel.getComponent(i)).setAlignmentX(0.0f);

		JComponent traitPanel = Box.createHorizontalBox();
		traitPanel.add(traitNamesPanel);
		traitPanel.add(traitValuesPanel);
		traitPanel.add(Box.createHorizontalGlue());
		
		JComponent traitCompressPanel = Box.createHorizontalBox();
		traitCompressPanel.add(traitPanel);
		traitCompressPanel.add(Box.createHorizontalGlue());
		
		JScrollPane traitScroll = new JScrollPane(traitCompressPanel);
		traitScroll.getViewport().setOpaque(false);
		traitScroll.setOpaque(false);
		traitScroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 10));
		traitScroll.setPreferredSize(textScroll.getPreferredSize());
		
		JComponent leftPanel=Box.createVerticalBox();
		textScroll.setAlignmentX(0.0f);
		leftPanel.add(textScroll);
		traitScroll.setAlignmentX(0.0f);
		leftPanel.add(traitScroll);
		leftPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH,leftPanel.getPreferredSize().height));
		leftPanel.setMinimumSize(leftPanel.getPreferredSize());
		Dimension d = leftPanel.getPreferredSize();
		d.height = Integer.MAX_VALUE;
		leftPanel.setMaximumSize(d);

		messageLabel.setSize(messageLabel.getPreferredSize());
		messageLabel.setLocation((IMAGE_PANEL_WIDTH-messageLabel.getWidth())/2, 20);

		labelPanel.setOpaque(false);
		labelPanel.add(messageLabel);
		d=new Dimension(IMAGE_PANEL_WIDTH,PANEL_HEIGHT);
		labelPanel.setPreferredSize(d);
		labelPanel.setMinimumSize(d);
		labelPanel.setMaximumSize(d);
				
		setOpaque(false);
		add(leftPanel);
		add(labelPanel);
	}

	/** Sets the current image. */
	public void setImage(ScaledImage image){
		if (image!=null)
			setImage(image,1.0);
		else
			deleteImage();		
	}
	
	/** Sets the current image. tw and th especify tentative
	 *  dimensions. 
	 * */
	void setImage(ScaledImage img,double f){
		this.image=img.clone();
		int tw=(int)(image.getWidth()*f);
		int th=(int)(image.getHeight()*f);
		int w=Math.min(tw,IMAGE_PANEL_WIDTH);
		int h=Math.min(th,getHeight());
		if (tw!=w || th!=h)
			if (tw*h>th*w)
				h=th*w/tw;
			else w=tw*h/th;			
		image.scaleTo(w,h);
		
		labelPanel.setBorder(BorderFactory.createEmptyBorder());
		labelPanel.removeAll();
		imageLabel=new JLabel(new ImageIcon(image.getBufferedImage()));
		imageLabel.setSize(image.getWidth(),image.getHeight());
		labelPanel.add(imageLabel);
		imageLabel.setLocation(getImageLocation(image.getWidth(),image.getHeight()));
		
		Point p=SwingUtilities.convertPoint(labelPanel.getParent(), labelPanel.getLocation(), this);
		repaint(p.x-10,p.y,labelPanel.getWidth()+10,labelPanel.getHeight());
	}
	
	/** Given the width and height determines the location of the image. */
	private Point getImageLocation(int w,int h){
		int y;
		if (h>UPPER_IMAGE_PANEL_HEIGHT)
			y=0;
		else
			y=(UPPER_IMAGE_PANEL_HEIGHT-h)/2;
		return new Point((295-w)/2,y);
	}
	
	private void deleteImage(){
		image = null;
		labelPanel.removeAll();
		if (editable) {
			labelPanel.add(messageLabel);	
			labelPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		}
		repaint();
		messageLabel.revalidate();
	}
}
