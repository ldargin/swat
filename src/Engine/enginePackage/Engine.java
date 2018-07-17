package Engine.enginePackage;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.MenuElement;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Prop;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Sentence;
import com.storytron.uber.Stage;
import com.storytron.uber.Verb;
import com.storytron.uber.operator.Operator;

public final class Engine implements AbstractEngine {
	public static int FATE = 0;
	public static int MAXEVENTS = 1000; // this is used to cut off infinite loop storyworlds
	public static String deadEndMessage = "No options here; try something else.";
	private boolean storyIsOver; // flag used to communicate that the story has ended
	private boolean isEpilogueNow; // flag used to communicate that the epilogue has begun
	private boolean isPenultimateDone; // flag used to indicate that the penultimate verb has been executed;
	private boolean isHappilyDone; // flag used to indicate that the happily ever after verb has been executed;
	private int cMoments; // raw count of ticks that have elapsed since the beginning of the story
	private int cInactivity = 0; // counter for inactivity
	private int playerInactivity =0;
	
	private int iMinute, iHour, iDay; // the days, hours, and minutes that have elapsed since the beginning of the story
//	private int iProtagonist; // index of the Actor played by the human
	private int iReactingActor; // index of the Actor reacting to an Event
	private int timeLimit; // an upper limit on how long the story can progress without activity until terminated by the engine
	private ArrayList<Sentence> historyBook; // the record of Events that have taken place
	private Sentence chosenPlan; // the plan that is is being built
	public Deikto dk;
	private Sentence thisEvent; // the Event currently being reacted to
	private Sentence hypotheticalEvent; // not currently in use; intended for use in processing of actor anticipation
	private Interpreter interpreter;
	private LinkedList<Alarm> alarms = new LinkedList<Alarm>();
	private boolean permitFateToReact, fatesRole;
	private boolean abortIf; // used to abort a Plan if conditions have changed at time of execution
	private Random random;
	private EnginePlayerIO playerIO;
	RehearsalLogger rehearsalLogger = null;
	boolean runningRehearsal = false;
	public LinkedList<String> storybook = new LinkedList<String>();
	BlockingQueue<String> storybookQueue = new LinkedBlockingQueue<String>();
	int inputCounter=0; // This is used by LogDataCollector.java
	
	public EngineLogger logger; 
//**********************************************************************
	public Engine(Deikto tdk, EnginePlayerIO pip) {
		this(tdk,pip,new EngineLogger());
		logger.setLogging(false);
	}
	public Engine(Deikto tdk, EnginePlayerIO pip,EngineLogger logger) {
		// switching off the logger while we don't use it.
		this.logger = logger;
		dk = tdk;
		playerIO = pip;
		chosenPlan = new Sentence(dk.getActorCount());
		hypotheticalEvent = new Sentence(dk.getActorCount());
		historyBook = new ArrayList<Sentence>();
		interpreter = new Interpreter(this, dk);
		cMoments = 0;
		iMinute = 0;
		iHour = 0;
		iDay = 0;
//		iProtagonist = 1;
		timeLimit = tdk.getInactivityTimeout();
		cInactivity=0;
		playerInactivity=0;
		random = new Random(); // insert a longint here if you want repeatable randomness
	}
	
	/** Sets player IO handler to use to get player input. */
	public void setPlayerInputIO(EnginePlayerIO pIO){
		playerIO = pIO;
	}
	/** Gets the player IO handler. */
	public EnginePlayerIO getPlayerInputIO(){
		return playerIO;
	}
//**********************************************************************	
	public static class Alarm implements Serializable {
		private static final long serialVersionUID = 0L;
		final static int MEETACTOR = 0;
		final static int MEETPROP = 1;
		final static int MEETSTAGE = 2;
		final static int MEETTIME = 3;
		int actor1;
		int type;
		int index;
		public Alarm(int tActor1, int tType, int tIndex) {
			actor1 = tActor1;
			type = tType;
			index = tIndex;
		}
		
		public void assertEquals(Alarm a){
			assert actor1==a.actor1;
			assert type==a.type;
			assert index==a.index;
		}
	}

