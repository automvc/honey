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
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * @author Kingstar
 * @since  1.0
 */
final class _ObjectToSQLHelper {

	private final static String INSERT_INTO = "insert into ";
	
	private static boolean  showSQL=HoneyConfig.getHoneyConfig().isShowSQL();

	private _ObjectToSQLHelper() {}
	
	static <T> String _toSelectSQL(T entity, String fieldNameList) {
		checkPackage(entity);
		
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
//		StringBuffer valueBuffer = new StringBuffer();
		try {
			String tableName = _toTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields();

			sqlBuffer.append("select " + fieldNameList + " from "); //need replace
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0; i < len; i++) {
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

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
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
		checkPackage(entity);
		
		Set<String> conditionFieldSet=null;
		if(condition!=null) conditionFieldSet=condition.getFieldSet();
		
		StringBuffer sqlBuffer = new StringBuffer();
		String tableName = _toTableName(entity);
		List<PreparedValue> list = new ArrayList<>();
		boolean firstWhere = true;
		try {
			Field fields[] = entity.getClass().getDeclaredFields(); 
			String columnNames;
			
			String packageAndClassName = entity.getClass().getName();
			columnNames = HoneyContext.getBeanField(packageAndClassName);
			if (columnNames == null) {
				columnNames = HoneyUtil.getBeanField(fields);
				HoneyContext.addBeanField(packageAndClassName, columnNames);
			}
			
			if (condition != null) {
				condition.setSuidType(SuidType.SELECT);
				String selectField = ConditionHelper.processSelectField(columnNames, condition);
				if (selectField != null) columnNames = selectField;
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
			     ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
			}

		setContext(sqlBuffer.toString(), list, tableName);

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
		
//		if (updateFields.length == 0 || "".equals(updateFieldList.trim()))
		
		if( (setColmns==null || (setColmns.length==1 && "".equals(setColmns[0].trim()) ) )
		   && (updatefieldSet==null || updatefieldSet.size()==0) ){
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

			sqlBuffer.append("update ");
			sqlBuffer.append(tableName);
			sqlBuffer.append(" set ");

			//v1.7.2
			if (condition != null) {
				condition.setSuidType(SuidType.UPDATE); //UPDATE
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
						&& ( (updatefieldSet ==null) || (updatefieldSet != null && !updatefieldSet.contains(fields[i].getName())) ) // 在updatefieldSet为新值，entity 的为旧值可放在where条件    v1.8
						) {	//在指定的setColmns,且还没有用在set,setAdd,setMultiply的字段,才转成update set的部分.
					
//					在updatefieldSet为新值，entity 的为旧值可放在where条件    v1.8
//					//v1.7.2
//					if (updatefieldSet != null && updatefieldSet.contains(fields[i].getName())) 
//						continue; //Condition已包含的set条件,不再作转换处理

					if (firstSet) {
						sqlBuffer.append(" ");
						firstSet = false;
					} else {
						sqlBuffer.append(" , ");//update 的set部分不是用and  ，而是用逗号的
					}
					sqlBuffer.append(_toColumnName(fields[i].getName()));

					if (fields[i].get(entity) == null) {
						sqlBuffer.append(" =null"); //  =
					} else {

						sqlBuffer.append("=");
						sqlBuffer.append("?");

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
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

			list.addAll(whereList);
			
			if(firstSet) {
				Logger.logSQL("update SQL(updateFields) : ", sqlBuffer.toString());
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
					sqlBuffer.append(" , ");//update 的set部分不是用and  ，而是用逗号的
				}
				sqlBuffer.append(_toColumnName(fields[i].getName()));

				if (fields[i].get(entity) == null) {
					sqlBuffer.append(" =null"); //  =
				} else {

					sqlBuffer.append("=");
					sqlBuffer.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
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
		
		list.addAll(whereList);

		if(firstSet) {
			Logger.logSQL("update SQL(updateFields) : ", sqlBuffer.toString());
			throw new BeeErrorGrammarException("BeeErrorGrammarException: the SQL update set part is empty!");
		}
		
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		
		if(condition!=null){
			 condition.setSuidType(SuidType.UPDATE); //UPDATE
			 //即使condition包含的字段是whereColumn里的字段也会转化到sql语句.
			 firstWhere= ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
		}
		
		sql = sqlBuffer.toString();

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

/*	static <T> String _toInsertSQL(T entity, int includeType) throws IllegalAccessException {
		checkPackage(entity);
		
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlValue = new StringBuffer(") values (");
//		StringBuffer valueBuffer = new StringBuffer();
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
		for (int i = 0; i < len; i++) {
			fields[i].setAccessible(true);
						if (fields[i].get(entity) == null){
			//				continue;
							if(isIncludeNullField) {
							
							}else{
								continue;
							}
						}
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

//					valueBuffer.append(",");
//					valueBuffer.append(fields[i].get(entity));

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(preparedValue);
				}
			}
		}

		sqlBuffer.append(sqlValue);
		sqlBuffer.append(")");
//		sqlBuffer.append(" ;");

		sql = sqlBuffer.toString();

//		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
//		HoneyContext.setPreparedValue(sql, list);
//		HoneyContext.setSqlValue(sql, valueBuffer.toString());
//		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
		
		setContext(sql, list, tableName);
		
		return sql;
	}*/

//	static <T> SqlValueWrap _toInsertSQL0(T entity, int includeType, String excludeFieldList) throws IllegalAccessException {
	static <T> String _toInsertSQL0(T entity, int includeType, String excludeFieldList) throws IllegalAccessException {
		checkPackage(entity);
		
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
//		StringBuffer sqlValue = new StringBuffer(") values (");
		StringBuffer sqlValue = new StringBuffer(" (");
		boolean isFirst = true;
		String tableName = _toTableName(entity);
		sqlBuffer.append(INSERT_INTO);
		sqlBuffer.append(tableName);
		sqlBuffer.append("(");

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0; i < len; i++) {
			fields[i].setAccessible(true);
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
				sqlValue.append("?");

				preparedValue = new PreparedValue();
				preparedValue.setType(fields[i].getType().getName());
				preparedValue.setValue(fields[i].get(entity));
				list.add(preparedValue);
			}
		}
		sqlValue.append(")");
		
        sqlBuffer.append(") values");
		sqlBuffer.append(sqlValue);
		sql=sqlBuffer.toString();
		
		if("tRue".equals((String)OneTimeParameter.getAttribute("_SYS_Bee_Return_PlaceholderValue"))){
			OneTimeParameter.setAttribute("_SYS_Bee_PlaceholderValue", sqlValue.toString());
		}
		
		setContext(sql, list, tableName);

		return sql;
	}

	//只需要解析值   for array[]
	static <T> List<PreparedValue> _toInsertSQL_for_ValueList(String sql_i, T entity, String excludeFieldList) throws IllegalAccessException {
		checkPackage(entity);

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0; i < len; i++) {
			fields[i].setAccessible(true);

			if ("serialVersionUID".equals(fields[i].getName())) {
				continue;
			} else if (fields[i] != null && fields[i].isAnnotationPresent(JoinTable.class)) {
				continue;
			} else if (!"".equals(excludeFieldList) && isExcludeField(excludeFieldList, fields[i].getName())) continue;

			preparedValue = new PreparedValue();
			preparedValue.setType(fields[i].getType().getName());
			preparedValue.setValue(fields[i].get(entity));
			list.add(preparedValue);
		}

//		if (showSQL) { //just insert array to this method
		if (HoneyUtil.isMysql() && !showSQL) {
             //no need set context
		} else {
			HoneyContext.setPreparedValue(sql_i, list);
		}
		return list;
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

						preparedValue = new PreparedValue();
						preparedValue.setType(fields[i].getType().getName());
						preparedValue.setValue(fields[i].get(entity));
						list.add(k++, preparedValue);
					}
				}
			}//end for
			
			if(condition!=null){
				 condition.setSuidType(SuidType.DELETE); //delete
				 firstWhere= ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
			}
			
			
			sql = sqlBuffer.toString();

			setContext(sql, list, tableName);
			
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
	
	static void setContext(String sql,List<PreparedValue> list,String tableName){
		HoneyContext.setContext(sql, list, tableName);
	}
	
//  static void addInContextForCache(String sql,String sqlValue, String tableName){ //changed v1.8
    static void addInContextForCache(String sql, String tableName){
    	HoneyContext.addInContextForCache(sql, tableName);
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
	
	static <T> void setInitIdByAuto(T entity) {

//		boolean needGenId = HoneyConfig.getHoneyConfig().genid_forAllTableLongId;
//		if (!needGenId) return;
		if(entity==null) return ;
		boolean needGenId = HoneyContext.isNeedGenId(entity.getClass());
		if (!needGenId) return;

		Field field = null;
		boolean hasValue=false;
		Long v=null;
		try {
			field = entity.getClass().getDeclaredField("id");
			field.setAccessible(true);
		  //if (field.get(entity) != null) return;
			if (field.get(entity) != null) {
				hasValue=true;
			}
		} catch (NoSuchFieldException e) {
			//is no id field , ignore.
			return;	
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (!field.getType().equals(Long.class)) return; //just set the Long id field

		String tableKey = _toTableName(entity);
		long id = GenIdFactory.get(tableKey);
		field.setAccessible(true);
		try {
			field.set(entity, id);
			if(hasValue){
				v=(Long)field.get(entity);
				Logger.warn(" [ID WOULD BE OVERRIDE] "+entity.getClass()+" 's id field value is "+v +" would be replace by "+id);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

	}
}
