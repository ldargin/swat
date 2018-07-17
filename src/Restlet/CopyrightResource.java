package Restlet;

import java.rmi.RemoteException;

import org.json.JSONArray;
import org.restlet.Context;
import org.restlet.resource.Resource;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.storytron.enginecommon.SessionLogoutException;

public class CopyrightResource extends Resource {
	String sessionID;
	/*
 * Resource: {sessionID}/CopyrightInfo
just returns copyright info
public String getCopyright(String sessionID) 

GET: public String getCopyright(String sessionID) 
POST nothing
PUT: nothing
DELETE: nothing
 */
	public CopyrightResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		this.sessionID = (String) request.getAttributes().get("sessionID");
		//getVariants().add(new Variant(MediaType.TEXT_PLAIN));
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
	}
	
	public boolean allowPut() {
		return false;
	}
	
	public boolean allowPost() {
		return false;
	}
	
	public boolean  allowDelete() {
		return false;
	}
	
	 public boolean setModifiable() {
		 return false;
	 }
	 
	 public boolean setReadable() {
		 return true;
	 }
	 
	 public Representation represent(Variant variant) throws ResourceException {
		 Representation result = null;
		 System.out.println("GET Copyright");
		 try {
			 	String copyrightData  = ServerData.janus.getCopyright(sessionID);
			 	JSONArray jsCopyright = new JSONArray();
			 	jsCopyright.put(copyrightData);
				result = new JsonRepresentation(jsCopyright );
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (SessionLogoutException e) {
				// TODO Auto-generated catch block
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
			System.out.println("DONE");
			return result;
	 }
	 
	 /*
	 private Representation representError(Variant variant, ErrorMessage em) throws ResourceException {
		 Representation result = null;
		 if (variant.getMediaType().equals(MediaType.APPLICATION_JSON)) {
			 result = new JsonRepresentation(em.toJSON());
		 } else {
			 result = new StringRepresentation(em.toString());
		 }
		 return result;
	 }
	*/ 
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

 