package org.seesharp.tianchi;

import org.apache.dubbo.rpc.Invoker;

public abstract class AbstractLoadBalanceRule implements LoadBalanceRule {

    private LoadBalanceContext lbContext;

    public AbstractLoadBalanceRule(LoadBalanceContext lbContext) {
        this.lbContext = lbContext;
    }

    @Override
    public LoadBalanceContext getLoadBalanceContext() {
        return lbContext;
    }

    @Override
    public void setLoadBalanceContext(LoadBalanceContext lb) {
        this.lbContext = lb;
    }

    public void addNewComer(Invoker invoker, int weight){}
}
