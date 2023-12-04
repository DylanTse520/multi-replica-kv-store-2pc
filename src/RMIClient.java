// RMIClient.java: A key-value store client program using RMI.

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

public class RMIClient {

    // Get input and server and work with the server accordingly
    private static void handleInput(String input, Server server) throws ServerNotActiveException, RemoteException {
        // Split the operation from the key and value
        String[] inputParts = input.split(" ");
        switch (inputParts[0]) {
            case "PUT":
                // PUT needs key and value
                if (inputParts.length >= 3) {
                    // Store the key value in the store
                    StoreOperationResult result = server.operateStore(UUID.randomUUID(), new StoreOperation("PUT", inputParts[1], inputParts[2]));
                    Helper.log((result.success() ? "Succeeded: " : "Failed: ") + result.value());
                } else {
                    // Not having key or value
                    Helper.log("Failed: Missing key or value from input");
                }
                break;
            case "GET":
            case "DELETE":
                // DELETE needs key
                if (inputParts.length >= 2) {
                    StoreOperationResult result = server.operateStore(UUID.randomUUID(), new StoreOperation(inputParts[0], inputParts[1], ""));
                    Helper.log((result.success() ? "Succeeded: " : "Failed: ") + result.value());
                } else {
                    // Not having key
                    Helper.log("Failed: Missing key from input");
                }
                break;
            default:
                // When the operation name is wrong
                Helper.log("Failed: invalid operation name");
        }
    }

    public static void main(String[] args) throws IOException {
        // Prompt user to input connection information
        Helper.log("Input the hostname or IP address and the port number of the server in the form of <hostname>" +
                " <port number>, for example: \"localhost 1005\".");
        // To enable user input from console using BufferReader
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        // Reading connection info using readLine
        String input = reader.readLine();
        // Splitting the input line to get hostname or IP address and port number
        String[] parts;
        try {
            parts = input.split(" ");
        } catch (PatternSyntaxException e) {
            Helper.log("Cannot parse the input, please try again");
            throw new RuntimeException(e);
        }
        // The first part should be the hostname or IP address
        String host = parts[0];
        // The second part should be the port number
        String port = parts[1];

        Server server;
        try {
            // Get the registry
            Registry registry = LocateRegistry.getRegistry(host, Integer.parseInt(port));
            // Getting the Sort class object
            server = (Server) registry.lookup("RMIServer");
            Helper.log("Connected to server on " + host + ":" + port + ".");
        } catch (NotBoundException e) {
            Helper.log("Cannot connect to the given hostname or IP address and port number due to NotBoundException");
            throw new RuntimeException(e);
        }

        try {
            // Prompt user to choose whether we prepopulate the server
            Helper.log("Input whether you want to prepopulate the server, y/n.");
            // Read user input for prepopulate instruction
            input = reader.readLine();
            if (Objects.equals(input, "y")) {
                // Prepopulate server's key-value store
                Helper.log("Pre-populating server's key-value store.");
                // Read pre-population data
                File prePopulationFile = new File("src/PRE_POPULATION");
                // Create the scanner
                Scanner sc = new Scanner(prePopulationFile);
                // Read each line
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    // Send data to server
                    handleInput("PUT " + line, server);
                }
                sc.close();
                Helper.log("Finished pre-populating server's key-value store.");

                // Complete the minimum operation request
                Helper.log("Completing minimum operation request.");
                // Read minimum operation data
                File operationFile = new File("src/MINIMUM_OPERATION");
                // Create the scanner
                sc = new Scanner(operationFile);
                // Read each line
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    // Send data to server
                    handleInput(line, server);
                }
                sc.close();
                Helper.log("Finished minimum operations.");
            }

            // Prompt user with input instructions
            Helper.log("Input your requests. Separate operation name (PUT, GET or DELETE), key (integers only) " + "and/or value (integers only) by space. For example: PUT 1 10; GET 1; DELETE 1.");
            // Process user input
            String line;
            while ((line = reader.readLine()) != null) {
                // Send data to server
                handleInput(line, server);
            }
        } catch (NumberFormatException e) {
            Helper.log("Failed: Illegal key or value, needed integer");
            throw new RuntimeException(e);
        } catch (ServerNotActiveException e) {
            Helper.log("Failed: Server is not active");
            throw new RuntimeException(e);
        } catch (MalformedURLException murle) {
            Helper.log("Failed: due to MalformedURLException");
            throw new RuntimeException(murle);
        } catch (RemoteException re) {
            Helper.log("Failed: due to RemoteException");
            throw new RuntimeException(re);
        } catch (ArithmeticException ae) {
            Helper.log("Failed: due to ArithmeticException");
            throw new RuntimeException(ae);
        } catch (IOException e) {
            Helper.log("Failed: due to IOException");
            throw new RuntimeException(e);
        } finally {
            // When done, close the connection and exit
            reader.close();
        }
    }

}