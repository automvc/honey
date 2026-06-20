package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.JoinType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.StringUtils;

class _MoreObjectToSQLHelper3 {

	private static final String COMMA = ",";
	private static final String ONE_SPACE = " ";
	private static final String EQUAL = " = ";
	private static final String EQUAL_QUEST = " = ?";
	private static final String DOT = ".";
	private static final String AND = " " + K.and + " ";

	private static final int DEFAULT_INCLUDE_TYPE = IncludeType.EXCLUDE_BOTH.getValue();

	private _MoreObjectToSQLHelper3() {}

	static <T> String _toSelectSQL(T entity) {
		return _toSelectSQL(entity, DEFAULT_INCLUDE_TYPE, null);
	}

	static <T> String _toSelectSQL(T entity, int start, int size) {
		Condition condition = BeeFactoryHelper.getCondition();
		condition.start(start);
		condition.size(size);
		condition.setSuidType(SuidType.SELECT);
		return _toSelectSQL(entity, DEFAULT_INCLUDE_TYPE, condition);
	}

	static <T> String _toSelectSQL(T entity, Condition condition) {
		int includeType;
		if (condition == null || condition.getIncludeType() == null)
			includeType = DEFAULT_INCLUDE_TYPE;
		else
			includeType = condition.getIncludeType().getValue();

		if (condition != null) condition.setSuidType(SuidType.SELECT);

		return _toSelectSQL(entity, includeType, condition);
	}

