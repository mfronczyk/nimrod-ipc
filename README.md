nimrod-ipc
==========

A simple API for interprocess communication for java using ZeroMQ.

ZeroMQ supports many advanced messaging scenarios. This API covers two major use-cases which I find useful when developing enterprise/distributed applications whilst requiring minimal configuration and code to use these features :

1) Remote method invocation : A thread in running in one jvm (client process) can call any exposed method in any exposed class running in another jvm (server process) passing to it any required arguments for the target method. The calling application thread is blocked until a response object is returned by the called method or an exception is thrown or, if a timeout is supplied, the timeout expires. Any number of calling threads in the client jvm can be in progress whilst the server jvm is servicing any number of client calls. The calls are multiplexed from the client process over one pre-existing transport connection to the server and dispatched to and handled by descrete worker threads in server to maximise concurrency. The size of thread pools are configurable. The response is not dictated by FIFO to the server but rather how long the specific call takes to complete in the server. A jvm process can be a client to any number of server processes or a server to any number of different clients processes or a mixture of both. A server process can make services (methods) available on one or more transports. Applicable ZeroMQ Transports covered are "ipc" for processes co-located on same physical (or virtual) server or "tcp" if processes are running on different servers. Leveraging ZeroMQ features it does not matter the order that the processes are instantiated. A client process will connect to a server process when the server process becomes available. This might be immediately if the server process is already running or later if the server process is yet to start. Appropriate exceptions will be thrown during time that server process is unavailable. Whilst the underlying ZeroMQ messaging is asynchronous the API RMI calls are synchronous in their nature because thats how I have contrived it but that said they can behave in an asynchronous manner if desired. If the target method running in the client process side delegates the activity to another application thread and returns immediately this is effectively implementing a form of asynchronous messaging with gauranteed delivery. 

2) Publish/Subscribe : From a publisher instance any thread can simply and directly publish on any subject a message payload containing any serializable object. A subscriber instance can dynamically register (or unregister) any number of callbacks on any subject and directly receive the deserialzed object. On the subscriber side messages for the same subject will be dispatched to a temporarily dedicated thread for that subject via a queue executor therefore maintaining the strict sequence the messages arrive in. Other messages arriving at the same time but for different subjects will be dispatched to corresponding threads as appropriate according to same rules but constrained by threadpool configuration. Two types of queue executors are available. The sequential executor will receive and process all messages received for a given subject. The conflating executor will receive and process the latest message received for a given subject skipping any intermediate ones that arrived during the processing of the current message skipping to the latest on completion. One-to-many and many-to-one patterns are supported. Simple wildcard subscription is available. If a subscriber subscribes to aaaa.bbbb.* it will receive messages published on aaaa.bbbb.cccc and aaaa.bbbb.cccc.dddd but not on aaaa.cccc etc. Note that threading on the queue executor is done on the subscription subject and all messages for a wildcard subscription will be directed to the same thread if there is a queue. Currently latest values will not be re-published following a wildcard subscription (See note below describing agent process). I am considering the usefulness of triggering a republish of all values whose subject match the wildcard but I am not sure this is a good idea.

The main challenge using ZeroMQ is the single thread restriction on socket interactions. Most of the work in developing this API is around the desire to have the freedom and ease of multi-threading typical in java application code but leveraging all the effficiencies of the ZeroMQ messaging transport. Communication from the application threads into the main, singular ZeroMQ thread and back out to the corresponding waiting application thread is through ZeroMQ itself via an inproc socket type. This is achieved by using a combination of standard java concurrent programming techniques and by following ZeroMQ standard messaging patterns. Typically this takes the form of a central, single-threaded pump or eventloop interacting with 2 sockets (except for Publisher side of PubSub that only needs 1), one internal socket (REQ/ROUTER) and one external (ROUTER/DEALER for RMI and PUB/SUB for, unsuprisingly, PubSub) and using ZMQ poller. In decoupling the application threads from the main communication thread another benefit is also mitigation of the common problem of slow-consumer and hence reduced likelyhood of dropping messages. Messages are dispatched quickly away from the ZeroMQ thread and buffered in API managed queues. Messages are only dropped because you allow them to be i.e. through choosing a conflating queue executor.

Serializers are optional and pluggable and if there is more than one configured then the type to use for an RMI call or message publish/subscribe can optionally be supplied in the API calls. I have used kryo and protobuf. If the payload/return type class specified is a byte[] then no serialization/deserialization will take place in the API layer.

A further optional extension or mode is available. By running a supplied agent process two extra functions become available :

1) Latest value re-publish on initial subscription. So when a subject is initially subscribed to by a subscriber process the publisher of that subject will re-publish its most recent value so the subscriber process immediately sees the current value rather than having to wait for the next change.

2) Any start or stop of either a RMI service (server or client side) or a PubSub service (publisher or subscriber side) will be communicated to all other processes running the API which are configured to connect to the agent and have registered callbacks expressing an interest in a particular services' start or stop event.

Planned enhancements : 
1) Implementing 'futures' using a hybrid of RMI and PubSub. The new API call will be a RMI call with a callback method parameter also supplied. The API call on the client side will return immediately but generate a unique response subject and subscribe on it prior to passing it along with usual RMI parameters to server side. On completion of the RMI call the server side will publish the result using the unique subject provided when complete. The client side will receive the result and call the callback method originally provided passing the result and unsubscribe. The latest value cache logic will be bypassed or a time-to-live logic applied to stop cache filling up with old 'futures' values.
2) Creating an annotation processor to allow annotations to be used directly on exposed methods on server side code to remove the need the boiler-plate code currently required to unmarshall the target RMI method parameters and wrap the actual target method on the server side and marshal back the response object. 

Prerequisites :
Native libraries for ZeroMQ built/installed and available to jvm via java.library.path. Similarly, native libraries for java bindings for ZeroMQ for the target deployment environment. Excellent documentation/instructions along with downloads are available at http://zeromq.org/intro:get-the-software and http://zeromq.org/bindings:java.
I have developed/tested against Stable Release 4.0.4.
In theory jeromq https://github.com/zeromq/jeromq should work also as an alternative to the native version, but I have yet to test to confirm.

An explanation of the library name :
I am a freelance software engineer. Nimrod Technology Services is the name of my incorporated company in Canada. Amongst other things Nimrod is the name of an aircraft my father flew in whilst serving in the RAF. Nimrod is also term coined by Bugs Bunny from loony tunes and has come to mean stupid or simple-minded...and I like that association and irony in that I am striving to keep api's as simple as possible in their usage, hiding away the complexity. Nimrod is also the name of a programming language and appologies to those who have arrived here thinking that this is a library or extension to the actual Nimrod language.
