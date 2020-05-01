package simpledb;

import java.util.*;
import java.util.concurrent.*;

public class LockManager {
    private final ConcurrentHashMap<PageId, Object> locks;
    private final ConcurrentHashMap<PageId, TransactionId> exclusiveLocks;
    private final ConcurrentHashMap<PageId, ConcurrentLinkedDeque<TransactionId>> sharedLocks;
    private final ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<PageId>> transactionHoldLocks;
    private final ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<PageId>> transactionHoldXLocks;
    private final ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<TransactionId>> dependencyGraph;

    private LockManager() {
        locks = new ConcurrentHashMap<>();
        sharedLocks = new ConcurrentHashMap<>();
        exclusiveLocks = new ConcurrentHashMap<>();
        transactionHoldLocks = new ConcurrentHashMap<>();
        transactionHoldXLocks = new ConcurrentHashMap<>();
        dependencyGraph = new ConcurrentHashMap<>();
    }

    public static LockManager GetLockManager() {
        return new LockManager();
    }


    private boolean hasLock(TransactionId tid, PageId pid, boolean isReadOnly) {
        if (exclusiveLocks.containsKey(pid) && tid.equals(exclusiveLocks.get(pid))) {
            return true;
        }
        return isReadOnly && sharedLocks.containsKey(pid) && sharedLocks.get(pid).contains(tid);
    }

    private Object getLock(PageId pid) {
        locks.putIfAbsent(pid, new Object());
        return locks.get(pid);
    }

    public boolean acquireLock(TransactionId tid, PageId pid, Permissions perm) throws TransactionAbortedException {
        if (perm == Permissions.READ_ONLY) {
            if (hasLock(tid, pid, true)) return true;
            acquireSLock(tid, pid);
        } else if (perm == Permissions.READ_WRITE) {
            if (hasLock(tid, pid, false)) return true;
            acquireXLock(tid, pid);
        }
        updateTransactionLocks(tid, pid);
        return true;
    }

    private void acquireSLock(TransactionId tid, PageId pid) throws TransactionAbortedException {
        Object lock = getLock(pid);

        while (true) {
            synchronized (lock) {
                TransactionId holder = exclusiveLocks.get(pid);
                boolean notBlocked = (holder == null || holder.equals(tid));

                if (notBlocked) {
                    removeDependency(tid);
                    addSTransaction(pid, tid);
                    return;
                }
                ArrayList<TransactionId> holders = new ArrayList<>();
                holders.add(holder);
                updateDependency(tid, holders);
            }
        }
    }

    private void acquireXLock(TransactionId tid, PageId pid) throws TransactionAbortedException {
        Object lock = getLock(pid);

        while (true) {
            synchronized (lock) {
                ArrayList<TransactionId> holders = new ArrayList<>();
                if (exclusiveLocks.containsKey(pid)) {
                    holders.add(exclusiveLocks.get(pid));
                }
                if (sharedLocks.containsKey(pid)) {
                    holders.addAll(sharedLocks.get(pid));
                }

                boolean notBlocked = holders.size() == 0 || (holders.size() == 1 && holders.get(0).equals(tid));

                if (notBlocked) {
                    removeDependency(tid);
                    addXTransaction(pid, tid);
                    return;
                }
                updateDependency(tid, holders);
            }
        }
    }

    private void addSTransaction(PageId pid, TransactionId tid) {
        sharedLocks.putIfAbsent(pid, new ConcurrentLinkedDeque<>());
        sharedLocks.get(pid).add(tid);
    }

    private void addXTransaction(PageId pid, TransactionId tid) {
        exclusiveLocks.put(pid, tid);
        transactionHoldXLocks.putIfAbsent(tid, new ConcurrentLinkedDeque<>());
        transactionHoldXLocks.get(tid).add(pid);
    }


    private void removeDependency(TransactionId tid) {
        synchronized (dependencyGraph) {
            dependencyGraph.remove(tid);
            for (TransactionId curtid : dependencyGraph.keySet()) {
                dependencyGraph.get(curtid).remove(tid);
            }
        }
    }

    private void updateDependency(TransactionId acquirer, ArrayList<TransactionId> holders)
            throws TransactionAbortedException {
        dependencyGraph.putIfAbsent(acquirer, new ConcurrentLinkedDeque<>());
        boolean hasChange = false;
        ConcurrentLinkedDeque<TransactionId> childs = dependencyGraph.get(acquirer);
        for (TransactionId holder : holders) {
            if (!childs.contains(holder) && !holder.equals(acquirer)) {
                hasChange = true;
                dependencyGraph.get(acquirer).add(holder);
            }
        }
        if (hasChange) {
            checkDeadLock(acquirer, new HashSet<>());
        }
    }


    private void checkDeadLock(TransactionId root, HashSet<TransactionId> visit) throws TransactionAbortedException {
        // DFS Checking self-loop
        if (!dependencyGraph.containsKey(root))
            return;
        for (TransactionId child : dependencyGraph.get(root)) {
            if (visit.contains(child)) {
                throw new TransactionAbortedException();
            }
            visit.add(child);
            checkDeadLock(child, visit);
            visit.remove(child);
        }
    }

    private void updateTransactionLocks(TransactionId tid, PageId pid) {
        transactionHoldLocks.putIfAbsent(tid, new ConcurrentLinkedDeque<>());
        transactionHoldLocks.get(tid).add(pid);
    }


    public void releasePage(TransactionId tid, PageId pid) {
        if (holdsLock(tid, pid)) {
            Object lock = getLock(pid);
            synchronized (lock) {
                if (sharedLocks.containsKey(pid)) {
                    sharedLocks.get(pid).remove(tid);
                }
                if (exclusiveLocks.containsKey(pid) && exclusiveLocks.get(pid).equals(tid)) {
                    exclusiveLocks.remove(pid);
                }
                if (transactionHoldLocks.containsKey(tid)) {
                    transactionHoldLocks.get(tid).remove(pid);
                }
                if (transactionHoldXLocks.containsKey(tid)) {
                    transactionHoldXLocks.get(tid).remove(pid);
                }
            }
        }
    }

    public void releasePages(TransactionId tid) {
        if (transactionHoldLocks.containsKey(tid)) {
            for (PageId pid : transactionHoldLocks.get(tid)) {
                releasePage(tid, pid);
            }
        }
        transactionHoldXLocks.remove(tid);
    }

    public boolean holdsLock(TransactionId tid, PageId pid) {
        return hasLock(tid, pid, true) || hasLock(tid, pid, false);
    }

    public ConcurrentHashMap<TransactionId, ConcurrentLinkedDeque<PageId>> getTransactionDirtiedPages() {
        return transactionHoldXLocks;
    }
}
