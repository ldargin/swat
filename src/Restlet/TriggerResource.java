package Restlet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;
import json.*;
import com.storytron.enginecommon.StorytellerReturnData;
import org.restlet.Context;
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


// POST: public StorytellerReturnData getTrigger(String sessionID)
public class TriggerResource extends Resource {
	String requestSessionID;
	public TriggerResource(Context context, Request request, Response response) {
		super(context, request, response);
		requestSessionID = request.getAttributes().get("sessionID").toString();
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_PLAIN));
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
	 
	 // Implement POST.  Calls getTrigger()  Note: using POST because getTrigger changes data on the server
	 public void acceptRepresentation(Representation entity) throws ResourceException {
		 try {
			 //System.out.println("POST Trigger");
			 
			 if (entity.getMediaType().equals(MediaType.APPLICATION_WWW_FORM, true)) {
				 ServerData sD = new ServerData();
				 int restMove = sD.addTurn(requestSessionID);
				 System.out.println("Move: " + String.valueOf(restMove));
				 StorytellerReturnData result = ServerData.janus.getTrigger(requestSessionID);
				 

				 //Representation rep = new JsonRepresentation(new StorytellerReturnDataJson(result).toJSON());
				 byte[] jsByteArray = new StorytellerReturnDataJson(result).toString().getBytes();

				 System.out.println(jsByteArray.length);
				 ByteArrayOutputStream baos = new ByteArrayOutputStream(300);
				 BufferedOutputStream out =
				        new BufferedOutputStream(
				          new DeflaterOutputStream(baos));
				 out.write(jsByteArray);
				 out.close();
				 byte[] deflateByteArray = baos.toByteArray();
				 System.out.println("Length:" + deflateByteArray.length);
				 ByteArrayInputStream bais = new ByteArrayInputStream(deflateByteArray);
				 Representation rep = new InputRepresentation(bais, MediaType.APPLICATION_ALL);
				 
				 getResponse().setEntity(rep);
			 } else {
				 getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			 }
			 System.out.println("DONE");
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
