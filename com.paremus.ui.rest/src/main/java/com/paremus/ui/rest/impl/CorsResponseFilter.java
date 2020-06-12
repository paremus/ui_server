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
package com.paremus.ui.rest.impl;

import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.RestUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsExtension;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

@Component
@ParemusRestUI
@JaxrsExtension
@SuppressWarnings("unused")
public class CorsResponseFilter implements ContainerResponseFilter {

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String method = requestContext.getMethod();
        // System.err.println("XXX CORS filter method=" + method + " path=" + requestContext.getUriInfo().getPath());

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        RestUtils.allowOrigin(requestContext.getHeaderString("Origin")).forEach((k, v) -> {
            headers.add(k, v);
        });

        if ("OPTIONS".equals(method)) {
            headers.add(ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
            headers.add(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE");
            // how long the response to the preflight request can be cached
            headers.add(ACCESS_CONTROL_MAX_AGE, "86400"); // 24 hours
        }

    }

}
