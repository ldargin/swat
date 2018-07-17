package storyTellerPackage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.io.InputStream;

import com.storytron.uber.Sentence;


/** 
 * Utility class to manipulate saved stories.
 * <p>
 * Its main purpose is to provide a way to write and read
 * saved stories from streams. 
 * <p>
 * The attributes a SavedStory holds are:
 * <ul>
 * <li>the storyworld identifier,</li>
 * <li>the storyworld version,</li>
 * <li>the amount of input of the last sentence,</li>
 * <li>the label of the starting verb,</li>
 * <li>the state of the world</li>
 * <li>and the input that must be played from the state 
 *     to get to the saved point.</li>
 * </ul>
 * <p>
 * There are no single methods to read and write saved stories from/to streams.
 * This is because some stream manipulations must be performed when reading
 * or writing. A short format header is written as plain text, the rest of the
 * data is gzipped and encoded in base64 so it can be copied and pasted from/to
 * messages. This is the general procedure for reading:
 * <pre>
 * SavedStory ss = new SavedStory();
 * ss.readFormatHeader(is); // checks format and format version
 * DataInputStream di = SavedStory.convertInputStreamToDataInput(is);
 * ss.readHeader(di); // reads saved story header
 * ss.readBody(di); // reads saved story body
 * ... now do something with ss fields ... 
 * </pre>
 * In the above example {@link #convertInputStreamToDataInput(InputStream)} converts 
 * the {@link InputStream} <code>is</code> to a {@link DataInputStream} which correctly 
 * decodes the base64 and gzipped stream.
 * <p>
 * Now it follows the general procedure for writing saved stories:
 * <pre>
 * ... create a SavedStory ss ...
 * ss.writeFormatHeader(os);
 * DataOutputStream dos = ss.convertOutputStreamToDataOutput(os);
 * ss.write(dos);
 * </pre> 
 * In the above example {@link #convertOutputStreamToDataOutput(OutputStream)} converts the 
 * {@link OutputStream} <code>os</code> to a {@link DataOutputStream} which correctly encodes
 *  the base64 and gzipped stream.
 *  <p>
 * Exceptions might be thrown when reading or writing, so extra care must be taken
 * to handle the stream in those cases. 
 * <p>
 * It follows the description of the stream format to save the story.
 * <p>
 * First there's a format header "SS ". This header is used to discard files that
 * may not be saved stories. The whitespace (" ") after "SS" is a delimiter
 * that allows to put a version number for the format in the future, for instance
 * "SS1 " or "SS2.1 ". The header is always written in ascii at the beginning of
 * the stream.
 * <p>
 * Next, there's a story header, which holds the storyworld name and the storyworld
 * version. This values are supposed to be read with {@link DataInput#readUTF()} and
 * {@link DataInput#readInt()}.
 * <p>
 * Then comes the body of the saved story, which holds the rest of the attributes.
 * <ul>
 * <li>The amount of input of the last sentence can be read with 
 *     {@link DataInput#readShort()}.</li>
 * <li>The starting verb label can be read with {@link DataInput#readUTF()}.</li>
 * <li>Next comes the length in bytes of the storyworld state data, that can be read with
 * {@link DataInput#readInt()}.</li>
 * <li> Then it comes the storyworld state data as a sequence of bytes of the specified 
 *      length.</li>
 * <li>Next comes the length of the recorded input (the amount of stored values).</li>
 * <li>Finally comes the sequence of values of the recorded input, each of this values
 * can be read with {@link DataInput#readInt()}.</li>
 * </ul>
 * */
public final class SavedStory {

	public String storyworldID;
	public int dkversion;
	public short sentenceAmountOfInput;
	public boolean[] hotWordSockets = new boolean[Sentence.MaxWordSockets];
	public String startingVerbLabel;
	public byte[] state;
	public LinkedList<Integer> recordedInput;
	
	/** Constructs a saved story. */
	public SavedStory(){}
	
