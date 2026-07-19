package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.exception.ConfigWrongException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.util.StringUtils;

class EntityWrapper {
	List<String> columnList; // 列名list
	Map<String, Object> columnAndValue;// 列名和值的map
	List<PreparedValue> preList;// 占位对应的值/类型
}

public final class ParseSqlHelper {

	private static final String SEPARATOR = "#";

	private ParseSqlHelper() {}

	@SuppressWarnings("unchecked")
	static <T> EntityWrapper parseEntity(T entityOrClass, int includeType) {
		if (entityOrClass == null) return null;

		EntityWrapper wrapper = new EntityWrapper();
		Class<?> clazz;
		boolean paramIsClass = false;

		if (entityOrClass instanceof Class) {
			clazz = (Class<?>) entityOrClass;
			paramIsClass = true;
		} else {
			clazz = entityOrClass.getClass();
		}

		List<String> columnList = null;
		boolean hasCacheColumnList = false;
		columnList = (List<String>) HoneyContext1.get(StringConst.FOREVER_COLUMNLIST + clazz.getName());
		if (columnList != null) {
			wrapper.columnList = columnList;
			hasCacheColumnList = true;
		}

		Object value = null;
		List<PreparedValue> preList = null;

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
				if (!hasCacheColumnList) columnList = new ArrayList<>();
			}

			column = _toColumnName(fields[i].getName(), clazz);
			if (!hasCacheColumnList) columnList.add(column);

			if (paramIsClass) continue;

			try {
				HoneyUtil.setAccessibleTrue(fields[i]);
				value = fields[i].get(entityOrClass);
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

		if (!hasCacheColumnList) {
			wrapper.columnList = columnList;
			HoneyContext1.set(StringConst.FOREVER_COLUMNLIST + entityOrClass.getClass().getName(), columnList);
		}
		wrapper.columnAndValue = columnAndValue;
		wrapper.preList = preList;

		return wrapper;
	}

	@SuppressWarnings("unchecked")
	public static List<String> getColumnList(Class<?> clazz) {
		List<String> columnList = (List<String>) HoneyContext1.get(StringConst.FOREVER_COLUMNLIST + clazz.getName());
		if (columnList != null) return columnList;
		Field fields[] = HoneyUtil.getFields(clazz);

		String column = null;
		int len = fields.length;
		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipField(fields[i])) continue;
			if (HoneyUtil.isSkipFieldJustFetch(fields[i])) continue;

