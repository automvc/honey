package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeIllegalBusinessException;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * @author Kingstar
 * @since  1.0
 */
final class _ObjectToSQLHelper {

	private final static String INSERT_INTO = "insert into ";

	private _ObjectToSQLHelper() {}
	
	static <T> String _toSelectSQL(T entity, String fieldNameList) {
		checkPackage(entity);
		
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		try {
			String tableName = _toTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields();

			sqlBuffer.append("select " + fieldNameList + " from "); //need replace
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);
				if (fields[i].get(entity) == null || "serialVersionUID".equals(fields[i].getName()) 
				 || fields[i].isAnnotationPresent(JoinTable.class)){
					continue;
				}else {
					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}

					sqlBuffer.append(_toColumnName(fields[i].getName()));
					sqlBuffer.append("=");
					sqlBuffer.append("?");

					valueBuffer.append(",");
					valueBuffer.append(fields[i].get(entity));

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(k++, preparedValue);
				}
			}

//			sqlBuffer.append(" ;");

			sql = sqlBuffer.toString();
			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sql, valueBuffer.toString());
			addInContextForCache(sql, valueBuffer.toString(), tableName);//2019-09-29
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;

	}
	
	static <T> String _toSelectSQL(T entity, int includeType, Condition condition) {
		checkPackage(entity);
		
		Set<String> conditionFieldSet=null;
		if(condition!=null) conditionFieldSet=condition.getFieldSet();
		
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		String tableName = _toTableName(entity);
		List<PreparedValue> list = new ArrayList<>();
		boolean firstWhere = true;
		try {
			Field fields[] = entity.getClass().getDeclaredFields(); 

			String packageAndClassName = entity.getClass().getName();
			String columnNames = HoneyContext.getBeanField(packageAndClassName);
			if (columnNames == null) {
				columnNames = HoneyUtil.getBeanField(fields);
				HoneyContext.addBeanField(packageAndClassName, columnNames);
			}

			sqlBuffer.append("select " + columnNames + " from ");
			sqlBuffer.append(tableName);
			
			int len = fields.length;
			
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);
				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
					continue;
				} else {
					
					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
						continue; //id=null不作为过滤条件
					
					if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName())) 
						continue; //Condition已包含的,不再遍历

					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName()));
					
					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" is null");
					} else {
						sqlBuffer.append("=");
						sqlBuffer.append("?");

						valueBuffer.append(",");
						valueBuffer.append(fields[i].get(entity));

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						list.add(k++, preparedValue);
					}
				}
			}//end for
			
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
			
			if(condition!=null){
				 condition.setSuidType(SuidType.SELECT);
			     ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere);
			}
//			sqlBuffer.append(";");

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sqlBuffer.toString(), list);
			HoneyContext.setSqlValue(sqlBuffer.toString(), valueBuffer.toString()); //用于log显示
			addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29


		return sqlBuffer.toString();
	}
	
	static <T> String _toSelectSQL(T entity, int includeType) {
         return _toSelectSQL(entity, includeType, null);
	}
	
