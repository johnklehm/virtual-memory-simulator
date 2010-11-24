import java.awt.Button;
import java.awt.Color;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class ControlPanel extends Frame {
	private static final long serialVersionUID = 6280534328667165120L;
	Kernel kernel;
	Button runButton = new Button("run");
	Button stepButton = new Button("step");
	Button resetButton = new Button("reset");
	Button exitButton = new Button("exit");
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
		super();
	}

	public ControlPanel(String title) {
		super(title);
	}

	@Override
	public boolean action(Event e, Object arg) {
		if (e.target == runButton) {
			setStatus("RUN");
			runButton.setEnabled(false);
			stepButton.setEnabled(false);
			resetButton.setEnabled(false);
			kernel.run();
			setStatus("STOP");
			resetButton.setEnabled(true);
			return true;
		} else if (e.target == stepButton) {
			setStatus("STEP");
			kernel.step();
			if (kernel.runcycles == kernel.runs) {
				stepButton.setEnabled(false);
				runButton.setEnabled(false);
			}
			setStatus("STOP");
			return true;
		} else if (e.target == resetButton) {
			reset();
			return true;
		} else if (e.target == exitButton) {
			System.exit(0);
			return true;
		}

		for (int i = 0; i < pageButtonList.size(); ++i) {
			if (e.target == pageButtonList.get(i)) {
				kernel.getPage(i);
				return true;
			}
		}

		return false;
	}

	public void addPhysicalPage(int pageNum, int physicalPage) {
		if (physicalPage < pageLabelList.size()) {
			pageLabelList.get(physicalPage).setText("page " + pageNum);
		}
	}

	public void init(Kernel useKernel) {
		kernel = useKernel;
		kernel.setControlPanel(this);
		setLayout(null);
		setBackground(Color.white);
		setForeground(Color.black);
		setSize(635, 545);
		setFont(new Font("Courier", 0, 12));

		runButton.setForeground(Color.blue);
		runButton.setBackground(Color.lightGray);
		runButton.setBounds(0, 25, 70, 15);
		add(runButton);

		stepButton.setForeground(Color.blue);
		stepButton.setBackground(Color.lightGray);
		stepButton.setBounds(70, 25, 70, 15);
		add(stepButton);

		resetButton.setForeground(Color.blue);
		resetButton.setBackground(Color.lightGray);
		resetButton.setBounds(140, 25, 70, 15);
		add(resetButton);

		exitButton.setForeground(Color.blue);
		exitButton.setBackground(Color.lightGray);
		exitButton.setBounds(210, 25, 70, 15);
		add(exitButton);

		int xIncrement = 140;
		int yIncrement = 15;
		int width = 70;
		int height = 15;
		int yPadding = 25;
		int buttonsPerCol = 32;
		int totalPages = 64;

		// memory page buttons laid out in columns
		for (int i = 0; i < totalPages; ++i) {
			int x = (i / buttonsPerCol) * xIncrement;
			int y = (i % buttonsPerCol + 2) * yIncrement + yPadding;
			Button b = new Button("page " + (i));
			b.setBounds(x, y, width, height);
			b.setForeground(Color.magenta);
			b.setBackground(Color.lightGray);
			add(b);
			pageButtonList.add(b);

			Label l = new Label(null, Label.CENTER);
			l.setBounds(x + width, y, width - 10, height);
			l.setForeground(Color.red);
			l.setFont(new Font("Courier", 0, 10));
			add(l);
			pageLabelList.add(l);
		}

		statusValueLabel.setBounds(345, 0 + 25, 100, 15);
		add(statusValueLabel);

		timeValueLabel.setBounds(345, 15 + 25, 100, 15);
		add(timeValueLabel);

		instructionValueLabel.setBounds(385, 45 + 25, 100, 15);
		add(instructionValueLabel);

		addressValueLabel.setBounds(385, 60 + 25, 230, 15);
		add(addressValueLabel);

		pageFaultValueLabel.setBounds(385, 90 + 25, 100, 15);
		add(pageFaultValueLabel);

		virtualPageValueLabel.setBounds(395, 120 + 25, 200, 15);
		add(virtualPageValueLabel);

		physicalPageValueLabel.setBounds(395, 135 + 25, 200, 15);
		add(physicalPageValueLabel);

		RValueLabel.setBounds(395, 150 + 25, 200, 15);
		add(RValueLabel);

		MValueLabel.setBounds(395, 165 + 25, 200, 15);
		add(MValueLabel);

		inMemTimeValueLabel.setBounds(395, 180 + 25, 200, 15);
		add(inMemTimeValueLabel);

		lastTouchTimeValueLabel.setBounds(395, 195 + 25, 200, 15);
		add(lastTouchTimeValueLabel);

		lowValueLabel.setBounds(395, 210 + 25, 230, 15);
		add(lowValueLabel);

		highValueLabel.setBounds(395, 225 + 25, 230, 15);
		add(highValueLabel);

		Label virtualOneLabel = new Label("virtual", Label.CENTER);
		virtualOneLabel.setBounds(0, 15 + 25, 70, 15);
		add(virtualOneLabel);

		Label virtualTwoLabel = new Label("virtual", Label.CENTER);
		virtualTwoLabel.setBounds(140, 15 + 25, 70, 15);
		add(virtualTwoLabel);

		Label physicalOneLabel = new Label("physical", Label.CENTER);
		physicalOneLabel.setBounds(70, 15 + 25, 70, 15);
		add(physicalOneLabel);

		Label physicalTwoLabel = new Label("physical", Label.CENTER);
		physicalTwoLabel.setBounds(210, 15 + 25, 70, 15);
		add(physicalTwoLabel);

		Label statusLabel = new Label("status: ", Label.LEFT);
		statusLabel.setBounds(285, 0 + 25, 65, 15);
		add(statusLabel);

		Label timeLabel = new Label("time: ", Label.LEFT);
		timeLabel.setBounds(285, 15 + 25, 50, 15);
		add(timeLabel);

		Label instructionLabel = new Label("instruction: ", Label.LEFT);
		instructionLabel.setBounds(285, 45 + 25, 100, 15);
		add(instructionLabel);

		Label addressLabel = new Label("address: ", Label.LEFT);
		addressLabel.setBounds(285, 60 + 25, 85, 15);
		add(addressLabel);

		Label pageFaultLabel = new Label("page fault: ", Label.LEFT);
		pageFaultLabel.setBounds(285, 90 + 25, 100, 15);
		add(pageFaultLabel);

		Label virtualPageLabel = new Label("virtual page: ", Label.LEFT);
		virtualPageLabel.setBounds(285, 120 + 25, 110, 15);
		add(virtualPageLabel);

		Label physicalPageLabel = new Label("physical page: ", Label.LEFT);
		physicalPageLabel.setBounds(285, 135 + 25, 110, 15);
		add(physicalPageLabel);

		Label RLabel = new Label("R: ", Label.LEFT);
		RLabel.setBounds(285, 150 + 25, 110, 15);
		add(RLabel);

		Label MLabel = new Label("M: ", Label.LEFT);
		MLabel.setBounds(285, 165 + 25, 110, 15);
		add(MLabel);

		Label inMemTimeLabel = new Label("inMemTime: ", Label.LEFT);
		inMemTimeLabel.setBounds(285, 180 + 25, 110, 15);
		add(inMemTimeLabel);

		Label lastTouchTimeLabel = new Label("lastTouchTime: ", Label.LEFT);
		lastTouchTimeLabel.setBounds(285, 195 + 25, 110, 15);
		add(lastTouchTimeLabel);

		Label lowLabel = new Label("low: ", Label.LEFT);
		lowLabel.setBounds(285, 210 + 25, 110, 15);
		add(lowLabel);

		Label highLabel = new Label("high: ", Label.LEFT);
		highLabel.setBounds(285, 225 + 25, 110, 15);
		add(highLabel);

		// close with the X button
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				e.getWindow().dispose();
			}
		});

		useKernel.initGUI();

		setVisible(true);
	}

	public void paintPage(Page page) {
		virtualPageValueLabel.setText(Integer.toString(page.id));
		physicalPageValueLabel.setText(Integer.toString(page.physical));
		RValueLabel.setText(Integer.toString(page.R));
		MValueLabel.setText(Integer.toString(page.M));
		inMemTimeValueLabel.setText(Integer.toString(page.inMemTime));
		lastTouchTimeValueLabel.setText(Integer.toString(page.lastTouchTime));
		lowValueLabel.setText(Long.toString(page.low, Kernel.addressradix));
		highValueLabel.setText(Long.toString(page.high, Kernel.addressradix));
	}

	public void removePhysicalPage(int physicalPage) {
		if (physicalPage < pageLabelList.size()) {
			pageLabelList.get(physicalPage).setText(null);
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
		runButton.setEnabled(true);
		stepButton.setEnabled(true);
	}

	public void setStatus(String status) {
		statusValueLabel.setText(status);
	}
}
