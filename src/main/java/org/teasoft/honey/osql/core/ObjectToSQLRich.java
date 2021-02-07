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
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * @author Kingstar
 * @since  1.0
 */
public class ObjectToSQLRich extends ObjectToSQL implements ObjToSQLRich {

	private DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbFeature();
	private static final String ASC = K.asc;
	
//	private static boolean  showSQL=HoneyConfig.getHoneyConfig().isShowSQL();
	private int batchSize = HoneyConfig.getHoneyConfig().getBatchSize();

	@Override
	public <T> String toSelectSQL(T entity, int size) {

		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
		regPagePlaceholder();
		sql = dbFeature.toPageSql(sql, size);
		HoneyUtil.setPageNum(wrap.getList());
         
		setContext(sql, wrap.getList(), wrap.getTableNames());
		Logger.logSQL("select SQL(entity,size): ", sql);
		return sql;
	}
	
	private void regPagePlaceholder(){
		HoneyUtil.regPagePlaceholder();
	}

	@Override
	public <T> String toSelectSQL(T entity, int start, int size) {

		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
		regPagePlaceholder();
		sql = dbFeature.toPageSql(sql, start, size);
		HoneyUtil.setPageNum(wrap.getList());
		setContext(sql, wrap.getList(), wrap.getTableNames());

		Logger.logSQL("select(entity,start,size) SQL: ", sql);
		return sql;
	}

	@Override
	public <T> String toSelectSQL(T entity, String selectFields, int start, int size) {

		SqlValueWrap wrap = toSelectSQL_0(entity, selectFields);
		String sql = wrap.getSql();
		regPagePlaceholder();
		sql = dbFeature.toPageSql(sql, start, size);
		HoneyUtil.setPageNum(wrap.getList());
		setContext(sql, wrap.getList(), wrap.getTableNames());

		Logger.logSQL("select(entity,selectFields,start,size) SQL: ", sql);
		return sql;
	}
	
	@Override
	public <T> String toSelectSQL(T entity, String fields) throws ObjSQLException {
		
		String newSelectFields=HoneyUtil.checkAndProcessSelectField(entity,fields);
		
		String sql = _ObjectToSQLHelper._toSelectSQL(entity, newSelectFields);

//		sql=sql.replace("#fieldNames#", fieldList);
//		sql=sql.replace("#fieldNames#", newSelectFields);  //TODO 打印值会有问题

		Logger.logSQL("select SQL(selectFields) : ", sql);

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
		
		SqlValueWrap wrap=toSelectSQL_0(entity);
		String sql=wrap.getSql();
//		sql=sql.replace(";", " "); //close on 2019-04-27
//		sql+="order by "+orderBy+" ;";
		sql+=K.orderBy+" "+orderBy;
		setContext(sql, wrap.getList(), wrap.getTableNames());
		
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

		SqlValueWrap wrap = toSelectSQL_0(entity);
		String sql = wrap.getSql();
//		sql = sql.replace(";", " "); //close on 2019-04-27
//		sql += "order by " + orderBy + " ;";
		sql += K.orderBy+" " + orderBy;
		setContext(sql, wrap.getList(), wrap.getTableNames());

		return sql;
	}

	@Override
	public <T> String toUpdateSQL(T entity, String updateFieldList) {
		if (updateFieldList == null) return null;

		String sql = "";
		String updateFields[] = updateFieldList.split(",");

		if (updateFields.length == 0 || "".equals(updateFieldList.trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

		sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, -1);
		return sql;
	}

	@Override
	public <T> String toUpdateSQL(T entity, String updateFieldList, IncludeType includeType) {
		if (updateFieldList == null) return null;

		String sql = "";
		String updateFields[] = updateFieldList.split(",");

		if (updateFields.length == 0 || "".equals(updateFieldList.trim())) throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

		sql = _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, includeType.getValue());
		return sql;
	}

	@Override
	public <T> String toSelectFunSQL(T entity, FunctionType functionType,String fieldForFun,Condition condition){
		return _toSelectFunSQL(entity,functionType.getName(),fieldForFun,condition);
	}

