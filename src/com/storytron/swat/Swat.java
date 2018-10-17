package com.storytron.swat;



import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.FocusManager;
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
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import storyTellerPackage.SavedStory;
import storyTellerPackage.Storyteller;
import Engine.enginePackage.Janus;

import com.storytron.enginecommon.BadStoryworldException;
import com.storytron.enginecommon.IncompatibleVersionException;
import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.LocalJanus;
import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.RehearsalResult;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.StorytellerRemote;
import com.storytron.enginecommon.SwatRemote;
import com.storytron.enginecommon.Triplet;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.loglizard.LogDownloadManager;
import com.storytron.swat.loglizard.LogLizard;
import com.storytron.swat.loglizard.TreeOutputStream;
import com.storytron.swat.util.Compressed;
import com.storytron.swat.util.DropDown;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.OSXAdapter;
import com.storytron.swat.util.ProgressDialog;
import com.storytron.swat.util.UndoableAction;
import com.storytron.swat.verbeditor.OperatorEditor;
import com.storytron.swat.verbeditor.VerbEditor;
import com.storytron.swat.verbeditor.VerbPropertiesEditor;
import com.storytron.uber.Deikto;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Sentence;
import com.storytron.uber.Verb;
import com.storytron.uber.Deikto.LogIssue;
import com.storytron.uber.Deikto.WritingException;
import com.storytron.uber.Script.Node;
import com.storytron.uber.deiktotrans.DeiktoLoader.BadVersionException;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;

/**
 * This class is the root of the Swat tool application. 
 * <p>
 * When instantiating {@link Swat}, saving properties to disk must be done
 * in a shutdown hook registered by the programmer.
 * <p>
 * The reaction to the CMD-Q event is implemented in the {@link #quit()} method.
 * <p>
 * The {@link StorytellerRemote#logout(String)} is called by a shutdown hook 
 * registered by the {@link Swat#Swat()} constructor.
 * <p>
 * Swat is started from the {@link SwatMain} class, which sets some system
 * properties needed before the jvm loads the awt classes. 
 */
public final class Swat {
	public static enum EditorEnum {
		NoEditorHasFocus,
		VerbEditorHasFocus,
		ActorEditorHasFocus,
		RelationshipEditorHasFocus,
		PropEditorHasFocus,
		StageEditorHasFocus,
	};
	private static final long serialVersionUID = 1L;

	private static JMenuBar menuBar;
	protected JFrame myFrame;
	private EditorEnum editorInFocus = EditorEnum.NoEditorHasFocus;
	public JMenuItem 
		newMenuItem,
		openMenuItem,
		saveMenuItem, 
		undoMenuItem,
		redoMenuItem,
		cutMenuItem, 
		copyMenuItem,
		pasteMenuItem, 
		copyOption,
		pasteOption,
		pasteOptionLink,
		copyRole, 
		pasteRole,
		pasteRoleLink,
		
		verbEditorMenuItem, 
		actorEditorMenuItem,
		relationshipEditorMenuItem,
		propEditorMenuItem, 
		stageEditorMenuItem,
					
		rehearsalLizardMenuItem, 
		storytellerLizardMenuItem, 
		comeFromLizardMenuItem, 
					
		relationshipsMenuItem;
	public Node clipboard;
	public Script clipboardScript;
	public Deikto dk;
	private LinkedList<String> dkResourceNames;
	private LogLizard logLizard;
	public ComesFromLizard comeFromLizard;
	public VerbEditor verbEditor;
	ActorEditor actorEditor;
	RelationshipEditor relationshipEditor;
	PropEditor propEditor;
	StageEditor stageEditor;
	private OperatorEditor operatorEditor;
	public CopyrightEditor copyrightEditor;
	private static boolean soundOn;
	private UndoManager undoManager = new UndoManager();
	public static final int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	private static final Color noteSearchColor = new Color(0.75f, 1.0f, 0.75f);
	private SwatRemote swatJanus; // interface for calling Janus
	private boolean quitting=false;
	public static final Color darkShadow = UIManager.getLookAndFeelDefaults().getColor("TextField.darkShadow");
	public static final Color shadow = UIManager.getLookAndFeelDefaults().getColor("TextField.shadow");

	private static String rehearsalSessionID = "";
	
	/** 
	 * This flags is used to tell if a menu key bindings has executed or not.
	 * Sometimes the menu items are not fired, I don't know why.
	 *  */
	private boolean menuAcceleratorEventProcessed=false;
	
	private JFileChooser chooser = new JFileChooser("res/data");
	private JDialog badLogFrame;
	private DefaultListModel logListModel;
	private LogDownloaderThread logDownloader;
	public LogDownloadManager logManager;
	private File file=new File(""); // for inclusion in the main title bar
	private RelationshipSettings relationshipSettings;
	private TerminationSettings terminationSettings;
	private Storyteller st; 
	private String login, password;
	private static boolean altPressed, controlPressed;

	private static Map<String,AudioClip> clips=new TreeMap<String,AudioClip>();
	
	private FileFilter swatFileFilter = new FileFilter(){
		@Override
		public boolean accept(File f) {
			String name=f.getName();
			int i=name.lastIndexOf('.');
			String ext=i==-1?null:name.substring(i).toLowerCase();
			return f.isDirectory() || ext!=null && ext.equals(".stw");
		}

		@Override
		public String getDescription() {
			return "Storyworld file ( .stw )";
		}			
	};
	
	// Setting stwfile to null forces prompt and selection of a storyworld file.
	// Set to storyworld filename to automatically load that storyworld file.
	// Examples: "testdata/Test.stw" or "/Users/billmaya/Dropbox/Projects-Dropbox/Siboot/Siboot-for-Storytron/SWAT/My storyworlds/Siboot.stw"
	static String stwfile = null; 
	
	static ProgressMonitor rehearsalBar;
	
//*********************************************************************
	/**
	 * <ul>
	 *  <li>Sets tooltip delays.</li>
	 *  <li>Sets global keystrokes.</li>
	 *  <li>Builds the application menu bar.</li>
	 *  <li>Presents a file chooser to the user so she can 
	 *  pick up a storyworld to load.</li>
	 *  <li>Builds all the swat editors.</li>
	 * </ul>
	 * */
	
	public Swat() {
		this(stwfile); 
	}
	
	/** 
	 * This constructor is called from testing routines.
	 * If stwfile is not null, it picks that file to load
	 * instead of asking the user. 
	 *  */
	public Swat(String stwfile) {
		
		earlyInitialization();
		
		try {
			VerbPropertiesEditor.loadExpressionList(); // can throw IOException
			OperatorDictionary.loadOperators(); // can throw IOException, ParserConfigurationException, SAXException
			Swat.this.init();
		} catch (Exception e) { // TODO Handle specific exceptions instead of generic exception
			Utils.showErrorDialog(null, "There was an error launching Swat.", "Launch error", e);
			System.exit(0);
		}

		File cd = Utils.getWorkingDirectory();
		if (cd != null) chooser.setCurrentDirectory(cd);

		openStoryworld(stwfile);		
		updateEditor();

	}
	
	public void openStoryworld(String stwfile) {
		
		try{
			if (stwfile==null) { 
				chooser.setFileFilter(swatFileFilter);
				switch(chooser.showOpenDialog(myFrame)){
				case JFileChooser.APPROVE_OPTION:
					file = chooser.getSelectedFile();
					Utils.setWorkingDirectory(chooser.getCurrentDirectory());
					break;
				default:
					Utils.setWorkingDirectory(chooser.getCurrentDirectory());
					System.exit(0);
				}
			} else 
				file = new File(stwfile);
			
			FileInputStream fis = new FileInputStream(file);
			dk = new Deikto(file);
			dk.roleVerbs = new HashMap<Role,ArrayList<Verb>>();
			dk.optionRoles = new HashMap<Role.Option,ArrayList<Role>>();
			dk.readXML(fis, true);
			fis.close();
			dkResourceNames = dk.getResourceNames();
			final LinkedList<LogIssue> errors=dk.checkScripts(null,true);
			if (!errors.isEmpty()) 
				showLogIssues(errors);
			
		} catch (BadVersionException e){
			Utils.showErrorDialog(null, "There was an error when reading the file\n"+chooser.getSelectedFile().getPath()+"\nI do not know how to load version "+e.version,"File error");
			System.exit(0);
		} catch (SAXParseException e) {
			Utils.showErrorDialog(null, "There was an error while reading the file\n"+chooser.getSelectedFile().getPath()+"\nThe file has an invalid format. Line: "+e.getLineNumber()+" Column: "+e.getColumnNumber(),"Reading error",e);
			System.exit(0);
		} catch (IOException e) {
			Utils.showErrorDialog(null, "There was an error when trying to access the file\n"+chooser.getSelectedFile().getPath(),"File error",e);
			System.exit(0);
		} catch (LimitException e) {
			e.printStackTrace();
			Utils.displayLimitExceptionMessage(e,"File error","There was an error when reading the file\n"+chooser.getSelectedFile().getPath());
			System.exit(0);
		} catch (Deikto.ReadingException e) {
			e.printStackTrace();
			switch(e.t) {
			case OperatorDoesNotExist:
				Utils.showErrorDialog(null, "There was an error when reading the file\n"+chooser.getSelectedFile().getPath()+"\n\nI found and unknown operator "+e.s0+"\nwhen reading the script\n"+e.s1,"File error");
				break;
			case WordDescriptionTraitDoesNotExist:
				Utils.showErrorDialog(null, "There was an error when reading the file\n"+chooser.getSelectedFile().getPath()+"\n\nI found and unknown trait "+e.s0+"\n when reading traits of "+e.s1,"File error");
				break;
			case WordDescriptionPTraitDoesNotExist:
				Utils.showErrorDialog(null, "There was an error when reading the file\n"+chooser.getSelectedFile().getPath()+"\n\nI found and unknown perception trait "+e.s0+"\nwhen reading traits of "+e.s1+" towards "+e.s2,"File error");
				break;
			}
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			Utils.showErrorDialog(null, "There was an error while reading the file\n"+chooser.getSelectedFile().getPath()+"\nMake sure this file is a storyworld file and it is not corrupt.","Reading error",e);
			System.exit(0);
		}
	}
	
	private void updateEditor() {
		
		updateFrameTitle();

		verbEditor.init(dk);
		operatorEditor.init(dk);
		actorEditor.init(dk);
		propEditor.init(dk);
		stageEditor.init(dk);
		relationshipEditor.init(dk);
		
		copyrightEditor.setText(dk.getCopyright());

		setEditorInFocus(EditorEnum.VerbEditorHasFocus);
		myFrame.setVisible(true);
	}
	
	private void updateFrameTitle(){
		myFrame.setTitle(file.getName()+" v"+dk.version);
	}
	
