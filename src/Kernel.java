import java.lang.Thread;
import java.io.*;
import java.util.*;

public class Kernel extends Thread {

	//virtual pages should always be at least twice physical pages
	private static int virtPageNum = 2;
	private int physicalPageCount = 1;

	private String output;
	private static final String ls = System
			.getProperty("line.separator");
	private String configFileName;
	private String commandFileName;
	private ControlPanel controlPanel;
	private Vector<Page> memVector;
	private Vector<Instruction> instructVector;
	private boolean doStdoutLog = false;
	private boolean doFileLog = false;
	private int block = (int) Math.pow(2,12);
	private long address_limit = 16384;
	public int runs;
	public int runcycles;
	public static byte addressradix = 16;

	public Kernel(String confFileName, String commFileName) {
		configFileName = confFileName;
		commandFileName = commFileName;
		memVector = new Vector<Page>();
		instructVector = new Vector<Instruction>();
		
		init();
	}
	
	private void init() {
		if (configFileName != null) {
			ConfigFile cf = new ConfigFile(configFileName);
			address_limit = cf.getAddressLimit();
			physicalPageCount = cf.getPhysicalPageCount();
			virtPageNum = cf.getNumberVirtualPages();
			output = cf.getOutputFileName();
			doStdoutLog = cf.getStdoutLogginEnabled();
			doFileLog = cf.getFileLoggingEnabled();
			block = cf.getBlockSize();
			// pull predefined address space from the config file
			memVector = cf.getMemory();
			
			System.out.printf(
				"ConfigFile:" + ls +
				"addrLim: %d" + ls +
				"PhysicalPageCount: %d" + ls +
				"VirtualPageCount: %d" + ls +
				"OutFile: %s" + ls +
				"LogStdout: %b" + ls +
				"FileLog: %b" + ls,
				address_limit, physicalPageCount, virtPageNum, output, doStdoutLog, doFileLog);
		}
		
		if (commandFileName != null) {			
			MOSSFile mf = new MOSSFile(commandFileName, address_limit);
			instructVector = mf.getInstructions();
		}
		
		//check memory space for errors
		validate();
	}
	
	public void initGUI() {
		// update gui with physical page settings
		for (int i = 0; i < memVector.size(); i++) {
			Page page = memVector.elementAt(i);
			if (page.physical == -1) {
				controlPanel.removePhysicalPage(i);
			} else {
				controlPanel.addPhysicalPage(i, page.physical);
			}
		}
	}
	
	private void validate() {
		// were done if there's nothing here
		if (memVector.size() == 0 || instructVector.size() == 0) {
			return;
		}
		
		int physical_count = 0;
		int map_count = 0;
		long high = 0;
		long low = 0;
	
		// adjust map count per number of physical pages == -1
		if (map_count < physicalPageCount) {
			for (int i = 0; i < virtPageNum; i++) {
				Page page = memVector.elementAt(i);
				if (page.physical == -1 && map_count < physicalPageCount) {
					page.physical = i;
					map_count++;
				}
			}
		}
		long runcycles = instructVector.size();
		if (runcycles < 1) {
			System.out
					.println("MemoryManagement: no instructions present for execution.");
			System.exit(-1);
		}
		
		//
		for (int i = 0; i < virtPageNum; i++) {
			Page page = memVector.elementAt(i);
			if (page.physical != -1) {
				map_count++;
			}
			for (int j = 0; j < virtPageNum; j++) {
				Page tmp_page = memVector.elementAt(j);
				if (tmp_page.physical == page.physical && page.physical >= 0) {
					physical_count++;
				}
			}
			if (physical_count > 1) {
				System.out
						.println("MemoryManagement: Duplicate physical page's in "
								+ configFileName);
				System.exit(-1);
			}
			physical_count = 0;
		}
		
		// validate instruction boundaries
		for (int i = 0; i < instructVector.size(); i++) {
			high = block * (virtPageNum + 1);
			Instruction instruct = (Instruction) instructVector.elementAt(i);
			if (instruct.addr < 0 || instruct.addr > high) {
				System.out
						.printf(
								"MemoryManagement: Instruction (%s %x) out of bounds. Range: %x %x%s",
								instruct.inst, instruct.addr, low, high, ls);
				System.exit(-1);
			}
		}
	}

	public void setControlPanel(ControlPanel newControlPanel) {
		controlPanel = newControlPanel;
	}

