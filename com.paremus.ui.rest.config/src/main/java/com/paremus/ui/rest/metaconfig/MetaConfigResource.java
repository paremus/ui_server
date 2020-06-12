/*-
 * #%L
 * com.paremus.ui.rest.config
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
package com.paremus.ui.rest.metaconfig;

import com.paremus.ui.metaconfig.MetaConfig;
import com.paremus.ui.metaconfig.MetaConfigDTO;
import com.paremus.ui.rest.api.AbstractResource;
import com.paremus.ui.rest.api.FrameworkInfo;
import com.paremus.ui.rest.api.ParemusRestUI;
import com.paremus.ui.rest.api.RestUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component(configurationPid = MetaConfigResource.PID)
@ParemusRestUI
@JaxrsResource
@JaxrsName("Paremus Configuration")
@Path(MetaConfigResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class MetaConfigResource extends AbstractResource<MetaConfigDTO> implements FrameworkInfo {
    static final String PATH = "config";
    static final String PID = "com.paremus.ui.metaconfig";

    private Map<String, MetaConfig> metaConfigs = new ConcurrentHashMap<>();
    private Map<Object, String> services = new HashMap<>();

    private Map<String, String> fwid2uhost = new HashMap<>();
    private Set<MetaConfigDTO.ConfigType> excludes = new HashSet<>();

    @interface Config {
        MetaConfigDTO.ConfigType[] excludes();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private void addMetaConfig(MetaConfig metaConfig, Map<String, Object> props) {
        String fwId = metaConfig.getFrameworkId();
        metaConfigs.put(fwId, metaConfig);
        Object serviceId = props.get(Constants.SERVICE_ID);
        services.put(serviceId, fwId);

        // hostname may not be unique
        String hostname = metaConfig.getHostname();
        long count = fwid2uhost.values().stream().filter(h -> h.equals(hostname) || h.startsWith(hostname + "_")).count();

        String uhostname = hostname + (count > 0 ? ("_" + count) : "");
        fwid2uhost.put(fwId, uhostname);

        System.out.println("XXX addMetaConfig: " + uhostname);
    }

    private void removeMetaConfig(MetaConfig metaConfig, Map<String, Object> props) {
        Object serviceId = props.get(Constants.SERVICE_ID);
        String fwId = services.get(serviceId);
        metaConfigs.remove(fwId);
        String uhostname = fwid2uhost.remove(fwId);
        System.out.println("XXX removeMetaConfig: " + uhostname);
    }

    public MetaConfigResource() {
        super(MetaConfigDTO.class);
    }

    @Activate
    void activate(Config config) {
        modified(config);
    }

    @Modified
    void modified(Config config) {
        excludes.clear();
        if (config.excludes() != null) {
            excludes.addAll(Arrays.asList(config.excludes()));
        }
    }

    private String getFwId(String id) {
        int i = id.lastIndexOf('~');
        return id.substring(i + 1);
    }


    @DELETE
    @Path("{id}")
    public String deleteConfig(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("id") String id) {
        String fwId = getFwId(id);
        MetaConfig metaConfig = metaConfigs.get(fwId);

        if (metaConfig == null) {
            RestUtils.badRequestError(request, response, "Unknown framework id: " + fwId);
        }

        try {
            metaConfig.deleteConfig(id);
        } catch (Exception e) {
            throw new ServerErrorException("eek!", 500, e);
        }

        return "OK";
    }

    @PUT
    @Path("{id}")
    public MetaConfigDTO updateConfig(@Context HttpServletRequest request, @Context HttpServletResponse response,
                                      MetaConfigDTO dto, @PathParam("id") String id) {
        String fwId = getFwId(dto.id);
        MetaConfig metaConfig = metaConfigs.get(fwId);

        if (metaConfig == null) {
            RestUtils.badRequestError(request, response, "Unknown framework id: " + fwId);
        } else {
            try {
                metaConfig.updateConfig(dto.pid, dto.factoryPid, dto.isFactory, dto.values);
            } catch (IllegalArgumentException e) {
                RestUtils.badRequestError(request, response, "Validation failed: " + e.getMessage());
            } catch (Exception e) {
                throw new ServerErrorException("eek!", 500, e);
            }
        }

        return dto;
    }

    @Override
    protected MetaConfigDTO getDto(String id) {
        String fwId = getFwId(id);
        MetaConfig metaConfig = metaConfigs.get(fwId);

        if (metaConfig == null) {
            return null;
        }

        return metaConfig.getConfig(id);
    }

    @Override
    protected Collection<MetaConfigDTO> getDtos() {
        List<MetaConfigDTO> dtos = new ArrayList<>();

        for (MetaConfig mc : metaConfigs.values()) {
            mc.getConfig().stream()
                    .filter(dto -> !excludes.contains(dto.type))
                    .forEach(dtos::add);
        }

        return dtos;
    }

    @Override
    public String getFrameworkId(String hostname) {
        return fwid2uhost.entrySet().stream()
                .filter(e -> e.getValue().equals(hostname))
                .map(e -> e.getKey())
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getHostname(String frameworkId) {
        return fwid2uhost.get(frameworkId);
    }

    @Override
    public List<String> getBundleHosts(String symbolicName, String version) {
        return metaConfigs.values().stream()
                .filter(mc -> mc.getBundleVersions(symbolicName).contains(version))
                .map(mc -> mc.getFrameworkId())
                .map(this::getHostname)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<String>> getHosts(List<String> symbolicNameVersions) {
        Map<String, List<String>> hostMap = new HashMap<>();

        metaConfigs.forEach((fwId, mc) -> {
            String host = getHostname(fwId);
            mc.checkBundles(symbolicNameVersions).forEach(nv -> {
                List<String> hosts = hostMap.get(nv);
                if (hosts == null) {
                    hosts = new ArrayList<>();
                    hostMap.put(nv, hosts);
                }
                hosts.add(host);
            });
        });

        return hostMap;
    }

    @Override
    public Map<String, String> getHostMap() {
        return Collections.unmodifiableMap(fwid2uhost);
    }

    @Override
    public Map<String, String> getHostInfo(String fwId) {
        MetaConfig metaConfig = metaConfigs.get(fwId);
        if (metaConfig != null) {
            return metaConfig.getHostInfo();
        }
        return null;
    }
}
