package engPackage;

import java.util.Arrays;

import com.storytron.enginecommon.Triplet;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;

public class RehearsalLogger {

	private Janus.SessionData sD;
	private static final int MAX_POISONING_REPORTS = 100;
	
	RehearsalLogger(Janus.SessionData sD){
		this.sD = sD;
	}
	
	public void reactSentenceVerbCandidacy(int tiVerb) {
		++sD.verbData.get(tiVerb).candidacies;
	}

	public void reactSentenceVerbActivation(int tiVerb) {
		++sD.verbData.get(tiVerb).activations;
	}

	/**	 Called from the story engine whenever somebody reacts to an event. This happens
	 *  regardless of whether the role is activated; that's why we're incrementing
	 *  scRoleCandidacies regardless, and incrementing scRoleActivations if Engine.mfRoleActive
	 *  is true.
	 */
	public void reactSentenceRoleCandidacy(int tiVerb, int tiRole) {
			++sD.verbData.get(tiVerb).roleData.get(tiRole).candidacies;
	}

	public void recordPoison(ScriptPath sp, Script s, String poisonExplanation) {
		if (sD.poisonings.size()>MAX_POISONING_REPORTS)
			return;
			
		final String[] scriptPath = sp.getScriptLocators(s);
		boolean foundMatch = false;
		int i = 0;
		while (!foundMatch & (i < sD.poisonings.size())) {
			boolean scriptMatch = s.getType()==sD.poisonings.get(i).first && Arrays.equals(scriptPath,sD.poisonings.get(i).second);
			boolean poisonMatch = poisonExplanation.equals(sD.poisonings.get(i).third);
			foundMatch = scriptMatch & poisonMatch;
			++i;
		}
		if (i == sD.poisonings.size()) // no match, so add it
			sD.poisonings.add(new Triplet<Script.Type,String[],String>(s.getType(),scriptPath, poisonExplanation));
	}

	public void reactSentenceRoleActivation(int tiVerb, int tiRole) {
		++sD.verbData.get(tiVerb).roleData.get(tiRole).activations;
	}

	public void reactSentenceOptionCandidacy(int tiVerb, int tiRole, int tiOption) {
		++sD.verbData.get(tiVerb).roleData.get(tiRole).optionData.get(tiOption).candidacies;
	}

	public void reactSentenceOptionActivation(int tiVerb, int tiRole, int tiOption) {
		++sD.verbData.get(tiVerb).roleData.get(tiRole).optionData.get(tiOption).activations;
	}

}