/*	static <T> String _toUpdateSQL(T entity, String whereColumn, int includeType) throws ObjSQLException, IllegalAccessException {
		checkPackage(entity);
		
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		StringBuffer whereValueBuffer = new StringBuffer();
		boolean firstSet = true;
		boolean isExistWhere = false; //don't delete
		StringBuffer whereStament = new StringBuffer();
		String tableName = _toTableName(entity);
		sqlBuffer.append("update ");
		sqlBuffer.append(tableName);
		sqlBuffer.append(" set ");

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		List<PreparedValue> whereList = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0, k = 0, w = 0; i < len; i++) {
			fields[i].setAccessible(true);
			if ("id".equalsIgnoreCase(whereColumn) && fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName()))
				throw new ObjSQLException("ObjSQLException: in the update(T entity), the id field of entity must not be null !");
			//			if (fields[i].get(entity) == null || "serialVersionUID".equals(fields[i].getName()))
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
				continue;
			} else {
				if (whereColumn.equalsIgnoreCase(fields[i].getName())) { //java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String
					whereStament.append(" where ");
					whereStament.append(_toColumnName(fields[i].getName()));

					if (fields[i].get(entity) == null) {
						whereValueBuffer.append(" is null");
					} else {

						whereStament.append("=");
						whereStament.append("?");

						whereValueBuffer.append(",");
						whereValueBuffer.append(fields[i].get(entity));

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						whereList.add(w++, preparedValue);
					}
					isExistWhere = true;
				} else { //set value

					if (firstSet) {

						sqlBuffer.append(" ");
						firstSet = false;
					} else {
						//  sqlBuffer.append(" and "); //update 的set部分不是用and  ，而是用逗号的
						sqlBuffer.append(" , ");
					}

					sqlBuffer.append(_toColumnName(fields[i].getName()));
					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" =null"); //  =
					} else {

						sqlBuffer.append("=");
						sqlBuffer.append("?");

						valueBuffer.append(",");
						valueBuffer.append(fields[i].get(entity));

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						list.add(k++, preparedValue);
					}
				}
			}
		}//end for
		sqlBuffer.append(whereStament);
//		sqlBuffer.append(" ;");
		sql = sqlBuffer.toString();

		list.addAll(whereList);
		valueBuffer.append(whereValueBuffer);

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
		HoneyContext.setPreparedValue(sql, list);
		HoneyContext.setSqlValue(sql, valueBuffer.toString());
		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
//		if(!isExistWhere) {sql="no where stament for filter!"; throw new ObjSQLException("no where stament for filter!"); }

		return sql;
	}*/
	
//	static <T> String _toUpdateSQL(T entity, String whereColumn, int includeType) {
	static <T> String _toUpdateSQL(T entity, int includeType) { //whereColumn is id
		checkPackage(entity);
		Field field = null;
		try {
			field = entity.getClass().getDeclaredField("id");
		} catch (Exception e) {
			//if have exception, express "id".equalsIgnoreCase(whereColumn) is false.
		}
		if (field == null) {
			throw new ObjSQLException("ObjSQLException: in the update(T entity) or update(T entity,IncludeType includeType), the id field of entity must not be null !");
		}
		//		UpdateBy id
		return _toUpdateBySQL(entity, new String[] { "id" }, includeType); // update by whereColumn(now is id)
	}
	
	static <T> String _toUpdateSQL(T entity, String setColmn[], int includeType) {
		return _toUpdateSQL(entity, setColmn, includeType, null);
	}

	//v1.7.2 add para Condition condition
	static <T> String _toUpdateSQL(T entity, String setColmns[], int includeType, Condition condition) {
		checkPackage(entity);

		Set<String> conditionFieldSet = null;
		if (condition != null) conditionFieldSet = condition.getFieldSet();
		
		Set<String> updatefieldSet=null;
		if (condition != null) updatefieldSet=condition.getUpdatefieldSet();

		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
//		StringBuffer valueBuffer = new StringBuffer(); //delete 2020-06
		StringBuffer whereValueBuffer = new StringBuffer();
		boolean firstSet = true;
		boolean firstWhere = true;
		boolean isExistWhere = false;
		StringBuffer whereStament = new StringBuffer();
		List<PreparedValue> list = new ArrayList<>();
		String tableName = _toTableName(entity);
		
		try {

			sqlBuffer.append("update ");
			sqlBuffer.append(tableName);
			sqlBuffer.append(" set ");

			//v1.7.2
			if (condition != null) {
				condition.setSuidType(SuidType.UPDATE); //UPDATE
//				firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, whereValueBuffer, list, condition);//bug
//				firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, valueBuffer, list, condition);
				firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, list, condition);
			}

			Field fields[] = entity.getClass().getDeclaredFields();
			int len = fields.length;

			List<PreparedValue> whereList = new ArrayList<>();

			PreparedValue preparedValue = null;
			for (int i = 0, w = 0; i < len; i++) {//delete:k = 0,
				fields[i].setAccessible(true);
//				if (isContainField(setColmns, fields[i].getName())) { //set value.setColmn不受includeType影响,都会转换
				if (isContainField(setColmns, fields[i].getName())     
						&& ( (updatefieldSet ==null) || (updatefieldSet != null && !updatefieldSet.contains(fields[i].getName())) ) // 在updatefieldSet为新值，entity 的为旧值可放在where条件    v1.7.3
						) {	//在指定的setColmns,且还没有用在set,setAdd,setMultiply的字段,才转成update set的部分.
					
//					在updatefieldSet为新值，entity 的为旧值可放在where条件    v1.7.3
//					//v1.7.2
//					if (updatefieldSet != null && updatefieldSet.contains(fields[i].getName())) 
//						continue; //Condition已包含的set条件,不再作转换处理

					if (firstSet) {
						sqlBuffer.append(" ");
						firstSet = false;
					} else {
						//  sqlBuffer.append(" and "); //update 的set部分不是用and  ，而是用逗号的
						sqlBuffer.append(" , ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName()));

					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" =null"); //  =
					} else {

						sqlBuffer.append("=");
						sqlBuffer.append("?");

//						valueBuffer.append(",");
//						valueBuffer.append(fields[i].get(entity));

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
//						list.add(k++, preparedValue);
						list.add(preparedValue);
					}
				} else {//where

					if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
						continue;
					} else {
						if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
							continue; //id=null不作为过滤条件
						
						//v1.7.2
						if (conditionFieldSet != null && conditionFieldSet.contains(fields[i].getName())) 
							continue; //Condition已包含的,不再遍历

						if (firstWhere) {
							whereStament.append(" where ");
							firstWhere = false;
						} else {
							whereStament.append(" and ");
						}
						whereStament.append(_toColumnName(fields[i].getName()));

						if (fields[i].get(entity) == null) {
							whereStament.append(" is null");
						} else {

							whereStament.append("=");
							whereStament.append("?");

							whereValueBuffer.append(",");
							whereValueBuffer.append(fields[i].get(entity));

							preparedValue = new PreparedValue();
							preparedValue.setType(fields[i].getType().getName());
							preparedValue.setValue(fields[i].get(entity));
							whereList.add(w++, preparedValue);
						}
						isExistWhere = true;
					}
				}//end else
			}//end for
			
			sqlBuffer.append(whereStament);
			//sqlBuffer.append(" ;");

			list.addAll(whereList);
