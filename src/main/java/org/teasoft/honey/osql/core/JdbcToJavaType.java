/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * @author Kingstar
 * @since  1.17
 */
public class JdbcToJavaType {
	private static final String STRING = "String";
	private static Map<String, Map<String, String>> dbJdbc2JavaTypeMap = new HashMap<>();
	
	private static PropertiesReader jdbcTypeCustomProp = new PropertiesReader("jdbcTypeToFieldType.properties");
	private static PropertiesReader jdbcTypeCustomProp_specificalDB = null;

	private JdbcToJavaType() {}
	
	static {
		String dbName = HoneyConfig.getHoneyConfig().getDbName();
		initTypeMapConfig(dbName);
	}
	
	public static Map<String, String> getJdbcToJavaType(String dbName) {
		String t_dbName = (dbName == null ? dbName : dbName.toLowerCase());
		Map<String, String> map = dbJdbc2JavaTypeMap.get(t_dbName);
		if (map == null) {
			initTypeMapConfig(dbName);
			map = dbJdbc2JavaTypeMap.get(t_dbName);
			if (map == null) {
				map=getCommon();
				dbJdbc2JavaTypeMap.put(t_dbName, map);
			}
		}

		return map;
	}

	/**
	 * support set the JdbcToJavaType custom
	 * @param databaseName
	 * @param oneDb_Jdbc2JavaTypeMap
	 */
	public static void setJdbcToJavaType(String databaseName, Map<String, String> oneDb_Jdbc2JavaTypeMap) {
		dbJdbc2JavaTypeMap.put(databaseName, oneDb_Jdbc2JavaTypeMap);
	}

	
	public static void appendJdbcToJavaType(String databaseName,Map<String, String> oneDb_Java2DbTypeMap) {
		Map<String, String> map = dbJdbc2JavaTypeMap.get(databaseName.toLowerCase());
		if (map == null) {
			setJdbcToJavaType(databaseName.toLowerCase(), oneDb_Java2DbTypeMap);
		} else {
			map.putAll(oneDb_Java2DbTypeMap);
		}
	}
	
	public static void initTypeMapConfig(String dbName) {

		if (dbName == null) {
			Logger.warn("The dbName is null");
			return;
		}

		String proFileName = "jdbcTypeToFieldType-{DbName}.properties";

		initJdbcTypeMap(dbName);
		appendJdbcTypeCustomProp(dbName);

		jdbcTypeCustomProp_specificalDB = new PropertiesReader(
				proFileName.replace("{DbName}", dbName));
		if (jdbcTypeCustomProp_specificalDB != null) {
			appendJdbcTypeCustomProp_specificalDB(dbName);
		}
	}
	
	private static void appendJdbcTypeCustomProp(String dbName) {
		Map<String, String> map = dbJdbc2JavaTypeMap.get(dbName.toLowerCase());
		if (map == null) {
			map= new HashMap<>(); 
		}
		for (String s : jdbcTypeCustomProp.getKeys()) {
			map.put(s, jdbcTypeCustomProp.getValue(s));
		}
		setJdbcToJavaType(dbName, map);
	}

	private static void appendJdbcTypeCustomProp_specificalDB(String dbName) {
		Map<String, String> map = dbJdbc2JavaTypeMap.get(dbName.toLowerCase());
		if (map == null) {
			map= new HashMap<>(); 
		}
		for (String s : jdbcTypeCustomProp_specificalDB.getKeys()) {
			map.put(s, jdbcTypeCustomProp_specificalDB.getValue(s));
		}
		setJdbcToJavaType(dbName, map);
	}
	
