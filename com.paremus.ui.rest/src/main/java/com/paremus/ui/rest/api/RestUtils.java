/*-
 * #%L
 * com.paremus.ui.rest
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
package com.paremus.ui.rest.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME;

public class RestUtils {
    public static final String SESSION_COOKIE_NAME = "Paremus-SessionId";

    public static final String API_BASE = "/api";
    public static final String ROOT_CONTEXT_NAME = "paremus-root";
    public static final String ROOT_CONTEXT_FILTER = "(" + HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ROOT_CONTEXT_NAME + ")";

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static <T> T requireAvailable(T optionalReference) {
        if (optionalReference == null) {
            throw new MissingServiceException();
        } else {
            return optionalReference;
        }
    }

    public static <T> T requireAvailable(T optionalReference, String errorMessage) {
        if (optionalReference == null) {
            throw new MissingServiceException(errorMessage);
        } else {
            return optionalReference;
        }
    }

    public static Map<String, String> allowOrigin(String origin) {
        HashMap<String, String> map = new HashMap<>(2);
        if (origin != null) {
            map.put(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            map.put(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            map.put("Vary", "Origin");
        }
        return map;
    }

    public static void sendError(HttpServletRequest request, HttpServletResponse response, int status, String message) {
        // CORS filter not invoked for sendError response
        allowOrigin(request.getHeader("Origin")).forEach((k, v) -> {
            response.setHeader(k, v);
        });
        try {
            response.sendError(status, message);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void badRequestError(HttpServletRequest request, HttpServletResponse response, String message) {
        sendError(request, response, HttpServletResponse.SC_BAD_REQUEST, message);
    }
}
