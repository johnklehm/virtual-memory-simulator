import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class DINFile extends TraceFile {
	public DINFile(String fName, long addrlim) throws
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
				
				//skip this line if it's a comment
				if (command.startsWith("//")) {
					continue;
				}
				
				// is the command valid
				Instruction.Type instr;
				if (command.equals("1")) {
					instr = Instruction.Type.WRITE;
				} else if (command.equals("2")) {
					instr = Instruction.Type.READ;
				} else if (command.equals("0")) {
					instr = Instruction.Type.READ;
				} else {
					throw new ParseException(
							String.format("Invalid memory command %s, in %s.",
							command, fileName));
				}

				long address = Long.parseLong(st.nextToken(), 16);

				// is the address valid? if not then compress it
				if (address < 0 || address > addressLimit) {
					address %= addressLimit;
				}

				instructions.add(new Instruction(instr, address));
			}
		} finally {
			in.close();
		}
	}
}
