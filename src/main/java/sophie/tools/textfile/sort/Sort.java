package sophie.tools.textfile.sort;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.geirove.exmeso.ExternalMergeSort;

import sophie.io.NullOutputStream;
import sophie.tools.textfile.sort.SortUI;
import org.geirove.exmeso.CloseableIterator;


public class Sort {
	static final String VERSION = "1.0";
	static final String TITLE = Sort.class.getSimpleName() + " " + VERSION;
	static final boolean GNU_SORT_COMPATIBLE = false;
	static final Options commandLineOptions;
	static final String SIunit = "KMGTPEZY";
	
	static { // Initializer of commandOptions
		OptionGroup sortKindOptionGroup = new OptionGroup()
				.addOption(new Option("g", "general-numeric-sort", false, "compare according to general numerical value"))
				.addOption(new Option("h", "human-numeric-sort", false, "compare human readable numbers (e.g., 2K 1G)"))
				.addOption(new Option("n", "numeric-sort", false, "compare according to string numerical value"))
				.addOption(new Option("M", "month-sort", false, "compare (unknown) < 'JAN' < ... < 'DEC'"))
				.addOption(new Option("R", "random-sort", false, "shuffle, but group identical keys."))
				.addOption(new Option("V", "version-sort", false, "natural sort of (version) numbers within text"))
				.addOption(Option.builder()
						.longOpt("sort")
						.hasArg()
						.argName("WORD")
						.desc("sort according to WORD: general-numeric -g, human-numeric -h, numeric -n, month -M, random -R, version -V")
						.build());
		OptionGroup processKindOptionGroup = new OptionGroup()
				.addOption(new Option("m", "merge", false, "merge already sorted files; do not sort"))
				.addOption(new Option("c", false, "check for sorted input; do not sort"))
				.addOption(new Option("C", false, "like -c, but do not report first bad line"))
				.addOption(Option.builder()
						.longOpt("check")
						.hasArg()
						.optionalArg(true)
						.argName("[arg]")
						.desc("none|diagnose-first: the same as -c, quiet|silent: the same as -C")
						.build());
		commandLineOptions = new Options()
				.addOptionGroup(sortKindOptionGroup)
				.addOptionGroup(processKindOptionGroup)
				.addOption("b", "ignore-leading-blanks", false, "ignore leading blanks")
				.addOption("d", "dictionary-order", false, "consider only blanks and alphanumeric characters")
				.addOption("f", "ignore-case", false, "fold lower case to upper case characters")
				.addOption("i", "ignore-nonprinting", false, "consider only printable characters")
				.addOption(Option.builder()
						.longOpt("random-source")
						.hasArg()
						.argName("FILE")
						.desc("get random bytes from FILE")
						.build())
				.addOption("r", "reverse", false, "reverse the result of comparisons")
				.addOption(Option.builder()
						.longOpt("batch-size")
						.hasArg()
						.argName("NMERGE")
						.desc("merge at most NMERGE inputs at once; for more use temp files")
						.build())
				.addOption(Option.builder()
						.longOpt("compress-program")
						.hasArg()
						.argName("PROG")
						.desc("compress temporaries with PROG; decompress them with PROG -d.  (Embedded GZIP is used regardless of PROG)")
						.build())
				.addOption(null, "debug", false, "annotate the part of the line used to sort, and warn about questionable usage to stderr")
				.addOption(Option.builder()
						.longOpt("files0-from")
						.hasArg()
						.argName("F")
						.desc("read input from the files specified by NUL-terminated names in file F; If F is '-' then read names from standard input")
						.build())
				.addOption(Option.builder("k")
						.longOpt("key")
						.hasArg()
						.argName("KEYDEF")
						.desc("sort via a key; KEYDEF gives location and type")
						.build())
				.addOption(Option.builder("o")
						.longOpt("output")
						.hasArg()
						.argName("FILE")
						.desc("write result to FILE instead of standard output")
						.build())
				.addOption("s", "stable", false, "stabilize sort by disabling last-resort comparison")
				.addOption(Option.builder("S")
						.longOpt("buffer-size")
						.hasArg()
						.argName("SIZE")
						.desc("use SIZE for main memory buffer")
						.build())
				.addOption(Option.builder("t")
						.longOpt("field-separator")
						.hasArg()
						.argName("SEP")
						.desc("use SEP instead of non-blank to blank transition. SEP:=c|'c'|'\\t'|'/t'")
						.build())
				.addOption(Option.builder("T")
						.longOpt("temporary-directory")
						.hasArg()
						.argName("DIR")
						.desc("use DIR for temporaries, not TMPDIR")
						.build())
				.addOption("u", "unique", false, "with -c, check for strict ordering; without -c, output only the first of an equal run")
				.addOption(Option.builder()
						.longOpt("parallel")
						.hasArg()
						.argName("N")
						.desc("change the number of sorts run concurrently to N")
						.build())
				.addOption("z", "zero-terminated", false, "line delimiter is NUL, not newline")
				.addOption(Option.builder()
						.longOpt("header")
						.hasArg()
						.argName("n [, every|first]")
						.desc("n: the number of header lines, every: every file has header lines, first: only the first file has header lines.")
						.build())
				.addOption(Option.builder()
						.longOpt("locale")
						.hasArg()
						.argName("LOCALE")
						.desc("use LOCALE for collation; none, default or language [, country [, variant]]. LOCALE:=locale|text-locale,number-locale")
						.build())
				.addOption(Option.builder()
						.longOpt("encoding")
						.hasArg()
						.argName("CHARSET")
						.desc("use CHARSET to read and write. CHARSET:=charset|in-charset, out-charset")
						.build())
				.addOption(null, "cli", false, "run in CLI mode")
				.addOption(null, "help", false, "show help message");
	}
	