//			valueBuffer.append(whereValueBuffer);
			
			if(firstSet) {
				Logger.logSQL("update SQL(updateFields) :", sqlBuffer.toString());
				throw new BeeErrorGrammarException("BeeErrorGrammarException: the SQL update set part is empty!");
			}

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		if (condition != null) {
			condition.setSuidType(SuidType.UPDATE); //UPDATE
			//即使condition包含的字段是whereColumn里的字段也会转化到sql语句.
//			firstWhere = ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere);
			firstWhere = ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
		}

		sql = sqlBuffer.toString();

////		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
//		HoneyContext.setPreparedValue(sql, list);
////		HoneyContext.setSqlValue(sql, valueBuffer.toString());  //change get the value from list
////		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
//		
//		String value=HoneyUtil.list2Value(list);
//		HoneyContext.setSqlValue(sql, value);
//		addInContextForCache(sql, value, tableName);//2019-09-29
		
		setContext(sql, list, tableName);
		

		//不允许更新一个表的所有数据
		//v1.7.2 只支持是否带where检测
		if (firstWhere) {
			boolean notUpdateWholeRecords = HoneyConfig.getHoneyConfig().isNotUpdateWholeRecords();
			if (notUpdateWholeRecords) {
				Logger.logSQL("update SQL: ", sql);
				throw new BeeIllegalBusinessException("BeeIllegalBusinessException: It is not allowed update whole records in one table.");
				//return "";
			}
		}

		return sql;
	}
	
	//for updateBy
	static <T> String _toUpdateBySQL(T entity, String whereColumns[], int includeType) {
		return _toUpdateBySQL(entity, whereColumns, includeType, null);
	}
	
	//for updateBy
	//v1.7.2 add para Condition condition
	static <T> String _toUpdateBySQL(T entity, String whereColumns[], int includeType, Condition condition){
		checkPackage(entity);
		
		Set<String> conditionFieldSet=null;
		if(condition!=null) conditionFieldSet=condition.getFieldSet();
		
		Set<String> updatefieldSet=null;
		if (condition != null) updatefieldSet=condition.getUpdatefieldSet();
		
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
//		StringBuffer valueBuffer = new StringBuffer();
//		StringBuffer whereValueBuffer = new StringBuffer();
		boolean firstSet = true;
		boolean firstWhere = true;
		boolean isExistWhere = false;
		StringBuffer whereStament = new StringBuffer();
		String tableName = _toTableName(entity);
		List<PreparedValue> list = new ArrayList<>();
		
		try{
		
		sqlBuffer.append("update ");
		sqlBuffer.append(tableName);
		sqlBuffer.append(" set ");
		
//		setMultiply,setAdd,是在处理字段前已完成处理的,所以不受指定的where条件的字段(即String whereColumns[])的影响.
		//v1.7.2
		if (condition != null) {
			condition.setSuidType(SuidType.UPDATE); //UPDATE
//			firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, whereValueBuffer, list, condition);
//			firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, valueBuffer, list, condition);
			firstSet = ConditionHelper.processConditionForUpdateSet(sqlBuffer, list, condition);
		}

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		
		List<PreparedValue> whereList = new ArrayList<>();

		PreparedValue preparedValue = null;
		for (int i = 0,  w = 0; i < len; i++) { //delete:k = 0,
			fields[i].setAccessible(true);
			if (! isContainField(whereColumns, fields[i].getName())) { //set value.  不属性whereColumn的,将考虑转为set.  同一个实体的某个属性的值,若用于set部分了,再用于where部分就没有意义.
				
				//set 字段根据includeType过滤
				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
					continue;
				}
				if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName()))
					continue; //id=null跳过,id不更改.
				
				//v1.7.2
				if (updatefieldSet != null && updatefieldSet.contains(fields[i].getName())) 
					continue; //Condition已包含的set条件,不再作转换处理
				
				if (firstSet) {
					sqlBuffer.append(" ");
					firstSet = false;
				} else {
					//  sqlBuffer.append(" and "); //update 的set部分不是用and  ，而是用逗号的
					sqlBuffer.append(" , ");
				}
				sqlBuffer.append(_toColumnName(fields[i].getName()));

				if (fields[i].get(entity) == null) {
					sqlBuffer.append(" =null"); //  =
				} else {

					sqlBuffer.append("=");
					sqlBuffer.append("?");

//					valueBuffer.append(",");
//					valueBuffer.append(fields[i].get(entity));

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
//					list.add(k++, preparedValue);
					list.add(preparedValue);
				}
			} else {// where .   此部分只会有whereColumn的字段

//				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
//					continue;
//				} else {
				
//				v1.7.2
				if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName())) 
					continue; //Condition已包含的,不再遍历
				
				//指定作为条件的,都转换
					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName()))
						continue; //id=null不作为过滤条件
					if (firstWhere) {
						whereStament.append(" where ");
						firstWhere = false;
					} else {
						whereStament.append(" and ");
					}
					whereStament.append(_toColumnName(fields[i].getName()));

					if (fields[i].get(entity) == null) {
						whereStament.append(" is null");
					} else {

						whereStament.append("=");
						whereStament.append("?");

//						whereValueBuffer.append(",");
//						whereValueBuffer.append(fields[i].get(entity));

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						whereList.add(w++, preparedValue);
					}
					isExistWhere = true;
