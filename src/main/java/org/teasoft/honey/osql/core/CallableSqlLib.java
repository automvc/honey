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

/**
 * 存储过程方式Sql操作DB的接口CallableSql的实现类.Procedure sql operate the DB.
 * @author Kingstar
 * @since  1.0
 */
public class CallableSqlLib implements CallableSql {

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
			conn = getConn();
//          callSql = "{call batchOrder(?,?,?)}"; 
//			callSql = "{call " + callSql + "}"; // callSql like : batchOrder(?,?,?)
			callSql=getCallSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL(CALLABLE_SQL, callSql + VALUES + values);
			rs = cstmt.executeQuery();

			rsList = new ArrayList<>();

			Field field[] = returnType.getClass().getDeclaredFields();
			int columnCount = field.length;

			while (rs.next()) {
				targetObj = (T) returnType.getClass().newInstance();
				for (int i = 0; i < columnCount; i++) {
					if(HoneyUtil.isSkipField(field[i])) continue;
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
			try {
				if(rs!=null) rs.close();
			} catch (Exception e2) {
				// ignore
			}
			checkClose(cstmt, conn);
		}

		returnType = null;
		targetObj = null;

		return rsList;
	}

	@Override
	public int modify(String callSql, Object[] preValues) { //没有 输出参数情形
		int result = 0;
		Connection conn = null;
		CallableStatement cstmt =null;
		try {
			conn = getConn();
			callSql=getCallSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL(CALLABLE_SQL, callSql + VALUES + values);
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
			callSql=getCallSql(callSql);
			cstmt = conn.prepareCall(callSql);
			Logger.logSQL("Callable SQL,getCallableStatement: ",callSql);
			String key=getIdString(cstmt);
			setConnLocal(key, conn);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}

		return cstmt;

	}

	@Override
	public int modify(CallableStatement cstmt) { //无输出参数情形
		int result = 0;
		try {
			String key=getIdString(cstmt);
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
		
		List<String[]> list=null;
		
		Connection conn = null;
		ResultSet rs = null;
		CallableStatement cstmt = null;

		try {
			conn = getConn();
			callSql=getCallSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL(CALLABLE_SQL, callSql + VALUES + values);
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
			callSql=getCallSql(callSql);
			cstmt = conn.prepareCall(callSql);

			StringBuffer values = initPreparedValues(cstmt, preValues);
			Logger.logSQL(CALLABLE_SQL, callSql + VALUES + values);
			rs = cstmt.executeQuery();

			json = TransformResultSet.toJson(rs,null);

		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			checkClose(cstmt, conn);
		}

		return json.toString();
	}
	
	private String getCallSql(String callSql) {
		return "{call " + callSql + "}";
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
		map.remove(key);
		connLocal.remove();
		return s;
	}
	
	private String getIdString(CallableStatement cstmt) {
//		return cstmt.toString(); //mysql is different in  modify(CallableStatement cstmt),getCallableStatement(String callSql)
		return cstmt.hashCode()+"";
	}

	private StringBuffer initPreparedValues(CallableStatement cstmt, Object[] preValues) throws SQLException {
       
		if(preValues==null) return new StringBuffer("preValues is null!");
        
		StringBuffer valueBuffer = new StringBuffer();
		int len=preValues.length;
		for (int i = 0; i < len; i++) {
			int k=-1; //V1.17
			if(preValues[i]!=null) k = HoneyUtil.getJavaTypeIndex(preValues[i].getClass().getName());
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
