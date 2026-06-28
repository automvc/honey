/*
 * Copyright 2016-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.MoreObjToSQL;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.MoreTable;
import org.teasoft.bee.osql.api.SuidRich;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.ConditionImpl.FunExpress;
import org.teasoft.honey.osql.shortcut.BF;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * 多表Select/Update/Insert/Delete,MoreTable实现类.Multi table Select/Update/Insert/Delete, MoreTable implementation class.
 * <br>特别说明:
 * <br>1.1) 多表Select只生成一条sql; 
 * <br>1.2) 而多表Update/Insert/Delete是每个实体会生成一条sql;底层是使用SuidRich接口
 * <br>
 * <br>2)	1:n:1, Table1->List<Table2>->Table3; 
 * <br>2.1)	Select. 对于List<Table2>,只有listTable2.get(0)会被解析用于where的过滤条件.
 * <br>2.2)	Insert/Delete. List<Table2>整个list的记录都会操作；但对于Table3，只有list.get(0)对应的Table3会被操作；
 * @author Kingstar
 * @since  3.0.0
 */
public class MoreObjSQL extends AbstractCommOperate implements MoreTable {

	private BeeSql beeSql;
	private MoreObjToSQL moreObjToSQL;
	private static final int FAIL_RETURN_NUM = -1;

	private static final String SELECT_SQL = "select SQL: ";

