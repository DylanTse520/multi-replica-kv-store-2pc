// Server.java: the server interface

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.util.UUID;

public interface Server extends Remote {

    // The method to ask this server to operate on its store
    StoreOperationResult operateStore(UUID uuid, StoreOperation operation) throws RemoteException,
            ServerNotActiveException;

    // The method to prepare this server to operate on its store in a two phase commit
    void prepareOperation(UUID uuid, StoreOperation operation, Integer initPort) throws RemoteException,
            ServerNotActiveException;

    // The method to ask this server to commit the operation in a two phase commit
    void commitOperation(UUID uuid, Integer initPort) throws RemoteException,
            ServerNotActiveException;

    // The method to ack to this server after preparation or commitment
    void ack(UUID uuid, String ackType, Integer initPort) throws RemoteException,
            ServerNotActiveException;

}
