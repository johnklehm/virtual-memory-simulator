import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class CompressAddressSpace {
	public static void main(String args[]) throws FileNotFoundException {
		if (args.length < 1) {
			System.out.println(
					"Usage: java CompressAddressSpace <TRACEFILE> [MAX_ADDRESSSPACE_HEX]");
		} else {
			Scanner in = new Scanner(new File(args[0]));
			long maxAddress = 0xFFFFFFFF;
			if (args.length == 2) {
				maxAddress = Long.parseLong(args[1], 16);
			}

			while (in.hasNext() == true) {
				String type = in.next();
				String direction = in.next();
				long address = in.nextLong(16);

				address = address % maxAddress;

				System.out.printf("%s\t%s\t%x\n", type, direction, address);
			}
		}
	}
}