	public static void initJdbcTypeMap(String dbName) {

		if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.ORACLE.toLowerCase(), forOracle());
		else if (DatabaseConst.SQLite.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.SQLite.toLowerCase(), forSQLite());
		else if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.MYSQL.toLowerCase(), forMySQL());
		else if (DatabaseConst.MariaDB.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.MariaDB.toLowerCase(), forMySQL());
		else if (DatabaseConst.H2.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.H2.toLowerCase(), forH2());
		else if (DatabaseConst.PostgreSQL.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.PostgreSQL.toLowerCase(), forPostgreSQL());
		else if (DatabaseConst.SQLSERVER.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.SQLSERVER.toLowerCase(), forSQLSERVER());
		else if (DatabaseConst.Cassandra.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.Cassandra.toLowerCase(), forCassandra());
		else if (DatabaseConst.MongoDB.equalsIgnoreCase(dbName))
			setJdbcToJavaType(DatabaseConst.MongoDB.toLowerCase(), forMongoDB());
		else 
			setJdbcToJavaType(dbName.toLowerCase(), getCommon());
	}

	
	private static Map<String, String> getCommon() {
		Map<String, String> jdbc2JavaType = new HashMap<>();
		
		//url: https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
		//		https://docs.oracle.com/javadb/10.8.3.0/ref/rrefjdbc20377.html
		//		https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html
		jdbc2JavaType.put("CHAR", STRING);
		jdbc2JavaType.put("VARCHAR", STRING);
		jdbc2JavaType.put("LONGVARCHAR", STRING);
		jdbc2JavaType.put("CHARACTER", STRING);

		jdbc2JavaType.put("NVARCHAR", STRING);
		jdbc2JavaType.put("NCHAR", STRING);

		jdbc2JavaType.put("NUMERIC", "BigDecimal");
		jdbc2JavaType.put("DECIMAL", "BigDecimal");

		jdbc2JavaType.put("BIT", "Boolean");
		jdbc2JavaType.put("BOOLEAN", "Boolean");

		//rs.getObject(int index)  bug   
		//pst.setByte(i+1,(Byte)value); break;设置查询没问题,结果也能返回,用rs.getObject拿结果时才报错
		jdbc2JavaType.put("TINYINT", "Byte");
		jdbc2JavaType.put("SMALLINT", "Short");

		jdbc2JavaType.put("INT", "Integer");
		jdbc2JavaType.put("INTEGER", "Integer");

		jdbc2JavaType.put("BIGINT", "Long");
		jdbc2JavaType.put("REAL", "Float");
		jdbc2JavaType.put("FLOAT", "Float"); //notice: mysql在创表时,要指定float的小数位数,否则查询时不能用=精确查询
		jdbc2JavaType.put("DOUBLE", "Double");

		jdbc2JavaType.put("BINARY", "byte[]");
		jdbc2JavaType.put("VARBINARY", "byte[]");
		jdbc2JavaType.put("LONGVARBINARY", "byte[]");
		
		jdbc2JavaType.put("image","byte[]");

		jdbc2JavaType.put("DATE", "Date");
		jdbc2JavaType.put("TIME", "Time");
		jdbc2JavaType.put("TIMESTAMP", "Timestamp");

		jdbc2JavaType.put("CLOB", "Clob");
		jdbc2JavaType.put("BLOB", "Blob");
		jdbc2JavaType.put("ARRAY", "Array");

		jdbc2JavaType.put("NCLOB", "java.sql.NClob");//JDK6
		jdbc2JavaType.put("ROWID", "java.sql.RowId"); //JDK6
		jdbc2JavaType.put("SQLXML", "java.sql.SQLXML"); //JDK6

		// JDBC 4.2 JDK8
		jdbc2JavaType.put("TIMESTAMP_WITH_TIMEZONE", "Timestamp");
		jdbc2JavaType.put("TIMESTAMP WITH TIME ZONE", "Timestamp"); //test in oralce 11g
		jdbc2JavaType.put("TIMESTAMP WITH LOCAL TIME ZONE", "Timestamp");//test in oralce 11g
		
		//V1.11
		jdbc2JavaType.put("JSON", STRING);
		//mysql 8.0
		jdbc2JavaType.put("TEXT", STRING);
		jdbc2JavaType.put("LONGTEXT", STRING);
		jdbc2JavaType.put("TINYTEXT", STRING);
		jdbc2JavaType.put("MEDIUMTEXT", STRING);
		
		return jdbc2JavaType;
	}

	private static Map<String, String> forOracle() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();

