package storyTellerPackage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import sun.rmi.transport.*;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.DefaultEditorKit;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.storytron.enginecommon.BadStoryworldException;
import com.storytron.enginecommon.BgItemData;
import com.storytron.enginecommon.EngineDiedException;
import com.storytron.enginecommon.IncompatibleVersionException;
import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.LocalJanus;
import com.storytron.enginecommon.MenuElement;
import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.ScaledImage;
import com.storytron.enginecommon.SentencesPanel;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.StorytellerRemote;
import com.storytron.enginecommon.StorytellerReturnData;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.BackgroundInformationPanel;
import com.storytron.swat.CopyrightEditor;
import com.storytron.swat.Swat;
import com.storytron.swat.util.FlowScrollLayout;
import com.storytron.swat.util.OSXAdapter;
import com.storytron.swat.util.ProgressDialog;
import com.storytron.uber.Deikto;
import com.storytron.uber.Sentence;

import engPackage.Janus;


/**
 * Implementation of the tool for playing stories.
 * 
 * <h4>Clean up</h4>
 * 
 * There are various ways to close storyteller. By hitting CMD-Q on Mac, by
 * quitting through the File menu Quit option, by closing the window using the window
 * manager and by killing the application. With the exception of the first and the 
 * last one, we catch all of the other exits to do a proper clean up. The clean up 
 * consists in saving custom storyteller properties to disk, and telling the server 
 * that we are quitting, so it frees whatever resources being used for the current session.  
 * <p>
 * When hitting CMD-Q clean up tasks must be done through custom shutdown hooks: 
 * {@link Runtime#addShutdownHook(Thread)}.
 * <p>
 * Why don't logout the session before quitting instead of using shutdown hooks?
 * Because we think that when hitting CMD-Q in Mac the best place to call the cleanup 
 * is in the shutdown hooks. The CMD-Q Mac event is not well integrated into the
 * java event model, so we found no other obvious place to do the clean up.
 * <p>
 * However we do have a handler for the CMD-Q event ({@link #quit()}). It is used for 
 * showing a save dialog if the current story has some progress to save. We do not
 * make the clean up there, however, because we don't know in which thread the
 * quit handler is executed. We don't want to do the clean up in the event thread.
 * <p>
 * The {@link StorytellerRemote#logout(String)} server method is called every time
 * storyteller is closed and it is not hooked to a running instance of {@link Swat}.
 * This is the case when constructed with {@link #Storyteller(String, boolean)}.
 * It is possible that the logout method be not called if Storyteller is told to
 * terminate the virtual machine on closing, that's the case when passing true
 * as argument of the constructor above. Therefore, the clean up is delegated to
 * the shutdown hooks that the caller of storyteller constructor must register
 * himself.
 * <p>
 * If storyteller is constructed with 
 * {@link #Storyteller(StorytellerRemote, String, Deikto, JFileChooser, com.storytron.swat.Swat.LogDownloaderThread)},
 * then we assume there is a {@link Swat} instance, and the method 
 * {@link StorytellerRemote#closeStoryteller(String)} will be called when closing storyteller.
 * This is so to preserve some session data on the server that may be needed to
 * run log lizard from swat.
 * <p>
 * Before calling any of these server cleanup methods, custom storyteller properties 
 * are saved to disk in all of the circumstances mentioned above, except when shutdown
 * hooks are needed to make the clean up (when the programmer must register a hook
 * that saves the properties, too).
 * */
public class Storyteller extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final Font THE_END_FONT = new Font("Serif", Font.PLAIN, 48);
	private static final Font THE_END_SMALL_FONT = new Font("Serif", Font.BOLD, 16);
	private static final int TOP_PANELS_HEIGHT = 130;
	private static final int SAVE_INACTIVE_SESSION_TIMEOUT = 15*60*1000;

	private Swat swat;
	private String login, password;
	public JComponent topPanel, leftPanel, leftOuterPanel, leftVerticalPanel, leftTopPanel, rightTopPanel, rightPanel, poisonPanel;
	public JLabel rightExpressionLabel = new JLabel();
	private JComponent bottomPanel;
	private JScrollPane leftSP;
	private JScrollPane bottomScrollPanel;
	private JSplitPane mainPanel;
	private String startVerbLabel = "once upon a time";
	private Pair<String[],String[]> actorTraitNames, stageTraitNames, propTraitNames, relationshipNames;
	private String[] actorNames, stageNames, propNames;
	private Map<String,BgItemData> actorBgData = new ConcurrentHashMap<String,BgItemData>();
	private Map<String,BgItemData> stageBgData = new ConcurrentHashMap<String,BgItemData>();
	private Map<String,BgItemData> propBgData = new ConcurrentHashMap<String,BgItemData>();
	private ActionListener doneAction;
	private SentenceDisplay.HotButtonListener inputSentenceListener;
	private JButton donebt;
	private JButton poisoningsButton;
	private JDialog storybookFrame;
	private JLabel actorNameLabel;
	private JTextArea storybookArea = new JTextArea();
	private JDialog backgroundInformation;
	private BackgroundInformationPanel bgPanel;
	private CopyrightEditor copyrightDisplay;
	private RelationshipBrowser relationshipBrowser;
	private SentenceDisplay inputSentence;
	private int time = 0, dtime = 0; 
	private boolean showBottom, freshSentence=true;
	private boolean clearLeftPanelOnNextTriggerSentence = false;
	private StorytellerRemote janus;
	private String sessionID;
	private Deikto dk = null;
	private Swat.LogDownloaderThread logDownloader;
	private boolean print2stdout = false;
	private LinkedList<Integer> recordedInput = new LinkedList<Integer>();
	private LinkedList<JMenuItem> recentStoryMenuItems = new LinkedList<JMenuItem>();
	private JMenu storytellerMenu = new JMenu("Storyteller");
	private JMenu viewMenu;
	private JFileChooser chooser;
	private File lastSavedStory;
	private String storyworldID;
	private byte[] lastLoadedState = null;
	private ClockDisplay clockDisplay = new ClockDisplay();
	private short sentenceAmountOfInput=0;
	private short sentenceCounter=0;
	private short lastSavedSentence=0;
	private short lastSavedAmountOfInput=0;
	private int dkversion;
	private boolean exitOnClose=false;
	private final JMenuBar menubar = new JMenuBar();
	private boolean clean = false;
	private boolean quitting = false;
	private String copyrightText;
	private TaskTracker tt=new TaskTracker();
	
	/** State saved in case the session expires. */
	private byte[] sessionState = null;
	
	/** Adjuster for the amount of milliseconds to wait between pollings of the server. */
	private TimeoutAdjuster firstTimeoutAdjuster;
	private TimeoutAdjuster secondTimeoutAdjuster;
	
	/** Timer to adjust timeouts. */
	private Timer timerAdjusterTimer;
	
	/** A timer to save the state of inactive sessions before they expire in the server. */
	private Timer saveInactiveSessionTimer;
	
	/** 
	 * These maps are cleared each time the player performs an action
	 * that can change the world state.
	 *  */
	private Map<String,float[]> actorTraits = new ConcurrentHashMap<String,float[]>(); 
	private Map<String,float[]> stageTraits = new ConcurrentHashMap<String,float[]>();
	private Map<String,float[]> propTraits = new ConcurrentHashMap<String,float[]>();
	private Map<String,float[][]> relationshipValues = new ConcurrentHashMap<String, float[][]>();
	private String[] knownActors, knownStages, knownProps;
	
	/** Sentences which have been received by each player. */
	private ArrayList<LinkedList<LabeledSentence>> playerSentences = new ArrayList<LinkedList<LabeledSentence>>();
	/** Index of the first sentence shown last time in the left panel. */
	private ArrayList<Integer> playerSentencesOffset = new ArrayList<Integer>();
	/** Active actor. */
	int activeActor = -1;
	/** Actor whose response is being awaited by the engine. */
	int reactingActor = -1;
	
	/** Type of the background info items being displayed. */
	private BIType bit;
	
	
	/** 
	 * This method is used to test the stand alone Storyteller. 
	 * Please do not remove it. The method that starts the applet
	 * is in {@link Applet}. 
	 * */
	static void launch(String args[]) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			UIManager.put("Button.foreground",new ColorUIResource(Color.black));
		} catch (Exception evt) {
		}
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
		      public void uncaughtException(Thread t, Throwable e) {
		    	  e.printStackTrace();
		    	  Utils.showErrorDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), "The application has failed. You have found a bug!", "Uncaught error", e);
		      }
		    });

		Storyteller st=null;
		try {
			st=new Storyteller("Bagatelle.stw",true);
			st.setVisible(true);
			st.startStory("once upon a time");

			final Storyteller fst=st;
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run(){
					if (fst.isClean())
						return;
					try {
						Utils.saveProperties();
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {	
						fst.logout(fst.logDownloader==null);
					} catch(Exception exc){
						exc.printStackTrace();
					}
				}
			});
		} catch (NotBoundException re) {
			re.printStackTrace();
			Utils.showErrorDialog(st, "I'm sorry, but I'm not able to play the storyworld.\nThe server is unavailable.\nStoryteller version: "+SharedConstants.version,"Connection error");
		} catch (BadStoryworldException e) {
			e.printStackTrace();
			Utils.showErrorDialog(st, "I'm sorry, but I cannot start the story.\nThe storyworld has errors!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Storyworld error");
		} catch (Exception re) {
			re.printStackTrace();
			Utils.showErrorDialog(st, "Error when connecting to the server.","Connection error",re);
		}

	}	
	
	/** 
	 * Opens the given storyworld. If exitOnClose is true {@link System#exit(int)}
	 * is called when closing the window.
	 * */
	public Storyteller(String stwID,boolean exitOnClose) throws RemoteException, NotBoundException {
		super("Storyteller");
		
		this.exitOnClose=exitOnClose;
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		storyworldID = stwID;
		chooser = new JFileChooser(Utils.getWorkingDirectory());
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher(){
			private final int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			private Action cutTextAction = new DefaultEditorKit.CutAction();
			private Action copyTextAction = new DefaultEditorKit.CopyAction();
			private Action pasteTextAction = new DefaultEditorKit.PasteAction();
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getID() != KeyEvent.KEY_PRESSED) 
					return false;
				
				java.awt.Window w=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				
				switch(e.getKeyCode()){
				case KeyEvent.VK_X:
					if ((e.getModifiers() & keyMask)!=0 && owner!=null){
						cutTextAction.actionPerformed(getActionEvent(owner,e));
						return true;
					}
					break;
				case KeyEvent.VK_C:
					if ((e.getModifiers() & keyMask)!=0 && owner!=null){
						copyTextAction.actionPerformed(getActionEvent(owner,e));
						return true;
					}
					break;
				case KeyEvent.VK_V:					
					if ((e.getModifiers() & keyMask)!=0 && owner!=null){
						pasteTextAction.actionPerformed(getActionEvent(owner,e));
						return true;
					}
					break;
					// Close the window if ctrl+W o ctrl+Q is pressed  
				case KeyEvent.VK_W:
					if ((e.getModifiers() & keyMask)!=0){
						//if (w!=Storyteller.this && Storyteller.this.isVisible())
						w.dispose();
						if (w==Storyteller.this && Storyteller.this.exitOnClose)
							System.exit(0);
						
						return true;
					}
					break;
				case KeyEvent.VK_Q:
					if ((e.getModifiers() & keyMask)!=0 && !quitting){
						if (saveDialog()) {
							if (Storyteller.this.exitOnClose)
								System.exit(0);	
							else
								Storyteller.this.dispose();
						}
						return true;
					}
					break;
				case KeyEvent.VK_ENTER:
					if (MenuSelectionManager.defaultManager().getSelectedPath().length>0)
						return true;
					break;
				case KeyEvent.VK_ESCAPE:				
					if (w==Storyteller.this) {
						javax.swing.MenuElement[] me = MenuSelectionManager.defaultManager().getSelectedPath();
						MenuSelectionManager.defaultManager().clearSelectedPath();
						return me.length>0;
					} 
					break;
				}
				
				return false;
			}
			ActionEvent getActionEvent(Component owner,KeyEvent e){
				return new ActionEvent(owner,ActionEvent.ACTION_PERFORMED,"",e.getWhen(),e.getModifiers());
			}
		});

		if (exitOnClose)
			try {
				OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[])null));
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		
		if (SharedConstants.useRemoteInterface == false) {
			try {
				janus = new Janus();
				System.out.println("ok, started Janus");
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (SAXException e) {
				throw new RuntimeException(e);
			}
		} else {
			try {
				janus = (StorytellerRemote)
					Naming.lookup(SharedConstants.getRMIServiceString(SharedConstants.getDefaultServiceNum()));
			} catch (MalformedURLException e1) {
				throw new RuntimeException(e1);
			} catch (ConnectException ce) {
				// Try to connect through http tunnelling
				try {
//					RMISocketFactory.setSocketFactory(new sun.rmi.transport.proxy.RMIHttpToCGISocketFactory());
					janus = (StorytellerRemote)
						Naming.lookup(SharedConstants.getRMIServiceString(SharedConstants.getDefaultServiceNum()));
				} catch (Exception e2) {
					throw new RuntimeException(e2);
				}
			}
			
		}

		initWidgets();
	}