	/** 
	 * Shows the log panel, with the error messages resulting for loading
	 * a storyworld. 
	 * */
	private void showLogIssues(Iterable<LogIssue> logIssueList){
		clearLogIssues();
		if (badLogFrame==null) {
			badLogFrame = new JDialog(myFrame,"Sniffy Lizard");
			logListModel = new DefaultListModel();
			final JList jlist = new JList(logListModel);
			JScrollPane logScrollPane =  new JScrollPane(jlist,
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			jlist.setCellRenderer(new DefaultListCellRenderer(){
				private static final long serialVersionUID = 1L;
				@Override
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					Component c = super.getListCellRendererComponent(list, value, index, isSelected,cellHasFocus);
					((JComponent)c).setToolTipText(Utils.toHtmlTooltipFormat("Double-click to jump to this Script."));
					return c;
				}
			});
			jlist.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						LogIssue o = (LogIssue)jlist.getSelectedValue();
						if (o != null && o.s!=null) {
							setEditorInFocus(EditorEnum.VerbEditorHasFocus);
							showScript(o.sp,o.s);
						}
					}
				}
			});
			
			badLogFrame.setContentPane(logScrollPane);
			badLogFrame.pack();
			badLogFrame.setSize(300, 400);
			badLogFrame.setLocation(900,200);
			badLogFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		}
		for(LogIssue l:logIssueList)
			logListModel.addElement(l);
		if (!badLogFrame.isVisible())
			badLogFrame.setVisible(true);
	}
	/** Clears the log list display. */
	private void clearLogIssues(){
		if (logListModel!=null)
			logListModel.clear();
	}
	/** Converts errors received from the server to LogIssues. */
	private Iterable<LogIssue> toLogIssues(Iterable<Triplet<Script.Type,String[],String>> errors){
		LinkedList<LogIssue> issueList = new LinkedList<LogIssue>();
		for(Triplet<Script.Type,String[],String> p:errors){
			try {
				Pair<ScriptPath,Script> s = dk.getScriptPath(p.first,p.second);
				if (s!=null)
					issueList.add(new LogIssue(s.first,s.second,p.third));
			} catch (Exception e){}
		}
		return issueList;
	}
	
	/** Shows the operator editor. */
	public void showOperatorEditor() {
		operatorEditor.setVisible(true);
	}

	/** Shows a script in swat. */
	public void showScript(ScriptPath sp,Script s){
		if (s.getType()==Script.Type.OperatorBody) {
			operatorEditor.setCustomOperator(s.getCustomOperator());
			showOperatorEditor();
		} else if (s.getType()==Script.Type.WordsocketLabel || s.getType()==Script.Type.WordsocketSuffix) {
			verbEditor.setVerb(sp.getVerb());
			verbEditor.showSentenceDisplayEditor(sp,s);
		} else
			verbEditor.setScriptPath(sp,s);
	}
	
	/** Shows the copyright editor. */
	private void showCopyrightEditor() {
		copyrightEditor.setVisible(true);
	}
	
	private void editCopyrightText(final String newText){
		final String oldText = Utils.emptyIfNull(dk.getCopyright());
		if (oldText.equals(newText))
			return;
		dk.setCopyright(newText);
		
		new UndoableAction(Swat.this,false,"edit copyright"){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				dk.setCopyright(newText);
				copyrightEditor.setText(newText);
				showCopyrightEditor();
			}
			@Override
			public void myUndo() {
				dk.setCopyright(oldText);
				copyrightEditor.setText(oldText);
				showCopyrightEditor();
			}
		};
	}
	
	private void earlyInitialization() {
		myFrame = new JFrame();
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myFrame.setSize(1000, 750);
		myFrame.setVisible(true);
		
		try {
			OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[])null));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener(){
			public void eventDispatched(AWTEvent event) {
				if (event instanceof InputEvent) {
					controlPressed = (((InputEvent)event).getModifiers() & keyMask)!=0;
					altPressed = ((InputEvent)event).isAltDown();
				}
			}
		},AWTEvent.MOUSE_EVENT_MASK|AWTEvent.KEY_EVENT_MASK);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher(){
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
					if ((e.getModifiers() & keyMask)!=0 && owner!=null) {
						if (!(owner instanceof JTextComponent) && dk!=null)
							cutMenuItem.getAction().actionPerformed(getActionEvent(owner,e));
						else cutTextAction.actionPerformed(getActionEvent(owner,e));
						return true;
					}
					break;
				case KeyEvent.VK_C:
					if ((e.getModifiers() & keyMask)!=0 && owner!=null){
						if (!(owner instanceof JTextComponent) && dk!=null)
							copyMenuItem.getAction().actionPerformed(getActionEvent(owner,e));
						else copyTextAction.actionPerformed(getActionEvent(owner,e));
						return true;
					}
					break;
				case KeyEvent.VK_V:					
					if ((e.getModifiers() & keyMask)!=0 && owner!=null){
						if (!(owner instanceof JTextComponent) && dk!=null) 
							pasteMenuItem.getAction().actionPerformed(getActionEvent(owner,e));
						else pasteTextAction.actionPerformed(getActionEvent(owner,e));
						return true;
					}
					break;
					// Close the window if ctrl+W o ctrl+Q is pressed  
				case KeyEvent.VK_W:
					if ((e.getModifiers() & keyMask)!=0){
						if (w==myFrame && dk!=null)
							quitDialog();
						else 
							w.dispose();
						return true;
					}
					break;
				case KeyEvent.VK_Q:
					if ((e.getModifiers() & keyMask)!=0 && !quitting){
						quitDialog();
						return true;
					} else if (dk==null)
						w.dispose();
					break;
				case KeyEvent.VK_ESCAPE:				
					if (w==myFrame) {
						MenuElement[] me = MenuSelectionManager.defaultManager().getSelectedPath();
						MenuSelectionManager.defaultManager().clearSelectedPath();
						return me.length>0;
					} 
					break;
				case KeyEvent.VK_ENTER:
					if (MenuSelectionManager.defaultManager().getSelectedPath().length>0)
						return true;
					break;
				}
				
				if (dk==null) // don't process other events
					return false;
				
				switch(e.getKeyCode()){
				case KeyEvent.VK_S:
				case KeyEvent.VK_Z:
				case KeyEvent.VK_Y:
					if ((e.getModifiers() & keyMask)!=0)
						menuAcceleratorEventProcessed = false;
					break;
				default:;
				}
				return false;
			}
			ActionEvent getActionEvent(Component owner,KeyEvent e){
				return new ActionEvent(owner,ActionEvent.ACTION_PERFORMED,"",e.getWhen(),e.getModifiers());
			}
		});
		
	}
	
	private void incompatibleVersionException(IncompatibleVersionException re){
		Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
		Utils.showErrorDialog(getMyFrame(), "I'm not compatible with the server version: "+re.serverVersion+".\n"+
				                            "Please, use another version of me in order to connect.","Connection error");
	}
	
	private void init() {

		// We want the tooltips to remain for long on the screen.
		ToolTipManager.sharedInstance().setDismissDelay(3600000);
		
		myFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {	quitDialog(); }
		});
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){	logout();	}
		});

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(new KeyEventPostProcessor(){
			public boolean postProcessKeyEvent(KeyEvent e) {
				if (e.getID() != KeyEvent.KEY_PRESSED ||
					e.getSource() instanceof EventInfo
					|| dk==null)
					return false;

				if (menuAcceleratorEventProcessed)
					return false;
				
				switch(e.getKeyCode()){
				case KeyEvent.VK_Z:
					if ((e.getModifiers() & keyMask)!=0){
						undoMenuItem.doClick();
						return true;
					}
					break;
				case KeyEvent.VK_Y:
					if ((e.getModifiers() & keyMask)!=0){
						redoMenuItem.doClick();
						return true;
					}
					break;
				case KeyEvent.VK_S:
					if ((e.getModifiers() & keyMask)!=0){
						saveMenuItem.doClick();
						return true;
					}
					break;
				default:;					
				}
				return false;
			}
		});		

		loadClip("add.aiff");
		loadClip("copy.aiff");
		loadClip("cut.aiff");
		loadClip("delete.aiff");
		loadClip("editOperator.aiff");
		loadClip("paste.aiff");
		loadClip("save.aiff");
		loadClip("eeew.wav");
		
		soundOn = true;
		JMenuItem aboutSwatMenuItem = new JMenuItem("About...");
		aboutSwatMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String versionLabel = "Swat version " + String.format("%04d", SharedConstants.version);
		
				JOptionPane.showMessageDialog(myFrame.getContentPane(),
						versionLabel + "\nCopyright \u00A9 2009 Storytron", "About Swat",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		JMenuItem soundsMenuItem = new JMenuItem("Sounds...");
		soundsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				if (JOptionPane.showConfirmDialog(myFrame, "Keep sounds on?",
						"Sounds", JOptionPane.YES_NO_OPTION) == 0)
					soundOn = true;
				else
					soundOn = false;
			}
		});
		
		// TODO New menu item
		newMenuItem = new JMenuItem("New");
		newMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stwfile = null;
				
				stwfile = createNewStoryworld();
				if (stwfile != "") {
					openStoryworld(stwfile); 
					updateEditor();
				}
			}
		});
		
		openMenuItem = new JMenuItem("Open");
		openMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				openStoryworld(stwfile);
				updateEditor();
			}
		});
		
		saveMenuItem = new JMenuItem("Save");
		saveMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				menuAcceleratorEventProcessed = true;
				
				writeStoryworld(dk.getFile());
				undoManager.saving();
				updateFrameTitle();
			}
		});
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', keyMask, false));

		JMenuItem saveAsMenuItem = new JMenuItem("Save as...");
		saveAsMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				menuAcceleratorEventProcessed = true;
				
				writeStoryworld(null);
				undoManager.saving();
				updateFrameTitle();
			}
		});

		JMenuItem closeMenuItem = new JMenuItem("Quit");
		closeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quitDialog();
			}
		});
		closeMenuItem.setAccelerator(KeyStroke
				.getKeyStroke('W', keyMask, false));

		undoMenuItem = new JMenuItem("Undo");
		undoMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				menuAcceleratorEventProcessed = true;

				undoManager.undo();
			}
		});
		undoMenuItem
		.setAccelerator(KeyStroke.getKeyStroke('Z', keyMask, false));
		
		undoMenuItem.setEnabled(false);

		redoMenuItem = new JMenuItem("Redo");
		redoMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				menuAcceleratorEventProcessed = true;

				undoManager.redo();
			}
		});
		redoMenuItem
		.setAccelerator(KeyStroke.getKeyStroke('Y', keyMask, false));
		redoMenuItem.setEnabled(false);
		
		cutMenuItem = new JMenuItem();
		cutMenuItem.setAction(new AbstractAction("Cut") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				java.awt.Window w=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				if (w==myFrame)
					verbEditor.cutScriptSelection();
				else if (w==operatorEditor)
					operatorEditor.scriptEditor.cutSelection();
				else if (w==verbEditor.sentenceDisplayEditor)
					verbEditor.sentenceDisplayEditor.scriptEditor.cutSelection();
			}
		});
		cutMenuItem.setAccelerator(KeyStroke.getKeyStroke('X', keyMask, false));

		copyMenuItem = new JMenuItem();
		copyMenuItem.setAction(new AbstractAction("Copy") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				java.awt.Window w=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				if (w==myFrame && verbEditor.getScriptBeingEdited()!=null) {
					verbEditor.copyScriptSelection();
					Swat.playSound("copy.aiff");
				} else if (w==operatorEditor) {
					operatorEditor.scriptEditor.copySelection();
					Swat.playSound("copy.aiff");
				} else if (w==verbEditor.sentenceDisplayEditor) {
					verbEditor.sentenceDisplayEditor.scriptEditor.copySelection();
					Swat.playSound("copy.aiff");
				}
			}
		});
		copyMenuItem
				.setAccelerator(KeyStroke.getKeyStroke('C', keyMask, false));

		pasteMenuItem = new JMenuItem();
		pasteMenuItem.setAction(new AbstractAction("Paste") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				java.awt.Window w=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				if (w==myFrame && verbEditor.getScriptBeingEdited()!=null)
					verbEditor.pasteNode(clipboard);
				else if (w==operatorEditor)
					operatorEditor.scriptEditor.pasteNode(clipboard);
				else if (w==verbEditor.sentenceDisplayEditor)
					verbEditor.sentenceDisplayEditor.scriptEditor.pasteNode(clipboard);
			}
		});
		pasteMenuItem.setAccelerator(KeyStroke
				.getKeyStroke('V', keyMask, false));
		
		copyOption = new JMenuItem("Copy Option");
		copyOption.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playSound("copy.aiff");
				verbEditor.copyOption();
			}
		});
		copyOption.setEnabled(false);

		pasteOption = new JMenuItem("Paste Option");
		pasteOption.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				verbEditor.pasteOption();
			}
		});
		pasteOption.setEnabled(false);

		pasteOptionLink = new JMenuItem("Paste Option Link");
		pasteOptionLink.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				verbEditor.pasteOptionLink();
			}
		});
		pasteOptionLink.setEnabled(false);
		
		copyRole = new JMenuItem("Copy Role");
		copyRole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playSound("copy.aiff");
				verbEditor.copyRole();
			}
		});
		copyRole.setEnabled(false);

		pasteRole = new JMenuItem("Paste Role");
		pasteRole.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				verbEditor.pasteRole();
			}
		});
		pasteRole.setEnabled(false);

		pasteRoleLink = new JMenuItem("Paste Role Link");
		pasteRoleLink.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				verbEditor.pasteRoleLink();
			}
		});
		pasteRoleLink.setEnabled(false);

		comeFromLizardMenuItem = new JMenuItem("ComeFrom Lizard");
		comeFromLizardMenuItem.setToolTipText(Utils.toHtmlTooltipFormat("Lists all Roles that include this Verb as an Option. Double-click on the list item to jump straight to it."));
		comeFromLizardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				comeFromLizard = new ComesFromLizard(Swat.this, dk.getVerbs());
				comeFromLizardMenuItem.setEnabled(false);
			}
		});
		comeFromLizardMenuItem.setEnabled(true);

		JMenuItem notesSearchLizardMenuItem = new JMenuItem("Notes Search Lizard");
		notesSearchLizardMenuItem.setToolTipText(Utils.toHtmlTooltipFormat("Searches notes through your Scripts for a specified text string."));
		notesSearchLizardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				noteSearch();
			}
		});
		notesSearchLizardMenuItem.setEnabled(true);

		JMenuItem roleSearchLizardItem = new JMenuItem("Role Search Lizard");
		roleSearchLizardItem.setToolTipText(Utils.toHtmlTooltipFormat("Searches Roles through your Verbs for a specified text string."));
		roleSearchLizardItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				roleSearch();
			}
		});
		roleSearchLizardItem.setEnabled(true);

		JMenuItem operatorSearchLizardMenuItem = new JMenuItem("Operator Search Lizard");
		operatorSearchLizardMenuItem.setToolTipText(Utils.toHtmlTooltipFormat("Displays a list of the Operators in all the Scripts."));
		operatorSearchLizardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SearchLizard(Swat.this);
			}
		});
		operatorSearchLizardMenuItem.setEnabled(true);

		JMenuItem sniffyLizardMenuItem = new JMenuItem("Sniffy Lizard");
		sniffyLizardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final LinkedList<LogIssue> errors=dk.checkScripts(null,true);
				if (errors.isEmpty())
					Utils.showOptionDialog(myFrame,	"No errors were found.", "Sniffy Lizard",
							JOptionPane.OK_OPTION,JOptionPane.INFORMATION_MESSAGE,null,new Object[]{"Dismiss"},"Dismiss");
				else
					showLogIssues(errors);
			}
		});
		
		rehearsalLizardMenuItem = new JMenuItem("Rehearsal Lizard");
		rehearsalLizardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int stageTraits = dk.getStageTraits().size();
				if (stageTraits==0) {
					Verb v = findVerbUsingWordsocketType(Operator.Type.StageTrait);
					if (v!=null) {
						Utils.showErrorDialog(getMyFrame(), "I cannot run rehearsals because there are no StageTraits declared while verb\n\""+v.getLabel()+"\" has a wordsocket of that type.\n"+
                            "Please, declare some StageTraits or modify your verb to run a rehearsal.","Rehearsal error");
						return;
					}
				}
				
				int propTraits = dk.getPropTraits().size();
				if (propTraits==0) {
					Verb v = findVerbUsingWordsocketType(Operator.Type.PropTrait);
					if (v!=null) {
						Utils.showErrorDialog(getMyFrame(), "I cannot run rehearsals because there are no PropTraits declared while verb\n\""+v.getLabel()+"\" has a wordsocket of that type.\n"+
                            "Please, declare some PropTraits or modify your verb to run a rehearsal.","Rehearsal error");
						return;
					}
				}
				
				rehearsalLizardMenuItem.setEnabled(false);
				try {
					logout();
					if (!SharedConstants.useRemoteInterface) {
						if (swatJanus==null)
							swatJanus = new Janus();
						if (logDownloader!=null)
							logDownloader.restart(swatJanus, logDownloader.getSessionID());
						// Log in to the server 
						rehearsalSessionID = swatJanus.login(SharedConstants.REMOTE_INTERFACE_VERSION,login, password);

						// Send a copy of Deikto to the server
						try {
							((LocalJanus)swatJanus).copyLocalDeikto(dk, rehearsalSessionID);
							startRehearsalLizard();
						} catch(BadStoryworldException ex) { 
							clearLogIssues();
							LinkedList<LogIssue> localErrors = dk.checkScripts(null,false);
							if (localErrors.isEmpty())
								showLogIssues(toLogIssues(ex.errors));
							else
								showLogIssues(localErrors);
						}

					} else {
						if (swatJanus==null) {
							try {
								swatJanus = (SwatRemote)
								Naming.lookup(SharedConstants.getRMIServiceString(SharedConstants.getDefaultServiceNum()));
							} catch (ConnectException ce) {
								// Try to connect through http tunnelling
								try {
									RMISocketFactory.setSocketFactory(new sun.rmi.transport.proxy.RMIHttpToCGISocketFactory());
									swatJanus = (SwatRemote)
									Naming.lookup(SharedConstants.getRMIServiceString(SharedConstants.getDefaultServiceNum()));
								} catch (Exception e2) {
									throw new RuntimeException(e2);
								}
							}
						}
						if (logDownloader!=null)
							logDownloader.restart(swatJanus, logDownloader.getSessionID());
						// Login to the server
						// TODO: Add real user logins
						rehearsalSessionID = swatJanus.login(SharedConstants.REMOTE_INTERFACE_VERSION,login, password);

						//	Send a copy of Deikto the server
						try {
							swatJanus.loadDeiktoFromZip(rehearsalSessionID,Utils.compressDeikto(dk));
							startRehearsalLizard();
						} catch(BadStoryworldException ex) {
							clearLogIssues();
							LinkedList<LogIssue> localErrors = dk.checkScripts(null,false);
							if (localErrors.isEmpty())
								showLogIssues(toLogIssues(ex.errors));
							else
								showLogIssues(localErrors);
						}
					}
				} catch (IncompatibleVersionException re) {
					incompatibleVersionException(re);
					rehearsalLizardMenuItem.setEnabled(true);
				} catch (NotBoundException re) {
					re.printStackTrace();
					Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
					Utils.showErrorDialog(myFrame, "I'm sorry, but I'm not able to run rehearsals.\nThe server is unavailable.\nSwat version: "+SharedConstants.version,"Connection error");
					rehearsalLizardMenuItem.setEnabled(true);
				} catch (final RemoteException re) {
					re.printStackTrace();
					Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
					Utils.showErrorDialog(getMyFrame(), "Cannot connect to the engine server.","Connection error",re);
					rehearsalLizardMenuItem.setEnabled(true);
				} catch (Exception re){ 
					re.printStackTrace();
					Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
					Utils.showErrorDialog(getMyFrame(), "Cannot run rehearsals.","Connection error",re);
					rehearsalLizardMenuItem.setEnabled(true);
				}
				return;
			}
		});
		rehearsalLizardMenuItem.setEnabled(false);

		storytellerLizardMenuItem = new JMenuItem("Storyteller Lizard");
		storytellerLizardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Utils.setCursor(myFrame,Cursor.WAIT_CURSOR);
				storytellerLizardMenuItem.setEnabled(false);
				rehearsalLizardMenuItem.setEnabled(false);

				// Remove the previous session if any.
				new Thread() {
					public void run() {
						try {
							logout();
							if (SharedConstants.useRemoteInterface){
								try {
									swatJanus = (SwatRemote)
									Naming.lookup(SharedConstants.getRMIServiceString(SharedConstants.getDefaultServiceNum()));
								} catch (NotBoundException re) {
									re.printStackTrace();
									SwingUtilities.invokeLater(new Runnable(){
										public void run() {
											storytellerLizardMenuItem.setEnabled(true);
											rehearsalLizardMenuItem.setEnabled(true);
											Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
											Utils.showErrorDialog(myFrame, "I'm sorry, but I'm not able to play the storyworld.\nThe server is unavailable.\nSwat version: "+SharedConstants.version,"Connection error");
										}
									});
									return;
								} catch (ConnectException ce) {
									// Try to connect through http tunnelling
									try {
										RMISocketFactory.setSocketFactory(new sun.rmi.transport.proxy.RMIHttpToCGISocketFactory());
										swatJanus = (SwatRemote)
											Naming.lookup(SharedConstants.getRMIServiceString(SharedConstants.getDefaultServiceNum()));
									} catch (Exception e2) {
										throw new RuntimeException(e2);
									}
								} catch (final RemoteException re) {
									re.printStackTrace();
									SwingUtilities.invokeLater(new Runnable(){
										public void run() {
											storytellerLizardMenuItem.setEnabled(true);
											rehearsalLizardMenuItem.setEnabled(true);
											Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
											Utils.showErrorDialog(getMyFrame(), "Cannot connect to the engine server.","Connection error",re);
										}
									});
									return;
								}
							} else {
								if (swatJanus==null)
									swatJanus = new Janus();
								System.out.println("ok, started Janus");
							}

							// Login to the server	
							startStorytellerLizard();

						} catch (Exception re) {
							re.printStackTrace();
							storytellerLizardMenuItem.setEnabled(true);
							rehearsalLizardMenuItem.setEnabled(true);
							Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
							re.printStackTrace();
							Utils.showErrorDialog(getMyFrame(), "I wont be able to play the storyworld.\nThere was an error when starting the story.\n","Connection error",dk.getFile().getName(),dk.version,re,null);
						}
					}
				}.start();
			}
		});

		JMenuItem logLizardMenuItem = new JMenuItem("Log Lizard");
		logLizardMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openLogLizard(myFrame);
			}
		});

		verbEditorMenuItem = new JMenuItem("Verb Editor");
		verbEditorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEditorInFocus(EditorEnum.VerbEditorHasFocus);
			}
		});
		verbEditorMenuItem.setEnabled(false);

		actorEditorMenuItem = new JMenuItem("Actor Editor");

		actorEditorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEditorInFocus(EditorEnum.ActorEditorHasFocus);
			}
		});
		actorEditorMenuItem.setEnabled(true);

		relationshipEditorMenuItem = new JMenuItem("Relationship Editor");
		relationshipEditorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEditorInFocus(EditorEnum.RelationshipEditorHasFocus);
			}
		});
		relationshipEditorMenuItem.setEnabled(true);
		
		propEditorMenuItem = new JMenuItem("Prop Editor");
		propEditorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEditorInFocus(EditorEnum.PropEditorHasFocus);
			}
		});
		propEditorMenuItem.setEnabled(true);

		stageEditorMenuItem = new JMenuItem("Stage Editor");
		stageEditorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEditorInFocus(EditorEnum.StageEditorHasFocus);
			}
		});
		stageEditorMenuItem.setEnabled(true);

		JMenuItem operatorMenuItem = new JMenuItem("Operator editor");
		operatorMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showOperatorEditor();
			}
		});

		JMenuItem copyrightMenuItem = new JMenuItem("Copyrights editor");
		copyrightMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showCopyrightEditor();
			}
		});
		
		
		relationshipsMenuItem = new JMenuItem("Relationships");
		relationshipsMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (relationshipSettings==null) { 
					relationshipSettings = new RelationshipSettings(Swat.this);
					Point p = getMyFrame().getLocationOnScreen();
					p.x+=200;
					p.y+=100;
					relationshipSettings.setLocation(p);
				}
				relationshipSettings.setVisible(true);
			}
		});

		JMenuItem terminationMenuItem = new JMenuItem("Termination");
		terminationMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				terminationSettings=null;
				if (terminationSettings==null) { 
					terminationSettings = new TerminationSettings(Swat.this);
					
					Point p = getMyFrame().getLocationOnScreen();
					p.x+=200;
					p.y+=100;
					terminationSettings.setLocation(p);
				}
				terminationSettings.refresh();
				terminationSettings.setVisible(true);
			}
		});

		JMenu swatMenu = new JMenu("Swat");
		swatMenu.setMnemonic(KeyEvent.VK_S);
		swatMenu.add(aboutSwatMenuItem);
		swatMenu.add(soundsMenuItem);
		swatMenu.addSeparator();
		swatMenu.add(closeMenuItem);

		JMenu storyworldMenu = new JMenu("Storyworld");
		storyworldMenu.setMnemonic(KeyEvent.VK_W);
		storyworldMenu.add(relationshipsMenuItem);
		storyworldMenu.add(terminationMenuItem);
		storyworldMenu.addSeparator();
		storyworldMenu.add(newMenuItem);
		storyworldMenu.add(openMenuItem);
		storyworldMenu.addSeparator();
		storyworldMenu.add(saveMenuItem);
		storyworldMenu.add(saveAsMenuItem);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		editMenu.add(undoMenuItem);
		editMenu.add(redoMenuItem);
		editMenu.addSeparator();
		editMenu.add(cutMenuItem);
		editMenu.add(copyMenuItem);
		editMenu.add(pasteMenuItem);
		editMenu.addSeparator();
		editMenu.add(copyOption);
		editMenu.add(pasteOption);
		editMenu.add(pasteOptionLink);
		editMenu.addSeparator();
		editMenu.add(copyRole);
		editMenu.add(pasteRole);
		editMenu.add(pasteRoleLink);

		JMenu lizardsMenu = new JMenu("Lizards");
		lizardsMenu.setMnemonic(KeyEvent.VK_L);
		lizardsMenu.add(storytellerLizardMenuItem);
		lizardsMenu.add(logLizardMenuItem);
		lizardsMenu.add(rehearsalLizardMenuItem);
		lizardsMenu.addSeparator();
		lizardsMenu.add(comeFromLizardMenuItem);
		lizardsMenu.add(roleSearchLizardItem);
		lizardsMenu.add(operatorSearchLizardMenuItem);
		lizardsMenu.add(notesSearchLizardMenuItem);
		lizardsMenu.addSeparator();
		lizardsMenu.add(sniffyLizardMenuItem);
		
		JMenu editorsMenu = new JMenu("Editors");
		editorsMenu.setMnemonic(KeyEvent.VK_D);
		editorsMenu.add(verbEditorMenuItem);
		editorsMenu.add(actorEditorMenuItem);
		editorsMenu.add(stageEditorMenuItem);
		editorsMenu.add(propEditorMenuItem);
		editorsMenu.addSeparator();
		editorsMenu.add(relationshipEditorMenuItem);
		editorsMenu.addSeparator();
		editorsMenu.add(operatorMenuItem);
		editorsMenu.add(copyrightMenuItem);

		menuBar = new JMenuBar();
		menuBar.add(swatMenu);
		menuBar.add(storyworldMenu);
		menuBar.add(editMenu);
		menuBar.add(lizardsMenu);
		menuBar.add(editorsMenu);

		myFrame.setJMenuBar(menuBar);

		verbEditor = new VerbEditor(this);
		relationshipEditor = new RelationshipEditor(this);
		actorEditor = new ActorEditor(this);
		propEditor = new PropEditor(this);
		stageEditor = new StageEditor(this);

		operatorEditor = new OperatorEditor(this);
		operatorEditor.pack();
		operatorEditor.setLocationRelativeTo(myFrame);
		
		copyrightEditor = new CopyrightEditor(myFrame,true) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onTextChange(String newText) {
				editCopyrightText(newText);
			}
		};
		copyrightEditor.setLocationRelativeTo(myFrame);
		copyrightEditor.setTitle("Copyrights");
		
		loadLoginData();
	} // End init()
	
	/** Returns the first verb found having an active wordsocket with the given type. */
	private Verb findVerbUsingWordsocketType(Operator.Type t) {
		for(Verb v:dk.getVerbs()) { 
			for(int i=0;i<Sentence.MaxWordSockets;i++) {
				if (v.isWordSocketActive(i) && v.getWordSocketType(i)==t)
					return v;
			}
		}
		return null;
	}

	private void loadLoginData(){
		// Load generated login data
		File storedLogin = new File("storedLogin");
		BufferedReader lineReader = null;
		BufferedWriter lineWriter = null;


		try {
			if (storedLogin.exists() && storedLogin.canRead())
			{
				// Read in the file
				lineReader = new BufferedReader(new FileReader(storedLogin));
				login = lineReader.readLine();
				password = lineReader.readLine();

			} else {
				// create the user id as a guid
				
				UUID loginObj = UUID.randomUUID();
				login = loginObj.toString() + '\n';
				password = "pw";  // Passwords are not checked yet
				
				//write the user id to the file
				storedLogin.createNewFile();
				lineWriter = new BufferedWriter(new FileWriter(storedLogin));
				lineWriter.write(login);
				lineWriter.write(password);
			}
		}
		catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
		catch (IOException ioe){
			ioe.printStackTrace();
		}
		finally {
			try {
				if (lineReader!= null)
					lineReader.close();
				if (lineWriter!= null)
					lineWriter.close();

			}
			catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

//************************************************************************
	/** Tells if the control key is being pressed right now. */
	public static boolean isControlDown(){ return controlPressed; }
	/** Tells if the alt key is being pressed right now. */
	public static boolean isAltDown(){ return altPressed; }
	
	/** Refresh the relationship settings if they are visible. */
	public void refreshRelationshipSettings(){
		if (relationshipSettings!=null && relationshipSettings.isVisible())
			relationshipSettings.refresh();
	}
	
	/** 
	 * Shows the save dialog if there are pending changes and returns 
	 * true iff we must quit. Otherwise, it never returns (actually quits).
	 * */
	public boolean quit(){
		if (st!=null && st.isVisible()) {
			st.toFront();
			if (st.saveDialog())
				st.dispose();
			else
				return false;
		}
		
		if (undoManager.isSavingNeeded()){
			switch(Utils.showOptionDialog(myFrame,
				"Should I save your storyworld changes?", "Save?",
				JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,new Object[]{"Save changes","Discard changes"},"Save")) {
				case JOptionPane.YES_OPTION:
					if (writeStoryworld(dk.getFile(),false)) {
						logDownloader=null;
						System.exit(0);
					}
					return false;
				case JOptionPane.NO_OPTION:
					logDownloader=null;
					System.exit(0);
					return true;
				default:
					return false;
			} 
		} else {
			logDownloader=null;
			System.exit(0);
			return true;
		}
	}
	
	/** 
	 * Shows the save dialog if there are pending changes and quits.
	 * */
	private boolean quitDialog() {
		quitting=true;		
		boolean res=quit();
		quitting=false;
		return res;
	}
	
	@SuppressWarnings("null")
	public String createNewStoryworld() {
		
		String newStoryworld = "";
		File newFilename = null;
		File resourceDirectory = null;
		
		chooser.setDialogTitle("Create New Storyworld");
		chooser.setFileFilter(swatFileFilter);
		//chooser.setSelectedFile(new File(this.file.getName()));
			
		if (chooser.showSaveDialog(myFrame) == JFileChooser.APPROVE_OPTION) { // Save 
			
			newFilename = Utils.addExtension(chooser.getSelectedFile(), ".stw");
			newStoryworld = newFilename.getName().substring(0, newFilename.getName().indexOf("."));
			Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			resourceDirectory = Utils.getResourceDir(newFilename); // Get the file where images will be saved
		} else { // Cancel 
			
			Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			return newStoryworld;
		}
		
		// Warning if a resource subdirectory already exists for the storyworld they are trying to create  
		if (resourceDirectory.exists()) {
			if (resourceDirectory.isDirectory()) {
				
				JFrame parentComponent = getMyFrame(); 
				
				String message = "You must delete the subdirectory " + resourceDirectory.getName() + " before you can create your new " + newStoryworld + " storyworld.";
				String title = "Resource Subdirectory Already Exists";
					
				JOptionPane.showOptionDialog(
					parentComponent,
					message, 
					title,
					JOptionPane.OK_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null,
					new Object[]{"OK"}, 
					"OK");
					
				newStoryworld = "";
				return newStoryworld;
			}
		}
		
		// TODO Copy starter.stw to selected directory with new filename
		
	/*	File sourceFile = new File("/testdata/starter.stw");
		File destinationFile = newFilename;
		try {
			Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		return newStoryworld;
	}
	
	/** 
	 * Saves current storyworld to the given file. If the file is null
	 * a file chooser is presented to the user.
	 * @return false iff the user cancels. 
	 * */
	public boolean writeStoryworld(File saveFile) {
		
		return writeStoryworld(saveFile, true);
	}
	
	private boolean writeStoryworld(File saveFile, final boolean playSound) {
		
		//  *** Write the XML string to Dictionary.xml ***
		
		// Use the filename passed in or ask the user for a directory and filename
		File file = null;
		if (saveFile != null) file = saveFile; 	
		else { 
			chooser.setFileFilter(swatFileFilter);
			chooser.setSelectedFile(new File(this.file.getName()));
			
			if (chooser.showSaveDialog(myFrame) == JFileChooser.APPROVE_OPTION) {
				
				file = Utils.addExtension(chooser.getSelectedFile(), ".stw");
				Utils.setWorkingDirectory(chooser.getCurrentDirectory());
			} else {
				
				Utils.setWorkingDirectory(chooser.getCurrentDirectory());
				return false;
			}
		}
		
		File rdir = Utils.getResourceDir(file); // Get the file where images will be saved
		
		// Warn the user if we showed the file chooser to him
		// and there is a file with the same name than the resource directory.  
		if (saveFile==null && rdir.exists() && !rdir.isDirectory()){
			switch(JOptionPane.showOptionDialog(getMyFrame(), 
					"The file\n"+Utils.getResourceDir(file).getPath()+"\nalready exists. Saving the storyworld with\nthe choosen name requires deleting this file.", "Warning",
					JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,new Object[]{"Proceed","Cancel"},"Cancel")){
				case JOptionPane.YES_OPTION:
					break;
				default: 
					return false;
			}
		}
		this.file = file;

		// Instantiate the progress dialog. 
		// We will save the storyworld in a separate thread. It takes a while to save.
		// The progress dialog will be modal, so the user cannot mess the storyworld
		// while it is being written.
		final ProgressDialog saveBar = new ProgressDialog(getMyFrame());
		saveBar.setModal(true);
		saveBar.setStatus("Writing storyworld ...");
		saveBar.getBar().setIndeterminate(true);
		final Point result=new Point();

		Thread saveThread = new Thread(){
			@Override
			public void run() {
				try {
					// Create the resource dir. This will reuse any existing directory
					// with the resource directory name (<storyworld>_rsc). If a
					// non-file directory exists with that name, it will be deleted. 
					File fdir=createResourceDir(Swat.this.file);
					// Now we write the resources to the resource directory.
					// If we are writing over the previous copy of the storyworld
					// we will preserve those resources that have not been modified
					// since loaded.
					dk.writeResources(fdir);
					// While writing new resources, some new files could have been
					// created. In the following line we retrieve again all of the 
					// referenced file names.
					LinkedList<String> resourceNames = dk.getResourceNames();
					// Now we will delete the previously referenced files that are no longer used.
					// The previously referenced files were stored in dkResourceNames when
					// loading the storyworld, or when last saving it.
					removeNonReferencedFiles(fdir,dkResourceNames,resourceNames);
					// Nos whe set the currently referenced files in dkResourceNames, so they are
					// available next time we save.
					dkResourceNames = resourceNames;
					// If there were no resources to write we want to remove the resource
					// directory if it is empty.
					removeIfEmpty(fdir);
					// Now we set the storyworld file to be the one selected by the user.
					// This file will be used to retrieve the images from the resource directory.
					dk.setFile(Swat.this.file);
					FileOutputStream myFileWriter = new FileOutputStream(Swat.this.file);
					// We write the storyworld master file.
					dk.writeXML(myFileWriter);
					myFileWriter.flush();
					myFileWriter.close();
					if (playSound)
						playSound("save.aiff");
					
					// This is a trick to tell the event thread if we saved or not successfully.
					result.x=1;
				} catch (Deikto.WritingException e) {
					switch(e.t){
					case ResourceDirFileDeletion:
						Utils.showErrorDialog(getMyFrame(), "Can not delete regular file\n"+e.f.getPath()+"\nto create a directory with the same name.","File error",e);
						break;
					case ResourceDirCreation:
						Utils.showErrorDialog(getMyFrame(), "Can not create directory\n"+e.f.getPath()+"\nMost likely you don't have permissions to\nwrite the parent directory.","File error",e);
						break;
					case ResourceDeletion:
						Utils.showErrorDialog(getMyFrame(), "Can not delete unreferenced file\n"+e.f.getPath()+"\nin resource directory. Most likely you don't have\npermissions to write the parent directory.","File error",e);
						break;
					case ResourceDirDeletion:
						Utils.showErrorDialog(getMyFrame(), "Can not delete empty resource directory\n"+e.f.getPath()+"\nMost likely you don't have permissions to\nwrite the parent directory.","File error",e);
						break;
					}
				} catch (java.io.FileNotFoundException e) {
					e.printStackTrace();
					if (Swat.this.file.exists())
						Utils.showErrorDialog(getMyFrame(), "There was an error when accessing the file\n"+Swat.this.file.getPath(),"File error",e);
					else
						Utils.showErrorDialog(getMyFrame(), "There was an error when creating the file\n"+Swat.this.file.getPath(),"File error",e);
				} catch (java.io.IOException e) {
					e.printStackTrace();
					Utils.showErrorDialog(getMyFrame(), "There was an error when writing the storyworld.","File error",e);
				} finally {
					// Whatever the reason we leave the try block, we want the
					// progress dialog to close.
					saveBar.dispose();
					Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
				}
			}
		};
		saveThread.start();
		Utils.setCursor(myFrame,Cursor.WAIT_CURSOR);
		// Because saveBar is a modal dialog, this call will block until the
		// dialog is disposed by the thread we just started.
		saveBar.setVisible(true);

		// result.x is set to 1 by the thread we created if the writing operation
		// was successful.
		return result.x==1;
	}

	/** 
	 * Erases non referenced files in the directory fdir. 
	 * @param oldReferenced contains the files that can be deleted. This is to prevent
	 *        deleting user files he might want to keep. 
	 * */
	private void removeNonReferencedFiles(File fdir,LinkedList<String> oldReferenced,LinkedList<String> newReferenced) 
					throws WritingException {
		for(String r:oldReferenced) {
			if (!newReferenced.contains(r)) {
				File f = new File(fdir,r);
				System.out.println("Removing unreferenced file "+f.getAbsolutePath());
				recursiveDelete(f);
			}
		}
	}
	
	/** Deletes a file f. If f is a directory removes its contents too.  */
	private void recursiveDelete(File f) throws WritingException {
		if (f.isDirectory()){
			for(File c:f.listFiles())
				recursiveDelete(c);
		}
		if (!f.delete() && f.exists())
			throw new WritingException(WritingException.Type.ResourceDeletion,f);
	}

	/** Removes fdir if it is empty. */
	private void removeIfEmpty(File fdir) throws WritingException {
		if (!fdir.delete() && fdir.exists()) {
			File[] lf = fdir.listFiles();
			if (lf!=null && lf.length==0)
				throw new WritingException(WritingException.Type.ResourceDirDeletion,fdir);
		}
	}
	
	/** Creates the directory where resources will be stored. 
	 * @param stwfile is the file were the storyworld will be saved. */
	private File createResourceDir(File stwfile) throws WritingException {
		File fdir=Utils.getResourceDir(stwfile);
		if (fdir.exists() && !fdir.isDirectory())
			if (!fdir.delete())
				throw new WritingException(WritingException.Type.ResourceDirFileDeletion,fdir);
		if (!fdir.exists()) 
			if (!fdir.mkdir())
				throw new WritingException(WritingException.Type.ResourceDirCreation,fdir);
		return fdir;
	}

	private void logout(){
		if (st!=null)
			st.logout(true);
	}
	
//************************************************************************
	public EditorEnum getEditorInFocus(){ return editorInFocus; }
	
	public void setEditorInFocus(EditorEnum ed){
		// CC commented out the next line because it prevents returning to an editor from Rehearsal
		if (editorInFocus == ed) return;

		switch(editorInFocus){
		case VerbEditorHasFocus:
			verbEditorMenuItem.setEnabled(true);
			break;
		case ActorEditorHasFocus:
			actorEditorMenuItem.setEnabled(true);
			break;
		case PropEditorHasFocus:
			propEditorMenuItem.setEnabled(true);
			break;
		case StageEditorHasFocus:
			stageEditorMenuItem.setEnabled(true);
			break;
		case RelationshipEditorHasFocus:
			relationshipEditorMenuItem.setEnabled(true);
			break;
		default:;
		}
		
		editorInFocus = ed;
		myFrame.getContentPane().removeAll();
		switch(ed){
		case VerbEditorHasFocus:
			enableVerbMenus();
			myFrame.getContentPane().add(verbEditor.getMyPanel(),BorderLayout.CENTER);
			verbEditorMenuItem.setEnabled(false);
			break;
		case ActorEditorHasFocus:
			disableVerbMenus();
			myFrame.getContentPane().add(actorEditor.getMyPanel(),BorderLayout.CENTER);
			actorEditorMenuItem.setEnabled(false);
			break;
		case RelationshipEditorHasFocus:
			disableVerbMenus();
			myFrame.getContentPane().add(relationshipEditor,BorderLayout.CENTER);
			relationshipEditorMenuItem.setEnabled(false);
//			relationshipEditor.relaxRelationships();
			break;
		case PropEditorHasFocus:
			disableVerbMenus();
			myFrame.getContentPane().add(propEditor.getMyPanel(),BorderLayout.CENTER);
			propEditorMenuItem.setEnabled(false);
			break;
		case StageEditorHasFocus:
			disableVerbMenus();
			myFrame.getContentPane().add(stageEditor.getMyPanel(),BorderLayout.CENTER);
			stageEditorMenuItem.setEnabled(false);
			break;
		default:
			System.out.println("Swat.setEditor: Unknown editor: "+ed.name());
		}
		refresh();
		myFrame.validate();
		myFrame.repaint();
	}
	public void refresh(){
		switch(editorInFocus){
		case VerbEditorHasFocus:
				verbEditor.refresh();
				break;
		case ActorEditorHasFocus:			
			actorEditor.refresh();
			break;
		case RelationshipEditorHasFocus:
			relationshipEditor.refresh();
			break;
		case PropEditorHasFocus:
			propEditor.refresh();
			break;
		case StageEditorHasFocus:
			stageEditor.refresh();
			break;
		default:
			System.out.println("Swat.refresh: Unknown editor: "+editorInFocus.name());
		}
	}
//**********************************************************************
	public void disableVerbMenus() {
		cutMenuItem.setEnabled(false);
		copyMenuItem.setEnabled(false);
		pasteMenuItem.setEnabled(false);
		copyOption.setEnabled(false);
		pasteOption.setEnabled(false);
		pasteOptionLink.setEnabled(false);
		copyRole.setEnabled(false);
		pasteRole.setEnabled(false);
		pasteRoleLink.setEnabled(false);
	}

//**********************************************************************
	public void enableVerbMenus() {
		cutMenuItem.setEnabled(true);
		copyMenuItem.setEnabled(true);
		pasteMenuItem.setEnabled(true);
		verbEditor.updateSwatMenu();
	}

//**********************************************************************
	public void enableLizards() {
		rehearsalLizardMenuItem.setEnabled(true);
	}

	public void openLogLizard(Frame owner){
		if (logLizard!=null && logLizard.getOwner()!=owner)
			logLizard.dispose();
		
		try {
			if (logDownloader==null) {
				logDownloader = new LogDownloaderThread(swatJanus);
				logDownloader.start();
				logManager = new LogDownloadManager(logDownloader);
			}
			logLizard=new LogLizard(owner,logManager);
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
//***********************************************************************
	private void noteSearch() {
		JFrame noteSearchFrame = new JFrame("Search Notes");
		final ArrayList<Triplet<ScriptPath,Script,String>> notes = new ArrayList<Triplet<ScriptPath,Script,String>>();
		final TextField searchField = new TextField(18);

		final JScrollPane notesScrollPane = new JScrollPane();

		Action searchAction = new AbstractAction("search") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				final String searchText = searchField.getText();
				if (searchText != null) {
					notes.clear();
					dk.traverseScripts(new Script.NodeTraverser(){
						public boolean traversing(Script s, Node n) {
							if (Utils.containsIgnoreCase(n.getDescription(),searchText))
								notes.add(new Triplet<ScriptPath,Script,String>(new ScriptPath(verb,role,option),s,n.getDescription()));
							return true;
						}
					});
					displayNotes(notesScrollPane,notes);
				}
			}
		};
		searchField.setText("");
		searchField.setColumns(18);
		searchField.setAction(searchAction);

		JPanel subPanel = new JPanel();
		subPanel.setBackground(noteSearchColor);
		subPanel.add(new JLabel("search term:"));
		subPanel.setMaximumSize(new Dimension(1000, 40));
		subPanel.add(searchField);
		JButton searchButton = new JButton();
		searchButton.setAction(searchAction);
		subPanel.add(searchButton);
		
		JPanel noteSearchPanel = new JPanel(new BorderLayout());
		noteSearchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noteSearchPanel.setBackground(Color.white);
		noteSearchPanel.add(subPanel,BorderLayout.NORTH);
		noteSearchPanel.add(notesScrollPane,BorderLayout.CENTER);

		noteSearchFrame.getContentPane().add(noteSearchPanel,BorderLayout.CENTER);
		noteSearchFrame.setSize(800, 200);
		noteSearchFrame.setLocation(100, 200);
		noteSearchFrame.setVisible(true);
		noteSearchFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private void roleSearch() {
		JFrame noteSearchFrame = new JFrame("Search Roles");
		final LinkedList<RoleEntry> notes = new LinkedList<RoleEntry>();
		final TextField searchField = new TextField(18);
		final JScrollPane notesScrollPane = new JScrollPane();

		Action searchAction = new AbstractAction("search") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				final String searchText = searchField.getText();
				if (searchText != null) {
					notes.clear();
					for(Verb v:dk.getVerbs()){
						for(Role.Link r:v.getRoles()){
							if (Utils.containsIgnoreCase(r.getLabel(),searchText))
								notes.add(new RoleEntry(v,r));
						}
					}
					displayRoles(notesScrollPane,notes);
				}
			}
		};
		searchField.setText("");
		searchField.setColumns(18);
		searchField.setAction(searchAction);

		JPanel subPanel = new JPanel();
		subPanel.setBackground(noteSearchColor);
		subPanel.add(new JLabel("search term:"));
		subPanel.setMaximumSize(new Dimension(1000, 40));
		subPanel.add(searchField);
		JButton searchButton = new JButton();
		searchButton.setAction(searchAction);
		subPanel.add(searchButton);
		
		JPanel noteSearchPanel = new JPanel(new BorderLayout());
		noteSearchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		noteSearchPanel.setBackground(Color.white);
		noteSearchPanel.add(subPanel,BorderLayout.NORTH);
		noteSearchPanel.add(notesScrollPane,BorderLayout.CENTER);

		noteSearchFrame.getContentPane().add(noteSearchPanel,BorderLayout.CENTER);
		noteSearchFrame.setSize(800, 200);
		noteSearchFrame.setLocation(100, 200);
		noteSearchFrame.setVisible(true);
		noteSearchFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);	
	}

//**********************************************************************
	// The underlying data model of the notes table
	public static class NotesTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		private Object[][] data;

		private String[] headings = new String[] { "Script",	"Note" };

		private int cNotes = 0;

		public NotesTableModel(ArrayList<Triplet<ScriptPath,Script,String>> notes, int rows, int cols) {
			int i = 0;
			data = new Object[notes.size() + 1][2];
			for (Triplet<ScriptPath,Script,String> pText : notes) {
				data[i][0] = pText.first.getPath(pText.second);
				data[i][1] = pText.third;
				++i;
			}
			cNotes = i;
		}

		public int getRowCount() {
			return cNotes;
		}

		public int getColumnCount() {
			return data[0].length;
		}

		public Object getValueAt(int row, int column) {
			return data[row][column];
		}

		public void setValueAt(Object value, int row, int col) {
			String strVal = new String(value.toString());
			data[row][col] = (Object) strVal;
			fireTableDataChanged();
		}

		public String getColumnName(int column) {
			return headings[column];
		}
	}

	public void displayNotes(JScrollPane notesScrollPane,final ArrayList<Triplet<ScriptPath,Script,String>> notes) {
		TableModel model = new NotesTableModel(notes, notes.size(), 2);

		final JTable table = new JTable(model);
		final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setToolTipText(Utils.toHtmlTooltipFormat("Double-click to jump directly to this Script."));
		table.setDefaultRenderer(Object.class,renderer);
		table.setCellSelectionEnabled(false);
		table.setRowSelectionAllowed(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount()==2 && table.getSelectedRow()!=-1) {
					setEditorInFocus(Swat.EditorEnum.VerbEditorHasFocus);
					ScriptPath sp=notes.get(table.getSelectedRow()).first;
					Script s=notes.get(table.getSelectedRow()).second;
					Swat.this.showScript(sp,s);
				}
			}
		});

		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		notesScrollPane.setViewportView(table);
	}

	private final static class RoleEntry{
		public Verb v;
		public Role.Link r;
		public RoleEntry(Verb v,Role.Link r){
			this.v=v;
			this.r=r;
		}
		@Override
		public String toString(){
			return v.getLabel()+": "+r.getLabel();
		}
	}
	
	public void displayRoles(JScrollPane notesScrollPane,final LinkedList<RoleEntry> roles) {
		DefaultListModel model = new DefaultListModel();
		for(RoleEntry r:roles)
			model.addElement(r);
		
		final JList list = new JList(model);
		final DefaultListCellRenderer renderer = new DefaultListCellRenderer();
		renderer.setToolTipText(Utils.toHtmlTooltipFormat("Double-click to jump directly to this Role."));
		list.setCellRenderer(renderer);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount()==2 && list.getSelectedIndex()!=-1) {
					setEditorInFocus(Swat.EditorEnum.VerbEditorHasFocus);
					RoleEntry r=roles.get(list.getSelectedIndex());
					if (dk.findVerb(r.v.getLabel())==-1 || r.v.getRole(r.r.getLabel())==null)
						return;
					Swat.this.setEditorInFocus(EditorEnum.VerbEditorHasFocus);
					verbEditor.setVerb(r.v);
					verbEditor.setRole(r.r);
				}
			}
		});

		notesScrollPane.setViewportView(list);
	}

