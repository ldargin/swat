package com.storytron.swat.verbeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Role;
import com.storytron.uber.Sentence;
import com.storytron.uber.Verb;
import com.storytron.uber.Role.Option;
import com.storytron.uber.Role.Option.OptionWordSocket;
import com.storytron.uber.operator.Operator;

/** A panel for editing word sockets. */
public class SocketEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int ROW_HEIGHT = 23;
	private final JComboBox[] socketComboBox = new JComboBox[Sentence.MaxWordSockets];
	private final Swat.TextField[] noteFields = new Swat.TextField[Sentence.MaxWordSockets];
	private VerbEditor verbEditor;
	VerbPropertiesEditor verbPropertiesEditor;

	public SocketEditor(VerbEditor tVerbEditor,VerbPropertiesEditor vpe) {
		super(null);

		verbEditor = tVerbEditor;
		verbPropertiesEditor = vpe;
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBackground(Utils.darkBackground);
		configure();
	}

	private static class TypeComboBoxItems {
		int i;
		Operator.Type t;
	
		public TypeComboBoxItems(int i,Operator.Type t){
			this.i=i;
			this.t=t;
		}
		@Override
		public String toString() { 
			if (-1==i)
				return "";
			else return String.valueOf(i)+t; 
		}
	}
	
	private void configure() {
		JComponent topBox=Box.createHorizontalBox();
		topBox.add(Box.createHorizontalGlue());
		topBox.add(Box.createRigidArea(new Dimension(100,20)));
		JLabel label = new JLabel("Note to myself");
		label.setToolTipText(Utils.toHtmlTooltipFormat("It's a good idea to explain to yourself what this WordSocket means so that you don't get confused in the future. This text will popup on the label for the WordSocket in Options using this Verb. Very handy!"));
		topBox.add(label);
		topBox.add(Box.createRigidArea(new Dimension(60,20)));

		JComponent combobPanel = new JPanel(new GridLayout(0,1));
		JComponent notesPanel = new JPanel(new GridLayout(0,1));
		
		combobPanel.setOpaque(false);
		notesPanel.setOpaque(false);
		
		for (int iSock=0; (iSock < Sentence.MaxWordSockets); ++iSock) {
			final int iSocket = iSock;
			
			socketComboBox[iSocket] = new JComboBox();

			// add Socket label
			switch (iSocket) {
			case 0: { 
				JLabel l = new JLabel("Subject");
				Dimension d = new Dimension(l.getPreferredSize().width,ROW_HEIGHT);
				l.setPreferredSize(d);
				l.setAlignmentX(1.0f);
				combobPanel.add(l);
				break; 
				}
			case 1: { 
				JLabel l = new JLabel("Verb");
				Dimension d = new Dimension(l.getPreferredSize().width,ROW_HEIGHT);
				l.setPreferredSize(d);
				l.setAlignmentX(1.0f);
				combobPanel.add(l);
				break; 
			}
			default: {
				socketComboBox[iSocket].setBackground(Color.white);
				socketComboBox[iSocket].setMaximumRowCount(10);
				socketComboBox[iSocket].setActionCommand("block");
				socketComboBox[iSocket].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (!e.getActionCommand().equals("block")) {
							socketComboBox[iSocket].hidePopup();
							final Verb verb = verbEditor.getVerb(); 
							
							final boolean oldIsActive = verb.isWordSocketActive(iSocket);
							
							TypeComboBoxItems selection = (TypeComboBoxItems) socketComboBox[iSocket].getSelectedItem();
							final boolean newIsActive = Operator.Type.UnType != selection.t;
							
							// Do nothing if type is not changed.
							if (newIsActive==oldIsActive && newIsActive && selection.t==verb.getWordSocketType(iSocket))
								return;

							// Store the old wordsockets, and the references to them. 
							final LinkedList<OptionWordSocket> wss = oldIsActive?new LinkedList<OptionWordSocket>():null;
							if (oldIsActive) {
								for (Verb zVerb: verbEditor.swat.dk.getVerbs())
									for (Role.Link zRole: zVerb.getRoles()) 
										for (Option zOption: zRole.getRole().getOptions()) 
											if (zOption.getLabel().equals(verb.getLabel()))
												wss.add(zOption.getWordSocket(iSocket));
							}
							
							final Verb.WSData oldWSData = verb.getWSData(iSocket);
							final Verb.WSData newWSData = newIsActive?new Verb.WSData(selection.t):null;
							
							if (newIsActive) {
								newWSData.text = verb.defaultWordsocketTextScript(iSocket);
								newWSData.suffix = verb.defaultSuffixScript(iSocket,"");
							}
							verb.setWSData(iSocket, newWSData);
							
							changeWordSocket(verb, iSocket, newIsActive);
							refreshSocketBox(iSocket);

							// Store the new scripts in case of redoing
							final LinkedList<OptionWordSocket> nwss = newIsActive?new LinkedList<OptionWordSocket>():null;
							if (newIsActive) {
								for (Verb zVerb: verbEditor.swat.dk.getVerbs()) 
									for (Role.Link zRole: zVerb.getRoles()) 
										for (Option zOption: zRole.getRole().getOptions()) 
											if (zOption.getLabel().equals(verb.getLabel()))
												nwss.add(zOption.getWordSocket(iSocket));
							}
							if (verbEditor.getOption()!=null &&
									verbEditor.getOption().getPointedVerb()==verb) {
								// redraw the Option panel
								Option option = verbEditor.getOption();
								verbEditor.setOption(null);
								verbEditor.setOption(option); 
							}
							if (verbEditor.sentenceDisplayEditor.isVisible())
								verbEditor.sentenceDisplayEditor.refresh();
							verbPropertiesEditor.witnessesEditor.reloadActorWordsockets();
							verbPropertiesEditor.presenceEditor.reloadActorWordsockets();
							
							new UndoableAction(verbEditor.swat,false,"set wordsocket of "+verbEditor.getVerb().getLabel()){
								private static final long serialVersionUID = 1L;
								@Override
								public void myRedo() {
									verbEditor.setVerb(verb);
									
									verb.setWSData(iSocket, newWSData);
									if (newIsActive) {
										Iterator<OptionWordSocket> itDesirable = nwss.iterator();
										for (Verb zVerb: verbEditor.swat.dk.getVerbs()) 
											for (Role.Link zRole: zVerb.getRoles()) 
												for (Option zOption: zRole.getRole().getOptions()) 
													if (zOption.getLabel().equals(verb.getLabel()))
														zOption.setWordSocket(iSocket,itDesirable.next());
									}

									refreshSocketBox(iSocket);

									if (verbEditor.getOption()!=null &&
											verbEditor.getOption().getPointedVerb()==verb) {
										// redraw the Option panel
										Option option = verbEditor.getOption();
										verbEditor.setOption(null);
										verbEditor.setOption(option); 
									}
									verbPropertiesEditor.setVisible(true);
									if (verbEditor.sentenceDisplayEditor.isVisible())
										verbEditor.sentenceDisplayEditor.refresh();
									verbPropertiesEditor.witnessesEditor.reloadActorWordsockets();
									verbPropertiesEditor.presenceEditor.reloadActorWordsockets();
								}
								@Override
								public void myUndo() {
									verbEditor.setVerb(verb);
									
									verb.setWSData(iSocket, oldWSData);
									refreshSocketBox(iSocket);
									
									if (oldIsActive) {
										Iterator<OptionWordSocket> itDesirable = wss.iterator();
										for (Verb zVerb: verbEditor.swat.dk.getVerbs()) 
											for (Role.Link zRole: zVerb.getRoles()) 
												for (Option zOption: zRole.getRole().getOptions()) 
													if (zOption.getLabel().equals(verb.getLabel()))
														zOption.setWordSocket(iSocket,itDesirable.next());
									} else
										changeWordSocket(verb, iSocket, false);
									
									if (verbEditor.getOption()!=null &&
											verbEditor.getOption().getPointedVerb()==verb) {
										// redraw the Option panel
										Option option = verbEditor.getOption();
										verbEditor.setOption(null);
										verbEditor.setOption(option); 
									}
									verbPropertiesEditor.setVisible(true);
									if (verbEditor.sentenceDisplayEditor.isVisible())
										verbEditor.sentenceDisplayEditor.refresh();
									verbPropertiesEditor.witnessesEditor.reloadActorWordsockets();
									verbPropertiesEditor.presenceEditor.reloadActorWordsockets();
								}
							};
						}
					}
				});

				socketComboBox[iSocket].addItem(new TypeComboBoxItems(-1,Operator.Type.UnType));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.Actor));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.Prop));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.Stage));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.Verb));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.ActorTrait));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.PropTrait));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.StageTrait));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.MoodTrait));
				socketComboBox[iSocket].addItem(new TypeComboBoxItems(iSocket+1,Operator.Type.Quantifier));
				
				socketComboBox[iSocket].setPreferredSize(new Dimension(50,ROW_HEIGHT));
				socketComboBox[iSocket].setAlignmentX(1.0f);
				combobPanel.add(socketComboBox[iSocket]);
			}	
			}

			// add Notes TextField
			final Swat.TextField notesField = new Swat.TextField();
			notesField.setPreferredSize(new Dimension(150,ROW_HEIGHT));
			notesField.addActionListener(new EditorListener(notesField){
				private static final long serialVersionUID = 1L;
				@Override
				public boolean timedActionPerformed(ActionEvent e) {
					final Verb verb = verbEditor.getVerb();
					final String oldv=verb.getNote(iSocket);
					final String newv=notesField.getText().trim();
					if (newv.equals(oldv)) return true;
					
					verb.getWSData(iSocket).note = newv;
					
					new UndoableAction(verbEditor.swat,false,"change wordsocket note"){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							verbEditor.setVerb(verb);
							verb.getWSData(iSocket).note = newv;
							notesField.setText(newv);
							verbPropertiesEditor.setVisible(true);
							notesField.requestFocusInWindow();
						}
						@Override
						public void myUndo() {
							verbEditor.setVerb(verb);
							verb.getWSData(iSocket).note = oldv;
							notesField.setText(oldv);
							verbPropertiesEditor.setVisible(true);
							notesField.requestFocusInWindow();
						}
					};
					return true;
				}
				@Override
				public String getText() { return verbEditor.getVerb().getNote(iSocket); }
			});
			notesPanel.add(notesField);
			noteFields[iSocket] = notesField;

			if (socketComboBox[iSocket]!=null) 
				socketComboBox[iSocket].setActionCommand("");
		}
		
		JComponent bottomPanel = Box.createHorizontalBox();
		bottomPanel.add(combobPanel);
		bottomPanel.add(notesPanel);

		topBox.setAlignmentX(1.0f);
		add(topBox);
		bottomPanel.setAlignmentX(1.0f);
		add(bottomPanel);
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.darkGray),"WordSockets"));
	}

	private void changeWordSocket(Verb tVerb, int tiWordSocket, boolean isAdditionNotDeletion) {
		for (Verb zVerb: verbEditor.swat.dk.getVerbs()) {
			for (Role.Link zRole: zVerb.getRoles()) {
				for (Option zOption: zRole.getRole().getOptions()) {
					if (zOption.getLabel().equals(tVerb.getLabel())) {
						if (isAdditionNotDeletion) {
							// We must add the new WordSocket
							zOption.initWordSocket(tiWordSocket,true);
						} else
							// We must delete the existing WordSocket
							zOption.setWordSocket(tiWordSocket,null);
					}
				}
			}		
		}
	}

	private int indexOf(JComboBox jcb,Operator.Type wt){
		for(int i=0;i<jcb.getItemCount();i++)
			if (((TypeComboBoxItems)jcb.getItemAt(i)).t==wt)
				return i;
		return -1;
	}
	
	public void refresh(){
		for(int i=0;i<Sentence.MaxWordSockets;i++) 
			refreshSocketBox(i);
	}
	
	private void refreshSocketBox(int i){
		if (verbEditor.getVerb().isWordSocketActive(i))
			noteFields[i].setText(verbEditor.getVerb().getNote(i));
		else
			noteFields[i].setText("");
		noteFields[i].setEnabled(verbEditor.getVerb().isWordSocketActive(i));

		if (i>1) {
			socketComboBox[i].setActionCommand("block");
			if (verbEditor.getVerb().isWordSocketActive(i))
				socketComboBox[i].setSelectedIndex(indexOf(socketComboBox[i],verbEditor.getVerb().getWordSocketType(i)));
			else
				socketComboBox[i].setSelectedIndex(indexOf(socketComboBox[i],Operator.Type.UnType));
			socketComboBox[i].setActionCommand("");
		}
			
	}
	
	public abstract static class Test {
		public static void disableWordSocket(SocketEditor se,int i){
			se.socketComboBox[i].setSelectedIndex(0);
		}
		public static void setWordSocketType(SocketEditor se,int iWs,Operator.Type t){
			for(int i=0;i<se.socketComboBox[iWs].getItemCount();i++) {
				if (((TypeComboBoxItems)se.socketComboBox[iWs].getItemAt(i)).t==t){
					se.socketComboBox[iWs].setSelectedIndex(i);
					break;
				}
			}
		}
		public static void setWordSocketNote(SocketEditor se,int i,String note){
			se.noteFields[i].setText(note);
			for(ActionListener al:se.noteFields[i].getActionListeners())
				al.actionPerformed(null);
		}
	}
	
}
