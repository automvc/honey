package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.honey.osql.constant.DatabaseConst;
import org.teasoft.honey.osql.constant.NullEmpty;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyUtil {
	
	private static Map<String,String> jdbcTypeMap=new HashMap<String,String>();
	private static Map<String,Integer> javaTypeMap=new HashMap<String,Integer>();
	
	private static PropertiesReader jdbcTypeCustomProp = new PropertiesReader("/jdbcTypeToFieldType.properties");
	private static PropertiesReader jdbcTypeCustomProp_specificalDB =null;
	
	static{
		
		String proFileName="/jdbcTypeToFieldType-{DbName}.properties";
		jdbcTypeCustomProp_specificalDB=new PropertiesReader(proFileName.replace("{DbName}", HoneyConfig.getHoneyConfig().getDbName()));
		
		initJdbcTypeMap();
		appendJdbcTypeCustomProp();
		appendJdbcTypeCustomProp_specificalDB();
		
		initJavaTypeMap();
	}

	public static int[] mergeArray(int total[], int part[], int start, int end) {

		try {
			for (int i = 0; i < part.length; i++) {
				total[start + i] = part[i];
			}
		} catch (Exception e) {
			Logger.print(" HoneyUtil.mergeArray() " + e.getMessage());
		}

		return total;
	}
	
	static String getBeanField(Field fields[]){
	    if(fields==null) return "";
	    StringBuffer s=new StringBuffer();
	    int len=fields.length;
		for (int i = 0; i <len;  i++) {
			if("serialVersionUID".equals(fields[i].getName())) continue;
			 s.append(NameTranslateHandle.toColumnName(fields[i].getName()));
			if(i<len-1) s.append(",");
		}
		return s.toString();
	}
	
/*	static boolean isNumberType(Field field){
		if (
			(field.getType() == Integer.class)|| (field.getType() == Long.class)
		  ||(field.getType() == Short.class) || (field.getType() == Byte.class)
		  ||(field.getType() == Double.class)|| (field.getType() == Float.class)
		  ||(field.getType() == BigInteger.class)||(field.getType() == BigDecimal.class)
		  )  return true;
		else return false;
	}*/
	
	
	
	/**
	 * jdbc type->java type
	 * 将jdbc的数据类型转换为java的类型 
	 * @param jdbcType
	 * @return the string of java type
	 */
	public static String getFieldType(String jdbcType) {

		String javaType = jdbcTypeMap.get(jdbcType);

		if(javaType!=null) return javaType;
		
		if (null == jdbcTypeMap.get(jdbcType)){
			
		    //fix UNSIGNED,  like :TINYINT UNSIGNED 
			String tempType=jdbcType.trim();
			if(tempType.endsWith(" UNSIGNED")){
				int i=tempType.indexOf(" ");
				javaType=jdbcTypeMap.get(tempType.substring(0,i));
				if(javaType!=null) return javaType;
			}
			javaType = "[UNKNOWN TYPE]" + jdbcType;
		}

		return javaType;
	}
    
	private static void initJdbcTypeMap() {

		//url: https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
//		https://docs.oracle.com/javadb/10.8.3.0/ref/rrefjdbc20377.html
//		https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html
		jdbcTypeMap.put("CHAR", "String");
		jdbcTypeMap.put("VARCHAR", "String");
		jdbcTypeMap.put("LONGVARCHAR", "String");
		
		jdbcTypeMap.put("NVARCHAR", "String");
		jdbcTypeMap.put("NCHAR", "String");
		
		jdbcTypeMap.put("NUMERIC", "BigDecimal");
		jdbcTypeMap.put("DECIMAL", "BigDecimal");

		jdbcTypeMap.put("BIT", "Boolean");

		//rs.getObject(int index)  bug   
		//pst.setByte(i+1,(Byte)value); break;设置查询没问题,结果也能返回,用rs.getObject拿结果时才报错
		jdbcTypeMap.put("TINYINT", "Byte");
		jdbcTypeMap.put("SMALLINT", "Short");
//		jdbcTypeMap.put("TINYINT", "Integer");
//		jdbcTypeMap.put("SMALLINT", "Integer");

		jdbcTypeMap.put("INT", "Integer");
		jdbcTypeMap.put("INTEGER", "Integer");

		jdbcTypeMap.put("BIGINT", "Long");
		jdbcTypeMap.put("REAL", "Float");
//		jdbcTypeMap.put("FLOAT", "Double");
		jdbcTypeMap.put("FLOAT", "Float");
		jdbcTypeMap.put("DOUBLE", "Double");

		jdbcTypeMap.put("BINARY", "byte[]");
		jdbcTypeMap.put("VARBINARY", "byte[]");
		jdbcTypeMap.put("LONGVARBINARY", "byte[]");

		jdbcTypeMap.put("DATE", "Date");
		jdbcTypeMap.put("TIME", "Time");
		jdbcTypeMap.put("TIMESTAMP", "Timestamp");

		jdbcTypeMap.put("CLOB", "Clob");
		jdbcTypeMap.put("BLOB", "Blob");
		jdbcTypeMap.put("ARRAY", "Array");
		
		jdbcTypeMap.put("NCLOB", "java.sql.NClob");//JDK6
		jdbcTypeMap.put("ROWID","java.sql.RowId"); //JDK6
		jdbcTypeMap.put("SQLXML","java.sql.SQLXML"); //JDK6
		
//		
		// JDBC 4.2 JDK8
		jdbcTypeMap.put("TIMESTAMP_WITH_TIMEZONE", "Timestamp");
		jdbcTypeMap.put("TIMESTAMP WITH TIME ZONE", "Timestamp"); //test in oralce 11g
		jdbcTypeMap.put("TIMESTAMP WITH LOCAL TIME ZONE", "Timestamp");//test in oralce 11g
						
		String dbName = HoneyConfig.getHoneyConfig().getDbName();

		if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName) 
		 ||DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
			jdbcTypeMap.put("MEDIUMINT", "Integer");
//			jdbcTypeMap.put("DATETIME", "Date");
			jdbcTypeMap.put("DATETIME", "Timestamp");//fix on 2019-01-19
			jdbcTypeMap.put("TINYBLOB", "Blob");
			jdbcTypeMap.put("MEDIUMBLOB", "Blob");
			jdbcTypeMap.put("LONGBLOB", "Blob");
			jdbcTypeMap.put("YEAR", "Integer");  //TODO 
			
			jdbcTypeMap.put("INT UNSIGNED", "Long");
			jdbcTypeMap.put("BIGINT UNSIGNED", "BigInteger");
		} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
