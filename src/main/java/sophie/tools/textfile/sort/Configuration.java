package sophie.tools.textfile.sort;

import java.nio.charset.Charset;
import java.util.Locale;

class Configuration {
	String[] inputFileNames;	// null means standard in
	String outputFileName;	// null means standard out
	Charset inputEncoding = Charset.defaultCharset();
	Charset outputEncoding = Charset.defaultCharset();
	KeyField[] keyFields;
	ProcessKind processKind = ProcessKind.Sort;
	CheckKind checkKind;
	int headerLines;
	boolean headerEveryFile;
	String compressProgram;
	boolean debug;
	int mergeBatchSize = 16;
	String randomSource;
	boolean stable; 
	boolean unique;
	int bufferSize;
	String bufferSizeSuffix;
	boolean defaultFieldSeparator = true;
	char fieldSeparator;
	String tmpDirectory;
	int numberOfParallel;
	boolean zeroTerminated; // Line is NUL terminated
	Locale textLocale = Locale.getDefault();
	Locale numberLocale = Locale.getDefault();
	boolean reverse;
	boolean globalKeyOnly;
	boolean hasRandom;
	
	Configuration() {
	}
	
	void print(IndentedReporter out) {
		out.println("Configuration {");
		out.inc();
		try {
			if(inputFileNames == null) {
				out.println("Input: stdin");
			} else if(inputFileNames.length == 1) {
				out.println("Input: " + inputFileNames[0]);
			} else {
				out.println("Input: [");
				out.inc();
				try {
					for(int i = 0; i < inputFileNames.length; i++) {
						out.println(inputFileNames[i] + ((i + 1 < inputFileNames.length)? "," : ""));
					}
				} finally {
					out.dec();
				}
				out.println("]");
			}
			if(outputFileName == null) {
				out.println("Output: stdout");
			} else {
				out.println("Output: " + outputFileName);
			}
			out.println("inputEncoding: " + inputEncoding);
			out.println("outputEncoding: " + outputEncoding);
			if(keyFields != null) {
				out.println("keyFields: [");
				out.inc();
				try {
					for(int i = 0; i < keyFields.length; i++) {
						keyFields[i].print(out);
					}
				} finally {
					out.dec();
				}
				out.println("]");
			} else {
				out.println("keyFields: " + keyFields);
			}
			out.println("processKind: " + processKind);
			out.println("checkKind: " + checkKind);
			out.println("compressProgram: " + compressProgram);
			out.println("debug: " + debug);
			out.println("mergeBatchSize: " + mergeBatchSize);
			out.println("randomSource: " + randomSource);
			out.println("stable: " + stable);
			out.println("bufferSize: " + bufferSize + ((bufferSize != 0)? bufferSizeSuffix: ""));
			out.println("defaultFieldSeparator: " + defaultFieldSeparator);
			if(Character.isISOControl(fieldSeparator)) {
				out.println("fieldSeparator: 0x" + Integer.toHexString(fieldSeparator));
			} else if(fieldSeparator == '\'') {
				out.println("fieldSeparator: '\\''");
			} else {
				out.println("fieldSeparator: '" + fieldSeparator + "'");
			}
			out.println("tmpDirectory: " + tmpDirectory);
			out.println("numberOfParallel: " + numberOfParallel);
			out.println("unique: " + unique);
			out.println("zeroTerminated: " + zeroTerminated);
			out.println("header: " + headerLines + ((headerLines == 0)? "": headerEveryFile? ", every": ", first"));
			out.println("locale: {text:" + textLocale + ", number: " + numberLocale + "}");
			out.println("globalKeyOnly: " + globalKeyOnly);
			out.println("reverse: " + reverse);
		} finally {
			out.dec();
		}
		out.println("}");
	}
}