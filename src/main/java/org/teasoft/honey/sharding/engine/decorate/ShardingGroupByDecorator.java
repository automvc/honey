/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.sharding.FunStruct;
import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.NameTranslateHandle;
import org.teasoft.honey.util.ObjectCreatorFactory;
import org.teasoft.honey.util.StringUtils;
import org.teasoft.honey.util.currency.CurrencyArithmetic;

/**
 * @author Kingstar
 * @since  2.0
 */
public class ShardingGroupByDecorator {
	
	public static <T> void groupAndAggregateEntity(List<T> list) {
		// 有分组的,则需要
		GroupFunStruct groupFunStruct = HoneyContext.getCurrentGroupFunStruct();
		groupAndAggregateEntity(list, groupFunStruct);
	}
	
	public static <T> void groupAndAggregateEntity(List<T> list,GroupFunStruct groupFunStruct) {
		
		if (list == null || list.size() <= 1 || groupFunStruct==null) return;
		
		List<String> groupFields = groupFunStruct.getGroupFields();
		if (groupFields == null || groupFields.size() == 0) return;  //没有分组,则不需要
		
//		FunStruct funStructs[] = groupFunStruct.getFunStructs();
		List<FunStruct> funStructs = groupFunStruct.getFunStructs();
		//有分组,但没有聚合,也不需要;  要是同一个分组的数据分布在不同节点,可以在后面排序
//		没有聚合函数,可能有多条一样的数据, 这个方法,处理不了. 
//		if ((funStruts == null || funStruts.length == 0) && ! groupFunStruct.isNeedGroupWhenNoFun()) return;
		if (funStructs == null || funStructs.size() == 0) return; 

//		String groupFields[]=new String[]{"userid","name"};
////		String groupFields[]=new String[]{"userid"};
//		//FunType,fieldName,
////		Class fieldType
//		GroupFunStruct g1=new GroupFunStruct("maxTotal","max");
//		GroupFunStruct g2=new GroupFunStruct("minTotal","min");
//		GroupFunStruct g3=new GroupFunStruct("countTotal","count");
//		GroupFunStruct g4=new GroupFunStruct("sumTotal","sum");
//		GroupFunStruct g5=new GroupFunStruct("avgTotal","avg");
//		GroupFunStruct gfsArray[]=new GroupFunStruct[5];
//		gfsArray[0]=g1;
//		gfsArray[1]=g2;
//		gfsArray[2]=g3;
//		gfsArray[3]=g4;
//		gfsArray[4]=g5;

		Field field = null;
		Field oldField = null;
		String groupKey;
		Map<String, Map<String, Object>> valueMap = new HashMap<>();
		Class<?> elementClass=list.get(0).getClass();

		boolean isMax = false;
		boolean isMin = false;

		try {
			T currentEntity;
			Map<String, T> groupEntityMap = new LinkedHashMap<>(); //存每一个分组的一条记录

			for (int i = 0; i < list.size(); i++) {
				currentEntity = list.get(i);
				groupKey = "";
				for (int j = 0; j < groupFields.size(); j++) {
					String fieldName0=_toFieldName(groupFields.get(j),elementClass);
					field = currentEntity.getClass().getDeclaredField(fieldName0);
					field.setAccessible(true);
					groupKey += field.get(currentEntity) + ",";
				}
//			System.err.println(groupKey);
				T old = groupEntityMap.get(groupKey);
				if (old == null) {
					groupEntityMap.put(groupKey, currentEntity); //第一条,放groupEntityMap
				} else { //只有分组, 没有聚合,则不需要执行
					Map<String, Object> t;
					for (int k = 0; k < funStructs.size(); k++) {
						String fieldName=_toFieldName(funStructs.get(k).getFieldName(),elementClass);
						field = currentEntity.getClass().getDeclaredField(fieldName);
						field.setAccessible(true);
						Object fun = field.get(currentEntity);

						Object oldFun;
//						oldFun = valueMap.get(groupKey + "," + gfsArray[k].getFieldName()); //第三次才有;因为第二次在后来才放进去. 
						t = valueMap.get(groupKey);  // valueMap 结构 groupKey: <funField:Object>
						if (t == null) { //第二条,还没有放valueMap
							oldFun = null;
							valueMap.put(groupKey, new LinkedHashMap<String, Object>());
						} else { //第三条起,旧值从valueMap中取; valueMap得到一个Map,再根据funField取值
							oldFun = t.get(funStructs.get(k).getFieldName());
						}

						if (oldFun == null) { //第二条,    此处,取第一条的值
							oldField = old.getClass().getDeclaredField(fieldName);
							oldField.setAccessible(true);
							oldFun = oldField.get(old);
						}

//						if(valueMap.get(groupKey)==null) valueMap.put(groupKey, new LinkedHashMap<String,Object>());

						if (FunctionType.COUNT.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType())) {
							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
								long r = Long.parseLong(fun.toString());
								if (oldFun != null
										&& StringUtils.isNotBlank(oldFun.toString())) {
									long r0 = Long.parseLong(oldFun.toString());
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(),r0 + r);
								} else {
//									valueMap.put(groupKey + "," + gfsArray[k].getFieldName(),fun);
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(), fun);// 第一次的是null,直接存第二次的
								}

							}
//							else { 两次都是null,不用存}
						} else if (FunctionType.SUM.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType())) {
							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
								if (oldFun != null
										&& StringUtils.isNotBlank(oldFun.toString())) {
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(),
											CurrencyArithmetic.add(fun.toString(),
													oldFun.toString()));
								} else {
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(), fun);
								}
							}
						} else if ((isMax = FunctionType.MAX.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType()))
								|| (isMin = FunctionType.MIN.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType()))) {
							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
								if (oldFun != null && StringUtils.isNotBlank(oldFun.toString())) {
									double d = Double.parseDouble(fun.toString());
									double d0 = Double.parseDouble(oldFun.toString());
									if ((isMax && d > d0) || (isMin && d < d0)) 
										valueMap.get(groupKey).put(funStructs.get(k).getFieldName(), fun);
								} else {
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(), fun);
								}
							}
