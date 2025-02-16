package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.GenId;
import org.teasoft.bee.osql.annotation.GenUUID;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalBusinessException;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.distribution.UUID;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * ObjectToSQL帮助类.ObjectToSQL Helper.
 * @author Kingstar
 * @since  1.0
 */
final class _ObjectToSQLHelper {

	private static final String INSERT_INTO = K.insert + K.space + K.into + K.space;
	private static final String REPLACE_INTO = K.replace + K.space + K.into + K.space;

	private static boolean showSQL = HoneyConfig.getHoneyConfig().showSQL;

	private _ObjectToSQLHelper() {}

	static <T> String _toSelectSQL(T entity, String fieldNameList) {
		checkPackage(entity);

		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		try {
			String tableName = _toTableName(entity);
			Field fields[] = HoneyUtil.getFields(entity.getClass());

			sqlBuffer.append(K.select).append(" ").append(fieldNameList).append(" ").append(K.from).append(" ");
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);
				if (HoneyUtil.isContinue(-1, fields[i].get(entity), fields[i])) {
					continue;
				} else {
					if (firstWhere) {
						sqlBuffer.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" ").append(K.and).append(" ");
					}

					sqlBuffer.append(_toColumnName(fields[i].getName(), entity.getClass()));
					sqlBuffer.append("=");
					sqlBuffer.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
					list.add(preparedValue);
				}
			}

			sql = sqlBuffer.toString();

			setContext(sql, list, tableName);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;

	}

	static <T> String _toSelectSQL(T entity, int includeType, Condition condition) {
		return _toSelectSQL(entity, includeType, condition, false);
	}

	static <T> String _toSelectSQL(T entity, int includeType, Condition condition, boolean isCheckOneFunction) {
		checkPackage(entity);

//		Set<String> conditionFieldSet=null;
//		if(condition!=null) conditionFieldSet=condition.getWhereFields();

		StringBuffer sqlBuffer = new StringBuffer();
		String tableName = _toTableName(entity);
		List<PreparedValue> list = new ArrayList<>();
		boolean firstWhere = true;
//		boolean isFun=false;
		try {
			Field fields[] = HoneyUtil.getFields(entity.getClass());
			String columnNames;

			String packageAndClassName = entity.getClass().getName();
			columnNames = HoneyContext.getBeanField(packageAndClassName);
			if (columnNames == null) {
				columnNames = HoneyUtil.getBeanField(fields, entity.getClass());
				HoneyContext.addBeanField(packageAndClassName, columnNames);
			}
			if (condition != null) {
//				condition=condition.clone();
				condition.setSuidType(SuidType.SELECT);

				// V2.0
				boolean checkGroup = OneTimeParameter.isTrue(StringConst.Check_Group_ForSharding)
						&& ShardingUtil.hadSharding();
				List<String> groupNameslist = condition.getGroupByFields();
				int size = groupNameslist == null ? -1 : groupNameslist.size();
				if (checkGroup) checkGroup = checkGroup && size >= 1;
				OneTimeParameter.setTrueForKey(StringConst.Get_GroupFunStruct);

				// v1.9
				String fun = ConditionHelper.processFunction(columnNames, condition);
				if (isCheckOneFunction) {
					if (fun.contains(",")) {
						throw new BeeErrorGrammarException("The method just support use one Function!");
					}
					if ("".equals(fun)) {
						throw new BeeErrorGrammarException("The method need set the Function with Condition selectFun!");
					}
				}

				String selectField = ConditionHelper.processSelectField(columnNames, condition);

				if (isCheckOneFunction) {
					columnNames = fun;
				} else if (selectField != null && StringUtils.isEmpty(fun)) {
					columnNames = selectField;
				} else if (selectField != null && StringUtils.isNotEmpty(fun)) {
					columnNames = selectField + "," + fun;
				} else if (selectField == null && StringUtils.isNotEmpty(fun)) {
					columnNames = fun;
				}
//				else {
//					//full column names
//				}

				if (checkGroup) {
					// 判断,是否是分片,且有group分组,聚合
					// pre自定义和返回string[]的查询,也不需要. 可以传入标记, select,selectJson的才可以.

//				 改写sql: 	avg改写; select没有分组字段的,要补上;    是否放这?    

//					String groupFields[], GroupFunStruct gfsArray[]
//					private String fieldName;
//					private String functionType;

					Map<String, String> orderByMap = condition.getOrderBy();
					boolean isEmptyOrderByMap = orderByMap.size() == 0;
					boolean needGroupWhenNoFun = false; // must NoFun

					// hadSharding, 分组的字段没有查询出来,则加分组的字段
					for (String g : groupNameslist) {
						if (columnNames.contains("," + g) || columnNames.contains(g + ",") || columnNames.equals(g)) {
							// already contain group field
						} else {
							columnNames += "," + g;
						}
						if (isEmptyOrderByMap)
							condition.orderBy(g);
						else {
							if (!needGroupWhenNoFun && !orderByMap.containsKey(g)) needGroupWhenNoFun = true;
						}

						// 一个实体,原来有一个Map属性,取出后,能清空里面的元素吗???
					}
					GroupFunStruct gfStruct = null;
					if (StringUtils.isNotEmpty(fun)) { // && size>=1
						gfStruct = (GroupFunStruct) OneTimeParameter.getAttribute(StringConst.Return_GroupFunStruct);
					}

					// 排序没有分组字段,添加进去?
					if (gfStruct != null) {
						gfStruct.setGroupFields(groupNameslist);
						gfStruct.setNeedGroupWhenNoFun(needGroupWhenNoFun); // 有一个排序字段, 但不是分组字段的, 多个分片的记录汇聚合到一起后,可能不合顺序.
						gfStruct.setColumnNames(columnNames);

						HoneyContext.setCurrentGroupFunStruct(gfStruct);
					}
				} // checkGroup

			}

			sqlBuffer.append(K.select).append(" ").append(columnNames).append(" ").append(K.from).append(" ");
			sqlBuffer.append(tableName);

			int len = fields.length;
			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);
				if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
					continue;
				} else {

					if (isNullPkOrId(fields[i], entity)) continue; // 主键=null不作为过滤条件

//					if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName()))  //closed in V1.9
//						continue; //Condition已包含的,不再遍历

					if (firstWhere) {
						sqlBuffer.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" ").append(K.and).append(" ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName(), entity.getClass()));

					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" ").append(K.isNull);
					} else {
						sqlBuffer.append("=");
						sqlBuffer.append("?");

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
						list.add(preparedValue);
					}
				}
			} // end for

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		if (condition != null) {
			condition.setSuidType(SuidType.SELECT);
			// 2.0 sharding不支持同时多种DB,不会走这
			if (HoneyContext.isNeedRealTimeDb()) {
				HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName);
				OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
			}
			OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
			ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
		}

		setContext(sqlBuffer.toString(), list, tableName); // 若使用表名占位用于sharding, 则拆分sql运行后,要重置缓存.
															// todo 这样,就不用在这修改生成sql的代码

		return sqlBuffer.toString();
	}

	static <T> String _toSelectSQL(T entity, int includeType) {
		return _toSelectSQL(entity, includeType, null);
	}

	static <T> String _toUpdateSQL(T entity, int includeType) { // whereColumn is id
		checkPackage(entity);
		Field field = null;

		// V1.11 support custom primary key
		String pkName = ""; // primary key
		String alias = "";
		try {
			field = HoneyUtil.getField(entity.getClass(), "id");
			pkName = "id";
		} catch (NoSuchFieldException e) {
			pkName = HoneyUtil.getPkFieldName(entity);
			if (!"".equals(pkName)) {
				alias = "(" + pkName + ")";
			} else {
				// if have exception, express "id".equalsIgnoreCase(whereColumn) is false.
				throw new ObjSQLException(
						"ObjSQLException: in the update(T entity) or update(T entity,IncludeType includeType), the id field is missing !");
			}
		}

		if (field == null && !pkName.contains(",")) {// 名称不为id的单主键,需要重新获取field,以检测其值是否为null
			try {
				field = HoneyUtil.getField(entity.getClass(), pkName);
			} catch (NoSuchFieldException e) {
				// ignore
			}
		}

		try {
			if (field != null) {
				HoneyUtil.setAccessibleTrue(field);
				// 是联合主键时不检测值是否为null
				if (field.get(entity) == null) {
					throw new ObjSQLException(
							"ObjSQLException: in the update(T entity) or update(T entity,IncludeType includeType), "
									+ "the id field" + alias + " of entity must not be null !");
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		// UpdateBy id
//		return _toUpdateBySQL(entity, new String[] { "id" }, includeType); // update by whereColumn(now is id)
//		return _toUpdateBySQL(entity, new String[] { pkName }, includeType); // update by whereColumn(now is primary key)

		String pks[] = pkName.split(",");
		return _toUpdateBySQL(entity, pks, includeType);
	}

	static <T> String _toUpdateSQL(T entity, String setColmn[], int includeType) {
		return _toUpdateSQL(entity, setColmn, includeType, null);
	}

	// v1.7.2 add para Condition condition
	static <T> String _toUpdateSQL(T entity, String setColmns[], int includeType, Condition condition) {
		checkPackage(entity);

//		Set<String> conditionFieldSet = null;
//		if (condition != null) conditionFieldSet = condition.getWhereFields();

		Set<String> updatefieldSet = null;
		if (condition != null) {
//			condition = condition.clone();
			updatefieldSet = condition.getUpdatefields();
		}

//		if (updateFields.length == 0 || "".equals(updateFieldList.trim()))

		if ((setColmns == null || (setColmns.length == 1 && "".equals(setColmns[0].trim())))
//		    && (updatefieldSet==null || updatefieldSet.size()==0) ){
				&& (ObjectUtils.isEmpty(updatefieldSet))) {
			throw new ObjSQLException("ObjSQLException: in SQL update set at least include one field.");
		}

		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		boolean firstSet = true;
		boolean firstWhere = true;
		boolean isExistWhere = false;
		StringBuffer whereStament = new StringBuffer();
		List<PreparedValue> list = new ArrayList<>();
		String tableName = _toTableName(entity);

		try {

			sqlBuffer.append(K.update).append(" ");
			sqlBuffer.append(tableName);
			sqlBuffer.append(" ").append(K.set).append(" ");

			// v1.7.2 处理通过condition设置的部分
			if (condition != null) {
				condition.setSuidType(SuidType.UPDATE); // UPDATE
				OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
				firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, list, condition);
			}

			Field fields[] = HoneyUtil.getFields(entity.getClass());
			int len = fields.length;

			List<PreparedValue> whereList = new ArrayList<>();

			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);

////				if (isContainField(setColmns, fields[i].getName())) { //set value.setColmn不受includeType影响,都会转换
//				if (isContainField(setColmns, fields[i].getName())     
////						&& ( (updatefieldSet ==null) || (updatefieldSet != null && !updatefieldSet.contains(fields[i].getName())) ) // 在updatefieldSet为新值，entity 的为旧值可放在where条件    v1.8
//						) {	//在指定的setColmns,且还没有用在set,setAdd,setMultiply的字段,才转成update set的部分.

//					在updatefieldSet为新值，entity 的为旧值可放在where条件    v1.8      V1.9但指定了是setColmns,则只会转为set部分
//					if (updatefieldSet != null && updatefieldSet.contains(fields[i].getName())) { 
//						Logger.warn("The field ["+fields[i].getName()+"] which value is '"+fields[i].get(entity)+"', already set in condition! It will be ignored!");
//						continue; //Condition已包含的set条件,不再作转换处理
//					}

				if (isContainField(setColmns, fields[i].getName())
						// v1.9.8 实体中已通过condition.set(arg1,arg2)等设置的字段,不再转化到set 部分,但可以转到where部分
						&& ((updatefieldSet == null) || (!updatefieldSet.contains(fields[i].getName())))) { // 在指定的setColmns,且还没有用在Condition的set,setAdd,setMultiply的字段(另外处理),才在此处转成update
																											// set的部分.
					if (firstSet) {
						firstSet = false;
					} else {
						sqlBuffer.append(", ");// update 的set部分不是用and ，而是用逗号的
					}

					sqlBuffer.append(_toColumnName(fields[i].getName(), entity.getClass()));

					if (fields[i].get(entity) == null) {
						sqlBuffer.append("=").append(K.Null); // =
					} else {

						sqlBuffer.append("=");
						sqlBuffer.append("?");

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
						list.add(preparedValue);
					}
				} else {// where

					if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
						continue;
					} else {
//						if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
//							continue; //id=null不作为过滤条件
						if (isNullPkOrId(fields[i], entity)) continue; // 主键=null不作为过滤条件

						// v1.7.2
//						if (conditionFieldSet != null && conditionFieldSet.contains(fields[i].getName()))  //closed in V1.9
//							continue; //Condition已包含的,不再遍历

						if (firstWhere) {
							whereStament.append(" ").append(K.where).append(" ");
							firstWhere = false;
						} else {
							whereStament.append(" ").append(K.and).append(" ");
						}
						whereStament.append(_toColumnName(fields[i].getName(), entity.getClass()));

						if (fields[i].get(entity) == null) {
							whereStament.append(" ").append(K.isNull);
						} else {

							whereStament.append("=");
							whereStament.append("?");

							preparedValue = new PreparedValue();
							preparedValue.setType(fields[i].getType().getName());
							preparedValue.setValue(fields[i].get(entity));
							if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
							whereList.add(preparedValue);
						}
						isExistWhere = true;
					}
				} // end else
			} // end for

			sqlBuffer.append(whereStament);

			list.addAll(whereList);

			if (firstSet) {
				Logger.logSQL("update SQL: ", sqlBuffer.toString());
				throw new BeeErrorGrammarException("BeeErrorGrammarException: the SQL update set part is empty!");
			}

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		if (condition != null) {
			condition.setSuidType(SuidType.UPDATE); // UPDATE
			// 即使condition包含的字段是setColmns里的字段也会转化到sql语句.
//			firstWhere = ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere);
			OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
			firstWhere = ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
		}

		sql = sqlBuffer.toString();

		setContext(sql, list, tableName);

		// 不允许更新一个表的所有数据
		// v1.7.2 只支持是否带where检测
		if (firstWhere) {
			boolean notUpdateWholeRecords = HoneyConfig.getHoneyConfig().notUpdateWholeRecords;
			if (notUpdateWholeRecords) {
				Logger.logSQL("update SQL: ", sql);
				throw new BeeIllegalBusinessException(
						"BeeIllegalBusinessException: It is not allowed update whole records in one table.");
				// return "";
			}
		}

		return sql;
	}

	// for updateBy
	static <T> String _toUpdateBySQL(T entity, String whereColumns[], int includeType) {
		return _toUpdateBySQL(entity, whereColumns, includeType, null);
	}

	// for updateBy
	// v1.7.2 add para Condition condition
	static <T> String _toUpdateBySQL(T entity, String whereColumns[], int includeType, Condition condition) {
		checkPackage(entity);

		Set<String> conditionFieldSet = null;
		if (condition != null) {
//			condition = condition.clone();
			conditionFieldSet = condition.getWhereFields();
		}

		Set<String> updatefieldSet = null;
		if (condition != null) updatefieldSet = condition.getUpdatefields();

		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		boolean firstSet = true;
		boolean firstWhere = true;
		boolean isExistWhere = false;
		StringBuffer whereStament = new StringBuffer();
		String tableName = _toTableName(entity);
		List<PreparedValue> list = new ArrayList<>();

		try {

			sqlBuffer.append(K.update).append(" ");
			sqlBuffer.append(tableName);
			sqlBuffer.append(" ").append(K.set).append(" ");

//		setMultiply,setAdd,是在处理字段前已完成处理的,所以不受指定的where条件的字段(即String whereColumns[])的影响.
			// v1.7.2
			if (condition != null) {
				condition.setSuidType(SuidType.UPDATE); // UPDATE
				OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
				firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, list, condition);
			}

			Field fields[] = HoneyUtil.getFields(entity.getClass());
			int len = fields.length;

			List<PreparedValue> whereList = new ArrayList<>();

			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);
				if (!isContainField(whereColumns, fields[i].getName())) { // set value. 不属于whereColumn的,将考虑转为set.
																			// 同一个实体的某个属性的值,若用于WHERE部分了,再用于UPDATE SET部分就没有意义

					// set 字段根据includeType过滤
					if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
						continue;
					}
