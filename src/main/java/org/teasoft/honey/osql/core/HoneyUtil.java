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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.annotation.Ignore;
import org.teasoft.bee.osql.annotation.JoinTable;
import org.teasoft.bee.osql.annotation.JoinType;
import org.teasoft.bee.osql.exception.BeeErrorFieldException;
import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.bee.osql.exception.JoinTableException;
import org.teasoft.bee.osql.exception.JoinTableParameterException;
import org.teasoft.honey.osql.constant.NullEmpty;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.util.PropertiesReader;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyUtil {

	private static Map<String, String> jdbcTypeMap = new HashMap<String, String>();
	private static Map<String, Integer> javaTypeMap = new HashMap<String, Integer>();

	private static PropertiesReader jdbcTypeCustomProp = new PropertiesReader("/jdbcTypeToFieldType.properties");
	private static PropertiesReader jdbcTypeCustomProp_specificalDB = null;

	static {
		String proFileName = "/jdbcTypeToFieldType-{DbName}.properties";
		
		initJdbcTypeMap();
		appendJdbcTypeCustomProp();
		
		String dbName = HoneyConfig.getHoneyConfig().getDbName();
		if (dbName != null) {
			jdbcTypeCustomProp_specificalDB = new PropertiesReader(proFileName.replace("{DbName}", dbName));
			appendJdbcTypeCustomProp_specificalDB();
		}

		initJavaTypeMap();
	}

	public static int[] mergeArray(int total[], int part[], int start, int end) {

		try {
			for (int i = 0; i < part.length; i++) {
				total[start + i] = part[i];
			}
		} catch (Exception e) {
			Logger.error(" HoneyUtil.mergeArray() " + e.getMessage());
		}

		return total;
	}

	static String getBeanField(Field field[]) {
		if (field == null) return "";
		StringBuffer s = new StringBuffer();
		int len = field.length;
		boolean isFirst = true;

		for (int i = 0; i < len; i++) {
//			if ("serialVersionUID".equals(field[i].getName()) || field[i].isSynthetic()) continue;
//			if (field[i] != null && field[i].isAnnotationPresent(JoinTable.class)) continue;
			if(isSkipField(field[i])) continue;
			if (isFirst) {
				isFirst = false;
			} else {
				s.append(",");
			}

			s.append(NameTranslateHandle.toColumnName(field[i].getName()));
			//			if(i<len-1) s.append(",");
		}
		return s.toString();
	}

	static <T> MoreTableStruct[] getMoreTableStructAndCheckBefore(T entity) {
		String packageAndClassName = entity.getClass().getName();
		String key = "ForMoreTable:" + packageAndClassName; //ForMoreTable
		MoreTableStruct moreTableStruct[] = HoneyContext.getMoreTableStructs(key);
		if (moreTableStruct == null) {
			moreTableStruct = _getMoreTableStructAndCheckBefore(entity);
			HoneyContext.addMoreTableStructs(key, moreTableStruct);
		}

		return moreTableStruct;
	}

	private static <T> MoreTableStruct[] _getMoreTableStructAndCheckBefore(T entity) {

		if (entity == null) return null;

		String entityFullName = entity.getClass().getName();
		
		Field field[] = entity.getClass().getDeclaredFields();

		MoreTableStruct moreTableStruct[] = new MoreTableStruct[3];
		moreTableStruct[0] = new MoreTableStruct();
		Field subField[] = new Field[2];
		int subEntityFieldNum = 0;
		
		Set<String> mainFieldSet =new HashSet<>();
		Map<String,String> dulMap=new HashMap<>();

		String tableName = _toTableName(entity);
		StringBuffer columns = new StringBuffer();
		int len = field.length;
		boolean isFirst = true;
		
		String mailField="";//v1.8
		for (int i = 0; i < len; i++) {
//			if ("serialVersionUID".equals(field[i].getName()) || field[i].isSynthetic()) continue;
			if(isSkipFieldForMoreTable(field[i])) continue; //有Ignore注释,将不再处理JoinTable
			if (field[i] != null && field[i].isAnnotationPresent(JoinTable.class)) {
				//s.append(",");
				//s.append(_getBeanFullField_0(field[i]));
				subEntityFieldNum++;
				if (subEntityFieldNum == 1) subField[0] = field[i];
				if (subEntityFieldNum == 2) subField[1] = field[i];
				continue;
			}
			if (isFirst) {
				isFirst = false;
			} else {
				columns.append(",");
			}
			columns.append(tableName);
			columns.append(".");
			
			mailField=NameTranslateHandle.toColumnName(field[i].getName());
			columns.append(mailField);  
			
			mainFieldSet.add(mailField);  //v1.8
		}// main table for end

		if (subEntityFieldNum > 2) { //只支持一个实体里最多关联两个实体
			throw new JoinTableException("One entity only supports two JoinTable at most! " + entityFullName + " has " + subEntityFieldNum + " JoinTable now !");
		}

		JoinTable joinTable[] = new JoinTable[2];
		String subTableName[] = new String[2];
		
		boolean hasOtherJoin=false; 

		if (subField[0] != null) {
			joinTable[0] = subField[0].getAnnotation(JoinTable.class);

			String errorMsg = checkJoinTable(joinTable[0]);
			if (!"".equals(errorMsg)) {
				throw new JoinTableParameterException("Error: mainField and subField can not just use only one." + errorMsg);
			}
			if(joinTable[0].joinType()!=JoinType.JOIN) hasOtherJoin=true;
		}

		if (subField[1] != null) {
			joinTable[1] = subField[1].getAnnotation(JoinTable.class);

			String errorMsg = checkJoinTable(joinTable[1]);
			if (!"".equals(errorMsg)) {
				throw new JoinTableParameterException("Error: mainField and subField can not just use only one." + errorMsg);
			}
			if(joinTable[1].joinType()!=JoinType.JOIN) hasOtherJoin=true;
		}
		
		if(hasOtherJoin && subEntityFieldNum==2)
		       throw new JoinTableException("Just support JoinType.JOIN in this version when a entity has two JoinTable annotation fields!");
		
		//if no exception , set for main table
		moreTableStruct[0].tableName = tableName;
		moreTableStruct[0].entityFullName = entityFullName;
		moreTableStruct[0].entityName = entity.getClass().getSimpleName();
		moreTableStruct[0].joinTableNum=subEntityFieldNum;  //一个实体包含关联的子表数
		//moreTableStruct[0].columnsFull = columns.toString();  //还要子表列

		//set for subTable1 and subTable2
		//for (int j = 0; j < subField.length; j++) {
		for (int j = 0; j < 2; j++) { // 2 subTables
			if (subField[j] != null) {

				String mainColumn = _toColumnName(joinTable[j].mainField());
				String subColumn = _toColumnName(joinTable[j].subField());
//				subTableName[j] = _toTableNameByEntityName(subField[j].getType().getSimpleName());
				subTableName[j] = _toTableNameByEntityName(subField[j].getType().getName());  //从表可能有注解,要用包名去检查

				moreTableStruct[1 + j] = new MoreTableStruct();
				//从表的  
				moreTableStruct[1 + j].subEntityField = subField[j];
				moreTableStruct[1 + j].tableName = subTableName[j]; //各实体对应的表名
				moreTableStruct[1 + j].entityFullName = subField[j].getType().getName();
				moreTableStruct[1 + j].entityName = subField[j].getType().getSimpleName();

				moreTableStruct[1 + j].mainField = joinTable[j].mainField();
				moreTableStruct[1 + j].subField = joinTable[j].subField();
				moreTableStruct[1 + j].joinType = joinTable[j].joinType();
				String t_subAlias = joinTable[j].subAlias();
				String useSubTableName;
				if (t_subAlias != null && !"".equals(t_subAlias)) {
					moreTableStruct[1 + j].subAlias = t_subAlias;
					useSubTableName = t_subAlias;
					moreTableStruct[1 + j].hasSubAlias = true;
				} else {
					useSubTableName = subTableName[j];
				}
				if(!"".equals(mainColumn) && !"".equals(subColumn)){
//				   moreTableStruct[1 + j].joinExpression = tableName + "." + mainColumn + "=" + useSubTableName + "." + subColumn;
				   //v1.9
					String mainColumnArray[]=mainColumn.split(",");
					String subColumnArray[]=subColumn.split(",");
					if(mainColumnArray.length!=subColumnArray.length) {
						throw new JoinTableException("The number of field in mainField & subField is different , mainField is: "+mainColumnArray.length+" ,subField is : "+subColumnArray.length);
					}
					moreTableStruct[1 + j].joinExpression="";
					for (int i = 0; i < mainColumnArray.length; i++) {
						if(i!=0) moreTableStruct[1 + j].joinExpression += K.space+K.and+K.space;
						moreTableStruct[1 + j].joinExpression +=tableName + "." + mainColumnArray[i] + "=" + useSubTableName + "." + subColumnArray[i];
					}
					
				}
				moreTableStruct[1 + j].useSubTableName = useSubTableName;
				try {
					subField[j].setAccessible(true);
					moreTableStruct[1 + j].subObject = subField[j].get(entity);
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				}

				StringBuffer subColumns = _getBeanFullField_0(subField[j], useSubTableName,entityFullName,mainFieldSet,dulMap);
				moreTableStruct[1 + j].columnsFull = subColumns.toString(); 

				columns.append(",");
				columns.append(subColumns);
			}
		}//end subFieldEntity for

		moreTableStruct[0].columnsFull = columns.toString(); //包含子表的列
		
		moreTableStruct[0].subDulFieldMap=dulMap;

		//		return columns.toString();
		return moreTableStruct;
	}

	//for moreTable
	static StringBuffer _getBeanFullField_0(Field entityField, String tableName,String entityFullName,Set<String> mainFieldSet,Map<String,String> dulMap) {
//		entityFullName just for tip
		//		    if(entityField==null) return "";
		//		    Field field[] = entity.getClass().getDeclaredFields(); //error

		Field field[] = entityField.getType().getDeclaredFields();

		//		String tableName = _toTableNameByEntityName(entityField.getType().getSimpleName());//有可能用别名
		StringBuffer columns = new StringBuffer();
		int len = field.length;
		boolean isFirst = true;
		String subFieldName="";
		for (int i = 0; i < len; i++) {
//			if ("serialVersionUID".equals(field[i].getName()) || field[i].isSynthetic()) continue;
			if(HoneyUtil.isSkipFieldForMoreTable(field[i])) continue; //有Ignore注释,将不再处理JoinTable
			if (field[i] != null && field[i].isAnnotationPresent(JoinTable.class)) {
//				Logger.error("注解字段的实体: " + entityField.getType().getName() + "里面又包含了注解:" + field[i].getType());
				String entityFieldName=entityField.getType().getName();
				if(!entityFieldName.equals(field[i].getType().getName())){
				   Logger.warn("Annotation JoinTable field: " +entityField.getName()+"(in "+ entityFullName + ") still include JoinTable field:" + field[i].getName() + "(will be ignored)!");
				}
				continue;
			}

			if (isFirst) {
				isFirst = false;
			} else {
				columns.append(",");
			}
			subFieldName=NameTranslateHandle.toColumnName(field[i].getName());
			
			if(!mainFieldSet.add(subFieldName) && isConfuseDuplicateFieldDB()){
				
				if (isSQLite()) {
					dulMap.put(tableName + "." + subFieldName, tableName + "." + subFieldName); 
				} else {
					dulMap.put(tableName + "." + subFieldName, tableName + "_" + subFieldName + "_$"); 
				}
				columns.append(tableName);
				columns.append(".");
				columns.append(subFieldName);
				if (isSQLite()) {
					columns.append("  "+K.as+" '" + tableName + "." + subFieldName+"'");
				} else {
					columns.append("  " + tableName + "_" + subFieldName + "_$");
				}
			}else{
				columns.append(tableName);
				columns.append(".");
				columns.append(subFieldName);
			}
		}
		
		return columns;
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

		if (javaType != null) return javaType;

		if (null == jdbcTypeMap.get(jdbcType)) {

			//fix UNSIGNED,  like :TINYINT UNSIGNED 
			String tempType = jdbcType.trim();
			if (tempType.endsWith(" UNSIGNED")) {
				int i = tempType.indexOf(" ");
				javaType = jdbcTypeMap.get(tempType.substring(0, i));
				if (javaType != null) return javaType;
			}
			
			if (javaType == null){
				javaType =jdbcTypeMap.get(jdbcType.toLowerCase());
				if (javaType != null) return javaType;
				
				if (javaType == null){
					javaType =jdbcTypeMap.get(jdbcType.toUpperCase());
					if (javaType != null) return javaType;
				}
				
				if (javaType == null){
					javaType =jdbcTypeMap.get(jdbcType.toUpperCase());
					if (javaType != null) return javaType;
				}
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

		jdbcTypeMap.put("INT", "Integer");
		jdbcTypeMap.put("INTEGER", "Integer");

		jdbcTypeMap.put("BIGINT", "Long");
		jdbcTypeMap.put("REAL", "Float");
		jdbcTypeMap.put("FLOAT", "Float"); //notice: mysql在创表时,要指定float的小数位数,否则查询时不能用=精确查询
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
		jdbcTypeMap.put("ROWID", "java.sql.RowId"); //JDK6
		jdbcTypeMap.put("SQLXML", "java.sql.SQLXML"); //JDK6

		// JDBC 4.2 JDK8
		jdbcTypeMap.put("TIMESTAMP_WITH_TIMEZONE", "Timestamp");
		jdbcTypeMap.put("TIMESTAMP WITH TIME ZONE", "Timestamp"); //test in oralce 11g
		jdbcTypeMap.put("TIMESTAMP WITH LOCAL TIME ZONE", "Timestamp");//test in oralce 11g

		String dbName = HoneyConfig.getHoneyConfig().getDbName();

		if (DatabaseConst.MYSQL.equalsIgnoreCase(dbName) || DatabaseConst.MariaDB.equalsIgnoreCase(dbName)) {
			jdbcTypeMap.put("MEDIUMINT", "Integer");
//			jdbcTypeMap.put("DATETIME", "Date");
			jdbcTypeMap.put("DATETIME", "Timestamp");//fix on 2019-01-19
			jdbcTypeMap.put("TINYBLOB", "Blob");
			jdbcTypeMap.put("MEDIUMBLOB", "Blob");
			jdbcTypeMap.put("LONGBLOB", "Blob");
			jdbcTypeMap.put("YEAR", "Integer"); //todo 
			
			jdbcTypeMap.put("TINYINT", "Byte");
			jdbcTypeMap.put("SMALLINT", "Short");
			jdbcTypeMap.put("TINYINT UNSIGNED", "Short");
			jdbcTypeMap.put("SMALLINT UNSIGNED", "Integer");

			jdbcTypeMap.put("INT UNSIGNED", "Long");
			jdbcTypeMap.put("BIGINT UNSIGNED", "BigInteger");
		} else if (DatabaseConst.ORACLE.equalsIgnoreCase(dbName)) {
//			https://docs.oracle.com/cd/B12037_01/java.101/b10983/datamap.htm
//			https://docs.oracle.com/cd/B19306_01/java.102/b14188/datamap.htm
			jdbcTypeMap.put("LONG", "String");
			jdbcTypeMap.put("VARCHAR2", "String");
			jdbcTypeMap.put("NVARCHAR2", "String");
			jdbcTypeMap.put("NUMBER", "BigDecimal"); //oracle TODO
			jdbcTypeMap.put("RAW", "byte[]");

			jdbcTypeMap.put("INTERVALYM", "String"); //11g 
			jdbcTypeMap.put("INTERVALDS", "String"); //11g
			jdbcTypeMap.put("INTERVAL YEAR TO MONTH", "String"); //just Prevention
			jdbcTypeMap.put("INTERVAL DAY TO SECOND", "String");//just Prevention
//			jdbcTypeMap.put("TIMESTAMP", "Timestamp");   exist in comm

		} else if (DatabaseConst.SQLSERVER.equalsIgnoreCase(dbName)) {
//			jdbcTypeMap.put("SMALLINT", "Short");  //comm
			jdbcTypeMap.put("TINYINT", "Short");
//			jdbcTypeMap.put("TIME","java.sql.Time");  exist in comm
//			 DATETIMEOFFSET // SQL Server 2008  microsoft.sql.DateTimeOffset
			jdbcTypeMap.put("DATETIMEOFFSET", "microsoft.sql.DateTimeOffset");
			jdbcTypeMap.put("microsoft.sql.Types.DATETIMEOFFSET", "microsoft.sql.DateTimeOffset");
			
		} else if (DatabaseConst.PostgreSQL.equalsIgnoreCase(dbName)) {	

			jdbcTypeMap.put("bigint","Long");
			jdbcTypeMap.put("int8","Long");
			jdbcTypeMap.put("bigserial","Long");
			jdbcTypeMap.put("serial8","Long");

			jdbcTypeMap.put("integer","Integer");
			jdbcTypeMap.put("int","Integer");
			jdbcTypeMap.put("int4","Integer");
			
			jdbcTypeMap.put("serial","Integer");
			jdbcTypeMap.put("serial4","Integer");
			
			jdbcTypeMap.put("smallint","Short");
			jdbcTypeMap.put("int2","Short");
			jdbcTypeMap.put("smallserial","Short");
			jdbcTypeMap.put("serial2","Short");

			jdbcTypeMap.put("money", "BigDecimal");
			jdbcTypeMap.put("numeric", "BigDecimal");
			jdbcTypeMap.put("decimal", "BigDecimal");
			
			jdbcTypeMap.put("bit","String");
			jdbcTypeMap.put("bit varying","String");
			jdbcTypeMap.put("varbit","String");
			jdbcTypeMap.put("character","String");
			jdbcTypeMap.put("char","String");
			jdbcTypeMap.put("character varying","String");
			jdbcTypeMap.put("varchar","String");
			jdbcTypeMap.put("text","String");
			jdbcTypeMap.put("bpchar","String");//get from JDBC

			jdbcTypeMap.put("boolean","Boolean");
			jdbcTypeMap.put("bool","Boolean");
			
			jdbcTypeMap.put("double precision","Double"); //prevention
			jdbcTypeMap.put("float8","Double");

			jdbcTypeMap.put("real","Float");
			jdbcTypeMap.put("float4","Float");

//			jdbcTypeMap.put("cidr","
//			jdbcTypeMap.put("inet ","
//			jdbcTypeMap.put("macaddr","
//			jdbcTypeMap.put("macaddr8","

			jdbcTypeMap.put("json","String");  //
//			jdbcTypeMap.put("jsonb","

			jdbcTypeMap.put("bytea","byte[]");  //

			jdbcTypeMap.put("date","Date");
//			jdbcTypeMap.put("interval","
			jdbcTypeMap.put("time","Time");
			jdbcTypeMap.put("timestamp","Timestamp");

			jdbcTypeMap.put("time without time zone","Time");
			jdbcTypeMap.put("timetz","Time");
			jdbcTypeMap.put("timestamp without time zone","Timestamp");
			jdbcTypeMap.put("timestamptz","Timestamp");

		} else if (DatabaseConst.H2.equalsIgnoreCase(dbName) 
			    || DatabaseConst.SQLite.equalsIgnoreCase(dbName)) {
			jdbcTypeMap.put("MEDIUMINT", "Integer");
			jdbcTypeMap.put("INT4", "Integer");
			jdbcTypeMap.put("INT2", "Short");
			jdbcTypeMap.put("INT8", "Long");
			
			jdbcTypeMap.put("NUMBER", "BigDecimal");
			jdbcTypeMap.put("NUMERIC", "BigDecimal");

			jdbcTypeMap.put("BOOLEAN", "Boolean");
			jdbcTypeMap.put("BOOL", "Boolean");
			jdbcTypeMap.put("BIT", "Boolean");

			jdbcTypeMap.put("FLOAT8", "Double");
			jdbcTypeMap.put("FLOAT4 ", "Float");

			jdbcTypeMap.put("CHARACTER", "String");
			jdbcTypeMap.put("VARCHAR2", "String");
			jdbcTypeMap.put("NVARCHAR2", "String");
			jdbcTypeMap.put("VARCHAR_IGNORECASE", "String");
		} 
		
//		else if (DatabaseConst.H2.equalsIgnoreCase(dbName)) {  // can not use elseif again.
		if (DatabaseConst.H2.equalsIgnoreCase(dbName)) {
			
			//	/h2/docs/html/datatypes.html#real_type
			jdbcTypeMap.put("SIGNED", "Integer");
			jdbcTypeMap.put("DEC", "BigDecimal");
			jdbcTypeMap.put("YEAR", "Byte");
			jdbcTypeMap.put("BINARY VARYING", "byte[]");
			jdbcTypeMap.put("WITHOUT TIME ZONE", "Time");
			
			jdbcTypeMap.put("BINARY LARGE OBJECT","Blob");     //java.sql.Blob
			jdbcTypeMap.put("CHARACTER LARGE OBJECT","Clob");  //java.sql.Clob
			
			jdbcTypeMap.put("CHARACTER VARYING","String"); 
			jdbcTypeMap.put("VARCHAR_CASESENSITIVE","String"); 
			jdbcTypeMap.put("VARCHAR_IGNORECASE","String"); 
			
		}else if (DatabaseConst.SQLite.equalsIgnoreCase(dbName)) {
			
			jdbcTypeMap.put("VARYING CHARACTER", "String");
			jdbcTypeMap.put("NATIVE CHARACTER", "String");
			jdbcTypeMap.put("TEXT", "String");
			jdbcTypeMap.put("DOUBLE PRECISION", "Double");
			
			jdbcTypeMap.put("DATETIME", "String");
			jdbcTypeMap.put("INTEGER", "Long");  // INTEGER  PRIMARY key
			
			jdbcTypeMap.put("UNSIGNED BIG INT", "Long");
			
			jdbcTypeMap.put("VARYING", "String");
		}

	}

	private static void appendJdbcTypeCustomProp() {
		for (String s : jdbcTypeCustomProp.getKeys()) {
			jdbcTypeMap.put(s, jdbcTypeCustomProp.getValue(s));
		}
	}

	private static void appendJdbcTypeCustomProp_specificalDB() {
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
		
		//支持原生类型
		javaTypeMap.put("int", 2);
		javaTypeMap.put("long", 3);
		javaTypeMap.put("double", 4);
		javaTypeMap.put("float", 5);
		javaTypeMap.put("short", 6);
		javaTypeMap.put("byte", 7);
		javaTypeMap.put("boolean", 9);

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

	public static int getJavaTypeIndex(String javaType) {
		//    	return javaTypeMap.get(javaTypeMap)==null?-1:javaTypeMap.get(javaTypeMap);
		return javaTypeMap.get(javaType) == null ? -1 : javaTypeMap.get(javaType);
	}

	/*
	 * 首字母转换成大写
	 */
	public static String firstLetterToUpperCase(String str) {
		return NameUtil.firstLetterToUpperCase(str);
	}

//	static boolean isContinueForMoreTable(int includeType, Object object, String fieldName) {
//		return (((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.EMPTY_STRING) && object == null)
//				|| ((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.NULL) && "".equals(object)) // "  "也要排除
//		|| "serialVersionUID".equals(fieldName));
//	}

	static boolean isContinue(int includeType, Object object, Field field) {
		//		v1.7.0 第三个参数由String fieldName改为Field field.
		//		object字段上对应的值
		if (field != null) {
//			if (field.isAnnotationPresent(JoinTable.class)) return true; //v1.7.0  
//			if (field.isSynthetic()) return true;
			if(isSkipField(field)) return true;
		}

//		String fieldName ="";
//		if(field!=null) fieldName= field.getName();   //serialVersionUID放在isSkipField判断.
		
		//exclude:  NULL and "" and "  "
		if(-3==includeType) { //v1.9
			if(StringUtils.isBlank((String)object)) return true;
		}
		
//		return (((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.EMPTY_STRING) && object == null)
//				|| ((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.NULL) && "".equals(object)) 
//		|| "serialVersionUID".equals(fieldName));
		
//		includeType == NullEmpty.EMPTY_STRING && object == null  要包括空字符,但对象不是空字符,而是null,则跳过.
		return (((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.EMPTY_STRING) && object == null)
				|| ((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.NULL) && "".equals(object)) );
	}
	
	
	static boolean isSkipField(Field field) {
		if (field != null) {
			if ("serialVersionUID".equals(field.getName())) return true;
			if (field.isSynthetic()) return true;
			if (field.isAnnotationPresent(JoinTable.class)) return true;
			if (field.isAnnotationPresent(Ignore.class)) return true; //v1.9
		}
		return false;
	}
	
	static boolean isSkipFieldForMoreTable(Field field) {
		if (field != null) {
			if ("serialVersionUID".equals(field.getName())) return true;
			if (field.isSynthetic()) return true;
//			if (field.isAnnotationPresent(JoinTable.class)) return true;
			if (field.isAnnotationPresent(Ignore.class)) return true; //v1.9
		}
		
		return false;
	}
	

	/**
	 * 
	 * @param pst PreparedStatement
	 * @param objTypeIndex
	 * @param i  prarmeter index
	 * @param value
	 * @throws SQLException
	 */
	static void setPreparedValues(PreparedStatement pst, int objTypeIndex, int i, Object value) throws SQLException {

		if (null == value) {
			setPreparedNull(pst, objTypeIndex, i);
			return;
		}

		switch (objTypeIndex) {
			case 1:
				pst.setString(i + 1, (String) value);
				break;
			case 2:
				pst.setInt(i + 1, (Integer) value);
				break;
			case 3:
				pst.setLong(i + 1, (Long) value);
				break;
			case 4:
				pst.setDouble(i + 1, (Double) value);
				break;
			case 5:
				pst.setFloat(i + 1, (Float) value);
				break;
			case 6:
				pst.setShort(i + 1, (Short) value);
				break;
			case 7:
				pst.setByte(i + 1, (Byte) value);
				break;
			case 8:
				pst.setBytes(i + 1, (byte[]) value);
				break;
			case 9:
				pst.setBoolean(i + 1, (Boolean) value);
				break;
			case 10:
				pst.setBigDecimal(i + 1, (BigDecimal) value);
				break;
			case 11:
				pst.setDate(i + 1, (Date) value);
				break;
			case 12:
				pst.setTime(i + 1, (Time) value);
				break;
			case 13:
				pst.setTimestamp(i + 1, (Timestamp) value);
				break;
			case 14:
				pst.setBlob(i + 1, (Blob) value);
				break;
			case 15:
				pst.setClob(i + 1, (Clob) value);
				break;
			case 16:
				pst.setNClob(i + 1, (NClob) value);
				break;
			case 17:
				pst.setRowId(i + 1, (RowId) value);
				break;
			case 18:
				pst.setSQLXML(i + 1, (SQLXML) value);
				break;
			case 19:
				//	        	pst.setBigInteger(i+1, (BigInteger)value);break;
			default:
				pst.setObject(i + 1, value);
		} //end switch
	}

	static Object getResultObject(ResultSet rs, String typeName, String columnName) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);

		switch (k) {
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

	static Object getResultObjectByIndex(ResultSet rs, String typeName, int index) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);

		switch (k) {
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

	public static void setPreparedNull(PreparedStatement pst, int objTypeIndex, int i) throws SQLException {

		pst.setNull(i + 1, Types.NULL);
	}

	public static String genSerializableNum() {
		String s = Math.random() + "";
		int end = s.length() > 12 ? 12 : s.length();
		return "159" + s.substring(2, end) + "L";
	}

	public static String deleteLastSemicolon(String sql) {
		String new_sql = sql.trim();
		if (new_sql.endsWith(";")) return new_sql.substring(0, new_sql.length() - 1); //fix oracle ORA-00911 bug.oracle用jdbc不能有分号
		return sql;
	}

	public static <T> void checkPackage(T entity) {
		if (entity == null) return;
		if(entity.getClass().getPackage()==null) return ; //2020-04-19 if it is default package or empty package, do not check. Suggest by:pcode
		
		String packageName = entity.getClass().getPackage().getName();
		//		传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但spring开头不能过滤,否则spring想用bee就不行了.
		if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
			throw new BeeIllegalEntityException("BeeIllegalEntityException: Illegal Entity, " + entity.getClass().getName());
		}
	}

	//将非null的字段值以Map形式返回
	public static <T> Map<String, Object> getColumnMapByEntity(T entity) {
		Map<String, Object> map = new HashMap<>();
		Field fields[] = entity.getClass().getDeclaredFields();
		int len = fields.length;
		try {
			for (int i = 0; i < len; i++) {
				fields[i].setAccessible(true);
//				if (fields[i].get(entity) == null || "serialVersionUID".equals(fields[i].getName()) || fields[i].isSynthetic() || fields[i].isAnnotationPresent(JoinTable.class)) {
				if (fields[i].get(entity) == null || isSkipField(fields[i])) {
					continue;
				} else {
					map.put(_toColumnName(fields[i].getName()), fields[i].get(entity));
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return map;
	}
	
	public static String list2Value(List<PreparedValue> list){
		
		return list2Value(list,false);
	}
	//List<PreparedValue>  to valueBuffer
	public static String list2Value(List<PreparedValue> list,boolean needType){
		StringBuffer b=new StringBuffer();
		if(list==null || list.size()==0) return "";
		
		String type="";
		
		int size=list.size();
		for (int j = 0; j < size; j++) {
			b.append(list.get(j).getValue());
			type=list.get(j).getType();
			if(needType && type !=null) {
				b.append("(");
				
				if(type.startsWith("java.lang.")){
					b.append(type.substring(10));
				}else{
					b.append(type);
				}
				b.append(")");
			}
			if(j!=size-1) b.append(",");
		}
		
		return b.toString();
	}
	

	/**
	 *  ! just use in debug env.  please set off in prod env.
	 * @param sql
	 * @param list
	 * @return
	 */
	public static String getExecutableSql(String sql, List<PreparedValue> list){
		if(list==null || list.size()==0) return sql;
		
		int size=list.size();
		Object value=null;
		for (int j = 0; j < size; j++) {
			value=list.get(j).getValue();
			if(value==null || value instanceof Number){  //v1.8.15    Null no need ' and '
				sql=sql.replaceFirst("\\?", String.valueOf(value));
			}else{
				sql=sql.replaceFirst("\\?", "'"+String.valueOf(value)+"'");
			}
		}
		
		return sql;
	}
	
	static <T> String checkAndProcessSelectField(T entity, String fieldList) {

		if (fieldList == null) return null;

		Field fields[]=entity.getClass().getDeclaredFields();
		String packageAndClassName=entity.getClass().getName();
		String columnsdNames=HoneyContext.getBeanField(packageAndClassName);
		if (columnsdNames == null) {
			columnsdNames=HoneyUtil.getBeanField(fields);//获取属性名对应的DB字段名
			HoneyContext.addBeanField(packageAndClassName, columnsdNames);
		}

		return checkAndProcessSelectFieldViaString(columnsdNames, fieldList, null);
	}
	 
	 static String checkAndProcessSelectFieldViaString(String columnsdNames,String fieldList,Map<String,String> subDulFieldMap){
			
		if(fieldList==null) return null;
		 
//		Field fields[] = entity.getClass().getDeclaredFields();
//		String packageAndClassName = entity.getClass().getName();
//		String columnsdNames = HoneyContext.getBeanField(packageAndClassName);
//		if (columnsdNames == null) {
//			columnsdNames = HoneyUtil.getBeanField(fields);//获取属性名对应的DB字段名
//			HoneyContext.addBeanField(packageAndClassName, columnsdNames);
//		}
		
		columnsdNames=columnsdNames.toLowerCase();//不区分大小写检测

		String errorField = "";
		boolean isFirstError = true;
		String selectFields[] = fieldList.split(",");
		String newSelectFields = "";
		boolean isFisrt = true;
		String colName;
        String checkColName;

		for (String s : selectFields) {
			colName=_toColumnName(s);
			checkColName=colName.toLowerCase();
//			if(isMoreTable){  //带有点一样转换
//			}
			
//			if (!columnsdNames.contains(colName)) {
			if(!(  
			     columnsdNames.contains(","+checkColName+",") || columnsdNames.startsWith(checkColName+",") 
			  || columnsdNames.endsWith(","+checkColName) ||  columnsdNames.equals(checkColName) 
			  || columnsdNames.contains("."+checkColName+",")  || columnsdNames.endsWith("."+checkColName)
			  || columnsdNames.contains(","+checkColName+" ") || columnsdNames.startsWith(checkColName+" ")  //取别名
			  || columnsdNames.contains("."+checkColName+" ") //取别名
			  )  ){
				if (isFirstError) {
					errorField += s;
					isFirstError = false;
				} else {
					errorField += "," + s;
				}
			}
			
			String newField;
			if (subDulFieldMap == null) {
				newField=null;
			} else {
				newField=subDulFieldMap.get(colName);
			}
			if (newField != null) {
				if (isSQLite()) {
					colName=colName + K.as + " '" + newField + "'";
				} else {//oracle
					colName=colName + " " + newField;
				}
			}
			if (isFisrt) {
				newSelectFields += colName;
				isFisrt = false;
			} else {
				newSelectFields += ", " + colName;
			}

		}//end for

		if (!"".equals(errorField)) throw new BeeErrorFieldException("ErrorField: " + errorField);
		
		if("".equals(newSelectFields.trim())) return null;
		
		return newSelectFields;
	} 

	private static String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	private static String _toTableNameByEntityName(String entityName) {
		return NameTranslateHandle.toTableName(entityName);
	}

	private static String checkJoinTable(JoinTable joinTable) {
		String mainField;
		String subField;
		String errorMsg = "";
		mainField = joinTable.mainField();
		subField = joinTable.subField();
		int errorCount=0;
		
		if (mainField == null) {
			errorMsg = "mainField is null! ";
			errorCount++;
		} else if ("".equals(mainField.trim())) {
			errorMsg += "mainField is empty! ";
			errorCount++;
		}

		if (subField == null) {
			errorMsg += "subField is null! ";
			errorCount++;
		} else if ("".equals(subField.trim())) {
			errorMsg += "subField is empty! ";
			errorCount++;
		}
        if(errorCount==1)
		    return errorMsg;
        else return "";
	}
	
	public static boolean isMysql() {
//		return false;    //test  用来测HoneyContext.justGetPreparedValue("abc"); 检查是否还有元素,  不准确
		return    DatabaseConst.MYSQL.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName()) 
			   || DatabaseConst.MariaDB.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	//oracle,SQLite
	public static boolean isConfuseDuplicateFieldDB(){
		
		return DatabaseConst.ORACLE.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
			|| DatabaseConst.SQLite.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
				;
	}
	
	public static boolean isSQLite() {
		return DatabaseConst.SQLite.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isSqlServer() {
		return DatabaseConst.SQLSERVER.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
	public static boolean isOracle(){
		return DatabaseConst.ORACLE.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}
	
//	static boolean needCountAffectRows(){
//		return isSQLite() || DatabaseConst.H2.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
//	}
	
	public static void setPageNum(List<PreparedValue> list) {
		int array[] = (int[]) OneTimeParameter.getAttribute("_SYS_Bee_Paing_NumArray");
		for (int i = 0; array != null && i < array.length; i++) {
			PreparedValue p = new PreparedValue();
			p.setType("Integer");
			p.setValue(array[i]);
			if (HoneyUtil.isSqlServer()) { //top n
				list.add(0, p);
			} else { //default the page num in the last.
				list.add(p);
			}
		}
	}

	public static boolean isRegPagePlaceholder() {
	    return OneTimeParameter.isTrue("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPagePlaceholder() {
		OneTimeParameter.setTrueForKey("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPageNumArray(int array[]) {
		OneTimeParameter.setAttribute("_SYS_Bee_Paing_NumArray", array);
	}
	
	public static boolean isSqlKeyWordUpper() {
		String kwCase = HoneyConfig.getHoneyConfig().sqlKeyWordCase;
		return "upper".equalsIgnoreCase(kwCase) ? true : false;
	}

}
