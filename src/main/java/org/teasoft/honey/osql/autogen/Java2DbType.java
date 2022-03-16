/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.autogen;

import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.osql.DatabaseConst;

/**
 * @author Kingstar
 * @since  1.9
 */
public class Java2DbType {

	private static Map<String, Map<String, String>> java2DbTypeMap = new HashMap<>();

	static {
		java2DbTypeMap.put(DatabaseConst.ORACLE.toLowerCase(), forOracle());
		java2DbTypeMap.put(DatabaseConst.SQLite.toLowerCase(), forSQLite());
		java2DbTypeMap.put(DatabaseConst.MYSQL.toLowerCase(), forMySQL());
		java2DbTypeMap.put(DatabaseConst.MariaDB.toLowerCase(), forMySQL());
		java2DbTypeMap.put(DatabaseConst.H2.toLowerCase(), forH2());
		java2DbTypeMap.put(DatabaseConst.PostgreSQL.toLowerCase(), forPostgreSQL());
		java2DbTypeMap.put(DatabaseConst.SQLSERVER.toLowerCase(), forSQLSERVER());
		java2DbTypeMap.put(DatabaseConst.Cassandra.toLowerCase(), forCassandra()); //V1.11
		//...
	}
	
	private Java2DbType(){}

	public static Map<String, String> getJava2DbType(String databaseName) {
		return java2DbTypeMap.get(databaseName.toLowerCase());
	}
	
	/**
	 * support set the Java2DbTypeMap custom
	 * @param databaseName
	 * @param OneDb_Java2DbTypeMap
	 * @since 1.11
	 */
	public static void setJava2DbType(String databaseName, Map<String, String> OneDb_Java2DbTypeMap) {
		java2DbTypeMap.put(databaseName, OneDb_Java2DbTypeMap);
	}

	/**
	 * support append the Java2DbTypeMap custom
	 * @param databaseName
	 * @param OneDb_Java2DbTypeMap
	 * @since 1.11
	 */
	public static void appendJava2DbType(String databaseName, Map<String, String> OneDb_Java2DbTypeMap) {
		Map<String, String> map = java2DbTypeMap.get(databaseName.toLowerCase());
		if (map == null) {
			java2DbTypeMap.put(databaseName.toLowerCase(), OneDb_Java2DbTypeMap);
		} else {
			map.putAll(OneDb_Java2DbTypeMap);
		}
	}
	
	/**
	 * 此方法只是考虑一般情况而采用默认值,需根据具体情况修改
	 * <br>This method only considers the general situation 
	 * <br>and adopts the default value, which needs to be modified
	 * <br> according to the specific situation.
	 * @return java类型对应的oracle列类型.the Oracle field type corresponding to the Java type.
	 */
	private static Map<String, String> forOracle() {
		Map<String, String> java2DbType = new HashMap<>();
		java2DbType.put("java.lang.String", "varchar2(255)");
		java2DbType.put("java.lang.Integer", "number(10)");
		java2DbType.put("java.lang.Long", "number(19)");
		java2DbType.put("java.lang.Double", "number(19,6)");
		java2DbType.put("java.lang.Float", "number(19,6)");
		java2DbType.put("java.lang.Short", "number(5)");
		java2DbType.put("java.lang.Byte", "number(3)");
//		java2DbType.put("[B", 8); //byte[]  
		java2DbType.put("java.lang.Boolean", "varchar2(1)");

		//支持原生类型
		java2DbType.put("int", "number(10)");
		java2DbType.put("long", "number(19)");
		java2DbType.put("double", "number(19,6)");
		java2DbType.put("float", "number(19,6)");
		java2DbType.put("short", "number(5)");
		java2DbType.put("byte", "number(3)");
		java2DbType.put("boolean", "varchar2(1)");
		
		java2DbType.put("char", "char(1)"); //V1.11

		java2DbType.put("java.math.BigDecimal", "number(19)");

		java2DbType.put("java.sql.Date", "date");
		java2DbType.put("java.util.Date", "date"); //V1.11
		java2DbType.put("java.sql.Time", "date");
		java2DbType.put("java.sql.Timestamp", "timestamp"); //V1.11 javabean与DB的要对应
		//javabean是Timestamp,DB是varchar2,只能设置,查询会有问题.
//		java2DbType.put("java.sql.Timestamp", "varchar2(100)"); 
		java2DbType.put("java.sql.Blob", "blob");
		java2DbType.put("java.sql.Clob", "clob");

		java2DbType.put("java.sql.NClob", "nclob");
//		java2DbType.put("java.sql.RowId", 17);
//		java2DbType.put("java.sql.SQLXML", 18);

		java2DbType.put("java.math.BigInteger", "number(19,6)");

		return java2DbType;
	}

