[![Maven](https://img.shields.io/maven-central/v/org.primefaces/primefaces.svg)](https://repo.maven.apache.org/maven2/org/primefaces/primefaces-selenium/)

# primefaces-selenium

PrimeFaces testing support based on JUnit5, Selenium and the concept of page objects / fragments. Heavily inspired by Arquillian Graphene.  
It also supports JUnit5 parallel test execution to speed up tests.

PrimeFaces-Selenium provides a hook-in to either startup a local server (`deployment.adapter`),
or use a remote adress (`deployment.baseUrl`).

It also manage and download the Selenium WebDriver. Currently supported: `firefox`, `chrome` and `safari`  
You can also manage it by yourself via `webdriver.adapter`.

## Dependencies

```xml
<dependency>
    <groupId>org.primefaces</groupId>
    <artifactId>primefaces-selenium-core</artifactId>
    <version>11.0.0-RC1</version>
</dependency>
<dependency>
    <groupId>org.primefaces</groupId>
    <artifactId>primefaces-selenium-components</artifactId>
    <version>11.0.0-RC1</version>
</dependency>
```

## Configuration

PrimeFaces-Selenium can be configuredy by providing a `/primefaces-selenium/config.properties`.
A sample `DeploymentAdapter` for Tomcat can be found here: [Tomcat Adapter](https://github.com/primefaces/primefaces/blob/master/primefaces-integration-tests/src/test/java/org/primefaces/integrationtests/TomcatDeploymentAdapter.java)

Properties:
|       property name      |   type  | default |                 description                 |
|:------------------------:|:-------:|---------|:-------------------------------------------:|
|   deployment.baseUrl     | String  |         | the base URL                                |
|   deployment.adapter     | org.primefaces.extensions.selenium.spi.DeploymentAdapter |      | Adapter implementation to start/stop a container |
|    webdriver.adapter     | org.primefaces.extensions.selenium.spi.WebDriverAdapter  |      | Adapter implementation to create a WebDriver  |
|    webdriver.browser     | String  |         |       firefox / chrome / safari             |
|   webdriver.headless     | boolean | false   |    if browser should be openend headless    |
|   webdriver.version      | String  | newest  |  the webdriver version which should be used |
|       timeout.gui        |   int   | 2       |       GUI timeout for waits in seconds      |
|       timeout.ajax       |   int   | 10      |      AJAX timeout for guards in seconds     |
|       timeout.http       |   int   | 10      |      HTTP timeout for guards in seconds     |
|   timeout.documentLoad   |   int   | 15      |       Document load timeout in seconds      |
|    timeout.fileUpload    |   int   | 20      |         FileUpload timeout in seconds       |
|   onloadScripts.adapter  | org.primefaces.extensions.selenium.spi.OnloadScriptsAdapter | | Adapter implementation to provide custom onload scripts  |
|    disableAnimations     | boolean | true    | If animations should be disabled for tests  |
|  scrollElementIntoView   | String  |         | Scroll the element to be clicked into view via the configured #scrollIntoView option. Valid options are a boolean or object |

## Status

Currently, only the following components are implemented (partially):

#### HTML

- Link

#### JSF / PrimeFaces

- AccordionPanel
- AutoComplete
- Calendar
- CascadeSelect
- Chips
- CommandButton
- CommandLink
- ConfirmDialog
- ConfirmPopup
- ~~DataList~~ (use DataView)
- DataTable
- DataView
- DatePicker
- Dialog
- FileUpload
- InputMask
- InputNumber
- ~~InputSwitch~~ (use ToggleSwitch)
- InputText
- InputTextarea
- Messages
- OutputLabel
- OverlayPanel
- Panel
- Password
- Rating
- Schedule
- SelectBooleanCheckbox
- SelectBooleanButton
- SelectManyCheckbox
- SelectManyMenu
- SelectOneButton
- SelectOneMenu
- SelectOneRadio
- Slider
- Spinner
- TabView
- TextEditor
- Timeline
- ToggleSwitch
- Tree
- TreeTable
- TriStateCheckbox

## Usage

Example view:

```java
import org.openqa.selenium.support.FindBy;
import org.primefaces.extensions.selenium.AbstractPrimePage;
import org.primefaces.extensions.selenium.component.InputText;
import org.primefaces.extensions.selenium.component.SelectOneMenu;

public class IndexPage extends AbstractPrimePage {

    @FindBy(id = "form:manufacturer")
    private SelectOneMenu manufacturer;

    @FindBy(id = "form:car")
    private InputText car;

    public SelectOneMenu getManufacturer() {
        return manufacturer;
    }

    public InputText getCar() {
        return car;
    }

    @Override
    public String getLocation() {
        return "index.xhtml";
    }
}
```

Example test:

```java
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.primefaces.extensions.selenium.AbstractPrimePageTest;

public class IndexPageTest extends AbstractPrimePageTest {

    @Inject
    private AnotherPage another;

    @Test
    public void myFirstTest(IndexPage index) throws InterruptedException {
        // right page?
        Assertions.assertTrue(index.isAt());
        assertNotDisplayed(index.getCar());

        // just to follow the browser with a human eye for the showcase :D - not need in your real tests
        Thread.sleep(2000);

        // select manufacturer
        assertDisplayed(index.getManufacturer());
        index.getManufacturer().select("BMW");
        Assertions.assertTrue(index.getManufacturer().isSelected("BMW"));

        // just to follow the browser with a human eye for the showcase :D - not need in your real tests
        Thread.sleep(2000);

        // type car
        assertDisplayed(index.getCar());
        index.getCar().setValue("E30 M3");

        // just to follow the browser with a human eye for the showcase :D - not need in your real tests
        Thread.sleep(2000);

        another.goTo();

        ...
    }
}
```

Creating component without annotations:

```java
InputText input = PrimeSelenium.createFragment(InputText.class, By.id("test"));
```

## Build & Run

- Build by source `mvn clean install`
