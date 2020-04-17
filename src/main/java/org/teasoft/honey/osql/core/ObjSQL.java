/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ObjToSQL;
import org.teasoft.bee.osql.Suid;

/**
 * 通过对象来操作数据库，并返回结果
 * @author Kingstar
 * Create on 2013-6-30 下午10:19:27
 * @since  1.0
 */
public class ObjSQL implements Suid {
	

	private BeeSql beeSql;// = BeeFactory.getHoneyFactory().getBeeSql();
	private ObjToSQL objToSQL = BeeFactory.getHoneyFactory().getObjToSQL();

	public ObjSQL() {}

	public BeeSql getBeeSql() {
		if(this.beeSql==null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}


	@Override
	public <T> List<T> select(T entity) {

		if (entity == null) return null;

		List<T> list = null;
		String sql = objToSQL.toSelectSQL(entity);
		Logger.logSQL("select SQL: ", sql);
		list = getBeeSql().select(sql, entity); // 返回值用到泛型
		return list;
	}
	
	@Override
	public <T> int update(T entity) {
		// 当id为null时抛出异常  在转sql时抛出

		if (entity == null) return -1;

		String sql = "";
		int updateNum = -1;
		try {
			sql = objToSQL.toUpdateSQL(entity);
			Logger.logSQL("update SQL: ", sql);
			updateNum = getBeeSql().modify(sql);
		} catch (ObjSQLException e) {
			throw e;
		}

		return updateNum;
	}

	@Override
	public <T> int insert(T entity){

		if (entity == null) return -1;

		String sql = objToSQL.toInsertSQL(entity);
		int insertNum = -1;
		Logger.logSQL("insert SQL: ", sql);
		insertNum = getBeeSql().modify(sql);
		return insertNum;
	}

	@Override
	public int delete(Object entity) {

		if (entity == null) return -1;

		String sql = objToSQL.toDeleteSQL(entity);
		int deleteNum = -1;
		Logger.logSQL("delete SQL: ", sql);
		deleteNum = getBeeSql().modify(sql);
		return deleteNum;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;

		List<T> list = null;
		String sql = objToSQL.toSelectSQL(entity,condition);
		Logger.logSQL("select SQL: ", sql);
		list = getBeeSql().select(sql, entity); 
		return list;
	}

	@Override
	public <T> int delete(T entity, Condition condition) {
		if (entity == null) return -1;

		String sql = objToSQL.toDeleteSQL(entity,condition);
		int deleteNum = -1;
		if (!"".equals(sql)) {
			Logger.logSQL("delete SQL: ", sql);
		}
		deleteNum = getBeeSql().modify(sql);
		return deleteNum;
	}

}
