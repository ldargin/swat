package Restlet;

import org.restlet.Component;
import org.restlet.data.Protocol;


public class RestMain {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    try {
	    	
	    	
	    	
	    	
	        // Create a new Component.  
	        Component component = new Component();  
	  
	        // Add a new HTTP server listening on port 8182.
	        // local debug setting
	        component.getServers().add(Protocol.HTTP, 80);  
	        // production server settings
	        //component.getServers().add(Protocol.HTTP, "208.70.148.138", 80);
	        // Attach the sample application.  
	        component.getDefaultHost().attach(new RestApplication());  
	  
	        // Start the component.  
	        component.start();  
	    } catch (Exception e) {  
	        // Something is wrong.  
	        e.printStackTrace();  
	    }  

	}

}
