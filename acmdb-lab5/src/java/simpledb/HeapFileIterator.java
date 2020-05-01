package simpledb;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class HeapFileIterator implements DbFileIterator {
    private int page_id;
    private int table_id;
    private int max_pages;
    private TransactionId tid;
    private BufferPool buffer_pool;

    // 这里一开始以为要用page iterator,但其实我们要遍历的是table上的tuple
    // page只是tuple存储时的数据结构；而且page.iterator返回的就是Tuple类型的iterator
    private Iterator<Tuple> cur_tuple_iter = null;

    public HeapFileIterator(TransactionId tid, int table_id, int max_pages) {
        this.tid = tid;
        this.page_id = 0;
        this.table_id = table_id;
        this.max_pages = max_pages;
        this.buffer_pool = Database.getBufferPool();
    }

    private Iterator<Tuple> GetTupleIterator(HeapPageId pid) throws TransactionAbortedException, DbException {
        // TODO: What's the parameter 'perm' used for??
//        boolean isEmpty; // Avoid Empty Pages
//        HeapPage page;
        HeapPageId page_id = pid;

//        do{
//            page = (HeapPage) buffer_pool.getPage(tid, page_id, null);
//            isEmpty = (page.numSlots == page.getNumEmptySlots());
//            page_id = new HeapPageId(page_id.getTableId(), page_id.pageNumber() + 1);
//        }while(isEmpty && page_id.pageNumber() < max_pages);
        HeapPage page = (HeapPage) buffer_pool.getPage(tid, page_id, Permissions.READ_ONLY);

        return page.iterator();
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        if (this.cur_tuple_iter != null){
            throw new DbException("already opened");
        }
        this.cur_tuple_iter = GetTupleIterator(new HeapPageId(table_id, page_id));
    }

    @Override
    public boolean hasNext() throws DbException, TransactionAbortedException {
        if (cur_tuple_iter == null) {
            System.err.println("null ptr ");
            return false;
        }
        if (cur_tuple_iter.hasNext()) {
            return true;
        }

        if (page_id + 1 >= max_pages) {
            // TODO: Problem!! large data replacement but iterator has no next!
//            System.err.println("full page " + page_id + " " + max_pages);
//            throw new DbException("Page exceed max-page-size");
            return false;
        }
        // ptr at the last tuple of current page, but still there are non-empty pages.
        page_id++;
        cur_tuple_iter = GetTupleIterator(new HeapPageId(table_id, page_id));
        return cur_tuple_iter.hasNext();
    }

    @Override
    public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException("DbFile EOF!");
        }
        return cur_tuple_iter.next();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        close();
        open();
    }

    @Override
    public void close() {
        cur_tuple_iter = null;
        page_id = 0;
    }
}

