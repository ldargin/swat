package Restlet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import org.json.JSONArray;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.InputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import com.storytron.enginecommon.Pair;

public class SessionResource extends Resource {
/*
 * Resource: session
login gate: login()

GET: nothing.  just confirm that the session exists for now
POST: startStorytellerSession()  {does POST return a lot of data?}
PUT: nothing.  
DELETE: public void logout(String sessionID) throws RemoteException 
 */
	
	public String sessionID;
	
	public SessionResource(Context context, Request request, Response response) {
		super(context, request, response);

		// get the Session ID from the URL, if it exists
		if (request.getAttributes().containsKey("sessionID"))
			this.sessionID = (String) request.getAttributes().get("sessionID");
		
		//getVariants().add(new Variant(MediaType.TEXT_PLAIN));
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
		return true;
	}
	
	 public boolean setModifiable() {
		 return true;
	 }
	 
	 public boolean setReadable() {
		 return false;
	 }	
	
	 // TODO: Implement GET.  Must first modify Janus and create a new remote interface to check sessions.
	 // Implement POST.  Calls startStorytellerSession()
	 @SuppressWarnings("unchecked")
	public void acceptRepresentation(Representation entity) throws ResourceException {
		 try {
			 System.out.println("POST Session");
			 ServerData sD = new ServerData();
			 
			 if (entity.getMediaType().equals(MediaType.APPLICATION_WWW_FORM, true)) {
				 Form form = new Form(entity);
				 
				 ArrayList sInfo = ServerData.janus.startStorytellerSession(Integer.parseInt(form.getFirstValue("clientVersion", true)), form.getFirstValue("userName", true), form.getFirstValue("storyworldFile", true));
				 sessionID = sInfo.get(0).toString();
				 ServerData.ServerSessionStats sst = sD.new ServerSessionStats();
				 ServerData.trackSession(sessionID, sst);
				 

				 
				 JSONArray jsSInfo = new JSONArray();
				 // the session ID {string}
				 jsSInfo.put(sInfo.get(0));
				 
				 // the storyworld version {int}
				 jsSInfo.put(sInfo.get(1).toString());
				 
				 // the visible relationship names and descriptions Pair<String[],String[]>
				 Pair<String[],String[]> relationshipNamesAndDescs = (Pair<String[],String[]>)(sInfo.get(2));
				 String[] relationshipNames = (String[])relationshipNamesAndDescs.first;
				 String[] relationshipDescs = (String[])relationshipNamesAndDescs.second;
				 JSONArray jsRelNamesDesc = new JSONArray();
				 JSONArray jsRelNames = new JSONArray();
				 JSONArray jsRelDescs = new JSONArray();
				 for (String sName: relationshipNames) 
					 jsRelNames.put(sName);
				 for (String sDesc: relationshipDescs) 
					 jsRelDescs.put(sDesc);
				 jsRelNamesDesc.put(jsRelNames);
				 jsRelNamesDesc.put(jsRelDescs);
	//test			 jsSInfo.put(jsRelNamesDesc);
				 
				 //actor trait names and descriptions Pair<String[],String[]> 
				 Pair<String[],String[]> actorTraitNamesAndDescs = (Pair<String[],String[]>)(sInfo.get(3));
				 String[] actorTraitNames = (String[])actorTraitNamesAndDescs.first;
				 String[] actorTraitDescs = (String[])actorTraitNamesAndDescs.second;
				 JSONArray jsActorTraitNamesDesc = new JSONArray();
				 JSONArray jsActorTraitNames = new JSONArray();
				 JSONArray jsActorTraitDescs = new JSONArray();
				 for (String sName: actorTraitNames) 
					 jsRelNames.put(sName);
				 for (String sDesc: actorTraitDescs) 
					 jsRelDescs.put(sDesc);
				 jsActorTraitNamesDesc.put(jsActorTraitNames);
				 jsActorTraitNamesDesc.put(jsActorTraitDescs);
				//test			 jsSInfo.put(jsActorTraitNamesDesc);
				 
				 //stage trait names and descriptions Pair<String[],String[]> 
				 // the visible relationship names and descriptions Pair<String[],String[]>
				 Pair<String[],String[]> stageTraitNamesAndDescs = (Pair<String[],String[]>)(sInfo.get(4));
				 String[] stageTraitNames = (String[])stageTraitNamesAndDescs.first;
				 String[] stageTraitDescs = (String[])stageTraitNamesAndDescs.second;
				 JSONArray jsStageTraitNamesDesc = new JSONArray();
				 JSONArray jsStageTraitNames = new JSONArray();
				 JSONArray jsStageTraitDescs = new JSONArray();
				 for (String sName: stageTraitNames) 
					 jsStageTraitNames.put(sName);
				 for (String sDesc: stageTraitDescs) 
					 jsStageTraitDescs.put(sDesc);
				 jsStageTraitNamesDesc.put(jsStageTraitNames);
				 jsStageTraitNamesDesc.put(jsStageTraitDescs);		
				//test			 jsSInfo.put(jsStageTraitNamesDesc);
				 
				 //prop trait names and descriptions Pair<String[],String[]> 
				 Pair<String[],String[]> propTraitNamesAndDescs = (Pair<String[],String[]>)(sInfo.get(5));
				 String[] propTraitNames = (String[])propTraitNamesAndDescs.first;
				 String[] propTraitDescs = (String[])propTraitNamesAndDescs.second;
				 JSONArray jsPropTraitNamesDesc = new JSONArray();
				 JSONArray jsPropTraitNames = new JSONArray();
				 JSONArray jsPropTraitDescs = new JSONArray();
				 for (String sName: propTraitNames) 
					 jsPropTraitNames.put(sName);
				 for (String sDesc: propTraitDescs) 
					 jsPropTraitDescs.put(sDesc);
				 jsPropTraitNamesDesc.put(jsPropTraitNames);
				 jsPropTraitNamesDesc.put(jsPropTraitDescs);		
				//test			 jsSInfo.put(jsPropTraitNamesDesc);		
				 
				 // actor names string[]
				 String[] actorNames = (String[])sInfo.get(6);
				 JSONArray jsActorNames = new JSONArray();
				 for (String sName: actorNames) 
					 jsActorNames.put(sName);
				//test			 jsSInfo.put(jsActorNames);
			 
				 // stage names string[]
				 String[] stageNames = (String[])sInfo.get(7);
				 JSONArray jsStageNames = new JSONArray();
				 for (String sName: stageNames) 
					 jsStageNames.put(sName);
				//test			 jsSInfo.put(jsStageNames);
				 
				 // prop names string[]
				 String[] propNames = (String[])sInfo.get(8);
				 JSONArray jsPropNames = new JSONArray();
				 for (String sName: propNames) 
					 jsPropNames.put(sName);
				//test			 jsSInfo.put(jsPropNames);
				 
				 System.out.println(jsSInfo.toString());
				 
				 //Representation rep = new JsonRepresentation(jsSInfo);
				 //Representation rep = new StringRepresentation(jsSInfo.toString());
				 byte[] jsByteArray = jsSInfo.toString().getBytes();

				 System.out.println(jsByteArray.length);
				 ByteArrayOutputStream baos = new ByteArrayOutputStream(300);
				 BufferedOutputStream out =
				        new BufferedOutputStream(
				          new DeflaterOutputStream(baos));
				 

				 
				 out.write(jsByteArray);
				 out.close();

				 byte[] gzByteArray = baos.toByteArray();
				 System.out.println("Length:" + gzByteArray.length);
				 
				 ByteArrayInputStream bais = new ByteArrayInputStream(gzByteArray);
				 Representation rep = new InputRepresentation(bais, MediaType.APPLICATION_ALL);

				 getResponse().setEntity(rep);
				 System.out.println("DONE");
			 } else {
				 getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			 }
		 } catch (Exception e) {
			 getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
		 }
		 
	 }
	 
	 //Implement DELETE.  Calls logout
	 public void removeRepresentations() throws ResourceException {
		 try {
			 if (null == this.sessionID) {
				 ErrorMessage em = new ErrorMessage();
				 Representation rep = representError(MediaType.APPLICATION_JSON, em);
				 getResponse().setEntity(rep);
				 getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				 return;
			 }
			
			 // delete the session
			 ServerData sD = new ServerData();
			 ServerData.janus.logout(this.sessionID);
			 sD.untrackSession(this.sessionID);
			 getResponse().setStatus(Status.SUCCESS_OK);
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
