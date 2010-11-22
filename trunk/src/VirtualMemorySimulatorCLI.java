// The main MemoryManagement program, created by Alexander Reeder, 2000 Nov 19

import java.io.*;

public class VirtualMemorySimulatorCLI {
	public static void main(String[] args) {
		Kernel kernel;

		if (args.length < 1 || args.length > 2) {
			System.out
			.println("Usage: 'java MemoryManagement <COMMAND FILE> <PROPERTIES FILE>'");
			System.exit(-1);
		}

		File f = new File(args[0]);

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

		if (args.length == 2) {
			f = new File(args[1]);

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

		kernel = new Kernel();
		if (args.length == 1) {
			kernel.init(args[0], null);
		} else {
			kernel.init(args[0], args[1]);
		}
	}
}