	// TODO includeType 还没有使用。
	private static <T> String _toSelectSQL(T entity, int includeType, Condition condition) {
		checkPackage(entity);

		if (includeType == IncludeType.INCLUDE_NULL.getValue() || includeType == IncludeType.INCLUDE_BOTH.getValue()) {
			throw new ConfigWrongException(
					"Do not use the entity field which value is null in Moretable! Can use condition.op(field, Op.eq, null)");
		}

		Map<String, MoreTableStruct3> moreTableStructMap = ParseSqlHelper.parseJoins(entity);
		if (moreTableStructMap == null || moreTableStructMap.isEmpty())
			throw new BeeException("Entity for Moretable operate must have JoinTable setting!");

		String mainTableName = _toTableName(entity);
		String mainTableAlias = mainTableName;
		// 不能到分页时才设置,因多表时,有重名字段,也需要用到dbName判断使用不同的语法,而动态获取dbName要用到路由.
		if (HoneyContext.isNeedRealTimeDb()) { // TODO
			// main table confirm the Datasource.
			HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), mainTableName);
			OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
		}

		Map<String, String> subDulColumnMap = null;
		MoreTableStructOverall overall = null;

		// 拦截器处理 2.0
		InterceptorChain chain = null;
		int index = 1;
		for (Entry<String, MoreTableStruct3> entry : moreTableStructMap.entrySet()) {
			if (index == 1) {
				chain = (InterceptorChain) OneTimeParameter.getAttribute(StringConst.InterceptorChainForMoreTable);
			}
			if (entry.getValue() != null) {
//			   //2.0从表对应的从实体,不用来计算分片. 从表分片的下标与主表的一致.  (分片拦截器不会处理,但其它拦截器要执行)
				doBeforePasreSubEntity(entry.getValue().subObject, chain);// 应该要放到这,要先拦截处理,再解析

				if (index == 1) overall = entry.getValue().overall;

				boolean isConfuseDuplicateFieldDB = HoneyUtil.isConfuseDuplicateFieldDB();
				if (isConfuseDuplicateFieldDB && index == 1) {
					subDulColumnMap = new LinkedHashMap<>();
					entry.getValue().overall.subDulColumnMap = subDulColumnMap;
				}
			}
			index++;
		}

		boolean checkGroup = false;
		List<String> groupNameslist = null;
		int groupSize = 0;
		if (condition != null) {
			groupNameslist = condition.getGroupByFields();
			groupSize = groupNameslist == null ? -1 : groupNameslist.size();
			checkGroup = OneTimeParameter.isTrue(StringConst.Check_Group_ForSharding) && ShardingUtil.hadSharding();
			if (checkGroup) checkGroup = checkGroup && groupSize >= 1;
			OneTimeParameter.setTrueForKey(StringConst.Get_GroupFunStruct);
		}

		boolean needRewritePagingSql = true;
		if (needRewritePagingSql) {
			if (condition == null) { // 没有condition,不可能有分页
				needRewritePagingSql = false;
				// 0.用户主动设置本次查询不需要分页改写
			} else if (condition != null && condition.isDoNotRewritePagingSql()) {
				needRewritePagingSql = false;
			} else if (groupSize >= 1) { // 1. 有分组
				// 有分组; 不需要分页改写; 前面已计算有结果，先利用，避免不必要的计算。
				needRewritePagingSql = false; // 有使用聚合查询就不用改写 （与分片无关）
			} else if (HoneyContext.getSysCommStrInheritableLocal(StringConst.FunType) != null) {
				// 2.有聚合查询; 不需要分页改写;
				needRewritePagingSql = false;
			} else if (!ConditionHelper3.isNeedRewriteSqlByPage(condition)) {
				// 3.无分页 (有分页才要改写)
				needRewritePagingSql = false;
			} else if (!overall.isHasAnySubListEntity()) {
				// 4.没有一对多
				needRewritePagingSql = false;
			}

			// 只查询主表的数据

			// 以下为排除不需要改写分页的算法(伪代码表示)
			// 可以自动判断，决定是否进行精确分页改写；
			// 需要改写分页sql，进行以下判断，看是否是真的需要改写
			// 以下是必要非充分条件
			// 1) 有分页
			// 2) 有一对多

			// 以下有一条满足则不需要改写:
			// 1) 只查询主表的数据； ??? 如何检测??
			// 2) 有聚合查询；或有分组
			// 3）可以确定最多只能查到一条主表记录 (所有多的子表，都设置有主键值)
			//

//			if (needRewritePagingSql) {// 主表设置了主键值,只查一条记录,不用改写
//				Object idValeu = HoneyUtil.getIdValue(entity);
//				if (idValeu != null) needRewritePagingSql = false;
//			}
			// 主表有主键值，但可能从表对应了多表也不行。

			// 若符合条件，则设置改写标识为false
			// needRewritePagingSql = false;
		}

		StringBuffer fullColumns = new StringBuffer();
		StringBuffer joinPart = new StringBuffer();
		StringBuffer tablePart = new StringBuffer();
		StringBuffer joinExp;
		StringBuffer filter = new StringBuffer();
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer tableNamesForCache = new StringBuffer();

		List<PreparedValue> preList0 = new ArrayList<>();
		String tempTablePlaceholder = "#{temp-table-bee_paging}#";
		String mainColumn;
		String subColumn;
		Set<String> columnSet = new HashSet<>();
		EntityWrapper mainEntityWrapper = ParseSqlHelper.parseEntity(entity, includeType);
		tableNamesForCache.append(mainTableName); // 用于缓存记录，要用真正的表名，不能用别名

		concat(fullColumns, COMMA, mainTableName + ".", mainEntityWrapper.columnList);
		columnSet.addAll(mainEntityWrapper.columnList); // TODO TEST columnSet

		Map<String, Object> columnAndValue = mainEntityWrapper.columnAndValue;
		for (Entry<String, Object> item : columnAndValue.entrySet()) {
			concat(filter, AND, mainTableName, item.getKey(), EQUAL_QUEST);
		}

		if (!mainEntityWrapper.preList.isEmpty()) preList0.addAll(mainEntityWrapper.preList);

		int structIndex = 0;
		for (Entry<String, MoreTableStruct3> entry : moreTableStructMap.entrySet()) {
			joinExp = new StringBuffer(); // fixed

			String subAlias = entry.getKey();
			MoreTableStruct3 moreTableStruct = entry.getValue();
			Class<?> subClass = moreTableStruct.subClass;
			Object subObject = moreTableStruct.subObject;
			String mainAlias = moreTableStruct.mainAlias;

			if (structIndex == 0 && StringUtils.isNotBlank(mainAlias)) mainTableAlias = mainAlias;
			structIndex++;

			EntityWrapper subEntityWrapper;
			if (subObject == null)
				subEntityWrapper = ParseSqlHelper.parseEntity(subClass, includeType);
			else
				subEntityWrapper = ParseSqlHelper.parseEntity(subObject, includeType);

			concatSubColumnName(fullColumns, COMMA, subAlias, subEntityWrapper.columnList, columnSet, subDulColumnMap);

			String subTableName = _toTableNameByClass(subClass);
			tableNamesForCache.append(StringConst.TABLE_SEPARATOR).append(subTableName);

			JoinType joinType = moreTableStruct.joinType;
			String[] mainFields = moreTableStruct.mainFields;
			String[] subFields = moreTableStruct.subFields;

			List<Class<?>> typeTree = moreTableStruct.typeTree;

			// 拼接 mainFields , subFields
			for (int i = 0; i < mainFields.length; i++) {
				if (joinExp.length() > 0) {
					joinExp.append(ONE_SPACE).append(K.and).append(ONE_SPACE);
				}
				mainColumn = toColumnName(mainFields[i], typeTree.get(moreTableStruct.layer - 2));
				subColumn = toColumnName(subFields[i], subClass);

				joinExp.append(mainAlias).append(DOT).append(mainColumn) // TODO main class??
						.append(EQUAL).append(subAlias).append(DOT).append(subColumn);
			}

			if (tablePart.length() == 0) {
				tablePart.append(ShardingUtil.appendTableIndexIfNeed(mainTableName)).append(ONE_SPACE);
//				tablePart.append(mainAlias).append(ONE_SPACE);
				tablePart.append(mainTableAlias).append(ONE_SPACE);
//				if (!mainAlias.equalsIgnoreCase(mainTableName)) {
//					tablePart.append(mainAlias).append(ONE_SPACE);
//				}
				if (needRewritePagingSql) tablePart.append(tempTablePlaceholder);
			}

			if (joinType == JoinType.WHERE) {
				if (filter.length() > 0) filter.append(ONE_SPACE).append(K.and).append(ONE_SPACE);
				filter.append(joinExp);

				tablePart.append(COMMA).append(ONE_SPACE).append(ShardingUtil.appendTableIndexIfNeed(subTableName))
						.append(ONE_SPACE);
//				if (!subAlias.equalsIgnoreCase(subTableName)) {
				tablePart.append(subAlias).append(ONE_SPACE);
//				}
			} else {
				if (HoneyUtil.isSqlKeyWordUpper())
					joinPart.append(joinType.getType().toUpperCase());
				else
					joinPart.append(joinType.getType());

				joinPart.append(ShardingUtil.appendTableIndexIfNeed(subTableName)).append(ONE_SPACE); // TODO
//				if (!subAlias.equalsIgnoreCase(subTableName)) {
				joinPart.append(subAlias).append(ONE_SPACE);
//				}
				joinPart.append(K.on).append(ONE_SPACE);
				joinPart.append(joinExp);
			}

			Map<String, Object> subColumnAndValue = subEntityWrapper.columnAndValue;
			for (Entry<String, Object> item : subColumnAndValue.entrySet()) {
				concat(filter, AND, subAlias, item.getKey(), EQUAL_QUEST);
			}

			if (!subEntityWrapper.preList.isEmpty()) preList0.addAll(subEntityWrapper.preList);
		}

		// selectColumn
		String columnNames;
		PointSelectFieldOrFunWrapper pointSelectFieldOrFunWrapper = pointSelectFieldOrFun(fullColumns.toString(),
				condition, subDulColumnMap, mainTableAlias, entity.getClass(), moreTableStructMap);
		String pointSelectFieldOrFun = pointSelectFieldOrFunWrapper.pointSelectFieldOrFun;

		if (StringUtils.isNotBlank(pointSelectFieldOrFun))
			columnNames = pointSelectFieldOrFun;
		else
			columnNames = fullColumns.toString();

		// 2.4.0 For sharding
		if (condition != null && checkGroup) {
			// 判断,是否是分片,且有group分组,聚合
			// pre自定义和返回string[]的查询,也不需要. 可以传入标记, select,selectJson的才可以.

//		 改写sql: 	avg改写; select没有分组字段的,要补上;    是否放这?    

//			String groupFields[], GroupFunStruct gfsArray[]
//			private String fieldName;
//			private String functionType;
			// TODO 可能要命名转换。
			Map<String, String> orderByMap = condition.getOrderBy();
			boolean isEmptyOrderByMap = orderByMap.size() == 0;
			boolean needGroupWhenNoFun = false; // must NoFun

			// hadSharding, 分组的字段没有查询出来,则加分组的字段
			for (String g : groupNameslist) {
				if (columnNames.contains("," + g) || columnNames.contains(g + ",") || columnNames.equals(g)) {
					// already contain group field
				} else {
					if (!g.contains(".")) {
						columnNames += "," + mainTableName + "." + g; // just adjust select field.
					} else {
						columnNames += "," + g;
					}
				}
				if (isEmptyOrderByMap)
					condition.orderBy(g); // will adjust name in ConditionHelper
				else {
					if (!needGroupWhenNoFun && !orderByMap.containsKey(g)) needGroupWhenNoFun = true;
				}
				// 一个实体,原来有一个Map属性,取出后,能清空里面的元素吗???
			}
			GroupFunStruct gfStruct = null;
			if (pointSelectFieldOrFunWrapper.hasFun) { // && size>=1
				gfStruct = (GroupFunStruct) OneTimeParameter.getAttribute(StringConst.Return_GroupFunStruct);
			}

			// 排序没有分组字段,添加进去?
			if (gfStruct != null) {
				gfStruct.setGroupFields(groupNameslist);
				gfStruct.setNeedGroupWhenNoFun(needGroupWhenNoFun); // 有一个排序字段, 但不是分组字段的, 多个分片的记录汇聚合到一起后,可能不合顺序.
				gfStruct.setColumnNames(columnNames);

				HoneyContext.setCurrentGroupFunStruct(gfStruct);
			}
		} // checkGroup

		sqlBuffer.append(K.select).append(ONE_SPACE).append(columnNames).append(ONE_SPACE).append(K.from)
				.append(ONE_SPACE).append(tablePart);

		StringBuffer pagingRewriteSql = new StringBuffer();
		StringBuffer sqlBufferComm = new StringBuffer();

		// JOIN ON
		if (joinPart.length() > 0) {
			sqlBufferComm.append(joinPart);
		}

		boolean firstWhere = true;
		// where filter
		if (filter.length() > 0) {
			sqlBufferComm.append(ONE_SPACE).append(K.where).append(ONE_SPACE);
			sqlBufferComm.append(filter);
			firstWhere = false;
		}

		QueryConditionWrap queryConditionWrap = ConditionHelper3.processQueryCondition(condition, firstWhere,
				mainTableAlias, entity.getClass(), moreTableStructMap);
		if (queryConditionWrap != null) {
			sqlBufferComm.append(queryConditionWrap.getSqlBuffer());
			preList0.addAll((List) queryConditionWrap.getPvList());
			firstWhere = queryConditionWrap.isFirst();
		}

		List<PreparedValue> preList = new ArrayList<>();
		sqlBuffer.append(sqlBufferComm);

		if (needRewritePagingSql) {
			pagingRewriteSql.append("select distinct ");
			pagingRewriteSql.append(mainTableName).append(".id").append(" from ")
					.append(ShardingUtil.appendTableIndexIfNeed(mainTableName)).append(ONE_SPACE)
//					.append(mainTableName).append(ONE_SPACE);
					.append(mainTableAlias).append(ONE_SPACE);

			pagingRewriteSql.append(sqlBufferComm);

			preList.addAll(preList0); // temp_paging need those params
			// TODO 这里应该不需要 for_update
			ConditionHelper3.processPagingAndForUpdate(pagingRewriteSql, preList, condition, firstWhere, null);

			pagingRewriteSql.insert(0, "join (");
			// TODO 需要改为具体的主键；还要考虑多主键的情况
			pagingRewriteSql.append(") bee_paging on ").append(mainTableName).append(".id = bee_paging.id");

			int tempIndex = sqlBuffer.indexOf(tempTablePlaceholder);
			sqlBuffer.replace(tempIndex, tempIndex + tempTablePlaceholder.length(), pagingRewriteSql.toString());
		}
		preList.addAll(preList0);

		if (!needRewritePagingSql) {
			// paging, ForUpdate, check
			ConditionHelper3.processPagingAndForUpdate(sqlBuffer, preList, condition, firstWhere, null); // useSubTableNames
		}

		String sql = sqlBuffer.toString();
		HoneyContext.setPreparedValue(sql, preList);
		addInContextForCache(sql, tableNamesForCache.toString());

		return sql;
	}

	private static void concat(StringBuffer strBuffer, String separator, String prefix, List<String> list) {
		for (String str : list) {
			if (strBuffer.length() > 0) strBuffer.append(separator);

			strBuffer.append(prefix);
			strBuffer.append(str);
		}
	}

	private static void concatSubColumnName(StringBuffer strBuffer, String separator, String prefix, List<String> list,
			Set<String> columnSet, Map<String, String> subDulColumnMap) {

		boolean isConfuseDuplicateFieldDB = HoneyUtil.isConfuseDuplicateFieldDB();
		boolean isSQLite = HoneyUtil.isSQLite();

		for (String subColumnName : list) {
			if (strBuffer.length() > 0) strBuffer.append(separator);

			strBuffer.append(prefix);
			strBuffer.append(".");
			strBuffer.append(subColumnName);

			// fixed
			if (!columnSet.add(subColumnName) && isConfuseDuplicateFieldDB) {
				if (isSQLite) {
					subDulColumnMap.put(prefix + "." + subColumnName, prefix + "." + subColumnName);
				} else {
					subDulColumnMap.put(prefix + "." + subColumnName, prefix + "_" + subColumnName + "_$");
				}
				if (isSQLite) {
					strBuffer.append(" " + K.as + " '" + prefix + "." + subColumnName + "'");
				} else {
					strBuffer.append(" " + prefix + "_" + subColumnName + "_$");
				}
			}

		}
	}

	private static void concat(StringBuffer strBuffer, String separator, String mainAlias, String fieldname, String str) {
		if (strBuffer.length() > 0) strBuffer.append(separator);
		strBuffer.append(mainAlias);
		strBuffer.append(DOT);
		strBuffer.append(fieldname);
		strBuffer.append(str);
	}

	private static <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	@SuppressWarnings("rawtypes")
	private static String _toTableNameByClass(Class c) {
		return NameTranslateHandle.toTableName(c.getName());
	}

	public static String toColumnName(String fieldName) {
		return HoneyUtil.toColumnName(fieldName);
	}

	@SuppressWarnings("rawtypes")
	private static String toColumnName(String fieldName, Class entityClass) {
		return HoneyUtil.toColumnName(fieldName, entityClass);
	}

	static void addInContextForCache(String sql, String tableName) {
		CacheSuidStruct struct = new CacheSuidStruct();
		struct.setSql(sql);
		struct.setTableNames(tableName);

		HoneyContext.setCacheInfo(sql, struct);
	}

	private static void regInterceptorSubEntity() {
		OneTimeParameter.setTrueForKey(StringConst.InterceptorSubEntity);
		ShardingReg.regMoreTableQuery();
	}

	private static void doBeforePasreSubEntity(Object subEntity, InterceptorChain chain) {
		if (subEntity != null && chain != null) {
			regInterceptorSubEntity(); // 2.0从表对应的从实体,不用来计算分片. 从表分片的下标与主表的一致
			chain.beforePasreEntity(subEntity, SuidType.SELECT);
		} else { // subEntity为空也要记录; 主表分表时,要用到
			ShardingReg.regMoreTableQuery();
		}
	}

