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
package com.paremus.ui.metaconfig;

import org.osgi.service.metatype.AttributeDefinition;

import java.util.List;
import java.util.Map;

public class MetaConfigDTO {
    public enum AttributeType {
        BOOLEAN(AttributeDefinition.BOOLEAN),
        BYTE(AttributeDefinition.BYTE),
        CHARACTER(AttributeDefinition.CHARACTER),
        DOUBLE(AttributeDefinition.DOUBLE),
        FLOAT(AttributeDefinition.FLOAT),
        INTEGER(AttributeDefinition.INTEGER),
        LONG(AttributeDefinition.LONG),
        PASSWORD(AttributeDefinition.PASSWORD),
        SHORT(AttributeDefinition.SHORT),
        STRING(AttributeDefinition.STRING);

        private int value;

        AttributeType(int value) {
            this.value = value;
        }

        public static AttributeType from(int value) {
            for (AttributeType at : values()) {
                if (at.value == value)
                    return at;
            }
            throw new IllegalArgumentException("Invalid AttributeType ordinal: " + value);
        }
    }

    public static class AttributeDTO {
        public String id;
        public String name;
        public String description;
        public boolean required;
        public AttributeType type;

        /**
         * Cardinality determines how many values are required and how to store them.
         * If cardinality = x:
         * <pre>
         *    x = Integer.MIN_VALUE    no limit, but use List
         *    x &lt; 0                 -x = max occurrences, store in List
         *    x &gt; 0                 x = max occurrences, store in array []
         *    x = Integer.MAX_VALUE    no limit, but use array []
         *    x = 0                    1 occurrence required
         * </pre>
         */
        public int cardinality;

        public Map<String, String> options;

        public String[] defaultValue;
    }

    public String id;
    public String name;
    public String description;
    public String host;
    public List<AttributeDTO> attributes;

    public String pid;

    public String factoryPid;

    public boolean isFactory;

    /**
     * If <code>true</code> a real Configuration exists.
     * Otherwise it could be either a Managed Service or MetaType definition available but no configuration.
     */
    public boolean hasConfiguration;

    /**
     * Attribute values.
     */
    public Map<String, String[]> values;

    /**
     * Helper so UI can sort config by type.
     */
    public enum ConfigType {
        Factory, FactoryConfigured, MetatypeConfigured, NotConfigured, UnknownConfigured;
    }
    public ConfigType type;
}