	/** Constructs a saved story with the given field values. */
	public SavedStory(
				String storyworldName,
				int dkversion,
				short sentenceAmountOfInput,
				boolean[] hotWordSockets,
				String startingVerbLabel,
				byte[] state,
				LinkedList<Integer> recordedInput
			){
		this.storyworldID = storyworldName;
		this.dkversion = dkversion;
		this.sentenceAmountOfInput = sentenceAmountOfInput;
		this.hotWordSockets = hotWordSockets;
		this.startingVerbLabel = startingVerbLabel;
		this.state = state;
		this.recordedInput = recordedInput;
	}

	/** 
	 * Reads the format header of a saved story from the given stream.
	 * <p>
	 * The header comprehends the format identifier and the format version. 
	 * @throws InvalidFormatException if the file is detected not to be 
	 * 									a saved story. 
	 * */
	public InputStream readFormatHeader(InputStream br) throws IOException, InvalidFormatException {
		if (br.read()!=(char)'S' || br.read()!=(char)'S')
			throw new InvalidFormatException();
		
		while(br.read()!=(char)' '); // read format version
		
		return br;
	}

	/** Conversion used to read saved data from an input stream. */
	public static DataInputStream convertInputStreamToDataInput(InputStream is) throws IOException {
		return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new Base64.InputStream(is))));
	}

	/** Conversion used to write data to an output stream. */
	public static DataOutputStream convertOutputStreamToDataOutput(OutputStream os) throws IOException {
		return new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new Base64.OutputStream(os))));
	}

	/** 
	 * Reads the header of a saved story from the given stream.
	 * <p>
	 * The header comprehends the storyworld identifier and the storyworld version. 
	 * @throws InvalidFormatException if the file is detected not to be 
	 * 									a saved story. 
	 * */
	public DataInputStream readHeader(DataInputStream br) throws IOException {
		storyworldID = br.readUTF();
		dkversion = br.readInt();
		return br;
	}

	/** Reads the body of a saved story. */
	public void readBody(DataInput br) throws IOException {
		sentenceAmountOfInput=br.readShort();
		startingVerbLabel = br.readUTF();
		int stateSize = br.readInt();
		if (stateSize>0){
			state=new byte[stateSize];
			br.readFully(state);
		} else 
			state=null;

		if (recordedInput==null)
			recordedInput = new LinkedList<Integer>();
		else
			recordedInput.clear();
		
		{   // read recorded input
			int size=br.readInt();
			for(int i=0;i<size;i++)
				recordedInput.add(br.readInt());
		}
		
		{  // read hot wordsockets
			int size=0;
			// read hot wordsockets if there are more bytes
			// This is for backward compatibility (Aug 12th, 2008)
			// In some time all saved stories will be expected
			// to have these bytes.
			try { size=br.readShort(); } catch (EOFException e){}
			for(int i=0;i<size;i++)
				hotWordSockets[br.readShort()]=true;
		}
	}
	
	/** 
	 * Writes the format header of a saved story to a given stream.
	 * <p>
	 * The header comprehends the format identifier and the format version. 
	 * */
	public OutputStream writeFormatHeader(OutputStream os) throws IOException {
		os.write((char)'S');
		os.write((char)'S');
		os.write((char)' '); // write format version
		return os;
	}
	
	/** Writes a story to a data output stream. */
	public void write(DataOutput bw) throws IOException {
		bw.writeUTF(storyworldID);
		bw.writeInt(dkversion);
		bw.writeShort(sentenceAmountOfInput);
		bw.writeUTF(startingVerbLabel);
		if (state!=null) {
			bw.writeInt(state.length);
			bw.write(state);
		} else 
			bw.writeInt(0);
		if (recordedInput!=null) {
			bw.writeInt(recordedInput.size());
			for(int i:recordedInput)
				bw.writeInt(i);
		} else
			bw.writeInt(0);
		
		// write hot wordsockets
		bw.writeShort(countHotWordsockets());
		for(int i=0;i<hotWordSockets.length;i++)
			if (hotWordSockets[i])
				bw.writeShort(i);
	}

	private short countHotWordsockets() {
		short count = 0; 
		for(int i=0;i<hotWordSockets.length;i++)
			if (hotWordSockets[i])
				count++;
		return count;
	}

	public final static class InvalidFormatException extends Exception {
		private static final long serialVersionUID = 0L;
	} 
}
