package sophie.tools.textfile.sort;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

class ExternalTextFileIterator implements Iterator<TextLine> {
	Pattern generalNumericPattern = Pattern.compile("(\\+|-|)(\\.|\\d).*");
	Pattern generalNumericNanInfinityPattern = Pattern.compile("(?i)(\\+|-|)(Nan|Infinity|Inf)");
	Configuration configuration;
	KeyField[] keyFields;
	boolean hasCr = false;
	boolean hasNl = false;
	boolean hasCrLf = false;
	boolean debug;
	char decimalPoint;
	char groupSeperator;
	String[] files;
	int fileIndex;
	MessageDigest messageDigest;
	boolean messageDigestHasSeed;
	boolean messageDigestCloneable;
	byte[] messageDigestSeed;
	InputStream in;
	TextLine textLine;
	HashMap<String, Integer> monthMap;
	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	int lastChar;
	long lineSeq;
	int fileNumber;
	int lineNumber;
	int headerLines;
	boolean headerEveryFile;
	String[] header;
	
	String lineSeparator() {
		if((hasCr? 1: 0) + (hasNl? 1: 0) + (hasCrLf? 1: 0) == 1) {
			if(hasCr)
				return "\r";
			else if(hasNl)
				return "\n";
			else
				return "\r\n";
		}
		return System.getProperty("line.separator");
	}
	
	
	public String[] getHeader() {
		if(header == null) {
			return null;
		} else {
			int i;
			for(i = 0; i < header.length; i++) {
				if(header[i] == null) {
					break;
				}
			}
			String[] header = new String[i];
			System.arraycopy(this.header, 0, header, 0, i);
			return header;
		}
	}

	private String readLine() throws IOException {
		for(;;) {
			if(in == null) {
				if(fileIndex < files.length) {
					String file = files[fileIndex];
					if(file == null || file.equals("-")) {
						in = System.in;
					} else {
						in = new BufferedInputStream(new FileInputStream(file));
					}
				} else {
					throw new EOFException();
				}
				lastChar = -1;
				fileNumber++;
				lineNumber = 0;
			}
			int c;
			while((c = in.read()) != -1) {
				if(configuration.zeroTerminated) {
					if(c == 0 && configuration.zeroTerminated) {
						String line = new String(buffer.toByteArray(), configuration.inputEncoding);
						buffer.reset();
						lineSeq++;
						lineNumber++;
						if(lineNumber > headerLines || (!headerEveryFile && fileNumber > 1)) {
							return line;
						} else {
							if(fileNumber == 1) {
								header[lineNumber - 1] = line;
							}
						}
					} else {
						buffer.write(c);
					}
				} else {
					if(c == '\n') {
						if(lastChar == '\r') {
							hasCrLf = true;
							lastChar = c;
						} else {
							hasNl = true;
							lastChar = c;
							String line = new String(buffer.toByteArray(), configuration.inputEncoding);
							buffer.reset();
							lineSeq++;
							lineNumber++;
							if(lineNumber > headerLines || (!headerEveryFile && fileNumber > 1)) {
								return line;
							} else {
								if(fileNumber == 1) {
									header[lineNumber - 1] = line;
								}
							}
						}
					} else if(c == '\r') {
						if(lastChar == '\r') {
							hasCr = true;
						}
						lastChar = c;
						String line = new String(buffer.toByteArray(), configuration.inputEncoding);
						buffer.reset();
						lineSeq++;
						lineNumber++;
						if(lineNumber > headerLines || (!headerEveryFile && fileNumber > 1)) {
							return line;
						} else {
							if(fileNumber == 1) {
								header[lineNumber - 1] = line;
							}
						}
					} else {
						if(lastChar == '\r') {
							hasCr = true;
						}
						lastChar = c;
						buffer.write(c);
					}
				}
			}
			if(configuration.zeroTerminated) {
				if(lastChar == '\r') {
					hasCr = true;
				}
			}
			if(buffer.size() != 0) {
				String line = new String(buffer.toByteArray(), configuration.inputEncoding);
				buffer.reset();
				lineSeq++;
				lineNumber++;
				if(lineNumber > headerLines || (!headerEveryFile && fileNumber > 1)) {
					return line;
				} else {
					if(fileNumber == 1) {
						header[lineNumber - 1] = line;
					}
				}
			}
			if(files[fileIndex] != null && !files[fileIndex].equals("-")) {
				in.close();
			}
			in = null;  // Force to open next file
			fileIndex++;
		}
	}
	