//				}
			}//end else
		}//end for
		sqlBuffer.append(whereStament);
//		sqlBuffer.append(" ;");
		
		list.addAll(whereList);
//		valueBuffer.append(whereValueBuffer);

		if(firstSet) {
			Logger.logSQL("update SQL(updateFields) :", sqlBuffer.toString());
			throw new BeeErrorGrammarException("BeeErrorGrammarException: the SQL update set part is empty!");
		}
		
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		
		if(condition!=null){
			 condition.setSuidType(SuidType.UPDATE); //UPDATE
			 //即使condition包含的字段是whereColumn里的字段也会转化到sql语句.
//			 firstWhere= ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere);
			 firstWhere= ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
		}
		
		sql = sqlBuffer.toString();

////		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
//		HoneyContext.setPreparedValue(sql, list);
////		HoneyContext.setSqlValue(sql, valueBuffer.toString());
////		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
//		
//		String value=HoneyUtil.list2Value(list);
//		HoneyContext.setSqlValue(sql, value);
//		addInContextForCache(sqlBuffer.toString(), value, tableName);
		
		setContext(sql, list, tableName);

		//不允许更新一个表的所有数据
		//v1.7.2 只支持是否带where检测
		if (firstWhere) {
			boolean notUpdateWholeRecords = HoneyConfig.getHoneyConfig().isNotUpdateWholeRecords();
			if (notUpdateWholeRecords) {
				Logger.logSQL("update SQL: ", sql);
				throw new BeeIllegalBusinessException("BeeIllegalBusinessException: It is not allowed update whole records in one table.");
				//return "";
			}
		}
		
		return sql;
	}

	static <T> String _toInsertSQL(T entity, int includeType) throws IllegalAccessException {
		checkPackage(entity);
		
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlValue = new StringBuffer(") values (");
		StringBuffer valueBuffer = new StringBuffer();
		String sql = "";
		boolean isFirst = true;
		String tableName = _toTableName(entity);

		sqlBuffer.append(INSERT_INTO);
		sqlBuffer.append(tableName);
		sqlBuffer.append("(");

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0, k = 0; i < len; i++) {
			fields[i].setAccessible(true);
			/*			if (fields[i].get(entity) == null){
			//				continue;
							if(isIncludeNullField) {
							
							}else{
								continue;
							}
						}*/
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
				continue;
			} else {

				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuffer.append(",");
					sqlValue.append(",");
				}

				sqlBuffer.append(_toColumnName(fields[i].getName()));

				if (fields[i].get(entity) == null) {
					sqlValue.append("null");
				} else {
					sqlValue.append("?");

					valueBuffer.append(",");
					valueBuffer.append(fields[i].get(entity));

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(k++, preparedValue);
				}
			}
		}

		sqlBuffer.append(sqlValue);
		sqlBuffer.append(")");
