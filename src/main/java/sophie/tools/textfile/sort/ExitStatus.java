package sophie.tools.textfile.sort;

public enum ExitStatus {
	Success(0),
	OutOfOrder(1),
	Failure(2);
	
	final int status;
	ExitStatus(int status) {
		this.status = status;
	}
	int getStatus() {
		return status;
	}
}