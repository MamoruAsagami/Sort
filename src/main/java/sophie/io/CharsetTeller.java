package sophie.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class CharsetTeller {
	private CharCodeVerifierFilter[] verifiers;
	
	static abstract class CharCodeVerifierFilter {
		Charset charset;
		int confidence = 0;
		
		CharCodeVerifierFilter() {
		}
		
		public void reset() {
			confidence = 0;
		}
		
		protected abstract void handleChar(int c);
		protected abstract void handleEndOfFile();
		
		public int getConfidence() {
			return confidence;
		}

		public Charset getCharset() {
			return charset;
		}
	}

	static class Utf8VerifierFilter extends CharCodeVerifierFilter {
	/*
	 * UTF-8 Byte sequence:
	 * 
	 * 0xxxxxxx                                               (00-7f) 7bit
	 * 110yyyyx 10xxxxxx                                      (c0-df)(80-bf) 11bit
	 * 1110yyyy 10yxxxxx 10xxxxxx                             (e0-ef)(80-bf)(80-bf) 16bit
	 * 11110yyy 10yyxxxx 10xxxxxx 10xxxxxx                    (f0-f7)(80-bf)(80-bf)(80-bf) 21bit
	 * 111110yy 10yyyxxx 10xxxxxx 10xxxxxx 10xxxxxx           (f8-fb)(80-bf)(80-bf)(80-bf)(80-bf) 26bit
	 * 1111110y 10yyyyxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx  (fc-fd)(80-bf)(80-bf)(80-bf)(80-bf)(80-bf) 31bit
	 *
	 */
		static final int NORMAL = 0;
		static final int SEQUENCE = 1;
		
		int state = NORMAL;
		int remainingSequemceLength;
		
		Utf8VerifierFilter() {
			charset = Charset.forName("UTF-8");
			reset();
		}
		
		public void reset() {
			super.reset();
			state = NORMAL;
			remainingSequemceLength = 0;
		}
		
		protected void handleChar(int c) {
			switch(state) {
			case NORMAL:
				if((c & 0x80) != 0) {
					if((c & 0xe0) == 0xc0) {
						state = SEQUENCE;
						remainingSequemceLength = 1;
					} else if((c & 0xf0) == 0xe0) {
						state = SEQUENCE;
						remainingSequemceLength = 2;
					} else if((c & 0xf8) == 0xf0) {
						state = SEQUENCE;
						remainingSequemceLength = 3;
					} else if((c & 0xfc) == 0xf8) {
						state = SEQUENCE;
						remainingSequemceLength = 4;
					} else if((c & 0xfe) == 0xfc) {
						state = SEQUENCE;
						remainingSequemceLength = 5;
					} else {
						confidence = -10;
					}
				}
				break;
			case SEQUENCE:
				if((c & 0xc0) == 0x80) {
					remainingSequemceLength--;
					if(remainingSequemceLength == 0) {
						state = NORMAL;
						if(confidence == 0)
							confidence = 10;
					}
				} else {
					confidence = -10;
				}
				break;
			}			
		}
		protected void handleEndOfFile() {
			if(state != NORMAL) {
				confidence = -10;
			}
		}
	}

	static class Utf16VerifierFilter extends CharCodeVerifierFilter {
		/*
		 * UTF-16 Byte sequence:
		 * 
		 * (FE FF) (00 4D) (D8 00) (DF 02)
		 * (FF FE) (4D 00) (00 D8) (02 DF)
		 * (00 4D) (D8 00) (DF 02)		 *
		 */
		
			int oscillator = 0; // Let's hope it won't overflow.
			int evenByte;
			int evenZeros = 0; // Let's hope it won't overflow.
			int oddZeros = 0; // Let's hope it won't overflow.
			int bom = 0; // Byte order mark
			
			Utf16VerifierFilter() {
				charset = Charset.forName("UTF-16");
				reset();
			}
			
			public void reset() {
				super.reset();
				oscillator = 0; // Let's hope it won't overflow.
				evenZeros = 0; // Let's hope it won't overflow.
				oddZeros = 0; // Let's hope it won't overflow.
				bom = 0; // Byte order mark
			}
			
			protected void handleChar(int c) {
				if((oscillator & 1) == 0) {
					evenByte = c;
					if(c == 0 && evenZeros != Integer.MAX_VALUE) {
						evenZeros++;
					}
				} else {
					if(c == 0) {
						if(c == 0 && oddZeros != Integer.MAX_VALUE) {
							oddZeros++;
						}
					}
					if(oscillator == 1) {
						if(evenByte == 0xFE && c == 0xFF) {
							bom = 0xFEFF;
						} else if(evenByte == 0xFF && c == 0xFE) {
							bom = 0xFFFE;
						}
					}
				}
				oscillator++;
				
			}
			protected void handleEndOfFile() {
				if((oscillator & 1) == 0) {
					if(bom != 0) {
						confidence = 10;
					} else {
						if(evenZeros > oddZeros) {
							confidence = 5;
						}
					}
				} else {
					confidence = -10;
				}
			}
		}

	static class Iso2022JpVerifierFilter extends CharCodeVerifierFilter {
	/*
	 * ISO-2022-JP Byte sequence:
	 * 
	 * ESC ( B	ASCII
	 * ESC ( J	JIS X 0201-Roman
	 * ESC $ @	JIS C 6226-1978
	 * ESC $ B	JIS X 0208-1983	 *
	 */
		static final int NORMAL = 0;
		static final int ESC = 1;
		
		int state = NORMAL;
		int escIndex;
		int escC1;
		int escC2;
		
		Iso2022JpVerifierFilter() {
			charset = Charset.forName("ISO-2022-JP");
			reset();
		}
		
		public void reset() {
			super.reset();
			state = NORMAL;
		}

		protected void handleChar(int c) {
			if((c & 0x80) != 0) {
				confidence = -10;
			}
			switch(state) {
			case NORMAL:
				if(c == 0x1b) {
					state = ESC;
					escIndex = 0;
				}
				break;
			case ESC:
				if(escIndex == 0) {
					escC1 = c;
				} else if(escIndex == 1) {
					escC2 = c;
				}
				escIndex++;
				if(escIndex == 2) {
					if((escC1 == '(' && escC2 == 'B') ||
						(escC1 == '(' && escC2 == 'J') ||
						(escC1 == '$' && escC2 == '@') ||
						(escC1 == '$' && escC2 == 'B')) {
						if(confidence == 0)
							confidence = 10;
					} else {
						confidence = -10;
					}
					state = NORMAL;
				}
				break;
			}			
		}
		protected void handleEndOfFile() {
			if(state != NORMAL) {
				confidence = -10;
			}
		}
	}

	static class EucJpShiftJisVerifierFilter extends CharCodeVerifierFilter {
	/*
	 * EUC Byte sequence:
	 * 
	 * 0x00-0x7f: G0(ASCII)
	 * 0xa1-0xfe 0xa1-0xfe: G1(JIS X 0208)
	 * 0x8e(SS2) 0xa1-0xfe: G2(Half face Kana)
	 * 0x8f(SS3) 0xa1-0xfe 0xa1-0xfe: G3(JIS X 0212)
	 */
	/*
	 * Shift-JIS Byte sequence:
	 * 
	 * (0x81-0x9F | 0xE0-0xFC), (0x40-0x7E, 0x80-0xFC)
	 */
		static final int NORMAL = 0;
		static final int X0208 = 1;
		static final int KANA = 2;
		static final int X0212 = 3;
		//
		static final int SHIFT = 1;
		
		int eucState = NORMAL;
		int index;
		int eucConfidence = 0;
		int eucCount = 0;
		
		int shiftState = NORMAL;
		int shiftConfidence = 0;
		int shiftCount = 0;
		
		EucJpShiftJisVerifierFilter() {
			reset();
		}
		
		public void reset() {
			super.reset();
			charset = Charset.forName("EUC-JP");
			eucState = NORMAL;
			eucConfidence = 0;
			eucCount = 0;
			
			shiftState = NORMAL;
			shiftConfidence = 0;
			shiftCount = 0;
		}
		
		protected void handleChar(int c) {
			switch(eucState) {
			case NORMAL:
				if(c >= 0xa1 && c <= 0xfe) {
					eucState = X0208;
				} else if(c == 0x8e) { // SS2
					eucState = KANA;
				} else if(c == 0x8f) { // SS3
					eucState = X0212;
					index = 0;					
				} else if((c & 0x80) != 0) {
					eucConfidence = -10;
				}
				break;
			case X0208:
				if(c >= 0xa1 && c <= 0xfe) {
					eucCount++;
					if(eucConfidence >= 0)
						eucConfidence = 10;
				} else {
					eucConfidence = -1;
				}
				eucState = NORMAL;
				break;
			case KANA:
				if(c >= 0xa1 && c <= 0xfe) {
					eucCount++;
					if(eucConfidence == 0)
						eucConfidence = 5;
				} else {
					eucConfidence = -1;
				}
				eucState = NORMAL;
				break;
			case X0212:
				if(c >= 0xa1 && c <= 0xfe) {
					if(index == 1) {
						eucCount++;
						if(eucConfidence >= 0)
							eucConfidence = 10;					
					}
				} else {
					eucConfidence = -1;
				}
				index++;
				if(index == 2)
					eucState = NORMAL;
				break;
			}			
			switch(shiftState) {
			case NORMAL:
				if((c >= 0x81 && c <= 0x9F) || (c >= 0xE0 && c <= 0xFC)) {
					shiftState = SHIFT;
				}
				break;
			case SHIFT:
				if((c >= 0x40 && c <= 0x7E) || (c >= 0x80 && c <= 0xFC)) {
					shiftCount++;
					if(shiftConfidence >= 0)
						shiftConfidence = 10;
				} else {
					shiftConfidence = -1;
				}
				shiftState = NORMAL;
				break;
			}			
		}
		
		protected void handleEndOfFile() {
			/*
			System.out.println("eucState: " + eucState + " eucConfidence: " + eucConfidence + " eucCount: " + eucCount);
			System.out.println("shiftState: " + shiftState + " shiftConfidence: " + shiftConfidence + " shiftCount: " + shiftCount);
			*/
			if(eucState != NORMAL) {
				eucConfidence = -10;
			}
			if(shiftState != NORMAL) {
				shiftConfidence = -10;
			}
			if(eucConfidence > 0 && shiftConfidence > 0) {
				if(eucCount > shiftCount) {
					shiftConfidence = 0;
				}
			}
			if(eucConfidence > 0) {
				confidence = eucConfidence;
				charset = Charset.forName("EUC-JP");
			} else if(shiftConfidence > 0) {
				confidence = shiftConfidence;
				charset = Charset.forName("Shift_JIS");
			}
		}
	}

	static class EucJpVerifierFilter extends CharCodeVerifierFilter {
	/*
	 * EUC Byte sequence:
	 * 
	 * 0x00-0x7f: G0(ASCII)
	 * 0xa1-0xfe 0xa1-0xfe: G1(JIS X 0208)
	 * 0x8e(SS2) 0xa1-0xfe: G2(Half face Kana)
	 * 0x8f(SS3) 0xa1-0xfe 0xa1-0xfe: G3(JIS X 0212)
	 */
	/*
	 * Shift-JIS Byte sequence:
	 * 
	 * (0x81-0x9F | 0xE0-0xFC), (0x40-0x7E, 0x80-0xFC)
	 */
		static final int NORMAL = 0;
		static final int X0208 = 1;
		static final int KANA = 2;
		static final int X0212 = 3;
		//
		static final int SHIFT = 2;
		
		int state = NORMAL;
		int index;
		
		EucJpVerifierFilter() {
			charset = Charset.forName("EUC-JP");
			reset();
		}
		
		public void reset() {
			super.reset();
			state = NORMAL;
		}
		
		protected void handleChar(int c) {
			switch(state) {
			case NORMAL:
				if(c >= 0xa1 && c <= 0xfe) {
					state = X0208;
				} else if(c == 0x8e) { // SS2
					state = KANA;
				} else if(c == 0x8f) { // SS3
					state = X0212;
					index = 0;					
				} else if((c & 0x80) != 0) {
					confidence = -10;
				}
				break;
			case X0208:
				if(c >= 0xa1 && c <= 0xfe) {
					if(confidence >= 0)
						confidence = 10;
				} else {
					confidence = -1;
				}
				state = NORMAL;
				break;
			case KANA:
				if(c >= 0xa1 && c <= 0xfe) {
					if(confidence == 0)
						confidence = 5;
				} else {
					confidence = -1;
				}
				state = NORMAL;
				break;
			case X0212:
				if(c >= 0xa1 && c <= 0xfe) {
					if(index == 1) {
						if(confidence >= 0)
							confidence = 10;					
					}
				} else {
					confidence = -1;
				}
				index++;
				if(index == 2)
					state = NORMAL;
				break;
			}			
		}
		
		protected void handleEndOfFile() {
			if(state != NORMAL) {
				confidence = -10;
			}
		}
	}
	
	static class ShiftJisVerifierFilter extends CharCodeVerifierFilter {
	/*
	 * Shift-JIS Byte sequence:
	 * 
	 * (0x81-0x9F | 0xE0-0xFC), (0x40-0x7E, 0x80-0xFC)
	 */
		static final int NORMAL = 0;
		static final int SHIFT = 1;
		
		int state = NORMAL;
		
		ShiftJisVerifierFilter() {
			charset = Charset.forName("Shift_JIS");
			reset();
		}
		
		public void reset() {
			super.reset();
			state = NORMAL;
		}
		
		protected void handleChar(int c) {
			switch(state) {
			case NORMAL:
				if((c >= 0x81 && c <= 0x9F) || (c >= 0xE0 && c <= 0xFC)) {
					state = SHIFT;
				}
				break;
			case SHIFT:
				if((c >= 0x40 && c <= 0x7E) || (c >= 0x80 && c <= 0xFC)) {
					if(confidence >= 0)
						confidence = 10;
				} else {
					confidence = -1;
				}
				state = NORMAL;
				break;
			}			
		}
		protected void handleEndOfFile() {
			if(state != NORMAL) {
				confidence = -10;
			}
		}
	}

	public CharsetTeller() {
		
		verifiers = new CharCodeVerifierFilter[] {
				new Utf8VerifierFilter(),
				new Utf16VerifierFilter(),
				new Iso2022JpVerifierFilter(),
				new EucJpShiftJisVerifierFilter()
		};
				
	}
	
	public void handle(int c) {
		for(CharCodeVerifierFilter verifier: verifiers) {
			verifier.handleChar(c);
		}
	}

	public Charset getCharset() {
		Charset value = Charset.defaultCharset();
		String defaultName = value.name();
		int confidence = 0;
		for(CharCodeVerifierFilter e: verifiers) {
			if(e.getConfidence() > confidence) {
				value = e.getCharset();
				confidence = e.getConfidence();
			}		
		}
		if(defaultName.equals("windows-31j") && value.name().equals("Shift_JIS")) {
			value = Charset.defaultCharset();
		}
		return value;
	}
	
	public void close() {
		for(CharCodeVerifierFilter verifier: verifiers) {
			verifier.handleEndOfFile();
		}
	}
	
	public Charset getCharset(byte[] buffer, int off, int length) {
		for(CharCodeVerifierFilter e: verifiers) {
			e.reset();
		}
		for(int i = 0; i < length; i++) {
			handle(buffer[off + i] & 0xff);
		}
		for(CharCodeVerifierFilter e: verifiers) {
			e.handleEndOfFile();
		}
		return getCharset();
	}
	
	public static Charset getCharset(File file) throws IOException {
		CharsetTeller charsetTeller = new CharsetTeller();
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		try {
			int c;
			while((c = in.read()) != -1) {
				charsetTeller.handle(c);
			}
			charsetTeller.close();
		} finally {
			in.close();
		}
		charsetTeller.close();
		return charsetTeller.getCharset();
	}
	
	public static Charset getCharset(InputStream in) throws IOException {
		CharsetTeller charsetTeller = new CharsetTeller();
		int c;
		while((c = in.read()) != -1) {
			charsetTeller.handle(c);
		}
		charsetTeller.close();
		charsetTeller.close();
		return charsetTeller.getCharset();
	}
	
	public static Charset getCharset(String fileName) throws IOException {
		return getCharset(new File(fileName));
	}
	public static Charset getCharset(byte[] data) throws IOException {
		return getCharset(new ByteArrayInputStream(data));
	}
}
