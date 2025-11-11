package est.DreamDecode.exception;

public class DreamNotFoundException extends CustomException {

    public DreamNotFoundException(Long dreamId) {
        super("Dream not found with id " + dreamId);
    }

    public DreamNotFoundException(String message) {
        super(message);
    }
}

