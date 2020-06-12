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
package com.paremus.ui.rest.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.osgi.util.converter.ConversionException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.osgi.util.converter.Converters.standardConverter;

/**
 * implements filtering defined in react-admin simple rest provider
 *
 * @see <a href="https://github.com/marmelab/react-admin/tree/master/packages/ra-data-simple-rest">Simple Rest Provider</a>
 */
public class ResourceFilter {
    private static final String OP_GT = "gt";
    private static final String OP_GTE = "gte";
    private static final String OP_LT = "lt";
    private static final String OP_LTE = "lte";
    private static final String OP_IN = "in";
    private static final String OP_EQ = "eq";
    private static final List<String> ops = Arrays.asList(OP_GT, OP_GTE, OP_LT, OP_LTE);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Field> dtoFields = new HashMap<>();

    private final Map<String, Object> filter;
    private final Map<String, String> filterOp = new HashMap<>();
    final private Class<?> clazz;

    public ResourceFilter(Class<?> clazz, String qFilter) {
        this.clazz = clazz;
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        for (Field f : clazz.getFields()) {
            dtoFields.put(f.getName(), f);
        }
        dtoFields.put("q", null);

        try {
            filter = new ConcurrentHashMap<>(mapper.readValue(qFilter, Map.class));
        } catch (IOException e) {
            throw new IllegalArgumentException("bad filter map: " + qFilter);
        }

        for (String fkey : filter.keySet()) {
            if (getFieldOp(fkey) == null) {
                throw new IllegalArgumentException("bad filter field: " + fkey);
            }
        }
    }

    private String[] getFieldOp(String field) {
        if (dtoFields.containsKey(field)) {
            return new String[]{field, OP_EQ};
        }

        int i = field.lastIndexOf('_');

        if (i > 0) {
            String fname = field.substring(0, i);
            String op = field.substring(i + 1);
            if (ops.contains(op) && dtoFields.containsKey(fname)) {
                return new String[]{fname, op};
            }
        }

        return null;
    }

    public List<String> getFilters() {
        List<String> filters = new ArrayList<>();
        for (String fkey : filter.keySet()) {
            Object match = filter.get(fkey);
            String[] fieldOp = getFieldOp(fkey);
            String fname = fieldOp[0];
            String op = match instanceof List ? OP_IN : fieldOp[1];

            String sMatch = match != null ? match.toString() : "";
            StringBuilder ldap = new StringBuilder();

            if (fkey.equals("q")) {
                if (!sMatch.isEmpty()) {
                    if (sMatch.startsWith("("))
                        sMatch = "." + sMatch;
                    filters.add(sMatch); // RegEx filter
                }
            } else {
                try {
                    Field field = dtoFields.get(fname);
                    Class<?> type = field.getType();
                    if (type.isAssignableFrom(Instant.class)) {
                        match = Instant.parse(sMatch).getEpochSecond();
                        fname += ".epochSecond";
                    }
                } catch (Exception e) {
                }

                String ldap_op;
                switch (op) {
                    case OP_GT:
                        ldap_op = "!<=";
                        break;
                    case OP_GTE:
                        ldap_op = ">=";
                        break;
                    case OP_LT:
                        ldap_op = "!>=";
                        break;
                    case OP_LTE:
                        ldap_op = "<=";
                        break;
                    case OP_IN:
                        ldap_op = null;
                        break;
                    default:
                        ldap_op = "=";
                        break;
                }

                if (ldap_op == null) {
                    String name = fname;
                    ((List<?>) match).forEach(in -> {
                        ldap.append('(').append(name).append('=').append(in).append(')');
                    });
                    ldap.append(")");
                } else if (ldap_op.startsWith("!")) {
                    ldap_op = ldap_op.substring(1);
                    ldap.append("(!");
                    ldap.append('(').append(fname).append(ldap_op).append(match).append(')');
                    ldap.append(")");
                } else {
                    ldap.append('(').append(fname).append(ldap_op).append(match).append(')');
                }

                if (ldap.length() > 0) {
                    ldap.insert(0, "(&").append(')');
                    filters.add(ldap.toString());
                }
            }
        }
        return filters;
    }

    public boolean accept(Object dto) {
        if (!clazz.isInstance(dto)) {
            return false;
        }

        for (String fkey : filter.keySet()) {
            Object match = filter.get(fkey);
            String[] fieldOp = getFieldOp(fkey);
            String fname = fieldOp[0];
            String op = match instanceof List ? OP_IN : fieldOp[1];

            if (fkey.equals("q")) {
                // special "q" filter makes a full-text search on all text fields
                String sMatch = match != null ? match.toString() : "";
                if (sMatch.isEmpty())
                    continue;

                Pattern pMatch = null;
                try {
                    pMatch = Pattern.compile(sMatch);
                } catch (PatternSyntaxException e) {
                }

                boolean matched = false;

                for (Field f : dtoFields.values()) {
                    if (f != null) {
                        String sValue = null;
                        Object oValue = null;
                        try {
                            oValue = f.get(dto);
                        } catch (IllegalAccessException e) {
                            return false;
                        }

                        if (oValue instanceof String) {
                            sValue = (String) oValue;
                        } else {
                            try {
                                // avoid calling Object.toString()
                                oValue.getClass().getDeclaredMethod("toString");
                                sValue = oValue.toString();
                            } catch (Exception e) {
                                try {
                                    sValue = mapper.writeValueAsString(oValue);
                                    // remove json quotes for easier searching
                                    sValue = sValue.replaceAll("\"", "");
                                } catch (JsonProcessingException ex) {
                                    sValue = String.valueOf(oValue);
                                }
                            }
                        }
                        if ((pMatch != null && pMatch.matcher(sValue).find())
                                || (pMatch == null && sValue.contains(sMatch))) {
                            matched = true;
                            break;
                        }
                    }
                }
                if (!matched)
                    return false;
            } else {
                Object value = null;
                try {
                    Field field = dtoFields.get(fname);
                    value = field.get(dto);
                } catch (IllegalAccessException e) {
                    return false;
                }

                boolean matched = false;

                try {
                    switch (op) {
                        case OP_GT:
                            matched = compare(value, match) > 0;
                            break;
                        case OP_GTE:
                            matched = compare(value, match) >= 0;
                            break;
                        case OP_LT:
                            matched = compare(value, match) < 0;
                            break;
                        case OP_LTE:
                            matched = compare(value, match) <= 0;
                            break;
                        case OP_IN:
                            matched = ((List) match).contains(value);
                            break;
                        default:
                            matched = match.equals(value);
                            break;
                    }
                } catch (ConversionException e) {
                }

                if (!matched)
                    return false;
            }
        }

        return true;
    }


    private int compare(Object value, Object match) {
        if (!(value instanceof Comparable)) {
            throw new ConversionException("value not Comparable: " + value);
        }

        Comparable<Object> cmp = (Comparable) value;
        Class<?> cmpClass = cmp.getClass();

        if (cmpClass.isInstance(match)) {
            return cmp.compareTo(match);
        }

        try {
            return cmp.compareTo(standardConverter().convert(match).to(cmpClass));
        } catch (ConversionException e) {
            try {
                Instant instant = Instant.parse(match.toString());
                return cmp.compareTo(standardConverter().convert(instant).to(cmpClass));
            } catch (DateTimeParseException e2) {
            }

            throw e;
        }
    }
}
