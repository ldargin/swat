package Engine.enginePackage;

import java.util.ArrayList;

import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.MenuElement;

/**
 * <p>Plays input recorded in a previous session and handles control
 * to another IO handler when finished.</p>
 * 
 * <p>Data send by the engine to the IO handler is ignored, except for the
 * last trigger sentence which is sent to the next IO handler.</p> 
 * */
public class ResumerIO implements EnginePlayerIO {
	private int[] input;
	private int pos;
	private EnginePlayerIO nextIOHandler; 
	private Janus.SessionData sD;

	public ResumerIO(int[] input, Janus.SessionData sD, EnginePlayerIO nextPlayer){
		if (input.length==0)
			throw new IllegalArgumentException("Input can not be empty.");
		this.input = input;
		pos=0;
		this.sD = sD;
		this.nextIOHandler = nextPlayer;
	}
	
	public void onCycleStart(){
		Janus.onCycleStart(sD);
	}
	
	/** Gets recorded input. */
	public int getPlayerDone(LabeledSentence labeledSentence, int playerId) throws InterruptedException {
		if (pos==input.length-1) {
			// Send the last trigger sentence if undoing.
			if (input[pos]<0 && lastls!=null)
				nextIOHandler.sendTriggerSentence(lastls, lasttime, lastShowBottom, playerId);
			sD.engine.setPlayerInputIO(nextIOHandler);
			if (sD.engine.rehearsalLogger!=null)
				sD.engine.runningRehearsal = true;
			if (Janus.Test.stressTestLatch!=null)
				Janus.Test.stressTestLatch.countDown();
		}
		int res = Math.max(-1000,input[pos++]);
		if (!sD.storyIsStopped) {
			if (res<0) { 
				for(int i=0;i>res;i--) { 
					if (!sD.lastInputs.isEmpty())
						sD.lastInputs.removeLast();
				}
			} else
				sD.lastInputs.add(res);
		}
		return res;
	}

	/** Gets recorded input. */
	public int getPlayerSelection(LabeledSentence labeledSentence,
			ArrayList<MenuElement> menuElements, int wordSocket, int playerId) throws InterruptedException {
		if (pos==input.length-1) {
			// Send the last trigger sentence.
			if (lastls!=null)
				nextIOHandler.sendTriggerSentence(lastls, lasttime, lastShowBottom, playerId);
			if (sD.engine.rehearsalLogger!=null)
				sD.engine.runningRehearsal = true;
			// Pass control to the next IO handler.
			sD.engine.setPlayerInputIO(nextIOHandler);
			if (Janus.Test.stressTestLatch!=null)
				Janus.Test.stressTestLatch.countDown();
		}
		int res = Math.max(-1000,input[pos++]);
		if (!sD.storyIsStopped) {
			if (res<0) { 
				for(int i=0;i>res;i--) { 
					if (!sD.lastInputs.isEmpty())
						sD.lastInputs.removeLast();
				}
			} else
				sD.lastInputs.add(res);
		}

		return res;
	}

	private LabeledSentence lastls;
	private int lasttime;
	private boolean lastShowBottom;
	
	/** Records the last trigger sentence. */
	public void sendTriggerSentence(LabeledSentence labeledSentence, int time,
			boolean showBottom, int payerId) {
		lastls = labeledSentence;
		lasttime = time;
		lastShowBottom = showBottom;
	}

	public void theEnd() throws InterruptedException {
		nextIOHandler.theEnd();
	}

	public void writeStorybook(String sentence) {
		nextIOHandler.writeStorybook(sentence);
	}

}
