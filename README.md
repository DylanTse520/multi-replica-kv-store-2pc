# Project 3: Multi-Replica Key-Value Store with 2 Phase Commit

## Assignment overview

The purpose of the assignment is to extend what was built in project 2,
which is a multithreaded RPC server that can take multiple client requests
at the same time to perform three basic operations: PUT, GET, and DELETE
in a key-value store. In this assignment, we need to extend project 2 with
two requirements. First, instead of one server, we should now replicate the
server across five distinct servers. Client should be able to perform
PUT and DELETE to any of the servers and GET request from any of the servers
should get consistent data back. Second, to enable consistent PUT and DELETE
request, we need to implement 2 phase commit protocol. So PUT or DELETE request
to one server would be received and committed in other servers as well.

## Technical impression

After evaluated the project requirement, I identified two major differences
between project 2 and project 3, which are replicating five servers and implement
2 phase commit protocol. I first need to find a way of replicating the servers.
Previously I create the RMI registry in a different terminal and the server need
to use the same port. This would be cumbersome and error-prone in project 3.
I would also need to have 10 terminals to create the clusters, which doesn't
seem like a good idea. So this time, I create the RMI registry in code and takes
user input as the port number. This allows me to combine the RMI registry and
the server in one terminal and also allow me to create as many servers as I want
by simply using a for-loop. Now I can create 5 server cluster in one terminal.
After enabling five server cluster, I need to implement the 2 phase commit
protocol. I analyze what I need to do in the part: after a client post a request
to one of the servers, the server should prepare other servers to the same
request. After other server finish preparation, they should send ack back to the
initiating server. Once the initiating server identified all ACKs, it should
ask other servers to commit the request. Other servers should then commit the
request and send ack back to the initiating server. The initiating server should
identify all ACKs to finish the request. With the workflow in mind, I then
update the server interface with needed methods, such as prepareRequest,
commitRequest, ack. In order to handle the 2 phase commit in one place, I also
need to combine my previous three store operation methods as one. I then
implement the methods and test them to see if they are correct.

## Build and run the server cluster and clients

### Build the server and client

Under ```/src``` directory, open a terminal and compile 7 java programs with the following commands:

```shell
javac Helper.java
javac StoreOperation.java
javac StoreOperationResult.java
javac Server.java
javac ServerImpl.java
javac RMIServer.java
javac RMIClient.java
```

Create the jar file to execute with the following commands:

```shell
jar cmf RMIServer.mf RMIServer.jar RMIServer.class Helper.class StoreOperation.class StoreOperationResult.class Server.class ServerImpl.class
jar cmf RMIClient.mf RMIClient.jar RMIClient.class Helper.class StoreOperation.class StoreOperationResult.class Server.class PRE_POPULATION MINIMUM_OPERATION
```

### Run the server cluster and clients

Under ```/src``` directory, open a terminal to start the RMI server cluster and enter five port numbers
for each of the servers with the following commands:

```shell
java -jar RMIServer.jar
1001 1002 1003 1004 1005
```

Open several terminals and navigate to the same directory.
In each terminal, start one RMI client and enter the hostname and port number with the following commands.
This would start one RMI client and connect it to the given server.
You can connect each client to different servers to test the 2 phase commit for them.

```shell
java -jar RMIClient.jar
localhost 1001
```

Then the client would ask if you want to prepopulate the server. Type "y" for yes, otherwise for no.

To put new records in the store, try "PUT 1 10".
To get a value by a key, try "GET 1".
To delete a key, try "DELETE 1".

### MISC
The PRE_POPULATION file is for storing the pre-population records.
The MINIMUM_OPERATION file is for storing the operations to be completed by the clients.
