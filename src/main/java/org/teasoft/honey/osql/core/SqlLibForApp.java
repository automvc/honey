/*
 * Copyright 2016-2026 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.app.BeeSqlForApp;
import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.type.SetParaTypeConvert;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.type.SetParaTypeConverterRegistry;

/**
 * @author Kingstar
 * @since  1.17
 */
public class SqlLibForApp extends AbstractBase implements BeeSql, Serializable {

	private static final long serialVersionUID = 1596710362262L;

	private static boolean showSQL = HoneyConfig.getHoneyConfig().showSQL;

	private BeeSqlForApp beeSqlForApp;

	private static boolean isFirst = true;
	private static String SUCCESS_MSG = "[Bee] ==========Load BeeSqlForApp implement class successfully!";

	@Override
	public ResultSet selectRs(String sql) {
		Logger.warn("[Bee] ==========SqlLibForApp do not support the method: selectRs");
		return null;
	}

	public BeeSqlForApp getBeeSqlForApp() {
		if (beeSqlForApp != null) return beeSqlForApp;
		try {
			if (HoneyConfig.getHoneyConfig().isAndroid)
				beeSqlForApp = (BeeSqlForApp) Class.forName("org.teasoft.beex.android.SqlLibExtForAndroid").newInstance();
			else if (HoneyConfig.getHoneyConfig().isHarmony)
				beeSqlForApp = (BeeSqlForApp) Class.forName("org.teasoft.beex.harmony.SqlLibExtForHarmony").newInstance();
			if (isFirst) {
				Logger.info(SUCCESS_MSG);
				isFirst = false;
			} else {
				Logger.debug(SUCCESS_MSG);
			}
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		}

		if (beeSqlForApp == null) {
			Logger.warn("[Bee] ==========Load BeeSqlForApp implement class fail!");
			beeSqlForApp = new SqlLibEmptyForApp();
		}

		return beeSqlForApp;
	}

	public void setBeeSqlForApp(BeeSqlForApp beeSqlForApp) {
		this.beeSqlForApp = beeSqlForApp;
	}

	@Override
	public <T> List<T> select(String sql, Class<T> entityClass) {
		return selectSomeField(sql, entityClass);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> selectSomeField(String sql, Class<T> entityClass) {

		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();

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
		List<T> rsList = null;
		try {
			rsList = getBeeSqlForApp().select(sql, entityClass, toStringArray(sql));
			addInCache(sql, rsList, rsList.size());

		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
		}
		logSelectRows(rsList.size());

		return rsList;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String selectFun(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		if (sql == null || "".equals(sql.trim())) return null;

		boolean isReg = updateInfoInCache(sql, "String", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, null, sql);
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				return (String) cacheObj;
			}
		}

		String result = null;
		try {
			result = getBeeSqlForApp().selectFun(sql, toStringArray(sql));

			addInCache(sql, result, 1);
		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
		}

		return result;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String[]> select(String sql) {

		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();
		boolean isReg = updateInfoInCache(sql, "List<String[]>", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, null, sql);
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				List<String[]> list = (List<String[]>) cacheObj;
				logSelectRows(list.size());
				return list;
			}
		}

		List<String[]> list = new ArrayList<>();
		try {
			list = getBeeSqlForApp().select(sql, toStringArray(sql));

			logSelectRows(list.size());
			addInCache(sql, list, list.size());

		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
		}

