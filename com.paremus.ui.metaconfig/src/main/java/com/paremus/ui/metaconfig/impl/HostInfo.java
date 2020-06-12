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
package com.paremus.ui.metaconfig.impl;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.FormatUtil;

import java.util.HashMap;
import java.util.Map;

public class HostInfo {

    public static Map<String, String> getInfo() {
        Map<String, String> info = new HashMap<>();

        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hw = si.getHardware();

            ComputerSystem computerSystem = hw.getComputerSystem();
            CentralProcessor.ProcessorIdentifier processorIdentifier = hw.getProcessor().getProcessorIdentifier();

            String model = String.format("%s [%s]", computerSystem.getManufacturer(), computerSystem.getModel());
            info.put("model", model);

            String cpu_id = processorIdentifier.getIdentifier();
            info.put("cpu_id", cpu_id);

            String cpu_name = processorIdentifier.getName();
            info.put("cpu_name", cpu_name);

            long memTotal = hw.getMemory().getTotal();
            String memory = FormatUtil.formatBytes(memTotal);
            info.put("memory", memory);

            String os = si.getOperatingSystem().toString();
            info.put("os", os);

            long max = 0;
            int count = 0;

            for (HWDiskStore disk : hw.getDiskStores()) {
                long size = disk.getSize();
                if (size > 1000L * 1000 * 1000) {
                    info.put("disk" + count++,
                            disk.getName() + " " + FormatUtil.formatBytes(size));
                    if (size > max) {
                        max = size;
                        info.put("disk", FormatUtil.formatBytes(size));
                    }
                }
            }
        }
        catch (RuntimeException e) {
            System.err.println("eek! error gettting host info: " + e);
        }

        return info;
    }
}
