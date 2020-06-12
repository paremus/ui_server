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
import com.paremus.ui.rest.api.AbstractResource;
import com.paremus.ui.rest.dto.EnsembleDTO;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(service = FakeEnsembleResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Ensembles")
@Path(FakeEnsembleResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FakeEnsembleResource extends AbstractResource<EnsembleDTO> {
    static final String PATH = "ensembles";
    private LinkedHashMap<String, EnsembleDTO> dtos = new LinkedHashMap<>();

    public FakeEnsembleResource() {
        super(EnsembleDTO.class);
    }

    @Reference
    @SuppressWarnings("unused")
    private FakeZkServerResource zkServerResource;

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        add("DEMO", "env=test", "env=dev");
        add("ZK1", "env=infra");
    }

    private void add(String id, String... tags) {
        EnsembleDTO dto = new EnsembleDTO();
        dto.id = id;
        dto.tags = new ArrayList<>();

        for (String tag : tags) {
            dto.tags.add(tagMap(tag));
        }

        dtos.put(id, dto);
    }

    private Map<String, String> tagMap(String tag) {
        Map<String, String> m = new HashMap<>(2);

        String[] kv = tag.split("=", 2);
        m.put("name", kv[0]);
        m.put("value", kv.length == 2 ? kv[1] : "");

        return m;
    }

    @POST
    public EnsembleDTO createDto(@Context HttpServletResponse response, EnsembleDTO dto) {
        dtos.put(dto.id, dto);
        return dto;
    }

    @DELETE
    @Path("{id}")
    public EnsembleDTO deleteDto(@Context HttpServletResponse response, @PathParam("id") String id) {
        EnsembleDTO dto = dtos.get(id);

        if (dto == null)
            throw new NotFoundException();

        zkServerResource.getDtos().removeIf(d -> id.equals(d.ensemble_id));

        return dtos.remove(id);
    }

    @Override
    protected Collection<EnsembleDTO> getDtos() {
        return dtos.values();
    }

    @Override
    protected EnsembleDTO getDto(String id) {
        return dtos.get(id);
    }
}
