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
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implements GET filter, range and sort parameters and sets Content-Range response header.
 * Implements OPTIONS to set CORS response headers.
 *
 * @see <a href=https://github.com/marmelab/FakeRest>FakeRest</a>
 */
public abstract class AbstractResource<T> {

    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private static final String CONTENT_RANGE = "Content-Range";

    protected abstract Collection<T> getDtos();

    protected abstract T getDto(String id);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Field> dtoFields = new HashMap<>();

    // see Double.valueOf(String)
    final String Digits = "(\\p{Digit}+)";
    final String Exp = "[eE][+-]?" + Digits;
    final String fpRegex =
            ("[\\x00-\\x20]*" +  // Optional leading whitespace
                    "([+-]?" + // Optional sign character
                    "(" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +
                    "(\\.(" + Digits + ")(" + Exp + ")?)" +
                    ")[\\D]*"); // no other digits

    final Pattern fpPat = Pattern.compile(fpRegex);

    protected final Class<T> clazz;

    protected AbstractResource(Class<T> clazz) {
        this.clazz = clazz;
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        for (Field f : clazz.getFields()) {
            dtoFields.put(f.getName(), f);
        }
    }

    @GET
    public Collection<T> getDtos(
            @Context HttpServletResponse response,
            @QueryParam("filter") String qFilter,
            @QueryParam("range") String qRange,
            @QueryParam("sort") String qSort) {

        ResourceFilter filter = null;
        Integer[] range = null;
        String[] sort = null;
        String sortKey = null;

        try {
            if (qFilter != null) {
                filter = new ResourceFilter(clazz, qFilter);
            }

            if (qRange != null) {
                range = mapper.readValue(qRange, Integer[].class);
                if (range.length != 2 || range[0] < 0 || range[0] > range[1]) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad range: " + qRange);
                    return null;
                }
            }

            if (qSort != null) {
                sort = mapper.readValue(qSort, String[].class);
                sortKey = sort.length > 0 ? sort[0] : "";

                if (!dtoFields.containsKey(sortKey)) {
                    sortKey = sortKey.replaceFirst("[.].*", "");
                    if (!dtoFields.containsKey(sortKey)) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad sort field: " + sort[0]);
                        return null;
                    }
                }
            }

        } catch (IOException e) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (IOException e1) {
            }
            return null;
        }

        List<T> result = new ArrayList<>(getDtos());

        if (filter != null) {
            result = result.stream().filter(filter::accept).collect(Collectors.toList());
        }

        if (sort != null) {
            Field f = dtoFields.get(sortKey);
            int direction = sort.length == 1 ? 1 : "desc".equalsIgnoreCase(sort[1]) ? -1 : 1;
            boolean subSort = !sort[0].equals(sortKey);
            String subKey = sort[0].replaceFirst("[^.]*[.]", "");
            result.sort((a, b) -> {
                try {
                    Object ao = f.get(a);
                    Object bo = f.get(b);
                    if (subSort) {
                        if (ao instanceof Map) {
                            ao = ((Map) ao).get(subKey);
                        }
                        if (bo instanceof Map) {
                            bo = ((Map) bo).get(subKey);
                        }
                    }
                    if (ao instanceof String) {
                        Matcher matcher = fpPat.matcher((String) ao);
                        if (matcher.matches()) {
                            ao = Double.valueOf(matcher.group(1));
                        }
                    }
                    if (bo instanceof String) {
                        Matcher matcher = fpPat.matcher((String) bo);
                        if (matcher.matches()) {
                            bo = Double.valueOf(matcher.group(1));
                        }
                    }
                    Comparable ac = (ao instanceof Comparable) ? (Comparable) ao : String.valueOf(ao);
                    Comparable bc = (bo instanceof Comparable) ? (Comparable) bo : String.valueOf(bo);
                    return direction * ac.compareTo(bc);
                } catch (IllegalAccessException e) {
                    return 0;
                }
            });
        }

        int items = result.size();

        if (range != null) {
            response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_RANGE);

            if (range[0] >= items && range[0] > 0) {
                String contentLength = HttpHeaders.CONTENT_LENGTH;
                response.addHeader(CONTENT_RANGE, "*/" + items);
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return null;
            }

            if (range[1] >= items) {
                range[1] = items - 1;
            }

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            String contentRange = String.format("items %d-%d/%d", range[0], range[1] < 0 ? 0 : range[1], items);
            response.addHeader(CONTENT_RANGE, contentRange);
            result = result.subList(range[0], range[1] + 1);
        }

        return result;
    }

    @GET
    @Path("{id}")
    public T getDto(@Context HttpServletResponse response, @PathParam("id") String id) {
        T dto = getDto(id);
        if (dto == null)
            throw new NotFoundException();

        return dto;
    }

}
