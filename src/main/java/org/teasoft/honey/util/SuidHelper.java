/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyUtil;

/**
 * @author Kingstar
 * @since  1.9
 */
public final class SuidHelper {

	private SuidHelper() {}

	/**
	 * 将List<String[]>里第一列字符串转成逗号分隔的字符串.
	 * <br>Convert the first column string in list < string [] to comma separated string
	 * @param list List<String[]>
	 * @return 逗号分隔的字符串.
	 */
	public static String parseFirstColumn(List<String[]> list) {

		if (list == null) return "";
		String str = "";

		for (int i = 0; i < list.size(); i++) {
			str += list.get(i)[0];
			if (i != list.size() - 1) str += ",";
		}

		return str;
	}

	/**
	 * 将List里的字符数组转成entity结构.Convert the character array in the list to entity structure.
	 * @param list 需要转换的字符数组List.List of character arrays to be converted.
	 * @param startRow 开始行.start row.
	 * @param fieldNames 字段名(多个用逗号隔开).Field names (multiple separated by commas).
	 * @param entity 要转成的结构.the structure to be transformed into.
	 * @return 实体List. list of entity.
	 */
	public static <T> List<T> parseToEntity(List<String[]> list, int startRow, String fieldNames, T entity) {
		if (StringUtils.isBlank(fieldNames)) return Collections.emptyList();
		String[] fieldName = fieldNames.split(",");
		return parseToEntity(list, startRow, fieldName, entity);
	}

	/**
	 * 将List里的字符数组转成entity结构.Convert the character array in the list to entity structure.
	 * @param list 需要转换的字符数组List.List of character arrays to be converted.
	 * @param startRow 开始行.start row.
	 * @param endRow 结束行.end row.
	 * @param fieldNames 字段名(多个用逗号隔开).Field names (multiple separated by commas).
	 * @param entity 要转成的结构.the structure to be transformed into.
	 * @return 实体List. list of entity.
	 */
	public static <T> List<T> parseToEntity(List<String[]> list, int startRow, int endRow, String fieldNames, T entity) {
		if (StringUtils.isBlank(fieldNames)) return Collections.emptyList();
		String[] fieldName = fieldNames.split(",");
		return parseToEntity(list, startRow, endRow, fieldName, entity);
	}

	/**
	 * 将List里的字符数组转成entity结构.Convert the character array in the list to entity structure.
	 * @param list 需要转换的字符数组List.List of character arrays to be converted.
	 * @param startRow 开始行.start row.
	 * @param fieldName 字段名数组.field name array.
	 * @param entity 要转成的结构.the structure to be transformed into.
	 * @return 实体List. list of entity.
	 */
	public static <T> List<T> parseToEntity(List<String[]> list, int startRow, String[] fieldName, T entity) {
		if (ObjectUtils.isEmpty(list)) return Collections.emptyList();
		return parseToEntity(list, startRow, list.size() - 1, fieldName, entity);
	}

	/**
	 * 将List里的字符数组转成entity结构.Convert the character array in the list to entity structure.
	 * @param list 需要转换的字符数组List.List of character arrays to be converted.
	 * @param startRow 开始行.start row.
	 * @param endRow 结束行.end row.
	 * @param fieldName 字段名数组.field name array.
	 * @param entity 要转成的结构.the structure to be transformed into.
	 * @return 实体List. list of entity.
	 */
	public static <T> List<T> parseToEntity(List<String[]> list, int startRow, int endRow, String[] fieldName,
			final T entity) {
		T targetObj = null;
		List<T> rsList = null;
		Field field = null;
		String col[] = null;

		if (ObjectUtils.isEmpty(list)) return Collections.emptyList();

		rsList = new ArrayList<>();
		if (startRow < 0) startRow = 0;

		if (endRow > list.size() - 1) endRow = list.size() - 1;
		try {
			for (int i = startRow; i <= endRow; i++) {
				col = list.get(i);
				if (col == null || (col.length == 1 && fieldName.length != 1)) continue; // 忽略空行
				targetObj = (T) entity.getClass().newInstance();
				for (int j = 0; j < fieldName.length; j++) {
					if (StringUtils.isBlank(fieldName[j])) continue;
					try {
						field = HoneyUtil.getField(entity.getClass(), fieldName[j]);// 可能会找不到Javabean的字段
					} catch (NoSuchFieldException e) {
						if (i == startRow) Logger.warn("Can not find the field name : " + fieldName[j]);
						continue;
					}
					HoneyUtil.setAccessibleTrue(field);
					try {
						HoneyUtil.setFieldValue(field, targetObj, ObjectCreatorFactory.create(col[j], field.getType())); // 对相应Field设置
					} catch (IllegalArgumentException e) {
						Logger.warn(e.getMessage());
					}
				}
				rsList.add(targetObj);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		} catch (InstantiationException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			targetObj = null;
		}

		return rsList;
	}

	/**
	 * 将to在from有的属性,都复制到to中.
	 * @param from
	 * @param to
	 * @return 处理后的to对象
	 */
	public static <T> T copyEntity(Object from, T to) {

		if (from == null || to == null) return to;

		Field field = null;
		Field fields[] = HoneyUtil.getFields(to.getClass());

		int len = fields.length;

		for (int i = 0; i < len; i++) {
			int modifiers = fields[i].getModifiers();
			if (modifiers == 8 || modifiers == 16 || modifiers == 24 || modifiers == 26) {
				continue; // static,final,private static final
			}
			HoneyUtil.setAccessibleTrue(fields[i]);

			try {
				field = HoneyUtil.getField(from.getClass(), fields[i].getName());
			} catch (NoSuchFieldException e) {
				continue;
			}

			try {
				HoneyUtil.setAccessibleTrue(field);
				HoneyUtil.setFieldValue(fields[i], to, field.get(from));
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			}
		}

		return to;
	}

	/**
	 * 将entity实体转为Map,字段为serialVersionUID,JoinTable,Ignore,值为null,将被忽略.
	 * entity To Map, if the value of field is null will be ignored.
	 * also ignore serialVersionUID,JoinTable,Ignore.
	 * @param entity 需要转为Map的entity实体
	 * @return instance of Map<String, Object>
	 */
	public static <T> Map<String, Object> entityToMap(T entity) {

		if (entity == null) return null;

		Map<String, Object> map = null;
		Object v = null;
		Field fields[] = HoneyUtil.getFields(entity.getClass());

		for (int i = 0; i < fields.length; i++) {
			if (i == 0) map = new LinkedHashMap<>();
			HoneyUtil.setAccessibleTrue(fields[i]);

			try {
				v = fields[i].get(entity);
				if (v == null) continue;
				if (HoneyUtil.isSkipField(fields[i])) continue;
				map.put(fields[i].getName(), v);
			} catch (Exception e) {
				Logger.warn(e.getMessage(), e);
			}
		}
		return map;
	}

	public static <T> List<T> copyListEntity(List<?> fromList, Class<T> toClassType) {
		List<T> toList = null;

		if (fromList != null && fromList.size() > 0) {
			int size0 = fromList.size();
			toList = new ArrayList<>(size0);
			try {
				for (int i = 0; i < size0; i++) {
					T t = toClassType.newInstance();
					t = SuidHelper.copyEntity(fromList.get(i), t);
					toList.add(t);
				}
			} catch (Exception e) {
				Logger.warn(e.getMessage(), e);
			}
		}

		return toList;
	}

}
