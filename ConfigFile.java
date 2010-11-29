import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ConfigFile {
	private static final String ls = System.getProperty("line.separator");
	private boolean doStdoutLog;
	private boolean doFileLog;
	private static byte addressradix = 16;
	private int virtualPageCount = 0;
	private int physicalPageCount = 0;
	private int block;
	private long address_limit;
	private String outputFileName;
	List<Page> physicalMemory;

	public ConfigFile(String fileName) throws Exception {
		outputFileName = "tracefile";
		doFileLog = false;
		doStdoutLog = false;
		block = (int) Math.pow(2, 12);
		physicalMemory = new ArrayList<Page>();
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

	public List<Page> getMemory() {
		return physicalMemory;
	}

	public int getVirtualPageCount() {
		return virtualPageCount;
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

	public void parse(String config) throws Exception {
		if (config != null) {
			File f = new File(config);
			BufferedReader in = null;

			// first pass read in all the global settings
			try {
				in = new BufferedReader(new InputStreamReader(
						new FileInputStream(f)));

				String line;
				while ((line = in.readLine()) != null) {
					String tmp = null;

					if (line.startsWith("enable_logging")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							if (st.nextToken().startsWith("true")) {
								doStdoutLog = true;
							}
						}
					} else if (line.startsWith("log_file")) {
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
					} else if (line.startsWith("pagesize")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
						}
						block = Common.s2i(tmp);
						if (block < 2 || block > Math.pow(2, 26)) {
							// TODO throw Exception parse error
							System.out
									.println("MemoryManagement: pagesize is out of bounds");
							System.exit(-1);
						}
					} else if (line.startsWith("addressradix")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
							tmp = st.nextToken();
							addressradix = Byte.parseByte(tmp);
							if (addressradix < 0 || addressradix > 20) {
								// TODO throw Exception parse error
								System.out
										.println("MemoryManagement: addressradix out of bounds.");
								System.exit(-1);
							}
						}
					} else if (line.startsWith("numpages")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
							virtualPageCount = Common.s2i(st.nextToken());
							if (virtualPageCount < 2 || virtualPageCount > 64) {
								// TODO throw Exception parse error
								System.out
										.println("MemoryManagement: numpages out of bounds.");
								System.exit(-1);
							}
						}

					} else if (line.startsWith("numphyspages")) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens()) {
							tmp = st.nextToken();
						}
						physicalPageCount = Common.s2i(tmp);
						if (physicalPageCount < 1 || physicalPageCount > 32) {
							// TODO throw Exception parse error
							System.out
									.println("MemoryManagement: physicalPageCount out of bounds.");
							System.exit(-1);
						}
					}
				}
				in.close();
			} finally {
				if (in != null) {
					in.close();
				}
			}

			address_limit = block * virtualPageCount - 1;

			// second pass grab the memset settings for the initial memory state
			// have to do this after we calculate the address_limit
			try {
				in = new BufferedReader(new InputStreamReader(
						new FileInputStream(f)));

				String line;
				while ((line = in.readLine()) != null) {
					if (line.startsWith("memset")) {
						StringTokenizer st = new StringTokenizer(line);
						st.nextToken();
						String tmp = null;
						while (st.hasMoreTokens()) {
							int currentPhysicalPage = 0;
							int id = Common.s2i(st.nextToken());
							tmp = st.nextToken();
							if (tmp.startsWith("x")) {
								currentPhysicalPage = -1;
							} else {
								currentPhysicalPage = Common.s2i(tmp);
							}
							if ((0 > id || id > virtualPageCount)
									|| (-1 > currentPhysicalPage || currentPhysicalPage > (physicalPageCount))) {
								// TODO throw Exception parse error
								System.out
										.printf(
												"id:%d curPhys:%d virtualPageCount:%d phyPageCount:%d%s",
												id, currentPhysicalPage,
												virtualPageCount,
												physicalPageCount, ls);
								System.out
										.println("MemoryManagement: Invalid page value in "
												+ config
												+ " "
												+ id
												+ ls
												+ "Line:" + line);
								System.exit(-1);
							}
							byte R = Common.s2b(st.nextToken());
							if (R < 0 || R > 1) {
								// TODO throw Exception parse error
								System.out
										.println("MemoryManagement: Invalid R value in "
												+ config);
								System.exit(-1);
							}
							byte M = Common.s2b(st.nextToken());
							if (M < 0 || M > 1) {
								// TODO throw Exception parse error
								System.out
										.println("MemoryManagement: Invalid M value in "
												+ config);
								System.exit(-1);
							}
							int inMemTime = Common.s2i(st.nextToken());
							if (inMemTime < 0) {
								// TODO throw Exception parse error
								System.out
										.println("MemoryManagement: Invalid inMemTime in "
												+ config);
								System.exit(-1);
							}
							int lastTouchTime = Common.s2i(st.nextToken());
							if (lastTouchTime < 0) {
								// TODO throw Exception parse error
								System.out
										.println("MemoryManagement: Invalid lastTouchTime in "
												+ config);
								System.exit(-1);
							}
							physicalMemory.add(new Page(id, currentPhysicalPage, R,
									M, inMemTime, lastTouchTime, address_limit,
									0));
						}
					}
				}
				in.close();
			} finally {
				if (in != null) {
					in.close();
				}
			}
		}
	}
}
