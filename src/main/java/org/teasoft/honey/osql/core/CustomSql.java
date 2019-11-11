package org.teasoft.honey.osql.core;

import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * Custom SQL manage class
 * 用户自定义SQL 管理类
 * @author Kingstar
 * @since  1.0
 */
public class CustomSql {
	
	private static PropertiesReader customSqlProp;
	
	static {
		customSqlProp=new PropertiesReader("/bee.sql.properties");
	}
	
	public static String getCustomSql(String sqlId){
		return customSqlProp.getValue(sqlId);
	}

}
