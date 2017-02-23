package sophie.tools.textfile.sort;

import java.io.PrintStream;

class IndentedReporter {
	PrintStream out;
	int level;
	public IndentedReporter(PrintStream out) {
		super();
		this.out = out;
	}
	
	void inc() {
		level++;
	}
	
	void dec() {
		level--;
	}
	
	void println(String text) {
		for(int i = 0; i < level; i++) {
			out.print("  ");
		}
		out.println(text);
	}
}