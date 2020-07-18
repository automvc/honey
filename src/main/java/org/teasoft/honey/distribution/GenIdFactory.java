/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.teasoft.bee.distribution.GenId;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class GenIdFactory {

	private static GenId genId;
	private static Map<String, GenId> map = new ConcurrentHashMap<>();
	private static String defaultGenType = "SerialUniqueId";
	
	public static long get() {
		return get("");
	}
	
	public static long[] getRangeId(int sizeOfIds) {
		return getRangeId("", sizeOfIds);
	}

	public static long get(String bizType) {
		return get(bizType, defaultGenType);
	}

	public static long get(String bizType, String genType) {
		genId = getGenId(bizType, genType);
		return genId.get();
	}

	public static long[] getRangeId(String bizType, int sizeOfIds) {
		return getRangeId(bizType, defaultGenType, sizeOfIds);
	}

	public static long[] getRangeId(String bizType, String genType, int sizeOfIds) {
		genId = getGenId(bizType, genType);
		return genId.getRangeId(sizeOfIds);
	}

	private static GenId getGenId(String bizType, String genType) {
		String key = genType + "::" + bizType;
		genId = map.get(key);
		if (genId == null) {
			switch (genType) {

				case "OneTimeSnowflakeId":
					genId = new OneTimeSnowflakeId();
				case "PearFlowerId":
					genId = new PearFlowerId();
				case "SerialUniqueId":
				default:
					genId = new SerialUniqueId();
			}
			map.put(key, genId);
			//TODO 要选择不同类型   每种ID,还要选择不同的业务类型,如不同的表名,只给自己的表拿ID(表名隔离).
			//默认用SerialUniqueId, 单机,用workerid=0.  插入到表可以保证单调连续,全局唯一.
		}
		return genId;
	}
}
