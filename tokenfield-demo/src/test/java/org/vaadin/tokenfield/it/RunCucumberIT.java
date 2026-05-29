package org.vaadin.tokenfield.it;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.JUNIT_PLATFORM_NAMING_STRATEGY_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PUBLISH_QUIET_PROPERTY_NAME;

/**
 * Entry point for the BDD browser suite: runs every scenario in {@code
 * src/test/resources/features/token_field.feature} against the demo, through
 * the same Jetty/Playwright harness the deleted {@code *IT.java} classes
 * used.
 *
 * <p>This class — not the {@code .feature} file — is what {@code
 * maven-failsafe-plugin} actually discovers: failsafe matches {@code
 * **&#47;*IT.java} class names on the test classpath, and has no notion of a
 * {@code .feature} file. {@link Suite @Suite} + {@link IncludeEngines
 * @IncludeEngines("cucumber")} hands this one discovered class to the
 * Cucumber JUnit Platform engine, which then discovers and runs the
 * scenarios themselves.
 *
 * <p>There is deliberately no {@code AbstractDemoIT}/{@code DemoOptions}
 * equivalent here: {@code @UsePlaywright} is a JUnit <b>Jupiter</b> extension,
 * and Cucumber scenarios run on the JUnit <b>Platform</b>'s Cucumber engine,
 * not Jupiter — Jupiter extensions never see them. {@link PlaywrightHooks}
 * owns the browser/page lifecycle instead, via plain Cucumber {@code
 * @Before}/{@code @After} hooks, with state shared into the step classes
 * through the {@code cucumber-picocontainer}-managed {@link DemoWorld}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.vaadin.tokenfield.it")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty,summary,html:target/cucumber/report.html")
@ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, value = "true")
@ConfigurationParameter(key = JUNIT_PLATFORM_NAMING_STRATEGY_PROPERTY_NAME, value = "long")
public class RunCucumberIT {
}
