package Restlet;

import org.restlet.resource.Resource;

public class ActorBGDataResource extends Resource {
/*
 * Resource {sessionID}/ActorBGData/{actor}
GET: public BgItemData getActorBgData(String sessionID,String actorName)
				throws RemoteException, SessionLogoutException;
 */
	// this might be better implemented as a common resource, unique to each storyworld
/* BgitemData data type
 * 	private String label;
	private String description;
	private ImageGetter imageGetter;	
 */
	// Note.. return the image file name instead of an imagegetter
	// post all images in a site accessible by modifying the image file name
	// actor.getLabel(),actor.getDescription(), actor.getImageName()
}
