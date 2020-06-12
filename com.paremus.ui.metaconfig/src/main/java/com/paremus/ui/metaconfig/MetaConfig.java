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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MetaConfig {

    String getFrameworkId();

    String getHostname();

    Map<String, String> getHostInfo();

    List<String> getBundleVersions(String symbolicName);

    List<String> checkBundles(List<String> symbolicNameVersions);

    void deleteConfig(String id);

    MetaConfigDTO getConfig(String id);

    Collection<MetaConfigDTO> getConfig();

    void updateConfig(String pid, String factoryPid, boolean isFactory, Map<String, String[]> values);

}
