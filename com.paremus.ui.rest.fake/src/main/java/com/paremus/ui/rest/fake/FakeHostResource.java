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
package com.paremus.ui.rest.fake;

import com.paremus.ui.metaconfig.MetaConfig;
import com.paremus.ui.rest.api.AbstractResource;
import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.RestUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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

@Component(service = FakeHostResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Hosts")
@Path("hosts")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FakeHostResource extends AbstractResource<FakeHostDTO> {

    @Reference
    private FakeBehaviourResource behaviourResource;

    @Reference
    private MetaConfig metaConfig;

    public FakeHostResource() {
        super(FakeHostDTO.class);
    }

    private List<FakeHostDTO> dtos = new ArrayList<>();

    private void activate() {
        dtos.add(mkHost("fabric-n1", "Raspberry Pi [model 3]", "1.0 GiB", "ARMv7"));
        dtos.add(mkHost("fabric-n2", "Raspberry Pi [model 4]", "4.0 GiB", "ARMv7"));
        dtos.add(mkHost("fabric-n3", "Intel [NUC]", "16.0 GiB", "Intel64 Family 6 Model 70 Stepping 1"));

        String hostname = metaConfig.getHostname();
        FakeHostDTO localhost = mkHost(hostname, null, null, null);
        localhost.info.putAll(metaConfig.getHostInfo());
        dtos.add(localhost);
    }

    private FakeHostDTO mkHost(String host, String model, String memory, String cpu) {
        FakeHostDTO dto = new FakeHostDTO();
        dto.id = host;
        dto.fwId = "some-frame-work-uuid";

        dto.info = new HashMap<>();
        dto.info.put("model", model);
        dto.info.put("memory", memory);
        dto.info.put("cpu_id", cpu);
        dto.info.put("disk", "31.0 GiB");

        return dto;
    }

    @Override
    protected Collection<FakeHostDTO> getDtos() {
        dtos.forEach(dto -> {
            dto.behaviours = behaviourResource.getBehaviours(dto.id);
        });
        return dtos;
    }

    @Override
    protected FakeHostDTO getDto(String id) {
        return getDtos().stream()
                .filter(dto -> dto.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    @PUT
    @Path("{id}")
    public FakeHostDTO uninstallBehaviour(@Context HttpServletRequest request, @Context HttpServletResponse response,
                                          FakeHostDTO hostDTO, @PathParam("id") String id) {
        if (hostDTO.uninstall == null || hostDTO.uninstall.isEmpty()) {
            RestUtils.badRequestError(request, response, "uninstall missing");
        }

        behaviourResource.uninstallBehaviour(hostDTO.id, hostDTO.uninstall);

        return hostDTO;
    }

}