	private <T> String _toSelectFunSQL(T entity, String funType,String fieldForFun,Condition condition){
		
		checkPackage(entity);
		
		if (fieldForFun == null || funType == null) return null;
		boolean isContainField = false;
		StringBuffer sqlBuffer = new StringBuffer();
		String sql = null;
		try {
			String tableName =_toTableName(entity);
			String selectAndFun;
			if ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun)) {
//		        selectAndFun = " select " + funType + "(" + fieldForFun + ") from ";  //  count(*)
//				selectAndFun = "select count(*) from ";
				selectAndFun = K.select+" "+K.count+"(*) "+K.from+" ";
			}else {
//				selectAndFun = "select " + funType + "(" + _toColumnName(fieldForFun) + ") from ";
				selectAndFun = K.select+" " + funType + "(" + _toColumnName(fieldForFun) + ") "+K.from+" ";   //TODO funType要能转大小写风格
			}
			sqlBuffer.append(selectAndFun);
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			Field fields[] = entity.getClass().getDeclaredFields();
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
			  fields[i].setAccessible(true);
//			  if (fields[i]!= null && fields[i].isAnnotationPresent(JoinTable.class)){//v1.7.0 排除多表的实体字段
//				continue;
//			  }
			  //bug , default can not filter the empty string.
//			  if (fields[i].get(entity) == null|| "serialVersionUID".equals(fields[i].getName()) || fields[i].isSynthetic()) {// 要排除没有设值的情况
////				if (fields[i].getName().equals(fieldForFun)) {
//				if ( (fields[i].getName().equals(fieldForFun))
//			     || ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun)) ) {  //排除count(*)
//					isContainField = true;
//				}
//				continue;
			  
				if ((fields[i].getName().equals(fieldForFun))
						|| ("count".equalsIgnoreCase(funType) && "*".equals(fieldForFun))) { //排除count(*)
					isContainField = true;
				}
					
				if (HoneyUtil.isContinue(-1, fields[i].get(entity),fields[i])) {
						continue;
				} else {
					if (fields[i].getName().equals(fieldForFun)) {
						isContainField = true;
					}

					if (firstWhere) {
//						sqlBuffer.append(" where ");
						sqlBuffer.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
//						sqlBuffer.append(" and ");
						sqlBuffer.append(" ").append(K.and).append(" ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName()));

					sqlBuffer.append("=");
					sqlBuffer.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(k++, preparedValue);
				}
			}

			if (condition != null) {
				condition.setSuidType(SuidType.SELECT);
				ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere);
			}
			
			sql = sqlBuffer.toString();
			
			setContext(sql, list, tableName);

