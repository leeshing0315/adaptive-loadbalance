package org.seesharp.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.Map;

public class HistoryDiffTimeRule extends AbstractLoadBalanceRule {

    private static final int DIRECT_SEND_LIMIT = 100;

    public HistoryDiffTimeRule(LoadBalanceContext lbContext) {
        super(lbContext);
    }

    @Override
    public <T> Invoker<T> choose(List<Invoker<T>> invokers) {
        Invoker<T> result = null;
        long minCost = Long.MAX_VALUE;

        Map<URL, InvokerStats> invokerStatMap = this.getLoadBalanceContext().getInvokerStatsCache();
        InvokerStats invokerStats = null;
        int totalResponseTime = 0;
        boolean hasInvokerNoException = false;
        boolean hasInvokerException = false;
        long maxInvokerDiffTimeSum = -1;
        Invoker<T> maxDiffTimeInvkoer = null;
        for (Invoker<T> invoker : invokers) {
            invokerStats = invokerStatMap.get(invoker.getUrl());
            if (invokerStats == null || invokerStats.getHistoryDiffTime().getRemain() < DIRECT_SEND_LIMIT) {
                result = invoker;
                break;
            }
            if (invokerStats.getHistoryDiffTime().getDiffExceptionSum() > 0) {
                hasInvokerException = true;
            } else {
                hasInvokerNoException = true;
            }
            long currentInvokerDiffTimeSum = invokerStats.getHistoryDiffTime().getDiffTimeSum();
            if (maxInvokerDiffTimeSum < currentInvokerDiffTimeSum) {
                maxInvokerDiffTimeSum = currentInvokerDiffTimeSum;
                maxDiffTimeInvkoer = invoker;
            }
            totalResponseTime += invokerStats.getHistoryDiffTime().getDiffTimeSum();
//            long temp = invokerStats.getHistoryDiffTime().getDiffTimeSum();
//            if (temp < minCost) {
//                result = invoker;
//                minCost = temp;
//            }
        }
        if (result == null) {
            // some invokers normal and some invokers has error
            if (hasInvokerNoException && hasInvokerException) {
                result = chooseByBothFactor(result);
            }
            // all invokers normal
            else if (hasInvokerNoException) {
                result = chooseByDiffTimeSum(invokers);
            }
            // all invokers has error
            else if (hasInvokerException) {
                result = chooseByExceptionNum(invokers);
            }
        }

        return result;
    }

    private <T> Invoker<T> chooseByBothFactor(Invoker<T> result) {
        return null;
    }

    private <T> Invoker<T> chooseByDiffTimeSum(List<Invoker<T>> invokers) {
        long minCost = Long.MAX_VALUE;
        Invoker<T> result = null;
        for (Invoker<T> invoker : invokers) {
            InvokerStats invokerStats = this.getLoadBalanceContext().getInvokerStatsCache().get(invoker.getUrl());
            long temp = invokerStats.getHistoryDiffTime().getDiffTimeSum();
            if (temp < minCost) {
                result = invoker;
                minCost = temp;
            }
        }
        return result;
    }

    private <T> Invoker<T> chooseByExceptionNum(List<Invoker<T>> invokers) {
        int minExceptionNum = Integer.MAX_VALUE;
        Invoker<T> result = null;
        for (Invoker<T> invoker : invokers) {
            InvokerStats invokerStats = this.getLoadBalanceContext().getInvokerStatsCache().get(invoker.getUrl());
            int temp = invokerStats.getHistoryDiffTime().getDiffExceptionSum();
            if (temp < minExceptionNum) {
                result = invoker;
                minExceptionNum = temp;
            }
        }
        return result;
    }

    private long estimateCost(Map<URL, InvokerStats> invokerStatMap, Invoker invoker) {
        InvokerStats invokerStats = invokerStatMap.get(invoker.getUrl());
        if (invokerStats == null || invokerStats.getHistoryDiffTime().getRemain() < DIRECT_SEND_LIMIT) {
            return 0;
        }
        return invokerStats.getHistoryDiffTime().getDiffTimeSum();
    }
}
