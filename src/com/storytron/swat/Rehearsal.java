package com.storytron.swat;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.storytron.enginecommon.Triplet;
import com.storytron.enginecommon.Utils;
import com.storytron.enginecommon.VerbData;
import com.storytron.swat.util.LineBreaker;
import com.storytron.uber.Deikto;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.Verb;

public final class Rehearsal extends JDialog {
	private static final long serialVersionUID = 1L;
	private static int verbRadius = 60;  // radius of the circle enclosing the central verb
	private static final int roleRadius = 100;  // radius of the circle on whose edge roles are placed
	private static final int optionRadius = 300; // radius of the circle on whose edge options are placed
	private ArrayList<VerbData> verbData = new ArrayList<VerbData>(); // the raw results data returned from the engine
	// The centralVerb is the verb placed in the center of the screen, whose activity we are displaying.
	private String centralVerbLabel = "X";
	private Verb centralVerb;
	private VerbData centralVerbData;
	private Deikto dk;
	// hotspots are the rectangles on the screen that respond to a mouseclick
	// They are calculated during painting, but works, does not affect much performace,
	// and needs the Graphic object to findout the size the rectangles must have.
	private ArrayList<HotSpot> hotspots = new ArrayList<HotSpot>();
	// poisonings records all cases of poisoning during the rehearsal
	private ArrayList<Triplet<Script.Type,String[],String>> poisonings = new ArrayList<Triplet<Script.Type,String[],String>>();
	private JButton poisonButton, threadKillerButton, loopyBoobyButton, backButton;
	// This traces the sequence of steps that the user has taken in traversing the network of results.
	// It is used by the Back button to retrace those steps.
	private ArrayList<Verb> previousVerbs = new ArrayList<Verb>();
	private final Font trebuchetFont = new Font("Trebuchet", Font.BOLD, 14);
	private RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
	private Color verbCircleColor = new Color(216, 216, 255);
	private Color backgroundColor = new Color(192, 192, 255);
	private Color verbTextColor = new Color(0,128,0);
	private Color roleTextColor = new Color(0, 0, 255);
	private Stroke verbCircleStroke = new BasicStroke(3);
	 /* break line "Great Text To Split" as "Great " "Text " "To " "Split" and
	  * "GreatTextToSplit" as "Great" "Text" "To" "Split" 
	  * */
	private LineBreaker lineBreaker = new LineBreaker("([^\\p{javaUpperCase}\\s]+)|(\\p{javaUpperCase}+[^\\p{javaUpperCase}\\s]*)|\\s+|\\S+",8);
//**********************************************************************	
	private class HotSpot {
		Rectangle box;
		String label;
		Verb mVerb;
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		public HotSpot(Rectangle tBox, String tLabel) {
			box = tBox;
			label = tLabel;
			mVerb = dk.getVerb(tLabel);
		}
		//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	}
//**********************************************************************	
	public Rehearsal(final Swat swat) {
		super(swat.getMyFrame());
		this.dk = swat.dk;
		hotspots.clear();

		setLayout(null);
		Box westBox = Box.createVerticalBox();
		westBox.setLocation(0,0);
		poisonButton = new JButton("Poison");
		poisonButton.setToolTipText(Utils.toHtmlTooltipFormat("display all poisonings (Scripts that failed to calculate correctly because of an intrinsic problem)"));
		poisonButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ScriptTable st = new ScriptTable(swat);
				st.displayScriptOwners(Rehearsal.this,poisonings);
			}
		});
		poisonButton.setEnabled(true);
		westBox.add(poisonButton);
		
		threadKillerButton = new JButton("ThreadKillers");
		threadKillerButton.setToolTipText(Utils.toHtmlTooltipFormat("display threadkillers (Verb executions that did not have any Options executed in response)"));
		threadKillerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayThreadKillers();
			}
		});
		threadKillerButton.setEnabled(true);
		westBox.add(threadKillerButton);

		loopyBoobyButton = new JButton("LoopyBoobys");
		loopyBoobyButton.setToolTipText(Utils.toHtmlTooltipFormat("display Loopy Boobys (Verb executions that got caught in repetitive loops)"));
		loopyBoobyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayLoopyBoobies();
			}
		});
		loopyBoobyButton.setEnabled(true);
		westBox.add(loopyBoobyButton);

		backButton = new JButton("Back");
		backButton.setToolTipText("go back to previous Verb");
		backButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int cSteps = previousVerbs.size()-1;
				centralVerb = previousVerbs.get(cSteps);
  				centralVerbLabel = centralVerb.getLabel();
  				centralVerbData = verbData.get(dk.findVerb(centralVerbLabel));
				previousVerbs.remove(cSteps);
				repaint();
  				if (cSteps == 0)
  					backButton.setEnabled(false);
				}
		});
		backButton.setEnabled(false);
		westBox.add(backButton);
		
		setContentPane(new RehearsalPanel());
		setPreferredSize(new Dimension(924,700));
		setSize(getPreferredSize());
		setLocationRelativeTo(swat.getMyFrame());
		getContentPane().add(westBox);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(WindowEvent e) {
				swat.rehearsalLizardMenuItem.setEnabled(true);
			}
		});
		addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
			}
			public void mouseEntered(MouseEvent e) {
			}
			public void mouseExited(MouseEvent e) {
			}
			public void mousePressed(MouseEvent e) {
			}
			public void mouseReleased(MouseEvent e) {
				for (int i=0; (i<hotspots.size()); ++i) {
					if (hotspots.get(i).box.contains(e.getX(), e.getY())) {
						// Replace the current centralVerb and centralVerbLabel with the selected one
						previousVerbs.add(centralVerb);
						centralVerbLabel = hotspots.get(i).label;
						centralVerb = hotspots.get(i).mVerb;
						centralVerbData = verbData.get(dk.findVerb(centralVerbLabel));
						backButton.setEnabled(true);
						repaint();
					} 
				}
			}
		});
	}