	private void takeGeneralNumeric(KeyField keyField, Field field, String line, int startFieldIndex, int endFieldIndex, boolean allowSIsuffix) {
		if(keyField.ignore || keyField.translate) {
			line = Sort.transform(keyField, line, startFieldIndex, endFieldIndex);
			startFieldIndex = 0;
			endFieldIndex = line.length();
		}
		String trimmed = line.substring(startFieldIndex, endFieldIndex).trim();
		final short signedMagnetude;
		final double realNumber;
		if(generalNumericPattern.matcher(trimmed).matches()) {
			signedMagnetude = 0;
			realNumber = Double.valueOf(trimmed);
		} else if(generalNumericNanInfinityPattern.matcher(trimmed).matches()) {
			boolean minus = false;
			if(trimmed.startsWith("+")) {
				trimmed = trimmed.substring(1);
			} else if(trimmed.startsWith("-")) {
				minus = true;
				trimmed = trimmed.substring(1);
			} else {
				// Nooo
			}
			if(trimmed.equalsIgnoreCase("Nan")) {
				signedMagnetude = -1;
				realNumber = Double.NaN;
			} else {
				signedMagnetude = 0;
				realNumber = minus? Double.NEGATIVE_INFINITY: Double.POSITIVE_INFINITY;
			}
		} else {
			signedMagnetude = -2;
			realNumber = 0;
		}
		field.signedMagnitude = signedMagnetude;
		field.realNumber = realNumber;
	}
	
	private void takeNumeric(KeyField keyField, Field field, String line, int startFieldIndex, int endFieldIndex, boolean allowSIsuffix) {
		if(keyField.ignore || keyField.translate) {
			line = Sort.transform(keyField, line, startFieldIndex, endFieldIndex);
			startFieldIndex = 0;
			endFieldIndex = line.length();
		}
		StringBuilder intBuilder = new StringBuilder();
		StringBuilder fracBuilder = new StringBuilder();
		char[] ca = line.toCharArray(); 
		int i = startFieldIndex;
		// Let's skip leading blanks.
		for(; i < endFieldIndex && Character.isWhitespace(ca[i]); i++) {
			// Nothing to do;
		}
		if(i < endFieldIndex && ca[i] == '-') {
			field.signedMagnitude = -1;
			i++;
		}
		// Let's skip leading zeros.
		boolean hasLeadingZero = false;
		for(; i < endFieldIndex &&
				(ca[i] == '0' ||
				ca[i] == groupSeperator ||
				(groupSeperator == 0xa0 && ca[i] == ' ' && i + 1 < endFieldIndex && Character.isDigit(ca[i+1])) // French has 0xa0 as group separator
				// || (decimalPoint == '.' && ca[i] == ',') || // not in the specification but seems helpful
				|| (decimalPoint == ',' && ca[i] == '.') // not in the specification but seems helpful
				);
			i++) {
			hasLeadingZero = true;
		}
		for(; i < endFieldIndex &&
				(Character.isDigit(ca[i]) ||
				ca[i] == groupSeperator ||
				(groupSeperator == 0xa0 && ca[i] == ' ' && i + 1 < endFieldIndex && Character.isDigit(ca[i+1])) // French has 0xa0 as group separator
				// || (decimalPoint == '.' && ca[i] == ',') || // not in the specification but seems helpful
				|| (decimalPoint == ',' && ca[i] == '.') // not in the specification but seems helpful
				);
			i++) {
			if(Character.isDigit(ca[i]))
				intBuilder.append(ca[i]);
		}
		if(intBuilder.length() == 0 && (hasLeadingZero || (i < endFieldIndex && ca[i] == decimalPoint))) {
			intBuilder.append('0');
		}
		if(i < endFieldIndex && ca[i] == decimalPoint) {
			i++;
			for(; i < endFieldIndex &&
					(Character.isDigit(ca[i]) ||
					ca[i] == groupSeperator ||
					(groupSeperator == 0xa0 && ca[i] == ' ' && i + 1 < endFieldIndex && Character.isDigit(ca[i+1])) // French has 0xa0 as group separator
					// || (decimalPoint == '.' && ca[i] == ',') || // not in the specification but seems helpful
					|| (decimalPoint == ',' && ca[i] == '.') // not in the specification but seems helpful
					);
				i++) {
				if(Character.isDigit(ca[i]))
					fracBuilder.append(ca[i]);
			}
		}
		if(allowSIsuffix) {
			if(i < endFieldIndex) {
				char suffix = ca[i];
				i++;
				if(suffix == 'k') suffix = 'K';
				// suffix = Character.toUpperCase(suffix); // This one relaxes the specification.
				int index = Sort.SIunit.indexOf(suffix);
				if(index >= 0) {
					int sign = (field.signedMagnitude >= 0)? 1: -1;
					field.signedMagnitude = (short)(sign * (index + 1));
				}
			}
		}
		char[] intDigits = intBuilder.toString().toCharArray();
		char[] fracDigits = fracBuilder.toString().toCharArray();
		field.integralPart = new byte[intDigits.length];
		for(int k = 0; k < intDigits.length; k++) {
			field.integralPart[k] = (byte)intDigits[k];
		}
		field.fractionalPart = new byte[fracDigits.length];
		for(int k = 0; k < fracDigits.length; k++) {
			field.fractionalPart[k] = (byte)fracDigits[k];
		}
	}