//			https://docs.oracle.com/cd/B12037_01/java.101/b10983/datamap.htm
//			https://docs.oracle.com/cd/B19306_01/java.102/b14188/datamap.htm
			jdbcTypeMap.put("LONG","String");
			jdbcTypeMap.put("VARCHAR2","String");
			jdbcTypeMap.put("NVARCHAR2","String");
			jdbcTypeMap.put("NUMBER", "BigDecimal"); //oracle TODO
			jdbcTypeMap.put("RAW", "byte[]");

			jdbcTypeMap.put("INTERVALYM","String"); //11g 
			jdbcTypeMap.put("INTERVALDS","String"); //11g
			jdbcTypeMap.put("INTERVAL YEAR TO MONTH","String"); //just Prevention
			jdbcTypeMap.put("INTERVAL DAY TO SECOND","String");//just Prevention
//			jdbcTypeMap.put("TIMESTAMP", "Timestamp");   exist in comm
			
		} else if (DatabaseConst.SQLSERVER.equalsIgnoreCase(dbName)) {
			jdbcTypeMap.put("SMALLINT", "Short");
			jdbcTypeMap.put("TINYINT", "Short");
//			jdbcTypeMap.put("TIME","java.sql.Time");  exist in comm
//			 DATETIMEOFFSET // SQL Server 2008  microsoft.sql.DateTimeOffset
			jdbcTypeMap.put("DATETIMEOFFSET","microsoft.sql.DateTimeOffset");
		}

	}

	private static void appendJdbcTypeCustomProp() {
		for (String s : jdbcTypeCustomProp.getKeys()) {
			jdbcTypeMap.put(s, jdbcTypeCustomProp.getValue(s));
		}
	}
	
	private static void appendJdbcTypeCustomProp_specificalDB() {
//		System.out.println(jdbcTypeCustomProp_specificalDB.getKeys());
		for (String s : jdbcTypeCustomProp_specificalDB.getKeys()) {
			jdbcTypeMap.put(s, jdbcTypeCustomProp_specificalDB.getValue(s));
		}
	}
	
	private static void initJavaTypeMap() {

		javaTypeMap.put("java.lang.String", 1);
		javaTypeMap.put("java.lang.Integer", 2);
		javaTypeMap.put("java.lang.Long", 3);
		javaTypeMap.put("java.lang.Double", 4);
		javaTypeMap.put("java.lang.Float", 5);
		javaTypeMap.put("java.lang.Short", 6);
		javaTypeMap.put("java.lang.Byte", 7);
//		javaTypeMap.put("[Ljava.lang.Byte;", 8); //  Byte[]
		javaTypeMap.put("[B", 8); //byte[]  
		javaTypeMap.put("java.lang.Boolean", 9);

		javaTypeMap.put("java.math.BigDecimal", 10);

		javaTypeMap.put("java.sql.Date", 11);
		javaTypeMap.put("java.sql.Time", 12);
		javaTypeMap.put("java.sql.Timestamp", 13);
		javaTypeMap.put("java.sql.Blob", 14);
		javaTypeMap.put("java.sql.Clob", 15);
		
		javaTypeMap.put("java.sql.NClob", 16);
		javaTypeMap.put("java.sql.RowId", 17);
		javaTypeMap.put("java.sql.SQLXML", 18);
		
		javaTypeMap.put("java.math.BigInteger", 19);
		
	}
	
    public static int getJavaTypeIndex(String javaType){
//    	return javaTypeMap.get(javaTypeMap)==null?-1:javaTypeMap.get(javaTypeMap);
    	return javaTypeMap.get(javaType)==null?-1:javaTypeMap.get(javaType);
    }
    
	
	/*
	 * 首字母转换成大写
	 */
	public static  String firstLetterToUpperCase(String str) {
         return NameUtil.firstLetterToUpperCase(str);
	}
	
	static boolean isContinue(int includeType,Object object,String fieldName){
		return (
				( (includeType==NullEmpty.EXCLUDE || includeType==NullEmpty.EMPTY_STRING ) && object == null)
				|| ( (includeType==NullEmpty.EXCLUDE ||includeType==NullEmpty.NULL) && "".equals(object) ) //TODO "  "也要排除
				|| "serialVersionUID".equals(fieldName)  
				) ;
	}
	
	/**
	 * 
	 * @param pst PreparedStatement
	 * @param objTypeIndex
	 * @param i  prarmeter index
	 * @param value
	 * @throws SQLException
	 */
	static void setPreparedValues(PreparedStatement pst,int objTypeIndex,int i,Object value) throws SQLException{
		 
		if(null==value) {
			setPreparedNull(pst,objTypeIndex,i);  
			return ;
		}
		
		switch(objTypeIndex){
	        case 1:
	        	pst.setString(i+1, (String)value); break;
	        case 2:	
	        	pst.setInt(i+1,(Integer)value); break;
	        case 3:	
	        	pst.setLong(i+1,(Long)value); break;	   	
	        case 4:	
	        	pst.setDouble(i+1,(Double)value); break;
	        case 5:	
	        	pst.setFloat(i+1,(Float)value); break;
	        case 6:	
	        	pst.setShort(i+1,(Byte)value); break;
	        case 7:
	        	pst.setByte(i+1,(Byte)value); break;
	        case 8:
	        	pst.setBytes(i+1,(byte[])value); break;
	        case 9:
	        	pst.setBoolean(i+1,(Boolean)value); break;
	        case 10:	
	        	pst.setBigDecimal(i+1,(BigDecimal)value); break;
	        case 11:
	        	pst.setDate(i+1,(Date)value); break;
	        case 12:
	        	pst.setTime(i+1,(Time)value); break;
	        case 13:
	        	pst.setTimestamp(i+1,(Timestamp)value); break;
	        case 14: 	
	        	pst.setBlob(i+1,(Blob)value); break;
	        case 15: 
	        	pst.setClob(i+1,(Clob)value); break;
	        case 16: 
	        	pst.setNClob(i+1, (NClob)value);break;
	        case 17: 
	        	pst.setRowId(i+1, (RowId)value);break;	
	        case 18: 
	        	pst.setSQLXML(i+1, (SQLXML)value);break;	
	        case 19: 
//	        	pst.setBigInteger(i+1, (BigInteger)value);break;
	        default:
	        	pst.setObject(i+1,value);
		} //end switch
	}
	
	
	 static Object getResultObject(ResultSet rs, String typeName,String columnName) throws SQLException{
		
		int k = HoneyUtil.getJavaTypeIndex(typeName);
		
		switch(k){
	        case 1:
	        	return rs.getString(columnName); 
	        case 2:	
	        	return rs.getInt(columnName);
	        case 3:	
	        	return rs.getLong(columnName);
	        case 4:	
	        	return rs.getDouble(columnName);
	        case 5:	
	        	return rs.getFloat(columnName);
	        case 6:	
	        	return rs.getShort(columnName);
	        case 7:
	        	return rs.getByte(columnName);
	        case 8:
	        	return rs.getBytes(columnName);
	        case 9:
	        	return rs.getBoolean(columnName);
	        case 10:	
	        	return rs.getBigDecimal(columnName);
	        case 11:
	        	return rs.getDate(columnName);
	        case 12:
	        	return rs.getTime(columnName);
	        case 13:
	        	return rs.getTimestamp(columnName); 
	        case 14: 	
	        	return rs.getBlob(columnName); 
	        case 15: 
	        	return rs.getClob(columnName); 
	        case 16: 
	        	return rs.getNClob(columnName);
	        case 17: 
	        	return rs.getRowId(columnName);
	        case 18: 
	        	return rs.getSQLXML(columnName);
	        case 19: 
//	        	no  getBigInteger
	        default:
	        	return rs.getObject(columnName);
		} //end switch
		
	}
	 
	 static Object getResultObjectByIndex(ResultSet rs, String typeName,int index) throws SQLException{
			
		int k = HoneyUtil.getJavaTypeIndex(typeName);
		
		switch(k){
	        case 1:
	        	return rs.getString(index); 
	        case 2:	
	        	return rs.getInt(index);
	        case 3:	
	        	return rs.getLong(index);
	        case 4:	
	        	return rs.getDouble(index);
	        case 5:	
	        	return rs.getFloat(index);
	        case 6:	
	        	return rs.getShort(index);
	        case 7:
	        	return rs.getByte(index);
	        case 8:
	        	return rs.getBytes(index);
	        case 9:
	        	return rs.getBoolean(index);
	        case 10:	
	        	return rs.getBigDecimal(index);
	        case 11:
	        	return rs.getDate(index);
	        case 12:
	        	return rs.getTime(index);
	        case 13:
	        	return rs.getTimestamp(index); 
	        case 14: 	
	        	return rs.getBlob(index); 
	        case 15: 
	        	return rs.getClob(index); 
	        case 16: 
	        	return rs.getNClob(index);
	        case 17: 
	        	return rs.getRowId(index);
	        case 18: 
	        	return rs.getSQLXML(index);
	        case 19: 
//	        	no  getBigInteger
	        default:
	        	return rs.getObject(index);
		} //end switch
		
	}
	
	
	public static void setPreparedNull(PreparedStatement pst,int objTypeIndex,int i) throws SQLException{
		
		pst.setNull(i+1,Types.NULL);
	}
	
	public static String genSerializableNum(){
		String s=Math.random()+"";
		int end=s.length()>12?12:s.length();
		return "159"+s.substring(2,end)+"L";
	}

	public static String deleteLastSemicolon(String sql){
		String new_sql=sql.trim();
		if(new_sql.endsWith(";"))
			return new_sql.substring(0, new_sql.length()-1); //fix oracle ORA-00911 bug.oracle用jdbc不能有分号
		return sql;
	}
	
	public static <T> void checkPackage(T entity){
		if(entity==null) return;
		String packageName=entity.getClass().getPackage().getName();
//		传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但spring开头不能过滤,否则spring想用bee就不行了.
		if(packageName.startsWith("java.") || packageName.startsWith("javax.")){
			throw new BeeIllegalEntityException("BeeIllegalEntityException: Illegal Entity, "+entity.getClass().getName());
		}
	}
}
