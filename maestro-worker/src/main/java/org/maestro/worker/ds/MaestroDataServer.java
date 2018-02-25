package org.maestro.worker.ds;

import org.apache.commons.configuration.AbstractConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.resource.Resource;
import org.maestro.common.ConfigurationWrapper;
import org.maestro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MaestroDataServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MaestroDataServer.class);

    private static final String WORKER_LOGS_CONTEXT = "/logs/worker";
    private static final String TEST_LOGS_CONTEXT = "/logs/test";

    private static final int DEFAULT_DS_PORT = 0;

    private Server server;
    private File logDir;
    private int dataServerPort;

    public MaestroDataServer(final File logDir) {
        this.logDir = logDir;
    }

    private void runServerInt() throws Exception {
        AbstractConfiguration config = ConfigurationWrapper.getConfig();

        dataServerPort = config.getInteger("data.server.port", DEFAULT_DS_PORT);

        server = new Server(dataServerPort);

        ResourceHandler logResourceHandler = new ResourceHandler();
        logResourceHandler.setStylesheet(this.getClass().getResource("jetty-dir.css").getPath());


        // Serve the tests logs on /logs/tests
        ContextHandler context0 = new ContextHandler();
        context0.setContextPath(TEST_LOGS_CONTEXT);
        context0.addAliasCheck(new ContextHandler.ApproveAliases());

        context0.setBaseResource(Resource.newResource(logDir));
        context0.setHandler(logResourceHandler);

        logger.debug("Serving files from {} on /logs/tests", logDir.getPath());

        ResourceHandler workerResourceHandler = new ResourceHandler();
        workerResourceHandler.setStylesheet(this.getClass().getResource("jetty-dir.css").getPath());


        // Serve the worker logs on /logs/worker
        ContextHandler context1 = new ContextHandler();
        context1.setContextPath(WORKER_LOGS_CONTEXT);
        context1.addAliasCheck(new ContextHandler.ApproveAliases());

        context1.setBaseResource(Resource.newResource(Constants.MAESTRO_LOG_DIR));
        context1.setHandler(workerResourceHandler);
        logger.debug("Serving files from {} on /logs/worker", Constants.MAESTRO_LOG_DIR);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { context0, context1 });

        server.setHandler(contexts);

        logger.debug("Starting the data server");
        server.start();
        if (dataServerPort == 0) {
            dataServerPort = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        }

        logger.info("The data server is now serving worker log files on {}{}", getServerURL(), WORKER_LOGS_CONTEXT);
        logger.info("The data server is now serving test log files on {}{}", getServerURL(), TEST_LOGS_CONTEXT);
    }

    @Override
    public void run() {
        try {
            runServerInt();
            server.join();
        } catch (Exception e) {
            logger.error("Unable to start the data server: {}", e.getMessage(), e);
        }
    }


    /**
     * Gets the data server base URL. The front-end uses this to download the report files
     * @return
     */
    public String getServerURL() {
        AbstractConfiguration config = ConfigurationWrapper.getConfig();
        String host = config.getString("data.server.host", null);

        // Host configuration takes priority over detection
        if (host == null) {
            host = ((ServerConnector) server.getConnectors()[0]).getHost();

            // If null, it's binding to all interfaces and that's OK
            if (host == null) {
                try {
                    host = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.error("Unable to determine the address of the data server local host. Please set it " +
                            "manually in the configuration file via 'data.server.host' setting. Using 127.0.0.1 ...");
                    host = "127.0.0.1";
                }
            }
        }

        return "http://" + host + ":" + dataServerPort;
    }
}
