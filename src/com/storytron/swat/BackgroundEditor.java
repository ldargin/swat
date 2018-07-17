package com.storytron.swat;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;

import com.storytron.enginecommon.ScaledImage;
import com.storytron.uber.FloatTrait;

/**
 * A class for editing background information about things.
 * It displays an image, a text description and a list of traits.
 * */
public abstract class BackgroundEditor extends JDialog implements AWTEventListener, ComponentListener {
	static final long serialVersionUID=0;
	
	/** Called whenever the description text is edited. */
	public abstract void onDescriptionChange(String newDescription);
	
	/** Called whenever the image is edited. */
	public abstract void onImageChange(ScaledImage newImage);

	/** 
	 * Height of the image panel portion where the image is 
	 * vertically centered. 
	 * */
	private static final int UPPER_IMAGE_PANEL_HEIGHT = 210;

	private JButton imageButton;
	private JButton deleteButton;
	private JFileChooser chooser;
	private GlassPane gp;
	
	private BackgroundInformationPanel bgPanel;
	

	/** Sets the description text. */
	public void setDescription(String desc){
		bgPanel.setDescription(desc);
	}
	
	/** 
	 * Sets an image. If the filename is null the current image is deleted.
	 * The image is set to have height of 200 pixels.
	 * @return true if the image was set. It is possible to return false when
	 * the given filename is wrong or is not a loadable image.
	 *  */
	private boolean setImage(String filename){
		if (bgPanel.setImage(filename)) {
			ydiff=bgPanel.getImage().getHeight();
			return true;
		}
		return false;
	}

	/** Sets the traits values. Call first {@link #setTraits(Iterable)}. */
	public void setTraitValues(float[] values){
		bgPanel.setTraitValues(values);
	};

	/** Sets the traits. */
	public void setTraits(Iterable<FloatTrait> traits){
		bgPanel.setTraits(traits);
	};

	public void setTraitNames(Iterable<String> traitNames,Iterable<String> traitDescriptions){
		bgPanel.setTraitNames(traitNames,traitDescriptions);
	};

	public BackgroundEditor(JFrame owner){
		super(owner);

		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		bgPanel = new BackgroundInformationPanel(true){
			private static final long serialVersionUID = 1L;
			@Override
			public void onDescriptionChange(String newDescription) {
				BackgroundEditor.this.onDescriptionChange(newDescription);
			}
			public void onImageChange(ScaledImage newImage) {
				BackgroundEditor.this.onImageChange(newImage);
			};
		};
		bgPanel.setBorder(BorderFactory.createLineBorder(Color.black));

		
		chooser = new JFileChooser();
		chooser.setFileFilter(new FileFilter(){
			@Override
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;

				String name=f.getName();
				int i=name.lastIndexOf('.');
				String ext=i==-1?null:name.substring(i).toLowerCase();
				return ext!=null && (ext.equals(".png") || ext.equals(".jpeg")
						|| ext.equals(".bmp") || ext.equals(".gif"));
			}

			@Override
			public String getDescription() {
				return "Image file ( .png, .jpeg, .bmp, .gif )";
			}			
		});

