package sophie.tools.textfile.sort;

@SuppressWarnings("serial")
class OptionTypeError extends Exception {
	OptionTypeError(String message) {
		super(message);
	}
	
	OptionTypeError(String message, String option) {
		super(option + ": " + message);
	}
}