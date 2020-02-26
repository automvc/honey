/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * Java驼峰命名与DB下划线命名互转
 * Java Camel and UnderScore transform.
 * DB<-->Java,eg: order_no<-->orderNo.
 * @author Kingstar
 * @since  1.5
 */
public class UnderScoreAndCamelName implements NameTranslate{
	
	@Override
	public String toTableName(String entityName) {
		return NameUtil.toUnderscoreNaming(entityName);
	}

	@Override
	public String toColumnName(String fieldName) {
          return NameUtil.toUnderscoreNaming(fieldName);
	}

	@Override
	public String toEntityName(String tableName) {
		if (HoneyConfig.getHoneyConfig().isDbNamingToLowerCaseBefore()) {
			//need lowercase first if the name has upper case
			tableName = tableName.toLowerCase(); 
		}
		return NameUtil.firstLetterToUpperCase(NameUtil.toCamelNaming(tableName));
	}

	@Override
	public String toFieldName(String columnName) {
		if (HoneyConfig.getHoneyConfig().isDbNamingToLowerCaseBefore()) {
			//need lowercase first if the name has upper case
			columnName = columnName.toLowerCase();  //if not , BEE_NAME->BEENAME  -> ??
		}
		return NameUtil.toCamelNaming(columnName);
	}

}
