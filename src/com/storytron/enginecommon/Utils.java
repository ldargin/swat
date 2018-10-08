package com.storytron.enginecommon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;

import org.xml.sax.SAXParseException;

import Engine.enginePackage.EngineLogger.MsgType;

import com.storytron.swat.util.LineBreaker;
import com.storytron.uber.Deikto;
import com.storytron.uber.Role;
import com.storytron.uber.Verb;
import com.storytron.uber.Deikto.LogIssue;
import com.storytron.uber.deiktotrans.DeiktoLoader.BadVersionException;

import com.storytron.swat.Swat;

/**
 * A class to hold utility methods, non-related to anything else.  
 * */
public final class Utils {

	public static final Color darkBackground = new Color(208, 208, 244);
	public static final Color lightBackground = new Color(236, 236, 255);
	public static final Color lightlightBackground = new Color(243, 243, 255);
	public static final Color lightGrayBackground = new Color(230, 230, 230);
	public static final Color darkColumnBackground = new Color(224, 224, 255);
	public static final Color STORYTELLER_LEFT_COLOR = new Color(227, 205, 181);
	public static final Color STORYTELLER_RIGHT_COLOR = new Color(246, 239, 231);
	public static final Color STORYTELLER_LEFT_HEADER_BORDER_COLOR = new Color(204, 184, 160);
	public static final Color STORYTELLER_RIGHT_HEADER_BORDER_COLOR = new Color(220, 215, 209);
	public static final Color STORYTELLER_LEFT_HEADER_COLOR = new Color(238, 222, 207);
	public static final Color STORYTELLER_RIGHT_HEADER_COLOR = new Color(255, 255, 255);
	public static final Color STORYTELLER_BACKGROUND = new Color(108,99,92);

	public final static float MAXI_VALUE = 0.9999f;
	public final static float MINI_VALUE = -0.9999f;
	public final static float MAXI_NVALUE = 9999.0f;
	public final static float MINI_NVALUE = -9999.0f;

	public static FileFilter SAVED_STORY_FILE_FILTER = new FileFilter(){
		@Override
		public boolean accept(File f) {						
			return f.isDirectory() || f.getName().toLowerCase().endsWith(".sst");
		}
		@Override
		public String getDescription() {
			return "Saved story (*.sst)";
		}
	};

	public static FileFilter STORY_TRACE_FILE_FILTER = new FileFilter(){
		@Override
		public boolean accept(File f) {						
			return f.isDirectory() || f.getName().toLowerCase().endsWith(".str");
		}
		@Override
		public String getDescription() {
			return "Trace (*.str)";
		}
	};

	public static FileFilter SAVED_STORY_TRACE_FILE_FILTER = new FileFilter(){
		@Override
		public boolean accept(File f) {						
			return f.isDirectory() || f.getName().toLowerCase().endsWith(".str")
			 		|| f.getName().toLowerCase().endsWith(".sst");
		}
		@Override
		public String getDescription() {
			return "Saved stories, traces (*.sst, *.str)";
		}
	};
	
	/** Tells if a filename contains an extension. */
	public static boolean containsExtension(String filename,String ext) {
		int i=filename.lastIndexOf('.');
		return i!=-1 && filename.substring(i).toLowerCase().equals(ext);
	}

	/** 
	 * Adds an extension to a given filename if it does not 
	 * contain it already.
	 * */
	public static File addExtension(File f,String ext) {
		if (containsExtension(f.getName(), ext))
			return f;
		else
			return new File(f.getPath()+ext);
	}

	/** Converts a collection of Integers to an array of ints. */
	public static int[] toArray(Collection<Integer> c){
		int[] ints = new int[c.size()];
		int i=0;
		for(Integer ci:c)
			ints[i++]=ci;
		return ints;
	}
	
