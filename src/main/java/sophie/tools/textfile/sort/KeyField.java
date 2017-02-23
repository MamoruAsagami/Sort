package sophie.tools.textfile.sort;

class KeyField {
	int startField;	// 1..n
	int	startChar;  // 1..n, 0 means 1
	int endField;	// 1..n, 0 means to-the-end of the line
	int endChar;	// 1..n, 0 means to-the-end of the field
	SortKind sortKind;
	boolean ignore;
	IgnoreKind ignoreKind;
	boolean translate;
	TranslateKind translateKind;
	boolean  skipStartBlanks;		// Skip leading blanks when finding start
	boolean  skipEndBlanks;		// Skip leading blanks when finding end
	boolean  reverse;
	
	boolean isDefault() {
	  return ! (ignore
	            || translate
	            || skipStartBlanks
	            || skipEndBlanks
	            || sortKind != SortKind.Text
	            || reverse
	           );
	}
	
	boolean isDefaultExceptForReverse() {
		  return ! (ignore
		            || translate
		            || skipStartBlanks
		            || skipEndBlanks
		            || sortKind != SortKind.Text
		            // || reverse
		           );
		}
	KeyField() {
		endField = Integer.MAX_VALUE;
		sortKind = SortKind.Text;
	}
	void print(IndentedReporter out) {
		out.println("KeyField {");
		out.inc();
		try {
			out.println("startField: " + startField);
			out.println("startChar: " + startChar);
			out.println("endField: " + endField);
			out.println("endChar: " + endChar);
			out.println("sortKind: " + sortKind);
			out.println("ignore: " + ignore);
			out.println("ignoreKind: " + ignoreKind);
			out.println("translate: " + translate);
			out.println("translateKind: " + translateKind);
			out.println("skipStartBlanks: " + skipStartBlanks);
			out.println("skipEndBlanks: " + skipEndBlanks);
			out.println("reverse: " + reverse);
		} finally {
			out.dec();
		}
		out.println("}");
	}
}