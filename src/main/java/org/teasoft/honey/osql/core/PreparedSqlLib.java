package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.PreparedSql;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * 支持带占位符(?)的sql操作.sql语句是DB能识别的SQL,非面向对象的sql.
 * 若是简单的操作,建议用面向对象的操作方式,ObjSQL和ObjSQLRich.
 * @author Kingstar
 * @since  1.0
 * 支持如name=#{name}的map参数形式
 * @since  1.2
 */
public class PreparedSqlLib implements PreparedSql {

	private BeeSql beeSql;// = BeeFactory.getHoneyFactory().getBeeSql();

	public BeeSql getBeeSql() {
		if(this.beeSql==null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}
	
	@Override
	public <T> List<T> select(String sql, T entity, Object[] preValues) {

		initPreparedValues(sql, preValues,entity);
		Logger.logSQL("PreparedSqlLib select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}
	
	@Override
	public <T> List<T> select(String sqlStr, T returnType, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map,returnType);
		Logger.logSQL("PreparedSqlLib select SQL: ", sql);
		return getBeeSql().select(sql, returnType);
	}
	
	@Override
	public <T> List<T> selectSomeField(String sql, T entity, Object[] preValues) {

		initPreparedValues(sql, preValues,entity);
		Logger.logSQL("PreparedSqlLib selectSomeField SQL: ", sql);
		return getBeeSql().selectSomeField(sql, entity);
	}

	@Override
	public <T> List<T> selectSomeField(String sqlStr, T returnType, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map,returnType);
		Logger.logSQL("PreparedSqlLib selectSomeField SQL: ", sql);
		return getBeeSql().selectSomeField(sql, returnType);
	}

	@Override
	public String selectFun(String sql, Object[] preValues) throws ObjSQLException {

		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib selectFun SQL: ", sql);
		return getBeeSql().selectFun(sql);
	}

	@Override
	public String selectFun(String sqlStr, Map<String, Object> map) throws ObjSQLException {
		String sql=initPrepareValuesViaMap(sqlStr,map);
		Logger.logSQL("PreparedSqlLib selectFun SQL: ", sql);
		return getBeeSql().selectFun(sql);
	}

	@Override
	public List<String[]> select(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib select SQL: ", sql);
		return getBeeSql().select(sql);
	}
	

	@Override
	public List<String[]> select(String sqlStr, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map);
		Logger.logSQL("PreparedSqlLib select SQL: ", sql);
		return getBeeSql().select(sql);
	}

	@Override
	public int modify(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib modify SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	public int modify(String sqlStr, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map);
		Logger.logSQL("PreparedSqlLib modify SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	public String selectJson(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib selectJson SQL: ", sql);
		return getBeeSql().selectJson(sql);
	}
	
	@Override
	public String selectJson(String sqlStr, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map);
		Logger.logSQL("PreparedSqlLib selectJson SQL: ", sql);
		return getBeeSql().selectJson(sql);
	}
	
	private <T> void initPreparedValues(String sql, Object[] preValues, T entity) {
		StringBuffer valueBuffer = initPreparedValues(sql, preValues);
		if (valueBuffer.length() > 0) {
			String tableName = _toTableName(entity);
			addInContextForCache(sql, valueBuffer.toString(), tableName);
		}
	}
	private StringBuffer initPreparedValues(String sql, Object[] preValues) {

		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();
		StringBuffer valueBuffer = new StringBuffer();
		for (int i = 0, k = 0; i < preValues.length; i++) {
			preparedValue = new PreparedValue();
			preparedValue.setType(preValues[i].getClass().getName());
			preparedValue.setValue(preValues[i]);
			list.add(k++, preparedValue);

			valueBuffer.append(",");
			valueBuffer.append(preValues[i]);
		}

		if (valueBuffer.length() > 0) {
			valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sql, valueBuffer.toString());
		}
		return valueBuffer;
	}
	
	private <T> String initPrepareValuesViaMap(String sqlStr, Map<String, Object> map, T entity) {
		SqlValueWrap wrap = processSql(sqlStr);
		String sql = wrap.getSql();
		StringBuffer valueBuffer = initPreparedValues(sql, wrap.getValueBuffer().toString(), map);

		if (valueBuffer.length() > 0) {
			String tableName = _toTableName(entity);
			addInContextForCache(sql, valueBuffer.toString(), tableName);
		}

		return sql;
	}
	
	private String initPrepareValuesViaMap(String sqlStr, Map<String, Object> map){
		SqlValueWrap wrap=processSql(sqlStr);
		String sql=wrap.getSql();
		initPreparedValues(sql,wrap.getValueBuffer().toString(), map);
		return sql;
	}
	
	private StringBuffer initPreparedValues(String sql, String paraList,Map<String,Object> map) {

		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();
		StringBuffer valueBuffer = new StringBuffer();
		Object value;
		
		String keys[]=paraList.split(",");  //map's key
		
		
		for (int i = 0, k = 0; i < keys.length; i++) {
			preparedValue = new PreparedValue();
			value=null;
			
			int len=keys[i].length();
			if(keys[i].startsWith("%")){
				if(keys[i].endsWith("%")){  //    %para%
					keys[i]=keys[i].substring(1,len-1);
					value="%"+map.get(keys[i])+"%";
					preparedValue.setValue(value);
				}else{  //   %para
					keys[i]=keys[i].substring(1,len);
					value="%"+map.get(keys[i]);
					preparedValue.setValue(value);
				}
			}else if(keys[i].endsWith("%")){  //  para%
				keys[i]=keys[i].substring(0,len-1);
				value=map.get(keys[i])+"%";
				preparedValue.setValue(value);
			}else{
				value=map.get(keys[i]);
				preparedValue.setValue(value);
			}
			
			preparedValue.setType(map.get(keys[i]).getClass().getName());
			
			list.add(k++, preparedValue);

			valueBuffer.append(",");
			valueBuffer.append(value);
		}

		if (valueBuffer.length() > 0) {
			valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sql, valueBuffer.toString());
		}
		
		return valueBuffer;
	}

	private SqlValueWrap  processSql(String sql){
		return TokenUtil.process(sql, "#{", "}", "?");
	}
	
	private static void addInContextForCache(String sql,String sqlValue, String tableName){
		_ObjectToSQLHelper.addInContextForCache(sql, sqlValue, tableName);
	}
	
	private static String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

}
