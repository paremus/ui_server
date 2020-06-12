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

import com.paremus.ui.rest.api.EventApi;
import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.AbstractResource;
import com.paremus.ui.rest.dto.IdentityDTO;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.stream.Collectors;

@Component(service = EventTopicResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Event Topics")
@Path("event_topics")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventTopicResource extends AbstractResource<IdentityDTO> {

    @Reference
    private EventApi events;

    public EventTopicResource() {
        super(IdentityDTO.class);
    }

    private IdentityDTO mkIdentity(String topic) {
        IdentityDTO dto = new IdentityDTO();
        dto.id = topic;
        return dto;
    }

    @Override
    protected Collection<IdentityDTO> getDtos() {
        return events.getTopics().stream().map(t -> mkIdentity(t)).collect(Collectors.toList());
    }

    @Override
    protected IdentityDTO getDto(String id) {
        return null;
    }

}
