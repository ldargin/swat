package storyTellerPackage;

import java.awt.KeyboardFocusManager;
import java.rmi.NotBoundException;

import javax.swing.JApplet;
import javax.swing.UIManager;

import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.Utils;


public class Applet extends JApplet {
	private static final long serialVersionUID = 0L;

	private Storyteller st;
	
	@Override
	public void init() {
		System.setProperty("apple.awt.antialiasing", "on");
		System.setProperty("apple.awt.textantialiasing", "on");

		super.init();

		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
		} catch (Exception evt) {
		}
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
		      public void uncaughtException(Thread t, Throwable e) {
		    	  e.printStackTrace();
		    	  Utils.showErrorDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), "The application has failed. You have found a bug!", "Uncaught error", e);
		      }
		    });

		String storyworldID = getParameter("storyworldID");
		if (storyworldID != null) {
			Storyteller st=null;
			try {
				st=new Storyteller(storyworldID,false);
				st.setVisible(true);
				st.startStory("once upon a time");
			} catch (NotBoundException re) {
				re.printStackTrace();
				Utils.showErrorDialog(st, "I'm sorry, but I'm not able to play the storyworld.\nThe server is unavailable.\nStoryteller version: "+SharedConstants.version,"Connection error");
			} catch (Exception re) {
				re.printStackTrace();
				Utils.showErrorDialog(st, "Error when connecting to the server.","Connection error",re);
			}
		} else {
			System.out.println("No storyworld specified");
		}
	}
	
	@Override
	public void destroy() {
		if (st==null || st.isClean())
			return;

		try {
			Utils.saveProperties();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			st.logout(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
