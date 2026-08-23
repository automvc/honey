/*
 * Copyright 2022-2026 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.teasoft.bee.sharding.GroupFunStruct;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyContext1;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.LocalType;
import org.teasoft.honey.osql.core.StringConst;
import org.teasoft.honey.util.ObjectUtils;

/**
 * RecoverAddedSelectColumn, support: OrderBy,GroupBy
 * 
 * @author Kingstar
 * @since 3.0.0
 */
public class RecoverAddedSelectColumnDecorator {

	@SuppressWarnings("unchecked")
	public static <T> void recover(List<T> rsList) {

		List<String> addedOrderByFields = (List<String>) HoneyContext1.get(LocalType.SHARDING,
				StringConst.AddedOrderByField);

		GroupFunStruct groupFunStruct = (GroupFunStruct)HoneyContext1.getExtForSharding(StringConst.GroupFunStruct);
		Set<String> addedGroupByColumn = null;
		if (groupFunStruct != null)
			addedGroupByColumn = groupFunStruct.getAddedGroupByColumn();

		if (rsList == null || rsList.isEmpty()
				|| (ObjectUtils.isEmpty(addedOrderByFields) && ObjectUtils.isEmpty(addedGroupByColumn))) return;

		//这里是操作实体, 用的是字段
		if (ObjectUtils.isNotEmpty(addedOrderByFields)) {
			Logger.debug("Recover auto AddedSelectColumn(OrderBy):" + addedOrderByFields);
			recover0(rsList, addedOrderByFields);
		}

		if (ObjectUtils.isNotEmpty(addedGroupByColumn)) {
			Logger.debug("Recover auto AddedSelectColumn(GroupBy):" + addedGroupByColumn);
			recover0(rsList, new ArrayList<>(addedGroupByColumn));
		}
	}

	private static <T> void recover0(List<T> rsList, List<String> addedFields) {
		try {
			T entity = (T) rsList.get(0).getClass().newInstance();
			Field[] fields = new Field[addedFields.size()];
			String fieldName;
			for (int i = 0; i < fields.length; i++) {
				fieldName = addedFields.get(i);
				int ch = fieldName.indexOf('.');
				if (ch > 0) {
					String entityname = rsList.get(0).getClass().getSimpleName().toLowerCase();
					if (!fieldName.toLowerCase().startsWith(entityname + ".")) continue; // TODO 暂时不支持删除子表自动加的字段。
					else fieldName = fieldName.substring(ch + 1);
				}
				fields[i] = HoneyUtil.getField(rsList.get(0).getClass(), fieldName);
				HoneyUtil.setAccessibleTrue(fields[i]);
				Object v = HoneyUtil.getValue(fields[i], entity);
				for (T t : rsList) {
					HoneyUtil.setFieldValue(fields[i], t, v);
				}
			}
		} catch (InstantiationException | IllegalAccessException | NoSuchFieldException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	// 返回List<String[]>,且没有分组,才会用这里的
	@SuppressWarnings("unchecked")
	public static <T> void recoverStringArrayForOrderBy(List<String[]> rsList) {

		if (rsList == null || rsList.size() == 0) return;

		List<String> addedOrderByFields = (List<String>) HoneyContext1.get(LocalType.SHARDING,
				StringConst.AddedOrderByColumn);  // 找下标，要用的是列名

		if (ObjectUtils.isEmpty(addedOrderByFields)) return;

		GroupFunStruct groupFunStruct = (GroupFunStruct) HoneyContext1.getInheritable(LocalType.SHARDING,
				StringConst.AddedOrderByFieldStruct);
		if (groupFunStruct == null) return;

		// 找到新加列的下标
		Set<Integer> addedOrderByFieldsIndex = new TreeSet<>();
		for (String s : addedOrderByFields) {
			addedOrderByFieldsIndex.add(groupFunStruct.getIndexByColumn(s)); // 找下标，要用的是列名
		}

		int oldLen = rsList.get(0).length;
		String tempArray[];
		String old[];
		for (int m = 0; m < rsList.size(); m++) {
			old = rsList.get(m);
			tempArray = new String[oldLen - addedOrderByFields.size()];
			for (int j = 0, k = 0; j < oldLen; j++) {
				if (addedOrderByFieldsIndex.contains(j)) continue; // 跳过自动加的分组列
				tempArray[k++] = old[j];
			}
			rsList.set(m, tempArray); // reset new value
		}
	}

}
