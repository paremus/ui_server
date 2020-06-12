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
import com.paremus.ui.rest.dto.FabricDTO;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;

@Component(service = FakeFabricResource.class)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Fabrics")
@Path(FakeFabricResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FakeFabricResource extends AbstractResource<FabricDTO> {
    static final String PATH = "fabrics";
    private LinkedHashMap<String, FabricDTO> dtos = new LinkedHashMap<>();

    public FakeFabricResource() {
        super(FabricDTO.class);
    }

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        add("eu-west-1", "Ireland",
                system("com.paremus.example.pkg.nginx-system", "1.0.4", true),
                system("com.paremus.example.zookeeper.zookeeper-system", "1.0.3", true)
        );
        add("eu-west-2", "London",
                system("com.paremus.example.enroute.microservice.microservice-jdbc-system", "1.0.2", true),
                system("com.paremus.example.enroute.microservice.microservice-pkg-system", "1.0.2", false)
        );
        add("eu-west-3", "Paris",
                system("com.paremus.example.enroute.microservice.microservice-jdbc-system", "1.0.2", true),
                system("com.paremus.example.enroute.microservice.microservice-pkg-system", "1.0.2", false)
        );
        add("us-east-1", "Virginia",
                system("com.paremus.example.enroute.microservice.microservice-jdbc-system", "1.0.2", true),
                system("com.paremus.example.enroute.microservice.microservice-pkg-system", "1.0.2", false)
        );
        add("us-east-2", "Ohio",
                system("com.paremus.example.enroute.microservice.microservice-jdbc-system", "1.0.2", true),
                system("com.paremus.example.enroute.microservice.microservice-pkg-system", "1.0.2", false)
        );
        add("us-west-1", "California",
                system("com.paremus.example.enroute.microservice.microservice-jdbc-system", "1.0.2", true),
                system("com.paremus.example.enroute.microservice.microservice-pkg-system", "1.0.2", false)
        );
        add("us-west-2", "Oregon",
                system("com.paremus.example.enroute.microservice.microservice-jdbc-system", "1.0.2", true),
                system("com.paremus.example.enroute.microservice.microservice-pkg-system", "1.0.2", false)
        );
    }


    private void add(String id, String location, FabricDTO.SystemDTO... systems) {
        FabricDTO dto = new FabricDTO();
        dto.id = id;
        dto.location = location;
        dto.owner = "Paremus";
        dto.managementURIs = Arrays.asList("http://localhost:8080", "http://localhost:3000");
        dto.actualFibres = 3;
        dto.expectedFibres = 3;
        dto.systems = Arrays.asList(systems);

        dtos.put(id, dto);
    }

    private FabricDTO.SystemDTO system(String id, String version, boolean running) {
        FabricDTO.SystemDTO dto = new FabricDTO.SystemDTO();
        dto.id = id;
        dto.version = version;
        dto.running = running;
        return dto;
    }

    @Override
    protected Collection<FabricDTO> getDtos() {
        return dtos.values();
    }

    @Override
    protected FabricDTO getDto(String id) {
        return dtos.get(id);
    }
}
