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
        PageId pid = page.getId();
        int pgNo = pid.pageNumber();
        int pageSize = BufferPool.getPageSize();
        byte[] data = page.getPageData();

        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.seek((long) pgNo * pageSize);
        raf.write(data, 0, data.length);
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        int pageSize = BufferPool.getPageSize();
        return (int) (f.length() / pageSize);
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        HeapPageId pid;
        HeapPage page;
        for(int pgNo = 0; pgNo < numPages(); pgNo++) {
            pid = new HeapPageId(getId(), pgNo);
            page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);

            if(page.getNumEmptySlots() > 0) {
                page.insertTuple(t);
                return new ArrayList<>(Collections.singletonList(page));
            }
        }
        pid = new HeapPageId(getId(), numPages());
        page = new HeapPage(pid, HeapPage.createEmptyPageData());
        page.insertTuple(t);
        writePage(page);
        return new ArrayList<>();
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        RecordId rid = t.getRecordId();
        HeapPageId pid = (HeapPageId) rid.getPageId();
        assert pid.getTableId() == getId();
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
        page.deleteTuple(t);
        return new ArrayList<>(Collections.singletonList(page));
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        return new HeapFileIterator(tid);
    }

    private class HeapFileIterator implements DbFileIterator {
        private int cur;
        private int end;
        private Iterator<Tuple> it;
        private TransactionId tid;

        public HeapFileIterator(TransactionId tid) {
            this.tid = tid;
            cur = end = numPages();
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
            while(cur < end && !getTupleIterator(cur).hasNext()) cur++;
            it = cur < end ? getTupleIterator(cur) : null;
        }

        @Override
        public boolean hasNext() {
            return cur < end;
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }

            Tuple tuple = it.next();
            if(!it.hasNext()) {
                ++cur;
                while(cur < end && !getTupleIterator(cur).hasNext()) cur++;
                it = cur < end ? getTupleIterator(cur) : null;
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
            cur = end;
            it = null;
        }
    }
}