//				if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName()))
//					continue; //id=null跳过,id不更改.
					if (isNullPkOrId(fields[i], entity)) continue; // 主键=null不作为过滤条件

					// v1.7.2
					if (updatefieldSet != null && updatefieldSet.contains(fields[i].getName())) {
						Logger.warn("The field [" + fields[i].getName() + "] which value is '" + fields[i].get(entity)
								+ "', already set in condition! It will be ignored!");
						continue; // Condition已包含的set条件,不再作转换处理
					}

					if (firstSet) {
//					sqlBuffer.append(" ");
						firstSet = false;
					} else {
						sqlBuffer.append(" , ");// update 的set部分不是用and ，而是用逗号的
					}
					sqlBuffer.append(_toColumnName(fields[i].getName(), entity.getClass()));

					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" =").append(K.Null); // =
					} else {

						sqlBuffer.append("=");
						sqlBuffer.append("?");

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
						list.add(preparedValue);
					}
				} else {// where . 此部分只会有显式指定的whereColumn的字段

//				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
//					continue;
//				} else {

//				v1.7.2
					if (conditionFieldSet != null && conditionFieldSet.contains(fields[i].getName())) {
//					continue; //Condition已包含的,不再遍历 //closed in V1.9
						// if the field use in set part, will filter the default value.
						if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
							continue; // 指定作为条件的,都转换. 但condition已有设置为where条件的, entity的字段要遵循默认过虑
						}
					}

					// 指定作为条件的,都转换
