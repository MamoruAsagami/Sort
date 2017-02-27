package sophie.tools.textfile.sort;

public class KeyField {
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
	
	public int getStartField() {
		return startField;
	}

	public void setStartField(int startField) {
		this.startField = startField;
	}

	public int getStartChar() {
		return startChar;
	}

	public void setStartChar(int startChar) {
		this.startChar = startChar;
	}

	public int getEndField() {
		return endField;
	}

	public void setEndField(int endField) {
		this.endField = endField;
	}

	public int getEndChar() {
		return endChar;
	}

	public void setEndChar(int endChar) {
		this.endChar = endChar;
	}

	public SortKind getSortKind() {
		return sortKind;
	}

	public void setSortKind(SortKind sortKind) {
		this.sortKind = sortKind;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}

	public IgnoreKind getIgnoreKind() {
		return ignoreKind;
	}

	public void setIgnoreKind(IgnoreKind ignoreKind) {
		this.ignoreKind = ignoreKind;
	}

	public boolean isTranslate() {
		return translate;
	}

	public void setTranslate(boolean translate) {
		this.translate = translate;
	}

	public TranslateKind getTranslateKind() {
		return translateKind;
	}

	public void setTranslateKind(TranslateKind translateKind) {
		this.translateKind = translateKind;
	}

	public boolean isSkipStartBlanks() {
		return skipStartBlanks;
	}

	public void setSkipStartBlanks(boolean skipStartBlanks) {
		this.skipStartBlanks = skipStartBlanks;
	}

	public boolean isSkipEndBlanks() {
		return skipEndBlanks;
	}

	public void setSkipEndBlanks(boolean skipEndBlanks) {
		this.skipEndBlanks = skipEndBlanks;
	}

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

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