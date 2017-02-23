package sophie.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ThrowableUtilities {
	public static String toStackTraceString(Throwable th) {
    	StringWriter stringWriter = new StringWriter();
    	PrintWriter printWriter = new PrintWriter(stringWriter);
    	th.printStackTrace(printWriter);
    	printWriter.flush();
		return stringWriter.toString();
	}
}
