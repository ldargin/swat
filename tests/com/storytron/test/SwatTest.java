package com.storytron.test;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import storyTellerPackage.RelationshipBrowser;
import storyTellerPackage.Storyteller;

import com.storytron.enginecommon.Pointer;
import com.storytron.swat.ActorEditor;
import com.storytron.swat.PropEditor;
import com.storytron.swat.RelationshipEditor;
import com.storytron.swat.RelationshipSettings;
import com.storytron.swat.ScriptTreeCellRenderer;
import com.storytron.swat.Scriptalyzer;
import com.storytron.swat.Swat;
import com.storytron.swat.tree.TNode;
import com.storytron.swat.verbeditor.OperatorEditor;
import com.storytron.swat.verbeditor.PresenceEditor;
import com.storytron.swat.verbeditor.ScriptEditor;
import com.storytron.swat.verbeditor.SentenceDisplayEditor;
import com.storytron.swat.verbeditor.VerbEditor;
import com.storytron.swat.verbeditor.VerbPropertiesEditor;
import com.storytron.swat.verbeditor.VerbTree;
import com.storytron.uber.Actor;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Verb;
import com.storytron.uber.Deikto.TraitType;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;


/** Test of various features of Swat GUI. */
public class SwatTest {

	private static Swat swat;
	private static final String storyworld = "testdata/Test.stw";
	private static TestUndoableActionManager tam = new TestUndoableActionManager();
	
