package Restlet;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import com.storytron.enginecommon.StorytellerRemote;

public class ServerData {
	static StorytellerRemote janus;
	public static ConcurrentHashMap<String, ServerSessionStats> sessionStats = new ConcurrentHashMap<String, ServerSessionStats>();
	
	public class ServerSessionStats {
		public ServerSessionStats() {
			// TODO Auto-generated constructor stub
		}
		int restTurns = 0;
		Date lastUsed = new Date();

	}
	
	public static ServerSessionStats trackSession(String sessionID, ServerSessionStats stat) {

		sessionStats.putIfAbsent(sessionID, stat);
		return stat;
	}  
	
	public void untrackSession(String sessionID) {
		sessionStats.remove(sessionID);
	}
	
	public int addTurn(String sessionID) {
		ServerSessionStats stat = sessionStats.get(sessionID);
		updateLastUsed(stat);
		return ++(stat.restTurns);
	}
	
	public void updateLastUsed(ServerSessionStats stat) {
		stat.lastUsed = new Date();
	}
	
}