//		sqlBuffer.append(" ;");

		sql = sqlBuffer.toString();

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
		HoneyContext.setPreparedValue(sql, list);
		HoneyContext.setSqlValue(sql, valueBuffer.toString());
		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
		return sql;
	}

	//	 * for entity[]
	static <T> SqlValueWrap _toInsertSQL0(T entity, int includeType, String excludeFieldList) throws IllegalAccessException {
		checkPackage(entity);
		
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlValue = new StringBuffer(") values (");
		StringBuffer valueBuffer = new StringBuffer();

		SqlValueWrap wrap = new SqlValueWrap();

		boolean isFirst = true;
		String tableName = _toTableName(entity);
		wrap.setTableNames(tableName);//2019-09-29
		
		sqlBuffer.append(INSERT_INTO);
		sqlBuffer.append(tableName);
		sqlBuffer.append("(");

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0, k = 0; i < len; i++) {
			fields[i].setAccessible(true);
			/*			if (fields[i].get(entity) == null){
			//				continue;
							if(isIncludeNullField) {
								
							}else{
								continue;
							}
						}*/
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
				continue;
			} else {

				if (!"".equals(excludeFieldList) && isExcludeField(excludeFieldList, fields[i].getName())) continue;

				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuffer.append(",");
					sqlValue.append(",");
				}

				sqlBuffer.append(_toColumnName(fields[i].getName()));

//					if(fields[i].get(entity) == null){
//						sqlValue.append("null");
//					}else{
				sqlValue.append("?");

				valueBuffer.append(",");
				valueBuffer.append(fields[i].get(entity));

				preparedValue = new PreparedValue();
				preparedValue.setType(fields[i].getType().getName());
				preparedValue.setValue(fields[i].get(entity));
				list.add(k++, preparedValue);
			}
		}

		sqlBuffer.append(sqlValue);
		sqlBuffer.append(")");
