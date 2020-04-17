/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Cache;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * 直接操作数据库，并返回结果.在该类中的sql字符串要是DB能识别的SQL语句
 * @author Kingstar
 * Create on 2013-6-30 下午10:32:53
 * @since  1.0
 */
public class SqlLib implements BeeSql {
	
	private Cache cache=BeeFactory.getHoneyFactory().getCache();

	public SqlLib() {}

	private Connection getConn() throws SQLException {
		Connection conn = null;

		conn = HoneyContext.getCurrentConnection(); //获取已开启事务的连接
		if (conn == null) {
//			try {
				conn = SessionFactory.getConnection(); //不开启事务时
//			} catch (Exception e) {
//				Logger.print("Have Error when get the Connection: ", e.getMessage());
//			}
		}

		return conn;
	}

	//要是写的sql对应的结构与entity的结构不一致,将会有问题
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> select(String sql, T entity) {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		//要是没有更新缓存,证明之前还没有登记过缓存,就不能去查缓存.
		boolean isReg=updateInfoInCache(sql,"List<T>",SuidType.SELECT);
		if(isReg){
		Object cacheObj=cache.get(sql);  //这里的sql还没带有值
		if(cacheObj!=null) return (List<T>)cacheObj;
		}
		
		Connection conn = null;
		PreparedStatement pst = null;
		T targetObj = null;
		List<T> rsList = null;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);
			
			setPreparedValues(pst, sql);

			ResultSet rs = pst.executeQuery();
			rsList = new ArrayList<T>();

			Field field[] = entity.getClass().getDeclaredFields();
			int columnCount = field.length;

			while (rs.next()) {

				targetObj = (T) entity.getClass().newInstance();
				for (int i = 0; i < columnCount; i++) {
					if("serialVersionUID".equals(field[i].getName())) continue;
					if (field[i]!= null && field[i].isAnnotationPresent(JoinTable.class)) continue;
					field[i].setAccessible(true);
					try {
						field[i].set(targetObj, rs.getObject(_toColumnName(field[i].getName())));
					} catch (IllegalArgumentException e) {
						field[i].set(targetObj,_getObject(rs,field[i]));
					}
					
				}
				rsList.add(targetObj);
			}
			
