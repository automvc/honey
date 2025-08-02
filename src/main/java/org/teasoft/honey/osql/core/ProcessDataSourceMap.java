/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.teasoft.bee.ds.DataSourceBuilder;
import org.teasoft.bee.ds.DataSourceBuilderFactory;
import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.util.Converter;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  2.1
 */
class ProcessDataSourceMap {

	private ProcessDataSourceMap() {}

	static void parseDbNameByDsMap(Map<String, DataSource> dsMap) {

//		if(! HoneyContext.isNeedRealTimeDb()) return ;  //在设置DataSourceMap前,就要设置同时使用多种类型数据库时的配置信息

//		Map<String, DataSource> dsMap = getDataSourceMap();
		if (dsMap == null) return;
		int i = 0;
		String dbName = "";
		Map<String, String> dsName2DbName = new LinkedHashMap<>();
		for (Map.Entry<String, DataSource> entry : dsMap.entrySet()) {
			dsName2DbName.put(entry.getKey(), getDbName(entry.getValue()));
			if (i == 0) {
				dbName = dsName2DbName.get(entry.getKey());
				i++;
			}

		}
//		HoneyContext.setDsName2DbName(dsName2DbName);
		Logger.info("[Bee] Parse DataSourceMap: dataSource name to database name , result: " + dsName2DbName);
//		HoneyConfig.getHoneyConfig().dbName=dbName;
		HoneyConfig.getHoneyConfig().setDbName(dbName); // 默认设置第1个数据源
//		HoneyContext.resetAferSetDbName(); // 2.5.2
//		BeeFactory.getHoneyFactory().setDbFeature(null); 
		HoneyContext.setDsName2DbName(dsName2DbName);
		HoneyUtil.refreshSetParaAndResultTypeHandlerRegistry();
	}

	static Map<String, DataSource> refreshDataSourceMap() {

//		List<Map<String, String>> dbsList = HoneyConfig.getHoneyConfig().getDbs();
		Map<String, Map<String, String>> dsMap = HoneyConfig.getHoneyConfig().getDbs();
		if (dsMap == null || dsMap.size() == 0) return null;

		List<Map<String, String>> dbsList = new ArrayList<>();
		for (Map.Entry<String, Map<String, String>> entry : dsMap.entrySet()) {
//		    Map<String, String> innerMap = entry.getValue();
//		    dbsList.add(innerMap);
			dbsList.add(entry.getValue());
		}

		if (dbsList == null || dbsList.size() == 0) return null;

		boolean extendFirst = HoneyConfig.getHoneyConfig().extendFirst;
		notifyClass("DataSourceToolRegHandler"); // 是否需要判断再显示?? 不需要,可以一下注册多个,DataSourceToolRegHandler只是注册bee框架的包装类,
													// 真正到使用某个builder时,没有jar才会报错.
		int size = dbsList.size();
		String dsNames[] = new String[size];
		Map<String, DataSource> dataSourceMap = new HashMap<>();
		Map<String, String> map;
		Map<String, String> base0 = null;
		Map<String, String> copyMap = null;

		String type;

		for (int i = 0; i < size; i++) {
			map = dbsList.get(i);
//			转换key名称
			map = Converter.transferKey(map);
			dsNames[i] = map.get("dsName");
			map.remove("dsName");

			if (extendFirst) {
				if (i == 0)
					base0 = map;
				else {
					copyMap = copyMap(base0);
					if (copyMap != null) { // 合并两个map, 后面的会覆盖[0]的
						copyMap.putAll(map);
						map = copyMap;
					}
				}
			}

//			type:Hikari,Druid,BeeMongo
			type = map.get("type"); // dsToolType, type也可以继承
			if (StringUtils.isBlank(type)) {
				String url = map.get("url");
				if (url != null && url.startsWith("mongodb://")) type = "BeeMongo";
			}

			if (StringUtils.isBlank(type)) {
				type = "Hikari"; // 兼容spirng boot,默认为Hikari
			} else if ("BeeMongo".equalsIgnoreCase(type)) {
				notifyClass("BeeMongodbRegHandler");
			}

			DataSourceBuilder builder = DataSourceBuilderFactory.getDataSourceBuilder(type);
			if (builder == null) {
				throw new ConfigWrongException("Did not config the DataSourceBuilder for " + type);
			}
			map.remove("type"); // V2.5.2
			dataSourceMap.put(dsNames[i], builder.build(map));
		}

		return dataSourceMap;
	}

	private static void notifyClass(String className) {
		try {
//			Class.forName("org.teasoft.beex.ds.DataSourceToolRegHandler");
			Class.forName("org.teasoft.beex.ds." + className);
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> copyMap(Map<String, String> obj) {
		try {
			Serializer jdks = new JdkSerializer();
			return (Map<String, String>) jdks.unserialize(jdks.serialize(obj));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return null;
	}

	private static String getDbName(DataSource ds) {
		Connection conn = null;
		String dbName = null;
		try {
			conn = ds.getConnection();
			if (conn != null) {
				dbName = conn.getMetaData().getDatabaseProductName();
				if (dbName.contains("Microsoft Access")) {
					Logger.debug("Transform the dbName:'" + dbName + "' to '" + DatabaseConst.MsAccess + "'");
					dbName = DatabaseConst.MsAccess;
				}
			}
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		} finally {
			try {
				if (conn != null) conn.close();
			} catch (Exception e2) {
				Logger.warn(e2.getMessage(), e2);
			}
		}

		return dbName;
	}

}
