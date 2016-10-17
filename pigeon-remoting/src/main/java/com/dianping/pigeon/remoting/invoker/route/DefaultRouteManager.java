/**
 * Dianping.com Inc.
 * Copyright (c) 2003-2013 All Rights Reserved.
 */
package com.dianping.pigeon.remoting.invoker.route;

import java.util.ArrayList;
import java.util.List;

import com.dianping.pigeon.config.ConfigManager;
import com.dianping.pigeon.remoting.invoker.route.quality.RequestQualityManager;
import com.dianping.pigeon.remoting.invoker.route.region.RegionPolicyManager;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.dianping.pigeon.config.ConfigManagerLoader;
import com.dianping.pigeon.log.LoggerLoader;
import com.dianping.pigeon.registry.RegistryManager;
import com.dianping.pigeon.registry.listener.RegistryEventListener;
import com.dianping.pigeon.registry.listener.ServiceProviderChangeEvent;
import com.dianping.pigeon.registry.listener.ServiceProviderChangeListener;
import com.dianping.pigeon.remoting.common.domain.Disposable;
import com.dianping.pigeon.remoting.common.domain.InvocationRequest;
import com.dianping.pigeon.remoting.common.util.Constants;
import com.dianping.pigeon.remoting.invoker.Client;
import com.dianping.pigeon.remoting.invoker.config.InvokerConfig;
import com.dianping.pigeon.remoting.invoker.exception.ServiceUnavailableException;
import com.dianping.pigeon.remoting.invoker.listener.ClusterListenerManager;
import com.dianping.pigeon.remoting.invoker.route.balance.LoadBalance;
import com.dianping.pigeon.remoting.invoker.route.balance.LoadBalanceManager;
import com.dianping.pigeon.remoting.invoker.route.balance.RandomLoadBalance;
import com.dianping.pigeon.remoting.invoker.route.balance.WeightedAutoawareLoadBalance;

public class DefaultRouteManager implements RouteManager, Disposable {

	private static final Logger logger = LoggerLoader.getLogger(DefaultRouteManager.class);

	public final static DefaultRouteManager INSTANCE = new DefaultRouteManager();

	private final RegionPolicyManager regionPolicyManager = RegionPolicyManager.INSTANCE;

	private final RequestQualityManager requestQualityManager = RequestQualityManager.INSTANCE;

	private final ConfigManager configManager = ConfigManagerLoader.getConfigManager();

	private static final ClusterListenerManager clusterListenerManager = ClusterListenerManager.getInstance();

	private ServiceProviderChangeListener providerChangeListener = new InnerServiceProviderChangeListener();

	private static List<String> preferAddresses = null;

	private static boolean enablePreferAddresses = ConfigManagerLoader.getConfigManager().getBooleanValue(
			"pigeon.route.preferaddresses.enable", false);

	private static boolean isWriteBufferLimit = ConfigManagerLoader.getConfigManager().getBooleanValue(
			Constants.KEY_DEFAULT_WRITE_BUFF_LIMIT, Constants.DEFAULT_WRITE_BUFF_LIMIT);

	private DefaultRouteManager() {
		RegistryEventListener.addListener(providerChangeListener);
		if (enablePreferAddresses) {
			preferAddresses = new ArrayList<String>();
			String preferAddressesConfig = ConfigManagerLoader.getConfigManager().getStringValue(
					"pigeon.route.preferaddresses", "");
			String[] preferAddressesArray = preferAddressesConfig.split(",");
			for (String addr : preferAddressesArray) {
				if (StringUtils.isNotBlank(addr)) {
					preferAddresses.add(addr.trim());
				}
			}
		}
	}

	public Client route(List<Client> clientList, InvokerConfig<?> invokerConfig, InvocationRequest request) {
		if (logger.isDebugEnabled()) {
			for (Client client : clientList) {
				if (client != null) {
					logger.debug("available service provider：\t" + client.getAddress());
				}
			}
		}
		List<Client> availableClients = getAvailableClients(clientList, invokerConfig, request);
		Client selectedClient = select(availableClients, invokerConfig, request);
		if (!selectedClient.isConnected()) {
			selectedClient.connect();
		}
		//不断select知道找到一个Connected的
		while (!selectedClient.isConnected()) {
			logger.info("[route] remove client:" + selectedClient);
			clusterListenerManager.removeConnect(selectedClient);
			availableClients.remove(selectedClient);
			if (availableClients.isEmpty()) {
				break;
			}
			selectedClient = select(availableClients, invokerConfig, request);
		}

		if (!selectedClient.isConnected()) {
			throw new ServiceUnavailableException("no available server exists for service[" + invokerConfig + "], env:"
					+ ConfigManagerLoader.getConfigManager().getEnv());
		}
		return selectedClient;
	}