			if (SqlStrFilter.checkFunSql(sql, funType)) {
				throw new ObjSQLIllegalSQLStringException("ObjSQLIllegalSQLStringException:sql statement with function is illegal. " + sql);
			}
			Logger.logSQL("select fun SQL : ", sql);
			if (!isContainField) throw new ObjSQLException("ObjSQLException:Miss The Field! The entity(" + tableName + ") don't contain the field:" + fieldForFun);

		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	
	//v1.9
	public <T> String toSelectFunSQL(T entity, Condition condition) {
		if (condition == null || condition.getIncludeType() == null)
			return _ObjectToSQLHelper._toSelectSQL(entity, -1, condition, true); // 过滤NULL和空字符串
		else
			return _ObjectToSQLHelper._toSelectSQL(entity, condition.getIncludeType().getValue(), condition, true);
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
			_ObjectToSQLHelper.setInitIdByAuto(entity);
			sql = _ObjectToSQLHelper._toInsertSQL0(entity, includeType.getValue(),"");
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}
		return sql;

	}

	@Override
	public <T> String toUpdateSQL(T entity, IncludeType includeType) {
		String sql = "";
		sql = _ObjectToSQLHelper._toUpdateSQL(entity, includeType.getValue());
		return sql;
	}

	@Override
	public <T> String[] toInsertSQL(T entity[]) {
		return toInsertSQL(entity, "");
	}
	
	@Override
	public <T> String[] toInsertSQL(T entity[], int batchSize) {
		return toInsertSQL(entity, batchSize, "");
	}

	private static final String index1 = "_SYS[index";
	private static final String index2 = "]_End ";
	private static final String index3 = "]";
	
	@Override
	public <T> String[] toInsertSQL(T entity[], String excludeFieldList) {
		return toInsertSQL(entity, batchSize, excludeFieldList);
	}
	
	@Override
	public <T> String[] toInsertSQL(T entity[],int batchSize, String excludeFieldList) {
		
		if(HoneyUtil.isMysql()) return toInsertSQLForMysql(entity, batchSize, excludeFieldList);
		
		String sql[] = null;  
		try {
			int len = entity.length;
			
			setInitArrayIdByAuto(entity);
			
			sql = new String[len];  //只用sql[0]
			String t_sql = "";

			t_sql = _ObjectToSQLHelper._toInsertSQL0(entity[0], 2, excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
			sql[0] = t_sql;
//			t_sql = t_sql + "[index0]";  //index0 不带,与单条共用.

			for (int i = 1; i < len; i++) { // i=1
				String sql_i=index1 + i + index2+sql[0];
				_ObjectToSQLHelper._toInsertSQL_for_ValueList(sql_i,entity[i], excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
//				t_sql = wrap.getSql(); //  每个sql不一定一样,因为设值不一样,有些字段不用转换. 不采用;因为不利于批处理
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}

	private <T> String[] toInsertSQLForMysql(T entity[],int batchSize, String excludeFieldList) {
		String sql[] = null;  
		try {
			int len = entity.length;
			
			setInitArrayIdByAuto(entity);
			
			sql = new String[len];  //只用sql[0]
			
			String t_sql = "";
 
			OneTimeParameter.setAttribute("_SYS_Bee_Return_PlaceholderValue", "tRue");
			t_sql = _ObjectToSQLHelper._toInsertSQL0(entity[0], 2, excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
			sql[0] = t_sql;
			
			List<PreparedValue> preparedValueList = new ArrayList<>();
			
//			if (showSQL) {
//				preparedValueList.addAll(HoneyContext._justGetPreparedValue(sql[0]));  //打印后要手动清除
//			} else {
//				preparedValueList.addAll(HoneyContext.getPreparedValue(sql[0])); //会删了,打印日志时不能用.  批处理,在v1.8开始,不会用于占位设值.
//			}
			preparedValueList.addAll(HoneyContext._justGetPreparedValue(sql[0]));  //统一使用这个.
			
			if(len==1) HoneyContext.setPreparedValue(t_sql+ index1 +"Batch:"+ 0 + index2, preparedValueList);
			List<PreparedValue> oneRecoreList;
			for (int i = 1; i < len; i++) { // i=1
				String sql_i=index1 + i + index2+sql[0];
				//不需要打印时,不会放上下文
				oneRecoreList=_ObjectToSQLHelper._toInsertSQL_for_ValueList(sql_i,entity[i], excludeFieldList); // i 默认包含null和空字符串.因为要用统一的sql作批处理
				//t_sql = wrap.getSql(); //  每个sql不一定一样,因为设值不一样,有些字段不用转换. 不采用;因为不利于批处理

				preparedValueList.addAll(oneRecoreList);
				
				if((i+1)%batchSize==0){ //i+1
					HoneyContext.setPreparedValue(t_sql +"  [Batch:"+ (i/batchSize) + index3, preparedValueList); //i
					preparedValueList = new ArrayList<>();
				}else if(i==(len-1)){
					HoneyContext.setPreparedValue(t_sql +"  [Batch:"+ (i/batchSize) + index3, preparedValueList); //i
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public String toDeleteByIdSQL(Class c, Integer id) {
		if(id==null) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Integer");
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public String toDeleteByIdSQL(Class c, Long id) {
		if(id==null) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer, id, "java.lang.Long");
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String toDeleteByIdSQL(Class c, String ids) {
		if(ids==null || "".equals(ids.trim())) return null;
		checkPackageByClass(c);
		SqlValueWrap sqlBuffer=toDeleteByIdSQL0(c);
		return _toSelectAndDeleteByIdSQL(sqlBuffer,ids);
	}

	@SuppressWarnings("rawtypes")
	private  SqlValueWrap toDeleteByIdSQL0(Class c){
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		
		String tableName =_toTableNameByClass(c);
		
//		sqlBuffer.append("delete from ")
		sqlBuffer.append(K.delete).append(" ").append(K.from).append(" ")
		.append(tableName)
//		.append(" where ")
		.append(" ").append(K.where).append(" ");
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
		return _toSelectAndDeleteByIdSQL(sqlBuffer,ids,getIdType(entity));
	}
	
	private <T> String getIdType(T entity) {
		Field field = null;
		String type=null;
		try {
			field = entity.getClass().getDeclaredField("id");
			type=field.getType().getSimpleName();
		} catch (Exception e) {
			//ignore
		}
		
		return type;
	}

	@Override
	public <T> String toSelectSQL(T entity, IncludeType includeType, Condition condition) {
		if (includeType == null)
			return _ObjectToSQLHelper._toSelectSQL(entity, -1, condition);
		else
			return _ObjectToSQLHelper._toSelectSQL(entity, includeType.getValue(), condition);
	}
	
	private <T> String _toUpdateBySQL(T entity, String whereFieldList, int includeType) {
		if (whereFieldList == null) return null;

		String sql = "";
		String whereFields[] = whereFieldList.split(",");

		if (whereFields.length == 0 || "".equals(whereFieldList.trim())) throw new ObjSQLException("ObjSQLException:whereFieldList at least include one field.");

		sql = _ObjectToSQLHelper._toUpdateBySQL(entity, whereFields, includeType);
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
		
		if(updateFieldList==null) updateFieldList="";
		String updateFields[] = updateFieldList.split(","); //setColmns
//		if (updateFields.length == 0 || "".equals(updateFieldList.trim()))  //close in v1.8    because: set can define in condition
//			throw new ObjSQLException("ObjSQLException:updateFieldList at least include one field.");

		if (condition == null || condition.getIncludeType() == null) {
			return _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, -1, condition);//includeType=-1
		} else {
			return _ObjectToSQLHelper._toUpdateSQL(entity, updateFields, condition.getIncludeType().getValue(), condition);
		}
	}

	private <T> String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, Number id,String numType) {
		if(id==null) return null;
		
		StringBuffer sqlBuffer=wrap.getValueBuffer();  //sqlBuffer
		sqlBuffer.append(_id()+"=").append("?");

		List<PreparedValue> list = new ArrayList<>();
		PreparedValue preparedValue = null;
		preparedValue = new PreparedValue();
		preparedValue.setType(numType);
		preparedValue.setValue(id);
		list.add(preparedValue);
		
		setContext(sqlBuffer.toString(), list, wrap.getTableNames());
		
		return sqlBuffer.toString();
	}
	
	private <T> String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, String ids) {
		return _toSelectAndDeleteByIdSQL(wrap, ids,null);
	}
	
	private <T> String _toSelectAndDeleteByIdSQL(SqlValueWrap wrap, String ids, String idType) {

		StringBuffer sqlBuffer=wrap.getValueBuffer(); //sqlBuffer
		List<PreparedValue> list=new ArrayList<>();
		PreparedValue preparedValue=null;

		String idArray[]=ids.split(",");
		//		String t_ids="id=?";
		String t_ids=_id() + "=?";

		preparedValue=new PreparedValue();
		//		preparedValue.setType(numType);//id的类型Object
		if (idType != null) {
			preparedValue.setType(idType);
			if ("Long".equals(idType) || "long".equals(idType)) {
				preparedValue.setValue(Long.parseLong(idArray[0]));
			} else if ("Integer".equals(idType) || "int".equals(idType)) {
				preparedValue.setValue(Integer.parseInt(idArray[0]));
			} else {
				preparedValue.setValue(idArray[0]);
			}
		} else {
			preparedValue.setValue(idArray[0]);
		}

		list.add(preparedValue);

		for (int i=1; i < idArray.length; i++) { //i from 1
			preparedValue=new PreparedValue();
			//			t_ids+=" or id=?";
			t_ids+=" " + K.or + " " + _id() + "=?";
			//			preparedValue.setType(numType);//id的类型Object
			if (idType != null) {
				preparedValue.setType(idType);

				if ("Long".equals(idType) || "long".equals(idType)) {
					preparedValue.setValue(Long.parseLong(idArray[i]));
				} else if ("Integer".equals(idType) || "int".equals(idType)) {
					preparedValue.setValue(Integer.parseInt(idArray[i]));
				} else {
					preparedValue.setValue(idArray[i]);
				}
			} else {
				preparedValue.setValue(idArray[i]);
			}

			list.add(preparedValue);
		}

		sqlBuffer.append(t_ids);
		setContext(sqlBuffer.toString(), list, wrap.getTableNames());

		return sqlBuffer.toString();
	}
	
	private <T> SqlValueWrap toSelectByIdSQL0(T entity) {
		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();

		String tableName = _toTableName(entity);
		Field fields[] = entity.getClass().getDeclaredFields();

		String packageAndClassName = entity.getClass().getName();
		String columnNames = HoneyContext.getBeanField(packageAndClassName);
		if (columnNames == null) {
			columnNames = HoneyUtil.getBeanField(fields);
			HoneyContext.addBeanField(packageAndClassName, columnNames);
		}

//		sqlBuffer.append(K.select+" " + columnNames + " "+K.from+" ");
		sqlBuffer.append(K.select).append(" ").append(columnNames).append(" ").append(K.from).append(" ");
		sqlBuffer.append(tableName).append(" ").append(K.where).append(" ");

		wrap.setValueBuffer(sqlBuffer); //sqlBuffer
		wrap.setTableNames(tableName);

		return wrap;
	}

	private <T> SqlValueWrap toSelectSQL_0(T entity) {
		return toSelectSQL_0(entity,null);
	}
	
	private <T> SqlValueWrap toSelectSQL_0(T entity,String selectField) {

		StringBuffer sqlBuffer = new StringBuffer();
		SqlValueWrap wrap = new SqlValueWrap();
		try {
			String tableName =_toTableName(entity);
			Field fields[] = entity.getClass().getDeclaredFields(); //返回所有字段,包括公有和私有    
			String fieldNames ="";
			if (selectField != null && !"".equals(selectField.trim())) {
				fieldNames = HoneyUtil.checkAndProcessSelectField(entity, selectField);
			} else {
				String packageAndClassName = entity.getClass().getName();
				fieldNames = HoneyContext.getBeanField(packageAndClassName);
				if (fieldNames == null) {
					fieldNames = HoneyUtil.getBeanField(fields);
					HoneyContext.addBeanField(packageAndClassName, fieldNames);
				}
			}
//			sqlBuffer.append("select " + fieldNames + " from ");
			sqlBuffer.append(K.select).append(" ").append(fieldNames).append(" ").append(K.from).append(" ");
			sqlBuffer.append(tableName);
			boolean firstWhere = true;
			int len = fields.length;
			List<PreparedValue> list = new ArrayList<>();
			PreparedValue preparedValue = null;
			for (int i = 0, k = 0; i < len; i++) {
				fields[i].setAccessible(true);
				if (HoneyUtil.isContinue(-1, fields[i].get(entity),fields[i])) {
					continue;	
				}else {
					if (firstWhere) {
//						sqlBuffer.append(" where ");
						sqlBuffer.append(" ").append(K.where).append(" ");
						firstWhere = false;
					} else {
//						sqlBuffer.append(" and ");
						sqlBuffer.append(" ").append(K.and).append(" ");
					}
					sqlBuffer.append(_toColumnName(fields[i].getName()));
					
					sqlBuffer.append("=");
					sqlBuffer.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					list.add(k++, preparedValue);
				}
			}

			wrap.setTableNames(tableName);
			wrap.setSql(sqlBuffer.toString());
			wrap.setList(list);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return wrap;
	}

	
	private static void setContext(String sql,List<PreparedValue> list,String tableName){
		HoneyContext.setContext(sql, list, tableName);
	}
	
	private static <T> void checkPackage(T entity) {
//		传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但spring开头不能过滤,否则spring想用bee就不行了.
		HoneyUtil.checkPackage(entity);
	}
	
	@SuppressWarnings("rawtypes")
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
	
	@SuppressWarnings("rawtypes")
	private String _toTableNameByClass(Class c){
		return NameTranslateHandle.toTableName(c.getName());
	}
	
	private static String _toColumnName(String fieldName){
		return NameTranslateHandle.toColumnName(fieldName);
	}
	
	private static String _id(){
		return NameTranslateHandle.toColumnName("id");
	}
	
	private <T> void setInitArrayIdByAuto(T entity[]) {
		
//		boolean needGenId=HoneyConfig.getHoneyConfig().genid_forAllTableLongId;
//		if(!needGenId) return ;
		
		if(entity==null || entity.length<1) return ;
		boolean needGenId = HoneyContext.isNeedGenId(entity[0].getClass());
		if (!needGenId) return;

		Field field = null;
		try {
			
			field = entity[0].getClass().getDeclaredField("id");
//			field.setAccessible(true);
//			if (field.get(entity[0]) != null) return; //即使没值,运行一次后也会有值,下次再用就会重复.而用户又不知道.    //TODO 要提醒是被覆盖了。
		} catch (NoSuchFieldException e) {
			//is no id field , ignore.
			return;
		} catch (Exception e) {
//			e.printStackTrace();
			Logger.error(e.getMessage());
			return;
		}

		if (!field.getType().equals(Long.class)) return; //just set the null Long id field

		int len = entity.length;
		String tableKey = _toTableName(entity[0]);
		long ids[] = GenIdFactory.getRangeId(tableKey, len);
		long id = ids[0];
		for (int i = 0; i < len; id++, i++) {
			field.setAccessible(true);
			try {
				field.set(entity[i], id);
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			}
		}
	}
}
