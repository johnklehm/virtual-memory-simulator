import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Observable;
import java.util.Vector;

public class Kernel extends Observable {
	private int virtualPageCount = 0;
	private int physicalPageCount = 0;

	private String output;
	private static final String ls = System.getProperty("line.separator");
	private String configFileName;
	private Vector<Page> initialMemoryState;
	private Vector<Page> memVector;
	private Vector<Instruction> instructVector;
	private int block = (int) Math.pow(2, 12);
	private long address_limit = 16384;
	private int currentCycle = 0;
	private int totalCycles = 0;
	private byte addressradix = 16;

	public Kernel() {
		memVector = new Vector<Page>();
		instructVector = new Vector<Instruction>();
	}

	public int getAddressRadix() {
		return addressradix;
	}

	public int getCurrentCycle() {
		return currentCycle;
	}

	public int getTotalCycles() {
		return totalCycles;
	}

	public Instruction getInstruction(int n) {
		return instructVector.get(n);
	}

	public Page getPage(int pageNum) {
		return memVector.get(pageNum);
	}

	public int getPhysicalPageCount() {
		return physicalPageCount;
	}

	public int getVirtualPageCount() {
		return virtualPageCount;
	}

	private void setInstructions(Vector<Instruction> v) {
		instructVector = v;
		totalCycles = instructVector.size();
	}

	public boolean loadTrace(String fileName) {
		boolean couldLoad = false;
		MOSSFile tf = new MOSSFile(fileName, address_limit);

		if (tf != null) {
			setInstructions(tf.getInstructions());

			// check memory space for errors
			couldLoad = validate();
		}

		return couldLoad;
	}

	public boolean loadConfig(String fileName) {
		boolean couldLoad = false;
		configFileName = fileName;
		ConfigFile cf = null;
		try {
			cf = new ConfigFile(configFileName);
		} catch (Exception e) {
			e.printStackTrace();
			setChanged();
			notifyObservers(new KernelEvent(KernelEvent.EventType.ERROR,
					e.getMessage(), 0));
		}

		if (cf != null) {
			address_limit = cf.getAddressLimit();
			physicalPageCount = cf.getPhysicalPageCount();
			virtualPageCount = cf.getVirtualPageCount();
			output = cf.getOutputFileName();
			block = cf.getBlockSize();
			initialMemoryState = cf.getMemory();

			initMemory();

			couldLoad = true;
		}

		return couldLoad;
	}

	private void initMemory() {
		for (int i = 0; i < virtualPageCount; ++i) {
			long low = block * i;
			long high = low + block - 1;
			memVector.addElement(new Page(i, -1, (byte) 0, (byte) 0, 0, 0,
					high, low));
		}

		// assign the initial state settings
		for (int i = 0; i < initialMemoryState.size() && i < memVector.size(); ++i) {
			memVector.set(i, initialMemoryState.get(i));
		}

		// load initial memory into physical pages
		int map_count = 0;
		for (int i = 0; i < virtualPageCount && map_count < physicalPageCount; ++i) {
			Page page = memVector.elementAt(i);
			if (page.physical == -1) {
				page.physical = i;
				++map_count;
			}
		}
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

	public void reset() {
		currentCycle = 0;
		memVector.clear();
		initMemory();
	}

	public void run() {
		step();
		while (currentCycle != totalCycles) {
			step();
		}
		printPageFaultCount();
	}

	public void step() {
		Instruction instruct = instructVector.elementAt(currentCycle);
		int replacePageNum = Virtual2Physical.pageNum(instruct.addr,
				virtualPageCount, block);
		Page page = memVector.elementAt(replacePageNum);

		setChanged();
		notifyObservers(new KernelEvent(KernelEvent.EventType.STEP, "",
				replacePageNum));

		String type = "unknown";
		String result = "unknown";
		if (instruct.inst.startsWith("READ")) {
			type = "READ";

			if (page.physical != -1) {
				page.R = 1;
				page.lastTouchTime = 0;
			}
		} else if (instruct.inst.startsWith("WRITE")) {
			type = "WRITE";

			if (page.physical != -1) {
				page.M = 1;
				page.lastTouchTime = 0;
			}
		}

		// if there was a page fault replace the page accordingly
		if (page.physical == -1) {
			result = "page fault";

			int oldestPage = PageFault.replacePage(memVector, virtualPageCount,
					replacePageNum);

			setChanged();
			notifyObservers(new KernelEvent(KernelEvent.EventType.REMOVED, "",
					oldestPage));
			setChanged();
			notifyObservers(new KernelEvent(KernelEvent.EventType.ADDED, "",
					replacePageNum));

		} else {
			result = "okay";
		}

		String instructionInfo = type + " "
				+ Long.toString(instruct.addr, addressradix) + "... " + result;

		setChanged();
		notifyObservers(new KernelEvent(KernelEvent.EventType.INFO,
				instructionInfo, 0));

		// update page statistics (lastTouched and inMemTime)
		for (int i = 0; i < virtualPageCount; ++i) {
			Page p = memVector.elementAt(i);
			if (p.R == 1 && p.lastTouchTime == 10) {
				p.R = 0;
			}
			if (p.physical != -1) {
				p.inMemTime = p.inMemTime + 10;
				p.lastTouchTime = p.lastTouchTime + 10;
			}
		}
		++currentCycle;
	}

	private boolean validate() {
		// we're done if there's nothing look at
		if (memVector.size() == 0 || instructVector.size() == 0) {
			return false;
		}

		// check that we have some memory operations
		totalCycles = instructVector.size();
		if (totalCycles < 1) {
			setChanged();
			notifyObservers(new KernelEvent(KernelEvent.EventType.ERROR,
					"No instructions present for execution.", 0));

			return false;
		}

		// check for duplicate pages
		for (int i = 0; i < virtualPageCount; ++i) {
			Page page = memVector.elementAt(i);

			for (int j = i + 1; j < virtualPageCount; ++j) {
				Page tmp_page = memVector.elementAt(j);
				if (tmp_page.physical == page.physical && page.physical >= 0) {
					setChanged();
					notifyObservers(new KernelEvent(
							KernelEvent.EventType.ERROR,
							"Duplicate physical page's in " + configFileName, 0));

					return false;
				}
			}
		}

		// check if all addresses are within the limits
		long low = 0;
		for (int i = 0; i < instructVector.size(); ++i) {
			Instruction instruct = instructVector.elementAt(i);
			if (instruct.addr < 0 || instruct.addr > address_limit) {
				String errorMessage = String.format(
						"Instruction (%s %x) out of bounds. Range: %x %x%s",
						instruct.inst, instruct.addr, low, address_limit, ls);
				setChanged();
				notifyObservers(new KernelEvent(KernelEvent.EventType.ERROR,
						errorMessage, 0));

				return false;
			}
		}

		return true;
	}
}
