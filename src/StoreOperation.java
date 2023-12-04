// StoreOperation.java: the class to keep one operation

import java.io.Serializable;

// Implementing Serializable to allow marshalling and unmarshalling
public record StoreOperation(String operation, String key, String value) implements Serializable {

    // Overriding to string method for logging
    @Override
    public String toString() {
        return switch (operation) {
            case "PUT" -> "operation = " + operation + ", key = " + key + ", value = " + value;
            case "GET", "DELETE" -> "operation = " + operation + ", key = " + key;
            default -> "";
        };
    }

}
