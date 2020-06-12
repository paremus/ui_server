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
import com.paremus.ui.rest.dto.FibreDTO;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;

@Component(service = FakeFibreResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Fibres")
@Path(FakeFibreResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FakeFibreResource extends AbstractResource<FibreDTO> {
    static final String PATH = "fibres";

    private LinkedHashMap<String, FibreDTO> dtos = new LinkedHashMap<>();

    private int address = 0;

    public FakeFibreResource() {
        super(FibreDTO.class);
    }

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        add("infra-0", "DEMO", "ZK1");
        add("simple-1", "ZK1");
        add("simple-2", "ZK1");
        add("simple-3");
        add("simple-4");
        add("simple-5");
        add("simple-6");
        add("simple-7");
        add("simple-8");
        add("simple-9");
    }

    private void add(String id, String... ensembles) {
        FibreDTO dto = new FibreDTO();
        dto.id = id;
        dto.address = "10.0.0." + ++address;
        dto.status = "ok";
        dto.security = true;
        dto.ensembles = new ArrayList<>(Arrays.asList(ensembles));
        dtos.put(id, dto);
    }

    @Override
    protected Collection<FibreDTO> getDtos() {
        return dtos.values();
    }

    @Override
    protected FibreDTO getDto(String id) {
        return dtos.get(id);
    }

    Collection<String> getKeys() {
        return dtos.keySet();
    }
}
