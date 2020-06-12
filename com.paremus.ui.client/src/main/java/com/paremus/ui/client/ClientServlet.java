/*-
 * #%L
 * com.paremus.ui.client
 * %%
 * Copyright (C) 2018 - 2019 Paremus Ltd
 * %%
 * Licensed under the Fair Source License, Version 0.9 (the "License");
 *
 * See the NOTICE.txt file distributed with this work for additional
 * information regarding copyright ownership. You may not use this file
 * except in compliance with the License. For usage restrictions see the
 * LICENSE.txt file distributed with this work
 * #L%
 */
package com.paremus.ui.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paremus.ui.rest.api.RestUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

// install client servlet on root context
@Component(configurationPid = ClientServlet.PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=" + RestUtils.ROOT_CONTEXT_FILTER,
        HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=" + ClientServlet.UI_BASE + "*",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=Client Servlet"
})
public class ClientServlet extends HttpServlet implements Servlet {
    public static final String PID = "com.paremus.ui.client";
    static final String UI_BASE = "/";
    static final String UI_CLIENT = "/ui-client";

    @interface UIConfig {
        String title() default "Paremus Product";

        String dashHead() default "Welcome to the Paremus Product!";

        String dashContent() default "Lorem ipsum sic dolor amet...";

        // Hide UI components: config | ensembles | events | fabrics
        String[] hide() default {};
    }

    private static final long serialVersionUID = 1L;

    private UIConfig uiConfig;

    private URI clientBase;
    private URL clientIndex;

    private long indexModified = 0;
    private byte[] indexData;

    @Activate
    private void activate(BundleContext context, UIConfig uiConfig) throws Exception {
        System.err.println("ClientServlet: activate!");

        this.uiConfig = uiConfig;

        String clientDir = context.getProperty("com.paremus.ui.client.dir");

        if (clientDir != null) {
            File baseDir = new File(clientDir);
            if (baseDir.isDirectory()) {
                clientBase = baseDir.toURI();
            } else {
                clientBase = new URI(clientDir);
            }
            clientIndex = clientBase.resolve("index.html").toURL();
        }

        if (clientIndex == null) {
            URL url = getClass().getResource(UI_CLIENT);
            if (url != null) {
                clientIndex = getClass().getResource(UI_CLIENT + "/index.html");
            }
        }

        if (clientIndex == null) {
            System.err.println("eek! ClientServlet: client not configured");
            // this exception does not appear in logs?
            throw new Exception("ClientServlet: client not configured");
        }

        System.err.println("ClientServlet: clientIndex=" + clientIndex);
    }

    @Deactivate
    private void deactivate() throws Exception {
        System.err.println("ClientServlet: deactivate");
    }

    private byte[] getIndexData() throws IOException {
        URLConnection connection = clientIndex.openConnection();
        long modified = connection.getLastModified();

        if (indexModified <= modified) {
            indexModified = modified;
            String s = readAll(connection.getInputStream());
            indexData = addSeedData(s, getSeedData()).getBytes(StandardCharsets.UTF_8);
        }
        return indexData;
    }

    private String addSeedData(String s, String jsonData) throws IOException {
        return s.replaceFirst("%TITLE%", uiConfig.title())
                .replaceFirst("\"%SEED_DATA%\"", jsonData);
    }

    private String getSeedData() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode seedNode = mapper.createObjectNode();

        seedNode.put("apiBase", RestUtils.API_BASE);

        ObjectNode dash = seedNode.putObject("dashboard");
        dash.put("head", uiConfig.dashHead());
        dash.put("content", uiConfig.dashContent());

        ArrayNode hide = seedNode.putArray("hide");
        for (String h : uiConfig.hide()) {
            hide.add(h);
        }

        return seedNode.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String resource = request.getRequestURI();
        System.err.println("ClientServlet: request uri: " + resource);

        if (resource.startsWith(UI_BASE))
            resource = resource.substring(UI_BASE.length());

        if (resource.isEmpty())
            resource = "index.html";

        URL url = tryResource(resource);

        // virtual paths within app should served with index.html
        if (url == null) {
            url = clientIndex;
        }

        if (url == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            OutputStream outputStream = response.getOutputStream();

            if (url.equals(clientIndex)) {
                outputStream.write(getIndexData());
            } else {
                InputStream inputStream = url.openStream();
                copy(inputStream, outputStream);
            }
            outputStream.close();
        }
    }

    private URL getResource(String name) {
        if (clientBase != null) {
            URI uri = clientBase.resolve(name);
            try (InputStream s = uri.toURL().openStream()) {
                return uri.toURL();
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
            return null;
        } else {
            return getClass().getResource(UI_CLIENT + "/" + name);
        }
    }

    /**
     * reloading URL e.g. /hosts/localhost fails because index.html contains <script src="js/bundle.js"/>
     * and we fail to load /hosts/js/bundle.js.
     *
     * It should be possible to configure create-react-app and/or webpack to inject src="/js/bundle.js",
     * but for now we'll handle it here:
     */
    private URL tryResource(String name) {
        URL url = getResource(name);
        if (url == null && name.contains("/")) {
            return tryResource(name.replaceFirst("[^/]*/",""));
        }
        return url;
    }

    private static long copy(InputStream source, OutputStream sink) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[8192];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    private static String readAll(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        return reader.lines().collect(Collectors.joining("\n"));
    }
}


