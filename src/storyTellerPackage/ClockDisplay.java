package storyTellerPackage;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.storytron.swat.util.LightweightPopup;

/** 
 * A popup display for showing passage of time.
 * It displays a text "nnn time passes" that fades in, stays a little, and
 * then fades out. 
 * */
class ClockDisplay extends JPanel {
	private static final long serialVersionUID = 0L;
	private static final Font CLOCK_FONT = new Font("Serif", Font.PLAIN, 18);
	/** Amount of milliseconds of the fade in effect. */
	private static final int FADEIN = 500;
	/** Amount of milliseconds the message stays on screen at full opacity. */
	private static final int STAYTIME = 1500;
	/** Amount of milliseconds of the fade out effect. */
	private static final int FADEOUT = 2000;
	/** Popup for drawing the clock onto. */
	private LightweightPopup clockDisplayPopup = new LightweightPopup();
	/** Text message to display. */
	private String text;
	/** Opacity to use when showing the message. */
	private int a=255;
	/** Starting time of a fading effect. */
	private long inittime;
	/** Tells whether we are fading in or out. */
	private boolean fadein;
	/** Timer for the STAYTIME period. */
	private Timer timer = new Timer(STAYTIME,new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			inittime = System.currentTimeMillis();
			opacityTimer.start();
		}});
	/** Timer for the fading effects. */
	private Timer opacityTimer = new Timer(30,new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			int delta = (int)(System.currentTimeMillis()-inittime);
			
			if (fadein) {
				a=Math.min((int)(255*delta/FADEIN),255);
				if (delta>=FADEIN) {
					opacityTimer.stop();
					timer.start();
					fadein=false;
				}
			} else {
				a=Math.max((int)(255-255*delta/FADEOUT),0);
				if (delta>=FADEOUT) {
					opacityTimer.stop();
					clockDisplayPopup.hidePopup();
				}
			}
			repaint();
		}});
	
	public ClockDisplay(){
		super(null);
		setOpaque(false);
		clockDisplayPopup.setContents(this);
		timer.setRepeats(false);
	}
	/** 
	 * Call this to display the passed time.
	 * If delta<=1 nothing is done. 
	 * */
	public void start(Storyteller st,int delta) {
		stop();
		if (delta<=1)
			return;
		
		text = delta+" moments pass";
		FontMetrics fm = getFontMetrics(CLOCK_FONT);
		clockDisplayPopup.setSize(fm.stringWidth(text),fm.getHeight());
		clockDisplayPopup.showPopup(st, st.getWidth()/2-clockDisplayPopup.getWidth()-15,50+41);

		a=0;
		inittime = System.currentTimeMillis();
		fadein=true;
		opacityTimer.start();
	};
		
	/** Stops showing the display. */
	public void stop() {
		timer.stop();
		opacityTimer.stop();
		clockDisplayPopup.hidePopup();
	};
	
	@Override
	public void paint(Graphics g){
		super.paint(g);
		g.setFont(CLOCK_FONT);
		g.setColor(new Color(0,0,0,a));
		g.drawString(text, 0, getFontMetrics(CLOCK_FONT).getAscent());
	}

}
