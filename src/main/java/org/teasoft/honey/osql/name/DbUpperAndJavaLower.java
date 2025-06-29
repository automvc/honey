/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

import org.teasoft.bee.osql.NameTranslate;

/**
 * 数据库使用大写字母，Java使用小写字母;忽略大小写,使用的字符是一样的.
 * 像Oracle,H2,要想忽略大小写,字符一样,则应选用DbUpperAndJavaLower.
 * Database use UpperCase and Java use the LowerCase.
 * ignore case, the name is same.
 * @author Kingstar
 * @since  1.17
 */
public class DbUpperAndJavaLower implements NameTranslate {

	@Override
	public String toTableName(String entityName) {
		return entityName.toUpperCase();
	}

	@Override
	public String toColumnName(String fieldName) {
//		return fieldName.toUpperCase();
		return KeyWord.transformNameIfKeyWork(fieldName.toUpperCase());
	}

	@Override
	public String toEntityName(String tableName) {
		return NameUtil.firstLetterToUpperCase(tableName.toLowerCase());
	}

	@Override
	public String toFieldName(String columnName) {
		return columnName.toLowerCase();
	}

}