	/**
	 * 按照权重、分组、region规则、服务质量过滤客户端选择
	 * 加入对oneway调用模式的优化判断
	 * @param clientList
	 * @param invokerConfig
	 * @param request
	 * @return
	 */
	//regionPolicyManager、requestQualityManager中优先regionPolicyManager
	public List<Client> getAvailableClients(List<Client> clientList, InvokerConfig<?> invokerConfig,
			InvocationRequest request) {

		if (regionPolicyManager.isEnableRegionPolicy()) {

			clientList = regionPolicyManager.getPreferRegionClients(clientList, invokerConfig, request);

		} else if (requestQualityManager.isEnableRequestQualityRoute()) {

			float least = configManager.getFloatValue("pigeon.invoker.quality.leastratio", 0.5f) * clientList.size();
			List<Client> qualityFilterClients = requestQualityManager.getQualityPreferClients(clientList, request, least);

			if(qualityFilterClients.size() >= least) {
				clientList = qualityFilterClients;
			}

		}

		boolean isWriteLimit = isWriteBufferLimit && request.getCallType() == Constants.CALLTYPE_NOREPLY;
		List<Client> filteredClients = new ArrayList<Client>(clientList.size());
		boolean existClientBuffToLimit = false;
		for (Client client : clientList) {
			if (client != null) {
				String address = client.getAddress();
				if (client.isActive() && RegistryManager.getInstance().getServiceWeightFromCache(address) > 0) {
					if (!isWriteLimit || client.isWritable()) {
						filteredClients.add(client);
					} else {
						existClientBuffToLimit = true;
					}
				}
			}
		}
		if (filteredClients.isEmpty()) {
			throw new ServiceUnavailableException("no available server exists for service[" + invokerConfig.getUrl()
					+ "] and group[" + invokerConfig.getGroup() + "]"
					+ (existClientBuffToLimit ? ", and exists some server's write buffer reach limit" : "") + ".");
		}
		return filteredClients;
	}

	private void checkClientNotNull(Client client, InvokerConfig<?> invokerConfig) {
		if (client == null) {
			throw new ServiceUnavailableException("no available server exists for service[" + invokerConfig + "], env:"
					+ ConfigManagerLoader.getConfigManager().getEnv());
		}
	}

	//preferAddresses越靠前越优先，如果找不到client的地址在preferAddresses中的，从传入的availableClients查找
	private Client select(List<Client> availableClients, InvokerConfig<?> invokerConfig, InvocationRequest request) {
		LoadBalance loadBalance = null;
		if (loadBalance == null) {
			loadBalance = LoadBalanceManager.getLoadBalance(invokerConfig, request.getCallType());
		}
		if (loadBalance == null) {
			//当没有配置loadBalance时，如果calltype为CALLTYPE_NOREPLY，使用RandomLoadBalance，否则使用WeightedAutoawareLoadBalance
			loadBalance = WeightedAutoawareLoadBalance.instance;
			if (request.getCallType() == Constants.CALLTYPE_NOREPLY) {
				loadBalance = RandomLoadBalance.instance;
			}
		}
		List<Client> preferClients = null;
		if (enablePreferAddresses) {
			if (availableClients != null && availableClients.size() > 1 && !CollectionUtils.isEmpty(preferAddresses)) {
				preferClients = new ArrayList<Client>();
				for (String addr : preferAddresses) {
					for (Client client : availableClients) {
						if (client.getHost().startsWith(addr)) {
							preferClients.add(client);
						}
					}
					if (preferClients.size() > 0) {
						break;
					}
				}
			}
		}
		if (preferClients == null || preferClients.size() == 0) {
			preferClients = availableClients;
		}
		Client selectedClient = loadBalance.select(preferClients, invokerConfig, request);
		checkClientNotNull(selectedClient, invokerConfig);

		return selectedClient;
	}

	@Override
	public void destroy() throws Exception {
		RegistryEventListener.removeListener(providerChangeListener);
	}

	class InnerServiceProviderChangeListener implements ServiceProviderChangeListener {
		@Override
		public void hostWeightChanged(ServiceProviderChangeEvent event) {
			RegistryManager.getInstance().setServiceWeight(event.getConnect(), event.getWeight());
		}

		@Override
		public void providerAdded(ServiceProviderChangeEvent event) {
		}

		@Override
		public void providerRemoved(ServiceProviderChangeEvent event) {
		}
	}

}
