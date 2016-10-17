package com.dianping.pigeon.util;

import org.apache.commons.lang.StringUtils;

/**
 * Created by chenchongze on 16/4/15.
 */
public class ServiceUtils {

    //serviceId为serviceName + ":" + group
    public static String getServiceId(String serviceName, String group) {
        String serviceId = serviceName;
        if (StringUtils.isNotBlank(group)) {
            serviceId = serviceId + ":" + group;
        }
        return serviceId;
    }
}
