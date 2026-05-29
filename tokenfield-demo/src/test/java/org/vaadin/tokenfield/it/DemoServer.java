package org.vaadin.tokenfield.it;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Boots an in-process Jetty serving the demo webapp for IDE-driven IT runs,
 * where none of the {@code pre-integration-test} Maven wiring in {@code
 * tokenfield-demo/pom.xml} (port reservation, WAR deploy via {@code
 * jetty-maven-plugin}) has run.
 *
 * <p>Deliberately coded against the ~2013-vintage, unrelocated Jetty that
 * {@code gwt-dev.jar} bundles (this module's {@code provided}-scope {@code
 * vaadin-client-compiler} → {@code gwt-dev}, needed to compile the
 * widgetset) rather than adding a real, current Jetty as a test dependency.
 * A real Jetty's classes share {@code org.eclipse.jetty.*} package names
 * with gwt-dev's bundled copy; both would then sit on one flat test
 * classpath with no reliable way to prefer one over the other, and mixing
 * them was confirmed to throw {@code IncompatibleClassChangeError} out of
 * a bare {@code new ServerConnector(...)}. Using only what's already there
 * avoids the clash instead of routing around it.</p>
 *
 * <p>Only used as a fallback: {@link DemoOptions} prefers {@code it.baseUrl}
 * when Maven/failsafe has set it, so a plain {@code mvn verify -DskipITs=false}
 * or CI run keeps exercising the actual packaged WAR via {@code
 * jetty-maven-plugin} (a real, current Jetty in its own plugin classloader),
 * unaffected by this class.</p>
 */
final class DemoServer {

    private static String baseUrl;

    private DemoServer() {
    }

    /**
     * Returns the base URL of a running demo instance: the one failsafe
     * started (via {@code it.baseUrl}), or — if that system property is
     * unset, meaning this is an IDE run — one this class starts itself.
     */
    static synchronized String baseUrl() {
        String failsafeUrl = System.getProperty("it.baseUrl");
        if (isUsable(failsafeUrl)) {
            return failsafeUrl;
        }
        if (baseUrl == null) {
            baseUrl = start();
            System.err.println("[DemoServer] Embedded demo server ready at " + baseUrl);
        }
        return baseUrl;
    }

    /**
     * Whether {@code it.baseUrl} is a real, usable Maven/failsafe-supplied
     * URL rather than something that must fall through to the embedded
     * server instead of being handed to Playwright as-is. Catches two
     * distinct failure shapes seen in practice, both from IDEs that surface
     * this module's failsafe {@code <systemPropertyVariables>} config
     * (declared as {@code http://localhost:${it.http.port}}) as a JUnit run
     * configuration default without ever running the Maven build-helper
     * goal that defines {@code it.http.port}:
     * <ul>
     *   <li>blank — an empty {@code -Dit.baseUrl=}</li>
     *   <li>the unresolved property truncated to just "http://localhost",
     *       with no port at all — which Playwright accepts as a syntactically
     *       valid base URL, so {@code page.navigate("/")} resolves to a bare
     *       "http://localhost/" that nothing is listening on, instead of
     *       failing loudly.</li>
     * </ul>
     */
    private static boolean isUsable(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            return new URI(url).getPort() > 0;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static String start() {
        File webapp = findWebapp();
        // webapp = <moduleRoot>/src/main/webapp
        File moduleRoot = webapp.getParentFile().getParentFile().getParentFile();
        File widgetsets = new File(moduleRoot, "target/classes/VAADIN/widgetsets");
        if (!widgetsets.isDirectory()) {
            throw new IllegalStateException(
                    "Widgetset not built (expected at " + widgetsets + "). The embedded IT server needs "
                            + "it compiled first: run `./mvnw -pl tokenfield-demo -am package -DskipTests` "
                            + "once, then re-run the test.");
        }

        try {
            SelectChannelConnector connector = new SelectChannelConnector();
            connector.setPort(0);

            Server server = new Server();
            server.addConnector(connector);

            WebAppContext context = new WebAppContext();
            context.setContextPath("/");
            context.setResourceBase(webapp.getAbsolutePath());
            context.setDescriptor(new File(webapp, "WEB-INF/web.xml").getAbsolutePath());
            // Serve off this JVM's test classpath (target/classes, including
            // the widgetset, plus the tokenfield/vaadin-server/vaadin-themes
            // jars) rather than a WEB-INF/lib this module never assembles.
            context.setParentLoaderPriority(true);
            server.setHandler(context);

            server.start();

            // Jetty's threads are non-daemon; without this an IDE test run
            // would hang after the last test instead of exiting.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception e) {
                    // best-effort shutdown, JVM is exiting anyway
                }
            }));

            return "http://localhost:" + connector.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start the embedded demo server for IDE IT runs", e);
        }
    }

    /**
     * Locates {@code tokenfield-demo/src/main/webapp}, whether the test's
     * working directory is the module ({@code src/main/webapp}, the Maven
     * default) or the repo root ({@code tokenfield-demo/src/main/webapp},
     * common for a repo-wide IDE run configuration).
     */
    private static File findWebapp() {
        for (String candidate : new String[] {"src/main/webapp", "tokenfield-demo/src/main/webapp"}) {
            File webapp = Paths.get(candidate).toAbsolutePath().normalize().toFile();
            if (new File(webapp, "WEB-INF/web.xml").isFile()) {
                return webapp;
            }
        }
        Path cwd = Paths.get("").toAbsolutePath();
        throw new IllegalStateException(
                "Could not find tokenfield-demo/src/main/webapp from working directory " + cwd
                        + ". Set the IT run configuration's working directory to the tokenfield-demo module.");
    }
}