//**********************************************************************
	/**
	 * @param logDownloader is the thread that will donwload the logger data, when available.
	 *                      the {@link Swat.LogDownloaderThread#wakeup()} method should be called on this thread to announce
	 *                      availability of data. The log downloader will poll periodically until the method
	 *                      {@link Swat.LogDownloaderThread#gotosleep()} is called again.
	 *                      If logging must not be enabled pass null in this argument. 
	 * */
	public Storyteller(StorytellerRemote str, Deikto tkd, JFileChooser chooser, Swat swat, Swat.LogDownloaderThread logDownloader,String login,String password) {
		super("Storyteller");

		this.swat = swat;
		this.login = login;
		this.password = password;
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		janus = str ;
		dk = tkd;
		storyworldID = dk.getFile().getName();
		this.logDownloader = logDownloader;
		
		if (logDownloader!=null)
			logDownloaderTimer = new Timer(2000,new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (!Storyteller.this.logDownloader.sessionExpired)
						Storyteller.this.logDownloader.getLogData();
				}
			});

		setFrameTitle(storyworldID,dk.version);

		initWidgets();

		this.chooser = chooser;
	}

	private void setFrameTitle(String stwID,int version) {
		setTitle(stwID+" v"+version);
	}
	
	/** Tells if Storyteller has cleanup itself. */
	public boolean isClean(){ return clean; }
	
	/** 
	 * Shows the save dialog if there are pending changes and returns 
	 * true iff we must quit. Otherwise, it never returns (actually quits).
	 * */
	public boolean quit(){
		if (sentenceCounter>lastSavedSentence || 
				sentenceCounter==lastSavedSentence && sentenceAmountOfInput>lastSavedAmountOfInput){
				switch(Utils.showOptionDialog(this,
					"Should I save your story?", "Save?",
					JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,new Object[]{"Save story","Save trace","Discard story"},"Save")) {
					case JOptionPane.YES_OPTION:
						// Save story
						return saveStoryDialog();
					case JOptionPane.NO_OPTION:
						// Save trace
						return saveTraceDialog();
					case JOptionPane.CANCEL_OPTION:
						// Don't save
						return true;
					default:
						return false;
				} 
		} else
			return true;
	}
	
	/** 
	 * Shows confirmation dialog for saving. 
	 * @return false iff the user cancels by closing the dialog or pressing 
	 * the cancel buttons of the file choosers. 
	 * */
	public boolean saveDialog(){
		quitting=true;
		boolean res=quit();
		quitting=false;
		return res;
	}
	
	
	public void logout(boolean wholeSession) {
		if (janus==null)
			return;
		
		try {	
			if (wholeSession)
				janus.logout(sessionID);
			else {
				if (logDownloaderTimer!=null)
					logDownloaderTimer.stop();
				janus.closeStoryteller(sessionID);
			}
		} catch(RemoteException re) {
			re.printStackTrace();
		} 
	}

	/**
	 *  Opens the dialog for saving stories. 
	 * @return false iff the user presses the cancel button. 
	 * */
	private boolean saveStoryDialog() {
		chooser.setFileFilter(Utils.SAVED_STORY_FILE_FILTER);
		chooser.setSelectedFile(lastSavedStory!=null && lastSavedStory.getName().toLowerCase().endsWith(".sst") ?lastSavedStory:new File("savedstory.sst"));
		if (chooser.showSaveDialog(Storyteller.this)==JFileChooser.APPROVE_OPTION) {
			Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			final File f=Utils.addExtension(chooser.getSelectedFile(),".sst");
			boolean res = false;
			saveInactiveSessionTimer.restart();
			try {
				saveStoryBytes(f,janus.getWorldState(sessionID));
				sessionState = null;
				res = true;
			} catch(SessionLogoutException ex){
				if (sessionState!=null) {
					saveStoryBytes(f,sessionState);
					res = true;
				} else
					Utils.showErrorDialog(Storyteller.this, "Your session on the server has expired.","Connection error");
			} catch(NoSuchObjectException ex){
				if (sessionState!=null) {
					saveStoryBytes(f,sessionState);
					res = true;
				} else
					Utils.showErrorDialog(Storyteller.this, "I'm sorry but I cannot save your story.\nThe server has been shut down!","Connection error");
			} catch(RemoteException ex){
				if (sessionState!=null) {
					saveStoryBytes(f,sessionState);
					res = true;
				} else
					Utils.showErrorDialog(Storyteller.this, "A connection error occurred when saving your story.","Connection error",storyworldID,dkversion,ex,getStoryTraceBytes());
			}
			if (res) {
				Utils.addRecentStoryFile(f.getAbsolutePath());
				reloadRecentStoryMenuItems();
			}
			return res;
		} else {
			Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			return false;
		}
	}

	private void saveStoryBytes(File f,byte[] bs) {
		try {
			saveStory(f,bs,null);
		} catch (IOException ex) {
			Utils.showErrorDialog(Storyteller.this, "Could not save story to file \n"+f.getPath(),"File error",ex);
		}
	}
	
	/**
	 *  Opens the dialog for saving story traces. 
	 * @return false iff the user presses the cancel button. 
	 * */
	private boolean saveTraceDialog(){
		chooser.setFileFilter(Utils.STORY_TRACE_FILE_FILTER);
		chooser.setSelectedFile(lastSavedStory!=null && lastSavedStory.getName().toLowerCase().endsWith(".str") ?lastSavedStory:new File("storytrace.str"));
		if (chooser.showSaveDialog(Storyteller.this)==JFileChooser.APPROVE_OPTION) {
			Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			final File f=Utils.addExtension(chooser.getSelectedFile(),".str");
			boolean res=false;
			try {
				saveStory(f,lastLoadedState,recordedInput);
				res=true;
			} catch (IOException ex) {
				Utils.showErrorDialog(Storyteller.this, "Could not save story to file \n"+f.getPath(),"File error",ex);
			}
			Utils.addRecentStoryFile(f.getAbsolutePath());
			reloadRecentStoryMenuItems();
			return res;
		} else { 
			Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			return false;
		}
	}
	
	/** Exception wrapper to use only inside startStory(...). */
	private static final class BadSTWExceptionWrapper extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public BadSTWExceptionWrapper(BadStoryworldException e) {	super(e);	}
	}
	
	/** Starts a new story. */
	public void startStory(final String startingVerbLabel) throws BadStoryworldException {
		try {
			tt.trackRunnable(new TaskTracker.Runnable(){
				public void run(long taskId) {
					try {
						startStory(startingVerbLabel,true,taskId);
					} catch (BadStoryworldException e) {
						throw new BadSTWExceptionWrapper(e);
					}
				}
			}).run();
		} catch (BadSTWExceptionWrapper e) {
			throw (BadStoryworldException)e.getCause();
		}
	}
	private void incompatibleVersionException(IncompatibleVersionException re){
		Utils.showErrorDialog(this, "I'm not compatible with the server version: "+re.serverVersion+".\n"+
				                            "Please, use a newer version of me in order to connect.","Connection error");
	}
	
	/** 
	 * Reloads a session state when it has expired. 
	 * It could fail if there is no available session state to restore.
	 * */
	private boolean reloadExpiredSession(long taskId) {
		if (sessionState==null)
			return false;
		try {
			createSession(true,taskId);
			
			saveInactiveSessionTimer.restart();
			
			if (!janus.startStory(sessionID, startVerbLabel, null, sessionState, logDownloader!=null))
				throw new ServerRefusesToLoadException();
			if (logDownloader!=null)
				logDownloaderTimer.start();
			sessionState = null;

			firstTimeoutAdjuster = new TimeoutAdjuster();
			secondTimeoutAdjuster = new TimeoutAdjuster();

			try {
				// Get the first trigger sentence from the engine.
				if (!mGetServerDataToDisplay(true,taskId).inputEnded())
					// if the input is not complete get sentence data 
					// from the engine, to initialize player selection
					mGetServerDataToDisplay(false,taskId);
				
			} finally {
				firstTimeoutAdjuster.setAdjusting(true);
				secondTimeoutAdjuster.setAdjusting(true);
			}

			if (tt.isCanceled(taskId))
				return false;
			if (logDownloader!=null) {
				logDownloader.getLogData();
				logDownloaderTimer.stop();
			}

			System.out.println("reloading expired session");
		} catch(final Exception e) {
			Runnable r = new Runnable(){
				public void run() {
					Utils.showErrorDialog(Storyteller.this, "There was an error when attempting to recover an expired session.","Connection error",e);
				}
			};
			if (SwingUtilities.isEventDispatchThread())
				r.run();
			else 
				SwingUtilities.invokeLater(r);
		}
		return true;
	}

	private void restartSession(long taskId) {
		try {
			createSession(true,taskId);
		} catch (final Exception e) {
			if (tt.isCanceled(taskId))
				return;
			Runnable r = new Runnable(){
				public void run() {
					Utils.showErrorDialog(Storyteller.this, "There was an error when attempting to restart an expired session.","Connection error",e);
				}
			};
			if (SwingUtilities.isEventDispatchThread())
				r.run();
			else 
				SwingUtilities.invokeLater(r);
		}
		return;
	}

	/** 
	 * Creates a new session if reloadWorld is true. Otherwise just resets the state of
	 * the current session. If the current session has expired you may call this method
	 * to create a new one with reloadWorld being true.
	 * */
	@SuppressWarnings("unchecked")
	private void createSession(boolean reloadWorld,long taskId) throws LimitException, BadStoryworldException, IOException,
									IncompatibleVersionException, RemoteException, SessionLogoutException {
		lastLoadedState = null;
		recordedInput.clear();
		storybookArea.setText("");
		activeActor = -1;

		if (reloadWorld) {
			if (sessionID!=null)
				janus.logout(sessionID);
			if (dk!=null) {
				sessionID = janus.login(SharedConstants.REMOTE_INTERFACE_VERSION,login, password);
				dkversion = dk.version;
				if (SharedConstants.useRemoteInterface)
					janus.loadDeiktoFromZip(sessionID, Utils.compressDeikto(dk));
				else
					((LocalJanus)janus).copyLocalDeikto(dk, sessionID);
				if (tt.isCanceled(taskId))
					return;
				
				relationshipNames = dk.getRelationshipNames();
				actorTraitNames = dk.getVisibleActorTraitNames();
				stageTraitNames = dk.getVisibleStageTraitNames();
				propTraitNames = dk.getVisiblePropTraitNames();
				actorNames = dk.getActorNames();
				stageNames = dk.getStageNames();
				propNames = dk.getPropNames();
			} else {
				ArrayList<Object> sessionStartResult = janus.startStorytellerSession(SharedConstants.REMOTE_INTERFACE_VERSION,"test", storyworldID);
				if (tt.isCanceled(taskId))
					return;
				sessionID = (String)sessionStartResult.get(0);
				dkversion = (Integer)sessionStartResult.get(1);
				relationshipNames = (Pair<String[],String[]>)sessionStartResult.get(2);
				actorTraitNames = (Pair<String[],String[]>)sessionStartResult.get(3);
				stageTraitNames = (Pair<String[],String[]>)sessionStartResult.get(4);
				propTraitNames = (Pair<String[],String[]>)sessionStartResult.get(5);
				actorNames = (String[])sessionStartResult.get(6);
				stageNames = (String[])sessionStartResult.get(7);
				propNames = (String[])sessionStartResult.get(8);
			}
			
			viewMenu.removeAll();
			ButtonGroup bg = new ButtonGroup();
			for(int i=0;i<actorNames.length;i++) {
				JMenuItem mi = new JRadioButtonMenuItem(actorNames[i]);
				final int playerId = i+1;
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setActiveActor(playerId);
					}
				});
				bg.add(mi);
				viewMenu.add(mi);
			}
		}
		initSentenceVectors();

		if (logDownloader!=null) {
			logDownloaderTimer.stop();
			logDownloader.restart(janus, sessionID);
		}
	}
	
	private void startStory(String startingVerbLabel,boolean reloadWorld,long taskId) throws BadStoryworldException {
		startVerbLabel = startingVerbLabel;
		try {
			createSession(reloadWorld,taskId);
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run() { setFrameTitle(storyworldID,dkversion);	}
			});
			if (!janus.startStory(sessionID, startVerbLabel, null, null, logDownloader!=null)) {
				if (tt.isCanceled(taskId))
					return;
				Utils.showErrorDialog(this, "The server refuses to run this storyworld.","Connection error");
				return;
			}
			if (tt.isCanceled(taskId))
				return;
			if (logDownloader!=null) 
				logDownloaderTimer.start();

			sessionState = null;
			sentenceCounter=0;
			sentenceAmountOfInput=0;
			lastSavedSentence=0;
			lastSavedAmountOfInput=0;
			actorTraits.clear();
			stageTraits.clear();
			propTraits.clear();
			
			firstTimeoutAdjuster = new TimeoutAdjuster();
			secondTimeoutAdjuster = new TimeoutAdjuster();
			saveInactiveSessionTimer.restart();
			sessionState = null;

			try {
				// Get the first trigger sentence from the engine.
				if (!getServerDataToDisplay(null,true,taskId))
					// if the input is not complete get sentence data 
					// from the engine, to initialize player selection
					while(!getServerDataToDisplay(taskId));
			} finally {
				firstTimeoutAdjuster.setAdjusting(true);
				secondTimeoutAdjuster.setAdjusting(true);
			}

		} catch(final IncompatibleVersionException re) {	
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					incompatibleVersionException(re);
				}
			});
			return;
		} catch(SessionLogoutException e){
			if (tt.isCanceled(taskId))
				return;
			if (!reloadWorld)
				startStory(startingVerbLabel,true,taskId);
			else {
				SwingUtilities.invokeLater(new Runnable(){
					public void run() {
						if (logDownloader!=null)
							logDownloaderTimer.stop();
						Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot start the story.\nYour session on the server has expired.","Connection error");
					}
				});
			}
			return;
		} catch(final EngineDiedException e) {
			if (tt.isCanceled(taskId))
				return;
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					storytellerMenu.setEnabled(true);
					if (logDownloader!=null)
						logDownloaderTimer.stop();
					Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot start the story.\nThe engine has failed.","Engine error",storyworldID,dkversion,e.getCause(),getStoryTraceBytes());
				}
			});
		} catch (NoSuchObjectException re) {
			if (tt.isCanceled(taskId))
				return;
			re.printStackTrace();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					storytellerMenu.setEnabled(true);
					if (logDownloader!=null)
						logDownloaderTimer.stop();
					Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot start the story.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
				}
			});
		} catch(final LimitException e) {
			if (tt.isCanceled(taskId))
				return;
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					storytellerMenu.setEnabled(true);
					if (logDownloader!=null)
						logDownloaderTimer.stop();
					Utils.displayLimitExceptionMessage(e, "Editing error", "I'm sorry, but I cannot start the story.\nThere was an error when loading the storyworld.");
				}
			});
		} catch (final RemoteException re) {
			if (tt.isCanceled(taskId))
				return;
			re.printStackTrace();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					storytellerMenu.setEnabled(true);
					if (logDownloader!=null)
						logDownloaderTimer.stop();
					Utils.showErrorDialog(Storyteller.this, "A connection error occurred when restarting the story.","Connection error",storyworldID,dkversion,re,null);
				}
			});
		} catch (final IOException e) {
			if (tt.isCanceled(taskId))
				return;
			e.printStackTrace();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					storytellerMenu.setEnabled(true);
					if (logDownloader!=null)
						logDownloaderTimer.stop();
					Utils.showErrorDialog(Storyteller.this, "Error while preparing to send the storyworld.","Storyteller error",storyworldID,dkversion,e,null);
				}
			});
		}

		if (logDownloader!=null) {
			logDownloader.getLogData();
			logDownloaderTimer.stop();
		}
		
		return;
	}
	
	private Timer logDownloaderTimer;
	private ProgressDialog progressDialog;
	
	/** Save the story to a given file */
	private void saveStory(File f,byte[] state,LinkedList<Integer> recordedInput) 
			throws IOException {
		OutputStream os = new FileOutputStream(f);
		SavedStory ss = createSavedStory(state,recordedInput);
		try {
			ss.writeFormatHeader(os);
			DataOutputStream dos = SavedStory.convertOutputStreamToDataOutput(os);
			try {
				ss.write(dos);
			} finally {
				dos.close();
				os = null;
			}
			lastSavedStory=f;
			lastSavedSentence = sentenceCounter;
			lastSavedAmountOfInput = sentenceAmountOfInput;
		} finally {
			if (os!=null)
				os.close();
		}
	}

	/** Writes a saved story to a string. */
	private byte[] getStoryTraceBytes(){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		SavedStory ss = createSavedStory(lastLoadedState,recordedInput);
		try {
			ss.writeFormatHeader(bos);
			DataOutputStream os = SavedStory.convertOutputStreamToDataOutput(bos);
			try {
				ss.write(os);
			} finally {
				os.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bos.toByteArray(); 
	}
	
	/** Creates a saved story for the current state of storyteller. */
	private SavedStory createSavedStory(byte[] state,LinkedList<Integer> recordedInput) { 
		return new SavedStory(storyworldID,
					dkversion,
					sentenceAmountOfInput,
					inputSentence.getHotWordSockets(),
					startVerbLabel,
					state,
					recordedInput
				);
	}

	/** Loads a story from a file. */
	private void loadStory(final File f) {
		cleanupTheEnd();
		removePoisoningsButton();
		try {
			InputStream is = new FileInputStream(f);
			final SavedStory ss = new SavedStory();
			try {
				ss.readFormatHeader(is);
			} catch (SavedStory.InvalidFormatException ex) {
				Utils.showErrorDialog(Storyteller.this,
						"The given file is not a saved story file \n"+f.getPath(), "Loading");
				is.close();
				return;
			}
			
			final DataInputStream br = SavedStory.convertInputStreamToDataInput(is);
			ss.readHeader(br);
			if (!checkSavedStoryVersion(Storyteller.this, ss, storyworldID, dkversion, f.getName())){
				br.close();
				return;
			}
				
			Utils.setCursor(Storyteller.this,Cursor.WAIT_CURSOR);
			SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
				public void run(long taskId) {
					tt.execute(new TaskTracker.Runnable(){
						public void run(long taskId) {
							myRun(true,taskId);
						}
						
						public void myRun(boolean retry,long taskId) {
							try {
								loadStory(br,ss,"Loading "+f.getName()+" ...",taskId);
								if (tt.isCanceled(taskId))
									return;

								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
										Utils.addRecentStoryFile(f.getAbsolutePath());
										lastSavedStory=f;
										reloadRecentStoryMenuItems();
									};
								});
							} catch(SessionLogoutException e){
								if (tt.isCanceled(taskId))
									return;
								if (retry) {
									restartSession(taskId);
									myRun(false,taskId);
								} else {
									SwingUtilities.invokeLater(new Runnable(){
										public void run() {
											if (logDownloader!=null)
												logDownloaderTimer.stop();
											if (progressDialog!=null) {
												progressDialog.dispose();
												progressDialog = null;
											}
											storytellerMenu.setEnabled(true);
											Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
											Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot load the story.\nYour session on the server has expired.","Connection error");
										}
								});
								}
							} catch(final EngineDiedException e) {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null)
											logDownloaderTimer.stop();
										if (progressDialog!=null) {
											progressDialog.dispose();
											progressDialog = null;
										}
										storytellerMenu.setEnabled(true);
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot load the story.\nThe engine has failed.","Engine error",storyworldID,dkversion,e.getCause(),getStoryTraceBytes());
									}
								});
							} catch (NoSuchObjectException re) {
								if (tt.isCanceled(taskId))
									return;
								re.printStackTrace();
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null)
											logDownloaderTimer.stop();
										if (progressDialog!=null) {
											progressDialog.dispose();
											progressDialog = null;
										}
										storytellerMenu.setEnabled(true);
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot load the story.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
									}
								});
							} catch(final RemoteException e) {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null)
											logDownloaderTimer.stop();
										if (progressDialog!=null) {
											progressDialog.dispose();
											progressDialog = null;
										}
										storytellerMenu.setEnabled(true);
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot load the story.","Server error",storyworldID,dkversion,e,null);
									}
								});
							} catch(ServerRefusesToLoadException e) {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null)
											logDownloaderTimer.stop();
										if (progressDialog!=null) {
											progressDialog.dispose();
											progressDialog = null;
										}
										storytellerMenu.setEnabled(true);
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
										Utils.showErrorDialog(Storyteller.this, "The server refuses to load the given state.\n"+
												"This is probably because of an incompatibility\n"+
												"with the current storyworld.","Connection error");
									}
								});
							} catch(final IOException e) {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null)
											logDownloaderTimer.stop();
										if (progressDialog!=null) {
											progressDialog.dispose();
											progressDialog = null;
										}
										storytellerMenu.setEnabled(true);
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
										Utils.showErrorDialog(Storyteller.this, "Could not read the saved story file.","Reading error",e);
									}
								});
							} finally {
								try {
									br.close();
								} catch (IOException ex) {
									throw new RuntimeException(ex);
								}
							}
						}
					});
				}
			}));
			
		} catch (IOException ex) {
			ex.printStackTrace();
			if (!f.exists()) {
				Utils.deleteRecentStoryFile(f.getAbsolutePath());
				reloadRecentStoryMenuItems();
				Utils.showErrorDialog(Storyteller.this, "Could not load story from file \n"+f.getPath()+"\nThe file does not exist.","File error");
			} else
				Utils.showErrorDialog(Storyteller.this, "Could not load story from file \n"+f.getPath(),"File error",ex);
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
			Utils.showErrorDialog(Storyteller.this, "The file "+f.getPath()+"\nis not in the correct format.","File error",ex);
		}
	}
	
	public static boolean checkSavedStoryVersion(Component parent,SavedStory ss,String storyworldID,int dkversion,String fileName){
		if (ss.storyworldID.length()>0 && !ss.storyworldID.equals(storyworldID))
			switch(Utils.showOptionDialog(parent,
					"The story file \""+fileName+"\" seems to belong to a storyworld \n("+ss.storyworldID+") distinct from the current one ("+storyworldID+").\nLoading it could fail.\nDo you want to load it anyway?", "Load?",
					JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null,new Object[]{"Load","Don't load"},"Don't load")) {
					case JOptionPane.YES_OPTION:
						break;
					default:
						return false;
				}
		if (ss.dkversion<dkversion)
			switch(Utils.showOptionDialog(parent,
					"The story file \""+fileName+"\" seems to belong to an older version\nof "+storyworldID+".\nLoading it could fail. Do you want to load it anyway?", "Load?",
					JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null,new Object[]{"Load","Don't load"},"Don't load")) {
					case JOptionPane.YES_OPTION:
						break;
					default:
						return false;
				}
		else if (ss.dkversion>dkversion)
			switch(Utils.showOptionDialog(parent,
					"The story file \""+fileName+"\" seems to belong to a newer version\nof "+storyworldID+".\nLoading it could fail. Do you want to load it anyway?", "Load?",
					JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE,null,new Object[]{"Load","Don't load"},"Don't load")) {
					case JOptionPane.YES_OPTION:
						break;
					default:
						return false;
				}
		
		return true;
	}
	
	/** Loads a story. */
	private void loadStory(DataInput br,SavedStory ss,String statusMsg,long taskId) 
				throws SessionLogoutException, EngineDiedException, 
						RemoteException, ServerRefusesToLoadException, IOException {

		boolean excThrown = true;
		byte[] oldLastLoadedState=lastLoadedState;
		String oldVerbLabel=startVerbLabel;
		
		ss.readBody(br);
		recordedInput = ss.recordedInput;
		lastLoadedState = ss.state;
		startVerbLabel = ss.startingVerbLabel;

		try {
			if (!recordedInput.isEmpty() && dk==null) {
				Utils.showErrorDialog(Storyteller.this, "That is a story trace file.\nI can not load that kind of file because it only makes\nsense to use in a storyteller started from Swat.","File error");
				lastLoadedState=oldLastLoadedState;
				startVerbLabel=oldVerbLabel;
				return;
			}
			
			saveInactiveSessionTimer.restart();
			storytellerMenu.setEnabled(false);
			clearLeftPanels();
			clearRightPanels();
			
			validate();
			repaint();
				
			progressDialog = new ProgressDialog(Storyteller.this);
			progressDialog.setStatus(statusMsg);
			progressDialog.setVisible(true);
			
			if (logDownloader!=null) {
				logDownloaderTimer.stop();
				logDownloader.restart(janus, sessionID);
			}

			if (!janus.startStory(sessionID, "once upon a time", Utils.toArray(recordedInput), lastLoadedState, logDownloader!=null))
				throw new ServerRefusesToLoadException();
			excThrown = false;
		} finally {
			if (tt.isCanceled(taskId))
				return;
			if (excThrown) {
				lastLoadedState=oldLastLoadedState;
				startVerbLabel=oldVerbLabel;
			} else {
				sentenceCounter=0;
				lastSavedSentence=0;
				lastSavedAmountOfInput=sentenceAmountOfInput;
			}
		}
		
		if (logDownloader!=null)
			logDownloaderTimer.start();
		sessionState = null;

		storybookArea.setText("");
		initSentenceVectors();
		activeActor = -1;

		firstTimeoutAdjuster = new TimeoutAdjuster();
		secondTimeoutAdjuster = new TimeoutAdjuster();
		
		try {
			// Get the first trigger sentence from the engine.
			if (!getServerDataToDisplay(ss.hotWordSockets,true,taskId))
				// if the input is not complete get sentence data 
				// from the engine, to initialize player selection
				while (!getServerDataToDisplay(ss.hotWordSockets,taskId));
			
		} finally {
			if (tt.isCanceled(taskId))
				return;
			firstTimeoutAdjuster.setAdjusting(true);
			secondTimeoutAdjuster.setAdjusting(true);
		}
		
		if (progressDialog!=null) {
			progressDialog.dispose();
			progressDialog = null;
		}
		
		if (logDownloader!=null) {
			logDownloader.getLogData();
			logDownloaderTimer.stop();
		}
	}
	
	private void initWidgets() {
		ToolTipManager.sharedInstance().setDismissDelay(3600000);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {	
				if (saveDialog()) {
					if (exitOnClose)
						System.exit(0);
					else
						dispose();
				}
			}
		});

		timerAdjusterTimer = new Timer(600000,new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (firstTimeoutAdjuster!=null)
					firstTimeoutAdjuster.setAllowDecreasing(true);
				if (secondTimeoutAdjuster!=null)
					secondTimeoutAdjuster.setAllowDecreasing(true);
			}
		});
		
		saveInactiveSessionTimer = new Timer(SAVE_INACTIVE_SESSION_TIMEOUT,new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run(){
						try {
							sessionState = janus.getWorldState(sessionID);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}.start();
			}
		});
		saveInactiveSessionTimer.setRepeats(false);
		
		storybookArea.setEditable(false);
		storybookArea.setBackground(Utils.STORYTELLER_LEFT_COLOR);

		leftTopPanel = Box.createHorizontalBox();
		leftTopPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		leftTopPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
		leftTopPanel.setMinimumSize(new Dimension(50,40));
		leftTopPanel.setPreferredSize(new Dimension(50,40));
		leftTopPanel.add(Box.createHorizontalGlue());
		leftTopPanel.add(Box.createHorizontalGlue());
		
		leftPanel = new SentencesPanel(null);
		leftPanel.setLayout(new BoxLayout(leftPanel,BoxLayout.Y_AXIS));
		leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		leftPanel.setOpaque(false);
		leftPanel.add(Box.createVerticalGlue());
		
		leftSP = new JScrollPane(leftPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		leftSP.setOpaque(false);
		leftSP.getViewport().setOpaque(false);
		leftSP.setBorder(BorderFactory.createEmptyBorder());
		leftSP.getVerticalScrollBar().setUnitIncrement(20);
		leftSP.getVerticalScrollBar().setBlockIncrement(40);
		
		JComponent leftHeaderFillPanel = new JPanel(null);
		leftHeaderFillPanel.setLayout(new BoxLayout(leftHeaderFillPanel,BoxLayout.X_AXIS));
		leftHeaderFillPanel.setBackground(Utils.STORYTELLER_LEFT_HEADER_COLOR);
		leftHeaderFillPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(5,0,0,0,Utils.STORYTELLER_BACKGROUND),
				BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0,0,8,0,Utils.STORYTELLER_LEFT_COLOR),
						BorderFactory.createCompoundBorder(
								BorderFactory.createMatteBorder(0,0,1,0,Utils.STORYTELLER_LEFT_HEADER_BORDER_COLOR),
								BorderFactory.createMatteBorder(0,0,2,0,Utils.STORYTELLER_LEFT_COLOR)
						)
				)));
		leftHeaderFillPanel.add(Box.createVerticalGlue());
		leftHeaderFillPanel.add(Box.createHorizontalGlue());
		
		JComponent leftHeaderPanel = new JPanel(new BorderLayout());
		leftHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,41));
		leftHeaderPanel.setPreferredSize(new Dimension(10,41));
		leftHeaderPanel.add(leftHeaderFillPanel,BorderLayout.CENTER);
		
		// Left header image
		try {
			leftHeaderPanel.add(new JLabel(new ImageIcon(Utils.getImagePath("left_st_header.png"))),BorderLayout.WEST);
		} catch (NullPointerException npe) { 
			System.out.println("Could not find left header image" );
			leftHeaderPanel.add(new JLabel("What Happens"),BorderLayout.WEST);
		}
		
		leftOuterPanel = new JPanel(null);
		leftOuterPanel.setLayout(new BoxLayout(leftOuterPanel,BoxLayout.Y_AXIS));
		leftOuterPanel.setBorder(BorderFactory.createMatteBorder(0,3,3,3,Utils.STORYTELLER_BACKGROUND));
		leftOuterPanel.setBackground(Utils.STORYTELLER_LEFT_COLOR);
		leftOuterPanel.add(leftHeaderPanel);
		leftOuterPanel.add(leftTopPanel);
		leftOuterPanel.add(leftSP);

		actorNameLabel = new JLabel("(Actor)");

		JComponent rightHeaderFillPanel = new JPanel(null);
		rightHeaderFillPanel.setLayout(new BoxLayout(rightHeaderFillPanel,BoxLayout.X_AXIS));
		rightHeaderFillPanel.setBackground(Utils.STORYTELLER_RIGHT_HEADER_COLOR);
		rightHeaderFillPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(5,0,0,0,Utils.STORYTELLER_BACKGROUND),
				BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0,0,8,0,Utils.STORYTELLER_RIGHT_COLOR),
						BorderFactory.createCompoundBorder(
								BorderFactory.createMatteBorder(0,0,1,0,Utils.STORYTELLER_RIGHT_HEADER_BORDER_COLOR),
								BorderFactory.createMatteBorder(0,0,2,0,Utils.STORYTELLER_RIGHT_COLOR)
						)
				)));
		rightHeaderFillPanel.add(Box.createRigidArea(new Dimension(10,10)));
		rightHeaderFillPanel.add(Box.createVerticalGlue());
		rightHeaderFillPanel.add(actorNameLabel);
		rightHeaderFillPanel.add(Box.createHorizontalGlue());
		
		JComponent rightHeaderPanel = new JPanel(new BorderLayout());
		rightHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,41));
		rightHeaderPanel.setPreferredSize(new Dimension(10,41));
		rightHeaderPanel.add(rightHeaderFillPanel,BorderLayout.CENTER);
		
		// Right header image
		try {
			rightHeaderPanel.add(new JLabel(new ImageIcon(Utils.getImagePath("right_st_header.png"))),BorderLayout.WEST);
		} catch (NullPointerException npe) { 
			System.out.println("Could not find right header image" );
			rightHeaderPanel.add(new JLabel("You Decide"),BorderLayout.WEST);
		}
		
		rightTopPanel = Box.createHorizontalBox();
		rightTopPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		rightTopPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,TOP_PANELS_HEIGHT));
		rightTopPanel.setMinimumSize(new Dimension(50,TOP_PANELS_HEIGHT));
		rightTopPanel.setPreferredSize(new Dimension(50,TOP_PANELS_HEIGHT));
		
		poisonPanel = Box.createHorizontalBox();
		poisonPanel.setBorder(BorderFactory.createEmptyBorder(0,15,0,15));
		
		JComponent auxRightTopPanel = Box.createVerticalBox();
		poisonPanel.setAlignmentX(0.0f);
		auxRightTopPanel.add(poisonPanel);
		rightTopPanel.setAlignmentX(0.0f);
		auxRightTopPanel.add(rightTopPanel);
		
		rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		rightPanel.add(auxRightTopPanel,BorderLayout.NORTH);
		rightPanel.setFocusCycleRoot(true);

		JPanel rightOuterPanel = new JPanel(new BorderLayout());
		rightOuterPanel.setOpaque(false);
		rightOuterPanel.setBorder(BorderFactory.createMatteBorder(0,3,3,3,Utils.STORYTELLER_BACKGROUND));
		rightOuterPanel.add(rightHeaderPanel,BorderLayout.NORTH);
		rightOuterPanel.add(rightPanel,BorderLayout.CENTER);
		
		topPanel = new JPanel(new GridLayout(1,0));
		topPanel.setOpaque(false);
		topPanel.add(leftOuterPanel);
		topPanel.add(rightOuterPanel);
		
		
		doneAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//System.out.println("Done Button Pressed");
				recordSentenceOffset(activeActor);

				if (logDownloaderTimer!=null)
					logDownloaderTimer.start();
				inputSentence.setEnabled(false);
				if (donebt!=null)
					donebt.setEnabled(false);
				saveInactiveSessionTimer.restart();
				actorTraits.clear();
				stageTraits.clear();
				propTraits.clear();
				knownActors=null;
				knownStages=null;
				knownProps=null;
				freshSentence=true;
				relationshipValues.clear();
				
				// Start a new thread to wait for the response to arrive.
				tt.execute(new TaskTracker.Runnable(){
					public void run(long taskId){
						myRun(true,taskId);
					}
					
					public void myRun(boolean retry,long taskId){
						try {
							// result of 1000 indicates the "done" button was pressed.
							setResult(1000,true,taskId);
							if (tt.isCanceled(taskId))
								return;
							sentenceAmountOfInput=0;
							sentenceCounter++;
							if (print2stdout)
								System.out.println("called setResult()");

							if (logDownloader!=null) {
								logDownloader.getLogData();
								logDownloaderTimer.stop();
							}
							sessionState = null;
						} catch(SessionLogoutException e) {
							if (tt.isCanceled(taskId))
								return;
							if (retry && reloadExpiredSession(taskId)) {
								if (tt.isCanceled(taskId))
									return;
								myRun(false,taskId);
							} else {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null)
											logDownloaderTimer.stop();
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nYour session on the server has expired.","Connection error");
									}
								});
							}
						} catch(final EngineDiedException e) {
							if (tt.isCanceled(taskId))
								return;
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null)
										logDownloaderTimer.stop();
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe engine has failed.","Engine error",storyworldID,dkversion,e.getCause(),getStoryTraceBytes());
								}
							});
						} catch (NoSuchObjectException re) {
							if (tt.isCanceled(taskId))
								return;
							re.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null)
										logDownloaderTimer.stop();
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
								}
							});
						} catch(final RemoteException e){
							if (tt.isCanceled(taskId))
								return;
							e.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null)
										logDownloaderTimer.stop();
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nA connection error occurred when sending your sentence to the server.","Connection error",storyworldID,dkversion,e,null);
								}
							});
						}
					}
				});
			}
		};

		inputSentenceListener = new SentenceDisplay.HotButtonListener() {
			public void actionPerformed(ActionEvent e,final int fromLast) {
				if (logDownloaderTimer!=null)
					logDownloaderTimer.start();
				inputSentence.setEnabled(false);
				saveInactiveSessionTimer.restart();
				if (donebt!=null)
					donebt.setEnabled(false);
				tt.execute(new TaskTracker.Runnable(){
					public void run(long taskId){
						myRun(true,taskId);
					}
					
					public void myRun(boolean retry,long taskId){
						try {
							setResult(-fromLast-1,false,taskId);
							if (tt.isCanceled(taskId))
								return;
							if (logDownloader!=null) {
								logDownloader.getLogData();
								logDownloaderTimer.stop();
							}
							sessionState = null;
						} catch(SessionLogoutException e) {
							if (tt.isCanceled(taskId))
								return;
							if (retry && reloadExpiredSession(taskId)) {
								if (tt.isCanceled(taskId))
									return;
								myRun(false,taskId);
							} else {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null)
											logDownloaderTimer.stop();
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nYour session on the server has expired.","Connection error");
									}
								});
							}
						} catch(final EngineDiedException e) {
							if (tt.isCanceled(taskId))
								return;
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null)
										logDownloaderTimer.stop();
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe engine has failed.","Engine error",storyworldID,dkversion,e.getCause(),getStoryTraceBytes());
								}
							});
						} catch (NoSuchObjectException re) {
							if (tt.isCanceled(taskId))
								return;
							re.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null)
										logDownloaderTimer.stop();
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
								}
							});
						} catch(final RemoteException e){
							if (tt.isCanceled(taskId))
								return;
							e.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null)
										logDownloaderTimer.stop();
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nA connection error occurred when undoing your choice.","Connection error",storyworldID,dkversion,e,null);
								}
							});
						}
					}
				});
			}
			
			public void menuActionPerformed(ActionEvent e, final int i) {
				
				if (logDownloaderTimer!=null)
					logDownloaderTimer.start();
				inputSentence.setEnabled(false);
				saveInactiveSessionTimer.restart();
				tt.execute(new TaskTracker.Runnable(){
					public void run(long taskId){
						myRun(true,taskId);	
					}
					
					public void myRun(boolean retry,long taskId){
						try {
							setResult(i,false,taskId);
							if (tt.isCanceled(taskId))
								return;
							sessionState = null;
						} catch(SessionLogoutException e) {
							if (tt.isCanceled(taskId))
								return;
							if (retry && reloadExpiredSession(taskId)) {
								if (tt.isCanceled(taskId))
									return;
								myRun(false,taskId);
							} else {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										if (logDownloader!=null) {
											logDownloader.getLogData();
											logDownloaderTimer.stop();
										}
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nYour session on the server has expired.","Connection error");
									}
								});
							}
						} catch(final EngineDiedException e) {
							if (tt.isCanceled(taskId))
								return;
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null) {
										logDownloader.getLogData();
										logDownloaderTimer.stop();
									}
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe engine has failed.","Engine error",storyworldID,dkversion,e.getCause(),getStoryTraceBytes());
								}
							});
						} catch (NoSuchObjectException re) {
							if (tt.isCanceled(taskId))
								return;
							re.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null) {
										logDownloader.getLogData();
										logDownloaderTimer.stop();
									}
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
								}
							});
						} catch(final RemoteException e){
							if (tt.isCanceled(taskId))
								return;
							e.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									if (logDownloader!=null) {
										logDownloader.getLogData();
										logDownloaderTimer.stop();
									}
									storytellerMenu.setEnabled(true);
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nA connection error occurred when sending your choice to the server.","Connection error",storyworldID,dkversion,e,null);
								}
							});
						}
						if (logDownloader!=null) {
							logDownloader.getLogData();
							logDownloaderTimer.stop();
						}
					}
				});
			}
		};
		
		JMenuItem storybookButton = new JMenuItem("Storybook");
		storybookButton.setToolTipText("View the storybook");
		storybookButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Utils.setCursor(Storyteller.this, Cursor.WAIT_CURSOR);
				tt.execute(new TaskTracker.Runnable(){
					public void run(long taskId) {
						myRun(true,taskId);
					}
					
					public void myRun(boolean retry,long taskId) {
						try {
							//String[] ss=getStorybook();
							getStorybook();
							//if (tt.isCanceled(taskId))
							//	return;
							//for(String s:ss){
							//	storybookArea.append(s);
							//	storybookArea.append("\n");
							//}
							SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
								public void run(long taskId) {
									if (tt.isCanceled(taskId))
										return;
									Utils.setCursor(Storyteller.this, Cursor.DEFAULT_CURSOR);
									if (storybookFrame == null) {
										storybookFrame = new JDialog(Storyteller.this,"Storybook");
										storybookFrame.add(new JScrollPane(storybookArea));
										storybookFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
										storybookFrame.setSize(500, 300);
									}
									storybookFrame.setVisible(true);
								}
							}));
						} catch(SessionLogoutException e) {
							if (tt.isCanceled(taskId))
								return;
							if (retry && reloadExpiredSession(taskId)) {
								if (tt.isCanceled(taskId))
									return;
								myRun(false,taskId);
							} else {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you the storybook.\nYour session on the server has expired.","Connection error");
									}
								});
							}
						} catch (NoSuchObjectException re) {
							if (tt.isCanceled(taskId))
								return;
							re.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you the storybook.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
								}
							});
						} catch(final RemoteException e){
							if (tt.isCanceled(taskId))
								return;
							e.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you the storybook.\nA connection error occurred when fetching storybook entries.","Connection error",storyworldID,dkversion,e,null);
								}
							});
						}
					}
				});
			}
		});
		
		JMenuItem peopleMenuItem = new JMenuItem("People");
		peopleMenuItem.setVisible(true);
		peopleMenuItem.setToolTipText("View the people in this Storyworld");
		peopleMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				showBackgroundInformation(BIType.Actor);
			}
		});
		peopleMenuItem.setEnabled(true);

		
		JMenuItem placesMenuItem = new JMenuItem("Places");
		placesMenuItem.setVisible(true);
		placesMenuItem.setToolTipText("View the places in this Storyworld");
		placesMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showBackgroundInformation(BIType.Stage);
			}
		});
		placesMenuItem.setEnabled(true);


		JMenuItem thingsMenuItem = new JMenuItem("Things");
		thingsMenuItem.setVisible(true);
		thingsMenuItem.setToolTipText("View the things in this Storyworld");
		thingsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showBackgroundInformation(BIType.Prop);
			}
		});
		thingsMenuItem.setEnabled(true);

		JMenuItem relationshipsMenuItem = new JMenuItem("Relationships");
		relationshipsMenuItem.setToolTipText("View the actor relationships.");
		relationshipsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (relationshipNames!=null && relationshipNames.first.length>0)
					showRelationships();
				else {
					JOptionPane.showOptionDialog(Storyteller.this, "There are no relationships to show in this storyworld.","Relationships",
												JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,new Object[]{"Close"},null);
				}
			}
		});
		
		FlowScrollLayout flowlayout=new FlowScrollLayout();
		flowlayout.setAlignment(FlowLayout.LEFT);
		flowlayout.setHgap(10);
		bottomPanel=new JPanel(flowlayout);
		bottomPanel.setBackground(Color.white);
		
		bottomScrollPanel = new JScrollPane(bottomPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		flowlayout.setScrollPane(bottomScrollPanel);
		flowlayout.setUniformHeight(true);
		bottomScrollPanel.setBorder(new TitledBorder("The situation"));
		bottomScrollPanel.setBackground(Color.white);
		bottomScrollPanel.getViewport().setBackground(Color.white);
		bottomScrollPanel.setMinimumSize(new Dimension(10, 10));
		bottomScrollPanel.setPreferredSize(new Dimension(10, 165));
		bottomScrollPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));
		
		mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.setResizeWeight(1.0);
		mainPanel.add(topPanel);
		setContentPane(mainPanel);
		
		JMenuItem restartMenuItem = new JMenuItem("Restart story");
		restartMenuItem.setToolTipText("<html>Starts the story from the beginning<br>all over again.</html>");
		restartMenuItem.setMnemonic(KeyEvent.VK_R);
		restartMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Utils.setCursor(Storyteller.this,Cursor.WAIT_CURSOR);
				new Thread(){
					@Override
					public void run() {
						tt.cancelTasks(5000);
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								removePoisoningsButton();
								cleanupTheEnd();
							};
						});
						tt.trackRunnable(new TaskTracker.Runnable(){
							public void run(long taskId) {
								try {
									startStory(startVerbLabel,false,taskId);
								} catch (BadStoryworldException e){
									throw new RuntimeException(e);
								}
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
									};
								});
							}
						}).run();
					}
				}.start();
			}
		});
		
		JMenuItem loadMenuItem = new JMenuItem("Load story...");
		loadMenuItem.setToolTipText("<html>Resumes a story from a previously<br>saved point.</html>");
		loadMenuItem.setMnemonic(KeyEvent.VK_L);
		loadMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				chooser.setFileFilter(swat==null?Utils.SAVED_STORY_FILE_FILTER:Utils.SAVED_STORY_TRACE_FILE_FILTER);
				chooser.setSelectedFile(new File(""));
				if (chooser.showOpenDialog(Storyteller.this)==JFileChooser.APPROVE_OPTION)
					loadStory(chooser.getSelectedFile());
				Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			}
		});

		JMenuItem saveMenuItem = new JMenuItem("Save story...");
		saveMenuItem.setToolTipText("<html>Saves your current point in the story<br>so you can resume from it, later.</html>");
		saveMenuItem.setMnemonic(KeyEvent.VK_S);
		saveMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) { saveStoryDialog();	}
		});
		
		JMenuItem saveTraceMenuItem = new JMenuItem("Save story trace...");
		saveTraceMenuItem.setToolTipText("<html>Saves a trace that allows to reproduce<br>your steps in a storyteller started<br>from Swat.</html>");
		saveTraceMenuItem.setMnemonic(KeyEvent.VK_S);
		saveTraceMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) { saveTraceDialog(); }
		});

		JMenuItem aboutStorytellerMenuItem = new JMenuItem("About Storyteller...");
		aboutStorytellerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String versionLabel = "Storyteller version " + String.format("%04d", SharedConstants.version);
		
				JOptionPane.showMessageDialog(Storyteller.this.getContentPane(),
						versionLabel + "\nCopyright \u00A9 2009 Storytron", "About Storyteller",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		JMenuItem aboutStoryworldMenuItem = new JMenuItem("About "+storyworldID);
		aboutStoryworldMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
					public void run(long taskId) {
						myRun(true,taskId);
					}
					public void myRun(boolean retry,long taskId) {
						Utils.setCursor(Storyteller.this,Cursor.WAIT_CURSOR);
						try {
							String s=getCopyright();
							if (tt.isCanceled(taskId))
								return;
							copyrightDisplay.setText(s);
							copyrightDisplay.setVisible(true);
						} catch(SessionLogoutException e) {
							if (tt.isCanceled(taskId))
								return;
							if (retry && reloadExpiredSession(taskId)) {
								if (tt.isCanceled(taskId))
									return;
								myRun(false,taskId);
							} else {
								if (tt.isCanceled(taskId))
									return;
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you the copyright information.\nYour session on the server has expired.","Connection error");
									}
								});
							}
						} catch (NoSuchObjectException re) {
							if (tt.isCanceled(taskId))
								return;
							re.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you the copyright information.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
								}
							});
						} catch(final RemoteException e){
							if (tt.isCanceled(taskId))
								return;
							e.printStackTrace();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {
									Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you the copyright information.\nA connection error occurred when fetching the copyright text.","Connection error",storyworldID,dkversion,e,null);
								}
							});
						}finally {
							Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
						}
					}
				}));
			}
		});

		JMenuItem closeMenuItem = new JMenuItem("Quit");
		closeMenuItem.setMnemonic(KeyEvent.VK_Q);
		closeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (saveDialog()) {
					if (Storyteller.this.exitOnClose)
						System.exit(0);
					else
						Storyteller.this.dispose();
				}
			}
		});

		storytellerMenu.setEnabled(false);
		storytellerMenu.setMnemonic(KeyEvent.VK_S);
		storytellerMenu.add(saveMenuItem);
		storytellerMenu.add(loadMenuItem);
		storytellerMenu.add(restartMenuItem);
		storytellerMenu.add(loadMenuItem);
		storytellerMenu.add(saveTraceMenuItem);
		storytellerMenu.addSeparator();
		storytellerMenu.addSeparator();
		storytellerMenu.add(aboutStorytellerMenuItem);
		storytellerMenu.addSeparator();
		storytellerMenu.add(closeMenuItem);

		JMenu storyworldMenu = new JMenu("Storyworld");
		storyworldMenu.setMnemonic(KeyEvent.VK_W);
		storyworldMenu.add(aboutStoryworldMenuItem);
		storyworldMenu.addSeparator();
		storyworldMenu.add(peopleMenuItem);
		storyworldMenu.add(placesMenuItem);
		storyworldMenu.add(thingsMenuItem);
		storyworldMenu.add(relationshipsMenuItem);

		storyworldMenu.addSeparator();
		storyworldMenu.add(storybookButton);

		viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		
		menubar.add(storytellerMenu);
		menubar.add(storyworldMenu);
		menubar.add(viewMenu);
		
	    // See bug 469 to find out why we instantiate this here.
		backgroundInformation = createBackgroundEditor();
		
		copyrightDisplay = new CopyrightEditor(this,false){
			private static final long serialVersionUID = 1L;
			@Override
			public void onTextChange(String newText) {}
		};
		copyrightDisplay.setLocationRelativeTo(this);
		
		reloadRecentStoryMenuItems();
		
		poisoningsButton = new JButton("Poisonings!");
		poisoningsButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				swat.openLogLizard(Storyteller.this);
			}
		});
		
		setJMenuBar(menubar);

		setSize(800, 600);
		validate();
	}

	public void addPoisoningsButton() {
		if (poisoningsButton.getParent()==null) {
			poisonPanel.add(poisoningsButton);
			poisonPanel.revalidate();
			poisonPanel.repaint();
		}
	}
	private void removePoisoningsButton() {
		poisonPanel.remove(poisoningsButton);
	}
	
	/** Clears the panel at the right. */
	private void clearLeftPanels(){
		while(leftPanel.getComponentCount()>1)
			leftPanel.remove(0);
	}

	/** Clears the panel at the right. */
	private void clearRightPanels(){
		rightTopPanel.removeAll();
		if (rightPanel.getComponentCount()>1)
			rightPanel.remove(1);
	}

	/** Populates the top panel. */
	private void populateRightTopPanel(){
		if (rightTopPanel.getComponentCount()>0)
			return;

		rightTopPanel.add(Box.createRigidArea(new Dimension(10,0)));
		rightExpressionLabel.setAlignmentY(0.0f);
		rightTopPanel.add(rightExpressionLabel);
		rightTopPanel.add(Box.createHorizontalGlue());
	}
	
	/** Shows the face for the right sentence. */
	private void showSentenceFace(JLabel expressionLabel,LabeledSentence tEvent){
		if (tEvent.rawSentence.getIWord(Sentence.Verb)<0 || tEvent.expressionLabel==null)
			expressionLabel.setIcon(null);
		else {
			try {
				ImageIcon tempIcon = new ImageIcon(Utils.getImagePath("emoticubes/"+tEvent.expressionLabel+".png"));
				expressionLabel.setIcon(tempIcon);
				expressionLabel.setSize(tempIcon.getIconWidth(),tempIcon.getIconHeight());
			} catch (NullPointerException npe) { 
				System.out.println("Could not find emoticube file for: " + tEvent.expressionLabel );
				expressionLabel.setIcon(null);
			}
		}
	}
	
	private void reloadRecentStoryMenuItems(){
		for(JMenuItem mi:recentStoryMenuItems)
			storytellerMenu.remove(mi);
		recentStoryMenuItems.clear();
		
		int insertAt=storytellerMenu.getItemCount()-4;
		int i=0;
		for(final String file:Utils.getRecentStoryFiles()){
			if (swat==null && file.toLowerCase().endsWith(".str"))
				continue;
			final JMenuItem mi = new JMenuItem(i+" Load "+new File(file).getName());
			mi.setToolTipText("Loads "+file);
			mi.setMnemonic(KeyEvent.VK_0+i);
			mi.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					loadStory(new File(file));
				}
			});
			recentStoryMenuItems.add(mi);
			storytellerMenu.insert(mi,insertAt);
			i++;
		}
	}

	/** 
	 * Get traits for the given entity. 
	 * @param bit is used to identify the kind of entity.
	 * */
	private float[] getTraits(BIType bit,String label) throws RemoteException, SessionLogoutException {
		float[] values = null;
		switch(bit){
		case Actor:
			if (actorTraits.containsKey(label))
				values = actorTraits.get(label);
			else {
				values = janus.getActorTraits(sessionID, label);
				if (values==null)
					values = new float[]{-2f};
				actorTraits.put(label, values);
			}
			break;
		case Stage:
			if (stageTraits.containsKey(label))
				values = stageTraits.get(label);
			else {
				values = janus.getStageTraits(sessionID, label);
				if (values==null)
					values = new float[]{-2f};
				stageTraits.put(label, values);
			}
			break;
		case Prop:
			if (propTraits.containsKey(label))
				values = propTraits.get(label);
			else {
				values = janus.getPropTraits(sessionID, label);
				if (values==null)
					values = new float[]{-2f};
				propTraits.put(label, values);
			}
			break;
		}
		if (values!=null && values.length>0 && values[0]==-2f)
			return null;
		else
			return values;
	}

	
	/** Get known entities for a given type. */
	private String[] getKnownEntitieNames(BIType bit) throws RemoteException, SessionLogoutException {
		switch(bit){
		case Actor:
			if (knownActors!=null)
				return knownActors;
			else {
				String[] unknowns = janus.getActorsUnknownToProtagonist(sessionID);
				knownActors = new String[actorNames.length - unknowns.length];
				int count=0;
				for(String n:actorNames) {
					if (!Utils.contains(unknowns, n))
						knownActors[count++] = n;
				}
				return knownActors;
			}
		case Stage:
			if (knownStages!=null)
				return knownStages;
			else {
				String[] unknowns = janus.getStagesUnknownToProtagonist(sessionID);
				knownStages = new String[stageNames.length - unknowns.length];
				int count=0;
				for(String n:stageNames) {
					if (!Utils.contains(unknowns, n))
						knownStages[count++] = n;
				}
				return knownStages;
			}
		//case Prop:
		default:
			if (knownProps!=null)
				return knownProps;
			else {
				String[] unknowns = janus.getPropsUnknownToProtagonist(sessionID);
				knownProps = new String[propNames.length - unknowns.length];
				int count=0;
				for(String n:propNames) {
					if (!Utils.contains(unknowns, n))
						knownProps[count++] = n;
				}
				return knownProps;
			}
		}
	}

	
	private JList backgroundInfoList = null;
	private JScrollPane bgListScroll = null;
	private enum BIType { Actor, Stage, Prop}
	
	/** Shows the background information window. */
	private void showBackgroundInformation(BIType t){
		this.bit = t;
		
		Color c = Utils.STORYTELLER_RIGHT_COLOR;
		Pair<String[],String[]> traitNames;
		switch(bit){
		case Actor:
			backgroundInformation.setTitle("People");
			traitNames = actorTraitNames;
			//c = new Color(202,228,254);
			break;
		case Stage:
			backgroundInformation.setTitle("Places");
			traitNames = stageTraitNames;
			//c = new Color(252,230,152);
			break;
		default:
			backgroundInformation.setTitle("Things");
			traitNames = propTraitNames;
			//c = new Color(255,240,255);
			break;
		}
		backgroundInformation.getContentPane().setBackground(c);
		backgroundInformation.setBackground(c);

		bgPanel.setDescription("");
		bgPanel.setTraitNames(Arrays.asList(traitNames.first),Arrays.asList(traitNames.second));

		((DefaultListModel)backgroundInfoList.getModel()).removeAllElements();

		backgroundInformation.pack();
		backgroundInformation.setVisible(true);
		backgroundInfoList.requestFocusInWindow();

		tt.execute(new TaskTracker.Runnable() {
			public void run(long taskId) {
				myRun(true,taskId);
			}
			public void myRun(boolean retry,long taskId) {
				try {
					final String[] names = getKnownEntitieNames(bit);
					
					SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
						public void run(long taskId) {
							if (tt.isCanceled(taskId))
								return;
							for (String n: names)
								((DefaultListModel)backgroundInfoList.getModel()).addElement(n);
							if (backgroundInfoList.getModel().getSize()>0)
								backgroundInfoList.setSelectedIndex(0);
							backgroundInformation.pack();
						}
					}));
				} catch(SessionLogoutException ex){
					if (tt.isCanceled(taskId))
						return;
					if (retry && reloadExpiredSession(taskId)) {
						if (tt.isCanceled(taskId))
							return;
						myRun(false,taskId);
					} else {
						if (tt.isCanceled(taskId))
							return;
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you background information.\nYour session on the server has expired.","Connection error");
							}
						});
					}
				} catch (NoSuchObjectException re) {
					if (tt.isCanceled(taskId))
						return;
					re.printStackTrace();
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you background information.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
						}
					});
				} catch(final RemoteException e){
					if (tt.isCanceled(taskId))
						return;
					e.printStackTrace();
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you background information.\nA connection error occurred when fetching traits.","Connection error",storyworldID,dkversion,e,null);
						}
					});
				}
			}
		});

	}
	
	private JDialog createBackgroundEditor() {
		bgPanel = new BackgroundInformationPanel(false){
			private static final long serialVersionUID = 1L;
			@Override
			public void onDescriptionChange(String newDescription) {}
			@Override
			public void onImageChange(ScaledImage newImage) {}
		};
		bgPanel.setDescription("");
		
		final JDialog backgroundInformation = new JDialog(this);
		backgroundInformation.setResizable(false);
		backgroundInformation.getContentPane().add(bgPanel,BorderLayout.CENTER);
		
		backgroundInfoList = new JList(new DefaultListModel());
		backgroundInfoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		backgroundInfoList.getSelectionModel().addListSelectionListener(
				new ListSelectionListener(){
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting())
							return;
						
						final String name =(String)backgroundInfoList.getSelectedValue();
						if (name==null)
							return;
						Utils.setCursor(backgroundInformation, Cursor.WAIT_CURSOR);
						tt.execute(new TaskTracker.Runnable() {
							public void run(long taskId) {
								myRun(true,taskId);
							}
							public void myRun(boolean retry,long taskId) {
								try {
									final BgItemData i=getBgData(bit,name);
									final float[] ts=getTraits(bit,i.getLabel());
									SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
										public void run(long taskId) {
											if (tt.isCanceled(taskId))
												return;
											if (!name.equals((String)backgroundInfoList.getSelectedValue()))
												return;
											bgPanel.setDescription(i.getDescription());
											bgPanel.setTraitValues(ts);
											bgPanel.setImage(i.getImage()!=null?new ScaledImage(i.getImage()):null);
											Utils.setCursor(backgroundInformation, Cursor.DEFAULT_CURSOR);
										}
									}));
								} catch(SessionLogoutException ex){
									if (tt.isCanceled(taskId))
										return;
									if (retry && reloadExpiredSession(taskId)) {
										if (tt.isCanceled(taskId))
											return;
										myRun(false,taskId);
									} else {
										if (tt.isCanceled(taskId))
											return;
										SwingUtilities.invokeLater(new Runnable(){
											public void run() {
												Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you background information.\nYour session on the server has expired.","Connection error");
											}
										});
									}
								} catch (NoSuchObjectException re) {
									if (tt.isCanceled(taskId))
										return;
									re.printStackTrace();
									SwingUtilities.invokeLater(new Runnable(){
										public void run() {
											Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you background information.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
										}
									});
								} catch(final RemoteException e){
									if (tt.isCanceled(taskId))
										return;
									e.printStackTrace();
									SwingUtilities.invokeLater(new Runnable(){
										public void run() {
											Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you background information.\nA connection error occurred when fetching traits.","Connection error",storyworldID,dkversion,e,null);
										}
									});
								}
							}
						});
					}
				});

		//backgroundInfoList.setBackground(new Color(255, 240,255));
		bgListScroll = new JScrollPane(backgroundInfoList,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		backgroundInformation.getContentPane().add(bgListScroll, BorderLayout.WEST);
		
		backgroundInformation.pack();
		backgroundInformation.setLocationRelativeTo(this);

		return backgroundInformation;
	}

	private String getCopyright() throws SessionLogoutException, RemoteException {
		if (copyrightText==null) {
			if (dk!=null)
				return dk.getCopyright();
			else
				copyrightText = janus.getCopyright(sessionID);
		}
		
		return copyrightText;
	}
	
	/** Searches for a given actor image. */
	private BufferedImage getActorImage(String actorLabel) throws SessionLogoutException, RemoteException {
		BgItemData i=getActorBgData(actorLabel);
		if (i==null)
			return null;
		else
			return i.getImage();
	}

	/** Searches for a given stage image. */
	private BufferedImage getStageImage(String stageLabel) throws SessionLogoutException, RemoteException {
		BgItemData i=getStageBgData(stageLabel);
		if (i==null)
			return null;
		else
			return i.getImage();
	}
	
	/** Searches for a given prop image. */
	private BufferedImage getPropImage(String propLabel) throws SessionLogoutException, RemoteException {
		BgItemData i=getPropBgData(propLabel);
		if (i==null)
			return null;
		else
			return i.getImage();
	}
	
	private BufferedImage resizeForBottomPanel(BufferedImage original){
		if (original==null)
			return null;
		int w = Math.min(100, original.getWidth());
		int h = Math.min(100, original.getHeight());
		if (w*original.getHeight()<h*original.getWidth()) // w/h < ow/oh
			return Utils.getResizedImage(original, w, original.getHeight()*w/original.getWidth());
		else
			return Utils.getResizedImage(original, original.getWidth()*h/original.getHeight(), h);
	}
	
	private void sendTriggerSentence(LabeledSentence tEvent, int tTime, boolean tShowBottom, int playerId) {
		//result = -2;
		showBottom = tShowBottom;
		if (showBottom) {
			if (bottomScrollPanel.getParent()==null) {
				mainPanel.add(bottomScrollPanel);
				mainPanel.setDividerSize(5);
			}
		} else {
			mainPanel.remove(bottomScrollPanel);
			mainPanel.setDividerSize(0);
		}
		
		if (clearLeftPanelOnNextTriggerSentence)
			dtime = tTime-time;
		else
			dtime += tTime-time;
		
		clockDisplay.start(Storyteller.this,dtime);
		time = tTime;
		
		bottomPanel.removeAll();
		if (showBottom)
			updateBottomPanel(tEvent);

		// we don't need to refer to the left sentence, just display it.
		final SentenceDisplay lsd = new SentenceDisplay(this, null, true);
		lsd.revise(tEvent);
		
		JLabel expression = new JLabel(getLittleExpressionImage(tEvent));
		expression.setAlignmentY(0.0f);
		
		final JComponent sentenceRow = Box.createHorizontalBox();
		sentenceRow.add(expression);
		final JComponent lsdPanel = lsd.getSentencePanel();
		lsdPanel.setAlignmentY(0.0f);
 		sentenceRow.add(lsdPanel);
		
		if (clearLeftPanelOnNextTriggerSentence) {
			while (leftPanel.getComponentCount()>1)
				leftPanel.remove(0);
		}
		clearLeftPanelOnNextTriggerSentence = false;
		
		leftPanel.add(sentenceRow,leftPanel.getComponentCount()-1);
		lsdPanel.revalidate();
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				lsd.doLayout();
				lsdPanel.revalidate();
			}
		});

		populateRightPanel(new SentenceDisplay(this, inputSentenceListener, false));

		validate();
		// Make the new actions visible
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				sentenceRow.scrollRectToVisible(sentenceRow.getBounds());
			}
		});
	}

	private void populateRightPanel(SentenceDisplay sd)  {
		// we want the first two wordSockets active at first; they'll change as user enters choices
		if (sd!=null)
			inputSentence = sd;
		if (rightPanel.getComponentCount()>1)
			rightPanel.remove(1);
		if (sd!=null) {
			JComponent isPanel = inputSentence.getSentencePanel();
			rightPanel.add(isPanel);
			isPanel.revalidate();
		} else
			rightPanel.revalidate();

		populateRightTopPanel();
	}
	
	private void addPlayerSentence(int playerId,LabeledSentence s) {
		if (playerId<0)
			return;

		playerSentences.get(playerId).add(s);

		if (activeActor==playerId) {
			storybookArea.append("  ");
			storybookArea.append(s.toString());
			storybookArea.append("\n");
		}
	}

	private void recordSentenceOffset(int playerId) {
		if (playerId<0)
			return;
		playerSentencesOffset.set(playerId,playerSentences.get(playerId).size());
	}

	private void initSentenceVectors() {
		activeActor = -1;
		reactingActor = -1;
		playerSentences.clear();
		playerSentencesOffset.clear();
		for(int i=0;i<actorNames.length+1;i++) {
			playerSentences.add(new LinkedList<LabeledSentence>());
			playerSentencesOffset.add(0);
		}
	}
	
	private void setActiveActor(int playerId) {
		if (activeActor==playerId)
			return;

		activeActor = playerId;
		actorNameLabel.setText("("+actorNames[playerId-1]+")");
		while (leftPanel.getComponentCount()>1)
			leftPanel.remove(0);
		
		((JRadioButtonMenuItem)viewMenu.getMenuComponent(playerId-1)).setSelected(true);
		
		if (showBottom)
			updateBottomPanel(playerSentences.get(activeActor).getLast());

		storybookArea.setText("");

		for(LabeledSentence s: playerSentences.get(activeActor).subList(playerSentencesOffset.get(activeActor)
																		, playerSentences.get(activeActor).size())) {
			final SentenceDisplay lsd = new SentenceDisplay(this, null, true);
			lsd.revise(s);
			JLabel expression = new JLabel(getLittleExpressionImage(s));
			expression.setAlignmentY(0.0f);
			final JComponent sentenceRow = Box.createHorizontalBox();
			sentenceRow.add(expression);
			final JComponent lsdPanel = lsd.getSentencePanel();
			lsdPanel.setAlignmentY(0.0f);
	 		sentenceRow.add(lsdPanel);
			
			leftPanel.add(sentenceRow,leftPanel.getComponentCount()-1);
			lsdPanel.revalidate();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					lsd.doLayout();
					lsdPanel.revalidate();
				}
			});
		}
		for(LabeledSentence s: playerSentences.get(activeActor)) {
			storybookArea.append("  ");
			storybookArea.append(s.toString());
			storybookArea.append("\n");
		}
		populateRightPanel(activeActor==reactingActor?inputSentence:null);
		
		validate();
		repaint();
	}
	
	/** Returns a small expression image for a given event. */
	private ImageIcon getLittleExpressionImage(LabeledSentence tEvent){
		if (tEvent.rawSentence.getIWord(Sentence.Verb)<0 || tEvent.expressionLabel==null)
			return null;
		else {
			try {
				return new ImageIcon(Utils.getResizedImage(ImageIO.read(Utils.getImagePath("emoticubes/"+tEvent.expressionLabel+".png")), 65, 65));
			} catch (NullPointerException npe) { 
				System.out.println("Could not find emoticube file for: " + tEvent.expressionLabel );
				return null;
			} 
			catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	private void updateBottomPanel(final LabeledSentence tEvent){
		tt.execute(new TaskTracker.Runnable(){
			public void run(long taskId) {
				myRun(true,taskId);
			}
			
			public void myRun(boolean retry,long taskId) {
				try {
					/** Get images in advance. */
					getStageImage(tEvent.stageLabel);
					if (tt.isCanceled(taskId))
						return;
					for (String label: tEvent.actorsPresent)
						getActorImage(label);
					if (tt.isCanceled(taskId))
						return;
					for (String label: tEvent.propsPresent)
						getPropImage(label);
					if (tt.isCanceled(taskId))
						return;
					SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
						public void run(long taskId) {
							if (tt.isCanceled(taskId))
								return;

							bottomPanel.removeAll();
							JPanel stagePanel = new JPanel();
							stagePanel.setLayout(new BoxLayout(stagePanel, BoxLayout.Y_AXIS));
							stagePanel.setBackground(Color.white);
							stagePanel.add(new JLabel("<html><center>You are at</center></html>"));
							stagePanel.add(new JLabel("<html><center>"+tEvent.stageLabel+"</center></html>"));
							ImageIcon stageIcon;
							try {
								stageIcon = new ImageIcon(resizeForBottomPanel(getStageImage(tEvent.stageLabel)));
								if ((stageIcon.getIconHeight() > 0) && (stageIcon.getIconWidth() > 0))
									stagePanel.add(new JLabel(stageIcon));
							} catch (NullPointerException npe) {
								// Either local class’ classl loader is the System class loader,
								// or the image resource could not be found by the applet class loader.

							// Impossible to throw exceptions. Previous calls
							// already fetched the remote data.
							} catch (SessionLogoutException e) {
							} catch (RemoteException e) {}

							bottomPanel.add(stagePanel);

							try {
								for (String label: tEvent.actorsPresent) {
									JPanel actorPanel = new JPanel();
									actorPanel.setBackground(Color.white);
									actorPanel.setLayout(new BoxLayout(actorPanel, BoxLayout.Y_AXIS));
									actorPanel.add(new JLabel("<html><center>"+label+"<br>is here</center></html>"));
									BufferedImage bi = resizeForBottomPanel(getActorImage(label));
									if (bi!=null) {
										ImageIcon actorIcon = new ImageIcon(bi);
										if ((actorIcon.getIconHeight() > 0) && (actorIcon.getIconWidth() > 0))
											actorPanel.add(new JLabel(actorIcon));
									}
									actorPanel.add(Box.createVerticalGlue());
									bottomPanel.add(actorPanel);
								}

								for (String label: tEvent.propsPresent) {
									JPanel propPanel = new JPanel();
									propPanel.setBackground(Color.white);
									propPanel.setLayout(new BoxLayout(propPanel, BoxLayout.Y_AXIS));
									propPanel.add(new JLabel("<html><center>"+label+"<br>is here</center></html>"));
									BufferedImage bi = resizeForBottomPanel(getPropImage(label));
									if (bi!=null) {
										ImageIcon propIcon = new ImageIcon(resizeForBottomPanel(getPropImage(label)));
										if ((propIcon.getIconHeight() > 0) && (propIcon.getIconWidth() > 0))
											propPanel.add(new JLabel(propIcon));
									}
									propPanel.add(Box.createVerticalGlue());
									bottomPanel.add(propPanel);
								}
							// Impossible to throw exceptions. Previous calls
							// already fetched the remote data.
							} catch (SessionLogoutException e) {
							} catch (RemoteException e) {}
							
							bottomPanel.revalidate();
							validate();
						}
					}));
				} catch(SessionLogoutException e) {
					if (tt.isCanceled(taskId))
						return;
					if (retry && reloadExpiredSession(taskId)) {
						if (tt.isCanceled(taskId))
							return;
						myRun(false,taskId);
					} else {
						if (tt.isCanceled(taskId))
							return;
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nYour session on the server has expired.","Connection error");
							}
						});
					}
				} catch (NoSuchObjectException re) {
					if (tt.isCanceled(taskId))
						return;
					re.printStackTrace();
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
						}
					});
				} catch(final RemoteException e){
					if (tt.isCanceled(taskId))
						return;
					e.printStackTrace();
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(Storyteller.this, "A connection error occurred when fetching data from the server.","Connection error",storyworldID,dkversion,e,null);
						}
					});
				}
			}
		});
	}
