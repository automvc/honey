package org.honey.osql.core;

import org.bee.osql.CustomSQL;
import org.honey.osql.util.PropertiesReader;

/**
 * Custom SQL manage class
 * 用户自定义SQL 管理类
 * @author Kingstar
 * @since  1.0
 */
public class CustomSqlLib { //implements CustomSQL{
	
	private static PropertiesReader customSql;
	
	static {
		customSql=new PropertiesReader("/bee.sql.properties");
	}
	
//	@Override
	public static String getCustomSql(String sqlId){
		return customSql.getValue(sqlId);
	}

}
