/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
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
	
	private int cacheWorkResultSetSize=HoneyConfig.getHoneyConfig().cache_workResultSetSize;
	private boolean enableMultiDs=HoneyConfig.getHoneyConfig().multiDS_enable;
	
	private static boolean  showSQL=HoneyConfig.getHoneyConfig().showSQL;

	public SqlLib() {}

	private Connection getConn() throws SQLException {
		return HoneyContext.getConn();
	}
	
	@Override
	public <T> List<T> select(String sql, T entity) {
		return selectSomeField(sql, entity);
	}

/*	//要是写的sql对应的结构与entity的结构不一致,将会有问题
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> select(String sql, T entity) {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		//要是没有更新缓存,证明之前还没有登记过缓存,就不能去查缓存.
		boolean isReg=updateInfoInCache(sql,"List<T>",SuidType.SELECT);
		if(isReg){
		Object cacheObj=cache.get(sql);  //这里的sql还没带有值
		if(cacheObj!=null) return (List<T>)cacheObj;
		initRoute(SuidType.SELECT,entity.getClass());
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
	}*/

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> selectSomeField(String sql, T entity) {
		
//		if(sql==null || "".equals(sql.trim())) return null;
		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();

		boolean isReg=updateInfoInCache(sql,"List<T>",SuidType.SELECT);
		if (isReg) {
			Object cacheObj = cache.get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<T> list=(List<T>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
			initRoute(SuidType.SELECT, entity.getClass(), sql);
		}
		
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs =null;
		T targetObj = null;
		List<T> rsList = null;
		Map<String,Field> map=null;
		boolean hasException=false;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
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
			hasException=true;
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
			if (hasException) {
				checkClose(rs, pst, null);
				closeConn(conn);
			} else {
				checkClose(rs, pst, conn);
			}
			entity = null;
			targetObj = null;
			map=null;
		}
		logSelectRows(rsList.size());

		return rsList;
	}

	/**
	 * SQL function: max,min,avg,sum,count. 如果统计的结果集为空,除了count返回0,其它都返回空字符.
	 */
	@Override
	public String selectFun(String sql) throws ObjSQLException {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		boolean isReg = updateInfoInCache(sql, "String", SuidType.SELECT);
		if (isReg) {
			Object cacheObj = cache.get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				return (String) cacheObj;
			}

			initRoute(SuidType.SELECT, null, sql);
		}
		
		String result = null;
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		boolean hasException = false;
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

			boolean hasMore = false;
			if (rs.next()) {
				hasMore = true;
			}
			
			if (hasMore) {
				throw new ObjSQLException("ObjSQLException:The size of ResultSet more than 1.");
			}
			
			addInCache(sql, result,"String",SuidType.SELECT,1);

		} catch (SQLException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
			if (hasException) {
				checkClose(rs, pst, null);
				closeConn(conn);
			} else {
				checkClose(rs, pst, conn);
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String[]> select(String sql) {
		
//		if(sql==null || "".equals(sql.trim())) return null;
		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();
		
		boolean isReg = updateInfoInCache(sql, "List<String[]>", SuidType.SELECT);
		if (isReg) {
			Object cacheObj = cache.get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<String[]> list=(List<String[]>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
			initRoute(SuidType.SELECT, null, sql);
		}
		
		List<String[]> list = new ArrayList<String[]>();
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		boolean hasException = false;
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
			
			logSelectRows(list.size());
			
			addInCache(sql, list,"List<String[]>",SuidType.SELECT,list.size());
			
		} catch (SQLException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
			if (hasException) {
				checkClose(rs, pst, null);
				closeConn(conn);
			} else {
				checkClose(rs, pst, conn);
			}
		}

		return list;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String,Object>> selectMapList(String sql) {
		
		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();
		
		boolean isReg = updateInfoInCache(sql, "List<Map<String,Object>>", SuidType.SELECT);
		if (isReg) {
			Object cacheObj = cache.get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<Map<String,Object>> list=(List<Map<String,Object>>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
			initRoute(SuidType.SELECT, null, sql);
		}
		
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		boolean hasException = false;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);
			setPreparedValues(pst, sql);
			rs = pst.executeQuery();

			list=TransformResultSet.toMapList(rs);
			
			logSelectRows(list.size());
			
			addInCache(sql, list,"List<Map<String,Object>>",SuidType.SELECT,list.size());
			
		} catch (SQLException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
			if (hasException) {
				checkClose(rs, pst, null);
				closeConn(conn);
			} else {
				checkClose(rs, pst, conn);
			}
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
		
		initRoute(SuidType.MODIFY,null,sql);
		
		int num = 0;
		Connection conn = null;
		PreparedStatement pst = null;
		boolean hasException = false;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			num = pst.executeUpdate(); //该语句必须是一个 SQL 数据操作语言（Data Manipulation Language，DML）语句
										//，比如 INSERT、UPDATE 或 DELETE 语句；或者是无返回内容的 SQL 语句，比如 DDL 语句。
		} catch (SQLException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			clearInCache(sql, "int",SuidType.MODIFY,num); //has clearContext(sql)
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}
		
		Logger.logSQL(" | <--  Affected rows: ", num+"");
		
		return num;
	}
	
	@Override
	public long insertAndReturnId(String sql) {

		if (sql == null || "".equals(sql)) return -2L;

		initRoute(SuidType.INSERT, null, sql);

		int num = 0;
		long returnId = -1L;
		Connection conn = null;
		PreparedStatement pst = null;
		boolean hasException = false;
		try {
			conn = getConn();
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql, new String[] { "id" });
			setPreparedValues(pst, sql);
			num = pst.executeUpdate();

			ResultSet rsKey = pst.getGeneratedKeys();
			rsKey.next();
			returnId = rsKey.getLong(1);
		} catch (SQLException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} finally {
			clearInCache(sql, "int", SuidType.INSERT, num); //has clearContext(sql)
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}

		Logger.logSQL(" | <--  Affected rows: ", num + "");

		return returnId; //id
	}

	/**
	 * @since  1.1
	 */
	@Override
	public String selectJson(String sql) {
		
		if(sql==null || "".equals(sql.trim())) return null;
		
		boolean isReg = updateInfoInCache(sql, "StringJson", SuidType.SELECT);
		if (isReg) {
			Object cacheObj = cache.get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				return (String) cacheObj;
			}
			initRoute(SuidType.SELECT, null, sql);
		}
		
		StringBuffer json=new StringBuffer("");
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		boolean hasException = false;
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
			clearContext(sql);
			if (hasException) {
				checkClose(rs, pst, null);
				closeConn(conn);
			} else {
				checkClose(rs, pst, conn);
			}
		}

		return json.toString();
	}

	@Override
	public int batch(String sql[]) {
		if(sql==null) return -1;
		int batchSize = HoneyConfig.getHoneyConfig().insertBatchSize;

		return batch(sql,batchSize);
	}
	
	@Override
	public int batch(String sql[], int batchSize) {
		
		if(sql==null || sql.length<1) return -1;
		
		if(HoneyUtil.isMysql()) return batchForMysql(sql, batchSize);
		
		initRoute(SuidType.INSERT,null,sql[0]);
		
		int len = sql.length;
		int total = 0;
		int temp=0;
		
		Connection conn = null;
		PreparedStatement pst = null;
		boolean oldAutoCommit=false;
		boolean hasException = false;
		
		try {
			conn = getConn();
			oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql[0]);
			pst = conn.prepareStatement(exe_sql);
		
		if (len <= batchSize){
			total=batch(sql[0],0,len,conn,pst);
		}else {
			for (int i = 0; i < len / batchSize; i++) {
				temp = batch(sql[0], i * batchSize, (i + 1) * batchSize,conn,pst);
				total+=temp;
				pst.clearBatch();  //clear Batch
				pst.clearParameters();
			}
			
			if (len % batchSize != 0) { //尾数不成批
				temp = batch(sql[0], len - (len % batchSize), len,conn,pst);
				total+=temp;
			}
		}
//		conn.setAutoCommit(oldAutoCommit);
		} catch (SQLException e) {
			hasException=true;
			if (isConstraint(e)) {
				Logger.error("Please confirm whether it is Primary Key violation !!");
				clearContext(sql[0],batchSize,len);
				Logger.error(e.getMessage());
				return total;
			}
			Logger.debug(e.getMessage());
			throw ExceptionHelper.convert(e);
		} finally {
//			bug :Lock wait timeout exceeded; 
//			如果分批处理时有异常,如主键冲突,则又没有提交,就关不了连接.
//			所以需要先将提交改回原来的状态.
//			要是原来就是自动提交,报异常,  也要关
			try {
				if (conn != null) conn.setAutoCommit(oldAutoCommit);
			} catch (Exception e3) {
				//ignore
				Logger.debug(e3.getMessage());
			}
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
				
			//更改操作需要清除缓存
			clearInCache(sql[0], "int[]",SuidType.INSERT,total);
		}

		return total;
	}

	private static final String index1 = "_SYS[index";
	private static final String index2 = "]_End ";
	private static final String index3 = "]";

	private int batch(String sql, int start, int end, Connection conn,PreparedStatement pst) throws SQLException {
		int a=0;
		for (int i = start; i < end; i++) { //start... (end-1)
			
			if (showSQL) {
				OneTimeParameter.setAttribute("_SYS_Bee_BatchInsert", i + "");
				String sql_i;
				if (i == 0)
					sql_i = sql;
				else
					sql_i = index1 + i + index2 + sql;

				Logger.logSQL(" insert[] SQL : ", sql_i);
			}
			
			if (i == 0)
				setAndClearPreparedValues(pst, sql);
			else
				setAndClearPreparedValues(pst, index1 + i + index2 + sql);
			pst.addBatch();
		}
		int array[]=pst.executeBatch();    //oracle will return [-2,-2,...,-2]
		
		if(HoneyUtil.isOracle()){
			a=pst.getUpdateCount();//oracle is ok. but mysql will return 1 alway.So mysql use special branch.
		}else{
			a=countFromArray(array);
		}
		conn.commit();
		
		Logger.logSQL(" | <-- index["+ (start) +"~"+(end-1)+ index3+" Affected rows: ", a+"");

		return a;
	}
	
	private int countFromArray(int array[]){
		int a=0;
		for (int i=0; i < array.length; i++) {
			a+=array[i];
		}
		return a;
	}
	
