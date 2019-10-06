package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.honey.osql.cache.CacheSuidStruct;

/**
 * @author Kingstar
 * @since  1.0
 */
final class _ObjectToSQLHelper {

	private final static String INSERT_INTO = "insert into ";

	private _ObjectToSQLHelper() {}

	static <T> String _toSelectSQL(T entity, String fieldNameList) {
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		try {
			String tableName = ConverString.getTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields();

			sqlBuffer.append("select " + fieldNameList + " from "); //need replace
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);
				if (fields[i].get(entity) == null || "serialVersionUID".equals(fields[i].getName()))
					continue;
				else {
					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}

					sqlBuffer.append(transformStr(fields[i].getName()));
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

			sqlBuffer.append(" ;");

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

	static <T> String _toSelectSQL(T entity, int includeType) {

		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		try {
			String tableName = ConverString.getTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields(); //返回所有字段,包括公有和私有     //改为以最高权限访问？2012-07-15 no

			String packageAndClassName = entity.getClass().getName();
			String fieldNames = HoneyContext.getBeanField(packageAndClassName);
			if (fieldNames == null) {
				fieldNames = HoneyUtil.getBeanField(fields);
				HoneyContext.addBeanField(packageAndClassName, fieldNames);
			}

			sqlBuffer.append("select " + fieldNames + " from ");
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);
				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i].getName())) {
					continue;
				} else {

					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
						continue; //id=null不作为过滤条件

					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}
					sqlBuffer.append(HoneyUtil.transformStr(fields[i].getName()));
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
			}

			sqlBuffer.append(";");

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sqlBuffer.toString(), list);
			HoneyContext.setSqlValue(sqlBuffer.toString(), valueBuffer.toString()); //用于log显示
			addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sqlBuffer.toString();
	}

	static <T> String _toUpdateSQL(T entity, String whereColmn, int includeType) throws ObjSQLException, IllegalAccessException {
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		StringBuffer whereValueBuffer = new StringBuffer();
		boolean firstSet = true;
		boolean isExistWhere = false; //don't delete
		StringBuffer whereStament = new StringBuffer();
		String tableName = ConverString.getTableName(entity);
		sqlBuffer.append(" update ");
		sqlBuffer.append(tableName);
		sqlBuffer.append(" set ");

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		List<PreparedValue> whereList = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0, k = 0, w = 0; i < len; i++) {
			fields[i].setAccessible(true);
			if ("id".equalsIgnoreCase(whereColmn) && fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName()))
				throw new ObjSQLException("ObjSQLException: in the update(T entity), the id field of entity must not be null !");
			//			if (fields[i].get(entity) == null || "serialVersionUID".equals(fields[i].getName()))
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i].getName())) {
				continue;
			} else {
				if (whereColmn.equalsIgnoreCase(fields[i].getName())) { //java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String
					whereStament.append(" where ");
					whereStament.append(transformStr(fields[i].getName()));

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

					sqlBuffer.append(transformStr(fields[i].getName()));
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
		sqlBuffer.append(" ;");
		sql = sqlBuffer.toString();

		list.addAll(whereList);
		valueBuffer.append(whereValueBuffer);

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
		HoneyContext.setPreparedValue(sql, list);
		HoneyContext.setSqlValue(sql, valueBuffer.toString());
		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
//		if(!isExistWhere) {sql="no where stament for filter!"; throw new ObjSQLException("no where stament for filter!"); }

		return sql;
	}

	static <T> String _toUpdateSQL(T entity, String setColmn[], int includeType) throws IllegalAccessException {
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		StringBuffer whereValueBuffer = new StringBuffer();
		boolean firstSet = true;
		boolean firstWhere = true;
		boolean isExistWhere = false;
		StringBuffer whereStament = new StringBuffer();
		String tableName = ConverString.getTableName(entity);
		sqlBuffer.append(" update ");
		sqlBuffer.append(tableName);
		sqlBuffer.append(" set ");

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		List<PreparedValue> whereList = new ArrayList<>();

		PreparedValue preparedValue = null;
		for (int i = 0, k = 0, w = 0; i < len; i++) {
			fields[i].setAccessible(true);
			if (isContainField(setColmn, fields[i].getName())) { //set value

				if (firstSet) {
					sqlBuffer.append(" ");
					firstSet = false;
				} else {
					//  sqlBuffer.append(" and "); //update 的set部分不是用and  ，而是用逗号的
					sqlBuffer.append(" , ");
				}
				sqlBuffer.append(transformStr(fields[i].getName()));

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
			} else {

				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i].getName())) {
					continue;
				} else {

					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName()))
						continue; //id=null不作为过滤条件
					if (firstWhere) {
						whereStament.append(" where ");
						firstWhere = false;
					} else {
						whereStament.append(" and ");
					}
					whereStament.append(transformStr(fields[i].getName()));

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
		sqlBuffer.append(" ;");
		sql = sqlBuffer.toString();

		list.addAll(whereList);

		valueBuffer.append(whereValueBuffer);

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
		HoneyContext.setPreparedValue(sql, list);
		HoneyContext.setSqlValue(sql, valueBuffer.toString());
		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
		//		if(!isExistWhere) {sql="no where stament for filter!"; throw new ObjSQLException("no where stament for filter!"); }

		return sql;
	}

	static <T> String _toInsertSQL(T entity, int includeType) throws IllegalAccessException {

		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlValue = new StringBuffer(") values (");
		StringBuffer valueBuffer = new StringBuffer();
		String sql = "";
		boolean isFirst = true;
		String tableName = ConverString.getTableName(entity);

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
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i].getName())) {
				continue;
			} else {

				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuffer.append(",");
					sqlValue.append(",");
				}

				sqlBuffer.append(transformStr(fields[i].getName()));

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
		sqlBuffer.append(" ;");

		sql = sqlBuffer.toString();

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
		HoneyContext.setPreparedValue(sql, list);
		HoneyContext.setSqlValue(sql, valueBuffer.toString());
		addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
		return sql;
	}

	//	 * for entity[]
	static <T> SqlValueWrap _toInsertSQL0(T entity, int includeType, String excludeFieldList) throws IllegalAccessException {

		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlValue = new StringBuffer(") values (");
		StringBuffer valueBuffer = new StringBuffer();

		SqlValueWrap wrap = new SqlValueWrap();

		boolean isFirst = true;
		String tableName = ConverString.getTableName(entity);
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
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i].getName())) {
				continue;
			} else {

				if (!"".equals(excludeFieldList) && isExcludeField(excludeFieldList, fields[i].getName())) continue;

				if (isFirst) {
					isFirst = false;
				} else {
					sqlBuffer.append(",");
					sqlValue.append(",");
				}

				sqlBuffer.append(transformStr(fields[i].getName()));

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
		sqlBuffer.append(" ;");

		if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);

		wrap.setSql(sqlBuffer.toString());
		wrap.setList(list);
		wrap.setValueBuffer(valueBuffer);

		return wrap;
	}

	static <T> SqlValueWrap _toInsertSQL_for_ValueList(T entity, String excludeFieldList) throws IllegalAccessException {

		StringBuffer valueBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();

		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		for (int i = 0, k = 0; i < len; i++) {
			fields[i].setAccessible(true);

			if ("serialVersionUID".equals(fields[i].getName()))
				continue;
			else if (!"".equals(excludeFieldList) && isExcludeField(excludeFieldList, fields[i].getName())) continue;

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
		String sql = "";
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		boolean firstWhere = true;
		try {
			String tableName = ConverString.getTableName(entity);

			sqlBuffer.append("delete from ");
			sqlBuffer.append(tableName);

			Field fields[] = entity.getClass().getDeclaredFields();
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);

				if (HoneyUtil.isContinue(includeType, fields[i].get(entity),fields[i].getName())) {
					continue;
				} else {

					if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
						continue; //id=null不作为过滤条件

					if (firstWhere) {
						sqlBuffer.append(" where ");
						firstWhere = false;
					} else {
						sqlBuffer.append(" and ");
					}
					sqlBuffer.append(HoneyUtil.transformStr(fields[i].getName()));

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
			sqlBuffer.append(" ;");
			sql = sqlBuffer.toString();

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sql, valueBuffer.toString());
			addInContextForCache(sqlBuffer.toString(), valueBuffer.toString(), tableName);//2019-09-29
			//不允许删整张表
			//if(!notFirstWhere) {sql="delete * from "+tableName + "where id='still do not set id'"; throw new SQLException(); }

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;
	}

	private static boolean isContainField(String fields[], String fieldName) {
		int len = fields.length;
		for (int i = 0; i < len; i++) {
			if (fields[i].equalsIgnoreCase(fieldName)) {
				return true;
			}
		}
		return false;
	}

	//转成带下画线的
	private static String transformStr(String str) {
		return HoneyUtil.transformStr(str);
	}

	private static boolean isExcludeField(String excludeFieldList, String checkField) {
		String excludeFields[] = excludeFieldList.split(",");
		for (String f : excludeFields) {
			if (f.equals(checkField)) return true;
		}

		return false;
	}
	
    static void addInContextForCache(String sql,String sqlValue, String tableName){
		CacheSuidStruct struct=new CacheSuidStruct();
		struct.setSql(sql);
		struct.setSqlValue(sqlValue);
		struct.setTableNames(tableName);
		
		HoneyContext.setCacheInfo(sql, struct);  //同一线程内,sql是否可以标识区分
	}
}