		imageButton = new JButton("Choose an image ...");
		imageButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (chooser.showOpenDialog(BackgroundEditor.this)==JFileChooser.APPROVE_OPTION){
					File f=chooser.getSelectedFile();
					setImage(f.getPath());
					onImageChange(bgPanel.getImage());
				}
			}
		});

		deleteButton=new JButton("Remove the image");
		deleteButton.setEnabled(false);
		deleteButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) { 
				deleteImage();
				onImageChange(null);
			}
		});
		
		setPanels();
		
		addComponentListener(this);
	}

	public void componentHidden(ComponentEvent e) {
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
	}
	public void componentShown(ComponentEvent e) {
		Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
	}
	public void componentMoved(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {}
	
	private void setPanels(){

		JComponent upperPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		upperPanel.add(Box.createHorizontalGlue());
		upperPanel.add(imageButton);
		upperPanel.add(deleteButton);
			
		JComponent mainPanel = Box.createVerticalBox();
		mainPanel.add(upperPanel);
		mainPanel.add(bgPanel);

		mainPanel.setTransferHandler(new FileTransferHandler());

		getContentPane().add(mainPanel);
		setResizable(false);
		
		gp=new GlassPane();
		setGlassPane(gp);
		gp.setVisible(true);
		pack();
	}

	/** Sets the current image. */
	public void setImage(ScaledImage image){
		if (image!=null){
			setImage(image,1.0);
			ydiff=bgPanel.getImage().getHeight();
		} else
			deleteImage();		
	}
	
	/** Sets the current image. tw and th especify tentative
	 *  dimensions. 
	 * */
	private void setImage(ScaledImage img,double f){
		bgPanel.setImage(img,f);
		deleteButton.setEnabled(true);
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
		bgPanel.setImage((ScaledImage)null);
		deleteButton.setEnabled(false);
	}
	
	private int sqr(int x){ return x*x; }
	private boolean resizing=false;
	private int ydiff=0;
	public void mousePressed(MouseEvent e) {
		if (bgPanel.getImage()==null) 
			return;
		
		Point p=SwingUtilities.convertPoint(bgPanel.getLabelPanel(), bgPanel.getImageLabel().getLocation(),(Component)e.getSource());
		if (sqr(e.getX()-p.x)+sqr(e.getY()-p.y-bgPanel.getImageLabel().getHeight())<100){
			if (e.getY()-p.y+bgPanel.getImageLabel().getY()>UPPER_IMAGE_PANEL_HEIGHT)
				ydiff=e.getY()-p.y+bgPanel.getImageLabel().getY();
			else
				ydiff=2*(e.getY()-p.y)-Math.min(UPPER_IMAGE_PANEL_HEIGHT, bgPanel.getImage().getHeight());
			ydiff=Math.max(30,ydiff);
			resizing=true;
		}
		gp.repaint();
	}
	public void mouseReleased(MouseEvent e) { 
		if (resizing){
			Point p=SwingUtilities.convertPoint(bgPanel.getLabelPanel(), bgPanel.getImageLabel().getLocation(),(Component)e.getSource());
			resizing=false;
			int diff;
			if (e.getY()-p.y+bgPanel.getImageLabel().getY()>UPPER_IMAGE_PANEL_HEIGHT)
				diff=e.getY()-p.y+bgPanel.getImageLabel().getY();
			else
				diff=2*(e.getY()-p.y)-Math.min(UPPER_IMAGE_PANEL_HEIGHT, bgPanel.getImage().getHeight());
			if (diff<30)
				diff=30;
			double f=diff/(double)bgPanel.getImage().getHeight();
			setImage(bgPanel.getImage(),f);
			ydiff=bgPanel.getImage().getHeight();
			onImageChange(bgPanel.getImage());
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (resizing){
			Point p=SwingUtilities.convertPoint(bgPanel.getLabelPanel(), bgPanel.getImageLabel().getLocation(),(Component)e.getSource());
			if (e.getY()>p.y) {
				if (e.getY()-p.y+bgPanel.getImageLabel().getY()>UPPER_IMAGE_PANEL_HEIGHT)
					ydiff=e.getY()-p.y+bgPanel.getImageLabel().getY();
				else
					ydiff=2*(e.getY()-p.y)-Math.min(UPPER_IMAGE_PANEL_HEIGHT, bgPanel.getImage().getHeight());
				ydiff=Math.max(30,ydiff);
				gp.repaint();
			}
		}
	}
	
	public void eventDispatched(AWTEvent event) {
		if (event.getID()==MouseEvent.MOUSE_PRESSED)
			mousePressed((MouseEvent)event);
		else if (event.getID()==MouseEvent.MOUSE_RELEASED)
			mouseReleased((MouseEvent)event);
		else if (event.getID()==MouseEvent.MOUSE_DRAGGED)
			mouseDragged((MouseEvent)event);
	}

	private class GlassPane extends JComponent {
		static final long serialVersionUID=0;
		@Override
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			if (bgPanel.getImage()!=null){
				g.setColor(Color.black);
				int w=bgPanel.getImage().getWidth();
				int h=bgPanel.getImage().getHeight();
				if (resizing) {
					int tw = w*ydiff/h;
					int th = ydiff;
					w=Math.min(tw,BackgroundInformationPanel.IMAGE_PANEL_WIDTH);
					h=Math.min(th,bgPanel.getHeight());
					if (tw!=w || th!=h)
						if (tw*h>th*w)
							h=th*w/tw;
						else w=tw*h/th;			
				}
				Point p=SwingUtilities.convertPoint(bgPanel.getLabelPanel(), getImageLocation(w, h),this);
				if (resizing)
					g.drawRect(p.x, p.y, w, h);
			
				g.drawRect(p.x-5, p.y+h-5, 10, 10);
			}
		}
	}
	
	
	private int firstBlank(String s){
		for(int i=0;i<s.length();i++)
			if (Character.isWhitespace(s.charAt(i)))
				return i;
		return s.length();
	}
	
	class FileTransferHandler extends TransferHandler {
		static final long serialVersionUID=0;
		
	    @SuppressWarnings("unchecked")
		public boolean importData(JComponent c, Transferable t) {
			try {
				File f = null;
				if(t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					String s = (String)t.getTransferData(DataFlavor.stringFlavor);
					f=new File(new URI(s.substring(0,firstBlank(s))));
				} else if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
					List fl = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
					if (!fl.isEmpty())
						f=(File)fl.get(0);
				}
				if (f!=null) {
					boolean set=setImage(f.getPath());
					if (set)
						onImageChange(bgPanel.getImage());
					return set;
				}
			} catch( IOException exception )  {
				exception.printStackTrace();
			} catch( UnsupportedFlavorException ufException ){
				ufException.printStackTrace();
			} catch (URISyntaxException ex){
				ex.printStackTrace();
			}
			return false;
	    }

	    @Override
	    protected Transferable createTransferable(JComponent c) {
	        return null;
	    }

	    @Override
	    public int getSourceActions(JComponent c) {
	        return COPY;
	    }

	    @Override
	    protected void exportDone(JComponent c, Transferable data, int action) {
	    }

	    public boolean canImport(JComponent c, DataFlavor[] flavors) {
	        if (hasStringFlavor(flavors)) { return true; }
	        return false;
	    }

	    private boolean hasStringFlavor(DataFlavor[] flavors) {
	        for (int i = 0; i < flavors.length; i++){
	            if (DataFlavor.stringFlavor.equals(flavors[i]) ||
	            	DataFlavor.javaFileListFlavor.equals(flavors[i]))
	                return true;
	        }
	        return false;
	    }
	}
}
