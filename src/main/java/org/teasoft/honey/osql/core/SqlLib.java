/*
 * Copyright 2013-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.type.TypeHandler;
import org.teasoft.honey.osql.type.TypeHandlerRegistry;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.sharding.engine.ShardingAvgEngine;
import org.teasoft.honey.sharding.engine.ShardingGroupbyListStringArrayEngine;
import org.teasoft.honey.sharding.engine.ShardingModifyEngine;
import org.teasoft.honey.sharding.engine.ShardingMoreTableSelectEngine;
import org.teasoft.honey.sharding.engine.ShardingSelectEngine;
import org.teasoft.honey.sharding.engine.ShardingSelectFunEngine;
import org.teasoft.honey.sharding.engine.ShardingSelectJsonEngine;
import org.teasoft.honey.sharding.engine.ShardingSelectListStringArrayEngine;
import org.teasoft.honey.sharding.engine.ShardingSelectRsEngine;
import org.teasoft.honey.util.StringUtils;

/**
 * 直接操作数据库，并返回结果.在该类中的sql字符串要是DB能识别的SQL语句
 * Directly operate the database and return the result. 
 * <br>The SQL string in this class should be an SQL statement recognized by DB.
 * @author Kingstar
 * Create on 2013-6-30 下午10:32:53
 * @since  1.0
 */
public class SqlLib extends AbstractBase implements BeeSql, Serializable {
	
	private static final long serialVersionUID = 1596710362259L;
	private boolean showSQL = getShowSQL();

	private Connection getConn() throws SQLException {
		return HoneyContext.getConn();
	}
	
