/*
 * The License for this software can be found in the file LICENSE that is included with the distribution.
 */
package org.epics.pvdata.copy.timestampPlugin;

import org.epics.pvdata.copy.PVFilter;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.PVField;
/**
 * A filter that sets a timeStamp to the current time.
 * @author mrk
 * @since  2017.02.23
 */
public class  TimestampFilter implements PVFilter{
    private PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
    private TimeStamp timeStamp = TimeStampFactory.create();
    boolean current;
    boolean copy;
    PVField master;

    public static TimestampFilter create(String requestValue,PVField master)
    {
        PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
        if(!pvTimeStamp.attach(master)) return null;
        boolean current = false;
        boolean copy = false;
        if(requestValue.equals("current")) {
            current = true;
        } else if(requestValue.equals("copy")){
            copy = true;
        } else {
            return null;
        }
        return new TimestampFilter(current,copy,master);
    }

    private TimestampFilter(boolean current,boolean copy,PVField pvField)
    {
        this.current = current;
        this.copy = copy;
        this.master = pvField;
    }

    /* (non-Javadoc)
     * @see org.epics.pvdata.copy.PVFilter#filter(org.epics.pvdata.pv.PVField, org.epics.pvdata.misc.BitSet, boolean)
     */
    public boolean filter(PVField pvCopy,BitSet bitSet,boolean toCopy)
    {
        if(current) {	
            timeStamp.getCurrentTime();
            if(toCopy) {
                if(!pvTimeStamp.attach(pvCopy)) return false;
            } else {
                if(!pvTimeStamp.attach(master)) return false;
            }
            pvTimeStamp.set(timeStamp);
            bitSet.set(pvCopy.getFieldOffset());
            return true;
        }
        if(copy) {	
            if(toCopy) {
                if(!pvTimeStamp.attach(master)) return false;
                pvTimeStamp.get(timeStamp);
                if(!pvTimeStamp.attach(pvCopy)) return false;
                pvTimeStamp.set(timeStamp);
                bitSet.set(pvCopy.getFieldOffset());
            } else {
                if(!pvTimeStamp.attach(pvCopy)) return false;
                pvTimeStamp.get(timeStamp);
                if(!pvTimeStamp.attach(master)) return false;
                pvTimeStamp.set(timeStamp);
            }
            return true;
        }
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.copy.PVFilter#getName()
     */
    public String getName()
    {
        return TimestampPlugin.name;
    }
}
