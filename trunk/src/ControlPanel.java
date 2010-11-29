import java.awt.Button;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ControlPanel extends Frame implements Observer, ActionListener {
	private static final long serialVersionUID = 6280534328667165120L;
	Kernel kernel;
	Button runButton;
	Button stepButton;
	Button resetButton;
	Label statusValueLabel = new Label("STOP", Label.LEFT);
	Label timeValueLabel = new Label("0", Label.LEFT);
	Label instructionValueLabel = new Label("NONE", Label.LEFT);
	Label addressValueLabel = new Label("NULL", Label.LEFT);
	Label pageFaultValueLabel = new Label("NO", Label.LEFT);
	Label virtualPageValueLabel = new Label("x", Label.LEFT);
	Label physicalPageValueLabel = new Label("0", Label.LEFT);
	Label RValueLabel = new Label("0", Label.LEFT);
	Label MValueLabel = new Label("0", Label.LEFT);
	Label inMemTimeValueLabel = new Label("0", Label.LEFT);
	Label lastTouchTimeValueLabel = new Label("0", Label.LEFT);
	Label lowValueLabel = new Label("0", Label.LEFT);
	Label highValueLabel = new Label("0", Label.LEFT);
	List<Button> pageButtonList = new ArrayList<Button>();
	List<Label> pageLabelList = new ArrayList<Label>();

	public ControlPanel() {
		super("Virtual Memory Simulator");
		kernel = new Kernel();
		kernel.addObserver(this);
		
		init();
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if (cmd.equals("Step")) {
			step();
		} else if (cmd.equals("Run")) {
			run();
		} else if (cmd.equals("Reset")) {
			reset();
		}
		
		for (int i = 0; i < pageButtonList.size(); ++i) {
			if (cmd.equals(pageButtonList.get(i).getLabel())) {
				updatePageInfoArea(kernel.getPage(i));
				break;
			}
		}
	}
	
	private void run() {
		setStatus("RUN");
		runButton.setEnabled(false);
		stepButton.setEnabled(false);
		resetButton.setEnabled(false);
		kernel.run();
		setStatus("STOP");
		resetButton.setEnabled(true);
	}
	
	private void step() {
		setStatus("STEP");
		kernel.step();
		if (kernel.getTotalCycles() == kernel.getCurrentCycle()) {
			stepButton.setEnabled(false);
			runButton.setEnabled(false);
		}
		setStatus("STOP");
	}
	
	private void exit() {
		setVisible(false);
		dispose();
		System.exit(0);
	}
	
	private class OptionsMenu extends Menu implements ActionListener {
		private static final long serialVersionUID = 5766417785024148675L;

		public OptionsMenu() {
			super("Options");
			add(new MenuItem("Load Settings File")).addActionListener(this);
			add(new MenuItem("Load Memory Trace")).addActionListener(this);
			addSeparator();
			add(new MenuItem("Set Frame Size")).addActionListener(this);
			add(new MenuItem("Set Physical Memory")).addActionListener(this);
			addSeparator();
			add(new MenuItem("Exit")).addActionListener(this);
		}
		
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			
			if (cmd.equals("Load Memory Trace")) {
				loadTrace();
			} else if (cmd.equals("Set Frame Size")) {
				setFrameSize();
			} else if (cmd.equals("Set Physical Memory")) {
				setPhysicalMemory();
			} else if (cmd.equals("Load Settings File")) {
				loadConfig();
			} else if (cmd.equals("Exit")) {
				exit();
			}
		}
	}
	
	private void loadConfig() {
		FileDialog fd = new FileDialog(this, "Choose a Trace File");
		fd.setVisible(true);
		String fileName = fd.getDirectory() + fd.getFile();

		if ((fileName == null) || (!kernel.loadConfig(fileName))) {
			// TODO ERROR
		}
	}
	
	private void loadTrace() {
		FileDialog fd = new FileDialog(this, "Choose a Trace File");
		fd.setVisible(true);
		String fileName = fd.getDirectory() + fd.getFile();
		
		if ((fileName != null) && (kernel.loadTrace(fileName))) {
			int x = 0;
			int y = 25;
			int colWidth = 140;
			int rowHeight = 15;
			int width = 70;
			int height = 15;
			int yPadding = 25;
			int numPages = kernel.getVirtualPageCount();
			drawPageButtons(x, y, colWidth, rowHeight, yPadding, width, height,
				numPages);

			validate();
			
			setEnableKernelButtons(true);
		}
	}
	
	private void setFrameSize() {
		
	}
	
	private void setPhysicalMemory() {
		
	}
	
	public void init() {
		setLayout(null);
		setBackground(Color.white);
		setForeground(Color.black);
		setSize(635, 545);
		setFont(new Font("Courier", 0, 12));

		// build the menu bar
		MenuBar mb = new MenuBar();
		mb.add(new OptionsMenu());
		setMenuBar(mb);		

		// layout variables
		int x;
		int y;
		int colWidth;
		int rowHeight;
		int yPadding;


		x = 0;
		y = 25;
		rowHeight = 25;
		colWidth = 40;
		yPadding = 25;
		
		// kernel controls
		stepButton = new Button("Step");
		stepButton.setBounds(x, y + yPadding, colWidth, rowHeight);
		stepButton.addActionListener(this);
		stepButton.setEnabled(false);
		add(stepButton);
		
		x += colWidth;
		runButton = new Button("Run");
		runButton.setBounds(x, y + yPadding, colWidth, rowHeight);
		runButton.addActionListener(this);
		runButton.setEnabled(false);
		add(runButton);
		
		x+= colWidth;
		resetButton = new Button("Reset");
		resetButton.setBounds(x, y + yPadding, colWidth, rowHeight);
		resetButton.addActionListener(this);
		resetButton.setEnabled(false);
		add(resetButton);

		x = 285;
		rowHeight = 15;
		colWidth = 110;
		yPadding = 25;
		drawInfoButtons(x, y, rowHeight, colWidth, yPadding);
		
		// close with the X button
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});

		// update with physical page settings
		for (int i = 0; i < kernel.getPhysicalPageCount(); ++i) {
			Page page = kernel.getPage(i);
			if (page.physical == -1) {
				removePhysicalPage(i);
			} else {
				addPhysicalPage(i);
			}
		}

		setVisible(true);
	}

	public void updateInstructionInfoArea(Instruction instruct, boolean isFault) {
		instructionValueLabel.setText(instruct.inst);
		addressValueLabel.setText(Long.toString(instruct.addr,
			kernel.getAddressRadix()));

		if (isFault) {
			pageFaultValueLabel.setText("YES");
		} else {
			pageFaultValueLabel.setText("NO");
		}
		
		int curCycle = kernel.getCurrentCycle();
		timeValueLabel.setText(Integer.toString(curCycle * 10) + " (ns)");
	}
	
	public void updatePageInfoArea(Page page) {
		virtualPageValueLabel.setText(Integer.toString(page.id));
		physicalPageValueLabel.setText(Integer.toString(page.physical));
		RValueLabel.setText(Integer.toString(page.R));
		MValueLabel.setText(Integer.toString(page.M));
		inMemTimeValueLabel.setText(Integer.toString(page.inMemTime));
		lastTouchTimeValueLabel.setText(Integer.toString(page.lastTouchTime));
		lowValueLabel.setText(Long.toString(page.low, kernel.getAddressRadix()));
		highValueLabel.setText(Long.toString(page.high, kernel.getAddressRadix()));
	}

	public void addPhysicalPage(int virtPageNum) {
		if (virtPageNum < pageLabelList.size()) {
			pageLabelList.get(virtPageNum).setText("page " +
				kernel.getPage(virtPageNum).physical);
		}
	}
	
	public void removePhysicalPage(int virtPageNum) {
		if (virtPageNum < pageLabelList.size()) {
			pageLabelList.get(virtPageNum).setText(null);
		}
	}

	private void reset() {
		statusValueLabel.setText("STOP");
		timeValueLabel.setText("0");
		instructionValueLabel.setText("NONE");
		addressValueLabel.setText("NULL");
		pageFaultValueLabel.setText("NO");
		virtualPageValueLabel.setText("x");
		physicalPageValueLabel.setText("0");
		RValueLabel.setText("0");
		MValueLabel.setText("0");
		inMemTimeValueLabel.setText("0");
		lastTouchTimeValueLabel.setText("0");
		lowValueLabel.setText("0");
		highValueLabel.setText("0");

		kernel.reset();
		
		for(Label l : pageLabelList) {
			l.setText(null);
		}
		
		setEnableKernelButtons(true);
	}

	public void setStatus(String status) {
		statusValueLabel.setText(status);
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		if (arg1 instanceof KernelEvent) {
			KernelEvent e = (KernelEvent)arg1;
			switch (e.getType()) {
				case STEP: {
					Instruction instr = kernel.getInstruction(
						kernel.getCurrentCycle());
					updateInstructionInfoArea(instr, false);
					Page p = kernel.getPage(e.getData());
					updatePageInfoArea(p);
					break;
				} case ADDED: {
					Instruction instr = kernel.getInstruction(
							kernel.getCurrentCycle());
					updateInstructionInfoArea(instr, true);
					addPhysicalPage(e.getData());
					break;
				} case REMOVED: {
					removePhysicalPage(e.getData());
					break;
				} case INFO: case ERROR:{
					System.out.println(e.getMessage());
					break;
				}
			}
		}	
	}
	
	private void setEnableKernelButtons(boolean areEnabled) {
		for (Button b : pageButtonList) {
			b.setEnabled(areEnabled);
		}
		
		runButton.setEnabled(areEnabled);
		stepButton.setEnabled(areEnabled);
		resetButton.setEnabled(areEnabled);
	}
	
	private void drawInfoButtons(int x, int y, int rowHeight, int colWidth, int yPadding) {
		int oldY = y;

		Label statusLabel = new Label("status: ", Label.LEFT);
		statusLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(statusLabel);

		y += rowHeight;
		Label timeLabel = new Label("time: ", Label.LEFT);
		timeLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(timeLabel);

		y += rowHeight * 2;
		Label instructionLabel = new Label("instruction: ", Label.LEFT);
		instructionLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(instructionLabel);

		y += rowHeight;
		Label addressLabel = new Label("address: ", Label.LEFT);
		addressLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(addressLabel);

		y += rowHeight;
		Label pageFaultLabel = new Label("page fault: ", Label.LEFT);
		pageFaultLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(pageFaultLabel);

		y += rowHeight;
		Label virtualPageLabel = new Label("virtual page: ", Label.LEFT);
		virtualPageLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(virtualPageLabel);

		y += rowHeight;
		Label physicalPageLabel = new Label("physical page: ", Label.LEFT);
		physicalPageLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(physicalPageLabel);

		y += rowHeight;
		Label RLabel = new Label("R: ", Label.LEFT);
		RLabel.setBounds(x,  y + yPadding, colWidth, rowHeight);
		add(RLabel);

		y += rowHeight;
		Label MLabel = new Label("M: ", Label.LEFT);
		MLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(MLabel);

		y += rowHeight;
		Label inMemTimeLabel = new Label("inMemTime: ", Label.LEFT);
		inMemTimeLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(inMemTimeLabel);

		y += rowHeight;
		Label lastTouchTimeLabel = new Label("lastTouchTime: ", Label.LEFT);
		lastTouchTimeLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(lastTouchTimeLabel);

		y += rowHeight;
		Label lowLabel = new Label("low: ", Label.LEFT);
		lowLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(lowLabel);

		y += rowHeight;
		Label highLabel = new Label("high: ", Label.LEFT);
		highLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(highLabel);

		// info column value layout
		x += colWidth ;
		y = oldY;
		statusValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(statusValueLabel);

		y += rowHeight;
		timeValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(timeValueLabel);

		y += rowHeight * 2;
		instructionValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(instructionValueLabel);

		y += rowHeight;
		addressValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(addressValueLabel);

		y += rowHeight;
		pageFaultValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(pageFaultValueLabel);

		y += rowHeight;
		virtualPageValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(virtualPageValueLabel);

		y += rowHeight;
		physicalPageValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(physicalPageValueLabel);

		y += rowHeight;
		RValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(RValueLabel);

		y += rowHeight;
		MValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(MValueLabel);

		y += rowHeight;
		inMemTimeValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(inMemTimeValueLabel);

		y += rowHeight;
		lastTouchTimeValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(lastTouchTimeValueLabel);

		y += rowHeight;
		lowValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(lowValueLabel);

		y += rowHeight;
		highValueLabel.setBounds(x, y + yPadding, colWidth, rowHeight);
		add(highValueLabel);
	}
	
	private void drawPageButtons(int x, int y, int colWidth, int rowHeight,
			int yPadding, int width, int height, int numPages) {
		// build the buttons of the memory pages
		
		int yStart = y;
		int xStart = x;
		
		// TODO: calculate this based on remaining screen space
		int buttonsPerCol = 32;

		// memory page buttons laid out in columns
		for (int i = 0; i < numPages; ++i) {
			x = (i / buttonsPerCol) * colWidth + xStart;
			y = (i % buttonsPerCol + 2) * rowHeight + yPadding + yStart;
			Button b = new Button("page " + (i));
			b.setBounds(x, y, width, height);
			b.setForeground(Color.black);
			b.setBackground(Color.lightGray);
			b.addActionListener(this);
			add(b);
			pageButtonList.add(b);

			Label l = new Label(null, Label.CENTER);
			l.setBounds(x + width, y, width - 10, height);
			l.setForeground(Color.red);
			l.setFont(new Font("Courier", 0, 10));
			add(l);
			pageLabelList.add(l);
		}
	}
}
