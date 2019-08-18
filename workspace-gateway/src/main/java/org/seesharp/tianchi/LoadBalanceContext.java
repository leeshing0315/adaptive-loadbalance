package org.seesharp.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LoadBalanceContext {
    private static LoadBalanceContext ourInstance = new LoadBalanceContext();
    private AbstractLoadBalanceRule rule;
    private Map<URL, InvokerStats> invokerStatsCache = new ConcurrentHashMap<>();
    private Lock lock = new ReentrantLock();
    private volatile long maxResponseTime = 0;

    private LoadBalanceContext() {
//        rule = new RandomRule(this);
//        rule = new RoundRobinRule(this);
//        rule = new HistoryDiffTimeRule(this);
        rule = new NeoWeightedResponseTimeRule(this);
    }

    public static LoadBalanceContext getInstance() {
        return ourInstance;
    }

    public Lock getLock() {
        return lock;
    }

    public <T> Invoker<T> choose(List<Invoker<T>> invokers) {
        try {
            Invoker<T> newComer = maintainInvokers(invokers);
            if (newComer != null) {
                return newComer;
            }

            if (CollectionUtils.isEmpty(invokers)) {
                return null;
            }
            if (invokers.size() == 1) {
                return invokers.get(0);
            }
            return rule.choose(invokers);
        } catch (Exception e) {
            return null;
        }
    }

    public void noteResponseTime(Invoker invoker, long nsecs, boolean hasException) {
        lock.lock();
        if (maxResponseTime < nsecs) {
            maxResponseTime = nsecs;
        }
        try {
            InvokerStats stats = invokerStatsCache.get(invoker.getUrl());
            if (stats != null) {
                if (hasException) {
                    stats.noteResponseTime(maxResponseTime * 3, true);
                } else {
                    stats.noteResponseTime(nsecs, false);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private <T> Invoker<T> maintainInvokers(List<Invoker<T>> invokers) {
        Invoker<T> newComer = null;
        for (Invoker<T> invoker : invokers) {
            if (!invokerStatsCache.containsKey(invoker.getUrl())) {
                lock.lock();
                try {
                    if (!invokerStatsCache.containsKey(invoker.getUrl())) {
                        // 100 400 500
                        if (invoker.getUrl().getHost().contains("large")) {
                            invokerStatsCache.put(invoker.getUrl(), new InvokerStats(500));
                            rule.addNewComer(invoker, 500);
                        } else if (invoker.getUrl().getHost().contains("medium")) {
                            invokerStatsCache.put(invoker.getUrl(), new InvokerStats(400));
                            rule.addNewComer(invoker, 400);
                        } else {
                            invokerStatsCache.put(invoker.getUrl(), new InvokerStats(100));
                            rule.addNewComer(invoker, 100);
                        }

                        newComer = invoker;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        return newComer;
    }

    public Map<URL, InvokerStats> getInvokerStatsCache() {
        return invokerStatsCache;
    }
}