	static public boolean isPrintable( char c ) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }
	
	static String transform(KeyField keyField, String line, int startFieldIndex, int endFieldIndex) {
		final boolean applyIgnore = keyField.ignore;
		final boolean applyTranslate = keyField.translate;
		final char[] ca = line.toCharArray();
		final StringBuilder builder = new StringBuilder();
		
		
		for(int i = startFieldIndex; i < endFieldIndex; i++) {
			int c = ca[i];
			boolean ignore = false; 
			if(applyIgnore) {
				switch(keyField.ignoreKind) {
				case NoDictionary:
					ignore = !(Character.isAlphabetic(c) || Character.isDigit(c) || Character.isWhitespace(c));
					break;
				case NoPrinting:
					ignore = !isPrintable((char)c);
					break;
				default:
					throw new IllegalStateException("Unknown IgnoreKind");
				}
			}
			if(!ignore) {
				if(applyTranslate) {
					switch(keyField.translateKind) {
					case ToUpper:
						c = Character.toUpperCase(c);
						break;
					default:
						throw new IllegalStateException("Unknown TranslateKind");
					}
				}
				builder.append((char)c);
			}
		}
		return builder.toString();
	}
	
	static int[] splitLine(String line) {
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		char[] characters = line.toCharArray();
		indexList.add(0); // start
		for(int i = 0; i < characters.length;) {
			for(; i < characters.length && Character.isWhitespace(characters[i]); i++) {
			}
			for(; i < characters.length && !Character.isWhitespace(characters[i]); i++) {
			}
			if(i < characters.length) {
				indexList.add(i); // limit
				indexList.add(i); // start
			}
		}
		indexList.add(characters.length); // limit
		int[] indexes = new int[indexList.size()];
		for(int i = 0; i < indexes.length; i++) {
			indexes[i] = indexList.get(i);
		}
		return indexes;
	}

	static int[] splitLine(String line, char separator) {
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		char[] characters = line.toCharArray();
		indexList.add(0); // start
		for(int i = 0; i < characters.length; i++) {
			if(characters[i] == separator) {
				indexList.add(i); // limit
				indexList.add(i + 1); // start
			}
		}
		indexList.add(characters.length); // limit
		int[] indexes = new int[indexList.size()];
		for(int i = 0; i < indexes.length; i++) {
			indexes[i] = indexList.get(i);
		}
		return indexes;
	}
	
	static int estimateChunkSize(Configuration configuration) {
		
		String suffix = configuration.bufferSizeSuffix; //%bKMGTPEZY
		long bufferSize = configuration.bufferSize;
		if(bufferSize != 0) {
			if(suffix.equals("%")) {
				bufferSize = (long)(Runtime.getRuntime().maxMemory() * ((float)configuration.bufferSize/100));
			} else {
				long unit;
				switch(suffix) {
				case "": case "b":
					unit = 1;
					break;
				case "K":
					unit = 1024;
					break;
				case "M":
					unit = 1024*1024;
					break;
				case "G":
				default:
					unit = 1024*1024*1024;
				}
				bufferSize = bufferSize * unit;
			}
		}
		bufferSize = Math.max(10 * 1024 * 1024, bufferSize);
		return (int)(bufferSize / 256);  // Assuming a line takes 256 bytes. 
	}
	
	static void sort(Configuration configuration) throws IOException {
		TextLineComparator externalTextLineComparator = new TextLineComparator(configuration, false);
		ExternalSerializer externalSerializer = new ExternalSerializer(configuration, externalTextLineComparator);
		TextLineComparator internalTextLineComparator = new TextLineComparator(configuration, configuration.stable);
		InternalSerializer internalSerializer = new InternalSerializer(configuration, configuration.stable);
		ExternalMergeSort<TextLine> sort = ExternalMergeSort.newSorter(internalSerializer, internalTextLineComparator)
				.withChunkSize(estimateChunkSize(configuration))
				.withMaxOpenFiles(configuration.mergeBatchSize)
				//.withDistinct(configuration.unique) // Doesn't work when the output fits in a single chunk.
				.withDistinct(false)	// The default is true, let's make it false.
				.withCleanup(true)
				.withTempDirectory((configuration.tmpDirectory != null)? new File(configuration.tmpDirectory): null)
				.build();
		List<File> sortedChunks = sort.writeSortedChunks(externalSerializer .readValues());
		CloseableIterator<TextLine> sorted = sort.mergeSortedChunks(sortedChunks);
		externalSerializer.writeValues(sorted);
		sorted.close();
	}
	
	static ExitStatus merge(Configuration configuration) throws IOException {
		TextLineComparator externalTextLineComparator = new TextLineComparator(configuration, false);
		ExternalSerializer externalSerializer = new ExternalSerializer(configuration, externalTextLineComparator);
		ExternalTextFileIterator externalTextFileIterator = externalSerializer.externalTextFileIterator;
		TextLineComparator internalTextLineComparator = new TextLineComparator(configuration, false);
		InternalSerializer internalSerializer = new InternalSerializer(configuration, false);
		String[] inputFiles =  (configuration.inputFileNames == null)? new String[] {null}: configuration.inputFileNames;
		File tempDirectory = (configuration.tmpDirectory != null)? new File(configuration.tmpDirectory): null;
		ArrayList<File> sortedChunks = new ArrayList<File>();
		try { // this try section is to handle temporary file cleanup. 
			for(String inputFile: inputFiles) {
				externalTextFileIterator.setFile(inputFile);
				File tempFile = File.createTempFile("sort", ".chunk", tempDirectory);
				tempFile.deleteOnExit();
				SortedItertorFilter sortedItertorFilter = new SortedItertorFilter(externalTextFileIterator, externalTextLineComparator, false);
				OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));
				try {
					internalSerializer.writeValues(sortedItertorFilter, out);
				} finally {
					out.close();
				}
				ExitStatus status = sortedItertorFilter.status;
				if(status != ExitStatus.Success) {
					int fileNumber = externalTextFileIterator.fileNumber;
					int lineNumber = externalTextFileIterator.lineNumber;
					System.err.println("File: " + fileNumber + " line: " + lineNumber + ": " + sortedItertorFilter.getFailureMessage());
					return status;
				}
				sortedChunks.add(tempFile);
			}
			ExternalMergeSort<TextLine> sort = ExternalMergeSort.newSorter(internalSerializer, internalTextLineComparator)
					.withChunkSize(estimateChunkSize(configuration))
					.withMaxOpenFiles(configuration.mergeBatchSize)
					//.withDistinct(configuration.unique) // Doesn't work when the output fits in a single chunk.
					.withDistinct(false)	// The default is true, let's make it false.
					.withCleanup(true)
					.withTempDirectory((configuration.tmpDirectory != null)? new File(configuration.tmpDirectory): null)
					.build();
			CloseableIterator<TextLine> sorted = sort.mergeSortedChunks(sortedChunks);
			externalSerializer.writeValues(sorted);
			sorted.close();
		} finally {
			for(File sortedChunk: sortedChunks) {
				sortedChunk.delete();
			}
		}
		return ExitStatus.Success;
	}
	
	static ExitStatus check(Configuration configuration) throws IOException {
		TextLineComparator externalTextLineComparator = new TextLineComparator(configuration, false);
		ExternalSerializer externalSerializer = new ExternalSerializer(configuration, externalTextLineComparator);
		ExternalTextFileIterator externalTextFileIterator = externalSerializer.externalTextFileIterator;
		InternalSerializer internalSerializer = new InternalSerializer(configuration, false);
		String[] inputFiles =  (configuration.inputFileNames == null)? new String[] {null}: configuration.inputFileNames;
		for(String inputFile: inputFiles) {
			externalTextFileIterator.setFile(inputFile);
			SortedItertorFilter sortedItertorFilter = new SortedItertorFilter(externalTextFileIterator, externalTextLineComparator, configuration.unique);
			OutputStream out = new BufferedOutputStream(new NullOutputStream());
			try {
				internalSerializer.writeValues(sortedItertorFilter, out);
			} finally {
				out.close();
			}
			ExitStatus status = sortedItertorFilter.status;
			if(status != ExitStatus.Success) {
				switch(configuration.checkKind) {
				case DiagnoseFirst:
					int fileNumber = externalTextFileIterator.fileNumber;
					int lineNumber = externalTextFileIterator.lineNumber;
					System.err.println("File: " + fileNumber + " line: " + lineNumber + ": " + sortedItertorFilter.getFailureMessage());
					break;
				case Quiet:
					break;
				case Silent:
					break;
				default:
					throw new IllegalStateException("Unknown CheckKind");
				}
				return status;
			}
		}
		return ExitStatus.Success;
	}
	
	static String[] filesFrom(String fileSpec, String option) throws UnrecognizedOptionException, IOException {
		Charset charset = Charset.defaultCharset();
		String sep = "\0";
		String[] segments = fileSpec.split(",");
		String fileName = segments[0];
		for(int i = 1; i < segments.length; i++) {
			String segment = segments[i].trim();
			if(segment.matches("(?i)encoding *= *.*")) {
				String optionValue = segment.substring(segment.indexOf('=') + 1).trim();
				charset = Charset.forName(optionValue);
			} else if(segment.matches("(?i)sep *= *.*")) {
				String optionValue = segment.substring(segment.indexOf('=') + 1).trim();
				if(optionValue.matches("'.+'")) {
					StringBuilder builder = new StringBuilder();
					for(int j = 1; j < optionValue.length() - 1; j++) {
						char c = optionValue.charAt(j);
						if((c == '\\' || c == '/') && j + 1 <  optionValue.length() - 1) {
							j++;
							c = optionValue.charAt(j);
							switch(c) {
							case 't': builder.append('\t'); break;
							case 'r': builder.append('\r'); break;
							case 'n': builder.append('\n'); break;
							case 'f': builder.append('\f'); break;
							case '0': builder.append('\0'); break;
							default: builder.append(c);
							}
						} else {
							builder.append(c);
						}
					}
					sep = builder.toString();
				} else {
					throw new UnrecognizedOptionException(option + ": sep expects '...'");
				}
			} else {
				throw new UnrecognizedOptionException(option + ": encoding or sep expected");
			}
		}
		boolean stdin = fileName.equals("-");
		final InputStream in;
		if(stdin) {
			in = System.in;
		} else {
			in = new FileInputStream(fileName);
		}
		ArrayList<String> fileList = new ArrayList<String>();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int c;
		while((c = in.read()) != -1) {
			if(sep.indexOf((char)c) >= 0) {
				if(out.size() != 0) {
					String file = out.toString(charset.name()).trim();
					if(file.length() != 0) {
						fileList.add(file);
					}
					out.reset();
				}
			} else {
				out.write(c);
			}
		}
		if(out.size() != 0) {
			String file = out.toString(charset.name()).trim();
			if(file.length() != 0) {
				fileList.add(file);
			}
			out.reset();
		}
		if(!stdin) {
			in.close();
		}
		return fileList.toArray(new String[fileList.size()]);
	}
	
	static int numberOption(String value, String option) throws OptionTypeError {
		if(value.matches("\\d+")) {
			return Integer.valueOf(value);
		} else {
			throw new OptionTypeError("Number expected", option);
		}
	}
	
	static char charOption(String optionValue, String option) throws OptionTypeError, UnrecognizedOptionException {
		if(optionValue.length() == 1) {
			return optionValue.charAt(0);
		} else {
			if(optionValue.matches("'.+'")) {
				if(optionValue.length() == 3) {
					return optionValue.charAt(1);
				} else if(optionValue.length() == 4) {
					char c = optionValue.charAt(1);
					if(c == '\\' || c == '/') {
						c = optionValue.charAt(2);
						switch(c) {
						case 't': c = '\t'; break;
						case 'r': c = '\r'; break;
						case 'n': c = '\n'; break;
						case 'f': c = '\f'; break;
						case '0': c = '\0'; break;
						}
						return c;
					}
				}
			} 
		}
		throw new UnrecognizedOptionException(option + " expects x, 'x', '\\x' or '/x'");
	}
	
	static Charset[] encoding(String optionValue, String option) throws UnrecognizedOptionException {
		Charset[] charsets = new Charset[2];
		String[] segments = optionValue.split(",");
		if(segments.length == 1) {
			charsets[0] = charsets[1] = Charset.forName(segments[0].trim()); 
		} else if(segments.length == 2) {
			charsets[0] = Charset.forName(segments[0].trim());
			charsets[1] = Charset.forName(segments[1].trim()); 
		} else {
			throw new UnrecognizedOptionException(option + ": Expected charset or incharset, out-charset");
		}
		return charsets;
	}
	
	static void header(Configuration configuration, String optionValue, String option) throws OptionTypeError, UnrecognizedOptionException {
		String[] segments = optionValue.split(",");
		configuration.headerLines = numberOption(segments[0].trim(), option);
		if(segments.length < 2) {
			configuration.headerEveryFile = true;
		} else if(segments.length == 2) {
			String value = segments[1].trim();
			if(value.equalsIgnoreCase("every")) {
				configuration.headerEveryFile = true;
			} else if(value.equalsIgnoreCase("first")) {
				configuration.headerEveryFile = false;
			} else {
				throw new UnrecognizedOptionException(option + ": Expected every or first");
			}
		} else {
			throw new UnrecognizedOptionException(option + ": Expected n [, every|first] ");
		}
	}

	static Locale[] locale(String optionValue, String option) throws UnrecognizedOptionException {
		Locale[] locales = new Locale[2];
		String[] segments = optionValue.split(",");
		if(segments.length >= 1) {
			if(segments[0].trim().equalsIgnoreCase("none") || optionValue.equalsIgnoreCase("no")) {
				locales[0] = null;
			} else if(segments[0].trim().equalsIgnoreCase("default")) {
				locales[0] =  Locale.getDefault();
			} else {
				locales[0] =  Locale.forLanguageTag(segments[0].trim());
			}
		} 
		if(segments.length == 1) {
			locales[1] = locales[0];
		} else if(segments.length == 2) {
			if(segments[1].trim().equalsIgnoreCase("none") || optionValue.equalsIgnoreCase("no")) {
				locales[1] = null;
			} else if(segments[1].trim().equalsIgnoreCase("default")) {
				locales[1] =  Locale.getDefault();
			} else {
				locales[1] =  Locale.forLanguageTag(segments[1].trim());
			}
		} else {
			throw new UnrecognizedOptionException(option + ": Expected locale or text-locale, number-locale");
		}
		return locales;
		/*
		if(optionValue.equalsIgnoreCase("none") || optionValue.equalsIgnoreCase("no")) {
			return null;
		} else if(optionValue.equalsIgnoreCase("default")) {
			return Locale.getDefault();
		} else {
			return Locale.forLanguageTag(optionValue);
			
			String[] segments = optionValue.split(",");
			if(segments.length == 1) {
				return new Locale(segments[0].trim());
			} else if(segments.length == 2) {
				return new Locale(segments[0].trim(), segments[1].trim());
			} else if(segments.length == 3) {
				return new Locale(segments[0].trim(), segments[1].trim(), segments[2].trim());
			} else {
				throw new UnrecognizedOptionException("Expected language [, country [, variant]]" + option);
			}

		}
		*/
	}
	
	static KeyField globalKeyField(CommandLine commandLine) throws UnrecognizedOptionException {
		KeyField keyField = new KeyField();
		if(commandLine.hasOption("sort")) {
			switch(commandLine.getOptionValue("sort")) {
			case "general-numeric": keyField.sortKind = SortKind.GeneralNumeric; break;
			case "human-numeric": keyField.sortKind = SortKind.HumanNumeric; break;
			case "numeric": keyField.sortKind = SortKind.Numeric; break;
			case "month": keyField.sortKind = SortKind.Month; break;
			case "random": keyField.sortKind = SortKind.Random; break;
			case "version": keyField.sortKind = SortKind.Version; break;
			default:
				throw new UnrecognizedOptionException("--sorts expects general-numeric, human-numeric, numeric, month, random or version.");
			}
		} else if(commandLine.hasOption("g")) {
			keyField.sortKind = SortKind.GeneralNumeric;
		} else if(commandLine.hasOption("h")) {
			keyField.sortKind = SortKind.HumanNumeric;
		} else if(commandLine.hasOption("n")) {
			keyField.sortKind = SortKind.Numeric;
		} else if(commandLine.hasOption("M")) {
			keyField.sortKind = SortKind.Month;
		} else if(commandLine.hasOption("R")) {
			keyField.sortKind = SortKind.Random;
		} else if(commandLine.hasOption("V")) {
			keyField.sortKind = SortKind.Version;
		} else {
			// Nothing to do
		}
		if(commandLine.hasOption("b")) {
			keyField.skipStartBlanks = true;
			keyField.skipEndBlanks = true;
		}
		if(commandLine.hasOption("d")) {
			keyField.ignore = true;
			keyField.ignoreKind = IgnoreKind.NoDictionary;
		}
		if(commandLine.hasOption("i")) {
			keyField.ignore = true;
			keyField.ignoreKind = IgnoreKind.NoPrinting;
		}
		if(commandLine.hasOption("f")) {
			keyField.translate = true;
			keyField.translateKind = TranslateKind.ToUpper;
		}
		if(commandLine.hasOption("r")) {
			keyField.reverse = true;
		}
		return keyField;
	}
	
	static void applyFCOPTS(KeyField keyField, String fcopts, boolean start, String option) throws MissingArgumentException, UnrecognizedOptionException, AlreadySelectedException {
		char[] ca = fcopts.toCharArray();
		int i = 0;
		for(; i < ca.length; i++) {
			if(!Character.isDigit(ca[i])) {
				break;
			}
		}
		if(i == 0) {
			throw new MissingArgumentException(option + ": Field number expected");
		}
		int fieldPos = Integer.valueOf(fcopts.substring(0, i));
		if(fieldPos <= 0) {
			throw new IllegalArgumentException(option + ": Field position needs to be 1 or more: " + fieldPos);
		}
		int charPos = 0;
		if(i < ca.length && ca[i] == '.') {
			i++;
			int n = i;
			for(; i < ca.length; i++) {
				if(!Character.isDigit(ca[i])) {
					break;
				}
			}
			if(i == n) {
				throw new MissingArgumentException(option + ": Character number expected");
			}
			charPos = Integer.valueOf(fcopts.substring(n, i));
			if(charPos <= 0) {
				throw new IllegalArgumentException(option + ": Character position needs to be 1 or more: " + charPos);
			}
		}
		if(start) {
			keyField.startField = fieldPos;
			keyField.startChar = charPos;
		} else {
			keyField.endField = fieldPos;
			keyField.endChar = charPos;
		}
		char sortOption = '?';
		char ignoreOption = '?';
		
		for(char c: fcopts.substring(i).toCharArray()) {
			switch(c) {
			case'b':
				if(start)
					keyField.skipStartBlanks=true;
				else
					keyField.skipEndBlanks=true;
				break;
			case'd':
				if(keyField.ignore) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + ignoreOption + "'");
				}
				ignoreOption = c;
				keyField.ignore = true;
				keyField.ignoreKind = IgnoreKind.NoDictionary;
				break;
			case'f':
				keyField.translate = true;
				keyField.translateKind = TranslateKind.ToUpper;
				break;
			case'g':
				if(keyField.sortKind != SortKind.Text) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + sortOption + "'");
				}
				sortOption = c;
				keyField.sortKind = SortKind.GeneralNumeric;
				break;
			case'h':
				if(keyField.sortKind != SortKind.Text) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + sortOption + "'");
				}
				sortOption = c;
				keyField.sortKind = SortKind.HumanNumeric;
				break;
			case'i':
				if(keyField.ignore) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + ignoreOption + "'");
				}
				ignoreOption = c;
				keyField.ignore = true;
				keyField.ignoreKind = IgnoreKind.NoPrinting;
				break;
			case'M':
				if(keyField.sortKind != SortKind.Text) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + sortOption + "'");
				}
				sortOption = c;
				keyField.sortKind = SortKind.Month;
				break;
			case'n':
				if(keyField.sortKind != SortKind.Text) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + sortOption + "'");
				}
				sortOption = c;
				keyField.sortKind = SortKind.Numeric;
				break;
			case'R':
				if(keyField.sortKind != SortKind.Text) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + sortOption + "'");
				}
				sortOption = c;
				keyField.sortKind = SortKind.Random;
				break;
			case'r':
				sortOption = c;
				keyField.reverse=true;
				break;
			case'V':
				if(keyField.sortKind != SortKind.Text) {
					throw new AlreadySelectedException(option + ": The option '" + c + "' was specified but an option from this group has already been selected: '" + sortOption + "'");
				}
				sortOption = c;
				keyField.sortKind = SortKind.Version;
				break;
			default:
				throw new UnrecognizedOptionException(option + ": '" + c + "' is not recognized");
			}		
		}
		
	}
	
	static KeyField keyField(String keydef, KeyField globalKeyField, String option) throws MissingArgumentException, UnrecognizedOptionException, AlreadySelectedException  {
		// "KEYDEF is F[.C][OPTS][,F[.C][OPTS]]
		KeyField keyField = new KeyField();
		String[] segments = keydef.split(",");
		applyFCOPTS(keyField, segments[0].trim(), true, option);
		if(segments.length >= 2) {
			applyFCOPTS(keyField, segments[1].trim(), false, option);
		}
		if(segments.length > 2) {
			throw new UnrecognizedOptionException(option + ": Extra field specification");
		}
		/*
		if(keyField.endField < keyField.startField) {
			throw new IllegalArgumentException(option + ": Key position range error - EndField < StartField");
		} else if(keyField.endField == keyField.startField) {
			if(keyField.endChar != 0) {
				if(keyField.endChar < keyField.startChar) {
					throw new IllegalArgumentException(option + ": Key position range error - EndField = StartField and EndChar < startChar");
				}
			}
		} else {
			// Nothing to do
		}
		*/
		if(keyField.isDefault()) {
	          keyField.ignore = globalKeyField.ignore;
	          keyField.ignoreKind = globalKeyField.ignoreKind;
	          keyField.translate = globalKeyField.translate;
	          keyField.translateKind = globalKeyField.translateKind;
	          keyField.skipStartBlanks = globalKeyField.skipStartBlanks;
	          keyField.skipEndBlanks = globalKeyField.skipEndBlanks;
	          keyField.sortKind = globalKeyField.sortKind;
	          keyField.reverse = globalKeyField.reverse;
		}
		return keyField;
	}
	
	static Configuration buildConfiguration(CommandLine commandLine) throws UnrecognizedOptionException, OptionTypeError, IOException, AlreadySelectedException, MissingArgumentException {
		Configuration configuration = new Configuration();
		if(commandLine.hasOption("m")) {
			configuration.processKind = ProcessKind.Merge;
		} else if(commandLine.hasOption("c")) {
			configuration.processKind = ProcessKind.Check;
			configuration.checkKind = CheckKind.DiagnoseFirst;
		} else if(commandLine.hasOption("C")) {
			configuration.processKind = ProcessKind.Check;
			configuration.checkKind = CheckKind.Quiet;
		} else if(commandLine.hasOption("check")) {
			configuration.processKind = ProcessKind.Check;
			if(commandLine.hasOption("check")) {
				String checkValue = commandLine.getOptionValue("check");
				if(checkValue == null) {
					configuration.checkKind = CheckKind.DiagnoseFirst;
				} else {
					switch(checkValue) {
					case "diagnose-first": configuration.checkKind = CheckKind.DiagnoseFirst; break;
					case "quiet": case "silent": 
						configuration.checkKind = CheckKind.Quiet;
						break;
					default:
						throw new UnrecognizedOptionException("--check expects diagnose-first, quiet or silent.");
					}
				}
			}
		} else {
			// Nothing to do
		}
		if(commandLine.hasOption("random-source")) {
			configuration.randomSource = commandLine.getOptionValue("random-source");
		}
		if(commandLine.hasOption("batch-size")) {
			configuration.mergeBatchSize = numberOption(commandLine.getOptionValue("batch-size"), "--batch-size");
		}
		if(commandLine.hasOption("compress-program")) {
			configuration.compressProgram = commandLine.getOptionValue("compress-program");
		}
		if(commandLine.hasOption("debug")) {
			configuration.debug = true;
		}
		if(commandLine.hasOption("files0-from")) {
			configuration.inputFileNames = filesFrom(commandLine.getOptionValue("files0-from"), "files0-from");
		}
		if(commandLine.hasOption("o")) {
			configuration.outputFileName = commandLine.getOptionValue("o");
		}
		if(commandLine.hasOption("s")) {
			configuration.stable = true;
		}
		if(commandLine.hasOption("S")) {
			String optionValue = commandLine.getOptionValue("S");
			if(optionValue.matches("\\d+[%bKMGTPEZY]")) {
				configuration.bufferSize = numberOption(optionValue.substring(0, optionValue.length() - 1), "-S");
				configuration.bufferSizeSuffix = optionValue.substring(optionValue.length() - 1);
			} else {
				configuration.bufferSize = numberOption(optionValue, "-S");
			}
		}
		if(commandLine.hasOption("t")) {
			configuration.fieldSeparator = charOption(commandLine.getOptionValue("t"), "-t");
			configuration.defaultFieldSeparator = false;
		}
		if(commandLine.hasOption("T")) {
			configuration.tmpDirectory = commandLine.getOptionValue("T");
		}
		if(commandLine.hasOption("u")) {
			configuration.unique = true;
		}
		if(commandLine.hasOption("parallel")) {
			configuration.numberOfParallel = numberOption(commandLine.getOptionValue("parallel"), "--parallel");
		}
		if(commandLine.hasOption("z")) {
			configuration.zeroTerminated = true;
		}
		if(commandLine.hasOption("header")) {
			header(configuration, commandLine.getOptionValue("header"), "--header");
		}
		if(commandLine.hasOption("locale")) {
			Locale[] locales = locale(commandLine.getOptionValue("locale"), "--locale");
			configuration.textLocale = locales[0];
			configuration.numberLocale = locales[1];
		}
		if(commandLine.hasOption("encoding")) {
			Charset[] charsets =  encoding(commandLine.getOptionValue("encoding"), "--encoding");
			configuration.inputEncoding = charsets[0];
			configuration.outputEncoding = charsets[1];
		}
		String[] args = commandLine.getArgs();
		if(args.length > 0) {
			if(configuration.inputFileNames == null) {
				if(!(args.length == 1 && args[0].equals("-"))) {
					configuration.inputFileNames = args;
				}
			} else {
				throw new AlreadySelectedException("--files0-from: " + 
					(args.length == 1?"input file was": "input files were")
					+ " specified but input has already been selected");
			}
		}
		
		KeyField globalKeyField = globalKeyField(commandLine);
		globalKeyField.startField = Integer.MAX_VALUE;
		ArrayList<KeyField> keyFieldList = new ArrayList<KeyField>();
		if(commandLine.hasOption("k")) {
			String[] keydefs = commandLine.getOptionValues("k");
			for(int i = 0; i < keydefs.length; i++) {
				String keydef = keydefs[i];
				keyFieldList.add(keyField(keydef, globalKeyField, "-k[" + i + "]"));
			}
		}
		if(keyFieldList.size() == 0 && !globalKeyField.isDefaultExceptForReverse()) {
			configuration.globalKeyOnly = true;
			keyFieldList.add(globalKeyField);
		}
		for(KeyField keyField: keyFieldList) {
			if(keyField.sortKind == SortKind.Random) {
				configuration.hasRandom = true;
			}
		}
		configuration.keyFields = keyFieldList.toArray(new KeyField[keyFieldList.size()]);
		configuration.reverse = globalKeyField.reverse;
		
		return configuration;
	}
	
	static void showHelpMessage() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(80);
		String footer = "\n" +
				"KEYDEF is F[.C][OPTS][,F[.C][OPTS]] for start and stop position, where F is a\n" +
				"field number and C a character position in the field; both are origin 1, and\n" +
				"the stop position defaults to the line's end.  If neither -t nor -b is in\n" +
				"effect, characters in a field are counted from the beginning of the preceding\n" +
				"whitespace.  OPTS is one or more single-letter ordering options [bdfgiMhnRrV],\n" +
				"which override global ordering options for that key.  If no key is given, use\n" +
				"the entire line as the key.  Use --debug to diagnose incorrect key usage.\n" +
				"\n" +
				"SIZE may be followed by the following multiplicative suffixes:\n" +
				"% 1% of memory, b 1, K 1024 (default), and so on for M, G, T, P, E, Z, Y.\n";
		System.out.println(TITLE + " - Sort text files");
		formatter.printHelp( "Sort [option ...] [input-file ...]", commandLineOptions);
		System.out.println(footer);
	}
	
	static void warning(String message) {
		System.out.flush();
		System.err.println("Warning: " + message);
		System.err.flush();
	}
	
	static String keyName(int i) {
		return "-k[" + i + "]";
	}
	
	static void checkConfiguration(Configuration configuration) {
		if(configuration.numberOfParallel != 0) {
			warning("--parallel: " + "Not supported.");
		}
		if(configuration.tmpDirectory != null) {
			File file = new File(configuration.tmpDirectory);
			if(!file.isDirectory()) {
				warning("--temporary-directory: " + "No such directory - " + configuration.tmpDirectory);
			}
		}
		if(configuration.randomSource != null && !configuration.hasRandom) {
			warning("--random-source: It's specified but no random keys are specified.");
		}
		if(configuration.keyFields != null) {
			KeyField[] keyFields = configuration.keyFields;
			for(int i = 0; i < keyFields.length; i++) {
				KeyField keyField = keyFields[i];
				boolean emptyField = false;
				if(keyField.startField > keyField.endField) {
					emptyField = true;
					warning(keyName(i) + ":  + Emply field - startField > endField");
				} else if(keyField.startField == keyField.endField) {
					if(keyField.skipStartBlanks == keyField.skipEndBlanks) {
						if(keyField.skipStartBlanks == keyField.skipEndBlanks && keyField.endChar != 0 && keyField.startChar >= keyField.endChar) {
							emptyField = true;
							warning(keyName(i) + ":  + Emply field - startField == endField and endChar <= startChar");
						}
					} else {
						warning(keyName(i) + ":  + StartField == endField and StartBlanks != skipEndBlanks");
					}
				} else {
					assert keyField.startField < keyField.endField;
				}
				
				final boolean hasAutoSkipEndBlanks;
				switch(keyField.sortKind) {
				case GeneralNumeric:
				case HumanNumeric:
				case Numeric:
				case Month:
				case Version:
					hasAutoSkipEndBlanks = true;
					break;
				case Random:
				case Text:
					hasAutoSkipEndBlanks = false;
					break;
				default:
					throw new IllegalStateException("Unknown SortKind");
				}
				// numericGroup has implicit skip blank, non-numericGroup not

				// if(GNU_SORT_COMPATIBLE) {
				boolean lineOffset = keyField.endField == 1 && keyField.endChar > 0; /* -k1.x,1.y  */
				if (!emptyField && !configuration.globalKeyOnly && configuration.defaultFieldSeparator && !lineOffset
					&& (   (!keyField.skipStartBlanks && !hasAutoSkipEndBlanks)
							|| (!keyField.skipStartBlanks && keyField.startChar > 0)
							|| (!keyField.skipEndBlanks && keyField.endChar > 0))) {

					warning(keyName(i) + ": Leading blanks are significant in " + keyField.sortKind + ".  Consider also specifying 'b'");
				}
				// }
				if((GNU_SORT_COMPATIBLE? configuration.globalKeyOnly: true) && hasAutoSkipEndBlanks) {
					if(keyField.startField != Integer.MAX_VALUE && keyField.endField != keyField.startField) {
						warning(keyName(i) + ": " + keyField.sortKind + " field spans multiple fields");
					}
				}
			}
		}
	}

	public static void main(String args[]) {
		if(args.length != 0) {
			ExitStatus exitStatus = ExitStatus.Success; // Assume success at first.
			try {
				CommandLineParser parser = new DefaultParser();
				CommandLine commandLine = parser.parse(commandLineOptions, args);
				if(commandLine.hasOption("help")) {
					showHelpMessage();
				} else if(args.length >= 1 && args[0].equals("--version")) {
					// commandLineOptions can't accept --version and --version-sort
					System.out.println(TITLE + " - Sort text files");
				} else {
					Configuration configuration = buildConfiguration(commandLine);
					if(configuration.debug) {
						checkConfiguration(configuration);
					}
					if(configuration.debug) {
						configuration.print(new IndentedReporter(System.out));					
					}
					switch(configuration.processKind) {
					case Check:
						exitStatus = check(configuration);
						break;
					case Merge:
						exitStatus = merge(configuration);
						break;
					case Sort:
						sort(configuration);
						break;
					default:
						throw new IllegalStateException("Unknown ProcessKind");
					}
				}
			} catch(Throwable e) {
				if(	e instanceof UnrecognizedOptionException
					|| e instanceof MissingArgumentException
					|| e instanceof AlreadySelectedException
					|| e instanceof OptionTypeError) {
					System.err.println(e.getMessage());
				} else {
					e.printStackTrace(System.err);
				}
				exitStatus = ExitStatus.Failure;
			}
			System.exit(exitStatus.getStatus());
		} else {
			SortUI.main(args);
		}
    }
}
