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

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static com.paremus.ui.metaconfig.MetaConfigDTO.AttributeType;

public class MetaConfigUtils {

    public static Hashtable<String, Object> convert(Map<String, String[]> values, ObjectClassDefinition ocd) {
        Map<String, AttributeDefinition> id2ad = new HashMap<>();

        if (ocd != null) {
            for (AttributeDefinition ad : ocd.getAttributeDefinitions(ObjectClassDefinition.ALL)) {
                id2ad.put(ad.getID(), ad);
            }
        }

        Hashtable<String, Object> properties = new Hashtable<>();

        for (String id : values.keySet()) {
            String[] value = values.get(id);

            if (id2ad.isEmpty()) {
                properties.put(id, value);
            } else {
                AttributeDefinition ad = id2ad.get(id);
                if (ad == null) {
                    throw new IllegalArgumentException(String.format("(%s) unknown property", id));
                }

                String sValue = String.join(",", value);

                if (sValue.isEmpty() || sValue.equals("null")) {
                    if (ad.getDefaultValue() != null || sValue.equals("null")) {
                        continue; // delete the property, so default value is used
                    }
                } else {
                    String valid = ad.validate(sValue);
                    if (valid != null && !valid.isEmpty()) {
                        throw new IllegalArgumentException(String.format("(%s=%s) %s", id, sValue, valid));
                    }
                }

                properties.put(id, toValue(value, AttributeType.from(ad.getType()), ad.getCardinality()));
            }
        }

        return properties;
    }

    static Object toValue(String[] value, AttributeType type, int cardinality) {
        if (cardinality == 0) {
            return toValue(value[0], type);
        }

        int max = cardinality > 0 ? cardinality : -cardinality;
        int size = value.length > max ? max : value.length;
        Object[] oValue = newArray(type, size);

        for (int i = 0; i < size; i++) {
            oValue[i] = toValue(value[i], type);
        }

        return (cardinality > 0) ? oValue : Arrays.asList(oValue);
    }

    static Object toValue(String value, AttributeType type) {
        switch (type) {
            case BOOLEAN:
                return Boolean.parseBoolean(value);
            case BYTE:
                return Byte.parseByte(value);
            case CHARACTER:
                return value.charAt(0);
            case DOUBLE:
                return Double.parseDouble(value);
            case FLOAT:
                return Float.parseFloat(value);
            case INTEGER:
                return Integer.parseInt(value);
            case LONG:
                return Long.parseLong(value);
            case SHORT:
                return Short.parseShort(value);
            case STRING:
            case PASSWORD:
                return value;
            default:
                return null;
        }
    }

    static Object[] newArray(AttributeType type, int size) {
        switch (type) {
            case BOOLEAN:
                return new Boolean[size];
            case BYTE:
                return new Byte[size];
            case DOUBLE:
                return new Double[size];
            case FLOAT:
                return new Float[size];
            case INTEGER:
                return new Integer[size];
            case LONG:
                return new Long[size];
            case SHORT:
                return new Short[size];
            case CHARACTER:
            case STRING:
            case PASSWORD:
                return new String[size];
            default:
                return null;
        }
    }
}
