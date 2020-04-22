/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ObjSQLIllegalSQLStringException;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeErrorFieldException;
import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjectToSQLRich extends ObjectToSQL implements ObjToSQLRich {

	private DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbFeature();
	private static final String ASC = "asc";

	@Override
	public <T> String toSelectSQL(T entity, int size) {
		
//		String sql=dbFeature.toPageSql(toSelectSQL(entity), size);

		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
//		sql = dbFeature.toPageSql(sql, size)+";";
		sql = dbFeature.toPageSql(sql, size);

		setPreparedValue(sql, wrap);
		Logger.logSQL("select SQL(entity,size): ", sql);
		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity, int start, int size) {

		// String sql=dbFeature.toPageSql(toSelectSQL(entity), start, size);
		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
//		sql = dbFeature.toPageSql(sql, start, size)+";";
		sql = dbFeature.toPageSql(sql, start, size);

		setPreparedValue(sql, wrap);

		Logger.logSQL("select(entity,start,size) SQL:", sql);
		return sql;
	}
	
	@Override
	public <T> String toSelectSQL(T entity,String selectFields,int start,int size){
		
		SqlValueWrap wrap = toSelectSQL_0(entity,selectFields);
		String sql = wrap.getSql();
//		sql = dbFeature.toPageSql(sql, start, size)+";";
		sql = dbFeature.toPageSql(sql, start, size);

		setPreparedValue(sql, wrap);

		Logger.logSQL("select(entity,selectFields,start,size) SQL:", sql);
		return sql;
	}
	
	

	@Override
	public <T> String toSelectSQL(T entity, String fields) throws ObjSQLException {
		
		String newSelectFields=checkSelectField(entity,fields);
		
//		String sql = _ObjectToSQLHelper._toSelectSQL(entity);
		String sql = _ObjectToSQLHelper._toSelectSQL(entity, newSelectFields);

//		sql=sql.replace("#fieldNames#", fieldList);
//		sql=sql.replace("#fieldNames#", newSelectFields);  //TODO 打印值会有问题

		Logger.logSQL("select SQL(selectFields) :", sql);

		return sql;
	}

	@Override
	public <T> String toSelectOrderBySQL(T entity, String orderFieldList) throws ObjSQLException {

		String orderFields[] = orderFieldList.split(",");
		int lenA = orderFields.length;

		String orderBy = "";
		for (int i = 0; i < lenA; i++) {
			orderBy += orderFields[i] + " " + ASC;
			if (i < lenA - 1) orderBy += ",";
		}
		
//		String sql=toSelectSQL(entity);
		SqlValueWrap wrap=toSelectSQL_0(entity);
		String sql=wrap.getSql();
//		sql=sql.replace(";", " "); //close on 2019-04-27
		sql+="order by "+orderBy+" ;";
		
		setPreparedValue(sql,wrap);
		
		return sql;
	}

	@Override
	public <T> String toSelectOrderBySQL(T entity, String orderFieldList, OrderType[] orderTypes) throws ObjSQLException {
		
		String orderFields[] = orderFieldList.split(",");
		int lenA = orderFields.length;

		if (lenA != orderTypes.length) throw new ObjSQLException("ObjSQLException :The lenth of orderField is not equal orderTypes'.");

		String orderBy = "";
		for (int i = 0; i < lenA; i++) {
			orderBy += orderFields[i] + " " + orderTypes[i].getName();
			if (i < lenA - 1) orderBy += ",";
		}

		//		String sql=toSelectSQL(entity);
		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
//		sql = sql.replace(";", " "); //close on 2019-04-27
		sql += "order by " + orderBy + " ;";

		setPreparedValue(sql, wrap);

		return sql;
	}

	@Override
	public <T> String toUpdateSQL(T entity, String updateFieldList) {
		if (updateFieldList == null) return null;

		String sql = "";
//		try {
			String updateFields[] = updateFieldList.split(",");

			if (updateFields.length == 0 || "".equals(updateFieldList.trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

			sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, -1);
//		} catch (IllegalAccessException e) {
//			throw ExceptionHelper.convert(e);
//		}
		return sql;
	}

	@Override
	public <T> String toUpdateSQL(T entity, String updateFieldList, IncludeType includeType) {
		if (updateFieldList == null) return null;
		
		String sql = "";
//		try {
			String updateFields[] = updateFieldList.split(",");

			if (updateFields.length == 0 || "".equals(updateFieldList.trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

			sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, includeType.getValue());
//		} catch (IllegalAccessException e) {
//			throw ExceptionHelper.convert(e);
//		}
		return sql;
	}

	@Override
	public <T> String toSelectFunSQL(T entity, FunctionType functionType,String fieldForFun) throws ObjSQLException {
		return _toSelectFunSQL(entity,functionType.getName(),fieldForFun);
	}

	private <T> String _toSelectFunSQL(T entity, String funType,String fieldForFun) throws ObjSQLException {
		
		checkPackage(entity);
		
		if (fieldForFun == null || funType == null) return null;
		boolean isContainField = false;
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		String sql = null;
		try {
			String tableName =_toTableName(entity);
			String selectAndFun;
			if ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun))
				//		    selectAndFun = " select " + funType + "(" + fieldForFun + ") from ";  //  count(*)
				selectAndFun = "select count(*) from ";
			else
				selectAndFun = "select " + funType + "(" + _toColumnName(fieldForFun) + ") from ";

			sqlBuffer.append(selectAndFun);
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			Field fields[] = entity.getClass().getDeclaredFields(); // 改为以最高权限访问？2012-07-15
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
			  fields[i].setAccessible(true);
			  if (fields[i]!= null && fields[i].isAnnotationPresent(JoinTable.class)){//v1.7.0 排除多表的实体字段
				continue;
			  }
			  if (fields[i].get(entity) == null|| "serialVersionUID".equals(fields[i].getName())) {// 要排除没有设值的情况
//				if (fields[i].getName().equals(fieldForFun)) {
				if ( (fields[i].getName().equals(fieldForFun))
			     || ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun)) ) {  //排除count(*)
					isContainField = true;
				}
				continue;
				} else {
					if (fields[i].getName().equals(fieldForFun)) {
						isContainField = true;
					}

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
			addInContextForCache(sql, valueBuffer.toString(), tableName);
			

			if (SqlStrFilter.checkFunSql(sql, funType)) {
				throw new ObjSQLIllegalSQLStringException("ObjSQLIllegalSQLStringException:sql statement with function is illegal. " + sql);
			}
			Logger.logSQL("select fun SQL :", sql);
			if (!isContainField) throw new ObjSQLException("ObjSQLException:Miss The Field! The entity(" + tableName + ") don't contain the field:" + fieldForFun);

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity, IncludeType includeType) {
		return _ObjectToSQLHelper._toSelectSQL(entity, includeType.getValue());
	}

	@Override
	public <T> String toDeleteSQL(T entity, IncludeType includeType) {
		return _ObjectToSQLHelper._toDeleteSQL(entity, includeType.getValue());
	}

	@Override
	public <T> String toInsertSQL(T entity, IncludeType includeType) {
		String sql = null;
		try {
			sql = _ObjectToSQLHelper._toInsertSQL(entity, includeType.getValue());
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;

	}

	@Override
	public <T> String toUpdateSQL(T entity, IncludeType includeType) {
		String sql = "";
//		try {
//			sql = _ObjectToSQLHelper._toUpdateSQL(entity, "id", includeType.getValue());
			sql = _ObjectToSQLHelper._toUpdateSQL(entity, includeType.getValue());
//		} catch (IllegalAccessException e) {
//			throw ExceptionHelper.convert(e);
//		} catch (ObjSQLException e) {
//			throw e;
//		}
		return sql;

	}

	@Override
	public <T> String[] toInsertSQL(T entity[]) {
		return toInsertSQL(entity, "");
	}

	private static String index1 = "[index";
	private static String index2 = "]";

	@Override
	public <T> String[] toInsertSQL(T entity[], String excludeFieldList) {
		String sql[] = null;
		try {
			int len = entity.length;
			sql = new String[len];
			String t_sql = "";
			SqlValueWrap wrap;

			wrap = _ObjectToSQLHelper._toInsertSQL0(entity[0], 2, excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
			t_sql = wrap.getSql();
			sql[0] = t_sql;
			t_sql = t_sql + "[index0]";
			setPreparedValue(t_sql, wrap);
			Logger.logSQL("insert[] SQL :", t_sql);

			for (int i = 1; i < len; i++) { // i=1
				wrap = _ObjectToSQLHelper._toInsertSQL_for_ValueList(entity[i], excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
				//				t_sql = wrap.getSql(); //  每个sql不一定一样,因为设值不一样,有些字段不用转换. 不采用;因为不利于批处理

				setPreparedValue_ForArray(sql[0] + index1 + i + index2, wrap);
				Logger.logSQL("insert[] SQL :", sql[0] + index1 + i + index2);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	
	@Override
	public String toDeleteByIdSQL(Class c, Integer id) {
		if(id==null) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Integer");
	}
	
	@Override
	public String toDeleteByIdSQL(Class c, Long id) {
		if(id==null) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Long");
	}

	@Override
	public String toDeleteByIdSQL(Class c, String ids) {
		if(ids==null || "".equals(ids.trim())) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer,ids);
	}

	private  SqlValueWrap toDeleteByIdSQL0(Class c){
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		
		String tableName =_toTableNameByClass(c);
		
		sqlBuffer.append("delete from ")
		.append(tableName)
		.append(" where ")
		;
		
		wrap.setValueBuffer(sqlBuffer); //sqlBuffer
		wrap.setTableNames(tableName);
		
		return wrap;
	}
	
	@Override
	public <T> String toSelectByIdSQL(T entity, Integer id) {
		
		SqlValueWrap sqlBuffer = toSelectByIdSQL0(entity);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Integer");
	}

	@Override
	public <T> String toSelectByIdSQL(T entity, Long id) {
		SqlValueWrap sqlBuffer = toSelectByIdSQL0(entity);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Long");
	}

	@Override
	public <T> String toSelectByIdSQL(T entity, String ids) {
		if(ids==null || "".equals(ids.trim())) return null;
		SqlValueWrap sqlBuffer=toSelectByIdSQL0(entity);
		return _toSelectAndDeleteByIdSQL(sqlBuffer,ids);
	}

	@Override
	public <T> String toSelectSQL(T entity, IncludeType includeType, Condition condition) {
		return _ObjectToSQLHelper._toSelectSQL(entity, includeType.getValue(),condition); 
	}
	
	private <T> String _toUpdateBySQL(T entity, String whereFieldList,int includeType) {
		if (whereFieldList == null) return null;

		String sql = "";
//		try {
			String whereFields[] = whereFieldList.split(",");

			if (whereFields.length == 0 || "".equals(whereFieldList.trim())) 
				throw new ObjSQLException("ObjSQLException:whereFieldList at least include one field.");

//			sql = _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, -1);
			sql = _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, includeType);
//		} catch (IllegalAccessException e) {
//			throw ExceptionHelper.convert(e);
//		}
		return sql;
	}
	
	@Override
	public <T> String toUpdateBySQL(T entity, String whereFieldList) {
	    return _toUpdateBySQL(entity, whereFieldList, -1);
	}

	@Override
	public <T> String toUpdateBySQL(T entity, String whereFieldList, IncludeType includeType) {
		return _toUpdateBySQL(entity, whereFieldList, includeType.getValue());
	}

	@Override
	public <T> String toUpdateBySQL(T entity, String whereFieldList, Condition condition) {

		String whereFields[] = whereFieldList.split(",");
		if (whereFields.length == 0 || "".equals(whereFieldList.trim()))
			throw new ObjSQLException("ObjSQLException:whereFieldList at least include one field.");

		if (condition == null || condition.getIncludeType() == null) {
			return _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, -1, condition); //includeType=-1
		} else {
			return _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, condition.getIncludeType().getValue(), condition);
		}
	}

	@Override
	public <T> String toUpdateSQL(T entity, String updateFieldList, Condition condition) {

		String updateFields[] = updateFieldList.split(","); //setColmns
		if (updateFields.length == 0 || "".equals(updateFieldList.trim()))
			throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

		if (condition == null || condition.getIncludeType() == null) {
			return _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, -1, condition);//includeType=-1
		} else {
			return _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, condition.getIncludeType().getValue(), condition);
		}
	}

	private <T> String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, Number id,String numType) {
		if(id==null) return null;
		
		StringBuffer sqlBuffer=wrap.getValueBuffer();  //sqlBuffer
		
//		StringBuffer sqlBuffer=toSelectByIdSQL0(entity);
//		sqlBuffer.append("id=").append("?").append(";");
		sqlBuffer.append("id=").append("?");

		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		preparedValue = new PreparedValue();
		preparedValue.setType(numType);
		preparedValue.setValue(id);
		list.add(preparedValue);
		
		HoneyContext.setPreparedValue(sqlBuffer.toString(), list);
		HoneyContext.setSqlValue(sqlBuffer.toString(), id+""); //用于log显示
		addInContextForCache(sqlBuffer.toString(), id+"", wrap.getTableNames());
		
		return sqlBuffer.toString();
	}
	
	private <T> String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, String ids) {
		
		StringBuffer sqlBuffer =wrap.getValueBuffer(); //sqlBuffer
		
		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		
		String idArray[]=ids.split(",");
		String t_ids="id=?";
		
		preparedValue = new PreparedValue();
//		preparedValue.setType(numType);//id的类型Object
		preparedValue.setValue(idArray[0]);
		list.add(preparedValue);
		
		for (int i = 1; i < idArray.length; i++) { //i from 1
			preparedValue = new PreparedValue();
			t_ids+=" or id=?";
//			preparedValue.setType(numType);//id的类型Object
			preparedValue.setValue(idArray[i]);
			list.add(preparedValue);
		}
		
//		sqlBuffer.append(t_ids).append(";");
		sqlBuffer.append(t_ids);
		
		HoneyContext.setPreparedValue(sqlBuffer.toString(), list);
		HoneyContext.setSqlValue(sqlBuffer.toString(), ids); //用于log显示
		addInContextForCache(sqlBuffer.toString(), ids, wrap.getTableNames());
		
		return sqlBuffer.toString();
	}
	
	private  <T> SqlValueWrap toSelectByIdSQL0(T entity){
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		
//		StringBuffer valueBuffer = new StringBuffer();
//		try {
			String tableName =_toTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields();

			String packageAndClassName = entity.getClass().getName();
			String columnNames = HoneyContext.getBeanField(packageAndClassName);
			if (columnNames == null) {
				columnNames = HoneyUtil.getBeanField(fields);
				HoneyContext.addBeanField(packageAndClassName, columnNames);
			}

			sqlBuffer.append("select " + columnNames + " from ");
			sqlBuffer.append(tableName)
			.append(" where ");
			
			wrap.setValueBuffer(sqlBuffer); //sqlBuffer
			wrap.setTableNames(tableName);
			
		return wrap;
	}

	private <T> SqlValueWrap toSelectSQL_0(T entity) {
		return toSelectSQL_0(entity,null);
	}
	private <T> SqlValueWrap toSelectSQL_0(T entity,String selectField) {

		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer valueBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		try {
			String tableName =_toTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields(); //返回所有字段,包括公有和私有    
			String fieldNames ="";
			if (selectField != null && !"".equals(selectField.trim())) {
				fieldNames = checkSelectField(entity, selectField);
			} else {
				String packageAndClassName = entity.getClass().getName();
				fieldNames = HoneyContext.getBeanField(packageAndClassName);
				if (fieldNames == null) {
					fieldNames = HoneyUtil.getBeanField(fields);
					HoneyContext.addBeanField(packageAndClassName, fieldNames);
				}
			}
			sqlBuffer.append("select " + fieldNames + " from ");
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

//			sqlBuffer.append(";");   //close on 2019-04-27

			if (valueBuffer.length() > 0) valueBuffer.deleteCharAt(0);

			wrap.setTableNames(tableName);//2019-09-29
			wrap.setSql(sqlBuffer.toString());
			wrap.setList(list);
			wrap.setValueBuffer(valueBuffer);

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return wrap;
	}

	private void setPreparedValue(String sql, SqlValueWrap wrap) {
		HoneyContext.setPreparedValue(sql, wrap.getList());
		HoneyContext.setSqlValue(sql, wrap.getValueBuffer().toString());
		addInContextForCache(sql, wrap.getValueBuffer().toString(), wrap.getTableNames());
	}
	
	private void setPreparedValue_ForArray(String sql, SqlValueWrap wrap) {
		HoneyContext.setPreparedValue(sql, wrap.getList());
		HoneyContext.setSqlValue(sql, wrap.getValueBuffer().toString());
//		addInContextForCache(sql, wrap.getValueBuffer().toString(), wrap.getTableNames());
	}
	
	private <T> String checkSelectField(T entity,String fieldList){
		Field fields[] = entity.getClass().getDeclaredFields();
		String packageAndClassName = entity.getClass().getName();
		String columnsdNames = HoneyContext.getBeanField(packageAndClassName);
		if (columnsdNames == null) {
			columnsdNames = HoneyUtil.getBeanField(fields);//获取属性名对应的DB字段名
			HoneyContext.addBeanField(packageAndClassName, columnsdNames);
		}

		String errorField = "";
		boolean isFirstError = true;
		String selectFields[] = fieldList.split(",");
		String newSelectFields = "";
		boolean isFisrt = true;

		for (String s : selectFields) {

			if (!columnsdNames.contains(_toColumnName(s))) {
				if (isFirstError) {
					errorField += s;
					isFirstError = false;
				} else {
					errorField += "," + s;
				}
			}
			if (isFisrt) {
				newSelectFields += _toColumnName(s);
				isFisrt = false;
			} else {
				newSelectFields += ", " + _toColumnName(s);
			}

		}//end for

		if (!"".equals(errorField)) throw new BeeErrorFieldException("ErrorField: " + errorField);
		
		return newSelectFields;
	}
	
   private static void addInContextForCache(String sql,String sqlValue, String tableName){
	   _ObjectToSQLHelper.addInContextForCache(sql, sqlValue, tableName);
	}
   
	private static <T> void checkPackage(T entity) {
//		传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但spring开头不能过滤,否则spring想用bee就不行了.
		HoneyUtil.checkPackage(entity);
	}
	
	public static void checkPackageByClass(Class c){
		if(c==null) return;
		String packageName=c.getPackage().getName();
		if(packageName.startsWith("java.") || packageName.startsWith("javax.")){
			throw new BeeIllegalEntityException("BeeIllegalEntityException: Illegal Entity, "+c.getName());
		}
	}
	
	private String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	private String _toTableNameByClass(Class c){
		return NameTranslateHandle.toTableName(c.getName());
	}
	
	private static String _toColumnName(String fieldName){
		return NameTranslateHandle.toColumnName(fieldName);
	}
}
