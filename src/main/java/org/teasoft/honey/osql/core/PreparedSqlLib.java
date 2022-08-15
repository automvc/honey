package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.PreparedSql;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.bee.osql.exception.SqlNullException;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.util.ObjectUtils;

/**
 * 支持带占位符(?)的sql操作.sql语句是DB能识别的SQL,非面向对象的sql.
 * 若是简单的操作,建议用面向对象的操作方式,ObjSQL和ObjSQLRich.
 *  <br>Support sql string with placeholder.The sql statement is really DB's grammar,not object oriented type.
 * @author Kingstar
 * @since  1.0
 * 支持如name=#{name},name like #{name%}的map参数形式
 * @since  1.2
 */
public class PreparedSqlLib implements PreparedSql {

	private BeeSql beeSql;

	//V1.11
	private InterceptorChain interceptorChain;
	
	private String dsName;//用于设置当前对象使用的数据源名称
	private NameTranslate nameTranslate; //用于设置当前对象使用的命名转换器.使用默认的不需要设置
	
	private static final String SELECT_SQL = "PreparedSql select SQL: ";
	private static final String SELECT_MoreTable_SQL = "PreparedSql select MoreTable SQL: ";
	private static final String SELECT_SOME_FIELD_SQL = "PreparedSql selectSomeField SQL: ";
	private static final String SELECT_JSON_SQL = "PreparedSql selectJson SQL: ";
	private static final String STRING_IS_NULL = "sql statement string is Null !";
	private static final String START_GREAT_EQ_0 = "Parameter 'start' need great equal 0!";
	private static final String SIZE_GREAT_0 = "Parameter 'size' need great than 0!";

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
	public InterceptorChain getInterceptorChain() {
		if (interceptorChain == null)
			interceptorChain = BeeFactory.getHoneyFactory().getInterceptorChain();
		return HoneyUtil.copy(interceptorChain);
	}

	public void setInterceptorChain(InterceptorChain interceptorChain) {
		this.interceptorChain = interceptorChain;
	}

	@Override
	public void setDataSourceName(String dsName) {
		this.dsName = dsName;
	}

	@Override
	public String getDataSourceName() {
		return dsName;
	}
	
	@Override
	public void setNameTranslate(NameTranslate nameTranslate) {
		this.nameTranslate=nameTranslate;
	}
	

