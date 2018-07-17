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


public class SentenceResource extends Resource {
/*
 * Resource: {sessionID}/Sentence:
 * 
 * 
 * GET: public StorytellerReturnData getTrigger(String sessionID)
 * 
POST:  public boolean setResult(String sessionID, int newResult) 
			throws RemoteException, SessionLogoutException, EngineDiedException;
Note: SetResult
1000 = done button was pressed
if (newResult<0) { // undoing
-- I do not understand the concept of a hot wordsocket
ok, it must be, a wordsocket that is already on the screen.
clicking that sents a negative number of its index.
otherwise, clicking a menu item sends a positive number of its index.
I can trace that by behavior, rather than code.
The only question now is: how much of that processing should remain on the client, and how much on the 

server/endpoint.
 */
	String requestSessionID;
	public SentenceResource(Context context, Request request, Response response) {
		super(context, request, response);
		requestSessionID = request.getAttributes().get("sessionID").toString();
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	
	public boolean allowGet() {
		return false;
	}
	public boolean allowPut() {
		return false;
	}
	
	public boolean allowPost() {
		return true;
	}
	
	public boolean  allowDelete() {
		return false;
	}
	
	 public boolean setModifiable() {
		 return true;
	 }
	 
	 public boolean setReadable() {
		 return false;
	 }		
	 
	 // Implement POST.  Calls setResult()
	 public void acceptRepresentation(Representation entity) throws ResourceException {
		 try {
			 System.out.println("POST Sentence");
			 if (entity.getMediaType().equals(MediaType.APPLICATION_WWW_FORM, true)) {
				 Form form = new Form(entity);
				 
				 //ArrayList<Object> sInfo = ServerData.janus.startStorytellerSession(Integer.parseInt(form.getFirstValue("clientVersion", true)), form.getFirstValue("userName", true), form.getFirstValue("storyworldFile", true));
				 int newResult = Integer.parseInt(form.getFirstValue("newResult", true));
				 boolean result = ServerData.janus.setResult(requestSessionID, newResult);
				 
				 JSONArray jsResult = new JSONArray();
				 jsResult.put(result);
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
