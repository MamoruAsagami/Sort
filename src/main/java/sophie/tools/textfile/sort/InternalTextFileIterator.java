package sophie.tools.textfile.sort;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

class InternalTextFileIterator implements Iterator<TextLine> {
	KeyField[] keyFields;
	boolean stable;
	DataInputStream dataIn;
	TextLine textLine;
	GZIPInputStream gzipIn;
	
	private TextLine read() throws IOException {
		TextLine textLine = new TextLine();
		if(stable) {
			int n = dataIn.readInt();
			if((n & 0x8000_0000) == 0) { // <= Integer.MAX_VALUE
				textLine.seq = n;
			} else {
				// > Integer.MAX_VALUE
				int high = n & 0x7fff_ffff; // high 32 bits (clear long mark)
				int low = dataIn.readInt(); // low 32 bits
				textLine.seq = ((long)high << 32) | ((long)low & 0xffff_ffffL);
			}
		}
		if(keyFields.length != 0) {
			textLine.fields = new Field[keyFields.length];
			for(int i = 0; i < keyFields.length; i++) {
				Field field = textLine.fields[i] = new Field();
				switch(keyFields[i].sortKind) {
				case Text:
					field.start = dataIn.readChar();
					field.limit = dataIn.readChar();
					break;
				case GeneralNumeric:
					field.signedMagnitude = dataIn.readShort();
					field.realNumber = dataIn.readDouble();
					break;
				case HumanNumeric:
					{
						field.signedMagnitude = dataIn.readShort();
						int length = dataIn.readInt();
						field.integralPart = new byte[length];
						dataIn.read(field.integralPart, 0, length);
						length = dataIn.readInt();
						field.fractionalPart = new byte[length];
						dataIn.read(field.fractionalPart, 0, length);
						field.signedMagnitude = dataIn.readShort();
					}
					break;
				case Numeric:
					{
						field.signedMagnitude = dataIn.readShort();
						int length = dataIn.readInt();
						field.integralPart = new byte[length];
						dataIn.read(field.integralPart, 0, length);
						length = dataIn.readInt();
						field.fractionalPart = new byte[length];
						dataIn.read(field.fractionalPart, 0, length);
					}
					break;
				case Month:
					field.month = dataIn.readShort();
					break;
				case Random:
					int length = dataIn.readInt();
					field.digest = new byte[length];
					dataIn.read(field.digest, 0, length);
					break;
				case Version:
					field.version = dataIn.readUTF();
					break;
				default:
					throw new IllegalStateException("Unknown SortKind");
				}
			}
		}
		textLine.line = dataIn.readUTF();
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
			if(gzipIn != null) {
				try {
					gzipIn.close();
					gzipIn = null;
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
			}
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
				if(gzipIn != null) {
					try {
						gzipIn.close();
						gzipIn = null;
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}
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
	
	InternalTextFileIterator(Configuration configuration, InputStream in) throws IOException {
		this.keyFields = configuration.keyFields;
		this.stable = configuration.stable;
		if(configuration.compressProgram != null) {
			in = new GZIPInputStream(in);
		}
		dataIn = new DataInputStream(in);
	}
}