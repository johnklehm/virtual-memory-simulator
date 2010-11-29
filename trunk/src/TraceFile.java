import java.util.ArrayList;
import java.util.List;

public class TraceFile {
	protected static final String ls = System.getProperty("line.separator");
	protected List<Instruction> instructions;
	protected String fileName;
	protected long addressLimit;
	
	public TraceFile (String fName, long addrlim) {
		fileName = fName;
		addressLimit = addrlim;
		instructions = new ArrayList<Instruction>();
	}
	
	public List<Instruction> getInstructions() {
		return instructions;
	}
	
	public class ParseException extends Exception {
		private static final long serialVersionUID = -579688230792667518L;

		public ParseException(String message) {
			super(message);
		}
	}
}
