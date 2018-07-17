package Restlet;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.rmi.RemoteException;

import org.json.JSONArray;
import org.restlet.Context;
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

import com.storytron.enginecommon.SessionLogoutException;

public class StageTraitsResource extends Resource {
/*
 * Resource {sessionID}/StageTraits
GET: public float[] getStageTraits(String sessionID,String stage)
 */
	private String sessionID;
	private String stageName;

	public StageTraitsResource(Context context, Request request, Response response) {
		super(context, request, response);
		this.sessionID = (String) request.getAttributes().get("sessionID");
		this.stageName = (String) request.getAttributes().get("stage");
		
		// Cheap URL decoding.  Just handle space characters
		//this.stageName = this.stageName.replaceAll("%20", " ");
		try {
			this.stageName = URLDecoder.decode(this.stageName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
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

		 try {
			 	System.out.println("GET StageTraits");
			 	System.out.println("Stage Name:" + stageName);
			 
			 	float[] stageTraits  = ServerData.janus.getStageTraits(sessionID, stageName);
			 	JSONArray jsStageTraits = new JSONArray();
			 	for (float f: stageTraits)
			 		jsStageTraits.put(String.valueOf(f));
				result = new JsonRepresentation(jsStageTraits );
			} catch (RemoteException e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (SessionLogoutException e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
			System.out.println("DONE");
			return result;
	 }
	 
/*	 private Representation representError(Variant variant, ErrorMessage em) throws ResourceException {
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
