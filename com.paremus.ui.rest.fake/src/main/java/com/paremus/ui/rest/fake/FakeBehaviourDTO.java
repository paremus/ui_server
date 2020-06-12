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

public class FakeBehaviourDTO extends IdentityDTO {
    public String name;
    public String description;
    public String author;
    public String consumed;
    public String bundle;
    public String version;
    public Collection<String> hosts;
    public String installHost;
}
