/*-
 * #%L
 * com.paremus.ui.rest.config
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
package com.paremus.ui.rest.metaconfig.test;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Arrays;

/**
 * Configuration for mock security behaviour to demonstrate available metatype annotations.
 */
@Component(configurationPid = MockSecurityBehaviour.PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Designate(ocd = MockSecurityBehaviour.Config.class)
public class MockSecurityBehaviour {
    final static String PID = "test.security.behaviour";

    @ObjectClassDefinition(
            name = "Smart Security Behaviour",
            description = "Configuration for the Smart Security Behaviour." +
                    " This is a mock service to demonstrate configuration types."
    )
    @interface Config {
        @AttributeDefinition(type = AttributeType.INTEGER,
                name = "Time on",
                description = "How long light stays on after sensor is triggered (seconds)",
                min = "10",
                max = "300"
        )
        int duration() default 60;

        @AttributeDefinition(
                description = "Colour each light emits when activated"
        )
        LightColour colour() default LightColour.WHITE;

        @AttributeDefinition(
                name = "Sensor-id to Light-id mapping",
                description = "Each mapping should be specified on a new line as 'name=value'",
                cardinality = Integer.MAX_VALUE
        )
        String[] sensor2light();

        @AttributeDefinition(
                name = "User name",
                description = "The name of the user allowed to access the Management Console",
                required = false
        )
        String user() default "admin";

        @AttributeDefinition(type = AttributeType.PASSWORD,
                description = "The password for the user allowed to access the Management Console",
                required = false
        )
        String _password();
    }

    @Activate
    protected void activate(Config cfg) {
        System.err.println("MockSecurityBehaviour: activate: " + toString(cfg));
    }

    @Modified
    protected void modified(Config cfg) {
        System.err.println("MockSecurityBehaviour: modified: " + toString(cfg));
    }

    @Deactivate
    protected void deactivate(Config cfg) {
        System.err.println("MockSecurityBehaviour: deactivate: " + toString(cfg));
    }

    private String toString(Config cfg) {
        return String.format("duration=%d, colour=%s password=%s sensor2light=%s",
                cfg.duration(), cfg.colour(), cfg._password(),
                Arrays.asList(cfg.sensor2light()));
    }
}


