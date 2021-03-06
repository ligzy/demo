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

import java.util.concurrent.atomic.AtomicBoolean;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.DataProvider;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.Sequencer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.TimeoutHandler;

/**
 * 可以往回倒的事件处理器
 * Title:        United Risk Management System
 * Description:  
 * Copyright:    Copy Right (c) 2015—2016
 * Company:      ROOTNET
 * @author       Jeremy Li
 * @date         2016年2月25日
 * @version      1.0
 */
public final class BatchEventRewindableProcessor<T> implements EventProcessor {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExceptionHandler<? super T> exceptionHandler = new FatalExceptionHandler();
    private final DataProvider<T> dataProvider;
    private final SequenceBarrier sequenceBarrier;

    private SequenceBarrier sequenceBarrierComfirm = null;

    private final EventHandler<? super T> eventHandler;
    private final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    private long rewindPosition = sequence.get();

    private final TimeoutHandler timeoutHandler;

    /**
     * Construct a {@link EventProcessor} that will automatically track the
     * progress by updating its sequence when the
     * {@link EventHandler#onEvent(Object, long, boolean)} method returns.
     *
     * @param dataProvider
     *            to which events are published.
     * @param sequenceBarrier
     *            on which it is waiting.
     * @param eventHandler
     *            is the delegate to which events are dispatched.
     */
    public BatchEventRewindableProcessor(final DataProvider<T> dataProvider, final SequenceBarrier sequenceBarrier, final EventHandler<? super T> eventHandler) {
        this.dataProvider = dataProvider;
        this.sequenceBarrier = sequenceBarrier;
        this.eventHandler = eventHandler;
        if (eventHandler instanceof SequenceReportingEventHandler) {
            ((SequenceReportingEventHandler<?>) eventHandler).setSequenceCallback(sequence);
        }

        timeoutHandler = (eventHandler instanceof TimeoutHandler) ? (TimeoutHandler) eventHandler : null;
    }

    public void setSequenceBarrierComfirm(SequenceBarrier sequenceComfirm) {
        this.sequenceBarrierComfirm = sequenceComfirm;
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void halt() {
        running.set(false);
        sequenceBarrier.alert();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Set a new {@link ExceptionHandler} for handling exceptions propagated out
     * of the {@link BatchEventRewindableProcessor}
     *
     * @param exceptionHandler
     *            to replace the existing exceptionHandler.
     */
    public void setExceptionHandler(final ExceptionHandler<? super T> exceptionHandler) {
        if (null == exceptionHandler) {
            throw new NullPointerException();
        }

        this.exceptionHandler = exceptionHandler;
    }

    /**
     * It is ok to have another thread rerun this method after a halt().
     *
     * @throws IllegalStateException
     *             if this object instance is already running in a thread
     */
    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Thread is already running");
        }
        sequenceBarrier.clearAlert();

        notifyStart();

        T event = null;
        long nextSequence = sequence.get() + 1L;
        try {
            while (true) {
                try {

                    //还需要监控重发的ID号才行。
                    final long availableSequence = sequenceBarrier.waitFor(nextSequence);

                    while (nextSequence <= availableSequence) {

                        event = dataProvider.get(nextSequence);
                        eventHandler.onEvent(event, nextSequence, nextSequence == availableSequence);

                        sequence.set(nextSequence);

                        // 1.event.wait();
                        // 2.
                        if (sequenceBarrierComfirm != null) {
                            long confirmSequence = -1L;
                            do {
                                confirmSequence = sequenceBarrierComfirm.waitFor(nextSequence);
                            } while (confirmSequence < nextSequence);
                        }

                        nextSequence++;
                    }

                    // sequence.set(availableSequence);

                } catch (final TimeoutException e) {
                    notifyTimeout(sequence.get());
                } catch (final AlertException ex) {
                    if (!running.get()) {
                        break;
                    }

                    //重传序号
                    {
                        nextSequence = sequence.get() + 1L;
                        sequenceBarrier.clearAlert();
                    }

                } catch (final Throwable ex) {
                    exceptionHandler.handleEventException(ex, nextSequence, event);
                    sequence.set(nextSequence);
                    nextSequence++;
                }
            }
        } finally {
            notifyShutdown();
            running.set(false);
        }
    }

    /**
     * 往回倒多少个。
     * @param n
     */
    public void rewind(long n) {
        setposition(sequence.get() - n);
    }

    /**
     * 防止重复多次设置。
     * @param n
     */
    public void setposition(long n) {
        rewindPosition = Math.max(n, rewindPosition);
        sequence.set(Math.max(rewindPosition - 1, -1));
        sequenceBarrier.alert();
    }

    private void notifyTimeout(final long availableSequence) {
        try {
            if (timeoutHandler != null) {
                timeoutHandler.onTimeout(availableSequence);
            }
        } catch (Throwable e) {
            exceptionHandler.handleEventException(e, availableSequence, null);
        }
    }

    /**
     * Notifies the EventHandler when this processor is starting up
     */
    private void notifyStart() {
        if (eventHandler instanceof LifecycleAware) {
            try {
                ((LifecycleAware) eventHandler).onStart();
            } catch (final Throwable ex) {
                exceptionHandler.handleOnStartException(ex);
            }
        }
    }

    /**
     * Notifies the EventHandler immediately prior to this processor shutting
     * down
     */
    private void notifyShutdown() {
        if (eventHandler instanceof LifecycleAware) {
            try {
                ((LifecycleAware) eventHandler).onShutdown();
            } catch (final Throwable ex) {
                exceptionHandler.handleOnShutdownException(ex);
            }
        }
    }
}