	public void getPage(int pageNum) {
		Page page;
		try {
			page = (Page) memVector.elementAt(pageNum);
		} catch (Exception e) {
			page = new Page(pageNum, 0, (byte)0, (byte)0, 0, 0, 0, 0);
		}
		controlPanel.paintPage(page);
	}

	private void printPageFaultCount() {
		try {
			PrintStream out = new PrintStream(
					new FileOutputStream(output, true));

			out.println("Number of page missed: " + PageFault.faultCount);

			out.println("Total Memory Accesses: " + instructVector.size());
			out.close();
		} catch (IOException e) {
			/* Do nothing */
		}
	}

	private void printLogFile(String message) {
		try {
			PrintStream out = new PrintStream(
					new FileOutputStream(output, true));
			out.println(message);
			out.close();
		} catch (IOException e) {
			/* Do nothing */
		}
	}

	public void run() {
		step();
		while (runs != runcycles) {
			step();
		}
		printPageFaultCount();
	}

	public void step() {
		int i = 0;

		Instruction instruct = instructVector.elementAt(runs);
		controlPanel.instructionValueLabel.setText(instruct.inst);
		controlPanel.addressValueLabel.setText(Long.toString(instruct.addr,
				addressradix));
		getPage(Virtual2Physical.pageNum(instruct.addr, virtPageNum, block));
		if (controlPanel.pageFaultValueLabel.getText() == "YES") {
			controlPanel.pageFaultValueLabel.setText("NO");
		}
		if (instruct.inst.startsWith("READ")) {
			Page page = (Page) memVector.elementAt(Virtual2Physical.pageNum(
					instruct.addr, virtPageNum, block));
			if (page.physical == -1) {
				if (doFileLog) {
					printLogFile("READ "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... page fault");
				}
				if (doStdoutLog) {
					System.out.println("READ "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... page fault");
				}
				PageFault.replacePage(memVector, virtPageNum, Virtual2Physical
						.pageNum(instruct.addr, virtPageNum, block),
						controlPanel);
				controlPanel.pageFaultValueLabel.setText("YES");
			} else {
				page.R = 1;
				page.lastTouchTime = 0;
				if (doFileLog) {
					printLogFile("READ "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... okay");
				}
				if (doStdoutLog) {
					System.out.println("READ "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... okay");
				}
			}
		}
		if (instruct.inst.startsWith("WRITE")) {
			Page page = (Page) memVector.elementAt(Virtual2Physical.pageNum(
					instruct.addr, virtPageNum, block));
			if (page.physical == -1) {
				if (doFileLog) {
					printLogFile("WRITE "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... page fault");
				}
				if (doStdoutLog) {
					System.out.println("WRITE "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... page fault");
				}
				PageFault.replacePage(memVector, virtPageNum, Virtual2Physical
						.pageNum(instruct.addr, virtPageNum, block),
						controlPanel);
				controlPanel.pageFaultValueLabel.setText("YES");
			} else {
				page.M = 1;
				page.lastTouchTime = 0;
				if (doFileLog) {
					printLogFile("WRITE "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... okay");
				}
				if (doStdoutLog) {
					System.out.println("WRITE "
							+ Long.toString(instruct.addr, addressradix)
							+ " ... okay");
				}
			}
		}
		for (i = 0; i < virtPageNum; i++) {
			Page page = (Page) memVector.elementAt(i);
			if (page.R == 1 && page.lastTouchTime == 10) {
				page.R = 0;
			}
			if (page.physical != -1) {
				page.inMemTime = page.inMemTime + 10;
				page.lastTouchTime = page.lastTouchTime + 10;
			}
		}
		runs++;
		controlPanel.timeValueLabel.setText(Integer.toString(runs * 10)
				+ " (ns)");
	}

	public void reset() {
		memVector.removeAllElements();
		instructVector.removeAllElements();
		controlPanel.statusValueLabel.setText("STOP");
		controlPanel.timeValueLabel.setText("0");
		controlPanel.instructionValueLabel.setText("NONE");
		controlPanel.addressValueLabel.setText("NULL");
		controlPanel.pageFaultValueLabel.setText("NO");
		controlPanel.virtualPageValueLabel.setText("x");
		controlPanel.physicalPageValueLabel.setText("0");
		controlPanel.RValueLabel.setText("0");
		controlPanel.MValueLabel.setText("0");
		controlPanel.inMemTimeValueLabel.setText("0");
		controlPanel.lastTouchTimeValueLabel.setText("0");
		controlPanel.lowValueLabel.setText("0");
		controlPanel.highValueLabel.setText("0");
		init();
	}
}
