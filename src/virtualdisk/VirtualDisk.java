package virtualdisk;
/*
 * VirtualDisk.java
 *
 * A virtual asynchronous disk.
 *
 */

import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;

import common.Constants;
import common.Constants.DiskOperationType;
import dblockcache.DBuffer;

public abstract class VirtualDisk implements IVirtualDisk {

	private String volName;
	private RandomAccessFile file;
	private int maxVolSize;

	/*
	 * VirtualDisk Constructors
	 */
	public VirtualDisk(String volName, boolean format) throws FileNotFoundException,
			IOException {

		this.volName = volName;
		this.maxVolSize = Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS;

		/*
		 * mode: rws => Open for reading and writing, as with "rw", and also
		 * require that every update to the file's content or metadata be
		 * written synchronously to the underlying storage device.
		 */
		file = new RandomAccessFile(volName, "rws");

		/*
		 * Set the length of the file to be NUM_OF_BLOCKS with each block of
		 * size BLOCK_SIZE. setLength internally invokes ftruncate(2) syscall to
		 * set the length.
		 */
		file.setLength(Constants.BLOCK_SIZE * Constants.NUM_OF_BLOCKS);
		if(format) {
			formatStore();
		}
		/* Other methods as required */
	}
	
	public VirtualDisk(boolean format) throws FileNotFoundException,
	IOException {
		this(Constants.vdiskName, format);
	}
	
	public VirtualDisk() throws FileNotFoundException,
	IOException {
		this(Constants.vdiskName, false);
	}

	/*
	 * Start an asynchronous request to the underlying device/disk/volume. 
	 * -- buf is an DBuffer object that needs to be read/write from/to the volume.	
	 * -- operation is either READ or WRITE  
	 * 
	 * Start an asynchronous I/O request to the device/disk. 
	 * The blockID and buffer array are given by the DBuffer dbuf. 
	 * The operation is either READ or WRITE (DiskOperationType). 
	 */ 
	public abstract void startRequest(DBuffer buf, DiskOperationType operation) throws IllegalArgumentException, IOException;
	
	/*
	 * Clear the contents of the disk by writing 0s to it
	 */
	private void formatStore() {
		byte b[] = new byte[Constants.BLOCK_SIZE];
		setBuffer((byte) 0, b, Constants.BLOCK_SIZE);
		for (int i = 0; i < Constants.NUM_OF_BLOCKS; i++) {
			try {
				int seekLen = i * Constants.BLOCK_SIZE;
				file.seek(seekLen);
				file.write(b, 0, Constants.BLOCK_SIZE);
			} catch (Exception e) {
				System.out.println("Error in format: WRITE operation failed at the device block " + i);
			}
		}
	}

	/*
	 * helper function: setBuffer
	 */
	private static void setBuffer(byte value, byte b[], int bufSize) {
		for (int i = 0; i < bufSize; i++) {
			b[i] = value;
		}
	}

	/*
	 * Reads to the buffer associated with DBuffer from the underlying
	 * device/disk/volume
	 */
	protected int readBlock(DBuffer buf) throws IOException {
		int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
		/* Boundary check */
		if (maxVolSize < seekLen + Constants.BLOCK_SIZE) {
			return -1;
		}
		file.seek(seekLen);
		return file.read(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
		//Writes into buf.getBuffer()
	}

	/*
	 * Writes the buffer associated with DBuffer to the underlying
	 * device/disk/volume
	 */
	protected void writeBlock(DBuffer buf) throws IOException {
		int seekLen = buf.getBlockID() * Constants.BLOCK_SIZE;
		file.seek(seekLen);
		file.write(buf.getBuffer(), 0, Constants.BLOCK_SIZE);
	}

	
}
