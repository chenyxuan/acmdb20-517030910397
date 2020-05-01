package simpledb;

import java.util.*;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private final int gbfield;
    private final Type gbfieldtype;
    private final int afield;
    private final Op what;
    private TreeMap<Object, Integer > aggr;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("StringAggregator only support COUNT");
        }
        this.what = what;
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;

        aggr = new TreeMap<>();
        if (this.gbfield == Aggregator.NO_GROUPING) {
            aggr.put(0, 0);
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        if (gbfield == Aggregator.NO_GROUPING) {
            aggr.put(0, aggr.get(0) + 1);
        } else {
            Object gbKey = gbfieldtype == Type.INT_TYPE ?
                    ((IntField) tup.getField(gbfield)).getValue() :
                    ((StringField) tup.getField(gbfield)).getValue();
            if(aggr.containsKey(gbKey)) {
                aggr.put(gbKey, aggr.get(gbKey) + 1);
            } else {
                aggr.put(gbKey, 1);
            }
        }

    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        return new StringAggrDbIterator();
    }

    private class StringAggrDbIterator implements DbIterator {
        private Iterator<Tuple> it;
        private ArrayList<Tuple> res;

        public StringAggrDbIterator() {
            assert what == Op.COUNT;

            this.it = null;
            res = new ArrayList<>();
            if (gbfield == Aggregator.NO_GROUPING) {
                Tuple t = new Tuple(getTupleDesc());
                Field aggregateVal = new IntField(aggr.get(0));
                t.setField(0, aggregateVal);
                res.add(t);
            } else {
                for (Map.Entry<Object, Integer> e : aggr.entrySet()) {
                    Tuple t = new Tuple(getTupleDesc());
                    Field groupVal = (gbfieldtype == Type.INT_TYPE) ?
                            new IntField((int) e.getKey()) :
                            new StringField((String) e.getKey(), ((String) e.getKey()).length());
                    Field aggregateVal = new IntField(e.getValue());
                    t.setField(0, groupVal);
                    t.setField(1, aggregateVal);
                    res.add(t);
                }
            }
        }

        /**
         * Opens the iterator. This must be called before any of the other methods.
         *
         * @throws DbException when there are problems opening/accessing the database.
         */


        @Override
        public void open() throws DbException, TransactionAbortedException {
            it = res.iterator();
        }

        /**
         * Returns true if the iterator has more tuples.
         *
         * @return true f the iterator has more tuples.
         * @throws IllegalStateException If the iterator has not been opened
         */
        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (it == null) {
                throw new IllegalStateException("IntegerAggregator not open");

            }
            return it.hasNext();

        }

        /**
         * Returns the next tuple from the operator (typically implementing by reading
         * from a child operator or an access method).
         *
         * @return the next tuple in the iteration.
         * @throws NoSuchElementException if there are no more tuples.
         * @throws IllegalStateException  If the iterator has not been opened
         */
        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (it == null) {
                throw new IllegalStateException("IntegerAggregator not open");
            }
            return it.next();
        }

        /**
         * Resets the iterator to the start.
         *
         * @throws DbException           when rewind is unsupported.
         * @throws IllegalStateException If the iterator has not been opened
         */
        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            if (it == null) {
                throw new IllegalStateException("IntegerAggregator not open");
            }
            it = res.iterator();
        }

        /**
         * Returns the TupleDesc associated with this DbIterator.
         *
         * @return the TupleDesc associated with this DbIterator.
         */
        @Override
        public TupleDesc getTupleDesc() {
            if (gbfield == Aggregator.NO_GROUPING) {
                return new TupleDesc(new Type[]{Type.INT_TYPE});
            } else {
                return new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE});
            }
        }

        /**
         * Closes the iterator. When the iterator is closed, calling next(),
         * hasNext(), or rewind() should fail by throwing IllegalStateException.
         */
        @Override
        public void close() {
            it = null;
        }
    }

}