	/** 
	 * <p>Creates a new array of size {@code arr.length+1},
	 *  by copying all the {@code arr} elements and putting
	 *  newElement in the last position.</p>
	 * */
	public static Object[] append(Object[] arr,Object newElem){
		Object[] newArr = new Object[arr.length+1];
		for(int j=0;j<arr.length;j++)
			newArr[j]=arr[j];
		newArr[newArr.length-1]=newElem;
		return newArr;
	}
	
	/** 
	 * Concatenates the elements in a and b yielding the result
	 * in a new array. A null argument is considered as an empty array.
	 * Returns null if both arguments are null.
	 *  */
	public static int[] concat(int[] a,int[] b) {
		int[] concatenation=null;
		int size=0;
		if (a!=null)
			size+=a.length;
		if (b!=null)
			size+=b.length;
		if (size>0) {
			concatenation = new int[size];
			if (a!=null)
				System.arraycopy(a, 0, concatenation, 0, a.length);
			if (b!=null)
				System.arraycopy(b, 0, concatenation, a!=null?a.length:0, b.length);
		}
		return concatenation;
	}
	
	/** 
	 * <p>Creates a new array of size {@code arr.length+1},
	 *  by copying all the {@code arr} elements and putting
	 *  newElement in the last position.</p>
	 * */
	public static int[] removeIndex(int[] arr,int i){
		if (i<0 || arr.length<=i)
			return arr;
		
		int[] newArr = new int[arr.length-1];
		for(int j=0;j<i;j++)
			newArr[j]=arr[j];
		for(int j=i+1;j<arr.length;j++)
			newArr[j-1]=arr[j];
		return newArr;
	}
	
	/** 
	 * Concatenates the strings in {@code strs} starting from {@code start},
	 * using {@code sep} between each string and the next one.
	 * */
	public static String concatStrings(String[] strs,String sep,int start){
		if (strs.length<=start)
			return "";
		
		String res = strs[start];
		for(int i=start+1;i<strs.length;i++)
			res+=sep+strs[i];
		return res;
	} 
	
	/** Serializes and compresses a Deikto instance. */
	public static byte[] compressDeikto(Deikto tdk) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DeflaterOutputStream dos = new DeflaterOutputStream(baos);
			
		// Get the dictionary in XML format
		tdk.writeXML(dos);

