package json;
import org.json.JSONArray;
import org.json.JSONObject;

import Restlet.OperatorTypeValues;

import com.storytron.enginecommon.StorytellerReturnData;
import com.storytron.enginecommon.MenuElement;
import com.storytron.uber.Sentence;
import com.storytron.uber.operator.Operator;

public class StorytellerReturnDataJson {

	StorytellerReturnData STReturnData;
	public StorytellerReturnDataJson(StorytellerReturnData srd) {
		this.STReturnData = srd;
	}
	/*
	 * JSON Labels
	 * A = returnType
	 * B = poisoned
	 * C = hijacked
	 * D = whoKnows
	 * E = time
	 * F = location
	 * G = causalEvent
	 * H = pageNumber
	 * I = iWord
	 * J = type
	 * K = WSArray
	 * L = inclination
	 * M = belief
	 * N = outcomes
	 * O = isPartOfEpilogue
	 * P = seed
	 * Q = rawSentence
	 * R = labels
	 * S = descriptions
	 * T = expressionLabel
	 * U = stageLabel
	 * V = actorsPresent
	 * W = propsPresent
	 * X = suffixes
	 * Y = visible
	 * Z = tLabeledSentence
	 * A1 = tTime
	 * B1 = showBottom
	 * C1 = menuElements
	 * D1 = wordSocket
	 * 
	 */
	public JSONObject toJSON() {
		try {
			JSONObject jsonobj = new JSONObject();
			//jsonobj.put("sessionID", this.sessionInfo.get(0));
			// Add the storyteller return type
			// Using integers because the Kindle client runs Java 1.4, which does not 
			// support Enums
			int returnType;
			switch (this.STReturnData.returnType) {
			case SEND_TRIGGER_SENTENCE:	returnType=1; break;
			case GET_PLAYER_SELECTION: returnType=2; break;
			case GET_PLAYER_DONE: returnType=3; break;
			case THE_END: returnType=4; break;
			default: returnType=-1;
			}
			jsonobj.put("A", returnType);
			
			// tLabeledSentence is complex
			
			
			JSONObject jsLabeledSentence = new JSONObject();
			
			// rawSentence
			JSONObject jsRawSentence = new JSONObject();
			/*
			 * 		public static final int MaxWordSockets = 15;
		public static final int Subject = 0;
		public static final int Verb = 1;
		public static final int DefDirObject = 2;
		boolean poisoned; // poison flag for this Sentence as a plan
		boolean hijacked;
		ArrayList<Boolean> whoKnows = new ArrayList<Boolean>(); // who knows about this event?
		int time;
		int location; // index of Stage on which this Sentence took place
		int causalEvent;
		int pageNumber;
		WordSocket[] wordSocket;
		float inclination; // inclination for this Sentence as a plan
		ArrayList<Float> belief = new ArrayList<Float>(); // degree belief (0.0 - 1.0) in
		ArrayList<Integer> outcomes = new ArrayList<Integer>(); // usually just one
		boolean isPartOfEpilogue;
		// seed used for calculating labels of this event.
		public long seed = 0;
			 */
			Sentence rawSentence = this.STReturnData.tLabeledSentence.rawSentence;
//			jsRawSentence.put("MaxWordSockets", rawSentence.MaxWordSockets);
//			jsRawSentence.put("Subject", rawSentence.Subject);
//			jsRawSentence.put("Verb", rawSentence.Verb);
//			jsRawSentence.put("DefDirObject", rawSentence.DefDirObject);
			if (rawSentence.getPoisoned())
				jsRawSentence.put("B", 1);
			else
				jsRawSentence.put("B", 0);	
			//jsRawSentence.put("B", String.valueOf(rawSentence.getPoisoned()));
			
			if (rawSentence.getHijacked())
				jsRawSentence.put("C", 1);
			else
				jsRawSentence.put("C", 0);
			//jsRawSentence.put("C", String.valueOf(rawSentence.getHijacked()));
			// whoKnows
			//JSONArray jsWhoKnows = new JSONArray();
			// Using a single string to store the whoknows array, for now
			String whoKnows = "";
			for (int i=0;i<rawSentence.getWhoKnowsCount();i++) {
				if (rawSentence.getWhoKnows(i))
					whoKnows = whoKnows + "1";
				else
					whoKnows = whoKnows + "0";
			}
			
			//jsWhoKnows.put(String.valueOf(rawSentence.getWhoKnows(i)));
			//jsRawSentence.put("D", jsWhoKnows);
			jsRawSentence.put("D", whoKnows);
				
			jsRawSentence.put("E", rawSentence.getTime());
			jsRawSentence.put("F", rawSentence.getLocation());
			jsRawSentence.put("G", rawSentence.getCausalEvent());
			jsRawSentence.put("H", rawSentence.getPageNumber());
			JSONArray jsWordSocketArray = new JSONArray();
			for (int i = 0; i<Sentence.MaxWordSockets;i++) {
				JSONObject jsWordSocket = new JSONObject();
				jsWordSocket.put("I", rawSentence.getWordSocket(i).getIWord());
				Operator.Type wsOperatorType = rawSentence.getWordSocket(i).getType();
				int numOperatorType=0;
				switch(wsOperatorType) {
					case UnType: numOperatorType = OperatorTypeValues.UnType; break;
					case Actor: numOperatorType = OperatorTypeValues.Actor; break;
					case Prop: numOperatorType = OperatorTypeValues.Prop; break; 
					case Stage: numOperatorType = OperatorTypeValues.Stage; break;
					case Verb: numOperatorType = OperatorTypeValues.Verb; break;
					case ActorTrait: numOperatorType = OperatorTypeValues.ActorTrait; break;
					case PropTrait: numOperatorType = OperatorTypeValues.PropTrait; break;
					case StageTrait: numOperatorType = OperatorTypeValues.StageTrait; break;
					case MoodTrait: numOperatorType = OperatorTypeValues.MoodTrait; break;
					case Quantifier: numOperatorType = OperatorTypeValues.Quantifier; break;
					case Event: numOperatorType = OperatorTypeValues.Event; break;
					case Number: numOperatorType = OperatorTypeValues.Number; break;
					case Boolean: numOperatorType = OperatorTypeValues.Boolean; break;
					case Procedure: numOperatorType = OperatorTypeValues.Procedure; break;
					case BNumber: numOperatorType = OperatorTypeValues.BNumber; break;
					case Text: numOperatorType = OperatorTypeValues.Text; break;
					case ActorGroup: numOperatorType = OperatorTypeValues.ActorGroup; break;
					case PropGroup: numOperatorType = OperatorTypeValues.PropGroup; break;
					case StageGroup: numOperatorType = OperatorTypeValues.StageGroup; break;
					case VerbGroup: numOperatorType = OperatorTypeValues.VerbGroup; break;
					case ActorTraitGroup: numOperatorType = OperatorTypeValues.ActorTraitGroup; break;
					case PropTraitGroup: numOperatorType = OperatorTypeValues.PropTraitGroup; break;
					case StageTraitGroup: numOperatorType = OperatorTypeValues.StageTraitGroup; break;
					case MoodTraitGroup: numOperatorType = OperatorTypeValues.MoodTraitGroup; break;
					case QuantifierGroup: numOperatorType = OperatorTypeValues.QuantifierGroup; break;
				}
				jsWordSocket.put("J", numOperatorType);
				jsWordSocketArray.put(jsWordSocket);	
			}
			jsRawSentence.put("K", jsWordSocketArray);
			jsRawSentence.put("L", rawSentence.getInclination());
			
			// belief
			JSONArray jsBelief = new JSONArray();
			for (int i=0; i<rawSentence.getBeliefCount(); i++)
				jsBelief.put(rawSentence.getBelief(i));
			jsRawSentence.put("M", jsBelief);
			

			JSONArray jsOutcomes = new JSONArray();
			for (int i=0; i<rawSentence.getOutcomeCount();i++)
				jsOutcomes.put(rawSentence.getOutcome(i));
			jsRawSentence.put("N", jsOutcomes);

			if (rawSentence.getIsPartOfEpilogue())
				jsRawSentence.put("O", 1);
			else
				jsRawSentence.put("O", 0);
			//jsRawSentence.put("O", String.valueOf(rawSentence.getIsPartOfEpilogue()));
			
			jsRawSentence.put("P", rawSentence.seed);

			jsLabeledSentence.put("Q", jsRawSentence);
			
			// labels
			JSONArray jsLabels = new JSONArray();
			for (String slsLabel: this.STReturnData.tLabeledSentence.labels) {
				jsLabels.put(slsLabel);
			}
			jsLabeledSentence.put("R", jsLabels);
			
			// descriptions
			JSONArray jsDescriptions = new JSONArray();
			
			for (String slsDescription: this.STReturnData.tLabeledSentence.descriptions)
				jsDescriptions.put(slsDescription);
				//jsDescriptions.put(""); // TEST replacement for next line
				
			jsLabeledSentence.put("S", jsDescriptions);
			
			// expressionLabel
			jsLabeledSentence.put("T", this.STReturnData.tLabeledSentence.expressionLabel);
			
			// stageLabel
			jsLabeledSentence.put("U", this.STReturnData.tLabeledSentence.stageLabel);
			
			// actorsPresent
			JSONArray jsActorsPresent = new JSONArray();
			for (String slsActorsPresent: this.STReturnData.tLabeledSentence.actorsPresent)
				jsActorsPresent.put(slsActorsPresent);
			jsLabeledSentence.put("V", jsActorsPresent);
			
			// propsPresent
			JSONArray jsPropsPresent = new JSONArray();
			for (String slsPropsPresent: this.STReturnData.tLabeledSentence.propsPresent)
				jsPropsPresent.put(slsPropsPresent);
			jsLabeledSentence.put("W", jsPropsPresent);
			
			// suffixes
			JSONArray jsSuffixes = new JSONArray();
			for (String slsSuffixes: this.STReturnData.tLabeledSentence.suffixes)
				jsSuffixes.put(slsSuffixes);
			jsLabeledSentence.put("X", jsSuffixes);
			
			// visible
			JSONArray jsVisible = new JSONArray();
			for (boolean slsVisible: this.STReturnData.tLabeledSentence.visible) {
				if (slsVisible)
					jsVisible.put(1);
				else
					jsVisible.put(0);
			}
			//jsVisible.put(String.valueOf(slsVisible));
			jsLabeledSentence.put("Y", jsVisible);
			
			// Final
			jsonobj.put("Z", jsLabeledSentence);
			
			jsonobj.put("A1", this.STReturnData.tTime);
			if (this.STReturnData.showBottom)
				jsonobj.put("B1", 1);
			else
				jsonobj.put("B1", 0);
			//jsonobj.put("B1", String.valueOf(this.STReturnData.showBottom));
			JSONArray jsMenuElements = new JSONArray();
			JSONArray jsMenuElementNames = new JSONArray();
			JSONArray jsMenuElementsDescs = new JSONArray();
			if (!(STReturnData.menuElements == null)) { 
				for (MenuElement sMenuElement: STReturnData.menuElements) {
					 jsMenuElementNames.put(sMenuElement.getLabel());
					 // TEST replacement
					 jsMenuElementsDescs.put(sMenuElement.getDescription());
					 //jsMenuElementsDescs.put("");
				 }
			
				 jsMenuElements.put(jsMenuElementNames);
				 jsMenuElements.put(jsMenuElementsDescs);
				 jsonobj.put("C1", jsMenuElements);
			}
			 jsonobj.put("D1", STReturnData.wordSocket);
			 //System.out.println("wordsocket:" + String.valueOf(STReturnData.wordSocket));
			//inputEnded can be determined from returnType
			 System.out.println(jsonobj.toString());
			return jsonobj;
		} catch (Exception e) {
			return null;
		}
	}
	
	public String toString() {return toJSON().toString(); }	
}
