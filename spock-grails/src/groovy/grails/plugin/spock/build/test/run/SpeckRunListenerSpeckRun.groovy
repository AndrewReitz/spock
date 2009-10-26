/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugin.spock.build.test.run

import junit.framework.AssertionFailedError

import grails.plugin.spock.build.test.adapter.TestCaseAdapter
import grails.plugin.spock.build.test.io.SystemOutAndErrSwapper

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest

import org.junit.runner.Description
import org.junit.runner.notification.Failure

class SpeckRunListenerSpeckRun {
    final name
    final protected reports
    final protected statusOut
    
    final protected junitTest
    final protected startTime

    final runCount = 0
    final errorCount = 0
    final failureCount = 0

    final protected outAndErrSwapper = new SystemOutAndErrSwapper()
        
    SpeckRunListenerSpeckRun(name, reportFactory, statusOut) {
      this.name = name
      this.reports = reportFactory.createReports(name)
      this.statusOut = statusOut
      
      this.junitTest = new JUnitTest(name)
    }
    
    void start() {
      outAndErrSwapper.swapIn()
      reports*.start(junitTest)
      
      statusOut.print("Running test ${name}...")
      startTime = System.currentTimeMillis()
    }
    
    void finish() {
      if (failureCount == 0) statusOut.println("PASSED")
      
      def (out,err) = outAndErrSwapper.swapOut()*.toString()
      junitTest.runTime = System.currentTimeMillis() - startTime
      junitTest.setCounts(runCount, failureCount, 0)
      reports*.end(junitTest, out, err)
    }
    
    void testStarted(Description description) {
      ++runCount
      [System.out, System.err]*.println("--Output from ${description.methodName}--")

      def testCase = new TestCaseAdapter(description)
      reports*.formatter*.startTest(testCase)
    }
    
    void testFailure(Failure failure) {
      if (++failureCount == 1) statusOut.println()
      statusOut.println("                    ${failure.description.methodName}...FAILED")

      def testCase = new TestCaseAdapter(failure.description)
      def exception = failure.exception

      def formatterMethod = (exception instanceof AssertionFailedError) ? 'addFailure' : 'addError'
      reports*.formatter*."$formatterMethod"(testCase, exception)
    }
    
    void testFinished(Description description) {
      def testCase = new TestCaseAdapter(description)
      reports*.formatter*.endTest(testCase)
    }
}