//	@Override
	private int batchForMysql(String sql[], int batchSize) {

		if (sql == null || sql.length < 1) return -1;

		initRoute(SuidType.INSERT, null, sql[0]);

		int len = sql.length;
		int total = 0;
		int temp = 0;
		String placeholderValue = (String) OneTimeParameter.getAttribute("_SYS_Bee_PlaceholderValue");

		Connection conn = null;
		PreparedStatement pst = null;
		boolean oldAutoCommit=false;
		boolean hasException = false;
		try {
			conn = getConn();
			oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql[0]);

			String batchExeSql[];

			if (len <= batchSize) {
				batchExeSql = getBatchExeSql(exe_sql, len, placeholderValue); //batchExeSql[1] : ForPrint
				pst = conn.prepareStatement(batchExeSql[0]);

				total = _batchForMysql(sql[0], 0, len, conn, pst, batchSize, batchExeSql[1]);
			} else {
				batchExeSql = getBatchExeSql(exe_sql, batchSize, placeholderValue);
				pst = conn.prepareStatement(batchExeSql[0]);
				
				for (int i = 0; i < len / batchSize; i++) {
					temp = _batchForMysql(sql[0], i * batchSize, (i + 1) * batchSize, conn, pst, batchSize, batchExeSql[1]);
					total += temp;
				}

				if (len % batchSize != 0) { //尾数不成批
					batchExeSql = getBatchExeSql(exe_sql, (len % batchSize), placeholderValue);
					pst = conn.prepareStatement(batchExeSql[0]);   //fixed bug
					temp = _batchForMysql(sql[0], len - (len % batchSize), len, conn, pst, batchSize, batchExeSql[1]);
					total += temp;
				}
			}
//			conn.setAutoCommit(oldAutoCommit);
		} catch (SQLException e) {
			hasException=true;
			if (isConstraint(e)) {
				Logger.error("Please confirm whether it is Primary Key violation !!");
				clearContextForMysql(sql[0],batchSize,len);
				
				return total;
			}
			Logger.debug(e.getMessage());
			throw ExceptionHelper.convert(e);
		} finally {
//			bug :Lock wait timeout exceeded; 
//			如果分批处理时有异常,如主键冲突,则又没有提交,就关不了连接.
//			所以需要先将提交改回原来的状态.
//			要是原来就是自动提交,报异常, 也要关
			try {
				if (conn != null) conn.setAutoCommit(oldAutoCommit);
			} catch (Exception e2) {
				//ignore
				Logger.debug(e2.getMessage());
			}
			
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
			
			//更改操作需要清除缓存
			clearInCache(sql[0], "int[]", SuidType.INSERT,total);
		}

		return total;
	}
	
	private void clearContext(String sql_0, int batchSize, int len) {
		for (int i = 0; i < len; i++) {
			String sql_i = index1 + i + index2 + sql_0;
			clearContext(sql_i);
		}
	}
	
	private void clearContextForMysql(String sql_0, int batchSize, int len) {
		clearContext(sql_0, batchSize, len);

		int num = (len - 1) / batchSize;
		for (int k = 0; k <= num; k++) {
			String sqlForGetValue = sql_0 + "  [Batch:" + k + index3;
			clearContext(sqlForGetValue);
		}
	}
	
	private boolean isConstraint(SQLException e) {
		String className=e.getClass().getSimpleName();
		String fullClassName=e.getClass().getName();
		return ("MySQLIntegrityConstraintViolationException".equals(className) //mysql
				|| e.getMessage().startsWith("Duplicate entry ") //mysql  
				|| (e instanceof SQLIntegrityConstraintViolationException) 
				|| e.getMessage().contains("ORA-00001:")    //Oracle 
				|| (e instanceof BatchUpdateException) //PostgreSQL,...
				|| e.getMessage().contains("duplicate key") || e.getMessage().contains("DUPLICATE KEY")  //PostgreSQL
				|| e.getMessage().contains("primary key violation") || "org.h2.jdbc.JdbcBatchUpdateException".equals(fullClassName) //h2
				|| e.getMessage().contains("SQLITE_CONSTRAINT_PRIMARYKEY") || e.getMessage().contains("PRIMARY KEY constraint") //SQLite
				|| e.getMessage().contains("Duplicate entry")|| e.getMessage().contains("Duplicate Entry")
				|| e.getMessage().contains("Duplicate key") || e.getMessage().contains("Duplicate Key")
				);
	}

	private int _batchForMysql(String sql, int start, int end, Connection conn, PreparedStatement pst,int batchSize,String batchSqlForPrint) throws SQLException {
//		sql用于获取转换成获取占位的sqlKey和打印log. 
//		v1.8  打印的sql是单行打印；执行的是批的形式.
		if (showSQL) {
			//print log
			if(start==0 || (end-start!=batchSize)) {
				if(batchSize==1) OneTimeParameter.setTrueForKey("_SYS_Bee_BatchInsertFirst");
				Logger.logSQL(" insert[] SQL : ", batchSqlForPrint);
			}
			
			for (int i = start; i < end; i++) { //start... (end-1)
				OneTimeParameter.setAttribute("_SYS_Bee_BatchInsert", i + "");
				String sql_i;
				if (i == 0)
					sql_i = sql;
				else
					sql_i = index1 + i + index2 + sql;

				Logger.logSQL(" insert[] SQL : ", sql_i);
			}
		}
		
		int a = 0;
		String sqlForGetValue=sql+ "  [Batch:"+ (start/batchSize) + index3;
		setAndClearPreparedValues(pst, sqlForGetValue);
		a = pst.executeUpdate();  // not executeBatch
		conn.commit();
		
		Logger.logSQL(" | <-- [Batch:"+ (start/batchSize) + index3+" Affected rows: ", a+"");

		return a;
	}
	
	private String[] getBatchExeSql(String sql0,int size,String placeholderValue){
		StringBuffer batchSql=new StringBuffer(sql0);
		StringBuffer batchSql_forPrint=new StringBuffer(sql0);
		String batchExeSql[]=new String[2];
		for (int i = 0; i < size-1; i++) { //i=0
			batchSql.append(",");
			batchSql.append(placeholderValue);
			
			if(size>10 && i==1){ //超过10个的，会用省略格式。
				batchSql_forPrint.append(",......,");
			}else if(size>10 && i==size-2){
				batchSql_forPrint.append(placeholderValue);
				batchSql_forPrint.append("      ");
				batchSql_forPrint.append("Total of records : ");
				batchSql_forPrint.append(size);
			}else if(size>10 && i>1){
				//ignore
			}else{
				batchSql_forPrint.append(",");
				batchSql_forPrint.append(placeholderValue);
			}
		}
		
		batchExeSql[0]=batchSql.toString();
		batchExeSql[1]=batchSql_forPrint.toString();//for print log
		
		return batchExeSql;
	}

	protected void checkClose(Statement stmt, Connection conn) {
		HoneyContext.checkClose(stmt, conn);
	}
	
	protected void checkClose(ResultSet rs, Statement stmt, Connection conn) {
		HoneyContext.checkClose(rs, stmt, conn);
	}
	
	protected void closeConn(Connection conn) {
		HoneyContext.closeConn(conn);
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> List<T> moreTableSelect(String sql, T entity) {
		
//		if(sql==null || "".equals(sql.trim())) return null;
		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();
		
		boolean isReg = updateInfoInCache(sql, "List<T>", SuidType.SELECT);
		if (isReg) {
			Object cacheObj = cache.get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				
				List<T> list=(List<T>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
			initRoute(SuidType.SELECT, entity.getClass(), sql); //only multi-Ds,tables don't allow in different db.仅分库时，多表查询的多个表要在同一个数据源.
		}
		
		Connection conn=null;
		PreparedStatement pst=null;
		ResultSet rs=null;
		T targetObj=null;
		List<T> rsList=null;
		boolean hasException = false;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);
			
			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
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
            
            Map<String,String> dulSubFieldMap=moreTableStruct[0].subDulFieldMap;
			
//			String tableName=_toTableName(entity);
			String tableName=moreTableStruct[0].tableName;
			while (rs.next()) {
				
				//从表1设置
				Object subObj1 = subEntityFieldClass[0].newInstance();
				
				boolean isDul=false;
				String dulField="";
				
				for (int i = 0; i < fields1.length; i++) {
					
//					if("serialVersionUID".equals(fields1[i].getName()) || fields1[i].isSynthetic()) {
//						continue;
//					}
//					if (fields1[i]!= null && fields1[i].isAnnotationPresent(JoinTable.class)) continue;
					if(HoneyUtil.isSkipField(fields1[i])) continue;
					
					
					fields1[i].setAccessible(true);
					isDul=false;
					dulField="";
					try {
						String columnName=_toColumnName(fields1[i].getName());
						if(isConfuseDuplicateFieldDB()){
							dulField=dulSubFieldMap.get(subUseTable[0]+"."+columnName);
							if(dulField!=null){
								isDul=true;  //fixed bug.  need set true before fields1[i].set(  )
								fields1[i].set(subObj1, rs.getObject(dulField));
							}else{
							   fields1[i].set(subObj1, rs.getObject(columnName));
							}
						}else{
						    fields1[i].set(subObj1, rs.getObject(subUseTable[0]+"."+columnName));
						}
					} catch (IllegalArgumentException e) {
//						e.printStackTrace();
						if(isConfuseDuplicateFieldDB()){
							fields1[i].set(subObj1,_getObjectForMoreTable_ConfuseField(rs,fields1[i],isDul,dulField));
						}else{
						    fields1[i].set(subObj1,_getObjectForMoreTable(rs,subUseTable[0],fields1[i]));
						}
					}catch (SQLException e) {// for after use condition selectField method
						fields1[i].set(subObj1,null);
					}
				}
				
				//从表2设置(如果有)
				Object subObj2=null;
				if(subField[1]!=null){
					 subObj2 = subEntityFieldClass[1].newInstance();
					String columnName="";
					for (int i = 0; i < fields2.length; i++) {
						
//						if("serialVersionUID".equals(fields2[i].getName()) || fields2[i].isSynthetic()) {
//							continue;
//						}
//						if (fields2[i]!= null && fields2[i].isAnnotationPresent(JoinTable.class)) continue;
						if(HoneyUtil.isSkipField(fields2[i])) continue;
						
						fields2[i].setAccessible(true);
						isDul=false;
						dulField="";
						try {
							columnName=_toColumnName(fields2[i].getName());
							if(isConfuseDuplicateFieldDB()){
								dulField=dulSubFieldMap.get(subUseTable[1]+"."+columnName);
								if(dulField!=null){
									isDul=true;  //set true first
									fields2[i].set(subObj2, rs.getObject(dulField));	
								}else{
									fields2[i].set(subObj2, rs.getObject(columnName));
								}
							} else {
								fields2[i].set(subObj2, rs.getObject(subUseTable[1] + "." + columnName));
							}
						} catch (IllegalArgumentException e) {
							if(isConfuseDuplicateFieldDB()){
								fields2[i].set(subObj2,_getObjectForMoreTable_ConfuseField(rs,fields2[i],isDul,dulField));  //todo
							}else{
								fields2[i].set(subObj2,_getObjectForMoreTable(rs,subUseTable[1],fields2[i]));
							}
						}catch (SQLException e) {// for after use condition selectField method
							fields2[i].set(subObj2,null);
						}
					}
				}
				
				//主表设置
				targetObj = (T) entity.getClass().newInstance();
				for (int i = 0; i < columnCount; i++) {
//					if("serialVersionUID".equals(field[i].getName()) || field[i].isSynthetic()) continue;
					if(HoneyUtil.isSkipFieldForMoreTable(field[i])) continue;  //有Ignore注释,将不再处理JoinTable
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
						if (isConfuseDuplicateFieldDB()) {
							field[i].set(targetObj, rs.getObject(_toColumnName(field[i].getName())));
						} else {
							field[i].set(targetObj, rs.getObject(tableName + "." + _toColumnName(field[i].getName())));
						}
					} catch (IllegalArgumentException e) {
						field[i].set(targetObj,_getObjectForMoreTable(rs,tableName,field[i]));
				    } catch (SQLException e) { // for after use condition selectField method
					  field[i].set(targetObj,null);
				    }
					
				}
				
				rsList.add(targetObj);
			}
			
			addInCache(sql, rsList,"List<T>",SuidType.SELECT,rsList.size());
			
		} catch (SQLException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
			if (hasException) {
				checkClose(rs, pst, null);
				closeConn(conn);
			} else {
				checkClose(rs, pst, conn);
			}
		}

		entity = null;
		targetObj = null;
		
		logSelectRows(rsList.size());

		return rsList;
	}
	

	private void setPreparedValues(PreparedStatement pst, String sql) throws SQLException {
//		查询时设置值,就删了上下文,当查询回来,放缓存时,就不能用
//		List<PreparedValue> list = HoneyContext.getPreparedValue(sql); //拿了设值后就会删除.用于日志打印的,要早于此时.   bug: when add cache, no value. 
		List<PreparedValue> list = HoneyContext.justGetPreparedValue(sql); 
		if (null != list && list.size() > 0) _setPreparedValues(pst, list);
	}
	
	//只用于批处理
	private void setAndClearPreparedValues(PreparedStatement pst, String sql) throws SQLException {
		List<PreparedValue> list = HoneyContext.getAndClearPreparedValue(sql); 
		if (null != list && list.size() > 0) _setPreparedValues(pst, list);
	}

	private void _setPreparedValues(PreparedStatement pst, List<PreparedValue> list) throws SQLException {
		int size = list.size();
		for (int i = 0; i < size; i++) {
			int k = HoneyUtil.getJavaTypeIndex(list.get(i).getType());
			HoneyUtil.setPreparedValues(pst, k, i, list.get(i).getValue()); //i from 0
		}
	}
	
