/*
 * Copyright 2020 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.web;

import com.google.common.net.MediaType;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;

import java.io.File;  // Added for insecure file handling scenario
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tests {@link DecodeServlet}.
 */
public final class DecodeServletTestCase extends Assert {

  private static final String IMAGE_DATA_URI =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACEAAAAhAQAAAAB/n//CAAAAkklEQVR42mP4DwQNDJjkB4" +
      "E77A0M369N/d7A8CV6rjiQjPMFkWG1QPL7RVGg%2BAfREKCa/5/vA9V/nFSQ3sDwb7/KdiDJqX4dSH4pXN/A8DfyDVD2" +
      "988HQPUfPVaqA0XKz%2BgD9bIk1AP1fgwvB7KlS9VBdqXbA82PT9AH2fiaH2SXGdDM71fDgeIfhIvKsbkTTAIAKYVr0N" +
      "z5IloAAAAASUVORK5CYII=";

  @Test
  public void testDataURI() throws Exception {
    MockServletConfig config = new MockServletConfig();
    config.addInitParameter("maxAccessPerTime", "100");
    config.addInitParameter("accessTimeSec", "100");
    config.addInitParameter("maxEntries", "100");

    DecodeServlet servlet = new DecodeServlet();
    servlet.init(config);

    MockHttpServletRequest request = new MockHttpServletRequest();
    Map<String, String> params = new HashMap<>();
    params.put("u", IMAGE_DATA_URI);
    params.put("full", "false");
    request.setParameters(params);

    MockHttpServletResponse response = new MockHttpServletResponse();

    servlet.doGet(request, response);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertEquals(MediaType.PLAIN_TEXT_UTF_8.toString(), response.getContentType());
    assertEquals("--error-correction_level\n", response.getContentAsString());

    servlet.destroy();
  }

  // Scenario 1: Insecure File Handling (Arbitrary File Write)
  @Test
  public void testInsecureFileWrite() {
    try {
      String fileName = "output.txt";  // File name could come from untrusted input
      File file = new File("/tmp/" + fileName);

      // Vulnerable to arbitrary file write if fileName input isn't validated
      FileOutputStream fos = new FileOutputStream(file);
      fos.write("Potentially sensitive data".getBytes());
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Scenario 2: Missing Input Validation (Data URI Input)
  @Test
  public void testMissingInputValidation() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    // Malicious input simulation: A large data URI that could lead to DoS or other issues
    String largeDataUri = "data:image/png;base64," + "A".repeat(1000000);
    request.setParameter("u", largeDataUri);

    MockHttpServletResponse response = new MockHttpServletResponse();
    DecodeServlet servlet = new DecodeServlet();

    try {
      servlet.doGet(request, response);
      assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