//		https://docs.oracle.com/cd/B12037_01/java.101/b10983/datamap.htm
//		https://docs.oracle.com/cd/B19306_01/java.102/b14188/datamap.htm
		jdbc2JavaTypeMap.put("LONG", STRING);
		jdbc2JavaTypeMap.put("VARCHAR2", STRING);
		jdbc2JavaTypeMap.put("NVARCHAR2", STRING);
		jdbc2JavaTypeMap.put("NUMBER", "BigDecimal"); // oracle todo
		jdbc2JavaTypeMap.put("RAW", "byte[]");

		jdbc2JavaTypeMap.put("INTERVALYM", STRING); // 11g
		jdbc2JavaTypeMap.put("INTERVALDS", STRING); // 11g
		jdbc2JavaTypeMap.put("INTERVAL YEAR TO MONTH", STRING); // just Prevention
		jdbc2JavaTypeMap.put("INTERVAL DAY TO SECOND", STRING);// just Prevention
//		jdbcTypeMap.put("TIMESTAMP", "Timestamp");   exist in comm

		jdbc2JavaTypeMap.put("DATE", "Timestamp");
		jdbc2JavaTypeMap.put("BINARY_DOUBLE", "oracle.sql.BINARY_DOUBLE");
		jdbc2JavaTypeMap.put("BINARY_FLOAT", "oracle.sql.BINARY_FLOAT");
		
		return jdbc2JavaTypeMap;
	}

	private static Map<String, String> forMySQL() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();
		jdbc2JavaTypeMap.put("MEDIUMINT", "Integer");
//		jdbcTypeMap.put("DATETIME", "Date");
		jdbc2JavaTypeMap.put("DATETIME", "Timestamp");//fix on 2019-01-19
		jdbc2JavaTypeMap.put("TINYBLOB", "Blob");
		jdbc2JavaTypeMap.put("MEDIUMBLOB", "Blob");
		jdbc2JavaTypeMap.put("LONGBLOB", "Blob");
		jdbc2JavaTypeMap.put("YEAR", "Integer"); //todo 
		
		jdbc2JavaTypeMap.put("TINYINT", "Byte");
		jdbc2JavaTypeMap.put("SMALLINT", "Short");
		jdbc2JavaTypeMap.put("TINYINT UNSIGNED", "Short");
		jdbc2JavaTypeMap.put("SMALLINT UNSIGNED", "Integer");

		jdbc2JavaTypeMap.put("INT UNSIGNED", "Long");
		jdbc2JavaTypeMap.put("BIGINT UNSIGNED", "BigInteger");
		
		return jdbc2JavaTypeMap;
	}
	
	private static Map<String, String> forSQLSERVER() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();
		
//		jdbcTypeMap.put("SMALLINT", "Short");  //comm
		jdbc2JavaTypeMap.put("TINYINT", "Short");
