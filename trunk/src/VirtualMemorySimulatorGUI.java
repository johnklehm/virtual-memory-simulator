// The main MemoryManagement program, created by Alexander Reeder, 2000 Nov 19

public class VirtualMemorySimulatorGUI {
	public static void main(String[] args) {
		ControlPanel panel;
		Kernel kernel;

		kernel = new Kernel();
		panel = new ControlPanel();

		panel.init(kernel, null, null);
	}
}
