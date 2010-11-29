public class KernelEvent {
	static public enum EventType {
		STEP, REMOVED, ADDED, ERROR, INFO
	};

	private EventType type;
	private String message;
	private int data;

	public KernelEvent(EventType t, String m, int d) {
		type = t;
		message = new String(m);
		data = d;
	}

	public String getMessage() {
		return message;
	}

	public EventType getType() {
		return type;
	}
	
	public int getData() {
		return data;
	}
}