//		jdbcTypeMap.put("TIME","java.sql.Time");  exist in comm
//		 DATETIMEOFFSET // SQL Server 2008  microsoft.sql.DateTimeOffset
		jdbc2JavaTypeMap.put("DATETIMEOFFSET", "microsoft.sql.DateTimeOffset");
		jdbc2JavaTypeMap.put("microsoft.sql.Types.DATETIMEOFFSET", "microsoft.sql.DateTimeOffset");
		
		jdbc2JavaTypeMap.put("datetime","Timestamp");
		jdbc2JavaTypeMap.put("money","BigDecimal");
		jdbc2JavaTypeMap.put("smallmoney","BigDecimal");
		
		jdbc2JavaTypeMap.put("ntext",STRING);
		jdbc2JavaTypeMap.put("text",STRING);
		jdbc2JavaTypeMap.put("xml",STRING);
		
		jdbc2JavaTypeMap.put("smalldatetime","Timestamp");
		jdbc2JavaTypeMap.put("uniqueidentifier",STRING);
		
		jdbc2JavaTypeMap.put("hierarchyid","byte[]");
		jdbc2JavaTypeMap.put("image","byte[]");
		
		return jdbc2JavaTypeMap;
	}
	
	
	private static Map<String, String> _forH2AndSQLiteCommPart() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();
		
		
		jdbc2JavaTypeMap.put("MEDIUMINT", "Integer");
		jdbc2JavaTypeMap.put("INT4", "Integer");
		jdbc2JavaTypeMap.put("INT2", "Short");
		jdbc2JavaTypeMap.put("INT8", "Long");
		
		jdbc2JavaTypeMap.put("NUMBER", "BigDecimal");
		jdbc2JavaTypeMap.put("NUMERIC", "BigDecimal");

		jdbc2JavaTypeMap.put("BOOLEAN", "Boolean");
		jdbc2JavaTypeMap.put("BOOL", "Boolean");
		jdbc2JavaTypeMap.put("BIT", "Boolean");

		jdbc2JavaTypeMap.put("FLOAT8", "Double");
		jdbc2JavaTypeMap.put("FLOAT4 ", "Float");

		jdbc2JavaTypeMap.put("CHARACTER", STRING);
		jdbc2JavaTypeMap.put("VARCHAR2", STRING);
		jdbc2JavaTypeMap.put("NVARCHAR2", STRING);
		jdbc2JavaTypeMap.put("VARCHAR_IGNORECASE", STRING);
		
		return jdbc2JavaTypeMap;
	}

	private static Map<String, String> forH2() {
		Map<String, String> jdbc2JavaTypeMap = getCommon(); //part1
		
		jdbc2JavaTypeMap.putAll(_forH2AndSQLiteCommPart()); //part2
		
		//	/h2/docs/html/datatypes.html#real_type
		jdbc2JavaTypeMap.put("SIGNED", "Integer");
		jdbc2JavaTypeMap.put("DEC", "BigDecimal");
		jdbc2JavaTypeMap.put("YEAR", "Byte");
		jdbc2JavaTypeMap.put("BINARY VARYING", "byte[]");
		jdbc2JavaTypeMap.put("WITHOUT TIME ZONE", "Time");
		
		jdbc2JavaTypeMap.put("BINARY LARGE OBJECT","Blob");     //java.sql.Blob
		jdbc2JavaTypeMap.put("CHARACTER LARGE OBJECT","Clob");  //java.sql.Clob
		
		jdbc2JavaTypeMap.put("CHARACTER VARYING",STRING); 
		jdbc2JavaTypeMap.put("VARCHAR_CASESENSITIVE",STRING); 
		jdbc2JavaTypeMap.put("VARCHAR_IGNORECASE",STRING); 
		
		//if you want to change, can set in jdbcTypeToFieldType-H2.properties
		jdbc2JavaTypeMap.put("IDENTITY", "Long");
		jdbc2JavaTypeMap.put("UUID", "java.util.UUID");
//		jdbc2JavaTypeMap.put("TIME", "Time");  //in common
		jdbc2JavaTypeMap.put("OTHER", "Object");
		jdbc2JavaTypeMap.put("ENUM", "Integer");
		jdbc2JavaTypeMap.put("ARRAY", "Object[]");
		jdbc2JavaTypeMap.put("GEOMETRY", STRING);
		jdbc2JavaTypeMap.put("POINT", STRING);
		jdbc2JavaTypeMap.put("LINESTRING", STRING);
		jdbc2JavaTypeMap.put("POLYGON", STRING);
		jdbc2JavaTypeMap.put("MULTIPOINT", STRING);
		jdbc2JavaTypeMap.put("MULTILINESTRING", STRING);
		jdbc2JavaTypeMap.put("MULTIPOLYGON", STRING);
		jdbc2JavaTypeMap.put("GEOMETRYCOLLECTION", STRING);
//				INTERVAL\ YEAR=org.h2.api.Interval
//				INTERVAL\ MONTH=org.h2.api.Interval
//				INTERVAL\ DAY=org.h2.api.Interval
//				INTERVAL\ HOUR=org.h2.api.Interval
//				INTERVAL\ MINUTE=org.h2.api.Interval
//				INTERVAL\ SECOND=org.h2.api.Interval
//				INTERVAL\ YEAR\ TO\ MONTH=org.h2.api.Interval
//				INTERVAL\ DAY\ TO\ HOUR=org.h2.api.Interval
//				INTERVAL\ DAY\ TO\ MINUTE=org.h2.api.Interval
//				INTERVAL\ DAY\ TO\ SECOND=org.h2.api.Interval
//				INTERVAL\ HOUR\ TO\ MINUTE=org.h2.api.Interval
//				INTERVAL\ HOUR\ TO\ SECOND=org.h2.api.Interval
//				INTERVAL\ MINUTE\ TO\ SECOND=org.h2.api.Interval
		
		return jdbc2JavaTypeMap;
	}
	
	private static Map<String, String> forSQLite() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();//part1
		
		jdbc2JavaTypeMap.putAll(_forH2AndSQLiteCommPart()); //part2
		
		jdbc2JavaTypeMap.put("VARYING CHARACTER", STRING);
		jdbc2JavaTypeMap.put("NATIVE CHARACTER", STRING);
		jdbc2JavaTypeMap.put("TEXT", STRING);
		jdbc2JavaTypeMap.put("DOUBLE PRECISION", "Double");
		
		jdbc2JavaTypeMap.put("DATETIME", STRING);
		jdbc2JavaTypeMap.put("INTEGER", "Long");  // INTEGER  PRIMARY key
		
		jdbc2JavaTypeMap.put("UNSIGNED BIG INT", "Long");
		
		jdbc2JavaTypeMap.put("VARYING", STRING);
		
		jdbc2JavaTypeMap.put("DATE", STRING);
		jdbc2JavaTypeMap.put("TIMESTAMP", STRING);
		
		return jdbc2JavaTypeMap;
	}

	private static Map<String, String> forPostgreSQL() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();

		jdbc2JavaTypeMap.put("bigint","Long");
		jdbc2JavaTypeMap.put("int8","Long");
		jdbc2JavaTypeMap.put("bigserial","Long");
		jdbc2JavaTypeMap.put("serial8","Long");

		jdbc2JavaTypeMap.put("integer","Integer");
		jdbc2JavaTypeMap.put("int","Integer");
		jdbc2JavaTypeMap.put("int4","Integer");
		
		jdbc2JavaTypeMap.put("serial","Integer");
		jdbc2JavaTypeMap.put("serial4","Integer");
		
		jdbc2JavaTypeMap.put("smallint","Short");
		jdbc2JavaTypeMap.put("int2","Short");
		jdbc2JavaTypeMap.put("smallserial","Short");
		jdbc2JavaTypeMap.put("serial2","Short");

		jdbc2JavaTypeMap.put("money", "BigDecimal");
		jdbc2JavaTypeMap.put("numeric", "BigDecimal");
		jdbc2JavaTypeMap.put("decimal", "BigDecimal");
		
		jdbc2JavaTypeMap.put("bit",STRING);
		jdbc2JavaTypeMap.put("bit varying",STRING);
		jdbc2JavaTypeMap.put("varbit",STRING);
		jdbc2JavaTypeMap.put("character",STRING);
		jdbc2JavaTypeMap.put("char",STRING);
		jdbc2JavaTypeMap.put("character varying",STRING);
		jdbc2JavaTypeMap.put("varchar",STRING);
		jdbc2JavaTypeMap.put("text",STRING);
		jdbc2JavaTypeMap.put("bpchar",STRING);//get from JDBC

		jdbc2JavaTypeMap.put("boolean","Boolean");
		jdbc2JavaTypeMap.put("bool","Boolean");
		
		jdbc2JavaTypeMap.put("double precision","Double"); //prevention
		jdbc2JavaTypeMap.put("float8","Double");

		jdbc2JavaTypeMap.put("real","Float");
		jdbc2JavaTypeMap.put("float4","Float");

		jdbc2JavaTypeMap.put("json",STRING);
		jdbc2JavaTypeMap.put("jsonb",STRING);  //TODO

		jdbc2JavaTypeMap.put("bytea","byte[]"); //TODO bytea

		jdbc2JavaTypeMap.put("date","Date");
		jdbc2JavaTypeMap.put("time","Time");
		jdbc2JavaTypeMap.put("timestamp","Timestamp");

		jdbc2JavaTypeMap.put("time without time zone","Time");
		jdbc2JavaTypeMap.put("timetz","Time");
		jdbc2JavaTypeMap.put("timestamp without time zone","Timestamp");
		jdbc2JavaTypeMap.put("timestamptz","Timestamp");
		
		//if want to change, can set in jdbcTypeToFieldType-PostgreSQL.properties
		jdbc2JavaTypeMap.put("uuid","java.util.UUID");
		jdbc2JavaTypeMap.put("UUID","java.util.UUID");
		jdbc2JavaTypeMap.put("xml",STRING);
		jdbc2JavaTypeMap.put("cidr",STRING);
		jdbc2JavaTypeMap.put("inet",STRING);
		jdbc2JavaTypeMap.put("macaddr",STRING);
		jdbc2JavaTypeMap.put("macaddr8",STRING);
		
		return jdbc2JavaTypeMap;
	}

	private static Map<String, String> forCassandra() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();
		
		jdbc2JavaTypeMap.put("ascii", STRING);
		jdbc2JavaTypeMap.put("inet", STRING);
		
		jdbc2JavaTypeMap.put("timeuuid", "java.util.UUID");
		jdbc2JavaTypeMap.put("uuid", "java.util.UUID");
		
		jdbc2JavaTypeMap.put("boolean", "Boolean");
		jdbc2JavaTypeMap.put("varint", "Integer");
		
		jdbc2JavaTypeMap.put("duration", STRING);
		jdbc2JavaTypeMap.put("counter", "Long");
		
