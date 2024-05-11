package sophie.tools.textfile.sort;

import java.nio.charset.Charset;
import java.util.Locale;

public class Configuration {
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
	boolean csv;
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
	
	public Configuration() {
	}
	
	public String[] getInputFileNames() {
		return inputFileNames;
	}

	public void setInputFileNames(String[] inputFileNames) {
		this.inputFileNames = inputFileNames;
	}

	public String getOutputFileName() {
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public Charset getInputEncoding() {
		return inputEncoding;
	}

	public void setInputEncoding(Charset inputEncoding) {
		this.inputEncoding = inputEncoding;
	}

	public Charset getOutputEncoding() {
		return outputEncoding;
	}

	public void setOutputEncoding(Charset outputEncoding) {
		this.outputEncoding = outputEncoding;
	}

	public KeyField[] getKeyFields() {
		return keyFields;
	}

	public void setKeyFields(KeyField[] keyFields) {
		this.keyFields = keyFields;
	}

	public ProcessKind getProcessKind() {
		return processKind;
	}

	public void setProcessKind(ProcessKind processKind) {
		this.processKind = processKind;
	}

	public CheckKind getCheckKind() {
		return checkKind;
	}

	public void setCheckKind(CheckKind checkKind) {
		this.checkKind = checkKind;
	}

	public int getHeaderLines() {
		return headerLines;
	}

	public void setHeaderLines(int headerLines) {
		this.headerLines = headerLines;
	}

	public boolean isHeaderEveryFile() {
		return headerEveryFile;
	}

	public void setHeaderEveryFile(boolean headerEveryFile) {
		this.headerEveryFile = headerEveryFile;
	}

	public String getCompressProgram() {
		return compressProgram;
	}

	public void setCompressProgram(String compressProgram) {
		this.compressProgram = compressProgram;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public int getMergeBatchSize() {
		return mergeBatchSize;
	}

	public void setMergeBatchSize(int mergeBatchSize) {
		this.mergeBatchSize = mergeBatchSize;
	}

	public String getRandomSource() {
		return randomSource;
	}

	public void setRandomSource(String randomSource) {
		this.randomSource = randomSource;
	}

	public boolean isStable() {
		return stable;
	}

	public void setStable(boolean stable) {
		this.stable = stable;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public boolean isCsv() {
		return csv;
	}

	public void setCsv(boolean csv) {
		this.csv = csv;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public String getBufferSizeSuffix() {
		return bufferSizeSuffix;
	}

	public void setBufferSizeSuffix(String bufferSizeSuffix) {
		this.bufferSizeSuffix = bufferSizeSuffix;
	}

	public boolean isDefaultFieldSeparator() {
		return defaultFieldSeparator;
	}

	public void setDefaultFieldSeparator(boolean defaultFieldSeparator) {
		this.defaultFieldSeparator = defaultFieldSeparator;
	}

	public char getFieldSeparator() {
		return fieldSeparator;
	}

	public void setFieldSeparator(char fieldSeparator) {
		this.fieldSeparator = fieldSeparator;
	}

	public String getTmpDirectory() {
		return tmpDirectory;
	}

	public void setTmpDirectory(String tmpDirectory) {
		this.tmpDirectory = tmpDirectory;
	}

	public int getNumberOfParallel() {
		return numberOfParallel;
	}

	public void setNumberOfParallel(int numberOfParallel) {
		this.numberOfParallel = numberOfParallel;
	}

	public boolean isZeroTerminated() {
		return zeroTerminated;
	}

	public void setZeroTerminated(boolean zeroTerminated) {
		this.zeroTerminated = zeroTerminated;
	}

	public Locale getTextLocale() {
		return textLocale;
	}

	public void setTextLocale(Locale textLocale) {
		this.textLocale = textLocale;
	}

	public Locale getNumberLocale() {
		return numberLocale;
	}

	public void setNumberLocale(Locale numberLocale) {
		this.numberLocale = numberLocale;
	}

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	public boolean isGlobalKeyOnly() {
		return globalKeyOnly;
	}

	public void setGlobalKeyOnly(boolean globalKeyOnly) {
		this.globalKeyOnly = globalKeyOnly;
	}

	public void print(IndentedReporter out) {
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
			out.println("csv: " + csv);
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
	
	void normalize() {
		if(keyFields == null) {
			keyFields = new KeyField[0];
		}
		for(KeyField keyField: keyFields) {
			if(keyField.sortKind == SortKind.Random) {
				hasRandom = true;
			}
		}
	}
}