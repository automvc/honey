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
 * 用Map承载SQL信息.Record SQL information with map.
 * @author Kingstar
 * @since  1.9
 */
public class MapSqlImpl implements MapSql {

	 Map<MapSqlKey, String> sqlkeyMap = new HashMap<>();
//	Map<String, Object> whereConditonMap = new HashMap<>();
//	private Map<String, Object> whereConditonMap = new LinkedHashMap<>();
	 Map<String, Object> kv = new LinkedHashMap<>();
	 Map<String, Object> newKv = new LinkedHashMap<>(); //just for update set part
	 Map<MapSqlSetting, Boolean> settingMap = new HashMap<>();

	@Override
	public void put(MapSqlKey key, String value) {
		sqlkeyMap.put(key, value);
	}
	
	@Override
	public void put(String fieldName, Object value) {
		kv.put(fieldName, value);
	}
	
	@Override
	public void put(Map<String, ? extends Object> map) {
		kv.putAll(map);
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

	@Override
	public void putNew(String fieldName, Object newValue) {
		newKv.put(fieldName, newValue);
	}
	
	@Override
	public void putNew(Map<String, ? extends Object> map) {
		newKv.putAll(map);
	}

	//just for update set part
	Map<String, Object> getNewKvMap() {
		return newKv;
	}
	
	private Integer start;
	private Integer size;

	@Override
	public void start(Integer start) {
		this.start = start;
	}

	@Override
	public void size(Integer size) {
		this.size = size;
	}
	
	public Integer getStart() {
		return start;
	}

	public Integer getSize() {
		return size;
	}
	
}