	@BeforeClass
	public static void startSwat() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				swat = new Swat(storyworld);
			}
		});
	}

	@AfterClass
	public static void closeSwat(){
		swat.getMyFrame().dispose();
	}

	@After
	public void cleanTest() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				tam.clear();
				while(swat.undoMenuItem.isEnabled())
					swat.undoMenuItem.doClick();
				Swat.Test.closeAuxWindows(swat);
				Swat.Test.openVerbEditor(swat);
				while(VerbEditor.Test.isPreviousStateButtonEnabled(swat.verbEditor))
					VerbEditor.Test.gotoPreviousState(swat.verbEditor);
				swat.verbEditor.setScriptPath(null,null);
			}
		});
		waitForAllPendingEvents();
	}

	/** Waits for all the events to be processed. */
	void waitForAllPendingEvents() throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(new Runnable(){ public void run() {}	});
	}

	@Test
	public void testVerbPropertiesEditor() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				swat.verbEditor.setVerb("happily ever after");
				final VerbPropertiesEditor vpe = VerbEditor.Test.openPropertiesEditor(swat.verbEditor);
				final float v = VerbPropertiesEditor.Test.getTrivialMomentousValue(vpe);
				Assert.assertEquals("Trivial momentous slider value differs from expected.",-0.99f,v,0.05f);
				
				VerbPropertiesEditor.Test.setTrivialMomentousValue(vpe,0.99f);
				Assert.assertEquals("Trivial momentous value differs from expected.",swat.dk.getVerb("happily ever after").getTrivial_Momentous(),0.99,0.05);
				
				final Script desirable = swat.dk.getVerb("penultimate verb").getRole("Fate").getRole().
										getOption("happily ever after").getWordSocket(2).getDesirableScript();
				Assert.assertNotNull(desirable);
				Assert.assertTrue(swat.dk.getVerb("happily ever after").isWordSocketActive(2));
				VerbPropertiesEditor.Test.disableWordSocket(vpe,2);
				Assert.assertFalse(swat.dk.getVerb("happily ever after").isWordSocketActive(2));
				Assert.assertFalse(swat.dk.getVerb("penultimate verb").getRole("Fate").getRole().
									getOption("happily ever after").isWordSocketActive(2));

				swat.undoMenuItem.doClick();
				Assert.assertTrue(desirable==swat.dk.getVerb("penultimate verb").getRole("Fate").getRole().
												getOption("happily ever after").getWordSocket(2).
												getDesirableScript());
				Assert.assertTrue(swat.dk.getVerb("penultimate verb").getRole("Fate").getRole().
						getOption("happily ever after").isWordSocketActive(2));
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(Operator.Type.Actor,swat.dk.getVerb("happily ever after").getWordSocketType(2));
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setWordSocketType(vpe,2,Operator.Type.Prop);
					}
					@Override
					public void pos() {
						Assert.assertEquals(Operator.Type.Prop,swat.dk.getVerb("happily ever after").getWordSocketType(2));
						Assert.assertTrue(swat.dk.getVerb("happily ever after").isWordSocketActive(2));
						Assert.assertEquals("?Boolean?",((Script.Node)((Script.Node)swat.dk.getVerb("penultimate verb").getRole("Fate").
								getRole().getOption("happily ever after").getWordSocket(2).getAcceptableScript().
								getRoot().getFirstChild()).getFirstChild()).getOperator().getLabel());
					}
				};
				
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertFalse(swat.dk.getVerb("happily ever after").isWordSocketActive(5));
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setWordSocketType(vpe,5,Operator.Type.Actor);
					}
					@Override
					public void pos() {
						Assert.assertEquals(Operator.Type.Actor,swat.dk.getVerb("happily ever after").getWordSocketType(5));
						Assert.assertTrue(swat.dk.getVerb("happily ever after").isWordSocketActive(5));
					}
				};
				
				
				final SentenceDisplayEditor sde = VerbEditor.Test.openSentenceDisplayEditor(swat.verbEditor);

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getVerb("happily ever after").getNote(2));
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setWordSocketNote(vpe,2,"test note");
					}
					@Override
					public void pos() {
						Assert.assertEquals("test note", swat.dk.getVerb("happily ever after").getNote(2));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(swat.dk.getVerb("happily ever after").isVisible(2));
					}
					@Override
					public void action() {
						SentenceDisplayEditor.Test.toggleWordSocketVisible(sde,2);
					}
					@Override
					public void pos() {
						Assert.assertFalse(swat.dk.getVerb("happily ever after").isVisible(2));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertFalse(swat.dk.getVerb("happily ever after").getHijackable());
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.toggleHijackable(vpe);
					}
					@Override
					public void pos() {
						Assert.assertTrue(swat.dk.getVerb("happily ever after").getHijackable());
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(swat.dk.getVerb("happily ever after").getOccupiesDirObject());
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.toggleOccupiesDirObject(vpe);
					}
					@Override
					public void pos() {
						Assert.assertFalse(swat.dk.getVerb("happily ever after").getOccupiesDirObject());
					}
				};

				new TestUndoableAction(tam){
					Script abortif;
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getVerb("happily ever after").getAbortScript());
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.toggleAbortScript(vpe);
						abortif = swat.dk.getVerb("happily ever after").getAbortScript();
					}
					@Override
					public void pos() {
						Assert.assertNotNull(abortif);
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0, swat.dk.getVerb("happily ever after").getTimeToPrepare());
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setTimeToPrepareValue(vpe, 1);
					}
					@Override
					public void pos() {
						Assert.assertEquals(1, swat.dk.getVerb("happily ever after").getTimeToPrepare());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0, swat.dk.getVerb("happily ever after").getTimeToExecute());
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setTimeToExecuteValue(vpe, 1);
					}
					@Override
					public void pos() {
						Assert.assertEquals(1, swat.dk.getVerb("happily ever after").getTimeToExecute());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals("", swat.dk.getVerb("happily ever after").getDescription().trim());
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setDescription(vpe, "test description");
					}
					@Override
					public void pos() {
						Assert.assertEquals("test description", swat.dk.getVerb("happily ever after").getDescription());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals("assent", swat.dk.getVerb("happily ever after").getExpression());
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setExpression(vpe, "awestruck");
					}
					@Override
					public void pos() {
						Assert.assertEquals("awestruck", swat.dk.getVerb("happily ever after").getExpression());
					}
				};


				tam.undo();
				tam.redo();
			}
		});
	}

	private Scriptalyzer scriptalyzer;
	@Test
	public void testScriptalyzer() throws Exception {
		Verb verb = swat.dk.getVerb("testverb");
		swat.verbEditor.setScriptPath(new ScriptPath(verb,null,null),verb.getConsequence("SetQuiet_Chatty"));
		
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				scriptalyzer = VerbEditor.Test.openScriptalyzer(swat.verbEditor);
			}
		});
		
		ArrayList<JComponent> sliders=Scriptalyzer.Test.getSliders(scriptalyzer);
		Assert.assertNull("An empty space was expected at position 0",sliders.get(0));
		Assert.assertNull("An empty space was expected at position 1",sliders.get(1));
		Assert.assertNull("An empty space was expected at position 2",sliders.get(2));
		Assert.assertNull("An empty space was expected at position 3",sliders.get(3));
		Assert.assertNull("An empty space was expected at position 4",sliders.get(4));
		Assert.assertNull("An empty space was expected at position 5",sliders.get(5));
		Assert.assertNull("An empty space was expected at position 6",sliders.get(6));
		Assert.assertTrue("A slider was expected at postion 7", sliders.get(7) instanceof JSlider);
		Assert.assertTrue("A slider was expected at postion 8", sliders.get(8) instanceof JSlider);
		Assert.assertNull("An empty space was expected at position 9",sliders.get(9));
		Assert.assertNull("An empty space was expected at position 10",sliders.get(10));
		Assert.assertTrue("A checkbox was expected at postion 11", sliders.get(11) instanceof JCheckBox);
		Assert.assertNull("An empty space was expected at position 12",sliders.get(12));
		Assert.assertNull("An empty space was expected at position 13",sliders.get(13));
		Assert.assertTrue("A slider was expected at postion 14", sliders.get(14) instanceof JSlider);
		Assert.assertTrue("A slider was expected at postion 15", sliders.get(15) instanceof JSlider);
		
		Scriptalyzer.Histogram[] hist = Scriptalyzer.Test.getHistograms(scriptalyzer);
		Assert.assertNull("No histogram was expected at position 0",hist[0]);
		Assert.assertNull("No histogram was expected at position 1",hist[1]);
		Assert.assertNull("No histogram was expected at position 2",hist[2]);
		Assert.assertNull("No histogram was expected at position 3",hist[3]);
		Assert.assertNull("No histogram was expected at position 4",hist[4]);
		Assert.assertNull("No histogram was expected at position 5",hist[5]);
		Assert.assertTrue("A numeric histogram was expected at position 6",hist[6] instanceof Scriptalyzer.NumericHistogram);
		Assert.assertTrue("A numeric histogram was expected at position 7",hist[7] instanceof Scriptalyzer.NumericHistogram);
		Assert.assertTrue("A numeric histogram was expected at position 8",hist[8] instanceof Scriptalyzer.NumericHistogram);
		Assert.assertTrue("A numeric histogram was expected at position 9",hist[9] instanceof Scriptalyzer.NumericHistogram);
		Assert.assertTrue("A boolean histogram was expected at position 10",hist[10] instanceof Scriptalyzer.BooleanHistogram);
		Assert.assertTrue("A boolean histogram was expected at position 11",hist[11] instanceof Scriptalyzer.BooleanHistogram);
		Assert.assertNull("No histogram was expected at position 12",hist[12]);
		Assert.assertNull("No histogram was expected at position 13",hist[13]);
		Assert.assertTrue("A numeric histogram was expected at position 14",hist[14] instanceof Scriptalyzer.NumericHistogram);
		Assert.assertTrue("A numeric histogram was expected at position 15",hist[15] instanceof Scriptalyzer.NumericHistogram);
	}
	
	@Test
	public void testVerbEditing() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				VerbTree.Test.addNewVerb(VerbEditor.Test.getVerbTree(swat.verbEditor));
				VerbTree.Test.renameSelectedVerb(VerbEditor.Test.getVerbTree(swat.verbEditor), "testverb2");
				Assert.assertNotNull(swat.dk.getVerb("testverb2"));
				VerbEditor.Test.addConsequence(swat.verbEditor, "SetDontMoveMe");
				Assert.assertNotNull(swat.dk.getVerb("testverb2").getConsequence("SetDontMoveMe"));
				
				swat.verbEditor.setVerb("once upon a time");
				VerbEditor.Test.addOption(swat.verbEditor, "testverb2");
				Assert.assertNotNull(swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOption("testverb2"));
				VerbEditor.Test.moveOption(swat.verbEditor, "testverb", swat.verbEditor.getRole().getRole().getOptions().size()-1);
				Assert.assertEquals(swat.verbEditor.getRole().getRole().getOptions().size()-1,swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOptionIndex("testverb"));
				
				swat.verbEditor.setVerb("testverb2");
				VerbTree.Test.deleteSelectedVerb(VerbEditor.Test.getVerbTree(swat.verbEditor));
				Assert.assertNull(swat.dk.getVerb("testverb2"));
				Assert.assertNull(swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOption("testverb2"));
			}
		});

		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				swat.undoMenuItem.doClick();
				Assert.assertNotNull(swat.dk.getVerb("testverb2"));
				Assert.assertNotNull(swat.dk.getVerb("testverb2").getConsequence("SetDontMoveMe"));
				Assert.assertNotNull(swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOption("testverb2"));
				Assert.assertEquals(swat.verbEditor.getRole().getRole().getOptions().size()-1,swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOptionIndex("testverb"));
				swat.undoMenuItem.doClick();
				Assert.assertEquals(swat.verbEditor.getRole().getRole().getOptions().size()-1,swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOptionIndex("testverb2"));
				swat.undoMenuItem.doClick();
				Assert.assertNull(swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOption("testverb2"));
				Assert.assertNotNull(swat.dk.getVerb("testverb2"));
				swat.undoMenuItem.doClick();
				Assert.assertNull(swat.dk.getVerb("testverb2").getConsequence("SetDontMoveMe"));
				swat.undoMenuItem.doClick();
				Assert.assertNull(swat.dk.getVerb("testverb2"));
			}
		});
		
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				swat.redoMenuItem.doClick();
				Assert.assertNotNull(swat.dk.getVerb("testverb2"));
				Assert.assertNull(swat.dk.getVerb("testverb2").getConsequence("SetDontMoveMe"));
				swat.redoMenuItem.doClick();
				Assert.assertNotNull(swat.dk.getVerb("testverb2").getConsequence("SetDontMoveMe"));
				Assert.assertNull(swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOption("testverb2"));
				swat.redoMenuItem.doClick();
				Assert.assertNotNull(swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOption("testverb2"));
				Assert.assertEquals(swat.verbEditor.getRole().getRole().getOptions().size()-1,swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOptionIndex("testverb2"));
				swat.redoMenuItem.doClick();
				Assert.assertEquals(swat.verbEditor.getRole().getRole().getOptions().size()-1,swat.dk.getVerb("once upon a time").getRole("Fate").getRole().getOptionIndex("testverb"));
				swat.redoMenuItem.doClick();
				Assert.assertNull(swat.dk.getVerb("testverb2"));
			}
		});
	}
	
	@Test
	public void testCopyrightEditing() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				Swat.Test.showCopyrightEditor(swat);
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						Swat.Test.editCopyrightText(swat,"This is a sample copyright.");
					}
					@Override
					public void pos() {
						Assert.assertEquals("copyright text does not match","This is a sample copyright.",swat.dk.getCopyright());
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals("copyright text does not match","This is a sample copyright.",swat.dk.getCopyright());
					}
					@Override
					public void action() {
						Swat.Test.editCopyrightText(swat,"This is another sample copyright.");
					}
					@Override
					public void pos() {
						Assert.assertEquals("copyright text does not match","This is another sample copyright.",swat.dk.getCopyright());
					}
				};
				
				tam.undo();
				tam.redo();
			}
		});
	}

	@Test
	public void testDeiktoLimits() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final int rCount = swat.dk.roleCount;
				final int oCount = swat.dk.optionCount;

				new TestUndoableAction(tam,2){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						VerbTree.Test.addNewVerb(VerbEditor.Test.getVerbTree(swat.verbEditor));
						VerbTree.Test.renameSelectedVerb(VerbEditor.Test.getVerbTree(swat.verbEditor), "testverb3");
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getVerb("testverb3"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						VerbEditor.Test.addConsequence(swat.verbEditor, "SetDontMoveMe");
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getVerb("testverb3").getConsequence("SetDontMoveMe"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						VerbEditor.Test.addRole(swat.verbEditor, "my role");
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getVerb("testverb3").getRole("my role"));
						Assert.assertEquals("expected role count does not match",rCount+1, swat.dk.roleCount);
						Assert.assertEquals("expected option count does not match",oCount, swat.dk.optionCount);
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getVerb("testverb3").getRole("my role").getRole().getEmotionScript("AdjustDebt_Grace"));
					}
					@Override
					public void action() {
						VerbEditor.Test.addEmotion(swat.verbEditor, "AdjustDebt_Grace");
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getVerb("testverb3").getRole("my role").getRole().getEmotionScript("AdjustDebt_Grace"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getVerb("testverb3").getAbortScript());
					}
					@Override
					public void action() {
						swat.verbEditor.verbPropertiesEditor.setVisible(true);
						VerbPropertiesEditor.Test.toggleUseAbortScript(swat.verbEditor.verbPropertiesEditor);
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getVerb("testverb3").getAbortScript());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNotNull(swat.dk.getVerb("testverb3").getRole("my role"));
						Assert.assertEquals("expected role count does not match",rCount+1, swat.dk.roleCount);
						Assert.assertEquals("expected option count does not match",oCount, swat.dk.optionCount);
					}
					@Override
					public void action() {
						VerbEditor.Test.deleteRole(swat.verbEditor);
					}
					@Override
					public void pos() {
						Assert.assertNull("role was not deleted",swat.dk.getVerb("testverb3").getRole("my role"));
						Assert.assertEquals("expected role count does not match",rCount, swat.dk.roleCount);
						Assert.assertEquals("expected option count does not match",oCount, swat.dk.optionCount);
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getVerb("testverb3").getRole("my role"));
						Assert.assertEquals("expected role count does not match",rCount, swat.dk.roleCount);
						Assert.assertEquals("expected option count does not match",oCount, swat.dk.optionCount);
					}
					@Override
					public void action() {
						VerbTree.Test.deleteSelectedVerb(VerbEditor.Test.getVerbTree(swat.verbEditor));
					}
					@Override
					public void pos() {
						Assert.assertNull(swat.dk.getVerb("testverb3"));
						Assert.assertEquals("expected role count does not match",rCount, swat.dk.roleCount);
						Assert.assertEquals("expected option count does not match",oCount, swat.dk.optionCount);
					}
				};
				
				tam.undo();
				tam.redo();
			}
		});
	}
	
	@Test
	public void testActorEditor() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final ActorEditor ae = Swat.Test.openActorEditor(swat);
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getActor("new actor"));
					}
					@Override
					public void action() {
						ActorEditor.Test.addActor(ae);
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getActor("new actor"));
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						ActorEditor.Test.renameSelectedActor(ae,"test actor");
					}
					@Override
					public void pos() {
						Assert.assertNull(swat.dk.getActor("new actor"));
						Assert.assertNotNull(swat.dk.getActor("test actor"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getTrait(TraitType.Actor, "new Actor trait"));
					}
					@Override
					public void action() {
						ActorEditor.Test.addTrait(ae);
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getTrait(TraitType.Actor, "new Actor trait"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("Settest trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("SetPtest trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("SetCtest trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("test trait"));
						Assert.assertNull(swat.dk.getTrait(TraitType.Actor, "test trait"));
					}
					@Override
					public void action() {
						ActorEditor.Test.renameTrait(ae, "new Actor trait", "test trait");
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getTrait(TraitType.Actor, "test trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("Settest trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("SetPtest trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("SetCtest trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("test trait"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor").get(swat.dk.getTrait(TraitType.Actor, "test trait")));
					}
					@Override
					public void action() {
						ActorEditor.Test.setTraitValue(ae, "test trait",0.5f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(0.5f,swat.dk.getActor("test actor").get(swat.dk.getTrait(TraitType.Actor, "test trait")),0.02);
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor").get(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
					}
					@Override
					public void action() {
						ActorEditor.Test.setTraitValue(ae, "Cool_Volatile",-0.5f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(-0.5f,swat.dk.getActor("test actor").get(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")),0.02);
						Assert.assertEquals(0.5f,swat.dk.getActor("test actor").get(swat.dk.getTrait(TraitType.Actor, "test trait")),0.02);
					}
				};

				final PropEditor pe = Swat.Test.openPropEditor(swat);
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(swat.dk.getActor("Protagonist"),swat.dk.getActor(swat.dk.getProp("BareProp").getOwner()));
					}
					@Override
					public void action() {
						PropEditor.Test.setOwner(pe,"test actor");
					}
					@Override
					public void pos() {
						Assert.assertEquals(swat.dk.getActor("test actor"),swat.dk.getActor(swat.dk.getProp("BareProp").getOwner()));
					}
				};

				Swat.Test.openActorEditor(swat);

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNotNull(swat.dk.getTrait(TraitType.Actor, "test trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("test trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("Settest trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("SetPtest trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("SetCtest trait"));
					}
					@Override
					public void action() {
						ActorEditor.Test.deleteTrait(ae,"test trait");
					}
					@Override
					public void pos() {
						Assert.assertNull(swat.dk.getTrait(TraitType.Actor, "test trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("test trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("Settest trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("SetPtest trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("SetCtest trait"));
					}
				};

				final Pointer<Actor> a=new Pointer<Actor>(swat.dk.getActor("test actor"));
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(a.get()==swat.dk.getActor("test actor"));
						Assert.assertEquals(swat.dk.getActor("test actor"),swat.dk.getActor(swat.dk.getProp("BareProp").getOwner()));
					}
					@Override
					public void action() {
						ActorEditor.Test.deleteSelectedActor(ae);
					}
					@Override
					public void pos() {
						Assert.assertNull(swat.dk.getActor("test actor"));
						Assert.assertEquals(swat.dk.getActor("Fate"),swat.dk.getActor(swat.dk.getProp("BareProp").getOwner()));
					}
				};

				tam.undo();
				tam.redo();
			}
		});
	}

	@Test
	public void testActorTextTraitEditor() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final ActorEditor ae = Swat.Test.openActorEditor(swat);
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getActor("new actor"));
					}
					@Override
					public void action() {
						ActorEditor.Test.addActor(ae);
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getActor("new actor"));
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getActor("test actor"));
					}
					@Override
					public void action() {
						ActorEditor.Test.renameSelectedActor(ae,"test actor");
					}
					@Override
					public void pos() {
						Assert.assertNull(swat.dk.getActor("new actor"));
						Assert.assertNotNull(swat.dk.getActor("test actor"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getTextTrait(TraitType.Actor, "new Actor text trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("new Actor text trait"));
					}
					@Override
					public void action() {
						ActorEditor.Test.addTextTrait(ae);
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getTextTrait(TraitType.Actor, "new Actor text trait"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getTextTrait(TraitType.Actor, "test text trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("test text trait"));
						Assert.assertNotNull(swat.dk.getTextTrait(TraitType.Actor, "new Actor text trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("new Actor text trait"));
					}
					@Override
					public void action() {
						ActorEditor.Test.renameTextTrait(ae, "new Actor text trait", "test text trait");
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getTextTrait(TraitType.Actor, "test text trait"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("test text trait"));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getActor("test actor").getText(swat.dk.getTextTrait(TraitType.Actor, "test text trait")));
					}
					@Override
					public void action() {
						ActorEditor.Test.setTextTraitValue(ae, "test text trait","test value1");
					}
					@Override
					public void pos() {
						Assert.assertEquals("test value1",swat.dk.getActor("test actor").getText(swat.dk.getTextTrait(TraitType.Actor, "test text trait")));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNotNull(swat.dk.getTextTrait(TraitType.Actor, "test text trait"));
						Assert.assertEquals("test value1",swat.dk.getActor("test actor").getText(swat.dk.getTextTrait(TraitType.Actor, "test text trait")));
					}
					@Override
					public void action() {
						ActorEditor.Test.deleteTextTrait(ae,"test text trait");
					}
					@Override
					public void pos() {
						Assert.assertNull(swat.dk.getTextTrait(TraitType.Actor, "test text trait"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("test text trait"));
					}
				};

				
				final Pointer<Actor> a = new Pointer<Actor>(swat.dk.getActor("test actor"));
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(a.get()==swat.dk.getActor("test actor"));
						Assert.assertNotNull(swat.dk.getActor("test actor"));
					}
					@Override
					public void action() {
						ActorEditor.Test.deleteSelectedActor(ae);
					}
					@Override
					public void pos() {
						Assert.assertNull(swat.dk.getActor("test actor"));
					}
				};
				

				tam.undo();
				tam.redo();
			}
		});
	}
	
	@Test
	public void testRelationshipSettings() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final RelationshipSettings r = Swat.Test.openRelationships(swat);
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertFalse(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
					}
					@Override
					public void action() {
						RelationshipSettings.Test.toggleRelationshipVisibility(r, swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"));
					}
					@Override
					public void pos() {
						Assert.assertTrue(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						RelationshipSettings.Test.toggleRelationshipVisibility(r, swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"));
					}
					@Override
					public void pos() {
						Assert.assertFalse(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
					}
				};

				tam.undo();
				tam.redo();


				swat.undoMenuItem.doClick();
				Assert.assertTrue(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
				swat.undoMenuItem.doClick();
				Assert.assertFalse(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
				swat.redoMenuItem.doClick();
				Assert.assertTrue(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
				swat.redoMenuItem.doClick();
				Assert.assertFalse(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
				swat.undoMenuItem.doClick();
				Assert.assertTrue(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));

				Assert.assertTrue(swat.dk.areRelationshipsVisible());
				RelationshipSettings.Test.toggleRelationshipsVisible(r);
				Assert.assertFalse(swat.dk.areRelationshipsVisible());
				RelationshipSettings.Test.toggleRelationshipsVisible(r);
				Assert.assertTrue(swat.dk.areRelationshipsVisible());

				swat.undoMenuItem.doClick();
				Assert.assertFalse(swat.dk.areRelationshipsVisible());
				swat.undoMenuItem.doClick();
				Assert.assertTrue(swat.dk.areRelationshipsVisible());
				swat.redoMenuItem.doClick();
				Assert.assertFalse(swat.dk.areRelationshipsVisible());
				swat.redoMenuItem.doClick();
				Assert.assertTrue(swat.dk.areRelationshipsVisible());
			}
		});
	}

	@Test
	public void testRelationshipSettings2() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final RelationshipSettings r = Swat.Test.openRelationships(swat);
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertFalse(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
					}
					@Override
					public void action() {
						RelationshipSettings.Test.toggleRelationshipVisibility(r, swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"));
					}
					@Override
					public void pos() {
						Assert.assertTrue(swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(swat.dk.areRelationshipsVisible());
					}
					@Override
					public void action() {
						RelationshipSettings.Test.toggleRelationshipsVisible(r);
					}
					@Override
					public void pos() {
						Assert.assertFalse(swat.dk.areRelationshipsVisible());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						RelationshipSettings.Test.toggleRelationshipsVisible(r);
					}
					@Override
					public void pos() {
						Assert.assertTrue(swat.dk.areRelationshipsVisible());
					}
				};

				tam.undo();
				tam.redo();
			}
		});
	}

	@Test
	public void testRelationshipEditor() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final ActorEditor ae = Swat.Test.openActorEditor(swat);
				ActorEditor.Test.addActor(ae);
				ActorEditor.Test.renameSelectedActor(ae,"test actor2");

				ActorEditor.Test.addTrait(ae);
				ActorEditor.Test.renameTrait(ae, "new Actor trait", "test trait2");

				final RelationshipEditor re = Swat.Test.openRelationshipEditor(swat);
				RelationshipEditor.Test.setSelectedFromActor(re, "Protagonist");
				RelationshipEditor.Test.setSelectedToActor(re, "test actor2");

				new TestUndoableAction(tam,2){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.0f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setPerceptionValue(re, "test trait2",0.5f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(0.5f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
					}
				};

				new TestUndoableAction(tam,2){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.0f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setPerceptionValue(re, "Cool_Volatile",-0.5f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(-0.5f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
					}
				};

				new TestUndoableAction(tam,2){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.5f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setBackPerceptionValue(re, "test trait2",0.25f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(0.25f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.5f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
					}
				};

				new TestUndoableAction(tam,2){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(-0.5f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setBackPerceptionValue(re, "Cool_Volatile",-0.25f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(-0.25f,swat.dk.getActor("test actor2").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(-0.5f,swat.dk.getActor("Protagonist").getP(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0.99f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.99f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setConfidenceValue(re, "test trait2",0.25f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(0.25f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
						Assert.assertEquals(0.99f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.0f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setConfidenceValue(re, "Cool_Volatile",-0.25f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(-0.25f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0.99f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.25f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setBackConfidenceValue(re, "test trait2",0.125f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(0.125f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(0.25f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "test trait2"),swat.dk.getActor("test actor2")),0.02);
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(0.0f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(-0.25f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
					}
					@Override
					public void action() {
						RelationshipEditor.Test.setBackConfidenceValue(re, "Cool_Volatile",-0.125f);
					}
					@Override
					public void pos() {
						Assert.assertEquals(-0.125f,swat.dk.getActor("test actor2").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("Protagonist")),0.02);
						Assert.assertEquals(-0.25f,swat.dk.getActor("Protagonist").getC(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"),swat.dk.getActor("test actor2")),0.02);
					}
				};

				tam.undo();
				tam.redo();
			}
		});
	}

	@Test
	public void testOptionRoleLinks() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final VerbEditor ve = Swat.Test.openVerbEditor(swat);
				ve.setVerb("testverb");
				VerbEditor.Test.addRole(ve, "new test role");
				VerbEditor.Test.addOption(ve, "penultimate verb");
				swat.copyRole.doClick();

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(ve.getRole().getLabel(), "new test role");
						Assert.assertFalse(VerbEditor.Test.isRoleLinkButtonEnabled(ve));
						Assert.assertFalse(VerbEditor.Test.isOptionLinkButtonEnabled(ve));
					}
					@Override
					public void action() {
						swat.pasteRoleLink.doClick();
					}
					@Override
					public void pos() {
						Assert.assertTrue(VerbEditor.Test.isRoleLinkButtonEnabled(ve));
						Assert.assertTrue(VerbEditor.Test.isOptionLinkButtonEnabled(ve));
						ArrayList<JMenuItem> roleLinkItems = VerbEditor.Test.getRoleLinkItems(ve);
						Assert.assertEquals("unlink",roleLinkItems.get(0).getText());
						Assert.assertEquals("testverb: new test role",roleLinkItems.get(1).getText());
						ArrayList<JMenuItem> optionLinkItems = VerbEditor.Test.getOptionLinkItems(ve);
						Assert.assertTrue(!"unlink".equals(optionLinkItems.get(0).getText()));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						ve.setRole("link to new test role");
					}
					@Override
					public void action() {
						VerbTree.Test.duplicateSelectedVerb(VerbEditor.Test.getVerbTree(ve));
					}
					@Override
					public void pos() {
						Assert.assertTrue(VerbEditor.Test.isRoleLinkButtonEnabled(ve));
						Assert.assertTrue(VerbEditor.Test.isOptionLinkButtonEnabled(ve));
						ArrayList<JMenuItem> roleLinkItems = VerbEditor.Test.getRoleLinkItems(ve);
						Assert.assertEquals(4,roleLinkItems.size());
						Assert.assertEquals("unlink",roleLinkItems.get(0).getText());
						Assert.assertEquals("testverb: new test role",roleLinkItems.get(1).getText());
						Assert.assertEquals("testverb: link to new test role",roleLinkItems.get(2).getText());
						Assert.assertEquals("copy of testverb: link to new test role",roleLinkItems.get(3).getText());
						ArrayList<JMenuItem> optionLinkItems = VerbEditor.Test.getOptionLinkItems(ve);
						Assert.assertTrue(!"unlink".equals(optionLinkItems.get(0).getText()));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						ArrayList<JMenuItem> roleLinkItems = VerbEditor.Test.getRoleLinkItems(ve);
						roleLinkItems.get(0).doClick();
					}
					@Override
					public void pos() {
						Assert.assertFalse(VerbEditor.Test.isRoleLinkButtonEnabled(ve));
						Assert.assertTrue(VerbEditor.Test.isOptionLinkButtonEnabled(ve));
						ArrayList<JMenuItem> optionLinkItems = VerbEditor.Test.getOptionLinkItems(ve);
						Assert.assertEquals(4,optionLinkItems.size());
						Assert.assertEquals("unlink",optionLinkItems.get(0).getText());
						Assert.assertEquals("testverb: new test role: penultimate verb",optionLinkItems.get(1).getText());
						Assert.assertEquals("testverb: link to new test role: penultimate verb",optionLinkItems.get(2).getText());
						Assert.assertEquals("copy of testverb: link to new test role: penultimate verb",optionLinkItems.get(3).getText());
					}
				};

				tam.undo();
				tam.redo();
			}
		});
	}
	
	@Test
	public void testStorytellerRelationshipsWindow() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final ActorEditor ae = Swat.Test.openActorEditor(swat); 
				ActorEditor.Test.addActor(ae);
				ActorEditor.Test.renameSelectedActor(ae,"test actor3");
				ActorEditor.Test.addTrait(ae);
				ActorEditor.Test.renameTrait(ae, "new Actor trait", "test trait3");

				final RelationshipEditor re = Swat.Test.openRelationshipEditor(swat);
				RelationshipEditor.Test.setSelectedFromActor(re, "Protagonist");
				RelationshipEditor.Test.setSelectedToActor(re, "test actor3");

				RelationshipEditor.Test.setPerceptionValue(re, "test trait3",0.5f);
				RelationshipEditor.Test.setPerceptionValue(re, "Cool_Volatile",-0.5f);

				RelationshipEditor.Test.setBackPerceptionValue(re, "test trait3",0.25f);
				RelationshipEditor.Test.setBackPerceptionValue(re, "Cool_Volatile",-0.25f);

				final RelationshipSettings r = Swat.Test.openRelationships(swat);
				if (!swat.dk.isRelationshipVisible(swat.dk.getTrait(TraitType.Actor, "Cool_Volatile")))
					RelationshipSettings.Test.toggleRelationshipVisibility(r, swat.dk.getTrait(TraitType.Actor, "Cool_Volatile"));
			}
		});
		
		final Storyteller st = Swat.Test.openStoryteller(swat);
		Thread.sleep(1000);
		waitForAllPendingEvents();
		
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final RelationshipBrowser rb = Storyteller.Test.openRelationships(st);
				RelationshipBrowser.Test.setSelectedRelationship(rb, "Cool_Volatile");
				RelationshipBrowser.Test.setSelectedActor(rb, "test actor3");
				Assert.assertEquals("slightly cool",RelationshipBrowser.Test.getRelationshipValue(rb, "Protagonist"));
				RelationshipBrowser.Test.setSelectedActor(rb, "Protagonist");
				Assert.assertEquals("somewhat cool",RelationshipBrowser.Test.getRelationshipValue(rb, "test actor3"));
				RelationshipBrowser.Test.setSelectedRelationship(rb, "test trait3");
				RelationshipBrowser.Test.setSelectedActor(rb, "test actor3");
				Assert.assertEquals("medium-large",RelationshipBrowser.Test.getRelationshipValue(rb, "Protagonist"));
				RelationshipBrowser.Test.setSelectedActor(rb, "Protagonist");
				Assert.assertEquals("large",RelationshipBrowser.Test.getRelationshipValue(rb, "test actor3"));
			}
		});
	}
	
	@Test
	public void testVerbEditorStateStack() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				VerbEditor.Test.toggleConsequence(swat.verbEditor, "SetQuiet_Chatty");
				Assert.assertEquals(swat.dk.getVerb("testverb").getConsequence("SetQuiet_Chatty"),swat.verbEditor.getScriptBeingEdited());

				swat.verbEditor.setVerb("happily ever after");
				VerbEditor.Test.toggleAssumeRoleIf(swat.verbEditor);
				Assert.assertEquals(swat.dk.getVerb("happily ever after").getRole("Player").getRole().getAssumeRoleIfScript(),swat.verbEditor.getScriptBeingEdited());

				swat.verbEditor.setVerb("testverb");
				Assert.assertNotNull(swat.dk.getVerb("testverb").getConsequence("SetQuiet_Chatty"));
				VerbEditor.Test.deleteConsequence(swat.verbEditor, "SetQuiet_Chatty");
				Assert.assertNull(swat.dk.getVerb("testverb").getConsequence("SetQuiet_Chatty"));

				VerbEditor.Test.gotoPreviousState(swat.verbEditor);
				Assert.assertTrue(VerbEditor.Test.isPreviousStateButtonEnabled(swat.verbEditor));
				Assert.assertEquals(swat.dk.getVerb("happily ever after").getRole("Player").getRole().getAssumeRoleIfScript(),swat.verbEditor.getScriptBeingEdited());
				
				VerbEditor.Test.gotoPreviousState(swat.verbEditor);
				Assert.assertFalse(VerbEditor.Test.isPreviousStateButtonEnabled(swat.verbEditor));
				Assert.assertNull(swat.verbEditor.getScriptBeingEdited());
				Assert.assertEquals("testverb",swat.verbEditor.getVerb().getLabel());

				VerbEditor.Test.gotoNextState(swat.verbEditor);
				Assert.assertTrue(VerbEditor.Test.isPreviousStateButtonEnabled(swat.verbEditor));
				Assert.assertTrue(VerbEditor.Test.isNextStateButtonEnabled(swat.verbEditor));
				Assert.assertEquals(swat.dk.getVerb("happily ever after").getRole("Player").getRole().getAssumeRoleIfScript(),swat.verbEditor.getScriptBeingEdited());
				
				VerbEditor.Test.gotoNextState(swat.verbEditor);
				Assert.assertTrue(VerbEditor.Test.isPreviousStateButtonEnabled(swat.verbEditor));
				Assert.assertFalse(VerbEditor.Test.isNextStateButtonEnabled(swat.verbEditor));
				Assert.assertNull(swat.verbEditor.getScriptBeingEdited());
				Assert.assertEquals("testverb",swat.verbEditor.getVerb().getLabel());
			}
		});
	}
	
	@Test
	public void testOperatorEditor() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final OperatorEditor oe = Swat.Test.openOperatorEditor(swat);

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("newBNumberOperator"));
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("TestBNOperator"));
					}
					@Override
					public void action() {
						OperatorEditor.Test.createNewOperator(oe, Operator.Type.BNumber);
					}
					@Override
					public void pos() {
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("newBNumberOperator"));
					}
				};
				
				final Pointer<Operator> op = new Pointer<Operator>();

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("TestBNOperator"));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("newBNumberOperator"));
					}
					@Override
					public void action() {
						OperatorEditor.Test.renameOperator(oe, "TestBNOperator");
						op.set(swat.dk.getOperatorDictionary().getOperator("TestBNOperator"));

					}
					@Override
					public void pos() {
						Assert.assertNotNull(op.get());
						Assert.assertEquals(0,op.get().getCArguments());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("TestBNOperator"));
					}
					@Override
					public void action() {
						OperatorEditor.Test.createNewParameter(oe, Operator.Type.Actor);
					}
					@Override
					public void pos() {
						Assert.assertEquals(1,op.get().getCArguments());
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals("Actor",op.get().getArgumentLabel(0));
					}
					@Override
					public void action() {
						OperatorEditor.Test.renameParameter(oe,0,"TestAParam");
					}
					@Override
					public void pos() {
						Assert.assertEquals("TestAParam",op.get().getArgumentLabel(0));
					}
				};
				
				final VerbEditor ve=Swat.Test.openVerbEditor(swat);
				Verb v=swat.dk.getVerb("testverb");
				ve.setScriptPath(new ScriptPath(v,null,null),v.getConsequence("SetQuiet_Chatty"));
				Assert.assertEquals("SetQuiet_Chatty",ve.getScriptBeingEdited().getLabel());

				final ScriptEditor<VerbEditor.State> se = VerbEditor.Test.getScriptEditor(ve);
				Node n=(Node)se.getScript().getRoot().getChildAt(1);
				se.setSelectedNode(n);
				Assert.assertEquals(n,se.getSelectedNode());
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("Blend of:",ScriptTreeCellRenderer.getTokenLabel(n));
					}
					@Override
					public void action() {
						se.processTokenMenu(OperatorEditor.Test.getOperator(oe), false, false,null);
					}
					@Override
					public void pos() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals(n.getOperator(),OperatorEditor.Test.getOperator(oe));
						Assert.assertEquals("TestAParam?",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(0)));
						Assert.assertEquals(n.getChildAt(0),se.getSelectedNode());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
					}
					@Override
					public void action() {
						se.processTokenMenu(swat.dk.getOperatorDictionary().getOperator("ThisDirObject"), false, false,null);
					}
					@Override
					public void pos() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("ThisDirObject",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(0)));
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals("TestAParam",op.get().getArgumentLabel(0));
						Assert.assertEquals(1,op.get().getCArguments());
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals(1,n.getChildCount());
					}
					@Override
					public void action() {
						OperatorEditor.Test.createNewParameter(oe, Operator.Type.BNumber);
					}
					@Override
					public void pos() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("BNumber?",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(1)));
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals("TestAParam",op.get().getArgumentLabel(0));
						Assert.assertEquals("BNumber",op.get().getArgumentLabel(1));
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("ThisDirObject",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(0)));
					}
					@Override
					public void action() {
						OperatorEditor.Test.renameParameter(oe,1,"TestBNParam");
					}
					@Override
					public void pos() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("TestBNParam",op.get().getArgumentLabel(1));
						Assert.assertEquals(2,op.get().getCArguments());
						Assert.assertEquals("TestBNParam?",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(1)));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("ThisDirObject",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(0)));
						Assert.assertEquals("TestBNParam?",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(1)));
						Assert.assertEquals("TestAParam",op.get().getArgumentLabel(0));
					}
					@Override
					public void action() {
						OperatorEditor.Test.deleteParameter(oe,0);
					}
					@Override
					public void pos() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("TestBNParam",op.get().getArgumentLabel(0));
						Assert.assertEquals(1,op.get().getCArguments());
						Assert.assertEquals("TestBNParam?",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(0)));
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertNull(op.get().getToolTipText());
					}
					@Override
					public void action() {
						OperatorEditor.Test.setOperatorDescription(oe, "a test description");
					}
					@Override
					public void pos() {
						Assert.assertEquals("a test description",op.get().getToolTipText());
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertEquals("TestBNOperator of:",ScriptTreeCellRenderer.getTokenLabel(n));
						Assert.assertEquals("TestBNParam?",ScriptTreeCellRenderer.getTokenLabel((Node)n.getChildAt(0)));
						Assert.assertNotNull(swat.dk.getOperatorDictionary().getOperator("TestBNOperator"));
						Assert.assertSame(op.get(),swat.dk.getOperatorDictionary().getOperator("TestBNOperator"));
					}
					@Override
					public void action() {
						OperatorEditor.Test.deleteOperator(oe);
					}
					@Override
					public void pos() {
						Node n=(Node)se.getScript().getRoot().getChildAt(1);
						Assert.assertNull(swat.dk.getOperatorDictionary().getOperator("TestBNOperator"));
						Assert.assertEquals("BNumber?",ScriptTreeCellRenderer.getTokenLabel(n));
					}
				};
				
				tam.undo();
				tam.redo();
			}
		});
	}
	
	@Test
	public void testSniffing() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				VerbEditor ve = Swat.Test.openVerbEditor(swat);
				ve.setVerb("happily ever after");
				VerbPropertiesEditor vpe = VerbEditor.Test.openPropertiesEditor(ve);
				VerbPropertiesEditor.Test.setWordSocketType(vpe, 9, Operator.Type.Actor);
				
				ve.setVerb("penultimate verb");
				Role.Link r=ve.getVerb().getRole("Fate");
				Role.Option option = r.getRole().getOption("happily ever after");
				ve.setScriptPath(new ScriptPath(ve.getVerb(),r,option),option.getWordSocket(9).getAcceptableScript());
				ScriptEditor<?> se = VerbEditor.Test.getScriptEditor(ve);
				se.setSelectedNode((TNode)se.getScript().getRoot().getFirstChild());
				se.setSelectedNode((TNode)((TNode)se.getScript().getRoot().getFirstChild()).getFirstChild());
				se.processTokenMenu(OperatorDictionary.getGlobalOperator("AreSameActor"), false, false, null);
				
				se.setSelectedNode((TNode)((TNode)((TNode)se.getScript().getRoot().getFirstChild()).getFirstChild()).getFirstChild());
			}
		});
	}

	@Test
	public void testPresenceAndWitnessesEditing() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				final VerbEditor ve = Swat.Test.openVerbEditor(swat);
				ve.setVerb("testverb");
				final VerbPropertiesEditor vpe = VerbEditor.Test.openPropertiesEditor(ve);
				final JCheckBox[] ws = VerbPropertiesEditor.Test.getWitnessJCBs(vpe);
				final PresenceEditor.BooleanUndefinedControl[] cs = VerbPropertiesEditor.Test.getPresenceControls(vpe);

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(cs[2].getParent()!=null);
						Assert.assertTrue(cs[5]==null || cs[5].getParent()==null);
						Assert.assertTrue(cs[9]==null || cs[9].getParent()==null);
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setWordSocketType(vpe, 5, Operator.Type.Prop);
					}
					@Override
					public void pos() {
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(cs[2].getParent()!=null);
						Assert.assertTrue(cs[5]==null || cs[5].getParent()==null);
						Assert.assertTrue(cs[9]==null || cs[9].getParent()==null);
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setWordSocketType(vpe, 9, Operator.Type.Actor);
					}
					@Override
					public void pos() {
					}
				};
				
				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(ws[2].getParent()!=null && !ws[2].isEnabled());
						Assert.assertTrue(ws[5]==null || ws[5].getParent()==null);
						Assert.assertTrue(ws[9].getParent()!=null && !ws[9].isEnabled());
						Assert.assertEquals(Verb.Witnesses.ANYBODY_ON_STAGE, ve.getVerb().getWitnesses());
						Assert.assertTrue(cs[2].getParent()!=null);
						Assert.assertTrue(cs[5]==null || cs[5].getParent()==null);
						Assert.assertTrue(cs[9].getParent()!=null);
					}
					@Override
					public void action() {
						VerbPropertiesEditor.Test.setWitnesses(vpe,Verb.Witnesses.CUSTOM_SPEC);
					}
					@Override
					public void pos() {
						Assert.assertTrue(ws[2].isEnabled());
						Assert.assertTrue(ws[9].isEnabled());
						Assert.assertEquals(Verb.Witnesses.CUSTOM_SPEC, ve.getVerb().getWitnesses());
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertTrue(ve.getVerb().isWitness(9));
					}
					@Override
					public void action() {
						ws[9].doClick();
					}
					@Override
					public void pos() {
						Assert.assertFalse(ve.getVerb().isWitness(9));
					}
				};

				new TestUndoableAction(tam){
					@Override
					public void pre() {
						Assert.assertEquals(Verb.Presence.REQUIRED,ve.getVerb().getPresence(9));
					}
					@Override
					public void action() {
						cs[9].rightCB.doClick();
					}
					@Override
					public void pos() {
						Assert.assertEquals(Verb.Presence.ABSENT,ve.getVerb().getPresence(9));
					}
				};

				tam.undo();
				tam.redo();
			}
		});
	}

	/** 
	 * A class for encapsulating actions with preconditions and posconditions.
	 * The precondition is evaluated before the action, before redoing and after undoing.
	 * The poscondition is evaluated after the action, before undoing and after redoing.
	 * <p>
	 * Using this class should avoid having to write the pre and postconditions more than 
	 * once in the body of a test routine. 
	 * */
	private abstract class TestUndoableAction {
		int actionCount;
		public TestUndoableAction(TestUndoableActionManager tam){
			this(tam,1);
		}
		public TestUndoableAction(TestUndoableActionManager tam,int actionCount){
			this.actionCount = actionCount;
			tam.register(this);
			pre();
			action();
			pos();
		}
		public abstract void pre();
		public abstract void pos();
		public abstract void action();
		
		public final void undo(){
			for(int i=0;i<actionCount;i++)
				swat.undoMenuItem.doClick();
		}
		public final void redo(){
			for(int i=0;i<actionCount;i++)
				swat.redoMenuItem.doClick();
		}
	}
	
	/** 
	 * This manager keeps trac of all the actions in a test, and has methods
	 * to perform undoing and redoing of all the actions.
	 * */
	private static final class TestUndoableActionManager {
		ArrayList<TestUndoableAction> actions = new ArrayList<TestUndoableAction>();
		public void register(TestUndoableAction ta) {
			actions.add(ta);
		}
		public void redo(){	
			for(TestUndoableAction ta:actions) {
				ta.pre();
				ta.redo();
				ta.pos();
			}
		}
		public void undo(){
			ListIterator<TestUndoableAction> li = actions.listIterator(actions.size());
			while(li.hasPrevious())	{
				final TestUndoableAction ta=li.previous();
				ta.pos();
				ta.undo();
				ta.pre();
			}
		}
		public void clear(){ actions.clear(); }
	}
}
