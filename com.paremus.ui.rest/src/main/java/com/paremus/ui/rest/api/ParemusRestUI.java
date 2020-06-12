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

import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.component.annotations.ComponentPropertyType;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;

/**
 * An annotation which adds requirements for extension features needed by the
 * Paremus REST UI.
 */
@ComponentPropertyType
@Requirement(namespace = SERVICE_NAMESPACE, filter = ParemusRestUI.FILTER, effective = EFFECTIVE_ACTIVE)
@Requirement(namespace = IMPLEMENTATION_NAMESPACE, name = "paremus-rest-ui", version = "1.0.0", effective = EFFECTIVE_ACTIVE)
public @interface ParemusRestUI {
    public static final String FILTER = "(osgi.jaxrs.media.type="
            + APPLICATION_JSON + ")";

    String PREFIX_ = "osgi.jaxrs.";

    String application_select() default "(osgi.jaxrs.name=paremus-rest-ui)";

    String[] extension_select() default {FILTER, "(paremus.security.authz=shiro)"};
}

