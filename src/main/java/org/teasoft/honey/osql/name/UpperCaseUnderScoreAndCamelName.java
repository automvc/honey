/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

/**
 * DB<-->Java,eg: ORDER_NO<-->orderNo.
 * @author Kingstar
 * @since  1.5
 */
public class UpperCaseUnderScoreAndCamelName extends UnderScoreAndCamelName{

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
		//need lowercase first if the name has upper case
		tableName = tableName.toLowerCase();
		return NameUtil.firstLetterToUpperCase(NameUtil.toCamelNaming(tableName));
	}

	@Override
	public String toFieldName(String columnName) {
		//need lowercase first if the name has upper case
		columnName = columnName.toLowerCase(); //if not , BEE_NAME->BEENAME  -> ??
		return NameUtil.toCamelNaming(columnName);
	}
	
}
