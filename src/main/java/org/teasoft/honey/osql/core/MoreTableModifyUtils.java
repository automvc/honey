/*
 * Copyright 2016-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.List;
import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.annotation.FK;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.util.AnnoUtil;

/**
 * Parse entity for MoreTableModifyStruct.
 * @author Kingstar
 * use MoreInsertUtils name
 * @since  2.1.8
 * use MoreTableModifyUtils name
 * @since 2.4.0
 */
class MoreTableModifyUtils {

	private MoreTableModifyUtils() {}

	static <T> MoreTableModifyStruct _getMoreTableModifyStruct(T entity) {
		return _getMoreTableModifyStruct(entity, false);
	}

	static <T> MoreTableModifyStruct _getMoreTableModifyStruct(T entity, boolean isOneHasOneCheck) {
		if (entity == null) return null;

		Field field[] = HoneyUtil.getFields(entity.getClass());

		MoreTableModifyStruct moreTableModifyStruct = null;
		int subEntityFieldNum = 0;

		Field subField0 = null;
		Field subField1 = null;
		boolean subOneIsList = false;
		boolean subTwoIsList = false;

		boolean oneHasOne = false;

		String ref0[] = null;
		String ref1[] = null;
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
			throw new BeeException("One entity only supports two FK Annotation at most! " + entityFullName + " has "
					+ subEntityFieldNum + " now !");
		} else if (isOneHasOneCheck && subEntityFieldNum == 2) {
			throw new BeeException("The sub entity does not allow two fields to use FK Annotation!");
		}

		if (!isOneHasOneCheck && subEntityFieldNum == 1) { // 首次检测，isOneHasOneCheck=false,且第一层子表只有一个注解才会深入检测子表的属性
			if (!subOneIsList) { // 子表1不是List的情况才会检测。
				Object subEntity = null;
				try {
					if (subField0 != null) {
						HoneyUtil.setAccessibleTrue(subField0);
						subEntity = subField0.get(entity);
					}
				} catch (IllegalAccessException e) {
					Logger.debug(e.getMessage(), e);
				}
				MoreTableModifyStruct subStruct = _getMoreTableModifyStruct(subEntity, true);
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
			moreTableModifyStruct = new MoreTableModifyStruct();
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
			moreTableModifyStruct.subIsList = subIsList;
			moreTableModifyStruct.subField = subField;
			moreTableModifyStruct.ref = ref;
			moreTableModifyStruct.foreignKey = foreignKey;

			moreTableModifyStruct.oneHasOne = oneHasOne;
		}

		return moreTableModifyStruct;
	}

}
