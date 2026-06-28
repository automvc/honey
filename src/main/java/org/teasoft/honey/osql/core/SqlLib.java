/*
 * Copyright 2013-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.ResultAssemblerRegistry;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.honey.logging.Logger;
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
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * 直接操作数据库，并返回结果.在该类中的sql字符串要是DB能识别的SQL语句 Directly operate the database and
 * return the result. <br>
 * The SQL string in this class should be an SQL statement recognized by DB.
 * 
 * @author Kingstar Create on 2013-6-30 下午10:32:53
 * @since 1.0
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
				List<T> list = _selectSomeField(sql, entityClass); // 检测缓存的
				if (list != null) {// 若缓存是null,就无法区分了,所以没有数据,最好是返回空List,而不是null
					logDsTab();
					return list;
				}
				try {
					List<T> rsList;
					boolean jdbcStreamSelect = HoneyConfig.getHoneyConfig().sharding_jdbcStreamSelect;

					if (ShardingUtil.hadAvgSharding()) {
						int List_T = 2;
						rsList = (List<T>) new ShardingGroupbyListStringArrayEngine().asynProcess(sql, this, entityClass, List_T);
					} else if (jdbcStreamSelect && !ShardingUtil.hadGroupSharding()) {
						rsList = new ShardingSelectRsEngine().asynProcess(sql, entityClass, this); // 无结果集时,可能会报错 fixed V2.1
					} else {
						rsList = new ShardingSelectEngine().asynProcess(sql, entityClass, this);
					}
					addInCache(sql, rsList, rsList.size()); // 缓存Key,是否包括了分片的DS,Tables
					logSelectRows(rsList.size());

					return rsList;
				} finally {
					clearContext(sql); // 2.2 分片的主线程都要清主线程的上下文
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
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<T> list = (List<T>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}
		if (isShardingMain()) return null; // sharding时,主线程没有缓存就返回.

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
				targetObj = ResultAssemblerHandler.rowToEntity(rs, entityClass);
				rsList.add(targetObj);
			}
			addInCache(sql, rsList, rsList.size());
		} catch (SQLException | IllegalAccessException | InstantiationException e) {
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
		if (sql == null || "".equals(sql.trim())) return null;
		if (isSimpleMode()) {
			return _selectFun(sql, entityClass);
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {

				String cacheValue = _selectFun(sql, entityClass); // 检测缓存的
				if (cacheValue != null) {
					logDsTab();
					return cacheValue;
				}

				try {
					String fun = "";
					String funType = HoneyContext.getSysCommStrInheritableLocal(StringConst.FunType);
					if (FunctionType.AVG.getName().equalsIgnoreCase(funType)) { // avg need change sql
						String newSql = ShardingAvgEngine.rewriteAvgSql(sql);
						HoneyContext.setPreparedValue(newSql, HoneyContext.justGetPreparedValue(sql));
						List<String[]> rsList = new ShardingSelectListStringArrayEngine().asynProcess(newSql, this, entityClass);
						fun = ShardingAvgEngine.avgResultEngine(rsList);
						clearContext(newSql);
					} else {
						fun = new ShardingSelectFunEngine().asynProcess(sql, this, entityClass);
					}

					addInCache(sql, fun, 1);

					return fun;
				} finally {
					clearContext(sql); // 2.2 分片的主线程都要清主线程的上下文
				}

			} else { // 子线程执行
				return _selectFun(sql, entityClass);
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
						rsList = (List<String[]>) new ShardingGroupbyListStringArrayEngine().asynProcess(sql, this,
								entityClass, List_String_Array);
					} else {
						rsList = new ShardingSelectListStringArrayEngine().asynProcess(sql, this, entityClass);
					}

					addInCache(sql, rsList, rsList.size());

					return rsList;

				} finally {
					clearContext(sql); // 2.2 分片的主线程都要清主线程的上下文
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
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<String[]> list = (List<String[]>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}
		if (isShardingMain()) return null; // sharding时,主线程没有缓存就返回.

		List<String[]> list = new ArrayList<>();
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
			list = TransformResultSet.toStringsList(rs);

			logSelectRows(list.size());
			addInCache(sql, list, list.size());

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

		return list;
	}

	// do not support sharding
	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> selectMapList(String sql) {
		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();

		boolean isReg = updateInfoInCache(sql, "List<Map<String,Object>>", SuidType.SELECT, null);
		if (isReg) { // V1.9还未使用
			initRoute(SuidType.SELECT, null, sql);
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<Map<String, Object>> list = (List<Map<String, Object>>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}

		List<Map<String, Object>> list = new ArrayList<>();
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

			list = TransformResultSet.toMapList(rs);

			logSelectRows(list.size());

			addInCache(sql, list, list.size());

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

		return list;
	}

	// 对应jdbc的executeUpdate方法
	/*
	 * modify include insert,delete and update.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public int modify(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
//		if (sql == null || "".equals(sql)) return -2;
		if (sql == null || "".equals(sql)) return -1; // 2.4.0
//		boolean clearFlag;
		if (isSimpleMode()) {
//			clearFlag = true;
			return _modify(sql, entityClass, true); // 1.x版本及不用分片走的分支
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {// 拦截到的要分片的主线程
				try {
					int num = new ShardingModifyEngine().asynProcess(sql, entityClass, this);
					logAffectRow(num);
					clearInCache(sql, "int", SuidType.MODIFY, num); // 父线程才清缓存
					return num;
				} finally {
					clearContext(sql); // 2.2 分片的主线程都要清主线程的上下文
				}
			} else { // 子线程执行
//				clearFlag = false;
				return _modify(sql, entityClass, false);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private int _modify(String sql, final Class entityClass, boolean clearFlag) {

		initRoute(SuidType.MODIFY, entityClass, sql);

		int num = 0;
		Connection conn = null;
		PreparedStatement pst = null;
		boolean hasException = false;
		try {
			conn = getConn();
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			pst = conn.prepareStatement(exe_sql);
			setPreparedValues(pst, sql);
			num = pst.executeUpdate();
		} catch (SQLException e) {
			hasException = true; // finally要用到
			if (catchModifyDuplicateException(e))
				return num;
			else
				throw ExceptionHelper.convert(e);
		} finally {
			if (clearFlag) clearInCache(sql, "int", SuidType.MODIFY, num); // has clearContext(sql)
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
		boolean notCatch = HoneyConfig.getHoneyConfig().notCatchModifyDuplicateException;
		if (!notCatch && isConstraint(e)) { // 内部捕获并且是重复异常,则由Bee框架处理
			boolean notShow = HoneyConfig.getHoneyConfig().notShowModifyDuplicateException;
			if (!notShow) {
				Logger.warn(e.getMessage());
			} else {
				Logger.debug(e.getMessage());
			}
			return true;
		}
		return false;
	}

	// 支持Sharding
	@Override
	public long insertAndReturnId(String sql) {

//		if (sql == null || "".equals(sql)) return -2L;
		if (sql == null || "".equals(sql)) return -1L; // 2.4.0

		initRoute(SuidType.INSERT, null, sql); // entityClass在context会设置

		int num = 0;
		long returnId = -1L;
		Connection conn = null;
		PreparedStatement pst = null;
		boolean hasException = false;
		ResultSet rsKey = null;
		try {
			conn = getConn();
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			String pkName = (String) OneTimeParameter.getAttribute(StringConst.PK_Column_For_ReturnId);
			if (StringUtils.isBlank(pkName)) pkName = "id";
			pst = conn.prepareStatement(exe_sql, pkName.split(",")); // pkName要用column name.
			setPreparedValues(pst, sql);
			num = pst.executeUpdate();

			rsKey = pst.getGeneratedKeys();
			rsKey.next();
			returnId = rsKey.getLong(1); // 主键字段没值时,可能会报异常
		} catch (SQLException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} finally {
			closeRs(rsKey);
			clearInCache(sql, "int", SuidType.INSERT, num); // has clearContext(sql)
			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}
		}

		Logger.logSQL(" | <--  Affected rows: " + num);

		return returnId; // id
	}

	/**
	 * @since 1.1
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String selectJson(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		if (sql == null || "".equals(sql.trim())) return null;

		if (isSimpleMode()) { // 无分片
			return _selectJson(sql, entityClass);
		} else { // 有分片
			if (HoneyContext.getSqlIndexLocal() == null) { // 有分片的主线程

				String cacheValue = _selectJson(sql, entityClass); // 检测缓存的
				if (cacheValue != null) {
					logDsTab();
					return cacheValue;
				}

				try {
					JsonResultWrap wrap;
					if (ShardingUtil.hadAvgSharding()) {
						wrap = (JsonResultWrap) new ShardingGroupbyListStringArrayEngine().asynProcess(sql, this,
								entityClass, 3);
					} else {
						wrap = new ShardingSelectJsonEngine().asynProcess(sql, this, entityClass);
					}

					logSelectRows(wrap.getRowCount());
					String json = wrap.getResultJson();
					addInCache(sql, json, -1); // 没有作最大结果集判断

					return json;

				} finally {
					clearContext(sql); // 2.2 分片的主线程都要清主线程的上下文
				}
			} else { // 子线程执行
				return _selectJson(sql, entityClass);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private String _selectJson(String sql, final Class entityClass) {
//		if(sql==null || "".equals(sql.trim())) return null;

		boolean isReg = updateInfoInCache(sql, "StringJson", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, entityClass, sql);
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				return (String) cacheObj;
			}
		}
		if (isShardingMain()) return null; // sharding时,主线程没有缓存就返回.

		String json = "";
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

			JsonResultWrap wrap = TransformResultSet.toJson(rs, entityClass);
			json = wrap.getResultJson();
			logSelectRows(wrap.getRowCount()); // 这里的日志,是容易输出,但从缓存取,则计算不了,是多少行.

			addInCache(sql, json, -1); // 没有作最大结果集判断

		} catch (SQLException e) {
			hasException = true; // fixbug 2021-05-01
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
		if (sql == null) return -1;
		int batchSize = HoneyConfig.getHoneyConfig().insertBatchSize;

		return batch(sql, batchSize);
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
		boolean first = false;
		boolean last = false;

		try {
			conn = getConn();
			oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql[0]);
			pst = conn.prepareStatement(exe_sql);

			if (len <= batchSize) {
				first = true;
				total = batch(sql[0], 0, len, conn, pst);
			} else {
				for (int i = 0; i < len / batchSize; i++) {
					int start = i * batchSize;
					int end = (i + 1) * batchSize;
					try {
						temp = batch(sql[0], start, end, conn, pst);
						total += temp;
					} catch (SQLException e) {
						hasException = true; // finally要用到
						if (catchModifyDuplicateException(e)) {
							// do not return in batch loop
							String affectNum = "?";
							if (HoneyUtil.isOracle() || HoneyUtil.isSQLite() || HoneyUtil.isSqlServer()) affectNum = "0";
							Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: "
									+ affectNum + "  , this batch have exception !" + shardingIndex());
							if (HoneyUtil.isH2()) Logger.logSQL("the number of affected rows is inaccurate !");
						} else {// 不捕获,则重新抛出异常
							throw new SQLException(e);
						}
					} finally {
						pst.clearBatch(); // clear Batch
						pst.clearParameters();
					}
				} // end for

				if (len % batchSize != 0) { // 尾数不成批
					last = true;
					temp = batch(sql[0], len - (len % batchSize), len, conn, pst);
					total += temp;
				}
			}

			allCommitIfNeed(conn);

		} catch (SQLException e) {
			hasException = true;
			if (catchModifyDuplicateException(e)) {
				if (first || last) {
					String flag = "last";
					if (first) flag = "first";
					int start = len - (len % batchSize);
					int end = len;
					String affectNum = "?";
					if (HoneyUtil.isOracle() || HoneyUtil.isSQLite() || HoneyUtil.isSqlServer()) affectNum = "0";
					Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: " + affectNum
							+ "  , the " + flag + " batch have exception !" + shardingIndex());
					if (HoneyUtil.isH2()) Logger.logSQL("the number of affected rows is inaccurate!");
				}
				logAffectRow(total);
				return total; // 外层try,处理异常后就会返回
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

			clearContext(sql[0], batchSize, len);
			// 更改操作需要清除缓存
			clearInCache(sql[0], "int[]", SuidType.INSERT, total);
		}
		logAffectRow(total);
		return total;
	}

	private static final String INDEX3 = "]";
	private static final String INSERT_ARRAY_SQL = " insert[] SQL : ";

	private int batch(String sql, int start, int end, Connection conn, PreparedStatement pst) throws SQLException {
		int a = 0;
		for (int i = start; i < end; i++) { // start... (end-1)

			if (showSQL) {
				if (i == 0) Logger.logSQL(LogSqlParse.parseSql(INSERT_ARRAY_SQL, sql));
				OneTimeParameter.setAttribute("_SYS_Bee_BatchInsert", i + "");
				String sql_i;
				sql_i = INDEX1 + i + INDEX2 + shardingIndex() + sql;// V2.2
				Logger.logSQL(LogSqlParse.parseSql(INSERT_ARRAY_SQL, sql_i));
			}
			setAndClearPreparedValues(pst, INDEX1 + i + INDEX2 + shardingIndex() + sql);// V2.2
			pst.addBatch();
		}
		// 同一批次的,有部分记录有像主键重复之类的,就会在此句抛异常. 同一批次的,H2 可以部分插入成功,MySQL却不可以
		int array[] = pst.executeBatch(); // oracle will return [-2,-2,...,-2]

		if (HoneyUtil.isOracle()) {
//			int array[]=pst.executeBatch();  //不能放在此处.executeBatch()是都要运行的
			a = pst.getUpdateCount();// oracle is ok. but mysql will return 1 alway.So mysql use special branch.
		} else {
			a = countFromArray(array);
		}

		eachBatchCommitIfNeed(conn); // if need

		Logger.logSQL(
				" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: " + a + "" + shardingIndex());

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

	private int countFromArray(int array[]) {
		int a = 0;
		if (array == null) return a;
		for (int i = 0; i < array.length; i++) {
			a += array[i];
		}
		return a;
	}

	private int batchForMysql(String sql[], final int batchSize) {

		if (sql == null || sql.length < 1) return -1;

		initRoute(SuidType.INSERT, null, sql[0]);

		int len = sql.length;
		int total = 0;
		int temp = 0;
		// 一条插入语句的值的占位符
		String placeholderValue = (String) OneTimeParameter.getAttribute("_SYS_Bee_PlaceholderValue");

		Connection conn = null;
		PreparedStatement pst = null;
		boolean oldAutoCommit = false;
		boolean hasException = false;
		boolean last = false;
		boolean first = false;

		try {
			conn = getConn();
			oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql[0]);

			String batchExeSql[];

			if (len <= batchSize) {
				first = true;
				batchExeSql = getBatchExeSql(exe_sql, len, placeholderValue); // batchExeSql[1] : ForPrint
				pst = conn.prepareStatement(batchExeSql[0]);

				total = _batchForMysql(sql[0], 0, len, conn, pst, batchSize, batchExeSql[1]);
			} else {
				batchExeSql = getBatchExeSql(exe_sql, batchSize, placeholderValue);
				pst = conn.prepareStatement(batchExeSql[0]);

				for (int i = 0; i < len / batchSize; i++) {
					int start = i * batchSize;
					int end = (i + 1) * batchSize;
					try {
						// not executeBatch
						temp = _batchForMysql(sql[0], start, end, conn, pst, batchSize, batchExeSql[1]);
						total += temp;
					} catch (SQLException e) {
						hasException = true; // finally要用到
						if (catchModifyDuplicateException(e)) {
							// do not return in batch loop
							Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3
									+ " Affected rows: 0  , this batch have exception !" + shardingIndex());
						} else {
							throw new SQLException(e);
						}
					}
				} // end for

				if (len % batchSize != 0) { // 尾数不成批
					last = true;
					batchExeSql = getBatchExeSql(exe_sql, (len % batchSize), placeholderValue); // 最后一批,getBatchExeSql返回的语句可能不一样
					pst = conn.prepareStatement(batchExeSql[0]); // fixed bug
					temp = _batchForMysql(sql[0], len - (len % batchSize), len, conn, pst, batchSize, batchExeSql[1]);
					total += temp;
				}
			}

			allCommitIfNeed(conn);

//			conn.setAutoCommit(oldAutoCommit);
		} catch (SQLException e) {
			hasException = true;

			if (catchModifyDuplicateException(e)) {
				if (first || last) {
					String flag = "last";
					if (first) flag = "first";
					int start = len - (len % batchSize);
					int end = len;
					Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: 0  , the "
							+ flag + " batch have exception !" + shardingIndex());
				}
				logAffectRow(total);
				return total; // 外层try,处理异常后就会返回
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
				// ignore
				Logger.debug(e2.getMessage());
			}

			if (hasException) {
				checkClose(pst, null);
				closeConn(conn);
			} else {
				checkClose(pst, conn);
			}

			clearContextForMysql(sql[0], batchSize, len);
			// 更改操作需要清除缓存
			clearInCache(sql[0], "int[]", SuidType.INSERT, total);
		}
		logAffectRow(total);
		return total;
	}

	private void clearContextForMysql(String sql_0, final int batchSize, final int len) {

		clearContext(sql_0, batchSize, len);

		int num = (len - 1) / batchSize;
		for (int i = 0; i <= num; i++) {
			String sqlForGetValue = shardingIndex() + sql_0 + "  [Batch:" + i + INDEX3; // fixed bug V2.2
			clearContext(sqlForGetValue);
		}
	}

	private int _batchForMysql(String sql, int start, int end, Connection conn, PreparedStatement pst, int batchSize,
			String batchSqlForPrint) throws SQLException {
//		sql用于获取转换成获取占位的sqlKey和打印log. 
//		v1.8  打印的sql是单行打印；执行的是批的形式.
		if (showSQL) {
			// print log
			if (start == 0 || (end - start != batchSize)) {
//				if(batchSize==1) OneTimeParameter.setTrueForKey("_SYS_Bee_BatchInsertFirst");
				Logger.logSQL(LogSqlParse.parseSql(INSERT_ARRAY_SQL, batchSqlForPrint));
			}

			for (int i = start; i < end; i++) { // start... (end-1)
				OneTimeParameter.setAttribute("_SYS_Bee_BatchInsert", i + "");
				String sql_i;
//				if (i == 0)
//					sql_i = sql;
//				else
				sql_i = INDEX1 + i + INDEX2 + shardingIndex() + sql;

				Logger.logSQL(LogSqlParse.parseSql(INSERT_ARRAY_SQL, sql_i));
			}
		}

		int a = 0;
		String sqlForGetValue = shardingIndex() + sql + "  [Batch:" + (start / batchSize) + INDEX3; // V2.2
		setAndClearPreparedValues(pst, sqlForGetValue);
		a = pst.executeUpdate(); // not executeBatch

		eachBatchCommitIfNeed(conn); // if need

		Logger.logSQL(" | <-- [Batch:" + (start / batchSize) + INDEX3 + " Affected rows: " + a + "" + shardingIndex());

		return a;
	}

	private String[] getBatchExeSql(String sql0, final int size, String placeholderValue) {
		StringBuffer batchSql = new StringBuffer(sql0);
		StringBuffer batchSql_forPrint = new StringBuffer(sql0);
		String batchExeSql[] = new String[2];
		for (int i = 0; i < size - 1; i++) { // i=0
			batchSql.append(",");
			batchSql.append(placeholderValue);

			if (size > 10 && i == 1) { // 超过10个的，会用省略格式。
				batchSql_forPrint.append(",......,");
			} else if (size > 10 && i == size - 2) {
				batchSql_forPrint.append(placeholderValue);
				batchSql_forPrint.append("      ");
				batchSql_forPrint.append("Total of records : ");
				batchSql_forPrint.append(size);
			} else if (size > 10 && i > 1) {
				// ignore
			} else {
				batchSql_forPrint.append(",");
				batchSql_forPrint.append(placeholderValue);
			}
		}

		batchExeSql[0] = batchSql.toString();
		batchExeSql[1] = batchSql_forPrint.toString();// for print log

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
				// ignore
			}
		}
	}

	protected void closeConn(Connection conn) {
		HoneyContext.closeConn(conn);
	}

	@Override
	public <T> List<T> moreTableSelect(String sql, final T entity) {
		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();

		if (isSimpleMode()) {
			return _moreTableSelect3(sql, entity); // 1.x版本及不用分片走的分支 2026
		} else {
			if (HoneyContext.getSqlIndexLocal() == null) {
				List<T> list = _moreTableSelect3(sql, entity); // 检测缓存的
				if (list != null) {
					logDsTab();
					return list;
				}

				try {
					// rsList还要排序
					List<T> rsList = new ShardingMoreTableSelectEngine().asynProcess(sql, entity, this);

					addInCache(sql, rsList, rsList.size());
					return rsList;

				} finally {
					clearContext(sql); // 2.2 分片的主线程都要清主线程的上下文
				}

			} else { // 子线程执行
				return _moreTableSelect3(sql, entity);
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	private <T> List<T> _moreTableSelect3(String sql, final T entity) {

		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();

		Map<String, MoreTableStruct3> moreTableStructMap = ParseSqlHelper.parseJoins(entity);

		String allEntityType = ""; // 标识同一个sql，但是组装出的实体不一样。

		if (moreTableStructMap != null && !moreTableStructMap.isEmpty()) {
			MoreTableStruct3 firstStruct = moreTableStructMap.values().iterator().next();
			allEntityType = firstStruct.overall.allEntityType.toString();
		}

		boolean isReg = updateInfoInCache(sql, "List<T>" + allEntityType, SuidType.SELECT, entity.getClass());
		if (isReg) {
			initRoute(SuidType.SELECT, entity.getClass(), sql); // 多表查询的多个表要在同一个数据源.
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);

				List<T> list = (List<T>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}
		if (isShardingMain()) return null; // sharding时,主线程没有缓存就返回.

		Class<T> entityClass = toClassT(entity);
		// Assemble the result by yourself. 多表查询是注册有,才会用.
		if (ResultAssemblerRegistry.hadReg(entityClass)) return _moreTableSelectAssemble(sql, entityClass);
		// 没注册有,则用后面的解析

		Connection conn = null;
		PreparedStatement pst = null;
		ResultSet rs = null;

		List<T> rsList;
		boolean hasException = false;
		try {
			conn = getConn();
			pst = conn.prepareStatement(HoneyUtil.deleteLastSemicolon(sql));
			setPreparedValues(pst, sql);
			rs = pst.executeQuery();

			// process result; 将查询结果转化成中间对象
//			List<MoreTableResultWrapper<T>> listResultWrapper = TransformResultSet.rsForMoretable3(rs, entity);
//			rsList=TransformResultSet.genRsForMoretable3(listResultWrapper, entityClass, moreTableStructMap);

			rsList = TransformResultSet.genRsViaMidRsForMoretable(TransformResultSet.toMidRsForMoretable(rs, entity),
					entityClass, moreTableStructMap);

			addInCache(sql, rsList, rsList.size());
		} catch (SQLException e) {
			hasException = true;
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException
				| InstantiationException e) {
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

		logSelectRows(rsList.size());

		return rsList;
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> toClassT(T entity) {
		return (Class<T>) entity.getClass();
	}

	// Assemble the result by yourself.
	private <T> List<T> _moreTableSelectAssemble(String sql, final Class<T> entityClass) {
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
				targetObj = ResultAssemblerHandler.rowToEntity(rs, entityClass);
				rsList.add(targetObj);
			}

			addInCache(sql, rsList, rsList.size());

		} catch (SQLException | IllegalAccessException | InstantiationException e) {
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

	private void setPreparedValues(PreparedStatement pst, String sql) throws SQLException {
//		查询时设置值,就删了上下文,当查询回来,放缓存时,就不能用
//		List<PreparedValue> list = HoneyContext.getPreparedValue(sql); //拿了设值后就会删除.用于日志打印的,要早于此时.   bug: when add cache, no value. 
		List<PreparedValue> list = HoneyContext.justGetPreparedValue(sql);
		if (null != list && list.size() > 0) _setPreparedValues(pst, list);
	}

	// 只用于批处理
	private void setAndClearPreparedValues(PreparedStatement pst, String sql) throws SQLException {
		List<PreparedValue> list = HoneyContext.getAndClearPreparedValue(sql);
		if (null != list && list.size() > 0) _setPreparedValues(pst, list);
	}

	private void _setPreparedValues(PreparedStatement pst, List<PreparedValue> list) throws SQLException {
		int size = list.size();
		for (int i = 0; i < size; i++) {
			int k;
			int jsonType = list.get(i).getJsonType();
			if (jsonType == 1)
				k = 26; // Json annotation
			else if (jsonType == 2)
				k = 34; // Jsonb just for pgsql
			else
				k = HoneyUtil.getJavaTypeIndex(list.get(i).getType());
			HoneyUtil.setPreparedValues(pst, k, i, list.get(i).getValue()); // i from 0
		}
	}

	private boolean isShardingMain() {// 有分片(多个)
		return HoneyContext.getSqlIndexLocal() == null && ShardingUtil.hadSharding(); // 前提要是HoneyContext.hadSharding()
	}

}
