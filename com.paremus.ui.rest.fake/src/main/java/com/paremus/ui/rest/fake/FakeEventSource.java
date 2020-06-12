/*-
 * #%L
 * com.paremus.ui.rest.fake
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

import com.paremus.ui.rest.api.EventApi;
import com.paremus.ui.rest.dto.EventDTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.time.Instant;
import java.util.Map;

@Component(service = FakeEventSource.class, immediate = true)
public class FakeEventSource {
    @Reference
    private EventApi eventApi;
    private Thread thread;
    private boolean stop = false;

    private Runnable runner = () -> {
        while (!stop) {
            try {
                int index = (int) (Math.random() * classes.length);
                PayloadBase payload = (PayloadBase) classes[index].getConstructor().newInstance();

                EventDTO event = new EventDTO();
                event.source = payload.source;
                event.timestamp = payload.timestamp;
                event.topic = payload.getClass().getName();
                event.data = payload;
                eventApi.add(event);

                int millis = (int) (Math.random() * 10000);
                Thread.sleep(millis);
            } catch (Exception e) {
                System.err.println("SSE: send loop ignored: " + e);
            }
        }
    };

    @Activate
    private void activate() {
        thread = new Thread(runner);
        thread.start();
    }

    @Deactivate
    private void deactivate() {
        stop = true;
        thread.interrupt();
    }

    public static int bids[] = new int[]{-1, 0, 0, 0, 0, 1, 4, 32, 128};
    private static String[] sources = {"node1", "node2", "node3", "node4"};
    private static String[] targets = {"node10", "node11", "node12", "node13"};
    private static String[] messages = {"each", "peach", "pear", "plum"};
    private static String[] types = {"LightCommand", "SensorReading", "SecurityAlert"};

    static class PayloadBase {
        public String source;
        public Instant timestamp;

        public PayloadBase() {
            int index = (int) (Math.random() * sources.length);
            source = sources[index];
            timestamp = Instant.now();
        }
    }

    static class ManagementAlert extends PayloadBase {
        public enum AlertType {
            CONSUMER_NOT_FOUND,
            NO_HOSTS,
            INSTALL_FAILED,
            CONSUMER_NOT_CONFIGURED;
        }

        public AlertType alert;
        public String message;
        public String eventType;
        public Map<String, Object> properties;

        private static AlertType[] alerts = AlertType.values();

        public ManagementAlert() {
            int ai = (int) (Math.random() * alerts.length);
            alert = alerts[ai];
            int mi = (int) (Math.random() * messages.length);
            message = messages[mi];
            int ei = (int) (Math.random() * types.length);
            eventType = types[ei];
        }
    }

    static class ManagementBidRequest extends PayloadBase {
        public String eventType;
        public Map<String, Object> eventData;

        public ManagementBidRequest() {
            int ei = (int) (Math.random() * types.length);
            eventType = types[ei];
        }
    }

    static class ManagementInstallRequest extends PayloadBase {
        public String targetNode;
        public String eventType;
        public Map<String, Object> eventData;

        public ManagementInstallRequest() {
            int ti = (int) (Math.random() * targets.length);
            targetNode = targets[ti];
            int ei = (int) (Math.random() * types.length);
            eventType = types[ei];
        }
    }

    static class ManagementBidResponse extends PayloadBase {
        public enum ResponseCode {
            BID,
            ALREADY_INSTALLED,
            INSTALL_OK,
            FAIL;
        }

        public ResponseCode code;
        public Integer bid;
        public String eventType;
        public String targetNode;


        private static ResponseCode[] codes = ResponseCode.values();

        public ManagementBidResponse() {
            int bi = (int) (Math.random() * bids.length);
            bid = bids[bi];
            int ci = (int) (Math.random() * codes.length);
            code = codes[ci];
            int ti = (int) (Math.random() * targets.length);
            targetNode = targets[ti];
            int ei = (int) (Math.random() * types.length);
            eventType = types[ei];
        }
    }

    static Class[] classes = new Class[]{
            ManagementAlert.class,
            ManagementBidRequest.class,
            ManagementBidResponse.class,
            ManagementBidResponse.class,
            ManagementBidResponse.class,
            ManagementInstallRequest.class,
    };

}
