/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.annotation.JoinType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerPagingStruct;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.ShardingReg;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * MoreObjectToSQL帮助类.MoreObjectToSQL Helper
 * @author Kingstar
 * @since  1.7
 */
public class _MoreObjectToSQLHelper {

	private static final String COMMA = ",";
	private static final String ONE_SPACE = " ";
	private static final String DOT = ".";

	private _MoreObjectToSQLHelper() {}

	private static DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}

	static <T> String _toSelectSQL(T entity) {
		return _toSelectSQL(entity, -1, null, -1, -1);
	}

	static <T> String _toSelectSQL(T entity, int start, int size) {
		return _toSelectSQL(entity, -1, null, start, size);
	}

	static <T> String _toSelectSQL(T entity, Condition condition) {
		int includeType;
		if (condition == null || condition.getIncludeType() == null)
			includeType = -1;
		else
			includeType = condition.getIncludeType().getValue();

//		condition=condition.clone();
		return _toSelectSQL(entity, includeType, condition, -1, -1);
	}

	private static void regInterceptorSubEntity() {
		OneTimeParameter.setTrueForKey(StringConst.InterceptorSubEntity);
		ShardingReg.regMoreTableQuery();
	}

	private static <T> String _toSelectSQL(T entity, int includeType, Condition condition, int start, int size) {

		checkPackage(entity);
		String sql = "";
		Set<String> whereFields = null;
		if (condition != null) {
//			condition = condition.clone();
			whereFields = condition.getWhereFields();
		}
		StringBuffer sqlBuffer = new StringBuffer();
		StringBuffer sqlBufferMainWhere = new StringBuffer();// 放主表的where条件
		StringBuffer sqlBuffer2 = new StringBuffer();
		boolean firstWhere = true;

		try {

			String tableName = _toTableName(entity);
//			String tableNameWithPlaceholder=tableName;
//			tableName=tableName.replace(StringConst.ShardingTableIndexStr, ""); //baseTableName 2.4.0
			OneTimeParameter.setAttribute(StringConst.TABLE_NAME, tableName);
			OneTimeParameter.setTrueForKey(StringConst.MoreStruct_to_SqlLib);

			// 不能到分页时才设置,因多表时,有重名字段,也需要用到dbName判断使用不同的语法,而动态获取dbName要用到路由.
			if (HoneyContext.isNeedRealTimeDb()) { // V1.9
				HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName); // main table confirm the
																									// Datasource.
				OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
			}

			MoreTableStruct moreTableStruct[] = HoneyUtil.getMoreTableStructAndCheckBefore(entity);

			// 拦截器处理 2.0
			InterceptorChain chain = null;
			for (int index = 1; index <= 2; index++) { // 从表在数组下标是1和2. 0是主表 sub table index is :1 ,2
				if (index == 1)
					chain = (InterceptorChain) OneTimeParameter.getAttribute(StringConst.InterceptorChainForMoreTable);
				if (moreTableStruct[index] != null) {
//				   //2.0从表对应的从实体,不用来计算分片. 从表分片的下标与主表的一致.  (分片拦截器不会处理,但其它拦截器要执行)
					doBeforePasreSubEntity(moreTableStruct[index].subObject, chain);// V1.11 应该要放到这,要先拦截处理,再解析. 2022-09-05
				}
			}

//			如何多表查询没有声明表别名，可以使用逻辑表作为别名？？
//			if(moreTableStruct[1].hasSubAlias) moreTableStruct[1].subAlias="ordersdetail";
			// 没用,解析MoreTableStruct时,已生成了字段等.

//			if (moreTableStruct[1] == null) { //v1.9
//				throw new BeeErrorGrammarException(
//						"MoreTable select on " + entity.getClass().getName() + " must own at least one JoinTable annotation!");
//			}//V1.11 closed.  move to inner of getMoreTableStructAndCheckBefore

			boolean twoTablesWithJoinOnStyle = HoneyConfig.getHoneyConfig().moreTable_twoTablesWithJoinOnStyle;

			boolean moreTable_columnListWithStar = HoneyConfig.getHoneyConfig().moreTable_columnListWithStar;
			String columnNames;
			columnNames = moreTableStruct[0].columnsFull;

			List<PreparedValue> list = new ArrayList<>();
			List<PreparedValue> mainList = new ArrayList<>();
			boolean needAdjustPageForList = false;
			String sqlStrForList = "";

			// 解析主表实体的where条件 分页要调整的,顺序早于opOn, 但where部分却迟于opOn
//			firstWhere=parseMainObject(entity, tableName, sqlBuffer0, mainList, firstWhere, includeType);  //顺序有问题

//			Integer pageStart=ConditionHelper.getPageStart(condition); //不需要判断
			Integer pageSize = ConditionHelper.getPageSize(condition);

			// 多表查询不同时传 start!=-1 && size!=-1 , condition
			if (moreTableStruct[0].subOneIsList && !ShardingUtil.hadSharding()) { // 从表1是List,且需要分页; 2.4.0.8有分片时,不改写.
																					// 因分片的分页是要改写sql的.

//				从表有一条记录 已包含在condition!=null里,也是不会转换的
				if (start == -1 && size == -1 && condition == null) {
					// do nothing
				} else if (start == -1 && size == -1 && pageSize == null) {
					// do nothing
//				}else if(start!=-1 && size!=-1 && condition==null){ 从表有值, 有可能不正确,所以不改写.	
				} else if (start != -1 && size != -1 && condition == null && moreTableStruct[1].subObject == null) {

//					若condition!=null, 要判断不包括从表的字段.  todo
//					主表id不为空的,也不用.  因主表最多能查一条记录

					parseMainObject(entity, tableName, sqlBufferMainWhere, mainList, firstWhere, includeType); // 因顺序原因,调整时,需要多解析一次
					Boolean idHasValue = OneTimeParameter.isTrue("idHasValue");

					if (!idHasValue) { // right join也不管用. List类型,不允许用right join

						needAdjustPageForList = true; // List类型子表,调整sql的情型
						StringBuffer sqlForList = new StringBuffer();

//						String mainColumnsForListType=moreTableStruct[0].mainColumnsForListType;
						sqlForList.append(K.select).append(" ")
//						.append(mainColumnsForListType)
								.append("*") // 用于调整(改写)sql的
								.append(" ").append(K.from).append(" ");
						sqlForList.append(tableName);
//						adjustTableNameForShardingIfNeed(tableName, sqlForList); //2.4.0.8有分片时,不改写.  因分片的分页是要改写sql的.
						sqlForList.append(sqlBufferMainWhere); // 添加解析主表实体的where条件

						adjustSqlServerPagingPkIfNeed(sqlStrForList, entity.getClass(), tableName);

//						HoneyUtil.regPagePlaceholder();
						sqlStrForList = getDbFeature().toPageSql(sqlForList.toString(), start, size);
//						HoneyUtil.setPageNum(list);

						// 后面不用再分页. Condition里的分页参数呢????
						start = -1;
						size = -1;

					}
				} else if ((start != -1 && size != -1) || pageSize != null) {
					// 因分页,从表是List类型的,得到的数据条数未必准确
					Logger.warn("MoreTable subTable's type is List, paging maybe not accurate!");
				}
			}

			if (condition != null) {
				condition.setSuidType(SuidType.SELECT);

//				ConditionHelper.processOnExpression(condition,moreTableStruct,list);  // on expression    因顺序原因,不放在这

				// V2.4.0
				boolean checkGroup = OneTimeParameter.isTrue(StringConst.Check_Group_ForSharding)
						&& ShardingUtil.hadSharding();
				List<String> groupNameslist = condition.getGroupByFields();
				int t_size = groupNameslist == null ? -1 : groupNameslist.size();
				if (checkGroup) checkGroup = checkGroup && t_size >= 1;
				OneTimeParameter.setTrueForKey(StringConst.Get_GroupFunStruct);

				String selectField = ConditionHelper.processSelectField(columnNames, condition,
						moreTableStruct[0].subDulFieldMap);

				// v1.9.8 给声明要查的字段自动加上 表名.
				selectField = _addMaintableForSelectField(selectField, tableName);

				// v1.9
				String fun = ConditionHelper.processFunction(columnNames, condition); // 字段相同,要取不一样的别名,才要传subDulFieldMap

				if (selectField != null && StringUtils.isEmpty(fun)) {
					columnNames = selectField;
				} else if (selectField != null && StringUtils.isNotEmpty(fun)) {
					columnNames = selectField + "," + fun;
				} else if (selectField == null && StringUtils.isNotEmpty(fun)) {
					columnNames = fun;
				} else {
					// 若指定了字段,则测试也不用*代替
					if (moreTable_columnListWithStar) {
						columnNames = "*";
					}
				}

				// 2.4.0
				if (checkGroup) {
					// 判断,是否是分片,且有group分组,聚合
					// pre自定义和返回string[]的查询,也不需要. 可以传入标记, select,selectJson的才可以.

//				 改写sql: 	avg改写; select没有分组字段的,要补上;    是否放这?    

//					String groupFields[], GroupFunStruct gfsArray[]
//					private String fieldName;
//					private String functionType;

					Map<String, String> orderByMap = condition.getOrderBy();
					boolean isEmptyOrderByMap = orderByMap.size() == 0;
					boolean needGroupWhenNoFun = false; // must NoFun

					// hadSharding, 分组的字段没有查询出来,则加分组的字段
					for (String g : groupNameslist) {
						if (columnNames.contains("," + g) || columnNames.contains(g + ",") || columnNames.equals(g)) {
							// already contain group field
						} else {
							if (!g.contains(".")) {
								columnNames += "," + moreTableStruct[0].tableName + "." + g; // just adjust select field.
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
					if (StringUtils.isNotEmpty(fun)) { // && size>=1
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

			} else { // V1.9
				if (moreTable_columnListWithStar) {
					columnNames = "*";
				}
			}

//			String tableName = _toTableName(entity);
//			String tableName = moreTableStruct[0].tableName;

//			String tableNamesForCache=tableName;//V1.9
			StringBuffer tableNamesForCache = new StringBuffer(tableName);// V1.9
			sqlBuffer.append(K.select).append(" ").append(columnNames).append(" ").append(K.from).append(" ");

			if (needAdjustPageForList) {
				sqlBuffer.append("(");
				sqlBuffer.append(sqlStrForList);
				sqlBuffer.append(")");
				sqlBuffer.append(" ");

				list.addAll(mainList);
			}

			sqlBuffer.append(tableName);
			if (!needAdjustPageForList) adjustTableNameForShardingIfNeed(tableName, sqlBuffer); // sqlBuffer

//			String useSubTableNames[]=new String[2];
			String useSubTableNames[] = new String[3]; // v1.9.8 useSubTableNames[2] add main tableName 放主表实际表名

			// closed on v1.9.8
			// v1.7.1 当有两个子表时,即使配置了用join..on,也会解析成where m1=sub1f1 and m2=sub2f1
//			if(moreTableStruct[0].joinTableNum>1 && twoTablesWithJoinOnStyle && moreTableStruct[1].joinType==JoinType.JOIN){
//				Logger.warn("SQL grammar type will use 'where ... =' replace 'join .. on' !");
//			}

			if (condition != null) {
				OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
				ConditionHelper.processOnExpression(condition, moreTableStruct, list); // on expression
			}

			// 2 left join, rith join ...
			if (moreTableStruct[0].joinTableNum == 2 && StringUtils.isNotBlank(moreTableStruct[1].joinExpression)
					&& StringUtils.isNotBlank(moreTableStruct[2].joinExpression)) {

				addJoinPart(sqlBuffer, moreTableStruct[1], tableNamesForCache);
				sqlBuffer.append(ONE_SPACE);
				addJoinPart(sqlBuffer, moreTableStruct[2], tableNamesForCache);

				// 只有一个子表关联,且选用join type
				// 排除以下情况: where m1=sub1f1 and m2=sub2f1 (放到else处理)
			} else if ((moreTableStruct[1].joinType != JoinType.JOIN
					|| (twoTablesWithJoinOnStyle && moreTableStruct[0].joinTableNum == 1))
					&& (StringUtils.isNotBlank(moreTableStruct[1].joinExpression))) { // 需要有表达式

				addJoinPart(sqlBuffer, moreTableStruct[1], tableNamesForCache);

			} else {// where写法
				// 从表 最多两个
				for (int s = 1; s <= 2; s++) { // 从表在数组下标是1和2. 0是主表
					if (moreTableStruct[s] != null) {

						useSubTableNames[s - 1] = moreTableStruct[s].useSubTableName; // for conditon parse todo ??????

						sqlBuffer.append(COMMA);
						sqlBuffer.append(ShardingUtil.appendTableIndexIfNeed(moreTableStruct[s].tableName));
//					tableNamesForCache+="##"+moreTableStruct[s].tableName; //V1.9
						tableNamesForCache.append(StringConst.TABLE_SEPARATOR).append(moreTableStruct[s].tableName);// v1.9.8
						if (moreTableStruct[s].hasSubAlias) {// 从表定义有别名
							sqlBuffer.append(ONE_SPACE);
							sqlBuffer.append(moreTableStruct[s].subAlias);
						}

						if (StringUtils.isNotBlank(moreTableStruct[s].joinExpression)) {
							if (firstWhere) {
								sqlBuffer2.append(" ").append(K.where).append(" ");
								firstWhere = false;
							} else {
								sqlBuffer2.append(" ").append(K.and).append(" ");
							}
							sqlBuffer2.append(moreTableStruct[s].joinExpression);
						}
					}
				} // for end
			}

			// 添加解析主表实体的where条件
//			sqlBuffer2.append(sqlBuffer0);
//			list.addAll(mainList);

			firstWhere = parseMainObject(entity, tableName, sqlBuffer2, list, firstWhere, includeType); // sqlBuffer2

			sqlBuffer.append(sqlBuffer2);

//			InterceptorChain chain=null;
//			//处理子表相应字段到where条件
//			for (int index = 1; index <= 2; index++) { // 从表在数组下标是1和2. 0是主表   sub table index is :1 ,2 
//				if(index==1) chain=(InterceptorChain)OneTimeParameter.getAttribute(StringConst.InterceptorChainForMoreTable);
//				if (moreTableStruct[index] != null) {
////					parseSubObject(sqlBuffer, valueBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
////					bug: firstWhere需要返回,传给condition才是最新的
////					firstWhere=parseSubObject(sqlBuffer, valueBuffer, list, conditionFieldSet, firstWhere, includeType, moreTableStruct, index);
//					doBeforePasreSubEntity(moreTableStruct[index].subObject, chain);//V1.11    bug??应该要移到前面,要先拦截处理,再解析. 2022-09-05
//					firstWhere=parseSubObject(sqlBuffer, list, whereFields, firstWhere, includeType, moreTableStruct, index);
//				}
//			}

			// 处理子表相应字段到where条件
			for (int index = 1; index <= 2; index++) {
				if (moreTableStruct[index] != null) {
					firstWhere = parseSubObject(sqlBuffer, list, whereFields, firstWhere, includeType, moreTableStruct,
							index);
				}
			}

			if (HoneyContext.isNeedRealTimeDb()) {
				HoneyContext.initRouteWhenParseSql(SuidType.SELECT, entity.getClass(), tableName);
				OneTimeParameter.setTrueForKey(StringConst.ALREADY_SET_ROUTE);
			}

			// V2.0 将分页统一放到condition再处理.
			if (start != -1 && size != -1 && condition == null) { // 多表查询,不会只传一个size MoreTable不会同时传condition和分页的原生参数.
				// 只是传分页参数,不用将condition记录到sharding上下文.
				condition = BeeFactoryHelper.getCondition();
				condition.start(start);
				condition.size(size);
			}

			if (condition != null) {
				condition.setSuidType(SuidType.SELECT);
				useSubTableNames[2] = tableName; // v1.9.8 useSubTableNames[2] add main tableName 放主表实际表名

				OneTimeParameter.setAttribute(StringConst.Column_EC, entity.getClass());
				ConditionHelper.processCondition(sqlBuffer, list, condition, firstWhere, useSubTableNames); // 这句会有分页.
			}

//			if(start!=-1 && size!=-1){ //若传参及Condition都有分页,转出来的sql可能语法不对.   //应该合并.    MoreTable不会同时传两样过来.
//				HoneyUtil.regPagePlaceholder();
//				adjustSqlServerPagingPkIfNeed(sqlBuffer.toString(), entity.getClass(),tableName);
//				sql=getDbFeature().toPageSql(sqlBuffer.toString(), start, size);
//				HoneyUtil.setPageNum(list);
//			}else{
//				sql=sqlBuffer.toString();
//			}

			sql = sqlBuffer.toString();

			HoneyContext.setPreparedValue(sql, list);
			addInContextForCache(sql, tableNamesForCache.toString());// tableName还要加上多表的.
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return sql;
	}

	private static void adjustTableNameForShardingIfNeed(String tableName, StringBuffer sqlBuffer) {
		if (ShardingUtil.useTableIndex(tableName)) {
			sqlBuffer.append(StringConst.ShardingTableIndexStr);
			sqlBuffer.append(" ");
			sqlBuffer.append(tableName); // 2.4.0 baseTableName作为别名
		}
	}

	private static void doBeforePasreSubEntity(Object subEntity, InterceptorChain chain) {
		if (subEntity != null && chain != null) {
			regInterceptorSubEntity(); // 2.0从表对应的从实体,不用来计算分片. 从表分片的下标与主表的一致
			chain.beforePasreEntity(subEntity, SuidType.SELECT);
		} else { // subEntity为空也要记录; 主表分表时,要用到
			ShardingReg.regMoreTableQuery();
		}
	}

	private static void adjustSqlServerPagingPkIfNeed(String sql, Class entityClass, String tableName) {

		if (!HoneyUtil.isSqlServer()) return;
		String pkName = "id";
		String pkName0 = HoneyUtil.getPkFieldNameByClass(entityClass);

//		if ("".equals(pkName)) return; //自定义主键为空,则不需要替换

		// 多表查询,要改为带表名
		if (!"".equals(pkName0)) pkName = pkName0.split(",")[0]; // 有多个,只取第一个

		pkName = _toColumnName(pkName, entityClass);

		SqlServerPagingStruct struct = new SqlServerPagingStruct();
		struct.setJustChangeOrderColumn(true);
		struct.setOrderColumn(tableName + "." + pkName);
		HoneyContext.setSqlServerPagingStruct(sql, struct);
	}

	private static boolean parseSubObject(StringBuffer sqlBuffer2, List<PreparedValue> list,
			Set<String> conditionFieldSet, boolean firstWhere, int includeType, MoreTableStruct moreTableStruct[],
			int index) throws IllegalAccessException {

		Object entity = moreTableStruct[index].subObject;

		if (entity == null) return firstWhere;

		PreparedValue preparedValue = null;
		String useSubTableName = moreTableStruct[index].useSubTableName;

		Field fields[] = null;
		if (index == 1 && moreTableStruct[0].subOneIsList) {
			fields = HoneyUtil.getFields(moreTableStruct[index].subClass);
		} else if (index == 2 && moreTableStruct[0].subTwoIsList) {
			fields = HoneyUtil.getFields(moreTableStruct[index].subClass);
		} else {
			fields = HoneyUtil.getFields(moreTableStruct[index].subEntityField.getType());
		}

		int len = fields.length;
		for (int i = 0; i < len; i++) {
			HoneyUtil.setAccessibleTrue(fields[i]);
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) {
				continue;
			} else {
				if (isNullPkOrId(fields[i], entity)) continue; // 主键=null不作为过滤条件
				if (firstWhere) {
					sqlBuffer2.append(" ").append(K.where).append(" ");
					firstWhere = false;
				} else {
					sqlBuffer2.append(" ").append(K.and).append(" ");
				}
				sqlBuffer2.append(useSubTableName);
				sqlBuffer2.append(DOT);
				sqlBuffer2.append(_toColumnName(fields[i].getName(), entity.getClass()));

				if (fields[i].get(entity) == null) {
					sqlBuffer2.append(" ").append(K.isNull);
				} else {
					sqlBuffer2.append("=");
					sqlBuffer2.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
					list.add(preparedValue);
				}
			}
		} // end for

		return firstWhere;
	}

	static void addInContextForCache(String sql, String tableName) {
		CacheSuidStruct struct = new CacheSuidStruct();
		struct.setSql(sql);
		struct.setTableNames(tableName);

		HoneyContext.setCacheInfo(sql, struct);
	}

	private static <T> void checkPackage(T entity) {
		HoneyUtil.checkPackage(entity);
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	@SuppressWarnings("rawtypes")
	private static String _toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
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

	private static void addJoinPart(StringBuffer sqlBuffer, MoreTableStruct moreTableStruct,
			StringBuffer tableNamesForCache) {

		if (moreTableStruct.joinType == JoinType.FULL_JOIN) {
			Logger.warn("Pleae confirm the Database supports 'full join' type!");
		}

		if (HoneyUtil.isSqlKeyWordUpper())
			sqlBuffer.append(moreTableStruct.joinType.getType().toUpperCase());
		else
			sqlBuffer.append(moreTableStruct.joinType.getType());
		sqlBuffer.append(moreTableStruct.tableName);
		// 只加表下标占位字符
		if (ShardingUtil.useTableIndex(moreTableStruct.tableName)) sqlBuffer.append(StringConst.ShardingTableIndexStr);

		tableNamesForCache.append(StringConst.TABLE_SEPARATOR).append(moreTableStruct.tableName);// v1.9.8
		if (moreTableStruct.hasSubAlias) {// 从表定义有别名
			sqlBuffer.append(ONE_SPACE);
			sqlBuffer.append(moreTableStruct.subAlias);
		}
		sqlBuffer.append(ONE_SPACE);
		sqlBuffer.append(K.on);
		sqlBuffer.append(ONE_SPACE);

		sqlBuffer.append(moreTableStruct.joinExpression); // sqlBuffer not sqlBuffer2

		if (StringUtils.isNotBlank(moreTableStruct.onExpression)) { // v1.9.8 on expression
			sqlBuffer.append(ONE_SPACE);
			sqlBuffer.append(K.and); // and
			sqlBuffer.append(ONE_SPACE);
			sqlBuffer.append(moreTableStruct.onExpression);
		}
	}

	private static <T> boolean parseMainObject(T entity, String tableName, StringBuffer sqlBuffer0,
			List<PreparedValue> list, boolean firstWhere, int includeType) throws IllegalAccessException {

		Field fields[] = HoneyUtil.getFields(entity.getClass());
		PreparedValue preparedValue = null;

		int len = fields.length;
		for (int i = 0; i < len; i++) {
			HoneyUtil.setAccessibleTrue(fields[i]);

//			if (fields[i].isAnnotationPresent(JoinTable.class)) {
//				continue;  //JoinTable已在上面另外处理
//			}
//			if (HoneyUtil.isContinueForMoreTable(includeType, fields[i].get(entity),fields[i].getName())) {
			if (HoneyUtil.isContinue(includeType, fields[i].get(entity), fields[i])) { // 包含了fields[i].isAnnotationPresent(JoinTable.class)的判断
				continue;
			} else {

//				if (fields[i].get(entity) == null && "id".equalsIgnoreCase(fields[i].getName())) 
//					continue; //id=null不作为过滤条件
				if (isNullPkOrId(fields[i], entity)) continue; // 主键=null不作为过滤条件

				if (fields[i].get(entity) != null && isPrimaryKey(fields[i])) {
					OneTimeParameter.setTrueForKey("idHasValue");
				}

//				if(whereFields!=null && whereFields.contains(fields[i].getName()))   //closed in V1.9
//					continue; //Condition已包含的,不再遍历

				if (firstWhere) {
					sqlBuffer0.append(" ").append(K.where).append(" ");
					firstWhere = false;
				} else {
					sqlBuffer0.append(" ").append(K.and).append(" ");
				}
				sqlBuffer0.append(tableName);
				sqlBuffer0.append(DOT);
				sqlBuffer0.append(_toColumnName(fields[i].getName(), entity.getClass()));

				if (fields[i].get(entity) == null) {
					sqlBuffer0.append(" ").append(K.isNull);
				} else {
					sqlBuffer0.append("=");
					sqlBuffer0.append("?");

					preparedValue = new PreparedValue();
					preparedValue.setType(fields[i].getType().getName());
					preparedValue.setValue(fields[i].get(entity));
					if (AnnoUtil.isJson(fields[i])) preparedValue.setField(fields[i]);
					list.add(preparedValue);
				}
			}
		} // end for

		return firstWhere;
	}

	// V1.11
	private static boolean isNullPkOrId(Field field, Object entity) {
		try {
			if (field.get(entity) == null && isPrimaryKey(field)) return true;
		} catch (Exception e) {
			// ignroe
		}
		return false;
	}

	// V1.11
	private static boolean isPrimaryKey(Field field) {
		if ("id".equalsIgnoreCase(field.getName())) return true;
		return AnnoUtil.isPrimaryKey(field);
	}

}
