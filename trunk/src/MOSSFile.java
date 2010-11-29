import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class MOSSFile extends TraceFile {
	public MOSSFile(String fName, long addrlim) throws
			IOException, ParseException, NumberFormatException {
		super(fName, addrlim);
		File f = new File(fileName);
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(f)));
		try {
			String line;
			while ((line = in.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);
				String command = st.nextToken();
				Instruction.Type instr;
				
				//skip this line if it's a comment
				if (command.startsWith("//")) {
					continue;
				}
				
				// is the command valid
				if (command.equals("READ")) {
					instr = Instruction.Type.READ;
				} else if (command.equals("WRITE")) {
					instr = Instruction.Type.WRITE;
				} else {
					throw new ParseException(
							String.format("Invalid memory command %s, in %s.",
							command, fileName));
				}
				
				String addressFormat = st.nextToken();
				long address = 0;
				if (addressFormat.equals("bin")) {
					address = Long.parseLong(st.nextToken(), 2);
				} else if (addressFormat.equals("oct")) {
					address = Long.parseLong(st.nextToken(), 8);
				} else if (addressFormat.equals("hex")) {
					address = Long.parseLong(st.nextToken(), 16);
				} else if (addressFormat.equals("dec")) {
					address = Long.parseLong(st.nextToken());
				} else if (addressFormat.equals("random")) {
					address = Common.randomLong(addressLimit);
				} else { // guess decimal address as last resort
					address = Long.parseLong(addressFormat);
				}

				// is the address valid
				if (address < 0 || address > addressLimit) {
					throw new ParseException(String.format(
							"Address: %x out of range in %s. Max address %x.",
							address, fileName, addressLimit));
				}

				instructions.add(new Instruction(instr, address));
			}
		} finally {
			in.close();
		}
	}
}
