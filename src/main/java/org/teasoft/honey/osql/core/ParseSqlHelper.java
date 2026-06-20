package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.annotation.JoinTable3;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.util.StringUtils;

class EntityWrapper {
	List<String> columnList; //列名list
	List<String> classFieldnameList;//类字段list
	Map<String, Object> columnAndValue;//列名和值的map
	List<PreparedValue> preList;//占位对应的值/类型
}

public final class ParseSqlHelper {

	private static final String SEPARATOR = "#";

	private ParseSqlHelper() {
	}

	static <T> EntityWrapper parseEntity(T entity, int includeType) {
		if (entity == null) return null;

		Class<?> clazz;
		boolean paramIsClass = false;

		if (entity instanceof Class) {
			clazz = (Class<?>) entity;
			paramIsClass = true;
		} else {
			clazz = entity.getClass();
		}

		Object value = null;
		List<PreparedValue> preList = null;
		List<String> classFieldnameList = null;
		List<String> columnList = null;

		Field fields[] = HoneyUtil.getFields(clazz);
		int len = fields.length;
		Map<String, Object> columnAndValue = new LinkedHashMap<>();

		boolean isFirst = true;
		String column = null;

		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipField(fields[i])) continue;
			if (HoneyUtil.isSkipFieldJustFetch(fields[i])) continue;

			if (isFirst) {
				isFirst = false;
				preList = new ArrayList<>();
				classFieldnameList = new ArrayList<>();
				columnList = new ArrayList<>();
			}

			column = _toColumnName(fields[i].getName(), clazz);
			columnList.add(column);
			classFieldnameList.add(fields[i].getName());

			if (paramIsClass) continue;

			try {
				HoneyUtil.setAccessibleTrue(fields[i]);
				value = fields[i].get(entity);
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			}
			if (value == null) {
				continue; // MoreTable, 字段值为null的不处理；可以用condition.op(field, Op.eq, null)
			} else if (HoneyUtil.isContinueByCheckIncludeType(includeType, value)) {
				continue;
			}

