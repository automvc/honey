/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.teasoft.bee.osql.MapSql;
import org.teasoft.bee.osql.MapSqlKey;
import org.teasoft.bee.osql.MapSqlSetting;

/**
 * @author Kingstar
 * @since  1.9
 */
public class MapSqlImpl implements MapSql {

	private Map<MapSqlKey, String> sqlkeyMap = new HashMap<>();
//	Map<String, Object> whereConditonMap = new HashMap<>();
//	private Map<String, Object> whereConditonMap = new LinkedHashMap<>();
	private Map<String, Object> kv = new LinkedHashMap<>();
	private Map<MapSqlSetting, Boolean> settingMap = new HashMap<>();

	@Override
	public void put(MapSqlKey key, String value) {
		sqlkeyMap.put(key, value);
	}

	@Override
	public void put(String fieldName, Object value) {
		kv.put(fieldName, value);
	}
	
	@Override
	public void put(MapSqlSetting MapSqlSetting, boolean value) {
		settingMap.put(MapSqlSetting, value);
	}

	Map<MapSqlKey, String> getSqlkeyMap() {
		return sqlkeyMap;
	}

	Map<String, Object> getKvMap() {
		return kv;
	}
	
	Map<MapSqlSetting, Boolean> getSqlSettingMap() {
		return settingMap;
	}

}
