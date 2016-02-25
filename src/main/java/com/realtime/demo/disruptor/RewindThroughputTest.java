/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.realtime.demo.disruptor;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;
import static com.lmax.disruptor.support.PerfTestUtil.failIfNot;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.AbstractPerfTestDisruptor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.Sequencer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.support.FizzBuzzEvent;
import com.lmax.disruptor.support.FizzBuzzEventHandler;
import com.lmax.disruptor.support.FizzBuzzStep;
import com.lmax.disruptor.util.DaemonThreadFactory;

public final class RewindThroughputTest extends AbstractPerfTestDisruptor {
    private static final int NUM_EVENT_PROCESSORS = 3;
    private static final int BUFFER_SIZE = 16;
    private static final long ITERATIONS = 1024L;
    private final ExecutorService executor =
        Executors.newFixedThreadPool(NUM_EVENT_PROCESSORS, DaemonThreadFactory.INSTANCE);

    private final long expectedResult;

    {
        long temp = 0L;

        for (long i = 0; i < ITERATIONS; i++) {
            boolean fizz = 0 == (i % 3L);
            boolean buzz = 0 == (i % 5L);

            if (fizz && buzz) {
                ++temp;
            }
        }

        expectedResult = temp;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private final RingBuffer<FizzBuzzEvent> ringBuffer =
        createSingleProducer(FizzBuzzEvent.EVENT_FACTORY, BUFFER_SIZE, new YieldingWaitStrategy());

    private final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

    private final FizzBuzzEventHandler fizzHandler = new FizzBuzzEventHandler(FizzBuzzStep.FIZZ);

    private final BatchEventConfirmProcessor<FizzBuzzEvent> batchProcessorFizz =
        new BatchEventConfirmProcessor<FizzBuzzEvent>(ringBuffer, sequenceBarrier, fizzHandler);
    //BatchEventConfirmProcessor
    //BatchEventProcessor
    private final FizzBuzzEventHandler buzzHandler = new FizzBuzzEventHandler(FizzBuzzStep.BUZZ);
    private final BatchEventConfirmProcessor<FizzBuzzEvent> batchProcessorBuzz =
        new BatchEventConfirmProcessor<FizzBuzzEvent>(ringBuffer, sequenceBarrier, buzzHandler);

    // merge
    private final SequenceBarrier sequenceBarrierFizzBuzz =
        ringBuffer.newBarrier(batchProcessorFizz.getSequence(), batchProcessorBuzz.getSequence());

    private final FizzBuzzEventHandler fizzBuzzHandler =
        new FizzBuzzEventHandler(FizzBuzzStep.FIZZ_BUZZ);
    private final BatchEventRewindableProcessor<FizzBuzzEvent> batchProcessorFizzBuzz =
        new BatchEventRewindableProcessor<FizzBuzzEvent>(
            ringBuffer,
            sequenceBarrierFizzBuzz,
            fizzBuzzHandler);

    // 用于确认
    private final SequenceBarrier sequenceBarrierConfirm =
        ringBuffer.newBarrier(batchProcessorFizzBuzz.getSequence());

    {
        ringBuffer.addGatingSequences(batchProcessorFizzBuzz.getSequence());

        batchProcessorFizz.setSequenceBarrierComfirm(sequenceBarrierConfirm);
        batchProcessorBuzz.setSequenceBarrierComfirm(sequenceBarrierConfirm);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected int getRequiredProcessorCount() {
        return 3;
    }

    @Override
    protected long runDisruptorPass() throws Exception {
        System.out.println("runDisruptorPass");
        CountDownLatch latch = new CountDownLatch(1);
        fizzBuzzHandler.reset(latch, batchProcessorFizzBuzz.getSequence().get() + ITERATIONS);

        executor.submit(batchProcessorFizz);
        executor.submit(batchProcessorBuzz);
        executor.submit(batchProcessorFizzBuzz);

        long start = System.currentTimeMillis();

        new Thread() {
            @Override
            public void run() {
                int i = 0;
                long pos = -1;
                while (i < ITERATIONS) {
                    System.out.println(
                        "\r\nCapacity:"
                            + ringBuffer.remainingCapacity()
                            + ">8:"
                            + ringBuffer.hasAvailableCapacity(8));

                    if (ringBuffer.hasAvailableCapacity(8)) {
                        long next = ringBuffer.next();
                        FizzBuzzEvent event = ringBuffer.get(next);
                        event.setValue(i++);
                        ringBuffer.publish(next);
                        long current = ringBuffer.getCursor();
                        event = ringBuffer.get(current);
                        System.out.println(
                            "Published event current:" + current + " ," + event.getValue());
                    }

                    long current = ringBuffer.getCursor();

                    FizzBuzzEvent event = ringBuffer.get(current);
                    System.out.println("ringBuffer event " + (current) + "," + event.getValue());

                    current = batchProcessorFizzBuzz.getSequence().get();
                    event = ringBuffer.get(current);
                    System.out
                        .println("ringBuffer event FizzBuzz " + (current) + " " + event.getValue());

                    long temppos = batchProcessorFizzBuzz.getSequence().get();
                    if (temppos > pos && temppos > 0 && temppos % 10 == 0) {
                        pos = temppos;
                        System.out.println("rewind 5 from:" + pos);
                        batchProcessorFizzBuzz.rewind(5);

                    }
                    current = batchProcessorFizzBuzz.getSequence().get();
                    event = ringBuffer.get(current);
                    System.out
                        .println("ringBuffer event FizzBuzz " + (current) + " " + event.getValue());

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        latch.await();
        long opsPerSecond = (ITERATIONS * 1000L) / (System.currentTimeMillis() - start);

        System.out.println("ITERATIONS ok " + ITERATIONS);

        batchProcessorFizz.halt();
        batchProcessorBuzz.halt();
        batchProcessorFizzBuzz.halt();

        failIfNot(expectedResult, fizzBuzzHandler.getFizzBuzzCounter());
        System.out.println("opsPerSecond : " + opsPerSecond);
        return opsPerSecond;
    }

    public static void main(String[] args) throws Exception {
        new RewindThroughputTest().testImplementations();
    }
}
