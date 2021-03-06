/*
 * Copyright 2013 eXo Platform SAS
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

package juzu.impl.bridge.portlet;

import juzu.test.AbstractWebTestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class ConsumeEventTestCase extends AbstractWebTestCase {

  /** . */
  public static final LinkedList<String> sourceNames = new LinkedList<String>();

  /** . */
  public static final LinkedList<String> eventNames = new LinkedList<String>();

  /** . */
  public static final LinkedList<Object> eventPayloads = new LinkedList<Object>();


  @Deployment(testable = false)
  public static WebArchive createDeployment() {
    URL portletXML = Thread.currentThread().getContextClassLoader().getResource("bridge/portlet/event/consume/portlet.xml");
    return createPortletDeployment("bridge.portlet.event.consume", portletXML);
  }

  @ArquillianResource
  URL deploymentURL;

  @Drone
  WebDriver driver;

  @Test
  public void testFoo() throws Exception {
    assertEquals(Collections.emptyList(), eventNames);
    assertEquals(Collections.emptyList(), eventPayloads);
    driver.get(getPortletURL().toString());
    WebElement trigger = driver.findElement(By.id("trigger"));
    trigger.click();
    assertEquals(Arrays.asList("event1", "event1", "event2", "event3"), sourceNames);
    assertEquals(Arrays.asList("event1", "event1", "event2", "event3"), eventNames);
    assertEquals(Arrays.asList(3, "4", 5, "6"), eventPayloads);
  }
}
