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
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.MapSql;
import org.teasoft.bee.osql.api.MapSuid;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * 操作数据库不依赖javabean结构的类.The class that operation database does not depend on Javabean.
 * @author Kingstar
 * @since  1.9
 */
public class MapSuidImpl extends AbstractCommOperate implements MapSuid {

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
		List<String[]> list =null;
		try {
		doBeforePasreEntity(SuidType.SELECT);

		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, select List<String[]> SQL: ", sql);
		list = getBeeSql().select(sql);
		}finally {
		doBeforeReturn();
		}
		return list;
	}

	@Override
	public String selectJson(MapSql mapSql) {
		String json ="";
		try {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, selectJson SQL: ", sql);
		 json = getBeeSql().selectJson(sql);
	    }finally {
		doBeforeReturn();
	    }
		return json;
	}

	@Override
	public List<Map<String, Object>> select(MapSql mapSql) {
		List<Map<String, Object>> list =null;
		try {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, select List<Map> SQL: ", sql);
		list = getBeeSql().selectMapList(sql);
	    }finally {
		doBeforeReturn();
	    }
		return list;
	}

	@Override
	public int count(MapSql mapSql) {
		String total =null;
		try {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toCountSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, count SQL: ", sql);
		 total = getBeeSql().selectFun(sql);
	    }finally {
		doBeforeReturn();
	    }
		return StringUtils.isBlank(total) ? 0 : Integer.parseInt(total);
	}

	@Override
	public Map<String, Object> selectOne(MapSql mapSql) {
		List<Map<String, Object>> list =null;
		try {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, selectOne Map SQL: ", sql);
		list = getBeeSql().selectMapList(sql);
	    }finally {
		doBeforeReturn();
	    }
		if (ObjectUtils.isNotEmpty(list)) {
			return list.get(0);
		} else {
			return Collections.emptyMap();
		}
	}

	@Override
	public int insert(MapSql mapSql) {
		if (mapSql == null) return -1;
		int insertNum =0;
		try {
		doBeforePasreEntity(SuidType.INSERT);

		String sql = MapSqlProcessor.toInsertSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, insert SQL: ", sql);

		insertNum = getBeeSql().modify(sql);
	    }finally {
		doBeforeReturn();
	    }

		return insertNum;
	}

	@Override
	public long insertAndReturnId(MapSql mapSql) {

		if (mapSql == null) return -1;
		
		long newId=0;
		try {
		doBeforePasreEntity(SuidType.INSERT);
		String sql = MapSqlProcessor.toInsertSqlByMap(mapSql, true); // will get pkName and set into OneTimeParameter
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, insertAndReturnId SQL: ", sql);

		Object obj = OneTimeParameter.getAttribute(StringConst.MapSuid_Insert_Has_ID);
		
		if (obj != null) {
			newId = Long.parseLong(obj.toString());
			if (newId > 1) { //设置有大于1的值,使用设置的
				OneTimeParameter.getAttribute(StringConst.PK_Name_For_ReturnId); //不使用insertAndReturnId,提前消费一次性变量
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
        }finally {
		doBeforeReturn();
	    }
		return newId;

	}

	@Override
	public int delete(MapSql mapSql) {
		int a =0;
		try {
		doBeforePasreEntity(SuidType.DELETE);
		String sql = MapSqlProcessor.toDeleteSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, delete SQL: ", sql);
		 a = getBeeSql().modify(sql);
	    }finally {
		doBeforeReturn();
	    }
		return a;
	}

	@Override
	public int update(MapSql mapSql) {
		int a =0;
		try {
		doBeforePasreEntity(SuidType.UPDATE);
		String sql = MapSqlProcessor.toUpdateSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, update SQL: ", sql);
		 a = getBeeSql().modify(sql);
     	}finally {
		doBeforeReturn();
    	}
		return a;
	}
	
	private void doBeforePasreEntity(SuidType suidType) {
		Object entity=null;
		super.doBeforePasreEntity(entity, suidType);
	}

}