//					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName()))
//						continue; //id=null不作为过滤条件
					if (isNullPkOrId(fields[i], entity)) continue; // 主键=null不作为过滤条件

					if (firstWhere) {
						whereStament.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
						whereStament.append(" ").append(K.and).append(" ");
					}
					whereStament.append(_toColumnName(fields[i].getName(), entity.getClass()));

					if (fields[i].get(entity) == null) {
						whereStament.append(" ").append(K.isNull);
					} else {

						whereStament.append("=");
						whereStament.append("?");

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
						whereList.add(preparedValue);
					}
					isExistWhere = true;
//				}
				} // end else
			} // end for
			sqlBuffer.append(whereStament);

			list.addAll(whereList);

			if (firstSet) {
				Logger.logSQL("update SQL(updateBy): ", sqlBuffer.toString());
				throw new BeeErrorGrammarException("BeeErrorGrammarException: the SQL update set part is empty!");
			}

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		if (condition != null) {
			condition.setSuidType(SuidType.UPDATE); // UPDATE
			OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
			// 即使condition包含的字段是whereColumn里的字段也会转化到sql语句.
			firstWhere = ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
		}

		sql = sqlBuffer.toString();

		setContext(sql, list, tableName);

		// 不允许更新一个表的所有数据
		// v1.7.2 只支持是否带where检测
		if (firstWhere) {
			boolean notUpdateWholeRecords = HoneyConfig.getHoneyConfig().notUpdateWholeRecords;
			if (notUpdateWholeRecords) {
				Logger.logSQL("update SQL(updateBy): ", sql);
				throw new BeeIllegalBusinessException(
						"BeeIllegalBusinessException: It is not allowed update whole records in one table.");
				// return "";
			}
		}

		return sql;
	}

