package com.realtime.demo.disruptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class Diamond {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Disruptor<LongEvent> disruptor =
            new Disruptor<>(
                LongEvent::new,
                1024,
                executor,
                ProducerType.SINGLE,
                new SleepingWaitStrategy());

        //register five consumers and a final conclude
        disruptor
            .handleEventsWith(
                new Consumer(1),
                new Consumer(2),
                new Consumer(3),
                new Consumer(4),
                new Consumer(5))
            .then(new Conclude());

        disruptor.start();

        for (int i = 0; i < 10; i++) {
            disruptor.publishEvent((event, sequence, newValue) -> event.setValue(1), i);
        }

        disruptor.shutdown();
        executor.shutdown();
    }

    public static class Consumer implements EventHandler<LongEvent> {
        private int i;
        public Consumer(int i) {
            this.i = i;
        }

        @Override
        public void onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println("Consumer: " + i + " sequence:" + sequence);
            event.setValue(event.getValue() + 1);
            System.out.println(event.getTag());
        }
    }

    public static class Conclude implements EventHandler<LongEvent> {
        @Override
        public void onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println("Conclude: " + event.getValue());
        }
    }

    public static class LongEvent {
        private long value;
        private volatile String tag = "";

        public void setTag(String tag) {
            this.tag += tag;
        }
        public String getTag() {
            return tag;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public long getValue() {
            return this.value;
        }
    }
}
