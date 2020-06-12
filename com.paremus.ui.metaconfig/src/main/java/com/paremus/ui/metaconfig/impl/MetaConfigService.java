/*-
 * #%L
 * com.paremus.ui.metaconfig
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
package com.paremus.ui.metaconfig.impl;

import com.paremus.ui.metaconfig.MetaConfig;
import com.paremus.ui.metaconfig.MetaConfigDTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.RequireMetaTypeImplementation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@RequireConfigurationAdmin
@RequireMetaTypeImplementation
@Component(property = {Constants.SERVICE_EXPORTED_INTERFACES + "=*",
        // "com.paremus.dosgi.scope=global"
})
public class MetaConfigService implements MetaConfig {
    @Reference
    private ConfigurationAdmin cm;

    @Reference
    private MetaTypeService mts;

    private BundleContext context;

    private String frameworkId;

    private String hostName;

    private Map<String, MetaConfigDTO> configs = new LinkedHashMap<>();

    @Activate
    private void activate(BundleContext context) {
        this.context = context;
        frameworkId = context.getProperty(Constants.FRAMEWORK_UUID);

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "unknown";
        }
        System.out.println("XXX MetaConfigService.activate() hostname=" + hostName);
    }

    @Modified
    private void modified() {
        System.out.println("XXX MetaConfigService.modified()");
    }

    @Deactivate
    private void deactivate() {
        System.out.println("XXX MetaConfigService.deactivate()");
    }

    @Override
    public String getFrameworkId() {
        return frameworkId;
    }

    @Override
    public String getHostname() {
        return hostName;
    }

    @Override
    public Map<String, String> getHostInfo() {
       return HostInfo.getInfo();
    }

    @Override
    public List<String> getBundleVersions(String symbolicName) {
        List<String> versions = new ArrayList<>();
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName))
                versions.add(bundle.getVersion().toString());
        }
        return versions;
    }

    @Override
    public List<String> checkBundles(List<String> symbolicNameVersions) {
        List<String> result = new ArrayList<>();
        Map<String, List<String>> n2v = new HashMap<>();

        for (String nameVersion : symbolicNameVersions) {
            String nv[] = nameVersion.split(":");
            if (nv.length == 2) {
                List<String> versions = n2v.get(nv[0]);
                if (versions == null) {
                    versions = new ArrayList<String>();
                    n2v.put(nv[0], versions);
                }

                versions.add(nv[1]);
            }
        }

        for (Bundle bundle : context.getBundles()) {
            String name = bundle.getSymbolicName();
            List<String> checkVersions = n2v.get(name);
            if (checkVersions == null)
                continue;

            Version bv = bundle.getVersion();
            String version = String.format("%d.%d.%d.%s",
                    bv.getMajor(), bv.getMinor(), + bv.getMicro(), bv.getQualifier());
            String snapshotVersion = String.format("%d.%d.%d.%s",
                    bv.getMajor(), bv.getMinor(), + bv.getMicro(), "SNAPSHOT");

            if (checkVersions.contains(version)) {
                result.add(name + ":" + version);
            }
            else if (checkVersions.contains(snapshotVersion)) {
                result.add(name + ":" + snapshotVersion);
            }
        }

        return result;
    }

    @Override
    public void deleteConfig(String id) {
        try {
            String pid = getPid(id);
            String filter = String.format("(%s=%s)", Constants.SERVICE_PID, pid);
            Configuration[] cfns = cm.listConfigurations(filter);
            for (Configuration cfn : cfns) {
                cfn.delete();
            }
        } catch (InvalidSyntaxException e) {
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public MetaConfigDTO getConfig(String id) {
        MetaConfigDTO cached = configs.get(id);

        if (cached == null) {
            getConfig();
            return configs.get(id);
        }

        String pid = cached.pid;
        String factoryPid = cached.factoryPid;
        boolean factory = cached.isFactory;

        ObjectClassDefinition ocd = factoryPid == null ? getOCD(pid) : getOCD(factoryPid);
        MetaConfigDTO template = ocd != null ? createMetatypeDTO(ocd, pid, factory) : null;
        MetaConfigDTO dto = template;

        if (!factory) {
            try {
                String filter = String.format("(%s=%s)", Constants.SERVICE_PID, pid);
                Configuration[] cfns = new Configuration[0];
                try {
                    cfns = cm.listConfigurations(filter);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                if (cfns != null && cfns.length > 0) {
                    Configuration cfn = cfns[0];
                    dto = createMetatypeDTO(cfn, template);
                }
            } catch (InvalidSyntaxException e) {
            }
        }

        if (dto != null) {
            dto.id = id;
            dto.host = hostName;
        }

        return dto;
    }


    @Override
    public synchronized Collection<MetaConfigDTO> getConfig() {
        configs.clear();

        for (Bundle bundle : context.getBundles()) {
            MetaTypeInformation mti = mts.getMetaTypeInformation(bundle);

            // String[] locales = mti.getLocales();
            String locale = null;

            List<String> pids = Arrays.asList(mti.getPids());
            List<String> factoryPids = Arrays.asList(mti.getFactoryPids());
            Set<String> allPids = new LinkedHashSet<>(pids);
            allPids.addAll(factoryPids);

            for (String pid : allPids) {
                ObjectClassDefinition ocd = mti.getObjectClassDefinition(pid, locale);
                boolean factory = factoryPids.contains(pid) || isFactory(pid);
                MetaConfigDTO dto = createMetatypeDTO(ocd, pid, factory);
                dto.id = mkId(pid, factory);
                dto.host = hostName;
                configs.put(dto.id, dto);
            }
        }

        try {
            Configuration[] configurations = cm.listConfigurations(null);

            if (configurations != null) {
                for (Configuration cfn : configurations) {
                    boolean factoryCfn = cfn.getFactoryPid() != null;
                    String templatePid = factoryCfn ? cfn.getFactoryPid() : cfn.getPid();

                    Optional<MetaConfigDTO> optTemplate = configs.values().stream()
                            .filter(d -> templatePid.equals(d.pid) && (d.isFactory || !factoryCfn))
                            .sorted((a, b) -> a.isFactory ? 1 : b.isFactory ? -1 : 0)
                            .findFirst();


                    MetaConfigDTO template = optTemplate.isPresent() ? optTemplate.get() : null;
                    MetaConfigDTO dto = createMetatypeDTO(cfn, template);

                    dto.id = mkId(dto.pid, dto.isFactory);
                    dto.host = hostName;
                    configs.put(dto.id, dto);
                }
            }
        } catch (InvalidSyntaxException e) {
            // impossible
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return configs.values();
    }

    @Override
    public void updateConfig(String pid, String factoryPid, boolean isFactory, Map<String, String[]> values) {
        try {
            Configuration cfn = null;

            if (isFactory) {
                String uuid = UUID.randomUUID().toString();
                cfn = cm.getFactoryConfiguration(pid, uuid, "?");
            } else {
                cfn = cm.getConfiguration(pid, "?");
            }

            ObjectClassDefinition ocd = factoryPid == null ? getOCD(pid) : getOCD(factoryPid);
            Hashtable<String, Object> properties = MetaConfigUtils.convert(values, ocd);

            Dictionary<String, ?> oldProps = cfn.getProperties();
            if (oldProps != null) {
                Enumeration<String> keys = oldProps.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    properties.putIfAbsent(key, oldProps.get(key));
                }
            }

            cfn.update(properties);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private MetaConfigDTO createMetatypeDTO(ObjectClassDefinition ocd, String pid, boolean factory) {
        MetaConfigDTO dto = new MetaConfigDTO();

        dto.pid = pid;
        dto.isFactory = factory;
        dto.type = factory ? MetaConfigDTO.ConfigType.Factory : MetaConfigDTO.ConfigType.NotConfigured;

        dto.name = ocd.getName();
        dto.description = ocd.getDescription();
        dto.attributes = new ArrayList<>();
        dto.values = new HashMap<>();

        AttributeDefinition[] optAttrs = ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
        AttributeDefinition[] reqAttrs = ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);

        if (optAttrs != null) {
            for (AttributeDefinition ad : optAttrs) {
                MetaConfigDTO.AttributeDTO aDto = createAttributeDTO(ad, false);
                dto.attributes.add(aDto);
                // values shouldn't be populated with defaults
                // UI can use placeholder to show default without actually setting it.
                if (aDto.type == MetaConfigDTO.AttributeType.BOOLEAN) {
                    // Can't distinguish between boolean=false and not set, so populate values
                    setValue(aDto.id, aDto.defaultValue, dto.values);
                }
            }
        }

        if (reqAttrs != null) {
            for (AttributeDefinition ad : reqAttrs) {
                // AttributeDefinition(required) defaults true, but if the field has a default
                // then there's no need to make it required, as the default will be used.
                String desc = ad.getDescription();
                boolean optional = (ad.getDefaultValue() != null ||
                        (desc != null && desc.matches("(?i).*(default|only used).*")));
                MetaConfigDTO.AttributeDTO aDto = createAttributeDTO(ad, !optional);
                dto.attributes.add(aDto);
                if (aDto.type == MetaConfigDTO.AttributeType.BOOLEAN) {
                    setValue(aDto.id, aDto.defaultValue, dto.values);
                }
            }
        }

        return dto;
    }

    private MetaConfigDTO createMetatypeDTO(Configuration cfn, MetaConfigDTO template) {
        Set<Configuration.ConfigurationAttribute> attributes = cfn.getAttributes();
        Dictionary<String, Object> properties = cfn.getProperties();

        MetaConfigDTO dto = new MetaConfigDTO();
        dto.pid = cfn.getPid();
        dto.factoryPid = cfn.getFactoryPid();
        dto.attributes = new ArrayList<>();
        dto.values = new HashMap<>();

        if (template == null) {
            dto.type = MetaConfigDTO.ConfigType.UnknownConfigured;
            dto.name = dto.pid;
            dto.description = "This form is automatically generated from existing properties" +
                    " because there is no MetaType descriptor for this configuration.";
        } else if (template.isFactory) {
            dto.type = dto.factoryPid != null ? MetaConfigDTO.ConfigType.FactoryConfigured : MetaConfigDTO.ConfigType.MetatypeConfigured;
            dto.name = template.name;
            dto.description = template.description;
            dto.attributes = template.attributes; // no need to clone, as not modified

            for (MetaConfigDTO.AttributeDTO ad : template.attributes) {
                setValue(ad.id, ad.defaultValue, dto.values);
            }
        } else {
            dto = template;
            dto.type = MetaConfigDTO.ConfigType.MetatypeConfigured;

            // we have a configuration, so no missing attributes can be required
            dto.attributes.stream().forEach(a -> {
                if (properties.get(a.id) == null) {
                    a.required = false;
                }
            });
        }

        dto.hasConfiguration = true;

        Enumeration<String> keys = properties.keys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object value = properties.get(key);

            // ignore well known special properties
            switch (key) {
                case Constants.SERVICE_PID:
                case Constants.SERVICE_DESCRIPTION:
                case Constants.SERVICE_ID:
                case Constants.SERVICE_VENDOR:
                case ConfigurationAdmin.SERVICE_BUNDLELOCATION:
                case ConfigurationAdmin.SERVICE_FACTORYPID:
                    continue;
            }

            // ignore "hidden" keys
            if (key.startsWith(".com.paremus") || key.startsWith("com.paremus.nimble")) {
                continue;
            }

            String[] defaultValue;

            if (value instanceof Collection) {
                value = ((Collection) value).toArray();
            }

            if (value.getClass().isArray()) {
                Object[] aValue = (Object[]) value;
                defaultValue = new String[aValue.length];
                for (int i = 0; i < aValue.length; i++) {
                    defaultValue[i] = String.valueOf(aValue[i]);
                }
            } else {
                defaultValue = new String[]{value.toString()};
            }

            setValue(key, defaultValue, dto.values);

            boolean exists = dto.attributes.stream().anyMatch(a -> a.id.equals(key));

            if (!exists) {
                MetaConfigDTO.AttributeDTO aDto = new MetaConfigDTO.AttributeDTO();
                aDto.id = key;
                aDto.defaultValue = defaultValue;
                aDto.cardinality = value.getClass().isArray() ? defaultValue.length : 0;
                aDto.type = MetaConfigDTO.AttributeType.STRING;
                dto.attributes.add(aDto);
            }
        }

        return dto;
    }


    private MetaConfigDTO.AttributeDTO createAttributeDTO(AttributeDefinition ad, boolean required) {
        MetaConfigDTO.AttributeDTO dto = new MetaConfigDTO.AttributeDTO();

        dto.id = ad.getID();
        dto.name = ad.getName();

        dto.description = ad.getDescription();
        dto.cardinality = ad.getCardinality();
        dto.type = MetaConfigDTO.AttributeType.from(ad.getType());
        dto.defaultValue = ad.getDefaultValue();
        dto.required = required;

        String[] optionLabels = ad.getOptionLabels();
        String[] optionValues = ad.getOptionValues();

        if (optionLabels != null && optionLabels.length > 0) {
            dto.options = new HashMap<>();
            for (int i = 0; i < optionLabels.length; i++) {
                dto.options.put(optionValues[i], optionLabels[i]);
            }
        }

        return dto;
    }


    private ObjectClassDefinition getOCD(String pid) {
        for (Bundle bundle : context.getBundles()) {
            MetaTypeInformation mti = mts.getMetaTypeInformation(bundle);

            List<String> pids = Arrays.asList(mti.getPids());
            List<String> factoryPids = Arrays.asList(mti.getFactoryPids());
            Set<String> allPids = new HashSet<>(pids);
            allPids.addAll(factoryPids);

            if (allPids.contains(pid)) {
                return mti.getObjectClassDefinition(pid, null);
            }
        }
        return null;
    }

    private String mkId(String pid, boolean factory) {
        // a configuration factory and a non-factory configuration may have the same pid
        // we want a unique id, so prefix factory configuration with !
        String id = (factory ? "!" : "") + pid;

        // id needs to be safe to use in URI path
        // i.e. GET /metatype/:id, so it can't contain '?' which starts a query
        // URI encoding won't help as the client decodes before calling GET
        id = id.replace('?', '@');

        return id + "~" + frameworkId;
    }

    private String getPid(String id) {
        int i = id.lastIndexOf('~');
        return i > 0 ? id.substring(0, i) : null;
    }

    private boolean isFactory(String pid) {
        try {
            String filter = String.format("(%s=%s)", Constants.SERVICE_PID, pid);
            return !context.getServiceReferences(ManagedServiceFactory.class, filter).isEmpty();
        } catch (InvalidSyntaxException e) {
            return false;
        }
    }

    private void setValue(String id, String[] value, Map<String, String[]> values) {
        if (value != null) {
            values.put(id, value);
        }
    }

}
