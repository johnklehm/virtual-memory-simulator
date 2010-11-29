public class Instruction {
	public static enum Type {READ, WRITE};
	public Type inst;
	public long addr;

	public Instruction(Type inst, long addr) {
		this.inst = inst;
		this.addr = addr;
	}

	public String typeString() {
		switch (inst) {
		case READ:
			return "READ";
		case WRITE:
			return "WRITE"; 
		}
		
		return "";
	}
}
