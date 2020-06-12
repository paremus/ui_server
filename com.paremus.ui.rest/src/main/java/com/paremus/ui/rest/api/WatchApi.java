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

import java.util.List;
import java.util.Observer;

public interface WatchApi {
    void register(String path, Class<?> clazz);

    void check(Object dto);

    void addObserver(String resource, Observer observer);

    void removeObserver(String resource, Observer observer);

    /**
     * get all filters for specified resource.
     *
     * @param resource the resource name
     * @return List of sub-lists containing filters for each client. Sub-list strings starting with "("
     * are LDAP expressions, otherwise they are RegEx expressions.
     */
    List<List<String>> getFilters(String resource);

}
