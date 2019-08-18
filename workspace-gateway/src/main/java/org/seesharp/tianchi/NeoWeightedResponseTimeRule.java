package org.seesharp.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

public class NeoWeightedResponseTimeRule extends RoundRobinRule {
    private static final int SERVER_WEIGHT_TASK_TIMER_INTERVAL = 500;
    private static final double INCREASE_FACTOR = 1.2;
    private static final double DECREASE_FACTOR = 0.9;

    private volatile List<URL> weightPositions = new ArrayList<>();
    private volatile List<Integer> accumulatedWeights = new ArrayList<Integer>();
    private AtomicBoolean serverWeightAssignmentInProgress = new AtomicBoolean(false);
    private Timer serverWeightTimer = null;
    private Lock lock = null;

    public NeoWeightedResponseTimeRule(LoadBalanceContext lbContext) {
        super(lbContext);
        initialize(lbContext);
    }

    private void initialize(LoadBalanceContext lbContext) {
        this.lock = lbContext.getLock();
        if (serverWeightTimer != null) {
            serverWeightTimer.cancel();
        }
        serverWeightTimer = new Timer("LoadBalancer-serverWeightTimer", true);
        serverWeightTimer.schedule(new DynamicServerWeightTask(), 0, SERVER_WEIGHT_TASK_TIMER_INTERVAL);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> serverWeightTimer.cancel()));
    }

    @Override
    public <T> Invoker<T> choose(List<Invoker<T>> invokers) {
        Invoker<T> invoker = null;

        while (invoker == null) {
            List<URL> currentPostions = weightPositions;
            List<Integer> currentWeights = accumulatedWeights;
            if (Thread.interrupted()) {
                return null;
            }

            int serverCount = invokers.size();

            if (serverCount == 0) {
                return null;
            }

            int serverIndex = 0;

            int maxTotalWeight = currentWeights.size() == 0 ? 0 : currentWeights.get(currentWeights.size() - 1);

            if (maxTotalWeight < 1 || serverCount != currentWeights.size()) {
                invoker = super.choose(invokers);
                if (invoker == null) {
                    return null;
                }
            } else {
                int randomWeight = ThreadLocalRandom.current().nextInt(maxTotalWeight);

                int n = 0;
                for (Integer integer : currentWeights) {
                    if (integer >= randomWeight) {
                        serverIndex = n;
                        break;
                    } else {
                        n++;
                    }
                }

                URL url = currentPostions.get(serverIndex);
                for (Invoker<T> i : invokers) {
                    if (i.getUrl().equals(url)) {
                        invoker = i;
                    }
                }
            }

            if (invoker == null) {
                Thread.yield();
                continue;
            }

            if (invoker.isAvailable()) {
                return invoker;
            }

            invoker = null;
        }
        return invoker;
    }

    void setWeightsPos(List<URL> weightsPos) {
        this.weightPositions = weightsPos;
    }

    void setWeights(List<Integer> weights) {
        this.accumulatedWeights = weights;
    }

    @Override
    public void addNewComer(Invoker invoker, int weight) {
        this.weightPositions.add(invoker.getUrl());
        if (this.accumulatedWeights.size() == 0) {
            this.accumulatedWeights.add(weight);
        } else {
            this.accumulatedWeights.add(this.accumulatedWeights.get(this.accumulatedWeights.size() - 1) + weight);
        }
    }

    class DynamicServerWeightTask extends TimerTask {
        public void run() {
            lock.lock();
            try {
//                if (!serverWeightAssignmentInProgress.compareAndSet(false, true)) {
//                    return;
//                }
                LoadBalanceContext lbContext = getLoadBalanceContext();
                if (lbContext.getInvokerStatsCache().size() == 0) {
                    return;
                }

                int totalResponseTime = 0;

                boolean someHasException = false;
                boolean someHasNoException = false;
                int maxResponseTimeAvg = -1;

                for (Map.Entry<URL, InvokerStats> entry : lbContext.getInvokerStatsCache().entrySet()) {
                    if (entry.getValue().getNumCount() == 0) {
//                        System.out.println("too less call");
                        return;
                    }
                    int currentResponseTimeAvg = entry.getValue().getResponseTimeAvg();
                    totalResponseTime += currentResponseTimeAvg;
                    if (currentResponseTimeAvg > maxResponseTimeAvg) {
                        maxResponseTimeAvg = currentResponseTimeAvg;
                    }
                    if (entry.getValue().getExceptionCount() > 0) {
                        someHasException = true;
                    } else {
                        someHasNoException = true;
                    }
                }
//                if (someHasException && someHasNoException) {
//                    setWeightByTimeAndException(lbContext, totalResponseTime, maxResponseTimeAvg);
//                } else {
//                    setWeightByTime(lbContext, totalResponseTime, maxResponseTimeAvg);
//                }
                setWeightByTime(lbContext, totalResponseTime, maxResponseTimeAvg);
            } catch (Exception e) {
                System.out.printf("Error running DynamicServerWeightTask for %s\n", e);
            } finally {
//                serverWeightAssignmentInProgress.set(false);
                lock.unlock();
            }
        }

        private void setWeightByTimeAndException(LoadBalanceContext lbContext, int totalResponseTime, int maxResponseTimeAvg) {
            int weightSoFar = 0;
            List<URL> finalWeightsPos = new ArrayList<URL>();
            List<Integer> finalWeights = new ArrayList<Integer>();
            int invokerSize = lbContext.getInvokerStatsCache().size();
            int averageOriginalWeight = totalResponseTime * (invokerSize - 1) / invokerSize;
            for (Map.Entry<URL, InvokerStats> entry : lbContext.getInvokerStatsCache().entrySet()) {
                InvokerStats invokerStats = entry.getValue();

                int fixedLastWeight = invokerStats.getWeight();

                int originalCurrentWeight = totalResponseTime - invokerStats.getResponseTimeAvg();
                int fixedCurrentWeight = fixedLastWeight * originalCurrentWeight / averageOriginalWeight;

                int finalWeight;
                if (invokerStats.getExceptionCount() > 0) {
                    if (fixedLastWeight > fixedCurrentWeight) {
                        finalWeight = Math.max((int) (fixedLastWeight * DECREASE_FACTOR), fixedCurrentWeight);
                    } else {
                        finalWeight = Math.min((int) (fixedLastWeight * INCREASE_FACTOR), fixedCurrentWeight);
                    }
                } else {
                    finalWeight = Math.max((int) (fixedLastWeight * INCREASE_FACTOR), fixedCurrentWeight);
                }

                weightSoFar += finalWeight;
                finalWeightsPos.add(entry.getKey());
                finalWeights.add(weightSoFar);

                invokerStats.setWeight(finalWeight);
//                invokerStats.logDistAndWeight();
                invokerStats.clear();
            }

//            cleansingAndSetWeights(lbContext, finalWeightsPos, finalWeights, minFinalWeight);

            setWeightsPos(finalWeightsPos);
            setWeights(finalWeights);
        }

        private void setWeightByTime(LoadBalanceContext lbContext, int totalResponseTime, int maxResponseTimeAvg) {
            int weightSoFar = 0;
            List<URL> finalWeightsPos = new ArrayList<URL>();
            List<Integer> finalWeights = new ArrayList<Integer>();
            int invokerSize = lbContext.getInvokerStatsCache().size();
            int averageOriginalWeight = totalResponseTime * (invokerSize - 1) / invokerSize;
            for (Map.Entry<URL, InvokerStats> entry : lbContext.getInvokerStatsCache().entrySet()) {
                InvokerStats invokerStats = entry.getValue();

                int fixedLastWeight = invokerStats.getWeight();

                int originalCurrentWeight = totalResponseTime - invokerStats.getResponseTimeAvg();
                int fixedCurrentWeight = fixedLastWeight * originalCurrentWeight / averageOriginalWeight;

                int finalWeight;
                if (fixedLastWeight > fixedCurrentWeight) {
                    finalWeight = Math.max((int) (fixedLastWeight * DECREASE_FACTOR), fixedCurrentWeight);
                } else {
                    finalWeight = Math.min((int) (fixedLastWeight * INCREASE_FACTOR), fixedCurrentWeight);
                }
//                finalWeight = fixedCurrentWeight;

                weightSoFar += finalWeight;
                finalWeightsPos.add(entry.getKey());
                finalWeights.add(weightSoFar);

                invokerStats.setWeight(finalWeight);
//                invokerStats.logDistAndWeight();
                invokerStats.clear();
            }

//            cleansingAndSetWeights(lbContext, finalWeightsPos, finalWeights, minFinalWeight);

            setWeightsPos(finalWeightsPos);
            setWeights(finalWeights);
        }

        private void cleansingAndSetWeights(LoadBalanceContext lbContext, List<URL> finalWeightsPos, List<Integer> finalWeights, int minFinalWeight) {
            Map<URL, InvokerStats> map = lbContext.getInvokerStatsCache();
            for (int i = 0; i < finalWeights.size(); i++) {
                int result = finalWeights.get(i) * 100 / minFinalWeight;
                map.get(finalWeightsPos.get(i)).setWeight(result);
                finalWeights.set(i, result);
            }
        }
    }
}
