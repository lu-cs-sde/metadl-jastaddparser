/* Copyright (c) 2005-2015, The JastAdd Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jastadd.ant;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

/**
 * An Ant output formatter for improved console logging of test results.
 * @author Jesper Öqvist <jesper.oqvist@cs.lth.se>
 */
public class SimpleTestFormatter implements JUnitResultFormatter {

  private PrintStream out = System.out;
  private int numRun = 0;
  private int numFail = 0;
  private int numErr = 0;
  private final Set<Test> failed = new HashSet<Test>();
  private static final boolean verbose = System.getProperty("verbose", "").equals("true");

  @Override
  public void addError(Test test, Throwable error) {
    failed.add(test);
    numErr += 1;
    logResult(test, "ERR");
    out.println(error.getMessage());
  }

  @Override
  public void addFailure(Test test, AssertionFailedError failure) {
    failed.add(test);
    numFail += 1;
    logResult(test, "FAIL");
    out.println(failure.getMessage());
  }

  @Override
  public void endTest(Test test) {
    if (verbose && !failed.contains(test)) {
      logResult(test, "PASS");
    }
  }

  @Override
  public void startTest(Test test) {
    numRun += 1;
  }

  @Override
  public void endTestSuite(JUnitTest testSuite) throws BuildException {
    out.println(String.format("Test completed: %d runs, %d failures, %d errors",
          numRun, numFail, numErr));
    out.flush();
  }

  @Override
  public void setOutput(OutputStream out) {
    this.out = new PrintStream(out);
  }

  @Override
  public void setSystemError(String err) {
    // don't echo test error output
  }

  @Override
  public void setSystemOutput(String out) {
    // don't echo test output
  }

  @Override
  public void startTestSuite(JUnitTest testSuite) throws BuildException {
    numRun = 0;
    numFail = 0;
    numErr = 0;
  }

  private void logResult(Test test, String result) {
    out.println("[" + result + "] " + String.valueOf(test));
    out.flush();
  }
}
