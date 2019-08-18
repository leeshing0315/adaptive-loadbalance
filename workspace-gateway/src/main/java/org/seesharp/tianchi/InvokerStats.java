package org.seesharp.tianchi;

import java.util.concurrent.atomic.AtomicLong;

public class InvokerStats {

    private final Distribution responseTimeDist = new Distribution();

    private final HistoryDiffTime historyDiffTime = new HistoryDiffTime();

    private volatile int weight;

    public InvokerStats(int initWeight) {
        this.weight = initWeight;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void noteResponseTime(long nsecs, boolean hasException) {
        responseTimeDist.noteValue(nsecs, hasException);
    }

    public int getResponseTimeAvg() {
        return responseTimeDist.getMeanWithInteger();
    }

    public long getNumCount() {
        return responseTimeDist.getNumValues();
    }

    public long getExceptionCount() {
        return responseTimeDist.getErrorCount();
    }

    public void clear() {
        responseTimeDist.clear();
    }

    public HistoryDiffTime getHistoryDiffTime() {
        return historyDiffTime;
    }
}
