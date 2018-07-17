package Restlet;

import org.json.JSONArray;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class StoryResource extends Resource {
	String sessionID;
/*
 * Resource: {sessionID}/Story
GET: public byte[] getWorldState(String sessionID) 
			throws RemoteException, SessionLogoutException;

POST: public boolean startStory(final String sessionID, String verbLabel, int[] input {null}, byte[] worldState {null}, boolean logging {false}) 

worldState,boolean logging) 
			throws RemoteException, SessionLogoutException;

TODO: PUT: public boolean startStory(final String sessionID, String verbLabel, int[] input {null}, byte[] worldState, boolean logging {false})
// this is to continue an expired session, using worldState data 

DELETE: public void closeStoryteller(String sessionID) throws RemoteException;
 */
	public StoryResource(Context context, Request request, Response response) {
		super(context, request, response);
		this.sessionID = (String) request.getAttributes().get("sessionID");
		//getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}	
	public boolean allowPut() {
		return false;
	}
	
	public boolean allowPost() {
		return true;
	}
	
	public boolean  allowDelete() {
		return true;
	}
	
	 public boolean setModifiable() {
		 return true;
	 }
	 
	 public boolean setReadable() {
		 return true;
	 }	

	 public Representation represent(Variant variant) throws ResourceException {
		 Representation result = null;

		 try {
			 	System.out.println("GET Story");
			 	byte[] worldState = ServerData.janus.getWorldState(sessionID);
			 	JSONArray jsWorldState = new JSONArray();
			 	for (byte bWorldState: worldState)
			 		jsWorldState.put(String.valueOf(bWorldState));
				result = new JsonRepresentation( jsWorldState );
				System.out.println("DONE");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}

			return result;
	 }
	 
	 // POST
	 public void acceptRepresentation(Representation entity) throws ResourceException {
		 try {
			 System.out.println("POST Story");
			 if (entity.getMediaType().equals(MediaType.APPLICATION_WWW_FORM, true)) {

				 Form form = new Form(entity);

				 String verbLabel = form.getFirstValue("verbLabel", true);

				 boolean result = ServerData.janus.startStory(sessionID, verbLabel, null, null, false);
				 
				 
				 // Process the first two triggers.  Do not send the data
				 //StorytellerReturnData result = ServerData.janus.getTrigger(sessionID);
				 
					try {
						while(null==(ServerData.janus.getTrigger(sessionID))) {
							Thread.sleep(100);
						} ;
					}  catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					try {
						while(null==(ServerData.janus.getTrigger(sessionID))) {
							Thread.sleep(100);
						} ;
					}  catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
//					try {
//						do {
//							Thread.sleep(250);
//						} while(null==(ServerData.janus.getTrigger(sessionID)));
//					}  catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}					
				// result = ServerData.janus.getTrigger(sessionID);				 

				 JSONArray jsResult = new JSONArray();
				 jsResult.put(String.valueOf(result));
				 Representation rep = new JsonRepresentation(jsResult);

				 getResponse().setEntity(rep);
				 System.out.println("DONE");
			 } else {
				 getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			 }
		 } catch (Exception e) {
			 getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		 }
		 
	 }
	 // DELETE
	 public void removeRepresentation() throws ResourceException {
		 try {
			 if (null == this.sessionID) {
				 ErrorMessage em = new ErrorMessage();
				 Representation rep = representError(MediaType.APPLICATION_JSON, em);
				 getResponse().setEntity(rep);
				 getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				 return;
			 }
			 System.out.println("DELETE Story");
			 ServerData.janus.closeStoryteller(sessionID);
			 System.out.println("DONE");
		 } catch (Exception e) {
				// TODO Auto-generated catch block
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} 
	 }
	 
	 
	 /*private Representation representError(Variant variant, ErrorMessage em) throws ResourceException {
		 Representation result = null;
		 if (variant.getMediaType().equals(MediaType.APPLICATION_JSON)) {
			 result = new JsonRepresentation(em.toJSON());
		 } else {
			 result = new StringRepresentation(em.toString());
		 }
		 return result;
	 }*/
	 
	 protected Representation representError(MediaType type, ErrorMessage em) throws ResourceException {
		 Representation result = null;
		 if (type.equals(MediaType.APPLICATION_JSON)) {
			 result = new JsonRepresentation(em.toJSON());
		 } else {
			 result = new StringRepresentation(em.toString());
		 }
		 return result;
	 }
}
