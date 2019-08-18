package org.seesharp.tianchi;

import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomRule extends AbstractLoadBalanceRule {

    public RandomRule(LoadBalanceContext lbContext) {
        super(lbContext);
    }

    @Override
    public <T> Invoker<T> choose(List<Invoker<T>> invokers) {
        return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
    }
}
