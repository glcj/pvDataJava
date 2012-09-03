/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvdata.factory;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.epics.pvdata.misc.SerializeHelper;
import org.epics.pvdata.pv.DeserializableControl;
import org.epics.pvdata.pv.LongArrayData;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVULongArray;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.SerializableControl;


/**
 * Base class for implementing PVLongArray.
 * @author mrk
 *
 */
public class BasePVULongArray extends AbstractPVScalarArray implements PVULongArray
{
    protected long[] value;   
    private LongArrayData longArrayData = new LongArrayData();
    
    /**
     * Constructor.
     * @param array The Introspection interface.
     */
    public BasePVULongArray(ScalarArray array)
    {
        super(array);
        value = new long[capacity];
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVArray#setCapacity(int)
     */
    @Override
    public void setCapacity(int len) {
    	if(capacity==len) return;
        if(!capacityMutable) {
            super.message("not capacityMutable", MessageType.error);
            return;
        }
        if(length>len) length = len;
        long[]newarray = new long[len];
        if(length>0) System.arraycopy(value,0,newarray,0,length);
        value = newarray;
        capacity = len;
    }      
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVLongArray#get(int, int, org.epics.pvdata.pv.LongArrayData)
     */
    @Override
    public int get(int offset, int len, LongArrayData data) {
        int n = len;
        if(offset+len > length) n = Math.max(0, length-offset);
        data.data = value;
        data.offset = offset;
        return n;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVLongArray#put(int, int, long[], int)
     */
    @Override
    public int put(int offset, int len, long[]from, int fromOffset) {
        if(super.isImmutable()) {
            super.message("field is immutable", MessageType.error);
            return 0;
        }
        if(from==value) return len;
        if(offset+len > length) {
            int newlength = offset + len;
            if(newlength>capacity) {
                setCapacity(newlength);
                newlength = capacity;
                len = newlength - offset;
                if(len<=0) return 0;
            }
            length = newlength;
        }
        System.arraycopy(from,fromOffset,value,offset,len);
        super.postPut();
        return len;     
    }
	/* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVLongArray#shareData(long[])
     */
    @Override
    public void shareData(long[] from) {
        this.value = from;
        super.capacity = from.length;
        super.length = from.length;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.SerializableArray#serialize(java.nio.ByteBuffer, org.epics.pvdata.pv.SerializableControl, int, int)
     */
    @Override
	public void serialize(ByteBuffer buffer, SerializableControl flusher, int offset, int count) {
    	// cache
    	final int length = this.length;
    	final long[] value = this.value;

		// check bounds
		if (offset < 0) offset = 0;
		else if (offset > length) offset = length;
		if (count < 0) count = length;

		final int maxCount = length - offset;
		if (count > maxCount)
			count = maxCount;
		
		// write
		SerializeHelper.writeSize(count, buffer, flusher);
		final int end = offset + count;
		int i = offset;
		while (true)
		{
        	final int maxIndex = Math.min(end-i, buffer.remaining()/(Long.SIZE/Byte.SIZE))+i;
			for (; i < maxIndex; i++)
				buffer.putLong(value[i]);
			if (i < end)
				flusher.flushSerializeBuffer();
			else
				break;
		}
	}
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.Serializable#deserialize(java.nio.ByteBuffer, org.epics.pvdata.pv.DeserializableControl)
     */
    @Override
	public void deserialize(ByteBuffer buffer, DeserializableControl control) {
		final int size = SerializeHelper.readSize(buffer, control);
		if (size >= 0) {
			// prepare array, if necessary
			if (size > capacity)
				setCapacity(size);
			// retrieve value from the buffer
			int i = 0;
			while (true)
			{
				final int maxIndex = Math.min(size-i, buffer.remaining()/(Long.SIZE/Byte.SIZE))+i;
				for (; i < maxIndex; i++)
					value[i] = buffer.getLong();
				if (i < size)
					control.ensureData(Long.SIZE/Byte.SIZE);
				else
					break;
			}
			// set new length
			length = size;
		}
		// TODO null arrays (size == -1) not supported
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// TODO anything else?
		if (obj instanceof PVULongArray) {
			PVULongArray b = (PVULongArray)obj;
			b.get(0, b.getLength(), longArrayData);
			if(longArrayData.data==value) return true;
			return Arrays.equals(longArrayData.data, value);
		}
		else
			return false;
	}
    /* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}
}