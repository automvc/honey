/*
 * Copyright 2013-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.shortcut.BF;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.config.ShardingRegistry;
import org.teasoft.honey.sharding.engine.batch.ShardingBatchInsertEngine;
import org.teasoft.honey.sharding.engine.batch.ShardingForkJoinBatchInsertEngine;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;
import org.teasoft.honey.util.currency.CurrencyArithmetic;

/**
 * SuidRich实现类.Suidrich implementation class.
 * @author Kingstar
 * @since  2.0
 */
public class MongodbObjSQLRich extends MongodbObjSQL implements SuidRich, Serializable {

	private static final long serialVersionUID = 1596710362263L;

	private static final String ID_IS_NULL = "in method selectById, id is null! ";
	private static final String START_GREAT_EQ_0 = StringConst.START_GREAT_EQ_0;
	private static final String SIZE_GREAT_0 = StringConst.SIZE_GREAT_0;
	private static final String TIP_SIZE_0 = "The size is 0, but it should be greater than 0 (>0)";

	private int defaultBatchSize = HoneyConfig.getHoneyConfig().insertBatchSize;

	@Override
	public <T> List<T> select(T entity, int size) {
		if (entity == null) return null;
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return Collections.emptyList();
		}
		if (size < 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);

		return select(entity, 0, size);
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return Collections.emptyList();
		}
		if (size < 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		Condition condition = BeeFactoryHelper.getCondition();
		condition.start(start).size(size);

		return select(entity, condition);
	}

	@Override
	public <T> List<T> select(T entity, String... selectField) {
		if (entity == null) return null;

		Condition condition = BeeFactoryHelper.getCondition();
		condition.selectField(selectField);

		return select(entity, condition);
	}

	@Override
	public <T> List<T> select(T entity, int start, int size, String... selectFields) {
		if (entity == null) return null;
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return Collections.emptyList();
		}
		if (size < 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		Condition condition = BeeFactoryHelper.getCondition();
		condition.selectField(selectFields);
		condition.start(start).size(size);

		return select(entity, condition);
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields) {
		return selectOrderBy(entity, orderFields, null);
	}

	@Override
	public <T> List<T> selectOrderBy(T entity, String orderFields, OrderType[] orderTypes) {
		if (entity == null) return null;

		List<T> list = null;
		try {
			doBeforePasreEntity(entity, SuidType.SELECT);

			list = getMongodbBeeSql().selectOrderBy(entity, orderFields, orderTypes);
		} finally {
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
			HoneyUtil.setInitArrayIdByAuto(entity);
			doBeforePasreEntity(entity, SuidType.INSERT);

			List<String> tabNameListForBatch = HoneyContext.getListLocal(StringConst.TabNameListForBatchLocal);
			List<String> dsNameListForBatch = HoneyContext.getListLocal(StringConst.DsNameListForBatchLocal);

			if (!ShardingUtil.isSharding()
					|| (ObjectUtils.isEmpty(tabNameListForBatch) && ObjectUtils.isEmpty(dsNameListForBatch))) {
				a = _insert(entity, batchSize, excludeFields);
			} else {
				try {
					String tableName = _toTableName(entity[0]);
					Logger.logSQL("Mongodb::batch insert, Collection:" + tableName + "  dsNameList:" + dsNameListForBatch
							+ "  collectionNameList:" + tabNameListForBatch);
					boolean isBroadcastTab = ShardingRegistry.isBroadcastTab(tableName);
					boolean forkJoin = HoneyConfig.getHoneyConfig().sharding_forkJoinBatchInsert;
					if (forkJoin && !isBroadcastTab) // BroadcastTab not use forkJoin
						a = new ShardingForkJoinBatchInsertEngine<T>().batchInsert(entity, batchSize, excludeFields, tabNameListForBatch, this);
					else 
						a = new ShardingBatchInsertEngine<T>().batchInsert(entity, batchSize, excludeFields, tabNameListForBatch, this);
				} catch (Exception e) {
					Logger.warn(e.getMessage(), e);
				} finally {
					HoneyContext.removeSysCommStrInheritableLocal(StringConst.ShardingBatchInsertDoing);
				}
			}

			HoneyUtil.revertId(entity);
		} finally {
			doBeforeReturn();
		}
		return a;
	}

	private <T> int _insert(T entity[], int batchSize, String excludeFields) {

//		HoneyUtil.setInitArrayIdByAuto(entity);
//		int a = getMongodbBeeSql().insert(entity, batchSize, excludeFields);
//		HoneyUtil.revertId(entity);
//		return a;

		return getMongodbBeeSql().insert(entity, batchSize, excludeFields);
	}

	private String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	private <T> void checkNull(T entity[]) {
		for (int i = 0; i < entity.length; i++) {
			if (entity[i] == null) throw new ObjSQLException("entity[] have null element, index: " + i);
		}
	}

	@Override
	public <T> int update(T entity, String... updateFields) {
		return _update(entity, updateFields);
	}

	public <T> int _update(T entity, String... updateFields) {
		Condition condition = null;
		return update(entity, condition, updateFields);
	}

	@Override
	public <T> T selectOne(T entity) {
		if (entity == null) return null;
		List<T> list = select(entity, 2); // 已处理拦截器链
		if (list == null || list.size() != 1) return null;
		return list.get(0);
	}

	@Override
	public <T> T selectFirst(T entity, Condition condition) {
		if (entity == null) return null;

		if (condition == null) {
			condition = BeeFactoryHelper.getCondition();
		} else {
			condition = condition.clone();
		}
		condition.size(1);

		List<T> list = select(entity, condition);
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

		if (FunctionType.COUNT == functionType) return count(entity, condition) + "";
		try {
			if (condition != null) condition = condition.clone();
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.SELECT);

			_regEntityClass1(entity);
			_regFunType(functionType);

			String rs = "";
			if (FunctionType.AVG == functionType && ShardingUtil.hadSharding()
					&& HoneyContext.getSqlIndexLocal() == null) { // avg sharding
				int count = count(entity, condition);
				String sum = selectWithFun(entity, FunctionType.SUM, fieldForFun, condition);
				if (count == 0 || StringUtils.isBlank(sum)) return ""; // fixed bug
				rs = CurrencyArithmetic.divide(sum, count + "");
			} else {
				rs = getMongodbBeeSql().selectWithFun(entity, functionType, fieldForFun, condition);
			}
			return rs;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> int count(T entity) {
		return count(entity, null);
	}

	@Override
	public <T> int count(T entity, Condition condition) {

		if (entity == null) return 0;
		try {
			if (condition != null) condition = condition.clone();
			regCondition(condition);
			_regFunType(FunctionType.COUNT);
			doBeforePasreEntity(entity, SuidType.SELECT);
			if (condition != null) condition.setSuidType(SuidType.SELECT);
			int c = getMongodbBeeSql().count(entity, condition);

			return c;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> int update(T entity, IncludeType includeType, String... updateFields) {
		return update(entity, _getCondition(includeType), updateFields);
	}

	private Condition _getCondition(IncludeType includeType) {
		return BeeFactoryHelper.getCondition().setIncludeType(includeType);
	}

	@Override
	public <T> List<T> select(T entity, IncludeType includeType) {
		if (entity == null) return null;
		return select(entity, _getCondition(includeType));
	}

	@Override
	public <T> int update(T entity, IncludeType includeType) {
		return updateBy(entity, _getCondition(includeType), "id");
	}

	@Override
	public <T> int insert(T entity, IncludeType includeType) {
		if (entity == null) return -1;

		if (ShardingUtil.isSharding()) {
			T array[] = (T[]) new Object[1];
			array[0] = entity;
			Logger.warn("In the Sharding type, change to call insert(T[] entity), it will ignore includeType parameter!");
			return insert(array);
		}

		long a = insertAndReturnId(entity, includeType);
		if (a > 0)
			return 1;
		else
			return 0;
	}

	@Override
	public <T> long insertAndReturnId(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		try {
			_ObjectToSQLHelper.setInitIdByAuto(entity); // 2.4.0 setInitIdByAuto > doBeforePasreEntity
			doBeforePasreEntity(entity, SuidType.INSERT);

			long returnId = getMongodbBeeSql().insertAndReturnId(entity, includeType);

			HoneyUtil.revertId(entity);

			return returnId;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> int delete(T entity, IncludeType includeType) {
		if (entity == null) return -1;
		return delete(entity, _getCondition(includeType));
	}

	@Override
	public <T> List<String[]> selectString(T entity) {
		Condition condition = null;
		return selectString(entity, condition);
	}

	@Override
	public <T> List<String[]> selectString(T entity, String... selectFields) {

		Condition condition = BeeFactoryHelper.getCondition();
		condition.selectField(selectFields);

		return selectString(entity, condition);
	}

	@Override
	public <T> List<String[]> selectString(T entity, Condition condition) {
		if (entity == null) return null;
		try {
			if (condition != null) condition = condition.clone();
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.SELECT);
			List<String[]> list = null;

			if (condition == null) condition = BeeFactoryHelper.getCondition(); // TODO

			if (condition.getSelectField() == null) {
				condition.selectField(HoneyUtil.getColumnNames(entity));
				// V2.1.8
				HoneyContext.setTrueInSysCommStrLocal(StringConst.MongoDB_SelectAllFields);
			}

			list = getMongodbBeeSql().selectString(entity, condition);

			HoneyContext.removeSysCommStrLocal(StringConst.MongoDB_SelectAllFields);

			return list;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> String selectJson(T entity) {
		Condition condition = null;
		return selectJson(entity, condition);
	}

	@Override
	public <T> String selectJson(T entity, IncludeType includeType) {
		return selectJson(entity, _getCondition(includeType));
	}

	@Override
	public <T> String selectJson(T entity, String... selectField) {
		Condition condition = BeeFactoryHelper.getCondition();
		return selectJson(entity, condition.selectField(selectField));
	}

	@Override
	public <T> String selectJson(T entity, int start, int size, String... selectFields) {
		if (entity == null) return null;
		if (size == 0) {
			Logger.warn(TIP_SIZE_0);
			return null;
		}
		if (size < 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		Condition condition = BeeFactoryHelper.getCondition();
		condition.selectField(selectFields).start(start).size(size);
		return selectJson(entity, condition);
	}

	private <T> T selectByIdObject(Class<T> entityClass, Object id) {
//		try {
//			T reqObj = entityClass.newInstance();
////			Field pkField=HoneyUtil.getPkField(entityClass);
////			HoneyUtil.setAccessibleTrue(pkField);
////			HoneyUtil.setFieldValue(pkField, reqObj, id); // 对相应Field设置
//
//			Condition condition = BF.getCondition();
//			if (entityClass.equals(String.class) && id.toString().contains(","))
//				condition.op(HoneyUtil.getPkName(entityClass), Op.in, id);
//			else
//				condition.op(HoneyUtil.getPkName(entityClass), Op.eq, id);
//
//			List<T> list = super.select(reqObj, condition);
//			if (ObjectUtils.isEmpty(list))
//				return null;
//			else
//				return list.get(0);
//
//		} catch (Exception e) {
//			Logger.warn(e.getMessage(), e);
//		}

		T t = null;
		List<T> list = null;
		try {
			regByIdForSharding(entityClass, id); // 2.5.2
			doBeforePasreEntity(entityClass, SuidType.SELECT);
			list = getMongodbBeeSql().selectById(entityClass, id);
			if (list == null || list.size() < 1) {
				t = null;
			} else {
				t = list.get(0);
			}
		} finally {
			if (list == null || list.size() < 1) {
				doBeforeReturn();
			} else {
				doBeforeReturn(list);
			}
		}
		return t;
	}

	@Override
	public <T> T selectById(Class<T> entityClass, Integer id) {
		if (entityClass == null) return null;

		if (id == null) {
			Logger.warn(ID_IS_NULL);
			return null;
		}

		return selectByIdObject(entityClass, id);
	}

	@Override
	public <T> T selectById(Class<T> entityClass, Long id) {
		if (entityClass == null) return null;

		if (id == null) {
			Logger.warn(ID_IS_NULL);
			return null;
		}

		return selectByIdObject(entityClass, id);
	}

	@Override
	public <T> T selectById(Class<T> entityClass, String id) {
		if (entityClass == null) return null;

		if (id == null) {
			Logger.warn(ID_IS_NULL);
			return null;
		}
		if (id.contains(",")) {
			throw new BeeIllegalParameterException(
					"The parameter 'id' of method selectById does not allow to contain comma!");
		}

		return selectByIdObject(entityClass, id);
	}

	@Override
	public <T> List<T> selectByIds(Class<T> entityClass, String ids) {
		if (entityClass == null) return null;

		if (ids == null) {
			Logger.warn("in method selectByIds,ids is null! ");
			return null;
		}
		List<T> list = null;
		try {
			regByIdForSharding(entityClass, ids); // 2.5.2
			doBeforePasreEntity(entityClass, SuidType.SELECT);
			list = getMongodbBeeSql().selectById(entityClass, ids);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@SuppressWarnings("rawtypes")
	public int deleteByIdObject(Class entityClass, Object id) {

//		try {
//			Object reqObj = entityClass.newInstance();
////			Field pkField=HoneyUtil.getPkField(entityClass);
////			HoneyUtil.setAccessibleTrue(pkField);
////			HoneyUtil.setFieldValue(pkField, reqObj, id); // 对相应Field设置
//
//			Condition condition = BF.getCondition();
//			if (entityClass.equals(String.class) && id.toString().contains(","))
//				condition.op(HoneyUtil.getPkName(entityClass), Op.in, id);
//			else
//				condition.op(HoneyUtil.getPkName(entityClass), Op.eq, id);
//
//			return super.delete(reqObj, condition);
//
//		} catch (Exception e) {
//			Logger.warn(e.getMessage(), e);
//		}

		try {
			regByIdForSharding(entityClass, id); // 2.5.2
			doBeforePasreEntity(entityClass, SuidType.DELETE);
			return getMongodbBeeSql().deleteById(entityClass, id);
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Integer id) {
		if (id == null) Logger.warn("in method deleteById, id is null! ");
		if (c == null || id == null) return 0;

		return deleteByIdObject(c, id);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, Long id) {
		if (id == null) Logger.warn("in method deleteById, id is null! ");
		if (c == null || id == null) return 0;

		return deleteByIdObject(c, id);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int deleteById(Class c, String ids) {
		if (ids == null) Logger.warn("in method deleteById, ids is null! ");
		if (c == null || ids == null) return 0;

		return deleteByIdObject(c, ids);
	}

	// 2.5.2
	private void regByIdForSharding(Class entityClass, Object idOrIds) {
//		OneTimeParameter.setAttribute(StringConst.ByIdWithClassForSharding, idOrIds);
		Condition condition = BF.getCondition();
		if (entityClass.equals(String.class) && idOrIds.toString().contains(","))
			condition.op(HoneyUtil.getPkName(entityClass), Op.in, idOrIds);
		else condition.op(HoneyUtil.getPkName(entityClass), Op.eq, idOrIds);
		regCondition(condition);
	}

	@Override
	public <T> String selectJson(T entity, Condition condition) {
		if (entity == null) return null;
		try {
			regCondition(condition);
			doBeforePasreEntity(entity, SuidType.SELECT);
			_regEntityClass1(entity);

			String json = getMongodbBeeSql().selectJson(entity, condition);

			return json;
		} finally {
			doBeforeReturn();
		}
	}

	@Override
	public <T> int updateBy(T entity, String... whereFields) {
		Condition condition = null;
		return updateBy(entity, condition, whereFields);
	}

	// v1.9
	@Override
	public <T> int updateById(T entity, Condition condition) {
		String pkName = "";
		try {
			HoneyUtil.getField(entity.getClass(), "id");
			pkName = "id";
		} catch (NoSuchFieldException e) {
			pkName = HoneyUtil.getPkFieldName(entity);
		}

		if (StringUtils.isBlank(pkName)) {
			throw new BeeErrorGrammarException("id(primary key) field can not empty!");
		}

		return updateBy(entity, condition, pkName);
	}

	@Override
	public <T> int updateBy(T entity, IncludeType includeType, String... whereFields) {
		return updateBy(entity, _getCondition(includeType), whereFields);
	}

	// v1.7.2
	@Override
	public <T> int updateBy(T entity, Condition condition, String... whereFields) {
		if (entity == null) return 0;
		try {
			doBeforePasreEntity(entity, SuidType.UPDATE);
			if (condition != null) condition = condition.clone();
			int updateNum = getMongodbBeeSql().updateBy(entity, condition, whereFields);

			return updateNum;
		} finally {
			doBeforeReturn();
		}
	}

	// v1.7.2
	@Override
	public <T> int update(T entity, Condition condition, String... updateFields) {
		if (entity == null) return 0;
		try {
			doBeforePasreEntity(entity, SuidType.UPDATE);

			if (condition != null) condition = condition.clone();
			int updateNum = getMongodbBeeSql().update(entity, condition, updateFields);

			return updateNum;

		} finally {
			doBeforeReturn();
		}
	}

	// v1.8
	@Override
	public <T> int update(T entity, Condition condition) {
		return update(entity, condition, "");
	}

	// 1.9
	@Override
	public <T> int insert(List<T> entityList) {
		if (entityList == null || entityList.size() < 1) return -1;
		T entity[] = toEntityArray(entityList);
		return insert(entity);
	}

	// 1.9
	@Override
	public <T> int insert(List<T> entityList, int batchSize) {
		if (entityList == null || entityList.size() < 1) return -1;
		T entity[] = toEntityArray(entityList);
		return insert(entity, batchSize);
	}

	// 1.9
	@Override
	public <T> int insert(List<T> entityList, String excludeFields) {
		if (entityList == null || entityList.size() < 1) return -1;
		T entity[] = toEntityArray(entityList);
		return insert(entity, excludeFields);
	}

	// 1.9
	@Override
	public <T> int insert(List<T> entityList, int batchSize, String excludeFields) {
		if (entityList == null || entityList.size() < 1) return -1;
		T entity[] = toEntityArray(entityList);

		return insert(entity, batchSize, excludeFields);
	}

	// 没能将entity传到SqlLib,需要注册
	private <T> void _regEntityClass1(T entity) {
		if (entity == null) return;
		HoneyContext.regEntityClass(entity.getClass());
	}

	private <T> void _regFunType(FunctionType functionType) {
		HoneyContext.regFunType(functionType);
	}

	@SuppressWarnings("unchecked")
	private <T> T[] toEntityArray(List<T> entityList) {

		int len = entityList.size();
		T entity[] = (T[]) new Object[len];

		for (int i = 0; i < len; i++) {
			entity[i] = entityList.get(i);
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
		if (!oldEntityFullName.equals(newEntityFullName)) {
			throw new BeeErrorGrammarException(
					"BeeErrorGrammarException: the oldEntity and newEntity must be same type!");
		}
		try {
			doBeforePasreEntity(newEntity, SuidType.UPDATE); // 拦截器只处理新实体； 旧实体oldEntity作为条件不在拦截器处理。

			int updateNum = getMongodbBeeSql().update(oldEntity, newEntity);

			return updateNum;
		} finally {
			doBeforeReturn();
		}

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
		if (entity == null) return 0;
		Object id = HoneyUtil.getIdValue(entity);
		if (id == null) return insert(entity);
		Object one = selectById(entity.getClass(), id.toString());
		if (one != null) return update(entity);
		else return insert(entity);
	}

	@Override
	public <T> boolean createTable(Class<T> entityClass, boolean isDropExistTable) {
		if (entityClass == null) return false;
		try {
			doBeforePasreEntity(entityClass, SuidType.DDL);
			boolean f = getMongodbBeeSql().createTable(entityClass, isDropExistTable);
			return f;
		} finally {
			doBeforeReturn();
		}
	}

	private static final String MSG = "Please use the relative method of CreateIndex or MongodbSuidRichExt in org.teasoft.beex.osql.mongodb!";

	@Override
	public <T> void indexNormal(Class<T> entityClass, String fields, String indexName) {
		Logger.warn(MSG);
	}

	@Override
	public <T> void unique(Class<T> entityClass, String fields, String indexName) {
		Logger.warn(MSG);
	}

	@Override
	public <T> void primaryKey(Class<T> entityClass, String fields, String keyName) {
		Logger.warn(MSG);
	}

	@Override
	public <T> void dropIndex(Class<T> entityClass, String indexName) {
		Logger.warn(MSG);
	}

//	private void doBeforePasreEntity(Object entity[], SuidType SuidType) {  //fixed bug. no set dataSource name
//		getInterceptorChain().beforePasreEntity(entity, SuidType);
//	}

}
