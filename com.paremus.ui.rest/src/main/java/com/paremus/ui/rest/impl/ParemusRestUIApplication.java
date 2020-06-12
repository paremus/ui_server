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

import com.paremus.ui.rest.api.RestUtils;
import org.osgi.annotation.bundle.Capability;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationBase;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsWhiteboardTarget;

import javax.ws.rs.core.Application;
import java.util.Set;

import static com.paremus.ui.rest.api.RestUtils.ROOT_CONTEXT_FILTER;
import static java.util.Collections.singleton;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;

@Component(service = Application.class,
        property = {HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=" + ROOT_CONTEXT_FILTER}
)
@JaxrsApplicationBase(RestUtils.API_BASE)
@JaxrsName("paremus-rest-ui")
@JaxrsWhiteboardTarget("(paremus.whiteboard.name=paremus-rest)")
@Capability(namespace = IMPLEMENTATION_NAMESPACE, name = "paremus-rest-ui", version = "1.0.0")
public class ParemusRestUIApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return singleton(MissingServiceExceptionMapper.class);
    }
}
