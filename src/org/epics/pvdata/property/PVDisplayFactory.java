/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvdata.property;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Type;

public final class PVDisplayFactory implements PVDisplay{
    public PVString pvDescription = null;
    public PVString pvFormat = null;
    public PVString pvUnits = null;
    public PVDouble pvLow = null;
    public PVDouble pvHigh = null;
    private static final String noDisplayFound = "No display structure was located";
    private static final String notAttached = "Not attached to an display structure";

    /**
     * Create a PVDisplay.
     * @return The newly created PVDisplay.
     */
    public static PVDisplay create() { return new PVDisplayFactory();} 
    /* (non-Javadoc)
     * @see org.epics.pvdata.property.PVDisplay#attach(org.epics.pvdata.pv.PVField)
     */
    @Override
    public boolean attach(PVField pvField) {
        if(pvField.getField().getType()!=Type.structure) {
            pvField.message(noDisplayFound,MessageType.error);
            return false;
        }
        PVStructure pvStructure = (PVStructure)(pvField);
        pvDescription = pvStructure.getStringField("description");
        if(pvDescription==null) {
            pvField.message(noDisplayFound,MessageType.error);
            return false;
        }
        pvFormat = pvStructure.getStringField("format");
        if(pvFormat==null) {
            pvField.message(noDisplayFound,MessageType.error);
            detach();
            return false;
        }
        pvUnits = pvStructure.getStringField("units");
        if(pvUnits==null) {
            pvField.message(noDisplayFound,MessageType.error);
            detach();
            return false;
        }
        pvLow = pvStructure.getDoubleField("limitLow");
        if(pvLow==null) {
            pvField.message(noDisplayFound,MessageType.error);
            detach();
            return false;
        }
        pvHigh = pvStructure.getDoubleField("limitHigh");
        if(pvHigh==null) {
            pvField.message(noDisplayFound,MessageType.error);
            detach();
            return false;
        }
        return true;

    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.property.PVDisplay#detach()
     */
    @Override
    public void detach() {
        pvDescription = null;
        pvFormat = null;
        pvUnits = null;
        pvLow = null;
        pvHigh = null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.property.PVDisplay#isAttached()
     */
    @Override
    public boolean isAttached() {
        if(pvDescription==null || pvFormat==null || pvUnits==null || pvLow==null || pvHigh==null) return false;
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.property.PVDisplay#get(org.epics.pvdata.property.Display)
     */
    @Override
    public void get(Display display) {
        if(pvDescription==null || pvFormat==null || pvUnits==null || pvLow==null || pvHigh==null) {
            throw new IllegalStateException(notAttached);
        }
        display.setDescription(pvDescription.get());
        display.setFormat(pvFormat.get());
        display.setUnits(pvUnits.get());
        display.setLow(pvLow.get());
        display.setHigh(pvHigh.get());

    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.property.PVDisplay#set(org.epics.pvdata.property.Display)
     */
    @Override
    public boolean set(Display display) {
        if(pvDescription==null || pvFormat==null || pvUnits==null || pvLow==null || pvHigh==null) {
            throw new IllegalStateException(notAttached);
        }
        if(pvDescription.isImmutable() || pvFormat.isImmutable() || pvUnits.isImmutable()) return false;
        if(pvLow.isImmutable() || pvHigh.isImmutable()) return false;
        pvDescription.put(display.getDescription());
        pvFormat.put(display.getFormat());
        pvUnits.put(display.getUnits());
        pvLow.put(display.getLow());
        pvHigh.put(display.getHigh());
        return true;
    }
}