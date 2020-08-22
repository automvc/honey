package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.CallableSql;
import org.teasoft.bee.osql.annotation.JoinTable;

/**
 * @author Kingstar
 * @since  1.0
 */
public class CallableSqlLib implements CallableSql {

	public static ThreadLocal<Connection> connLocal2 = new ThreadLocal<>();

	public static ThreadLocal<Map<String, Connection>> connLocal = new ThreadLocal<>();

	@Override
	public <T> List<T> select(String callSql, T entity, Object[] preValues) {

		Connection conn = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;
		List<T> rsList = null;
		T targetObj = null;
		try {
			conn = getConn();
			//      callSql = "{call batchOrder(?,?,?)}"; 
			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL("Callable SQL: ", callSql + "  values: " + values);
			rs = cstmt.executeQuery();

			rsList = new ArrayList<T>();

			Field field[] = entity.getClass().getDeclaredFields();
			int columnCount = field.length;

			while (rs.next()) {
				targetObj = (T) entity.getClass().newInstance();
				for (int i = 0; i < columnCount; i++) {
					if ("serialVersionUID".equals(field[i].getName())) continue;
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
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(cstmt, conn);
		}

		entity = null;
		targetObj = null;

		return rsList;
	}

	@Override
	public int modify(String callSql, Object[] preValues) { //TODO 没有 输出参数情形
		int result = 0;
		Connection conn = null;
		CallableStatement cstmt =null;
		try {
			conn = getConn();
//          callSql = "{call batchOrder(?,?,?)}"; 
			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			cstmt = conn.prepareCall(callSql);
//          cstmt.setInt(1,barcodeNum);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL("Callable SQL: ", callSql + "  values: " + values);
			result = cstmt.executeUpdate();
			
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}finally{
		  checkClose(cstmt, conn);
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

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
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
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
		return result;
	}

	@Override
	public List<String[]> select(String callSql, Object[] preValues) {
		
		List<String[]> list=null;
		
		Connection conn = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;

		try {
			conn = getConn();
			//      callSql = "{call batchOrder(?,?,?)}"; 
			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL("Callable SQL: ", callSql + "  values: " + values);
			rs = cstmt.executeQuery();

			list=TransformResultSet.toStringsList(rs);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(cstmt, conn);
		}

		return list;
	}

	@Override
	public String selectJson(String callSql, Object[] preValues) {
		
		StringBuffer json = new StringBuffer("");
		
		Connection conn = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;

		try {
			conn = getConn();
			//      callSql = "{call batchOrder(?,?,?)}"; 
			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL("Callable SQL: ", callSql + "  values: " + values);
			rs = cstmt.executeQuery();

			json = TransformResultSet.toJson(rs);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(cstmt, conn);
		}

		return json.toString();
	}

	private void setConnLocal(String key, Connection conn) {
		if (conn == null) return;
		Map<String, Connection> map = connLocal.get();
		if (null == map) map = new HashMap<>();
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
		return cstmt.toString();
	}

	private StringBuffer initPreparedValues(CallableStatement cstmt, Object[] preValues) throws SQLException {
       
		if(preValues==null) return new StringBuffer("preValues is null!");
        
		StringBuffer valueBuffer = new StringBuffer();
		int len=preValues.length;
		for (int i = 0; i < len; i++) {
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
		return HoneyContext.getConn();
	}

	protected void checkClose(Statement stmt, Connection conn) {
		HoneyContext.checkClose(stmt, conn);
	}
	
	private static String _toColumnName(String fieldName){
		return NameTranslateHandle.toColumnName(fieldName);
	}
	
	private Object _getObject(ResultSet rs, Field field) throws SQLException{
		return HoneyUtil.getResultObject(rs, field.getType().getName(), _toColumnName(field.getName()));
	}
}
