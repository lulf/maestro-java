/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.orpiske.mpt.maestro.worker.base;

import net.orpiske.mpt.common.worker.MaestroReceiverWorker;
import net.orpiske.mpt.common.worker.MaestroWorker;
import net.orpiske.mpt.common.worker.WorkerOptions;
import net.orpiske.mpt.common.worker.WorkerStateInfo;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.SingleWriterRecorder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WorkerLatencyWriterSanityTest {

    private static abstract class DummyWorker implements MaestroWorker {

        long startedEpochMillis = Long.MIN_VALUE;

        @Override
        public boolean isRunning() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setWorkerOptions(WorkerOptions workerOptions) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkerStateInfo getWorkerState() {
            return null;
        }

        @Override
        public void start() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stop() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void halt() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void run() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long startedEpochMillis() {
            return startedEpochMillis;
        }

        @Override
        public long messageCount() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class DummyReceiverWorker extends DummyWorker implements MaestroReceiverWorker {

        final SingleWriterRecorder recorder = new SingleWriterRecorder(TimeUnit.HOURS.toMillis(1), 3);

        @Override
        public Histogram takeLatenciesSnapshot(Histogram intervalHistogram) {
            return recorder.getIntervalHistogram(intervalHistogram);
        }
    }

    @Rule
    public TemporaryFolder tempTestFolder = new TemporaryFolder();


    @Test(timeout = 120_000L)
    public void shouldWriteLatenciesLeavingTheReportNotEmpty() throws IOException, InterruptedException {
        final int receivers = 10;
        final int events = 10;
        //there are 1 producer + 1 consumer each one emitting events
        final int totalEvents = events * receivers;
        final CountDownLatch eventsProcessed = new CountDownLatch(totalEvents);
        //use 1 capacity and wait until each message has been processed
        final DummyReceiverWorker[] dummyReceiverWorkers = new DummyReceiverWorker[receivers];
        final long globalStart = System.currentTimeMillis();
        final long fixedLatency = 100;
        for (int i = 0; i < receivers; i++) {
            dummyReceiverWorkers[i] = new DummyReceiverWorker();
            dummyReceiverWorkers[i].startedEpochMillis = globalStart;
        }
        final Thread roundRobinReceivers = new Thread(() -> {
            for (int i = 0; i < events; i++) {
                for (DummyReceiverWorker worker : dummyReceiverWorkers) {
                    worker.recorder.recordValue(fixedLatency);
                    eventsProcessed.countDown();
                }
            }
        });
        roundRobinReceivers.start();
        final File reportFolder = tempTestFolder.newFolder("report");
        final WorkerLatencyWriter latencyWriter = new WorkerLatencyWriter(reportFolder, Arrays.asList(dummyReceiverWorkers));
        final Thread writerThread = new Thread(latencyWriter);
        writerThread.setDaemon(true);
        writerThread.start();
        eventsProcessed.await();
        roundRobinReceivers.join();
        writerThread.interrupt();
        writerThread.join();
        final String latencyFileName = "receiverd-latency.hdr";
        final String[] reports = reportFolder.list((dir, name) -> name.equals(latencyFileName));
        Assert.assertArrayEquals(new String[]{latencyFileName}, reports);
        final File reportFile = new File(reportFolder, reports[0]);
        Assert.assertTrue(reportFile.length() > 0);
    }
}
