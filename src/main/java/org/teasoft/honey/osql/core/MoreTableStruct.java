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
 * 多表查询结构体.MoreTable Struct.
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
	 boolean oneHasOne; //v1.9.8  main table just has one subTable, and the subTable also only has one sub.
	                    //在主表只有从表1, 从表1也只有1个从表.
	 
	 Map<String,String> subDulFieldMap; //列名相同字段
	
	 String tableName;
//	 String entityName;
//	 String entityFullName; //inclue package
	 String columnsFull; // eg: tableName.column1,tableName.column2
	
	 String joinExpression;  // mainTable.mainField=subtableName.subField  
                                    //如果有subAlias,用subAlias   mainTable.mainField=subAlias.subField
	 Field subEntityField;   //for return  //用于返回拼装数据时,获取字段名
	 Object subObject;   //用于解析子对象的值到sql,   List字段时,是list里的实体对象.
	 String useSubTableName;  //有别名时,使用别名,而不是表名
	 
	 //v1.9.8
	 String onExpression="";  //eg: permit.valid = '1'  of (LEFT JOIN permit ON resource.id = permit.resource_id and permit.valid = '1') 
	 Class<?> subClass; //for listField   是list里的实体class类型.
	 boolean subOneIsList=false;   //子表1是否是List类型
	 boolean subTwoIsList=false;
//	 String mainColumnsForListType; //仅用于从表1是List类型
}