	private void takeMonth(KeyField keyField, Field field, String line, int startFieldIndex, int endFieldIndex) {
		if(keyField.ignore || keyField.translate) {
			line = Sort.transform(keyField, line, startFieldIndex, endFieldIndex);
			startFieldIndex = 0;
			endFieldIndex = line.length();
		}
		char[] ca = line.toCharArray(); 
		int i = startFieldIndex;
		// Let's skip leading blanks.
		for(; i < endFieldIndex && Character.isWhitespace(ca[i]); i++) {
			// Nothing to do;
		}
		if(i < endFieldIndex) {
			int c = ca[i];
			int begin = i;
			i++;
			if(Character.isDigit(c)) {
				for(; i < endFieldIndex && Character.isDigit(ca[i]); i++) {
				}
				int month = Integer.valueOf(line.substring(begin, i));
				if(month > 12) {
					month = 0;
				}
				field.month = (short)month;
			} else if(Character.isJavaIdentifierPart(c)) {
				for(; i < endFieldIndex && Character.isJavaIdentifierPart(ca[i]); i++) {
				}
				String  monthName = line.substring(begin, i).toUpperCase();
				Integer month = monthMap.get(monthName);
				field.month = (short)((month != null)? month: 0);
			} else {
				// Nothing to do. (field.month is already initialized)
			}
		} else {
			// Nothing to do. (field.month is already initialized)
		}
	}

	private void takeRandom(KeyField keyField, Field field, String line, int startFieldIndex, int endFieldIndex) throws IOException {
		if(Sort.GNU_SORT_COMPATIBLE) {
			field.start = (char)startFieldIndex;
			field.limit = (char)endFieldIndex;
		}
		if(keyField.ignore || keyField.translate) {
			line = Sort.transform(keyField, line, startFieldIndex, endFieldIndex);
			startFieldIndex = 0;
			endFieldIndex = line.length();
		}
		try {
			byte[] bytes= line.substring(startFieldIndex, endFieldIndex).getBytes("UTF-8");
			final byte[] digest;
			if(messageDigestHasSeed) {
				if(messageDigestCloneable) {
					MessageDigest md = (MessageDigest)messageDigest.clone();
					digest = md.digest(bytes);
				} else {
					messageDigest.reset();
					messageDigest.update(messageDigestSeed);
					digest = messageDigest.digest(bytes);
				}
			} else {
				messageDigest.reset();
				digest = messageDigest.digest(bytes);
			}
			field.digest = digest;
		} catch(UnsupportedEncodingException e) {
			throw new IOException(e);
		} catch(CloneNotSupportedException e) {
			throw new IOException(e);
		}
	}
	