//**********************************************************************	
	public void passData(ArrayList<VerbData> tVerbData, int tCentralVerbIndex, ArrayList<Triplet<Script.Type,String[],String>> tPoisonings) {
		verbData = tVerbData;
		centralVerbData = verbData.get(tCentralVerbIndex);
		centralVerb = dk.getVerb(tCentralVerbIndex);
		centralVerbLabel = centralVerb.getLabel();
		poisonings = tPoisonings;
	}
//**********************************************************************	
	private class RehearsalPanel extends JPanel {
	private static final long serialVersionUID = 1L;
		public RehearsalPanel(){
			super(new FlowLayout(FlowLayout.LEFT));
		}
		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			Stroke oldStroke = g2.getStroke();
			{
				RenderingHints temp = g2.getRenderingHints();
				temp.add(renderingHints);
				g2.setRenderingHints(temp);
			}
			g2.setColor(backgroundColor);
			int height = getContentPane().getHeight();
			int width = getContentPane().getWidth();
			g2.fillRect(0, 0, width, height);

			int centerY = height/2;
			int centerX = width/2;
			g2.setColor(Color.black);
			g2.setFont(trebuchetFont);
			Iterable<String> centralVerbL = linebreakString(centralVerbLabel, lineBreaker);
			Rectangle2D firstBounds = multilineStringSize(g2, centralVerbL);
			{
				int offsetX = (int)(firstBounds.getWidth()/2);
				int offsetY = (int)(firstBounds.getHeight()/2);		
				verbRadius = (int)(Math.hypot(firstBounds.getWidth(), firstBounds.getHeight())/2)+8;
				g2.setColor(verbCircleColor);
				g2.fillOval(centerX - verbRadius, centerY - verbRadius, 2*verbRadius, 2*verbRadius);
				g2.setStroke(verbCircleStroke);
				g2.setColor(Color.black);
				g2.drawOval(centerX - verbRadius, centerY - verbRadius, 2*verbRadius, 2*verbRadius);
				g2.setColor(verbTextColor);
				drawMultilineString(g2,centralVerbL, centerX-offsetX, centerY-offsetY-5);
				g2.setPaint(Color.black);
				String temp = ((Integer)centralVerbData.activations).toString();
				g2.drawString(temp,centerX-(int)stringSize(g2,temp).getWidth()/2, centerY+offsetY+7);
			}

			int cRoles = centralVerb.getRoleCount();
			float roleAngle = 6.28319f / cRoles;
			int mostActivations = 0;
			for (int i=0; (i<cRoles); ++i) {
				if (centralVerbData.roleData.get(i).activations > mostActivations)
					mostActivations = centralVerbData.roleData.get(i).activations;
			}
			float scaleFactor = 1.0f;
			if (mostActivations>30)
				scaleFactor = 30.0f / mostActivations;
			hotspots.clear();
			for (int i=0; (i<cRoles); ++i) {
				Role.Link zRole = centralVerb.getRole(i);
				float fStroke = scaleFactor*centralVerbData.roleData.get(i).activations;
				g2.setStroke(new BasicStroke(fStroke,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
				int stroke = (int)fStroke/2;
				int roleX = centerX - (int)(roleRadius * Math.sin(((double)i*roleAngle)));
				int roleY = centerY - (int)(roleRadius * Math.cos(((double)i*roleAngle)));
				Iterable<String> zRoleL = linebreakString(zRole.getLabel(), lineBreaker);
				Rectangle roleTextBounds = multilineStringSize(g2, zRoleL);
				{
					int offsetX = (int)(roleTextBounds.getWidth()/2);
					int offsetY = (int)(roleTextBounds.getHeight()/2);
					roleTextBounds.setLocation(roleX-offsetX, roleY-offsetY);
					g2.setPaint(roleTextColor);
					drawMultilineString(g2,zRoleL, roleX-offsetX, roleY-offsetY);
				}
				g2.setPaint(Color.black);
				int x2 = centerX - (int)((verbRadius+stroke) *Math.sin(((double)i*roleAngle)));
				int y2 = centerY - (int)((verbRadius+stroke) *Math.cos(((double)i*roleAngle)));
				roleTextBounds.setBounds((int)roleTextBounds.getX()-10,(int)roleTextBounds.getY()-10,
						(int)roleTextBounds.getWidth()+20,(int)roleTextBounds.getHeight()+20);
				{
					Point p = ellipseSegmentIntersect(roleTextBounds,x2,y2);
					if (stroke>0) {
						g2.drawLine(x2, y2, p.x, p.y);
					}
				}

				float optionRange = 6.28319f/3.5f;
				int cOptions = zRole.getRole().getOptions().size();
				float optionSeparation = optionRange/cOptions;

				for (int j=0; (j<cOptions); ++j) {
					String optionLabel = zRole.getRole().getOptions().get(j).getLabel();
					fStroke = scaleFactor*centralVerbData.roleData.get(i).optionData.get(j).activations;
					g2.setStroke(new BasicStroke(fStroke,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
					stroke = (int)fStroke;
					float startAngle = i*roleAngle-optionRange/2+optionSeparation/2;
					double localAngle = (double)(startAngle+j*optionSeparation);
					float sinValue = (float)Math.sin(localAngle);
					int optionX = centerX - (int)(optionRadius * sinValue);
					float cosValue = (float)Math.cos(localAngle);
					int optionY = centerY - (int)(optionRadius * cosValue);
					Iterable<String> optionL = linebreakString(optionLabel, lineBreaker);
					Rectangle optionTextBounds = multilineStringSize(g2, optionL);
					int offsetX = (int)((optionTextBounds.getWidth())/2);
					int offsetY = (int)((optionTextBounds.getHeight())/2);
					optionTextBounds.setLocation(optionX-offsetX, optionY-offsetY);

					g2.setColor(verbTextColor);
					drawMultilineString(g2,optionL, optionX-offsetX, optionY-offsetY);
					g2.setColor(Color.black);

					HotSpot zHotspot = new HotSpot(optionTextBounds, optionLabel);
					hotspots.add(zHotspot);
					if (stroke>0) {
						optionTextBounds.setBounds(
								(int)optionTextBounds.getX()-15,(int)optionTextBounds.getY()-15,
								(int)optionTextBounds.getWidth()+30,(int)optionTextBounds.getHeight()+30);
						Point p1 = ellipseSegmentIntersect(roleTextBounds,optionX,optionY);
						Point p2 = ellipseSegmentIntersect(optionTextBounds,roleX,roleY);
						g2.drawLine(p1.x, p1.y, p2.x, p2.y);
					}
				}
			}
			g2.setStroke(oldStroke);
		}
	}
	
//**********************************************************************	
    /** Calculates intersection of an ellipse and a half line
     * starting at its origin.
     * @param bounds boundings of the ellipse
     * @param x horizontal coordinate of the segment extreme
     * @return the intersection 
     */
	Point ellipseSegmentIntersect(Rectangle bounds,double x,double y) {
		// Calculate ellipse center
		double ex = bounds.getX()+bounds.getWidth()/2;
		double ey = bounds.getY()+bounds.getHeight()/2;
		// Put ellipse at the origin
		double px = x-ex;
		double py = y-ey;
		// Make the ellipse a circle of radius bounds.getWidth()/2
		py*=bounds.getWidth()/bounds.getHeight();
		// make the circle of radius 1.
		px/=bounds.getWidth()/2;
		py/=bounds.getWidth()/2;
		// Calculate segment length
		double l=Math.hypot(px,py);
		// Calculate the intersection point in world coordinates
		return new Point((int)(ex+(x-ex)/l),(int)(ey+(y-ey)/l));
	}
	
//	**********************************************************************	
	Rectangle stringSize(Graphics g, String s) {
		return g.getFontMetrics().getStringBounds(s,g).getBounds();
	}
	int stringWidth(Graphics g, String s) {
		return g.getFontMetrics().stringWidth(s);
	}

	// This are some methods to handle multiline text.
	// A method to get the size, other to draw, an another one
	// to break a single line into smaller ones.
	Rectangle multilineStringSize(Graphics g, Iterable<String> ss) {
		java.util.Iterator<String> i = ss.iterator();
		if (!i.hasNext()) return new Rectangle();
		else {
			int height = g.getFontMetrics().getAscent();
			int y = 0;
			Rectangle r = stringSize(g,i.next());
			while (i.hasNext()) {
				Rectangle temp = stringSize(g,i.next());
				temp.setLocation(0, y);
				r.add(temp);
				y+=height;
			}
			return r;
		}
	}

	void drawMultilineString(Graphics g, Iterable<String> ss, int x,int y) {
		int height = g.getFontMetrics().getAscent();
		// calculate maximum width
		int max=1;
		for(String s:ss) {
			int temp = stringWidth(g,s);
			if (temp>max) max = temp;
		}
		for(String s:ss){
			y+=height;
			g.drawString(s, x + (max - stringWidth(g,s))/2 , y);			
		}
	}

	/**
	 * Breaks a string into lines.
	 * @param s The string to break.
	 * @param lb The line breaker to use.
	 * @return A collection of lines.  
	 * */
	Collection<String> linebreakString(String s,LineBreaker lb){
		LinkedList<String> res = new LinkedList<String>();
		lb.setString(s);
		for (String t : lb)
		  res.add(t);
        return res;
	};
//**********************************************************************		
	// The underlying data model of the Thread Killers table
	public static class ThreadKillerTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
		private Object[][] data ;
		private String[] headings = new String[] {"Verb", "# of ThreadKills" };
		private int numThreadKillers = 0;
		public ThreadKillerTableModel(ArrayList<VerbData> verbData, int rows, int cols)
		{
			int i = 0;
			data = new Object[verbData.size()+1][2];
			for (VerbData vd: verbData) {
				if ((vd.candidacies>0) & (vd.activations==0)) {
					//data[i][0] = vd.myVerb.getLabel();
					data[i][0] = vd.verbName;
					data[i][1] = vd.candidacies;
					++i;
				}
			}
			numThreadKillers = i;
		}
	
		public int getRowCount() { return numThreadKillers; }
		public int getColumnCount() { return data[0].length; }
		
		public Object getValueAt(int row, int column) {
			return data[row][column];
		}
		
		public void setValueAt(Object value, int row, int col) {
			String strVal = new String(value.toString());
			data[row][col] = (Object)strVal;
			fireTableDataChanged();
		}
		
		public String getColumnName(int column) {
			return headings[column];
		}
	}
		
	public void displayThreadKillers() {
		
		TableModel model = new ThreadKillerTableModel(verbData, verbData.size(), 2);

		JTable table = new JTable(model);


		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		JDialog threadKillersFrame = new JDialog(this,"ThreadKillers");
		threadKillersFrame.getContentPane().setLayout(new BorderLayout());

		JScrollPane jsp = new JScrollPane(table);
		threadKillersFrame.getContentPane().add(jsp);
//		threadKillersFrame.pack();
		threadKillersFrame.setSize(600, 500);
		threadKillersFrame.setLocation(200,200);
		threadKillersFrame.setVisible(true);
		threadKillersFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}


//**********************************************************************		
	// The underlying data model of the Thread Killers table
	public static class LoopyBoobiesTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private Object[][] data ;
		private String[] headings = new String[] {"Verb", "Loop Length", "# of Loops" };
		private int numLoopyBoobies = 0;
		public LoopyBoobiesTableModel(ArrayList<VerbData> verbData, int rows, int cols)
		{
			int i = 0;
			data = new Object[verbData.size()+1][3];
		for (VerbData vd: verbData) {
			boolean verbIsLoopy = false;
				for (int j=0; (j<10); ++j) {
					if (vd.cLoopyBoobies[j]>0) 
					verbIsLoopy = true;
			}
			if (verbIsLoopy) {
					//String loopVerb = vd.myVerb.getLabel();
					String loopVerb = vd.verbName;
					//data[i][0] = vd.myVerb.getLabel();
					for (int j=0; (j<10); ++j) {
						if (vd.cLoopyBoobies[j]>0) {
							// textArea.append(" "+j+":"+vd.cLoopyBoobies[j]+"  ");
							data[i][0] = loopVerb;
							data[i][1] = j;
							data[i][2] = vd.cLoopyBoobies[j];
							++i;
				}
						}
					}
				}
			numLoopyBoobies = i;
		}
		
		public int getRowCount() { return numLoopyBoobies; }
		public int getColumnCount() { return data[0].length; }
		
		public Object getValueAt(int row, int column) {
			return data[row][column];
		}
		
		public void setValueAt(Object value, int row, int col) {
			String strVal = new String(value.toString());
			data[row][col] = (Object)strVal;
			fireTableDataChanged();
		}
		
		public String getColumnName(int column) {
			return headings[column];
		}
	}
	public void displayLoopyBoobies() {
		
		TableModel model = new LoopyBoobiesTableModel(verbData, verbData.size(), 3);

		JTable table = new JTable(model);


		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		JDialog loopyBoobies = new JDialog(this,"LoopyBoobies");
		loopyBoobies.getContentPane().setLayout(new BorderLayout());

		JScrollPane jsp = new JScrollPane(table);
		loopyBoobies.getContentPane().add(jsp);
		loopyBoobies.pack();
		loopyBoobies.setSize(600, 500);
		loopyBoobies.setLocation(200,200);
		loopyBoobies.setVisible(true);
		loopyBoobies.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
	}
//**********************************************************************
//	public JPanel getMyPanel() {
//		return myPanel;
//	}
//**********************************************************************	

}
