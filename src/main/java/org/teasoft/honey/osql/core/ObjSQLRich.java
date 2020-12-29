/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidRich;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLRich extends ObjSQL implements SuidRich {

	private ObjToSQLRich objToSQLRich; // = BeeFactory.getHoneyFactory().getObjToSQLRich();
//	private BeeSql beeSql = BeeFactory.getHoneyFactory().getBeeSql();
	
	public ObjToSQLRich getObjToSQLRich() {
		if(objToSQLRich==null) objToSQLRich=BeeFactory.getHoneyFactory().getObjToSQLRich();
		return objToSQLRich;
	}

	public void setObjToSQLRich(ObjToSQLRich objToSQLRich) {
		this.objToSQLRich = objToSQLRich;
	}

	@Override
	public <T> List<T> select(T entity, int size) {
		if (entity == null) return null;
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		String sql = getObjToSQLRich().toSelectSQL(entity, size);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		String sql = getObjToSQLRich().toSelectSQL(entity, start, size);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> List<T> select(T entity, String selectField) {//sqlLib.selectSomeField
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = getObjToSQLRich().toSelectSQL(entity, selectField);
			list = getBeeSql().selectSomeField(sql, entity);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> List<T> select(T entity, String selectFields, int start, int size) {
		if (entity == null) return null;
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		List<T> list = null;
		String sql = getObjToSQLRich().toSelectSQL(entity, selectFields,start,size);
		list = getBeeSql().selectSomeField(sql, entity);

		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = getObjToSQLRich().toSelectOrderBySQL(entity, orderFields);
			Logger.logSQL("selectOrderBy SQL: ", sql);
			list = getBeeSql().select(sql, entity);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields, OrderType[] orderTypes) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			String sql = getObjToSQLRich().toSelectOrderBySQL(entity, orderFields, orderTypes);
			Logger.logSQL("selectOrderBy SQL: ", sql);
			list = getBeeSql().select(sql, entity);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> int insert(T entity[]) {
		if (entity == null || entity.length<1) return -1;
//		int len = entity.length;
//		String insertSql[] = new String[len];
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity);
		_regEntityClass1(entity[0]);
		
		return getBeeSql().batch(insertSql);
	}

	@Override
	public <T> int insert(T entity[], String excludeFields) {
		if (entity == null || entity.length<1) return -1;
//		int len = entity.length;
//		String insertSql[] = new String[len];
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity, excludeFields);
		_regEntityClass1(entity[0]);
		
		return getBeeSql().batch(insertSql);
	}

	@Override
	public <T> int insert(T entity[], int batchSize) {
		if (entity == null || entity.length<1) return -1;
//		int len = entity.length;
//		String insertSql[] = new String[len];
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity,batchSize);
		_regEntityClass1(entity[0]);

		return getBeeSql().batch(insertSql, batchSize);
	}

	@Override
	public <T> int insert(T entity[], int batchSize, String excludeFields) {
		if (entity == null || entity.length<1) return -1;
//		int len = entity.length;
//		String insertSql[] = new String[len];
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity,batchSize, excludeFields);
		_regEntityClass1(entity[0]);
		
		return getBeeSql().batch(insertSql, batchSize);
	}

	@Override
	public <T> int update(T entity, String updateFields) {
		if (entity == null) return -1;
		int r = 0;
		String sql = getObjToSQLRich().toUpdateSQL(entity, updateFields);
		Logger.logSQL("update SQL(updateFields) :", sql);
		_regEntityClass1(entity);
		r = getBeeSql().modify(sql);

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
			String sql = getObjToSQLRich().toSelectFunSQL(entity, functionType, fieldForFun);
			_regEntityClass1(entity);
			s=getBeeSql().selectFun(sql);
		} catch (ObjSQLException e) {
			throw e;
		}

		return s;
	}

	@Override
	public <T> int update(T entity, String updateFields, IncludeType includeType) {
		if (entity == null) return -1;
		int r = 0;
		String sql = getObjToSQLRich().toUpdateSQL(entity, updateFields, includeType);
		Logger.logSQL("update SQL(updateFields) :", sql);
		_regEntityClass1(entity);
		r = getBeeSql().modify(sql);

		return r;
	}

	@Override
	public <T> List<T> select(T entity, IncludeType includeType) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectSQL(entity, includeType);
		Logger.logSQL("select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> int update(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		String sql = getObjToSQLRich().toUpdateSQL(entity, includeType);
		Logger.logSQL("update SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().modify(sql);
	}

	@Override
	public <T> int insert(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		String sql = getObjToSQLRich().toInsertSQL(entity, includeType);
		Logger.logSQL("insert SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().modify(sql);
	}

	@Override
	public <T> int delete(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		String sql = getObjToSQLRich().toDeleteSQL(entity, includeType);
		Logger.logSQL("delete SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().modify(sql);
	}

	@Override
	public <T> List<String[]> selectString(T entity) {

		if (entity == null) return null;

		List<String[]> list = null;
		String sql = getObjToSQLRich().toSelectSQL(entity);
		Logger.logSQL("List<String[]> select SQL: ", sql);
		_regEntityClass1(entity);
		list = getBeeSql().select(sql);

		return list;
	}

	@Override
	public <T> List<String[]> selectString(T entity, String selectFields) {

		if (entity == null) return null;

		List<String[]> list = null;
		try {
			String sql = getObjToSQLRich().toSelectSQL(entity, selectFields);
			_regEntityClass1(entity);
			list = getBeeSql().select(sql);
		} catch (ObjSQLException e) {
			throw e;
		}

		return list;
	}

	@Override
	public <T> String selectJson(T entity) {
		
		if (entity == null) return null;
		
		String sql = getObjToSQLRich().toSelectSQL(entity);
		Logger.logSQL("selectJson SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().selectJson(sql);
	}

	@Override
	public <T> String selectJson(T entity, IncludeType includeType) {
		if (entity == null) return null;
		
		String sql = getObjToSQLRich().toSelectSQL(entity, includeType);
		Logger.logSQL("selectJson SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().selectJson(sql);
	}

	@Override
	public <T> T selectById(T entity, Integer id) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		List<T> list = getBeeSql().select(sql, entity);
		
		return getIdEntity(list);
	}
	
	@Override
	public <T> T selectById(T entity, Long id) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		List<T> list = getBeeSql().select(sql, entity);
		
		return getIdEntity(list);
	}
	
	@Override
	public <T> T selectById(T entity, String id) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		
		List<T> list = getBeeSql().select(sql, entity);
		
		return getIdEntity(list);
	}
	
	private <T> T getIdEntity(List<T> list) {
		if(list==null || list.size()<1) {
			return null;
		}else {
			return list.get(0);
		}
	}

	@Override
	public <T> List<T> selectByIds(T entity, String ids) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, ids);
		Logger.logSQL("selectByIds SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Integer id) {
		if (c == null) return 0;
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, id);
		Logger.logSQL("deleteById SQL: ", sql);
		_regEntityClass2(c);
		return getBeeSql().modify(sql);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Long id) {
		if (c == null) return 0;
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, id);
		Logger.logSQL("deleteById SQL: ", sql);
		_regEntityClass2(c);
		return getBeeSql().modify(sql);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, String ids) {
		if (c == null) return 0;
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, ids);
		Logger.logSQL("deleteById SQL: ", sql);
		_regEntityClass2(c);
		return getBeeSql().modify(sql);
	}

	@Override
	@Deprecated
	public <T> List<T> select(T entity, IncludeType includeType, Condition condition) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectSQL(entity, includeType,condition);
		Logger.logSQL("select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}

	@Override
	public <T> String selectJson(T entity, IncludeType includeType, Condition condition) {
		if (entity == null) return null;
		
		String sql = getObjToSQLRich().toSelectSQL(entity, includeType,condition);
		Logger.logSQL("selectJson SQL: ", sql);
		_regEntityClass1(entity);
		
		return getBeeSql().selectJson(sql);
	}

	@Override
	public <T> int updateBy(T entity, String whereFields) {
		if (entity == null) return -1;
		int r = 0;
		String sql = getObjToSQLRich().toUpdateBySQL(entity, whereFields); //updateBy
		Logger.logSQL("update SQL(whereFields) :", sql);
		_regEntityClass1(entity);
		r = getBeeSql().modify(sql);

		return r;
	}

	@Override
	public <T> int updateBy(T entity, String whereFields, IncludeType includeType) {
		if (entity == null) return -1;
		int r = 0;
		String sql = getObjToSQLRich().toUpdateBySQL(entity, whereFields, includeType);//updateBy
		Logger.logSQL("update SQL(whereFields) :", sql);
		_regEntityClass1(entity);
		r = getBeeSql().modify(sql);

		return r;
	}
	
	//v1.7.2
	@Override
	public <T> int updateBy(T entity, String whereFields, Condition condition) {
		if (entity == null) return -1;
		int r = 0;
		String sql = getObjToSQLRich().toUpdateBySQL(entity, whereFields, condition);//updateBy
		Logger.logSQL("update SQL(whereFields) :", sql);
		_regEntityClass1(entity);
		r = getBeeSql().modify(sql);

		return r;
	}

	//v1.7.2
	@Override
	public <T> int update(T entity, String updateFields, Condition condition) {
		if (entity == null) return -1;
		int r = 0;
		String sql = getObjToSQLRich().toUpdateSQL(entity, updateFields, condition);
		Logger.logSQL("update SQL(updateFields) :", sql);
		_regEntityClass1(entity);
		r = getBeeSql().modify(sql);
		
		return r;
	}
	
	//v1.8
	@Override
	public <T> int update(T entity, Condition condition) {
		if (entity == null) return -1;
		int r = 0;
		String sql = getObjToSQLRich().toUpdateSQL(entity, "", condition);
		Logger.logSQL("update SQL(condition) :", sql);
		_regEntityClass1(entity);
		r = getBeeSql().modify(sql);
		
		return r;
	}
	
	//1.9
	@Override
	public <T> int insert(List<T> entityList) {
		if (entityList == null || entityList.size()<1) return -1;
		T entity[]=toEntityArray(entityList);
		return insert(entity);
	}
	
	//1.9
	@Override
	public <T> int insert(List<T> entityList, int batchSize) {
		if (entityList == null || entityList.size()<1) return -1;
		T entity[]=toEntityArray(entityList);
		return insert(entity, batchSize);
	}

	//1.9
	@Override
	public <T> int insert(List<T> entityList, String excludeFields) {
		if (entityList == null || entityList.size()<1) return -1;
		T entity[]=toEntityArray(entityList);
		return insert(entity, excludeFields);
	}

	//1.9
	@Override
	public <T> int insert(List<T> entityList, int batchSize, String excludeFields) {
		if (entityList == null || entityList.size()<1) return -1;
		T entity[]=toEntityArray(entityList);
		
		return insert(entity, batchSize, excludeFields);
	}

	private <T> void _regEntityClass1(T entity){
		if(entity==null) return ;
		HoneyContext.regEntityClass(entity.getClass());
	}
	
	@SuppressWarnings("rawtypes")
	private void _regEntityClass2(Class clazz){
		HoneyContext.regEntityClass(clazz);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T[] toEntityArray(List<T> entityList) {
		
		int len=entityList.size();
		T entity[]=(T[])new Object[len];
		
		for (int i=0; i < len; i++) {
			entity[i]=entityList.get(i);
		}
		
		return entity;
	}

}
