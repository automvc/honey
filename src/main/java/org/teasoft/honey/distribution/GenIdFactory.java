/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * @author Kingstar
 * @since  1.7.3
 */
public class GenIdFactory {

	private static GenId genId;
	private static Map<String, GenId> map = new ConcurrentHashMap<>();
	private static String defaultGenType;
	
	static{
		int idGenerator=HoneyConfig.getHoneyConfig().idGenerator;
		
		if(idGenerator==1) defaultGenType = "SerialUniqueId";
		else if(idGenerator==2) defaultGenType = "OneTimeSnowflakeId";
		else if(idGenerator==3) defaultGenType = "PearFlowerId";
		else defaultGenType = "SerialUniqueId";
	}
	
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

				case "SerialUniqueId":
					genId = new SerialUniqueId();
				case "OneTimeSnowflakeId":
					genId = new OneTimeSnowflakeId();
				case "PearFlowerId":
					genId = new PearFlowerId();
			}
			map.put(key, genId);
			//TODO 要选择不同类型   每种ID,还要选择不同的业务类型,如不同的表名,只给自己的表拿ID(表名隔离).
			//单机默认用SerialUniqueId, 用workerid=0.  插入到表可以保证单调连续,全局唯一.
		}
		return genId;
	}
}
