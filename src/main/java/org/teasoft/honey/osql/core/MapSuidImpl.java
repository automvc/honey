/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.MapSql;
import org.teasoft.bee.osql.MapSuid;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author Kingstar
 * @since  1.9
 */
public class MapSuidImpl implements MapSuid {

	private BeeSql beeSql;

	public BeeSql getBeeSql() {
		if (beeSql == null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	@Override
	public List<String[]> selectString(MapSql mapSql) {
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, List<String[]> select SQL: ", sql);
		return getBeeSql().select(sql);
	}

	@Override
	public String selectJson(MapSql mapSql) {
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, selectJson SQL: ", sql);
		return getBeeSql().selectJson(sql);
	}

	@Override
	public List<Map<String, Object>> select(MapSql mapSql) {
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, selectMap SQL: ", sql);
		return getBeeSql().selectMapList(sql);
	}

	@Override
	public Map<String, Object> selectOne(MapSql mapSql) {
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, selectOneMap SQL: ", sql);
		List<Map<String, Object>> list = getBeeSql().selectMapList(sql);

		if (ObjectUtils.isNotEmpty(list)) {
			return list.get(0);
		} else {
			return Collections.emptyMap();
		}
	}

	@Override
	public int delete(MapSql mapSql) {
		String sql = MapSqlProcessor.toDeleteSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, delete SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	public long insert(MapSql mapSql) {
		if(mapSql==null) return -1;
		String sql = MapSqlProcessor.toInsertSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, insert SQL: ", sql);
		return getBeeSql().modify(sql);
		
	}

}
