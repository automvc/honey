package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.PreparedSql;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.bee.osql.exception.SqlNullException;
import org.teasoft.honey.osql.name.NameUtil;

/**
 * 支持带占位符(?)的sql操作.sql语句是DB能识别的SQL,非面向对象的sql.
 * 若是简单的操作,建议用面向对象的操作方式,ObjSQL和ObjSQLRich.
 * @author Kingstar
 * @since  1.0
 * 支持如name=#{name},name like #{name%}的map参数形式
 * @since  1.2
 */
public class PreparedSqlLib implements PreparedSql {

	private BeeSql beeSql;

	public BeeSql getBeeSql() {
		if(beeSql==null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}
	
	private DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	
	@Override
	public <T> List<T> select(String sql, T entity, Object[] preValues) {
		
		initPreparedValues(sql, preValues,entity);
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}
	
	@Override
	public <T> List<T> select(String sql, T entity, Object[] preValues,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		regPagePlaceholder();
		
		String tableName="";
		if(isNeedRealTimeDb()) {
			tableName= _toTableName(entity);  //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(),tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}
		
		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues,entity);
		
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}
	
	@Override
	public <T> List<T> select(String sqlStr, T entity, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map,entity);
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}
	
	@Override
	public <T> List<T> select(String sqlStr, T entity, Map<String, Object> map,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		regPagePlaceholder();
		
		String tableName="";
		if(isNeedRealTimeDb()) {
			tableName= _toTableName(entity);  //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(),tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}
		
		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql=initPrepareValuesViaMap(pageSql,map,entity);
		
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql, entity);
	}
	
	@Override
	public <T> List<T> selectSomeField(String sql, T entity, Object[] preValues) {

		initPreparedValues(sql, preValues,entity);
		Logger.logSQL("PreparedSql selectSomeField SQL: ", sql);
		return getBeeSql().selectSomeField(sql, entity);
	}
	
	@Override
	public <T> List<T> selectSomeField(String sql, T entity, Object[] preValues,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");

		regPagePlaceholder();
		
		String tableName="";
		if(isNeedRealTimeDb()) {
			tableName= _toTableName(entity);  //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(),tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}
		
		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues,entity);
		
		Logger.logSQL("PreparedSql selectSomeField SQL: ", sql);
		return getBeeSql().selectSomeField(sql, entity);
	}

	@Override
	public <T> List<T> selectSomeField(String sqlStr, T entity, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map,entity);
		Logger.logSQL("PreparedSql selectSomeField SQL: ", sql);
		return getBeeSql().selectSomeField(sql, entity);
	}
	
	@Override
	public <T> List<T> selectSomeField(String sqlStr, T entity, Map<String, Object> map,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		regPagePlaceholder();
		
		String tableName="";
		if(isNeedRealTimeDb()) {
			tableName= _toTableName(entity);  //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(),tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}
		
		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql=initPrepareValuesViaMap(pageSql,map,entity);
		
		Logger.logSQL("PreparedSql selectSomeField SQL: ", sql);
		return getBeeSql().selectSomeField(sql, entity);
	}

	@Override
	public String selectFun(String sql, Object[] preValues) {

		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSql selectFun SQL: ", sql);
		return getBeeSql().selectFun(sql);
	}

	@Override
	public String selectFun(String sqlStr, Map<String, Object> map) {
		String sql = initPrepareValuesViaMap(sqlStr, map);
		Logger.logSQL("PreparedSql selectFun SQL: ", sql);
		return getBeeSql().selectFun(sql);
	}

	@Override
	public List<String[]> select(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql);
	}
	
	@Override
	public List<String[]> select(String sql, Object[] preValues,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		regPagePlaceholder();
		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues);
		
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql);
	}
	

	@Override
	public List<String[]> select(String sqlStr, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map);
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql);
	}
	
	@Override
	public List<String[]> select(String sqlStr, Map<String, Object> map,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		regPagePlaceholder();
		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql=initPrepareValuesViaMap(pageSql,map);
		
		Logger.logSQL("PreparedSql select SQL: ", sql);
		return getBeeSql().select(sql);
	}

	@Override
	@Deprecated
	public int modify(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSql modify SQL: ", sql);
		return getBeeSql().modify(sql);
	}

	@Override
	@Deprecated
	public int modify(String sqlStr, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map);
		Logger.logSQL("PreparedSql modify SQL: ", sql);
		return getBeeSql().modify(sql);
	}
	
	@Override
	@Deprecated
	public int modify(String sql) {
		Object[] preValues = null;
		return modify(sql, preValues);
	}

	@Override
	public String selectJson(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSql selectJson SQL: ", sql);
		return getBeeSql().selectJson(sql);
	}
	
	@Override
	public String selectJson(String sql, Object[] preValues,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		regPagePlaceholder();
		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues);
		
		Logger.logSQL("PreparedSql selectJson SQL: ", sql);
		return getBeeSql().selectJson(sql);
	}
	
	@Override
	public String selectJson(String sqlStr, Map<String, Object> map) {
		String sql=initPrepareValuesViaMap(sqlStr,map);
		Logger.logSQL("PreparedSql selectJson SQL: ", sql);
		return getBeeSql().selectJson(sql);
	}
	
	@Override
	public String selectJson(String sqlStr, Map<String, Object> map,int start,int size) {
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		regPagePlaceholder();
		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql=initPrepareValuesViaMap(pageSql,map);
		
		Logger.logSQL("PreparedSql selectJson SQL: ", sql); 
		return getBeeSql().selectJson(sql);
	}
	
	@Override
	public String selectJson(String sqlStr) {
		Object[] preValues=null;
		return selectJson(sqlStr, preValues);
	}

	@Override
	public List<String[]> select(String sql) {
		Object[] preValues=null;
		return select(sql, preValues);
	}
	
	@Override
	public String selectFun(String sql) {
		Object[] preValues = null;
		return selectFun(sql, preValues);
	}

	private <T> void initPreparedValues(String sql, Object[] preValues, T entity) {
		List list=_initPreparedValues(sql, preValues);
//		if (valueBuffer.length() > 0) {//bug. no placeholder will have problem.
//			String tableName = _toTableName(entity);
//			HoneyContext.setPreparedValue(sql, list);  
//			addInContextForCache(sql, tableName);  //有T才放缓存.
			
		    String tableName ="";
			if (isNeedRealTimeDb()) {
				tableName = (String) OneTimeParameter.getAttribute(StringConst.TABLE_NAME);
				if (tableName == null) {
					tableName = _toTableName(entity);
				}
			}else {
				tableName = _toTableName(entity);
			}
			
			//pre page 1, 3
			HoneyUtil.setPageNum(list);
			HoneyContext.setContext(sql, list, tableName);
//		}
	}
	
	
	private void initPreparedValues(String sql, Object[] preValues) {
		List list=_initPreparedValues(sql, preValues);
		// pre page 不放缓存 5,7
		HoneyUtil.setPageNum(list);
		HoneyContext.setPreparedValue(sql, list);  //没有entity,不放缓存.
	}
	
