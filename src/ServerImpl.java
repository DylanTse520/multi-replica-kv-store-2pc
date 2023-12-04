// ServerImpl.java: the class implementing the server interface

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Collections.synchronizedMap;

// Extending UnicastRemoteObject to allow RMI registry
public class ServerImpl extends UnicastRemoteObject implements Server {

    // The read write lock to allow synchronization
    ReadWriteLock rwl;

    // The key value store
    HashMap<String, String> keyValueStore;

    // The port number for this server
    Integer port;

    // The port numbers of other servers
    ArrayList<Integer> otherPorts;

    // The requests pending processing
    Map<UUID, StoreOperation> pendingRequests;

    // The preparing phase responses
    Map<UUID, Map<Integer, Boolean>> prepareResponses;

    // The committing phase responses
    Map<UUID, Map<Integer, Boolean>> commitResponses;

    // Write explicit constructor to declare the RemoteException exception
    public ServerImpl(Integer port, ArrayList<Integer> otherPorts) throws RemoteException {
        super();
        this.keyValueStore = new HashMap<>();
        this.port = port;
        this.otherPorts = otherPorts;
        this.rwl = new ReentrantReadWriteLock();
        this.pendingRequests = synchronizedMap(new HashMap<>());
        this.prepareResponses = synchronizedMap(new HashMap<>());
        this.commitResponses = synchronizedMap(new HashMap<>());
    }

    // Get the value from the key value store
    private StoreOperationResult getValue(String key) throws RemoteException {
        // Lock the lock before read
        rwl.readLock().lock();
        // Get the value from the store
        String value = keyValueStore.get(key);
        // Creating the result object
        StoreOperationResult result;
        if (value == null) {
            // If the key is not in the store
            result = new StoreOperationResult(false, "Key does not exist in map");
        } else {
            // Otherwise return the value
            result = new StoreOperationResult(true, value);
        }
        Helper.log(result.toString());
        // Unlock the lock after read
        rwl.readLock().unlock();
        return result;
    }

    // Put the value into the key value store
    private StoreOperationResult putValue(String key, String value) throws RemoteException {
        // Lock the lock before write
        rwl.writeLock().lock();
        // Store the key value in the store
        keyValueStore.put(key, value);
        // Creating the result object
        StoreOperationResult result = new StoreOperationResult(true, "Put " + key + ":" + value);
        Helper.log(result.toString());
        // Unlock the lock after write
        rwl.writeLock().unlock();
        return result;
    }

    // Delete the value from the key value store
    private StoreOperationResult deleteValue(String key) throws RemoteException {
        // Lock the lock before write
        rwl.writeLock().lock();
        // Creating the result object
        StoreOperationResult result;
        if (keyValueStore.containsKey(key)) {
            // If the key is in the store, remove the key
            keyValueStore.remove(key);
            result = new StoreOperationResult(true, "Deleted " + key);
        } else {
            // Otherwise log error
            result = new StoreOperationResult(false, "Key does not exist in map");
        }
        Helper.log(result.toString());
        // Unlock the lock after write
        rwl.writeLock().unlock();
        return result;
    }

    // To create the registry and prepare a server
    private void twoPhaseCommitServer(UUID uuid, StoreOperation operation, Integer desPort, String phase) throws RemoteException, ServerNotActiveException {
        try {
            // Get the registry
            Registry registry = LocateRegistry.getRegistry("localhost", desPort);
            // Getting the Sort class object
            Server server = (Server) registry.lookup("RMIServer");
            if (Objects.equals(phase, "prepare")) {
                // Prepare other server
                server.prepareOperation(uuid, operation, this.port);
            } else {
                // Ask other server to commit
                server.commitOperation(uuid, this.port);
            }
        } catch (NotBoundException e) {
            Helper.log(this.port + " failed to " + operation + " " + desPort + ".");
        }
    }

    // To check if other servers send prepare ack
    private Boolean checkAck(UUID uuid, StoreOperation operation, String phase) throws RemoteException, ServerNotActiveException {
        // The retry attempts
        int retryAttempt = 3;

        // Before consuming all retry attempts
        while (retryAttempt != 0) {
            // Count the number of ack
            int ackCount = 0;
            if (Objects.equals(phase, "prepare")) {
                // See all other servers
                Map<Integer, Boolean> responses = prepareResponses.get(uuid);
                for (Integer port : otherPorts) {
                    if (responses.get(port)) {
                        // If got ack
                        ackCount++;
                    } else {
                        // Otherwise prepare them again
                        twoPhaseCommitServer(uuid, operation, port, "prepare");
                    }
                }
            } else {
                // See all other servers
                Map<Integer, Boolean> responses = commitResponses.get(uuid);
                for (Integer port : otherPorts) {
                    if (responses.get(port)) {
                        // If got ack
                        ackCount++;
                    } else {
                        // Otherwise prepare them again
                        twoPhaseCommitServer(uuid, operation, port, "commit");
                    }
                }
            }
            // All Acked
            if (ackCount == 4) {
                return true;
            }

            // Wait for other servers to ack
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                Helper.log(this.port + " cannot wait for ack.");
                throw new RuntimeException(e);
            }
            retryAttempt--;
        }

