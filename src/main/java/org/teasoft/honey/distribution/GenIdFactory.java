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
 * @since  1.8
 */
public class GenIdFactory {

	private static Map<String, GenId> map = new ConcurrentHashMap<>();
	private static String defaultGenType;

	public static final String GenType_SerialUniqueId = "SerialUniqueId";
	public static final String GenType_OneTimeSnowflakeId = "OneTimeSnowflakeId";
	public static final String GenType_PearFlowerId = "PearFlowerId";
	public static final String GenType_IntSerialIdReturnLong = "IntSerialIdReturnLong";

	static {
		int idGenerator = HoneyConfig.getHoneyConfig().genid_generatorType;

		if (idGenerator == 1)
			defaultGenType = GenType_SerialUniqueId;
		else if (idGenerator == 2)
			defaultGenType = GenType_OneTimeSnowflakeId;
		else if (idGenerator == 3)
			defaultGenType = GenType_PearFlowerId;
		else if (idGenerator == 4)
			defaultGenType = GenType_IntSerialIdReturnLong;
		else
			defaultGenType = GenType_SerialUniqueId;
	}

	private GenIdFactory() {}

	/**
	 * 使用默认的命名key来获取id.
	 * @return long id num.
	 */
	public static long get() {
		return get("");
	}

	/**
	 * 获取一个范围的id.
	 * @param sizeOfIds size of Ids
	 * @return array of long id.
	 */
	public static long[] getRangeId(int sizeOfIds) {
		return getRangeId("", sizeOfIds);
	}

	/**
	 * get id from factory.default GenType class is: SerialUniqueId.
	 * @param bizType bizType作为隔离的命名空间.bizType as namespace.
	 * @return long id num.
	 */
	public static long get(String bizType) {
		return get(bizType, defaultGenType);
	}

	/**
	 * 
	 * @param bizType bizType作为隔离的命名空间.bizType as namespace.
	 * @param genType The value is one of "SerialUniqueId","OneTimeSnowflakeId" or "PearFlowerId".
	 * @return long id num.
	 */
	public static long get(String bizType, String genType) {
		GenId genId = getGenId(bizType, genType);
		return genId.get();
	}

	/**
	 * 获取一批id号
	 * @param bizType bizType作为隔离的命名空间.bizType as namespace.
	 * @param sizeOfIds 一批次获取id号的数量. size of Ids.
	 * @return 一批id号.array of id num.
	 */
	public static long[] getRangeId(String bizType, int sizeOfIds) {
		return getRangeId(bizType, defaultGenType, sizeOfIds);
	}

	/**
	 * 获取一批id号
	 * @param bizType bizType作为隔离的命名空间.bizType as namespace.
	 * @param genType The value is one of "SerialUniqueId","OneTimeSnowflakeId" or "PearFlowerId".
	 * @param sizeOfIds 一批次获取id号的数量. size of Ids.
	 * @return 一批id号.array of id num.
	 */
	public static long[] getRangeId(String bizType, String genType, int sizeOfIds) {
		GenId genId = getGenId(bizType, genType);
		return genId.getRangeId(sizeOfIds);
	}

	private static GenId getGenId(String bizType, String genType) {
		String key = genType + "::" + bizType;
		GenId genId = map.get(key);
		if (genId == null) {
			switch (genType) {
				case GenType_SerialUniqueId:
					genId = new SerialUniqueId();
					break;
				case GenType_OneTimeSnowflakeId:
					genId = new OneTimeSnowflakeId();
					break;
				case GenType_PearFlowerId:
					genId = new PearFlowerId();
					break;
				case GenType_IntSerialIdReturnLong:
					genId = new IntSerialIdReturnLong();
					break;
				default:
					genId = new SerialUniqueId();
			}
			map.put(key, genId);
			// 要选择不同类型 每种ID,还要选择不同的业务类型,如不同的表名,只给自己的表拿ID(表名隔离).
			// 单机默认用SerialUniqueId, 用workerid=0. 插入到表可以保证单调连续,全局唯一.
		}
		return genId;
	}

}
