/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvData.factory;

import java.util.Iterator;

import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVByte;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVFloat;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVShort;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Type;

/**
 * Base class for a PVStructure.
 * @author mrk
 *
 */
public class BasePVStructure extends AbstractPVField implements PVStructure
{
    private static Convert convert = ConvertFactory.getConvert();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private PVField[] pvFields;
    
    /**
     * Constructor.
     * @param parent The parent interface.
     * @param structure the reflection interface for the PVStructure data.
     */
    public BasePVStructure(PVStructure parent, Structure structure) {
        super(parent,structure);
        Field[] fields = structure.getFields();
        pvFields = new PVField[fields.length];
        for(int i=0; i < pvFields.length; i++) {
        	Field field = fields[i];
        	switch(field.getType()) {
        	case scalar: {
        		Scalar scalar = (Scalar)field;
        		pvFields[i] = pvDataCreate.createPVScalar(this,field.getFieldName(),scalar.getScalarType());
        		break;
        	}
        	case scalarArray: {
        		Array array = (Array)field;
        		pvFields[i] = pvDataCreate.createPVArray(this,field.getFieldName(),array.getElementType());
        		break;
        	}
        	case structure: {
        		Structure struct = (Structure)field;
        		pvFields[i] = pvDataCreate.createPVStructure(this, field.getFieldName(), struct.getFields());
        	}
        	}
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.factory.AbstractPVField#postPut()
     */
    public void postPut() {
        super.postPut();
        for(PVField pvField : pvFields) {
            postPutNoParent((AbstractPVField)pvField);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#postPut(org.epics.pvData.pv.PVField)
     */
    public void postPut(PVField subField) {
        Iterator<PVListener> iter;
        iter = super.pvListenerList.iterator();
        while(iter.hasNext()) {
            PVListener pvListener = iter.next();
            pvListener.dataPut(this,subField);
        }
        PVStructure pvParent = super.getParent();
        if(pvParent!=null) pvParent.postPut(subField);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getSubField(java.lang.String)
     */
    public PVField getSubField(String fieldName) {
        for(PVField pvField : pvFields) {
            if(pvField.getField().getFieldName().equals(fieldName)) return pvField;
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getStructure()
     */
    public Structure getStructure() {
        return (Structure)getField();
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.factory.AbstractPVField#setRecord(org.epics.pvData.pv.PVRecord)
     */
    public void setRecord(PVRecord record) {
        super.setRecord(record);
        for(PVField pvField : pvFields) {
            AbstractPVField abstractPVField = (AbstractPVField)pvField;
            abstractPVField.setRecord(record);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#replaceStructureField(java.lang.String, org.epics.pvData.pv.Structure)
     */
    public boolean replaceStructureField(String fieldName, Structure structure) {
        Structure oldStructure = (Structure)super.getField();
        int index = oldStructure.getFieldIndex(fieldName);
        PVStructure newField = pvDataCreate.createPVStructure(this, fieldName, structure.getFields());
        pvFields[index] = newField;
        // Must create and replace the Structure for this structure.
        Field[] oldFields = oldStructure.getFields();
        int length = oldFields.length;
        Field[] newFields = new Field[length];
        for(int i=0; i<length; i++) newFields[i] = oldFields[i];
        newFields[index] = newField.getStructure();
        Structure newStructure = fieldCreate.createStructure(
            oldStructure.getFieldName(),
            newFields);
        super.replaceField(newStructure);
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getPVFields()
     */
    public PVField[] getPVFields() {
        return pvFields;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getBooleanField(java.lang.String)
     */
    public PVBoolean getBooleanField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvBoolean) {
                return (PVBoolean)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type boolean ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getByteField(java.lang.String)
     */
    public PVByte getByteField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvByte) {
                return (PVByte)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type byte ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getShortField(java.lang.String)
     */
    public PVShort getShortField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvShort) {
                return (PVShort)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type short ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getIntField(java.lang.String)
     */
    public PVInt getIntField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvInt) {
                return (PVInt)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type int ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getLongField(java.lang.String)
     */
    public PVLong getLongField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvLong) {
                return (PVLong)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type long ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getFloatField(java.lang.String)
     */
    public PVFloat getFloatField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvFloat) {
                return (PVFloat)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type float ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getDoubleField(java.lang.String)
     */
    public PVDouble getDoubleField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvDouble) {
                return (PVDouble)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type double ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getStringField(java.lang.String)
     */
    public PVString getStringField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        if(pvField.getField().getType()==Type.scalar) {
            Scalar scalar = (Scalar)pvField.getField();
            if(scalar.getScalarType()==ScalarType.pvString) {
                return (PVString)pvField;
            }
        }
        super.message("fieldName " + fieldName + " does not have type string ",
                MessageType.error);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getStructureField(java.lang.String)
     */
    public PVStructure getStructureField(String fieldName) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.structure) {
            super.message(
                "fieldName " + fieldName + " does not have type structure ",
                MessageType.error);
            return null;
        }
        return (PVStructure)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#getArrayField(java.lang.String, org.epics.pvData.pv.ScalarType)
     */
    public PVArray getArrayField(String fieldName, ScalarType elementType) {
        PVField pvField = findSubField(fieldName,this);
        if(pvField==null) return null;
        Field field = pvField.getField();
        Type type = field.getType();
        if(type!=Type.scalarArray) {
            super.message(
                "fieldName " + fieldName + " does not have type array ",
                MessageType.error);
            return null;
        }
        Array array = (Array)field;
        if(array.getElementType()!=elementType) {
            super.message(
                    "fieldName "
                    + fieldName + " is array but does not have elementType " + elementType.toString(),
                    MessageType.error);
                return null;
        }
        return (PVArray)pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pv.PVStructure#appendPVField(org.epics.pvData.pv.PVField)
     */
    public void appendPVField(PVField pvField) {
        Structure structure = (Structure)super.getField();
        Field[] origFields = structure.getFields();
        Field[] newFields = new Field[origFields.length + 1];
        PVField[] newPVFields = new PVField[pvFields.length + 1];
        for(int i=0; i<origFields.length; i++) {
            newFields[i] = origFields[i];
            newPVFields[i] = pvFields[i];
        }
        newFields[newFields.length-1] = pvField.getField();
        Structure newStructure = fieldCreate.createStructure(structure.getFieldName(), newFields);
        newPVFields[newPVFields.length-1] = pvField;
        super.replaceField(newStructure);
        pvFields = newPVFields;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String prefix = "structure " + super.getField().getFieldName();
        return toString(prefix,0);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.factory.AbstractPVField#toString(int)
     */
    public String toString(int indentLevel) {
        return toString("structure",indentLevel);
    }       
    /**
     * Called by BasePVRecord.
     * @param prefix A prefix for the generated stting.
     * @param indentLevel The indentation level.
     * @return String showing the PVStructure.
     */
    protected String toString(String prefix,int indentLevel) {
        return getString(prefix,indentLevel);
    }
    
    private PVField findSubField(String fieldName,PVStructure pvStructure) {
        int index = fieldName.indexOf('.');
        if(index==-1) {
            index = fieldName.indexOf("[");
            if(index==0) index = fieldName.indexOf(']');
            if(index>0) {
                index++;
            }
        }
        String name = fieldName;
        String restOfName = null;
        if(index>0) {
            name = fieldName.substring(0, index);
            if(fieldName.length()>index) {
                restOfName = fieldName.substring(index);
            }
        }
        PVField pvField = pvStructure.getSubField(name);
        if(restOfName==null) return pvField;
        if(pvField.getField().getType()!=Type.structure) return null;
        return findSubField(restOfName,(PVStructure)pvField);
    }
    private String getString(String prefix,int indentLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        builder.append(super.toString(indentLevel));
        convert.newLine(builder,indentLevel);
        builder.append("{");
        for(int i=0, n= pvFields.length; i < n; i++) {
            convert.newLine(builder,indentLevel + 1);
            Field field = pvFields[i].getField();
            builder.append(field.getFieldName() + " = ");
            builder.append(pvFields[i].toString(indentLevel + 1));            
        }
        convert.newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
    
}