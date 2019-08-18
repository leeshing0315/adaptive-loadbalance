package org.seesharp.tianchi;

import org.apache.dubbo.rpc.Invoker;

import java.util.List;

public interface LoadBalanceRule {
    <T> Invoker<T> choose(List<Invoker<T>> invokers);

    LoadBalanceContext getLoadBalanceContext();

    void setLoadBalanceContext(LoadBalanceContext lbContext);
}
