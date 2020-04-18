package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * */
        public final Type fieldType;
        
        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        return new TDItemIterator();
    }

    /**
     * TDItemIterator implements the iterator of TDItem.
     * */
    private class TDItemIterator implements Iterator<TDItem> {
        private int curPos = 0;

        @Override
        public boolean hasNext() {
            return curPos < tdItems.length;
        }

        @Override
        public TDItem next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
            return tdItems[curPos++];
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     * TDItems is an array to record the types and names of fields.
     * */

    private TDItem[] tdItems;
    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        if(typeAr.length == 0) {
            throw new IllegalArgumentException("typeAr must contain at least one entry");
        }
        if(typeAr.length != fieldAr.length) {
            throw new IllegalArgumentException("typeAr must have the same length as fieldAr");
        }

        tdItems = new TDItem[typeAr.length];
        for (int i = 0; i < tdItems.length; i++) {
            tdItems[i] = new TDItem(typeAr[i], fieldAr[i]);
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        this(typeAr, new String[typeAr.length]);
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        return tdItems.length;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     * 
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        if(i < 0 || i >= tdItems.length) {
            throw new NoSuchElementException();
        }
        return tdItems[i].fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     * 
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        if(i < 0 || i >= tdItems.length) {
            throw new NoSuchElementException();
        }
        return tdItems[i].fieldType;
    }

    /**
     * Find the index of the field with a given name.
     * 
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        if(name == null) {
            throw new NoSuchElementException();
        }
        for(int i = 0; i < tdItems.length; i++) {
            if (name.equals(tdItems[i].fieldName)) {
                return i;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        int totSize = 0;
        for (TDItem item : tdItems) {
            totSize += item.fieldType.getLen();
        }
        return totSize;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     * 
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        int length1 = td1.numFields();
        int length2 = td2.numFields();
        Type[] types = new Type[length1 + length2];
        String[] strings = new String[length1 + length2];

        for(int i = 0; i < length1; i++) {
            types[i] = td1.getFieldType(i);
            strings[i] = td1.getFieldName(i);
        }
        for(int i = 0; i < length2; i++) {
            types[length1 + i] = td2.getFieldType(i);
            strings[length1 + i] = td2.getFieldName(i);
        }
        return new TupleDesc(types, strings);
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they are the same size and if the n-th
     * type in this TupleDesc is equal to the n-th type in td.
     * 
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        else {
            if(o instanceof TupleDesc) {
                if(this.numFields() != ((TupleDesc) o).numFields()) {
                    return false;
                }
                for(int i = 0; i < numFields(); i++) {
                    if(!getFieldType(i).equals(((TupleDesc) o).getFieldType(i))) {
                        return false;
                    }
                    if(!Utility.equalsWithNulls(getFieldName(i), ((TupleDesc) o).getFieldName(i))) {
                        return false;
                    }
                }
                return true;
            }
            else {
                return false;
            }
        }
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     * 
     * @return String describing this descriptor.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Fields: ");
        for (TDItem item : tdItems) {
            result.append(item.toString()).append(", ");
        }
        result.append(numFields()).append(" fields in all.");
        return result.toString();
    }
}
