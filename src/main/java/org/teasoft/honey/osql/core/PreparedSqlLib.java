package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.PreparedSql;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.bee.osql.exception.SqlNullException;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * 支持带占位符(?)的sql操作.sql语句是DB能识别的SQL,非面向对象的sql.
 * 若是简单的操作,建议用面向对象的操作方式,ObjSQL和ObjSQLRich.
 *  <br>Support sql string with placeholder.The sql statement is really DB's grammar,not object oriented type.
 * @author Kingstar
 * @since  1.0
 * 支持如name=#{name},name like #{name%}的map参数形式
 * @since  1.2
 */
public class PreparedSqlLib extends AbstractCommOperate implements PreparedSql {

	private BeeSql beeSql;

	private static final String SELECT_SQL = "PreparedSql select SQL: ";
	private static final String SELECT_MoreTable_SQL = "PreparedSql select MoreTable SQL: ";
	private static final String SELECT_SOME_FIELD_SQL = "PreparedSql selectSomeField SQL: ";
	private static final String SELECT_JSON_SQL = "PreparedSql selectJson SQL: ";
	private static final String STRING_IS_NULL = "sql statement string is Null !";
	private static final String START_GREAT_EQ_0 = StringConst.START_GREAT_EQ_0;
	private static final String SIZE_GREAT_0 = StringConst.SIZE_GREAT_0;

