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

import com.paremus.ui.rest.api.AbstractResource;
import com.paremus.ui.rest.api.EventApi;
import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.WatchApi;
import com.paremus.ui.rest.dto.EventDTO;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Events")
@Path(EventResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventResource extends AbstractResource<EventDTO> implements EventApi {
    static final String PATH = "events";
    private final int history = 10240;

    @Reference
    private WatchApi watch;

    private LinkedList<EventDTO> dtos = new LinkedList<>();
    private SortedSet<String> sources = new TreeSet<>();
    private SortedSet<String> topics = new TreeSet<>();
    private AtomicLong id = new AtomicLong();

    public EventResource() {
        super(EventDTO.class);
    }

    @Activate
    void activate() {
        watch.register(PATH, EventDTO.class);
    }

    @Override
    public void add(EventDTO event) {
        while (dtos.size() >= history) {
            dtos.poll();
        }

        if (event.id == null || event.id.isEmpty()) {
            event.id = String.valueOf(id.incrementAndGet());
        }

        dtos.add(event);
        sources.add(event.source);
        topics.add(event.topic);
        watch.check(event);
    }

    @Override
    public Collection<String> getSources() {
        return sources;
    }

    @Override
    public Collection<String> getTopics() {
        return topics;
    }

    @Override
    protected Collection<EventDTO> getDtos() {
        return dtos;
    }

    @Override
    protected EventDTO getDto(String id) {
        return dtos.stream().filter(dto -> dto.id.equals(id)).findFirst().orElse(null);
    }

}
