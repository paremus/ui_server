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
package com.paremus.ui.rest.impl;

import com.paremus.ui.rest.api.MissingServiceException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

public class MissingServiceExceptionMapper implements ExceptionMapper<MissingServiceException> {

    @Override
    public Response toResponse(MissingServiceException exception) {
        return Response.status(SERVICE_UNAVAILABLE).build();
    }

}
