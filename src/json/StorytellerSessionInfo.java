package json;

import java.util.ArrayList;
import org.json.JSONObject;

/*
 * 			result.add(sD.sessionID);
			result.add(sD.dk.version);
			result.add(sD.dk.getRelationshipNames());
			result.add(sD.dk.getVisibleActorTraitNames());
			result.add(sD.dk.getVisibleStageTraitNames());
			result.add(sD.dk.getVisiblePropTraitNames());
			result.add(sD.dk.getActorNames());
			result.add(sD.dk.getStageNames());
			result.add(sD.dk.getPropNames());
 */

public class StorytellerSessionInfo {
	ArrayList<Object> sessionInfo = null;
	
	public StorytellerSessionInfo(ArrayList<Object> sInfo) {
		sessionInfo = sInfo;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject jsonobj = new JSONObject();
			jsonobj.put("sessionID", this.sessionInfo.get(0));
			jsonobj.put("version", this.sessionInfo.get(1));
			jsonobj.put("RelationshipNames",this.sessionInfo.get(2));
			jsonobj.put("VisibleActorTraitNames", this.sessionInfo.get(3));
			jsonobj.put("VisiblePropTraitNames", this.sessionInfo.get(4));
			jsonobj.put("ActorNames", this.sessionInfo.get(5));
			jsonobj.put("StageNames", this.sessionInfo.get(6));
			jsonobj.put("PropNames", this.sessionInfo.get(7));
			return jsonobj;
		} catch (Exception e) {
			return null;
		}
	}
	
	public String toString() {return "da string:" + toJSON().toString(); }
	
}
