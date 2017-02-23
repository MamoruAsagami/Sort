package sophie.tools.textfile.sort;

class Field {
	String text;
	char start;
	char limit;
	short signedMagnitude;
	byte[] integralPart;
	byte[] fractionalPart;
	//short SIsuffix; // 0: none, 1: k or K, 2: M, 3: G, 4: T, 5: P, 6: E, 7: Z, 8Y
	double realNumber;
	short month;
	byte[] digest;
	String version;
}