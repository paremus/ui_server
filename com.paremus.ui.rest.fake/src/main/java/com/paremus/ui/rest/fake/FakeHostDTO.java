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
package com.paremus.ui.rest.fake;

import com.paremus.ui.rest.dto.IdentityDTO;

import java.util.Collection;
import java.util.Map;

public class FakeHostDTO extends IdentityDTO {
    // id is hostname
    public String fwId;

    public Map<String, String> behaviours;

    /**
     * system info:
     * <pre>
     *     model: Apple Inc.[MacBookPro11,3]
     *     memory: 7.8 GiB
     *     disk: 59.6 GiB // largest disk
     *     os: GNU/Linux Ubuntu 18.04.3 LTS (Bionic Beaver) build 4.9.184-linuxkit
     *     cpu_name: Intel(R) Core(TM) i7-4980HQ CPU @ 2.80GHz
     *     cpu_id: Intel64 Family 6 Model 70 Stepping 1
     *     disk1: /dev/sda 59.6 GiB
     *     disk2: etc
     * </pre>
     */
    public Map<String, String> info;

    /**
     * uninstall is required on PUT. It should be a key from the behaviours map, or "ALL" to reset node.
     */
    public String uninstall;
}
