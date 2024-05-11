package sophie.tools.textfile.sort;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import org.geirove.exmeso.ExternalMergeSort;

class InternalSerializer implements ExternalMergeSort.Serializer<TextLine> {
	Configuration configuration;
	KeyField[] keyFields;
	boolean stable;

	@Override
	public Iterator<TextLine> readValues(InputStream in) throws IOException {
		return new InternalTextFileIterator(configuration, in);
	}

	@Override
	public void writeValues(Iterator<TextLine> lines, OutputStream out) throws IOException {
		GZIPOutputStream gzipOut = null;
		if(configuration.compressProgram != null) {
			 out = gzipOut = new GZIPOutputStream(out);
		}
		DataOutputStream dataOut = new DataOutputStream(out);
		while(lines.hasNext()) {
			TextLine textLine = lines.next();
			if(stable) {
				long seq = textLine.seq;
				if((seq & 0x7fff_ffff_8000_0000L) == 0) {
					// < Integer.MAX_VALUE
					dataOut.writeInt((int)seq);
				} else {
					// >= Integer.MAX_VALUE
					dataOut.writeLong(seq | 0x8000_0000_0000_0000L /* long mark*/ );
				}
			}
			for(int i = 0; i < keyFields.length; i++) {
				Field field = textLine.fields[i];
				switch(keyFields[i].sortKind) {
				case Text:
					dataOut.writeShort(field.start);
					dataOut.writeShort(field.limit);
					if(field.text != null) {
						dataOut.writeBoolean(true);
						dataOut.writeUTF(field.text);
					} else {
						dataOut.writeBoolean(false);
					}
					break;
				case GeneralNumeric:
					dataOut.writeShort(field.signedMagnitude);
					dataOut.writeDouble(field.realNumber);
					break;
				case HumanNumeric:
					dataOut.writeShort(field.signedMagnitude);
					dataOut.writeInt(field.integralPart.length);
					dataOut.write(field.integralPart, 0, field.integralPart.length);
					dataOut.writeInt(field.fractionalPart.length);
					dataOut.write(field.fractionalPart, 0, field.fractionalPart.length);
					break;
				case Numeric:
					dataOut.writeShort(field.signedMagnitude);
					dataOut.writeInt(field.integralPart.length);
					dataOut.write(field.integralPart, 0, field.integralPart.length);
					dataOut.writeInt(field.fractionalPart.length);
					dataOut.write(field.fractionalPart, 0, field.fractionalPart.length);
					break;
				case Month:
					dataOut.writeShort(field.month);
					break;
				case Random:
					dataOut.writeInt(field.digest.length);
					dataOut.write(field.digest, 0, field.digest.length);
					if(Sort.GNU_SORT_COMPATIBLE) {
						dataOut.writeShort(field.start);
						dataOut.writeShort(field.limit);
						if(field.text != null) {
							dataOut.writeBoolean(true);
							dataOut.writeUTF(field.text);
						} else {
							dataOut.writeBoolean(false);
						}
					}
					break;
				case Version:
					dataOut.writeUTF(field.version);
					break;
				default:
					throw new IllegalStateException("Unknown SortKind");
				}
			}
			dataOut.writeUTF(textLine.line);
		}
		dataOut.flush();
		if(gzipOut != null) {
			gzipOut.finish();
		}
	}
	
	InternalSerializer(Configuration configuration, boolean stable) {
		this.configuration = configuration;
		this.keyFields = configuration.keyFields;
		this.stable = stable;
	}
}