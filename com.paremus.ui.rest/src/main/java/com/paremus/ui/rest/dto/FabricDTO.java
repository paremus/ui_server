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
package com.paremus.ui.rest.dto;

import java.util.Collection;

public class FabricDTO extends IdentityDTO {
    public static class SystemDTO {
        public String id;
        public String version;
        public boolean running;
    }
    public String location;
    public String owner;
    public int actualFibres;
    public int expectedFibres;
    public Collection<String> managementURIs;
    public Collection<SystemDTO> systems;
}