//		jdbcTypeMap.put("list", "java.util.List");
//		jdbcTypeMap.put("set", "java.util.Set");
//		jdbcTypeMap.put("map", "java.util.Map");
		
		jdbc2JavaTypeMap.put("list", "List");
		jdbc2JavaTypeMap.put("set", "Set");
		jdbc2JavaTypeMap.put("map", "Map");
		
		return jdbc2JavaTypeMap;
	}
	
	private static Map<String, String> forMongoDB() {
		Map<String, String> jdbc2JavaTypeMap = getCommon();
	
//		jdbc2JavaTypeMap.put("org.bson.types.ObjectId", "org.bson.types.ObjectId");
		jdbc2JavaTypeMap.put("org.bson.types.ObjectId", "String");
		jdbc2JavaTypeMap.put("java.lang.String", "String");
//		jdbc2JavaTypeMap.put("java.util.ArrayList", "java.util.ArrayList");
		jdbc2JavaTypeMap.put("java.util.ArrayList", "List");
		jdbc2JavaTypeMap.put("java.lang.Integer", "Integer");
		jdbc2JavaTypeMap.put("java.lang.Long", "Long");
		jdbc2JavaTypeMap.put("java.lang.Double", "Double");
		jdbc2JavaTypeMap.put("java.lang.Boolean", "Boolean");
		jdbc2JavaTypeMap.put("java.util.Date", "java.util.Date");
		
		jdbc2JavaTypeMap.put("org.bson.types.decimal128", "BigDecimal");
		
		jdbc2JavaTypeMap.put("org.bson.Document", "org.bson.Document");
		
		return jdbc2JavaTypeMap;
	}
	
//	private static Map<String, String> _forH2AndSQLiteCommPart2() {
//		Map<String, String> jdbc2JavaTypeMap = getCommon();
//		
//		
//		jdbc2JavaTypeMap.put("MEDIUMINT", "Integer");
//		jdbc2JavaTypeMap.put("INT4", "Integer");
////		jdbc2JavaTypeMap.put("LONGVARCHAR-part2222", STRING);
//		jdbc2JavaTypeMap.put("LONGVARCHAR", STRING+"2222");
//		
//		return jdbc2JavaTypeMap;
//		
//		
//	}
	
//	public static void main(String[] args) {
//		
////		Map<String, String> jdbc2JavaTypeMap = getCommon();
////		
////		jdbc2JavaTypeMap.putAll(_forH2AndSQLiteCommPart2()); //part2
////		
////		jdbc2JavaTypeMap.put("LONGVARCHAR", STRING+"33333");
////		
////		System.out.println(jdbc2JavaTypeMap);
//		
//	}

}