	public BeeSql getBeeSql() {
		if (beeSql == null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	private DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}

	@Override
	public <T> List<T> select(String sql, Class<T> entityClass, Object[] preValues) {
		doBeforePasreEntity(entityClass, SuidType.SELECT);//returnType的值,虽然不用作占位参数的值,但可以用作拦截器的业务逻辑判断
		initPreparedValues(sql, preValues, entityClass);
		sql = doAfterCompleteSql(sql);

		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().select(sql, entityClass);

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(String sql, Class<T> entityClass) {
		Object[] preValues = null;
		return select(sql, entityClass, preValues);
	}
	
	@Override
	public <T> List<T> select(String sql, Class<T> entityClass, Object[] preValues, int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity(entityClass, SuidType.SELECT);

		regPagePlaceholder();

		String tableName = "";
		if (isNeedRealTimeDb()) {
			tableName = _toTableName(entityClass); //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entityClass, tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}

		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues, entityClass);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().select(sql, entityClass);

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(String sqlStr, T entity, Map<String, Object> map) {
		doBeforePasreEntity(entity, SuidType.SELECT);
		String sql = initPrepareValuesViaMap(sqlStr, map, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().select(sql, toClassT(entity));

		doBeforeReturn(list);
		return list;
	}
	
	@SuppressWarnings("unchecked")
	private <T> Class<T> toClassT(T entity) {
		return (Class<T>)entity.getClass();
	}

	@Override
	public <T> List<T> select(String sqlStr, T entity, Map<String, Object> map, int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity(entity, SuidType.SELECT);

		regPagePlaceholder();

		String tableName = "";
		if (isNeedRealTimeDb()) {
			tableName = _toTableName(entity); //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}

		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql = initPrepareValuesViaMap(pageSql, map, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().select(sql, toClassT(entity));

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> selectSomeField(String sql, T entity, Object[] preValues) {

		doBeforePasreEntity(entity, SuidType.SELECT);

		initPreparedValues(sql, preValues, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SOME_FIELD_SQL, sql);
		List<T> list = getBeeSql().selectSomeField(sql, toClassT(entity));

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> selectSomeField(String sql, T entity, Object[] preValues, int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity(entity, SuidType.SELECT);

		regPagePlaceholder();

		String tableName = "";
		if (isNeedRealTimeDb()) {
			tableName = _toTableName(entity); //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}

		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SOME_FIELD_SQL, sql);
		List<T> list = getBeeSql().selectSomeField(sql, toClassT(entity));

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> selectSomeField(String sqlStr, T entity, Map<String, Object> map) {

		doBeforePasreEntity(entity, SuidType.SELECT);

		String sql = initPrepareValuesViaMap(sqlStr, map, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SOME_FIELD_SQL, sql);
		List<T> list = getBeeSql().selectSomeField(sql, toClassT(entity));

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> selectSomeField(String sqlStr, T entity, Map<String, Object> map, int start,
			int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity(entity, SuidType.SELECT);

		regPagePlaceholder();

		String tableName = "";
		if (isNeedRealTimeDb()) {
			tableName = _toTableName(entity); //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}

		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql = initPrepareValuesViaMap(pageSql, map, entity);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SOME_FIELD_SQL, sql);
		List<T> list = getBeeSql().selectSomeField(sql, toClassT(entity));

		doBeforeReturn(list);
		return list;
	}

	@Override
	public String selectFun(String sql, Object[] preValues) {

		doBeforePasreEntity();

		initPreparedValues(sql, preValues);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql selectFun SQL: ", sql);
		String s = getBeeSql().selectFun(sql);
		doBeforeReturn();
		return s;
	}

	@Override
	public String selectFun(String sqlStr, Map<String, Object> parameterMap) {

		doBeforePasreEntity();

		String sql = initPrepareValuesViaMap(sqlStr, parameterMap);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql selectFun SQL: ", sql);
		String s = getBeeSql().selectFun(sql);
		doBeforeReturn();
		return s;
	}

	@Override
	public List<String[]> select(String sql, Object[] preValues) {
		doBeforePasreEntity();
		initPreparedValues(sql, preValues);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<String[]> list = getBeeSql().select(sql);

		doBeforeReturn();
		return list;
	}

	@Override
	public List<String[]> select(String sql, Object[] preValues, int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity();

		regPagePlaceholder();
		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<String[]> list = getBeeSql().select(sql);

		doBeforeReturn();
		return list;
	}

	@Override
	public List<String[]> select(String sqlStr, Map<String, Object> map) {

		doBeforePasreEntity();

		String sql = initPrepareValuesViaMap(sqlStr, map);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<String[]> list = getBeeSql().select(sql);

		doBeforeReturn();
		return list;
	}

	@Override
	public List<String[]> select(String sqlStr, Map<String, Object> map, int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity();

		regPagePlaceholder();
		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql = initPrepareValuesViaMap(pageSql, map);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<String[]> list = getBeeSql().select(sql);

		doBeforeReturn();
		return list;
	}

	@Override
//	@Deprecated
	public int modify(String sql, Object[] preValues) {

		doBeforePasreEntity2();

		initPreparedValues(sql, preValues);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql modify SQL: ", sql);
		int r = getBeeSql().modify(sql);
		doBeforeReturn();
		return r;
	}

	@Override
//	@Deprecated
	public int modify(String sqlStr, Map<String, Object> map) {

		doBeforePasreEntity2(); //fixed bug

		String sql = initPrepareValuesViaMap(sqlStr, map);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql modify SQL: ", sql);
		int r = getBeeSql().modify(sql);
		doBeforeReturn();
		return r;
	}

	@Override
//	@Deprecated
	public int modify(String sql) {
		Object[] preValues = null;
		return modify(sql, preValues);
	}

	@Override
	public String selectJson(String sql, Object[] preValues) {

		doBeforePasreEntity();

		initPreparedValues(sql, preValues);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_JSON_SQL, sql);

		String json = getBeeSql().selectJson(sql);
		doBeforeReturn();
		return json;
	}

	@Override
	public String selectJson(String sql, Object[] preValues, int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity();

		regPagePlaceholder();
		sql = getDbFeature().toPageSql(sql, start, size);
		initPreparedValues(sql, preValues);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_JSON_SQL, sql);

		String json = getBeeSql().selectJson(sql);
		doBeforeReturn();
		return json;
	}

	@Override
	public String selectJson(String sqlStr, Map<String, Object> map) {

		doBeforePasreEntity();

		String sql = initPrepareValuesViaMap(sqlStr, map);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_JSON_SQL, sql);

		String json = getBeeSql().selectJson(sql);
		doBeforeReturn();
		return json;
	}

	@Override
	public String selectJson(String sqlStr, Map<String, Object> map, int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity();

		regPagePlaceholder();
		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql = initPrepareValuesViaMap(pageSql, map);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_JSON_SQL, sql);

		String json = getBeeSql().selectJson(sql);
		doBeforeReturn();
		return json;
	}

	@Override
	public String selectJson(String sql) {
		Object[] preValues = null;
		return selectJson(sql, preValues);
	}

	@Override
	public List<String[]> select(String sql) {
		Object[] preValues = null;
		return select(sql, preValues);
	}

	@Override
	public String selectFun(String sql) {
		Object[] preValues = null;
		return selectFun(sql, preValues);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> void initPreparedValues(String sql, Object[] preValues, T entity) {
		List list = _initPreparedValues(sql, preValues);
//		if (valueBuffer.length() > 0) {//bug. no placeholder will have problem.
//			String tableName = _toTableName(entity);
//			HoneyContext.setPreparedValue(sql, list);  
//			addInContextForCache(sql, tableName);  //有T才放缓存.

		String tableName = "";
		if (isNeedRealTimeDb()) {
			tableName = (String) OneTimeParameter.getAttribute(StringConst.TABLE_NAME);
			if (tableName == null) {
				tableName = _toTableName(entity);
			}
		} else {
			tableName = _toTableName(entity);
		}

		//pre page 1, 3
		HoneyUtil.setPageNum(list);
		HoneyContext.setContext(sql, list, tableName);
		//		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initPreparedValues(String sql, Object[] preValues) {
//		if(preValues==null || preValues.length==0) return ; //如果加了分页的,要使用setPageNum
		
		List list = _initPreparedValues(sql, preValues);
		// pre page 不放缓存 5,7
		boolean isSetted=HoneyUtil.setPageNum(list);
		
		if ((preValues == null || preValues.length == 0) && !isSetted)
			return; // 参数为空,且没有设置分页参数,则不设置 setPreparedValue
		else
			HoneyContext.setPreparedValue(sql, list); // 没有entity,不放缓存.
		
		_addTableforCacheIfNeed(sql); //2.4.0
	}
	
	//2.4.0
	private void _addTableforCacheIfNeed(String sql) {
		//2.4.0  关联方法没有T参数的,使其也可以纳入缓存管理
		String tablename = getRelativeTableOneTime();
		if (StringUtils.isNotBlank(tablename))
			HoneyContext.addInContextForCache(sql, tablename);   
		
//		注意:若该方法位置转换sql前面,有转换sql的,转换后还要用新sql更新缓存及上下文
	}

	@SuppressWarnings("rawtypes")
	private List _initPreparedValues(String sql, Object[] preValues) {

		if (sql == null || "".equals(sql.trim())) {
			throw new SqlNullException(STRING_IS_NULL);
		}

		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();

		for (int i = 0; preValues != null && i < preValues.length; i++) { //fixbug
			preparedValue = new PreparedValue();
			preparedValue.setType(preValues[i].getClass().getName());
			preparedValue.setValue(preValues[i]);
			list.add(preparedValue);
		}
		return list;
	}

	private <T> Map<String, Object> mergeMap(Map<String, Object> prameterMap, T entity) {
		Map<String, Object> columnMap = HoneyUtil.getColumnMapByEntity(entity);
		if(prameterMap!=null) columnMap.putAll(prameterMap); //merge, prameterMap will override columnMap,if have same key.
		return columnMap;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> String initPrepareValuesViaMap(String sqlStr, Map<String, Object> parameterMap,
			T entity) {

		if (sqlStr == null || "".equals(sqlStr.trim())) {
			throw new SqlNullException(STRING_IS_NULL);
		}
		parameterMap = mergeMap(parameterMap, entity);

		SqlValueWrap wrap = processSql2(sqlStr,parameterMap); //will return null when sql no placeholder like: select * from tableName
		String reSql;
		List list = null;

		String tableName = "";
		if (isNeedRealTimeDb()) {
			tableName = (String) OneTimeParameter.getAttribute(StringConst.TABLE_NAME);
			if (tableName == null) {
				tableName = _toTableName(entity);
			}
		} else {
			tableName = _toTableName(entity);
		}
//		String tableName = _toTableName(entity);

		if (wrap == null) {
			reSql = sqlStr;
			list = new ArrayList();
		} else {
//			String sql = wrap.getSql();
//			String mapKeys = wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
//			list = _initPreparedValues(mapKeys, parameterMap);
			list=wrap.getList();
			reSql = wrap.getSql();
		}

		HoneyUtil.setPageNum(list);
		//MAP PAGE 2,4
		HoneyContext.setContext(reSql, list, tableName);

		return reSql;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String initPrepareValuesViaMap(String sqlStr, Map<String, Object> map) {

		if (StringUtils.isBlank(sqlStr))  {
			throw new SqlNullException(STRING_IS_NULL);
		}
		
//       //if (ObjectUtils.isEmpty(map)) return sqlStr;  //TODO 2.4
//		SqlValueWrap wrap = processSql2(sqlStr,map); //bug.  wrap maybe null
//		if (wrap == null || ObjectUtils.isEmpty(map)) return sqlStr; //fix null bug     //map为空也有可能是要设置分页,不能提前返回 
//		String sql = wrap.getSql();
////		String mapKeys = wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
////		List list = _initPreparedValues(mapKeys, map);
		
		
		//V2.4.0
		SqlValueWrap wrap = processSql2(sqlStr,map);
//		if (wrap == null) return sqlStr;  //bug  没有起止标签,返回null,但可能是要加分页参数
		
		String sql;
		List list = null;
		
		if (wrap == null) {//map为空也有可能是要设置分页,不能提前返回
			sql = sqlStr;
			list = new ArrayList();
		} else {
 			list = wrap.getList();
			sql = wrap.getSql();
		}
		
		//6,8  map,page 不放缓存
		boolean isSetted=HoneyUtil.setPageNum(list); //设置分页参数
		
		if (ObjectUtils.isEmpty(map) && !isSetted) {
			// 参数为空,且没有设置分页参数,则不设置 setPreparedValue
		} else {
			HoneyContext.setPreparedValue(sql, list);
		}
		
		_addTableforCacheIfNeed(sql); //2.4.0
		
		return sql;
	}
	
	@SuppressWarnings("rawtypes")
	private List _initPreparedValues(String keys[], Map<String, Object> map) {
		Object value;
		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();

		for (int i = 0; i < keys.length; i++) {
			preparedValue = new PreparedValue();
			value = map.get(keys[i]);
			preparedValue.setValue(value);
			
//			preparedValue.setType(map.get(keys[i]).getClass().getName()); //null bug
			//fixed bug V2.0
			if (value != null)
				preparedValue.setType(value.getClass().getName());
			else  
				preparedValue.setType(Object.class.getName());
			
			list.add(preparedValue);
		}
		return list;
	}
	
	private SqlValueWrap processSql(String sql) {
		return TokenUtil.process(sql, "#{", "}", "?");
	}
	
	@SuppressWarnings("rawtypes")
	private SqlValueWrap processSql2(String sql,Map map) {
		return TokenUtil.process2(sql, "#{", "}", "?",map);
	}

	private String _toTableName(Object entity) {
//		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
		return HoneyUtil.toTableName(entity);  //fixed bug 2.1
	}

	private void regPagePlaceholder() {
		HoneyUtil.regPagePlaceholder();
	}

	private boolean isNeedRealTimeDb() {
		return HoneyContext.isNeedRealTimeDb();
	}

	@Override
	public List<Map<String, Object>> selectMapList(String sql) {
		return selectMapList(sql,null);
	}
	
	@Override
	public List<Map<String, Object>> selectMapList(String sqlStr, Map<String, Object> parameterMap) {
		doBeforePasreEntity();
		String sql = initPrepareValuesViaMap(sqlStr, parameterMap);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql selectMapList SQL: ", sql);

		List<Map<String, Object>> list = getBeeSql().selectMapList(sql);
		doBeforeReturn();
		
		return list;
	}
	

	@Override
	public List<Map<String, Object>> selectMapList(String sqlStr, Map<String, Object> parameterMap,
			int start, int size) {
		
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity();

		regPagePlaceholder();
		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql = initPrepareValuesViaMap(pageSql, parameterMap);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql selectMapList SQL: ", sql);
		List<Map<String, Object>> list = getBeeSql().selectMapList(sql);

		doBeforeReturn();
		return list;
	}

	@Override
	public <T> List<T> moreTableSelect(String sqlStr, T returnType) {
		return moreTableSelect(sqlStr, returnType, null);
	}

	@Override
	public <T> List<T> moreTableSelect(String sqlStr, T entity, Map<String, Object> map) {
		doBeforePasreEntity(entity, SuidType.SELECT);
		String sql = initPrepareValuesViaMap(sqlStr, map, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_MoreTable_SQL, sql);
		List<T> list = getBeeSql().select(sql, toClassT(entity));

		doBeforeReturn(list);
		return list;
	}
	
	private <T> String _moreTableSelect(String sqlStr, T entity, Map<String, Object> map,
			int start, int size) {
		if (size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		if (start < 0) throw new BeeIllegalParameterException(START_GREAT_EQ_0);

		doBeforePasreEntity(entity, SuidType.SELECT);

		regPagePlaceholder();

		String tableName = "";
		if (isNeedRealTimeDb()) {
			tableName = _toTableName(entity); //这里,取过了参数, 到解析sql的,就不能再取
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}

		String pageSql = getDbFeature().toPageSql(sqlStr, start, size);
		String sql = initPrepareValuesViaMap(pageSql, map, entity);
		sql = doAfterCompleteSql(sql);
		
		return sql;
	}

	@Override
	public <T> List<T> moreTableSelect(String sqlStr, T entity, Map<String, Object> map, int start,
			int size) {
		String sql = _moreTableSelect(sqlStr, entity, map, start, size);
		Logger.logSQL(SELECT_MoreTable_SQL, sql);
		List<T> list = getBeeSql().moreTableSelect(sql, entity);

		doBeforeReturn(list);
		return list;
	}
	
	private static boolean  showSQL=HoneyConfig.getHoneyConfig().showSQL;
	private static final String INDEX1 = "_SYS[index";
	private static final String INDEX2 = "]_End ";
	private static final String INDEX3 = "]";
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int insertBatch(String sqlStr, List<Map<String, Object>> parameterMapList, int batchSize) {

		if (sqlStr == null || "".equals(sqlStr.trim())) {
			throw new SqlNullException(STRING_IS_NULL);
		}

		if (ObjectUtils.isEmpty(parameterMapList)) {
			Logger.warn("parameterMapList is empty!");
			return 0;
		}
		
		sqlStr = HoneyUtil.deleteLastSemicolon(sqlStr);

		doBeforePasreEntity2();

		int size = parameterMapList.size();

		SqlValueWrap wrap = processSql(sqlStr);
		if (wrap == null || ObjectUtils.isEmpty(parameterMapList.get(0))) return 0;
		String insertSql[] = new String[size];
		insertSql[0] = wrap.getSql();

		String mapKeys = wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
		String keys[] = mapKeys.split(","); //map's key

		insertSql[0] = doAfterCompleteSql(insertSql[0]); //提前转换sql,可以少更新上下文
		_addTableforCacheIfNeed(insertSql[0]); //2.4.0

		String sql_i = null;
		List<PreparedValue> preparedValueList = new ArrayList<>();
		
		for (int i = 0; i < size; i++) {
			List oneRecoreList = _initPreparedValues(keys, parameterMapList.get(i));
			sql_i = INDEX1 + i + INDEX2 + insertSql[0];
			if (HoneyUtil.isMysql()) {
				if (i == 0) {
					OneTimeParameter.setAttribute("_SYS_Bee_PlaceholderValue", getPlaceholderValue(keys.length)); //fixed bug V2.0
					HoneyContext.setPreparedValue(sql_i, oneRecoreList);
				}
				preparedValueList.addAll(oneRecoreList); //用于mysql批量插入时设置值
				if((i+1)%batchSize==0){ //i+1    i+1不可能为0
					HoneyContext.setPreparedValue(insertSql[0] +"  [Batch:"+ (i/batchSize) + INDEX3, preparedValueList); //i
					preparedValueList = new ArrayList<>();
				}else if(i==(size-1)){
					HoneyContext.setPreparedValue(insertSql[0] +"  [Batch:"+ (i/batchSize) + INDEX3, preparedValueList); //i
				}
			}
			if (HoneyUtil.isMysql() && !showSQL) {
				//none
			} else {
				HoneyContext.setPreparedValue(sql_i, oneRecoreList);
			}
		}
//		HoneyContext.test();
		int a = getBeeSql().batch(insertSql,batchSize);
		doBeforeReturn();

		return a;

	}
	
	private String getPlaceholderValue(int size) {
		return HoneyUtil.getPlaceholderValue(size);
	}

	@Override
	public int insertBatch(String sqlStr, List<Map<String, Object>> parameterMapList) {
		int batchSize = HoneyConfig.getHoneyConfig().insertBatchSize;
		return insertBatch(sqlStr, parameterMapList, batchSize);
	}
	
	// 2.4.0
	@Override
	public void setRelativeTableOneTime(String... table) {
		if (StringUtils.isNotEmpty(table))
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME_RELATIVE, table);
	}

	// 2.4.0
	@Override
	public String getRelativeTableOneTime() {
		String[] tablename = (String[]) OneTimeParameter.getAttribute(StringConst.TABLE_NAME_RELATIVE);
		String names = null;
		if (StringUtils.isNotEmpty(tablename)) {
			if (tablename.length == 1)
				names = tablename[0].trim();
			else if (tablename.length > 1) {
				names = tablename[0].trim();
				for (int i = 1; i < tablename.length; i++) {
					if (StringUtils.isNotBlank(tablename[i]))
						names += StringConst.TABLE_SEPARATOR + tablename[i].trim();
				}
			}
		}
		return names;
	}

	private void doBeforePasreEntity() {
		Object entity=null;
		super.doBeforePasreEntity(entity, SuidType.SELECT);
	}
	
	private void doBeforePasreEntity2() {
		Object entity=null;
		super.doBeforePasreEntity(entity, SuidType.MODIFY);
	}

}