	private static Map<String, String> forSQLite() {

		Map<String, String> java2DbType = new HashMap<>();

		java2DbType.put("java.lang.String", "varchar(100)");//100
		java2DbType.put("java.lang.Integer", "int(11)");
		java2DbType.put("java.lang.Long", "bigint(20)");
		java2DbType.put("java.lang.Double", "FLOAT8");
		java2DbType.put("java.lang.Float", "FLOAT4");
		java2DbType.put("java.lang.Short", "INT2");
		java2DbType.put("java.lang.Byte", "TINYINT");
		java2DbType.put("java.lang.Boolean", "BOOLEAN");

		//支持原生类型
		java2DbType.put("int", "int(11)");
		java2DbType.put("long", "bigint(20)");
		java2DbType.put("double", "FLOAT8");
		java2DbType.put("float", "FLOAT4");
		java2DbType.put("short", "INT2");
		java2DbType.put("byte", "TINYINT");
		java2DbType.put("boolean", "BOOLEAN");
		
		java2DbType.put("char", "varchar(1)");//V1.11

		java2DbType.put("java.math.BigDecimal", "number(19,6)");

		java2DbType.put("java.sql.Date", "datetime");
		java2DbType.put("java.util.Date", "datetime");//V1.11
		java2DbType.put("java.sql.Time", "datetime");
		java2DbType.put("java.sql.Timestamp", "timestamp");
		java2DbType.put("java.sql.Blob", "Blob");
		java2DbType.put("java.sql.Clob", "text");

		java2DbType.put("java.math.BigInteger", "number(19)");

		return java2DbType;
	}

	private static Map<String, String> forMySQL() {
		Map<String, String> java2DbType = new HashMap<>();

		java2DbType.put("java.lang.String", "varchar(255)");
		java2DbType.put("java.lang.Integer", "int(11)");
		java2DbType.put("java.lang.Long", "bigint(20)");
		java2DbType.put("java.lang.Double", "Double");
		java2DbType.put("java.lang.Float", "Float(19,6)");
		java2DbType.put("java.lang.Short", "SMALLINT");
		java2DbType.put("java.lang.Byte", "TINYINT");
		java2DbType.put("java.lang.Boolean", "BIT");

		//支持原生类型
		java2DbType.put("int", "int(11)");
		java2DbType.put("long", "bigint(20)");
		java2DbType.put("double", "Double");
		java2DbType.put("float", "Float(19,6)");
		java2DbType.put("short", "SMALLINT");
		java2DbType.put("byte", "TINYINT");
		java2DbType.put("boolean", "BIT");
		java2DbType.put("Boolean", "BIT");
		
		java2DbType.put("char", "char(1)");//V1.11
		
		java2DbType.put("integer", "int(11)"); //for finding easily
		java2DbType.put("Integer", "int(11)"); //方便匹配
		java2DbType.put("string", "varchar(255)");
		java2DbType.put("String", "varchar(255)");
		java2DbType.put("Timestamp", "timestamp");
		java2DbType.put("Date", "datetime");
		java2DbType.put("timestamp", "timestamp");
		java2DbType.put("date", "datetime");

		java2DbType.put("java.math.BigDecimal", "DECIMAL(19,6)");

		java2DbType.put("java.sql.Date", "datetime");
		java2DbType.put("java.util.Date", "datetime");//V1.11
		java2DbType.put("java.sql.Time", "datetime");
		java2DbType.put("java.sql.Timestamp", "timestamp");
		java2DbType.put("java.sql.Blob", "Blob");
		java2DbType.put("java.sql.Clob", "Clob");

		java2DbType.put("java.math.BigInteger", "DECIMAL(19)");
		return java2DbType;
	}

	private static Map<String, String> forH2() {
		Map<String, String> java2DbType = new HashMap<>();

		java2DbType.put("java.lang.String", "VARCHAR2(255)");
		java2DbType.put("java.lang.Integer", "INT4");
		java2DbType.put("java.lang.Long", "BIGINT");
		java2DbType.put("java.lang.Double", "FLOAT8");
		java2DbType.put("java.lang.Float", "FLOAT4");
		java2DbType.put("java.lang.Short", "INT2");
		java2DbType.put("java.lang.Byte", "INT2");
		java2DbType.put("java.lang.Boolean", "BIT");

		//支持原生类型
		java2DbType.put("int", "INT4");
		java2DbType.put("long", "BIGINT");
		java2DbType.put("double", "FLOAT8");
		java2DbType.put("float", "FLOAT4");
		java2DbType.put("short", "INT2");
		java2DbType.put("byte", "INT2");
		java2DbType.put("boolean", "BIT");
		
		java2DbType.put("char", "CHAR(1)");//V1.11

		java2DbType.put("java.math.BigDecimal", "NUMBER");

		java2DbType.put("java.sql.Date", "datetime");
		java2DbType.put("java.util.Date", "datetime");//V1.11
		java2DbType.put("java.sql.Time", "datetime");
		java2DbType.put("java.sql.Timestamp", "timestamp");
		java2DbType.put("java.sql.Blob", "Blob");
		java2DbType.put("java.sql.Clob", "Clob");

		java2DbType.put("java.math.BigInteger", "NUMBER");
		return java2DbType;
	}

