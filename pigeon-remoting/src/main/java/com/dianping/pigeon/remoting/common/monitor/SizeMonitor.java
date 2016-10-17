package com.dianping.pigeon.remoting.common.monitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dianping.pigeon.config.ConfigManagerLoader;

public class SizeMonitor {

	private static Logger logger = LogManager.getLogger(SizeMonitor.class);

	private static final String sizeRangeConfig = ConfigManagerLoader.getConfigManager().getStringValue(
			"pigeon.monitor.size.range", "2,4,8,16,32,64,121,8,256,512,1024");

	private static int[] sizeRangeArray;

	private static final long sizeMin = ConfigManagerLoader.getConfigManager().getLongValue(
			"pigeon.monitor.msgsize.min", 0);

	private static class SizeHolder {
		public static final SizeMonitor INSTANCE = new SizeMonitor();
	}

	public static SizeMonitor getInstance() {
		return SizeHolder.INSTANCE;
	}

	private SizeMonitor() {
		init();
	}

	private void init() {
		sizeRangeArray = initRangeArray(sizeRangeConfig);
	}

	//根据rangeConfig生成一个数组，如果rangeConfig字符串中的第i个数字为a，第i+1个数字为b
	//那么数组中下标在a+1到b的值都为b
	private int[] initRangeArray(String rangeConfig) {
		String[] range = rangeConfig.split(",");
		int end = Integer.valueOf(range[range.length - 1]);
		int[] rangeArray = new int[end];
		int rangeIndex = 0;
		for (int i = 0; i < end; i++) {
			if (range.length > rangeIndex) {
				int value = Integer.valueOf(range[rangeIndex]);
				if (i >= value) {
					rangeIndex++;
				}
				rangeArray[i] = value;
			}
		}
		return rangeArray;
	}

	public String getLogSize(int size) {
		if (size > sizeMin) {
			try {
				return getLogSize(size, sizeRangeArray);
			} catch (Throwable t) {
				logger.warn("error while logging size:" + t.getMessage());
			}
		}
		return null;
	}

	//写日志 小于...k
	private String getLogSize(int size, int[] rangeArray) {
		if (size > 0 && rangeArray != null && rangeArray.length > 0) {
			String value = ">" + rangeArray[rangeArray.length - 1] + "k";
			int sizeK = (int) Math.ceil(size * 1d / 1024);
			if (rangeArray.length > sizeK) {
				value = "<" + rangeArray[sizeK] + "k";
			}
			return value;
		}
		return null;
	}
}