//	static <T> SqlValueWrap _toInsertSQL0(T entity, int includeType, String excludeFieldList) throws IllegalAccessException {
	static <T> String _toInsertSQL0(T entity, int includeType, String excludeFieldList) throws IllegalAccessException {
		checkPackage(entity);

		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
//		StringBuffer sqlValue = new StringBuffer(") values (");
		StringBuffer sqlValue = new StringBuffer(" (");
		boolean isFirst = true;
		String tableName = _toTableName(entity);
		if (HoneyUtil.isMysql() && AnnoUtil.isReplaceInto(entity)) // V1.11
			sqlBuffer.append(REPLACE_INTO);
		else
			sqlBuffer.append(INSERT_INTO);
		sqlBuffer.append(tableName);
		sqlBuffer.append("(");

		Field fields[] = HoneyUtil.getFields(entity.getClass());
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0; i < len; i++) {
			HoneyUtil.setAccessibleTrue(fields[i]);
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
				continue;
			} else {

				if (!"".equals(excludeFieldList) && isExcludeField(excludeFieldList, fields[i].getName())) continue;

				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuffer.append(",");
					sqlValue.append(",");
				}
				sqlBuffer.append(_toColumnName(fields[i].getName(), entity.getClass()));
				sqlValue.append("?");

				preparedValue = new PreparedValue();
				preparedValue.setType(fields[i].getType().getName());
				preparedValue.setValue(fields[i].get(entity));
				if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
				list.add(preparedValue);
			}
		}
		sqlValue.append(")");

		sqlBuffer.append(") ").append(K.values);
		sqlBuffer.append(sqlValue);
		sql = sqlBuffer.toString();

