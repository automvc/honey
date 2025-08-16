/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

/**
 * Java驼峰命名与DB下划线命名，且DB使用大写。
 * Java Camel and Database UnderScore & UpperCase transform.
 * Java<-->DB,eg: orderNo<-->ORDER_NO.
 * @author Kingstar
 * @since  1.5
 */
public class UpperCaseUnderScoreAndCamelName extends UnderScoreAndCamelName {

	@Override
	public String toTableName(String entityName) {
		return super.toTableName(entityName).toUpperCase();
	}

	@Override
	public String toColumnName(String fieldName) {
		return super.toColumnName(fieldName).toUpperCase();
	}

	@Override
	public String toEntityName(String tableName) {
		// need lowercase first if the name has upper case
		tableName = tableName.toLowerCase();
		return NameUtil.firstLetterToUpperCase(NameUtil.toCamelNaming(tableName));
	}

	@Override
	public String toFieldName(String columnName) {
		// need lowercase first if the name has upper case
		columnName = columnName.toLowerCase(); // if not , BEE_NAME->BEENAME -> ??
		return NameUtil.toCamelNaming(columnName);
	}

}
