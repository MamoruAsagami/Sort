package sophie.tools.textfile.sort;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

class ExternalSerializer {
	Configuration configuration;
	KeyField[] keyFields;
	TextLineComparator textLineComparator;
	ExternalTextFileIterator externalTextFileIterator;
	String lineSeparator;
	
	public ExternalTextFileIterator getExternalTextFileIterator() {
		return externalTextFileIterator;
	}

	//@Override
	public Iterator<TextLine> readValues() throws IOException {
		return externalTextFileIterator = new ExternalTextFileIterator(configuration);
	}
	
	private void writeLine(Writer writer, String line) throws IOException {
		writer.write(line);
		if(configuration.zeroTerminated) {
			writer.write(0);
		} else {
			writer.write(lineSeparator);
		}
	}

	//@Override
	public void writeValues(Iterator<TextLine> lines) throws IOException {
		lineSeparator = externalTextFileIterator.lineSeparator();
		OutputStream out = (configuration.outputFileName != null)? new FileOutputStream(configuration.outputFileName): System.out;
		Writer writer = new BufferedWriter(new OutputStreamWriter(out, configuration.outputEncoding));
		String[] header = externalTextFileIterator.getHeader();
		if(header != null) {
			for(String line: header) {
				writer.write(line);
				writer.write(lineSeparator);
			}
		}
		if(configuration.unique) {
			if(lines.hasNext()) {
				TextLine textLine = lines.next();
				writeLine(writer, textLine.line);
				TextLine prevLine = textLine;
				while(lines.hasNext()) {
					textLine = lines.next();
					if(textLineComparator.compare(textLine, prevLine) != 0) {
						writeLine(writer, textLine.line);
						prevLine = textLine;
					}
				}
			}
		} else {
			while(lines.hasNext()) {
				TextLine textLine = lines.next();
				writeLine(writer, textLine.line);
			}
		}
		writer.flush();
		if(configuration.outputFileName != null) {
			out.close();
		}
	}
	
	ExternalSerializer(Configuration configuration, TextLineComparator textLineComparator) throws IOException {
		this.configuration = configuration;
		this.keyFields = configuration.keyFields;
		this.textLineComparator = textLineComparator;
		externalTextFileIterator = new ExternalTextFileIterator(configuration);
	}
}