//							else {fun为empty,则不处理,保留原来的}
						}
//						else if (FunctionType.AVG.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType())) {
////							.println("AVG不应该走这里的分支...");
//						}
					}
					t = null;
				}
			} // end for

			for (Map.Entry<String, Map<String, Object>> entryKey : valueMap.entrySet()) {
				T tempEntity = groupEntityMap.get(entryKey.getKey());
				for (Map.Entry<String, Object> entry : entryKey.getValue().entrySet()) {
					try {
//						System.err.println(entry.getKey());
//						System.err.println(entry.getValue());
//						System.err.println(entry);
						
						String fieldName2=_toFieldName(entry.getKey(),elementClass);
						Field funField = tempEntity.getClass().getDeclaredField(fieldName2);
						funField.setAccessible(true);
						Object v = entry.getValue();
						if (v != null)
							v = ObjectCreatorFactory.create(v.toString(), funField.getType());
						funField.set(tempEntity, v);

					} catch (Exception e) {
//						throw ExceptionHelper.convert(e);
//						e.printStackTrace();
						Logger.warn(e.getMessage(), e);
					}
				}
			}

			if (list.size() != groupEntityMap.size()) { //更新为分组后的记录
				list.clear();
				for (Map.Entry<String, T> entry : groupEntityMap.entrySet()) {
					list.add(entry.getValue());
				}
			}

