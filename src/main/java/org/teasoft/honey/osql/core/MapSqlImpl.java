/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.teasoft.bee.osql.MapSqlKey;
import org.teasoft.bee.osql.MapSqlSetting;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.MapSql;

/**
 * 用Map承载SQL信息.Record SQL information with map.
 * @author Kingstar
 * @since  1.9
 */
public class MapSqlImpl implements MapSql {

	private Map<MapSqlKey, String> sqlkeyMap = new HashMap<>();
	private Map<String, Object> kv = new LinkedHashMap<>();
	private Map<String, Object> newKv = new LinkedHashMap<>(); //just for update set part
	private Map<MapSqlSetting, Boolean> settingMap = new HashMap<>();
	
	private Condition whereCondition;
	private Condition updateSetCondition;
	
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

	@Override
	public void where(Condition condition) { // 2.4.0
		this.whereCondition = condition;
	}
	
	@Override
	public void updateSet(Condition condition) {// 2.4.0
		this.updateSetCondition=condition;
	}

	// 2.4.0
	public Condition getWhereCondition() {
		return whereCondition;
	}
	
	// 2.4.0
	public Condition getUpdateSetCondition() {
		return updateSetCondition;
	}

	MapSqlImpl copyForCount(){
		MapSqlImpl n = new MapSqlImpl();
		n.kv = this.getKvMap();
//		n.sqlkeyMap = old.getSqlkeyMap();
		n.newKv = this.getNewKvMap();
		n.settingMap = this.getSqlSettingMap();
		
		n.whereCondition=this.getWhereCondition(); //2.4.0
		
//		n.start(old.getStart()); //ignore
//		n.size(old.getSize()); //ignore
		
		Map<MapSqlKey, String> map=this.getSqlkeyMap();
		for (Map.Entry<MapSqlKey, String> entry : map.entrySet()) {
			n.put(entry.getKey(), entry.getValue());
		}
		
		return n;
	}
	
}
