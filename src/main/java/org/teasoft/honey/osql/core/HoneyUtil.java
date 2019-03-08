package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.teasoft.honey.osql.constant.DatabaseConst;
import org.teasoft.honey.osql.constant.NullEmpty;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyUtil {
	
	private static Map<String,String> jdbcTypeMap=new HashMap<String,String>();
	private static Map<String,Integer> javaTypeMap=new HashMap<String,Integer>();
	
	private static PropertiesReader jdbcTypeCustomProp = new PropertiesReader("/jdbcTypeToFieldType.properties");
	
	static{
		initJdbcTypeMap();
		appendJdbcTypeCustomProp();
		
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
//			s.append(fields[i].getName());
			if(HoneyConfig.getHoneyConfig().isUnderScoreAndCamelTransform()){
			   s.append(HoneyUtil.toUnderscoreNaming(fields[i].getName()));
			}else{
			   s.append(fields[i].getName());
			}
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

		String javaType = "";

		if (null == jdbcTypeMap.get(jdbcType))
			javaType = "[UNKNOWN TYPE]" + jdbcType;
		else
			javaType = jdbcTypeMap.get(jdbcType);

		return javaType;
	}
    
	private static void initJdbcTypeMap() {

		//url: https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html

		jdbcTypeMap.put("CHAR", "String");
		jdbcTypeMap.put("VARCHAR", "String");
		jdbcTypeMap.put("LONGVARCHAR", "String");

		jdbcTypeMap.put("NUMERIC", "BigDecimal");
		jdbcTypeMap.put("DECIMAL", "BigDecimal");

		jdbcTypeMap.put("BIT", "Boolean");

		jdbcTypeMap.put("TINYINT", "Byte");
		jdbcTypeMap.put("SMALLINT", "Short");

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

		String dbName = HoneyConfig.getHoneyConfig().getDbName();

		if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName) 
		 ||DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
			jdbcTypeMap.put("MEDIUMINT", "Integer");
//			jdbcTypeMap.put("DATETIME", "Date");
			jdbcTypeMap.put("DATETIME", "Timestamp");//fix on 2019-01-19
			jdbcTypeMap.put("TINYBLOB", "Blob");
			jdbcTypeMap.put("MEDIUMBLOB", "Blob");
			jdbcTypeMap.put("LONGBLOB", "Blob");
			jdbcTypeMap.put("YEAR", "Integer");
		} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
			//TODO
		} else if (DatabaseConst.SQLSERVER.equalsIgnoreCase(dbName)) {
			//TODO
		}

	}

	private static void appendJdbcTypeCustomProp() {
		for (String s : jdbcTypeCustomProp.getKeys()) {
			jdbcTypeMap.put(s, jdbcTypeCustomProp.getValue(s));
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
	}
	
    public static int getJavaTypeIndex(String javaType){
//    	return javaTypeMap.get(javaTypeMap)==null?-1:javaTypeMap.get(javaTypeMap);
    	return javaTypeMap.get(javaType)==null?-1:javaTypeMap.get(javaType);
    }
    
	/**
	 * @param name
	 * @return UnderscoreNaming String
	 * @eg bee_name->beeName,bee_t_name->beeTName
	 */
	public static String toUnderscoreNaming(String name) {
		StringBuffer buf = new StringBuffer(name);
		for (int i = 1; i < buf.length() - 1; i++) {
			if (Character.isUpperCase(buf.charAt(i))) {
				buf.insert(i++, '_');
			}
		}
		return buf.toString().toLowerCase();
	}
	
	/**
	 * @param name
	 * @return
	 * @eg  beeName->bee_name,beeTName->bee_t_name
	 */
	public static String toCamelNaming(String name){
//		StringBuffer buf = new StringBuffer(name.toLowerCase()); //HELLO_WORLD->HelloWorld 字段名有可能是全大写的
		StringBuffer buf = new StringBuffer(name.trim());
		char temp;
		for (int i = 1; i < buf.length() - 1; i++) {
			temp=buf.charAt(i);
			if (buf.charAt(i)=='_') {
				buf.deleteCharAt(i);
				temp=buf.charAt(i);
				if(temp>='a' && temp<='z')
				    buf.setCharAt(i, (char)(temp-32));
			}
		}
		return buf.toString();
	}
	
	/*
	 * 首字母转换成大写
	 */
	public static  String firstLetterToUpperCase(String str) {
		String result = "";
		if (str.length() > 1) {
			result = str.substring(0, 1).toUpperCase()+ str.substring(1);
		} else {
			result = str.toUpperCase();
		}

		return result;
	}
	
    //从列名转成java命名规范
	public static String transformColumn(String column) {  
		if (HoneyConfig.getHoneyConfig().isUnderScoreAndCamelTransform()) {
			return HoneyUtil.toCamelNaming(column);
		} else {
			return column;
		}
	}
	
    //转成带下画线的
	public static String transformStr(String str) {  
		if (HoneyConfig.getHoneyConfig().isUnderScoreAndCamelTransform()) {
			return HoneyUtil.toUnderscoreNaming(str);
		} else {
			return str;
		}
	}
	
	public static boolean isContinue(int includeType,Object object,String fieldName){
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
	public static void setPreparedValues(PreparedStatement pst,int objTypeIndex,int i,Object value) throws SQLException{
		 
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
//	        	pst.setBytes(i+1,column31); break;
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
	        default:	
	        	pst.setObject(i+1,value);
		} //end switch
	}
	
	public static void setPreparedNull(PreparedStatement pst,int objTypeIndex,int i) throws SQLException{
		
		pst.setNull(i+1,Types.NULL);
	}
	
	public static String genSerializableNum(){
		return "159"+(Math.random()+"").substring(5)+"L";
	}
	
	public static String deleteLastSemicolon(String sql){
		sql=sql.trim();
		if(sql.endsWith(";"))
			sql=sql.substring(0, sql.length()-1);
		return sql;
	}
}
