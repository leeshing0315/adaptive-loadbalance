package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.seesharp.tianchi.LoadBalanceContext;

/**
 * @author daofeng.xjf
 * <p>
 * 客户端过滤器
 * 可选接口
 * 用户可以在客户端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.CONSUMER)
public class TestClientFilter implements Filter {

    private static final String START_TIME = "START_TIME";
    private final LoadBalanceContext loadBalanceContext = LoadBalanceContext.getInstance();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long startTime = System.currentTimeMillis();
        if (loadBalanceContext.getInvokerStatsCache().containsKey(invoker.getUrl())) {
            RpcInvocation rpcInvocation = (RpcInvocation) invocation;
            rpcInvocation.setAttachment(START_TIME, String.valueOf(startTime));
        }
        try {
            Result result = invoker.invoke(invocation);
            return result;
        } catch (RpcException e) {
            throw e;
        }
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        String startTime = invocation.getAttachment(START_TIME);
        long durationTime = System.currentTimeMillis() - Long.valueOf(invocation.getAttachment(START_TIME));
        if (StringUtils.isNotEmpty(startTime)) {
            if (result.hasException()) {
                loadBalanceContext.noteResponseTime(invoker, durationTime, true);
            } else {
                loadBalanceContext.noteResponseTime(invoker, durationTime, false);
            }
        }
        return result;
    }
}