	public void logScriptMsg(ScriptPath sp,Script s) throws InterruptedException {	logger.scriptMsg(sp,s);	}
	public void logValueMsgChild(String s) throws InterruptedException {	logger.valueMsgChild(s);	}
	public void logPoisonMsgChild(String s) throws InterruptedException {	logger.poisonMsgChild(s);	}
	public void logTokenMsg(Operator op) throws InterruptedException {	logger.tokenMsg(op);	}
	public void loggerUp() throws InterruptedException { logger.up(); }
//**********************************************************************		
	public boolean getStoryIsOver() { return storyIsOver; }
//**********************************************************************	
	public void setStoryIsOver(boolean newValue) { storyIsOver = newValue; }
//**********************************************************************	
	public void setPermitFateToReact() { permitFateToReact = true; }
//**********************************************************************	
	public void setFatesRole() { fatesRole = true; }
//**********************************************************************		
	public void setAbortIf(boolean newValue) { abortIf = newValue; }
//**********************************************************************		
//	public int getIProtagonist() { return iProtagonist; }
//**********************************************************************	
//	public void setIProtagonist(int newValue) { iProtagonist = newValue; }
//**********************************************************************		
	public int getHistoryBookSize() { return historyBook.size(); }
//**********************************************************************		
	public Sentence getHistoryBookPage(int iPageNumber) { return historyBook.get(iPageNumber); }
//**********************************************************************		
	public Sentence getThisEvent() { return thisEvent; }
	public boolean isLogging() { return logger.isLogging(); };
//**********************************************************************		
	public Sentence getHypotheticalEvent() { return hypotheticalEvent; }
	public int getHypotheticalEventIWord(int wordSocket) {	return hypotheticalEvent.getIWord(wordSocket); }
//**********************************************************************		
	public Sentence getChosenPlan() { return chosenPlan; }
	public int getChosenPlanIWord(int wordSocket){ return chosenPlan.getIWord(wordSocket); }
//**********************************************************************		
	public int getCMoments() { return cMoments; }
//**********************************************************************		
	public int getIMinute() { return iMinute; }
//**********************************************************************		
	public int getIHour() { return iHour; }
//**********************************************************************		
	public int getIDay() { return iDay; }
	public void setTimeLimit(int tTimeLimit) { timeLimit = tTimeLimit; }
	public int getTimeLimit() { return timeLimit; }
//**********************************************************************	
	public Interpreter getInterpreter() {
		return interpreter;
	}
//**********************************************************************		
	public void init() {
		storyIsOver = false;
		isEpilogueNow = false;
		isPenultimateDone = false;
		isHappilyDone = false;
		cMoments = 0;
		iMinute = 0;
		iHour = 0;
		iDay = 0;
		historyBook.clear();
		for (Actor ac:dk.getActors()) {
			ac.setOccupiedUntil(0);
		}

		// Special case: for "once upon a time", Fate is always the Subject
		int subjectActor = FATE;
		// otherwise, chose a DirObject randomly
		Sentence newEvent = new Sentence(dk.getActorCount());
		newEvent.setWordSocket(Sentence.Subject, subjectActor, Operator.Type.Actor);
		newEvent.setWordSocket(Sentence.Verb, 0, Operator.Type.Verb);
		if (dk.getVerb(newEvent.getIWord(Sentence.Verb)).isWordSocketActive(Sentence.DefDirObject))
			newEvent.setWordSocket(Sentence.DefDirObject, FATE, Operator.Type.Actor);
		else 
			newEvent.setWordSocket(Sentence.DefDirObject, -1, Operator.Type.UnType);
		
		newEvent.setCausalEvent(-1);
		newEvent.setTime(0);
		newEvent.setLocation(dk.getActor(subjectActor).getLocation());
		((Actor)dk.getActor(subjectActor)).plans.add(newEvent);
	}
//**********************************************************************
/*
	private ArrayList<Sentence> buildTale(int tiActor1, int tiActor2) {
		// This method figures out what Actor1 has to say to Actor2
		ArrayList<Sentence> result = new ArrayList<Sentence>();
		
		// Step 1: sweep through all Events in the HistoryBook,
		//   looking for the event that Actor1 DOES know about and Actor2 does NOT know about,
		//   having the highest value of trivial_Momentous.
		float bestTrivial_Momentous = -1.00f;
		int bestEvent = -1;
		for (int i=0; (i < historyBook.size()); ++i) {
			if (historyBook.get(i).getWhoKnows(tiActor1) && !historyBook.get(i).getWhoKnows(tiActor2)) {
				float zTrivial_Momentous = dk.getVerb(historyBook.get(i).getIWord(Sentence.VERB)).getTrivial_Momentous();
				if (zTrivial_Momentous > bestTrivial_Momentous) {				
					bestTrivial_Momentous = zTrivial_Momentous;
					bestEvent = i;
				}
			}
		}
		
		if (bestEvent >= 0) { // Did we find such an Event?
			// yes, now prepare the Tale.
			// First, work backwards up the causal chain from the headline as far as we know
			int zEvent = bestEvent;  // this is our loop control variable
			int firstEvent = bestEvent; // this will be the first KNOWN event in the causal chain
			while (historyBook.get(historyBook.get(zEvent).getCausalEvent()).getWhoKnows(tiActor1)) {
				firstEvent = zEvent;
				zEvent = historyBook.get(zEvent).getCausalEvent();
			}
		}
		else { // no such Event found, nothing to talk about
		}
		return result;
	}
	*/
//**********************************************************************		
  public int getReactingActor() {
  	return(iReactingActor);
  }
//**********************************************************************		
  public void setReactingActor(int newIReactingActor) {
	  // this is used by the Interpreter to run independent scripts
  	iReactingActor = newIReactingActor;
  }
//**********************************************************************	
  public int getEventCount() {
  	return historyBook.size();
  }
//**********************************************************************	
  public int getEventCausalEvent(int pageNumber) {
	  if (pageNumber<historyBook.size())
	  		return historyBook.get(pageNumber).getCausalEvent();
	  	else 
	  		return -1;
  }
//**********************************************************************
  	/** @return true iff the actor reacted. */
	private boolean reactSentence(int tiReactingActor, Sentence tEvent) throws InterruptedException {
		// This method determines whether tiReactingActor has any reaction to tEvent.
		// first task: determine if I have a Role to play
		boolean reacted=false;
		
		iReactingActor = tiReactingActor; // set up the higher-scope variable
 		int iRole = 0;
		boolean roleSearchIsOver = false;
		int iVerb = tEvent.getIWord(Sentence.Verb);
		Verb zVerb = dk.getVerb(iVerb);
		boolean triggerSent = false;
		
		// this section allows us to skip any Event with these properties:
		// event.verb.timeToExecute == 0
		// iReactingActor = protagonist
		
		while ((iRole < zVerb.getRoleCount()) && !roleSearchIsOver) {
			Role.Link zRoleLink = zVerb.getRole(iRole);
			Role zRole = zRoleLink.getRole();
			ScriptPath sp = new ScriptPath(zVerb,zRoleLink,null);
			if (runningRehearsal)
				rehearsalLogger.reactSentenceRoleCandidacy(iVerb, iRole);
				
			// the next line gets the result of the script and uses it for the decision
			permitFateToReact = false; // set this up for the test 7 lines down.
			fatesRole = false; // set up for forcing Fate only to react
			interpreter.executeScript(sp,((Script)zRole.getAssumeRoleIfScript()));
			if (interpreter.getPoison()) {
				if (runningRehearsal)
					rehearsalLogger.recordPoison(sp,zRole.getAssumeRoleIfScript(), interpreter.getPoisonCause());
			} else
				logger.scriptResultMsgChild(interpreter.getBoolean());
			
			boolean zBoolean = true;
			if (iReactingActor==FATE) 
				zBoolean = permitFateToReact | fatesRole;
			else
				zBoolean = !fatesRole;
			if (interpreter.getBoolean() && !interpreter.getPoison() && zBoolean) { // we found a Role!
				reacted=true;
				logger.roleMsg(dk.getActor(iReactingActor), zRoleLink);
				
				// Yes! I do assume this Role!
				roleSearchIsOver = (iReactingActor!=FATE); // this permits Fate to have multiple reactions to an event
				if (runningRehearsal) 
					rehearsalLogger.reactSentenceRoleActivation(iVerb, iRole);
				//janus.saveToLog("    "+dk.getActor(iReactingActor).label+" executing Role "+zRole.getLabel(), sD);
				
				// Execute any emotional reactions
				for (Script sc:zRole.getEmotions()) {
					interpreter.executeScript(sp,sc);					
					if (interpreter.getPoison() && runningRehearsal) 
						rehearsalLogger.recordPoison(sp,sc, interpreter.getPoisonCause());						
				}
				if (!thisEvent.getHijacked() & zRole.getOptions().size()>0) { // explore Options only if there are some
					if (iReactingActor == 1) { // the Protagonist is DEFINED to be Actor #1
//						if (dk.getActor(iReactingActor).getHuman()) {
						triggerSent = true;
						chosenPlan = playerInput(iRole,zRoleLink, iVerb, tEvent);
					} else
						chosenPlan = algorithmicInput(iRole, iVerb, zVerb, zRoleLink);
				} else 
					chosenPlan = null;
				
				
				if (chosenPlan != null) {
					// In the next line, we know that the Event being reacted to is the most
					//   recent addition to the historyBook, hence its index gives us the CausalEvent.
					setPlan(dk.getActor(iReactingActor),chosenPlan.clone(), tEvent.getPageNumber(), cMoments);
					//janus.saveToLog(dumpSentence("Setting plan: ", ((Actor)dk.getActor(iReactingActor)).plans.get(((Actor)dk.getActor(iReactingActor)).plans.size()-1)),sD);
					
					// set hijacking
					if (zVerb.getHijackable()) {
						tEvent.setHijacked(true);
					}
				}
				logger.up(); // roleMsg
			} 
			++iRole;
		}
		if (!triggerSent && (!roleSearchIsOver || (chosenPlan==null)) 
				&& (iReactingActor == 1) // the Protagonist is DEFINED to be Actor #1
//				&& (dk.getActor(iReactingActor).getHuman()) 
				&& iReactingActor != tEvent.getIWord(Sentence.Subject)) {
			// This Actor has witnessed the Event but has no Role to play
			// OR no Option to select.
			// We must nevertheless notify the Player if Player == iReactingActor.
			// The only way to do that is give him an Event with only an OK to respond
			setupTriggerSentence(tEvent);
		}
		return reacted;
	}
//**********************************************************************
	private void setPlan(Actor a, Sentence newPlan, int tiCausalEvent, int tTicks) {
		newPlan.setCausalEvent(tiCausalEvent);
		if ((tiCausalEvent>=0) && historyBook.get(tiCausalEvent).getIsPartOfEpilogue())
			newPlan.setIsPartOfEpilogue();
		int iLocation = dk.getActor(iReactingActor).getLocation();
		
		// this IF-statement insures that Plans by Fate take place on the DirObject's Stage
		if ((iReactingActor == FATE) && (newPlan.getDirObject() > 0)) {
			iLocation = dk.getActor(newPlan.getDirObject()).getLocation();
		}
		newPlan.setLocation(iLocation);
		int iVerb = newPlan.getIWord(Sentence.Verb);
		int preparationTime = dk.getVerb(iVerb).getTimeToPrepare();
		if (dk.getVerb(iVerb).getLabel().equals("arrive at")) {
			// special treatment for this verb
			int zTargetStage = newPlan.getIWord(2);	// target stage is 2
			int myLocation = newPlan.getIWord(3); // this is only for computational purposes
			preparationTime += (int)(dk.getStage(myLocation).getTravelingTime(dk.getStage(zTargetStage)));			
		}
		newPlan.setTime(tTicks+preparationTime);
		a.plans.add(newPlan);
		// CC Sept 12, 2007
		// got rid of this expression because for FATE and 'depart', it yields incorrect results		
		// dk.getActor(newPlan.getIWord(Sentence.Subject)).
		
		// CC Jan 13, 2008 Added if-statement to insure that Fate doesn't get stopped
		if (a!=dk.getActor(FATE))
			a.setOccupiedUntil(tTicks+preparationTime);
	}
//**********************************************************************
	private void setupTriggerSentence(Sentence tEvent) throws InterruptedException {
		Verb zVerb = dk.getVerb(tEvent.getIWord(Sentence.Verb));
		LabeledSentence tLabeledSentence = new LabeledSentence();
		tLabeledSentence.rawSentence = tEvent;
		if (zVerb.getLabel().equals("happily ever after")) {
			switch (tEvent.getIWord(3)) {
				case 0: { tLabeledSentence.expressionLabel = "fighting"; break; }
				case 1: { tLabeledSentence.expressionLabel = "disgusted"; break; }
				case 2: { tLabeledSentence.expressionLabel = "angry"; break; }
				case 3: { tLabeledSentence.expressionLabel = "irritated"; break; }
				case 4: { tLabeledSentence.expressionLabel = "disdain"; break; }
				case 5: { tLabeledSentence.expressionLabel = "formal"; break; }
				case 6: { tLabeledSentence.expressionLabel = "confident"; break; }
				case 7: { tLabeledSentence.expressionLabel = "proud"; break; }
				case 8: { tLabeledSentence.expressionLabel = "friendly"; break; }
				case 9: { tLabeledSentence.expressionLabel = "surpriseGood"; break; }
				case 10: { tLabeledSentence.expressionLabel = "exultant"; break; }
			}
		}
		else tLabeledSentence.expressionLabel = zVerb.getExpression();
		tLabeledSentence.stageLabel = dk.getStage(tEvent.getLocation()).getLabel();
		tLabeledSentence.suffixes = new String[Sentence.MaxWordSockets];
		tLabeledSentence.visible = zVerb.getVisible();
		int currentLocation = dk.getActor(iReactingActor).getLocation();
		for (int k=1; (k<dk.getActorCount()); ++k) {
			if ((k != iReactingActor) & (currentLocation != Stage.Nowhere) & (dk.getActor(k).getLocation() == currentLocation)) {
				tLabeledSentence.actorsPresent.add(dk.getActor(k).getLabel());
			}
		}
		for (Prop zProp:dk.getProps()) {
			if ((zProp.getLocation() == currentLocation) && zProp.getVisible() && zProp.getInPlay()) {
				tLabeledSentence.propsPresent.add(zProp.getLabel());
			}
		}
		ScriptPath sp = new ScriptPath(zVerb,null,null);
		interpreter.leftPanel = true;
		long oldSeed = interpreter.scriptRandom.getSeed();
		boolean newSeed = false;
		if (tEvent.seed!=0)
			interpreter.scriptRandom.setSeed(tEvent.seed);
		else {
			newSeed = true;
			tEvent.seed = interpreter.scriptRandom.getSeed();
		}
		for (int k=0; (k<Sentence.MaxWordSockets); ++k) {
			if (tEvent.getIWord(k)>=0)  {
				tLabeledSentence.suffixes[k] = calculateWordsocketSuffix(tEvent, sp, k, tEvent.getIWord(k));
				tLabeledSentence.labels[k] = calculateWordsocketLabel(tEvent,sp,k,tEvent.getIWord(k));
				tLabeledSentence.descriptions[k] = dk.getDescriptionByType(zVerb.getWordSocketType(k), tEvent.getIWord(k));
			} else
				tLabeledSentence.suffixes[k] = "";
		}
		if (!newSeed)
		  interpreter.scriptRandom.setSeed(oldSeed);
		playerIO.sendTriggerSentence(tLabeledSentence, cMoments, (dk.getStageCount()>2), iReactingActor);
	}
//**********************************************************************
	private Sentence playerInput(int tiRole, Role.Link tRole, int tIVerb, Sentence tEvent) throws InterruptedException {
		 
//		First we set up the fixed sentence that the player is responding to.
		setupTriggerSentence(tEvent);
		/* 
		Next, we start sending the player the options for his choices for
		WordSockets, starting with the Verb WordSocket (WordSocket #1).
		This is done with the call to getPlayerChoice(). This returns an
		integer reflecting the index of the menu item that the player chose.
		If the player instead chose to press the Back button, then return
		value will be equal to -1, in which case, then we must back up 
		to the last WordSocket that gave the player a choice, i.e., the 
		last WordSocket that is active and has more than one choice.
		If the value of choice is greater than or equal to zero, then it
		represents the player's choice of words and we must store that into
		the building sentence and move to the next active WordSocket -- and
		if that WordSocket has just one choice, we must fill it in and move
		on to the next WordSocket, until we reach the end of the sentence,
		at which point we activate the Done button and wait for the player's
		input, which will be either true for "Done" or false for "Back".
	*/ 

		interpreter.leftPanel = false;
		chosenPlan = new Sentence(dk.getActorCount());
		chosenPlan.setCausalEvent(tEvent.getPageNumber());
		chosenPlan.setWordSocket(Sentence.Subject, iReactingActor, Operator.Type.Actor);
		chosenPlan.setWordSocket(Sentence.Verb, -1, Operator.Type.Verb);
		chosenPlan.seed = interpreter.scriptRandom.getSeed();
		LabeledSentence zLabeledSentence = new LabeledSentence();
		ScriptPath spZero = new ScriptPath(dk.getVerb(0),null,null);
		zLabeledSentence.labels[Sentence.Subject] = calculateWordsocketLabel(chosenPlan,spZero,Sentence.Subject, iReactingActor);
		zLabeledSentence.suffixes[Sentence.Subject] = calculateWordsocketSuffix(chosenPlan,spZero,Sentence.Subject, iReactingActor);
		zLabeledSentence.visible[0] = dk.getVerb(0).isVisible(Sentence.Subject);
		int iWordSocket = 1;
		int iOption = -1;
		Verb chosenVerb = null;
		Verb parentVerb = dk.getVerb(tIVerb);
		ScriptPath chosenSP = null, optionSP=null;
		Role.Option zOption = null;
		int finalChoice = 0;
		ArrayList<MenuElement> menuElements = new ArrayList<MenuElement>();
		ArrayList<Integer> wordIndexes = new ArrayList<Integer>();
		ArrayList<Long> choicePointSeeds = new ArrayList<Long>();

		// calculate options
		ArrayList<MenuElement> optionElements = new ArrayList<MenuElement>();
		ArrayList<Integer> optionWordIndexes = new ArrayList<Integer>();
		long optionChoicePointSeed = interpreter.scriptRandom.getSeed();
		figureMenuElements(parentVerb,optionSP,optionElements,optionWordIndexes,iWordSocket, chosenSP, tRole, zOption);

		// There is no plan to choose
		if (optionElements.size()==0)
			return null;
		
		do {
			do {
				int choice = 0;
				do {
					if ((iWordSocket == Sentence.Verb) || (chosenVerb.isWordSocketActive(iWordSocket))) {
						long choicePointSeed;
						if (iWordSocket!=Sentence.Verb) {
							logger.wordsocketMsg(chosenSP.getVerb().getWordSocketFullLabel(iWordSocket));
							choicePointSeed = interpreter.scriptRandom.getSeed();
							figureMenuElements(parentVerb,optionSP,menuElements,wordIndexes,iWordSocket, chosenSP, tRole, zOption);
						} else {
							choicePointSeed = optionChoicePointSeed;
							menuElements.clear();
							menuElements.addAll(optionElements);
							wordIndexes.clear();
							wordIndexes.addAll(optionWordIndexes);
						}
							
						// first we must let the Player choose the verb.
						// The verb is treated separately from the other WordSockets.
						if (chosenVerb != null) {
							for (int j = 0; (j < iWordSocket); ++j) {
								if (chosenVerb.isWordSocketActive(j) && chosenVerb.isVisible(j)) {
									zLabeledSentence.labels[j] = calculateWordsocketLabel(chosenPlan,chosenSP, j, chosenPlan.getIWord(j));
									zLabeledSentence.descriptions[j] = dk.getDescriptionByType(chosenVerb.getWordSocketType(j), chosenPlan.getIWord(j));
									zLabeledSentence.suffixes[j] = calculateWordsocketSuffix(chosenPlan,chosenSP, j, chosenPlan.getIWord(j));
								}
							}
							if ((chosenVerb.isWordSocketActive(iWordSocket)) & (chosenVerb.isVisible(iWordSocket))) {
								chosenPlan.setWordSocket(iWordSocket, -1, chosenVerb.getWordSocketType(iWordSocket));
							}
						}
						if (menuElements.size() == 1) // if there's only one choice, force it onto the player
							choice = 0;
						else { // the choice is not forced upon the player
							zLabeledSentence.rawSentence = chosenPlan;
							zLabeledSentence.expressionLabel = null;
							zLabeledSentence.descriptions[0] = dk.getDescriptionByType(Operator.Type.Actor, chosenPlan.getIWord(0));
							if (chosenPlan.getIWord(Sentence.Verb)>=0) {
								zLabeledSentence.expressionLabel = dk.getVerb(chosenPlan.getIWord(Sentence.Verb)).getExpression();
							}
							if (menuElements.size() == 0) { // if there are no choices, put in the dead end message
								zLabeledSentence.labels[iWordSocket] = deadEndMessage;
								zLabeledSentence.descriptions[iWordSocket] = "There's no valid choice for this slot, so you must back up and try something else.";
							}
							else {
								if ((chosenVerb!=null) && !chosenVerb.isVisible(iWordSocket)) {
									zLabeledSentence.labels[iWordSocket] = "(invisible)";
									zLabeledSentence.descriptions[iWordSocket] = "This WordSocket is set to be invisible, but the Acceptable Script fails to specify its word.";									
								}
							}
 							choice = playerIO.getPlayerSelection(zLabeledSentence, menuElements, iWordSocket,iReactingActor);
 							inputCounter++;
						}
						if (choice>=0)
 							choicePointSeeds.add(choicePointSeed);
						if (iWordSocket == Sentence.Verb && 0<=choice && choice<tRole.getRole().getOptions().size()) {
							iOption = wordIndexes.get(choice);
							zOption = tRole.getRole().getOptions().get(iOption);
							chosenVerb = zOption.getPointedVerb();
							chosenSP = new ScriptPath(chosenVerb,null,null);
							zLabeledSentence.visible = chosenVerb.getVisible();
							chosenPlan.setWordSocket(Sentence.Verb, chosenVerb.getReference().getIndex(), Operator.Type.Verb);
							optionSP = new ScriptPath(parentVerb,tRole,zOption);
							logger.optionMsg(zOption);
							logger.valueMsgChild(String.valueOf(0.0f));
							logger.wordsocketsMsg();
						} else if ((choice >= 0) && (chosenVerb.isWordSocketActive(iWordSocket))) {
							Operator.Type zType = chosenVerb.getWordSocketType(iWordSocket);
							chosenPlan.setWordSocket(iWordSocket, wordIndexes.get(choice), zType);
							zLabeledSentence.suffixes[iWordSocket] = calculateWordsocketSuffix(chosenPlan,chosenSP, iWordSocket, chosenPlan.getIWord(iWordSocket));
							if (logger.isLogging())
								logger.valueMsgChild(interpreter.getValueString(zType, wordIndexes.get(choice), null));
							logger.up(); // wordsocketMsg()
						} else if (choice<0 && iWordSocket == Sentence.Verb && runningRehearsal) // abort choice procedure 
							return null;
					}
					++iWordSocket;
				} while ((choice >= 0) && (iWordSocket < Sentence.MaxWordSockets));
				if (choice < 0) { // user clicked on the Undo button
					boolean oldIsLogging = logger.isLogging();
					boolean optionChosen = chosenPlan.getWordSocket(Sentence.Verb).getIWord()!=-1;
					logger.setLogging(false); // turn of logging during undo
					--iWordSocket;
					chosenPlan.setWordSocket(iWordSocket, -1, Operator.Type.UnType); // clear out the most recently set WordSocket
					zLabeledSentence.labels[iWordSocket] = "";
					zLabeledSentence.suffixes[iWordSocket] = "";

					do {
						--iWordSocket;
						if (zOption.isWordSocketActive(iWordSocket)) {
							chosenPlan.setWordSocket(iWordSocket, -1, Operator.Type.UnType); // clear out the previous WordSocket
							zLabeledSentence.labels[iWordSocket] = "";
							zLabeledSentence.suffixes[iWordSocket] = "";

							interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
							figureMenuElements(parentVerb,optionSP,menuElements,wordIndexes,iWordSocket, chosenSP, tRole, zOption);
							interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
							choicePointSeeds.remove(choicePointSeeds.size()-1);
						}
						while (iWordSocket>Sentence.Verb && 
								(!zOption.isWordSocketActive(iWordSocket) || menuElements.size()<2)) {
							--iWordSocket;
							if (zOption.isWordSocketActive(iWordSocket)) {
								chosenPlan.setWordSocket(iWordSocket, -1, Operator.Type.UnType);
								zLabeledSentence.labels[iWordSocket] = "";
								zLabeledSentence.suffixes[iWordSocket] = "";
								interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
								figureMenuElements(parentVerb,optionSP,menuElements,wordIndexes, iWordSocket, chosenSP, tRole, zOption);
								interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
								choicePointSeeds.remove(choicePointSeeds.size()-1);
							}
						}
						choice++;
					} while (choice<0);
					logger.setLogging(oldIsLogging); // turn on logging again after undo
					if (optionChosen && iWordSocket<=Sentence.Verb) {
						logger.up(); // wordsocketMsg()
						logger.up(); // wordsocketsMsg()
						logger.up(); // optionMsg()
					} else
						logger.up(); // wordsocketMsg()
				}
			} while (iWordSocket < Sentence.MaxWordSockets);
			zLabeledSentence.rawSentence = chosenPlan;
			if (chosenPlan.getIWord(Sentence.Verb)>=0)
				zLabeledSentence.expressionLabel = dk.getVerb(chosenPlan.getIWord(Sentence.Verb)).getExpression();
			for (int j = 0; (j < Sentence.MaxWordSockets); ++j) {
				if (chosenVerb.isWordSocketActive(j) && chosenVerb.isVisible(j)) {
					zLabeledSentence.labels[j] = calculateWordsocketLabel(chosenPlan,chosenSP, j, chosenPlan.getIWord(j));
					zLabeledSentence.descriptions[j] = dk.getDescriptionByType(chosenVerb.getWordSocketType(j), chosenPlan.getIWord(j));
					zLabeledSentence.suffixes[j] = calculateWordsocketSuffix(chosenPlan,chosenSP, j, chosenPlan.getIWord(j));
				}
			}
			finalChoice = playerIO.getPlayerDone(zLabeledSentence,iReactingActor);
			inputCounter++;
			if (finalChoice<0 && iWordSocket>1) { // user clicked on the Undo button
				boolean oldIsLogging = logger.isLogging();
				boolean optionChosen = chosenPlan.getWordSocket(Sentence.Verb).getIWord()!=-1;
				logger.setLogging(false); // turn of logging during undo
				--iWordSocket;
				chosenPlan.setWordSocket(iWordSocket, -1, Operator.Type.UnType); // clear out the most recently set WordSocket
				zLabeledSentence.labels[iWordSocket] = "";
				zLabeledSentence.suffixes[iWordSocket] = "";
				int choice = finalChoice;
				do {
					--iWordSocket;
					if (zOption.isWordSocketActive(iWordSocket)) {
						chosenPlan.setWordSocket(iWordSocket, -1, Operator.Type.UnType); // clear out the previous WordSocket
						zLabeledSentence.labels[iWordSocket] = "";
						zLabeledSentence.suffixes[iWordSocket] = "";
						interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
						figureMenuElements(parentVerb,optionSP,menuElements,wordIndexes, iWordSocket, chosenSP, tRole, zOption);
						interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
						choicePointSeeds.remove(choicePointSeeds.size()-1);
					}
					while (iWordSocket>Sentence.Verb && 
							(!zOption.isWordSocketActive(iWordSocket) || menuElements.size()<2)) {
						--iWordSocket;
						if (zOption.isWordSocketActive(iWordSocket)) {
							chosenPlan.setWordSocket(iWordSocket, -1, Operator.Type.UnType);
							zLabeledSentence.labels[iWordSocket] = "";
							zLabeledSentence.suffixes[iWordSocket] = "";
							interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
							figureMenuElements(parentVerb,optionSP,menuElements,wordIndexes, iWordSocket, chosenSP, tRole, zOption);
							interpreter.scriptRandom.setSeed(choicePointSeeds.get(choicePointSeeds.size()-1));
							choicePointSeeds.remove(choicePointSeeds.size()-1);
						}
					}
					choice++;
				} while (choice<0);
				logger.setLogging(oldIsLogging); // turn on logging again after undo
				if (optionChosen && iWordSocket<=Sentence.Verb) {
					logger.up(); // wordsocketsMsg()
					logger.up(); // optionMsg()
				}
			}			
		} while (finalChoice<0);
		logger.up(); // wordsocketsMsg()
		logger.up(); // optionMsg()
		
		playerInactivity=0;
		if (runningRehearsal && iWordSocket>Sentence.Verb) {
			rehearsalLogger.reactSentenceOptionCandidacy(tIVerb, tiRole, iOption);
			rehearsalLogger.reactSentenceVerbCandidacy(zOption.getPointedVerb().getReference().getIndex());
			rehearsalLogger.reactSentenceOptionActivation(tIVerb, tiRole, iOption);
		}

		return chosenPlan;
	}
	