	private void takeVersion(KeyField keyField, Field field, String line, int startFieldIndex, int endFieldIndex)  {
		if(keyField.ignore || keyField.translate) {
			line = Sort.transform(keyField, line, startFieldIndex, endFieldIndex);
			startFieldIndex = 0;
			endFieldIndex = line.length();
		}
		char[] ca = line.toCharArray();
		int i = startFieldIndex;
		StringBuilder builder = new StringBuilder();
		for(; i < endFieldIndex && Character.isWhitespace(ca[i]); i++) {
			// Nothing to do;
		}
		for(; i < endFieldIndex && Sort.isPrintable(ca[i]) && !Character.isWhitespace(ca[i]); i++) {
			builder.append(ca[i]);
		}
		field.version = builder.toString();
	}
	
	private TextLine read() throws IOException {
		String line = readLine();
		TextLine textLine = new TextLine();
		textLine.seq = lineSeq;
		if(keyFields.length != 0) {
			textLine.fields = new Field[keyFields.length];
			int[] fieldIndexes = configuration.defaultFieldSeparator? Sort.splitLine(line): Sort.splitLine(line, configuration.fieldSeparator);
			for(int i = 0; i < keyFields.length; i++) {
				KeyField keyField = keyFields[i];
				Field field = textLine.fields[i] = new Field();
				int startFieldIndex;
				int startFieldLimit;
				int endFieldIndex;
				int endFieldLimit;
				if(keyField.startField == Integer.MAX_VALUE) {
					startFieldIndex = 0;
					startFieldLimit = endFieldIndex = endFieldLimit = line.length(); 
				} else {
					if(keyField.startField <= fieldIndexes.length / 2) {
						int index = keyField.startField > 0? (keyField.startField - 1) * 2: 0;
						startFieldIndex = fieldIndexes[index];
						startFieldLimit = fieldIndexes[index + 1];
					} else {
						startFieldIndex = startFieldLimit = line.length();
					}
					if(keyField.endField <= fieldIndexes.length / 2) {
						int index = keyField.endField > 0? (keyField.endField - 1) * 2: 0;
						endFieldIndex = fieldIndexes[index];
						endFieldLimit = fieldIndexes[index + 1];
					} else {
						endFieldIndex = endFieldLimit = line.length();
					}
				}
				if(keyField.skipStartBlanks) {
					for(; startFieldIndex < (Sort.GNU_SORT_COMPATIBLE? line.length(): startFieldLimit) && Character.isWhitespace(line.charAt(startFieldIndex)); startFieldIndex++) {
						// Nothing to do
					}
				}
				startFieldIndex = Math.min(startFieldLimit, startFieldIndex + ((keyField.startChar > 0)? keyField.startChar - 1: 0));
				if(keyField.endChar == 0) {
					endFieldIndex = endFieldLimit;
				} else {
					if(keyField.skipEndBlanks) {
						for(; endFieldIndex < (Sort.GNU_SORT_COMPATIBLE? line.length(): endFieldLimit) && Character.isWhitespace(line.charAt(endFieldIndex)); endFieldIndex++) {
							// Nothing to do
						}
					}
					endFieldIndex = Math.min(endFieldLimit, endFieldIndex + keyField.endChar);
				}
				if(endFieldIndex < startFieldIndex) {
					endFieldIndex = startFieldIndex;
				}
				if(debug) {
					System.out.println("file: " + fileNumber + " line: " + lineNumber + " key[" + i +  "] , " + keyFields[i].sortKind + "(" + startFieldIndex + ", " + endFieldIndex + "): \"" + line.substring(startFieldIndex, endFieldIndex) + "\"");
				}
				try {
					switch(keyFields[i].sortKind) {
					case Text:
						field.start = (char)startFieldIndex;
						field.limit = (char)endFieldIndex;
						break;
					case GeneralNumeric:
						takeGeneralNumeric(keyField, field, line, startFieldIndex, endFieldIndex, true);
						break;
					case HumanNumeric:
						takeNumeric(keyField, field, line, startFieldIndex, endFieldIndex, true);
						break;
					case Numeric:
						takeNumeric(keyField, field, line, startFieldIndex, endFieldIndex, false);
						break;
					case Month:
						takeMonth(keyField, field, line, startFieldIndex, endFieldIndex);
						break;
					case Random:
						takeRandom(keyField, field, line, startFieldIndex, endFieldIndex);
						break;
					case Version:
						takeVersion(keyField, field, line, startFieldIndex, endFieldIndex);
						break;
					default:
						throw new IllegalStateException("Unknown SortKind");
					}
				} catch(Exception e) {
					throw new RuntimeException("file: " + fileNumber + " line: " + lineNumber + " key[" + i +  "] , " + keyFields[i].sortKind + ": " + e, e);
				}
			}
		}
		textLine.line = line;
		return textLine;
	}

