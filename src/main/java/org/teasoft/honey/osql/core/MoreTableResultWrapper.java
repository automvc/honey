package org.teasoft.honey.osql.core;

import java.util.Map;

/**
 * one row record -> MoreTable entity
 * 
 * @since 3.0.0
 */
class MoreTableResultWrapper<T> {
	T mainObj = null; //保存主表一行数据
	String mainObjValueStr = null; //主表一行数据的字符类型值
	Map<String, Object> subObjMap = null; //<子表别名,子表一行数据>
	Map<String, StringBuffer> subObjValueStrMap = null;//<子表别名,子表一行数据的字符类型值>

	int rawRows; // just for log
}