//**********************************************************************
	private void setResult(int newResult,boolean clearPanels,long taskId) 
			throws SessionLogoutException, EngineDiedException, RemoteException {
		if (newResult<0) { // undoing
			for(int i=0;i>newResult;i--) {
				if (!recordedInput.isEmpty())
					recordedInput.removeLast();
				sentenceAmountOfInput--;
			}
		} else {
			recordedInput.add(newResult);
			sentenceAmountOfInput++;
		}
		try {
			while(!janus.setResult(sessionID, newResult) && !tt.isCanceled(taskId))
				Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		if (tt.isCanceled(taskId))
			return;
		
		// Process data that the server sends next
		if (!getServerDataToDisplay(null,clearPanels,taskId))
			while (!getServerDataToDisplay(taskId));
	}	
	private boolean getServerDataToDisplay(long taskId) 
		throws SessionLogoutException, EngineDiedException, RemoteException {
		return getServerDataToDisplay(null,false,taskId);
	}
	private boolean getServerDataToDisplay(boolean[] hotWordSockets,long taskId) 
				throws SessionLogoutException, EngineDiedException, RemoteException {
		return getServerDataToDisplay(hotWordSockets,false,taskId);
	}
	private StorytellerReturnData mGetServerDataToDisplay(boolean firstCall,long taskId) 
									throws SessionLogoutException, EngineDiedException, RemoteException {
		StorytellerReturnData res;
		TimeoutAdjuster timeoutAdjuster = firstCall?firstTimeoutAdjuster:secondTimeoutAdjuster;
		try {
			timeoutAdjuster.init();
			do {
				if (tt.isCanceled(taskId))
					return null;
				Thread.sleep(timeoutAdjuster.getTimeout());
				if (tt.isCanceled(taskId))
					return null;
			} while(null==(res=janus.getTrigger(sessionID)));
			timeoutAdjuster.done();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return res;
	}
	private boolean getServerDataToDisplay(final boolean[] hotWordSockets,final boolean firstCall,long taskId) 
					throws SessionLogoutException, EngineDiedException, RemoteException {
		// Get input from the server.  This replaces calls from Engine and Janus to Storyteller
		final StorytellerReturnData rD = mGetServerDataToDisplay(firstCall,taskId);
		if (tt.isCanceled(taskId))
			return true;

			// Process the response in the event dispatcher thread.
		SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable() {
			public void run(long taskId) {
				if (tt.isCanceled(taskId))
					return;

				storytellerMenu.setEnabled(true);

				// Run the method on the client that the Engine expects to run
				if (rD.returnType == StorytellerReturnData.engineCallType.SEND_TRIGGER_SENTENCE) {
					//System.out.println("SEND_TRIGGER_SENTENCE");
					addPlayerSentence(rD.playerId,rD.tLabeledSentence);
					if (activeActor==rD.playerId)
						sendTriggerSentence(rD.tLabeledSentence, rD.tTime, rD.showBottom, rD.playerId);	
				}
				if (rD.returnType == StorytellerReturnData.engineCallType.GET_PLAYER_DONE) {
					//System.out.println("GET_PLAYER_DONE");
					getPlayerDone(hotWordSockets,rD.tLabeledSentence,rD.playerId);
				}
				if (rD.returnType == StorytellerReturnData.engineCallType.GET_PLAYER_SELECTION) {
					//System.out.println("GET_PLAYER_SELECTION");
					getPlayerSelection(hotWordSockets,rD.tLabeledSentence, rD.menuElements,  rD.wordSocket, rD.playerId);
				}
				if (rD.returnType == StorytellerReturnData.engineCallType.THE_END) {
					//System.out.println("THE_END");
					theEnd();
				}

				repaint();
			}
		}));
		return rD.inputEnded();
	}	
	