	@Override
	public <T> List<T> select(String sql, final Class<T> entityClass) {
		return selectSomeField(sql, entityClass);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> selectSomeField(String sql, final Class<T> entityClass) {

		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();
		
		if (isSimpleMode()) {
			return _selectSomeField(sql, entityClass); // 1.x版本及不用分片走的分支
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {
//				List<T> list = getCache(sql, entity); // 总sql不能去查询,只能检测是否有缓存. 没有的话,要分开来查询.
				List<T> list =_selectSomeField(sql, entityClass); //检测缓存的
				if (list != null) {// 若缓存是null,就无法区分了,所以没有数据,最好是返回空List,而不是null
					logDsTab();
					return list; 
				}
				try {
				List<T> rsList;
				boolean jdbcStreamSelect =HoneyConfig.getHoneyConfig().sharding_jdbcStreamSelect;
				
				if (ShardingUtil.hadAvgSharding()) {
					int List_T = 2;
					rsList = (List<T>) new ShardingGroupbyListStringArrayEngine().asynProcess(sql, this, entityClass, List_T);
				} else if (jdbcStreamSelect && ! ShardingUtil.hadGroupSharding()) {
					rsList = new ShardingSelectRsEngine().asynProcess(sql, entityClass, this); // 无结果集时,可能会报错   fixed V2.1
				} else {
					rsList = new ShardingSelectEngine().asynProcess(sql, entityClass, this); 
				}
				addInCache(sql, rsList, rsList.size()); // 缓存Key,是否包括了分片的DS,Tables
				logSelectRows(rsList.size());
				
				return rsList;
				}finally {
				   clearContext(sql); //2.2 分片的主线程都要清主线程的上下文 
				}
				
			} else { // 子线程执行
				return _selectSomeField(sql, entityClass);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> _selectSomeField(String sql, final Class<T> entityClass) {
//		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();

		boolean isReg = updateInfoInCache(sql, "List<T>", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, entityClass, sql);
			Object cacheObj = getCache().get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<T> list = (List<T>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}
		if(isShardingMain()) return null; //sharding时,主线程没有缓存就返回.

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		T targetObj = null;
		List<T> rsList = null;
		boolean hasException = false;
		
		try {
			conn = getConn();
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
			rsList = new ArrayList<>();
			while (rs.next()) {
//				targetObj=TransformResultSet.rowToEntity(rs, entityClass);
				targetObj=ResultAssemblerHandler.rowToEntity(rs, entityClass);
				rsList.add(targetObj);
			}
			addInCache(sql, rsList, rsList.size());
		} catch (SQLException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} finally {
			closeRs(rs);
			clearContext(sql);
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
			targetObj = null;
		}
		logSelectRows(rsList.size());

		return rsList;
	}
	
	
	@Override
	public ResultSet selectRs(String sql) {

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			conn = getConn();
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
			return rs;
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
			if (conn != null) HoneyContext.regConnForSelectRs(conn);
		}
	}
	
	
	/**
	 * SQL function: max,min,avg,sum,count. 如果统计的结果集为空,除了count返回0,其它都返回空字符.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public String selectFun(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		if(sql==null || "".equals(sql.trim())) return null; 
		if (isSimpleMode()) {
			return _selectFun(sql,entityClass);
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {
				
				String cacheValue=_selectFun(sql,entityClass);  //检测缓存的
				if(cacheValue!=null) {
					logDsTab();
					return cacheValue;
				}
				
				try {
				String fun = "";
				String funType = HoneyContext.getSysCommStrLocal(StringConst.FunType);
				if (FunctionType.AVG.getName().equalsIgnoreCase(funType)) { //avg need change sql
					String newSql=ShardingAvgEngine.rewriteAvgSql(sql);
					HoneyContext.setPreparedValue(newSql,  HoneyContext.justGetPreparedValue(sql));
					List<String[]> rsList =new ShardingSelectListStringArrayEngine().asynProcess(newSql, this,entityClass);
					fun= ShardingAvgEngine.avgResultEngine(rsList);
					clearContext(newSql);
				} else {
					fun = new ShardingSelectFunEngine().asynProcess(sql, this, entityClass); 
				}
				
				addInCache(sql, fun, 1);
				
				return fun;
				}finally {
				   clearContext(sql); //2.2 分片的主线程都要清主线程的上下文 
				}
				
			} else { // 子线程执行
				return _selectFun(sql,entityClass);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private String _selectFun(String sql, Class entityClass) {

//		if(sql==null || "".equals(sql.trim())) return null; //往前放了

		boolean isReg = updateInfoInCache(sql, "String", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, entityClass, sql);
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				return (String) cacheObj;
			}
		}
		if (isShardingMain()) return null; // sharding时,主线程没有缓存就返回.

		String result = null;
		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		boolean hasException = false;
		try {
			conn = getConn();
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);

			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
			if (rs.next()) { // if
//				result= rs.getString(1); 
				if (rs.getObject(1) == null)
					result = "";
				else
					result = rs.getObject(1).toString();
			}

			if (rs.next()) {
				throw new ObjSQLException("The size of ResultSet more than 1.");
			}

			addInCache(sql, result, 1);

		} catch (SQLException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} finally {
			closeRs(rs);
			clearContext(sql);
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}

		return result;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String[]> select(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();

		if (isSimpleMode()) {
			return _select(sql, entityClass); // 1.x版本及不用分片走的分支
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {
				List<String[]> list = _select(sql, entityClass); // 检测缓存的
				if (list != null) {
					logDsTab();
					return list;
				}
				
				try {
				List<String[]> rsList;
				if (ShardingUtil.hadGroupSharding()) {
					int List_String_Array = 1;
					rsList = (List<String[]>) new ShardingGroupbyListStringArrayEngine().asynProcess(sql, this, entityClass, List_String_Array);
				} else {
					rsList = new ShardingSelectListStringArrayEngine().asynProcess(sql, this, entityClass);
				}
				
				addInCache(sql, rsList, rsList.size());

				return rsList;
				
				}finally {
				 clearContext(sql); //2.2 分片的主线程都要清主线程的上下文 
				}
				
			} else { // 子线程执行
				return _select(sql, entityClass);
			}
		}
	}

	private boolean isSimpleMode() {
		return !ShardingUtil.hadSharding();
	}
		
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String[]> _select(String sql, final Class entityClass) {
//		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();
		
		boolean isReg = updateInfoInCache(sql, "List<String[]>", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, entityClass, sql);
			Object cacheObj = getCache().get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<String[]> list=(List<String[]>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}
		if(isShardingMain()) return null; //sharding时,主线程没有缓存就返回.
		
		List<String[]> list = new ArrayList<>();
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
			list=TransformResultSet.toStringsList(rs);
			
			logSelectRows(list.size());
			addInCache(sql, list, list.size());
			
		} catch (SQLException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			closeRs(rs);
			clearContext(sql);
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}

		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String,Object>> selectMapList(String sql) {
		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();
		
		boolean isReg = updateInfoInCache(sql, "List<Map<String,Object>>", SuidType.SELECT, null);
		if (isReg) { //V1.9还未使用
			initRoute(SuidType.SELECT, null, sql);
			Object cacheObj = getCache().get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<Map<String,Object>> list=(List<Map<String,Object>>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}
		
		List<Map<String,Object>> list = new ArrayList<>();
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
			
			addInCache(sql, list, list.size());
			
		} catch (SQLException e) {
			hasException=true;
			throw ExceptionHelper.convert(e);
		} finally {
			closeRs(rs);
			clearContext(sql);
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}

		return list;
	}

	//对应jdbc的executeUpdate方法
	/*
	 * modify include insert,delete and update.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public int modify(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
//		if (sql == null || "".equals(sql)) return -2;
		if (sql == null || "".equals(sql)) return -1; //2.4.0
		if (isSimpleMode()) {
			return _modify(sql, entityClass); // 1.x版本及不用分片走的分支
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {// 拦截到的要分片的主线程
				try {
					int a = new ShardingModifyEngine().asynProcess(sql, entityClass, this);
					return a;
				} finally {
					clearContext(sql); // 2.2 分片的主线程都要清主线程的上下文
				}
			} else { // 子线程执行
				return _modify(sql, entityClass);
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private int _modify(String sql, final Class entityClass) {
		
		initRoute(SuidType.MODIFY, entityClass, sql);
		
		int num = 0;
		Connection conn = null;
		PreparedStatement pst = null;
		boolean hasException = false;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);
			setPreparedValues(pst, sql);
			num = pst.executeUpdate();
		} catch (SQLException e) {
			hasException=true; //finally要用到
			if(catchModifyDuplicateException(e)) return num;
			else throw ExceptionHelper.convert(e);
		} finally {
			clearInCache(sql, "int", SuidType.MODIFY, num); // has clearContext(sql)
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}
		
		logAffectRow(num);

		return num;
	}
	
	private boolean catchModifyDuplicateException(SQLException e) {
		boolean notCatch=HoneyConfig.getHoneyConfig().notCatchModifyDuplicateException;
		if (!notCatch && isConstraint(e)) { //内部捕获并且是重复异常,则由Bee框架处理 
			boolean notShow=HoneyConfig.getHoneyConfig().notShowModifyDuplicateException;
			if(! notShow) {
				Logger.warn(e.getMessage());
			}else {
				Logger.debug(e.getMessage());
			}
			return true;
		}
		return false;
	}
	
	//支持Sharding
	@Override
	public long insertAndReturnId(String sql) {

//		if (sql == null || "".equals(sql)) return -2L;
		if (sql == null || "".equals(sql)) return -1L; //2.4.0

		initRoute(SuidType.INSERT, null, sql);  //entityClass在context会设置

		int num = 0;
		long returnId = -1L;
		Connection conn = null;
		PreparedStatement pst = null;
		boolean hasException = false;
		ResultSet rsKey=null;
		try {
			conn = getConn();
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			String pkName = (String) OneTimeParameter.getAttribute(StringConst.PK_Name_For_ReturnId);
			if (StringUtils.isBlank(pkName)) pkName = "id";
			pst = conn.prepareStatement(exe_sql, pkName.split(","));
			setPreparedValues(pst, sql);
			num = pst.executeUpdate();

			rsKey = pst.getGeneratedKeys();
			rsKey.next();
			returnId = rsKey.getLong(1); //主键字段没值时,可能会报异常
		} catch (SQLException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} finally {
			closeRs(rsKey);
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

	private static final int JsonType=2;
	/**
	 * @since  1.1
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String selectJson(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		if(sql==null || "".equals(sql.trim())) return null;
		
		if (isSimpleMode()) { //无分片
			return _selectJson(sql,entityClass);
		} else { //有分片
			if (HoneyContext.getSqlIndexLocal() == null) { //有分片的主线程
				
				String cacheValue=_selectJson(sql,entityClass);  //检测缓存的
				if(cacheValue!=null) {
					logDsTab();
					return cacheValue;
				}
				
				try {
				JsonResultWrap wrap;
				if (ShardingUtil.hadAvgSharding()) {
					wrap= (JsonResultWrap) new ShardingGroupbyListStringArrayEngine().asynProcess(sql, this, entityClass,3);
				} else  {
					wrap = new ShardingSelectJsonEngine().asynProcess(sql, this,JsonType,entityClass); 
				}
				
				logSelectRows(wrap.getRowCount());
				String json =wrap.getResultJson();
				addInCache(sql, json, -1); // 没有作最大结果集判断
				
				return json;
				
				}finally {
				   clearContext(sql); //2.2 分片的主线程都要清主线程的上下文 
				}
			}else { // 子线程执行
				return _selectJson(sql,entityClass);
			}
		}
	}	
	
	@SuppressWarnings("rawtypes")
	private String _selectJson(String sql, final Class entityClass) {
//		if(sql==null || "".equals(sql.trim())) return null;
		
		boolean isReg = updateInfoInCache(sql, "StringJson", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, entityClass, sql);
			Object cacheObj = getCache().get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				return (String) cacheObj;
			}
		}
		if(isShardingMain()) return null; //sharding时,主线程没有缓存就返回.
		
		String json="";
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
			
			JsonResultWrap wrap = TransformResultSet.toJson(rs, entityClass);
			json = wrap.getResultJson();
			logSelectRows(wrap.getRowCount());  // 这里的日志,是容易输出,但从缓存取,则计算不了,是多少行.
			
			addInCache(sql, json, -1); // 没有作最大结果集判断

		} catch (SQLException e) {
			hasException = true;  //fixbug  2021-05-01
			throw ExceptionHelper.convert(e);
		} finally {
			closeRs(rs);
			clearContext(sql);
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}

		return json;
	}

	@Override
	public int batch(String sql[]) {
		if(sql==null) return -1;
		int batchSize = HoneyConfig.getHoneyConfig().insertBatchSize;

		return batch(sql,batchSize);
	}
	
	@Override
	public int batch(String sql[], int batchSize) {

		if (sql == null || sql.length < 1) return -1;

		if (HoneyUtil.isMysql()) return batchForMysql(sql, batchSize);

		initRoute(SuidType.INSERT, null, sql[0]);

		int len = sql.length;
		int total = 0;
		int temp = 0;

		Connection conn = null;
		PreparedStatement pst = null;
		boolean oldAutoCommit = false;
		boolean hasException = false;
		boolean first=false;
		boolean last=false;
		
		try {
			conn = getConn();
			oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql[0]);
			pst = conn.prepareStatement(exe_sql);
			

			if (len <= batchSize) {
				first=true;
				total = batch(sql[0], 0, len, conn, pst);
			} else {
				for (int i = 0; i < len / batchSize; i++) {
					int start=i * batchSize;
					int end=(i + 1) * batchSize;
					try {
						temp = batch(sql[0], start, end, conn, pst);
						total += temp;
					} catch (SQLException e) {
						hasException = true; // finally要用到
						if (catchModifyDuplicateException(e)) {
							//do not return in batch loop
							String affectNum="?";
							if(HoneyUtil.isOracle() || HoneyUtil.isSQLite() || HoneyUtil.isSqlServer()) affectNum="0";
							Logger.logSQL(
									" | <-- index[" + (start) + "~" + (end - 1) + INDEX3
											+ " Affected rows: " + affectNum + "  , this batch have exception !",
									"" + shardingIndex());
						    if(HoneyUtil.isH2()) Logger.logSQL("the number of affected rows is inaccurate !");
						} else {//不捕获,则重新抛出异常
							throw new SQLException(e);
						}
					} finally {
						pst.clearBatch(); // clear Batch
						pst.clearParameters();
					}
				} //end for

				if (len % batchSize != 0) { // 尾数不成批
					last=true;
					temp = batch(sql[0], len - (len % batchSize), len, conn, pst);
					total += temp;
				}
			}
			
			allCommitIfNeed(conn);
			
		} catch (SQLException e) {
			hasException=true;
			if(catchModifyDuplicateException(e)) {
				if(first || last) {
					String flag="last";
					if(first) flag="first";
					int start=len - (len % batchSize);
					int end=len;
					String affectNum="?";
					if(HoneyUtil.isOracle() || HoneyUtil.isSQLite() || HoneyUtil.isSqlServer()) affectNum="0";
					Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3
							+ " Affected rows: "+ affectNum + "  , the " + flag + " batch have exception !",
							"" + shardingIndex());
					if(HoneyUtil.isH2()) Logger.logSQL("the number of affected rows is inaccurate!");
				}
				logAffectRow(total);
				return total;  //外层try,处理异常后就会返回
			} else {
				throw ExceptionHelper.convert(e);
			}
		} finally {
//			bug :Lock wait timeout exceeded; 
//			如果分批处理时有异常,如主键冲突,则又没有提交,就关不了连接.
//			所以需要先将提交改回原来的状态.
//			要是原来就是自动提交,报异常,  也要关
			try {
				if (conn != null) conn.setAutoCommit(oldAutoCommit);
			} catch (Exception e3) {
				// ignore
				Logger.debug(e3.getMessage());
			}
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}

			clearContext(sql[0],batchSize,len);
			// 更改操作需要清除缓存
			clearInCache(sql[0], "int[]", SuidType.INSERT, total);
		}
		logAffectRow(total);
		return total;
	}

	private static final String INDEX3 = "]";
	private static final String INSERT_ARRAY_SQL = " insert[] SQL : ";

	private int batch(String sql, int start, int end, Connection conn, PreparedStatement pst)
			throws SQLException {
		int a = 0;
		for (int i = start; i < end; i++) { // start... (end-1)

			if (showSQL) {
				if (i == 0) Logger.logSQL(INSERT_ARRAY_SQL, sql);
				OneTimeParameter.setAttribute("_SYS_Bee_BatchInsert", i + "");
				String sql_i;
				sql_i = INDEX1 + i + INDEX2 +shardingIndex()+ sql;//V2.2
				Logger.logSQL(INSERT_ARRAY_SQL, sql_i);
			}
			setAndClearPreparedValues(pst, INDEX1 + i + INDEX2 +shardingIndex()+ sql);//V2.2
			pst.addBatch();
		}
		//同一批次的,有部分记录有像主键重复之类的,就会在此句抛异常. 同一批次的,H2 可以部分插入成功,MySQL却不可以
		int array[] = pst.executeBatch(); // oracle will return [-2,-2,...,-2]

		if (HoneyUtil.isOracle()) {
//			int array[]=pst.executeBatch();  //不能放在此处.executeBatch()是都要运行的
			a = pst.getUpdateCount();// oracle is ok. but mysql will return 1 alway.So mysql use special branch.
		} else {
			a = countFromArray(array);
		}
		
		eachBatchCommitIfNeed(conn); //if need

		Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: ",
				a + "" + shardingIndex());

		return a;
	}
	
	private void eachBatchCommitIfNeed(Connection conn) throws SQLException {
		boolean eachBatchCommit = HoneyConfig.getHoneyConfig().eachBatchCommit;
		if (eachBatchCommit && !HoneyContext.isTransactionConn()) {
//			System.err.println("-----在方法内部每个批次提交一次----");
			conn.commit();
		}
	}
	
	
	private void allCommitIfNeed(Connection conn) throws SQLException {
		boolean eachBatchCommit = HoneyConfig.getHoneyConfig().eachBatchCommit;
		if (!eachBatchCommit && !HoneyContext.isTransactionConn()) {
//			System.err.println("-----在方法内部所有批次一起提交----");
			conn.commit();
		}
	}
	
	
	private int countFromArray(int array[]){
		int a=0;
		if(array==null) return a;
		for (int i=0; i < array.length; i++) {
			a+=array[i];
		}
		return a;
	}
	
	private int batchForMysql(String sql[], final int batchSize) {

		if (sql == null || sql.length < 1) return -1;

		initRoute(SuidType.INSERT, null, sql[0]);

		int len = sql.length;
		int total = 0;
		int temp = 0;
		//一条插入语句的值的占位符
		String placeholderValue = (String) OneTimeParameter.getAttribute("_SYS_Bee_PlaceholderValue");

		Connection conn = null;
		PreparedStatement pst = null;
		boolean oldAutoCommit=false;
		boolean hasException = false;
		boolean last=false;
		boolean first=false;
		
		try {
			conn = getConn();
			oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql[0]);

			String batchExeSql[];

			if (len <= batchSize) {
				first=true;
				batchExeSql = getBatchExeSql(exe_sql, len, placeholderValue); //batchExeSql[1] : ForPrint
				pst = conn.prepareStatement(batchExeSql[0]);

				total = _batchForMysql(sql[0], 0, len, conn, pst, batchSize, batchExeSql[1]);
			} else {
				batchExeSql = getBatchExeSql(exe_sql, batchSize, placeholderValue);
				pst = conn.prepareStatement(batchExeSql[0]);
				
				for (int i = 0; i < len / batchSize; i++) {
					int start=i * batchSize;
					int end=(i + 1) * batchSize;
					try {
						temp = _batchForMysql(sql[0], start, end, conn, pst, batchSize, batchExeSql[1]); //not executeBatch
						total += temp;
					} catch (SQLException e) {
						hasException = true; // finally要用到
						if (catchModifyDuplicateException(e)) {
							//do not return in batch loop
							Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: 0  , this batch have exception !","" + shardingIndex());
						} else {
							throw new SQLException(e);
						}
					}
				}//end for

				if (len % batchSize != 0) { //尾数不成批
					last=true;
					batchExeSql = getBatchExeSql(exe_sql, (len % batchSize), placeholderValue);  //最后一批,getBatchExeSql返回的语句可能不一样
					pst = conn.prepareStatement(batchExeSql[0]);   //fixed bug
					temp = _batchForMysql(sql[0], len - (len % batchSize), len, conn, pst, batchSize, batchExeSql[1]);
					total += temp;
				}
			}
			
			allCommitIfNeed(conn);
			
//			conn.setAutoCommit(oldAutoCommit);
		} catch (SQLException e) {
			hasException=true;
			
			if(catchModifyDuplicateException(e)) {
				if(first || last) {
					String flag="last";
					if(first) flag="first";
					int start=len - (len % batchSize);
					int end=len;
					Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: 0  , the "+flag+" batch have exception !","" + shardingIndex());
				}
				logAffectRow(total);
				return total; //外层try,处理异常后就会返回
			} else {
				throw ExceptionHelper.convert(e);
			}
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
			
			clearContextForMysql(sql[0],batchSize,len);
			//更改操作需要清除缓存
			clearInCache(sql[0], "int[]", SuidType.INSERT,total);
		}
		logAffectRow(total);
		return total;
	}
	
	private void clearContextForMysql(String sql_0, final int batchSize, final int len) {
		
		clearContext(sql_0, batchSize, len);

		int num = (len - 1) / batchSize;
		for (int i = 0; i <= num; i++) {
			String sqlForGetValue = shardingIndex() + sql_0 + "  [Batch:" + i + INDEX3; //fixed bug V2.2
			clearContext(sqlForGetValue);
		}
	}
	
	private int _batchForMysql(String sql, int start, int end, Connection conn, PreparedStatement pst,int batchSize,String batchSqlForPrint) throws SQLException {
//		sql用于获取转换成获取占位的sqlKey和打印log. 
//		v1.8  打印的sql是单行打印；执行的是批的形式.
		if (showSQL) {
			//print log
			if(start==0 || (end-start!=batchSize)) {
//				if(batchSize==1) OneTimeParameter.setTrueForKey("_SYS_Bee_BatchInsertFirst");
				Logger.logSQL(INSERT_ARRAY_SQL, batchSqlForPrint);
			}
			
			for (int i = start; i < end; i++) { //start... (end-1)
				OneTimeParameter.setAttribute("_SYS_Bee_BatchInsert", i + "");
				String sql_i;
//				if (i == 0)
//					sql_i = sql;
//				else
					sql_i = INDEX1 + i + INDEX2 +shardingIndex() + sql;

				Logger.logSQL(INSERT_ARRAY_SQL, sql_i);
			}
		}
		
		int a = 0;
		String sqlForGetValue=shardingIndex() +sql+ "  [Batch:"+ (start/batchSize) + INDEX3; //V2.2
		setAndClearPreparedValues(pst, sqlForGetValue);
		a = pst.executeUpdate();  // not executeBatch
		
		eachBatchCommitIfNeed(conn); //if need
		
		Logger.logSQL(" | <-- [Batch:"+ (start/batchSize) + INDEX3+" Affected rows: ", a+""+shardingIndex());

		return a;
	}
	
	private String[] getBatchExeSql(String sql0, final int size, String placeholderValue) {
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
	
	private void closeRs(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				//ignore
			}
		}
	}
	
	protected void closeConn(Connection conn) {
		HoneyContext.closeConn(conn);
	}
	
	@Override
	public <T> List<T> moreTableSelect(String sql, final T entity) {
		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();
		
		if (isSimpleMode()) {
			return _moreTableSelect(sql, entity); // 1.x版本及不用分片走的分支
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {
				List<T> list =_moreTableSelect(sql, entity); //检测缓存的
				if (list != null) {
					logDsTab();
					return list; 
				}
				
				try {
				//rsList还要排序
				List<T> rsList =new ShardingMoreTableSelectEngine().asynProcess(sql, entity, this);
				
				addInCache(sql, rsList, rsList.size());
				return rsList;
				
				}finally {
				   clearContext(sql); //2.2 分片的主线程都要清主线程的上下文 
				}
				
			} else { // 子线程执行
				return _moreTableSelect(sql, entity);
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> List<T> _moreTableSelect(String sql, final T entity) {
		
		if(sql==null || "".equals(sql.trim())) return Collections.emptyList();
		
		MoreTableStruct moreTableStruct[]=HoneyUtil.getMoreTableStructAndCheckBefore(entity);
		
//		不经过MoreTable,直接传入sql,需要重新生成结构  V1.11
		if(moreTableStruct==null) {
			OneTimeParameter.setTrueForKey(StringConst.MoreStruct_to_SqlLib);
			moreTableStruct=HoneyUtil.getMoreTableStructAndCheckBefore(entity);
		}
		
		boolean subOneIsList1=moreTableStruct[0].subOneIsList;
		boolean subTwoIsList2=moreTableStruct[0].subTwoIsList;
		String listFieldType=""+subOneIsList1+subTwoIsList2+moreTableStruct[0].oneHasOne;
		boolean isReg = updateInfoInCache(sql, "List<T>" + listFieldType, SuidType.SELECT, entity.getClass());
		if (isReg) {
//			listFieldTypeForMoreTable=listFieldType; //for sharding. 主线程才会注册
			initRoute(SuidType.SELECT, entity.getClass(), sql); //多表查询的多个表要在同一个数据源.
			Object cacheObj = getCache().get(sql); //这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				
				List<T> list=(List<T>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}
		if(isShardingMain()) return null; //sharding时,主线程没有缓存就返回.
		
		Connection conn=null;
		PreparedStatement pst=null;
		ResultSet rs=null;
		T targetObj=null;
		List<T> rsList=null;
		boolean hasException = false;
		int recordRow=0;
		try {
			conn = getConn();
			String exe_sql=HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);
			
			setPreparedValues(pst, sql);

			rs = pst.executeQuery();
			rsList = new ArrayList<>();

			Field field[] = HoneyUtil.getFields(entity.getClass());
			int columnCount = field.length;
			
//			MoreTableStruct moreTableStruct[]=HoneyUtil.getMoreTableStructAndCheckBefore(entity);
			boolean oneHasOne=moreTableStruct[0].oneHasOne;
			
			Field subField[] = new Field[2];
			String subUseTable[]=new String[2];
			String variableName[]=new String[2];
			Class subEntityFieldClass[]=new Class[2];
			for (int i = 1; i <= 2; i++) {
				if(moreTableStruct[i]!=null){
					subField[i-1]=moreTableStruct[i].subEntityField;
					variableName[i-1]=subField[i-1].getName();
					if (subOneIsList1 && i==1) {
						subEntityFieldClass[0]=moreTableStruct[1].subClass;  //v1.9.8 List Field
					} else if (subTwoIsList2 && i==2) {
						subEntityFieldClass[1]=moreTableStruct[2].subClass;  //v1.9.8 List Field
					}else {
						subEntityFieldClass[i - 1] = subField[i - 1].getType();
					}
//					if(moreTableStruct[i].hasSubAlias){
//						subUseTable[i-1]=moreTableStruct[i].subAlias;
//					}else{
//						subUseTable[i-1]=moreTableStruct[i].tableName;
//					}
					subUseTable[i-1]=moreTableStruct[i].useSubTableName;
				}
			}
			
			Field fields1[] = HoneyUtil.getFields(subEntityFieldClass[0]);
			Field fields2[] =null;
			
            if(subField[1]!=null){
            	fields2=HoneyUtil.getFields(subEntityFieldClass[1]);
            }
            
            Map<String,String> dulSubFieldMap=moreTableStruct[0].subDulFieldMap;
            
            boolean sub1_first=true;
            boolean sub2_first=true;
            
            Object v1=null;
            Object v2=null;
            
            Map<String,List> subOneMap=null;
            Map<String,List> subTwoMap=null;
            
            if(subOneIsList1) subOneMap=new HashMap<>();
            if(subTwoIsList2) subTwoMap=new HashMap<>();
            
            StringBuffer checkKey=null;
            StringBuffer checkKey2ForOneHasOne=null;
			
//			String tableName=_toTableName(entity);
			String tableName=moreTableStruct[0].tableName;
		
		while (rs.next()) {
			    recordRow++;
				boolean isDul=false;
				String dulField="";
				
				//从表2设置(如果有)  先设置,因oneHasOne时,设置从表1已经要用从表2了.
				sub2_first=true;
				Object subObj2=null;
				if(subField[1]!=null){
//					 subObj2 = subEntityFieldClass[1].newInstance();
					String columnName="";
					for (int i = 0; i < fields2.length; i++) {
						
						if(HoneyUtil.isSkipField(fields2[i])) continue;
						
						boolean isRegHandlerPriority2 = false;
						if (openFieldTypeHandler) {
							isRegHandlerPriority2 = TypeHandlerRegistry.isPriorityType(fields2[i].getType());
						}
						
						v2=null;
						HoneyUtil.setAccessibleTrue(fields2[i]);
						isDul=false;
						dulField="";
						try {
							columnName=_toColumnName(fields2[i].getName(),subEntityFieldClass[1]);
							//get v2
							if(isConfuseDuplicateFieldDB()){
								dulField=dulSubFieldMap.get(subUseTable[1]+"."+columnName);
								if(dulField!=null){
									isDul=true;  //set true first
									v2 = rs.getObject(dulField);
								}else{
									v2= rs.getObject(columnName);
								}
							} else {
								v2= rs.getObject(subUseTable[1] + "." + columnName);
							}
							
							boolean processAsJson = false;
							if (isJoson(fields2[i])) {
								TypeHandler jsonHandler = TypeHandlerRegistry.getHandler(Json.class);
								if (jsonHandler != null) {
//									v2 = jsonHandler.process(fields2[i].getType(), v2);
									v2 = jsonHandlerProcess(fields2[i], v2, jsonHandler);
									processAsJson = true;
								}
							}

							if (!processAsJson && isRegHandlerPriority2) { //process v2 by handler
								v2 = TypeHandlerRegistry.handlerProcess(fields2[i].getType(), v2);
							}
							
							if (v2 != null) {
								if (sub2_first) {
									subObj2 = createObject(subEntityFieldClass[1]);
									sub2_first = false;
								}
								HoneyUtil.setFieldValue(fields2[i], subObj2, v2);
							}
							
						} catch (IllegalArgumentException e) {
							//get v2
							if(isConfuseDuplicateFieldDB()){
								v2=_getObjectForMoreTable_ConfuseField(rs,fields2[i],isDul,dulField,subEntityFieldClass[1]);  //todo
							}else{
								v2=_getObjectForMoreTable_NoConfuse(rs,subUseTable[1],fields2[i],subEntityFieldClass[1]);
							}
							
							boolean alreadyProcess = false;
							try {
								if (openFieldTypeHandler) {//process v2 by handler
									Class type = fields2[i].getType();
									TypeHandler handler = TypeHandlerRegistry.getHandler(type);
									if (handler != null) {
										Object newV2 = handler.process(type, v2);//process v2 by handler
										if (newV2 != null) {
											if (sub2_first) {
												subObj2 = createObject(subEntityFieldClass[1]);
												sub2_first = false;
											}
											HoneyUtil.setFieldValue(fields2[i], subObj2, newV2);
											alreadyProcess = true;
										}
									}
								}
							} catch (Exception e2) {
								alreadyProcess = false;
							}
							
							if (!alreadyProcess && v2 != null) {
								if (sub2_first) {
									subObj2 = createObject(subEntityFieldClass[1]);
									sub2_first = false;
								}
								HoneyUtil.setFieldValue(fields2[i], subObj2, v2);
							}
						}catch (SQLException e) {// for after use condition selectField method
//							fields2[i].set(subObj2,null);
						}
					}
				}
				
				sub1_first=true;
				Field subField2InOneHasOne=null;
				if(oneHasOne) checkKey2ForOneHasOne=new StringBuffer();
				//从表1设置
				Object subObj1 = subEntityFieldClass[0].newInstance();
//				Object subObj1 =null; //不行.   当它的从表在第1位时,就报null
				
				for (int i = 0; i < fields1.length; i++) {

					if (oneHasOne) {
						if (HoneyUtil.isSkipFieldForMoreTable(fields1[i])) continue; //从表1也有1个从表
					} else {
						if (HoneyUtil.isSkipField(fields1[i])) continue;
					}

					boolean isRegHandlerPriority1 = false;
					if (openFieldTypeHandler) {
						isRegHandlerPriority1 = TypeHandlerRegistry.isPriorityType(fields1[i].getType());
					}
					v1 = null;
					HoneyUtil.setAccessibleTrue(fields1[i]);
					isDul = false;
					dulField = "";
					try {

						if (oneHasOne && fields1[i] != null && fields1[i].isAnnotationPresent(JoinTable.class)) {
							if (subField[1] != null && fields1[i].getName().equals(variableName[1]) && subObj2 != null) {
								HoneyUtil.setAccessibleTrue(fields1[i]);
								if (subTwoIsList2) {
									subField2InOneHasOne = fields1[i];
								} else {
									HoneyUtil.setFieldValue(fields1[i], subObj1, subObj2); //设置子表2的对象     要考虑List. 
								}

								if (sub1_first) {
									sub1_first = false;
								}
							}
							continue; // go back
						}

						String columnName = _toColumnName(fields1[i].getName(), subEntityFieldClass[0]);
						//get v1
						if (isConfuseDuplicateFieldDB()) {
							dulField = dulSubFieldMap.get(subUseTable[0] + "." + columnName);
							if (dulField != null) {
								isDul = true; //fixed bug.  need set true before fields1[i].set(  )
								v1 = rs.getObject(dulField);
							} else {
								v1 = rs.getObject(columnName);
							}
						} else {
							v1 = rs.getObject(subUseTable[0] + "." + columnName);
						}
						
						boolean processAsJson = false;
						if (isJoson(fields1[i])) {
							TypeHandler jsonHandler = TypeHandlerRegistry.getHandler(Json.class);
							if (jsonHandler != null) {
//								v1 = jsonHandler.process(fields1[i].getType(), v1);
								v1 = jsonHandlerProcess(fields1[i], v1, jsonHandler);
								processAsJson = true;
							}
						}

						if (!processAsJson && isRegHandlerPriority1) { //process v1 by handler
							v1 = TypeHandlerRegistry.handlerProcess(fields1[i].getType(), v1);
						}

						if (v1 != null) {
							if (sub1_first) {
								sub1_first = false;
							}
							HoneyUtil.setFieldValue(fields1[i], subObj1, v1);
						}
					} catch (IllegalArgumentException e) {
						if(isConfuseDuplicateFieldDB()){
							v1=_getObjectForMoreTable_ConfuseField(rs,fields1[i],isDul,dulField,subEntityFieldClass[0]);
						}else{
							v1=_getObjectForMoreTable_NoConfuse(rs,subUseTable[0],fields1[i],subEntityFieldClass[0]);
						}
						
						
						boolean alreadyProcess = false;
						try {
							if (openFieldTypeHandler) {//process v1 by handler
								Class type = fields1[i].getType();
								TypeHandler handler = TypeHandlerRegistry.getHandler(type);
								if (handler != null) {
									Object newV1 = handler.process(type, v1);//process v1 by handler
									if (newV1 != null) {
										if (sub1_first) {
											sub1_first = false;
										}
										HoneyUtil.setFieldValue(fields1[i], subObj1, newV1);
										alreadyProcess=true;
									}
								}
							}
						} catch (Exception e2) {
							alreadyProcess = false;
						}
						
						if (!alreadyProcess && v1 != null) {
							if (sub1_first) {
								sub1_first = false;
							}
							HoneyUtil.setFieldValue(fields1[i], subObj1, v1);
						}
					}catch (SQLException e) {// for after use condition selectField method
//						fields1[i].set(subObj1,null);
					}
					
					if(oneHasOne) checkKey2ForOneHasOne.append(v1);
				}   // end for fields1
				
//				if(sub1_first) subObj1=null;  //没有创建过,设置为null
				if(sub1_first && (!oneHasOne || (oneHasOne && sub2_first))) subObj1=null;  //没有创建过,设置为null(是oneHasOne时,子表1里的子表2也是null才行)
				
				
//				Integer id=null;    //配置一个主键是id的项,即可用,效率也高些    行. 有可能只查几个字段
				checkKey=new StringBuffer();   
				Field subOneListField=null;
				Field subTwoListField=null;
				
				//主表设置  oneHasOne can not set here
				targetObj = (T) entity.getClass().newInstance();
				for (int i = 0; i < columnCount; i++) {
//					if("serialVersionUID".equals(field[i].getName()) || field[i].isSynthetic()) continue;
					if(HoneyUtil.isSkipFieldForMoreTable(field[i])) continue;  //有Ignore注释,将不再处理JoinTable
					if (field[i]!= null && field[i].isAnnotationPresent(JoinTable.class)) {
						HoneyUtil.setAccessibleTrue(field[i]);
						if(field[i].getName().equals(variableName[0])){
							if(subOneIsList1) subOneListField=field[i];  //子表1字段是List
							else HoneyUtil.setFieldValue(field[i], targetObj,subObj1); //设置子表1的对象
						}else if(!oneHasOne && subField[1]!=null && field[i].getName().equals(variableName[1])){
							//oneHasOne在遍历子表1时设置
							if(subTwoIsList2) subTwoListField=field[i];  
							else HoneyUtil.setFieldValue(field[i], targetObj,subObj2); //设置子表2的对象
						}
						continue;  // go back
					}
					
					boolean isRegHandlerPriority = false;
					if (openFieldTypeHandler) {
						isRegHandlerPriority = TypeHandlerRegistry.isPriorityType(field[i].getType());
					}

					HoneyUtil.setAccessibleTrue(field[i]);
					Object v = null;

					try {
						//get v
						if (isConfuseDuplicateFieldDB()) {
							v = rs.getObject(_toColumnName(field[i].getName(), entity.getClass()));
						} else {
							try {
							    v = rs.getObject(tableName + "."+ _toColumnName(field[i].getName(), entity.getClass()));
							} catch (SQLException e) {
								v = rs.getObject( _toColumnName(field[i].getName(), entity.getClass()));//condition.selectFun(FunctionType.COUNT, "*", "count1"); //像这种不带表名
							}
						}
						
						boolean processAsJson = false;
						if (isJoson(field[i])) {
							TypeHandler jsonHandler = TypeHandlerRegistry.getHandler(Json.class);
							if (jsonHandler != null) {
//								v = jsonHandler.process(field[i].getType(), v);
								v = jsonHandlerProcess(field[i], v, jsonHandler);
								processAsJson = true;
							}
						}

						if (!processAsJson && isRegHandlerPriority) { //process v by handler
							v = TypeHandlerRegistry.handlerProcess(field[i].getType(), v);
						}

						HoneyUtil.setFieldValue(field[i], targetObj, v);
						checkKey.append(v);
					} catch (IllegalArgumentException e) {
						v = _getObjectForMoreTable(rs, tableName, field[i], entity.getClass());

						boolean alreadyProcess = false;
						try {
							if (openFieldTypeHandler) {//process v by handler
								Class type = field[i].getType();
								TypeHandler handler = TypeHandlerRegistry.getHandler(type);
								if (handler != null) {
									Object newV = handler.process(type, v);//process v by handler
									HoneyUtil.setFieldValue(field[i], targetObj, newV);
									alreadyProcess = true;
								}
							}
						} catch (Exception e2) {
							alreadyProcess = false;
						}
						
						if (!alreadyProcess) HoneyUtil.setFieldValue(field[i], targetObj, v);
						
					} catch (SQLException e) { // for after use condition selectField method
						HoneyUtil.setFieldValue(field[i], targetObj, null);
					}
					
				} //end for
				
				
				if(oneHasOne) checkKey2ForOneHasOne.insert(0, checkKey); //主表+从表1
				if(subTwoIsList2 && oneHasOne && subObj1!=null && subField2InOneHasOne!=null) { //for oneHasOne List   oneHasOne 或者 两个都在主表,只会存在其中一种
					List subTwoList = subTwoMap.get(checkKey2ForOneHasOne.toString());  //需要等从表1遍历完,等到完整checkKey2ForOneHasOne
					if (subTwoList == null) { //表示,还没有添加该行记录
						subTwoList=new ArrayList();
						subTwoList.add(subObj2);
//						subField2InOneHasOne.set(subObj1, subTwoList);  //subObj1
						HoneyUtil.setFieldValue(subField2InOneHasOne, subObj1, subTwoList);  //subObj1
						subTwoMap.put(checkKey2ForOneHasOne.toString(), subTwoList);
						
//						rsList.add(targetObj);
					} else {
						subTwoList.add(subObj2);
					}
				}
				
				//全是null的数据不会到这里,所以不用判断
				if (subOneIsList1 && subObj1!=null) { //子表1是List类型字段
					List subOneList = subOneMap.get(checkKey.toString());  //需要等主表遍历完,等到完整checkKey
					if (subOneList == null) { //表示主表,还没有添加该行记录
						subOneList=new ArrayList();
						subOneList.add(subObj1);
//						subOneListField.set(targetObj, subOneList);
						HoneyUtil.setFieldValue(subOneListField, targetObj, subOneList);
						subOneMap.put(checkKey.toString(), subOneList);
						
						rsList.add(targetObj);
					} else {
						if(!oneHasOne) 
							subOneList.add(subObj1);
						else if(subObj2==null)
							subOneList.add(subObj1);
					}
				}else if(subTwoIsList2 && !oneHasOne && subObj2!=null) {
					List subTwoList = subTwoMap.get(checkKey.toString());  //需要等主表遍历完,等到完整checkKey
					if (subTwoList == null) { //表示主表,还没有添加该行记录
						subTwoList=new ArrayList();
						subTwoList.add(subObj2);
//						subTwoListField.set(targetObj, subTwoList);
						HoneyUtil.setFieldValue(subTwoListField, targetObj, subTwoList);
						subTwoMap.put(checkKey.toString(), subTwoList);
						
						rsList.add(targetObj);
					} else {
						subTwoList.add(subObj2);
					}
				}else {
					rsList.add(targetObj);
				}
				
				
			} //end  while (rs.next())
		
			addInCache(sql, rsList, rsList.size());
			
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
			closeRs(rs);
			clearContext(sql);
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
			targetObj = null;
		}
		
//		子表是List类型时，要连原始数据行数也打印日志
		if(subOneIsList1 || subTwoIsList2)
		   Logger.logSQL(" | <--  ( select raw record rows: ", recordRow + " )");
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
			Field f=list.get(i).getField();
			if(f!=null && f.isAnnotationPresent(Json.class)) {
					//Json annotation key is : 26
					HoneyUtil.setPreparedValues(pst, 26, i, list.get(i).getValue()); //i from 0	
			}else {
				int k = HoneyUtil.getJavaTypeIndex(list.get(i).getType());
				HoneyUtil.setPreparedValues(pst, k, i, list.get(i).getValue()); //i from 0	
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	private Object _getObjectForMoreTable(ResultSet rs, String tableName, Field field,
			Class entityClass) throws SQLException {
		if (isConfuseDuplicateFieldDB()) {// 主表时会用到
			return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName(), entityClass));
		} else {
			try {
				return HoneyUtil.getResultObject(rs, field.getType().getName(), tableName + "." + _toColumnName(field.getName(), entityClass));
			} catch (SQLException e) {
				return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName(), entityClass)); // no table name, 不带表名
			}
		}
	}
	
	// not  oracle,SQLite
	@SuppressWarnings("rawtypes")
	private Object _getObjectForMoreTable_NoConfuse(ResultSet rs, String tableName, Field field,
			Class entityClass) throws SQLException {
		try {
			return HoneyUtil.getResultObject(rs, field.getType().getName(), tableName + "." + _toColumnName(field.getName(), entityClass));
		} catch (SQLException e) {
			return HoneyUtil.getResultObject(rs, field.getType().getName(),  _toColumnName(field.getName(), entityClass));  // no table name, 不带表名
		}
	}
	
	//oracle,SQLite
	@SuppressWarnings("rawtypes")
	private Object _getObjectForMoreTable_ConfuseField(ResultSet rs, Field field, boolean isDul, String otherName,Class entityClass) throws SQLException {

		if (isDul) return HoneyUtil.getResultObject(rs, field.getType().getName(), otherName);

		return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName(),entityClass));
	}
	
	private boolean isShardingMain() {//有分片(多个)
		return   HoneyContext.getSqlIndexLocal() == null && ShardingUtil.hadSharding(); //前提要是HoneyContext.hadSharding()
	}
	
}
