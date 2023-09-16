/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.List;
import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.annotation.FK;
import org.teasoft.honey.osql.util.AnnoUtil;

/**
 * @author Kingstar
 * @since  2.1.8
 */
public class MoreInsertUtils {

	private MoreInsertUtils() {}

	static <T> MoreTableInsertStruct _getMoreTableInsertStruct(T entity) {
		return _getMoreTableInsertStruct(entity, false);
	}

	static <T> MoreTableInsertStruct _getMoreTableInsertStruct(T entity,
			boolean isOneHasOneCheck) {
		if (entity == null) return null;

		Field field[] = entity.getClass().getDeclaredFields();

		MoreTableInsertStruct moreTableStruct = null;
		int subEntityFieldNum = 0;

		Field subField0 = null;
		Field subField1 = null;
		boolean subOneIsList = false;
		boolean subTwoIsList = false;

		boolean oneHasOne = false;

		String ref0[] = null;
		String ref1[]= null;
		String foreignKey0[] = null;
		String foreignKey1[] = null;

		int len = field.length;
		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipFieldForMoreTable(field[i])) continue;

			if (field[i] != null && AnnoUtil.isFK(field[i])) {
				subEntityFieldNum++;
				FK fk = field[i].getAnnotation(FK.class);
				if (subEntityFieldNum == 1) {
					subField0 = field[i];
					ref0 = fk.refBy();
					foreignKey0 = fk.value();
				} else if (subEntityFieldNum == 2) {
					subField1 = field[i];
					ref1 = fk.refBy();
					foreignKey1 = fk.value();
				}

				if (List.class.isAssignableFrom(field[i].getType())) {
					if (subEntityFieldNum == 1) {
						subOneIsList = true;
					} else if (subEntityFieldNum == 2) {
						subTwoIsList = true;
					}
				}
			}
		} // for end

		if (subEntityFieldNum > 2) { // 只支持一个实体里最多关联两个实体
			String entityFullName = entity.getClass().getName();
			throw new BeeException("One entity only supports two FK Annotation at most! "
					+ entityFullName + " has " + subEntityFieldNum + " now !");
		} else if (isOneHasOneCheck && subEntityFieldNum == 2) {
			throw new BeeException("The sub entity does not allow two fields to use FK Annotation!");
		}

		if (!isOneHasOneCheck && subEntityFieldNum == 1) { // 首次检测，isOneHasOneCheck=false,且第一层子表只有一个注解才会深入检测子表的属性
			if (!subOneIsList) { // 子表1不是List的情况才会检测。
				Object subEntity = null;
				try {
					if (subField0 != null) {
//						subField0.setAccessible(true);
						HoneyUtil.setAccessibleTrue(subField0);
						subEntity = subField0.get(entity);
					}
				} catch (IllegalAccessException e) {
					Logger.debug(e.getMessage(), e);
				}
				MoreTableInsertStruct subStruct = _getMoreTableInsertStruct(subEntity, true);
				if (subStruct != null && subStruct.subField.length == 1) {
					subTwoIsList = subStruct.subIsList[0];
					subField1 = subStruct.subField[0];
					ref1 = subStruct.ref[0];
					foreignKey1 = subStruct.foreignKey[0];

					oneHasOne = true;

					subEntityFieldNum++;
				}
			}
		}

		if (subEntityFieldNum > 0) {
			moreTableStruct = new MoreTableInsertStruct();
			boolean subIsList[] = new boolean[subEntityFieldNum];
			Field subField[] = new Field[subEntityFieldNum];
			String ref[][] = new String[subEntityFieldNum][];
			String foreignKey[][] = new String[subEntityFieldNum][];
			subIsList[0] = subOneIsList;
			subField[0] = subField0;

			ref[0] = ref0;
			foreignKey[0] = foreignKey0;
			if (subEntityFieldNum == 2) {
				subIsList[1] = subTwoIsList;
				subField[1] = subField1;
				ref[1] = ref1;
				foreignKey[1] = foreignKey1;
			}
			moreTableStruct.subIsList = subIsList;
			moreTableStruct.subField = subField;
			moreTableStruct.ref = ref;
			moreTableStruct.foreignKey = foreignKey;

			moreTableStruct.oneHasOne = oneHasOne;
		}

		return moreTableStruct;
	}

}
