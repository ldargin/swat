package Engine.enginePackage;

import java.util.ArrayList;
import java.util.Random;

import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.MenuElement;
import com.storytron.uber.Sentence;

/** 
 * A class for feeding rehearsals with random data. It takes care of
 * figuring which is the starting verb, and also backtracks when finding
 * dead ends.
 *  */
public class RehearsalIO implements EnginePlayerIO {

	public static Random random = new Random();
	private Engine engine;
	public int firstVerbIndex = -1;
	
	private ArrayList<ArrayList<Integer>> choices = new ArrayList<ArrayList<Integer>>();
	private ArrayList<Boolean> initializedChoice = new ArrayList<Boolean>();
	private int lastChoice = -1;
	
	public RehearsalIO(Engine engine) {
		this.engine = engine;
	}
	
	public int getPlayerDone(LabeledSentence labeledSentence,int playerId)
			throws InterruptedException {
		setInitialVerb(labeledSentence.rawSentence,Sentence.MaxWordSockets);
		for(int i=0;i<=lastChoice;i++) {
			choices.get(i).clear();
			initializedChoice.set(i,false);
		}
		lastChoice = -1;
		return 1000;
	}

	public int getPlayerSelection(LabeledSentence labeledSentence,
			ArrayList<MenuElement> menuElements, int wordSocket, int playerId)
			throws InterruptedException {
		setInitialVerb(labeledSentence.rawSentence,wordSocket);
		if (menuElements.isEmpty()) { // backtrack
			int d = distanceToAvailableChoice();
			// reset data of undone choices 
			for(int i=lastChoice-d+1;i<=lastChoice;i++) {
				choices.get(i).clear();
				initializedChoice.set(i,false);
			}
			lastChoice -= d+1;
			return -d-1;
		} else { // select the next available choice
			lastChoice++;
			if (choices.size()==lastChoice) {
				choices.add(new ArrayList<Integer>());
				initializedChoice.add(false);
			}
			// lastChoices contains the available choices that have not been tried
			// yet.
			ArrayList<Integer> lastChoices = choices.get(lastChoice);
			if (!initializedChoice.get(lastChoice)) {
				initializedChoice.set(lastChoice,true);
				for(int i=0;i<menuElements.size();i++)
					lastChoices.add(i);
			}
			// Choose an available choice at random and remove it. 
			return lastChoices.remove(random.nextInt(lastChoices.size()));
		}
	}
	
	private int distanceToAvailableChoice(){
		for(int i=lastChoice;i>=0;i--) {
			if (!choices.get(i).isEmpty())
				return lastChoice-i;
		}
		return lastChoice+1;
	}
	
	private void setInitialVerb(Sentence rawSentence, int wordSocket){
		if (firstVerbIndex<0) {
			if (wordSocket>Sentence.Verb)
				firstVerbIndex = rawSentence.getIWord(Sentence.Verb);
			else
				firstVerbIndex = engine.getThisEvent().getIWord(Sentence.Verb);
		}
	}

	public void onCycleStart() {}

	public void sendTriggerSentence(LabeledSentence labeledSentence, int time,
			boolean showBottom, int playerId) throws InterruptedException {
	}

	public void theEnd() throws InterruptedException {	}
	public void writeStorybook(String sentence) {
		engine.writeStorybook(sentence);
	}
}