			if (columnList == null) columnList = new ArrayList<>();
			column = _toColumnName(fields[i].getName(), clazz);
			columnList.add(column);
		}

		HoneyContext1.set(StringConst.FOREVER_COLUMNLIST + clazz.getName(), columnList);
		return columnList;
	}

	public static void checkFieldName(String columnName, Class<?> clazz, String fieldNameForLog) {
		List<String> list = ParseSqlHelper.getColumnList(clazz);
		if (list == null) return;
		for (String f : list) {
			if (columnName.equals(f)) return;
		}
		throw new ConfigWrongException(
				"The fieldName: '" + fieldNameForLog + "' do not in the class: " + clazz.getName());
	}

	static <T> Map<String, MoreTableStruct3> parseJoins(T entity) {
		return parseJoins(entity, SuidType.SELECT);
	}

	@SuppressWarnings("unchecked")
	static <T> Map<String, MoreTableStruct3> parseJoins(T entity, SuidType suidType) {
		if (entity == null) return null;

		String packageAndClassName = entity.getClass().getName();
		String key = StringConst.ForMoreTableStruct3 + suidType.name() + packageAndClassName;

		// 每一次的对象不一样。 本次线程有效
		Map<String, MoreTableStruct3> map = (Map<String, MoreTableStruct3>) HoneyContext1
				.getInheritable(LocalType.REQUEST, key);
		if (map == null) {
			map = parseJoins0(entity, suidType);
			HoneyContext1.setInheritable(LocalType.REQUEST, key, map);
		}
		return map;
	}

	static <T> Map<String, MoreTableStruct3> parseJoins0(T entity, SuidType suidType) {

		Map<String, MoreTableStruct3> result = new LinkedHashMap<>();
		if (entity == null) return result;

		MoreTableStruct3 moreTableStruct_first = null;
		MoreTableStructOverall overall = new MoreTableStructOverall();
		// 是在解析entity成sql时才设置overall的subDulColumnMap

		Field field[] = HoneyUtil.getFields(entity.getClass());
		int len = field.length;
		boolean hasJoinField = false;
		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipFieldForMoreTable(field[i])) continue; // 有Ignore注释,将不再处理JoinTable
			if (field[i] != null
					&& (AnnoUtil.isJoinTable(field[i]) || (SuidType.MODIFY == suidType && AnnoUtil.isFK(field[i])))) {
				hasJoinField = true;
				int layer = 2;
				List<String> parentTree = new ArrayList<>();

				MoreTableStruct3 moreTableStruct = new MoreTableStruct3(field[i], entity, layer, parentTree, suidType);
				if (result.isEmpty()) {
					moreTableStruct_first = moreTableStruct;
					moreTableStruct_first.overall = overall;
					List<String> subClassIsList = new LinkedList<>();
					subClassIsList.add(entity.getClass().getName());
					moreTableStruct_first.overall.allEntityType = subClassIsList;
				}
				moreTableStruct_first.overall.allEntityType
						.add(moreTableStruct.currentIsList + SEPARATOR + moreTableStruct.subClass.getName()); // TODO
																												// MD5

				result.put(moreTableStruct.subAlias, moreTableStruct);

				// just need adjust in layer 2
				if (StringUtils.isBlank(moreTableStruct.mainAlias)) {
					moreTableStruct.mainAlias = _toTableName(entity);
				}

				List<Class<?>> typeTree = new ArrayList<Class<?>>();
				typeTree.add(entity.getClass()); // root
				typeTree.add(moreTableStruct.subClass);

				moreTableStruct.typeTree = typeTree;
				Logger.debug(
						"The layer is: 1, class is: " + entity.getClass() + ", alias is: " + moreTableStruct.mainAlias);

				String subType = ", class is: ";
				if (moreTableStruct.currentIsList) {
					subType = ", class is List, element type is: ";
				}
				Logger.debug("The layer is: " + layer + subType + moreTableStruct.subClass.getName() + ", alias is: "
						+ moreTableStruct.subAlias);

				int orignalSize = result.size();
				_parseOneHasOne(moreTableStruct, result, layer, overall, suidType);
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
			int layer, MoreTableStructOverall overall, SuidType suidType) {

		Field field[] = HoneyUtil.getFields(currentMoreTableStruct.subClass);
		int len = field.length;
		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipFieldForMoreTable(field[i])) continue; // 有Ignore注释,将不再处理JoinTable //todo
//			if (field[i] != null && AnnoUtil.isJoinTable(field[i])) {
			if (field[i] != null
					&& (AnnoUtil.isJoinTable(field[i]) || (SuidType.MODIFY == suidType && AnnoUtil.isFK(field[i])))) {

				layer = layer + 1;
				if (layer >= 10) {
					// todo
					Logger.info("MoreTable do not support the join layer more than {layer}! It will be ignored!"
							.replace("{layer}", layer + ""));
					return;
				}

				if (currentMoreTableStruct.typeTree.contains(field[i].getType())) {
					continue;// 循环引用，则跳过
				}

				List<String> parentTree2 = new ArrayList<>(currentMoreTableStruct.parentTree);

				MoreTableStruct3 moreTableStruct2 = new MoreTableStruct3(field[i], currentMoreTableStruct.subObject,
						layer, parentTree2, suidType, true, currentMoreTableStruct.subAlias); // subAlias作用下一层的主表

				List<Class<?>> currentTypeTree = new ArrayList<Class<?>>(currentMoreTableStruct.typeTree);
				currentTypeTree.add(moreTableStruct2.subClass);
				moreTableStruct2.typeTree = currentTypeTree;

				overall.allEntityType
						.add(moreTableStruct2.currentIsList + SEPARATOR + moreTableStruct2.subClass.getName());// TODO
																												// MD5

				result.put(moreTableStruct2.subAlias, moreTableStruct2);

				String subType = ", class is: ";
				if (moreTableStruct2.currentIsList) {
					subType = ", class is List, element type is: ";
					overall.setHasAnySubListEntity(moreTableStruct2.currentIsList);
				}
				Logger.debug("The layer is: " + layer + subType + moreTableStruct2.subClass.getName() + ", alias is: "
						+ moreTableStruct2.subAlias);

				_parseOneHasOne(moreTableStruct2, result, layer, overall, suidType);
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