			columnAndValue.put(column, value);
			preList.add(new PreparedValue(fields[i], value));
		}

		EntityWrapper wrapper = new EntityWrapper();
		wrapper.columnAndValue = columnAndValue;
		wrapper.preList = preList;
		wrapper.columnList = columnList;
		wrapper.classFieldnameList = classFieldnameList;

		return wrapper;
	}

	static <T> Map<String, MoreTableStruct3> parseJoins(T entity) {
		if (entity == null) return null;

		String packageAndClassName = entity.getClass().getName();
		String key = "ForMoreTable3:" + packageAndClassName;

		// TODO 每一次的对象不一样。   本次线程有效？？
		Map<String, MoreTableStruct3> map = (Map<String, MoreTableStruct3>) HoneyContext.getCommonCache(key);

		map=null;  //TODO
		if (map == null) {
			map = parseJoins0(entity);
			HoneyContext.addCommonCache(key, map);
		}
		return map;
	}

	static <T> Map<String, MoreTableStruct3> parseJoins0(T entity) {

		Map<String, MoreTableStruct3> result = new LinkedHashMap<>();
		if (entity == null) return result;

		MoreTableStruct3 moreTableStruct_first = null;
		MoreTableStructOverall overall = new MoreTableStructOverall();

		Field field[] = HoneyUtil.getFields(entity.getClass());
		int len = field.length;
		boolean hasJoinField = false;
		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipFieldForMoreTable(field[i])) continue; // 有Ignore注释,将不再处理JoinTable //TODO
			if (field[i] != null && field[i].isAnnotationPresent(JoinTable3.class)) {
				hasJoinField = true;
				int layer = 2;
				List<String> parentTree = new ArrayList<>();

				MoreTableStruct3 moreTableStruct = new MoreTableStruct3(field[i], entity, layer, parentTree);
				if (result.isEmpty()) {
					moreTableStruct_first = moreTableStruct;
					moreTableStruct_first.overall = overall;
					List<String> subClassIsList = new LinkedList<>();
					subClassIsList.add(entity.getClass().getName());
					moreTableStruct_first.overall.allEntityType = subClassIsList;
				}
				moreTableStruct_first.overall.allEntityType.add(moreTableStruct.currentIsList + SEPARATOR
						+ moreTableStruct.subClass.getName()); // TODO MD5

				result.put(moreTableStruct.subAlias, moreTableStruct);

				// just need adjust in layer 2
				if (StringUtils.isBlank(moreTableStruct.mainAlias)) {
					moreTableStruct.mainAlias = _toTableName(entity);
				}

				List<Class<?>> typeTree = new ArrayList<Class<?>>();
				typeTree.add(entity.getClass()); // root
				typeTree.add(moreTableStruct.subClass);

				moreTableStruct.typeTree = typeTree;
				// TODO change to debug
				Logger.info("The layer is: 1, class is: " + entity.getClass() + ", alias is: "
						+ moreTableStruct.mainAlias);

				String subType = ", class is: ";
				if (moreTableStruct.currentIsList) {
					subType = ", class is List, element type is: ";
				}
				Logger.info("The layer is: " + layer + subType + moreTableStruct.subClass.getName()
						+ ", alias is: " + moreTableStruct.subAlias);

				int orignalSize = result.size();
				_parseOneHasOne(moreTableStruct, result, layer, overall);
				int newSize = result.size();
				if (newSize > orignalSize) moreTableStruct.hasNextLayer = true;
			}
		}
		if (!hasJoinField) {
			throw new ConfigWrongException("No Join Field in " + entity.getClass().getName());
		}
		return result;
	}

	static <T> void _parseOneHasOne(MoreTableStruct3 currentMoreTableStruct, Map<String, MoreTableStruct3> result,
			int layer, MoreTableStructOverall overall) {

		Field field[] = HoneyUtil.getFields(currentMoreTableStruct.subClass);
		int len = field.length;
		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipFieldForMoreTable(field[i])) continue; // 有Ignore注释,将不再处理JoinTable //TODO
			if (field[i] != null && field[i].isAnnotationPresent(JoinTable3.class)) {
				layer = layer + 1;
				if (layer >= 10) {
					// TODO
					Logger.info("MoreTable do not support the join layer more than {layer}! It will be ignored!"
							.replace("{layer}", layer + ""));
					return;
				}

				if (currentMoreTableStruct.typeTree.contains(field[i].getType())) {
					continue;// 循环引用，则跳过
				}

				List<String> parentTree2 = new ArrayList<>(currentMoreTableStruct.parentTree);

				MoreTableStruct3 moreTableStruct2 = new MoreTableStruct3(field[i], currentMoreTableStruct.subObject,
						layer, parentTree2, true, currentMoreTableStruct.subAlias); // subAlias作用下一层的主表

				List<Class<?>> currentTypeTree = new ArrayList<Class<?>>(currentMoreTableStruct.typeTree);
				currentTypeTree.add(moreTableStruct2.subClass);
				moreTableStruct2.typeTree = currentTypeTree;

				overall.allEntityType.add(moreTableStruct2.currentIsList + SEPARATOR
						+ moreTableStruct2.subClass.getName());// TODO MD5

				result.put(moreTableStruct2.subAlias, moreTableStruct2);

				String subType = ", class is: ";
				if (moreTableStruct2.currentIsList) {
					subType = ", class is List, element type is: ";
					overall.setHasAnySubListEntity(moreTableStruct2.currentIsList);
				}
				Logger.info("The layer is: " + layer + subType + moreTableStruct2.subClass.getName()
						+ ", alias is: " + moreTableStruct2.subAlias);

				_parseOneHasOne(moreTableStruct2, result, layer, overall);
			}
		}
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	@SuppressWarnings("rawtypes")
	private static String _toColumnName(String fieldName, Class entityClass) {
		return HoneyUtil.toColumnName(fieldName, entityClass);
	}
}
