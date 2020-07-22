package engPackage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.StackChunk;
import com.storytron.enginecommon.StackChunkGroup;
import com.storytron.enginecommon.Utils;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Sentence;
import com.storytron.uber.TextTrait;
import com.storytron.uber.Verb;
import com.storytron.uber.Role.Option.OptionWordSocket;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.ActorTraitOperator;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;
import com.storytron.uber.operator.PTraitOperator;
import com.storytron.uber.operator.ParameterOperator;
import com.storytron.uber.operator.TraitOperator;

/**
   Very important specification for how the stack is ordered:
   For an operator with arguments like so:
   <pre>
      Operator
         Arg1
         Arg2
         Arg3
    </pre>  
    Arg1 is pushed onto the stack first, then Arg2, then Arg3.
    Thus, Arg3 is the first value that pops off the stack when the operator is executed
    This code probably contains some ordering errors, so we must be on the lookout for
    bugs arising from such errors.
<p>
<h3>Custom operators</h3>
The interpreter can execute custom operators. Execution of a custom operators is
different from execution of other operators in the fact that the
arguments must remain in the stack throughout the execution of the script 
operator body. At any point of the execution of that script, the arguments could
be needed from wherever they are, buried in the stack. For that sake, we maintain
a stack of offsets, which has at the top the stack offset of the arguments of the
custom operator being executed. If there is more than one custom operator being
executed, then the offset refers to the arguments of that operator which was invoked
the latest.
<p>
Whenever a custom operator is executed, the offset of the arguments is pushed in the
offset stack. Whenever execution of a custom operator finishes, the offset of the
arguments is popped from the offset stack.
<p>
For a custom operator
<pre>
   CustomOperator
      Arg1
      Arg2
 </pre>  
when starting execution the values of Arg1 and Arg2 would be pushed in the stack,
then the index stackTop-2 would be saved as the offset of the arguments,
and then the script body of CustomOperator would be executed. This is how the
offsets will look:
<pre>
stack: [Arg1,Arg2]
offset stack: [0]
</pre> 
Then the result would be pushed in the stack just after the arguments. All of this tasks are
performed in {@link #pushCustomOperator()}. 
<pre>
stack: [Arg1,Arg2,Result]
offset stack: [0]
</pre> 
<p>
The clean up consist
in popping the result, popping all of the arguments, pushing the result again
and popping the stackTop-2 from the offset stack. So the stack 
<pre>
stack: [Result]
offset stack: []
</pre> 
<p>
During the execution of the custom operator, we might need one of the parameter
values in the stack, so we get the stack offset first (offsetStack[offsetStackTop-1]),
and then we get the value of the 0-based ith parameter (stack[offsetStack[offsetStackTop-1]+i]).
Getting the parameter values is done in {@link #pushParameterOperator()}.
<pre>
stack: [Arg1,Arg2,othervalue,othervalue,othervalue]
offset stack: [0]
</pre> 

<h3>Object arguments</h3>
Originally, the interpreter used just one stack of indexes to handle all kind of
operator arguments. When we added the Text type we had to setup a new Object stack.
When an Object argument is pushed, the value is stored in the Object stack and
the index of the Object stack is stored in the normal stack.  
When an Object argument is popped, the value stored in the Object stack is popped and
the index of the Object stack is popped from the normal stack.
This logic is encapsulated by the methods {@link #popString()} and {@link #pushString(String)}.
<p>
The stack string indexes are pushed in the normal stack because thus we can preserve
the order in which they were pushed. This information is needed by
{@link #pushParameterOperator()}, to get the appropriate parameter value.
So, after pushing some string values the stacks may look as:
<pre>
normal stack: [non-string value,0,non-string value,non-string value,1]
string stack: ["a string","another string"]
</pre> 
In this example we have pushed a non-string value, then "a string", then two other
non-string values, and finally "another string". Looking at the normal stack we can 
tell exactly in which order the string and non-string values where pushed. 
*/
public final class Interpreter {
	private static final long serialVersionUID = 1L;
	final static int MAXSTACKSIZE = 50;
	final static int MAXOFFSETSTACKSIZE = 50;
	private float[] stack = new float[MAXSTACKSIZE];
	private int stackTop = 0;
	private int[] offsetStack = new int[MAXOFFSETSTACKSIZE];
	private int offsetStackTop = 0;
	private Object[] objectStack = new Object[MAXSTACKSIZE];
	private int objectStackTop = 0;
	private boolean poison = false; 
	private String poisonCause;
	private int[] candidateStack = new int[ScriptPath.NESTING_LIMIT];
	private Operator.Type[] candidateTypeStack = new Operator.Type[ScriptPath.NESTING_LIMIT];
	private int candidateStackTop = 0;
	private Deikto dk;
	private AbstractEngine engine;
	private Operator mOperator;
	private Node mNode;
	private float mFloat;
	private boolean mBool;
	private String myText;
	private boolean isScriptalyzerActive;
	private Random random;
	public CustomRandom scriptRandom;
	private StackChunkGroup stackChunkGroup;
	public Map<Node,Short> node2id;
	private boolean accumulateChunks;
	private float verbActorBox;
	private float verbPropBox;
	private float verbStageBox;
	private float verbEventBox;
	private float verbVerbBox;
	private float verbBNumberBox;
	private float roleActorBox;
	private float rolePropBox;
	private float roleStageBox;
	private float roleEventBox;
	private float roleVerbBox;
	private float roleBNumberBox;
	public static final float UNDEFINED_BOX_VALUE = -2.0f;
	public float globalActorBox;
	public float globalPropBox;
	public float globalStageBox; 
	public float globalEventBox;
	public float globalVerbBox;
	public float globalBNumberBox;
	public float score;
	public boolean leftPanel=false;
	private Script theScript;
	private ScriptPath theScriptPath;
	private ArrayList<Float> desirableList = new ArrayList<Float>();
	private Object myAcceptable;
//**********************************************************************	
	public Interpreter(AbstractEngine tEngine, Deikto tdk) {
		random = new Random(65432);
		scriptRandom = new CustomRandom(0);
		engine = tEngine;
		dk = tdk;
		stackTop = 0;
		candidateStackTop = 0;
		accumulateChunks = false;
		globalActorBox = UNDEFINED_BOX_VALUE;
		globalStageBox = UNDEFINED_BOX_VALUE;
		globalPropBox = UNDEFINED_BOX_VALUE;
		globalVerbBox = UNDEFINED_BOX_VALUE;
		globalEventBox = UNDEFINED_BOX_VALUE;
		globalBNumberBox = UNDEFINED_BOX_VALUE;
		score = UNDEFINED_BOX_VALUE;
	}
	/** Call this to get the method to execute for a given operator. */
	public static Method getMethod(Operator.OpType operatorType,String codeLabel){
		if (!codeLabel.equals("no method")) {
			String prefix;
			if (operatorType==Operator.OpType.Read)
				prefix = "push".concat(codeLabel);
			else if (operatorType==Operator.OpType.Write)
				prefix = "pop".concat(codeLabel);
			else
				prefix = codeLabel;
			try {
				return Interpreter.class.getMethod(prefix, (Class<?>[])null);
			} catch (java.lang.NoSuchMethodException e) {
				System.out.println("error: no such method: " + prefix);
			}
		}
		return null;
	}
//***********************************************************************
	public boolean getPoison() {
		return poison;
	}
//***********************************************************************
	public String getPoisonCause() {
		return poisonCause;
	}
//***********************************************************************
	public float executeScript(ScriptPath sp,Script s) {
		theScript = s;
		theScriptPath = sp;
		poison = false;
		myText = null;
		stackTop=0;
		offsetStackTop=0;
		myAcceptable = null;
		if (accumulateChunks) {
			stackChunkGroup = new StackChunkGroup();
			stackChunkGroup.enumeration = s.getRoot().preorderEnumeration();
		}
		if (isScriptalyzerActive && s.getType()==Script.Type.OperatorBody) {
			// insert random parameters if we are testing a custom operator script.
			for(int i=0;i<s.getCustomOperator().getCArguments();i++) {
				if (s.getCustomOperator().getArgumentDataType(i)==Operator.Type.Text)
					pushString("");
				else 
					push(getRandomValue(s.getCustomOperator().getArgumentDataType(i)));
			}
			mOperator = s.getCustomOperator();
			pushCustomOperator(s);
		} else {
			try {
				engine.logScriptMsg(sp,s);
				executeNode(s.getRoot());
				engine.loggerUp(); // leaving scriptMsg
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		if (stackTop != 0)
			setPoison("Interpreter:executeScript: stackTop != 0 "+stackTop);
		
		// clear the object stack
		for(int i=0;i<objectStackTop;i++)
			objectStack[i] = null;
		objectStackTop = 0;
		
		return stack[stackTop];
	}
//**********************************************************************	
	public void executeNode(Node zNode) {
		poison = false;
		int cNodes = zNode.getChildCount();
		Operator zOperator = zNode.getOperator();

		try {
			engine.logTokenMsg(zOperator);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		if ((cNodes != zOperator.getCArguments()) & (!isScriptalyzerActive)) { // this is a safety check
			setPoison("ExecuteNode: invalid Script token: "+zOperator.getLabel());
			try {
				engine.loggerUp(); // tokenMsg
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return;
		}

		// now we must decide whether this is a conventional operator
		//  or a special looping PickBest operator. Why don't I just let the regular
		//  code invocation do its job and assign all this code to the individual
		//  operators? Because this code has to be re-entrant in case we get
		//  nested operators with such implicit loops. I couldn't figure out how
		//  to make the code re-entrant using the regular invocation. If you figure
		//  out how to do so, then you can put all this junk inside operator-specific
		//  methods.
		String zLabel = zOperator.getLabel();

		// For lazy evaluation
		Boolean isAND = zLabel.startsWith("AND");
		Boolean isOR = zLabel.startsWith("OR");

		if (zLabel.startsWith("PickBest")) 
			pickBestSomething(zOperator, zNode);
		// The second term and third terms in this if-statement are, like, totally grody.
		else if ((zOperator.getMenu()==OperatorDictionary.Menu.History) 
				&& !(zOperator.getLabel().equals("MainClauseIs"))
				&& !(zOperator.getLabel().startsWith("Event@")))
			examineHistoryBook(zOperator, zNode);
		// this stretch of nine if-statements is really stupid,
		// but I don't want to risk bugs arising from similar Operator labels.
		// I worry that it eats up too many CPU cycles.
		else if (zLabel.startsWith("All") && (zLabel.endsWith("Which") || zLabel.endsWith("Who")))
			pushAllWordsWhich(zNode);
		else if (zLabel.startsWith("Actor@"))
			actorLoop(zOperator, zNode);
		else if (zLabel.startsWith("Prop@"))
			propLoop(zOperator, zNode);
		else if (zLabel.startsWith("Stage@"))
			stageLoop(zOperator, zNode);
		else if (zLabel.startsWith("Event@"))
			eventLoop(zOperator, zNode);
		else if ((zLabel.startsWith("This")) && (Character.isDigit(zLabel.charAt(4))))
			parseThisOperator(zLabel);
		else if ((zLabel.startsWith("Past")) && (Character.isDigit(zLabel.charAt(4))))
			parsePastOperator(zLabel, zNode);
		else if ((zLabel.startsWith("Chosen")) && (Character.isDigit(zLabel.charAt(6))))
			parseChosenOperator(zLabel);
		else { // this is a conventional operator
			int i=0;
			while(i<cNodes) {
				executeNode((Node)zNode.getChildAt(i));
				if (poison)
					break;

				// Lazy evaluation for AND operators
				if (isAND && i<cNodes-1 && !isScriptalyzerActive) {
					if (peek()==0.0) {
						// push code to show that a a short-circuit occurs
						for (int j=i+1;j<cNodes;j++)
							push(0);
						break;
					}
				}
				
				// Lazy evaluation for OR operators
				if (isOR && i<cNodes-1 && !isScriptalyzerActive) {
					if (peek()==1.0) {
						for (int j=i+1;j<cNodes;j++)
							push(1);
						break;
					}
				}
				
				i++;
			}
			if (poison) {
				// pop computed children
				for(int j=0;j<i+1;j++)
					pop();
				// push dummy result
				pushDummyResult(zOperator.getDataType());
			} else
				executeToken(zNode);
		}
		if (!poison)
			logOperatorAction(zOperator);
		if (accumulateChunks) {
			int iStack = stackTop;
			if (stackTop>0)
				iStack = stackTop-1;
			Short s=node2id.get(zNode);
			if (s!=null) // if null we are not interested in the stack chunk.
				stackChunkGroup.stackChunks.add(new StackChunk(stack[iStack], zOperator.getDataType(), s));
		}
		try {
			engine.loggerUp(); // tokenMsg
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// Pushes a dummy result in the stack  
	private void pushDummyResult(Operator.Type t) {
		switch(t){
		case Text:
			pushString("");
			break;
		default:
			if (t.name().contains("Group"))
				pushObject(null);
			else
				push(0);
		}
	}
			
//**********************************************************************	
	private void examineHistoryBook(Operator tOperator, Node zNode) {
		boolean saveAccumulateChunks = accumulateChunks;
		accumulateChunks = false;
		if (tOperator.getLabel().equals("EventHappened")) {
			int iPageNumber = engine.getHistoryBookSize();
			if (iPageNumber==0) {
				push(0);
			}
			else {
				boolean eventFound = false;
				++candidateStackTop;
				while ((iPageNumber>0) & !eventFound) {
					--iPageNumber;
					candidateStack[candidateStackTop] = iPageNumber;
					candidateTypeStack[candidateStackTop] = Operator.Type.Event;
					executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
					eventFound = stack[stackTop-1]!=0.0f && !poison;
					pop(); // clear that item off the stack
					poison = false;
				}
				--candidateStackTop;
				if (eventFound)
					push(1.0f);
				else
					push(0.0f);
			}
			// note that we're exiting this routine with stack[stackTop-1] == eventFound
			if (isScriptalyzerActive) { pop(); push(myNextBoolean()); }
		}
		else if (tOperator.getLabel().equals("CausalEventHappened")) {
			int iPageNumber = engine.getHistoryBookSize()-1;
			if (iPageNumber<0 || iPageNumber==0 || engine.getEventCausalEvent(iPageNumber)<0) {
				push(0);
			}
			else {
				boolean eventFound = false;
				++candidateStackTop;
				while ((iPageNumber>0) & !eventFound) {
					iPageNumber = engine.getEventCausalEvent(iPageNumber);
					if (iPageNumber >= 0) {
						candidateStack[candidateStackTop] = iPageNumber;
						candidateTypeStack[candidateStackTop] = Operator.Type.Event;
		  			executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
		  			eventFound = (stack[stackTop-1]!=0.0f && !poison);
		  			pop(); // clear that item off the stack
		  			poison = false;
					}
				}
				--candidateStackTop;
				if (eventFound)
					push(1.0f);
				else
					push(0.0f);
				// note that we're exiting this routine with stack[stackTop-1] == eventFound
			}
			if (isScriptalyzerActive) { pop(); push(myNextBoolean()); }
		}
		else if (tOperator.getLabel().equals("LookUpEvent")) {
			int iPageNumber = engine.getHistoryBookSize();
			if (iPageNumber==0) {
				push(-1);
				setPoison("LookupEvent: no event found!");
			}
			else {
				boolean eventFound = false;
				++candidateStackTop;
				while ((iPageNumber>0) & !eventFound) {
					--iPageNumber;
					candidateStack[candidateStackTop] = iPageNumber;
					candidateTypeStack[candidateStackTop] = Operator.Type.Event;
		  			executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
		  			eventFound = (stack[stackTop-1]!=0.0f && !poison);
		  			pop(); // clear that item off the stack
		  			poison = false;
				}
				--candidateStackTop;
				push(iPageNumber); // save the result
				if (!eventFound) setPoison("LookupEvent: no event found!");
				if (isScriptalyzerActive) { pop(); push(random.nextInt(10)); }
			}
			if (isScriptalyzerActive) { pop(); push(random.nextInt(10)); }
		}
		else if (tOperator.getLabel().equals("LookUpCausalEvent")) {
			int iPageNumber = engine.getHistoryBookSize()-1;
			if (engine.getEventCausalEvent(iPageNumber)<0) {
				push(-1);
				if (iPageNumber<0) setPoison("LookupCausalEvent: no event found!");
			}
			else {
				boolean eventFound = false;
				++candidateStackTop;
				while ((iPageNumber>0) & !eventFound) {
					iPageNumber = engine.getEventCausalEvent(iPageNumber);
					if (iPageNumber>=0) {
						candidateStack[candidateStackTop] = iPageNumber;
						candidateTypeStack[candidateStackTop] = Operator.Type.Event;
		  			executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
		  			eventFound = (stack[stackTop-1]!=0.0f && !poison);
		  			pop(); // clear that item off the stack
		  			poison = false;
					}
				}
				--candidateStackTop;
				push(iPageNumber); // save the result
				if (!eventFound) setPoison("LookupCausalEvent: no event found!");
			}
			if (isScriptalyzerActive) { pop(); push(random.nextInt(10)); }
		}
		else if (tOperator.getLabel().equals("CountEvents")) {
			int cEvents = 0;
			++candidateStackTop;
			for (int i=0; (i<engine.getHistoryBookSize()); ++i) {
				candidateStack[candidateStackTop] = i;
				candidateTypeStack[candidateStackTop] = Operator.Type.Event;
				executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
				if (stack[stackTop-1]!=0.0f && !poison)
					++cEvents;
				pop(); // clear that item off the stack
	  			poison = false;
			}
			--candidateStackTop;
			push(cEvents); // push the count
			if (isScriptalyzerActive) { pop(); push(random.nextInt(10)); }
		}
		else if (tOperator.getLabel().equals("CountCausalEvents")) {
			int cEvents = 0;
			int iPageNumber = engine.getHistoryBookSize()-1;
			boolean eventFound = false;
			++candidateStackTop;
			while ((iPageNumber>0) & (iPageNumber<engine.getHistoryBookSize()) & !eventFound) {
				iPageNumber = engine.getEventCausalEvent(iPageNumber);
				if (iPageNumber>=0) {
					candidateStack[candidateStackTop] = iPageNumber;
					candidateTypeStack[candidateStackTop] = Operator.Type.Event;
	  			executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
	  			if (stack[stackTop-1]!=0.0f && !poison)
	  				++cEvents;
	  			pop(); // clear that item off the stack
	  			poison = false;
				}
			}
			--candidateStackTop;
			push(cEvents); // push the count
			if (isScriptalyzerActive) { pop(); push(random.nextInt(10)); }
		}
		else if (tOperator.getLabel().equals("ElapsedTimeSince")) {
			int iPageNumber = engine.getHistoryBookSize();
			if (iPageNumber==0) {
				push(-1);
				setPoison("ElapsedTimeSince: no event found!");
			}
			else {
				boolean eventFound = false;
				++candidateStackTop;
				while ((iPageNumber>0) & !eventFound) {
					--iPageNumber;
					candidateStack[candidateStackTop] = iPageNumber;
					candidateTypeStack[candidateStackTop] = Operator.Type.Event;
					executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
					eventFound = (stack[stackTop-1] != 0.0f && !poison);
					pop(); // clear that item off the stack
		  			poison = false;
				}
				--candidateStackTop;
				if (eventFound)
					push(engine.getCMoments()-engine.getHistoryBookPage(iPageNumber).getTime()); // save the result
				else
					push(engine.getCMoments());
				if (isScriptalyzerActive) { pop(); push(random.nextInt(100)); }
			}
			if (isScriptalyzerActive) { pop(); push(random.nextInt(100)); }
		}
		else if (tOperator.getLabel().equals("MainClauseIs")) {
			pushMainClauseIs();
		}
		else if (tOperator.getLabel().equals("IHaventDoneThisBefore")) {
			pushIHaventDoneThisBefore();			
		}
		else if (tOperator.getLabel().equals("IHaventDoneThisSince")) {
  			executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
			pushIHaventDoneThisSince();			
		}
		accumulateChunks = saveAccumulateChunks;
	}
//**********************************************************************
	/** Returns the text computed by a script. */
	public String getText(){ return myText; }
	/** Returns the float number computed by a script. */
	public float getFloat(){ return mFloat; }
	/** Returns the boolean computed by a script. */
	public boolean getBoolean(){ return mBool; }
	
	/** Returns the indexes of the words accepted by an Acceptable script that was just executed. */
	@SuppressWarnings("unchecked")
	public void getAcceptables(List<Integer> wordIndexes){
		if (myAcceptable==null)
			return;
		
		if (myAcceptable instanceof ArrayList)
			wordIndexes.addAll((ArrayList)myAcceptable);
		else
			wordIndexes.add((Integer)myAcceptable);
	}
	
	private void pickBestSomething(Operator tOperator, Node zNode) {
		// this is a PickBest operator requiring a loop
		boolean saveAccumulateChunks = accumulateChunks;
		accumulateChunks = false;
		float bestDesirable = Utils.MINI_VALUE;
		int bestIndex = -1;
		int size = 0;
		int startIndex = 0;
		++candidateStackTop;
		String subLabel = tOperator.getLabel().substring(8); // "PickBest" is 8 characters long; this isolates the ending
		if (candidateStackTop > 7)
			setPoison("Interpreter:pickBestSomething:candidateStackTop > 7");
		else {
			if (subLabel.equals("Actor")) {
				startIndex = 1;
				size = dk.getActorCount();
				candidateTypeStack[candidateStackTop] = Operator.Type.Actor;
			}
			else if (subLabel.equals("Prop")) {
				startIndex = 1;
				size = dk.getPropCount();
				candidateTypeStack[candidateStackTop] = Operator.Type.Prop;
			}
			else if (subLabel.equals("Stage")) {
				startIndex = 1;
				size = dk.getStageCount();
				candidateTypeStack[candidateStackTop] = Operator.Type.Stage;
			}
			else if (subLabel.equals("Verb")) {
				size = dk.getVerbCount();
				candidateTypeStack[candidateStackTop] = Operator.Type.Verb;
			}
			else if (subLabel.equals("Event")) {
				size = engine.getHistoryBookSize();
				candidateTypeStack[candidateStackTop] = Operator.Type.Event;
			}
			else if (subLabel.equals("PropTrait")) {
				size = dk.getPropTraits().size();
				candidateTypeStack[candidateStackTop] = Operator.Type.PropTrait;
			}
			else if (subLabel.equals("StageTrait")) {
				size = dk.getStageTraits().size();
				candidateTypeStack[candidateStackTop] = Operator.Type.StageTrait;
			}
			else if (subLabel.equals("MoodTrait")) {
				size = Actor.MoodTrait.values().length;
				candidateTypeStack[candidateStackTop] = Operator.Type.MoodTrait;
			}
			else if (subLabel.equals("Quantifier")) {
				size = dk.quantifiers.size()-1;
				candidateTypeStack[candidateStackTop] = Operator.Type.Quantifier;
			}
			else if (subLabel.equals("ActorTrait")) {
//				startIndex = 7;
				size = dk.getActorTraits().size();
				candidateTypeStack[candidateStackTop] = Operator.Type.ActorTrait;
			}
			else { // if we get this far, there's a problem
				setPoison("error: PickBest Operator without proper ending: "+tOperator.getLabel());
			}
			
			int i = startIndex;
			while (i<size) {
				if ((!subLabel.equals("Prop") || (dk.getProp(i).getInPlay()))) {
					candidateStack[candidateStackTop] = i;
					executeNode((Node)zNode.getChildAt(0)); // the acceptable operator
					if (poison)
						pop();
					else {
						executeNode((Node)zNode.getChildAt(1)); // the desirable operator
						float desirable = pop(); // get the result of the computation
						float acceptable = pop(); // // 
						if (acceptable>0.0f && desirable>bestDesirable) {
							bestDesirable = desirable;
							bestIndex = i;
						}
					}
				}
				++i;
				poison=false;
	    }
		}
		--candidateStackTop;
  		push(bestIndex);
  		if (bestIndex < 0) // no hit! Poison it!
  			setPoison("PickBest"+subLabel+" finds no "+subLabel);
  		accumulateChunks = saveAccumulateChunks;
	}
//**********************************************************************	
	private void parseThisOperator(String tLabel) {
		String zLabel = tLabel.substring(4);
		int end = 0;
		while (Character.isDigit(zLabel.charAt(end))) 
			end++;
		Integer iSocket = Integer.parseInt(zLabel.substring(0,end))-1;
		Operator.Type zType = Sentence.getTypeFromLabel(zLabel.substring(end));
		if (isScriptalyzerActive)
			push(getRandomValue(zType)); 
		else {
			if (engine.getThisEvent().getWordSocketType(iSocket) != zType) {
				setPoison("WordSocket data type does not match Operator data type for "+tLabel);
				push(-1);
			}
			else { push(engine.getThisEvent().getIWord(iSocket)); }
		}
	}
	//**********************************************************************	
	private void parseChosenOperator(String tLabel) {
		String zLabel = tLabel.substring(6);
		int end = 0;
		while (Character.isDigit(zLabel.charAt(end))) 
			end++;
		Integer iSocket = 0;
		iSocket = Integer.parseInt(zLabel.substring(0,end))-1;
		Operator.Type zType = Sentence.getTypeFromLabel(zLabel.substring(end));
		if (isScriptalyzerActive) {
			switch (zType) {
				case Actor: { push(1+random.nextInt(dk.getActorCount()-1)); break; }
				case Prop: { push(1+random.nextInt(dk.getPropCount()-1)); break; }
				case Stage: { push(1+random.nextInt(dk.getStageCount()-1)); break; }
				case ActorTrait: { push(1+random.nextInt(dk.getActorTraits().size()-1)); break; }
				case PropTrait: { push(1+random.nextInt(dk.getPropTraits().size()-1)); break; }
				case StageTrait: { push(1+random.nextInt(dk.getStageTraits().size()-1)); break; }
				case MoodTrait: { push(1+random.nextInt(Actor.MoodTrait.values().length-1)); break; }
				case Quantifier: { push(1+random.nextInt(dk.quantifiers.size()-2)); break; }
			}
		}
		else {
			if (engine.getChosenPlan().getWordSocketType(iSocket) != zType) {
				setPoison("parseChosenOperator: WordSocket data type does not match Operator data type for "+tLabel);
				push(-1);
			}
			else { push(engine.getChosenPlanIWord(iSocket)); }
		}
	}
	//**********************************************************************	
	private void parsePastOperator(String tLabel, Node zNode) {
		executeNode((Node)zNode.getChildAt(0)); // this operator always has just one argument
		int iPage = (int)pop();
		if (iPage<0) {
			setPoison("parsePastOperator has iPage<0"+tLabel);
			push(-1);
		}
		else {
			String zLabel = tLabel.substring(4);		
			int end = 0;
			while (Character.isDigit(zLabel.charAt(end))) 
				end++;
			Integer iSocket = Integer.parseInt(zLabel.substring(0,end))-1;
			Operator.Type zType = Sentence.getTypeFromLabel(zLabel.substring(end));
			if (isScriptalyzerActive) {
				push(1+random.nextInt(dk.getActorCount()-1)); 
			}
			else {
				if (engine.getHistoryBookPage(iPage).getWordSocketType(iSocket) != zType) {
					// CC 12/18/08: there's no point in poisoning here because this can happen
					// as a result of normal operations in HistoryBook searches.
//					setPoison("parsePastOperator: WordSocket data type does not match Operator data type for "+tLabel);
					push(-1); // this is necessary to reset the Stack
				}
				else { push(engine.getHistoryBookPage(iPage).getIWord(iSocket)); }
			}
		}
	}
//**********************************************************************	
	public void executeToken(Node tToken) {
		mNode = tToken;
		mOperator = tToken.getOperator();
		try {
			tToken.getOperator().getMethod().invoke(this, (Object[])null);
		} catch (java.lang.IllegalAccessException e) {
			setPoison("bad operator code A: " + mOperator.getLabel());
		} catch (java.lang.reflect.InvocationTargetException e) {
			setPoison("bad operator code B: " + mOperator.getLabel());
			if (e.getCause().getCause()!=null && e.getCause() instanceof RuntimeException)
				throw (RuntimeException)e.getCause();
			else if (e.getCause().getCause()!=null)
				throw new RuntimeException(e.getCause().getCause());
			else
				throw new RuntimeException(e.getCause());
		} catch (java.lang.NullPointerException e) {
			setPoison("bad operator code C: " + mOperator.getLabel());
			throw new RuntimeException(e);
		}
	}
//***********************************************************************
	/** Adds the elements of a group into another one. */
	@SuppressWarnings("unchecked")
	private static void addGroup(Object input, Collection<Object> output){
		// if input is a Collection, add its elements to the result
		if (input instanceof Collection)
			output.addAll((Collection<?>)input);
		else // if input is not a Collection, assume that it is an 
			// element that must be added to the result
			output.add(input);
	}
	
	/** Joins two groups. */
	public void pushGroup2() {
		Object o2 = popObject();
		Object o1 = popObject();
		final ArrayList<Object> output = new ArrayList<Object>();
		addGroup(o1,output);
		addGroup(o2,output);
		pushObject(output);
	}

	/** Joins three groups. */
	public void pushGroup3() {
		Object o3 = popObject();
		Object o2 = popObject();
		Object o1 = popObject();
		final ArrayList<Object> output = new ArrayList<Object>();
		addGroup(o1,output);
		addGroup(o2,output);
		addGroup(o3,output);
		pushObject(output);
	}

	/** Push a group holding a single word. */
	public void pushWord2Group() {
		pushObject(popInt());
	}

	/** Creates a group of three elements. */
	public void pushGroup3Elems() {
		Object o3 = popInt();
		Object o2 = popInt();
		Object o1 = popInt();
		final ArrayList<Object> output = new ArrayList<Object>();
		addGroup(o1,output);
		addGroup(o2,output);
		addGroup(o3,output);
		pushObject(output);
	}

	/** Creates a group of five elements. */
	public void pushGroup5Elems() {
		Object o5 = popInt();
		Object o4 = popInt();
		Object o3 = popInt();
		Object o2 = popInt();
		Object o1 = popInt();
		final ArrayList<Object> output = new ArrayList<Object>();
		addGroup(o1,output);
		addGroup(o2,output);
		addGroup(o3,output);
		addGroup(o4,output);
		addGroup(o5,output);
		pushObject(output);
	}

	/** Creates a group of seven elements. */
	public void pushGroup7Elems() {
		Object o7 = popInt();
		Object o6 = popInt();
		Object o5 = popInt();
		Object o4 = popInt();
		Object o3 = popInt();
		Object o2 = popInt();
		Object o1 = popInt();
		final ArrayList<Object> output = new ArrayList<Object>();
		addGroup(o1,output);
		addGroup(o2,output);
		addGroup(o3,output);
		addGroup(o4,output);
		addGroup(o5,output);
		addGroup(o6,output);
		addGroup(o7,output);
		pushObject(output);
	}

	/** Creates a group of nine elements. */
	public void pushGroup9Elems() {
		Object o9 = popInt();
		Object o8 = popInt();
		Object o7 = popInt();
		Object o6 = popInt();
		Object o5 = popInt();
		Object o4 = popInt();
		Object o3 = popInt();
		Object o2 = popInt();
		Object o1 = popInt();
		final ArrayList<Object> output = new ArrayList<Object>();
		addGroup(o1,output);
		addGroup(o2,output);
		addGroup(o3,output);
		addGroup(o4,output);
		addGroup(o5,output);
		addGroup(o6,output);
		addGroup(o7,output);
		addGroup(o8,output);
		addGroup(o9,output);
		pushObject(output);
	}


	public void pushAllWordsWhich(Node n) {
		ArrayList<Integer> acceptableList = new ArrayList<Integer>();
		if (getPoison())
			pushObject(acceptableList);
		// It pushes an array of indexes of the words that are acceptable
		++candidateStackTop;
		acceptableList.clear();
		int endIndex = 0;
		boolean acceptZerothItem=false;
		Operator.Type zType = n.getOperator().getDataType(); 
		switch (zType) {
		case ActorGroup: { endIndex = dk.getActorCount(); break; }
		case PropGroup: { endIndex = dk.getPropCount(); break; }
		case StageGroup: { endIndex = dk.getStageCount(); break; }
		case VerbGroup: { endIndex = dk.getVerbCount(); break; }
		case ActorTraitGroup: { acceptZerothItem=true; endIndex = dk.getActorTraits().size(); break; }
		case PropTraitGroup: { acceptZerothItem=true; endIndex = dk.getPropTraits().size(); break; }
		case StageTraitGroup: { acceptZerothItem=true; endIndex = dk.getStageTraits().size(); break; }
		case MoodTraitGroup: { acceptZerothItem=true; endIndex = Actor.MoodTrait.values().length; break; }
		case QuantifierGroup: { acceptZerothItem=true; endIndex = dk.quantifiers.size()-1; break; }
		}
		int i=0;
		boolean availableForUse = true;
		while (i<endIndex) {
			switch (zType) {
			case ActorGroup: { availableForUse = dk.getActor(i).getActive(); break; }
			case PropGroup: { availableForUse = dk.getProp(i).getInPlay(); break; }
			case StageGroup: { availableForUse = dk.getStage(i).getDoorOpen(); break; }
			}
			if (availableForUse) {
				candidateStack[candidateStackTop] = i;
				candidateTypeStack[candidateStackTop] = Operator.getElementType(zType);
				// special case: obviate inActive Actors
				executeNode((Node)n.getFirstChild());
				boolean accepted = pop()!=0;
				if (poison)
					poison = false;
				else if (accepted && (i>0 || acceptZerothItem))
					acceptableList.add(i);
			}
			++i;
		}

		pushObject(acceptableList);
		--candidateStackTop;
	}

//***********************************************************************
	public ArrayList<Float> executeDesirableLoop(ScriptPath sp,Iterable<Integer> acceptableList,OptionWordSocket tWordSocket, Operator.Type tType) 
					throws InterruptedException {
		// This method executes the evaluation loop for desirable scripts.
		//  The argument tType specifies the type of word that is under consideration.
		// It considers only those words that are entered in the acceptableList
		desirableList.clear();
		Script zScript = tWordSocket.getDesirableScript();
		++candidateStackTop;
		for(int i:acceptableList) {
	  		candidateStack[candidateStackTop] = i;
			candidateTypeStack[candidateStackTop] = tType;
			mFloat = -0.99f;
	    	executeScript(sp,zScript);
			desirableList.add(mFloat);
		}
		poison = false;
		--candidateStackTop;
		return(desirableList);
	}
//**********************************************************************	 
	void logOperatorAction(Operator tOperator) {
		if (!poison && engine.isLogging()) {
			float x = 0.0f;
			if (stackTop == 0)
				x = stack[stackTop];
			else
				x = stack[stackTop-1];
			
			try {
				engine.logValueMsgChild(getValueString(tOperator.getDataType(),x,objectStack[objectStackTop>0?objectStackTop-1:0]));
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public String getValueString(Operator.Type t,float x,Object o){
		String subString;
		switch (t) {
			case Actor: { 
				if ((x>=0) & (x<dk.getActorCount()))
					subString = dk.getActor((int)x).getLabel(); 
				else
					subString = "invalid Actor index: "+String.valueOf(x); 
				break; 
			}
			case Prop: { 
				if ((x>=0) & (x<dk.getPropCount())) 
					subString = dk.getProp((int)x).getLabel(); 
				else
					subString = "invalid Prop index: "+String.valueOf(x); 
				break; 
			}
			case Stage: {
				if ((x>=0) & (x<dk.getStageCount())) 
					subString = dk.getStage((int)x).getLabel(); 
				else
					subString = "invalid Stage index: "+String.valueOf(x); 
				break; 
			}
			case Verb: { 
				if ((x>=0) & (x<dk.getVerbCount())) 
					subString = dk.getVerb((int)x).getLabel(); 
				else
					subString = "invalid Verb index: "+String.valueOf(x); 
				break; 
			}
			case ActorTrait: { 
				if ((x>=0) & (x<dk.getActorTraits().size())) 
					subString = dk.getActorTraits().get((int)x).getLabel(); 
				else
					subString = "invalid ActorTrait index: "+String.valueOf(x); 
				break; 
			}
			case PropTrait: { 
				if ((x>=0) & (x<dk.getPropTraits().size())) 
					subString = dk.getPropTraits().get((int)x).getLabel(); 
				else
					subString = "invalid PropTrait index: "+String.valueOf(x); 
				break; 
			}
			case StageTrait: {
				if ((x>=0) & (x<dk.getStageTraits().size())) 
					subString = dk.getStageTraits().get((int)x).getLabel(); 
				else
					subString = "invalid StageTrait index: "+String.valueOf(x); 
				break; 
			}
			case MoodTrait: { 
				if ((x>=0) & (x<Actor.MoodTrait.values().length)) 
					subString = Actor.MoodTrait.values()[(int)x].toString(); 
				else
					subString = "invalid MoodTrait index: "+String.valueOf(x); 
				break; 
			}
			case Quantifier: {
				if ((x>=0) & (x<dk.quantifiers.size())) 
					subString = dk.quantifiers.get((int)x).getLabel(); 
				else
					subString = "invalid Quantifier index: "+String.valueOf(x); 
				break; 
			}
			case Event: {
				if ((x>=0) && (x <engine.getHistoryBookSize())) {
					Sentence zEvent = engine.getHistoryBookPage((int)x);
					String doString;
					if ((zEvent.getIWord(Sentence.DefDirObject)>=0) && (dk.getVerb(zEvent.getIWord(Sentence.Verb)).getWordSocketType(Sentence.DefDirObject)==Operator.Type.Actor))
						doString = dk.getActor(zEvent.getIWord(Sentence.DefDirObject)).getLabel();
					else
						doString = "";
					subString = dk.getActor(zEvent.getIWord(Sentence.Subject)).getLabel()+" "
							  + dk.getVerb(zEvent.getIWord(Sentence.Verb)).getLabel()+" "
							  + doString; 
				}
				else subString = "nonexistent Event";
				break; 
				}
			case Number: { subString = String.valueOf(x); break; }
			case BNumber: { subString = String.valueOf(x); break; }
			case Boolean: { 
				if (x == 0.0f)
					subString = "false";
				else
					subString = "true";
				break; 
				}
			case Text: { subString = (String)objectStack[objectStackTop>0?objectStackTop-1:0]; break; }
			case ActorGroup:
			case PropGroup:
			case StageGroup:
			case VerbGroup:
			case ActorTraitGroup:
			case StageTraitGroup:
			case PropTraitGroup:
			case MoodTraitGroup:
			case QuantifierGroup:
				if (o instanceof ArrayList) {
					ArrayList<Integer> group = (ArrayList<Integer>)o;
					if (group==null) {
						subString="";
						break;
					}
					Operator.Type zType = Operator.getElementType(t);
					subString = group.isEmpty()?"Empty":dk.getLabelByDataType(zType, group.get(0));
					int i=1;
					while(i<4 && i<group.size()) {
						subString += ", "+dk.getLabelByDataType(zType, group.get(i));
						i++;
					}
					if (i>=4)
						subString += " ...";
				} else if (o!=null) {
					Operator.Type zType = Operator.getElementType(t);
					subString = dk.getLabelByDataType(zType,(Integer)o);
				} else
					subString = "";
				break;
			default: { subString = t.name(); }
		}
		return subString;
	}
	
	private float getRandomValue(Operator.Type t){
		switch(t){
		case Actor:
			return random.nextInt(dk.getActorCount());
		case Stage:
			return random.nextInt(dk.getStageCount());
		case Prop:
			return random.nextInt(dk.getPropCount());
		case ActorTrait:
			return random.nextInt(dk.getActorTraits().size());
		case Verb:
			return random.nextInt(dk.getVerbCount());
		case Event:
			return random.nextInt(engine.getHistoryBookSize());
		case MoodTrait:
			return random.nextInt(Actor.MoodTrait.values().length);
		case PropTrait:
			return random.nextInt(dk.getPropTraits().size());
		case Quantifier:
			return random.nextInt(dk.quantifiers.size()-1);
		case StageTrait:
			return random.nextInt(dk.getStageTraits().size());
		case BNumber:
			return randomBNumber();
		case Number:
			return random.nextInt();
		case Boolean:
			return random.nextInt(2);
		}
		return 0;
	}

	/** Executes a custom operator. */
	public void pushCustomOperator(){
		pushCustomOperator(((CustomOperator)mOperator).getBody());
	}
	private void pushCustomOperator(Script body){
		CustomOperator op = (CustomOperator)mOperator;
		pushStackOffset(op.getCArguments());
		try {
			engine.logTokenMsg(body.getRoot().getOperator());
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		executeNode((Node)body.getRoot().getFirstChild());
		popStackOffset();
		float result=pop();
		// pop arguments from the stack
		for(int i=0;i<op.getCArguments();i++)
			pop();
		// push result
		push(result);
		try {
			if (!poison)
				logOperatorAction(op);
			engine.loggerUp();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}
	
	/** pushes a parameter value into the stack. */
	public void pushParameterOperator(){
		if (mOperator.getDataType()==Operator.Type.Text)
			pushObject(objectStack[(int)stack[offsetStack[offsetStackTop-1]+((ParameterOperator)mOperator).getParameterIndex()]]);
		else
			push(stack[offsetStack[offsetStackTop-1]+((ParameterOperator)mOperator).getParameterIndex()]);
	}
	
	private void pushStackOffset(int nargs){
		if (offsetStackTop >= offsetStack.length)
			setPoison("offset stack overflow at " + mOperator.getMethod().getName() + "!!!!");
		else
			offsetStack[offsetStackTop++] = stackTop-nargs;
	}

	private void popStackOffset(){
		offsetStackTop--;
	}
	
//**********************************************************************	 
	public void setAccumulateChunks(boolean newValue) {
		accumulateChunks = newValue;
	}
//**********************************************************************	 
	public float getStackBottom() {
		return(stack[0]);
	}
//**********************************************************************	 
	public StackChunkGroup getStackChunkGroup() {
		return(stackChunkGroup);
	}
//**********************************************************************	 
	public String popString() {	return popObject();	}

	public void pushString(String tValue) {
		pushObject(tValue);
	}

	@SuppressWarnings("unchecked")
	public <T> T popObject() {
		if (stackTop <= 0) {
			setPoison("stack underflow at " + mOperator.getMethod().getName() + "!!!!");
			return null;
		}
		else {
			final T s=(T)objectStack[--objectStackTop];
			objectStack[objectStackTop]=null;
			stackTop--;
			return s;
		}
	}

	public <T> void pushObject(T tValue) {
		if (stackTop >= MAXSTACKSIZE)
			setPoison("stack overflow at " + mOperator.getMethod().getName() + "!!!!");
		stack[stackTop++] = objectStackTop;
		objectStack[objectStackTop++] = tValue;
		return;
	}

	public int popInt() {
		return (int)pop();
	}
	
	public float pop() {
		if (stackTop <= 0) {
			setPoison("stack underflow at " + (mOperator.getMethod()!=null?mOperator.getMethod().getName():mOperator.getLabel()) + "!!!!");
			return -1;
		}
		else 
			return stack[--stackTop];
	}
	
	public float peek() {
		if (stackTop <= 0) {
			setPoison("stack underflow at " + (mOperator.getMethod()!=null?mOperator.getMethod().getName():mOperator.getLabel()) + "!!!!");
			return -1;
		}
		else 
			return stack[stackTop-1];
	}
//**********************************************************************	 
	public void push(float tValue) {
		if (stackTop >= MAXSTACKSIZE)
			setPoison("stack overflow at " + (mOperator.getMethod()!=null?mOperator.getMethod().getName():mOperator.getLabel()) + "!!!!");
		if (Float.isNaN(tValue) | Float.isInfinite(tValue))
			setPoison("NaN " + mOperator.getMethod().getName() + "!!!!");
		stack[stackTop++] = tValue;
		return;
	}
//**********************************************************************	 
	void setPoison(String tCause) {
		if (!poison && !isScriptalyzerActive) {
			poison = true;
			poisonCause = tCause;
			if (!SharedConstants.isRemote)
				System.out.println(tCause);
			try{
				engine.logPoisonMsgChild(tCause);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
//**********************************************************************	 
	public void setScriptalyzer(boolean newValue) {
		isScriptalyzerActive = newValue;
	}
//**********************************************************************	 
//	 here begin the operator-specific methods
//**********************************************************************	 
	public void pushAdd() {
		float x1 = pop();
		float x2 = pop();
		push(x1 + x2);
	}
//**********************************************************************	 
	public void pushSubtract() {
		float x1 = pop();
		float x2 = pop();
		push(x2 - x1);
	}
//**********************************************************************	 
	public void pushMultiply() {
		float x1 = pop();
		float x2 = pop();
		push(x1 * x2);
	}
//**********************************************************************	 
	public void pushDivide() {
		float x1 = pop();
		float x2 = pop();
		if (x1 == 0.0) {
			setPoison("Interpreter: divide by zero");
			push(0.0f);
		}
		else
			push(x2 / x1);
	}
//**********************************************************************	 
	public void pushArithmeticInversion() {
		float x1 = pop();
		push(-x1);
	}
//**********************************************************************	 
	public void pushAbsval() {
		float x1 = pop();
		if (x1 > 0)
			push(x1);
		else
			push(-x1);
	}
//**********************************************************************	 
	public void pushBigger() {
		float x1 = pop();
		float x2 = pop();
		if (x1 > x2)
			push(x1);
		else
			push(x2);
	}
//**********************************************************************	 
	public void pushSmaller() {
		float x1 = pop();
		float x2 = pop();
		if (x1 < x2)
			push(x1);
		else
			push(x2);
	}
//**********************************************************************	 
	public void pushLogicalAND() {
		float x1 = pop();
		float x2 = pop();
		if (x1 * x2 > 0)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushLogicalAND3() {
		float x1 = pop();
		float x2 = pop();
		float x3 = pop();
		if (x1 * x2 * x3 > 0)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushLogicalAND4() {
		float x1 = pop();
		float x2 = pop();
		float x3 = pop();
		float x4 = pop();
		if (x1 * x2 * x3 * x4 > 0)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushLogicalOR() {
		float x1 = pop();
		float x2 = pop();
		if (x1 + x2 > 0)
			push(1);
		else
			push(0);
}
//	**********************************************************************	 
	public void pushLogicalOR3() {
		float x1 = pop();
		float x2 = pop();
		float x3 = pop();
		if (x1+x2+x3 > 0)
			push(1);
		else
			push(0);
}
//	**********************************************************************	 
	public void pushLogicalOR4() {
		float x1 = pop();
		float x2 = pop();
		float x3 = pop();
		float x4 = pop();
		if (x1+x2+x3+x4 > 0)
			push(1);
		else
			push(0);
}
//**********************************************************************	 
	public void pushLogicalEOR() {
		float x1 = pop();
		float x2 = pop();
		if (x1 + x2 == 1)
			push(1);
		else
			push(0);
}
//**********************************************************************	 
	public void pushLogicalInversion() {
		float x1 = pop();
		if (x1 == 0)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushAreSame() {
		float x1 = pop();
		float x2 = pop();
		if (x1 == x2)
			push(1);
		else
			push(0);
	}
	
	public void pushAreSameText() {
		String x1 = popString();
		String x2 = popString();
		if (x1.equals(x2))
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushIGreaterThan() {
		float lower = pop();
		float upper = pop();
		if (upper > lower)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushILessThan() {
		float lower = pop();
		float upper = pop();
		if (upper < lower)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushIGreaterThanOrEqual() {
		float lower = pop();
		float upper = pop();
		if (upper >= lower)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushILessThanOrEqual() {
		float lower = pop();
		float upper = pop();
		if (upper <= lower)
			push(1);
		else
			push(0);
}
//**********************************************************************	 
	public void pushBGreaterThan() {
		float lower = pop();
		float upper = pop();
		if (upper > lower)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushBLessThan() {
		float lower = pop();
		float upper = pop();
		if (upper < lower)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushBGreaterThanOrEqual() {
		float lower = pop();
		float upper = pop();
		if (upper >= lower)
			push(1);
		else
			push(0);
}
//**********************************************************************	 
	public void pushBLessThanOrEqual() {
		float lower = pop();
		float upper = pop();
		if (upper <= lower)
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void pushReactingActorIsAWitness() {
		// For Swat 1.0, ReactingActor is ALWAYS a witness!
		if (engine.getReactingActor()!=Engine.FATE)
			push(1.0f);
		else
			push(0.0f);
	}
//**********************************************************************	 
	public void pushReactingActorIsInvolved() {
		int zReactingActor = engine.getReactingActor();
		int result = 0;
		Verb zVerb = dk.getVerb(engine.getThisEvent().getIWord(Sentence.Verb));
		for (int i = 0; (i < Sentence.MaxWordSockets); ++i) {
			if (zVerb.isWordSocketActive(i) && zVerb.getWordSocketType(i) == Operator.Type.Actor) {
				if (engine.getThisEvent().getIWord(i) == zReactingActor)
					result = 1;					
			}
		}
		push(result);
	}
//**********************************************************************	 
	public void undefined() {
		setPoison("undefined"+mOperator.getDataType());
		push(0);
	}
//**********************************************************************	 
	public void constant() {
		push(mNode.getNumericValue(dk));
	}
	
	public void constantText() {
		pushString((String)mNode.getConstant());
	}

	public void pushTheName(){
		pushString(dk.getLabelByDataType(theScriptPath.getVerb().getWordSocketType(theScript.getIWordSocket()), engine.getThisEvent().getIWord(theScript.getIWordSocket())));
	}

	public void pushWordName(){
		pushString(dk.getLabelByDataType(mOperator.getArgumentDataType(0), (int)pop()));
	}

	public void pushConcat() {
		String latterString = popString();
		pushString(popString()+latterString);
	}
	
	public void pushConcat3() {
		String string3 = popString();
		String string2 = popString();
		pushString(popString()+string2+string3);
	}
	
	public void pushConcat4() {
		String string4 = popString();
		String string3 = popString();
		String string2 = popString();
		pushString(popString()+string2+string3+string4);
	}
	
	public void pushNominativePronoun(){
		Sentence zEvent = engine.getThisEvent();
		int iActor = (int)pop();
		if (engine.getReactingActor()==iActor)
			pushString(leftPanel?"you":"I");
		else {
			// first check to see if iActor is the Subject and in an acceptable location
			boolean gotcha=true;
			 if ((iActor==zEvent.getIWord(Sentence.Subject)) 
						& ((theScript.getIWordSocket()>Sentence.Subject) 
								| (theScript.getType()==Script.Type.WordsocketSuffix)))
					pushString(dk.getActor(iActor).getFemale()?"she":"he");
			else if ((iActor==zEvent.getDirObject()) 
					& ((theScript.getIWordSocket()>Sentence.DefDirObject) 
							| (theScript.getType()==Script.Type.WordsocketSuffix)))
				pushString(dk.getActor(iActor).getFemale()?"she":"he");
			else {
				gotcha=false;
				int i=3;
				while (!gotcha & (i<Sentence.MaxWordSockets)) {
					 if ((iActor==zEvent.getIWord(i)) 
								& ((theScript.getIWordSocket()>i) 
										| (theScript.getType()==Script.Type.WordsocketSuffix))) {
							pushString(dk.getActor(iActor).getFemale()?"she":"he");
							gotcha=true;
					 }
					 ++i;					
				}
			}
			if (!gotcha) pushTheName();
		}
	}
	public void pushGenitivePronoun(){
		Sentence zEvent = engine.getThisEvent();
		int iActor = (int)pop();
		if (engine.getReactingActor()==iActor)
			pushString(leftPanel?"your":"my");
		else {
			// first check to see if iActor is the Subject and in an acceptable location
			 if ((iActor==zEvent.getIWord(Sentence.Subject)) 
						& ((theScript.getIWordSocket()>Sentence.Subject) 
								| (theScript.getType()==Script.Type.WordsocketSuffix)))
					pushString(dk.getActor(iActor).getFemale()?"her":"his");
			else if ((iActor==zEvent.getDirObject()) 
					& ((theScript.getIWordSocket()>Sentence.DefDirObject) 
							| (theScript.getType()==Script.Type.WordsocketSuffix)))
				pushString(dk.getActor(iActor).getFemale()?"her":"his");
			else pushTheName();
		}
	}
	public void pushAccusativePronoun(){
		Sentence zEvent = engine.getThisEvent();
		int iActor = (int)pop();
		if (engine.getReactingActor()==iActor)
			pushString(leftPanel?"you":"me");
		else {
			// first check to see if iActor is the Subject and in an acceptable location
			 if ((iActor==zEvent.getIWord(Sentence.Subject)) 
						& ((theScript.getIWordSocket()>Sentence.Subject) 
								| (theScript.getType()==Script.Type.WordsocketSuffix)))
					pushString(dk.getActor(iActor).getFemale()?"her":"him");
			else if ((iActor==zEvent.getDirObject()) 
					& ((theScript.getIWordSocket()>Sentence.DefDirObject) 
							| (theScript.getType()==Script.Type.WordsocketSuffix)))
				pushString(dk.getActor(iActor).getFemale()?"her":"him");
			else pushTheName();
		}
	}
		
	public void pushReflexivePronoun(){
		Sentence zEvent = engine.getThisEvent();
		int iActor = (int)pop();
		if (engine.getReactingActor()==iActor)
			pushString(leftPanel?"yourself":"myself");
		else {
			// first check to see if iActor is the Subject and in an acceptable location
			 if ((iActor==zEvent.getIWord(Sentence.Subject)) 
						& ((theScript.getIWordSocket()>Sentence.Subject) 
								| (theScript.getType()==Script.Type.WordsocketSuffix)))
					pushString(dk.getActor(iActor).getFemale()?"herself":"himself");
			else if ((iActor==zEvent.getDirObject()) 
					& ((theScript.getIWordSocket()>Sentence.DefDirObject) 
							| (theScript.getType()==Script.Type.WordsocketSuffix)))
				pushString(dk.getActor(iActor).getFemale()?"herself":"himself");
			else pushTheName();
		}
	}
	
	public void pushConjugatedVerb() {
		Sentence zEvent = engine.getThisEvent();
		int iSubject = zEvent.getIWord(Sentence.Subject);
		String verbString = popString();
		if (engine.getReactingActor()!=iSubject)
			verbString+="s";	
		pushString(verbString);
	}

	public void pushPickUpperTextIf(){
		String lowerString = popString();
		String upperString = popString();
		if (pop()==0.0f)
			pushString(lowerString);
		else
			pushString(upperString);
	}
	public void pushIfSubjectIsNotProtagonist(){
		String alternateString = popString();
		if (engine.getReactingActor()==engine.getThisEvent().getIWord(Sentence.Subject))
			pushTheName();
		else
			pushString(alternateString);
	}
	public void pushIfSubjectIsReactingActor(){
		String lowerString = popString();
		String upperString = popString();
		if (engine.getReactingActor()==engine.getThisEvent().getIWord(Sentence.Subject))
			pushString(upperString);
		else
			pushString(lowerString);
	}
	public void pushCalculatedText3(){
		pushCalculatedText(3);
	}
	public void pushCalculatedText5(){
		pushCalculatedText(5);
	}
	public void pushCalculatedText7(){
		pushCalculatedText(7);
	}
	public void pushCalculatedText9(){
		pushCalculatedText(9);
	}
	public void pushCalculatedText11(){
		pushCalculatedText(11);
	}
	public void pushCalculatedText(int nTile){
		float windowSize = 2* (1.0f / (float)nTile);
		int iCut = nTile;
		float x = pop();
		float fCut = 1.0f;
		String testString;
		String resultString = "???";
		boolean foundIt = false;
		while (iCut>0) { 
			// it is necessary to execute the entire loop to clear the stack
			testString = popString();
			fCut -= windowSize;
			if ((fCut<x) & !foundIt) {
				resultString = testString;
				foundIt = true;
			}
			--iCut;
		}
		pushString(resultString);
	}

//**********************************************************************	 
	public void anyActor() {
	}
//**********************************************************************	 
	public void anyStage() {
	}
//**********************************************************************	 
	public void anyProp() {
	}
//**********************************************************************	 
	public void anyVerb() {
	}
//**********************************************************************	 
	public void anyEvent() {
	}
//**********************************************************************	 
	public void anyTrait() {
	}
//**********************************************************************	 
	public void anyQuantifier() {
	}
//**********************************************************************	 
	public void pushVisibleActorTrait() {
		int iInnerTrait = (int) pop();
		if (iInnerTrait>=0 && iInnerTrait<dk.getActorTraits().size())
			if (dk.getActorTraits().get(iInnerTrait).isVisible())
				push(1);
			else
				push(0);
		else {
			setPoison("Actor trait index ("+iInnerTrait+") out of range"); 
			push(0); 
		}
	}
//**********************************************************************	 
	public void pushReactingActor() {
		push(engine.getReactingActor());
		if (isScriptalyzerActive) { pop(); push(1+random.nextInt(dk.getActorCount()-1)); }
	}
//**********************************************************************	 
	public void pushStoryMinute() {
		push(engine.getIMinute());
	}
//**********************************************************************	 
	public void pushStoryHour() {
		push(engine.getIHour());
	}
//**********************************************************************	 
	public void pushStoryDay() {
		push(engine.getIDay());
	}
//**********************************************************************	 
	public void pushStoryTime() {
		push(engine.getCMoments());
	}
//**********************************************************************	 
	public void pushEventCount() {
		push(engine.getHistoryBookSize());
	}
//**********************************************************************	 
	public void pushStoryOver() {
		if (engine.getStoryIsOver())
			push(1);
		else
			push(0);
	}
//**********************************************************************	 
	public void popStoryOver() {
		if (pop() == 1)
			engine.setStoryIsOver(true);
		else
			engine.setStoryIsOver(false);
	}
//**********************************************************************	 
	public void pushProtagonist() {
		push(engine.getReactingActor());
	}
//**********************************************************************	 
	public void pushActive() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			if (dk.getActor(iActor).getActive())
				push(1);
			else
				push(0);
		} else { 
			setPoison("Active: actor index out of range"); 
			push(0);
		}
	}
//**********************************************************************	 
	public void popActive() {
		boolean newActive = (pop() > 0);
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			dk.getActor(iActor).setActive(newActive);
		} else { 
			setPoison("SetActive: actor index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushFemale() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			if (dk.getActor(iActor).getFemale())
				push(1);
			else
				push(0);
		} else { 
			setPoison("Female: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushDontMoveMe() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			if (dk.getActor(iActor).getDontMoveMe())
				push(1);
			else
				push(0);
		} else { 
			setPoison("DontMoveMe: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popDontMoveMe() {
		boolean newValue=(pop()>0);
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			dk.getActor(iActor).setDontMoveMe(newValue);
		} else { 
			setPoison("SetDontMoveMe: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushUnconscious() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			if (dk.getActor(iActor).getUnconscious())
				push(1);
			else
				push(0);
		} else { 
			setPoison("Unconscious: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popUnconscious() {
		boolean newUnconscious = (pop() > 0);
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			dk.getActor(iActor).setUnconscious(newUnconscious);
		} else { 
			setPoison("SetUnconscious: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushLocation() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).getLocation());
		} else { 
			setPoison("Location: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushSpyingOn() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).getSpyingOn());
		} else { 
			setPoison("SpyingOn: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popSpyingOn() {
		int spy = (int) pop();
		int spiedUpon = (int) pop();
		int howLong = (int) pop();
		if ((spy>=0) & (spy<dk.getActorCount()) & (spiedUpon>=0) & (spiedUpon<dk.getActorCount())) {
			dk.getActor(spy).setSpyingOn(dk.getActor(spiedUpon));
			dk.getActor(spy).setHowLongToSpy(engine.getIMinute()+howLong);
		} else { 
			setPoison("SetSpying On: actor index out of range");
		}
	}
//**********************************************************************	 
	public void pushTargetStage() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).getTargetStage());
		} else { 
			setPoison("TargetStage: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popTargetStage() {
		int iStage = (int) pop();
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount()) & (iStage>=0) & (iStage<dk.getStageCount())) {
			dk.getActor(iActor).setTargetStage(dk.getStage(iStage));		
			engine.getThisEvent().addOutcome(-iStage);
		} else { 
			setPoison("SetTargetStage: actor or stage index out of range");
		}
	}
//**********************************************************************	 
	public void pushOccupiedUntil() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).getOccupiedUntil());
		} else { 
			setPoison("OccupiedUntil: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popOccupiedUntil() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			dk.getActor(iActor).setOccupiedUntil((int) pop());
		} else { 
			setPoison("SetOccupiedUntil: actor index out of range");
			pop();
		}
	}
	//**********************************************************************	 
	public void pushOnThePhoneWith() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).getOnThePhoneWith());
		} else { 
			setPoison("OnThePhoneWith: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popOnThePhoneWith() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			dk.getActor(iActor).setOnThePhoneWith((int) pop());
		} else { 
			setPoison("SetOnThePhoneWith: actor index out of range");
			pop();
		}
	}
//**********************************************************************
	@SuppressWarnings("unchecked")
	public void pushActorTextTrait() {
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount())
			pushString(Utils.emptyIfNull(dk.getActor(iActor).getText(((TraitOperator<TextTrait>)mOperator).getTrait())));
		else { 
			setPoison(mOperator.getLabel()+": actor index out of range");
			pushString("");
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}

	@SuppressWarnings("unchecked")
	public void pushActorTrait() {
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount())
			push(dk.getActor(iActor).get(((ActorTraitOperator)mOperator).getTraitType(),((TraitOperator<FloatTrait>)mOperator).getTrait()));
		else { 
			setPoison(mOperator.getLabel()+": actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	public void pushPActorTrait() {
		int iActor2 = (int) pop();
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount()) {
			if (iActor2>=0 & iActor2<dk.getActorCount())
				push(dk.getActor(iActor).get(((PTraitOperator)mOperator).getPTraitType(),((PTraitOperator)mOperator).getTrait(),dk.getActor(iActor2)));
			else { 
				setPoison(mOperator.getLabel()+": actor index"+iActor2+" out of range");
				push(0);
				}
			}
		else { 
			setPoison(mOperator.getLabel()+": actor index"+iActor+" out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
	//**********************************************************************	 
	public void pushPPropTrait() {
		int iProp2 = (int) pop();
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount()) {
			if (iProp2>=0 & iProp2<dk.getPropCount())
				push(dk.getActor(iActor).get(((PTraitOperator)mOperator).getPTraitType(),((PTraitOperator)mOperator).getTrait(),dk.getProp(iProp2)));
			else { 
				setPoison(mOperator.getLabel()+": prop index"+iProp2+" out of range");
				push(0);
			}
		}
		else { 
			setPoison(mOperator.getLabel()+": actor index"+iActor+" out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
	//**********************************************************************	 
	public void pushPStageTrait() {
		int iStage2 = (int) pop();
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount()) {
			if (iStage2>=0 & iStage2<dk.getStageCount())
				push(dk.getActor(iActor).get(((PTraitOperator)mOperator).getPTraitType(),((PTraitOperator)mOperator).getTrait(),dk.getStage(iStage2)));
			else { 
				setPoison(mOperator.getLabel()+": stage index"+iStage2+" out of range");
				push(0);
			}
		}
		else { 
			setPoison(mOperator.getLabel()+": actor index"+iActor+" out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
	//**********************************************************************	 
	@SuppressWarnings("unchecked")
	public void popActorTrait() {
		float x = pop();
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount())
			dk.getActor(iActor).set(((TraitOperator<FloatTrait>)mOperator).getTrait(),x);
		else { 
			setPoison(mOperator.getLabel()+": actor index"+iActor+" out of range");
			pop();
		}
	}
	//**********************************************************************	 
	public void popPActorTrait() {
		float x = pop();
		int iActor2 = (int) pop();
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount()) {
			if (iActor2>=0 & iActor2<dk.getActorCount())
				dk.getActor(iActor).set(((PTraitOperator)mOperator).getPTraitType(),((PTraitOperator)mOperator).getTrait(),dk.getActor(iActor2),x);
			else { 
				setPoison(mOperator.getLabel()+": actor index"+iActor2+" out of range");
				pop();
			}
		}
		else { 
			setPoison(mOperator.getLabel()+": actor index"+iActor+" out of range");
			pop();
		}
	}
	//**********************************************************************	 
	public void popPPropTrait() {
		float x = pop();
		int iProp2 = (int) pop();
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount()) {
			if (iProp2>=0 & iProp2<dk.getPropCount())
				dk.getActor(iActor).set(((PTraitOperator)mOperator).getPTraitType(),((PTraitOperator)mOperator).getTrait(),dk.getProp(iProp2),x);
			else { 
				setPoison(mOperator.getLabel()+": prop index"+iProp2+" out of range");
				pop();
			}
		}
		else { 
			setPoison(mOperator.getLabel()+": actor index"+iActor+" out of range");
			pop();
		}
	}
	//**********************************************************************	 
	public void popPStageTrait() {
		float x = pop();
		int iStage2 = (int) pop();
		int iActor = (int) pop();
		if (iActor>=0 & iActor<dk.getActorCount()) {
			if (iStage2>=0 & iStage2<dk.getStageCount())
				dk.getActor(iActor).set(((PTraitOperator)mOperator).getPTraitType(),((PTraitOperator)mOperator).getTrait(),dk.getStage(iStage2),x);
			else { 
				setPoison(mOperator.getLabel()+": stage index"+iStage2+" out of range");
				pop();
			}
		}
		else { 
			setPoison(mOperator.getLabel()+": actor index"+iActor+" out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushDisgusted_Aroused() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).get(Actor.MoodTrait.Disgusted_Aroused));
		} else { 
			setPoison("Disgusted_Aroused: actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	/*public void popDisgusted_Aroused() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			dk.getActor(iActor).set(Actor.MoodTrait.Disgusted_Aroused,pop());
		} else { 
			setPoison("SetDisgusted_Aroused: actor index out of range");
			pop();
		}
	}
	*/
//**********************************************************************	 
	public void pushSad_Happy() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).get(Actor.MoodTrait.Sad_Happy));
		} else { 
			setPoison("Sad_Happy: actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	public void pushFearful_Angry() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).get(Actor.MoodTrait.Fearful_Angry));
		} else { 
			setPoison("Fearful_Angry: actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//	**********************************************************************	 
	public void pushTired_Energetic() {
		int iActor = (int) pop();
		if ((iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getActor(iActor).get(Actor.MoodTrait.Tired_Energetic));
		} else { 
			setPoison("Tired_Energetic: actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	public void pushDebt_Grace() {
		int owedTo = (int) pop();
		int ower = (int) pop();
		if ((ower>=0) & (ower<dk.getActorCount()) & (owedTo>=0) & (owedTo<dk.getActorCount())) {
			push(dk.getActor(ower).get(Actor.ExtraTrait.debt_Grace,dk.getActor(owedTo)));
		} else { 
			setPoison("pushDebt_Grace: actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	public void adjustDebt_Grace() {
		float adjustment = pop();
		int creditor = (int) pop();
		int debtor = (int) pop();
		if ((creditor>=0) && (creditor<dk.getActorCount()) && (debtor>=0) && (debtor<dk.getActorCount())) {
			push(dk.getActor(debtor).get(Actor.ExtraTrait.debt_Grace,dk.getActor(creditor)));
			push(adjustment);
			pushBSum();
			adjustment = pop();
			dk.getActor(debtor).set(Actor.ExtraTrait.debt_Grace,dk.getActor(creditor), adjustment);
		} else { 
			setPoison("adjustDebt_Grace: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushKinship() {
		int perceived = (int) pop();
		int perceiver = (int) pop();
		if ((perceiver>=0) & (perceiver<dk.getActorCount()) & (perceived>=0) & (perceived<dk.getActorCount())) {
			push(dk.getActor(perceiver).get(Actor.ExtraTrait.stranger_Kin,dk.getActor(perceived)));
		} else { 
			setPoison("Kinship: actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	public void popKinship() {
		float newKinship = pop();
		int perceived = (int) pop();
		int perceiver = (int) pop();
		if ((perceiver>=0) & (perceiver<dk.getActorCount()) & (perceived>=0) & (perceived<dk.getActorCount())) {
			dk.getActor(perceiver).set(Actor.ExtraTrait.stranger_Kin,dk.getActor(perceived),newKinship);
		} else { 
			setPoison("SetKinship: actor index out of range");
		}
	}
//**********************************************************************	 
	public void pushPropTraitP2Worthless_Valuable() {
		push(dk.findPropTraitWord("P2Worthless_Valuable"));
	}
//**********************************************************************	 
	@SuppressWarnings("unchecked")
	public void pushPropTraitWord() {		
		push(dk.findPropTraitWord(((TraitOperator<FloatTrait>)mOperator).getTrait()));
	}
//**********************************************************************	 
	public void pushPropTraitKnowsMe() {
		push(dk.findPropTraitWord("KnowsMe"));
	}
//**********************************************************************	 
	public void pushStageTraitKnowsMe() {
		push(dk.findStageTrait("KnowsMe"));
	}
//**********************************************************************	 
	public void pushStageTraitUnwelcoming_Homey() {
		push(dk.findStageTrait("Unwelcoming_Homey"));
	}
//**********************************************************************	 
	public void pushKnowsActor() {
		int aboutWhom = (int) pop();
		int knower = (int) pop();
		if ((aboutWhom>=0) & (aboutWhom<dk.getActorCount()) & (knower>=0) & (knower<dk.getActorCount())) {
			if (dk.getActor(aboutWhom).getKnowsMe(dk.getActor(knower)))
				push(1);
			else
				push(0);
		} else { 
			setPoison("KnowsActor: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popSetKnowsActor() {
		boolean newValue = false;
		if (pop()>0.0f)
			newValue = true;
		else
			newValue = false;
		int aboutWhom = (int) pop();
		int knower = (int) pop();
		if ((aboutWhom>=0) & (aboutWhom<dk.getActorCount()) & (knower>=0) & (knower<dk.getActorCount())) {
			dk.getActor(aboutWhom).setKnowsMe(dk.getActor(knower), newValue);
		} else { 
			setPoison("KnowsActor: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushKnowsProp() {
		int aboutWhat = (int) pop();
		int knower = (int) pop();
		if ((aboutWhat>=0) & (aboutWhat<dk.getPropCount()) & (knower>=0) & (knower<dk.getActorCount())) {
			if (dk.getProp(aboutWhat).getKnowsMe(dk.getActor(knower)))
				push(1);
			else
				push(0);
		} else { 
			setPoison("KnowsProp: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popSetKnowsProp() {
		boolean newValue = false;
		if (pop()>0.0f)
			newValue = true;
		else
			newValue = false;
		int aboutWhat = (int) pop();
		int knower = (int) pop();
		if ((aboutWhat>=0) & (aboutWhat<dk.getPropCount()) & (knower>=0) & (knower<dk.getActorCount())) {
			dk.getProp(aboutWhat).setKnowsMe(dk.getActor(knower), newValue);
		} else { 
			setPoison("KnowsProp: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushKnowsStage() {
		int aboutWhere = (int) pop();
		int knower = (int) pop();
		if ((aboutWhere>=0) & (aboutWhere<dk.getStageCount()) & (knower>=0) & (knower<dk.getActorCount())) {
			if (dk.getStage(aboutWhere).getKnowsMe(dk.getActor(knower)))
				push(1);
			else
				push(0);
		} else { 
			setPoison("KnowsStage: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popSetKnowsStage() {
		boolean newValue = false;
		if (pop()>0.0f)
			newValue = true;
		else
			newValue = false;
		int aboutWhat = (int) pop();
		int knower = (int) pop();
		if ((aboutWhat>=0) & (aboutWhat<dk.getStageCount()) & (knower>=0) & (knower<dk.getActorCount())) {
			dk.getStage(aboutWhat).setKnowsMe(dk.getActor(knower), newValue);
		} else { 
			setPoison("KnowsStage: actor index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushCarried() {
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			if (dk.getProp(iProp).getCarried())
				push(1);
			else
				push(0);
		} else { 
			setPoison("Carried: prop index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popCarried() {
		float x = pop();
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			dk.getProp(iProp).setCarried(x > 0);
		} else { 
			setPoison("SetCarried: prop index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushVisible() {
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			if (dk.getProp(iProp).getVisible())
				push(1);
			else
				push(0);
		} else { 
			setPoison("Visible: prop index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popVisible() {
		float x = pop();
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			dk.getProp(iProp).setVisible(x > 0);
		} else { 
			setPoison("SetVisible: prop index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushInPlay() {
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			if (dk.getProp(iProp).getInPlay())
				push(1);
			else
				push(0);
		} else { 
			setPoison("InPlay: prop index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popInPlay() {
		float x = pop();
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			dk.getProp(iProp).setInPlay(x > 0);
		} else { 
			setPoison("SetInPlay: prop index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushPropOwner() {
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			push(dk.getProp(iProp).getOwner());
		} else { 
			setPoison("PropOwner: prop index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popPropOwner() {
		float x = pop();
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			dk.getProp(iProp).setOwner(dk.getActor((int)x));
		} else { 
			setPoison("SetPropOwner: prop index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushPropLocation() {
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			push(dk.getProp(iProp).getLocation());
		} else { 
			setPoison("PropLocation: prop index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popPropLocation() {
		float x = pop();
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			dk.getProp(iProp).setLocation(dk.getStage((int)x));
		} else { 
			setPoison("SetPropLocation: prop index out of range");
			pop();
		}
	}
//**********************************************************************	 
	@SuppressWarnings("unchecked")
	public void pushPropTextTrait() {
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			pushString(Utils.emptyIfNull(dk.getProp(iProp).getText(((TraitOperator<TextTrait>)mOperator).getTrait())));
		} else { 
			setPoison(((TraitOperator)mOperator).getTrait().getLabel()+": prop index out of range");
			pushString("");
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}

	@SuppressWarnings("unchecked")
	public void pushPropTrait() {
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			push(dk.getProp(iProp).getTrait(((TraitOperator<FloatTrait>)mOperator).getTrait()));
		} else { 
			setPoison(((TraitOperator)mOperator).getTrait().getLabel()+": prop index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	@SuppressWarnings("unchecked")
	public void popPropTrait() {
		float x = pop();
		int iProp = (int) pop();
		if ((iProp>=0) & (iProp<dk.getPropCount())) {
			dk.getProp(iProp).setTrait(((TraitOperator<FloatTrait>)mOperator).getTrait(),x);
		} else { 
			setPoison(((TraitOperator)mOperator).getTrait().getLabel()+": prop index out of range");
			pop();
		}
	}
//	**********************************************************************
	@SuppressWarnings("unchecked")
	public void pushStageTextTrait() {
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			pushString(Utils.emptyIfNull(dk.getStage(iStage).getText(((TraitOperator<TextTrait>)mOperator).getTrait())));
		} else { 
			setPoison(((TraitOperator)mOperator).getTrait().getLabel()+": stage index out of range");
			pushString("");
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}

	@SuppressWarnings("unchecked")
	public void pushStageTrait() {
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			push(dk.getStage(iStage).getTrait(((TraitOperator<FloatTrait>)mOperator).getTrait()));
		} else { 
			setPoison(((TraitOperator)mOperator).getTrait().getLabel()+": stage index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//	**********************************************************************	 
	@SuppressWarnings("unchecked")
	public void popStageTrait() {
		float x = pop();
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			dk.getStage(iStage).setTrait(((TraitOperator<FloatTrait>)mOperator).getTrait(),x);
		} else { 
			setPoison(((TraitOperator)mOperator).getTrait().getLabel()+": stage index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushActorsOnStage() {
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
//			push(dk.getStage(iStage).getPopulation());
			int count=0;
			for (int i=1; (i<dk.getActorCount()); ++i) {
				if (iStage==dk.getActor(i).getLocation()) ++count;
			}
			push(count);
		} else { 
			setPoison("ActorsOnStage: stage index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushStageOwner() {
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			push(dk.getStage(iStage).getOwner());
		} else { 
			setPoison("StageOwner: stage index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushXCoord() {
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			push((float) dk.getStage(iStage).getXCoord());
		} else { 
			setPoison("XCoord: stage index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popXCoord() {
		float x = pop();
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			dk.getStage(iStage).setXCoord((int)x);
		} else { 
			setPoison("SetXCoord: stage index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushYCoord() {
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			push((float) dk.getStage(iStage).getYCoord());
		} else { 
			setPoison("YCoord: stage index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popYCoord() {
		float x = pop();
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			dk.getStage(iStage).setYCoord((int)x);
		} else { 
			setPoison("SetYCoord: stage index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushUnwelcoming_Homey() {
		int iStage = (int) pop();
		int iActor = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount()) & (iActor>=0) & (iActor<dk.getActorCount())) {
			push(dk.getStage(iStage).getUnwelcoming_Homey(dk.getActor(iActor)));
		} else { 
			setPoison("Unwelcoming_Homey: stage index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	public void popUnwelcoming_Homey() {
		float x = pop();
		int iStage = (int) pop();
		int iActor = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount()) & (iActor>=0) & (iActor<dk.getActorCount())) {
			dk.getStage(iStage).setUnwelcoming_Homey(dk.getActor(iActor), x);
		} else { 
			setPoison("SetUnwelcoming_Homey: stage index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushDoorOpen() {
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			if (dk.getStage(iStage).getDoorOpen())
				push(1);
			else
				push(0);
		} else { 
			setPoison("DoorOpen: stage index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void popDoorOpen() {
		float x = pop();
		int iStage = (int) pop();
		if ((iStage>=0) & (iStage<dk.getStageCount())) {
			dk.getStage(iStage).setDoorOpen(x > 0);
		} else { 
			setPoison("SetDoorOpen: stage index out of range");
			pop();
		}
	}
//**********************************************************************	 
	public void pushTimeToPrepare() {
		int iVerb = (int) pop();
		if ((iVerb>=0) & (iVerb<dk.getVerbCount())) {
			push(dk.getVerb(iVerb).getTimeToPrepare());
		} else { 
			setPoison("TimeToPrepare: verb index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushTimeToExecute() {
		int iVerb = (int) pop();
		if ((iVerb>=0) & (iVerb<dk.getVerbCount())) {
			push(dk.getVerb(iVerb).getTimeToExecute());
		} else { 
			setPoison("TimeToExecute: verb index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushTrivial_Momentous() {
		int iVerb = (int) pop();
		if ((iVerb>=0) & (iVerb<dk.getVerbCount())) {
			push(dk.getVerb(iVerb).getTrivial_Momentous());
		} else { 
			setPoison("Trivial_Momentous: verb index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushTotalActors() {
		push(dk.getActorCount());
	}
//**********************************************************************	 
	public void pushTotalStages() {
		push(dk.getStageCount());
	}
//**********************************************************************	 
	public void pushTotalProps() {
		push(dk.getPropCount());
	}
//**********************************************************************	 
	public void pushTotalVerbs() {
		push(dk.getVerbCount());
	}
//**********************************************************************	 
	public void popRoleActive() {
		mBool = pop()>0;
	}
//**********************************************************************	 
	public void pushThisActor(int iSocket) {
		if (isScriptalyzerActive) { push(1+random.nextInt(dk.getActorCount()-1)); }
		else { push(engine.getThisEvent().getIWord(iSocket)); }
	}
//**********************************************************************	 
	public void pushThisSubject() { pushThisActor(Sentence.Subject); }
//**********************************************************************	 
	public void pushThisVerb() { push(engine.getThisEvent().getIWord(Sentence.Verb)); }
//**********************************************************************	 
	public void pushThisDirObject() { pushThisActor(Sentence.DefDirObject); }
//**********************************************************************	 
	public void pushThisTime() {
		push((float) engine.getThisEvent().getTime());
		if (isScriptalyzerActive) { pop(); push(random.nextInt(100)); }
	}
//**********************************************************************	 
	public void pushThisLocation() {
		push((float) engine.getThisEvent().getLocation());
		if (isScriptalyzerActive) { pop(); push(1+random.nextInt(dk.getStageCount()-1)); }
	}
//**********************************************************************	 
	public void pushThisCausalEvent() {
		push((float) engine.getThisEvent().getCausalEvent());
		if (isScriptalyzerActive) { pop(); push(0); }
	}
//**********************************************************************	 
	public void pushThisPageNumber() {
		push((float) engine.getThisEvent().getPageNumber());
		if (isScriptalyzerActive) { pop(); push(0); }
	}
//**********************************************************************	 
	public void pushThisHijacked() {
		if (engine.getThisEvent().getHijacked())
			push((float) 1.0);
		else
			push(0.0f);
	}
//**********************************************************************	 
	public void pushThisWhoKnows() {
		int zWho = (int) pop();
		if ((zWho >= 0) & (zWho < dk.getActorCount())) {
			if (engine.getThisEvent().getWhoKnows(zWho))
				push((float) 1.0);
			else
				push(0.0f);
		}	else {
			setPoison("ThisWhoKnows: actor index out of range");
			push(0.0f);
		}
		if (isScriptalyzerActive) { pop(); push(1+random.nextInt(dk.getActorCount()-1)); }
	}
//**********************************************************************	 
	public void pushThisBelief() {
		int zWho = (int) pop();
		if ((zWho >= 0) & (zWho < dk.getActorCount())) {
			push((float) engine.getThisEvent().getBelief(zWho));
		}	else {
			setPoison("ThisBelief: actor index out of range");
			push(0.0f);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//**********************************************************************	 
	public void pushChosenVerb() {
		push((float) (engine.getChosenPlanIWord(Sentence.Verb)));
	}
//**********************************************************************	 
	public void pushChosenDirObject() {
		if (engine.getChosenPlan().getWordSocket(Sentence.DefDirObject).getType() != Operator.Type.Actor)
			setPoison("Interpreter:pushChosenDirObject: WordSocket 3 is not Actor Type");
		push((float) (engine.getChosenPlanIWord(Sentence.DefDirObject)));
		if (isScriptalyzerActive) { pop(); push(1+random.nextInt(dk.getActorCount()-1)); }
	}
//**********************************************************************	 
	public void pushPastSubject() {
		int iPageNumber = (int)pop();
		if (isScriptalyzerActive) { push(1+random.nextInt(dk.getActorCount()-1)); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
				push((float) (engine.getHistoryBookPage(iPageNumber).getIWord(Sentence.Subject)));
			} else {
				setPoison("PastSubject: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastVerb() {
		int iPageNumber = (int)pop();
		if (isScriptalyzerActive) { push(1+random.nextInt(dk.getVerbCount()-1)); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
				push((float) (engine.getHistoryBookPage(iPageNumber).getIWord(Sentence.Verb)));
			} else {
				setPoison("PastVerb: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastDirObject() {
		int iPageNumber = (int)pop();
		if (isScriptalyzerActive) { push(1+random.nextInt(dk.getActorCount())); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
				if (engine.getHistoryBookPage(iPageNumber).getWordSocketType(Sentence.DefDirObject)==Operator.Type.Actor)
					push((float) (engine.getHistoryBookPage(iPageNumber).getIWord(Sentence.DefDirObject)));
				else {
					Verb v = dk.getVerb(engine.getHistoryBookPage(iPageNumber).getIWord(Sentence.Verb));
					setPoison("PastDirObject: the DirObject of \""+v.getCategory()+": "+v.getLabel()+"\" is not of type Actor");
					push(0);
				}
			} else {
				setPoison("PastDirObject: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastTime() {
		int iPage = (int)pop();
		if (isScriptalyzerActive) { pop(); push(random.nextInt(100)); }
		else {
			if ((iPage >= 0) & (iPage < engine.getHistoryBookSize())) {
				push(engine.getHistoryBookPage(iPage).getTime());
			} else {
				setPoison("PastLocation: history book pageNumber out of range");
				push(0);
			}
		}
	}
///**********************************************************************	 
	public void pushPastLocation() {
		int iPageNumber = (int)pop();
		if (isScriptalyzerActive) { push(random.nextInt(dk.getStageTraits().size())); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
				push(engine.getHistoryBookPage(iPageNumber).getLocation());
			} else {
				setPoison("PastLocation: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastCausalEvent() {
		int iPageNumber = (int)pop();
		if (isScriptalyzerActive) { push(0); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
				push((float) engine.getHistoryBookPage(iPageNumber).getCausalEvent());
			} else {
				setPoison("PastCausalEvent: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastPageNumber() {
		int iPageNumber = (int)pop();
		if (isScriptalyzerActive) { push(0); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
				push((float) engine.getHistoryBookPage(iPageNumber).getPageNumber());
			} else {
				setPoison("PastPageNumber: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastHijacked() {
		int iPageNumber = (int)pop();
		if (isScriptalyzerActive) { push(0); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
				if (engine.getHistoryBookPage(iPageNumber).getHijacked())
					push(1.0f);
				else
					push(0.0f);
			} else {
				setPoison("PastHijacked: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastWhoKnows() {
		int iPageNumber = (int)pop();
		int iActor = (int)pop();
		if (isScriptalyzerActive) { push(0); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())
					& (iActor >= 0) & (iActor < dk.getActorCount())) {
				if (engine.getHistoryBookPage(iPageNumber).getWhoKnows(iActor))
					push(1.0f);
				else
					push(0.0f);
			} else {
				setPoison("PastWhoKnows: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
	public void pushPastBelief() {
		int iPageNumber = (int) pop();
		int iActor = (int) pop();
		if (isScriptalyzerActive) { push(randomBNumber()); }
		else {
			if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())
					& (iActor >= 0) & (iActor < dk.getActorCount())) {
				push((float) engine.getHistoryBookPage(iPageNumber).getBelief(iActor));
			} else {
				setPoison("PastHistory: history book pageNumber out of range");
				push(0);
			}
		}
	}
//**********************************************************************	 
// Although these next methods don't do anything, they are necessary
// in order to keep the initialization of Script happy.
//**********************************************************************		
	public void pushEventHappened() {
	}
//**********************************************************************	 
	public void pushCausalEventHappened() {
	}
//**********************************************************************	 
	public void pushLookUpEvent() {
	}
//**********************************************************************	 
	public void pushLookUpCausalEvent() {
	}
//**********************************************************************	 
	public void pushCountEvents() {
	}
//**********************************************************************	 
	public void pushCountCausalEvents() {
	}
//**********************************************************************
	public void pushElapsedTimeSince() {		
	}
//**********************************************************************	 
	private void adjustMood(Actor.MoodTrait t) {
		float delta = pop();
		float accord = 0.0f;
		int ofWhom = engine.getReactingActor();
		float p2 = dk.getActor(ofWhom).get(t);
		float up2 = 0.0f;
		if (isScriptalyzerActive) { return; } // this should duplicate the effect of the method

		accord = dk.getActor(ofWhom).get(dk.getCool_VolatilTrait());
		push(delta);
		push(accord);
		pushBNumber2UNumber(); // convert accord BNumber to a UNumber
		pushBoundedProduct();
		push(p2);
		pushBSum(); // add the net change to the original value
		float newP2 = pop();		
		// Next, we calculate the change in C based on the difference between
		//   the original P2 and the new value of P2, blended with C
		push(p2);
		push(newP2);
		pushBoundedDifference(); // get difference between old and new P2s
		pushAbsval();   // use the absolute value of that difference
		pushUNumber2BNumber(); // convert to BNumber
		push(up2);
		push(0.0f); // specify equal blending ratio
		pushBlend(); // blend the two
		pop(); //float newC = pop();
		if (Actor.MoodTraits.contains(t))
			dk.getActor(ofWhom).set(t,newP2);
	}
//**********************************************************************	 
	public void adjustPActorTrait(){
		float delta = pop();
		Actor toWhom = dk.getActor((int)pop());
		FloatTrait t = dk.getActorTraits().get((int)pop());
		if (isScriptalyzerActive) { return; } // this should duplicate the effect of the method
		
		int ofWhom = engine.getReactingActor();
		float accord = dk.getActor(ofWhom).getAccord(t);
		float p2 = dk.getActor(ofWhom).getP(t,toWhom);
		float confidence = dk.getActor(ofWhom).getC(t,toWhom);
		
		// scale down delta by degree of confidence (multiply by uncertainty)
		push(confidence);
		pushArithmeticInversion();
		pushBNumber2UNumber();
		push(delta);
		pushMultiply(); // I use a regular multiplication here because values are less than 1.00
		delta = pop();
		
		push(delta);
		push(accord);
		pushBNumber2UNumber(); // convert accord BNumber to a UNumber
		pushBoundedProduct();
		push(p2);
		pushBSum(); // add the net change to the original value
		float newP2 = pop();		
		// Next, we calculate the change in C based on the difference between
		//   the original P2 and the new value of P2, blended with C
		push(p2);
		push(newP2);
		pushBoundedDifference(); // get difference between old and new P2s
		pushAbsval();   // use the absolute value of that difference
		pushUNumber2BNumber(); // convert to BNumber 
		push(confidence);
		push(0.0f); // specify equal blending ratio
		pushBlend(); // blend the two
		float newC = pop();
		dk.getActor(ofWhom).setP(t,toWhom,newP2);
		dk.getActor(ofWhom).setC(t,toWhom,newC);
	}
//**********************************************************************	 
	@SuppressWarnings("unchecked")
	public void pushPActorTraitWeight() {
		pushPActorTraitWeight(((TraitOperator<FloatTrait>)mOperator).getTrait());
	}
	public void pushPActorTraitWeight(FloatTrait t) {
		int forWhom = (int)pop();
		int ofWhom = (int)pop();
		if (((ofWhom >= 0) & (ofWhom < dk.getActorCount())) & ((forWhom >= 0) & (forWhom < dk.getActorCount()))) {
			float trueWeight = dk.getActor(forWhom).getWeight(t);
			float up2 = dk.getActor(ofWhom).getC(t,dk.getActor(ofWhom));
			// the second multiplier is the same as BNumber2UNumber
			float perceived = trueWeight * (1.0f - (1.0f-up2)/2.0f);
			push(perceived);
		} else {
			setPoison(mOperator.getLabel()+": actor index out of range");
			push(0);
		}
		if (isScriptalyzerActive) { pop(); push(randomBNumber()); }
	}
//	**********************************************************************	 
	public void pushCorrespondingP2Weight() {
	// assumes an InnerTrait input on the stack
		int iInnerTrait = (int) pop();
		if (iInnerTrait >= 0 & iInnerTrait < dk.getActorTraits().size())
			pushPActorTraitWeight(dk.getActorTraits().get(iInnerTrait));
		else setPoison("InnerTrait index out of range"); push(0); 
	}
//**********************************************************************
	public void adjustDisgusted_Aroused() {
		adjustMood(Actor.MoodTrait.Disgusted_Aroused);
	}
//**********************************************************************	 
	public void adjustSad_Happy() {
		adjustMood(Actor.MoodTrait.Sad_Happy);
	}
//**********************************************************************	 
	public void adjustFearful_Angry() {
		adjustMood(Actor.MoodTrait.Fearful_Angry);
	}
//	**********************************************************************	 
	public void adjustTired_Energetic() {
		adjustMood(Actor.MoodTrait.Tired_Energetic);
	}
//**********************************************************************	 
	public void createMeetingAlarm() {
		int whom = (int) pop(); // the Actor met
		int who = (int) pop(); // the Subject
		if ((who >= 0) & (who < dk.getActorCount())
				& (whom >= 0) & (whom < dk.getActorCount())) {
			engine.addAlarmMEETACTOR(who, whom);
		} else {
			setPoison("createMeetingAlarm: actor index out of range");
		}
	}
//**********************************************************************	 
	public void createStageAlarm() {
		int iStage = (int) pop();
		int iActor = (int) pop();
		if ((iActor >= 0) & (iActor < dk.getActorCount())
				& (iStage >= 0) & (iStage < dk.getStageCount())) {
			engine.addAlarmMEETSTAGE(iActor, iStage);
		} else {
			setPoison("createStageAlarm: actor or stage index out of range");
		}
	}
//**********************************************************************	 
	public void createPropAlarm() {
		int iProp = (int) pop();
		int iActor = (int) pop();
		if ((iActor >= 0) & (iActor < dk.getActorCount())
				& (iProp >= 0) & (iProp < dk.getPropCount())) {
			engine.addAlarmMEETPROP(iActor, iProp);
		} else {
			setPoison("createPropAlarm: actor or prop index out of range");
		}
	}
//**********************************************************************	 
	public void createClockAlarm() {
		int iWhen = (int) pop() + engine.getCMoments();
		int iActor = (int) pop();
		if ((iActor >= 0) & (iActor < dk.getActorCount()) & (iWhen > 0)) {
			engine.addAlarmMEETTIME(iActor, iWhen);
		} else {
			setPoison("createClockAlarm: actor index out of range");
		}
	}
//**********************************************************************	
// Although these next methods don't do anything, they are necessary
// in order to keep the initialization of Script happy.
//**********************************************************************		
	public void pushPickBestActor() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestStage() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestProp() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestVerb() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestEvent() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestActorTrait() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestPropTrait() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestStageTrait() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestMoodTrait() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	public void pushPickBestQuantifier() {
		// this operator doesn't need to do anything; it's all done by ExecuteNode
	}
//**********************************************************************	 
	private int matchingCandidate(Operator.Type tType) {
		int i = candidateStackTop;
		boolean foundMatch = false;
		while (!foundMatch & (i >= 0)) {
			foundMatch = candidateTypeStack[i] == tType;
			--i;
		}
		if (foundMatch)
			return (i+1);
		else
			return (-1);
	}
//**********************************************************************	 
	public void pushCandidateActor() {
		if (isScriptalyzerActive) 
			push(random.nextInt(1+dk.getActorCount()-1));
		else 
			push((float) candidateStack[matchingCandidate(Operator.Type.Actor)]);
	}
//**********************************************************************	 
	public void pushCandidateStage() {
		if (isScriptalyzerActive) 
			push(random.nextInt(1+dk.getStageCount()-1));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.Stage)]);
	}
//**********************************************************************	 
	public void pushCandidateProp() {
		if (isScriptalyzerActive) push(random.nextInt(1+dk.getPropCount()-1));
		else push((float) candidateStack[matchingCandidate(Operator.Type.Prop)]);
	}
//**********************************************************************	 
	public void pushCandidateVerb() {
		if (isScriptalyzerActive) 
			push(random.nextInt(dk.getVerbCount()));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.Verb)]);
	}
//**********************************************************************	 
	public void pushCandidateEvent() {
		if (isScriptalyzerActive) 
			push(random.nextInt(engine.getHistoryBookSize()));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.Event)]);
	}
//**********************************************************************	 
	public void pushCandidateActorTrait() {
		if (isScriptalyzerActive) 
			push(random.nextInt(dk.getActorTraits().size()));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.ActorTrait)]);
	}
//**********************************************************************	 
	public void pushCandidatePropTrait() {
		if (isScriptalyzerActive) 
			push(random.nextInt(dk.getPropTraits().size()));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.PropTrait)]);
	}
//**********************************************************************	 
	public void pushCandidateStageTrait() {
		if (isScriptalyzerActive) 
			push(random.nextInt(dk.getStageTraits().size()));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.StageTrait)]);
	}
//**********************************************************************	 
	public void pushCandidateMoodTrait() {
		if (isScriptalyzerActive) 
			push(random.nextInt(Actor.MoodTrait.values().length));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.MoodTrait)]);
	}
//**********************************************************************	 
	public void pushCandidateQuantifier() {
		if (isScriptalyzerActive) 
			push(random.nextInt(dk.quantifiers.size()));
		else 
		push((float) candidateStack[matchingCandidate(Operator.Type.Quantifier)]);
	}
//**********************************************************************	 
	public void pushQuantifierIsInterrogative() {
		int n = (int) pop();
		if ((n >= 0) & (n < dk.quantifiers.size())) {
			if (dk.quantifiers.get(n).getID() < 0)
				push(1.0f);
			else
				push(0.0f);
		} else {
			setPoison("QuantifierIsInterrogative: quantifier index out of range");
			push(0);
		}
	}
//**********************************************************************	 
	public void pushCorrespondingPActorTrait() {
	// assumes an InnerTrait input
		int iInnerTrait = (int) pop();
		int iTowardWhom = (int) pop();
		int iOfWhom = (int) pop();
		if ((iInnerTrait >= 0) & (iInnerTrait < dk.getActorTraits().size())) {
			if ((iOfWhom >= 0) & (iOfWhom<dk.getActorCount())) {
				if ((iTowardWhom >= 0) & (iTowardWhom<dk.getActorCount())) {
					push(dk.getActor(iOfWhom).getP(dk.getActorTraits().get(iInnerTrait),dk.getActor(iTowardWhom)));
				} else { setPoison("CorrespondingP2: TowardWhom out of range"); push(0); }
			} else { setPoison("CorrespondingP2: OfWhom out of range"); push(0); }
		} else { 
			setPoison("P2 trait index ("+iInnerTrait+") out of range"); 
			push(0); 
			}
	}
//**********************************************************************	 
	public void pushCorrespondingCActorTrait() {
	// assumes an InnerTrait input
		int iInnerTrait = (int) pop();
		int iTowardWhom = (int) pop();
		int iOfWhom = (int) pop();
		if ((iInnerTrait >= 0) & (iInnerTrait < dk.getActorTraits().size())) {
			if ((iOfWhom >= 0) & (iOfWhom<dk.getActorCount())) {
				if ((iTowardWhom >= 0) & (iTowardWhom<dk.getActorCount())) {
					push(dk.getActor(iOfWhom).getC(dk.getActorTraits().get(iInnerTrait),dk.getActor(iTowardWhom)));
				} else { setPoison("CorrespondingC: TowardWhom out of range"); push(0); }
			} else { setPoison("CorrespondingC: OfWhom out of range"); push(0); }
		} else { setPoison("C trait index out of range"); push(0); }
	}
//**********************************************************************	 
	public void pushCorrespondingWeight() {
	// assumes an InnerTrait input
		int iInnerTrait = (int) pop();
		int iOfWhom = (int) pop();
		if ((iInnerTrait >= 0) & (iInnerTrait < dk.getActorTraits().size()))
			if ((iOfWhom >= 0) & (iOfWhom<dk.getActorCount()))
				push(dk.getActor(iOfWhom).getWeight(dk.getActorTraits().get(iInnerTrait)));
			else {
				setPoison("CorrespondingWeight: OfWhom out of range"); 
				push(0);
			}
		else {
			setPoison("Weight trait index ("+iInnerTrait+") out of range"); 
			push(0); 
		}
	}
	public void pushCorrespondingPWeight() {
		// assumes an InnerTrait input
			int iInnerTrait = (int) pop();
			if ((iInnerTrait >= 0) & (iInnerTrait < dk.getActorTraits().size()))
				pushPActorTraitWeight(dk.getActorTraits().get(iInnerTrait));
			else {
				setPoison("PWeight trait index ("+iInnerTrait+") out of range"); 
				push(0); 
			}
		}
//**********************************************************************	 
	public void pushCorrespondingActorTrait() {
	// assumes an OuterTrait input on the stack
		int iActorTrait = (int) pop();
		int iOfWhom = (int) pop();
		if (iActorTrait >= 0 && iActorTrait < dk.getActorTraits().size()) {
			if (iOfWhom >= 0 && iOfWhom<dk.getActorCount()) 
				push(dk.getActor(iOfWhom).get(dk.getActorTraits().get(iActorTrait)));					
			else { setPoison("CorrespondingActorTrait: OfWhom out of range"); push(0); }
		} else { setPoison("actor trait index out of range"); push(0); }
	}
//	**********************************************************************	 
	public void pushCorrespondingPropTrait() {
	// assumes an OuterTrait input on the stack
		int iPropTrait = (int) pop();
		int iOfWhichProp = (int) pop();
		if ((iPropTrait >= 0) & (iPropTrait < dk.getPropTraits().size())) {
			if ((iOfWhichProp >= 0) & (iOfWhichProp<dk.getPropCount())) {
				String traitLabel = dk.getPropTraits().get(iPropTrait).getLabel();
				for(FloatTrait t:dk.getPropTraits())
					if (traitLabel.equals(t.getLabel())){ 
						push(dk.getProp(iOfWhichProp).getTrait(t));
						return;
					}
				
				push((float) 0.5);				
			} else { setPoison("CorrespondingPropTrait: iOfWhichProp out of range"); push(0); }
		} else { setPoison("prop trait index out of range"); push(0); }
	}
//**********************************************************************	 
	public void pushCorrespondingStageTrait() {
	// assumes a StageTrait input on the stack
		int iStageTrait = (int) pop();
		int iOfWhichStage = (int) pop();
		if ((iStageTrait >= 0) & (iStageTrait < dk.getStageTraits().size())) {
			if ((iOfWhichStage >= 0) & (iOfWhichStage<dk.getStageCount())) {
				String traitLabel = dk.getStageTraits().get(iStageTrait).getLabel();
				if (traitLabel.equals("XCoord")) {
					push(dk.getStage(iOfWhichStage).getXCoord());
				} else if (traitLabel.equals("YCoord")) {
					push(dk.getStage(iOfWhichStage).getYCoord());
				} else {
					for(FloatTrait t:dk.getStageTraits()) 
						if (traitLabel.equals(t.getLabel())) {
							push(dk.getStage(iOfWhichStage).getTrait(t));
							return;
						}
				// else
					push((float) 0.5);
				}
			} else { setPoison("CorrespondingStageTrait: Of Which Stage out of range"); push(0); }
		} else { setPoison("stage trait index out of range"); push(0); }
	}
//**********************************************************************	 
	public void pushCorrespondingMoodTrait() {
	// assumes a MoodTrait input on the stack
		int iMood = (int) pop();
		int iOfWhichActor = (int) pop();
		if ((iMood >= 0) & (iMood < Actor.MoodTrait.values().length)) {
			if ((iOfWhichActor >= 0) & (iOfWhichActor<dk.getActorCount())) {
				Actor.MoodTrait t = Actor.MoodTrait.values()[iMood]; 
				try {
					push(dk.getActor(iOfWhichActor).get(t));
				} catch (Exception e) {
					setPoison("CorrespondingMoodTrait: bad traitLabel: "+t.name());
				}
			} else { setPoison("CorrespondingMoodTrait: Of Which Actor out of range"); push(0); }
		} else { setPoison("CorrespondingMoodTrait: mood trait index out of range"); push(0); }
	}
//**********************************************************************	 
	public void pushBNumber2Quantifier() {
		float bNumber = pop();
		int qValue = 0;
		qValue = (int)((bNumber * 5.0f) + 5.5f);
		if (qValue < 0)
			qValue = 0;
		if (qValue > 10)
			qValue = 10;
		push(qValue);
	}
//**********************************************************************	 
	public void pushBlend() {
		float bWeightingFactor = pop();
		if (bWeightingFactor <= -1.00f) {
			setPoison("Blend weighting factor <= -1");
			bWeightingFactor = -1.00f;
		}
		if (bWeightingFactor >= 1.00f) {
			setPoison("Blend weighting factor >= 1");
			bWeightingFactor = 1.00f;
		}
		// this is a conversion from BNumber to UNumber
		float uWeightingFactor = 1.0f-((1.0f-bWeightingFactor)/2.0f);
		float x2 = pop();
		float x1 = pop();
		push(x2*uWeightingFactor + x1*(1.0f-uWeightingFactor));
	}
//	**********************************************************************	 
	public void pushBlend3() {
		float bWeightingFactor3 = pop();
		float x3 = pop();
		float bWeightingFactor2 = pop();
		float x2 = pop();
		float bWeightingFactor1 = pop();
		float x1 = pop();

		// this is a conversion from BNumber to UNumber
		float uWeightingFactor3 = 1.0f-((1.0f-bWeightingFactor3)/2.0f);
		float uWeightingFactor2 = 1.0f-((1.0f-bWeightingFactor2)/2.0f);
		float uWeightingFactor1 = 1.0f-((1.0f-bWeightingFactor1)/2.0f);
		
		float sumWeights = uWeightingFactor1 + uWeightingFactor2 + uWeightingFactor3;
		uWeightingFactor1 /= sumWeights;
		uWeightingFactor2 /= sumWeights;
		uWeightingFactor3 /= sumWeights;
				
		push(x1*uWeightingFactor1 + x2*uWeightingFactor2 + x3*uWeightingFactor3);
	}
//	**********************************************************************	 
	public void pushBlend4() {
		float bWeightingFactor4 = pop();
		float x4 = pop();
		float bWeightingFactor3 = pop();
		float x3 = pop();
		float bWeightingFactor2 = pop();
		float x2 = pop();
		float bWeightingFactor1 = pop();
		float x1 = pop();

		// this is a conversion from BNumber to UNumber
		float uWeightingFactor4 = 1.0f-((1.0f-bWeightingFactor4)/2.0f);
		float uWeightingFactor3 = 1.0f-((1.0f-bWeightingFactor3)/2.0f);
		float uWeightingFactor2 = 1.0f-((1.0f-bWeightingFactor2)/2.0f);
		float uWeightingFactor1 = 1.0f-((1.0f-bWeightingFactor1)/2.0f);
		
		float sumWeights = uWeightingFactor1 + uWeightingFactor2 + uWeightingFactor3 + uWeightingFactor4;
		uWeightingFactor1 /= sumWeights;
		uWeightingFactor2 /= sumWeights;
		uWeightingFactor3 /= sumWeights;
		uWeightingFactor4 /= sumWeights;
				
		push(x1*uWeightingFactor1 + x2*uWeightingFactor2 + x3*uWeightingFactor3 + x4*uWeightingFactor4);
	}
//**********************************************************************	 
	public void pushBlendBothily() {
		float bWeightingFactor = pop();
		if (bWeightingFactor <= -1.00f) {
			setPoison("Blend weighting factor <= -1");
			pop();
			pop();
			push (-1);
		}
		else {
			if (bWeightingFactor >= 1.00f) {
				setPoison("Blend weighting factor >= 1");
				pop();
				pop();
				push (-1);
			}
			else {
			// this is a conversion from BNumber to UNumber
				double uWeightingFactor = 1.0f-((1.0f-bWeightingFactor)/2.0f);
				pushBNumber2UNumber(); // convert BNumbers to UNumbers for exponentiation
				float x2 = pop();
				pushBNumber2UNumber();
				float x1 = pop();
				double y1 = Math.pow((double)x1, uWeightingFactor);
				double y2 = Math.pow((double)x2, 1.0 - uWeightingFactor);
				push((float)y1*(float)y2);
				pushUNumber2BNumber();
			}
		}
	}
//**********************************************************************	
	float boundedTransform(float unboundedNumber) {
		if (unboundedNumber > 0.0f) {
			if (unboundedNumber>Utils.MAXI_NVALUE)
				unboundedNumber = Utils.MAXI_NVALUE;
			return 1.0f - (1.0f / (1.0f + unboundedNumber));
		}
		else
		{
			if (unboundedNumber<Utils.MINI_NVALUE)
				unboundedNumber = Utils.MINI_NVALUE;
			return (1.0f / (1.0f - unboundedNumber)) -1.0f;
		}
	}
//**********************************************************************	 
	float boundedInverseTransform(float boundedNumber) {
		if (boundedNumber > 0.0f) {
			if (boundedNumber>Utils.MAXI_VALUE)
				boundedNumber = Utils.MAXI_VALUE;
			return (1.0f / (1.0f - boundedNumber)) -1.0f;
		}
		else
		{
			if (boundedNumber<Utils.MINI_VALUE)
				boundedNumber = Utils.MINI_VALUE;
			return 1.0f - (1.0f / (1.0f + boundedNumber));
		}
	}
//**********************************************************************	 
	public void pushNumber2BNumber() {
		push(boundedTransform(pop()));
	}
//**********************************************************************	 
	public void pushBNumber2Number() {
		push(boundedInverseTransform(pop()));
	}
//**********************************************************************	 
	public void pushBSum() {
		float x2 = boundedInverseTransform(pop());
		float x1 = boundedInverseTransform(pop());
		push(boundedTransform(x1+x2));
	}
//	**********************************************************************	 
	public void pushBSum3() {
		float x3 = boundedInverseTransform(pop());
		float x2 = boundedInverseTransform(pop());
		float x1 = boundedInverseTransform(pop());
		push(boundedTransform(x1+x2+x3));
	}
//	**********************************************************************	 
	public void pushBSum4() {
		float x4 = boundedInverseTransform(pop());
		float x3 = boundedInverseTransform(pop());
		float x2 = boundedInverseTransform(pop());
		float x1 = boundedInverseTransform(pop());
		push(boundedTransform(x1+x2+x3+x4));
	}
//**********************************************************************	 
	public void pushBoundedDifference() {
		float x2 = boundedInverseTransform(pop());
		float x1 = boundedInverseTransform(pop());
		push(boundedTransform(x1-x2));
	}
//**********************************************************************	 
	public void pushBoundedProduct() {
		float x2 = boundedInverseTransform(pop());
		float x1 = boundedInverseTransform(pop());
		push(boundedTransform(x1*x2));
	}
//**********************************************************************	 
	public void pushBoundedQuotient() {
		float x2 = boundedInverseTransform(pop());
		float x1 = boundedInverseTransform(pop());
		if (x2==0.0f) {
			setPoison("divide by zero in pushBoundedQuotoient");
			push(0);
		}
		else 	
			push(boundedTransform(x1/x2));
	}
//**********************************************************************	 
	public void pushMixedQuotient() {
		float x2 = pop();
		float x1 = pop();
		if ((x2<1.0f) & (x2>0.0f))
			x2 = 1.0f;
		if ((x2>-1.0f) & (x2<0.0f))
			x2 = -1.0f;
		if (x2 != 0) {
			push(x1/x2);
		}
		else {
			push(0.0f); // keep the stack healthy
			setPoison("divide by zero in MixedQuotient");
		}
	}
//**********************************************************************	 
	public void pushBoundedAbsval() {
		float x1 = pop();
		if (x1 > 0.0f)
			push(x1);
		else
			push(-x1);
	}
//**********************************************************************	 
	public void pushBoundedArithmeticInversion() {
		float x1 = pop();
		push(-x1);
	}
//**********************************************************************	 
	public void pushBoundedBigger() {
		float x2 = pop();
		float x1 = pop();
		if (x1 > x2)
			push(x1);
		else
			push(x2);
	}
//**********************************************************************	 
	public void pushBoundedSmaller() {
		float x2 = pop();
		float x1 = pop();
		if (x1 < x2)
			push(x1);
		else
			push(x2);
	}
//**********************************************************************	 
	public void pushQuantifier2BNumber() {
		int n = (int)pop();
		if ((n>=0) & (n<dk.quantifiers.size()-1)) { // the subtraction is to obviate the interrogative quantifier
			float x = ((float)dk.quantifiers.get(n).getID()-5.0f)/5.1f;
			if (x<Utils.MINI_VALUE)
				x= Utils.MINI_VALUE;
			if (x>Utils.MAXI_VALUE)
				x=Utils.MAXI_VALUE;
			push(x);
		}
		else push(0.0f);
	}
//**********************************************************************	 
	public void pushAdverb2UNumber() {
		int n = (int)pop();
		// the 10.1 is to keep it inside the UNumber range
		if ((n >= 0) & (n < dk.quantifiers.size()-1)) { // the subtraction is to obviate the interrogative quantifier
			float x = (float)dk.quantifiers.get(n).getID()/10.2f;
			if (x>=1.0f)
				x=Utils.MAXI_VALUE;
			push(x);
		}
		else push(0.0f);
	}
	//**********************************************************************	 
	public void pushAdverb2Scaler() {
		// The difference between this method and the above method
		// is that the above returns a BNumber while this returns
		// a Number between 0.0 and 1.0. It's just a type changing difference.
		int n = (int)pop();
		// the 10.1 is to keep it inside the UNumber range
		if ((n >= 0) & (n < dk.quantifiers.size()-1)) { // the subtraction is to obviate the interrogative quantifier
			float x = (float)dk.quantifiers.get(n).getID()/10.2f;
			if (x>=1.0f)
				x=Utils.MAXI_VALUE;
			push(x);
		}
		else push(0.0f);
	}
//**********************************************************************	 
	public void popAssumeRoleIf() {
		mBool = pop()!=0;
	}
//**********************************************************************	 
	public void popOptionAcceptable() {
		mBool = pop()!=0;
	}
//**********************************************************************	 
	public void popAcceptable() {
		myAcceptable = popObject();
	}
//**********************************************************************	 
	public void popDesirable() {
		mFloat = pop();
	}
	
	public void popWordsocketText() {
		if (poison)
			myText = "??? (poisoned)";
		else
			myText = popString();
	}
//**********************************************************************	 
	public void pushbooleanTrue() {
		push(1.0f);
	}
//**********************************************************************	 
	public void pushbooleanFalse() {
		push(0.0f);
	}
//**********************************************************************	 
	public void pushGreatestCValue() {
		Actor towardsWhom = dk.getActor((int)pop());
		int ofWhom = (int)pop();
		float highestC = -0.99f;
		Actor zActor = dk.getActor(ofWhom);
		for(FloatTrait t:dk.getActorTraits())
			if (zActor.getC(t,towardsWhom)>highestC)
				highestC = zActor.getC(t,towardsWhom);
		push(highestC);
	}
//**********************************************************************	 
	public void pushLeastCValue() {
		Actor towardsWhom = dk.getActor((int)pop());
		int ofWhom = (int)pop();
		float lowestC = +0.99f;
		Actor zActor = dk.getActor(ofWhom);
		for(FloatTrait t:dk.getActorTraits())
			if (zActor.getC(t,towardsWhom)<lowestC)
				lowestC = zActor.getC(t,towardsWhom);
		push(lowestC);
	}
//**********************************************************************	 
	public void pushBNumber2UNumber() {
		float x = pop();
		push(1.0f-((1.0f-x)/2.0f));
	}
//**********************************************************************	 
	public void pushUNumber2BNumber() {
		float x = pop();
		if (x>=0.0f)
			push(1.0f-1.999f*(1.0f-x));
		else
			push(0.0f);
	}
//**********************************************************************	 
	public void pushBoolean2Number() {
		// this method doesn't need to do anything; it's simply a data conversion method
	}
//**********************************************************************	
	private float randomBNumber() {
		float x = (2.0f*random.nextFloat())-1.0f;
		if (x > Utils.MAXI_VALUE)
			x = Utils.MAXI_VALUE;
		if (x < Utils.MINI_VALUE)
			x = Utils.MINI_VALUE;
		return x;
	}
//**********************************************************************	
	public void pushRandom() {
		float x = (2.0f*scriptRandom.nextFloat())-1.0f;
		if (x > Utils.MAXI_VALUE)
			x = Utils.MAXI_VALUE;
		else if (x < Utils.MINI_VALUE)
			x = Utils.MINI_VALUE;
		push(x);
	}
//**********************************************************************	
	private float myNextBoolean() {
		int x = random.nextInt(1000);
		return (x%1);
	}
/*
	//**********************************************************************	
	public void pushAnticipatedValue() {
		int hypotheticalVerb = (int)pop();
		int hypotheticalSubject = (int)pop();
		anticipatee = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(Sentence.Subject, hypotheticalSubject, Operator.Type.Actor);
		engine.getHypotheticalEvent().setWordSocket(Sentence.Verb, hypotheticalVerb, Operator.Type.Verb);
		for (int i=0; (i < 7); ++i) { // there are seven slots for WordSockets
			
		}
		// This means that only the FIRST SetAnticipatedValue script is executed.
		// There should be only one, but we haven't enforced that yet.
		int i=0;
		boolean gotcha=false;
		while ((i < dk.getVerb(hypotheticalVerb).getConsequenceCount()) & !gotcha) {
			Script script = dk.getVerb(hypotheticalVerb).getConsequence(i);
			if (script.getLabel().equals("SetAnticipatedValue")) {
				gotcha=true;
				int saveStackTop = stackTop;
				float x = executeScript(script); // execute the SetAnticipatedValue script
				stackTop = saveStackTop;
				push(x); // push its result onto the stack
			}
		}
	}
//**********************************************************************
	public void pushSetAnticipatedValue() {
		// This doesn't do anything other than to:
		pop();
		// clear the stack.
	}
//**********************************************************************
	public void pushAnticipatee() {
		push(anticipatee);
	}
//**********************************************************************
	public void pushNoSocket() {
		// this doesn't do anything at all.
	}
//**********************************************************************
	public void pushSetActorSocket() {
		int hypotheticalActor = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalActor, Operator.Type.Actor);
	}
//**********************************************************************
	public void pushSetPropSocket() {
		int hypotheticalProp = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalProp, Operator.Type.Prop);
	}
//**********************************************************************
	public void pushSetStageSocket() {
		int hypotheticalStage = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalStage, Operator.Type.Stage);
	}
//**********************************************************************
	public void pushSetVerbSocket() {
		int hypotheticalVerb = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalVerb, Operator.Type.Verb);
	}
//**********************************************************************
	public void pushSetQuantifierSocket() {
		int hypotheticalQuantifier = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalQuantifier, Operator.Type.Quantifier);
	}
//**********************************************************************
	public void pushSetActorTraitWordSocket() {
		int hypotheticalActorTrait = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalActorTrait, Operator.Type.ActorTrait);
	}
//**********************************************************************
	public void pushSetPropTraitWordSocket() {
		int hypotheticalPropTrait = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalPropTrait, Operator.Type.PropTrait);
	}
//**********************************************************************
	public void pushSetStageTraitWordSocket() {
		int hypotheticalStageTrait = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalStageTrait, Operator.Type.StageTrait);
	}
//**********************************************************************
	public void pushSetMoodTraitWordSocket() {
		int hypotheticalMoodTrait = (int)pop();
		int wordSocket = (int)pop();
		engine.getHypotheticalEvent().setWordSocket(wordSocket, hypotheticalMoodTrait, Operator.Type.MoodTrait);
	}
//**********************************************************************	 
	public void pushHypotheticalSubject() {
		push((float) (engine.getHypotheticalEventIWord(Sentence.Subject)));
		if (isScriptalyzerActive) { pop(); push(1+random.nextInt(dk.getActorCount()-1)); }
	}
//**********************************************************************	 
	public void pushHypotheticalVerb() {
		push((float) (engine.getHypotheticalEventIWord(Sentence.Verb)));
	}
//**********************************************************************	 
	public void pushHypotheticalDirObject() {
		push((float) (engine.getHypotheticalEventIWord(Sentence.DirObject)));
		if (isScriptalyzerActive) { pop(); push(1+random.nextInt(dk.getActorCount()-1)); }
	}
//**********************************************************************
	public void pushDirObject() {
		push(Sentence.DirObject);
	}
	*/
//**********************************************************************	 
	public void pushPermitFateToReact() {
		// Actually, the purpose of this operator is to set the ForceFateToReact flag
		// The operator itself returns a value of true.
		push(1.0f);
		engine.setPermitFateToReact();
	}
	//**********************************************************************	 
	public void pushFatesRole() {
		// Actually, the purpose of this operator is to set the ForceFateToReact flag
		// The operator itself returns a value of true.
		if (engine.getReactingActor()==Engine.FATE)
			push(1.0f);
		else push(0.0f);
		engine.setFatesRole();
	}
//**********************************************************************	 
	public void pushMainClauseIs() {
		// returns true if main clause of CandidateEvent contains these three elements
		int iDirObject = (int)pop();
		int iVerb = (int)pop();
		int iSubject = (int)pop();
		int iPageNumber = candidateStack[matchingCandidate(Operator.Type.Event)];
		if ((iPageNumber >= 0) & (iPageNumber < engine.getHistoryBookSize())) {
			Sentence candidateEvent = engine.getHistoryBookPage(iPageNumber);
			boolean subjectMatch = candidateEvent.getIWord(Sentence.Subject) == iSubject;
			boolean verbMatch = candidateEvent.getIWord(Sentence.Verb) == iVerb;
			boolean dirObjectMatch = candidateEvent.getIWord(Sentence.DefDirObject) == iDirObject;
			if (subjectMatch & verbMatch & dirObjectMatch)
				push(1.0f);
			else
				push(0.0f);
		} else {
			setPoison("MainClauseIs: candidateEvent pageNumber out of range");
			push(0);
		}
		
		if (isScriptalyzerActive) { pop(); push(0.0f); }
	}
//**********************************************************************	 
	public void pushNullify() {
		float objectBNumber = pop();
		int nullificationCondition = (int) pop();
		if (nullificationCondition == 1)
			push(-0.99f);
		else
			push(objectBNumber);
	}
//**********************************************************************	 
	public void pushVerbActorBox() {
			push(verbActorBox);
	}
//**********************************************************************	 
	public void popVerbActorBox() {
			verbActorBox = pop();
	}
//	**********************************************************************	 
	public void pushVerbPropBox() {
			push(verbPropBox);
	}
//**********************************************************************	 
	public void popVerbPropBox() {
			verbPropBox = pop();
	}
//	**********************************************************************	 
	public void pushVerbStageBox() {
			push(verbStageBox);
	}
//**********************************************************************	 
	public void popVerbStageBox() {
			verbStageBox = pop();
	}
//**********************************************************************	 
	public void pushVerbVerbBox() {
			push(verbVerbBox);
	}
//**********************************************************************	 
	public void popVerbVerbBox() {
			verbVerbBox = pop();
	}
//**********************************************************************	 
	public void pushVerbEventBox() {
			push(verbEventBox);
	}
//**********************************************************************	 
	public void popVerbEventBox() {
			verbEventBox = pop();
	}
//**********************************************************************	 
	public void pushVerbBNumberBox() {
			push(verbBNumberBox);
	}
//**********************************************************************	 
	public void popVerbBNumberBox() {
			verbBNumberBox = pop();
	}
//	**********************************************************************	 
	public void pushRoleActorBox() {
			push(roleActorBox);
	}
//**********************************************************************	 
	public void popRoleActorBox() {
			roleActorBox = pop();
	}
//	**********************************************************************	 
	public void pushRolePropBox() {
			push(rolePropBox);
	}
//**********************************************************************	 
	public void popRolePropBox() {
			rolePropBox = pop();
	}
//	**********************************************************************	 
	public void pushRoleStageBox() {
			push(roleStageBox);
	}
//**********************************************************************	 
	public void popRoleStageBox() {
			roleStageBox = pop();
	}
//**********************************************************************	 
	public void pushRoleVerbBox() {
			push(roleVerbBox);
	}
//**********************************************************************	 
	public void popRoleVerbBox() {
			roleVerbBox = pop();
	}
//**********************************************************************	 
	public void pushRoleEventBox() {
			push(roleEventBox);
	}
//**********************************************************************	 
	public void popRoleEventBox() {
			roleEventBox = pop();
	}
//**********************************************************************	 
	public void pushRoleBNumberBox() {
		push(roleBNumberBox);
	}
//**********************************************************************	 
	public void popRoleBNumberBox() {
		roleBNumberBox = pop();
	}
//**********************************************************************	 
	public void pushGlobalActorBox() {
		if (globalActorBox == UNDEFINED_BOX_VALUE)
			setPoison("uninitialized GlobalActorBox!");
		push(globalActorBox);
	}
//**********************************************************************	 
	public void popGlobalActorBox() {
		globalActorBox = pop();
	}
//	**********************************************************************	 
	public void pushGlobalPropBox() {
		if (globalPropBox == UNDEFINED_BOX_VALUE)
			setPoison("uninitialized GlobalPropBox!");
		push(globalPropBox);
	}
//**********************************************************************	 
	public void popGlobalPropBox() {
		globalPropBox = pop();
	}
//	**********************************************************************	 
	public void pushGlobalStageBox() {
		if (globalStageBox == UNDEFINED_BOX_VALUE)
			setPoison("uninitialized GlobalStageBox!");
		push(globalStageBox);
	}
//**********************************************************************	 
	public void popGlobalStageBox() {
		globalStageBox = pop();
	}
//**********************************************************************	 
	public void pushGlobalVerbBox() {
		if (globalVerbBox == UNDEFINED_BOX_VALUE)
			setPoison("uninitialized GlobalVerbBox!");
		push(globalVerbBox);
	}
//**********************************************************************	 
	public void popGlobalVerbBox() {
		globalVerbBox = pop();
	}
//**********************************************************************	 
	public void pushGlobalEventBox() {
		if (globalEventBox == UNDEFINED_BOX_VALUE)
			setPoison("uninitialized GlobalEventBox!");
		push(globalEventBox);
	}
//**********************************************************************	 
	public void popGlobalEventBox() {
		globalEventBox = pop();
	}
//**********************************************************************	 
	public void pushGlobalBNumberBox() {
		if (globalBNumberBox == UNDEFINED_BOX_VALUE)
			setPoison("uninitialized GlobalBNumberBox!");
		push(globalBNumberBox);
	}
//**********************************************************************	 
	public void popGlobalBNumberBox() {
		globalBNumberBox = pop();
	}
	//**********************************************************************	 
	public void pushScore() {
		if (score == UNDEFINED_BOX_VALUE)
			setPoison("uninitialized Score!");
		push(score);
	}
//**********************************************************************	 
	public void popScore() {
		score = pop();
	}
//**********************************************************************	 
	public void pushIHaventDoneThisBefore() {
		int endWordSocket = theScript.getIWordSocket();			
		boolean iHaveDoneThis=false;
		int iHistory=engine.getHistoryBookSize()-1;
		while ((iHistory>=0) && !iHaveDoneThis) {
			Sentence event = engine.getHistoryBookPage(iHistory);
			int iWordSocket=0;
			boolean match=true;
			while ((iWordSocket<endWordSocket) & match) {
				match = (event.getIWord(iWordSocket) == engine.getChosenPlanIWord(iWordSocket));
				++iWordSocket;
			}
			if (match) {
				Verb zVerb = dk.getVerb(event.getIWord(Sentence.Verb));
				if (endWordSocket < Sentence.MaxWordSockets) {
					Operator.Type scriptDataType = zVerb.getWordSocketType(endWordSocket);
					match = (event.getIWord(endWordSocket) == candidateStack[matchingCandidate(scriptDataType)]);
				}
			}
			iHaveDoneThis = match;
			--iHistory;
		}			
		if (iHaveDoneThis)
			push(0.0f);
		else
			push(1.0f);
	}
//**********************************************************************	 
	public void pushIHaventDoneThisSince() {
		int timeHorizon = (int)pop();
		int endWordSocket = theScript.getIWordSocket();	
		boolean iHaveDoneThis=false;
		int iHistory=engine.getHistoryBookSize()-1;
		if (iHistory>0) {
			Sentence event = engine.getHistoryBookPage(iHistory);
			while ((iHistory>0) & (event.getTime()>engine.getCMoments()-timeHorizon) & !iHaveDoneThis) {
				event = engine.getHistoryBookPage(iHistory);
				int iWordSocket=0;
				boolean match=true;
				while ((iWordSocket<endWordSocket) && match) {
					match = (event.getIWord(iWordSocket) == engine.getChosenPlanIWord(iWordSocket));
					++iWordSocket;
				}
				if (match) {
					Verb zVerb = dk.getVerb(event.getIWord(Sentence.Verb));
					if ((endWordSocket < Sentence.MaxWordSockets)) {
						Operator.Type scriptDataType = zVerb.getWordSocketType(endWordSocket);
						match &= (event.getIWord(endWordSocket) == candidateStack[matchingCandidate(scriptDataType)]);
					}
				}
				iHaveDoneThis = match;
				--iHistory;
			}
		}
		if (iHaveDoneThis)
 			push(0.0f);
		else
			push(1.0f);
	}
//**********************************************************************
	public void pushSwitcheroo() {
		float falseValue = pop();
		float trueValue = pop();
		float switcher = pop();
		if (switcher > 0.0f)
			push(trueValue);
		else
			push(falseValue);
	}
//**********************************************************************
	public void popAbortIf() {
		if (pop()>0.0f)
			engine.setAbortIf(true);
		else 
			engine.setAbortIf(false);
	}
//**********************************************************************
	public void pushSuitability() {
		float x = pop(); // the BNumber being tested
		pushQuantifier2BNumber();
		float y = pop(); // the CandidateQuantifier BNumbered
		if (x>y)
			push(1.0f - (x-y));
		else
			push(1.0f - (y-x));
	}
	//**********************************************************************
	public void pushScaleDown() {
		pushAdverb2Scaler(); // convert the adverb to a scaler
		float x = pop();
		pushBNumber2UNumber();
		pushBNumber2Number();
		float y = pop();
		push(x*y);
		pushNumber2BNumber();
		pushUNumber2BNumber();
	}
//**********************************************************************
	public void pushMaxi() {
		push(Utils.MAXI_VALUE);
	}
//**********************************************************************
	public void pushMini() {
		push(Utils.MINI_VALUE);
	}
//**********************************************************************
	public void pushActorSum() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushActorAverage() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushActorTally() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushPropSum() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushPropAverage() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushPropTally() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushStageSum() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushStageAverage() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
	//**********************************************************************
	public void pushStageTally() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
	//**********************************************************************
	public void pushEventSum() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************
	public void pushEventAverage() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
	//**********************************************************************
	public void pushEventTally() {
		// placeholder; this Operator is handled directly in ExecuteNode
	}
//**********************************************************************	
	private void actorLoop(Operator tOperator, Node zNode) {
		// this is either GroupSum or GroupAverage; it requires a loop
		boolean saveAccumulateChunks = accumulateChunks;
		accumulateChunks = false;
		++candidateStackTop;
		if (candidateStackTop > 7)
			setPoison("Interpreter:actorLoop:candidateStackTop > 7");
		float loopSum = 0.0f;
		int loopCount = 0;
		String zLabel = tOperator.getLabel();
		candidateTypeStack[candidateStackTop] = Operator.Type.Actor;
		for (int i = 1; (i < dk.getActorCount()); ++i) {
			if (dk.getActor(i).getActive()) {
				candidateStack[candidateStackTop] = i;
				executeNode((Node)zNode.getChildAt(0)); // the Acceptable operator
				if (pop()>0.0f && !poison) { // if actor meets acceptable criterion
					++loopCount;
					if (!zLabel.endsWith("Tally")) {
						push(loopSum);
						executeNode((Node)zNode.getChildAt(1)); // the Actor operator
						pushBSum();
						loopSum = pop();
					}
				}
			}
		}	
		if (zLabel.endsWith("Sum"))
			push(loopSum);
		else if (zLabel.endsWith("Tally"))
			push(loopCount);
		else if (zLabel.endsWith("Average"))
			push(loopSum/loopCount);
		else
			setPoison("Interpreter:actorLoop:bad operator label ending: "+zLabel);
		--candidateStackTop;
		accumulateChunks = saveAccumulateChunks;
	}
	//**********************************************************************	
	private void propLoop(Operator tOperator, Node zNode) {
		// this is either GroupSum or GroupAverage; it requires a loop
		boolean saveAccumulateChunks = accumulateChunks;
		accumulateChunks = false;
		++candidateStackTop;
		if (candidateStackTop > 7)
			setPoison("Interpreter:propLoop:candidateStackTop > 7");
		float loopSum = 0.0f;
		int loopCount = 0;
		String zLabel = tOperator.getLabel();
		candidateTypeStack[candidateStackTop] = Operator.Type.Prop;
		for (int i = 1; (i < dk.getPropCount()); ++i) {
			if (dk.getProp(i).getInPlay()) {
				candidateStack[candidateStackTop] = i;
				executeNode((Node)zNode.getChildAt(0)); // the Acceptable operator
				if (pop()>0.0f && !poison) { // if prop meets acceptable criterion
					++loopCount;
					if (!zLabel.endsWith("Tally")) {
						push(loopSum);
						executeNode((Node)zNode.getChildAt(1)); // the Prop operator
						pushBSum();
						loopSum = pop();
					}
				}
			}
		}	
		if (zLabel.endsWith("Sum"))
			push(loopSum);
		else if (zLabel.endsWith("Tally"))
			push(loopCount);
		else if (zLabel.endsWith("Average"))
			push(loopSum/loopCount);
		else
			setPoison("Interpreter:propLoop:bad operator label ending: "+zLabel);
		--candidateStackTop;
		accumulateChunks = saveAccumulateChunks;
	}
//**********************************************************************	
	private void stageLoop(Operator tOperator, Node zNode) {
		// this is either GroupSum or GroupAverage; it requires a loop
		boolean saveAccumulateChunks = accumulateChunks;
		accumulateChunks = false;
		++candidateStackTop;
		if (candidateStackTop > 7)
			setPoison("Interpreter:stageLoop:candidateStackTop > 7");
		float loopSum = 0.0f;
		int loopCount = 0;
		candidateTypeStack[candidateStackTop] = Operator.Type.Stage;
		String zLabel = tOperator.getLabel();
		for (int i = 1; (i < dk.getStageCount()); ++i) {
			if (dk.getStage(i).getDoorOpen()) {
				candidateStack[candidateStackTop] = i;
				executeNode((Node)zNode.getChildAt(0)); // the Acceptable operator
				if (pop()>0.0f && !poison) { // if stage meets acceptable criterion
					++loopCount;
					if (!zLabel.endsWith("Tally")) {
						push(loopSum);
						executeNode((Node)zNode.getChildAt(1)); // the Stage operator
						pushBSum();
						loopSum = pop();
					}
				}
			}
		}	
		if (zLabel.endsWith("Sum"))
			push(loopSum);
		else if (zLabel.endsWith("Tally"))
			push(loopCount);
		else if (zLabel.endsWith("Average"))
			push(loopSum/loopCount);
		else
			setPoison("Interpreter:stageLoop:bad operator label ending: "+zLabel);
		--candidateStackTop;
		accumulateChunks = saveAccumulateChunks;
	}
//**********************************************************************	
	private void eventLoop(Operator tOperator, Node zNode) {
		// this is either GroupSum or GroupAverage; it requires a loop
		boolean saveAccumulateChunks = accumulateChunks;
		accumulateChunks = false;
		++candidateStackTop;
		if (candidateStackTop > 7)
			setPoison("Interpreter:stageLoop:candidateStackTop > 7");
		float loopSum = 0.0f;
		int loopCount = 0;
		candidateTypeStack[candidateStackTop] = Operator.Type.Event;
		String zLabel = tOperator.getLabel();
		for (int i = 1; (i < engine.getHistoryBookSize()); ++i) {
			candidateStack[candidateStackTop] = i;
			executeNode((Node)zNode.getChildAt(0)); // the Acceptable operator
			if (pop()>0.0f && !poison) { // if event meets acceptable criterion
				++loopCount;
				if (!zLabel.endsWith("Tally")) {
					push(loopSum);
					executeNode((Node)zNode.getChildAt(1)); // the expression operator
					pushBSum();
					loopSum = pop();
				}
			}
		}	
		if (zLabel.endsWith("Sum"))
			push(loopSum);
		else if (zLabel.endsWith("Tally"))
			push(loopCount);
		else if (zLabel.endsWith("Average"))
			push(loopSum/loopCount);
		else
			setPoison("Interpreter:eventLoop:bad operator label ending: "+zLabel);
		--candidateStackTop;
		accumulateChunks = saveAccumulateChunks;
	}
//**********************************************************************
	public void pushSameAsThisOne() {
		int iSocket = theScript.getIWordSocket();
		int iWord = engine.getThisEvent().getWordSocket(iSocket).getIWord();
		switch(engine.getThisEvent().getWordSocket(iSocket).getType()){
		case Actor:
			if (!dk.getActor(iWord).getActive())
				setPoison("Actor "+dk.getActor(iWord).getLabel()+" is not active.");
			break;
		case Prop:
			if (!dk.getProp(iWord).getInPlay())
				setPoison("Prop "+dk.getProp(iWord).getLabel()+" is not in play.");
			break;
		case Stage:
			if (!dk.getStage(iWord).getDoorOpen())
				setPoison("Stage "+dk.getProp(iWord).getLabel()+" has the door closed.");
			break;
		};
		if (iWord == candidateStack[candidateStackTop])
			push(1.0f);
		else push(0.0f);
	}
//**********************************************************************
	public void pushIsAMemberOf9Set() {
		// Assumes that ThisQuantifier is on the top of the Stack
		int iQuantifier = (int)pop();
		boolean result = false;
		switch (iQuantifier) {
			case 0: { result = true; break; }
			case 1: { result = true; break; }
			case 2: { result = false; break; }
			case 3: { result = true; break; }
			case 4: { result = true; break; }
			case 5: { result = true; break; }
			case 6: { result = true; break; }
			case 7: { result = true; break; }
			case 8: { result = false; break; }
			case 9: { result = true; break; }
			case 10: { result = true; break; }
		}
		if (result) push(1.0f); else push(0.0f);
	}
//**********************************************************************
	public void pushIsAMemberOf7Set() {
		// Assumes that ThisQuantifier is on the top of the Stack
		int iQuantifier = (int)pop();
		boolean result = false;
		switch (iQuantifier) {
			case 0: { result = true; break; }
			case 1: { result = false; break; }
			case 2: { result = true; break; }
			case 3: { result = true; break; }
			case 4: { result = false; break; }
			case 5: { result = true; break; }
			case 6: { result = false; break; }
			case 7: { result = true; break; }
			case 8: { result = true; break; }
			case 9: { result = false; break; }
			case 10: { result = true; break; }
		}
		if (result) push(1.0f); else push(0.0f);
	}
//**********************************************************************
	public void pushIsAMemberOf5Set() {
		// Assumes that ThisQuantifier is on the top of the Stack
		int iQuantifier = (int)pop();
		boolean result = false;
		switch (iQuantifier) {
			case 0: { result = false; break; }
			case 1: { result = true; break; }
			case 2: { result = false; break; }
			case 3: { result = true; break; }
			case 4: { result = false; break; }
			case 5: { result = true; break; }
			case 6: { result = false; break; }
			case 7: { result = true; break; }
			case 8: { result = false; break; }
			case 9: { result = true; break; }
			case 10: { result = false; break; }
		}
		if (result) push(1.0f); else push(0.0f);
	}
//**********************************************************************
	public void pushIsAMemberOf3Set() {
		// Assumes that ThisQuantifier is on the top of the Stack
		int iQuantifier = (int)pop();
		boolean result = false;
		switch (iQuantifier) {
			case 0: { result = false; break; }
			case 1: { result = true; break; }
			case 2: { result = false; break; }
			case 3: { result = false; break; }
			case 4: { result = false; break; }
			case 5: { result = true; break; }
			case 6: { result = false; break; }
			case 7: { result = false; break; }
			case 8: { result = false; break; }
			case 9: { result = true; break; }
			case 10: { result = false; break; }
		}
		if (result) push(1.0f); else push(0.0f);
	}
//**********************************************************************	
	public void startEpilogue() {
		pop();
		engine.getThisEvent().setIsPartOfEpilogue();
	}
//**********************************************************************	 
}
