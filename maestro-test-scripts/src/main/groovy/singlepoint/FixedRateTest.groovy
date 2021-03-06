/*
 * Copyright 2018 Otavio R. Piske <angusyoung@gmail.com>
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

package singlepoint

import org.maestro.client.Maestro
import org.maestro.reports.InspectorReportResolver
import org.maestro.reports.InterconnectInspectorReportResolver
import org.maestro.reports.ReportsDownloader
import org.maestro.tests.rate.FixedRateTestExecutor
import org.maestro.tests.rate.singlepoint.FixedRateTestProfile
import org.maestro.common.LogConfigurator
import org.maestro.common.duration.TestDurationBuilder

maestroURL = System.getenv("MAESTRO_BROKER")
if (maestroURL == null) {
    println "Error: the maestro broker was not given"

    System.exit(1)
}

brokerURL = System.getenv("SEND_RECEIVE_URL")
if (brokerURL == null) {
    println "Error: the send/receive URL was not given"

    System.exit(1)
}

messageSize = System.getenv("MESSAGE_SIZE")
if (messageSize == null) {
    println "Error: the message size was not given"

    System.exit(1)
}

duration = System.getenv("TEST_DURATION")
if (duration == null) {
    println "Error: the test duration was not given"

    System.exit(1)
}

rate = System.getenv("RATE")
if (rate == null) {
    println "Error: the test rate was not given"

    System.exit(1)
}

parallelCount = System.getenv("PARALLEL_COUNT")
if (parallelCount == null) {
    println "Error: the test parallel count was not given"

    System.exit(1)
}

logLevel = System.getenv("LOG_LEVEL")
LogConfigurator.configureLogLevel(logLevel)

managementInterface = System.getenv("MANAGEMENT_INTERFACE");
inspectorName = System.getenv("INSPECTOR_NAME");

println "Connecting to " + maestroURL
maestro = new Maestro(maestroURL)

ReportsDownloader reportsDownloader = new ReportsDownloader(args[0])

FixedRateTestProfile testProfile = new FixedRateTestProfile()

testProfile.setBrokerURL(brokerURL)
testProfile.setDuration(TestDurationBuilder.build(duration))
testProfile.setMessageSize(messageSize)
testProfile.setMaximumLatency(20000)
testProfile.setRate(Integer.parseInt(rate))
testProfile.setParallelCount(Integer.parseInt(parallelCount))


if (managementInterface != null) {
    if (inspectorName != null) {
        testProfile.setInspectorName(inspectorName)
        testProfile.setManagementInterface(managementInterface)

        if(inspectorName == "InterconnectInspector") {
            reportsDownloader.addReportResolver("inspector", new InterconnectInspectorReportResolver())
        }
        else {
            reportsDownloader.addReportResolver("inspector", new InspectorReportResolver())
        }
    }
    else {
        println "A management interface was provided by no inspector name was given. Ignoring ..."
    }
}
else {
    println "No management interface address was given"
}


FixedRateTestExecutor testExecutor = new FixedRateTestExecutor(maestro, reportsDownloader, testProfile)
if (!testExecutor.run()) {
    maestro.stop()

    System.exit(1)
}

maestro.stop()
System.exit(0)


