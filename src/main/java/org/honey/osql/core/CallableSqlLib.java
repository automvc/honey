package org.honey.osql.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bee.osql.CallableSQL;

/**
 * @author KingStar
 * @since  1.0
 */
public class CallableSqlLib implements CallableSQL {

	public static ThreadLocal<Connection> connLocal2 = new ThreadLocal();

	public static ThreadLocal<Map<String, Connection>> connLocal = new ThreadLocal();

	@Override
	public <T> List<T> select(String sql, T entity, Object[] preValues) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int modify(String callSql, Object[] preValues) { //TODO 无输出参数情形
		int result = 0;
		Connection conn = null;
		try {
			conn = getConn();
			//      callSql = "{call batchOrder(?,?,?)}"; 
			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			CallableStatement cstmt = conn.prepareCall(callSql);
			//      cstmt.setInt(1,barcodeNum);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL("Callable SQL: ", callSql + "  values: " + values);
			result = cstmt.executeUpdate();
			checkClose(cstmt, conn);
		} catch (Exception e) {
			// TODO: handle exception
		}

		return result;

	}

	@Override
	public CallableStatement getCallableStatement(String callSql) { //可自定义输入参数
		Connection conn = null;
		CallableStatement cstmt = null;
		try {
			conn = getConn();
			//      callSql = "{call batchOrder(?,?,?)}"; 
			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			cstmt = conn.prepareCall(callSql);

			setConnLocal(getIdString(cstmt), conn);

		} catch (Exception e) {
			// TODO: handle exception
		}

		return cstmt;

	}

	@Override
	public int modify(CallableStatement cstmt) { //TODO 无输出参数情形
		int result = 0;
		try {
			Connection conn = getConnLocal(getIdString(cstmt));
			result = cstmt.executeUpdate();
			checkClose(cstmt, conn);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return result;
	}

	@Override
	public <T> List<T> select(CallableStatement cstmt, T entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String selectJson(CallableStatement cstmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> select(String sql, Object[] preValues) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String selectJson(String sql, Object[] preValues) {
		// TODO Auto-generated method stub
		return null;
	}

	private void setConnLocal(String key, Connection conn) {
		if (conn == null) return;
		Map<String, Connection> map = connLocal.get();
		if (null == map) map = new HashMap();
		map.put(key, conn);
		connLocal.set(map);
	}

	private Connection getConnLocal(String key) {
		Map<String, Connection> map = connLocal.get();
		if (null == map) return null;

		Connection s = map.get(key);
		if (s != null) map.remove(key);
		return s;
	}

	private String getIdString(CallableStatement cstmt) {
		String s = cstmt.toString();
		int index = cstmt.toString().indexOf(":");
		return s.substring(0, index);
	}

	private StringBuffer initPreparedValues(CallableStatement cstmt, Object[] preValues) throws SQLException {

		StringBuffer valueBuffer = new StringBuffer();
		for (int i = 0; i < preValues.length; i++) {
			int k = HoneyUtil.getJavaTypeIndex(preValues[i].getClass().getName());
			HoneyUtil.setPreparedValues(cstmt, k, i, preValues[i]); //i from 0
			valueBuffer.append(",");
			valueBuffer.append(preValues[i]);
		}

		if (valueBuffer.length() > 0) {
			valueBuffer.deleteCharAt(0);
		}
		return valueBuffer;
	}

	private Connection getConn() throws SQLException {
		Connection conn = null;
		conn = HoneyContext.getCurrentConnection();
		if (conn == null) {
			try {
				conn = SessionFactory.getConnection(); //不开户事务时
			} catch (Exception e) {
				Logger.print("Have Error when get the Connection: ", e.getMessage());
			}
		}
		return conn;

	}

	protected void checkClose(Statement stmt, Connection conn) {

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
				System.err.println("-----------SQLException in checkClose------");
			}
		}
		try {
			if (conn != null && conn.getAutoCommit()) {//自动提交时才关闭.如果开启事务,则由事务负责
				conn.close();
			}
		} catch (SQLException e) {
			System.err.println("-----------SQLException in checkClose------" + e.getMessage());
		}
	}
}