//			groupEntityMap.clear();
//			valueMap.clear();
			groupEntityMap = null;
			valueMap = null;

		} catch (Exception e) {
			Logger.debug(e.getMessage(),e);
//			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static String _toFieldName(String columnName,Class entityClass) {
		return NameTranslateHandle.toFieldName(columnName,entityClass);
	}
	
	
	public static void groupAndAggregateStringArray(List<String[]> list) {
		GroupFunStruct groupFunStruct = HoneyContext.getCurrentGroupFunStruct();
		groupAndAggregateStringArray(list, groupFunStruct);
	}
	
	public static void groupAndAggregateStringArray(List<String[]> list, GroupFunStruct groupFunStruct) {

		if (list == null || list.size() <= 1 || groupFunStruct==null) return;
		
		List<String> groupFields = groupFunStruct.getGroupFields();
		if (groupFields == null || groupFields.size() == 0) return;  //没有分组,则不需要
		
//		FunStruct funStructs[] = groupFunStruct.getFunStructs();
		List<FunStruct> funStructs = groupFunStruct.getFunStructs();
		//有分组,但没有聚合,也不需要;  要是同一个分组的数据分布在不同节点,可以在后面排序
//		没有聚合函数,可能有多条一样的数据, 这个方法,处理不了. 
//		if ((funStruts == null || funStruts.length == 0) && ! groupFunStruct.isNeedGroupWhenNoFun()) return;
		if (funStructs == null || funStructs.size() == 0) return; 
		
//		System.err.println(list.size());

//		Field field = null;
//		Field oldField = null;
		String groupKey;
		Map<String, Map<String, String>> valueMap = new HashMap<>();

		boolean isMax = false;
		boolean isMin = false;
		
		boolean hasAvg=false;
		List<Integer> avgNum=new ArrayList<>();

		try {
			String[] currentStringArray;
			Map<String, String[]> groupEntityMap = new LinkedHashMap<>();

			for (int i = 0; i < list.size(); i++) {
				currentStringArray = list.get(i);
				groupKey = "";
				for (int j = 0; groupFields!=null && j < groupFields.size(); j++) {
					groupKey += currentStringArray[groupFunStruct.getIndexByColumn(groupFields.get(j))] + ",";
				}
				String[] old = groupEntityMap.get(groupKey);
				if (old == null) {
					groupEntityMap.put(groupKey, currentStringArray);
				} else { //只有分组, 没有聚合,则不需要执行
					Map<String, String> t;
					for (int k = 0; k < funStructs.size(); k++) {
						int funIndex=groupFunStruct.getIndexByColumn(funStructs.get(k).getFieldName());
						String fun=currentStringArray[funIndex]; //当前值
						String oldFun;
//						oldFun = valueMap.get(groupKey + "," + gfsArray[k].getFieldName()); //第三次才有;因为第二次在后来才放进去. 
						t = valueMap.get(groupKey);
						if (t == null) {
							oldFun = null;
							valueMap.put(groupKey, new LinkedHashMap<String, String>());
						} else {
							oldFun = t.get(funStructs.get(k).getFieldName());
						}

						if (oldFun == null) {
							oldFun=old[groupFunStruct.getIndexByColumn(funStructs.get(k).getFieldName())];
						}

					if (FunctionType.SUM.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType())
						|| FunctionType.COUNT.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType())	
						) {
							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
								if (oldFun != null && StringUtils.isNotBlank(oldFun)) {
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(),
											CurrencyArithmetic.add(fun,oldFun));
								} else {
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(), fun);
								}
							}
						} else if ((isMax = FunctionType.MAX.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType()))
								|| (isMin = FunctionType.MIN.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType()))) {
							if (fun != null && StringUtils.isNotBlank(fun.toString())) {
								if (oldFun != null && StringUtils.isNotBlank(oldFun.toString())) {
									double d = Double.parseDouble(fun.toString());
									double d0 = Double.parseDouble(oldFun.toString());
									if ((isMax && d > d0) || (isMin && d < d0)) 
										valueMap.get(groupKey).put(funStructs.get(k).getFieldName(), fun);
								} else {//旧的为空,直接存新的
									valueMap.get(groupKey).put(funStructs.get(k).getFieldName(), fun);
								}
							}
//							else {fun为empty,则不处理,保留原来的}
						} else if (FunctionType.AVG.getName().equalsIgnoreCase(funStructs.get(k).getFunctionType())) {
							hasAvg=true;   
							if (i == 0) avgNum.add(funIndex);
						}
					}
					t = null;
				}
			} // end for

			for (Map.Entry<String, Map<String, String>> entryKey : valueMap.entrySet()) {  //valueMap里的值,第一行的也有计算了
				//将保存在valueMap的计算临时值,保存回groupEntityMap里的唯一记录
				String[] tempEntity = groupEntityMap.get(entryKey.getKey());
				for (Map.Entry<String, String> entry : entryKey.getValue().entrySet()) {
					try {
						int index=groupFunStruct.getIndexByColumn(entry.getKey());
						tempEntity[index]=entry.getValue();
					} catch (Exception e) {
//						e.printStackTrace();
						Logger.warn(e.getMessage(), e);
					}
				}
			}
			
			//更新为分组后的记录
			if (list.size() != groupEntityMap.size()) { 
				list.clear();
				for (Map.Entry<String, String[]> entry : groupEntityMap.entrySet()) {
					list.add(adjust(entry.getValue(),hasAvg,avgNum)); //要是有AVG,要删除自动增加的列
				}
			}


//			groupEntityMap.clear();
//			valueMap.clear();
			groupEntityMap = null;
			valueMap = null;

		} catch (Exception e) {
//			e.printStackTrace();
			Logger.warn(e.getMessage(), e);
		}
	}
	
	private static String[] adjust(String[] old,boolean hasAvg,List<Integer> avgNum) {
		if(! hasAvg) return old;
		
		int num=avgNum.size();
		String tempArray[]=new String[old.length-num*2];
		for (Integer index : avgNum) {
			tempArray[index]=CurrencyArithmetic.divide(tempArray[index+1], tempArray[index+2]);
		}
		for (int i = 0,k=0; i < old.length; i++) {
			tempArray[k++]=old[i];
			if(existInList(avgNum,i)) i=i+2; //跳过两列(自动增加的)
		}
		
		return tempArray;
	}
	
	private static boolean existInList(List<Integer> avgNum,int index) {
		for (Integer i : avgNum) {
			if(index==i) return true;
		}
		return false;
	}


}
