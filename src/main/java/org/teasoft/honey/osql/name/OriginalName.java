/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

import org.teasoft.bee.osql.NameTranslate;

/**
 * 返回原名称;但表名首字母转小写,实体名首字母转大写.
 * 像Oracle,H2,忽略大小写,字符一样,则应选用DbUpperAndJavaLower.
 * return original name,but the first letter in TableName is LowerCase,
 *  the first letter in EntityName is UpperCase.
 *  As Oracle,H2,if ignore case, the character are same,should select DbUpperAndJavaLower.
 * @author Kingstar
 * @since  1.5
 */
public class OriginalName implements NameTranslate{

	@Override
	public String toTableName(String entityName) {
		return NameUtil.firstLetterToLowerCase(entityName);
	}

	@Override
	public String toColumnName(String fieldName) {
		return fieldName;
	}

	@Override
	public String toEntityName(String tableName) {
		return NameUtil.firstLetterToUpperCase(tableName);
	}

	@Override
	public String toFieldName(String columnName) {
		return columnName;
	}

}
