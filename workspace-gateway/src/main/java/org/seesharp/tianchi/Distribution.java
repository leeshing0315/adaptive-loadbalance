package org.seesharp.tianchi;

public class Distribution {

    private volatile long numValues;
    private volatile long sumValues;
    private volatile long sumSquareValues;
    private volatile long minValue;
    private volatile long maxValue;
    private volatile long errorCount;

    public Distribution() {
        clear();
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public void noteValue(long val, boolean hasError) {
        numValues++;
        sumValues += val;
        sumSquareValues += val * val;
        if (numValues == 1) {
            minValue = val;
            maxValue = val;
        } else if (val < minValue) {
            minValue = val;
        } else if (val > maxValue) {
            maxValue = val;
        }
        if (hasError) {
            errorCount++;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        numValues = 0L;
        sumValues = 0L;
        sumSquareValues = 0L;
        minValue = Integer.MAX_VALUE;
        maxValue = -1L;
        errorCount = 0;
    }

    public long getNumValues() {
        return numValues;
    }

    public double getMean() {
        if (numValues < 1) {
            return 0.0;
        } else {
            return (double) sumValues / numValues;
        }
    }

    public int getMeanWithInteger() {
        if (numValues < 1) {
            return 0;
        } else {
            return (int) (sumValues / numValues);
        }
    }

    public double getVariance() {
        if (numValues < 2) {
            return 0.0;
        } else if (sumValues == 0.0) {
            return 0.0;
        } else {
            double mean = getMean();
            return ((double) sumSquareValues / numValues) - mean * mean;
        }
    }

    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    public long getMinimum() {
        return minValue;
    }

    public long getMaximum() {
        return maxValue;
    }

    public void add(Distribution anotherDistribution) {
        if (anotherDistribution != null) {
            numValues += anotherDistribution.numValues;
            sumValues += anotherDistribution.sumValues;
            sumSquareValues += anotherDistribution.sumSquareValues;
            minValue = (minValue < anotherDistribution.minValue) ? minValue
                    : anotherDistribution.minValue;
            maxValue = (maxValue > anotherDistribution.maxValue) ? maxValue
                    : anotherDistribution.maxValue;
        }
    }


    public String toString() {
        return new StringBuilder()
                .append("{Distribution:")
                .append("N=").append(getNumValues())
                .append(": ").append(getMinimum())
                .append("..").append(getMean())
                .append("..").append(getMaximum())
                .append("}")
                .toString();
    }
}