	@Override
	public boolean hasNext() {
		if(this.textLine != null)
			return true;
		try {
			this.textLine = read();
			return true;
		} catch (EOFException e) {
			return false;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public TextLine next() {
		if(this.textLine != null) {
			TextLine textLine = this.textLine;
			this.textLine = null;
			return textLine;
		} else {
			try {
				TextLine textLine = read();
				return textLine;
			} catch (EOFException e) {
				throw new NoSuchElementException("EOF");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	private void setUpMessageDigest() throws IOException {
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			if(configuration.randomSource != null) {
				messageDigestHasSeed = true;
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream in = new FileInputStream(configuration.randomSource);
				try {
					int c;
					for(int i = 0; i < 4096 && (c = in.read()) != -1 ; i++) {
						out.write(c);
					}
				} finally {
					in.close();
				}
				messageDigestSeed = out.toByteArray();
				messageDigest.update(messageDigestSeed);
				try {
					messageDigest.clone();
					messageDigestCloneable = true;
				} catch (CloneNotSupportedException e) {
					// messageDigestCloneable = false; // redundant
				}
			}
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}
			
	void setFile(String file) {
		files = new String[] {file};
		fileIndex = 0;
	}
	
	ExternalTextFileIterator(Configuration configuration) throws IOException {
		this.configuration = configuration;
		this.keyFields = configuration.keyFields;
		if(configuration.inputFileNames == null) {
			files = new String[] {null};
		} else {
			files = configuration.inputFileNames;
		}
		if(configuration.headerLines > 0) {
			headerLines = configuration.headerLines;
			header = new String[headerLines];
			headerEveryFile = configuration.headerEveryFile;
		}
		
		debug = configuration.debug;
		if(configuration.numberLocale != null) {
  	  		NumberFormat numberFormat = (DecimalFormat)DecimalFormat.getInstance(configuration.numberLocale);
  	  		if(numberFormat instanceof DecimalFormat) {
  	  			DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
  	  			DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
  	  			decimalPoint = symbols.getDecimalSeparator();
  	  			groupSeperator = symbols.getGroupingSeparator();
   	  		} else {
				decimalPoint = '.';
				groupSeperator = ',';
   	  		}
		} else {
			decimalPoint = '.';
			groupSeperator = ',';
		}
		monthMap = new HashMap<String, Integer>();
		HashSet<Locale> localeSet = new HashSet<Locale>();
		localeSet.add(Locale.forLanguageTag("en-US"));
		localeSet.add(Locale.getDefault());
		if(configuration.textLocale != null) {
			localeSet.add(configuration.textLocale);
		}
		if(configuration.numberLocale != null) {
			localeSet.add(configuration.numberLocale);
		}
		for(Locale locale: localeSet) {
			DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance(locale);
			String[] shortMonthNames = dateFormatSymbols.getShortMonths();
			String[] monthNames = dateFormatSymbols.getMonths();
			// For some reason, shortMonthNames.length == monthNames.length == 13
			for(int i = 0; i < shortMonthNames.length && i < 12; i++) {
				monthMap.put(shortMonthNames[i].toUpperCase(), i + 1);
			}
			for(int i = 0; i < monthNames.length && i < 12; i++) {
				monthMap.put(monthNames[i].toUpperCase(), i + 1);
			}
		}
		if(configuration.hasRandom) {
			setUpMessageDigest();
		}
	}
}