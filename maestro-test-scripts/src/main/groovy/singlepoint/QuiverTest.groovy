package singlepoint
/*
 *  Copyright 2017 Otavio R. Piske <angusyoung@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


@GrabConfig(systemClassLoader=true)

@Grab(group='commons-cli', module='commons-cli', version='1.3.1')
@Grab(group='org.apache.commons', module='commons-lang3', version='3.6')
@Grab(group='org.msgpack', module='msgpack-core', version='0.8.3')

@GrabResolver(name='Eclipse', root='https://repo.eclipse.org/content/repositories/paho-releases/')
@Grab(group='org.eclipse.paho', module='org.eclipse.paho.client.mqttv3', version='1.1.1')

@GrabResolver(name='orpiske-bintray', root='https://dl.bintray.com/orpiske/libs-release')
@Grab(group='org.maestro', module='maestro-tests', version='1.3.0-SNAPSHOT')


import org.maestro.client.Maestro
import org.maestro.client.exchange.MaestroNoteProcessor
import org.maestro.client.notes.PingResponse
import org.maestro.client.notes.TestFailedNotification
import org.maestro.common.client.notes.MaestroNote
import org.maestro.tests.TestExecutor

/**
 * This test executes tests via Maestro Agent using Quiver (https://github.com/ssorj/quiver/)
 */
class QuiverExecutor implements TestExecutor {
    /**
     * The simple processor for Maestro responses
     */
    class QuiverTestProcessor extends MaestroNoteProcessor {
        private boolean successful = true

        @Override
        protected void processPingResponse(PingResponse note) {
            println  "Elapsed time from " + note.getName() + ": " + note.getElapsed() + " ms"
        }

        @Override
        protected void processNotifyFail(TestFailedNotification note) {
            println "Test failed on " + note.getName()
            successful = false;
        }

        boolean isSuccessful() {
            return successful
        }
    }

    private Maestro maestro
    private String brokerURL
    private String sourceURL
    private QuiverTestProcessor testProcessor = new QuiverTestProcessor();

    QuiverExecutor(Maestro maestro) {
        this.maestro = maestro
    }


    String getBrokerURL() {
        return brokerURL
    }

    void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL
    }

    String getSourceURL() {
        return sourceURL
    }

    void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL
    }


    /**
     * These two methods are NO-OP in this case because there are no multiple iterations,
     * therefore cool down period is not required/used
     */
    long getCoolDownPeriod() {
        return 0
    }

    void setCoolDownPeriod(long period) {
        // NO-OP
    }

    private void processReplies() {
        println "Collecting replies "
        List<MaestroNote> replies = maestro.collect(1000, 10)

        testProcessor.process(replies)
    }

    private void setTestParameters(String brokerURL) {
        println "Sending ping request"
        maestro.pingRequest()

        println "Setting broker"
        maestro.setBroker(brokerURL)

        println "Downloading Quiver extension point code from ${sourceURL}"
        maestro.sourceRequest(sourceURL, null)

        println "Setting the broker to ${brokerURL}"
        maestro.setBroker(brokerURL)
    }

    private void startServices() {
        maestro.userCommand(0, "rhea")
    }

    /**
     * Test execution logic
     * @return
     */
    boolean run() {
        setTestParameters(brokerURL)
        startServices()
        processReplies()

        println "Waiting a while for the Quiver test is running"
        Thread.sleep(65000)

        processReplies()
        return testProcessor.isSuccessful()
    }
}


/**
 * Get the maestro broker URL via the MAESTRO_BROKER environment variable
 */
maestroURL = System.getenv("MAESTRO_BROKER")
if (maestroURL == null) {
    println "Error: the maestro broker URL was not given"

    System.exit(1)
}

brokerURL = System.getenv("BROKER_URL")
if (brokerURL == null) {
    println "Error: the broker URL was not given"

    System.exit(1)
}

sourceURL = System.getenv("SOURCE_URL")
if (sourceURL == null) {
    println "Warning: the quiver URL was not given. Using default: https://github.com/maestro-performance/maestro-quiver-agent.git"

    sourceURL = "https://github.com/maestro-performance/maestro-quiver-agent.git"
}


println "Connecting to " + maestroURL
maestro = new Maestro(maestroURL)

QuiverExecutor executor = new QuiverExecutor(maestro)
executor.setBrokerURL(brokerURL)
executor.setSourceURL(sourceURL)

if (!executor.run()) {
    maestro.stop()

    System.exit(1)
}

maestro.stop()
System.exit(0)