	@Override
	public <T> List<T> select(T entity) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			_doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
			String sql = getMoreObjToSQL().toSelectSQL(entity);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(LogSqlParse.parseSql(SELECT_SQL, sql));
			list = getBeeSql().moreTableSelect(sql, entity);
		} catch (Exception e) {
			throw new BeeException(e);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if (size <= 0) throw new BeeIllegalParameterException(StringConst.SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(StringConst.START_GREAT_EQ_0);
		List<T> list = null;
		try {
			_doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
			String sql = getMoreObjToSQL().toSelectSQL(entity, start, size);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(LogSqlParse.parseSql(SELECT_SQL, sql));
			list = getBeeSql().moreTableSelect(sql, entity);
		} catch (Exception e) {
			throw new BeeException(e);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;
		List<T> list = null;
		try {
			if (condition != null) condition = condition.clone();
			regCondition(condition);
			_doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
			// 传递要判断是否有group
			OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding);
			String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(LogSqlParse.parseSql(SELECT_SQL, sql));
			list = getBeeSql().moreTableSelect(sql, entity);
		} catch (Exception e) {
			throw new BeeException(e);
		} finally {
			doBeforeReturn(list);
		}
		return list;
	}

	@Override
	public <T> String selectWithFun(T entity, Condition condition) {

		if (entity == null) return null;
		String fun = null;
		try {
			if (condition != null) condition = condition.clone();
			ConditionImpl conditionImpl = (ConditionImpl) condition;
			List<FunExpress> funExpList = null;
			if (conditionImpl != null) funExpList = conditionImpl.getFunExpList();
			if (ObjectUtils.isEmpty(funExpList)) {
				throw new BeeIllegalSQLException("In selectWithFun, the aggregation function can not be empty!");
			}
			if (funExpList.size() > 1) {
				throw new BeeIllegalSQLException("In selectWithFun, just support one aggregation function!");
			}

			regCondition(condition);
			_doBeforePasreEntity(entity);// 因要解析子表,子表下放再执行
			_regEntityClass1ForSqlLib(entity);
			_regFunType(getFunctionType(funExpList.get(0).getFunctionType()));
			String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
			sql = doAfterCompleteSql(sql);
			Logger.logSQL(LogSqlParse.parseSql(SELECT_SQL, sql)); // fixed, need logSQL before call BeeSql
			fun = getBeeSql().selectFun(sql); // 因是select max(id) from ...的形式,只会返回一个字段还可以多表的也共用
		} catch (Exception e) {
			throw new BeeException(e);
		} finally {
			doBeforeReturn();
		}
		return fun;
	}

	private static FunctionType getFunctionType(String functionName) {
		for (FunctionType type : FunctionType.values()) {
			if (type.getName().equalsIgnoreCase(functionName)) {
				return type;
			}
		}
		return null;
	}

	// 没能将entity传到SqlLib,需要注册
	private <T> void _regEntityClass1ForSqlLib(T entity) {
		if (entity == null) return;
		HoneyContext.regEntityClass(entity.getClass());
	}

	private <T> void _regFunType(FunctionType functionType) {
		HoneyContext.regFunType(functionType);
	}

	@Override
	public <T> int count(T entity) {
		return count(entity, null);
	}

	@Override
	public <T> int count(T entity, Condition condition) {
		Condition con;
		if (condition != null) condition = condition.clone();
		con = condition == null ? BF.getCondition() : condition;

		String total = selectWithFun(entity, con.selectFun(FunctionType.COUNT, "*"));
		return StringUtils.isBlank(total) ? 0 : Integer.parseInt(total.trim());
	}

	private <T> String processAndReturnSql(T entity, Condition condition) {
		if (condition != null) condition = condition.clone();
		regCondition(condition);
		_doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
		_regEntityClass1ForSqlLib(entity);
		OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding);
		String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
		sql = doAfterCompleteSql(sql);
		return sql;
	}

	@Override
	public <T> List<String[]> selectString(T entity, Condition condition) {
		if (entity == null) return null;
		List<String[]> list = null;
		try {
//			if (condition != null) condition = condition.clone();
//			regCondition(condition);
//			_doBeforePasreEntity(entity); // 因要解析子表,子表下放再执行
//			_regEntityClass1(entity);
//			OneTimeParameter.setTrueForKey(StringConst.Check_Group_ForSharding);
//			String sql = getMoreObjToSQL().toSelectSQL(entity, condition);
//			sql = doAfterCompleteSql(sql);

			String sql = processAndReturnSql(entity, condition);
			Logger.logSQL(LogSqlParse.parseSql("select SQL(return List<String[]>): ", sql));
			list = getBeeSql().select(sql); // 要测试分片时,是否合适? 有T entity参数,是可以的.
		} catch (Exception e) {
			throw new BeeException(e);
		} finally {
			doBeforeReturn();
		}
		return list;
	}

	@Override
	public MoreObjSQL setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}

	private void _doBeforePasreEntity(Object entity) {
		ShardingReg.setTrue(StringConst.MoreTableSelectShardingFlag);
		super.doBeforePasreEntity(entity, SuidType.SELECT);
		OneTimeParameter.setAttribute(StringConst.InterceptorChainForMoreTable, getInterceptorChain());// 用于子表
	}

	public BeeSql getBeeSql() {
		if (this.beeSql == null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	public MoreObjToSQL getMoreObjToSQL() {
		if (moreObjToSQL == null) return BeeFactory.getHoneyFactory().getMoreObjToSQL();
		return moreObjToSQL;
	}

	public void setMoreObjToSQL(MoreObjToSQL moreObjToSQL) {
		this.moreObjToSQL = moreObjToSQL;
	}

	// -------------------more---insert/delete------V3.0.0------------------
	// 1:n:1, Table1->List<Table2>->Table3
	// List<Table2>整个list的记录都会操作；但对于Table3，只有list.get(0)对应的Table3会被操作；
	// OneToMany: Table1-> List<Table2>
	// List<Table2>整个list的记录都会操作。

	// ** 先拆解实体,再调用SuidRich; 而不是像多表查询,要解析成sql,再调用SqlLib.

	private SuidRich suidRich;

	public SuidRich getSuidRich() {
		if (this.suidRich == null) suidRich = BeeFactory.getHoneyFactory().getSuidRich();
		return suidRich;
	}

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}

	@Override
	public <T> int insert(T entity) {
		// 是否需要事务？不需要，事务由上一层负责.
		return modify3(entity, SuidType.INSERT);
	}

	@Override
	public <T> int update(T entity) {
		// 是否需要事务？不需要，事务由上一层负责.
		return modify3(entity, SuidType.UPDATE);
	}

	@Override
	public <T> int delete(T entity) {
		// 是否需要事务？不需要，事务由上一层负责.
		return modify3(entity, SuidType.DELETE);
	}

	// 多表的modify是调用suidRich完成的.拦截器,则使用suidRich的.
	private <T> int modify3(T entity, SuidType suidType) {
		if (entity == null) return FAIL_RETURN_NUM;

		if (SuidType.INSERT != suidType && SuidType.DELETE != suidType && SuidType.UPDATE != suidType) {
			throw new BeeException("Do not support the type: " + suidType + " in method insertOrDelete");
		}

		String type = "";
		if (SuidType.INSERT == suidType) {
			type = "insert";
		} else if (SuidType.DELETE == suidType) {
			type = "delete";
		} else if (SuidType.UPDATE == suidType) {
			type = "update";
		}

		Map<String, MoreTableStruct3> moreTableStructMap = ParseSqlHelper.parseJoins(entity, SuidType.MODIFY);
		if (moreTableStructMap == null || moreTableStructMap.isEmpty())
			throw new BeeException("Entity for MoreTable operate must have JoinTable setting!");

		// TODO 检查 但有可能主键是在SuidRich时还自动添加的。
		int mainAffectNum = modifyOneEntity3(entity, suidType, null);

		// 主表没有插入/删除成功，则不处理子表。
		// if insert/delete not successful or update main affect num<0, do not process sub entity.
		if ((SuidType.UPDATE == suidType && mainAffectNum < 0) || (SuidType.UPDATE != suidType && mainAffectNum <= 0)) {
			Logger.warn(type + " main entity " + entity.getClass().getName() + ", affected rows is " + mainAffectNum
					+ ", will ignore " + type + " sub entity!");
			return mainAffectNum;
		}

		MoreTableStruct3 moreTableStruct;
		Object lastLayerObject = null;
		for (Entry<String, MoreTableStruct3> entry : moreTableStructMap.entrySet()) {
			moreTableStruct = entry.getValue();

			Object subObject = moreTableStruct.subObject;
			// when the sub entity value is null, do not process.
			if (subObject == null) continue;

			String[] mainFields = moreTableStruct.mainFields;
			String[] subFields = moreTableStruct.subFields;
			Field mainField1Cache = null; // 有时，虽然只有使用一个主键，但不同的子实体对应的是主实体不同的字段。
			try {
				int layer = moreTableStruct.layer;
				if (layer == 2) {
					lastLayerObject = entity;
					if (mainFields.length == 1) { // cache main entity only one key field.
						mainField1Cache = lastLayerObject.getClass().getDeclaredField(mainFields[0]);
					}
				}

				if (!moreTableStruct.currentIsList) {// single object
					// set foreign key in sub entity with main table key
					boolean isSuperFieldNullValue = fillSubFieldWithSuperField(mainFields, subFields, layer,
							lastLayerObject, subObject, mainField1Cache);
					if (isSuperFieldNullValue) continue;

					int subAffect = modifyOneEntity3(subObject, suidType, subFields);
					Logger.logSQL(
							type + " sub entity:" + subObject.getClass().getName() + ", Affected rows: " + subAffect);
				} else { // sub is list, need use subListObject (subObject just have list[0])
					List<?> subListObject = moreTableStruct.subListObject;
					boolean isSuperFieldNullValue = false;
					int nullValueArray[] = new int[subListObject.size()];

					for (int i = 0; i < subListObject.size(); i++) {
						boolean isSuperFieldNullValueI = fillSubFieldWithSuperField(mainFields, subFields, layer,
								lastLayerObject, subListObject.get(i), mainField1Cache);
						if (isSuperFieldNullValueI) nullValueArray[i] = 1;
						isSuperFieldNullValue = isSuperFieldNullValue || isSuperFieldNullValueI;
					}

					int subAffect = 0;
					if (SuidType.INSERT == suidType) {
						if (!isSuperFieldNullValue) { // 上层主键值没有为空的，就一次将整个list插入
							subAffect = getSuidRich().insert(subListObject); // insert list
						} else {
							for (int k = 0; k < subListObject.size(); k++)
								if (nullValueArray[k] == 1) {
									// 上层主键值是空,即子表没有设置外键值时就不执行(也不会往下层执行)
									continue;
								} else {
									subAffect = getSuidRich().insert(subListObject.get(k));
									Logger.logSQL(type + " sub entity:" + subObject.getClass().getName()
											+ ", Affected rows: " + subAffect);
								}
						}
					} else if (SuidType.DELETE == suidType) {
						for (int k = 0; k < subListObject.size(); k++) {
							if (nullValueArray[k] == 1) {
								// 上层主键值是空,即子表没有设置外键值时就不执行(也不会往下层执行)
								continue;
							} else {
								subAffect = getSuidRich().delete(subListObject.get(k));
								Logger.logSQL(type + " sub entity:" + subObject.getClass().getName() + ", Affected rows: "
										+ subAffect);
							}
						}
					} else if (SuidType.UPDATE == suidType) {
						for (int k = 0; k < subListObject.size(); k++) {
							if (nullValueArray[k] == 1) {
								// 上层主键值是空,即子表没有设置外键值时就不执行(也不会往下层执行)
								continue;
							} else {
//								Object idValeu = HoneyUtil.getIdValue(subListObject.get(k)); //不支持联合主键 TODO
//								if (idValeu != null) subAffect=getSuidRich().update(subListObject.get(k)); // 有主键，使用主键作为过滤条件。
//								else subAffect = getSuidRich().updateBy(subListObject.get(k), subFields);

								subAffect = modifyOneEntity3(subListObject.get(k), suidType, subFields);

								Logger.logSQL(type + " sub entity:" + subObject.getClass().getName() + ", Affected rows: "
										+ subAffect);
							}
						}
					}
				}

			} catch (NoSuchFieldException | IllegalAccessException e) {
				Logger.warn("Have exception when " + type + " sub entity!");
			}
			lastLayerObject = subObject;
		}
		return mainAffectNum;
	}

	private <T> int modifyOneEntity3(T entity, SuidType suidType, String[] subFields) {// 子表是update时,不走这个分支
		int affectNum = 0;

		if (SuidType.INSERT == suidType) {
			affectNum = getSuidRich().insert(entity);
			// 为了保证支持更全面, 不使用insertAndReturnId,可以使用ID自动生成解决id值的问题. 后期可考虑使用配置，让某些特定的DB可以使用。
//			long mainTableReturnId=getSuidRich().insertAndReturnId(entity);  //要考虑不适用的情况。
		} else if (SuidType.DELETE == suidType) {
			affectNum = getSuidRich().delete(entity);
		} else if (SuidType.UPDATE == suidType) {
//			affectNum = getSuidRich().update(entity);  //TODO 子实体的主键未必有值；外键有值而矣
			// 用以下的办法，先判断主键是否有值即可。
			if (subFields == null) { // for main entity
				affectNum = getSuidRich().update(entity); // 有主键，使用主键作为过滤条件。
			} else {// for sub entity
				Object idValeu = HoneyUtil.getIdValue(entity); // 不支持联合主键 TODO
				if (idValeu != null)
					affectNum = getSuidRich().update(entity); // 有主键，使用主键作为过滤条件。
				else
					affectNum = getSuidRich().updateBy(entity, subFields);
			}
		}
		return affectNum;
	}

//	private boolean checkMainFields(String[] mainFields, String[] subFields, int layer,
//			Object lastLayerObject, Object subObject, Field mainField1Cache) throws NoSuchFieldException, IllegalAccessException {
//		// need set
//		return checkAndFillSubFieldWithSuperField(mainFields, subFields, layer, lastLayerObject, subObject, mainField1Cache, false);
//	}

	// return the flag is SuperField Null Value or not
	private boolean fillSubFieldWithSuperField(String[] mainFields, String[] subFields, int layer, Object lastLayerObject,
			Object subObject, Field mainField1Cache) throws NoSuchFieldException, IllegalAccessException {
		// need set
		return checkAndFillSubFieldWithSuperField(mainFields, subFields, layer, lastLayerObject, subObject,
				mainField1Cache, true);
	}

	private boolean checkAndFillSubFieldWithSuperField(String[] mainFields, String[] subFields, int layer,
			Object lastLayerObject, Object subObject, Field mainField1Cache, boolean isNeedSet)
			throws NoSuchFieldException, IllegalAccessException {

		Field mainField = null; // in last layer
		Field subField = null;
		for (int i = 0; i < mainFields.length; i++) {
			if (layer == 2 && mainField1Cache != null) {
				mainField = mainField1Cache;
			} else {
				mainField = lastLayerObject.getClass().getDeclaredField(mainFields[i]);
			}
			HoneyUtil.setAccessibleTrue(mainField);
			Object mainFieldValue = HoneyUtil.getValue(mainField, lastLayerObject);
			Object subFieldValeu = null;
			if (subObject != null) {
				subField = subObject.getClass().getDeclaredField(subFields[i]);
				HoneyUtil.setAccessibleTrue(subField);
				subFieldValeu = HoneyUtil.getValue(subField, subObject);
			}

			if (mainFieldValue == null) {
				return true;
			}
			if (isNeedSet) {
				if (mainFieldValue != null && !mainFieldValue.equals(subFieldValeu)) {
					if (subFieldValeu == null) {
						Logger.info(subFields[i] + " is null, will set with the value of " + mainFields[i]);
					} else {
						Logger.info("The value of " + subFields[i] + " value is not equal " + mainFields[i]
								+ ". Will override " + subFields[i] + " with " + mainFields[i]);
					}
					HoneyUtil.setFieldValue(subField, subObject, mainFieldValue);
				}
			}
		}
		return false;
	}
}