//**********************************************************************	
	private void getPlayerSelection(boolean[] hotWordSockets,LabeledSentence tLabeledSentence, ArrayList<MenuElement> menuElements, int wordSocket, int playerId) {
		reactingActor = playerId;
		setActiveActor(playerId);
		
		populateRightPanel(new SentenceDisplay(this, inputSentenceListener, false));
		
		inputSentence.getSentencePanel().revalidate();
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				inputSentence.doLayout();
				inputSentence.getSentencePanel().revalidate();
			}
		});
		if (hotWordSockets!=null)
			inputSentence.setHotWordSockets(hotWordSockets);
		inputSentence.revise(tLabeledSentence, menuElements, wordSocket);
		showSentenceFace(rightExpressionLabel, tLabeledSentence);

		if (menuElements==null || menuElements.isEmpty())
			inputSentence.requestFocusInWindow();
		if (freshSentence) {
			tt.execute(new TaskTracker.Runnable() {
				public void run(long taskId) {
					refreshAuxiliaryWindows(taskId);
				}
			});
			freshSentence = false;
		}

	}
//**********************************************************************	
	private void getPlayerDone(boolean[] hotWordSockets,LabeledSentence tLabeledSentence,int playerId) {
		clearLeftPanelOnNextTriggerSentence = true;
		reactingActor = playerId;
		setActiveActor(playerId);

		populateRightPanel(new SentenceDisplay(this, inputSentenceListener, false));

		inputSentence.getSentencePanel().revalidate();
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				inputSentence.doLayout();
				inputSentence.getSentencePanel().revalidate();
			}
		});
		if (hotWordSockets!=null)
			inputSentence.setHotWordSockets(hotWordSockets);
		inputSentence.revise(tLabeledSentence, null, Sentence.MaxWordSockets);
		donebt = inputSentence.putPeriodButton(doneAction);
		showSentenceFace(rightExpressionLabel, tLabeledSentence);
		donebt.requestFocusInWindow();
		if (freshSentence) {
			tt.execute(new TaskTracker.Runnable() {
				public void run(long taskId) {
					refreshAuxiliaryWindows(taskId);
				}
			});
			freshSentence = false;
		}
	}
