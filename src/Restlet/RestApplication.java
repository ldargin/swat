package Restlet;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.xml.parsers.ParserConfigurationException;

import Engine.enginePackage.Janus;
import org.restlet.Application;
import org.restlet.Restlet;  
import org.restlet.Router;  
import org.xml.sax.SAXException;

public class RestApplication extends Application {
	
	
    /** 
     * Creates a root Restlet that will receive all incoming calls. 
     */  
    @Override  
    public Restlet createRoot() {  
        // Create a router Restlet that routes each call to a  
        // new instance of HelloWorldResource.  
        Router router = new Router(getContext());  
  
        // Defines only one route
        router.attach("/test", TestResource.class);
        router.attach("/storyteller", SessionResource.class);
        router.attach("/storyteller/{sessionID}", SessionResource.class);
        router.attach("/storyteller/{sessionID}/Story", StoryResource.class);
        router.attach("/storyteller/{sessionID}/CopyrightInfo", CopyrightResource.class);
        router.attach("/storyteller/{sessionID}/Sentence", SentenceResource.class);
        router.attach("/storyteller/{sessionID}/Trigger", TriggerResource.class);
        router.attach("/storyteller/{sessionID}/ActorsUnknownToProtagonist", ActorsUnknownToProtagonistResource.class);
        router.attach("/storyteller/{sessionID}/StagesUnknownToProtagonist", StagesUnknownToProtagonistResource.class);
        router.attach("/storyteller/{sessionID}/PropsUnknownToProtagonist", PropsUnknownToProtagonistResource.class);
        router.attach("/storyteller/{sessionID}/ActorTraits/{actor}", ActorTraitsResource.class);
        router.attach("/storyteller/{sessionID}/StageTraits/{stage}", StageTraitsResource.class);
        router.attach("/storyteller/{sessionID}/PropTraits/{prop}", PropTraitsResource.class);
        //router.attach("/storyteller/{sessionID}/Storybook", StorybookResource.class);
        //router.attach("/storyteller/{sessionID}/ActorBGData/{actor}", ActorBGDataResource.class);
        //router.attach("/storyteller/{sessionID}/StageBGData/{stage}", StageBGDataResource.class);
        //router.attach("/storyteller/{sessionID}/PropBGData/{prop}", PropBGDataResource.class);
        //router.attach("/storyteller/{sessionID}/{relationshipName}/RelationshipValues", RelationshipValuesResource.class);

        try {
			ServerData.janus = new Janus();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//router.attach("session",SessionResource.class);
		
        return router;  
    }  

}
