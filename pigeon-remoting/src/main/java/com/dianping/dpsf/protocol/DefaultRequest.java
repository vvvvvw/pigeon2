/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.dpsf.protocol;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.dianping.pigeon.config.ConfigManagerLoader;
import com.dianping.pigeon.remoting.common.domain.InvocationRequest;
import com.dianping.pigeon.remoting.common.util.Constants;
import com.dianping.pigeon.remoting.common.util.InvocationUtils;
import com.dianping.pigeon.remoting.invoker.config.InvokerConfig;
import com.dianping.pigeon.remoting.invoker.domain.InvokerContext;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "seq", scope = DefaultRequest.class)
public class DefaultRequest implements InvocationRequest {

	/**
	 * 不能随意修改！
	 */
	private static final long serialVersionUID = 652592942114047764L;

	private byte serialize;

	@JsonProperty("seq")
	private long seq;

	private int callType = Constants.CALLTYPE_REPLY;

	private int timeout = 0;

	@JsonIgnore
	private transient long createMillisTime;

	@JsonProperty("url")
	private String serviceName;

	private String methodName;

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
	private Object[] parameters;

	private int messageType = Constants.MESSAGE_TYPE_SERVICE;

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
	private Object context;

	private String version;

	private String app = ConfigManagerLoader.getConfigManager().getAppName();

	@JsonIgnore
	private transient int size;

	private Map<String, Serializable> globalValues = null;

	private Map<String, Serializable> requestValues = null;

	public DefaultRequest(String serviceName, String methodName, Object[] parameters, byte serialize, int messageType,
						  int timeout, int callType, long seq) {
		this.serviceName = serviceName;
		this.methodName = methodName;
		this.parameters = parameters;
		this.serialize = serialize;
		this.messageType = messageType;
		this.timeout = timeout;
		this.callType = callType;
		this.seq = seq;
	}

	public DefaultRequest(String serviceName, String methodName, Object[] parameters, byte serialize, int messageType,
			int timeout, Class<?>[] parameterClasses) {
		this.serviceName = serviceName;
		this.methodName = methodName;
		this.parameters = parameters;
		this.serialize = serialize;
		this.messageType = messageType;
		this.timeout = timeout;
	}

	public DefaultRequest() {
	}

	public DefaultRequest(InvokerContext invokerContext) {
		if (invokerContext != null) {
			InvokerConfig<?> invokerConfig = invokerContext.getInvokerConfig();
			if (invokerConfig != null) {
				this.serviceName = invokerConfig.getUrl();
				this.serialize = invokerConfig.getSerialize();
				this.timeout = invokerConfig.getTimeout();
				this.setVersion(invokerConfig.getVersion());
				if (Constants.CALL_ONEWAY.equalsIgnoreCase(invokerConfig.getCallType())) {
					this.setCallType(Constants.CALLTYPE_NOREPLY);
				} else {
					this.setCallType(Constants.CALLTYPE_REPLY);
				}
			}
			this.methodName = invokerContext.getMethodName();
			this.parameters = invokerContext.getArguments();
			this.messageType = Constants.MESSAGE_TYPE_SERVICE;
		}
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public byte getSerialize() {
		return this.serialize;
	}

	public void setSequence(long seq) {
		this.seq = seq;
	}

	public long getSequence() {
		return this.seq;
	}

	public Object getObject() {
		return this;
	}

	public void setCallType(int callType) {
		this.callType = callType;
	}

	public int getCallType() {
		return this.callType;
	}

	public int getTimeout() {
		return this.timeout;
	}

	public long getCreateMillisTime() {
		return this.createMillisTime;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public String[] getParamClassName() {
		if (this.parameters == null) {
			return new String[0];
		}
		String[] paramClassNames = new String[this.parameters.length];

		int k = 0;
		for (Object parameter : this.parameters) {
			if (parameter == null) {
				paramClassNames[k] = "NULL";
			} else {
				paramClassNames[k] = this.parameters[k].getClass().getName();
			}
			k++;
		}
		return paramClassNames;
	}

	public Object[] getParameters() {
		return this.parameters;
	}

	public int getMessageType() {
		return this.messageType;
	}

	@Override
	public Object getContext() {
		return this.context;
	}

	@Override
	public void setContext(Object context) {
		this.context = context;
	}

	@Override
	public void setCreateMillisTime(long createTime) {
		this.createMillisTime = createTime;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("serialize", serialize).append("seq", seq).append("msgType", messageType)
				.append("callType", callType).append("timeout", timeout).append("url", serviceName)
				.append("method", methodName).append("created", createMillisTime);
		if (Constants.LOG_PARAMETERS) {
			builder.append("parameters", InvocationUtils.toJsonString(parameters));
		}

		return builder.toString();
	}

	@Override
	public void setSerialize(byte serialize) {
		this.serialize = serialize;
	}

	@Override
	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	@Override
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Map<String, Serializable> getGlobalValues() {
		return globalValues;
	}

	public void setGlobalValues(Map<String, Serializable> globalValues) {
		this.globalValues = globalValues;
	}

	public Map<String, Serializable> getRequestValues() {
		return requestValues;
	}

	public void setRequestValues(Map<String, Serializable> requestValues) {
		this.requestValues = requestValues;
	}

}
