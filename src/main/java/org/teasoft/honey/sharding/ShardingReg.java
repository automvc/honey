/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding;

import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.exception.ShardingErrorException;
import org.teasoft.bee.sharding.ShardingPageStruct;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class ShardingReg {
	
	
	//<<<<<<<<<<<<<<<<<<<<<<<<<解析时用.  start
	
	public static void regHadSharding() {
		ShardingUtil.setTrue(StringConst.HadSharding);
	}

	//下游还会判断,是否涉及多个库.
	public static void regShardingManyTables(List<String> dsNameList, List<String> tabNameList,
			List<String> tabSuffixList, Map<String, String> tab2DsMap) {
		HoneyContext.setListLocal(StringConst.TabNameListLocal, tabNameList);
		HoneyContext.setListLocal(StringConst.TabSuffixListLocal, tabSuffixList);
		//一般用于判断,是否是某个数据源
		HoneyContext.setListLocal(StringConst.DsNameListLocal, dsNameList); //极少用于指定ds;  一般用反查或tab2DsMap
		HoneyContext.setCustomMapLocal(StringConst.ShardingTab2DsMap, tab2DsMap);
	}
	
	public static void regFullInModifyAllNodes(SuidType suidType) {
		if(suidType!=SuidType.SELECT) {
			setTrueInSysCommStrLocal(StringConst.ShardingFullSelect);
			regHadSharding();
		}
	}
	
	public static void regFull(SuidType suidType) {
		//来到这里,说明是涉及全域操作,是没有设置分片值的.
		
		if(suidType==SuidType.SELECT || suidType==SuidType.DELETE  || suidType==SuidType.UPDATE) { //全域
			setTrueInSysCommStrLocal(StringConst.ShardingFullSelect);
			regHadSharding();
		}else if (SuidType.INSERT == suidType) {
			//记录插入时,分表的分片值,要设置
			triggerDoNotSetTabShadngValueException();
		}
	}
	
	//分片值只计算得数据源名称,应该查其下的所有表.
	public static void regSomeDsFull(SuidType suidType) {
		regFull(suidType); //若有异常,会在这句抛出.
		setTrueInSysCommStrLocal(StringConst.ShardingSomeDsFullSelect);
	}
	
	public static void regShardingJustDs(List<String> dsNameList) {  //一般用于判断,是否是某个数据源
		HoneyContext.setListLocal(StringConst.DsNameListLocal, dsNameList);  //极少用于指定ds;  一般用反查或tab2DsMap
	}
	
	private static final String DoNotSetTabShadngValue="Do not set the sharding value for table!";
	private static void triggerDoNotSetTabShadngValueException() {
		clearContext();
		//记录插入时,分表的分片值,要设置
        throw new ShardingErrorException(DoNotSetTabShadngValue);
	}
	
	public static void regBatchInsert(List<String> tabNameListForBatch, List<String> dsNameListForBatch) {
		HoneyContext.setListLocal(StringConst.TabNameListForBatchLocal, tabNameListForBatch);
		HoneyContext.setListLocal(StringConst.DsNameListForBatchLocal, dsNameListForBatch);
	}
	
	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>解析时用.  end
	
	
	
	
	//<<<<<<<<<<<<<<<<<<<<<<<<<重写,排序,分页时用.  start
	
	public static void regShadingPage(String beforeSql, String pagingSql, Integer start, Integer size) {
		
		if(size==null || (start==null && size==null)) return ;
		
		//  可能一些标记还要保留
		if (ShardingUtil.hadSharding()) {// 有分片才要记录
			ShardingPageStruct struct=new ShardingPageStruct();
			struct.setBeforeSql(beforeSql);
			struct.setPagingSql(pagingSql);
//			struct.setPagingType(pagingType);
			if(start!=null && start!=-1) struct.setStart(start);
			struct.setSize(size);
			
			//没设置有库分片值时,不会有值.
//			List<String> dsNameListLocal=HoneyContext.getListLocal(StringConst.DsNameListLocal);
//			if(dsNameListLocal!=null && dsNameListLocal.size()==1) struct.setPagingType("SameDS");
//			else struct.setPagingType("MoreDS");
			
			HoneyContext.setCurrentShardingPage(struct);
		}
	}
	
	//selectOrderBy
	public static void regShardingSort(String orderSql, String[] orderFields, OrderType[] orderTypes) {
		if (!ShardingUtil.hadSharding()) return;
		
		ShardingSortStruct struct = new ShardingSortStruct(orderSql, orderFields, orderTypes);
		HoneyContext.setCurrentShardingSort(struct);
	}
	
	//带condition
	public static void regShardingSort(ShardingSortStruct struct) {
		if (!ShardingUtil.hadSharding()) return;
		HoneyContext.setCurrentShardingSort(struct);
	}

//	private Map<String,String> orderByMap=new LinkedHashMap<>();
	public static void regShardingSort(Map<String, String> orderByMap) {

		if (!ShardingUtil.hadSharding()) return;
		if (ObjectUtils.isEmpty(orderByMap)) return;
		
//		String orderFields[] = new String[orderByMap.size()];
//		OrderType[] orderTypes = new OrderType[orderByMap.size()];
//		int lenA = orderFields.length;
//		String orderBy = "";
//		int i = 0;
//		for (Map.Entry<String, String> entry : orderByMap.entrySet()) {
//			String fName = entry.getKey();
//			String orderType = entry.getValue();
//			orderFields[i] = fName;
//			if (OrderType.DESC.getName().equals(orderType))
//				orderTypes[i] = OrderType.DESC;
//			else
//				orderTypes[i] = OrderType.ASC;
//			orderBy += fName + " " + orderType;
//			if (i < lenA - 1) orderBy += ",";
//			i++;
//		}
//
//		regShardingSort(orderBy, orderFields, orderTypes);
		
		regShardingSort(ShardingUtil.parseOrderByMap(orderByMap));
		
	}
	
	public static void regMoreTableQuery() {
		setTrueInSysCommStrLocal(StringConst.MoreTableQuery);
	}
	
	
	//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>重写,排序,分页时用.  end
	
	
	
	public static void regShardingBatchInsertDoing() {
		setTrueInSysCommStrLocal(StringConst.ShardingBatchInsertDoing);
	}
	
	
	
	//2.0
	public static void clearContext() {
			if(! ShardingUtil.isSharding()) return ;
			
			HoneyContext.removeConditionLocal();
			HoneyContext.removeSysCommStrLocal(StringConst.HadSharding);
			HoneyContext.removeSysCommStrLocal(StringConst.ShardingFullSelect);
			HoneyContext.removeSysCommStrLocal(StringConst.ShardingSomeDsFullSelect);
			HoneyContext.removeSysCommStrLocal(StringConst.HintDs);
			HoneyContext.removeSysCommStrLocal(StringConst.HintTab);
			HoneyContext.removeSysCommStrLocal(StringConst.FunType);
			HoneyContext.removeSysCommStrLocal(StringConst.MoreTableQuery);
			
			HoneyContext.removeListLocal(StringConst.TabNameListLocal);
			HoneyContext.removeListLocal(StringConst.TabSuffixListLocal);
			HoneyContext.removeListLocal(StringConst.DsNameListLocal);
			
			HoneyContext.removeListLocal(StringConst.TabNameListForBatchLocal);
			HoneyContext.removeListLocal(StringConst.DsNameListForBatchLocal);
			
			HoneyContext.removeCustomMapLocal(StringConst.ShardingTab2DsMap);
			
			HoneyContext.removeCurrentShardingPage();
			HoneyContext.removeCurrentShardingSort();
			HoneyContext.removeCurrentGroupFunStruct();
			
//			HoneyContext.currentGroupFunStruct.remove();
			HoneyContext.removeCurrentGroupFunStruct();
		}
	
	public static void setTrueInSysCommStrLocal(String key) {
		HoneyContext.setSysCommStrInheritableLocal(key, StringConst.tRue);
	}

}
