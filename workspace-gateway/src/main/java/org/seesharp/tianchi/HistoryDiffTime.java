package org.seesharp.tianchi;

public class HistoryDiffTime {
    private static final int HISTORY_WINDOW_SIZE = 64;

    private volatile long diffTimeSum;
    private volatile int head;
    private volatile int tail;
    //    private AtomicInteger remain;
    private volatile int remain;
    private volatile long[] diffTimeHistory = new long[HISTORY_WINDOW_SIZE];
    private volatile int diffExceptionSum;
    private volatile boolean[] diffExceptionHistory = new boolean[HISTORY_WINDOW_SIZE];

    private volatile int weight = 100;

//    private Lock lock = new ReentrantLock();

    public HistoryDiffTime() {
        this.head = 0;
        this.tail = HISTORY_WINDOW_SIZE - 1;
//        this.remain = new AtomicInteger(0);
        this.remain = 0;
        for (int i = 0; i < HISTORY_WINDOW_SIZE; i++) {
            this.diffTimeHistory[i] = 0;
        }
        this.diffTimeSum = 0;
    }

    public void noteRemainStart() {
//        this.lock.lock();
//        try {
//            this.remain.incrementAndGet();
        this.remain++;
//        } finally {
//            this.lock.unlock();
//        }
    }

    public void noteValue(long val, boolean hasException) {
//        lock.lock();
//        try {
//            this.remain.decrementAndGet();
        this.remain--;

        this.diffTimeSum -= this.diffTimeHistory[this.head];
        this.diffExceptionSum -= this.diffExceptionHistory[this.head] ? 1 : 0;
        this.head++;
        this.head %= HISTORY_WINDOW_SIZE;

        this.tail++;
        this.tail %= HISTORY_WINDOW_SIZE;
        this.diffTimeHistory[this.tail] = val;
        this.diffTimeSum += val;
        this.diffExceptionHistory[this.tail] = hasException;
        this.diffExceptionSum += hasException ? 1 : 0;
//        } finally {
//            lock.unlock();
//        }
    }

    public int getDiffExceptionSum() {
        return diffExceptionSum;
    }

    public void setDiffExceptionSum(int diffExceptionSum) {
        this.diffExceptionSum = diffExceptionSum;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public long getDiffTimeSum() {
        return diffTimeSum;
    }

    public void setDiffTimeSum(long diffTimeSum) {
        this.diffTimeSum = diffTimeSum;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public int getTail() {
        return tail;
    }

    public void setTail(int tail) {
        this.tail = tail;
    }

//    public AtomicInteger getRemain() {
//        return remain;
//    }
//
//    public void setRemain(AtomicInteger remain) {
//        this.remain = remain;
//    }

    public int getRemain() {
        return remain;
    }

    public void setRemain(int remain) {
        this.remain = remain;
    }

    public long[] getDiffTimeHistory() {
        return diffTimeHistory;
    }

    public void setDiffTimeHistory(long[] diffTimeHistory) {
        this.diffTimeHistory = diffTimeHistory;
    }
}
