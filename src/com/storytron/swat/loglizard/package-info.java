/**
Provides the implementation of LogLizard.
<p>
LogLizard is the tool that allows to inspect the workings of the 
engine behind a story. It will show the numbers behind each decision
and behavior. All the data will be expressed in terms of the objects 
the author has constructed in the storyworld.
<p>
The nature of this data, as the nature of the history of executing a program, 
is inherently tree-like. So the server and the client (where LogLizard resides) 
will be passing pieces of a big tree (the Log Tree) from one to the other.
<p>
LogLizard is one of the most complex tools at the implementation level. 
The complexity arises from the fact that a story runs in the server, and 
that it generates so many information that it cannot be transfered without 
severe penalties in bandwidth, memory and performance. 

<h4>Server side</h4>

As the main purpose of LogLizard is answering author queries about what 
happened in a story, only a small fraction of that data is inspected. 
Therefore, the natural solution was to carry from server to client only 
the needed data to answer the user queries.
<p>
This solved the bandwidth problem, but still remained the space and 
performance problems on the server. There is so many data to save on 
the server side, just in the case the author wants to inspect it later, 
that it still required a lot of space and effort to store.
<p>
The solution to the space problem, was to generate the data as requested 
by the author. So the server does not need to store any data, but just waits
for a request of the author to run the story with recorded input and pick 
the interesting observations.
<p>
Now, it only remained the performance problem. It could consume significant 
cpu power to run the story from the beginning up to the point where the 
interesting data was generated. To solve this problem, intermediate engine 
states are saved to disk, and when a request of data arrives, the closest 
state is loaded and the data is collected.
<p>
Saving the intermediate engine states would look like introducing the space 
problem again. But in fact, the engine states are quite compact so it's feasible 
saving a few of them per session.
<p>
Ok, what happens if the story is so long that we run out of space to save the 
intermediate engine states? Well, we just discard some of the older states, so 
we make room for new states. Whenever we run out of space, we really discard at 
once the states at even positions, thus we free up half of the space. 
As repeated, this process tends to make more sparse the states that are at the 
beginning of the story.
<p>
The performance could be bad if the user request a piece of information in the 
first half of the story, but in normal use I think it is unlikely that this be 
the typical case. In general, I believe the author will prefer to run a few steps 
of the story from a saved point, and then she will check LogLizard. If it doesn't
happen like this, I think that the performance behavior of the server will coerce 
the author into using that way. 

<h4>Client side</h4>
Client side of LogLizard is complicated, too.
<p>
The first problem is that the data the user wants to inspect could already need 
more memory than available. So the data obtained from the server is saved to disk 
and loaded from there as the author explores it (class {@link DiskIndexedTree}).
<p>
The other problem is that the server data can arrive in chunks that does not match 
exactly the requests. For instance, LogLizard can ask for branches of events 10 to 20,
 but the server sends first branch 10 to half of branch 15, and then the remaining of 
 branch 15 to branch 20. And the delivery could be further divided in smaller chunks.
<p>
This is so, because each branch can have just 10 nodes or 600000 nodes. Therefore, it 
will be needed for the server to send those nodes in chunks in order to don't make its 
internal buffers explode.
<p>
So once LogLizard finishes processing the first chunk of data, it must be able to resume 
that processing when the next chunk of data arrives, exactly at the same node it have 
previously stopped. For this, the client must keep track of the recursion stacks it uses 
to build its tree branches as the data arrives. All of this is handled in the class 
{@link LogTreeModel}. 
 */
package com.storytron.swat.loglizard;