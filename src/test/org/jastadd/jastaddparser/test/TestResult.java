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
package org.jastadd.jastaddparser.test;

/**
 * Possible results from a JastAddParser test.
 */
public enum TestResult {

  /**
   * JastAddParser successfully processes the input file(s)
   */
  JAP_PASS,

  /**
   * JastAddParser fails to process the input and terminates with specific
   * error output
   */
  JAP_ERR_OUTPUT,

  /**
   * JastAddParser successfully processes the input and the resulting file
   * matches the expected file. Standard output is ignored.
   */
  JAP_OUTPUT_PASS,

  /**
   * The generated parser successfully processes the parser input and outputs
   * nothing
   */
  EXEC_PASS,

  /**
   * The generated parser successfully processes the parser input and produces
   * the expected output
   */
  EXEC_OUTPUT_PASS,

  /**
   * Used internally by the test runner. The test will continue to the next
   * step if the process terminates normally, and fail otherwise.
   */
  STEP_PASS
}
