package com.vaadin.integration.eclipse.templates.v7;

/*
 * JET GENERATED do not edit!
 * The source templates are in the templates folder (note: not package).
 *
 * The JET source templates can be edited. They are then transformed into java
 * template classes by the JET plugin. To use the generated java templates, no
 * dependencies are required.
 */
public class TestTemplate {
  protected static String nl;
  public static synchronized TestTemplate create(String lineSeparator)
  {
    nl = lineSeparator;
    TestTemplate result = new TestTemplate();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "package ";
  protected final String TEXT_2 = ";" + NL + "" + NL + "import com.vaadin.testbench.ScreenshotOnFailureRule;" + NL + "import com.vaadin.testbench.TestBenchTestCase;" + NL + "import com.vaadin.testbench.elements.ButtonElement;" + NL + "import com.vaadin.testbench.elements.LabelElement;" + NL + "" + NL + "import org.junit.Before;" + NL + "import org.junit.Rule;" + NL + "import org.junit.Test;" + NL + "import org.openqa.selenium.firefox.FirefoxDriver;" + NL + "" + NL + "import java.util.List;" + NL + "" + NL + "import static org.junit.Assert.assertEquals;" + NL + "import static org.junit.Assert.assertFalse;" + NL + "" + NL + "/**" + NL + " * This class contains JUnit tests, which are run using Vaadin TestBench 4." + NL + " *" + NL + " * To run this, first get an evaluation license from" + NL + " * https://vaadin.com/addon/vaadin-testbench and follow the instructions at" + NL + " * https://vaadin.com/directory/help/installing-cval-license to install it." + NL + " *" + NL + " * Once the license is installed, you can run this class as a JUnit test." + NL + " */" + NL + "public class ";
  protected final String TEXT_3 = " extends TestBenchTestCase {" + NL + "    @Rule" + NL + "    public ScreenshotOnFailureRule screenshotOnFailureRule =" + NL + "            new ScreenshotOnFailureRule(this, true);" + NL + "" + NL + "    @Before" + NL + "    public void setUp() throws Exception {" + NL + "        setDriver(new FirefoxDriver()); // Firefox" + NL + "" + NL + "        // To use Chrome, first install chromedriver.exe from" + NL + "        // http://chromedriver.storage.googleapis.com/index.html" + NL + "        // on your system path (e.g. C:\\Windows\\System32\\)" + NL + "        //   setDriver(new ChromeDriver()); // Chrome" + NL + "" + NL + "        // To use Internet Explorer, first install iedriverserver.exe from" + NL + "        // http://selenium-release.storage.googleapis.com/index.html?path=2.43/" + NL + "        // on your system path (e.g. C:\\Windows\\System32\\)" + NL + "        //   setDriver(new InternetExplorerDriver()); // IE" + NL + "" + NL + "        // To test headlessly (without a browser), first install phantomjs.exe" + NL + "        // from http://phantomjs.org/download.html on your system path" + NL + "        // (e.g. C:\\Windows\\System32\\)" + NL + "        //   setDriver(new PhantomJSDriver()); // PhantomJS headless browser" + NL + "    }" + NL + "" + NL + "    /**" + NL + "     * Opens the URL where the application is deployed." + NL + "     */" + NL + "    private void openTestUrl() {" + NL + "        getDriver().get(\"http://localhost:8080/";
  protected final String TEXT_4 = "\");" + NL + "    }" + NL + "" + NL + "    @Test" + NL + "    public void testClickButton() throws Exception {" + NL + "        openTestUrl();" + NL + "" + NL + "        // At first there should be no labels" + NL + "        assertFalse($(LabelElement.class).exists());" + NL + "" + NL + "        // Click the button" + NL + "        ButtonElement clickMeButton = $(ButtonElement.class)." + NL + "                caption(\"Click Me\").first();" + NL + "        clickMeButton.click();" + NL + "" + NL + "        // There should now be one label" + NL + "        assertEquals(1, $(LabelElement.class).all().size());" + NL + "        // ... with the specified text" + NL + "        assertEquals(\"Thank you for clicking\"," + NL + "                $(LabelElement.class).first().getText());" + NL + "" + NL + "        // Click the button again" + NL + "        clickMeButton.click();" + NL + "" + NL + "        // There should now be two labels" + NL + "        List<LabelElement> allLabels = $(LabelElement.class).all();" + NL + "        assertEquals(2, allLabels.size());" + NL + "        // ... and the last label should have the correct text" + NL + "        LabelElement lastLabel = allLabels.get(1);" + NL + "        assertEquals(\"Thank you for clicking\", lastLabel.getText());" + NL + "    }" + NL + "}";

    // UIName is a name that is used for generating a URL for the web browser in the test.
    // It can be different from the UI class name: the class name usually ends with 'UI',
    // while UIName does not.
    public String generate(String applicationPackage, String uiName, String testClassName)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(TEXT_1);
    stringBuffer.append( applicationPackage );
    stringBuffer.append(TEXT_2);
    stringBuffer.append( testClassName );
    stringBuffer.append(TEXT_3);
    stringBuffer.append( uiName );
    stringBuffer.append(TEXT_4);
    return stringBuffer.toString();
  }
}