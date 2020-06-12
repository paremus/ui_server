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
import org.osgi.service.metatype.annotations.Option;

import java.util.Arrays;

/**
 * Configuration for mock security light to demonstrate available metatype annotations.
 * This is a factory configuration and is also localised.
 */
@Component(
        configurationPid = MockSecurityLight.PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Designate(ocd = MockSecurityLight.Config.class, factory = true)
public class MockSecurityLight {
    final static String PID = "test.security.light";

    @ObjectClassDefinition(localization = "OSGI-INF/l10n/light-config",
            name = "%name",
            description = "%description"
    )
    @interface Config {
        @AttributeDefinition(type = AttributeType.BOOLEAN,
                name = "%enabled.name",
                description = "%enabled.description"
        )
        boolean enabled() default true;

        @AttributeDefinition(
                name = "%colours.name",
                description = "%colours.description",
                cardinality = Integer.MAX_VALUE,    // to allow multiple selections
                options = {
                        @Option(label = "%colour.white", value = "WHITE"),
                        @Option(label = "%colour.red", value = "RED"),
                        @Option(label = "%colour.yellow", value = "YELLOW"),
                        @Option(label = "%colour.magenta", value = "MAGENTA"),
                        @Option(label = "%colour.green", value = "GREEN"),
                        @Option(label = "%colour.blue", value = "BLUE"),
                        @Option(label = "%colour.cyan", value = "CYAN"),
                }
        )
        String[] colours() default {"RED", "YELLOW", "GREEN"};

        @AttributeDefinition(
                name = "%light2port.name",
                description = "%light2port.description",
                cardinality = 4 // to limit number of mappings
        )
        String[] light2port();
    }


    @Activate
    protected void activate(Config cfg) {
        System.err.println("MockSecurityLight: activate: " + toString(cfg));
    }

    @Modified
    protected void modified(Config cfg) {
        System.err.println("MockSecurityLight: modified: " + toString(cfg));
    }

    @Deactivate
    protected void deactivate(Config cfg) {
        System.err.println("MockSecurityLight: deactivate: " + toString(cfg));
    }

    private String toString(Config cfg) {
        return String.format("enabled=%s, colours=%s, light2port=%s", cfg.enabled(),
                Arrays.asList(cfg.colours()), Arrays.asList(cfg.light2port()));
    }
}


