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
		Logger.logSQL("In MapSuid, select List<String[]> SQL: ", sql);
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
		Logger.logSQL("In MapSuid, select List<Map> SQL: ", sql);
		return getBeeSql().selectMapList(sql);
	}
	
	@Override
	public int count(MapSql mapSql) {
		String sql = MapSqlProcessor.toCountSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, count SQL: ", sql);
		String total= getBeeSql().selectFun(sql);
		return total == null ? 0 : Integer.parseInt(total);
	}

	@Override
	public Map<String, Object> selectOne(MapSql mapSql) {
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, selectOne Map SQL: ", sql);
		List<Map<String, Object>> list = getBeeSql().selectMapList(sql);

		if (ObjectUtils.isNotEmpty(list)) {
			return list.get(0);
		} else {
			return Collections.emptyMap();
		}
	}

	@Override
	public long insert(MapSql mapSql) {
		
		if(mapSql==null) return -1;
		
		String sql = MapSqlProcessor.toInsertSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, insert SQL: ", sql);
		
		Object obj =OneTimeParameter.getAttribute("_SYS_Bee_MapSuid_Insert_Has_ID");
		long newId;
		if (obj != null) {
			newId = Long.parseLong(obj.toString());
			if (newId > 1) {
				int insertNum = getBeeSql().modify(sql);
				if (insertNum == 1) {
					return newId;
				} else {
					return insertNum;
				}
			} else {
				if (HoneyUtil.isOracle()) {
					Logger.debug("Need create Sequence and Trigger for auto increment id. "
							+ "By the way,maybe use distribute id is better!");
				}
			}
		}
//		假如处理后id为空,则用db生成.
		//id will gen by db
		newId = getBeeSql().insertAndReturnId(sql);

		return newId;
		
	}
	
	@Override
	public int delete(MapSql mapSql) {
		String sql = MapSqlProcessor.toDeleteSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, delete SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	public int update(MapSql mapSql) {

		String sql = MapSqlProcessor.toUpdateSqlByMap(mapSql);
		Logger.logSQL("In MapSuid, update SQL: ", sql);
		return getBeeSql().modify(sql);
	}
	
}
