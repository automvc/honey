/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.sharding.FunStruct;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.HoneyContext1;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.LocalType;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.util.ObjectCreatorFactory;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.Printer;
import org.teasoft.honey.util.StringUtils;
import org.teasoft.honey.util.currency.CurrencyArithmetic;

/**
 * @author Kingstar
 * @since 2.0
 */
public class ShardingGroupByDecorator {

	public static <T> void groupAndAggregateEntity(List<T> list) {
		// 有分组的,则需要
//		GroupFunStruct groupFunStruct = HoneyContext.getCurrentGroupFunStruct();
		GroupFunStruct groupFunStruct = HoneyContext1.getInheritable(LocalType.SHARDING, GroupFunStruct.class);
		groupAndAggregateEntity(list, groupFunStruct);
	}

	// 无avg,且查询返回T
	public static <T> void groupAndAggregateEntity(List<T> list, GroupFunStruct groupFunStruct) {

		if (list == null || list.size() <= 1 || groupFunStruct == null) return;

		List<String> groupFields = groupFunStruct.getGroupFields();
//		if (groupFields == null || groupFields.size() == 0) return; // 没有分组,则不需要(x).  V3.0.0.8  没有分组，多个分片的数据会合在一起，要重新计算。

		List<FunStruct> funStructs = groupFunStruct.getFunStructs();
		// 有分组,但没有聚合,也不需要; 要是同一个分组的数据分布在不同节点,可以在后面排序
//		没有聚合函数,可能有多条一样的数据, 这个方法,处理不了.   //TODO
//		if ((funStruts == null || funStruts.length == 0) && ! groupFunStruct.isNeedGroupWhenNoFun()) return;
		if (funStructs == null || funStructs.size() == 0) return;

		Field field = null;
		Field oldField = null;
		String groupKey;
		Map<String, Map<String, Object>> valueMap = new HashMap<>(); // valueMap 结构 groupKey: <funFieldName : Object>
		Class<?> elementClass = list.get(0).getClass();

		boolean isMax = false;
		boolean isMin = false;

		try {
			T currentEntity;
			Map<String, T> groupEntityMap = new LinkedHashMap<>(); // 存每一个分组的一条记录
			FunctionType functionType;

			for (int i = 0; i < list.size(); i++) {
				currentEntity = list.get(i);
				groupKey = "";
				for (int j = 0; groupFields != null && j < groupFields.size(); j++) {
					// TODO String mainTableForGroupField;
					String fieldName0 = _toFieldName(groupFields.get(j), elementClass);
					field = currentEntity.getClass().getDeclaredField(fieldName0);
					HoneyUtil.setAccessibleTrue(field);
					groupKey += field.get(currentEntity) + ",";
				}
				T old = groupEntityMap.get(groupKey);
				if (old == null) {
					groupEntityMap.put(groupKey, currentEntity); // 第一条,放groupEntityMap
				} else { // 只有分组, 没有聚合,则不需要执行
					Map<String, Object> t;
					String funFieldName;
					for (int k = 0; k < funStructs.size(); k++) {
						funFieldName = funStructs.get(k).getFieldName();
						String fieldName = _toFieldName(funFieldName, elementClass);
						field = currentEntity.getClass().getDeclaredField(fieldName);
						HoneyUtil.setAccessibleTrue(field);
						Object fun = field.get(currentEntity);
						if (fun == null || StringUtils.isBlank(fun.toString())) {
							continue;
						}

						Object oldFun;
//						oldFun = valueMap.get(groupKey + "," + gfsArray[k].getFieldName()); //第三次才有;因为第二次在后来才放进去. 
						t = valueMap.get(groupKey); // valueMap 结构 groupKey: <funFieldName : Object>
						if (t == null) { // 第二条,还没有放valueMap
							oldFun = null;
							valueMap.put(groupKey, new LinkedHashMap<String, Object>());
						} else { // 第三条起,旧值从valueMap中取; valueMap得到一个Map,再根据funField取值
							oldFun = t.get(funFieldName);
						}

						if (oldFun == null) { // 第二条, 此处,取第一条的值
							oldField = old.getClass().getDeclaredField(fieldName);
							HoneyUtil.setAccessibleTrue(oldField);
							oldFun = oldField.get(old);
						}

						functionType = FunctionType.getByName(funStructs.get(k).getFunctionType());

						if (FunctionType.SUM == functionType || FunctionType.COUNT == functionType) {
//							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
							if (oldFun != null && StringUtils.isNotBlank(oldFun.toString())) {
								valueMap.get(groupKey).put(funFieldName, CurrencyArithmetic.add(fun.toString(), oldFun.toString()));
							} else {
								valueMap.get(groupKey).put(funFieldName, fun);
							}
//							}
						} else if ((isMax = FunctionType.MAX == functionType)
								|| (isMin = FunctionType.MIN == functionType)) {
//							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
									if (oldFun != null && StringUtils.isNotBlank(oldFun.toString())) {
										double d = Double.parseDouble(fun.toString());
										double d0 = Double.parseDouble(oldFun.toString());
										if ((isMax && d > d0) || (isMin && d < d0))
											valueMap.get(groupKey).put(funFieldName, fun);
									} else {
										valueMap.get(groupKey).put(funFieldName, fun);
									}
//							}
//							else {fun为empty,则不处理,保留原来的}
								}
//						else if (FunctionType.AVG== functionType) {
////							.println("AVG不应该走这里的分支...");
//						}
					}
					t = null;
				}
			} // end for

			// groupEntityMap 存每一个分组的一条记录; 在valueMap统计到的每组聚合数据，会存到 groupEntityMap
			for (Map.Entry<String, Map<String, Object>> entryKey : valueMap.entrySet()) {
				T tempEntity = groupEntityMap.get(entryKey.getKey());
				for (Map.Entry<String, Object> entry : entryKey.getValue().entrySet()) { // 同一组的多个聚合值，<funFieldName : Object>
					try {
						String fieldName2 = _toFieldName(entry.getKey(), elementClass);
						Field funField = tempEntity.getClass().getDeclaredField(fieldName2);
						HoneyUtil.setAccessibleTrue(funField);
						Object v = entry.getValue();
						if (v != null) v = ObjectCreatorFactory.create(v.toString(), funField.getType());
						HoneyUtil.setFieldValue(funField, tempEntity, v);
					} catch (Exception e) {
						Logger.warn(e.getMessage(), e);
					}
				}
			}

			if (list.size() != groupEntityMap.size()) { // 更新为分组后的记录; 每组只有一条; 若是没有分组的聚合,则最终只会剩下一条记录
				list.clear();
				for (Map.Entry<String, T> entry : groupEntityMap.entrySet()) {
					list.add(entry.getValue());
				}
			}

			groupEntityMap = null;
			valueMap = null;

		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
	}

	@SuppressWarnings("rawtypes")
	private static String _toFieldName(String columnName, Class entityClass) {
		return NameTranslateHandle.toFieldName(columnName, entityClass);
	}

	public static void groupAndAggregateStringArray(List<String[]> list) {
//		GroupFunStruct groupFunStruct = HoneyContext.getCurrentGroupFunStruct();
		GroupFunStruct groupFunStruct = HoneyContext1.getInheritable(LocalType.SHARDING, GroupFunStruct.class);
		groupAndAggregateStringArray(list, groupFunStruct);
	}

	// 查询返回List<String[]>; 有无 avg都可以。 返回T，但有avg的，也会有这里，因为String[]方便自动添加列。
	public static void groupAndAggregateStringArray(List<String[]> list, GroupFunStruct groupFunStruct) {

		if (list == null || list.size() <= 1 || groupFunStruct == null) return;

		List<String> groupFields = groupFunStruct.getGroupFields();
//		if (groupFields == null || groupFields.size() == 0) return; // 没有分组,则不需要(x).  V3.0.0.8  没有分组，多个分片的数据会合在一起，要重新计算。

		List<FunStruct> funStructs = groupFunStruct.getFunStructs();
		// 有分组,但没有聚合,也不需要; 要是同一个分组的数据分布在不同节点,可以在后面排序
//		没有聚合函数,可能有多条一样的数据, 这个方法,处理不了. 
//		if ((funStruts == null || funStruts.length == 0) && ! groupFunStruct.isNeedGroupWhenNoFun()) return;
		if (funStructs == null || funStructs.size() == 0) return;

		String groupKey;
		Map<String, Map<String, String>> valueMap = new HashMap<>();

		boolean isMax = false;
		boolean isMin = false;
		boolean hasAvg = false;
		Set<Integer> avgNum = new TreeSet<>(); // avgIndex

		try {
			String[] currentStringArray;
			Map<String, String[]> groupEntityMap = new LinkedHashMap<>();
			String mainTableForGroupField;
			String groupField;
			FunctionType functionType;
			for (int i = 0; i < list.size(); i++) {
				currentStringArray = list.get(i);
				groupKey = "";
				for (int j = 0; groupFields != null && j < groupFields.size(); j++) {
					mainTableForGroupField = groupFunStruct.getMainTableForGroupField();
					groupField = groupFields.get(j);
					if (StringUtils.isNotBlank(mainTableForGroupField) && !groupField.contains(".")) {
						groupField = mainTableForGroupField + "." + groupField;
					}
					groupKey += currentStringArray[groupFunStruct.getIndexByColumn(groupField)] + ",";
				}
				String[] old = groupEntityMap.get(groupKey);
				if (old == null) {
					groupEntityMap.put(groupKey, currentStringArray);
				} else { // 只有分组, 没有聚合,则不需要执行
					Map<String, String> t;
					String funFieldName;
					for (int k = 0; k < funStructs.size(); k++) {
						funFieldName = funStructs.get(k).getFieldName();
						Integer funIndex = groupFunStruct.getIndexByColumn(funFieldName); // null?
						if (funIndex == null) {
							Logger.warn("Can not found " + funFieldName + " in " + groupFunStruct.getColumnIndexMap());
							continue;
						}
						String fun = currentStringArray[funIndex]; // 当前值
						if (fun == null || StringUtils.isBlank(fun.toString())) {
							continue;
						}

						String oldFun;
//						oldFun = valueMap.get(groupKey + "," + gfsArray[k].getFieldName()); //第三次才有;因为第二次在后来才放进去. 
						t = valueMap.get(groupKey);
						if (t == null) {
							oldFun = null;
							valueMap.put(groupKey, new LinkedHashMap<String, String>());
						} else {
							oldFun = t.get(funFieldName);
						}

						if (oldFun == null) {
							oldFun = old[groupFunStruct.getIndexByColumn(funFieldName)];
						}

						functionType = FunctionType.getByName(funStructs.get(k).getFunctionType());

						if (FunctionType.SUM == functionType || FunctionType.COUNT == functionType) {
//							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
							if (oldFun != null && StringUtils.isNotBlank(oldFun)) {
								valueMap.get(groupKey).put(funFieldName, CurrencyArithmetic.add(fun, oldFun));
							} else {
								valueMap.get(groupKey).put(funFieldName, fun);
							}
//							}
						} else if ((isMax = FunctionType.MAX == functionType)
								|| (isMin = FunctionType.MIN == functionType)) {
//									if (fun != null && StringUtils.isNotBlank(fun.toString())) {
									if (oldFun != null && StringUtils.isNotBlank(oldFun.toString())) {
										double d = Double.parseDouble(fun.toString());
										double d0 = Double.parseDouble(oldFun.toString());
										if ((isMax && d > d0) || (isMin && d < d0))
											valueMap.get(groupKey).put(funFieldName, fun);
									} else {// 旧的为空,直接存新的
										valueMap.get(groupKey).put(funFieldName, fun);
									}
//									}
//							    else {fun为empty,则不处理,保留原来的}
								} else if (FunctionType.AVG == functionType) {
									hasAvg = true;
									avgNum.add(funIndex);
								}
					}
					t = null;
				}
			} // end for

			if (avgNum.isEmpty()) { // 若每组只有一行时，上面的没有设置avgNum；要在这检查设置
				String funFieldName;
				for (int k = 0; k < funStructs.size(); k++) {
					funFieldName = funStructs.get(k).getFieldName();
					Integer funIndex = groupFunStruct.getIndexByColumn(funFieldName); // null?
					functionType = FunctionType.getByName(funStructs.get(k).getFunctionType());
					if (funIndex == null) continue;
					if (FunctionType.AVG == functionType) {
						hasAvg = true;
						avgNum.add(funIndex);
					}
				}
			}

			HoneyContext1.setForSharding(StringConst.AvgIndexSet, avgNum);

			for (Map.Entry<String, Map<String, String>> entryKey : valueMap.entrySet()) { // valueMap里的值,第一行的也有计算了
				// 将保存在valueMap的计算临时值,保存回groupEntityMap里的唯一记录
				String[] tempEntity = groupEntityMap.get(entryKey.getKey());
				for (Map.Entry<String, String> entry : entryKey.getValue().entrySet()) {
					try {
						int index = groupFunStruct.getIndexByColumn(entry.getKey());
						tempEntity[index] = entry.getValue();
					} catch (Exception e) {
						Logger.warn(e.getMessage(), e);
					}
				}
			}

			if (list.size() != groupEntityMap.size()) {// 更新为分组后的记录; 每组只有一条; 若是没有分组的聚合,则最终只会剩下一条记录
				list.clear();
				for (Map.Entry<String, String[]> entry : groupEntityMap.entrySet()) {
					list.add(reAvg(entry.getValue(), hasAvg, avgNum)); // 每组只有一条；若有平均值会重新计算。
				}
			}

			groupEntityMap = null;
			valueMap = null;
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
		}
	}

	// 重新计算平均值;
//	注意： 还没排序，不能删除排序添加的列。  自动加的分组列可能与自动加的排序列一样，先不能删。
//	平均值多的两列，也先不删；不然列数变了，找不到自动加的分组列。
	private static String[] reAvg(String[] old, boolean hasAvg, Set<Integer> avgNum) { // 重新计算平均值
		if (!hasAvg) return old;
		for (Integer index : avgNum) {
			old[index] = CurrencyArithmetic.divide(old[index + 1], old[index + 2], 6);
		}
		return old;
	}

	public static void removeAutoGenColumnForStringArray(List<String[]> rsList) {
		GroupFunStruct groupFunStruct = HoneyContext1.getInheritable(LocalType.SHARDING, GroupFunStruct.class);

		if (rsList == null || rsList.size() == 0) return;
		Set<String> addedGroupByColumn = groupFunStruct.getAddedGroupByColumn();
		Set<Integer> avgIndex = (Set<Integer>) HoneyContext1.get(LocalType.SHARDING, StringConst.AvgIndexSet);
		if (ObjectUtils.isEmpty(avgIndex) && addedGroupByColumn.isEmpty()) return ;
		
		String old[];
		String tempArray[];
		Logger.debug("print select temp data start:");
		Logger.debug("select temp columns: " + groupFunStruct.getColumnNames());
		for (int m = 0; m < rsList.size(); m++) {
			old = rsList.get(m);
			Printer.printArrayStringInDebug(old);
			
			tempArray = removeAutoGenColumnForStringArray0(old, avgIndex, groupFunStruct);
			rsList.set(m, tempArray); // reset new value
		}
		Logger.debug("print select temp data end.");
		Logger.info("the Orginal Select Column: " + groupFunStruct.getOrginalSelectColumn());
	}

	// 删除自动加的列(为算平均值加了两列，还有分组列缺失时自动加的) 注意： 还没排序，不能删除排序添加的列。
	// 自动加的分组列可能与自动加的排序列一样，先不能删。
	// 这里只删多余的列， 不计算
	private static String[] removeAutoGenColumnForStringArray0(String[] old, Set<Integer> avgIndex,
			GroupFunStruct groupFunStruct) {
		Set<String> addedGroupByColumn = groupFunStruct.getAddedGroupByColumn();
//		if (ObjectUtils.isEmpty(avgNum) && addedGroupByColumn.isEmpty()) return old;

		Set<Integer> addedGroupByColumnIndex = new TreeSet<>();
		for (String s : addedGroupByColumn) {
			addedGroupByColumnIndex.add(groupFunStruct.getIndexByColumn(s));
		}

		int num = avgIndex.size();
		String tempArray[] = new String[old.length - num * 2 - addedGroupByColumn.size()];
		for (int i = 0, k = 0; i < old.length; i++) {
			if (addedGroupByColumnIndex.contains(i)) continue; // 跳过自动加的分组列
			tempArray[k++] = old[i];
			if (avgIndex.contains(i)) i = i + 2; // 跳过两列(为算平均值加了两列)
		}

		return tempArray;
	}

//	// 重新计算平均值; 删除自动加的列   注意： 还没排序，不能删除排序添加的列。  自动加的分组列可能与自动加的排序列一样，先不能删。
//	private static String[] adjustAvg(String[] old, boolean hasAvg, Set<Integer> avgNum, GroupFunStruct groupFunStruct) {
//		Set<String> addedGroupByColumn=groupFunStruct.getAddedGroupByColumn();
//		if (!hasAvg && addedGroupByColumn.isEmpty()) return old;
//		
//		Set<Integer> addedGroupByColumnIndex=new TreeSet<>();
//		for(String s:addedGroupByColumn) {
//			addedGroupByColumnIndex.add(groupFunStruct.getIndexByColumn(s));
//		}
//		
//		int num = avgNum.size();
//		String tempArray[] = new String[old.length - num * 2 - addedGroupByColumn.size()];
//		for (Integer index : avgNum) {
//			old[index] = CurrencyArithmetic.divide(old[index + 1], old[index + 2], 6);
//		}
//		for (int i = 0, k = 0; i < old.length; i++) {
//			if (addedGroupByColumnIndex.contains(i)) continue; //跳过自动加的分组列
//			tempArray[k++] = old[i];
//			if (avgNum.contains(i)) i = i + 2; // 跳过两列(自动增加的)
//		}
//
//		return tempArray;
//	}

}
