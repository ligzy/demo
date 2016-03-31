package com.realtime.demo;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.SmoothRateLimiter;

/**
 * 流量控制类
 * @author Jeremy Li
 *
 */
public class TrafficShaper {
    private static final ConcurrentMap<String, RateLimiter> resourceLimiterMap =
        Maps.newConcurrentMap();

    public static class RateLimitException extends Exception {

        private static final long serialVersionUID = 1L;

        private String resource;

        public String getResource() {
            return resource;
        }

        public RateLimitException(String resource) {
            super(resource + " should not be visited so frequently");
            this.resource = resource;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    public static void updateResourceQps(String resource, double qps) {
        RateLimiter limiter = resourceLimiterMap.get(resource);
        if (limiter == null) {
            limiter = RateLimiter.create(qps);
            RateLimiter putByOtherThread = resourceLimiterMap.putIfAbsent(resource, limiter);
            if (putByOtherThread != null) {
                limiter = putByOtherThread;
            }
        }
        limiter.setRate(qps);
    }

    public static void removeResource(String resource) {
        resourceLimiterMap.remove(resource);
    }

    public static void enter(String resource) throws RateLimitException {
        RateLimiter limiter = resourceLimiterMap.get(resource);
        if (limiter == null) {
            return;
        }
        if (limiter.acquire() > 0) {
            return;
        }
        if (!limiter.tryAcquire()) {
            throw new RateLimitException(resource);
        }
    }

    public static void exit(String resource) {

    }

    public TrafficShaper() {

    }

    public static void main(String[] args) {
        //do nothing when use RateLimiter
        TrafficShaper.updateResourceQps("ORDER20001", 2000000);

        long timeStart = System.nanoTime();
        int count = 5000000;
        rate(count);
        long timeEnd = System.nanoTime();

        System.out.println(
            "cost(us):"
                + (timeEnd - timeStart) / 1000
                + ",count:"
                + count
                + ",errorcount:"
                + errorcount
                + " avg(us):"
                + (timeEnd - timeStart) / count / 1000
                + " rate(count/s):"
                + String.format("%.1f", (double) count / (timeEnd - timeStart) * 1000000000));

    }
    private static int errorcount = 0;

    private static void rate(int count) {
        int i = 0;
        while (i < count) {
            try {
                TrafficShaper.enter("ORDER20001");
                i++;
                //System.out.println("acquired:" + i);
            } catch (RateLimitException e) {
                errorcount++;
                //  System.out.println("pos:" + i + " " + e.getMessage());
            }
        }

    }
}