	private static Map<String, String> forPostgreSQL() {
		Map<String, String> java2DbType = new HashMap<>();

		java2DbType.put("java.lang.String", "varchar(255)");
		java2DbType.put("java.lang.Integer", "int4");
		java2DbType.put("java.lang.Long", "int8");
		java2DbType.put("java.lang.Double", "float8");
		java2DbType.put("java.lang.Float", "float4");
		java2DbType.put("java.lang.Short", "int4");
		java2DbType.put("java.lang.Byte", "int4");
		java2DbType.put("java.lang.Boolean", "bit");

		//支持原生类型
		java2DbType.put("int", "int4");
		java2DbType.put("long", "int8");
		java2DbType.put("double", "float8");
		java2DbType.put("float", "float4");
		java2DbType.put("short", "int4");
		java2DbType.put("byte", "int4");
		java2DbType.put("boolean", "bit");
		
		java2DbType.put("char", "char(1)");//V1.11

		java2DbType.put("java.math.BigDecimal", "decimal(19,6)");

		java2DbType.put("java.sql.Date", "date");  //java.sql.Date转成数据库date类型，会丢失时分秒数据。
		java2DbType.put("java.util.Date", "timestamp"); //V1.11
		java2DbType.put("java.sql.Time", "time");
		java2DbType.put("java.sql.Timestamp", "timestamp");
		java2DbType.put("java.sql.Blob", "Blob");
		java2DbType.put("java.sql.Clob", "Clob");

		java2DbType.put("java.math.BigInteger", "decimal(19)");
		return java2DbType;
	}

	private static Map<String, String> forSQLSERVER() {
		Map<String, String> java2DbType = new HashMap<>();

		java2DbType.put("java.lang.String", "nvarchar(255)");
		java2DbType.put("java.lang.Integer", "int");
		java2DbType.put("java.lang.Long", "bigint"); //fixed
		java2DbType.put("java.lang.Double", "float");
		java2DbType.put("java.lang.Float", "real");
		java2DbType.put("java.lang.Short", "smallint");
		java2DbType.put("java.lang.Boolean", "char(1)");
		java2DbType.put("java.math.BigDecimal", "smallmoney");

		//支持原生类型
		java2DbType.put("int", "int");
		java2DbType.put("long", "bigint");
		java2DbType.put("double", "float");
		java2DbType.put("float", "real");
		java2DbType.put("short", "smallint");
//		java2DbType.put("byte", "smallmoney");
		java2DbType.put("boolean", "char(1)");
		
		java2DbType.put("char", "char(1)");//V1.11

		java2DbType.put("java.math.BigDecimal", "decimal(19,6)");

		java2DbType.put("java.sql.Date", "datetime");
		java2DbType.put("java.util.Date", "datetime");//V1.11
		java2DbType.put("java.sql.Time", "time");
//		java2DbType.put("java.sql.Timestamp", "timestamp");  //bug
		java2DbType.put("java.sql.Timestamp", "datetime");//V1.11
		java2DbType.put("java.sql.Blob", "Blob");
		java2DbType.put("java.sql.Clob", "Clob");

		java2DbType.put("java.math.BigInteger", "decimal(19)");
		return java2DbType;
	}
	
	private static Map<String, String> forCassandra() {
		Map<String, String> java2DbType = new HashMap<>();

		java2DbType.put("java.lang.String", "text");
		java2DbType.put("java.lang.Integer", "int");
		java2DbType.put("java.lang.Long", "bigint");
		java2DbType.put("java.lang.Double", "double");
		java2DbType.put("java.lang.Float", "float");
		java2DbType.put("java.lang.Short", "smallint");
		java2DbType.put("java.lang.Byte", "tinyint");
		java2DbType.put("java.lang.Boolean", "boolean");

		//支持原生类型
		java2DbType.put("int", "int");
		java2DbType.put("long", "bigint");
		java2DbType.put("double", "double");
		java2DbType.put("float", "Float");
		java2DbType.put("short", "smallint");
		java2DbType.put("byte", "tinyint");
		java2DbType.put("boolean", "boolean");
		java2DbType.put("Boolean", "boolean");
		java2DbType.put("char", "varchar");
		
		java2DbType.put("integer", "int"); //for finding easily
		java2DbType.put("Integer", "int"); //方便匹配
		java2DbType.put("string", "text");
		java2DbType.put("String", "text");
		java2DbType.put("Timestamp", "timestamp");
		java2DbType.put("Date", "date");
		java2DbType.put("timestamp", "timestamp");
		java2DbType.put("date", "date");
		java2DbType.put("time", "time");

		java2DbType.put("java.math.BigDecimal", "decimal");

		java2DbType.put("java.sql.Date", "date");
		java2DbType.put("java.util.Date", "date");//V1.11
		java2DbType.put("java.sql.Time", "time");
		java2DbType.put("java.sql.Timestamp", "timestamp");
		java2DbType.put("java.sql.Blob", "Blob");

		java2DbType.put("java.math.BigInteger", "decimal");
		java2DbType.put("java.util.UUID", "uuid");
		
		java2DbType.put("java.util.List","list");
		java2DbType.put("java.util.Set","set");
		java2DbType.put("java.util.Map","map");
		
		return java2DbType;
	}
}
