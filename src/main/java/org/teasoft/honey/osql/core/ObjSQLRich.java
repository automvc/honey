/*
 * Copyright 2013-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.osql.autogen.DdlToSql;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.sharding.engine.batch.ShardingBatchInsertEngine;
import org.teasoft.honey.sharding.engine.batch.ShardingForkJoinBatchInsertEngine;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;
import org.teasoft.honey.util.SuidHelper;

/**
 * SuidRich实现类.Suidrich implementation class.
 * @author Kingstar
 * @since  1.0
 */
public class ObjSQLRich extends ObjSQL implements SuidRich, Serializable {

	private static final long serialVersionUID = 1596710362258L;
	
	private ObjToSQLRich objToSQLRich; 
	
	private static final String SELECT_SQL = "select SQL: ";
	private static final String SELECT_JSON_SQL = "selectJson SQL: ";
	private static final String DELETE_BY_ID_SQL = "deleteById SQL: ";
	private static final String SELECT_BY_ID_SQL = "selectById SQL: ";
	private static final String UPDATE_SQL_WHERE_FIELDS = "update SQL(whereFields) :";
	private static final String UPDATE_SQL_UPDATE_FIELDS = "update SQL(updateFields) :";
	private static final String ID_IS_NULL = "in method selectById,id is null! ";
	private static final String START_GREAT_EQ_0 = StringConst.START_GREAT_EQ_0;
	private static final String SIZE_GREAT_0 = StringConst.SIZE_GREAT_0;
	private static final String TIP_SIZE_0 = "The size is 0, but it should be greater than 0 (>0)";
	