	@Override
	public <T> List<T> select(String sql, T returnType, Object[] preValues) {
		doBeforePasreEntity(returnType, SuidType.SELECT);//returnType的值,虽然不用作占位参数的值,但可以用作拦截器的业务逻辑判断
		initPreparedValues(sql, preValues, returnType);
		sql = doAfterCompleteSql(sql);

		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().select(sql, returnType);

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(String sql, T returnType) {
		Object[] preValues = null;
		return select(sql, returnType, preValues);
	}

	@Override
	public <T> List<T> select(String sql, T entity, Object[] preValues, int start, int size) {
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
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().select(sql, entity);

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(String sqlStr, T entity, Map<String, Object> map) {
		doBeforePasreEntity(entity, SuidType.SELECT);
		String sql = initPrepareValuesViaMap(sqlStr, map, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().select(sql, entity);

		doBeforeReturn(list);
		return list;
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
		List<T> list = getBeeSql().select(sql, entity);

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> selectSomeField(String sql, T entity, Object[] preValues) {

		doBeforePasreEntity(entity, SuidType.SELECT);

		initPreparedValues(sql, preValues, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SOME_FIELD_SQL, sql);
		List<T> list = getBeeSql().selectSomeField(sql, entity);

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
		List<T> list = getBeeSql().selectSomeField(sql, entity);

		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> selectSomeField(String sqlStr, T entity, Map<String, Object> map) {

		doBeforePasreEntity(entity, SuidType.SELECT);

		String sql = initPrepareValuesViaMap(sqlStr, map, entity);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SOME_FIELD_SQL, sql);
		List<T> list = getBeeSql().selectSomeField(sql, entity);

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
		List<T> list = getBeeSql().selectSomeField(sql, entity);

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
	@Deprecated
	public int modify(String sql, Object[] preValues) {

		doBeforePasreEntity();

		initPreparedValues(sql, preValues);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql modify SQL: ", sql);
		int r = getBeeSql().modify(sql);
		doBeforeReturn();
		return r;
	}

	@Override
	@Deprecated
	public int modify(String sqlStr, Map<String, Object> map) {

		doBeforePasreEntity();

		String sql = initPrepareValuesViaMap(sqlStr, map);

		sql = doAfterCompleteSql(sql);
		Logger.logSQL("PreparedSql modify SQL: ", sql);
		int r = getBeeSql().modify(sql);
		doBeforeReturn();
		return r;
	}

	@Override
	@Deprecated
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

	private void initPreparedValues(String sql, Object[] preValues) {
		List list = _initPreparedValues(sql, preValues);
		// pre page 不放缓存 5,7
		HoneyUtil.setPageNum(list);
		HoneyContext.setPreparedValue(sql, list); //没有entity,不放缓存.
	}

	//	private StringBuffer initPreparedValues(String sql, Object[] preValues) {
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
			String sql = wrap.getSql();
//			String mapKeys = wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
//			list = _initPreparedValues(mapKeys, parameterMap);
			list=wrap.getList();
			reSql = sql;
		}

		HoneyUtil.setPageNum(list);
		//MAP PAGE 2,4
		HoneyContext.setContext(reSql, list, tableName);

		return reSql;
	}

	private String initPrepareValuesViaMap(String sqlStr, Map<String, Object> map) {

		if (sqlStr == null || "".equals(sqlStr.trim())) {
			throw new SqlNullException(STRING_IS_NULL);
		}

		SqlValueWrap wrap = processSql2(sqlStr,map); //bug.  wrap maybe null
		if (wrap == null || ObjectUtils.isEmpty(map)) return sqlStr; //fix null bug
		String sql = wrap.getSql();
//		String mapKeys = wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
//		List list = _initPreparedValues(mapKeys, map);
		List list =wrap.getList();
		//6,8  map,page 不放缓存
		HoneyUtil.setPageNum(list);
		HoneyContext.setPreparedValue(sql, list);
		return sql;
	}
	
//	private List _initPreparedValues(String mapKeys, Map<String, Object> map) {
//		String keys[] = mapKeys.split(","); //map's key
//		return _initPreparedValues(keys, map,false);
//	}
	
//	private List _initPreparedValues(String keys[], Map<String, Object> map,boolean noWhere) {
	private List _initPreparedValues(String keys[], Map<String, Object> map) {
		Object value;
		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();

		for (int i = 0; i < keys.length; i++) {
			preparedValue = new PreparedValue();
			value = map.get(keys[i]);
			preparedValue.setValue(value);
			preparedValue.setType(map.get(keys[i]).getClass().getName());
			list.add(preparedValue);
		}
		return list;
	}
	
	private SqlValueWrap processSql(String sql) {
		return TokenUtil.process(sql, "#{", "}", "?");
	}
	
	private SqlValueWrap processSql2(String sql,Map map) {
		return TokenUtil.process2(sql, "#{", "}", "?",map);
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
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
		List<T> list = getBeeSql().select(sql, entity);

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

		doBeforePasreEntity();

		int size = parameterMapList.size();

		SqlValueWrap wrap = processSql(sqlStr);
		if (wrap == null || ObjectUtils.isEmpty(parameterMapList.get(0))) return 0;
		String insertSql[] = new String[size];
		insertSql[0] = wrap.getSql();

		String mapKeys = wrap.getValueBuffer().toString(); //wrap.getValueBuffer() is :map's key , get from like: #{name}
		String keys[] = mapKeys.split(","); //map's key

		insertSql[0] = doAfterCompleteSql(insertSql[0]); //提前转换sql,可以少更新上下文

		String sql_i = null;
		List<PreparedValue> preparedValueList = new ArrayList<>();
		
		for (int i = 0; i < size; i++) {
			List oneRecoreList = _initPreparedValues(keys, parameterMapList.get(i));
			sql_i = INDEX1 + i + INDEX2 + insertSql[0];
			if (HoneyUtil.isMysql()) {
				if (i == 0) {
					OneTimeParameter.setAttribute("_SYS_Bee_PlaceholderValue", getPlaceholderValue(size));
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
	
	
	private void doBeforePasreEntity() {
		if (this.dsName != null) HoneyContext.setTempDS(dsName);
		if(this.nameTranslate!=null) HoneyContext.setCurrentNameTranslate(nameTranslate);
		getInterceptorChain().beforePasreEntity(null, SuidType.SELECT);
	}

	private void doBeforePasreEntity(Object entity, SuidType suidType) {//都是select在用
		if (this.dsName != null) HoneyContext.setTempDS(dsName);
		if(this.nameTranslate!=null) HoneyContext.setCurrentNameTranslate(nameTranslate);
		getInterceptorChain().beforePasreEntity(entity, suidType);
	}

	private String doAfterCompleteSql(String sql) {
		//if change the sql,need update the context.
		sql = getInterceptorChain().afterCompleteSql(sql);
		return sql;
	}

	@SuppressWarnings("rawtypes")
	private void doBeforeReturn(List list) {
		if (this.dsName != null) HoneyContext.removeTempDS();
		if(this.nameTranslate!=null) HoneyContext.removeCurrentNameTranslate();
		getInterceptorChain().beforeReturn(list);
	}

	private void doBeforeReturn() {
		if (this.dsName != null) HoneyContext.removeTempDS();
		if(this.nameTranslate!=null) HoneyContext.removeCurrentNameTranslate();
		getInterceptorChain().beforeReturn();
	}

}
