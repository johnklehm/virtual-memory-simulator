import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class ConfigFile {
	private static final String ls = System.getProperty("line.separator");
	private boolean doStdoutLog;
	private boolean doFileLog;
	private byte addressradix = 16;
	private int virtPageNum;
	private int physicalPageCount = -1;
	private int block;
	private long address_limit;
	private String outputFileName;
	Vector<Page> memVector;

	public ConfigFile(String fileName) {
		outputFileName = "tracefile";
		doFileLog = false;
		virtPageNum = 63;
		doStdoutLog = false;
		block = (int) Math.pow(2, 12);
		memVector = new Vector<Page>();
		parse(fileName);
	}

	public long getAddressLimit() {
		return address_limit;
	}

	public int getBlockSize() {
		return block;
	}

	public boolean getFileLoggingEnabled() {
		return doFileLog;
	}

	public Vector<Page> getMemory() {
		return memVector;
	}

	public int getNumberVirtualPages() {
		return virtPageNum;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public int getPhysicalPageCount() {
		return physicalPageCount;
	}

	public boolean getStdoutLogginEnabled() {
		return doStdoutLog;
	}

	public void parse(String config) {
		String tmp = null;
		String line;
		File f;
		long high = 0;
		long low = 0;
		int i = 0;
		int id = 0;
		int currentPhysicalPage = 0;
		int inMemTime = 0;
		byte R = 0;
		byte M = 0;

		int lastTouchTime = 0;

		address_limit = (block * (virtPageNum + 1)) - 1;

		if (config != null) {
			f = new File(config);

			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(f)));
				while ((line = in.readLine()) != null) {
					if (line.startsWith("numphyspages")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
						}

						physicalPageCount = Common.s2i(tmp);

						if (physicalPageCount < 2 || physicalPageCount > 63) {
							System.out
									.println("MemoryManagement: physicalPageCount out of bounds.");
							System.exit(-1);
						}
					}
				}
				in.close();
			} catch (IOException e) {
				System.out.println("Error parsing numphyspages setting: "
						+ e.getMessage());
			}

			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(f)));
				while ((line = in.readLine()) != null) {
					if (line.startsWith("numpages")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
							virtPageNum = Common.s2i(st.nextToken()) - 1;
							if (virtPageNum < 2 || virtPageNum > 63) {
								System.out
										.println("MemoryManagement: numpages out of bounds.");
								System.exit(-1);
							}
							address_limit = (block * (virtPageNum + 1)) - 1;
						}
					}
				}
				in.close();
			} catch (IOException e) { /* Handle exceptions */
			}

			for (i = 0; i <= virtPageNum; i++) {
				high = (block * (i + 1)) - 1;
				low = block * i;
				memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
			}
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(f)));
				while ((line = in.readLine()) != null)

				{
					if (line.startsWith("memset")) {
						StringTokenizer st = new StringTokenizer(line);
						st.nextToken();
						while (st.hasMoreTokens()) {
							id = Common.s2i(st.nextToken());
							tmp = st.nextToken();
							if (tmp.startsWith("x")) {
								currentPhysicalPage = -1;
							} else {
								currentPhysicalPage = Common.s2i(tmp);
							}
							if ((0 > id || id > virtPageNum)
									|| (-1 > currentPhysicalPage || currentPhysicalPage > (physicalPageCount))) {
								System.out
										.printf(
												"id:%d curPhys:%d virtPageNum:%d phyPageCount:%d%s",
												id, currentPhysicalPage,
												virtPageNum, physicalPageCount,
												ls);
								System.out
										.println("MemoryManagement: Invalid page value in "
												+ config
												+ " "
												+ id
												+ ls
												+ "Line:" + line);
								System.exit(-1);
							}
							R = Common.s2b(st.nextToken());
							if (R < 0 || R > 1) {
								System.out
										.println("MemoryManagement: Invalid R value in "
												+ config);
								System.exit(-1);
							}
							M = Common.s2b(st.nextToken());
							if (M < 0 || M > 1) {
								System.out
										.println("MemoryManagement: Invalid M value in "
												+ config);
								System.exit(-1);
							}
							inMemTime = Common.s2i(st.nextToken());
							if (inMemTime < 0) {
								System.out
										.println("MemoryManagement: Invalid inMemTime in "
												+ config);
								System.exit(-1);
							}
							lastTouchTime = Common.s2i(st.nextToken());
							if (lastTouchTime < 0) {
								System.out
										.println("MemoryManagement: Invalid lastTouchTime in "
												+ config);
								System.exit(-1);
							}
							Page page = memVector.elementAt(id);
							page.physical = currentPhysicalPage;
							page.R = R;
							page.M = M;
							page.inMemTime = inMemTime;
							page.lastTouchTime = lastTouchTime;
						}
					}
					if (line.startsWith("enable_logging")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							if (st.nextToken().startsWith("true")) {
								doStdoutLog = true;
							}
						}
					}
					if (line.startsWith("log_file")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
						}
						if (tmp.startsWith("log_file")) {
							doFileLog = false;
							outputFileName = "tracefile";
						} else {
							doFileLog = true;
							doStdoutLog = false;
							outputFileName = tmp;
						}
					}
					if (line.startsWith("pagesize")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
						}

						block = Common.s2i(tmp);
						address_limit = (block * (virtPageNum + 1)) - 1;
						System.out.println("Block size " + block + "Limit: "
								+ address_limit);

						if (block < 64 || block > Math.pow(2, 26)) {
							System.out
									.println("MemoryManagement: pagesize is out of bounds");
							System.exit(-1);
						}
						for (i = 0; i <= virtPageNum; i++) {
							Page page = memVector.elementAt(i);
							page.high = (block * (i + 1)) - 1;
							page.low = block * i;
						}
					}
					if (line.startsWith("addressradix")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
							tmp = st.nextToken();
							addressradix = Byte.parseByte(tmp);
							if (addressradix < 0 || addressradix > 20) {
								System.out
										.println("MemoryManagement: addressradix out of bounds.");
								System.exit(-1);
							}
						}
					}
				}
				in.close();
			} catch (IOException e) { /* Handle exceptions */
			}
		}
	}
}
