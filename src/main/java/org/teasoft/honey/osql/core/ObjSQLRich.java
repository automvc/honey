/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.MapSql;
import org.teasoft.bee.osql.MapSqlKey;
import org.teasoft.bee.osql.MapSqlSetting;
import org.teasoft.bee.osql.MapSuid;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidRich;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.util.SuidHelper;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLRich extends ObjSQL implements SuidRich {

    private ObjToSQLRich objToSQLRich; 
	
    public ObjSQLRich() {
    }
    
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
		checkNull(entity);
//		int len = entity.length;
//		String insertSql[] = new String[len];
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity);
		_regEntityClass1(entity[0]);
		
		HoneyUtil.revertId(entity);
		
		return getBeeSql().batch(insertSql);
	}
	
	private <T> void checkNull(T entity[]) {
		for (int i = 0; i < entity.length; i++) {
			if(entity[i]==null) throw new ObjSQLException("entity[] have null element, index: "+i);
		}
	}

	@Override
	public <T> int insert(T entity[], String excludeFields) {
		if (entity == null || entity.length<1) return -1;
		checkNull(entity);
//		int len = entity.length;
//		String insertSql[] = new String[len];
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity, excludeFields);
		_regEntityClass1(entity[0]);
		
		HoneyUtil.revertId(entity);
		
		return getBeeSql().batch(insertSql);
	}

	@Override
	public <T> int insert(T entity[], int batchSize) {
		return insert(entity, batchSize, "");
	}

	@Override
	public <T> int insert(T entity[], int batchSize, String excludeFields) {
		if (entity == null || entity.length<1) return -1;
		checkNull(entity);
		if(batchSize<=0) batchSize=10;
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity,batchSize, excludeFields);
		_regEntityClass1(entity[0]);
		
		HoneyUtil.revertId(entity);
		
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
		return selectWithFun(entity, functionType, fieldForFun, null);
	}

	@Override
	public <T> String selectWithFun(T entity, FunctionType functionType, String fieldForFun, Condition condition) {
		if (entity == null) return null;
		String s = null;
		String sql = getObjToSQLRich().toSelectFunSQL(entity, functionType, fieldForFun, condition);
		_regEntityClass1(entity);
		s = getBeeSql().selectFun(sql);
		return s;
	}

	@Override
	public <T> int count(T entity) {
		return count(entity, null);
	}

	@Override
	public <T> int count(T entity, Condition condition) {
		String total = selectWithFun(entity, FunctionType.COUNT, "*", condition);
		return total == null ? 0 : Integer.parseInt(total);
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
	public <T> List<String[]> selectString(T entity, Condition condition) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectSQL(entity, condition.getIncludeType(), condition);
		Logger.logSQL("select SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().select(sql);
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
	public <T> String selectJson(T entity, String selectField) {
		if (entity == null) return null;
		
		String sql = getObjToSQLRich().toSelectSQL(entity, selectField);
		Logger.logSQL("selectJson(T entity, String selectField) SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().selectJson(sql);
	}
	
	@Override
	public <T> String selectJson(T entity, String selectFields, int start, int size) {
		if (entity == null) return null;
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		String sql = getObjToSQLRich().toSelectSQL(entity, selectFields,start,size);
		
		Logger.logSQL("selectJson(T entity, String selectField, int start, int size) SQL: ", sql);
		_regEntityClass1(entity);
		return getBeeSql().selectJson(sql);
	}

	@Override
	public <T> T selectById(T entity, Integer id) {
		if (entity == null) return null;

		if (id==null) {
			Logger.warn("in method selectById,id is null! ");
			return null;
		}
		
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		List<T> list = getBeeSql().select(sql, entity);
		
		return getIdEntity(list);
	}
	
	@Override
	public <T> T selectById(T entity, Long id) {
		if (entity == null) return null;

		if (id==null) {
			Logger.warn("in method selectById,id is null! ");
			return null;
		}
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		List<T> list = getBeeSql().select(sql, entity);
		
		return getIdEntity(list);
	}
	
	@Override
	public <T> T selectById(T entity, String id) {
		if (entity == null) return null;

		if (id==null) {
			Logger.warn("in method selectById,id is null! ");
			return null;
		}
		if(id.contains(",")) {
			throw new BeeIllegalParameterException("The parameter 'id' of method selectById does not allow to contain comma!");
		}
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, id);
		Logger.logSQL("selectById SQL: ", sql);
		
		List<T> list = getBeeSql().select(sql, entity);
		
		return getIdEntity(list);
	}
	
	@Override
	public <T> List<T> selectByIds(T entity, String ids) {
		if (entity == null) return null;

		if (ids==null) {
			Logger.warn("in method selectByIds,ids is null! ");
			return null;
		}
		String sql = getObjToSQLRich().toSelectByIdSQL(entity, ids);
		Logger.logSQL("selectByIds SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}
	
	private <T> T getIdEntity(List<T> list) {
		if(list==null || list.size()<1) {
			return null;
		}else {
			return list.get(0);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Integer id) {
		if(id==null) Logger.warn("in method deleteById,id is null! ");
		if (c == null || id==null) return 0;
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, id);
		Logger.logSQL("deleteById SQL: ", sql);
		_regEntityClass2(c);
		return getBeeSql().modify(sql);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Long id) {
		if(id==null) Logger.warn("in method deleteById,id is null! ");
		if (c == null || id==null) return 0;
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, id);
		Logger.logSQL("deleteById SQL: ", sql);
		_regEntityClass2(c);
		return getBeeSql().modify(sql);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, String ids) {
		if(ids==null) Logger.warn("in method deleteById,ids is null! ");
		if (c == null || ids==null) return 0;
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, ids);
		Logger.logSQL("deleteById SQL: ", sql);
		_regEntityClass2(c);
		return getBeeSql().modify(sql);
	}

	@Override
	@Deprecated
	public <T> List<T> select(T entity, IncludeType includeType, Condition condition) {
		if (entity == null) return null;
		String sql = getObjToSQLRich().toSelectSQL(entity, includeType, condition);
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
	public <T> String selectJson(T entity, Condition condition) {
		if (entity == null) return null;
		
		String sql = getObjToSQLRich().toSelectSQL(entity, condition.getIncludeType(),condition);
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
	
	//v1.9
	@Override
	public <T> int updateById(T entity, Condition condition) {
       return updateBy(entity, "id", condition);
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

	//没能将entity传到SqlLib,需要注册
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
	
	@Override
	public SuidRich setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}
	
	@Override
	public <T> boolean exist(T entity) {
		int r = count(entity);
		return r > 0 ? true : false;
	}

	@Override
	public <T> int update(T oldEntity, T newEntity) {

		if (oldEntity == null || newEntity == null) return -1;
		String oldEntityFullName = oldEntity.getClass().getName();
		String newEntityFullName = newEntity.getClass().getName();
		if (!oldEntityFullName.equals(newEntityFullName)) {
			throw new BeeErrorGrammarException(
					"BeeErrorGrammarException: the oldEntity and newEntity must be same type!");
		}

		Map<String, Object> oldMap = SuidHelper.entityToMap(oldEntity);
		Map<String, Object> newMap = SuidHelper.entityToMap(newEntity);

		MapSql updateMapSql = BeeFactoryHelper.getMapSql();
		updateMapSql.put(MapSqlKey.Table, _toTableName(oldEntity));
		updateMapSql.put(MapSqlSetting.IsNamingTransfer, true);
		updateMapSql.put(oldMap);
		updateMapSql.putNew(newMap);

		Logger.logSQL("update(T oldEntity, T newEntity) with MapSuid, ", "");
		MapSuid mapSuid = BeeFactoryHelper.getMapSuid();
		return mapSuid.update(updateMapSql);
	}
	
	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	/**
	 * 保存一个实体(一条记录).
	 * 如果可以区分开,建议明确调用insert(entity)或者update(entity),这样更加安全和高效.
	 * @param entity
	 * @return 返回受影响的行数.
	 * @since 1.9.8
	 */
	@Override
	public <T> int save(T entity) {
		Object id = HoneyUtil.getIdValue(entity);
		if (id == null) return insert(entity);
		Object one = selectById(entity, id.toString());
		if (one != null)
			return update(entity);
		else
			return insert(entity);
	}

}