//**********************************************************************
	private void theEnd() {
		bottomPanel.removeAll();
		mainPanel.remove(bottomScrollPanel);
		mainPanel.setDividerSize(0);
		rightExpressionLabel.setIcon(null);
		if (rightPanel.getComponentCount()>1)
			rightPanel.remove(1);
		populateRightTopPanel();
		
		Utils.setCursor(this, Cursor.WAIT_CURSOR);
		tt.execute(new TaskTracker.Runnable(){
			public void run(long taskId) {
				myRun(true,taskId);
			}
			
			public void myRun(boolean retry,long taskId) {
				try {
					refreshAuxiliaryWindows(taskId);
					//String[] ss=getStorybook();
					getStorybook();
					//if (tt.isCanceled(taskId))
					//	return;
					//for(String s:ss){
					//	storybookArea.append(s);
					//	storybookArea.append("\n");
					//}

					SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
						public void run(long taskId) {
							if (tt.isCanceled(taskId))
								return;
							Utils.setCursor(Storyteller.this, Cursor.DEFAULT_CURSOR);

							JLabel endtext = new JLabel("The End");
							endtext.setOpaque(false);
							endtext.setFont(THE_END_FONT);
							JLabel endtext2 = new JLabel("Nothing else happened");
							endtext2.setOpaque(false);
							endtext2.setFont(THE_END_SMALL_FONT);
							endtext2.setBorder(BorderFactory.createEmptyBorder(0,0,20,0));

							JTextArea jta = new JTextArea(storybookArea.getText());
							jta.setBackground(storybookArea.getBackground());
							jta.setEditable(false);
							
							JScrollPane scroll = new JScrollPane(jta);
							scroll.setOpaque(false);
							scroll.getViewport().setOpaque(false);
							scroll.setAlignmentX(0.0f);
							scroll.setPreferredSize(new Dimension(450,300));
							scroll.setBorder(BorderFactory.createCompoundBorder(
									BorderFactory.createEmptyBorder(0,0,10,0),
									scroll.getBorder()
								));

							JComponent theEndPanel = Box.createVerticalBox();
							theEndPanel.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
							endtext.setAlignmentX(0.0f);
							theEndPanel.add(endtext);
							endtext2.setAlignmentX(0.0f);
							theEndPanel.add(endtext2);
							theEndPanel.add(scroll);
							theEndPanel.add(Box.createVerticalGlue());
							
							rightPanel.add(theEndPanel);
							
							theEndPanel.revalidate();
							repaint();
						}
					}));
				} catch(SessionLogoutException e) {
					if (tt.isCanceled(taskId))
						return;
					if (retry && reloadExpiredSession(taskId)) {
						if (tt.isCanceled(taskId))
							return;
						myRun(false,taskId);
					} else {
						if (tt.isCanceled(taskId))
							return;
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nYour session on the server has expired.","Connection error");
							}
						});
					}
				} catch (NoSuchObjectException re) {
					if (tt.isCanceled(taskId))
						return;
					re.printStackTrace();
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
						}
					});
				} catch(final RemoteException e){
					if (tt.isCanceled(taskId))
						return;
					e.printStackTrace();
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(Storyteller.this, "A connection error occurred when fetching storybook entries.","Connection error",storyworldID,dkversion,e,null);
						}
					});
				}
			}
		});
	}
	
	private void cleanupTheEnd() {
		time = dtime = 0;
		freshSentence=true;
		
		clearLeftPanels();

		if (rightPanel.getComponentCount()>1)
			rightPanel.remove(1);

		validate();
		repaint();
	}

	//**********************************************************************		
	// Tell the server to end the story when closing the Storyteller window.
	// This also allows Janus to restore the state of Deikto after ending a local
	// storyteller session.
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSED) {
			timerAdjusterTimer.stop();
			saveInactiveSessionTimer.stop();
		}

		if (e.getID() == WindowEvent.WINDOW_CLOSED && !clean) {
			try {
				Utils.saveProperties();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			tt.cancelTasks(5000);
			new Thread(){
				@Override
				public void run() {
					try {
						logout(logDownloader==null);
						janus=null;
					} catch(Exception exc){
						exc.printStackTrace();
					} finally {
						if (exitOnClose)
							System.exit(0);
					}
				}
			}.start();
			if (exitOnClose)
				new Timer(20000,new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						System.exit(0);
					}
				}).start();
			clean=true;
		}
		super.processWindowEvent(e);
	}

	/** 
	 * Requests data for the auxiliary windows.
	 * This must never be called from the event thread.
	 * */
	private void refreshAuxiliaryWindows(long taskId){
		if (tt.isCanceled(taskId))
			return;
		if (relationshipBrowser!=null && relationshipBrowser.isVisible()) {
			SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
				public void run(long taskId) {
					if (tt.isCanceled(taskId))
						return;
					relationshipBrowser.refresh();
				}
			}));
		}
		refreshAuxiliaryWindows(true,taskId);
	}
	private void refreshAuxiliaryWindows(boolean retry,long taskId){
		try {
			if (backgroundInformation!=null && backgroundInformation.isVisible()) {
				final String[] names = getKnownEntitieNames(bit);
				final Object oldSelection = backgroundInfoList.getSelectedValue();
				int i = oldSelection==null?0:Utils.indexOf(names, oldSelection.toString());
				final int newSelectedIndex = Math.max(i, 0);
				if (tt.isCanceled(taskId))
					return;
				SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
					public void run(long taskId) {
						if (tt.isCanceled(taskId))
							return;
						((DefaultListModel)backgroundInfoList.getModel()).removeAllElements();
						for (String n: names)
							((DefaultListModel)backgroundInfoList.getModel()).addElement(n);
						if (backgroundInfoList.getModel().getSize()>0)
							backgroundInfoList.setSelectedIndex(newSelectedIndex);
					};
				}));
			}
			if (storybookFrame!=null &&storybookFrame.isVisible()) {
				//final String[] sb = getStorybook();
				getStorybook();
				 
				//SwingUtilities.invokeLater(new Runnable(){
				//	public void run() {
				//		for(String s:sb){
				//			storybookArea.append(s);
				//			storybookArea.append("\n");
				//		}
				//	};
				//});
			}
		} catch(SessionLogoutException e) {
			if (tt.isCanceled(taskId))
				return;
			if (retry && reloadExpiredSession(taskId)) {
				if (tt.isCanceled(taskId))
					return;
				refreshAuxiliaryWindows(false,taskId);
			} else {
				if (tt.isCanceled(taskId))
					return;
				SwingUtilities.invokeLater(new Runnable(){
					public void run() {
						Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nYour session on the server has expired.","Connection error");
					}
				});
			}
		} catch (NoSuchObjectException re) {
			if (tt.isCanceled(taskId))
				return;
			re.printStackTrace();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot continue playing this storyworld.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
				}
			});
		} catch(final RemoteException e){
			if (tt.isCanceled(taskId))
				return;
			e.printStackTrace();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					Utils.showErrorDialog(Storyteller.this, "A connection error occurred when fetching\ndata to refresh auxiliary windows.","Connection error",storyworldID,dkversion,e,null);
				}
			});
		}
	}
	
	/** Shows the relationship browser. */
	private void showRelationships(){
		if (relationshipBrowser==null) {
			createRelationshipBrowser(actorNames, relationshipNames);
			relationshipBrowser.setVisible(true);
			relationshipBrowser.requestFocusInWindow();
		} else {
			relationshipBrowser.setVisible(true);
			relationshipBrowser.requestFocusInWindow();
		}
	}

	/** 
	 * Creates the relationship browser. Don't call this directly, instead 
	 * call {@link #showRelationships()}. 
	 * */
	private void createRelationshipBrowser(final String[] actorNames,Pair<String[],String[]> relationshipNames){
		relationshipBrowser = new RelationshipBrowser(this,actorNames,relationshipNames.first,relationshipNames.second) {
			private static final long serialVersionUID = 0L;
			@Override
			public void getRelationValues(final String relationshipName) {
				float[][] values = relationshipValues.get(relationshipName);
				if (values==null) {
					Utils.setCursor(Storyteller.this,Cursor.WAIT_CURSOR);
					tt.execute(new TaskTracker.Runnable(){
						
						public void run(long taskId) {
							myRun(true,taskId);
						}
						
						public void myRun(boolean retry,long taskId) {
							try {
								final String[] knownActors = getKnownEntitieNames(BIType.Actor);
								final float[][] fvalues = janus.getRelationshipValues(sessionID,relationshipName);
								if (tt.isCanceled(taskId))
									return;
								relationshipValues.put(relationshipName, fvalues);
								SwingUtilities.invokeLater(tt.trackRunnable(new TaskTracker.Runnable(){
									public void run(long taskId) {
										if (tt.isCanceled(taskId))
											return;
										setKnownActors(knownActors);
										setRelationValues(getRelationshipValues(fvalues, knownActors, getSelectedActorName()));
										Utils.setCursor(Storyteller.this,Cursor.DEFAULT_CURSOR);
									}
								}));
							} catch(SessionLogoutException e) {
								if (tt.isCanceled(taskId))
									return;
								if (retry && reloadExpiredSession(taskId)) {
									if (tt.isCanceled(taskId))
										return;
									myRun(false,taskId);
								} else {
									if (tt.isCanceled(taskId))
										return;
									SwingUtilities.invokeLater(new Runnable(){
										public void run() {
											Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you Relationships.\nYour session on the server has expired.","Connection error");
										}
									});
								}
							} catch (NoSuchObjectException re) {
								if (tt.isCanceled(taskId))
									return;
								re.printStackTrace();
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										Utils.showErrorDialog(Storyteller.this, "I'm sorry, but I cannot show you Relationships.\nThe server has been shut down!\n"+Utils.currentTime()+"\nversion: "+SharedConstants.version,"Connection error");
									}
								});
							} catch(final RemoteException e){
								if (tt.isCanceled(taskId))
									return;
								e.printStackTrace();
								SwingUtilities.invokeLater(new Runnable(){
									public void run() {
										Utils.showErrorDialog(Storyteller.this, "A connection error occurred when fetching data from the server.","Connection error",storyworldID,dkversion,e,null);
									}
								});
							}
						}
					});
				} else
					setRelationValues(getRelationshipValues(values, knownActors, getSelectedActorName()));
			}
		};
		relationshipBrowser.setLocation(100, 50);
	}

	/** 
	 * Gets values from a stored matrix as received from the server.
	 * The return values are layout according to the requirements of 
	 * {@link RelationshipBrowser}.
	 * */
	private static float[] getRelationshipValues(float[][] values,String[] actorNames,String actorName) {
		float[] vs = new float[2*actorNames.length];
		int actorIndex = Utils.indexOf(actorNames, actorName);
		for(int i=0;i<actorNames.length;i++) {
			vs[i] = values[actorIndex][i];
			vs[actorNames.length+i] = values[i][actorIndex];
		}
		return vs;
	}
	
	private String[] getStorybook() throws RemoteException, SessionLogoutException {
		return janus.getStorybookEntries(sessionID);
	}

	/** @return names of the elements of the given type. */
	private BgItemData getBgData(BIType t,String name) throws SessionLogoutException, RemoteException {
		switch(t){
		case Actor:
			return getActorBgData(name);
		case Stage:
			return getStageBgData(name);
		default:
			return getPropBgData(name);
		}
	}

	/** 
	 * @return the background information for the given actor, null if such actor 
	 *         does not exist.
	 * */
	private BgItemData getActorBgData(String name) throws SessionLogoutException, RemoteException {
		BgItemData i = actorBgData.get(name);
		if (i==null && !actorBgData.containsKey(name)) {
			if (dk!=null)
				i = dk.getActorBgData(name, false);
			else
				i = janus.getActorBgData(sessionID,name);
			actorBgData.put(name, i);
		}
		return i;
	}

	/** 
	 * @return the background information for the given stage, null if such stage 
	 *         does not exist.
	 * */
	private BgItemData getStageBgData(String name) throws SessionLogoutException, RemoteException {
		BgItemData i = stageBgData.get(name);
		if (i==null && !stageBgData.containsKey(name)) {
			if (dk!=null)
				i = dk.getStageBgData(name, false);
			else
				i = janus.getStageBgData(sessionID,name);
			stageBgData.put(name, i);
		}
		return i;
	}

	/** 
	 * @return the background information for the given prop, null if such prop 
	 *         does not exist.
	 * */
	private BgItemData getPropBgData(String name) throws SessionLogoutException, RemoteException {
		BgItemData i = propBgData.get(name);
		if (i==null && !propBgData.containsKey(name)) {
			if (dk!=null)
				i = dk.getPropBgData(name, false);
			else
				i = janus.getPropBgData(sessionID,name);
			propBgData.put(name, i);
		}
		return i;
	}

	/** A class for tracking execution of tasks. */
	private static class TaskTracker {
		private long gen=0; 
		private boolean waiting = false;
		private ArrayList<Long> tasks=new ArrayList<Long>();
		private static java.lang.Runnable emptyR = new java.lang.Runnable() {public void run() {}};
		
		/** Executes the given task in a new thread. */
		public void execute(TaskTracker.Runnable r){
			new Thread(trackRunnable(r)).start();
		}
		/** 
		 * Wraps the task in a runnable instance that notifies the TaskTracker
		 * when it is complete.
		 * */
		public synchronized java.lang.Runnable trackRunnable(final TaskTracker.Runnable r){
			if (waiting)
				return emptyR; 
			else { 
				final long taskId = gen++;
				tasks.add(taskId);
				return new java.lang.Runnable() {
					public void run() {
						try {
							r.run(taskId);
						} finally {
							stopTracking(taskId);
						}
					}
				};
			}
		}
		
		/** Stops tracking a tasks. Returns true iff the task has been canceled. */
		private synchronized boolean stopTracking(long taskId){
			boolean canceled = !tasks.remove((Long)taskId);
			notify();
			return canceled;
		}
		
		/** Tells if a task has been canceled. */
		public synchronized boolean isCanceled(long taskId){
			return waiting || !tasks.contains((Long)taskId);
		}
		
		/** 
		 * Waits for a given amount of time if there is any task running,
		 * then cancel all the tasks. 
		 * */
		public synchronized void cancelTasks(long timeout) {
			waiting = true;
			long end=System.currentTimeMillis()+timeout;
			while(!tasks.isEmpty() && timeout>0) {
				try {
					wait(timeout);
				} catch (InterruptedException e) {}
				timeout=end-System.currentTimeMillis();
			}
			if (!tasks.isEmpty())
				tasks.clear();
			waiting = false;
		}
		
		/** A task that can be tracked to be canceled or waited for completion. */
		public interface Runnable {
			void run(long taskId); 
		}
	}

	/** 
	 * Class for an auto-adjusting timeout.
	 * <p>
	 * Manipulations of the timeout are subject to certain operation.
	 * The timeout estimates how long the operation takes to complete.
	 * <p>
	 * Upon start of the operation the {@link #init()} method must be called.
	 * When the operation finishes the method {@link #done()} must be called.
	 * Between calls to these methods, the timeout can be retrieved with 
	 * {@link #getTimeout()}.
	 * <p>
	 * If the timeout turns to be too short, it will be increased.
	 * The timeout is considered too short if more than one call is done during an operation.
	 * <p>
	 * If the timeout turns to be too long, it will be decreased only if 
	 * allowDecreasing was set to true and the timeout has been never too short since
	 * then.
	 * <p>
	 * Initially the timeout is not adjusted. To start adjusting the method {@link #setAdjusting(boolean)}
	 * must be called with <code>true</code>. 
	 * */
	private class TimeoutAdjuster {
		private final int[] timeouts = new int[5];
		private int top=0;
		private int pt=0;
		private int timeout=250;
		private long initTime;
		private int count;
		private boolean adjusting = false;
		private boolean allowDecreasing = true;
		
		/** Signals start of an operation. */
		void init(){ 
			if (!adjusting)
				return;
			
			initTime=System.currentTimeMillis();
			count=0;
		}
		
		/** @return the timeout as adjusted so far. */
		int getTimeout() {
			count++;
			return timeout; 
		}
		
		/** Signals end of an operation. */
		void done(){
			if (!adjusting){
				//System.out.println("just count: "+count);
				return;
			}
			
			if (top<timeouts.length)
				top++;
			timeouts[pt] = count>1 ? (int)(System.currentTimeMillis()-initTime)
						:  allowDecreasing ? timeout/2
						:  timeout;
			pt = (pt+1) % timeouts.length;
			
			if (count>1)
				allowDecreasing = false;
			
			int sum=0;
			for(int i=0;i<top;i++)
				sum+=timeouts[i];
			timeout = sum/top;
			//System.out.println("new timeout: "+timeout+" count: "+count);
		}

		/** 
		 * Tells whether the timeout should be adjusted according to the time required
		 * by the operation to complete.
		 * */
		public void setAdjusting(boolean adjusting) {
			this.adjusting = adjusting;
		}

		/** Tells if the timeout estimate is allowed to decrease. */
		public void setAllowDecreasing(boolean allowDecreasing) {
			this.allowDecreasing = allowDecreasing;
		}
	}
	
	private final static class ServerRefusesToLoadException extends Exception {
		private static final long serialVersionUID = 0L;
	}

	public static abstract class Test {
		
		public static RelationshipBrowser openRelationships(Storyteller st){
			((JMenuItem)((JMenu)st.menubar.getComponent(1)).getMenuComponent(5)).doClick();
			return st.relationshipBrowser;
		}
	}
}