		return list;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> selectMapList(String sql) {

		if (sql == null || "".equals(sql.trim())) return Collections.emptyList();
		boolean isReg = updateInfoInCache(sql, "List<Map<String,Object>>", SuidType.SELECT, null);
		if (isReg) {
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
		try {
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			list = getBeeSqlForApp().selectMapList(exe_sql, toStringArray(sql));

			logSelectRows(list.size());

			addInCache(sql, list, list.size());

		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
		}

		return list;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String selectJson(String sql) {

		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
		if (sql == null || "".equals(sql.trim())) return null;

		boolean isReg = updateInfoInCache(sql, "StringJson", SuidType.SELECT, entityClass);
		if (isReg) {
			initRoute(SuidType.SELECT, entityClass, sql);
			Object cacheObj = getCache().get(sql); // 这里的sql还没带有值
			if (cacheObj != null) {
				clearContext(sql);
				return (String) cacheObj;
			}
		}

		String json = "";
		try {
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);

			json = getBeeSqlForApp().selectJson(exe_sql, toStringArray(sql), entityClass);

			addInCache(sql, json, -1); // 没有作最大结果集判断

		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
		}

		return json;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public int modify(String sql) {
		Class entityClass = (Class) OneTimeParameter.getAttribute(StringConst.Route_EC);
//		if(sql==null || "".equals(sql)) return -2;
		if (sql == null || "".equals(sql)) return -1; // 2.4.0

		initRoute(SuidType.MODIFY, entityClass, sql);

		int num = 0;
		try {
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			num = getBeeSqlForApp().modify(exe_sql, toObjArray(sql));
		} finally {
			clearInCache(sql, "int", SuidType.MODIFY, num); // has clearContext(sql)
		}
		Logger.logSQL(" | <--  Affected rows: " + num);

		return num;
	}

	@Override
	public long insertAndReturnId(String sql) {
//		if (sql == null || "".equals(sql)) return -2L;
		if (sql == null || "".equals(sql)) return -1L; // 2.4.0

		initRoute(SuidType.INSERT, null, sql);

		long returnId = -1L;
		int num = 1;
		try {
			String exe_sql = HoneyUtil.deleteLastSemicolon(sql);
			returnId = getBeeSqlForApp().insertAndReturnId(exe_sql, toObjArray(sql));
			if (returnId == -1) num = 0;
		} finally {
			clearInCache(sql, "int", SuidType.INSERT, num);
		}
		Logger.logSQL(" | <--  Affected rows: " + num);

		return returnId;
	}

	@Override
	public int batch(String[] sql) {
		if (sql == null) return -1;
		int batchSize = HoneyConfig.getHoneyConfig().insertBatchSize;

		return batch(sql, batchSize);
	}

	@Override
	public int batch(String sql[], int batchSize) {

		if (sql == null || sql.length < 1) return -1;

		initRoute(SuidType.INSERT, null, sql[0]);

		int len = sql.length;
		int total = 0;
		int temp = 0;
		try {
			if (len <= batchSize) {
				total = batch(sql[0], 0, len);
			} else {
				for (int i = 0; i < len / batchSize; i++) {
					temp = batch(sql[0], i * batchSize, (i + 1) * batchSize);
					total += temp;
				}

				if (len % batchSize != 0) { // 尾数不成批
					temp = batch(sql[0], len - (len % batchSize), len);
					total += temp;
				}
			}
		} catch (Exception e) {
			clearContext(sql[0], batchSize, len);
		} finally {
			// 更改操作需要清除缓存
			clearInCache(sql[0], "int[]", SuidType.INSERT, total);
		}

		return total;
	}

	private static final String INDEX1 = "_SYS[index";
	private static final String INDEX2 = "]_End ";
	private static final String INDEX3 = "]";
	private static final String INSERT_ARRAY_SQL = " insert[] SQL : ";

	private int batch(String sql, int start, int end) {
		int a = 0;

		List<Object[]> listBindArgs = new ArrayList<>(end - start);
		for (int i = start; i < end; i++) { // start... (end-1)
			if (showSQL) {
				if (i == 0) Logger.logSQL(LogSqlParse.parseSql(INSERT_ARRAY_SQL, sql));

				OneTimeParameter.setAttribute("_SYS_Bee_BatchInsert", i + "");
				String sql_i = INDEX1 + i + INDEX2 + sql;
				Logger.logSQL(LogSqlParse.parseSql(INSERT_ARRAY_SQL, sql_i));
			}

			listBindArgs.add(toObjArray(INDEX1 + i + INDEX2 + sql, false));
		}

		sql = HoneyUtil.deleteLastSemicolon(sql);// 上面的sql还不能执行去分号,要先拿了缓存.
		a = getBeeSqlForApp().batchInsert(sql, listBindArgs);

		Logger.logSQL(" | <-- index[" + (start) + "~" + (end - 1) + INDEX3 + " Affected rows: "+ a);

		return a;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> List<T> moreTableSelect(String sql, final T entity) {

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

		Class<T> entityClass = toClassT(entity);

//		// Assemble the result by yourself. 多表查询是注册有,才会用.
//		if (ResultAssemblerRegistry.hadReg(entityClass)) return _moreTableSelectAssemble(sql, entityClass);
//		// 没注册有,则用后面的解析

		List<T> rsList;
		try {

			List<Map<String, String>> rsMapList = getBeeSqlForApp().selectMapListWithColumnName(HoneyUtil
					.deleteLastSemicolon(sql),
					toStringArray(sql));

			// process result; 将查询结果转化成中间对象
			List<MoreTableResultWrapper<T>> listResultWrapper = TransformResultSet.toMidRsForMoretable(rsMapList,
					entity);

			rsList = TransformResultSet.genRsViaMidRsForMoretable(listResultWrapper, entityClass, moreTableStructMap);

			addInCache(sql, rsList, rsList.size());

		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException
				| InstantiationException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			clearContext(sql);
		}

		logSelectRows(rsList.size());

		return rsList;
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> toClassT(T entity) {
		return (Class<T>) entity.getClass();
	}

	private static final String[] EMPTY_ARRAY = new String[0];

	private String[] toStringArray(String sql) {
		List<PreparedValue> list = HoneyContext.justGetPreparedValue(sql);
		if (list == null) {
			return EMPTY_ARRAY;
		} else {
			String str[] = new String[list.size()];
			Object ob;
			for (int i = 0; i < list.size(); i++) {
				ob = list.get(i).getValue();
				if (ob != null) str[i] = ob.toString();
				else str[i] = null;
			}
			return str;
		}
	}

	private Object[] toObjArray(String sql) {
		return toObjArray(sql, true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object[] toObjArray(String sql, boolean justGet) {
		List<PreparedValue> list;
		if (justGet) list = HoneyContext.justGetPreparedValue(sql);
		else list = HoneyContext.getAndClearPreparedValue(sql); // 用于批处理.

		if (list == null) {
			return EMPTY_ARRAY;
		} else {
			Object obj[] = new Object[list.size()];
			for (int i = 0; i < list.size(); i++) {
				obj[i] = list.get(i).getValue();

				// 提前将Json字段转成Json 字符串????
				int jsonType = list.get(i).getJsonType();
				if (jsonType == 1) { // jsut json; app no use pgsql, no jsonb
					SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
					if (converter != null) {
						obj[i] = converter.convert(obj[i]);
					}
				}
			}
			return obj;
		}
	}

}
