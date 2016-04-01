/**
 * 
 * Title:        United Risk Management System
 * Description:  
 * Copyright:    Copy Right (c) 2015—2016
 * Company:      ROOTNET
 * @author       Jeremy Li
 * @date         2016年4月1日
 */
package com.realtime.demo;

import java.util.concurrent.TimeUnit;

import org.isomorphism.util.TokenBucket;
import org.isomorphism.util.TokenBuckets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.realtime.demo.tokenbucket.TrafficShaper;
import com.realtime.demo.tokenbucket.TrafficShaper.RateLimitException;

import me.sudohippie.throttle.Throttle;
import me.sudohippie.throttle.strategy.ThrottleStrategy;
import me.sudohippie.throttle.strategy.bucket.FixedTokenBucketStrategy;

/**
 * @author Jeremy Li
 *
 */
public class TokenBucketTest {

    private int waitcount = 0;
    //////////////////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////////////
    TokenBucket bucket =
        TokenBuckets
            .builder()
            .withCapacity(1000)
            .withFixedIntervalRefillStrategy(1000, 1, TimeUnit.SECONDS)
            .build();

    //////////////////////////////////////////////////////////////////////////////////////
    // construct strategy
    ThrottleStrategy strategy = new FixedTokenBucketStrategy(1000, 1, TimeUnit.SECONDS);

    // provide the strategy to the throttler
    Throttle throttle = new Throttle(strategy);

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        TrafficShaper.updateResourceQps("ORDER20001", 1000);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        // throttle :)
        boolean isThrottled = throttle.canProceed();

        long timeStart = System.nanoTime();
        int count = 10000;
        int i = 0;
        while (i < count) {
            bbck_rate();
            i++;
        }
        long timeEnd = System.nanoTime();

        System.out.println(
            "cost(us):"
                + (timeEnd - timeStart) / 1000
                + ",count:"
                + count
                + ",waitcount:"
                + waitcount
                + " avg(us):"
                + (timeEnd - timeStart) / count / 1000
                + " rate(count/s):"
                + String.format("%.1f", (double) count / (timeEnd - timeStart) * 1000000000));
        System.out.println(isThrottled);

    }
    private void throttle() {
        while (!throttle.canProceed()) {
            long sleep = throttle.waitTime(TimeUnit.MILLISECONDS);
            waitcount++;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }
        }
    }

    private void bbck_rate() {

        bucket.consume(1);

    }

    private void guava_rate(int count) {

        try {
            TrafficShaper.enter("ORDER20001");
        } catch (RateLimitException e) {
            waitcount++;
        }

    }

}