//		"insert into orders() values ()"  //empty value
		if (isFirst) {
			OneTimeParameter.setTrueForKey("_SYS_Bee_NullObjectInsert");
		}

		if (OneTimeParameter.isTrue("_SYS_Bee_Return_PlaceholderValue")) {
			OneTimeParameter.setAttribute("_SYS_Bee_PlaceholderValue", sqlValue.toString());
		}

		setContext(sql, list, tableName);

		return sql;
	}

	// 只需要解析值 for array[]
	static <T> List<PreparedValue> _toInsertSQL_for_ValueList(String sql_i, T entity, String excludeFieldList)
			throws IllegalAccessException {
		checkPackage(entity);

		Field fields[] = HoneyUtil.getFields(entity.getClass());
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0; i < len; i++) {
			HoneyUtil.setAccessibleTrue(fields[i]);

//			if ("serialVersionUID".equals(fields[i].getName()) || fields[i].isSynthetic()) {
//				continue;
//			} else if (fields[i] != null && fields[i].isAnnotationPresent(JoinTable.class)) {
			if (HoneyUtil.isSkipField(fields[i])) continue;
			else if (HoneyUtil.isSkipFieldJustFetch(fields[i])) continue;
			else if (!"".equals(excludeFieldList) && isExcludeField(excludeFieldList, fields[i].getName())) continue;

			preparedValue = new PreparedValue();
			preparedValue.setType(fields[i].getType().getName());
			preparedValue.setValue(fields[i].get(entity));
			if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
			list.add(preparedValue);
		}

