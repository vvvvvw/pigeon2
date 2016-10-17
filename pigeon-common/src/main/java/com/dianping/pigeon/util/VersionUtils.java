package com.dianping.pigeon.util;

public class VersionUtils {

	public static final String VERSION = "2.7.8";
	//比较两个版本的大小，版本以.或者是-分隔
	public static int compareVersion(String version1, String version2) {
		String[] s1 = version1.split("\\.|-");
		String[] s2 = version2.split("\\.|-");

		int len1 = s1.length;
		int len2 = s2.length;
		int compareCount = len1;
		if (len1 <= len2) {
			compareCount = len1;
		} else if (len1 > len2) {
			compareCount = len2;
		}
		for (int i = 0; i < compareCount; i++) {
			int v1 = 0;
			try {
				v1 = Integer.parseInt(s1[i]);
			} catch (RuntimeException e) {
				return s1[i].compareToIgnoreCase(s2[i]);
			}
			int v2 = 0;
			try {
				v2 = Integer.parseInt(s2[i]);
			} catch (RuntimeException e) {
				return s1[i].compareToIgnoreCase(s2[i]);
			}
			int r = v1 - v2;
			if (r > 0) {
				return 1;
			}
			if (r < 0) {
				return -1;
			}
		}
		return len2 - len1; //相同条件下，len越大，//版本越低（这是为什么？写错？）
	}

}