//	private StringBuffer initPreparedValues(String sql, Object[] preValues) {
	private List _initPreparedValues(String sql, Object[] preValues) {
		
		if(sql==null || "".equals(sql.trim())) {
			throw new SqlNullException("sql statement string is Null !");
		}

		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();
		
		for (int i = 0; preValues!=null && i < preValues.length; i++) { //fixbug
			preparedValue = new PreparedValue();
			preparedValue.setType(preValues[i].getClass().getName());
			preparedValue.setValue(preValues[i]);
			list.add(preparedValue);
		}
		return list;
	}
	
	private <T> Map<String, Object> mergeMap(Map<String, Object> prameterMap, T entity){
		Map<String, Object> columnMap=HoneyUtil.getColumnMapByEntity(entity);
		columnMap.putAll(prameterMap);  //merge, prameterMap will override columnMap,if have same key.
		return columnMap;
	}
	
	private <T> String initPrepareValuesViaMap(String sqlStr, Map<String, Object> parameterMap, T entity) {

		if (sqlStr == null || "".equals(sqlStr.trim())) {
			throw new SqlNullException("sql statement string is Null !");
		}
		parameterMap = mergeMap(parameterMap, entity);

		SqlValueWrap wrap = processSql(sqlStr); //will return null when sql no placeholder like: select * from tableName
		String reSql;
		List list =null;
		
	    String tableName ="";
		if (isNeedRealTimeDb()) {
			tableName = (String) OneTimeParameter.getAttribute(StringConst.TABLE_NAME);
			if (tableName == null) {
				tableName = _toTableName(entity);
			}
		}else {
			tableName = _toTableName(entity);
		}
//		String tableName = _toTableName(entity);
		
		if (wrap == null) {
			reSql=sqlStr;
			list=new ArrayList();
		} else {
			String sql = wrap.getSql();
			String mapKeys = wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
			 list = _initPreparedValues(mapKeys, parameterMap);
			reSql=sql;
		}
		
		HoneyUtil.setPageNum(list);
		//MAP PAGE 2,4
		HoneyContext.setContext(reSql, list, tableName);
		
		return reSql;
	}
	
	private String initPrepareValuesViaMap(String sqlStr, Map<String, Object> map){
		
		if(sqlStr==null || "".equals(sqlStr.trim())) {
			throw new SqlNullException("sql statement string is Null !");
		}
		
		SqlValueWrap wrap=processSql(sqlStr); //bug.  wrap maybe null
		if(wrap==null) return sqlStr;  //fix null bug
		String sql=wrap.getSql();
		String mapKeys=wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
		List list=_initPreparedValues(mapKeys, map); 
		//6,8  map,page 不放缓存
		HoneyUtil.setPageNum(list);
		HoneyContext.setPreparedValue(sql, list);
		return sql;
	}
	
//	private List _initPreparedValues(String sql, String mapKeys,Map<String,Object> map) {
	private List _initPreparedValues(String mapKeys,Map<String,Object> map) {

		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();
		Object value;
		
		String keys[]=mapKeys.split(",");  //map's key
		
		
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
		}
		return list;
	}

	private SqlValueWrap  processSql(String sql){
		return TokenUtil.process(sql, "#{", "}", "?");
	}
	
//	private static void addInContextForCache(String sql, String tableName){
//		_ObjectToSQLHelper.addInContextForCache(sql, tableName);
//	}
	
	private static String _toTableName(Object entity){
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}
	
	private void regPagePlaceholder(){
		HoneyUtil.regPagePlaceholder();
	}
	
	private boolean isNeedRealTimeDb() {
		return HoneyContext.isNeedRealTimeDb();
	}
	
	@Override
	public List<Map<String, Object>> selectMapList(String sql) {
		Logger.logSQL("PreparedSql selectMapList SQL: ", sql);
		return getBeeSql().selectMapList(sql);
	}
	
	@Override
	public List<Map<String, Object>> selectMapList(String sql,int start,int size) {
		
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		
		sql = getDbFeature().toPageSql(sql, start, size);
		Logger.logSQL("PreparedSql selectMapList SQL: ", sql);
		
		return getBeeSql().selectMapList(sql);
	}

}