		dos.flush();
		dos.close();
		return baos.toByteArray();
	}

	/** Sets the cursor for a given component. */
	public static void setCursor(Component c,int cursor){
		c.setCursor(Cursor.getPredefinedCursor(cursor));
	}

	private static final String RECENT_STORY_FILES = "recentStoryFiles";
	/** Gets recent story file names. */
	public static synchronized String[] getRecentStoryFiles(){
		String prop = (String)getProperties().get(RECENT_STORY_FILES);
		if (prop!=null)
			return prop.split(";");
		else
			return new String[0];	
	}
	/** Adds a new file to the list of recent story files. */
	public static synchronized void addRecentStoryFile(String filepath){
		StringBuilder sb = new StringBuilder();
		int count=0;
		boolean found = false;
		for(String f:getRecentStoryFiles())			
			if (count<4 || count==4 && found) {
				sb.append(";");
				sb.append(f);
				found |= f.equals(filepath);
				count++;
			} 
		if (!found)
			sb.insert(0,filepath);
		else if (sb.length()>0)
			sb.deleteCharAt(0);
		getProperties().setProperty(RECENT_STORY_FILES, sb.toString());
	}

	
	/** 
	 * Deletes a file from the list of recent story files.
	 * Most likely because the file does not exist anymore or has been
	 * moved. 
	 * */
	public static synchronized void deleteRecentStoryFile(String filepath){
		StringBuilder sb = new StringBuilder();
		for(String f:getRecentStoryFiles())			
			if (!f.equals(filepath)) {
				sb.append(";");
				sb.append(f);
			} 
		if (sb.length()>0)
			sb.deleteCharAt(0);
		getProperties().setProperty(RECENT_STORY_FILES, sb.toString());
	}

	private static final String WORKING_DIRECTORY = "workingDirectory";
	/** Gets the directory that should be used for file choosers. */
	public static File getWorkingDirectory(){
		String wd = (String)getProperties().get(WORKING_DIRECTORY);
		if (wd==null)
			return null;
		else
			return new File(wd);
	}
	/** Sets the directory that should be used for file choosers. */
	public static void setWorkingDirectory(File path){
		getProperties().setProperty(WORKING_DIRECTORY,path.getAbsolutePath());
	}
	
	private static Properties properties;
	private static final String propertiesFile = "app.properties";
	/** Gets persistent global properties. */
	public static Properties getProperties(){
		if (properties==null)
			loadProperties();
		return properties;
	}
	/** Loads properties from disk. */
	private static void loadProperties(){
		properties = new Properties();
		File p = getUserApplicationSettingsPath();
		if (p==null)
			return;
		File f = new File(p,propertiesFile);
		if (!f.exists() || f.isDirectory())
			return;
		try {
			FileInputStream fs = new FileInputStream(f);
			properties.load(fs);
			fs.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	/** Saves properties to disk. */
	public static void saveProperties(){
		File p = getUserApplicationSettingsPath();
		if (p==null || properties==null)
			return;
		try {
			p.mkdirs();
			FileOutputStream fs = new FileOutputStream(new File(p,propertiesFile));
			properties.store(fs,null);
			fs.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/** Gets a path suitable for putting the application settings per user. */
	private static File getUserApplicationSettingsPath(){
		String os = System.getProperty("os.name","");
		String home = System.getProperty("user.home");
		if (home==null) // no user home, use system dependent dir
			if (os.contains("Mac") || os.contains("Windows"))
				return new File("","storytron");
			else
				return new File("",".storytron");
			// return new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		else if (os.contains("Mac"))
			return new File(home,"Library/Preferences/storytron");
		else if (os.contains("Windows"))
			return new File(home,"Application Data/storytron");
		else // Assume a unix
			return new File(home,".storytron");
	}
	
	/** Expands the last component of tp and all of its descendants in a given JTree. */
	public static void expandAll(JTree tree,TreePath tp){
		tree.expandPath(tp);
		int count = tree.getModel().getChildCount(tp.getLastPathComponent());
		for(int i=0;i<count;i++)
			expandAll(tree,new MTreePath(tp,
					tree.getModel().getChild(tp.getLastPathComponent(),i)));
	};

	/** Makes public a protected constructor. */
	public static class MTreePath extends TreePath {
		public static final long serialVersionUID = 0L;
		public MTreePath(TreePath parent,Object lastComponent) {
			super(parent,lastComponent);
		}
	}

	/**
     * Resizes an image using a Graphics2D object backed by a BufferedImage.
     * @param srcImg - source image to scale
     * @param w - desired width
     * @param h - desired height
     * @return - the new resized image
     */
    public static BufferedImage getResizedImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }

    /** Implements array linear search. */
	public static <T> boolean contains(T[] ts,T t){
		for(T tt:ts)
			if (tt==t || tt!=null && tt.equals(t))
				return true;
		return false;
	}

    /** Tells if s is contained in c ignoring case. */
    public static boolean containsIgnoreCase(String c,String s){
    	int limit = c.length()-s.length()+1;
    	for(int i=0;i<limit;i++)
    		if (s.regionMatches(true, 0, c, i, s.length()))
    			return true;
    	return false;
    }
    
	/** Helper method to read bytes from a file. */
	public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
    
        // Get the size of the file
        long length = file.length();
    
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
    
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
    
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
    
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
    
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

	/** 
	 * Return bytes of JPG image. If image is not null it is 
	 * serialized, otherwise the bytes are read from imageFile. 
	 * @return null if both arguments are null. 
	 * */
	public static byte[] getImageJPGBytes(File imageFile,ScaledImage image) throws IOException {
		if (image!=null){
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(image.getBufferedImage(),"JPG",os);
			return os.toByteArray(); 
		} else if (imageFile!=null)
			return Utils.getBytesFromFile(imageFile);
		else 
			return null; 
	}
	
	public static <K> int binarySearch(List<? extends Comparable<? super K>> c,K key){
		int res=Collections.binarySearch(c, key);
		if (res>=0)
			return res;
		else
			return -res-2;
	}
	
	/** Casts a byte array with ascii values to a char array. */
	public static char[] asciiToChar(byte[] bs){
		char[] res = new char[bs.length];
		for(int i=0;i<bs.length;i++)
			res[i]=(char)bs[i];
		return res;
	} 
	
	private static final Object[] errorDialogButtons = new Object[]{ "Close" };
	private static Font dialogFont;
	public static void showErrorDialog(Component parentComponent,String message,
			String title) {
		showErrorDialog(parentComponent,message,title,null);
	}
	public static void showErrorDialog(Component parentComponent,String message,
			String title, Throwable e) {
		showErrorDialog(parentComponent,message,title,null,0,e,null);
	}
	public static void showErrorDialog(Component parentComponent,String message,
										String title, String storywordID,
										int storywordVersion, Throwable e, byte[] storyTrace) {
		if (e!=null || storyTrace!=null || storywordID!=null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			pw.println(message);
			if (e!=null || storywordID!=null) {
				pw.println("\nThe storytron staff strongly encourages you to ask for assistance in their Bug Reports forum,\nhttp://www.storytron.com/ipb/index.php?showforum=35");
				pw.println("\nThe cryptic text below could help my creators to diagnose the problem:\n");
				pw.println("version: "+SharedConstants.version);
				if (storywordID!=null) {
					pw.println("Storyworld ID: "+storywordID);
					pw.println("Storyworld version: "+storywordVersion);
				}
				pw.println(Utils.currentTime());
				if (e!=null)
					e.printStackTrace(pw);
			} else {
				pw.println("\nversion: "+SharedConstants.version);
				pw.println(Utils.currentTime());
			}
			if (storyTrace!=null) {
				pw.println("\nStory trace:\n");
				pw.write(asciiToChar(storyTrace));
			}
			pw.close();
			message = sw.getBuffer().toString();
		}
		
		Object msg=createDialogTextArea(e!=null || storywordID!=null,message);
		JOptionPane.showOptionDialog(parentComponent, msg, title, JOptionPane.DEFAULT_OPTION, 
						JOptionPane.ERROR_MESSAGE,null,errorDialogButtons,null);
	}

	/** 
	 * Like {@link JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)}
	 * but the text can be copied and pasted elsewhere.
	 * */
	public static int showOptionDialog(Component parentComponent,String message,
										String title, int optionType,int messageType,
										Icon icon,Object[] options,Object initialValue) {

		Object msg = createDialogTextArea(false,message);
		return JOptionPane.showOptionDialog(parentComponent, msg, title, optionType, 
									messageType,icon,options,initialValue);
	}
	
	/** 
	 * Yields a jtextarea suitable for displaying messages in dialogs.
	 * @param affordCopy tells if the dialog look should afford selecting and 
	 *                   copying the text. Useful for long texts.   
	 * */
	private static Object createDialogTextArea(boolean affordCopy,String message){
		JTextArea jta = new JTextArea(message);
		if (!affordCopy) {
			if (dialogFont==null)
				dialogFont=new Font(jta.getFont().getName(),Font.BOLD,jta.getFont().getSize());
			jta.setFont(dialogFont);
		}
		jta.setOpaque(false);
		jta.setEditable(false);
		jta.validate();
		
		Object msg = jta;
		int lineCount = jta.getLineCount();
		if (affordCopy || lineCount>15) {
			jta.setRows(Math.min(15,lineCount));
			JScrollPane sp = new JScrollPane(jta);
			msg = sp;
		}

		return msg;
	}
	
	/** Shows an input dialog for multiline input. */
	public static String showMultilineInputDialog(Frame owner,Icon icon,String message,String title,String defaultText){
		final JTextArea area = new JTextArea(5, 20);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setText(defaultText);
		area.selectAll();
	    // The actions
	    Action nextFocusAction = new AbstractAction("Move Focus Forwards") {
			private static final long serialVersionUID = 1L;
	        public void actionPerformed(ActionEvent evt) {
	            ((Component)evt.getSource()).transferFocus();
	        }
	    };
	    Action prevFocusAction = new AbstractAction("Move Focus Backwards") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent evt) {
	            ((Component)evt.getSource()).transferFocusBackward();
	        }
	    };
		// Add actions
	    area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0), nextFocusAction.getValue(Action.NAME));
	    area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,KeyEvent.SHIFT_MASK), prevFocusAction.getValue(Action.NAME));
	    area.getActionMap().put(nextFocusAction.getValue(Action.NAME), nextFocusAction);
	    area.getActionMap().put(prevFocusAction.getValue(Action.NAME), prevFocusAction);
	    
		
		JScrollPane pane = new JScrollPane(area,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		JComponent center = Box.createVerticalBox();
		center.setBorder(BorderFactory.createEmptyBorder(10,5,10,10));
		JLabel msgLabel = new JLabel(message);
		msgLabel.setAlignmentX(0.0f);
		center.add(msgLabel);
		center.add(Box.createRigidArea(new Dimension(5,5)));
		pane.setAlignmentX(0.0f);
		center.add(pane);

		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		
		JComponent buttons = new JPanel();
		buttons.setOpaque(false);
		buttons.add(okButton);
		buttons.add(cancelButton);
		
		final JDialog d = new JDialog(owner,title,true);
		JLabel iconLabel = new JLabel(icon);
		iconLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,5));
		d.getContentPane().add(iconLabel,BorderLayout.WEST);
		d.getContentPane().add(center);
		d.getContentPane().add(buttons,BorderLayout.SOUTH);
		d.pack();
		d.setLocation((owner.getWidth()-d.getWidth())/2,(owner.getHeight()-d.getHeight())/2);
		
		final Pair<Integer,Integer> accepted = new Pair<Integer,Integer>(null,null);
		okButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				accepted.first = 1;
				d.dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});

		area.requestFocusInWindow();
		d.setVisible(true);
		
		if (accepted.first!=null)
			return area.getText();
		else
			return null;
	}
	
	/** Shows a message for the given Deikto.LimitException. */
	public static void displayLimitExceptionMessage(LimitException e,String title,String msg){
		switch(e.t) {
		case Actors:
			showErrorDialog(null, msg+"\n\nToo many actors (more than "+e.limit+").",title);
			break;
		case Stages:
			showErrorDialog(null, msg+"\n\nToo many stages (more than "+e.limit+").",title);
			break;
		case Props:
			showErrorDialog(null, msg+"\n\nToo many props (more than "+e.limit+").",title);
			break;
		case Verbs:
			showErrorDialog(null, msg+"\n\nToo many verbs (more than "+e.limit+").",title);
			break;
		case Roles:
			showErrorDialog(null, msg+"\n\nToo many roles (more than "+e.limit+") when\nreaching verb: "+e.s,title);
			break;
		case Options:
			showErrorDialog(null, msg+"\n\nToo many options (more than "+e.limit+")",title);
			break;
		case Nodes:
			showErrorDialog(null, msg+"\n\nToo many nodes (more than "+e.limit+")",title);
			break;
		case Name:
			showErrorDialog(null, msg+"\n\nToo long name: "+e.s,title);
			break;
		case Description:
			showErrorDialog(null, msg+"\n\nNode descriptions sum up more than "+e.limit+" characters when\nreaching the script: "+e.s,title);
			break;
		case Traits:
			showErrorDialog(null, msg+"\n\nToo many traits (more than "+e.limit+").",title);
			break;
		case InactivityTimeout:
			showErrorDialog(null, msg+"\n\nThe inactivity timeout is out of the range ["+Deikto.MINIMUM_INACTIVITY_TIMEOUT+"-"+Deikto.MAXIMUM_INACTIVITY_TIMEOUT+"]: "+e.s+".",title);
			break;
		case BNumber:
			showErrorDialog(null, msg+"\n\n"+e.s,title);
			break;
		case CustomOperator:
			showErrorDialog(null, msg+"\n\n"+e.s,title);
			break;
		default:
			showErrorDialog(null, msg+"\n\nThere is a problem with limits.",title);
		}
	}
	
	/** Makes visible a component inside a JScrollPane. */
	public static void scrollToVisible(JComponent comp) {
		comp.scrollRectToVisible(new Rectangle(comp.getWidth(),comp.getHeight()));
	}
	
	/** Counts amount of occurrences of a given character in a string. */
	public static int countAmountOfOccurrences(String s,char c){
		int count=0;
		for(int i=0;i<s.length();i++)
			if (s.charAt(i)==c)
				count++;
		return count;
	} 
	
	/** 
	 * Returns the index of a given element in the given array,
	 * or -1 if the element is not in the array.
	 * */
	public static <T> int indexOf(T[] a,T e){
		if (e!=null){
			for(int i=0;i<a.length;i++)
				if (e.equals(a[i]))
					return i;
		} else {
			for(int i=0;i<a.length;i++)
				if (a[i]==null)
					return i;
		}
		return -1;
	} 

	
	/** 
	 * Returns the index of a given element in the given array,
	 * or -1 if the element is not in the array.
	 * */
	public static int indexOf(int[] a,int e){
		for(int i=0;i<a.length;i++)
			if (a[i]==e)
				return i;
		return -1;
	} 


	/** Tells if a character is a vowel. */
	public static boolean vowel(char c){ 
		switch(Character.toLowerCase(c)){
		case 'a':
		case 'e':
		case 'i':
		case 'o':
		case 'u':
			return true;
		default:
			return false;
		} 
	}

	/** Tells if a word must be preceded by "a" or "an". */
	public static String a_an(String t){ 
		return t.length()==0?"a":vowel(t.charAt(0))?"an":"a"; 
	}

	private static final LineBreaker lb = new LineBreaker("(<[^>]*>)|([^\\p{javaUpperCase}\\-<\\s]+-?)|(\\p{javaUpperCase}+[^\\p{javaUpperCase}\\-<\\s]*-?)|\\s+|\\S+",50){
		@Override
		protected int wordLength(String s){
			if (s.length()==0 || s.charAt(0)=='<' && s.charAt(s.length()-1)=='>')
				return 0;
			else 
				return super.wordLength(s); 
		}
	};
	
	public static void main(String[] args) {
		//String test = "0123</eert> 5678 0 2 4 6789";
		String test="For most of history, China has been the richest, largest, most powerful, most learned, and most technologically advanced country in the world. The barbarians who destroyed the Roman Empire were the losers who lost to the guys who lost to the guys who lost to the guys who lost a war with China."
					+"\n<p></p>"
					+"\nChina never bothered conquering the world because they didn't think it worth their while to mess with smelly barbarians. While they weren't looking, some of those barbarians half a world away developed advanced technology, conquered the world, and imposed their will on an incredulous China in the 19th century. Its cultural self-confidence shattered, China fell apart under the impact of Western military superiority."
					+"\n<p></p>"
					+"\nIt took 150 years for China to pull itself together again. But now China is recovered and eager to resume its historical role as the leading nation of the world."; 
		System.out.println(breakStringHtml(test));
	}


	public static String breakStringHtml(String s){
		if (s==null)
			return null;

		String[] paragraphs = s.replaceAll("<p>|</p>","").split("\n\r|\n");
		
		StringBuilder res = new StringBuilder();
		res.append("<html>");
		for(String p:paragraphs) {
			lb.setString(p);
			for(String line : lb)
				res.append(line).append("<br>");
			if (p.length()==0)
				res.append("<br>");
		}
		res.append("</html>");
		return res.toString();
	}
	
	/**
	 * Removes occurences of &lt;/p&gt; and &lt;p&gt;, 
	 * replaces "\n" by &lt;br&gt; and enforces line width to 220 pixels. 
	 * */
	public static String toHtmlTooltipFormat(String s){
		if (s==null)
			return null;
		else if (s.length()>70)
			return "<html><div style=\"width: 220px; text-align: start;\">"
					+Utils.insertHtmlBreaks(s)+"</html>";
		else
			return "<html>"+Utils.insertHtmlBreaks(s)+"</html>";

	}
	
	public static String insertHtmlBreaks(String s){
		if (s==null)
			return null;

		return Utils.escapeXMLChars(s.replaceAll("<p>|</p>","").replaceAll("\n","<br>"));
	}

	private static Pattern escXMLPattern = Pattern.compile("(</|<)\\p{Alpha}+?(/>|>)|&\\p{Alpha}+?;|<|&");
	/** Escapes XML that do not form part of other tags. */
	public static String escapeXMLChars(String input){
		StringBuilder output = new StringBuilder();
		Matcher m = escXMLPattern.matcher(input);
		int last = 0;
		while(m.find()){
			output.append(input,last,m.start());
			if (m.end()==m.start()+1) {
				if (input.charAt(m.start())=='<')
					output.append("&lt;");
				else
					output.append("&amp;");
			} else
				output.append(m.group());
			last = m.end();
		}
		output.append(input,last,input.length());
		return output.toString();
	}
	
	/** 
	 * Returns null when given an empty string, and returns the given string 
	 * if it is non-empty.
	 * */
	public static String nullifyIfEmpty(String s){ 
		return s==null||s.trim().length()==0?null:s; 
	}
	
	/** 
	 * Returns the empty string when the given string is null, otherwise
	 * returns the string.
	 * */
	public static String emptyIfNull(String s){ return s==null?"":s; }
	
	private static SimpleDateFormat df = new SimpleDateFormat("MMMM d, h:mm Z");
	/** Returns a string representation of the current time. */
	public static String currentTime(){
        Date now = new Date();
        return df.format(now);
	}
	
	/** Gets the directory where resources will be stored. 
	 * @param stwfile is the file were the storyworld will be saved. */
	public static File getResourceDir(File stwfile){
		String dir = stwfile.getName();
		int i = dir.lastIndexOf('.');
		if (i!=-1)
			dir = dir.substring(0,i);
		
		return new File(stwfile.getParentFile(),dir+"_rsc");
	}

	/** 
	 * Returns maxLength-character prefix of a given string if it is that long,
	 * otherwise returns s.  
	 * */
	public static String truncate(String s,int maxLength){
		return s.length()<=maxLength?s:s.substring(0,maxLength);
	}

	/** Tells if a node is a parent of a searched node by looking at its parameters. */
	public static boolean markedBySearch(Object[] params){
		if (params==null || params.length==0)
			return false;
		return params[params.length-1]==MsgType.SEARCHMARK 
				|| params.length>1 && params[params.length-2]==MsgType.SEARCHMARK;
	}
	
	/** Returns the url of a given image. */
	public static URL getImagePath(String imagefile) {
		return getURL("images/"+imagefile);
	}
	
	/** Returns the url of a given sound. */
	public static URL getSoundPath(String soundfile) {
		return getURL("sounds/"+soundfile);
	}

	/** Returns the url of a given data file. */
	public static URL getDataURL(String datafile) {
		return getURL("data/"+datafile);
	}
	
	/** Returns the URL for a given file. */
	private static URL getURL(String file){
		return Utils.class.getClassLoader().getResource(file);
	}

}
