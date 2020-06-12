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
import com.paremus.ui.rest.dto.LoginDTO;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Component(service = LoginResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Login")
@Path("login")
@Produces(MediaType.APPLICATION_JSON)
public class LoginResource {
    @POST
    public String login(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            LoginDTO dto) throws IOException {
        try {
            Subject subject = SecurityUtils.getSubject();

            if (dto.username == null || dto.username.isEmpty()) {
                subject.logout();
                return null;
            }

            String host = request.getRemoteHost();
            AuthenticationToken token = new UsernamePasswordToken(dto.username, dto.password, dto.rememberMe, host);

            subject.login(token);
            // somehow shiro adds Set-Cookie headers to the response?

            return "\"OK\"";
        } catch (AuthenticationException e) {
            System.err.println("XXX login failed: " + e);
            RestUtils.sendError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "access denied");
            return null;
        }
    }

}
