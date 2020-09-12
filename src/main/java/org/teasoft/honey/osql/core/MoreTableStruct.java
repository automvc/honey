/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.util.Map;

import org.teasoft.bee.osql.annotation.JoinType;

/**
 * @author Kingstar
 * @since  1.7
 */
class MoreTableStruct {
	
	 JoinType joinType;  //联接类型
	 String mainField; //主表关联的字段
	 String subField;  //从表关联的字段
	 String subAlias;  //从表别名
	//main table don't record above 4 fields.
	 
	 int joinTableNum;  //just record in mainTable
	 
	 boolean hasSubAlias;
	 
	 Map<String,String> subDulFieldMap; //列名相同字段
	
	 String tableName;
	 String entityName;
	 String entityFullName; //inclue package
	 String columnsFull; // eg: tableName.column1,tableName.column2
	
	 String joinExpression;  // mainTable.mainField=subtableName.subField  
                                    //如何有subAlias,用subAlias   mainTable.mainField=subAlias.subField
	 Field subEntityField;   //for return
	 Object subObject;
	 String useSubTableName;  //有别名时,使用别名,而不是表名

}