	private int defaultBatchSize = HoneyConfig.getHoneyConfig().insertBatchSize;
	
    
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
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return Collections.emptyList();
		}
		if (size < 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		List<T> list =null;
        try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectSQL(entity, -1, size);
		sql = doAfterCompleteSql(sql);

		list = getBeeSql().select(sql, toClassT(entity));
        }finally {
		 doBeforeReturn(list);
        }

		return list;
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return Collections.emptyList();
		}
		if(size<0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if(start<0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);
		List<T> list =null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectSQL(entity, start, size);
		sql = doAfterCompleteSql(sql);
		
		list = getBeeSql().select(sql, toClassT(entity));
		}finally {
		doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> List<T> select(T entity, String... selectFields) {// sqlLib.selectSomeField
		if (entity == null) return null;
		List<T> list = null;
		try {
		doBeforePasreEntity(entity, SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectSQL(entity, selectFields);
		sql = doAfterCompleteSql(sql);
		list = getBeeSql().selectSomeField(sql, toClassT(entity));
		}finally {
		 doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> List<T> select(T entity, int start, int size, String... selectFields) {
		if (entity == null) return null;
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return Collections.emptyList();
		}
		if(size<0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if(start<0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);
		List<T> list = null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectSQL(entity, start, size, selectFields);
		sql = doAfterCompleteSql(sql);
		list = getBeeSql().selectSomeField(sql, toClassT(entity));
		}finally {
		 doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields) {
		if (entity == null) return null;
		List<T> list = null;
		try {
		doBeforePasreEntity(entity, SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectOrderBySQL(entity, orderFields);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("selectOrderBy SQL: ", sql);
		list = getBeeSql().select(sql, toClassT(entity));
	    }finally {
		 doBeforeReturn(list);
	    }
		return list;
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields, OrderType[] orderTypes) {
		if (entity == null) return null;
		List<T> list = null;
		try {
		doBeforePasreEntity(entity, SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectOrderBySQL(entity, orderFields, orderTypes);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("selectOrderBy SQL: ", sql);
		list = getBeeSql().select(sql, toClassT(entity));
		}finally {
		 doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> int insert(T entity[]) {
		return insert(entity, defaultBatchSize, "");
	}
	
	@Override
	public <T> int insert(T entity[], String excludeFields) {
		return insert(entity, defaultBatchSize, "");
	}

	@Override
	public <T> int insert(T entity[], int batchSize) {
		return insert(entity, batchSize, "");
	}

	@Override
	public <T> int insert(T entity[], int batchSize, String excludeFields) {
		if (entity == null || entity.length < 1) return -1;
		checkNull(entity);
		if (batchSize <= 0) batchSize = 10;
		
		if (ShardingUtil.isShardingBatchInsertDoing()) { // 正在执行分片的,不再走以下判断 // 防止重复解析
			return _insert(entity, batchSize, excludeFields);
		}
		int a = 0;
		try {
		HoneyUtil.setInitArrayIdByAuto(entity); //V2.1
		doBeforePasreEntity(entity, SuidType.INSERT);
		
		List<String> tabNameListForBatch = HoneyContext.getListLocal(StringConst.TabNameListForBatchLocal);
		List<String> dsNameListForBatch = HoneyContext.getListLocal(StringConst.DsNameListForBatchLocal);
		
		if (!ShardingUtil.isSharding() || (ObjectUtils.isEmpty(tabNameListForBatch) && ObjectUtils.isEmpty(dsNameListForBatch)) ) {
			a = _insert(entity, batchSize, excludeFields);
		} else {
			try {
				String tableName=_toTableName(entity[0]);
				Logger.logSQL("Batch insert, tableName:"+tableName+"  dsNameList:"+dsNameListForBatch+ "  tabNameList:"+tabNameListForBatch);
				boolean isBroadcastTab=ShardingRegistry.isBroadcastTab(tableName);
				boolean forkJoin = HoneyConfig.getHoneyConfig().sharding_forkJoinBatchInsert;
				if (forkJoin && !isBroadcastTab)
					a = new ShardingForkJoinBatchInsertEngine<T>().batchInsert(entity, batchSize, excludeFields, tabNameListForBatch, this);
				else
					a = new ShardingBatchInsertEngine<T>().batchInsert(entity, batchSize, excludeFields, tabNameListForBatch, this);
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
			} finally {
				HoneyContext.removeSysCommStrLocal(StringConst.ShardingBatchInsertDoing);
			}
		}

		HoneyUtil.revertId(entity); //2.1
		}finally {
		doBeforeReturn();
		}
		return a;
	}
	
	private <T> int _insert(T entity[], int batchSize, String excludeFields) {
		
		String insertSql[] = getObjToSQLRich().toInsertSQL(entity,batchSize, excludeFields);
		_regEntityClass1(entity[0]);
		insertSql[0] = doAfterCompleteSql(insertSql[0]);
		
//		HoneyUtil.revertId(entity); //close since V2.1
		
		int a= getBeeSql().batch(insertSql, batchSize);
		
		return a;
	}
	
	private String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	private <T> void checkNull(T entity[]) {
		for (int i = 0; i < entity.length; i++) {
			if(entity[i]==null) throw new ObjSQLException("entity[] have null element, index: "+i);
		}
	}
	
	@Override
	public <T> int update(T entity, String... updateFields) {
		return _update(entity, updateFields);
	}

	private <T> int _update(T entity, String... updateFields) {
		if (entity == null) return -1;
		int r = 0;
		try {
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateSQL(entity, updateFields);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(UPDATE_SQL_UPDATE_FIELDS, sql);
		r = getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return r;
	}

	@Override
	public <T> T selectOne(T entity) {
		if (entity == null) return null;
//		List<T> list = select(entity);  //已处理拦截器链
		List<T> list = select(entity, 2);  //已处理拦截器链     2.0
		if (list == null || list.size() != 1) return null;
		return list.get(0);
	}
	
	@Override
	public <T> T selectFirst(T entity, Condition condition) {
		if (entity == null) return null;
		
		List<T> list;
		if (condition == null) {
			list = select(entity, 1);
		} else {
			condition.size(1);
			list = select(entity, condition);
		}

		if (list == null || list.size() < 1) return null;
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
		try {
		regCondition(condition);
		doBeforePasreEntity(entity,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectFunSQL(entity, functionType, fieldForFun, condition);
		_regEntityClass1(entity);
		_regFunType(functionType);
		sql = doAfterCompleteSql(sql);
		s = getBeeSql().selectFun(sql);
		}finally {
		doBeforeReturn();
		}
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
	public <T> int update(T entity, IncludeType includeType, String... updateFields) {
		if (entity == null) return -1;
		int r = 0;
		try {
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateSQL(entity, includeType, updateFields);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(UPDATE_SQL_UPDATE_FIELDS, sql);
		r = getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return r;
	}

	@Override
	public <T> List<T> select(T entity, IncludeType includeType) {
		if (entity == null) return null;
		List<T> list =null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectSQL(entity, includeType);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		list = getBeeSql().select(sql, toClassT(entity));
		}finally {
		doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> int update(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		int r=0;
		try {
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateSQL(entity, includeType);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("update SQL: ", sql);
		
		r= getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return r;
	}

	@Override
	public <T> int insert(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		int r=0;
		try {
		doBeforePasreEntity(entity,SuidType.INSERT);
		String sql = getObjToSQLRich().toInsertSQL(entity, includeType);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("insert SQL: ", sql);
		if(OneTimeParameter.isTrue("_SYS_Bee_NullObjectInsert")){
			Logger.warn("All fields in object is null,would ignroe it!");
			r=0;
		}else {
		    r= getBeeSql().modify(sql);
		}
		HoneyUtil.revertId(entity);  //fixed bug
		}finally {
		doBeforeReturn();
		}
		return r;
	}
	
	@Override
	public <T> long insertAndReturnId(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		try {
		doBeforePasreEntity(entity,SuidType.INSERT);
		String sql = getObjToSQLRich().toInsertSQL(entity, includeType);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("insert SQL: ", sql);
		
		if(OneTimeParameter.isTrue("_SYS_Bee_NullObjectInsert")){
			Logger.warn("All fields in object is null,would ignroe it!");
			return -1;
		}
		
		return _insertAndReturnId(entity, sql);
		} finally {
		  doBeforeReturn();
		}
		
	}

	@Override
	public <T> int delete(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		int r=0;
		try {
		doBeforePasreEntity(entity,SuidType.DELETE);
		String sql = getObjToSQLRich().toDeleteSQL(entity, includeType);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("delete SQL: ", sql);
		r= getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return r;
	}

	@Override
	public <T> List<String[]> selectString(T entity) {

		if (entity == null) return null;
		
		List<String[]> list = null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);

		String sql = getObjToSQLRich().toSelectSQL(entity);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
//		Logger.logSQL("List<String[]> select SQL: ", sql);
		Logger.logSQL("select SQL(return List<String[]>): ", sql);
		list = getBeeSql().select(sql);
		}finally {
		doBeforeReturn();
		}
		return list;
	}

	@Override
	public <T> List<String[]> selectString(T entity, String... selectFields) {

		if (entity == null) return null;
		List<String[]> list = null;
		
		try {
		doBeforePasreEntity(entity, SuidType.SELECT);
		
		String sql = getObjToSQLRich().toSelectSQL(entity, selectFields);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		list = getBeeSql().select(sql);
	    }finally {
		doBeforeReturn();
	    }
		
		return list;
	}
	
	@Override
	public <T> List<String[]> selectString(T entity, Condition condition) {
		if (entity == null) return null;
		List<String[]> list = null;
		try {
		regCondition(condition);
		doBeforePasreEntity(entity,SuidType.SELECT);
		OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding); 
//		String sql = getObjToSQLRich().toSelectSQL(entity, condition.getIncludeType(), condition);
		String sql = getObjToSQLRich().toSelectSQL(entity, condition);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
//		Logger.logSQL(SELECT_SQL, sql);
		Logger.logSQL("select SQL(return List<String[]>): ", sql);
		list= getBeeSql().select(sql);
		}finally {
		doBeforeReturn();
		}
		return list;
	}
	
	@Override
	public <T> String selectJson(T entity) {
		
		if (entity == null) return null;
		String json =null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		
		String sql = getObjToSQLRich().toSelectSQL(entity);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_JSON_SQL, sql);
		json = getBeeSql().selectJson(sql);
		}finally {
		doBeforeReturn();
		}
		return json;
	}

	@Override
	public <T> String selectJson(T entity, IncludeType includeType) {
		if (entity == null) return null;
		String json=null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		
		String sql = getObjToSQLRich().toSelectSQL(entity, includeType);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_JSON_SQL, sql);
		json= getBeeSql().selectJson(sql);
		}finally {
		doBeforeReturn();
		}
		return json;
	}
	
	@Override
	public <T> String selectJson(T entity, String... selectField) {
		if (entity == null) return null;
		String json=null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		
		String sql = getObjToSQLRich().toSelectSQL(entity, selectField);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("selectJson(T entity, String selectField) SQL: ", sql);
		json= getBeeSql().selectJson(sql);
		}finally {
		doBeforeReturn();
		}
		return json;
	}
	
	@Override
	public <T> String selectJson(T entity, int start, int size, String... selectFields) {
		if (entity == null) return null;
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return null;
		}
		if(size<0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if(start<0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);
		String json=null;
		try {
		doBeforePasreEntity(entity,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectSQL(entity,start,size, selectFields);
		_regEntityClass1(entity); 
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("selectJson(T entity, String selectField, int start, int size) SQL: ", sql);
		json= getBeeSql().selectJson(sql);
		}finally {
		doBeforeReturn();
		}
		return json;
	}

	@Override
	public <T> T selectById(Class<T> entityClass, Integer id) {
		if (entityClass == null) return null;

		if (id==null) {
			Logger.warn(ID_IS_NULL);
			return null;
		}
		List<T> list =null;
		try {
		doBeforePasreEntity(entityClass,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectByIdSQL(entityClass, id);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_BY_ID_SQL, sql);
		list = getBeeSql().select(sql, entityClass);
		
		return getIdEntity(list);
		}finally {
		  _doBeforeReturn_IdEntity(list);
		}
	}
	
	@Override
	public <T> T selectById(Class<T> entityClass, Long id) {
		if (entityClass == null) return null;

		if (id==null) {
			Logger.warn(ID_IS_NULL);
			return null;
		}
		List<T> list =null;
		try {
		doBeforePasreEntity(entityClass,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectByIdSQL(entityClass, id);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_BY_ID_SQL, sql);
		list = getBeeSql().select(sql, entityClass);
		
		return getIdEntity(list);
		}finally {
		 _doBeforeReturn_IdEntity(list);
		}
	}
	
	@Override
	public <T> T selectById(Class<T> entityClass, String id) {
		if (entityClass == null) return null;

		if (id==null) {
			Logger.warn(ID_IS_NULL);
			return null;
		}
		if(id.contains(",")) {
			throw new BeeIllegalParameterException("The parameter 'id' of method selectById does not allow to contain comma!");
		}
		List<T> list =null;
		try {
		doBeforePasreEntity(entityClass,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectByIdSQL(entityClass, id);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_BY_ID_SQL, sql);
		
		list = getBeeSql().select(sql, entityClass);
		
		return getIdEntity(list);
	    }finally {
		  _doBeforeReturn_IdEntity(list);
		}
	}
	
	@Override
	public <T> List<T> selectByIds(Class<T> entityClass, String ids) {
		if (entityClass == null) return null;

		if (ids==null) {
			Logger.warn("in method selectByIds,ids is null! ");
			return null;
		}
		List<T> list=null;
		try {
		doBeforePasreEntity(entityClass,SuidType.SELECT);
		String sql = getObjToSQLRich().toSelectByIdSQL(entityClass, ids);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("selectByIds SQL: ", sql);
		list= getBeeSql().select(sql, entityClass);
		}finally {
		 doBeforeReturn(list);
		}
		return list;
	}
	
	private <T> T getIdEntity(List<T> list) {
		if(list==null || list.size()<1) {
//			doBeforeReturn();
			return null;
		}else {
//			doBeforeReturn(list);
			return list.get(0);
		}
	}
	
	private <T> void _doBeforeReturn_IdEntity(List<T> list) {
		if(list==null || list.size()<1) {
			doBeforeReturn();
		}else {
			doBeforeReturn(list);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Integer id) {
		if(id==null) Logger.warn("in method deleteById,id is null! ");
		if (c == null || id==null) return 0;
		try {
		doBeforePasreEntity(c,SuidType.DELETE);
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, id);
		_regEntityClass2(c);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(DELETE_BY_ID_SQL, sql);
		int a=getBeeSql().modify(sql);
		return a;
		}finally {
		 doBeforeReturn();
		}
		
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Long id) {
		if(id==null) Logger.warn("in method deleteById,id is null! ");
		if (c == null || id==null) return 0;
		int a=0;
		try {
		doBeforePasreEntity(c,SuidType.DELETE);
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, id);
		_regEntityClass2(c);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(DELETE_BY_ID_SQL, sql);
		a=getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return a;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, String ids) {
		if(ids==null) Logger.warn("in method deleteById,ids is null! ");
		if (c == null || ids==null) return 0;
		int a=0;
		try {
		doBeforePasreEntity(c,SuidType.DELETE);
		String sql = getObjToSQLRich().toDeleteByIdSQL(c, ids);
		_regEntityClass2(c);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(DELETE_BY_ID_SQL, sql);
		a=getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return a;
	}

	@Override
	@Deprecated
	public <T> List<T> select(T entity, IncludeType includeType, Condition condition) {
		if (entity == null) return null;
		if (includeType != null) {
			if (condition == null) condition = BeeFactoryHelper.getCondition();
			condition.setIncludeType(includeType);
		}
		return select(entity, condition);
	}

	@Override
	public <T> String selectJson(T entity, IncludeType includeType, Condition condition) {
		if (entity == null) return null;
		if (includeType != null) {
			if (condition == null) condition = BeeFactoryHelper.getCondition();
			condition.setIncludeType(includeType);
		}
		return selectJson(entity, condition);
	}
	
	@Override
	public <T> String selectJson(T entity, Condition condition) {
		if (entity == null) return null;
		String json=null;
		try {
		regCondition(condition);
		doBeforePasreEntity(entity,SuidType.SELECT);
		_regEntityClass1(entity);
		OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding); 
		String sql = getObjToSQLRich().toSelectSQL(entity, condition);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_JSON_SQL, sql);
		
		json= getBeeSql().selectJson(sql);
		}finally {
		doBeforeReturn();
		}
		return json;
	}

	@Override
	public <T> int updateBy(T entity, String... whereFields) {
		if (entity == null) return -1;
		int r = 0;
		try {
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateBySQL(entity, whereFields); //updateBy
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(UPDATE_SQL_WHERE_FIELDS, sql);
		r = getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return r;
	}

	@Override
	public <T> int updateBy(T entity, IncludeType includeType, String... whereFields) {
		if (entity == null) return -1;
		int r = 0;
		try {
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateBySQL(entity, includeType, whereFields);//updateBy
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(UPDATE_SQL_WHERE_FIELDS, sql);
		r = getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return r;
	}
	
	//v1.7.2
	@Override
	public <T> int updateBy(T entity, Condition condition, String... whereFields) {
		if (entity == null) return -1;
		int r = 0;
		try {
		regCondition(condition);
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateBySQL(entity, condition, whereFields);//updateBy
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(UPDATE_SQL_WHERE_FIELDS, sql);
		r = getBeeSql().modify(sql);
		}finally {
		doBeforeReturn();
		}
		return r;
	}
	
	//v1.9
	@Override
	public <T> int updateById(T entity, Condition condition) {
		String pkName="";
		try {
//			entity.getClass().getDeclaredField("id");
			HoneyUtil.getField(entity.getClass(), "id");
			pkName="id";
		} catch (NoSuchFieldException e) {
			pkName = HoneyUtil.getPkFieldName(entity);
		}
		
		if (StringUtils.isBlank(pkName)) {
			throw new BeeErrorGrammarException(
					"id(primary key) field can not empty!");
		}
		
	   //支持联合主键
       return updateBy(entity, condition, pkName);
	}

	//v1.7.2
	@Override
	public <T> int update(T entity, Condition condition, String... updateFields) {
		if (entity == null) return -1;
		int r = 0;
		try {
		regCondition(condition);
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateSQL(entity, condition, updateFields);
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(UPDATE_SQL_UPDATE_FIELDS, sql);
		r = getBeeSql().modify(sql);
		}finally {
		 doBeforeReturn();
		}
		return r;
	}
	
	//v1.8
	@Override
	public <T> int update(T entity, Condition condition) {
		if (entity == null) return -1;
		int r = 0;
		try {
		regCondition(condition);
		doBeforePasreEntity(entity,SuidType.UPDATE);
		String sql = getObjToSQLRich().toUpdateSQL(entity, condition, "");
		_regEntityClass1(entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("update SQL(condition) :", sql);
		r = getBeeSql().modify(sql);
		}finally {
		 doBeforeReturn();
		}
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
	
	private <T> void _regFunType(FunctionType functionType){
		HoneyContext.regFunType(functionType);
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
		
		// 变长参数后, 出现混淆,需要调整
		if (newEntity.getClass() == String.class) return _update(oldEntity, newEntity.toString());
		
		String oldEntityFullName = oldEntity.getClass().getName();
		String newEntityFullName = newEntity.getClass().getName();
		
		Logger.debug(oldEntityFullName);
		Logger.debug(newEntityFullName);
		
		if (!oldEntityFullName.equals(newEntityFullName)) {
			throw new BeeErrorGrammarException(
					"BeeErrorGrammarException: the oldEntity and newEntity must be same type!");
		}
		
		doBeforePasreEntity(newEntity,SuidType.UPDATE); //拦截器只处理新实体；  旧实体oldEntity作为条件不在拦截器处理。
		Map<String, Object> newMap = SuidHelper.entityToMap(newEntity);

		
//		updateBy不行, 使用updateSet  
		Condition condition=BeeFactoryHelper.getCondition();
		for (Map.Entry<String, Object> entry : newMap.entrySet()) {
			if(HoneyUtil.isNumber(entry.getValue()))
			   condition.set(entry.getKey(), (Number)entry.getValue());
			else
			  condition.set(entry.getKey(), (String)entry.getValue());
		}
		
		Logger.logSQL("update(T oldEntity, T newEntity), ", "");
		
		return update(oldEntity, condition); //这个方法会有:doBeforePasreEntity(entity,SuidType.UPDATE);
		
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
		if(entity==null) return 0;
		Object id = HoneyUtil.getIdValue(entity);
		if (id == null) return insert(entity);
		Object one = selectById(entity.getClass(), id.toString());
		if (one != null)
			return update(entity);
		else
			return insert(entity);
	}
	
	
	/**
	 * 根据Javabean生成数据库表,Javabean无需配置过多的字段信息.此方法只考虑通用情况,若有详细需求,不建议采用
	 * <br>According to the database table generated by JavaBean, JavaBean does not need to configure 
	 * <br>too much field information. This method only considers the general situation, and is not 
	 * <br>recommended if there are detailed requirements.
	 * @param entityClass table's entityClass(do not allow null).
	 * @param isDropExistTable whether drop the exist table before create
	 * @return flag whether create successfully.
	 */
	public <T> boolean createTable(Class<T> entityClass, boolean isDropExistTable) {

		if (entityClass == null) return false;

		if (isDropExistTable) {
			doBeforePasreEntity(entityClass, SuidType.DDL);
			_regEntityClass2(entityClass);
			String dropSql = DdlToSql.toDropTableSql(entityClass);
			try {
				dropSql = doAfterCompleteSql(dropSql);
				Logger.logSQL("Drop SQL:", dropSql);
				_doModifyForDdl(dropSql);
//				doBeforeReturn();
			} catch (Exception e) {
				if (!(HoneyUtil.isOracle() || HoneyUtil.isSqlServer())) {
					try {
						doBeforePasreEntity(entityClass, SuidType.DDL);
						_regEntityClass2(entityClass);
						dropSql = DdlToSql.toDropTableSimpleSql(entityClass);
						dropSql = doAfterCompleteSql(dropSql);
						Logger.logSQL("Drop SQL:", dropSql);
						_doModifyForDdl(dropSql);
//						doBeforeReturn();
					} catch (Exception e2) {
						Logger.warn(e2.getMessage());
					}
				}
			}finally {
				doBeforeReturn();
			}
		} else {// donot Drop ExistTable
			boolean isSharding = ShardingUtil.isSharding();
			if (!isSharding) { //已存在,则不创建      2.0  非分片才检测 
				try {
					int c = count(entityClass.newInstance());
					if (c > 0) return true;
				} catch (Exception e) {

				}
			}
		}
		doBeforePasreEntity(entityClass, SuidType.DDL); //fixed bug 2.1
		_regEntityClass2(entityClass);
		String sql = DdlToSql.toCreateTableSQL(entityClass);
		return _ddlModify(entityClass, sql);
	}
	
	/**
	 * 创建通用索引.create normal index
	 * @param entityClass table's entityClass(do not allow null).
	 * @param fields  field name,if more than one,separate with comma.
	 * @param indexName  index name
	 */
	public <T> void indexNormal(Class<T> entityClass, String fields, String indexName) {
		String PREFIX = "idx_";
		String IndexTypeTip = "normal";
		String IndexType = ""; // normal will empty
		_index(entityClass, fields, indexName, PREFIX, IndexTypeTip, IndexType);
	}
	
	/**
	 * 创建唯一索引.create unique index
	 * @param entityClass table's entity(do not allow null).
	 * @param fields  field name,if more than one,separate with comma.
	 * @param indexName  index name
	 */
	public <T> void unique(Class<T> entityClass, String fields, String indexName) {
		String PREFIX = "uie_";
		String IndexTypeTip = "unique";
		String IndexType = "UNIQUE "; // 后面有一个空格
		_index(entityClass, fields, indexName, PREFIX, IndexTypeTip, IndexType);
	}
	
	private <T> void _index(Class<T> entityClass, String fields, String indexName,
			String PREFIX, String IndexTypeTip, String IndexType) {
		
		doBeforePasreEntity(entityClass, SuidType.DDL);
		_regEntityClass2(entityClass);

		String indexSql = DdlToSql.toIndexSql(entityClass, fields, indexName, PREFIX,
				IndexTypeTip, IndexType);

		_ddlModify(entityClass, indexSql);
	}
	
	/**
	 * 创建主键,一般是多字段联合主键.create primary key
	 * @param entity table's entity(do not allow null).
	 * @param fields  field name,if more than one,separate with comma.
	 * @param keyName  key name
	 */
	public <T> void primaryKey(Class<T> entityClass, String fields, String keyName) {
		doBeforePasreEntity(entityClass, SuidType.DDL);
		_regEntityClass2(entityClass);
		
		String primaryKeySql=DdlToSql.toPrimaryKeySql(entityClass, fields, keyName);
		_ddlModify(entityClass, primaryKeySql);
	}
	
	private void _doModifyForDdl(String sql) {
		boolean old = HoneyConfig.getHoneyConfig().showSql_showExecutableSql;
		if (old) HoneyConfig.getHoneyConfig().showSql_showExecutableSql = false;
		getBeeSql().modify(sql);
		if (old) HoneyConfig.getHoneyConfig().showSql_showExecutableSql = old;
	}
	
	private <T> boolean _ddlModify(Class<T> entityClass,String sql) {
		boolean f = false;
		try {
			doBeforePasreEntity(entityClass, SuidType.DDL);
			_regEntityClass2(entityClass);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL("DDL SQL:", sql);
			_doModifyForDdl(sql);
//			doBeforeReturn();
			f = true;
		} catch (Exception e) {
           Logger.debug(e.getMessage(), e);
		}finally {
			doBeforeReturn();
		}
		return f;
	}
	
	@Override
	public <T> void dropIndex(Class<T> entityClass,  String indexName) {
		doBeforePasreEntity(entityClass, SuidType.DDL);
		_regEntityClass2(entityClass);
		
		String dropIndexSql=DdlToSql.toDropIndexSql(entityClass, indexName); 
		_ddlModify(entityClass, dropIndexSql);
		
	}
	
//	void doBeforePasreEntity(Object entity[], SuidType SuidType) { //fixed bug. no set dataSource name
//		getInterceptorChain().beforePasreEntity(entity, SuidType);
//	}

}