			addInCache(sql, rsList,"List<T>",SuidType.SELECT,rsList.size());
			
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}

		entity = null;
		targetObj = null;

		return rsList;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> selectSomeField(String sql, T entity) {
		
		if(sql==null || "".equals(sql.trim())) return null;

		boolean isReg=updateInfoInCache(sql,"List<T>",SuidType.SELECT);
		if(isReg){
		Object cacheObj=cache.get(sql);  //这里的sql还没带有值
		if(cacheObj!=null) return (List<T>)cacheObj;
		}
		
		Connection conn = null;
		PreparedStatement pst = null;
		T targetObj = null;
		List<T> rsList = null;
		Map<String,Field> map=null;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			ResultSet rs = pst.executeQuery();
			ResultSetMetaData rmeta = rs.getMetaData();
			int columnCount = rmeta.getColumnCount();
			rsList = new ArrayList<T>();

//			Field field[] = entity.getClass().getDeclaredFields();
			map=new Hashtable<>();
			
			Field field=null;
			String name=null;
			boolean isFirst=true;
			while (rs.next()) {

				targetObj = (T) entity.getClass().newInstance();
				for (int i = 0; i < columnCount; i++) {
//					if("serialVersionUID".equals(field[i].getName())) continue;
					try {
						name=_toFieldName(rmeta.getColumnName(i + 1));
						if(isFirst){
						field = entity.getClass().getDeclaredField(name);//可能会找不到Javabean的字段
						map.put(name, field);
						}else{
							field=map.get(name);
							if(field==null) continue;
						}
					} catch (NoSuchFieldException e) {
						continue;
					}
					field.setAccessible(true);
					try {
						field.set(targetObj, rs.getObject(i + 1)); //对相应Field设置
					} catch (IllegalArgumentException e) {
						field.set(targetObj, _getObjectByindex(rs,field,i+1));
					}
					

				}
				rsList.add(targetObj);
				isFirst=false;
			}

			addInCache(sql, rsList,"List<T>",SuidType.SELECT,rsList.size());
			
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}

		entity = null;
		targetObj = null;
		map=null;

		return rsList;
	}

	/**
	 * SQL function: max,min,avg,sum,count. 如果统计的结果集为空,除了count返回0,其它都返回空字符.
	 */
	@Override
	public String selectFun(String sql) throws ObjSQLException {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		boolean isReg=updateInfoInCache(sql,"String",SuidType.SELECT);
		if(isReg){
		Object cacheObj=cache.get(sql);  //这里的sql还没带有值
		if(cacheObj!=null) return (String)cacheObj;
		}
		
		String result = null;
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
			if (rs.next()) { //if
//				result= rs.getString(1); 
				if (rs.getObject(1) == null)
					result = "";
				else
					result = rs.getObject(1).toString();
			}

			rs.last();
			if (rs.getRow() > 1) {
				throw new ObjSQLException("ObjSQLException:The size of ResultSet more than 1.");
			}
			
			addInCache(sql, result,"String",SuidType.SELECT,1);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}

		return result;
	}

	@Override
	public List<String[]> select(String sql) {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		boolean isReg=updateInfoInCache(sql,"List<String[]>",SuidType.SELECT);
		if(isReg){
		Object cacheObj=cache.get(sql);  //这里的sql还没带有值
		if(cacheObj!=null) return (List<String[]>)cacheObj;
		}
		
		List<String[]> list = new ArrayList<String[]>();
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
			
//			ResultSetMetaData rmeta = rs.getMetaData();
//			int columnCount = rmeta.getColumnCount();
//			String str[] = null;
//			while (rs.next()) {
//				str = new String[columnCount];
//				for (int i = 0; i < columnCount; i++) {
//					str[i] = rs.getString(i + 1);
//				}
//				list.add(str);
//			}

			list=TransformResultSet.toStringsList(rs);
			
			addInCache(sql, list,"List<String[]>",SuidType.SELECT,list.size());
			
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}

		return list;
	}

	/*
	 * include insert,delete and update.
	 */
	//对应jdbc的executeUpdate方法
	@Override
	public int modify(String sql) {
		
		if(sql==null || "".equals(sql)) return -2;
		
		int num = 0;
		Connection conn = null;
		PreparedStatement pst = null;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			num = pst.executeUpdate(); //该语句必须是一个 SQL 数据操作语言（Data Manipulation Language，DML）语句
										//，比如 INSERT、UPDATE 或 DELETE 语句；或者是无返回内容的 SQL 语句，比如 DDL 语句。
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}
		
		
		//TODO  更改操作需要清除缓存
		if(num>0)
		   clearInCache(sql, "int",SuidType.MODIFY);
		
		return num;
	}

	/**
	 * @since  1.1
	 */
	@Override
	public String selectJson(String sql) {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		boolean isReg=updateInfoInCache(sql,"StringJson",SuidType.SELECT);
		if(isReg){
		Object cacheObj=cache.get(sql);  //这里的sql还没带有值
		if(cacheObj!=null) return (String)cacheObj;
		}
		
		StringBuffer json=new StringBuffer("");
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
			json = TransformResultSet.toJson(rs);
			
			addInCache(sql, json.toString(),"StringJson",SuidType.SELECT,-1);  //没有作最大结果集判断

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}

		return json.toString();
	}

	@Override
	public int[] batch(String sql[]) {
		if(sql==null) return null;
		int batchSize = HoneyConfig.getHoneyConfig().getBatchSize();
		//更改操作需要清除缓存
		clearInCache(sql[0]+ "[index0]", "int[]",SuidType.INSERT);
		return batch(sql,batchSize);
	}

	@Override
	public int[] batch(String sql[], int batchSize) {
		
		if(sql==null) return null;
		
		int len = sql.length;
		int total[] = new int[len];
		int part[] = new int[batchSize];
		
		Connection conn = null;
		PreparedStatement pst = null;
		try {
			conn = getConn();
			boolean oldAutoCommit=conn.getAutoCommit();
			conn.setAutoCommit(false);
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql[0]);
			pst = conn.prepareStatement(exe_sql);
		
		if (len <= batchSize){
			total=batch(sql,0,len,conn,pst);
		}else {
			for (int i = 0; i < len / batchSize; i++) {
				part = batch(sql, i * batchSize, (i + 1) * batchSize,conn,pst);
				total = HoneyUtil.mergeArray(total, part, i * batchSize, (i + 1) * batchSize);
				pst.clearBatch();  //clear Batch
//				pst.clearParameters();
			}
			
			if (len % batchSize != 0) { //尾数不成批
				int t2[] = batch(sql, len - (len % batchSize), len,conn,pst);
				total = HoneyUtil.mergeArray(total, t2, len - (len % batchSize), len);
			}
		}
//		conn.setAutoCommit(true);  //reset
		conn.setAutoCommit(oldAutoCommit);
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}

		return total;
	}

	private static String index1 = "[index";
	private static String index2 = "]";

	private int[] batch(String sql[], int start, int end, Connection conn,PreparedStatement pst) throws SQLException {
		int a[] = new int[end - start];
		for (int i = start; i < end; i++) { //start... (end-1)
			setPreparedValues(pst, sql[0] + index1 + i + index2);
			pst.addBatch();
		}
		a = pst.executeBatch(); //一次性提交      若两条数据,有一条插入不成功,返回0,0.  但实际上又有一条成功插入数据库(mysql测试,在自动提交的时候会有问题,不是自动提交不会)
		conn.commit();

		return a;
	}

	protected void checkClose(Statement stmt, Connection conn) {

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
//				e.printStackTrace();
				throw ExceptionHelper.convert(e);
			}
		}
		try {
			if (conn != null && conn.getAutoCommit()) {//自动提交时才关闭.如果开启事务,则由事务负责
				conn.close();
			}
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> moreTableSelect(String sql, T entity) {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		boolean isReg=updateInfoInCache(sql,"List<T>",SuidType.SELECT);
		if(isReg){
		Object cacheObj=cache.get(sql);  //这里的sql还没带有值
		if(cacheObj!=null) return (List<T>)cacheObj;
		}
		
		Connection conn = null;
		PreparedStatement pst = null;
		T targetObj = null;
		List<T> rsList = null;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);
			
			setPreparedValues(pst, sql);

			ResultSet rs = pst.executeQuery();
			rsList = new ArrayList<T>();

			Field field[] = entity.getClass().getDeclaredFields();
			int columnCount = field.length;
			
			MoreTableStruct moreTableStruct[]=HoneyUtil.getMoreTableStructAndCheckBefore(entity);
			Field subField[] = new Field[2];
			String subUseTable[]=new String[2];
			String variableName[]=new String[2];
			Class subEntityFieldClass[]=new Class[2];
			for (int i = 1; i <= 2; i++) {
				if(moreTableStruct[i]!=null){
					subField[i-1]=moreTableStruct[i].subEntityField;
					variableName[i-1]=subField[i-1].getName();
					subEntityFieldClass[i-1] = subField[i-1].getType();
//					if(moreTableStruct[i].hasSubAlias){
//						subUseTable[i-1]=moreTableStruct[i].subAlias;
//					}else{
//						subUseTable[i-1]=moreTableStruct[i].tableName;
//					}
					subUseTable[i-1]=moreTableStruct[i].useSubTableName;
				}
			}
			
			Field fields1[] = subEntityFieldClass[0].getDeclaredFields();
			Field fields2[] =null;
			
            if(subField[1]!=null){
            	fields2=subEntityFieldClass[1].getDeclaredFields();
            }
			
			String tableName=_toTableName(entity);
			while (rs.next()) {
				
				//从表1设置
				Object subObj1 = subEntityFieldClass[0].newInstance();
				for (int i = 0; i < fields1.length; i++) {
					
					if("serialVersionUID".equals(fields1[i].getName())) {
						continue;
					}
					if (fields1[i]!= null && fields1[i].isAnnotationPresent(JoinTable.class)) continue;
					
					fields1[i].setAccessible(true);
					try {
						fields1[i].set(subObj1, rs.getObject(subUseTable[0]+"."+_toColumnName(fields1[i].getName())));
					} catch (IllegalArgumentException e) {
						fields1[i].set(subObj1,_getObjectForMoreTable(rs,subUseTable[0],fields1[i]));
					}
				}
				
				//从表2设置(如果有)
				Object subObj2=null;
				if(subField[1]!=null){
					 subObj2 = subEntityFieldClass[1].newInstance();
					for (int i = 0; i < fields2.length; i++) {
						
						if("serialVersionUID".equals(fields2[i].getName())) {
							continue;
						}
						if (fields2[i]!= null && fields2[i].isAnnotationPresent(JoinTable.class)) continue;
						
						fields2[i].setAccessible(true);
						try {
							fields2[i].set(subObj2, rs.getObject(subUseTable[1]+"."+_toColumnName(fields2[i].getName())));
						} catch (IllegalArgumentException e) {
							fields2[i].set(subObj2,_getObjectForMoreTable(rs,subUseTable[1],fields2[i]));
						}
					}
				}
				
				//主表设置
				targetObj = (T) entity.getClass().newInstance();
				for (int i = 0; i < columnCount; i++) {
					if("serialVersionUID".equals(field[i].getName())) continue;
					if (field[i]!= null && field[i].isAnnotationPresent(JoinTable.class)) {
						field[i].setAccessible(true);
						if(field[i].getName().equals(variableName[0])){
							field[i].set(targetObj,subObj1); //设置子表1的对象
						}else if(subField[1]!=null && field[i].getName().equals(variableName[1])){
							field[i].set(targetObj,subObj2); //设置子表2的对象
						}
						continue;
					}
					field[i].setAccessible(true);
					try {
						field[i].set(targetObj, rs.getObject(tableName+"."+_toColumnName(field[i].getName())));
					} catch (IllegalArgumentException e) {
						field[i].set(targetObj,_getObjectForMoreTable(rs,tableName,field[i]));
					}
				}
				
				rsList.add(targetObj);
			}
			
			addInCache(sql, rsList,"List<T>",SuidType.SELECT,rsList.size());
			
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(pst, conn);
		}

		entity = null;
		targetObj = null;

		return rsList;
	}
	

	private void setPreparedValues(PreparedStatement pst, String sql) throws SQLException {
		List<PreparedValue> list = HoneyContext.getPreparedValue(sql);
		if (null != list && list.size() > 0) _setPreparedValues(pst, list);
	}

	private void _setPreparedValues(PreparedStatement pst, List<PreparedValue> list) throws SQLException {
		int size = list.size();
		for (int i = 0; i < size; i++) {
			int k = HoneyUtil.getJavaTypeIndex(list.get(i).getType());
			HoneyUtil.setPreparedValues(pst, k, i, list.get(i).getValue()); //i from 0
		}
	}
	
	private Object _getObject(ResultSet rs, Field field) throws SQLException{
		return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName()));
		
	}
	
	private Object _getObjectForMoreTable(ResultSet rs, String tableName,Field field) throws SQLException{
		return HoneyUtil.getResultObject(rs, field.getType().getName(), tableName+"."+ _toColumnName(field.getName()));
		
	}
	
	private Object _getObjectByindex(ResultSet rs,Field field, int index) throws SQLException{
		return HoneyUtil.getResultObjectByIndex(rs, field.getType().getName(),index);
	}
	
	private static String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	private static String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static String _toFieldName(String columnName) {
		return NameTranslateHandle.toFieldName(columnName);
	}
	
	//add on 2019-10-01
	private void addInCache(String sql, Object rs, String returnType, SuidType suidType,int resultSetSize) {
		
//		如果结果集超过一定的值则不放缓存
		int cacheWorkResultSetSize=HoneyConfig.getHoneyConfig().getCacheWorkResultSetSize();
		if(resultSetSize>cacheWorkResultSetSize){
		   HoneyContext.deleteCacheInfo(sql);
		   return;
		}
		
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) { //之前已定义有表结构,才放缓存.否则放入缓存,可能会产生脏数据.  不判断的话,自定义的查询也可以放缓存
		  cache.add(sql, rs);
		}
	}
	
//	查缓存前需要先更新缓存信息,才能去查看是否在缓存
//	private void updateInfoInCache(String sql, String returnType, SuidType suidType) {
	private boolean updateInfoInCache(String sql, String returnType, SuidType suidType) {
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
			struct.setReturnType(returnType);
			struct.setSuidType(suidType.getType());
			HoneyContext.setCacheInfo(sql, struct);
			return true;
		}
		//要是没有更新缓存,证明之前还没有登记过缓存,就不能去查缓存.
		return false;
	}
	
	private void clearInCache(String sql, String returnType, SuidType suidType) {
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
			struct.setReturnType(returnType);
			struct.setSuidType(suidType.getType());
			HoneyContext.setCacheInfo(sql, struct);
		}
		cache.clear(sql);
	}

}
