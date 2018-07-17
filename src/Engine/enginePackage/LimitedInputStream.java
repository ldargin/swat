package Engine.enginePackage;

import java.io.IOException;
import java.io.InputStream;

/** 
 * A stream that controls the amount of bytes it reads from an underlying stream.
 * <p>
 * It is implemented with an internal byte counter.
 * As input is read using the read methods the byte counter
 * is decremented. When the counter reaches a value smaller
 * than 0, the read methods throw an {@link IOException}. 
 * <p>
 * The byte counter can be set through {@link #resetByteCount(int)}
 * to any integer value.   
 * */
final class LimitedInputStream extends InputStream {
	private int byteCounter = Integer.MAX_VALUE;
	private InputStream is;
	
	public LimitedInputStream(InputStream is) { this.is=is; };
	
	private  void checkLength(int length) throws IOException {
		byteCounter-=length;
		if (byteCounter<0)
			throw new IOException("Stream too long.");
	}

	@Override
	public int read() throws IOException {
		checkLength(1);
		return is.read();
	}

	@Override
	public int available() throws IOException {
		return is.available();
	}
	@Override
	public void close() throws IOException {
		is.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		is.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return is.markSupported();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		checkLength(len);
		return is.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		checkLength(b.length);
		return is.read(b);
	}

	@Override
	public synchronized void reset() throws IOException {
		is.reset();
		byteCounter=Integer.MAX_VALUE;
	}

	@Override
	public long skip(long n) throws IOException {
		return is.skip(n);
	}
	
	public void resetByteCount(int limit){ byteCounter=limit; }
}