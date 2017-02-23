package sophie.tools.textfile.sort;

import java.text.Collator;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TextLineComparator implements Comparator<TextLine> {
	static Pattern versionSuffixPattern = Pattern.compile("(\\.[A-Za-z~][A-Za-z0-9~]*)*$");
	Configuration configuration;
	KeyField[] keyFields;
	Collator collator;
	boolean stable;
	boolean unique;
	boolean sequenceSignificant;
	
	private int textCompare(KeyField keyField, TextLine thisTextLine, Field thisField, TextLine thatTextLine, Field thatField) {
		String thisText = thisTextLine.line.substring(thisField.start, thisField.limit);
		String thatText = thatTextLine.line.substring(thatField.start, thatField.limit);
		if(keyField.ignore || keyField.translate) {
			thisText = Sort.transform(keyField, thisText, 0, thisText.length());
			thatText = Sort.transform(keyField, thatText, 0, thatText.length());
		}
		return (collator != null)? collator.compare(thisText, thatText): thisText.compareTo(thatText);
	}
	
	private int generalNumericCompare(Field thisField, Field thatField) {
		if(thisField.signedMagnitude < thatField.signedMagnitude) {
			return -1;
		} else if(thisField.signedMagnitude > thatField.signedMagnitude) {
			return 1;
		} else {
			return Double.compare(thisField.realNumber, thatField.realNumber);
		}
	}
	
	private boolean isEmptyNumber(Field field) {
		return field.integralPart.length == 0 && field.fractionalPart.length == 0;
	}
	
	@SuppressWarnings("unused")
	private boolean isZeroNumber(Field field) {
		return field.fractionalPart.length == 0 &&
				field.integralPart.length == 1  &&
				field.integralPart[0] == '0';
	}
	
	private int integralCompare(byte[] thisPart, byte[] thatPart) {
		if(thisPart.length < thatPart.length) {
			return -1;
		} else if(thisPart.length > thatPart.length) {
			return 1;
		} else {
			assert thisPart.length == thatPart.length;
			int i = 0;
			for(; i < thisPart.length && thisPart[i] == thatPart[i]; i++) {
			}
			if(i < thisPart.length) {
				return thisPart[i] < thatPart[i]? -1: 1;
			}
			return 0;
		}
	}
	
	private int fractionalCompare(byte[] thisPart, byte[] thatPart) {
		int i = 0;
		for(; i < thisPart.length && i < thatPart.length && thisPart[i] == thatPart[i]; i++) {
		}
		if(i < thisPart.length &&  i < thatPart.length) {
			return thisPart[i] < thatPart[i]? -1: 1;
		}
		if(thisPart.length < thatPart.length) {
			// 2.00 < 2.0 in this logic.
			return -1;
		} else if(thisPart.length > thatPart.length) {
			return 1;
		} else {
			return 0;
		}
	}
	
	private int numericCompare(Field thisField, Field thatField) {
		int thisSign = thisField.signedMagnitude;
		int thatSign = thatField.signedMagnitude;
		if(thisField.signedMagnitude < 0 && thatField.signedMagnitude >= 0) {
			return -1;
		} else if(thisField.signedMagnitude >= 0 && thatField.signedMagnitude < 0) {
			return 1;
		} else {
			assert thisSign == thatSign;
			int negater = (thisSign >= 0)? 1: -1;
			if(isEmptyNumber(thisField)) {
				return isEmptyNumber(thatField)? 0: -1 * negater;
			} else if(isEmptyNumber(thatField)) {
				return 1 * negater;
			} else {
				assert !(isEmptyNumber(thisField) || isEmptyNumber(thatField));
				int comp = integralCompare(thisField.integralPart, thatField.integralPart);
				if(comp != 0) {
					return comp * negater;
				}
				return fractionalCompare(thisField.fractionalPart, thatField.fractionalPart) * negater;
			}
		}
	}
	
	private int humanNumericCompare(Field thisField, Field thatField) {
		int thisSIsuffix = thisField.signedMagnitude;
		int thatSIsuffix = thatField.signedMagnitude;
		return (thisSIsuffix < thatSIsuffix)? -1:
			(thisSIsuffix > thatSIsuffix)? 1:
			numericCompare(thisField, thatField);	
	}
	
	private static int pvrOrder(int c) {
		// ~, [0-9], A,B,C, ..., a, b, ... z, ..., special characters
		if(Character.isDigit(c))
			return 0;
		else if (Character.isAlphabetic(c))
			return c;
		else if (c == '~')
			return -1;
		else 
			return Character.MAX_VALUE + 1 + c;
	}
	
	private static int pvrCompare(String pvr1, String pvr2) {
		// Prefix version revision compare
		int x1 = 0;
		int x2 = 0;
		char[] ca1 = pvr1 != null? pvr1.toCharArray(): new char[0];
		char[] ca2 = pvr2 != null? pvr2.toCharArray(): new char[0];
		int length1 = pvr1.length();
		int length2 = pvr2.length();
		while(x1 < length1 || x2 < length2) {
			int firstDiff = 0;
			while((x1 < length1 && !Character.isDigit(ca1[x1]))
				|| (x2 < length2 && !Character.isDigit(ca2[x2]))) {
				int c1 = (x1 >= length1) ? 0 : pvrOrder(ca1[x1]);
				int c2 = (x2 >= length2) ? 0 : pvrOrder(ca2[x2]);
				if(c1 != c2) {
					return c1 < c2? -1: 1;
				}
				if(x1 < length1) x1++;
				if(x2 < length2) x2++;
			}
			for(; x1 < length1 && ca1[x1] == '0'; x1++) {
				// Nothing to do.
			}
			for(; x2 < length2 && ca2[x2] == '0'; x2++) {
				// Nothing to do.
			}
			for(; x1 < length1 && Character.isDigit(ca1[x1]) && x2 < length2 && Character.isDigit(ca2[x2]); x1++, x2++) {
				if(firstDiff == 0) {
					if(ca1[x1] != ca2[x2]) {
						firstDiff = (ca1[x1] < ca2[x2])? -1: 1;
					}
				}
			}
			if(x1 < length1 && Character.isDigit(ca1[x1])) {
				return 1;
			}
			if(x2 < length2 && Character.isDigit(ca2[x2])) {
				return -1;
			}
			if(firstDiff != 0) {
				return firstDiff;
			}
		}
		return 0;
	}
	
	private static int versionCompare(String v1, String v2) {
		int simpleComp =  v1.compareTo(v2);
		if(simpleComp == 0)
			return 0;
		if(v1.length() == 0)
			return -1;
		if(v2.length() == 0)
			return 1;
		if(v1.equals("."))
			return -1;
		if(v2.equals("."))
			return 1;
		if(v1.equals(".."))
			return -1;
		if(v2.equals(".."))
			return 1;
		 /* special handle for other hidden files */
		if(v1.startsWith(".") &&  !v2.startsWith(".")) {
			return -1;
		}
		if(!v1.startsWith(".") &&  v2.startsWith(".")) {
			return 1;
		}
		if(v1.startsWith(".") &&  v2.startsWith(".")) {
			v1 = v1.substring(1);
			v2 = v2.substring(1);
		}
		final String v1PrefixVerRev;
		final String v1Suffix;
		Matcher v1Matcher = versionSuffixPattern.matcher(v1);
		if(v1Matcher.find()) {
			v1PrefixVerRev = v1.substring(0, v1Matcher.start());
			v1Suffix = v1.substring(v1Matcher.start(), v1Matcher.end());;
		} else {
			v1PrefixVerRev = v1;
			v1Suffix = null;
		}
		final String v2PrefixVerRev;
		final String v2Suffix;
		Matcher v2Matcher = versionSuffixPattern.matcher(v2);
		if(v2Matcher.find()) {
			v2PrefixVerRev = v2.substring(0, v2Matcher.start());
			v2Suffix = v2.substring(v2Matcher.start(), v2Matcher.end());;
		} else {
			v2PrefixVerRev = v2;
			v2Suffix = null;
		}
		if(Sort.GNU_SORT_COMPATIBLE) {
			// What if v1.x and v01.y?  These seem to be equal because .x and .y are ignored.
			if((v1Suffix != null ||v2Suffix != null) && v1PrefixVerRev.equals(v2PrefixVerRev)) {
				int comp = pvrCompare(v1, v2);
				if(comp != 0) {
					return comp;
				}
			} else {
				int comp = pvrCompare(v1PrefixVerRev, v2PrefixVerRev);
				if(comp != 0) {
					return comp;
				}
			}
		} else {
			int comp = pvrCompare(v1PrefixVerRev, v2PrefixVerRev);
			if(comp != 0) {
				return comp;
			}
			comp = pvrCompare(v1Suffix, v2Suffix);
			if(comp != 0) {
				return comp;
			}
		}
		return simpleComp;
	}
	
	private int randomCompare(KeyField keyField, TextLine thisTextLine, Field thisField, TextLine thatTextLine, Field thatField) {
		byte[] thisDigest = thisField.digest;
		byte[] thatDigest = thatField.digest;
		int i = 0;
		for(; i < thisDigest.length && i < thatDigest.length && thisDigest[i] == thatDigest[i]; i++) {
		}
		if(i < thisDigest.length &&  i < thatDigest.length) {
			return (thisDigest[i] & 0xff) < (thatDigest[i] & 0xff)? -1: 1;
		}
		if(thisDigest.length < thatDigest.length) {
			return -1;
		} else if(thisDigest.length > thatDigest.length) {
			return 1;
		} else {
			if(Sort.GNU_SORT_COMPATIBLE) {
				// tie break processing
				// The following is not exactly the same as GNU version, but similar idea. 
				String thisText = thisTextLine.line.substring(thisField.start, thisField.limit);
				String thatText = thatTextLine.line.substring(thatField.start, thatField.limit);
				if(keyField.ignore || keyField.translate) {
					thisText = Sort.transform(keyField, thisText, 0, thisText.length());
					thatText = Sort.transform(keyField, thatText, 0, thatText.length());
				}
				return (collator != null)? collator.compare(thisText, thatText): thisText.compareTo(thatText);
			} else {
				return 0;
			}
		}
	}
	
	@Override
	public int compare(TextLine thisLine, TextLine thatLine) {
		if(keyFields.length != 0) {
			for(int i = 0; i < keyFields.length; i++) {
				final int comp;
				switch(keyFields[i].sortKind) {
				case Text:
					comp = textCompare(keyFields[i], thisLine, thisLine.fields[i], thatLine, thatLine.fields[i]);
					break;
				case GeneralNumeric:
					comp = generalNumericCompare(thisLine.fields[i], thatLine.fields[i]);
					break;
				case HumanNumeric:
					comp = humanNumericCompare(thisLine.fields[i], thatLine.fields[i]);
					break;
				case Numeric:
					comp = numericCompare(thisLine.fields[i], thatLine.fields[i]);
					break;
				case Month:
					comp = Integer.compare(thisLine.fields[i].month, thatLine.fields[i].month);;
					break;
				case Random:
					comp = randomCompare(keyFields[i], thisLine, thisLine.fields[i], thatLine, thatLine.fields[i]);
					break;
				case Version:
					comp = versionCompare(thisLine.fields[i].version, thatLine.fields[i].version);
					break;
				default:
					throw new IllegalStateException("Unknown SortKind");
				}
				if(comp != 0)
					return keyFields[i].reverse? -comp: comp;
			}
			if(stable) {
				if(sequenceSignificant) {
					assert thisLine.seq != thatLine.seq;
					return Long.compare(thisLine.seq, thatLine.seq);
				} else {
					return 0;
				}
			}
			if(unique) {
				return 0;
			}
		}
		int comp = (collator != null)? collator.compare(thisLine.line, thatLine.line): thisLine.line.compareTo(thatLine.line);
		return configuration.reverse? -comp: comp;
	}
	
	TextLineComparator(Configuration configuration, boolean sequenceSignificant) {
		this.configuration = configuration;
		this.keyFields = configuration.keyFields;
		this.unique = configuration.unique;
		this.stable = configuration.stable;
		this.sequenceSignificant = sequenceSignificant;
		if(configuration.textLocale != null) {
			collator = Collator.getInstance(configuration.textLocale);
		}
	}
}