//**********************************************************************
	private static void loadClip(String tFileName){
		AudioClip clip=Applet.newAudioClip(Utils.getSoundPath(tFileName));
		if (clip == null)
			System.out.println("audio clip will not play: "+ tFileName);
		else
			clips.put(tFileName,clip);
	}
	public static void playSound(String tFileName) {
		if (soundOn) {
			AudioClip clip=clips.get(tFileName);
			if (clip==null) loadClip(tFileName);
			if (clip!=null) clip.play();
		}
	}
	
    /** Starts up swat and register a shutdown hook for saving custom properties. */
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
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){	Utils.saveProperties();	}
		});
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				new Swat();		
			}
		});
	}

//*****************************************************************************
	/**
	 * Implements the ComesFromLizard.
	 * This is a windows that sets the verb editor state when the user 
	 * double clicks on it.
	 * 
	 * It also implements a method {@link #verbChanged(VerbEditor)} that can
	 * be called to refresh the window whenever the selected verb on the 
	 * verb editor changes. 
	 * 
	 */
	public static class ComesFromLizard extends JDialog implements
			VerbEditor.VerbListener {
		private static final long serialVersionUID = 1L;
		Swat swat;
		Iterable<Verb> mVerbs;
		DefaultListModel list = new DefaultListModel();
		JList comeFromList = new JList(list);
		Color lightFill = new Color(255, 240, 255);

		public ComesFromLizard(Swat swat, Iterable<Verb> vs) {
			super(swat.getMyFrame());
			this.swat = swat;
			swat.verbEditor.addVerbListener(this);
			mVerbs = vs;
			comeFromList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			comeFromList.setVisibleRowCount(-1);
			comeFromList.setBackground(lightFill);
			comeFromList.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						ComesFromEntry o = (ComesFromEntry)comeFromList.getSelectedValue();
						if (o != null) {
							ComesFromLizard.this.swat.setEditorInFocus(Swat.EditorEnum.VerbEditorHasFocus);
							ComesFromLizard.this.swat.verbEditor.setState(
									ComesFromLizard.this.swat.verbEditor.new State(o.v,o.r,o.r.getRole().getOption(ComesFromLizard.this.swat.verbEditor.getVerb().getLabel()),null,null));
							reloadComeFromVerbs();
						}
					}
				}
			});
			add(new JScrollPane(comeFromList));
			setSize(300, 200);
			setLocation(500, 200);
			reloadComeFromVerbs();

			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					ComesFromLizard.this.swat.verbEditor.removeVerbListener(ComesFromLizard.this);
					ComesFromLizard.this.swat.comeFromLizardMenuItem.setEnabled(true);
				}
			});
			setVisible(true);
		}

		public void refreshTitle(){
			setTitle("Come Froms for {" + swat.verbEditor.getVerb().getLabel() + "}");
		}
		
		public void reloadComeFromVerbs() {
			refreshTitle();
			list.removeAllElements();
			for (Verb v : mVerbs) {
				for (Role.Link role : v.getRoles()) {
					for (Role.Option option : role.getRole().getOptions()) {
						if (option.getLabel().equals(swat.verbEditor.getVerb().getLabel()))
							list.addElement(new ComesFromEntry(v,role));
					}
				}
			}
		}

		/**
		 * Call me whenever you want to refresh this window state whenever 
		 * the selected verb in verb editor changes. 
		 */
		public void verbChanged(VerbEditor ve) {
			reloadComeFromVerbs();
		}
		
		private static class ComesFromEntry {
			public Verb v;
			public Role.Link r;
			public ComesFromEntry(Verb v,Role.Link r){
				this.v=v;
				this.r=r;
			}
			public String toString(){ return v.getLabel()+": "+r.getLabel(); }
		};
	}
