package Engine.enginePackage;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Vector;

/** 
 * A class for collecting statistics about server activity.
 * <p>
 * It collects the following statistics in a period of time:
 * <ul>
 * <li>Maximum number of simultaneous author sessions.</li>
 * <li>Maximum number of simultaneous player sessions.</li>
 * <li>Maximum number of simultaneous loaded storywordls.</li>
 * <li>Amount of saved sessions.</li>
 * <li>Amount of loaded sessions.</li>
 * <li>Amount of loaded traces.</li>
 * <li>Average reaction time for storyteller.</li>
 * <li>Maximum reaction time for storyteller.</li>
 * <li>Average reaction time for LogLizard.</li>
 * <li>Maximum reaction time for LogLizard.</li>
 * <li>Average reaction time for RehearsalLizard.</li>
 * <li>Maximum reaction time for RehearsalLizard.</li>
 * <li>Total surface byte count sent for LogLizard.</li>
 * <li>Total deep byte count sent for LogLizard.</li>
 * <li>Total sentence byte count sent for Storyteller.</li>
 * <li>Total background information sent for Storyteller.</li>
 * </ul>
 * <p>
 * Statistics are collected in memory, and then are sent to a database.
 * This class provides methods for creating tables in the database
 * and storing the values there. 
 * */
public class Statistics implements Cloneable {
	
	private class clickData {
		public clickData(String storyworld, Timestamp startTime, 
				String sessionID, String ip_hash, boolean isDone, boolean isStart, boolean isEnd) {
			super();
			this.ip_hash = ip_hash;
			this.sessionID = sessionID;
			this.startTime = startTime;
			this.storyworld = storyworld;
			this.isDone = isDone;
			this.isStart = isStart;
			this.isEnd = isEnd;
		}
		String storyworld;
		Timestamp startTime;
		String sessionID;
		String ip_hash;
		boolean isDone;
		boolean isStart;
		boolean isEnd;
		
		
	}
	
	private int simultaneousAuthorSessions; 
	private int maxSimultaneousAuthorSessions;
	private int simultaneousPlayerSessions; 
	private int maxSimultaneousPlayerSessions;
	private int maxSimultaneousStoryworlds;
	private int simultaneousStoryworldsCount;
	private int savedSessions;
	private int loadedSessions;
	private int loadedStoryTraces;
	private long storytellerReactionTimeSum;
	private int storytellerReactionTimeCount;
	private long maxStorytellerReactionTime;
	private long logLizardReactionTimeSum;
	private int logLizardReactionTimeCount;
	private long maxLogLizardReactionTime;
	private long rehearsalReactionTimeSum;
	private int rehearsalReactionTimeCount;
	private long maxRehearsalReactionTime;
	private long maxRehearsalCancelTime;
	private int surfaceLogLizardByteSum;
	private int deepLogLizardByteSum;
	private int sentenceByteSum;
	private int bgInfoByteSum;
	Vector<clickData> clickList = new Vector<clickData>();
	
