/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.pigeon.remoting.common.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import com.dianping.pigeon.config.ConfigManager;
import com.dianping.pigeon.config.ConfigManagerLoader;

public class InvocationUtils {

	private static ConcurrentHashMap<String, String> remoteCallNameCache = new ConcurrentHashMap<String, String>();

	private static ConfigManager configManager = ConfigManagerLoader.getConfigManager();

	private static final int defaultStrMaxLength = configManager.getIntValue(Constants.KEY_STRING_MAXLENGTH,
			Constants.DEFAULT_STRING_MAXLENGTH);

	private static final int defaultStrMaxItems = configManager.getIntValue(Constants.KEY_STRING_MAXITEMS,
			Constants.DEFAULT_STRING_MAXITEMS);

	public static String toJsonString(Object obj) {
		return StringizerUtils.forJson().from(obj, defaultStrMaxLength, defaultStrMaxItems);
	}

	public static String toJsonString(Object obj, int strMaxLength, int strMaxItems) {
		return StringizerUtils.forJson().from(obj, strMaxLength, strMaxItems);
	}
    //方法名加参数 ${methodName}(${parameterType1},${parameterType2}...)（parameterType是全限定类名）
	public static String getRemoteCallFullName(String methodName, Object[] parameters) {
		if (parameters != null) {
			StringBuilder str = new StringBuilder(methodName).append("(");
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i] == null) {
					str.append("null,");
				} else {
					str.append(parameters[i].getClass().getName()).append(",");
				}
			}
			if (parameters.length > 0) {
				str.deleteCharAt(str.length() - 1);
			}
			str.append(")");
			return str.toString();
		} else {
			return methodName;
		}
	}
	//根据serviceName和methodName和parameterTypes得到remoteCallNameCache存储的RemoteCallFullName或者生成后再存储
	public static String getRemoteCallFullName(String serviceName, String methodName, Class<?>[] parameterTypes) {
		if (parameterTypes != null) {
			String[] parameterTypes_ = new String[parameterTypes.length];
			for (int i = 0; i < parameterTypes.length; i++) {
				parameterTypes_[i] = parameterTypes[i].getSimpleName();
			}
			return getRemoteCallFullName(serviceName, methodName, parameterTypes_);
		} else {
			return getRemoteCallFullName(serviceName, methodName, new String[0]);
		}
	}

	//从本地remoteCallNameCache缓存中得到RemoteCallFullName，如果没有，根据serviceName，methodName，parameterTypes生成后存储到remoteCallNameCache中
	public static String getRemoteCallFullName(String serviceName, String methodName, String[] parameterTypes) {
		//cacheKey: ${serviceName}#${methodName}#${parameterType1}#${parameterType2}...
		String cacheKey = new StringBuilder(serviceName).append("#").append(methodName).append("#")
				.append(StringUtils.join(parameterTypes, "#")).toString();
		String name = remoteCallNameCache.get(cacheKey);
		if (name == null) {
			//使用/来分隔serviceName字符串
			List<String> serviceFrags = SplitterUtils.by("/").noEmptyItem().split(serviceName);
			int fragLenght = serviceFrags.size();
			name = "Unknown";
			StringBuilder sb = new StringBuilder(128);
			if (fragLenght > 2) {
				//生成${serviceFrags[len-2]}:${serviceFrags[len-1]}:${methodName}(${parameterType1},${parameterType2}...)（parameterType都是SimpleName）
				sb.append(serviceFrags.get(fragLenght - 2)).append(':').append(serviceFrags.get(fragLenght - 1))
						.append(':').append(methodName);
			} else {
				//生成${serviceName}:${methodName}(${parameterType1},${parameterType2}...)
				sb.append(serviceName).append(':').append(methodName);
			}
			sb.append('(');
			int pLen = parameterTypes.length;
			for (int i = 0; i < pLen; i++) {
				String parameter = parameterTypes[i];
				int idx = parameter.lastIndexOf(".");
				if (idx > -1) {
					parameter = parameter.substring(idx + 1);
				}
				sb.append(parameter);
				if (i < pLen - 1) {
					sb.append(',');
				}
			}
			sb.append(')');
			name = sb.toString();

			remoteCallNameCache.putIfAbsent(cacheKey, name);
		}
		return name;
	}

}
