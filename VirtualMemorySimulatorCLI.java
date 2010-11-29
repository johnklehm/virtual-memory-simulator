// The main MemoryManagement program, created by Alexander Reeder, 2000 Nov 19

import java.io.File;

public class VirtualMemorySimulatorCLI {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out
					.println("Usage: 'java MemoryManagement <CONFIG FILE> <TRACE FILE>'");
			System.exit(-1);
		}

		for (int i = 0; i < args.length; ++i) {
			File f = new File(args[i]);
	
			if (!(f.exists())) {
				System.out.println("MemoryM: error, file '" + f.getName()
						+ "' does not exist.");
				System.exit(-1);
			}
			if (!(f.canRead())) {
				System.out.println("MemoryM: error, read of " + f.getName()
						+ " failed.");
				System.exit(-1);
			}
		}

		Kernel k = new Kernel();
		k.loadConfig(args[0]);
		k.loadTrace(args[1]);
	}
}