	public static void main(String[] s) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection("jdbc:mysql://208.70.148.137:3306/statistics", "swat", "Fateacts");
//		dropTables(con);
//		createTables(con);
//		fillStatIdentifiersTable(con);
		Statistics st=new Statistics();
		st.savedSessions = 9;
		st.saveStatistics((byte)0, con);
		con.close();
	}

	/** Identifiers for statistics. */
	private enum StatisticId {
		maxSimASessions
		,maxSimPSessions
		,maxSimStoryworld
		,savedSessions
		,loadedSessions
		,loadedTraces
		,averageRTStoryteller
		,maxRTStoryteller
		,averageRTLogLizard
		,maxRTLogLizard
		,averageRTRehearsal
		,maxRTRehearsal
		,maxCTRehearsal
		,surfaceBCLogLizard
		,deepBCLogLizard
		,sentBCStoryteller
		,bgInfoStoryteller
	};
	
	/** Descriptions of the statistics. */
	private static EnumMap<StatisticId, String> statDesc = new EnumMap<StatisticId, String>(StatisticId.class);

	static {
		 statDesc.put(StatisticId.maxSimASessions, "Maximum amount of simultaneous author sessions.");
		 statDesc.put(StatisticId.maxSimPSessions, "Maximum amount of simultaneous player sessions.");
		 statDesc.put(StatisticId.maxSimStoryworld, "Maximum amount of simultaneous loaded Storyworlds.");
		 statDesc.put(StatisticId.savedSessions,"Amount of saved sessions.");
		 statDesc.put(StatisticId.loadedSessions,"Amount of loaded sessions.");
		 statDesc.put(StatisticId.loadedTraces,"Amount of loaded traces.");
		 statDesc.put(StatisticId.averageRTStoryteller,"Average reaction time for Storyteller.");
		 statDesc.put(StatisticId.maxRTStoryteller,"Maximum reaction time for Storyteller.");
		 statDesc.put(StatisticId.averageRTLogLizard,"Average reaction time for LogLizard.");
		 statDesc.put(StatisticId.maxRTLogLizard,"Maximum reaction time for LogLizard.");
		 statDesc.put(StatisticId.averageRTRehearsal,"Average reaction time for Rehearsal Lizard.");
		 statDesc.put(StatisticId.maxRTRehearsal,"Maximum reaction time for Rehearsal Lizard.");
		 statDesc.put(StatisticId.maxCTRehearsal,"Maximum time before canceling Rehearsal Lizard.");
		 statDesc.put(StatisticId.surfaceBCLogLizard,"Total surface byte count sent for LogLizard.");
		 statDesc.put(StatisticId.deepBCLogLizard,"Total deep byte count sent for LogLizard.");
		 statDesc.put(StatisticId.sentBCStoryteller,"Total sentence byte count sent to Storyteller.");
		 statDesc.put(StatisticId.bgInfoStoryteller,"Total background information sent to Storyteller.");
	}
	
	/** 
	 * Creates the tables in a database.
	 * <p>
	 * The table Layout is as follows:
	 * <ul>
	 * <li> There is a table StatIdentifiers(StatId,StatName,StatDescription) which
	 *      lists all the possible statistics that may be collected.
	 * </li>  
	 * <li> There is a table StatDumps(DumpId,ServerId,DumpTimeStamp) which
	 *      lists all the registered dumps of statistics.
	 *      A dump is a set of statistics registered by a server in a certain
	 *      period of time (currently 1 hour). The time stamp indicates
	 *      the time at which the period ends. Actual statistic values
	 *      are inserted in table FloatStats using the DumpId.
	 *      The time stamp in in UTC.
	 * </li>
	 * <li> The table FloatStats(DumpId,StatId,FloatValue) contains all the
	 *      statistic values.
	 * </li>
	 * </ul>
	 * */
	public static void createTables(Connection con) throws SQLException {
		Statement st = con.createStatement();
		st.executeUpdate(
				"CREATE TABLE StatIdentifiers ("+
				"StatId TINYINT UNSIGNED NOT NULL,"+
				"StatName VARCHAR(20)  CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL UNIQUE,"+
				"StatDescription VARCHAR(100)  CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,"+
				"PRIMARY KEY (StatId)"+
				")");
		st.executeUpdate(
				"CREATE TABLE StatDumps ("+
				"DumpId INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,"+
				"ServerId TINYINT UNSIGNED NOT NULL,"+
				"DumpTimeStamp DATETIME NOT NULL,"+
				"PRIMARY KEY (DumpId)"+
				")");
		st.executeUpdate(
				"CREATE TABLE FloatStats ("+
				"DumpId INTEGER UNSIGNED NOT NULL,"+
				"StatId TINYINT UNSIGNED NOT NULL,"+
				"FloatValue FLOAT NOT NULL,"+
				"PRIMARY KEY (DumpId, StatId)"+
				")");
	}

	/** Drops tables in a database. */
	public static void dropTables(Connection con) throws SQLException {
		Statement st = con.createStatement();
		st.executeUpdate("DROP TABLE StatDumps, FloatStats, StatIdentifiers");
	}
	
	/** Fills the table with the statistic names and descriptions in {@link Statistics#statDesc}. */
	public static void fillStatIdentifiersTable(Connection con) throws SQLException {
		PreparedStatement pst = con.prepareStatement(
			"INSERT INTO StatIdentifiers(StatId,StatName,StatDescription) VALUES(?,?,?)");
		for(StatisticId si:StatisticId.values()) {
			pst.setInt(1, si.ordinal());
			pst.setString(2, si.name());
			pst.setString(3, statDesc.get(si));
			pst.executeUpdate();
		}
	}
	
	/** Saves the statistics and resets them. This method is thread safe. */
	public void saveStatistics(byte serverId) throws SQLException {
		Connection con = DriverManager.getConnection("jdbc:mysql://208.70.148.137:3306/statistics", "swat", "Fateacts");
		try {
			cloneAndReset().saveStatistics(serverId,con);
		} finally {
			con.close();
		}
	}
	
	/** 
	 * Saves the statistics. This method is not thread safe.
	 * It is intended to be called on an instance exclusively owned by the
	 * calling thread.
	 * */
	private void saveStatistics(byte serverId,Connection con) throws SQLException {
		// Insert the dump info
		Statement st = con.createStatement();
		st.executeUpdate(
			"INSERT INTO StatDumps(ServerId,DumpTimeStamp) VALUES("+serverId+",UTC_TIMESTAMP())");

		// Get the assigned identifier for the dump.
		ResultSet rs=st.executeQuery("SELECT LAST_INSERT_ID()");
		rs.last();
		int dumpId = rs.getInt(1);
		
		// Insert the statistic values
		String insertionString="INSERT INTO FloatStats VALUES "
		+createStatTuple(dumpId,StatisticId.maxSimASessions,maxSimultaneousAuthorSessions)
		+","+createStatTuple(dumpId,StatisticId.maxSimPSessions,maxSimultaneousPlayerSessions)
		+","+createStatTuple(dumpId,StatisticId.maxSimStoryworld,maxSimultaneousStoryworlds)
		+","+createStatTuple(dumpId,StatisticId.savedSessions,savedSessions)
		+","+createStatTuple(dumpId,StatisticId.loadedSessions,loadedSessions)
		+","+createStatTuple(dumpId,StatisticId.loadedTraces,loadedStoryTraces)
		+","+createStatTuple(dumpId,StatisticId.averageRTStoryteller,average(storytellerReactionTimeSum,storytellerReactionTimeCount))
		+","+createStatTuple(dumpId,StatisticId.maxRTStoryteller,maxStorytellerReactionTime)
		+","+createStatTuple(dumpId,StatisticId.averageRTLogLizard,average(logLizardReactionTimeSum,logLizardReactionTimeCount))
		+","+createStatTuple(dumpId,StatisticId.maxRTLogLizard,maxLogLizardReactionTime)
		+","+createStatTuple(dumpId,StatisticId.averageRTRehearsal,average(rehearsalReactionTimeSum,rehearsalReactionTimeCount))
		+","+createStatTuple(dumpId,StatisticId.maxRTRehearsal,maxRehearsalReactionTime)
		+","+createStatTuple(dumpId,StatisticId.maxCTRehearsal,maxRehearsalCancelTime)
		+","+createStatTuple(dumpId,StatisticId.surfaceBCLogLizard,surfaceLogLizardByteSum)
		+","+createStatTuple(dumpId,StatisticId.deepBCLogLizard,deepLogLizardByteSum)
		+","+createStatTuple(dumpId,StatisticId.sentBCStoryteller,sentenceByteSum)
		+","+createStatTuple(dumpId,StatisticId.bgInfoStoryteller,bgInfoByteSum)
		;
		
		st.executeUpdate(insertionString);
		
		saveClicks(con, dumpId);

	}
	
	private void saveClicks(Connection con, int dumpId) throws SQLException {
		// Insert the click data
		// Store a record for every turn.  (are there datasets)
		PreparedStatement ps = con.prepareStatement("INSERT INTO storyclicks(storyworld, startdate, sessionid, ip_hash, code, dumpid) VALUES(?, ?, ?, ?, ?, ?)");
		
		// storing each click separately for now
		for (clickData click: clickList) {
			ps.setString(1, click.storyworld);
			ps.setTimestamp(2, click.startTime);
			ps.setString(3, click.sessionID);
			ps.setString(4, click.ip_hash);
			if (click.isDone)
				ps.setInt(5, 1);
			else if (click.isStart)
				ps.setInt(5, 1000);
			else if (click.isEnd)
				ps.setInt(5, 2000);
			else
				ps.setInt(5, 0);
			ps.setInt(6, dumpId);
			ps.execute();
		}
	}
	
	/** Auxiliary method to improve readability of saveStatistics. */
	private static String createStatTuple(int dumpId,StatisticId si,float value) {
		return "("+dumpId+","+si.ordinal()+","+value+")";
	} 
	
	/** @return 0 if c is zero, otherwise s/c. */
	private static float average(long s,int c){
		return c!=0? s/(float)c : 0;
	}
	
	/** Creates a copy of these statistics and then resets them. */
	protected synchronized Statistics cloneAndReset() {
		try {
			Statistics s = (Statistics)super.clone();
			// copy player turn statistics.
			s.clickList = new Vector<clickData>();
			for (clickData turn: clickList) {
				s.clickList.add(turn);
			}

			reset();
			return s;
		} catch(CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Resets all statistics. */
	public synchronized void reset() {
		maxSimultaneousAuthorSessions=simultaneousAuthorSessions;
		maxSimultaneousPlayerSessions=simultaneousPlayerSessions;
		maxSimultaneousStoryworlds=simultaneousStoryworldsCount;
		savedSessions=0;
		loadedSessions=0;
		loadedStoryTraces=0;
		storytellerReactionTimeSum=0;
		storytellerReactionTimeCount=0;
		maxStorytellerReactionTime=0;
		logLizardReactionTimeSum=0;
		logLizardReactionTimeCount=0;
		maxLogLizardReactionTime=0;
		rehearsalReactionTimeSum=0;
		rehearsalReactionTimeCount=0;
		maxRehearsalReactionTime=0;
		maxRehearsalCancelTime=0;
		surfaceLogLizardByteSum=0;
		deepLogLizardByteSum=0;
		sentenceByteSum=0;
		bgInfoByteSum=0;
		clickList.clear();
	}
	
	public synchronized void reportAuthorLogin(){
		simultaneousAuthorSessions++;
		if (simultaneousAuthorSessions>maxSimultaneousAuthorSessions)
			maxSimultaneousAuthorSessions=simultaneousAuthorSessions;
	}
	
	public synchronized void reportPlayerLogin(){
		simultaneousPlayerSessions++;
		if (simultaneousPlayerSessions>maxSimultaneousPlayerSessions)
			maxSimultaneousPlayerSessions=simultaneousPlayerSessions;
	}

	public synchronized void reportStoryworldCount(int count){
		simultaneousStoryworldsCount = count;
		if (maxSimultaneousStoryworlds<simultaneousStoryworldsCount)
			maxSimultaneousStoryworlds=simultaneousStoryworldsCount;
	}

	public synchronized void reportAuthorLogout(){	simultaneousAuthorSessions--;	}
	
	public synchronized void reportPlayerLogout(){	simultaneousPlayerSessions--;	}
	
	public synchronized void reportSavedSession(){ savedSessions++; }
	
	public synchronized void reportLoadedSession(){ loadedSessions++; }
	
	public synchronized void reportLoadedStoryTrace(){ loadedStoryTraces++; }
	
	public synchronized void reportStorytellerReactionTime(long reactionTime){
		storytellerReactionTimeSum += reactionTime;
		storytellerReactionTimeCount++;
		if (reactionTime>maxStorytellerReactionTime)
			maxStorytellerReactionTime = reactionTime;
	}
	
	public synchronized void reportLogLizardReactionTime(long reactionTime){
		logLizardReactionTimeSum += reactionTime;
		logLizardReactionTimeCount++;
		if (reactionTime>maxLogLizardReactionTime)
			maxLogLizardReactionTime = reactionTime;
	}

	public synchronized void reportRehearsalReactionTime(long reactionTime){
		rehearsalReactionTimeSum += reactionTime;
		rehearsalReactionTimeCount++;
		if (reactionTime>maxRehearsalReactionTime)
			maxRehearsalReactionTime = reactionTime;
	}

	public synchronized void reportRehearsalCancelTime(long reactionTime){
		if (reactionTime>maxRehearsalCancelTime)
			maxRehearsalCancelTime = reactionTime;
	}
	
	public synchronized void reportSurfaceLogLizardSentBytes(int byteCount){
		surfaceLogLizardByteSum += byteCount;
	}

	public synchronized void reportDeepLogLizardSentBytes(int byteCount){
		deepLogLizardByteSum += byteCount;
	}
	
	public synchronized void reportSentenceSentBytes(int byteCount){
		sentenceByteSum += byteCount;
	}

	public synchronized void reportBackgroundInformationSentBytes(int byteCount){
		bgInfoByteSum += byteCount;
	}
	
	public synchronized void reportClick(String storyworld, java.util.Date startTime, String sessionID, String ipAddress, boolean isDone, boolean isStart, boolean isEnd) {
		// get the current date 
		Timestamp sqlStartTime = new Timestamp(System.currentTimeMillis());
		String storyWorldId = storyworld;
		
		if (storyworld == null)
			storyWorldId = "NULL";
		
		clickData click = new clickData(storyWorldId, sqlStartTime, sessionID, ipAddress, isDone, isStart, isEnd);
		clickList.add(click);
	}

	public static abstract class Test {
		public static int getMaxSimAuthorSessions(Statistics st){ return st.maxSimultaneousAuthorSessions; }
		public static int getMaxSimPlayerSessions(Statistics st){ return st.maxSimultaneousPlayerSessions; }
		public static int getAuthorSessions(Statistics st){ return st.simultaneousAuthorSessions; }
		public static int getPlayerSessions(Statistics st){ return st.simultaneousPlayerSessions; }
		public static int getSavedSessions(Statistics st){ return st.savedSessions; }
		public static int getLoadedSessions(Statistics st){ return st.loadedSessions; }
		public static int getLoadedStoryTraces(Statistics st){ return st.loadedStoryTraces; }
		
		public static int getStorytellerReactionTimeCount(Statistics st){ return st.storytellerReactionTimeCount; }
		public static long getStorytellerReactionTimeSum(Statistics st){ return st.storytellerReactionTimeSum; }
		public static long getMaxStorytellerReactionTime(Statistics st){ return st.maxStorytellerReactionTime; }
		
		public static int getLogLizardReactionTimeCount(Statistics st){ return st.logLizardReactionTimeCount; }
		public static long getLogLizardReactionTimeSum(Statistics st){ return st.logLizardReactionTimeSum; }
		public static long getMaxLogLizardReactionTime(Statistics st){ return st.maxLogLizardReactionTime; }

		public static int getRehearsalReactionTimeCount(Statistics st){ return st.rehearsalReactionTimeCount; }
		public static long getRehearsalReactionTimeSum(Statistics st){ return st.rehearsalReactionTimeSum; }
		public static long getMaxRehearsalReactionTime(Statistics st){ return st.maxRehearsalReactionTime; }

		public static long getMaxRehearsalCancelTime(Statistics st){ return st.maxRehearsalCancelTime; }
		
		public static long getSurfaceLogLizardSentBytes(Statistics st){ return st.surfaceLogLizardByteSum; }
		public static long getDeepLogLizardSentBytes(Statistics st){ return st.deepLogLizardByteSum; }
		public static long getSentenceSentBytes(Statistics st){ return st.sentenceByteSum; }
		public static long getBackgroundInformationSentBytes(Statistics st){ return st.bgInfoByteSum; }
	}
}
