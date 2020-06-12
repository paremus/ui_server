/*-
 * #%L
 * com.paremus.ui.rest.fake
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
package com.paremus.ui.rest.fake;

import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.RestUtils;
import com.paremus.ui.rest.api.AbstractResource;
import com.paremus.ui.rest.dto.FibreDTO;
import com.paremus.ui.rest.dto.ZkServerDTO;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component(service = FakeZkServerResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus ZK Servers")
@Path(FakeZkServerResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FakeZkServerResource extends AbstractResource<ZkServerDTO> {
    static final String PATH = "zkservers";
    private LinkedHashMap<String, ZkServerDTO> dtos = new LinkedHashMap<>();

    private int id = 0;
    private Map<String, Integer> serverIds = new HashMap<>();
    private Map<String, Integer> ports = new HashMap<>();

    private final static String ROLE_OBSERVER = "observer";
    private final static String ROLE_PARTICIPANT = "participant";
    private final static String CROLE_FOLLOWER = "follower";
    private final static String CROLE_LEADER = "leader";
    private final static String CROLE_OBSERVER = "observer";

    private static final List<String> ROLES = Arrays.asList(ROLE_OBSERVER, ROLE_PARTICIPANT);

    @Reference
    @SuppressWarnings("unused")
    private FakeFibreResource fibreResource;

    public FakeZkServerResource() {
        super(ZkServerDTO.class);
    }

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        String[] fibres = fibreResource.getKeys().toArray(new String[0]);

        add("DEMO", fibres[0], CROLE_LEADER, ROLE_PARTICIPANT);
        add("ZK1", fibres[0], CROLE_OBSERVER, ROLE_OBSERVER);
        add("ZK1", fibres[1], CROLE_LEADER, ROLE_PARTICIPANT);
        add("ZK1", fibres[2], CROLE_FOLLOWER, ROLE_PARTICIPANT);
    }

    private ZkServerDTO add(String ensembleId, String fibreId, String currentRole, String role) {
        FibreDTO fibre = fibreResource.getDto(fibreId);
        String address = fibre.address;

        ZkServerDTO dto = new ZkServerDTO();
        dto.id = id++;

        dto.ensemble_id = ensembleId;
        dto.fibre_id = fibreId;
        dto.managementStatus = "managed";
        dto.currentRole = currentRole;
        dto.role = role;

        Integer serverId = serverIds.getOrDefault(ensembleId, 1);
        dto.serverId = serverId++;
        dto.address = address;
        dto.clientBindAddress = address;
        serverIds.put(ensembleId, serverId);

        Integer port = ports.getOrDefault(fibreId, 9140);
        dto.transactionPort = port++;
        dto.quorumPort = port++;
        dto.clientPort = port++;
        ports.put(fibreId, port);

        dtos.put(dto.id + "", dto);
        return dto;
    }

    @POST
    public ZkServerDTO createDto(@Context HttpServletRequest request, @Context HttpServletResponse response, ZkServerDTO dto) throws IOException {
        FibreDTO fibre = fibreResource.getDto(dto.fibre_id);

        if (fibre == null) {
            RestUtils.badRequestError(request, response, "Unknown fibre_id: " + dto.fibre_id);
            return null;
        }

        long count = dtos.values().stream().filter(d -> d.ensemble_id.equals(dto.ensemble_id)).count();

        String currentRole = count == 0 ? CROLE_LEADER : CROLE_FOLLOWER;

        return add(dto.ensemble_id, dto.fibre_id, currentRole, ROLE_PARTICIPANT);
    }

    @PUT
    @Path("{id}")
    public ZkServerDTO changeDto(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("id") String id, ZkServerDTO update) throws IOException {
        ZkServerDTO dto = dtos.get(id);

        if (!ROLES.contains(update.role)) {
            RestUtils.badRequestError(request, response, "Bad role: " + update.role);
            return null;
        }


        if (CROLE_LEADER.equals(dto.currentRole) && ROLE_OBSERVER.equals(update.role)) {
            Optional<ZkServerDTO> nextLeader = dtos.values().stream()
                    .filter(d -> d.id != dto.id && d.ensemble_id.equals(dto.ensemble_id) && ROLE_PARTICIPANT.equals(d.role)).findFirst();

            if (!nextLeader.isPresent()) {
                RestUtils.badRequestError(request, response, "Can't change role of leader without other particpants.");
                return null;
            }

            nextLeader.get().currentRole = CROLE_LEADER;
        }

        dto.role = update.role;
        dto.currentRole = ROLE_OBSERVER.equals(update.role) ? CROLE_OBSERVER : CROLE_FOLLOWER;
        return dto;
    }

    @DELETE
    @Path("{id}")
    public ZkServerDTO deleteDto(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("id") String id) throws IOException {
        ZkServerDTO dto = dtos.get(id);

        if (dto == null)
            throw new NotFoundException();

        if (CROLE_LEADER.equals(dto.currentRole)) {
            Optional<ZkServerDTO> nextLeader = dtos.values().stream()
                    .filter(d -> d.id != dto.id && d.ensemble_id.equals(dto.ensemble_id) && ROLE_PARTICIPANT.equals(d.role)).findFirst();
            if (!nextLeader.isPresent()) {
                RestUtils.badRequestError(request, response, "Can't remove last participant server, delete ensemble instead.");
                return null;
            }
            nextLeader.get().currentRole = CROLE_LEADER;
        }

        return dtos.remove(id);
    }

    @Override
    protected Collection<ZkServerDTO> getDtos() {
        return dtos.values();
    }

    @Override
    protected ZkServerDTO getDto(String id) {
        return dtos.get(id);
    }
}
