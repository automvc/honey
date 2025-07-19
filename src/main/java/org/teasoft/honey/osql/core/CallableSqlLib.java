package org.teasoft.honey.osql.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.CallableSql;
import org.teasoft.honey.logging.Logger;

/**
 * 存储过程方式Sql操作DB的接口CallableSql的实现类.Procedure sql operate the DB.
 * CallableSql do not support DB Sharding.
 * CallableSql have not BeeSql router system, but can use setDataSourceName method.
 * CallableSql do not support cache.
 * @author Kingstar
 * @since  1.0
 * some methods support Interceptor.
 * @since  2.4.0
 */
public class CallableSqlLib extends AbstractCommOperate implements CallableSql {

	private static final String VALUES = "  values: ";
	private static final String CALLABLE_SQL = "Callable SQL: ";
	private static final ThreadLocal<Map<String, Connection>> connLocal = new ThreadLocal<>();

	@Override
	public <T> List<T> select(String callSql, T returnType, Object[] preValues) {

		Connection conn = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;
		List<T> rsList = null;
		T targetObj = null;
		try {
			doBeforePasreEntity(returnType, SuidType.SELECT);// returnType的值,虽然不用作占位参数的值,但可以用作拦截器的业务逻辑判断

			conn = getConn();
//          callSql = "{call batchOrder(?,?,?)}"; 
//			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			callSql = getCallSql(callSql);
			callSql = doAfterCompleteSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			logSQL(CALLABLE_SQL, callSql + VALUES + values);
			rs = cstmt.executeQuery();

			rsList = new ArrayList<>();
			while (rs.next()) {
				targetObj = ResultAssemblerHandler.rowToEntity(rs, toClassT(returnType));
				rsList.add(targetObj);
			}
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (Exception e2) {
				// ignore
			}
			checkClose(cstmt, conn);
			doBeforeReturn(rsList);
			targetObj = null;
		}

		return rsList;
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> toClassT(T entity) {
		return (Class<T>) entity.getClass();
	}

	@Override
	public int modify(String callSql, Object[] preValues) { // 没有 输出参数情形
		int result = 0;
		Connection conn = null;
		CallableStatement cstmt = null;
		try {
			doBeforePasreEntity2();

			conn = getConn();
			callSql = getCallSql(callSql);
			callSql = doAfterCompleteSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			logSQL(CALLABLE_SQL, callSql + VALUES + values);
			result = cstmt.executeUpdate();

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(cstmt, conn);
			doBeforeReturn();
		}

		return result;

	}

	@Override
	public CallableStatement getCallableStatement(String callSql) { // 可自定义输入参数
		Connection conn = null;
		CallableStatement cstmt = null;
		try {
			conn = getConn();
			callSql = getCallSql(callSql);
			cstmt = conn.prepareCall(callSql);
			logSQL("Callable SQL,getCallableStatement: ", callSql);
			String key = getIdString(cstmt);
			setConnLocal(key, conn);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}

		return cstmt;

	}

	@Override
	public int modify(CallableStatement cstmt) { // 无输出参数情形
		int result = 0;
		try {
			String key = getIdString(cstmt);
			Connection conn = getConnLocal(key);
			result = cstmt.executeUpdate();
			checkClose(cstmt, conn);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
		return result;
	}

	@Override
	public List<String[]> select(String callSql, Object[] preValues) {

		List<String[]> list = null;
		Connection conn = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;

		try {
			doBeforePasreEntity();
			conn = getConn();
			callSql = getCallSql(callSql);
			callSql = doAfterCompleteSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			logSQL(CALLABLE_SQL, callSql + VALUES + values);
			rs = cstmt.executeQuery();

			list = TransformResultSet.toStringsList(rs);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(cstmt, conn);
			doBeforeReturn();
		}

		return list;
	}

	@Override
	public String selectJson(String callSql, Object[] preValues) {

		String json = "";
		Connection conn = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;

		try {
			doBeforePasreEntity();
			conn = getConn();
			callSql = getCallSql(callSql);
			callSql = doAfterCompleteSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			logSQL(CALLABLE_SQL, callSql + VALUES + values);
			rs = cstmt.executeQuery();

			JsonResultWrap wrap = TransformResultSet.toJson(rs, null);
			json = wrap.getResultJson();
			int rowCount = wrap.getRowCount();
			logSelectRows(rowCount);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(cstmt, conn);
			doBeforeReturn();
		}

		return json;
	}

	private void logSelectRows(int size) {
		Logger.logSQL(" | <--  select rows: "+ size);
	}

	private String getCallSql(String callSql) {
		return "{call " + callSql + "}";
	}

	private void setConnLocal(String key, Connection conn) {
		if (conn == null) return;
		Map<String, Connection> map = connLocal.get();
		if (map == null) map = new HashMap<>();
		map.put(key, conn);
		connLocal.set(map);
	}

	private Connection getConnLocal(String key) {
		Map<String, Connection> map = connLocal.get();
		if (map == null) return null;
		Connection s = map.get(key);
		map.remove(key);
		connLocal.remove();
		return s;
	}

	private String getIdString(CallableStatement cstmt) {
//		return cstmt.toString(); //mysql is different in  modify(CallableStatement cstmt),getCallableStatement(String callSql)
		return cstmt.hashCode() + "";
	}

	private StringBuffer initPreparedValues(CallableStatement cstmt, Object[] preValues) throws SQLException {

		if (preValues == null) return new StringBuffer("preValues is null!");

		boolean isShowSQL = isShowSQL();
		StringBuffer valueBuffer = new StringBuffer(); // for print log
		int len = preValues.length;
		for (int i = 0; i < len; i++) {
			int k = -1; // V1.17
			if (preValues[i] != null) k = HoneyUtil.getJavaTypeIndex(preValues[i].getClass().getName());
			HoneyUtil.setPreparedValues(cstmt, k, i, preValues[i]); // i from 0
			if (isShowSQL) {
				valueBuffer.append(",");
				valueBuffer.append(preValues[i]);
			}
		}

		if (isShowSQL && valueBuffer.length() > 0) {
			valueBuffer.deleteCharAt(0);
		}
		return valueBuffer;
	}

	private Connection getConn() throws SQLException {
		return HoneyContext.getConn();
	}

	protected void checkClose(Statement stmt, Connection conn) {
		HoneyContext.checkClose(stmt, conn);
	}

	private void doBeforePasreEntity() {
		Object entity = null;
		super.doBeforePasreEntity(entity, SuidType.SELECT);
	}

	private void doBeforePasreEntity2() {
		Object entity = null;
		super.doBeforePasreEntity(entity, SuidType.MODIFY);
	}

	private static boolean isShowSQL() {
		return HoneyConfig.getHoneyConfig().showSQL;
	}
	
	private static void logSQL(String hardStr, String sql) {
		HoneyUtil.logSQL(hardStr, sql);
	}

}
