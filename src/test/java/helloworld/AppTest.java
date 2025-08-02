package helloworld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class AppTest {
  @Test
  public void successfulResponse() {
    App app = new App();
    String result = app.handleRequest("World", null);
    assertEquals("Hello, World", result);
  }
  
  @Test
  public void testWithNullInput() {
    App app = new App();
    String result = app.handleRequest(null, null);
    assertEquals("Hello, null", result);
  }
}