//	private static void adjustTableNameForShardingIfNeed(String tableName, StringBuffer sqlBuffer) {
//		if (ShardingUtil.useTableIndex(tableName)) {
//			sqlBuffer.append(StringConst.ShardingTableIndexStr);
//			sqlBuffer.append(" ");
//			sqlBuffer.append(tableName); // 2.4.0 baseTableName作为别名
//		}
//	}

	private static PointSelectFieldOrFunWrapper pointSelectFieldOrFun(String columnNames, Condition condition,
			Map<String, String> subDulFieldMap, String mainTableAlias, Class<?> mainClass,
			Map<String, MoreTableStruct3> moreTableStructMap) {
		String selectField = ConditionHelper3.processSelectField(columnNames, condition, subDulFieldMap, mainTableAlias,
				mainClass, moreTableStructMap);

		// v1.9.8 给声明要查的字段自动加上 表名.
		selectField = _addMaintableForSelectField(selectField, mainTableAlias);

		// v1.9 // 字段相同,要取不一样的别名,才要传subDulFieldMap
		String fun = ConditionHelper3.processFunction(columnNames, condition, mainTableAlias, mainClass,
				moreTableStructMap);

		String pointSelectFieldOrFun = null;
		if (selectField != null && StringUtils.isEmpty(fun)) {
			pointSelectFieldOrFun = selectField;
		} else if (selectField != null && StringUtils.isNotEmpty(fun)) {
			pointSelectFieldOrFun = selectField + "," + fun;
		} else if (selectField == null && StringUtils.isNotEmpty(fun)) {
			pointSelectFieldOrFun = fun;
		} else {
			// 若指定了字段,则测试也不用*代替
			if (HoneyConfig.getHoneyConfig().moreTable_columnListWithStar) {
				pointSelectFieldOrFun = "*";
			}
		}

		return new PointSelectFieldOrFunWrapper(pointSelectFieldOrFun, StringUtils.isNotEmpty(fun));
	}

	// 为指定字段,没有带表名的,自动填上主表表名.
	private static String _addMaintableForSelectField(String selectField, String mainTableName) {

		if (StringUtils.isBlank(selectField)) return selectField;

		// String居然没有检测包含某字符总数的api

		String newStr = "";
		String str[] = selectField.split(",");
		int len = str.length;
		for (int i = 0; i < len; i++) {
			if (!str[i].contains(".")) {
				str[i] = mainTableName + "." + str[i].trim();
			}
			newStr += str[i];
			if (i != len - 1) newStr += ",";
		}
		return newStr;
	}
}

class PointSelectFieldOrFunWrapper {
	String pointSelectFieldOrFun;
	boolean hasFun;

	public PointSelectFieldOrFunWrapper(String pointSelectFieldOrFun, boolean hasFun) {
		this.pointSelectFieldOrFun = pointSelectFieldOrFun;
		this.hasFun = hasFun;
	}
}
