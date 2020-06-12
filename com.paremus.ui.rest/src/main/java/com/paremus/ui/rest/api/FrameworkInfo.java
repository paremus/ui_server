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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface FrameworkInfo {
    String getFrameworkId(String hostname);

    String getHostname(String frameworkId);

    Map<String, String> getHostInfo(String frameworkId);

    List<String> getBundleHosts(String symbolicName, String version);

    Map<String, List<String>> getHosts(List<String> symbolicNameVersions);

    Map<String, String> getHostMap();
}