	private String calculateWordsocketLabel(Sentence thisSentence,ScriptPath sp,int iWordsocket, int value){
		if (sp.getVerb().isVisible(iWordsocket) && sp.getVerb().getWordsocketTextScript(iWordsocket)!=null)
			return executeTextScript(thisSentence,iWordsocket,value,sp,sp.getVerb().getWordsocketTextScript(iWordsocket));
		else
			return dk.getLabelByDataType(sp.getVerb().getWordSocketType(iWordsocket), value);
	}
	private String calculateWordsocketSuffix(Sentence thisSentence,ScriptPath sp,int iWordsocket, int value){
		if (sp.getVerb().isVisible(iWordsocket) && sp.getVerb().getSuffix(iWordsocket)!=null)
			return executeTextScript(thisSentence,iWordsocket,value,sp,sp.getVerb().getSuffix(iWordsocket));
		else
			return "";
	}
	private String executeTextScript(Sentence thisSentence,int iWordsocket,int value,ScriptPath sp,Script s){
		boolean oldIsLogging = logger.isLogging();
		logger.setLogging(false);
		Sentence oldThisEvent = thisEvent;
		thisEvent = thisSentence;
		int oldValue = thisSentence.getIWord(iWordsocket);
		Operator.Type oldType = thisSentence.getWordSocketType(iWordsocket);
		thisSentence.setWordSocket(iWordsocket, value, sp.getVerb().getWordSocketType(iWordsocket));
		interpreter.executeScript(sp,s);
		thisSentence.setWordSocket(iWordsocket, oldValue, oldType); 
		thisEvent = oldThisEvent;
		logger.setLogging(oldIsLogging);
		return interpreter.getPoison()?"POISON: "+interpreter.getPoisonCause():interpreter.getText();
	}

//**********************************************************************
	private Sentence algorithmicInput(int tIRole, int tIVerb, Verb verb, Role.Link role) throws InterruptedException {
		Sentence bestPlan = null;
		float bestInclination = -1.00f;
		int bestOption = -1;
	
		for (int iOption=0; (iOption < role.getRole().getOptions().size()); ++iOption) {
			Role.Option zOption = role.getRole().getOptions().get(iOption);
			ScriptPath sp = new ScriptPath(verb,role,zOption);
			boolean poisonedOption = false;
			int thisVerbIndex = zOption.getPointedVerb().getReference().getIndex();
			logger.optionMsg(zOption);
			interpreter.executeScript(sp,((Script)zOption.getAcceptableScript()));
			if (interpreter.getPoison()) {
				if (runningRehearsal)
					rehearsalLogger.recordPoison(sp,zOption.getDesirableScript(), interpreter.getPoisonCause());
				logger.up(); // optionMsg
				continue;
			}
			logger.scriptResultMsgChild(interpreter.getBoolean());
			if (!interpreter.getBoolean()){
				logger.valueMsgChild("not acceptable");
				logger.up(); // optionMsg
				continue;
			}
			
			ArrayList<Float> desirable = new ArrayList<Float>(); // the word's desirability result
			if (runningRehearsal) {
				rehearsalLogger.reactSentenceOptionCandidacy(tIVerb, tIRole, iOption);
				rehearsalLogger.reactSentenceVerbCandidacy(zOption.getPointedVerb().getReference().getIndex());
			}
			chosenPlan = new Sentence(dk.getActorCount());
			chosenPlan.setWordSocket(Sentence.Subject, iReactingActor, Operator.Type.Actor);
			chosenPlan.setWordSocket(Sentence.Verb, thisVerbIndex, Operator.Type.Verb);
			// Here our task is to examine all the WordSlots in the Sentence and,
			//  for each active WordSlot, determine the desirable value for each
			//  of these words; this permits us to select the best value for
			//  inclusion in the final Plan.
			//
			// We start from item #2 because items 0 and 1 are Subject and Verb,
			//   which are never changeable and have no scripts.
			logger.wordsocketsMsg();
			for (int k=Sentence.DefDirObject; (k < Sentence.MaxWordSockets); ++k) {
				if (!poisonedOption && zOption.isWordSocketActive(k)) {
					Operator.Type zType = dk.getVerb(thisVerbIndex).getWordSocketType(k);
					ArrayList<Integer> wordIndexes = new ArrayList<Integer>();
					logger.wordsocketMsg(dk.getVerb(thisVerbIndex).getWordSocketFullLabel(k));
					interpreter.executeScript(sp,zOption.getWordSocket(k).getAcceptableScript());
					interpreter.getAcceptables(wordIndexes);
					if (wordIndexes.isEmpty()) {
						logger.disqualifiedMsgChild(zOption, dk.getVerb(thisVerbIndex).getWordSocketFullLabel(k));
						poisonedOption=true;
					}
					if (runningRehearsal && interpreter.getPoison())
						rehearsalLogger.recordPoison(sp,zOption.getWordSocket(k).getAcceptableScript(), interpreter.getPoisonCause());
					
					if (!poisonedOption) {
						desirable = interpreter.executeDesirableLoop(sp,wordIndexes,zOption.getWordSocket(k), zType);
						if (desirable.size()==0)
							poisonedOption=true;
						
						// populate chosenPlan
						if (!poisonedOption) {
							int bestIndex = 0;
							float bestDesirable = desirable.get(0);
							for (int i=1;i<desirable.size();i++){
								float d = desirable.get(i);
								if (d>bestDesirable) {
									bestDesirable = d;
									bestIndex = i;
								}
							}
							chosenPlan.setWordSocket(k, wordIndexes.get(bestIndex), zType);
							if (logger.isLogging())
								logger.valueMsgChild(interpreter.getValueString(zType, wordIndexes.get(bestIndex), null));
						}
					}
					logger.up(); // wordsocketMsg()
				}
			}
			logger.up(); // wordsocketsMsg()

			// We evaluate the inclination AFTER we have evaluated the option WordSockets
			//  because we may want to use the Chosen values from the WordSockets in the
			//  final calculation of the inclination.
			if (!poisonedOption) {
				interpreter.executeScript(sp,((Script)zOption.getDesirableScript()));
				if (interpreter.getPoison()) {
					poisonedOption=true;
					if (runningRehearsal)
						rehearsalLogger.recordPoison(sp,zOption.getDesirableScript(), interpreter.getPoisonCause());
				}
			}
			if (!poisonedOption) {
				if (interpreter.getFloat() > bestInclination) {
					// the Actor makes his choice!!!
					bestPlan = chosenPlan.clone();
					bestOption = iOption;
					bestInclination = interpreter.getFloat();
				}
				logger.valueMsgChild(String.valueOf(interpreter.getFloat()));
			}
			
			logger.up(); // optionMsg
		}
		if (bestPlan!=null) {
			// We have an option to go with. Let's implement it as a Plan
			if (runningRehearsal)
				rehearsalLogger.reactSentenceOptionActivation(tIVerb, tIRole, bestOption);
			logger.chooseOptionMsgChild(bestInclination, dk.getVerb(bestPlan.getIWord(Sentence.Verb)));
		}
		return bestPlan;
	}
//**********************************************************************
	private void figureMenuElements(Verb parentVerb,ScriptPath chosenOptionSP,ArrayList<MenuElement> zMenuElements,ArrayList<Integer> wordIndexes,int iWordSocket, ScriptPath chosenSP, Role.Link tRole, Role.Option tOption) throws InterruptedException {
		zMenuElements.clear();
		wordIndexes.clear();
		if (iWordSocket == Sentence.Verb) {
			ArrayList<Role.Option> options = tRole.getRole().getOptions();
			for (int i=0;i<options.size();i++) {
				Role.Option option = options.get(i);
				ScriptPath optionSP = new ScriptPath(parentVerb,tRole,option);
				interpreter.executeScript(optionSP,((Script)option.getAcceptableScript()));
				if (interpreter.getPoison()) {
					if (runningRehearsal)
						rehearsalLogger.recordPoison(optionSP,option.getDesirableScript(), interpreter.getPoisonCause());
					continue;
				}
				logger.scriptResultMsgChild(interpreter.getBoolean());
				if (!interpreter.getBoolean())
					continue;

				Verb cVerb = option.getPointedVerb();
				wordIndexes.add(i);
				String label = calculateWordsocketLabel(chosenPlan,new ScriptPath(cVerb,null,null), iWordSocket,cVerb.getReference().getIndex());
				zMenuElements.add(new MenuElement(label, cVerb.getDescription()));
			}
		}
		else if (chosenSP.getVerb().isWordSocketActive(iWordSocket)) { // this is a WordSocket other than Sentence.Verb
			Operator.Type ziType = chosenSP.getVerb().getWordSocketType(iWordSocket);
			interpreter.executeScript(chosenOptionSP,tOption.getWordSocket(iWordSocket).getAcceptableScript());
			interpreter.getAcceptables(wordIndexes);
			if (wordIndexes.size() == 0) {
				logger.disqualifiedMsgChild(tOption, chosenSP.getVerb().getWordSocketFullLabel(iWordSocket));
			}
			if (interpreter.getPoison() && runningRehearsal)
				rehearsalLogger.recordPoison(chosenOptionSP,tOption.getWordSocket(iWordSocket).getAcceptableScript(), interpreter.getPoisonCause());

			for (int wi: wordIndexes) {
				String label = calculateWordsocketLabel(chosenPlan,chosenSP, iWordSocket, wi);
				String description = dk.getDescriptionByType(ziType, wi);
				zMenuElements.add(new MenuElement(label, description));
			}
		}
	}
//**********************************************************************	
	private void setThisEvent(Sentence tEvent, int tPlan, int tActor) {
		thisEvent = tEvent;

		thisEvent.setTime(cMoments);
		int location = dk.getActor(tActor).getLocation();
		int zDirObject = tEvent.getDirObject();
		// special case for FATE actions, which always take place in the DirObject's location
		if ((tActor == FATE) && (zDirObject > 0))
			location = dk.getActor(zDirObject).getLocation();
		// special case for the verb 'depart for'; if you're nowhere, we put you at your destination
//		if ((dk.getVerb(tEvent.getIWord(Sentence.Verb)).getLabel().equals("depart for"))
//				&& (location == Stage.Nowhere))
//			location = tEvent.getIWord(2); // 2 is the DirStage
		// special case for the verb 'arrive at'; the Event takes place where you get, not where you left
		if (dk.getVerb(tEvent.getIWord(Sentence.Verb)).getLabel().equals("arrive at"))
			location = tEvent.getIWord(2); // 2 is the DirStage
		
		thisEvent.setLocation(location);
		thisEvent.setHijacked(false);
//		thisEvent.setCausalEvent(-1);
		
		if (runningRehearsal)
			rehearsalLogger.reactSentenceVerbActivation(thisEvent.getIWord(Sentence.Verb));
		
	}
//**********************************************************************		
	private void recordHistory(int iSubject, int iPlan) throws InterruptedException {
		// This method executes the Plan indexed by iActor and iPlan.
		// It transfers it into the HistoryBook, calculates consequences,
		//   and sets up role calculations.

		// Set up thisEvent, which is used by the Interpreter to read information
		//  in the plan.
				
		Actor zSubject = dk.getActor(iSubject);
		setThisEvent((Sentence) zSubject.plans.get(iPlan), iPlan, iSubject);
		Verb zVerb = dk.getVerb(thisEvent.getIWord(Sentence.Verb));

		int iDirObject = thisEvent.getDirObject();
		int iStage = thisEvent.getLocation();
		
		// initialize the WhoKnows and Belief values
		for (int i = 0; (i < dk.getActorCount()); ++i) {
			thisEvent.setWhoKnows(i, false);
			thisEvent.setBelief(i, 0.0f);
		}
			
		// September 30th, 2107: deleted the if-statement because it interferes with some Fate actions
//		if (zVerb.getWitnesses()!=Verb.Witnesses.NOBODY_AT_ALL) {
			thisEvent.setPageNumber(historyBook.size());
			historyBook.add(thisEvent);
//		}
		
		if (iPlan>=0)
			logger.pageMsgChild(thisEvent.getPageNumber());
		
		if (historyBook.size()>MAXEVENTS)
			storyIsOver = true;		
		
		// Execute all consequences of this Verb.
		ScriptPath sp = new ScriptPath(zVerb,null,null);
		for (Script zScript: zVerb.getConsequences()) {
			interpreter.executeScript(sp,zScript);
			if (interpreter.getPoison() && runningRehearsal)
				rehearsalLogger.recordPoison(sp,zScript, interpreter.getPoisonCause());				
		}
		if (zVerb.getLabel().equals("depart for")) {
			zSubject.setLocation(dk.getStage(Stage.Nowhere)); // move the Subject to Nowhere
			zSubject.setTargetStage(dk.getStage(0));
		}
		// process the special case of Verb 'arrive at' for alarms and spying
		//   and for updating Visible Trait pValues
		if (zVerb.getLabel().equals("arrive at")) {
			int jStage = thisEvent.getIWord(2); // 2 is the DirStage
			Stage zStage = dk.getStage(jStage);
			zSubject.setLocation(zStage); // move the Subject to the new location
			// move all the Subject's carried Props with him
			for (Prop pr:dk.getProps()) {
				if ((pr.getOwner()==iSubject) && pr.getCarried())
					pr.setLocation(zStage);
			}
			
			// discovery of Traits of all the Actors
			for (Actor ac:dk.getActors()) {
				if (ac.getLocation()==jStage) {
					// discovery is a two-way street:
					// the arriving Actor learns about visible Traits of Actors already on Stage
					// and the Actors already on Stage learn about visible Traits of the arriving Actor.
					ac.setKnowsMe(zSubject, true);
					zSubject.setKnowsMe(ac, true);
					for (FloatTrait t:dk.getActorTraits()) {
						if (t.isVisible()) {
							ac.setP(t, zSubject, zSubject.get(t));
							zSubject.setP(t, ac, ac.get(t));
						}
					}
				}
			}
			
			// discovery for all the Props of all the Actors
			for (Prop pr:dk.getProps()) {
				if ((pr.getLocation()==jStage) & pr.getVisible()) {
					// discovery is a two-way street:
					// the arriving Actor learns about visible Traits of Props already on Stage
					// and the Actors already on Stage learn about visible Traits of Props carried by the arriving Actor.
					for (Actor ac:dk.getActors()) {
						if (ac.getLocation()==jStage) {
							pr.setKnowsMe(ac, true);
							for (FloatTrait t:dk.getPropTraits()) {
								if (t.isVisible())
									ac.setP(t, pr, pr.getTrait(t));
							}
						}
					}
				}
			}
			
			// Make the arriving Actor aware of any visible Stage Traits
			for (FloatTrait t:dk.getStageTraits()) {
				if (t.isVisible()) {
					zSubject.setP(t, zStage, zStage.getTrait(t));
				}
			}

			
			dk.getActor(iSubject).setTargetStage(dk.getStage(0)); // clear out the TargetStage
			Iterator<Alarm> alarmit = alarms.iterator();
			while (alarmit.hasNext()) {
				Alarm alarm = alarmit.next();
				if (alarm.actor1 == thisEvent.getIWord(Sentence.Subject)) {
					// we have established that this alarm applies to this Subject;
					// Now the question is, does it apply to the Actor, Stage, or Prop?
					// First, we should figure out what Stage we're on
					
					// Next, we decide what is being sought.
					if (alarm.type == Alarm.MEETACTOR) {
						// OK, so this alarm is looking for an Actor; is he on iStage?
						if (dk.getActor(alarm.index).getLocation() == jStage) {
							// Yes! We have a meeting alarm! Create a plan for the meeting alarm.
							Sentence zSentence = new Sentence(dk.getActorCount());
							zSentence.setWordSocket(Sentence.Subject, FATE, Operator.Type.Actor); // FATE executes alarms
							zSentence.setWordSocket(Sentence.Verb, dk.findVerb("MeetingAlarm"), Operator.Type.Verb); // FATE executes alarms
							zSentence.setWordSocket(Sentence.DefDirObject, alarm.actor1, Operator.Type.Actor); // actor1 becomes our DIROBJECT
							zSentence.setWordSocket(4, alarm.index, Operator.Type.Actor); //  (yes, I know it's confusing)						
							setPlan(dk.getActor(FATE),zSentence, -1, cMoments);
							//janus.saveToLog(dumpSentence("Setting plan: ", zSentence),sD);
							alarmit.remove();
						}
					}
					else if (alarm.type == Alarm.MEETPROP) {
						// This alarm is looking for a Prop; is it on iStage?
						if ((dk.getProp(alarm.index).getLocation() == jStage) && (dk.getProp(alarm.index).getInPlay())) {
							// Yes! We have a prop alarm! Create a plan for the prop alarm.
							Sentence zSentence = new Sentence(dk.getActorCount());
							zSentence.setWordSocket(Sentence.Subject, FATE, Operator.Type.Actor); // FATE executes alarms
							zSentence.setWordSocket(Sentence.Verb, dk.findVerb("PropAlarm"), Operator.Type.Verb); // FATE executes alarms
							zSentence.setWordSocket(Sentence.DefDirObject, alarm.actor1, Operator.Type.Actor); // actor1 becomes our DIROBJECT
							zSentence.setWordSocket(3, alarm.index, Operator.Type.Prop); // prop becomes our PROP						
							setPlan(dk.getActor(FATE),zSentence, -1, cMoments);
							//janus.saveToLog(dumpSentence("Setting plan: ", zSentence),sD);
							alarmit.remove();
						}					
					}
					else if (alarm.type == Alarm.MEETSTAGE) {
						// This alarm is looking for a Stage; is it on iStage?
						if (alarm.index == iStage) {
							// Yes! We have a stage alarm! Create a plan for the stage alarm.
							Sentence zSentence = new Sentence(dk.getActorCount());
							zSentence.setWordSocket(Sentence.Subject, FATE, Operator.Type.Actor); // FATE executes alarms
							zSentence.setWordSocket(Sentence.Verb, dk.findVerb("StageAlarm"), Operator.Type.Verb); // FATE executes alarms
							zSentence.setWordSocket(Sentence.DefDirObject, alarm.actor1, Operator.Type.Actor); // actor1 becomes our DIROBJECT
							zSentence.setWordSocket(4, alarm.index, Operator.Type.Stage); // stage becomes our STAGE1							
							setPlan(dk.getActor(FATE),zSentence, -1, cMoments);
							//janus.saveToLog(dumpSentence("Setting plan: ", zSentence),sD);
							alarmit.remove();
						}						
					} // if alarm.stage > 0
				}
				// now the reverse case: B meets A instead of A meets B
				if (alarm.index == thisEvent.getIWord(Sentence.Subject)) {
					if (dk.getActor(alarm.actor1).getLocation() == jStage) {
						// Yes! We have a meeting alarm! Create a plan for the meeting alarm.
						Sentence zSentence = new Sentence(dk.getActorCount());
						zSentence.setWordSocket(Sentence.Subject, FATE, Operator.Type.Actor); // FATE executes alarms
						zSentence.setWordSocket(Sentence.Verb, dk.findVerb("MeetingAlarm"), Operator.Type.Verb); // FATE executes alarms
						zSentence.setWordSocket(Sentence.DefDirObject, alarm.index, Operator.Type.Actor); // actor1 becomes our DIROBJECT
						zSentence.setWordSocket(4, alarm.actor1, Operator.Type.Actor); //  (yes, I know it's confusing)						
						setPlan(dk.getActor(FATE),zSentence, -1, cMoments);
						alarmit.remove();
					}
				}
			} // if alarms.size() > 0
			// move any spies along with the subject
			for (Actor actor: dk.getActors()) {
				if ((actor.getSpyingOn() == iSubject) & !actor.getUnconscious() & !actor.getDontMoveMe() & actor.getActive()) {
					// The moving Actor (zSubject) is being spied upon by Actor i
					// Therefore we must move Actor i to the same stage (iStage)
					// Note that the spy moves without any effects on others.
					// And if a spy is being spied upon, he loses his spy this way.
					actor.setLocation(dk.getStage(iStage));
				}
			}
		} // end of special case processing of Verb 'arrive at'		
		
		dk.getActor(iSubject).setOccupiedUntil(cMoments + zVerb.getTimeToExecute());

		// Fate always gets to react first
		{
			logger.fateReactingMsg();
			final boolean reacted=reactSentence(FATE, thisEvent);
			logger.up();
			if (reacted)
				logger.reactsMsgChild();
		}
		// Next come the witnesses
		for (int i=1; (i < dk.getActorCount()); ++i) {
			if (dk.getActor(i).getAbleToAct() 
					& (i != iSubject) & (i != iDirObject)) {
				boolean doesWitness = false;
				switch (zVerb.getWitnesses()) {
					case EVERYBODY_EVERYWHERE: { doesWitness = true; break; }
					case ANYBODY_ON_STAGE: {
						doesWitness = (dk.getActor(i).getLocation()==iStage);
						break;
					}
					case CUSTOM_SPEC: {
						int j=2;
						boolean foundIt = false;
						while (!foundIt & (j<Sentence.MaxWordSockets)) {
							foundIt = ((thisEvent.getWordSocketType(j) == Operator.Type.Actor) 
										&& (zVerb.isWordSocketActive(j))
										&& (zVerb.isWitness(j))
										&& (i==thisEvent.getIWord(j)));
							if (!foundIt) ++j;
						}
						doesWitness = foundIt;
						break;
					}
					case SUBJECT_ONLY: { doesWitness = false; break; }
					case NOBODY_AT_ALL: { doesWitness = false; break; }
				}
				thisEvent.setWhoKnows(i, doesWitness);
				// this next line was to insure that actors stick all the way through tales;
				// but it prevents actors from acting simultaneously. Therefore I have
				// decided to comment it out for now.
//					dk.getActor(i).setOccupiedUntil(cTicks + zVerb.timeToExecute);
				
				// Spies don't get to react to Events
				if (doesWitness & (dk.getActor(i).getSpyingOn() == FATE)) {
					logger.witnessMsg(dk.getActor(i));
					final boolean reacted=reactSentence(i, thisEvent);
					logger.up();
					if (reacted)
						logger.reactsMsgChild();

				}
			}
		}
		// Now comes dirObject (but not if he's Fate)
		if (iDirObject > 0) {
			if (dk.getActor(iDirObject).getAbleToAct()) {
				boolean doesWitness = false;
				switch (zVerb.getWitnesses()) {
					case EVERYBODY_EVERYWHERE: { doesWitness = true; break; }
					case ANYBODY_ON_STAGE: {
						doesWitness = (dk.getActor(iDirObject).getLocation()==iStage);
						break;
					}
					case CUSTOM_SPEC: {
						int j=2;
						boolean foundIt = false;
						while (!foundIt & (j<Sentence.MaxWordSockets)) {
							foundIt = ((thisEvent.getWordSocketType(j) == Operator.Type.Actor) 
									&& (iDirObject==thisEvent.getIWord(j))
									&& (zVerb.isWitness(j)));
							if (!foundIt) ++j;
						}
						doesWitness = foundIt;
						break;
					}
					case SUBJECT_ONLY: { doesWitness = false; break; }
					case NOBODY_AT_ALL: { doesWitness = false; break; }
				}
				
				thisEvent.setWhoKnows(iDirObject, doesWitness);
				if (doesWitness) {
					if (zVerb.getOccupiesDirObject()) 
						dk.getActor(iDirObject).setOccupiedUntil(cMoments + zVerb.getTimeToExecute());
					logger.dirObjectMsg(dk.getActor(iDirObject));
					final boolean reacted=reactSentence(iDirObject, thisEvent);
					logger.up();
					if (reacted)
						logger.reactsMsgChild();
				}
			}
		}
		// Lastly comes Subject (but not if he's Fate)
		thisEvent.setWhoKnows(iSubject, true);
		if ((iSubject>0) & (iSubject!=iDirObject) & (zVerb.getWitnesses()!=Verb.Witnesses.NOBODY_AT_ALL)) {
			logger.subjectMsg(dk.getActor(iSubject));
			final boolean reacted=reactSentence(iSubject, thisEvent);
			logger.up();
			if (reacted)
				logger.reactsMsgChild();

		}
		// add this Event to the consequence ArrayList of its CausalEvent
		int causalEventIndex = thisEvent.getCausalEvent();
		if ((causalEventIndex>=0) && (causalEventIndex<historyBook.size()))
			historyBook.get(causalEventIndex).addOutcome(historyBook.size()-1);
		// write thisEvent into the Storybook for the player
//		if (thisEvent.getWhoKnows(iProtagonist) && playerIO!=null) 
			playerIO.writeStorybook(getSentenceString(thisEvent));
	}
	
