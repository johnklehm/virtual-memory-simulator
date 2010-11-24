import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class MOSSFile {
	private String fileName;
	private static final String ls = System.getProperty("line.separator");
	private Vector<Instruction> instructVector;
	private long address_limit;

	public MOSSFile(String fName, long addrlim) {
		fileName = fName;
		address_limit = addrlim;
		instructVector = new Vector<Instruction>();
		parse();
	}

	public Vector<Instruction> getInstructions() {
		return instructVector;
	}

	private void parse() {
		String line;
		String tmp = null;
		String command = "";

		// parse memory file
		File f = new File(fileName);
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(f)));
			int lcount = 0;
			while ((line = in.readLine()) != null) {
				lcount++;
				if ((lcount % 100) == 0) {
					System.out.printf("Line %d%s", lcount, ls);
				}

				if (line.startsWith("READ") || line.startsWith("WRITE")) {
					if (line.startsWith("READ")) {
						command = "READ";
					}
					if (line.startsWith("WRITE")) {
						command = "WRITE";
					}
					StringTokenizer st = new StringTokenizer(line);
					tmp = st.nextToken();
					tmp = st.nextToken();
					long addr = 0;

					if (tmp.startsWith("random")) {
						instructVector.addElement(new Instruction(command,
								Common.randomLong(address_limit)));
					} else {
						if (tmp.startsWith("bin")) {
							addr = Long.parseLong(st.nextToken(), 2);
						} else if (tmp.startsWith("oct")) {
							addr = Long.parseLong(st.nextToken(), 8);
						} else if (tmp.startsWith("hex")) {
							addr = Long.parseLong(st.nextToken(), 16);
						} else {
							addr = Long.parseLong(tmp);
						}

						if (0 > addr || addr > address_limit) {
							System.out
									.printf(
											"MemoryManagement: %x , Address out of range in %s.  Max address %x.",
											addr, fileName, address_limit);
							System.exit(-1);
						}
						instructVector
								.addElement(new Instruction(command, addr));
					}
				}
			}
			in.close();
		} catch (IOException e) { /* Handle exceptions */
		}
	}
}
