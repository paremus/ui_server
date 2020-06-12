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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.ResourceFilter;
import com.paremus.ui.rest.api.RestUtils;
import com.paremus.ui.rest.api.WatchApi;
import com.paremus.ui.rest.dto.WatchDTO;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Server Watch Resource")
@Path("watch")
@Produces(MediaType.SERVER_SENT_EVENTS)
@RequiresAuthentication
public class WatchResource implements WatchApi {

    static class Client {
        int available;
        long millis;
        String filter;
        String resource;
        String session;
        ResourceFilter resourceFilter;
        SseEventSink eventSink;
        Sse sse;
        long key;

        Client(ResourceFilter resourceFilter, String resource, String session, String filter, SseEventSink eventSink, Sse sse, long key) {
            this.resourceFilter = resourceFilter;
            this.resource = resource;
            this.session = session;
            this.filter = filter;
            this.eventSink = eventSink;
            this.sse = sse;
            this.key = key;
            this.millis = System.currentTimeMillis();
        }
    }

    private Map<Long, Client> clients = new ConcurrentHashMap<>();
    private Map<String, Class<?>> resource2class = new ConcurrentHashMap<>();
    private Map<String, Observable> observables = new ConcurrentHashMap<>();

    private AtomicLong id = new AtomicLong();

    private ObjectMapper mapper = new ObjectMapper();

    private ScheduledExecutorService executor;

    @Activate
    void activate() throws InvalidSyntaxException {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new WatchProcessor(), 10, 10, TimeUnit.SECONDS);
    }

    @Deactivate
    void deactivate() {
        executor.shutdownNow();
    }

    @Override
    public void register(String path, Class<?> clazz) {
        resource2class.put(path, clazz);
    }

    @Override
    public void check(Object dto) {
        clients.values().stream().forEach(c -> {
            if (c.resourceFilter.accept(dto)) {
                c.available++;
            }
        });
    }

    @Override
    public void addObserver(String resource, Observer observer) {
        if (!resource2class.containsKey(resource)) {
            throw new UnsupportedOperationException("resource does not allow watches: " + resource);
        }
        observables.putIfAbsent(resource, new WatchObservable());
        observables.get(resource).addObserver(observer);
    }

    @Override
    public void removeObserver(String resource, Observer observer) {
        observables.computeIfPresent(resource, (r, o) -> {
            o.deleteObserver(observer);
            return o;
        });
    }

    @Override
    public List<List<String>> getFilters(String resource) {
        return clients.values().stream()
                .filter(c -> c.resource.equals(resource))
                .map(c -> c.resourceFilter.getFilters())
                .collect(Collectors.toList());
    }

    private void clientsChanged(String resource) {
        observables.computeIfPresent(resource, (r, o) -> {
            o.notifyObservers(resource);
            return o;
        });
    }

    @GET
    public void eventStream(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SseEventSink eventSink,
            @Context Sse sse,
            @QueryParam("filter") String filter,
            @QueryParam("window") String window) throws IOException {
        eventStream(request, response, eventSink, sse, null, filter, window);
    }

    @GET
    @Path("{resource}")
    public void eventStream(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SseEventSink eventSink,
            @Context Sse sse,
            @PathParam("resource") String resource,
            @QueryParam("filter") String filter,
            @QueryParam("window") String window) throws IOException {
        System.err.println("SSE: subscribe: resource=" + resource + " filter=" + filter + " window=" + window);

//        String remoteAddr = request.getRemoteAddr();
//        String remoteHost = request.getRemoteHost();
//        int remotePort = request.getRemotePort();
//        System.err.println("SSE: client: addr=" + remoteAddr + ", host=" + remoteHost + ", port=" + remotePort);

        RestUtils.allowOrigin(request.getHeader("Origin")).forEach((k, v) -> {
            response.addHeader(k, v);
        });

        Class<?> resourceClass = resource2class.get(resource);

        if (resourceClass == null) {
            eventSink.close();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "resource does not allow watches: " + resource);
            return;
        }

        ResourceFilter resourceFilter = new ResourceFilter(resourceClass, filter);

        // client window is added to sessionId to make it unique between browser tabs
        Cookie[] cookies = request.getCookies();
        String sessionId = window + (cookies == null ? "" :
                Arrays.asList(cookies).stream()
                        .filter(c -> c.getName().equals(RestUtils.SESSION_COOKIE_NAME))
                        .map(c -> c.getValue())
                        .findFirst().orElse(""));

        // client is only allowed one watch per resource per client window
        // otherwise old watches only get detected when client window is closed
        long key = clients.values().stream().filter(c ->
                resource.equals(c.resource) && sessionId.equals(c.session))
                .map(c -> c.key)
                .findFirst()
                .orElse(id.incrementAndGet());

        Client oldClient = clients.remove(key);

        if (oldClient != null) {
            System.err.println("SSE: removed client: " + key);
            oldClient.eventSink.close();
        }

        System.err.println("SSE: subscribe: id=" + key);
        Client client = new Client(resourceFilter, resource, sessionId, filter, eventSink, sse, key);
        clients.put(key, client);
        clientsChanged(resource);
    }

    static class WatchObservable extends Observable {
        @Override
        public void notifyObservers(Object arg) {
            super.setChanged();
            super.notifyObservers(arg);
        }
    }

    class WatchProcessor implements Runnable {
        final long purgeDeadClients = 60 * 1000L;

        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();

                clients.values().stream().forEach(c -> {
                    SseEventSink sink = c.eventSink;
                    if (sink.isClosed()) {
                        System.err.println("SSE: closed; dropping client: " + c.key);
                        clients.remove(c.key);
                        clientsChanged(c.resource);
                    } else {
                        if (c.available > 0 || (now - c.millis) > purgeDeadClients) {
                            try {
                                WatchDTO watch = new WatchDTO();
                                watch.resource = c.resource;
                                watch.filter = c.filter;
                                watch.available = c.available;
                                c.available = 0;
                                c.millis = now;

                                String json = mapper.writeValueAsString(watch);
                                //System.out.println("SSE: send:client: " + c.key + " watch: " + json);
                                OutboundSseEvent sseEvent = c.sse.newEvent("watch", json);

                                sink.send(sseEvent).exceptionally((t) -> {
                                    System.err.println("SSE: send failed: " + t + "; dropping client: " + c.key);
                                    clients.remove(c.key);
                                    clientsChanged(c.resource);
                                    sink.close();
                                    return null;
                                });
                            } catch (IllegalStateException e) {
                                System.err.println("SSE: send failed: " + e + "; dropping client: " + c.key);
                                clients.remove(c.key);
                                clientsChanged(c.resource);
                                sink.close();
                            } catch (JsonProcessingException e) {
                                System.err.println("SSE: watch processor (ignored): " + e);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