	void writeStorybook(String tSentence){
		String entry = " "+tSentence;
		storybook.add(entry);
		storybookQueue.add(entry);
	}
	
	private String getSentenceString(Sentence tEvent) {
		final StringBuilder output = new StringBuilder(200);
		final Verb zVerb = dk.getVerb(tEvent.getIWord(Sentence.Verb));
		ScriptPath sp = new ScriptPath(zVerb,null,null);
		long oldSeed = interpreter.scriptRandom.getSeed();
		interpreter.scriptRandom.setSeed(tEvent.seed);
		interpreter.leftPanel = true;
		for (int k=0; (k<Sentence.MaxWordSockets); ++k) {
			if (zVerb.isWordSocketActive(k) && zVerb.isVisible(k))  {
				String s = calculateWordsocketLabel(tEvent,sp,k,tEvent.getIWord(k));
				if (s!=null && s.length()>0) {
					output.append(" ");
					output.append(s);
				}
				s = calculateWordsocketSuffix(tEvent, sp, k, tEvent.getIWord(k));
				if (s!=null && s.length()>0) {
					output.append(" ");
					output.append(s);
				}
			} 
		}
		interpreter.scriptRandom.setSeed(oldSeed);
		return output.toString();
	}

//**********************************************************************		
	private boolean correctPresence(Sentence tPlan) {
		Verb zVerb = dk.getVerb(tPlan.getIWord(Sentence.Verb));
		int iStage = dk.getActor(tPlan.getIWord(Sentence.Subject)).getLocation();
		boolean result = true;
		int i=2;
		if ((tPlan.getIWord(0)>FATE) & (tPlan.getDirObject()>FATE)) { // FATE is omnipresent
			// Check the wordSockets: are the necessary Actors present and/or not present?
			while (result & (i<Sentence.MaxWordSockets)) {
				if (zVerb.isWordSocketActive(i) && (zVerb.getWordSocketType(i)==Operator.Type.Actor)) {
					int iActor = tPlan.getIWord(i);
					switch (zVerb.getPresence(i)) {
						case REQUIRED: {
							result=((iActor==FATE)|((dk.getActor(iActor).getLocation()==iStage)&(iStage>Stage.Nowhere)));
							break;
						}
						case ABSENT: {
							result=((iActor==FATE)|(dk.getActor(iActor).getLocation()!=iStage));
							break;					
						}
					}
				}
				++i;
			}
		}
		return (result);
	}
//**********************************************************************
	private boolean abort(Sentence tPlan) {
		boolean result = false;
		
		// This will fill MOST but not all of the variables used in Interpreter.
		// However, there may still be some unaddressed variables.
		// We need to verify that this works properly for all reasonable uses.
		thisEvent = tPlan;
		
		Verb zVerb = dk.getVerb(tPlan.getIWord(Sentence.Verb));
		ScriptPath sp = new ScriptPath(zVerb,null,null);
		Script abortScript = zVerb.getAbortScript();
		if (abortScript!=null) {
			interpreter.executeScript(sp,abortScript);
			if (interpreter.getPoison() && runningRehearsal)
				rehearsalLogger.recordPoison(sp,abortScript, interpreter.getPoisonCause());				
			result = abortIf;
		}
		return result;
	}
//**********************************************************************		
	public void run() throws InterruptedException {
		// logger.beginLog(sD);
		while (!isHappilyDone) {
			iMinute = cMoments % 60;
			iHour = cMoments / 60;
			iDay = cMoments / 1440;

			// Variables bellow are assumed to be reinitialized in every iteration.
			// To test this assumption initialization is enforced now.
			// If the assumption does not hold then we will have to save 
			// and load this variables when saving the engine state.
			chosenPlan=null;
			thisEvent=null;
			hypotheticalEvent=null;
			permitFateToReact=false;
			abortIf=false;

			if (playerIO!=null)
				playerIO.onCycleStart();

			// Terminate the story if nothing has happened for a while
			if (((cInactivity/dk.getActorCount())>timeLimit) | (playerInactivity>timeLimit))
				storyIsOver = true;
			
			// process clock alarms
			Iterator<Alarm> alarmit = alarms.iterator();
			while (alarmit.hasNext()) {
				Alarm alarm = alarmit.next();
				if (alarm.type == Alarm.MEETTIME) {
					// Is the correct time for this Alarm?
					if (alarm.index == cMoments) {
						// Yes! We have a clock alarm! Create a plan for the clock alarm.
						Sentence zSentence = new Sentence(dk.getActorCount());
						zSentence.setWordSocket(Sentence.Subject, FATE, Operator.Type.Actor); // FATE executes alarms
						zSentence.setWordSocket(Sentence.Verb, dk.findVerb("ClockAlarm"), Operator.Type.Verb); // FATE executes alarms
						zSentence.setWordSocket(Sentence.DefDirObject, alarm.actor1, Operator.Type.Actor); // actor1 becomes our DIROBJECT
						setPlan(dk.getActor(FATE),zSentence, -1, cMoments);
						//janus.saveToLog(dumpSentence("Setting plan: ", zSentence),sD);	
						alarmit.remove();
					}						
				}
			}
			
			// process end of story
			if (storyIsOver) {
				Sentence penultimateEvent = new Sentence(dk.getActorCount());
				penultimateEvent.setWordSocket(Sentence.Subject, FATE, Operator.Type.Actor);
				penultimateEvent.setWordSocket(Sentence.Verb, dk.findVerb("penultimate verb"), Operator.Type.Verb);
				penultimateEvent.setWordSocket(Sentence.DefDirObject, 1, Operator.Type.Actor); // the protagonist
				setPlan(dk.getActor(FATE),penultimateEvent, -1, cMoments);
				/*
				setThisEvent(penultimateEvent, -1, FATE);
				logger.executeMsg(cMoments,dumpSentence(penultimateEvent));
				logger.pageMsgChild(penultimateEvent.getPageNumber());
				logger.fateReactingMsg();
				boolean reacts = reactSentence(FATE, penultimateEvent);
				logger.up();
				if (reacts)
					logger.reactsMsgChild();
				logger.save();
				*/
			}
			for (int iActor=0; (iActor < dk.getActorCount()); ++iActor) {
				if (!storyIsOver | (iActor==0)) {
					Actor zActor = (Actor)dk.getActor(iActor);
					if ((zActor.getAbleToAct()) & (zActor.getOccupiedUntil()<=cMoments)) {
						// First off, let's clear any SpyingOn activity that has lapsed
						if ((zActor.getHowLongToSpy() <= cMoments) && (zActor.getSpyingOn() > FATE)) {
							zActor.setSpyingOn(dk.getActor(FATE)); 
						}
						
						// Next, we relax moods. I'm not sure that this algorithm is the best one
						//   to use, but it is a decent rough approximation. Someday we'll make it better.
						for(Actor.MoodTrait t:Actor.MoodTraits)
							if (zActor.get(t) != 0.0f) {
								zActor.set(t,(zActor.get(t)/1.3f));
							}
						
						// check to see if zActor has any plans to execute
						boolean zfDidAnything = false;
						// the function of the two MentalAction booleans is to permit an Actor
						// to perform ALL the mental actions in his plan list as well as ONE
						// physical action
						boolean zfMentalAction = true;
						int jPlan = 0;
						while (jPlan < zActor.plans.size()) {
							Sentence thisPlan = (Sentence) zActor.plans.get(jPlan);
							if (thisPlan.getTime()<=cMoments) {
								int iVerb = ((Sentence)thisPlan).getIWord(Sentence.Verb);
								Verb zVerb = dk.getVerb(iVerb);
								boolean yfMentalAction = zVerb.getWitnesses().equals(Verb.Witnesses.SUBJECT_ONLY)
															  | zVerb.getWitnesses().equals(Verb.Witnesses.NOBODY_AT_ALL)
															  | (zVerb.getTimeToExecute()==0);
								if (zfMentalAction | yfMentalAction) {
									boolean zfTemp2 = ((zVerb.getLabel().equals("depart for")) & (zActor.getDontMoveMe()));
									boolean isEpilogueSafe = true;
									if (isEpilogueNow)
										isEpilogueSafe = thisPlan.getIsPartOfEpilogue();
									// If all conditions are correct, then execute the plan...
									if (!zfTemp2 && correctPresence(thisPlan) && isEpilogueSafe){
										if (jPlan>=0)
											logger.executeMsg(cMoments,dumpSentence(thisPlan));
										if (abort(thisPlan)) {
											// we can forget this Plan now that it has been aborted
											dk.getActor(iActor).plans.remove(jPlan);
											--jPlan; // correct for collapse of plans ArrayList
											logger.abortedMsgChild();
											logger.save();
										} else {
											recordHistory(iActor, jPlan);
											if (jPlan>=0)
												logger.save();
											// we can forget this Plan now that it has been executed
											dk.getActor(iActor).plans.remove(jPlan);
											--jPlan; // correct for collapse of plans ArrayList
											zfDidAnything = !yfMentalAction;
											zfMentalAction &= yfMentalAction;
											if ((iActor>0) & zfDidAnything)
												// reset the activity count if it's not Fate who acted
												//	AND it was a physical action
												cInactivity = 0; 
										}
									}
								}
							}
							++jPlan;
						}
						
						// This section considers moving zActor to a new Stage (unless he's Fate)
						if ((iActor>0) & !zfDidAnything & (zActor.getSpyingOn()==0) & !zActor.getDontMoveMe()) { // did zActor execute any plans?
							// first, a bit of housekeeping: maintain a count of actions so that
							//   we can terminate the story if nothing happens for a while.
							++cInactivity;
							
							// zActor is free to move. We check, in priority order:
							//   1. is there a TargetStage to move to?
							//   2. is there a Plan requiring us to move somewhere for execution?
							//   3. if not, and we're bored, pick a random Stage
							// First, create a new plan to move somewhere
		 					Sentence zSentence = new Sentence(dk.getActorCount());
							zSentence.setWordSocket(Sentence.Subject, iActor, Operator.Type.Actor); // yes, i is the index of zActor
							zSentence.setWordSocket(Sentence.Verb, dk.findVerb("depart for"), Operator.Type.Verb);
							zSentence.setWordSocket(3, zActor.getLocation(), Operator.Type.Stage);
							zSentence.setCausalEvent(-1);
							boolean zfYouAreGo = false; // this will tell us whether to implement the Plan
							int iTargetStage = zActor.getTargetStage();
							if (iTargetStage > 0) { // go to targetStage
									zSentence.setWordSocket(2, iTargetStage, Operator.Type.Stage);
									// April 5, 2011:
									// Figure out the causal event for this motion
									boolean gotcha=false;
									int iHistoryBook=historyBook.size()-1;
									while (!gotcha&(iHistoryBook>=0)) {
										Sentence oldSentence=historyBook.get(iHistoryBook);
										int iVerb=oldSentence.getIWord(Sentence.Verb);
										Verb oldVerb=dk.getVerb(iVerb);
										Script oldScript=oldVerb.getConsequence("SetTargetStage");
										if (oldScript!=null) {
											// this relies on a dirty trick with the Interpreter,
											// which, when executing token "SetTargetStage",
											// adds an outcome to the sentence that's equal to -iTargetStage
											int iOutcome=0;
											while ((iOutcome<oldSentence.getOutcomeSize())&!gotcha) {
												if (oldSentence.getOutcome(iOutcome)==-iTargetStage) {
													gotcha=true;
													zSentence.setCausalEvent(iHistoryBook);
												}
												++iOutcome;
											}
										}
										--iHistoryBook;
									}
									zfYouAreGo = true;
							} else {
								// There is no TargetStage; is there a Plan requiring us to move?
								float bestTrivial_Momentous = -1.00f;
								int bestTargetStage = 0;
								// examine all of our plans
								for (jPlan=0; (jPlan < zActor.plans.size()); ++jPlan) {
									Sentence thisPlan = (Sentence) zActor.plans.get(jPlan);
									int iVerb = ((Sentence)thisPlan).getIWord(Sentence.Verb);
									int zDirObject = ((Sentence) thisPlan).getDirObject();
									if ((dk.getVerb(iVerb).isWordSocketActive(Sentence.DefDirObject)) && (dk.getVerb(iVerb).getWordSocketType(Sentence.DefDirObject)==Operator.Type.Actor)) {
										iTargetStage = dk.getActor(zDirObject).getLocation();
										if ((iTargetStage > 0) & (dk.getStage(iTargetStage).getKnowsMe(dk.getActor(iActor)))) { // is our planned DirObject reachable?
											if (dk.getVerb(iVerb).getTrivial_Momentous() > bestTrivial_Momentous) {
												bestTrivial_Momentous = dk.getVerb(iVerb).getTrivial_Momentous();
												bestTargetStage = iTargetStage;
											}
										}
									}
								} // end of jPlan-loop
								if ((bestTargetStage > 0) && (bestTargetStage != zActor.getLocation())) {
									// we have a good place to go! Let's go!
									zSentence.setWordSocket(2, bestTargetStage, Operator.Type.Stage);	
									zfYouAreGo = true;
								}
								 /*{
									// Down to the last possibility: are we bored enough to wander?
									if ((60 - zActor.get(dk.getCool_VolatilTrait())*50.0f - zActor.getCBored()) < 0) {
										// Yes. Let's pick a likely place to go.
										bestTargetStage = 0; // I know, this is a waste of time, I'm just keeping Dave happy.
										float bestUnwelcoming_Homey = 1.0f-zActor.get(dk.getCool_VolatilTrait());
										for (int jStage=0; (jStage < dk.getStageCount()); ++jStage) {
											Stage zStage = dk.getStage(jStage);
											if ((jStage != zActor.getLocation()) & zStage.getKnowsMe(dk.getActor(iActor))) {
												// note use of RANDOM NUMBER here!!!
												if (zStage.getUnwelcoming_Homey(dk.getActor(iActor)) + random.nextFloat() > bestUnwelcoming_Homey) {
													bestUnwelcoming_Homey = zStage.getUnwelcoming_Homey(dk.getActor(iActor));
													bestTargetStage = jStage;
												}
											}
										} // end of j-loop
										if (bestTargetStage > 0) {
											// we found a place to go. Let's go!
											zSentence.setWordSocket(2, bestTargetStage, Operator.Type.Stage);	
											zfYouAreGo = true;
										}
									}
									else {
										// we are not bored enough to wander. So let's just sit here and be bored.
										zActor.setCBored(zActor.getCBored()+1);
									}
								}*/
							}
							
							if (zfYouAreGo) {
								zSentence.setLocation(zSentence.getIWord(3));
								setPlan(zActor,zSentence, zSentence.getCausalEvent(), cMoments);
								//janus.saveToLog(dumpSentence("Setting movement plan: ", zSentence), sD);
								zActor.setCBored(0);						
							}
						}
					}
				}
			}
			++cMoments;
			++playerInactivity;
			// set off counters for shutting down the Engine
			if (isPenultimateDone) isHappilyDone = true;
			if (storyIsOver) isHappilyDone = true;
		}
		logger.flushNodes(); // flush logger nodes if the story is over.
		if (playerIO!=null)
			playerIO.theEnd();
	}
//**********************************************************************		
	public void addAlarm(int tActor1, int tType, int tIndex) {
		alarms.add(new Alarm(tActor1, tType, tIndex));
	}
	public void addAlarmMEETACTOR(int who, int toWhom) {
		addAlarm(who,Alarm.MEETACTOR,toWhom);
	}
	public void addAlarmMEETSTAGE(int who, int stage) {
		addAlarm(who,Alarm.MEETSTAGE,stage);
	}
	public void addAlarmMEETPROP(int who, int prop) {
		addAlarm(who,Alarm.MEETPROP,prop);
	}
	public void addAlarmMEETTIME(int who, int when) {
		addAlarm(who,Alarm.MEETTIME,when);
	}
//**********************************************************************		
//	public void setPlayer(int tPlayer) {
//		iProtagonist = tPlayer;
//	}
//**********************************************************************		
	String dumpSentence(Sentence tSentence) {
		String appendedString="";
		String output = "";
		for (int i=0; (i < Sentence.MaxWordSockets); ++i)
			if (tSentence.getIWord(i)>=0) {
				appendedString = " "+dk.getLabelByDataType(dk.getVerb(tSentence.getIWord(Sentence.Verb)).getWordSocketType(i), tSentence.getIWord(i));
				output=output+appendedString;
			}
		output=output+"  "+dk.getStage(tSentence.getLocation()).getLabel();
		output=output+"  CausalEvent: "+tSentence.getCausalEvent();
		return output;
	}

