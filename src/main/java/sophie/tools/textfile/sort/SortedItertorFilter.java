package sophie.tools.textfile.sort;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

class SortedItertorFilter implements Iterator<TextLine> {
	boolean unique;
	Comparator<TextLine> comparator;
	Iterator<TextLine> in;
	ExitStatus status = ExitStatus.Success;
	String failureMessage;
	
	TextLine prev;
	TextLine next;
	
	private void failure(int comp) {
		status = ExitStatus.Failure;
		if(comp == 0) {
			failureMessage = "Not unique input";
		} else {
			failureMessage = "Out of order";
		}
	}
	public String getFailureMessage() {
		return failureMessage;
	}
	
	@Override
	public boolean hasNext() {
		if(next != null)
			return true;
		else if(status != ExitStatus.Success)
			return false;
		else {
			try {
				TextLine next = in.next();
				if(prev != null) {
					int comp = comparator.compare(next, prev);
					if(comp < 0) {
						failure(comp);
						return false;
					} else if(comp == 0 && unique) {
						failure(comp);
						return false;
					} else {
						// nothing to do.
					}
				}
				this.next = next;
				this.prev = next;
				return true;
			} catch(NoSuchElementException e) {
				return false;
			}
		}
	}

	@Override
	public TextLine next() {
		if(next != null) {
			TextLine value = next;
			next = null;
			return value;
		}  if(status != ExitStatus.Success) {
			throw new NoSuchElementException("EOF");
		} else {
			TextLine next = in.next();
			if(prev != null) {
				int comp = comparator.compare(next, prev);
				if(comp < 0) {
					failure(comp);
					throw new NoSuchElementException("Out of order");
				} else if(comp == 0 && unique) {
					failure(comp);
					throw new NoSuchElementException("Not unique input");
				} else {
					// nothing to do.
				}
			}
			this.prev = next;
			return next;
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	SortedItertorFilter(Iterator<TextLine> in, Comparator<TextLine> comparator, boolean unique) {
		this.in = in;
		this. comparator = comparator;
		this.unique = unique;
	}
}