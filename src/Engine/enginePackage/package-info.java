/**
Provides the implementation of the server and the engine.
<p>
Class {@link Janus} is the server implementation.
Class {@link Engine} is the engine implementation.
<p>
<h4>Server</h4>
The server handles sessions for playing a storyworld and rehearsals.
For each session it creates the engine instance that will interact
with storyteller or rehearsal lizard.
<p>
The server has some defenses against denial of service attacks.
This are explained further in the {@link Janus} documentation and
{@link JeriCustomServerSetup}.  

<h4>Engine</h4>

The {@link Engine} works over a storyworld model executing the reaction cycle.
This involves sending and receiving data from the storyteller. The interactions
are encapsulated through the interface {@link EnginePlayerIO}.
<p>
The class {@link StorytellerPlayerIO} implements EnginePlayerIO to provide
the communication with storyteller.
<p>  
The class {@link ResumerIO} implements {@link EnginePlayerIO} to play the story
with a predefined input, most likely recorded during a previous session, and then
handles control to another {@link EnginePlayerIO} instance (e.g. returns control
to a {@link StorytellerPlayerIO}).
<p>
The class {@link PlayerInputRecorder} implements {@link EnginePlayerIO} to play
recorded input. The main difference with {@link ResumerIO} is that the input may
not be known a priori, and new input can be defined while playing input defined 
earlier. This class is used exclusively for rerunning a part of a story by
{@link LogDataCollector}.
<p>
The engine is monitored through the class {@link EngineLogger}.
This class has methods invoked by the engine to report execution of different parts of
the reaction cycle. Thus {@link EngineLogger} can pick which data to collect during a
given run of the engine.
<p>
When collecting detailed log data, the EngineLogger is fed by the 
LogDataCollector with request of specific portions of log data.  
 */
package Engine.enginePackage;