//		if (showSQL) { //just insert array to this method
		if (HoneyUtil.isMysql() && !showSQL) { // if it is mysql batch insert, just use for print log.
			// no need set context
			// mysql 批操作时,仅用于打印日志. 所以当DB为mysql,且不用打印日志时,不用记录
			// mysql在其它分支处理
		} else {
			HoneyContext.setPreparedValue(sql_i, list);
		}
		return list;
	}

	static <T> String _toDeleteSQL(T entity, int includeType) {
		return _toDeleteSQL(entity, includeType, null);
	}

	static <T> String _toDeleteSQL(T entity, int includeType, Condition condition) {
		checkPackage(entity);

//		Set<String> conditionFieldSet=null;
//		if(condition!=null) conditionFieldSet=condition.getWhereFields();

		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		boolean firstWhere = true;
		try {
			String tableName = _toTableName(entity);

			sqlBuffer.append(K.delete).append(" ").append(K.from).append(" ");
			sqlBuffer.append(tableName);

			Field fields[] = HoneyUtil.getFields(entity.getClass());
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);

				if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
					continue;
				} else {

//					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
//						continue; //id=null不作为过滤条件
					if (isNullPkOrId(fields[i], entity)) continue; // 主键=null不作为过滤条件

//					if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName()))  //closed in V1.9
//						continue; //Condition已包含的,不再遍历

					if (firstWhere) {
						sqlBuffer.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" ").append(K.and).append(" ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName(), entity.getClass()));

					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" ").append(K.isNull);
					} else {

						sqlBuffer.append("=");
						sqlBuffer.append("?");

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
						list.add(preparedValue);
					}
				}
			} // end for

			if (condition != null) {
//				condition=condition.clone();
				condition.setSuidType(SuidType.DELETE); // delete
				OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
				firstWhere = ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
			}

			sql = sqlBuffer.toString();

			setContext(sql, list, tableName);

			// 不允许删整张表
			// 只支持是否带where检测 v1.7.2
			if (firstWhere) {
				boolean notDeleteWholeRecords = HoneyConfig.getHoneyConfig().notDeleteWholeRecords;
				if (notDeleteWholeRecords) {
					Logger.logSQL("delete SQL: ", sql);
					throw new BeeIllegalBusinessException(
							"BeeIllegalBusinessException: It is not allowed delete whole records in one table.");
				}
			}

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;
	}

	private static boolean isContainField(String checkFields[], String fieldName) {
		if (checkFields == null) return false;
		int len = checkFields.length;
		for (int i = 0; i < len; i++) {
			if (checkFields[i].equalsIgnoreCase(fieldName)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isExcludeField(String excludeFieldList, String checkField) {
		String excludeFields[] = excludeFieldList.split(",");
		for (String f : excludeFields) {
			if (f.equals(checkField)) return true;
		}

		return false;
	}

	static void setContext(String sql, List<PreparedValue> list, String tableName) {
//		tableName = tableName.replace(StringConst.ShardingTableIndexStr, ""); // 2.4.0.8   close in 2.4.2
		HoneyContext.setContext(sql, list, tableName);
	}

	private static <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}

	private static String _toTableName(Object entity) {
		String tableName = NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
		return ShardingUtil.appendTableIndexIfNeed(tableName); // 2.4.0.8
	}

	@SuppressWarnings("rawtypes")
	private static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
	}

	static <T> void setInitIdByAuto(T entity) {

		if (entity == null) return;
//		boolean needGenId = HoneyContext.isNeedGenId(entity.getClass());
//		if (!needGenId) return ;

		Field field = null;
		boolean hasValue = false;
//		Long v = null;
		Object obj = null;
		String pkName = "";
		String pkAlias = "";
		boolean isStringField = false;
		boolean hasGenUUIDAnno = false;
		boolean useSeparatorInUUID = false;
		try {
			// V1.11
			boolean noId = false;
			try {
				field = HoneyUtil.getField(entity.getClass(), "id");

				pkName = "id";
			} catch (NoSuchFieldException e) {
				noId = true;
			}
			if (noId) {
				pkName = HoneyUtil.getPkFieldName(entity);
				if ("".equals(pkName) || pkName == null || pkName.contains(",")) {
					Logger.warn("setInitIdByAuto just support single primary key.");
					return; // just support single primary key. 自动设置主键值,只支持单主键.
				}
				field = HoneyUtil.getField(entity.getClass(), pkName);
				pkAlias = "(" + pkName + ")";
			}

			if (field == null) return; // 没有主键,则提前返回

			boolean replaceOldValue = HoneyConfig.getHoneyConfig().genid_replaceOldId;

			if (field.isAnnotationPresent(GenId.class)) {
				GenId genId = field.getAnnotation(GenId.class);
				replaceOldValue = replaceOldValue || genId.override();
			} else if (field.isAnnotationPresent(GenUUID.class)) {
				GenUUID gen = field.getAnnotation(GenUUID.class);
				replaceOldValue = replaceOldValue || gen.override();
				hasGenUUIDAnno = true;
				useSeparatorInUUID = gen.useSeparator();
			} else {
				boolean needGenId = HoneyContext.isNeedGenId(entity.getClass());
				if (!needGenId) return;
			}

			isStringField = field.getType().equals(String.class);
			if (hasGenUUIDAnno && !isStringField) {
				Logger.warn("Gen UUID as id just support String type field!");
				return;
			}

//			if (!field.getType().equals(Long.class)) {
			if (errorType(field)) {// set Long or Integer type id
				Logger.warn("The id" + pkAlias + " field's " + field.getType()
						+ " is not Long/Integer, can not generate the Long/Integer id automatically!");
				return; // just set the Long/Integer id field
			}

			HoneyUtil.setAccessibleTrue(field);
			obj = field.get(entity);
			if (obj != null) {
				if (!replaceOldValue) return;
				hasValue = true;
//				v = (Long) obj; //只用于提示
			}
			OneTimeParameter.setTrueForKey(StringConst.OLD_ID_EXIST);
			OneTimeParameter.setAttribute(StringConst.OLD_ID, obj);
			OneTimeParameter.setAttribute(StringConst.Primary_Key_Name, pkName);
		} catch (NoSuchFieldException e) {
			// is no id field , ignore.
			return;
		} catch (Exception e) {
			Logger.error(e.getMessage());
			return;
		}

		String tableKey = _toTableName(entity);
		Object id;
		if (isInt(field)) {
			int intId = (int) GenIdFactory.get(tableKey, GenIdFactory.GenType_IntSerialIdReturnLong);
			id = intId;
		} else if (hasGenUUIDAnno && isStringField) {
			id = UUID.getId(useSeparatorInUUID);
		} else if (isStringField) {
			id = GenIdFactory.get(tableKey) + "";
		} else {
			long longId = GenIdFactory.get(tableKey);
			id = longId;
		}
		HoneyUtil.setAccessibleTrue(field);
		try {
			HoneyUtil.setFieldValue(field, entity, id);
			if (hasValue) {
				Logger.warn(" [ID WOULD BE REPLACED] " + entity.getClass() + " 's id field" + pkAlias + " value is "
						+ obj.toString() + " would be replace by " + id);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	static boolean errorType(Field field) {
		Class type = field.getType();
		return !(type.equals(Long.class) || type.equals(Integer.class) || type.equals(long.class)
				|| type.equals(int.class) || type.equals(String.class));
	}

	static boolean isInt(Field field) {
		Class type = field.getType();
		return type.equals(Integer.class) || type.equals(int.class);
	}

	// V1.11
	private static boolean isNullPkOrId(Field field, Object entity) {
		try {
			if (field.get(entity) == null && isPrimaryKey(field)) return true;
		} catch (Exception e) {
			// ignroe
		}
		return false;
	}

	// V1.11
	private static boolean isPrimaryKey(Field field) {
		if ("id".equalsIgnoreCase(field.getName())) return true;
		return AnnoUtil.isPrimaryKey(field);
	}
}