        // Not all acked
        return false;
    }

    @Override
    public StoreOperationResult operateStore(UUID uuid, StoreOperation operation) throws RemoteException, ServerNotActiveException {
        // Creating the result object
        StoreOperationResult result;
        // Switch depend on operation
        switch (operation.operation()) {
            case "GET" -> {
                // For get operation, get the result directly
                Helper.log(this.port + " received request from " + getClientHost() + " for " + operation + ".");
                result = getValue(operation.key());
            }
            case "PUT", "DELETE" -> {
                Helper.log(this.port + " received request from " + getClientHost() + " for " + operation + ".");
                // Putting the request in the list
                pendingRequests.put(uuid, operation);

                // For put operation, need to complete two phase commitment
                // Prepare phase
                Helper.log(this.port + " is preparing other servers for " + operation + ".");
                // Initialize the response map
                prepareResponses.put(uuid, new HashMap<>());
                // For each port, ask to prepare for operation
                for (Integer port : otherPorts) {
                    prepareResponses.get(uuid).put(port, false);
                    twoPhaseCommitServer(uuid, operation, port, "prepare");
                }
                Helper.log(this.port + " finished preparing other servers for " + operation + ".");
                // Check prepare ack
                Helper.log(this.port + " is checking preparation ack from other servers for " + operation + ".");
                if (!checkAck(uuid, operation, "prepare")) {
                    Helper.log(this.port + " didn't receive all ack from other servers for " + operation + ".");
                    result = new StoreOperationResult(false, "Other server didn't prepare.");
                    return result;
                }

                // Commit phase
                Helper.log(this.port + " is asking other servers to commit for " + operation + ".");
                // Initialize the response map
                commitResponses.put(uuid, new HashMap<>());
                // For each port, ask to commit for operation
                for (Integer port : otherPorts) {
                    commitResponses.get(uuid).put(port, false);
                    twoPhaseCommitServer(uuid, operation, port, "commit");
                }
                Helper.log(this.port + " finished asking other servers to commit for " + operation + ".");
                // Check commit ack
                Helper.log(this.port + " is checking commitment ack from other servers for " + operation + ".");
                if (!checkAck(uuid, operation, "commit")) {
                    Helper.log(this.port + " didn't receive all ack from other servers for " + operation + ".");
                    result = new StoreOperationResult(false, "Other server didn't commit.");
                    return result;
                }
                if (operation.operation().equals("PUT")) {
                    result = putValue(operation.key(), operation.value());
                } else {
                    result = deleteValue(operation.key());
                }
            }
            default -> throw new RuntimeException();
        }
        return result;
    }

    @Override
    public void prepareOperation(UUID uuid, StoreOperation operation, Integer initPort) throws RemoteException, ServerNotActiveException {
        Helper.log(this.port + " received preparation request from " + initPort + " for " + operation + ".");
        // If it's first time request, put it in the pending list
        if (!pendingRequests.containsKey(uuid)) {
            Helper.log(this.port + " is preparing" + " for " + operation + ".");
            pendingRequests.put(uuid, operation);
        }
        // Ack the preparation phase
        try {
            // Get the registry
            Registry registry = LocateRegistry.getRegistry("localhost", initPort);
            // Getting the Sort class object
            Server server = (Server) registry.lookup("RMIServer");
            server.ack(uuid, "prepare", port);
            Helper.log(this.port + " acked" + " for " + operation + ".");
        } catch (NotBoundException e) {
            Helper.log(this.port + " failed to prepare for " + operation + ".");
            pendingRequests.remove(uuid);
        }
    }

    @Override
    public void commitOperation(UUID uuid, Integer initPort) throws RemoteException, ServerNotActiveException {
        // Get the operation
        StoreOperation operation = pendingRequests.get(uuid);
        if (operation == null) {
            Helper.log(this.port + " received commitment request.");
            throw new RuntimeException();
        }
        Helper.log(this.port + " received commitment request from " + initPort + " for " + operation + ".");
        // Execute the operation
        switch (operation.operation()) {
            case "PUT" -> putValue(operation.key(), operation.value());
            case "DELETE" -> deleteValue(operation.key());
            default -> throw new RuntimeException();
        }
        // Remove the request for this operation
        pendingRequests.remove(uuid);
        // Ack the commitment phase
        try {
            // Get the registry
            Registry registry = LocateRegistry.getRegistry("localhost", initPort);
            // Getting the Sort class object
            Server server = (Server) registry.lookup("RMIServer");
            server.ack(uuid, "commit", port);
            Helper.log(this.port + " acked" + " for " + operation + ".");
        } catch (NotBoundException e) {
            Helper.log(this.port + " failed to commit for " + operation + ".");
            pendingRequests.remove(uuid);
        }
    }

    @Override
    public void ack(UUID uuid, String ackType, Integer initPort) throws RemoteException {
        // When server is asked to ack for any phase, update the corresponding map
        if (Objects.equals(ackType, "commit")) {
            commitResponses.get(uuid).put(initPort, true);
        } else if (Objects.equals(ackType, "prepare")) {
            prepareResponses.get(uuid).put(initPort, true);
        }
    }

}
