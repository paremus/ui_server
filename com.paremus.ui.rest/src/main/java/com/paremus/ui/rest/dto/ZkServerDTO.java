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

public class ZkServerDTO {
    // unique id
    public int id;

    // server id within ensemble
    public int serverId;

    public int clientPort;
    public int quorumPort;
    public int transactionPort;

    // ensemble to which server belongs
    public String ensemble_id;

    // fibre on which server is hosted
    public String fibre_id;

    public String address;
    public String clientBindAddress;

    public String managementStatus;
    public String currentRole;
    public String role;
}