//*****************************************************************************
	public JFrame getMyFrame() { return myFrame; }
	/** Sets the focus when it is lost for whatever reason. */
	private void arrangeFocus(){
		Component owner=FocusManager.getCurrentManager().getFocusOwner();
		if (owner==null || !owner.isEnabled() || !myFrame.getContentPane().isAncestorOf(owner))
			myFrame.getContentPane().requestFocusInWindow();
	}
	//*****************************************************************************
	/**
	 * Overrides some methods of undo manager to detect how many changes
	 * the user has made since the last save, and to update the undo and
	 * redo menu items of {@link Swat}.  
	 */
	class UndoManager extends javax.swing.undo.UndoManager {
		private static final long serialVersionUID = 1L;
		/** 
		 * The amount of changes that have been done to this storyworld since
		 * last saved. It may be negative when saving and later undoing the changes.
		 * */
		private int changeCount=0;
		/** 
		 * savingNeeded tells if the storyworld needs to be saved, regardless
		 * of the changeCount value. In some situations the changeCount can be
		 * zero, and nonetheless the storyworld has unsaved changes.
		 * */
		private boolean savingNeeded = false;

		@Override
		public synchronized boolean addEdit(UndoableEdit anEdit) {
			boolean b=super.addEdit(anEdit);
			undoMenuItem.setEnabled(canUndo());
			redoMenuItem.setEnabled(canRedo());
			undoMenuItem.setText(getUndoPresentationName());
			redoMenuItem.setText(getRedoPresentationName());
			// if savingNeeded==true don't care to update the storyworld version 
			if (!savingNeeded) {
				if (changeCount<0) {
					// if changeCount is negative the last saved change is in the
					// redo stack, but we are making a new change that will clear
					// the redo stack. Therefore we know for sure that we will have 
					// unsaved changes, whatever the changes we do from now on.
					savingNeeded=true;
				} else if (changeCount==0) {
					// before just now there were no changes to save,
					// so we should increase the storyworld version.
					dk.version++;
					updateFrameTitle();
				}
			}
			// update the change count
			changeCount++;
			return b;
		}

		@Override
		public synchronized void redo() throws CannotRedoException {
			super.redo();
			undoMenuItem.setEnabled(canUndo());
			redoMenuItem.setEnabled(canRedo());			
			undoMenuItem.setText(getUndoPresentationName());
			redoMenuItem.setText(getRedoPresentationName());
			// if savingNeeded==true don't care to update the storyworld version
			if (!savingNeeded) {
				if (changeCount==0) {
					// before just now there were no changes to save,
					// so we should increase the storyworld version.
					dk.version++;
					updateFrameTitle();
				} else if (changeCount==-1) {
					// we are recovering through redo the state we had the
					// last time we saved the storyworld. Therefore, we must
					// decrement back the storyworld version.
					dk.version--;
					updateFrameTitle();
				}
			}
			changeCount++;
			arrangeFocus();
		}

		@Override
		public synchronized void undo() throws CannotUndoException {
			super.undo();
			undoMenuItem.setEnabled(canUndo());
			redoMenuItem.setEnabled(canRedo());
			undoMenuItem.setText(getUndoPresentationName());
			redoMenuItem.setText(getRedoPresentationName());
			if (!savingNeeded) {
				if (changeCount==1) {
					// we are recovering through undo the state we had the
					// last time we saved the storyworld. Therefore, we must
					// decrement back the storyworld version.
					dk.version--;
					updateFrameTitle();
				} else if (changeCount==0) {
					// before just now, there were no changes to save,
					// so we should increase the storyworld version.
					dk.version++;
					updateFrameTitle();
				}
			}
			changeCount--;
			arrangeFocus();
		}
		
		/**
		 * Tells if there are unsaved changes.
		 * @see #saving() 
		 */
		public boolean isSavingNeeded() {
			return savingNeeded || changeCount!=0; 
		}
		/**
		 * Call me whenever the user saves so I start counting from zero the 
		 * unsaved changes.
		 * @see #isSavingNeeded()
		 * */
		public void saving() { 
			changeCount=0;
			savingNeeded=false;
		}
	}
	//************************************************************
	public javax.swing.undo.UndoManager getUndoManager() { return undoManager; }
	
	private void startRehearsalLizard() {
		try {
			Triplet<Boolean,int[],byte[]> rdialog = showRehearsalDialog();
			if (!rdialog.first)
				return;
			
			final Rehearsal rehearsal = new Rehearsal(this);
			// Show the progress bar
			rehearsalBar = new ProgressMonitor(this.getMyFrame(), "Opening Rehearsal", "Running rehearsal 1 of 10 . . .", 0, 100);
			rehearsalBar.setMillisToDecideToPopup(0);
			rehearsalBar.setMillisToPopup(0);
			rehearsalBar.setProgress(1);			
			
			final Timer progressTimer = new Timer();
			progressTimer.schedule(new TimerTask() {
				public void run() {
					try {
					
						final int rehearsalProgress = swatJanus.getRehearsalProgress(rehearsalSessionID);						
						
						if (rehearsalBar.isCanceled()) {
							swatJanus.cancelRehearsal(rehearsalSessionID);
							progressTimer.cancel();				
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {	
									rehearsalLizardMenuItem.setEnabled(true);
								}
							});
						} else if (rehearsalProgress == SharedConstants.TASK_CANCELLED) {
							progressTimer.cancel();
							
							// Display a timeout message
							rehearsalBar.setNote("Rehearsal timed out ...");
							rehearsalBar.setProgress(0);
							Thread.sleep(3000);
							
							rehearsalBar.close();
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {	
									rehearsalLizardMenuItem.setEnabled(true);
								}
							});
						} else if (rehearsalProgress < 100)
						   SwingUtilities.invokeLater(new Runnable(){
							   public void run() {	
								   rehearsalBar.setProgress(rehearsalProgress);
								   rehearsalBar.setNote("Running rehearsal "+(Math.max(0,rehearsalProgress-1)/10+1)+" of 10 . . .");
							   } 
						   });						   
						else {
							progressTimer.cancel();
							final RehearsalResult rResult = swatJanus.getRehearsalResults(rehearsalSessionID);
							SwingUtilities.invokeLater(new Runnable(){
								public void run() {	
									if (rResult==null) {
										// Display a timeout message
										rehearsalBar.setNote("Rehearsal timed out ...");
										rehearsalBar.setProgress(0);
										try {
											Thread.sleep(3000);
										} catch(InterruptedException e){
											e.printStackTrace();
										}
										
										rehearsalBar.close();
										rehearsalLizardMenuItem.setEnabled(true);
									} else {
										rehearsal.passData(rResult.verbData, rResult.centralVerbIndex, rResult.poisonings);
										disableVerbMenus();
										verbEditorMenuItem.setEnabled(true);
										rehearsalLizardMenuItem.setEnabled(false);
										rehearsalBar.setProgress(rehearsalProgress);
										rehearsal.setVisible(true);
									}
								}
							});
						}
					} catch (RemoteException tre) {
						System.out.println("Remote Exception in Timer ActionListener");
						tre.printStackTrace();
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {	
								rehearsalLizardMenuItem.setEnabled(true);
							}
						});
					}
					catch (Exception ex){ 
						System.out.println("Local Exception in Timer ActionListener");
						ex.printStackTrace();
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {	
								rehearsalLizardMenuItem.setEnabled(true);
							}
						});
					}
				}			
				
			},3000,3000);
		
			swatJanus.startRehearsal(rehearsalSessionID, rdialog.second, rdialog.third);
		} catch (RemoteException re) {
			re.printStackTrace();
		}
		catch (Exception e){ 
			e.printStackTrace();
		}
	}

	/** Shows a rehearsal dialog to choose where to start the rehearsal from. */
	private Triplet<Boolean,int[],byte[]> showRehearsalDialog(){
		final Triplet<Boolean,int[],byte[]> p = new Triplet<Boolean,int[],byte[]>(false,null,null);
		final JDialog rdialog = new JDialog(getMyFrame(),"Start rehearsal",true);

		JButton startButton = new JButton("Start from \"once upon a time\"");
		startButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				p.first=true;
				rdialog.dispose();
			}
		});
		JButton startFromButton = new JButton("Start from ...");
		startFromButton.setToolTipText("Loads a saved story and runs the rehearsal.");
		startFromButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				chooser.setFileFilter(Utils.SAVED_STORY_TRACE_FILE_FILTER);
				chooser.setSelectedFile(new File(""));
				switch(chooser.showOpenDialog(getMyFrame())){
				case JFileChooser.APPROVE_OPTION:
					break;
				default:
					return;
				};
				File f = chooser.getSelectedFile();
				try {
					readStory(f,p);
					rdialog.dispose();
				} catch (IOException ex) {
					ex.printStackTrace();
					if (!f.exists()) 
						Utils.showErrorDialog(getMyFrame(), "Could not load story from file \n"+f.getPath()+"\nThe file does not exist.","File error");
					else
						Utils.showErrorDialog(getMyFrame(), "Could not load story from file \n"+f.getPath(),"File error",ex);

					return;
				}
			}
		});

		final JComponent main = new JPanel(new GridLayout(0,1));
		main.setOpaque(false);
		main.add(startButton);
		main.add(startFromButton);
		for(final String file:Utils.getRecentStoryFiles()){
			final File f = new File(file);
			final JButton bt = new JButton("Start from "+f.getName());
			bt.setToolTipText("Loads "+file+" and runs the rehearsal.");
			bt.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					try {
						readStory(f,p);
						rdialog.dispose();
					} catch (IOException ex) {
						ex.printStackTrace();
						if (!f.exists()) {
							Utils.deleteRecentStoryFile(f.getAbsolutePath());
							main.remove(bt);
							Utils.showErrorDialog(getMyFrame(), "Could not load story from file \n"+f.getPath()+"\nThe file does not exist.","File error");
						} else
							Utils.showErrorDialog(getMyFrame(), "Could not load story from file \n"+f.getPath(),"File error",ex);

						return;
					}
				}
			});
			main.add(bt);
		}
		
		rdialog.getContentPane().add(main);
		rdialog.pack();
		rdialog.setLocationRelativeTo(getMyFrame());
		rdialog.setVisible(true);
		return p;
	}
	
	private void readStory(File f,Triplet<Boolean,int[],byte[]> p) throws IOException {
		InputStream is = new FileInputStream(f);
		final SavedStory ss = new SavedStory();
		try {
			ss.readFormatHeader(is);
			final DataInputStream br = SavedStory.convertInputStreamToDataInput(is);
			ss.readHeader(br);
			if (!Storyteller.checkSavedStoryVersion(getMyFrame(), ss, dk.getFile().getName(), dk.version, f.getName())) {
				br.close();
				return;
			}
			ss.readBody(br);
			p.first=true;
			p.second = Utils.toArray(ss.recordedInput);
			p.third = ss.state;
			Utils.addRecentStoryFile(f.getAbsolutePath());
		} catch (SavedStory.InvalidFormatException ex) {
			Utils.showErrorDialog(getMyFrame(),
					"The given file is not a saved story file \n"+f.getPath(), "Loading");
			is.close();
			return;
		}
	}
	
	private void startStorytellerLizard() {
		// Ask the log downloader to erase the previous log when starting a new story.
		if (logManager!=null)
			logDownloader.restart(swatJanus,logDownloader.getSessionID());
		else {
			logDownloader = new LogDownloaderThread(swatJanus);
			logDownloader.setPriority(Thread.MIN_PRIORITY);
			logDownloader.setDaemon(true);
			logDownloader.start();
			try {
				logManager = new LogDownloadManager(logDownloader);
			} catch (IOException exc)	{
				JOptionPane.showMessageDialog(getMyFrame(), "There was an error when creating a temporal file for the log on disk.\n"+exc.getMessage(),"File error",JOptionPane.ERROR_MESSAGE);
				storytellerLizardMenuItem.setEnabled(true);
				rehearsalLizardMenuItem.setEnabled(true);
				return;
			}
		}

		st = new Storyteller(swatJanus, dk,chooser,this,logDownloader,login,password);
		logManager.getTreeModel().surfaceStream.writerListener = new TreeOutputStream.WriterListener(){
			public void aboutToWrite(Object[] params) {
				if (Utils.markedBySearch(params))
					st.addPoisoningsButton();
			}
		};
		Utils.setCursor(myFrame,Cursor.DEFAULT_CURSOR);
		Utils.setCursor(st,Cursor.WAIT_CURSOR);
		st.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(WindowEvent e) {
				storytellerLizardMenuItem.setEnabled(true);
				rehearsalLizardMenuItem.setEnabled(true);
			}
		});
		st.setVisible(true);

		final String verbLabel =  dk.findVerb("once upon a time")>=0?"once upon a time":dk.getVerb(0).getLabel();
		
		try {
			st.startStory(verbLabel);
			Utils.setCursor(st,Cursor.DEFAULT_CURSOR);
		} catch(BadStoryworldException e) {
			clearLogIssues();
			LinkedList<LogIssue> localErrors = dk.checkScripts(null,false);
			if (localErrors.isEmpty())
				showLogIssues(toLogIssues(e.errors));
			else
				showLogIssues(localErrors);
			Utils.setCursor(st,Cursor.DEFAULT_CURSOR);
			Utils.showErrorDialog(st, "The storyworld has errors.\n\nIf they are fixed, I might be able to play it.", "Error when loading storyworld.");
			st.dispose();
		}
	}

	/**
	 * This is a class to share information in the event system. 
	 * Controls in the aplication can set the source of an event to an
	 * instance of this class. 
	 * Then an AWTEventPostprocessor will know what to do with it,
	 * like the want we implemented in {@link Swat#Swat()}. 
	 */
	public static class EventInfo extends Component {
		static final long serialVersionUID=0;
		Object source;
		public EventInfo(Object source){ this.source=source; }
	}
	
	/** An extension of {@link EditorListener} for editing {@link DropDown}s. */
	public static abstract class DropDownListener extends EditorListener 
		implements PopupMenuListener {
		private static final long serialVersionUID = 0L;
		DropDown d;
		public DropDownListener(DropDown d){
			super(d.getTextComponent());
			this.d=d;
			d.addPopupListener(this);
		}
		@Override
		public void updated() {
			if (!d.isListPicking())
				super.updated();
			else d.getJTextComponent().setForeground(Color.black);
		}
		public void popupMenuCanceled(PopupMenuEvent e) {}
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			if (!error) actionPerformed(null);
		}
	}
	/** An interface to inhibit the EditorListener when setting the
	 * editor text from code.
	 * The userInput attribute serves to classify inputs. 
	 */
	public interface TextComponent {
		public void setUserInput(boolean userInput);
		public boolean isUserInput();
		public void setText(String t);
		public JTextComponent getJTextComponent();
	}
	/** An implementation of the {@link TextComponent} interface for {@link JTextField}.*/
	public static class TextField extends JTextField implements TextComponent {
		static final long serialVersionUID=0;
		private boolean userInput=true;
		public void setUserInput(boolean userInput) { this.userInput = userInput;}
		public boolean isUserInput() {return userInput;	}
		public JTextComponent getJTextComponent(){ return this; };
		public TextField(int cols){ super(cols); }
		public TextField(String text){ super(text); }
		public TextField(){ super(); }
		@Override
		public void setText(String t){
			setUserInput(false);
			super.setText(t);
			setUserInput(true);
		}		
	}
	/** An implementation of the {@link TextComponent} interface for {@link JTextArea}.*/
	public static class TextArea extends JTextArea implements TextComponent {
		static final long serialVersionUID=0;
		private boolean userInput=true;
		public void setUserInput(boolean userInput) { this.userInput = userInput;}
		public boolean isUserInput() {return userInput;	}
		public JTextComponent getJTextComponent(){ return this; };
		public TextArea(int rows,int cols){ super(rows,cols); }
		public TextArea(String text){ super(text); }
		public TextArea(){ super(); }
		@Override
		public void setText(String t){
			setUserInput(false);
			super.setText(t);
			setUserInput(true);
		}		
	}
	public static class Slider extends JSlider {
		static final long serialVersionUID=0;
		private boolean userInput=true;
		public void setUserInput(boolean userInput) { this.userInput = userInput;}
		public boolean isUserInput() {return userInput;	}
		public Slider(int orientation,int min,int max,int init){
			super(orientation,min,max,init);
		}
		
		public void mSetValue(int n) {
			boolean temp=isUserInput();
			setUserInput(false);
			setValue(n);
			setUserInput(temp);
		}
	}

	/** Commands for the downloader thread. */
	public enum DownloaderCommand { 
		RESTART,
		GET_LOG,
		GET_LOG_REQUEST
	};

	/** Thread to update the treeModel for log lizard.
	 * Interrupt this thread to make it aware of new available data
	 * to download for the server.   
	 *  */
	public class LogDownloaderThread extends Thread {
		private BlockingQueue<DownloaderCommand> commands = new LinkedBlockingQueue<DownloaderCommand>();
		private StorytellerRemote janus;
		private String sessionID; 
		private StorytellerRemote tempJanus;
		private String tempSessionID;
		private LogProcessorThread logProcessor = new LogProcessorThread();
		public boolean sessionExpired = false;
		/**
		 * Creates a downloader for downloading data using the given 
		 * SwatRemote and session id. 
		 * */
		public LogDownloaderThread(StorytellerRemote storytellerJanus){
			janus = storytellerJanus;
			this.sessionID = "";
		}
		public StorytellerRemote getJanus(){ 
			return janus; 
		};
		public String getSessionID(){ 
			return sessionID; 
		};
		/**
		 * Restarts the downloader for downloading data using the given 
		 * SwatRemote and session id. The treeModel is cleared before restarting.
		 * */
		public void restart(StorytellerRemote storytellerJanus, String sessionID){
			tempSessionID = sessionID;
			tempJanus = storytellerJanus;
			commands.clear();
			sessionExpired = false;
			try {
				commands.put(DownloaderCommand.RESTART);
			} catch (InterruptedException e){}
		}
		/** Downloads the upper levels of the log tree. */
		public void getLogData(){
			try {
				commands.put(DownloaderCommand.GET_LOG);
			} catch (InterruptedException e){}
		}
		/** 
		 * Downloads a specific branch of the log tree. 
		 * The given branch must have been requested by the logManager.
		 *  */
		public void getRequestedLogData(){
			try {
				commands.put(DownloaderCommand.GET_LOG_REQUEST);
			} catch (InterruptedException e){}
		}

		@Override
		public void run() {
			logProcessor.start();
			do {
				DownloaderCommand command = null;
				try {
					command=commands.take();
					switch (command) {
					case GET_LOG_REQUEST:
						if (!sessionExpired) {
							final Triplet<Integer,Integer,Compressed<Object[]>[]> p=janus.getRequestedLogData(sessionID);
							if (logManager!=null && p!=null) {
								logProcessor.execute(new Runnable(){
									public void run() {
										try { 
											if (logManager!=null)
												logManager.processRequest(p);
										} catch(Exception e) { handleException(e);	}
									}
								});
							}
						}
						break;
					case RESTART:
						logProcessor.reset();
						logManager.reset();
						janus = tempJanus;
						sessionID = tempSessionID;
						break;
					case GET_LOG:
						// remove all GET_LOG commands, prior to downloading. 
						while(commands.remove(DownloaderCommand.GET_LOG));
						
						final Compressed<Object[]>[] data = janus.getLogData(sessionID);
						logProcessor.execute(new Runnable(){
							public void run() {
								try { logManager.reportNodes(data);
								} catch(Exception e) { handleException(e);	}
							}
						});
					}
				} catch (Exception e){	handleException(e);	}
			} while(true);
		}
		
		private void handleException(Exception e) {
			if (e instanceof SessionLogoutException) {
				if (!sessionExpired) {
					sessionExpired = true;
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(getMyFrame(), "Your session on the server has expired.","Connection error");
						}
					});
				}
			} else if (e instanceof RuntimeException) {
				if (logDownloader!=null || e.getCause()==null || !(e.getCause() instanceof IOException))
					e.printStackTrace();
			} else
				e.printStackTrace();
		}
	};

	/** A thread for processing log data. */
	private static class LogProcessorThread extends Thread {
		public LogProcessorThread(){ super("LogProcessorThread"); }
		private BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
		private boolean processing = false;
		@Override
		public void run() {
			try {
				while(true) {
					final Runnable r = tasks.take();
					processing = true;
					try {
						r.run();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						processing = false;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		/** Submits a task for execution. */
		public void execute(Runnable r){
			try {
				tasks.put(r);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		/** Resets the current tasks and waits for any currently executing task to finish. */
		public void reset() throws InterruptedException {
			tasks.clear();
			while(processing)
				Thread.sleep(200);
		}
	}

	/** A class for testing Swat functionality. */
	public static class Test {
		public static void showCopyrightEditor(Swat swat) {
			swat.showCopyrightEditor();
		}
		
		public static void editCopyrightText(Swat swat,final String newText){
			swat.editCopyrightText(newText);
		}

		public static ActorEditor openActorEditor(Swat swat){
			swat.actorEditorMenuItem.doClick();
			return swat.actorEditor;
		}

		public static OperatorEditor openOperatorEditor(Swat swat){
			swat.showOperatorEditor();
			return swat.operatorEditor;
		}

		public static RelationshipSettings openRelationships(Swat swat){
			swat.relationshipsMenuItem.doClick();
			return swat.relationshipSettings;
		}

		public static RelationshipEditor openRelationshipEditor(Swat swat){
			swat.relationshipEditorMenuItem.doClick();
			return swat.relationshipEditor;
		}

		public static VerbEditor openVerbEditor(Swat swat){
			swat.verbEditorMenuItem.doClick();
			return swat.verbEditor;
		}

		public static PropEditor openPropEditor(Swat swat){
			swat.propEditorMenuItem.doClick();
			return swat.propEditor;
		}

		public static Storyteller openStoryteller(Swat swat) throws InterruptedException {
			swat.storytellerLizardMenuItem.doClick();
			int i=0;
			while(swat.st==null && i++<20)
				Thread.sleep(200);
			return swat.st;
		}
		
		public static void closeAuxWindows(Swat swat){
			VerbEditor.Test.closeAuxWindows(swat.verbEditor);
			if (swat.terminationSettings!=null)
				swat.terminationSettings.dispose();
			if (swat.relationshipSettings!=null)
				swat.relationshipSettings.dispose();
			if (swat.copyrightEditor!=null)
				swat.copyrightEditor.setVisible(false);
			if (swat.st!=null)
				swat.st.dispose();
		}
	}
}