//		sqlBuffer.append(" ;");

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);

		wrap.setSql(sqlBuffer.toString());
		wrap.setList(list);
		wrap.setValueBuffer(valueBuffer);

		return wrap;
	}

	static <T> SqlValueWrap _toInsertSQL_for_ValueList(T entity, String excludeFieldList) throws IllegalAccessException {
		checkPackage(entity);
		
		StringBuffer valueBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0, k = 0; i < len; i++) {
			fields[i].setAccessible(true);

			if ("serialVersionUID".equals(fields[i].getName())){
				continue;
			}else if (fields[i]!= null && fields[i].isAnnotationPresent(JoinTable.class)){
				continue;
			}else if (!"".equals(excludeFieldList) && isExcludeField(excludeFieldList, fields[i].getName())) continue;

			valueBuffer.append(",");
			valueBuffer.append(fields[i].get(entity));

			preparedValue = new PreparedValue();
			preparedValue.setType(fields[i].getType().getName());
			preparedValue.setValue(fields[i].get(entity));
			list.add(k++, preparedValue);
		}

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);

		// wrap.setSql(sqlBuffer.toString()); //用sql[0]的
		wrap.setList(list);
		wrap.setValueBuffer(valueBuffer);

		return wrap;
	}
	
	static <T> String _toDeleteSQL(T entity, int includeType) {
		return _toDeleteSQL(entity, includeType,null);
	}

	static <T> String _toDeleteSQL(T entity, int includeType, Condition condition) {
		checkPackage(entity);
		
		Set<String> conditionFieldSet=null;
		if(condition!=null) conditionFieldSet=condition.getFieldSet();
		
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		boolean firstWhere = true;
		try {
			String tableName = _toTableName(entity);

			sqlBuffer.append("delete from ");
			sqlBuffer.append(tableName);

			Field fields[] = entity.getClass().getDeclaredFields();
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);

				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i])) {
					continue;
				} else {

					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
						continue; //id=null不作为过滤条件
					
					if(conditionFieldSet!=null && conditionFieldSet.contains(fields[i].getName())) 
						continue; //Condition已包含的,不再遍历

					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName()));
					
					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" is null");
					} else {

						sqlBuffer.append("=");
						sqlBuffer.append("?");

						valueBuffer.append(",");
						valueBuffer.append(fields[i].get(entity));

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						list.add(k++, preparedValue);
					}
				}
			}//end for
//			sqlBuffer.append(" ;");
			
			
			if(condition!=null){
				 condition.setSuidType(SuidType.DELETE); //delete
				 firstWhere= ConditionHelper.processCondition(sqlBuffer, valueBuffer, list, condition, firstWhere);
			}
			
			
			sql = sqlBuffer.toString();

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sql, valueBuffer.toString());
			addInContextForCache(sql, valueBuffer.toString(), tableName);//2019-09-29
			
			
			//不允许删整张表
			//v1.7.2 只支持是否带where检测
			if (firstWhere) {
				boolean notDeleteWholeRecords = HoneyConfig.getHoneyConfig().isNotDeleteWholeRecords();
				if (notDeleteWholeRecords) {
					Logger.logSQL("delete SQL: ", sql);
					throw new BeeIllegalBusinessException("BeeIllegalBusinessException: It is not allowed delete whole records in one table.");
					//return "";
				}
			}
			
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;
	}

	private static boolean isContainField(String checkFields[], String fieldName) {
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
	
	private static void setContext(String sql,List<PreparedValue> list,String tableName){
		HoneyContext.setPreparedValue(sql, list);
		String value=HoneyUtil.list2Value(list,true);
//		HoneyContext.setSqlValue(sql, value);
		addInContextForCache(sql, value, tableName);//2019-09-29
	}
	
    static void addInContextForCache(String sql,String sqlValue, String tableName){
		CacheSuidStruct struct=new CacheSuidStruct();
		struct.setSql(sql);
		struct.setSqlValue(sqlValue);
		struct.setTableNames(tableName);
		
		HoneyContext.setCacheInfo(sql, struct);
	}
    
	private static <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}
	
	private static String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	private static String _toColumnName(String fieldName){
		return NameTranslateHandle.toColumnName(fieldName);
	}
}
