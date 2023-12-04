// RMIServer.java: A key-value store server program using RMI.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class RMIServer {

    public static void main(String[] args) throws IOException {
        // Prompt user to input connection information
        Helper.log("Input five port numbers for the servers, seperated with space. For example: \"1001 1002 1003 1004" +
                " 1005\"");
        // To enable user input from console using BufferReader
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        // Reading connection info using readLine
        String input = reader.readLine();
        // Split the inputs
        String[] inputSplits = input.split(" ");
        // If not enough ports
        if (inputSplits.length < 5) {
            // Tell user not enough ports
            Helper.log("Need to have five ports and input does not have five ports. Please check your input and try again.");
            throw new RuntimeException();
        }

        // To parse the five port numbers
        Integer[] ports = new Integer[5];
        for (int i = 0; i < 5; i++) {
            try {
                ports[i] = Integer.parseInt(inputSplits[i]);
            } catch (NumberFormatException e) {
                // Tell user not wrong inputs
                Helper.log("The input cannot be parsed as integers. Please check your input and try again.");
                throw new RuntimeException(e);
            }
        }

        // Init the servers
        for (int i = 0; i < 5; i++) {
            try {
                // Get other ports list
                ArrayList<Integer> otherPorts = new ArrayList<>(List.of(ports));
                otherPorts.remove(ports[i]);
                // New the Server class object
                Server server = new ServerImpl(ports[i], otherPorts);
                // Create the registry
                Registry registry = LocateRegistry.createRegistry(ports[i]);
                // Bind the object in the registry
                registry.bind("RMIServer", server);
            } catch (AlreadyBoundException e) {
                // Prompt user with input error
                Helper.log("Cannot connect to port number " + ports[i] + ", please check your input and try again.");
                throw new RuntimeException(e);
            }
        }

        // When done, close the connection and exit
        reader.close();
    }

}