package com.storytron.enginecommon;

public final class SharedConstants {
	public static boolean isRemote = false;	// Set this to true for running on a server
	private static String baseServiceName = "SwatServer";
	/** Value used to detect if the client will have problems when using the server. */
	public static int REMOTE_INTERFACE_VERSION = 3024;
	public static int version = 3044; 	// The current version of Swat
	private static int basePort = 1100;
	public  static final int numServices = 5;
	private static byte defaultServiceNum = 2;

	private static String remoteHostname = "208.70.148.137";
	private static String testHost = "localhost";
	public static final boolean useRemoteInterface = false; // Use RMI interface
	public static final int TASK_CANCELLED = -1;
	 
	/** Instantiating this class makes no sense. */
	private SharedConstants() {}
	
	public static String getRMIServiceString(int serviceNum) {
		String serviceString;
		
		if (serviceNum < numServices)
			serviceString =  "rmi://"+getHostName()+"/" + getServiceName(serviceNum);
		else
			serviceString = "Invalid Service";
		
		return serviceString;
	}
	
	public static String getHostName() {
		String hostName;
		if (isRemote)
			hostName = remoteHostname;
		else
			hostName = testHost;
		
		return hostName;
	}
	
	public static String getServiceName(int serviceNum) {
		String serviceName =baseServiceName + getRemoteInterfaceVersionName() + "_" + getPort(serviceNum); 
		return serviceName;
	}
	
	public static int getPort(int serviceNum) {
		return basePort+serviceNum;
	}

	public static byte getDefaultServiceNum() {
		return defaultServiceNum;
	}
	public static String getRemoteInterfaceVersionName() {
		return String.valueOf(REMOTE_INTERFACE_VERSION);
	}
}
