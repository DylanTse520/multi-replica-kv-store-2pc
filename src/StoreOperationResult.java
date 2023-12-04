// StoreOperationResult.java: the class to keep result for one store operation, i.e. the status and the returned value

import java.io.Serializable;

// Implementing Serializable to allow marshalling and unmarshalling
public record StoreOperationResult(Boolean success, String value) implements Serializable {

    // Overriding to string method for logging
    @Override
    public String toString() {
        return (success ? "Succeeded: " : "Failed: ") + value;
    }

}
