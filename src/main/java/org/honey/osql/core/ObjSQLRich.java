/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.core;

import java.util.List;

import org.bee.osql.FunctionType;
import org.bee.osql.IncludeType;
import org.bee.osql.ObjSQLException;
import org.bee.osql.ObjToSQLRich;
import org.bee.osql.OrderType;
import org.bee.osql.SuidRich;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLRich extends ObjSQL implements SuidRich {

	private ObjToSQLRich objToSQLRich = BeeFactory.getHoneyFactory().getObjToSQLRich();
//	private BeeSql beeSql = BeeFactory.getHoneyFactory().getBeeSql();

	@Override
	public <T> List<T> select(T entity, int size) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectSQL(entity, size);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> List<T> select(T entity, int from, int size) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectSQL(entity, from, size);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> List<T> select(T entity, String selectField) {//sqlLib.selectSomeField
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = objToSQLRich.toSelectSQL(entity, selectField);
			list = getBeeSql().selectSomeField(sql, entity);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFieldList) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = objToSQLRich.toSelectOrderBySQL(entity, orderFieldList);
			Logger.logSQL("selectOrderBy SQL: ", sql);
			list = getBeeSql().select(sql, entity);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFieldList, OrderType[] orderTypes) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = objToSQLRich.toSelectOrderBySQL(entity, orderFieldList, orderTypes);
			Logger.logSQL("selectOrderBy SQL: ", sql);
			list = getBeeSql().select(sql, entity);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> int[] insert(T entity[]) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity);

		return getBeeSql().batch(insertSql);
	}

	@Override
	public <T> int[] insert(T entity[], String excludeFieldList) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity, excludeFieldList);

		return getBeeSql().batch(insertSql);
	}

	@Override
	public <T> int[] insert(T entity[], int batchSize) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity);

		return getBeeSql().batch(insertSql, batchSize);
	}

	@Override
	public <T> int[] insert(T entity[], int batchSize, String excludeFieldList) {
		if (entity == null) return null;
		int len = entity.length;
		String insertSql[] = new String[len];
		insertSql = objToSQLRich.toInsertSQL(entity, excludeFieldList);

		return getBeeSql().batch(insertSql, batchSize);
	}

	@Override
	public <T> int update(T entity, String updateFieldList) {
		if (entity == null) return 0;
		int r = 0;
		try {
			String sql = objToSQLRich.toUpdateSQL(entity, updateFieldList);
			Logger.logSQL("update SQL(updateFieldList) :", sql);
			r = getBeeSql().modify(sql);
		} catch (ObjSQLException e) {
			throw e;
		}

		return r;
	}

	@Override
	public <T> T selectOne(T entity) {
		if (entity == null) return null;
		List<T> list = select(entity);
		if (list == null || list.size() != 1) return null;

		return list.get(0);
	}

	@Override
	public <T> String selectWithFun(T entity, FunctionType functionType, String fieldForFun) {
		if (entity == null) return null;
		String s = null;
		try {
			String sql = objToSQLRich.toSelectFunSQL(entity, functionType, fieldForFun);
			s=getBeeSql().selectFun(sql);
		} catch (ObjSQLException e) {
			throw e;
		}

		return s;
	}

	@Override
	public <T> int update(T entity, String updateFieldList, IncludeType includeType) {
		if (entity == null) return 0;
		int r = 0;
		try {
			String sql = objToSQLRich.toUpdateSQL(entity, updateFieldList, includeType);
			Logger.logSQL("update SQL(updateFieldList) :", sql);
			r = getBeeSql().modify(sql);
		} catch (ObjSQLException e) {
			throw e;
		}

		return r;
	}

	@Override
	public <T> List<T> select(T entity, IncludeType includeType) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectSQL(entity, includeType);
		Logger.logSQL("select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> int update(T entity, IncludeType includeType) {
		if (entity == null) return 0;
		String sql = objToSQLRich.toUpdateSQL(entity, includeType);
		Logger.logSQL("update SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	public <T> int insert(T entity, IncludeType includeType) {
		if (entity == null) return 0;
		String sql = objToSQLRich.toInsertSQL(entity, includeType);
		Logger.logSQL("insert SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	public <T> int delete(T entity, IncludeType includeType) {
		if (entity == null) return 0;
		String sql = objToSQLRich.toDeleteSQL(entity, includeType);
		Logger.logSQL("delete SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	public <T> List<String[]> selectString(T entity) {

		if (entity == null) return null;

		List<String[]> list = null;
		String sql = objToSQLRich.toSelectSQL(entity);

		Logger.logSQL("List<String[]> select SQL: ", sql);
		list = getBeeSql().select(sql);

		return list;
	}

	@Override
	public <T> List<String[]> selectString(T entity, String selectFields) {

		if (entity == null) return null;

		List<String[]> list = null;
		try {
			String sql = objToSQLRich.toSelectSQL(entity, selectFields);
			list = getBeeSql().select(sql);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> String selectJson(T entity) {
		
		if (entity == null) return null;
		
		String sql = objToSQLRich.toSelectSQL(entity);
		Logger.logSQL("selectJson SQL: ", sql);
		
		return getBeeSql().selectJson(sql);
	}

	@Override
	public <T> String selectJson(T entity, IncludeType includeType) {
		if (entity == null) return null;
		
		String sql = objToSQLRich.toSelectSQL(entity, includeType);
		Logger.logSQL("selectJson SQL: ", sql);
		
		return getBeeSql().selectJson(sql);
	}

	@Override
	public <T> List<T> selectById(T entity, Integer id) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> List<T> selectById(T entity, Long id) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> List<T> selectById(T entity, String ids) {
		if (entity == null) return null;
		String sql = objToSQLRich.toSelectByIdSQL(entity, ids);
		Logger.logSQL("selectById SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Integer id) {
		if (c == null) return 0;
		String sql = objToSQLRich.toDeleteByIdSQL(c, id);
		Logger.logSQL("deleteById SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Long id) {
		if (c == null) return 0;
		String sql = objToSQLRich.toDeleteByIdSQL(c, id);
		Logger.logSQL("deleteById SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, String ids) {
		if (c == null) return 0;
		String sql = objToSQLRich.toDeleteByIdSQL(c, ids);
		Logger.logSQL("deleteById SQL: ", sql);
		return getBeeSql().modify(sql);
	}

}
