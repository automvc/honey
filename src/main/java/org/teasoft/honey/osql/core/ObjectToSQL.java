/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.ObjToSQL;

/**
 * 对象到SQL语句的转化 (标准sql)
 * @author Kingstar
 * @since  1.0
 */
public class ObjectToSQL implements ObjToSQL {

	@Override
	public <T> String toInsertSQL(T entity) {
		// return _toInsertSQL(entity,false);
		String sql = null;
		try {
			_ObjectToSQLHelper.setInitIdByAuto(entity);
			sql = _ObjectToSQLHelper._toInsertSQL0(entity, -1,"");
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity) {
		return _ObjectToSQLHelper._toSelectSQL(entity, -1); // 过滤NULL和空字符串
	}

	@Override
	public <T> String toDeleteSQL(T entity) {
		return _ObjectToSQLHelper._toDeleteSQL(entity, -1);
	}

	@Override
//	public <T> String toUpdateSQL(T entity) throws ObjSQLException {
	public <T> String toUpdateSQL(T entity) {
		String sql = null;
//		try {
//			sql = _ObjectToSQLHelper._toUpdateSQL(entity, "id", -1);
		sql = _ObjectToSQLHelper._toUpdateSQL(entity, -1);
			// Logger.logSQL("update SQL : ", sql);
//		} catch (IllegalAccessException e) {
//			throw ExceptionHelper.convert(e);
//		}
		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity, Condition condition) {

		if (condition == null || condition.getIncludeType() == null)
			return _ObjectToSQLHelper._toSelectSQL(entity, -1, condition); // 过滤NULL和空字符串
		else
			return _ObjectToSQLHelper._toSelectSQL(entity, condition.getIncludeType().getValue(), condition); //v1.7.0	
	}
	
	//v1.7.2
	@Override
	public <T> String toDeleteSQL(T entity, Condition condition) {
		if(condition==null || condition.getIncludeType()==null)
			  return _ObjectToSQLHelper._toDeleteSQL(entity, -1,condition); // 过滤NULL和空字符串
			else
			  return _ObjectToSQLHelper._toDeleteSQL(entity, condition.getIncludeType().getValue(),condition); 
	}
	
}
