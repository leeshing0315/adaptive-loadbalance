package org.seesharp.tianchi;

import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRule extends AbstractLoadBalanceRule {

    private AtomicInteger nextInvokerCyclicCounter;

    public RoundRobinRule(LoadBalanceContext lbContext) {
        super(lbContext);
        nextInvokerCyclicCounter = new AtomicInteger(0);
    }

    @Override
    public <T> Invoker<T> choose(List<Invoker<T>> invokers) {
        int invokerCount = invokers.size();
        if (invokerCount == 0) {
            return null;
        }

        int nextInvokerIndex = incrementAndGetModulo(invokerCount);
        return invokers.get(nextInvokerIndex);
    }

    private int incrementAndGetModulo(int modulo) {
        for (; ; ) {
            int current = nextInvokerCyclicCounter.get();
            int next = (current + 1) % modulo;
            if (nextInvokerCyclicCounter.compareAndSet(current, next))
                return next;
        }
    }
}