//	private Object _getObject(ResultSet rs, Field field) throws SQLException{
//		return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName()));
//	}
	
	private Object _getObjectForMoreTable(ResultSet rs, String tableName, Field field) throws SQLException {
		if (isConfuseDuplicateFieldDB()) {
			return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName()));
		} else {
			return HoneyUtil.getResultObject(rs, field.getType().getName(), tableName + "." + _toColumnName(field.getName()));
		}
	}
	
	//oracle,SQLite
	private Object _getObjectForMoreTable_ConfuseField(ResultSet rs, Field field, boolean isDul, String otherName) throws SQLException {

		if (isDul) return HoneyUtil.getResultObject(rs, field.getType().getName(), otherName);

		return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName()));
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
		if(resultSetSize>cacheWorkResultSetSize){
		   HoneyContext.deleteCacheInfo(sql);
		   return;
		}
		
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) { //之前已定义有表结构,才放缓存.否则放入缓存,可能会产生脏数据.  不判断的话,自定义的查询也可以放缓存
//			struct.setReturnType(returnType);  //因一进来updateInfoInCache时,已添加有
//			struct.setSuidType(suidType.getType());
//			HoneyContext.setCacheInfo(sql, struct);
			cache.add(sql, rs);
		}
	}
	
//	查缓存前需要先更新缓存信息,才能去查看是否在缓存
	private boolean updateInfoInCache(String sql, String returnType, SuidType suidType) {
		return HoneyContext.updateInfoInCache(sql, returnType, suidType);
	}
	
	private void clearInCache(String sql, String returnType, SuidType suidType, int affectRow) {
		CacheSuidStruct struct = HoneyContext.getCacheInfo(sql);
		if (struct != null) {
			struct.setReturnType(returnType);
			struct.setSuidType(suidType.getType());
			HoneyContext.setCacheInfo(sql, struct);
		}
		clearContext(sql);
		if (affectRow > 0) { //INSERT、UPDATE 或 DELETE成功,才清除结果缓存
			cache.clear(sql);
		}
	}
	
	private void clearContext(String sql) {
		HoneyContext.clearPreparedValue(sql);
		if(HoneyContext.isNeedRealTimeDb() && HoneyContext.isAlreadySetRoute()) { //当可以从缓存拿时，需要清除为分页已设置的路由
			HoneyContext.removeCurrentRoute();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void initRoute(SuidType suidType, Class clazz, String sql) {
		if (!enableMultiDs) return;
		if(HoneyContext.isNeedRealTimeDb() && HoneyContext.isAlreadySetRoute()) return; // already set in parse entity to sql.
		HoneyContext.initRoute(suidType, clazz, sql);
	}
	
	//Oracle,SQLite
	private boolean isConfuseDuplicateFieldDB(){
		return HoneyUtil.isConfuseDuplicateFieldDB();
	}
	
	private void logSelectRows(int size) {
		Logger.logSQL(" | <--  select rows: ", size + "");
	}
	
}
