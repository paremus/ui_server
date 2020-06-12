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

import com.paremus.ui.rest.api.AbstractResource;
import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.RestUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = FakeBehaviourResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Behaviours")
@Path(FakeBehaviourResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FakeBehaviourResource extends AbstractResource<FakeBehaviourDTO> {
    static final String PATH = "behaviours";
    private LinkedHashMap<String, FakeBehaviourDTO> dtos = new LinkedHashMap<>();

    public FakeBehaviourResource() {
        super(FakeBehaviourDTO.class);
    }

    @Activate
    @SuppressWarnings("unused")
    private void activate() {

        add("com.paremus.smart.security.SecurityBehaviour", "1.0.0", "Smart Security Light",
                "Activated by virtual sensor and then slowly dims security light",
                "fabric-n2"
        );

        add("com.paremus.smart.kitchen.SmartToaster", "1.2.0", "Smart Toaster",
                "Burns weather map onto toast"
        );

        add("com.paremus.smart.water.SmartDamDoor", "1.3.1", "Smart Dam Door",
                "Opens the flood gates"
        );
    }


    private void add(String bsn, String version, String name, String desc, String... hosts) {
        FakeBehaviourDTO dto = new FakeBehaviourDTO();
        dto.id = bsn + ":" + version;
        dto.bundle = bsn;
        dto.version = version;
        dto.name = name;
        dto.description = desc;
        dto.hosts = new HashSet<>();
        if (hosts != null) {
            dto.hosts.addAll(Arrays.asList(hosts));
        }

        dto.author = "Paremus";
        dto.consumed = "com.paremus.smart.behaviour.SmartDTO";

        dtos.put(dto.id, dto);
    }

    @Override
    protected Collection<FakeBehaviourDTO> getDtos() {
        return dtos.values();
    }

    @Override
    protected FakeBehaviourDTO getDto(String id) {
        return dtos.get(id);
    }

    @PUT
    @Path("{id}")
    public FakeBehaviourDTO installBehaviour(@Context HttpServletRequest request, @Context HttpServletResponse response,
                                             FakeBehaviourDTO installDto, @PathParam("id") String id) {
        FakeBehaviourDTO dto = dtos.get(id);

        if (dto == null) {
            RestUtils.sendError(request, response, HttpServletResponse.SC_NOT_FOUND, id + " not found");
        }

        if (installDto.installHost == null || installDto.installHost.isEmpty()) {
            RestUtils.badRequestError(request, response, "installHost missing");
        }

        dto.hosts.add(installDto.installHost);

        return dto;
    }

    Map<String, String> getBehaviours(String host) {
        return dtos.values().stream()
                .filter(d -> d.hosts.contains(host))
                .collect(Collectors.toMap(d -> d.bundle + ":" + d.version, d -> d.name));
    }

    void uninstallBehaviour(String host, String behaviour) {
        dtos.values().forEach(dto -> {
            if ("ALL".equals(behaviour) || dto.id.equals(behaviour)) {
                dto.hosts.remove(host);
            }
        });
    }
}