	/** Saves the engine state. */
	public void saveState(ObjectOutput out) throws IOException {
		// save deikto values
		dk.saveState(out);
		// save interpreter values
		out.writeFloat(interpreter.globalActorBox);
		out.writeFloat(interpreter.globalPropBox);
		out.writeFloat(interpreter.globalStageBox);
		out.writeFloat(interpreter.globalEventBox);
		out.writeFloat(interpreter.globalVerbBox);
		out.writeFloat(interpreter.globalBNumberBox);

		// save ticks
		out.writeInt(cMoments);
		// save inactivity counter
		out.writeInt(cInactivity);
		// save player inactivity
		out.writeInt(playerInactivity);
		// save storyIsOver
		out.writeBoolean(storyIsOver);
		out.writeBoolean(isEpilogueNow);
		out.writeBoolean(isHappilyDone);
		out.writeBoolean(isPenultimateDone);

		// save historyBook
		out.writeInt(historyBook.size());
		for(Sentence s:historyBook)
			out.writeObject(s);
		// save random seeds
		out.writeObject(random);
		out.writeLong(interpreter.scriptRandom.getSeed());
		// save alarms
		out.writeInt(alarms.size());
		for(Alarm a:alarms)
			out.writeObject(a);
		out.writeInt(storybook.size());
		for(String s:storybook)
			out.writeUTF(s);
	}
	/** Loads the engine state. */
	public void loadState(ObjectInput in) throws IOException {
		// loda deikto values
		dk.loadState(in);
		// load interpreter values
		interpreter.globalActorBox=in.readFloat();
		interpreter.globalPropBox=in.readFloat();
		interpreter.globalStageBox=in.readFloat();
		interpreter.globalEventBox=in.readFloat();
		interpreter.globalVerbBox=in.readFloat();
		interpreter.globalBNumberBox=in.readFloat();

		// load ticks
		cMoments = in.readInt();
		// load inactivity counter
		cInactivity = in.readInt();
		// load inactivity counter for the player
		playerInactivity = in.readInt();
		// load storyIsOver
		storyIsOver = in.readBoolean();
		isEpilogueNow = in.readBoolean();
		isHappilyDone = in.readBoolean();
		isPenultimateDone = in.readBoolean();

		try {
			// load storybook
			int size = in.readInt();
			Deikto.checkByteArraySize(size*4);
			historyBook.clear();
			for(int i=0;i<size;i++)
				historyBook.add((Sentence)in.readObject());
			//save random seeds
			random = (Random)in.readObject();
			interpreter.scriptRandom.setSeed(in.readLong());
			// load alarms
			size = in.readInt();
			Deikto.checkByteArraySize(size*16);
			alarms.clear();
			for(int i=0;i<size;i++)
				alarms.add((Alarm)in.readObject());
			size = in.readInt();
			Deikto.checkByteArraySize(size*16);
			storybookQueue.clear();
			storybook.clear();
			for(int i=0;i<size;i++)
				storybook.add(in.readUTF());
			storybookQueue.addAll(storybook);
		} catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void assertEqualStates(Engine e){
		dk.assertEqualStates(e.dk);
		
		assert interpreter.globalActorBox==e.interpreter.globalActorBox;
		assert interpreter.globalPropBox==e.interpreter.globalPropBox;
		assert interpreter.globalStageBox==e.interpreter.globalStageBox;
		assert interpreter.globalEventBox==e.interpreter.globalEventBox;
		assert interpreter.globalVerbBox==e.interpreter.globalVerbBox;
		assert interpreter.globalBNumberBox==e.interpreter.globalBNumberBox;
		
		assert cMoments == e.cMoments:"different cMoments";
		assert cInactivity == e.cInactivity:"different cInactivity";
		assert playerInactivity == e.playerInactivity:"different playerInactivity";
		assert storyIsOver == e.storyIsOver:"different storyIsOver";
		assert isEpilogueNow==e.isEpilogueNow;
		assert isHappilyDone==e.isHappilyDone;
		assert isPenultimateDone==e.isPenultimateDone;
		
		assert historyBook.size()==e.historyBook.size();
		int i=0;
		for(Sentence s:historyBook)
			s.assertEquals(e.historyBook.get(i++));
		
		assert interpreter.scriptRandom.getSeed()==e.interpreter.scriptRandom.getSeed();
		
		assert alarms.size()==e.alarms.size();
		i=0;
		for(Alarm a:alarms)
			a.assertEquals(e.alarms.get(i++));

		assert storybook.size()==e.storybook.size();
		i=0;
		Iterator<String> it = e.storybook.iterator(); 
		for(String s:storybook) {
			Object s1=it.next();
			assert s.equals(s1):String.valueOf(i)+": "+String.valueOf(s)+"!="+String.valueOf(s1);
			i++;
		}
	}
}
