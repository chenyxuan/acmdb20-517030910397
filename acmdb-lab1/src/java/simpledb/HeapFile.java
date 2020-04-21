package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private File f;
    private TupleDesc td;
    private int numPages;

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        this.f = f;
        this.td = td;
        numPages = (int) (f.length() / BufferPool.getPageSize());
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        int tableId = pid.getTableId();
        int pgNo = pid.pageNumber();
        int pageSize = BufferPool.getPageSize();
        byte[] data = HeapPage.createEmptyPageData();
        HeapPage page = null;

        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            raf.seek(pgNo * pageSize);
            raf.read(data, 0, data.length);
            page = new HeapPage(new HeapPageId(tableId, pgNo), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return page;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        return numPages;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        return new HeapFileIterator(tid);
    }

    private class HeapFileIterator implements DbFileIterator {
        private int cur;
        private Iterator<Tuple> it;
        private TransactionId tid;

        public HeapFileIterator(TransactionId tid) {
            this.tid = tid;
            cur = numPages;
            it = null;
        }

        private Iterator<Tuple> getTupleIterator(int pgNo)
            throws TransactionAbortedException, DbException {
            HeapPageId pid = new HeapPageId(getId(), pgNo);
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
            return page.iterator();
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            cur = 0;
            while(cur < numPages && !getTupleIterator(cur).hasNext()) cur++;
            it = cur < numPages ? getTupleIterator(cur) : null;
        }

        @Override
        public boolean hasNext() {
            return cur < numPages;
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            Tuple tuple = it.next();
            if(!it.hasNext()) {
                ++cur;
                while(cur < numPages && !getTupleIterator(cur).hasNext()) cur++;
                it = cur < numPages ? getTupleIterator(cur) : null;
            }
            return tuple;
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            close();
            open();
        }

        @Override
        public void close() {
            cur = numPages;
            it = null;
        }
    }
}

