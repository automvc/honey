package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.DatabaseConst;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.ObjSQLException;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.bee.osql.annotation.GenId;
import org.teasoft.bee.osql.annotation.GenUUID;
import org.teasoft.bee.osql.annotation.JustFetch;
import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.exception.BeeErrorFieldException;
import org.teasoft.bee.osql.exception.BeeIllegalEntityException;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.bee.osql.type.PostgreSQLJsonString;
import org.teasoft.bee.osql.type.PostgreSQLJsonbString2;
import org.teasoft.bee.osql.type.SetParaTypeConvert;
import org.teasoft.bee.spi.SqlFormat;
import org.teasoft.honey.distribution.GenIdFactory;
import org.teasoft.honey.distribution.UUID;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.name.NameUtil;
import org.teasoft.honey.osql.type.CharTypeHandler;
import org.teasoft.honey.osql.type.SetParaTypeConverterRegistry;
import org.teasoft.honey.osql.type.SqlDotDateTypeHandler;
import org.teasoft.honey.osql.type.TimestampTypeHandler;
import org.teasoft.honey.osql.type.TypeHandlerRegistry;
import org.teasoft.honey.osql.type.UtilDotDateTypeHandler;
import org.teasoft.honey.osql.type.UtilDotDateTypeToTimestampConvert;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.spi.SpiInstanceRegister;
import org.teasoft.honey.util.StringUtils;

/**
 * Bee的实现Honey的工具类.Honey Util.
 * @author Kingstar
 * @since  1.0
 */
public final class HoneyUtil {

	private static Map<String, Integer> javaTypeMap = new HashMap<>();

	static {
		initJavaTypeMap();
		initSetParaAndResultTypeHandlerRegistry();
	}

	static void refreshSetParaAndResultTypeHandlerRegistry() {
		initSetParaAndResultTypeHandlerRegistry();
	}

	private HoneyUtil() {}

	// 初始化 SQL设置参数转换注册器 和 查询结果类型转换注册器
	private static void initSetParaAndResultTypeHandlerRegistry() {

		// V2.1.6 若设置了关闭,则不再进行默认设置
		boolean closeDefaultParaResultRegistry = HoneyConfig.getHoneyConfig().closeDefaultParaResultRegistry;
		if (closeDefaultParaResultRegistry) return;
		
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.PostgreSQL);
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.H2);
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.SQLSERVER); // v2.1.8
		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeToTimestampConvert<java.util.Date>(), DatabaseConst.ORACLE); // v2.1.8
//		SetParaTypeConverterRegistry.register(java.util.Date.class, new UtilDotDateTypeConvert<java.util.Date>()); // 会少了时分秒  v2.1.8   jdk中,该类的功能就是这样的.

		TypeHandlerRegistry.register(char.class, new CharTypeHandler<Character>(), true);

		// 单DS 或者 DsMap中包含有 才执行. 触发时间,应该是在被更改配置时,调用一次
		if ((!HoneyConfig.getHoneyConfig().multiDS_enable) || (HoneyContext.getDsName2DbName() != null
				&& HoneyContext.getDsName2DbName().containsValue(DatabaseConst.SQLite))) {

			TypeHandlerRegistry.register(Timestamp.class, new TimestampTypeHandler<Timestamp>(), DatabaseConst.SQLite,
					true);
			TypeHandlerRegistry.register(java.util.Date.class, new UtilDotDateTypeHandler<java.util.Date>(),
					DatabaseConst.SQLite, true);
			TypeHandlerRegistry.register(java.sql.Date.class, new SqlDotDateTypeHandler<java.util.Date>(),
					DatabaseConst.SQLite, true);
		}
	}

	@SuppressWarnings("rawtypes")
	static String getBeanField(Field field[], Class entityClass) {
		if (field == null) return "";
		StringBuffer s = new StringBuffer();
		int len = field.length;
		boolean isFirst = true;

		for (int i = 0; i < len; i++) {
			if (isSkipField(field[i])) continue;

			if (field[i].isAnnotationPresent(JustFetch.class)) {
				if (StringUtils.isBlank(getJustFetchDefineName(field[i]))) continue; // V2.1.8
																						// 只用@JustFetch()时,该字段不转换到select列表中,用于像count(*)
																						// as para变量的接收
				if (isFirst) {
					isFirst = false;
				} else {
					s.append(",");
				}
				s.append(getJustFetchColumn(field[i]));
			} else {
				if (isFirst) {
					isFirst = false;
				} else {
					s.append(",");
				}
				s.append(toColumnName(field[i].getName(), entityClass));
			}

		}
		return s.toString();
	}

	private static String getJustFetchColumn(Field field) {

		String expression = getJustFetchDefineName(field);
		String c = "";
		String fName = toColumnName(field.getName());
		if (isSQLite()) {
			c = expression + " " + K.as + " " + fName; // fixed bug V2.1.8
		} else {
			c = expression + " " + fName;
		}

		return c;
	}

	private static String getJustFetchDefineName(Field field) {
		JustFetch justFetch = field.getAnnotation(JustFetch.class);
		String expression = justFetch.value();

		checkExpression(expression);

		return expression;
	}

	private static void checkExpression(String expression) {
		if (Check.isNotValidExpressionForJustFetch(expression)) {
			throw new BeeIllegalSQLException("The expression: '" + expression + "' is invalid in JustFetch Annotation!");
		}
	}

	/**
	 * jdbc type->java type
	 * 将jdbc的数据类型转换为java的类型 
	 * @param jdbcType
	 * @return the string of java type
	 */
	public static String getFieldType(String jdbcType) {

		String dbName = HoneyConfig.getHoneyConfig().getDbName();
		Map<String, String> jdbc2JavaTypeMap = JdbcToJavaType.getJdbcToJavaType(dbName);

		String javaType = jdbc2JavaTypeMap.get(jdbcType);

		if (javaType != null) return javaType;

		if (jdbc2JavaTypeMap.get(jdbcType) == null) {

			// fix UNSIGNED, like :TINYINT UNSIGNED
			String tempType = jdbcType.trim();
			if (tempType.endsWith(" UNSIGNED")) {
				int i = tempType.indexOf(" ");
				javaType = jdbc2JavaTypeMap.get(tempType.substring(0, i));
				if (javaType != null) return javaType;
			}

			if (javaType == null) {
				javaType = jdbc2JavaTypeMap.get(jdbcType.toLowerCase());
				if (javaType != null) return javaType;

				if (javaType == null) {
					javaType = jdbc2JavaTypeMap.get(jdbcType.toUpperCase());
					if (javaType != null) return javaType;
				}
			}

			if (HoneyUtil.isMongoDB()) {
				javaType = NameTranslateHandle.toEntityName(jdbcType);
			} else {
				javaType = "[UNKNOWN TYPE]" + jdbcType;
			}
			Logger.debug(javaType); // V1.17
		}

		return javaType;
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
		javaTypeMap.put("[B", 8); // byte[]
		javaTypeMap.put("java.lang.Boolean", 9);

		// 支持原生类型
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

		javaTypeMap.put("char", 20);
		javaTypeMap.put("java.util.Date", 21);

		javaTypeMap.put("java.sql.Array", 22);
		javaTypeMap.put("java.io.InputStream", 23);
		javaTypeMap.put("java.io.Reader", 24);
		javaTypeMap.put("java.sql.Ref", 25);

//	    javaTypeMap.put("org.teasoft.bee.osql.annotation.customizable.Json", 26);

		javaTypeMap.put("java.net.URL", 27);

//		javaTypeMap.put("java.util.UUID", 28);  //1 

		javaTypeMap.put("java.time.LocalDate", 31);
		javaTypeMap.put("java.time.LocalTime", 32);
		javaTypeMap.put("java.time.LocalDateTime", 33);

		// 34 jsonb for pgsql. 2.4.0
	}

	public static int getJavaTypeIndex(String javaType) {
		return javaTypeMap.get(javaType) == null ? -1 : javaTypeMap.get(javaType);
	}

	/**
	 * 首字母转换成大写
	 */
	public static String firstLetterToUpperCase(String str) {
		return NameUtil.firstLetterToUpperCase(str);
	}

	public static boolean isContinue(int includeType, Object object, Field field) {
		// object字段上对应的值
		if (field != null) {
			if (isSkipField(field)) return true;
			if (isSkipFieldJustFetch(field)) return true; // V1.11 JustFetch不用于where
		}

		return isContinueByCheckIncludeType(includeType, object);
	}

	public static boolean isContinueByCheckIncludeType(int includeType, Object object) {
		// exclude: NULL and "" and " "
		if (-3 == includeType && StringUtils.isBlank((String) object)) {
			return true;
		}

//		includeType == NullEmpty.EMPTY_STRING && object == null  要包括空字符,但对象不是空字符,而是null,则跳过.
//		return (((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.EMPTY_STRING) && object == null)
//				|| ((includeType == NullEmpty.EXCLUDE || includeType == NullEmpty.NULL) && "".equals(object)));

		return (((includeType == IncludeType.EXCLUDE_BOTH.getValue()
				|| includeType == IncludeType.INCLUDE_EMPTY.getValue()) && object == null)
				|| ((includeType == IncludeType.EXCLUDE_BOTH.getValue()
						|| includeType == IncludeType.INCLUDE_NULL.getValue()) && "".equals(object)));
	}

	public static boolean isSkipField(Field field) {
		if (field != null) {
			if ("serialVersionUID".equals(field.getName())) return true;
			if (AnnoUtil.isIgnore(field)) return true; // 1.17
			if (AnnoUtil.isJoinTable(field)) return true; // 3.0.0
			if (field.isSynthetic()) return true;
			if (AnnoUtil.isFK(field)) return true; // 2.1.8
			if (AnnoUtil.isGridFs(field)) return true; // 2.1.8
			if (AnnoUtil.isGridFsMetadata(field)) return true; // 2.1.8
		}
		return false;
	}

	static boolean isSkipFieldForMoreTable(Field field) {
		if (field != null) {
			if ("serialVersionUID".equals(field.getName())) return true;
			if (AnnoUtil.isIgnore(field)) return true; // 1.17
			if (field.isSynthetic()) return true;
		}

		return false;
	}

	public static boolean isSkipFieldJustFetch(Field field) {
		if (field != null) {
			if (field.isAnnotationPresent(JustFetch.class)) return true;
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static void setPreparedValues(PreparedStatement pst, int objTypeIndex, int i, Object value) throws SQLException {

		if (value == null) {
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
//			case 11: //you can define your SetParaTypeConvert for one of both
//				pst.setDate(i + 1, (Date) value);//process in default
//				void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
//				break;
			case 12:
				pst.setTime(i + 1, (Time) value);
				break;
			case 13:
				pst.setTimestamp(i + 1, (Timestamp) value);
				break;
//			case 14://you can define your SetParaTypeConvert for one of both
//				pst.setBlob(i + 1, (Blob) value);//process in default
//				pst.setBlob(parameterIndex, inputStream);
//				break;
//			case 15:  //you can define your SetParaTypeConvert for one of both
//				pst.setClob(i + 1, (Clob) value); //process in default
////			pst.setClob(i + 1, reader);
//				break;
//			case 16: // like 15
//				pst.setNClob(i + 1, (NClob) value);
//				break;
			case 17:
				pst.setRowId(i + 1, (RowId) value);
				break;
			case 18:
				pst.setSQLXML(i + 1, (SQLXML) value);
				break;
			case 20:
				pst.setString(i + 1, value.toString());
				break;

//	             process in define SetParaTypeConvert
//			case 21:
//				Date d= new Date(((java.util.Date)value).getTime());
////				pst.setDate(i + 1, d); //ok
//				pst.setObject(i + 1, d); //ok
//				//测试数据库是date,datetime,timestamp是否都可以
//				break;

			case 22:
				pst.setArray(i + 1, (java.sql.Array) value);
				break;

//			case 23,24
			// InputStream,Reader can define the SetParaTypeConvert for them.
//				pst.setAsciiStream(parameterIndex, java.io.InputStream x);
//				pst.setBinaryStream(parameterIndex, java.io.InputStream x);
//				pst.setCharacterStream(parameterIndex, reader);
//				pst.setNCharacterStream(parameterIndex, reader);
//				pst.setUnicodeStream(parameterIndex, x, length);

			case 25:
				pst.setRef(i + 1, (Ref) value);
				break;

			case 26: // Json Annotation //NOSONAR
			{
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
				if (converter != null) {
					String jsonStr = (String) converter.convert(value);
					if (isPostgreSQL()) { // 2.4.0 PostgreSQL donot support string type json, need convert to its type
						SetParaTypeConvert pgJosnStringConverter = SetParaTypeConverterRegistry
								.getConverter(PostgreSQLJsonString.class);
						Object jsonObjct = pgJosnStringConverter.convert(value);
						pst.setObject(i + 1, jsonObjct);
					} else {
						pst.setString(i + 1, jsonStr);
					}
					break;
				}
			}

			case 27:
				pst.setURL(i + 1, (java.net.URL) value);
				break;

//				pst.setUnicodeStream(parameterIndex, x, length);

//			case 28:   //2 
//				UUID u=(UUID)value;
//				pst.setObject(i + 1, u.toString());
//				pst.setString(i + 1, u.toString());
//				pst.setObject(i + 1, u);  
//				pst.setObject(i + 1, u, targetSqlType);
//				break;

			case 34: {
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
				if (converter != null) {
					String jsonStr = (String) converter.convert(value);
					if (isPostgreSQL()) { // 2.4.0 PostgreSQL donot support string type json, need convert to its type
						SetParaTypeConvert pgJosnStringConverter2 = SetParaTypeConverterRegistry
								.getConverter(PostgreSQLJsonbString2.class);
						Object jsonObjct = pgJosnStringConverter2.convert(value);
						pst.setObject(i + 1, jsonObjct);
					} else {
						pst.setString(i + 1, jsonStr);
					}
					break;
				}
			}

			case 19:
//	        	pst.setBigInteger(i+1, (BigInteger)value);break;
			default: {
//				先查找是否有对应类型的转换器;  到这里value不会是null;前面已处理
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(value.getClass()); // fixed bug
				if (converter != null) {
					value = converter.convert(value);
					pst.setObject(i + 1, value);
					// if did not define SetParaTypeConvert,will process by default
				} else if (objTypeIndex == 11) {
					pst.setDate(i + 1, (Date) value);
				} else if (objTypeIndex == 14) {
					pst.setBlob(i + 1, (Blob) value);
				} else if (objTypeIndex == 15) {
					pst.setClob(i + 1, (Clob) value);
				} else if (objTypeIndex == 16) {
					pst.setNClob(i + 1, (NClob) value);
				} else if (objTypeIndex == 23) {
					pst.setBinaryStream(i + 1, (java.io.InputStream) value);
				} else if (objTypeIndex == 24) {
					pst.setCharacterStream(i + 1, (java.io.Reader) value);
				} else {
					pst.setObject(i + 1, value);
				}

			}
		} // end switch
	}

	static Object getResultObject(ResultSet rs, String typeName, String columnName) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);
		if (isSQLite() && "java.sql.Timestamp".equals(typeName)) {
			k = 1;
		}

		switch (k) {
			case 1:
				return rs.getString(columnName);
			case 2:
				return rs.getInt(columnName); // DB是Null，也会是0
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

//				 19: BigInteger
//				 20:char

//				 21:java.util.Date 
			case 21:
				return rs.getTimestamp(columnName);// 改动态???
			case 22:
				return rs.getArray(columnName); // java.sql.Array
			case 23:
				return rs.getBinaryStream(columnName); // java.io.InputStream
			case 24:
				return rs.getCharacterStream(columnName); // java.io.Reader
			case 25:
				return rs.getRef(columnName); // java.sql.Ref

//			26:	annotation.customizable.Json

			case 27:
				return rs.getURL(columnName);

			case 19:
//	        	no  getBigInteger
			default:
				return rs.getObject(columnName);
		} // end switch

	}

	static Object getResultObjectByIndex(ResultSet rs, String typeName, int index) throws SQLException {

		int k = HoneyUtil.getJavaTypeIndex(typeName);
//		if (isSQLite() && "java.sql.Timestamp".equals(typeName)) {
		if (isSQLite() && ("java.sql.Timestamp".equals(typeName) || "java.sql.Date".equals(typeName))) {
			k = 1;
		}

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
			case 31:
				return rs.getDate(index);
			case 12:
			case 32:
				return rs.getTime(index);
			case 13:
			case 33:
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

//				19:BigInteger
//				20:char

//				 21:java.util.Date 
			case 21:
				return rs.getTimestamp(index);// 改动态???
//				return rs.getDate(index);  //Oracle 使用该方法获取会丢失:时分秒

			case 22:
				return rs.getArray(index); // java.sql.Array
			case 23:
				return rs.getBinaryStream(index); // java.io.InputStream
			case 24:
				return rs.getCharacterStream(index); // java.io.Reader
//				return rs.getAsciiStream(index); //java.io.InputStream	
//				return rs.getNCharacterStream(index); //java.io.Reader	
//				return rs.getNString(index);  //java.lang.String
			case 25:
				return rs.getRef(index); // java.sql.Ref

//				26:	annotation.customizable.Json

			case 27:
				return rs.getURL(index);

			case 19:
				// no getBigInteger
			default:
				return rs.getObject(index);
		} // end switch

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
		if (sql == null) return sql;
		String new_sql = sql.trim();
		if (new_sql.endsWith(";")) return new_sql.substring(0, new_sql.length() - 1); // fix oracle ORA-00911 bug.oracle用jdbc不能有分号

		sql = _deletePrefix(new_sql); // 2.2

		return sql;
	}

	public static String deletePrefix(String sql) { // 2.2
		if (sql == null) return sql;
		String new_sql = sql.trim();

		return _deletePrefix(new_sql);
	}

	private static String _deletePrefix(String new_sql) {
		if (new_sql.startsWith(START_MARK)) return new_sql.substring(20, new_sql.length()); // 2.2
		return new_sql;
	}

	public static <T> void checkPackage(T entity) {
		if (entity == null) return;

		String classFullName = entity.getClass().getName();
		// 传入的实体可以过滤掉常用的包开头的,如:java., javax. ; 但像spring开头不能过滤,否则spring想用bee就不行了.
		if (classFullName.startsWith("java.") || classFullName.startsWith("javax.")
				|| classFullName.startsWith("jakarta.")) {
			throw new BeeIllegalEntityException(
					"BeeIllegalEntityException: Illegal Entity, " + entity.getClass().getName());
		}
	}

	public static boolean isJavaPackage(Class<?> entityClass) {
		if (entityClass == null) return false;
		String classFullName = entityClass.getName();
		return (classFullName.startsWith("java.") || classFullName.startsWith("javax.")
				|| classFullName.startsWith("jakarta."));
	}

	/**
	 * 将非null的字段值以Map形式返回(被认为忽略的字段也不会返回,即isSkipField(fields[i]))
	 * @param entity
	 * @return
	 */
	public static <T> Map<String, Object> getColumnMapByEntity(T entity) {
		Map<String, Object> map = new LinkedHashMap<>();
		Field fields[] = HoneyUtil.getFields(entity.getClass());
		int len = fields.length;
		try {
			for (int i = 0; i < len; i++) {
				HoneyUtil.setAccessibleTrue(fields[i]);
				if (fields[i].get(entity) == null || isSkipField(fields[i])) {
					continue;
				} else {
					map.put(toColumnName(fields[i].getName(), entity.getClass()), fields[i].get(entity));
				}
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return map;
	}

	// List<PreparedValue> to value string
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String list2Value(List<PreparedValue> list, boolean needType) {
		StringBuffer b = new StringBuffer();
		if (list == null) return null;
		if (list.size() == 0) return "";

		String type = "";

		int size = list.size();
		Object value = null;
		for (int j = 0; j < size; j++) {

			value = list.get(j).getValue();

			// V1.11
			if (list.get(j).getJsonType() >= 1) {
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
				if (converter != null) {
					value = converter.convert(value);
				}
			}

			b.append(value);
			type = list.get(j).getType();
			if (needType && type != null) {
				b.append("(");

				if (type.startsWith("java.lang.")) {
					b.append(type.substring(10));
				} else {
					b.append(type);
				}
				b.append(")");
			}
			if (j != size - 1) b.append(",");
		}

		return b.toString();
	}

	/**
	 *  ! just use in debug env.  please set off in prod env.
	 * @param sql
	 * @param list
	 * @return executable sql string
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getExecutableSql(String sql, List<PreparedValue> list) {
		if (list == null || list.size() == 0) return sql;

		int size = list.size();
		Object value = null;
		for (int j = 0; j < size; j++) {
			value = list.get(j).getValue();
			// V1.11
			if (list.get(j).getJsonType() >= 1) {
				SetParaTypeConvert converter = SetParaTypeConverterRegistry.getConverter(Json.class);
				if (converter != null) {
					value = converter.convert(value);
				}
			}

			if (value != null && value instanceof CharSequence) { // V1.11
//				V1.17 添加单引号转义;双引号不需要转;
				sql = sql.replaceFirst("\\?", "'" + String.valueOf(value).replace("\\%", "\\\\%").replace("\\_", "\\\\_")
						.replace("'", "\\\\'").replace("$", "\\$") + "'"); // bug 2021-05-25
			} else {
				sql = sql.replaceFirst("\\?", String.valueOf(value));
			}
		}
		sql += " ;"; // V1.17 添加分号
		return sql;
	}

	// V2.1.7
	public static String sqlFormat(String sql) {
		boolean sqlFormatFlag = HoneyConfig.getHoneyConfig().showSql_sqlFormat;
		if (sqlFormatFlag) {
			SqlFormat sqlFormat = SpiInstanceRegister.getInstance(SqlFormat.class);
			if (sqlFormat != null) {
				try {
					sql = sqlFormat.format(sql);
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return sql;
	}

	static <T> String checkAndProcessSelectField(T entity, String... fieldList) {

		if (fieldList == null) return null;

		String packageAndClassName = entity.getClass().getName();
		String columnsdNames = HoneyContext.getBeanField(packageAndClassName);
		if (columnsdNames == null) {
			Field fields[] = HoneyUtil.getFields(entity.getClass());
			columnsdNames = HoneyUtil.getBeanField(fields, entity.getClass());// 获取属性名对应的DB字段名
			HoneyContext.addBeanField(packageAndClassName, columnsdNames);
		}

		return checkAndProcessSelectFieldViaString(columnsdNames, null, fieldList);
	}

	public static String checkAndProcessSelectFieldViaString(String columnsdNames, Map<String, String> subDulFieldMap,
			String... fields) {
//		return checkAndProcessSelectFieldViaString(columnsdNames, subDulFieldMap, true, fields);
		return checkAndProcessSelectFieldViaString(columnsdNames, subDulFieldMap, false, fields);
	}

	public static String checkAndProcessSelectFieldViaString(String columnsdNames, Map<String, String> subDulFieldMap,
			boolean check, String... fields) {
		if (fields == null) return null;

		columnsdNames = columnsdNames.toLowerCase();// 不区分大小写检测

		String errorField = "";
		boolean isFirstError = true;
		String selectFields[];

		if (fields.length == 1) { // 变长参数,只有一个时,才允许用逗号隔开
			selectFields = fields[0].split(",");
		} else {
			selectFields = fields;
		}
		StringUtils.trim(selectFields);

		String newSelectFields = "";
		boolean isFisrt = true;
		String colName;
		String checkColName;

		for (String s : selectFields) {
			colName = toColumnName(s);
			checkColName = colName.toLowerCase();
//			if(isMoreTable){  //带有点一样转换
//			}

			// todo 不准确。
			if (!(columnsdNames.contains("," + checkColName + ",") || columnsdNames.startsWith(checkColName + ",")
					|| columnsdNames.endsWith("," + checkColName) || columnsdNames.equals(checkColName)
					|| columnsdNames.contains("." + checkColName + ",") || columnsdNames.endsWith("." + checkColName)
					|| columnsdNames.contains("," + checkColName + " ") || columnsdNames.startsWith(checkColName + " ") // 取别名
					|| columnsdNames.contains("." + checkColName + " ") // 取别名
			)) {
				if (isFirstError) {
					errorField += s;
					isFirstError = false;
				} else {
					errorField += "," + s;
				}
			}

			String newField;
			if (subDulFieldMap == null) {
				newField = null;
			} else {
				newField = subDulFieldMap.get(colName);
			}
			if (newField != null) {
				if (isSQLite()) {
					colName = colName + "  " + K.as + " '" + newField + "'";
				} else {// oracle
					colName = colName + " " + newField;
				}
			}
			if (isFisrt) {
				newSelectFields += colName;
				isFisrt = false;
			} else {
				newSelectFields += ", " + colName;
			}

		} // end for

		if (!"".equals(errorField)) {
			if (check) {
				throw new BeeErrorFieldException("ErrorField: " + errorField);
			} else { // 2.4.0 fun 不检测字段是否是实体的
				Logger.debug("Please confirm the field(s) is right or not :" + errorField);
			}
		}

		if ("".equals(newSelectFields.trim())) return null;

		return newSelectFields;
	}

	public static String toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}

	private static String _toTableName(Object entity) {
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	@SuppressWarnings("rawtypes")
	public static String toColumnName(String fieldName, Class entityClass) {
		return NameTranslateHandle.toColumnName(fieldName, entityClass);
//		KeyWord.transformNameIfKeyWork(fieldName);
	}

//	private static String _toTableNameByEntityName(String entityName) {
//		return NameTranslateHandle.toTableName(entityName);
//	}

	public static boolean isMysql() {
		return DatabaseConst.MYSQL.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
				|| DatabaseConst.MariaDB.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	// Oracle,SQLite,Sql Server,and so on. 2.4.2 default just exclude MYSQL,MariaDB,H2
	public static boolean isConfuseDuplicateFieldDB() {
//		return DatabaseConst.ORACLE.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
//			|| DatabaseConst.SQLite.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())
//			|| DatabaseConst.SQLSERVER.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName()) 
//			|| DatabaseConst.PostgreSQL.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName()) //2.4.2
//				;
		// 2.4.2
		String dbName = HoneyConfig.getHoneyConfig().getDbName();
		return !DatabaseConst.MYSQL.equalsIgnoreCase(dbName) && !DatabaseConst.MariaDB.equalsIgnoreCase(dbName)
				&& !DatabaseConst.H2.equalsIgnoreCase(dbName);
	}

	public static boolean isSQLite() {
		return DatabaseConst.SQLite.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isSqlServer() {
		return DatabaseConst.SQLSERVER.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isOracle() {
		return DatabaseConst.ORACLE.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isH2() {
		return DatabaseConst.H2.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isCassandra() {
		return DatabaseConst.Cassandra.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isHbase() {
		return DatabaseConst.Hbase.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isMongoDB() {
		return DatabaseConst.MongoDB.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean isPostgreSQL() {
		return DatabaseConst.PostgreSQL.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName());
	}

	public static boolean setPageNum(List<PreparedValue> list) {
		int array[] = (int[]) OneTimeParameter.getAttribute("_SYS_Bee_Paing_NumArray");
		for (int i = 0; array != null && i < array.length; i++) {
			PreparedValue p = new PreparedValue();
//			p.setType("Integer"); //bug
			p.setType("java.lang.Integer"); // fixed bug
			p.setValue(array[i]);
			if (HoneyUtil.isSqlServer()) { // top n
				list.add(0, p);
			} else { // default the page num in the last.
				list.add(p);
			}
		}

		return array != null ? true : false;
	}

	public static boolean isRegPagePlaceholder() {
		return OneTimeParameter.isTrue("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPagePlaceholder() {
		if (isSqlServer()) return;
		if (ShardingUtil.hadSharding()) return; // 2.0 有分片则不用
		OneTimeParameter.setTrueForKey("_SYS_Bee_Paing_Placeholder");
	}

	public static void regPageNumArray(int array[]) {
		OneTimeParameter.setAttribute("_SYS_Bee_Paing_NumArray", array);
	}

	public static boolean isSqlKeyWordUpper() {
		String kwCase = HoneyConfig.getHoneyConfig().sqlKeyWordCase;
		return "upper".equalsIgnoreCase(kwCase) ? true : false;
	}

	public static <T> Field getPkField(T entity) {
		return getPkField(entity.getClass());
	}

	public static <T> Field getPkField(Class<T> entityClass) {
		Field field = null;
		try {
			field = HoneyUtil.getField(entityClass, "id");
		} catch (NoSuchFieldException e) {
			String pkName = getPkFieldNameByClass(entityClass);

			boolean hasException = false;
			if ("".equals(pkName)) {
				hasException = true;
			} else if (pkName != null && !pkName.contains(",")) {
				try {
					field = HoneyUtil.getField(entityClass, pkName);
				} catch (NoSuchFieldException e2) {
					hasException = true;
				}
			} else {
				// DB是否支持联合主键返回?? 不支持
				Logger.warn("Don't support return id value when the primary key more than one field!");
			}
			if (hasException) throw new ObjSQLException("Miss id field: the entity no id field!");
		}

		return field;
	}

	public static <T> Object getIdValue(T entity) {
		Field field = getPkField(entity);
		Object obj = null;
		try {
			if (field != null) {
				HoneyUtil.setAccessibleTrue(field);
				obj = field.get(entity);
			}
		} catch (IllegalAccessException e) {
			throw ExceptionHelper.convert(e);
		}

		return obj;
	}

	public static <T> boolean hasGenPkAnno(T entity) {
		Field field = getPkField(entity);
		if (field != null) {
			return AnnoUtil.isGenPkAnno(field);
		}
		return false;
	}

	public static <T> void revertId(T entity) {
		Field field = null;
		if (OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST)) {
			try {
				Object obj = OneTimeParameter.getAttribute(StringConst.OLD_ID);
				String pkName = (String) OneTimeParameter.getAttribute(StringConst.Primary_Key_Name);
				field = HoneyUtil.getField(entity.getClass(), pkName);

				HoneyUtil.setAccessibleTrue(field);
				HoneyUtil.setFieldValue(field, entity, obj);
			} catch (NoSuchFieldException e) {
				throw new ObjSQLException("Miss id field: the entity no id field!");
			} catch (IllegalAccessException e) {
				throw ExceptionHelper.convert(e);
			}
		}
	}

	public static <T> void revertId(T entity[]) {
		Field field = null;
		String pkName = (String) OneTimeParameter.getAttribute(StringConst.Primary_Key_Name); // 可能为null
		if (pkName == null) {
			for (int i = 0; i < entity.length; i++) {
				// 用掉
				OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST + i);
				OneTimeParameter.getAttribute(StringConst.OLD_ID + i);
			}
			return; // pkName == null时提前返回
		}

		for (int i = 0; i < entity.length; i++) {

			if (OneTimeParameter.isTrue(StringConst.OLD_ID_EXIST + i)) {
				try {
					Object obj = OneTimeParameter.getAttribute(StringConst.OLD_ID + i);
					field = HoneyUtil.getField(entity[i].getClass(), pkName);
					HoneyUtil.setAccessibleTrue(field);
					HoneyUtil.setFieldValue(field, entity[i], obj);
				} catch (NoSuchFieldException e) {
					throw new ObjSQLException("entity[] miss id field: the element in entity[] no id field!");
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				} catch (Exception e) {
					Logger.warn(e.getMessage(), e);
				}
			}
		}
	}

	public static <T> void setInitArrayIdByAuto(T entity[]) {

		if (entity == null || entity.length < 1) return;

//			boolean needGenId = HoneyContext.isNeedGenId(entity[0].getClass());
//			if (!needGenId) return;

		boolean hasValue = false;
//			Long v = null;

		Field field0 = null;
		String pkName = "";
		String pkAlias = "";
		boolean isStringField = false;
		boolean hasGenUUIDAnno = false;
		boolean useSeparatorInUUID = false;
		try {
			// V1.11
			boolean noId = false;
			try {
				field0 = HoneyUtil.getField(entity[0].getClass(), "id");
				pkName = "id";
			} catch (NoSuchFieldException e) {
				noId = true;
			}
			if (noId) {
				pkName = HoneyUtil.getPkFieldName(entity[0]);
				if ("".equals(pkName) || pkName.contains(",")) return; // just support single primary key.
				field0 = HoneyUtil.getField(entity[0].getClass(), pkName);// fixed 1.17
				pkAlias = "(" + pkName + ")";
			}

			if (field0 == null) return; // 没有主键,则提前返回

			boolean replaceOldValue = HoneyConfig.getHoneyConfig().genid_replaceOldId;

			if (field0.isAnnotationPresent(GenId.class)) {
				GenId genId = field0.getAnnotation(GenId.class);
				replaceOldValue = replaceOldValue || genId.override();
			} else if (field0.isAnnotationPresent(GenUUID.class)) {
				GenUUID gen = field0.getAnnotation(GenUUID.class);
				replaceOldValue = replaceOldValue || gen.override();
				hasGenUUIDAnno = true;
				useSeparatorInUUID = gen.useSeparator();
			} else {
				boolean needGenId = HoneyContext.isNeedGenId(entity[0].getClass());
				if (!needGenId) return;
			}

			isStringField = field0.getType().equals(String.class);
			if (hasGenUUIDAnno && !isStringField) {
				Logger.warn("Gen UUID as id just support String type field!");
				return;
			}

//				if (!field0.getType().equals(Long.class)) {//just set the null Long id field
			if (_ObjectToSQLHelper.errorType(field0)) {// set Long or Integer type id
				Logger.warn("The id" + pkAlias + " field's " + field0.getType()
						+ " is not Long/Integer, can not generate the Long/Integer id automatically!");
				return;
			}

//				if (field.get(entity[0]) != null) return; //即使没值,运行一次后也会有值,下次再用就会重复.而用户又不知道.    //要提醒是被覆盖了。

//				boolean replaceOldValue = HoneyConfig.getHoneyConfig().genid_replaceOldId;

			int len = entity.length;
			String tableKey = _toTableName(entity[0]);
			long ids[];
			long id = 0;
			if (_ObjectToSQLHelper.isInt(field0)) {
				ids = GenIdFactory.getRangeId(tableKey, GenIdFactory.GenType_IntSerialIdReturnLong, len);
				id = ids[0];
			} else if (!hasGenUUIDAnno) {
				ids = GenIdFactory.getRangeId(tableKey, len);
				id = ids[0];
			}

			Field field = null;
			for (int i = 0; i < len; id++, i++) {
				if (entity[i] == null) continue;
				hasValue = false;
//					v = null;

				field = HoneyUtil.getField(entity[i].getClass(), pkName);
				HoneyUtil.setAccessibleTrue(field);
				Object obj = field.get(entity[i]);

				if (obj != null) {
					if (!replaceOldValue) return;
					hasValue = true;
//						v = (Long) obj;
				}

//					OneTimeParameter.setTrueForKey(StringConst.OLD_ID_EXIST+i);
//					OneTimeParameter.setAttribute(StringConst.OLD_ID+i, obj);

				HoneyUtil.setAccessibleTrue(field);
				try {
					if (_ObjectToSQLHelper.isInt(field0))
						HoneyUtil.setFieldValue(field, entity[i], (int) id);
					else if (!hasGenUUIDAnno && isStringField) // 没有用GenUUID又是String
						HoneyUtil.setFieldValue(field, entity[i], id + "");
					else if (hasGenUUIDAnno && isStringField) // 用GenUUID
						HoneyUtil.setFieldValue(field, entity[i], UUID.getId(useSeparatorInUUID));
					else
						HoneyUtil.setFieldValue(field, entity[i], id);
					if (hasValue) {
						Logger.warn(" [ID WOULD BE REPLACED] entity[" + i + "] : " + entity[0].getClass() + " 's id field"
								+ pkAlias + " value is " + obj.toString() + " would be replace by " + id);
					}

					// fixed bug
					OneTimeParameter.setAttribute(StringConst.Primary_Key_Name, pkName);
					OneTimeParameter.setTrueForKey(StringConst.OLD_ID_EXIST + i);
					OneTimeParameter.setAttribute(StringConst.OLD_ID + i, obj);
				} catch (IllegalAccessException e) {
					throw ExceptionHelper.convert(e);
				}
			}
//				OneTimeParameter.setAttribute(StringConst.Primary_Key_Name, pkName); //bug,在分片批量插入时,多线程下,有可能设置不成功

		} catch (NoSuchFieldException e) {
			// is no id field , ignore.
			return;
		} catch (Exception e) {
			Logger.warn(e.getMessage(), e);
			return;
		}
	}

	// do not check default id
	public static <T> String getPkFieldName(T entity) {
		if (entity == null) return null;
		return getPkFieldNameByClass(entity.getClass());
	}

	/**
	 * 查找有PrimaryKey的字段; 没有检测默认的id
	 * @param c
	 * @return
	 */
	public static String getPkFieldNameByClass(Class<?> c) {

		if (c == null) return null;
		String classFullName = c.getName();
		String pkey = HoneyContext.getBeanCustomPKey(classFullName);
		if (pkey != null) return pkey;

		Field field[] = HoneyUtil.getFields(c);
		int len = field.length;
		boolean isFirst = true;

		pkey = "";
		for (int i = 0; i < len; i++) {
//			if (isSkipFieldForMoreTable(field[i])) continue; //JoinTable可以与PrimaryKey合用? 实际不会同时用
			if (isSkipField(field[i]) && !AnnoUtil.isFK(field[i])) continue; // V2.1.8 是FK,也有可能是Primary
			if (AnnoUtil.isPrimaryKey(field[i])) {
				if (isFirst)
					isFirst = false;
				else
					pkey += ",";
				pkey += field[i].getName();
			}
		} // end for

		HoneyContext.addBeanCustomPKey(classFullName, pkey);

		return pkey;
	}

	public static String getPlaceholderValue(int size) {
		StringBuffer placeholderValue = new StringBuffer(" (");
		for (int i = 0; i < size; i++) {
			if (i != 0) placeholderValue.append(",");
			placeholderValue.append("?");
		}
		if (size <= 0) placeholderValue.append("''");
		placeholderValue.append(")");

		return placeholderValue.toString();
	}

	private final static String NumberArrayTypes[] = { "[Ljava.lang.Long;", "[Ljava.lang.Integer;", "[Ljava.lang.Short;",
			"[Ljava.lang.Byte;", "[Ljava.lang.Number;", "[Ljava.lang.Double;", "[Ljava.lang.Float;",
			"[Ljava.math.BigInteger;", "[Ljava.math.BigDecimal;" };

	public static boolean isNumberArray(Class<?> c) {
		return isNumberArray0(c);
	}

	private static boolean isNumberArray0(Class<?> c) {
		if (c == null) return false;
		for (String type : NumberArrayTypes) {
			if (type.equals(c.getName())) return true;
		}
		return false;
	}

	public static boolean isNumberArray(Object v) {
		if (v == null) return false;
//		if (v ==Class.class) return isNumberArray0((Class<?>)v);
		if (v instanceof Class) isNumberArray0((Class<?>) v);

		if (v instanceof Integer[] || v instanceof Long[] || v instanceof Double[] || v instanceof Float[]
				|| v instanceof Number[] || v instanceof Short[] || v instanceof Byte[] || v instanceof BigInteger[]
				|| v instanceof BigDecimal[]) {
			return true;
		} else {
			return false;
		}
	}

	/**
	* 判断参数是否为数字
	* @param obj
	* @return 是数字返回true
	*/
	public static boolean isNumber(Object obj) {
		if (obj instanceof Integer || obj instanceof Long || obj instanceof Short || obj instanceof Byte
				|| obj instanceof Double || obj instanceof Float || obj instanceof BigInteger
				|| obj instanceof BigDecimal) {
			return true;
		}
		return false;
	}

	/**
	 * 只判断MySQL,MariaDB,Oracle,H2,SQLite,PostgreSQL,SQL Server,Cassandra
	 * @return
	 */
	public static boolean isUpperCaseDB() {
		if (isOracle())
			return true;
		else if (DatabaseConst.H2.equalsIgnoreCase(HoneyConfig.getHoneyConfig().getDbName())) return true;

		return false;
	}

	public static InterceptorChain copy(InterceptorChain obj) {
		try {
			Serializer jdks = new JdkSerializer();
			return (InterceptorChain) jdks.unserialize(jdks.serialize(obj));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return null;
	}

	public static <T extends Serializable> Object copyObject(T obj) {
		if (obj == null) return obj;
		try {
			Serializer jdks = new JdkSerializer();
			return jdks.unserialize(jdks.serialize(obj));
		} catch (Exception e) {
			Logger.debug(e.getMessage(), e);
		}
		return null;
	}

	// exclude union,union all
	public static boolean isNotSupportUnionQuery() {
		return HoneyConfig.getHoneyConfig().sharding_notSupportUnionQuery || HoneyUtil.isSQLite();
	}

	public static <T> String getColumnNames(T entity) {
		Field fields[] = HoneyUtil.getFields(entity.getClass());
		String columnNames;
		String packageAndClassName = entity.getClass().getName();
		columnNames = HoneyContext.getBeanField(packageAndClassName);
		if (columnNames == null) {
			columnNames = HoneyUtil.getBeanField(fields, entity.getClass());
			HoneyContext.addBeanField(packageAndClassName, columnNames);
		}

		return columnNames;
	}

	public static String toTableName(Object entity) {
		if (entity instanceof Class) return _toTableNameByClass((Class<?>) entity); // fixed bug 2.1
		if (entity instanceof String) return _toTableName2((String) entity);// fixed bug 2.1
		return NameTranslateHandle.toTableName(NameUtil.getClassFullName(entity));
	}

	private static String _toTableName2(String entityName) {
		return NameTranslateHandle.toTableName(entityName);
	}

	private static String _toTableNameByClass(Class<?> c) {
		return NameTranslateHandle.toTableName(c.getName());
	}

	public static void setFieldValue(Field field, Object targetObj, Object value) throws IllegalAccessException {
		CoreUtil.setFieldValue(field, targetObj, value);
	}

	public static void setAccessibleTrue(Field field) {
		CoreUtil.setAccessibleTrue(field);
	}

	public static Object getValue(Field field, Object obj) throws IllegalAccessException {
		return field.get(obj);
	}

	public static <T> Field[] getFields(Class<T> entityClass) {
		Field fields[] = entityClass.getDeclaredFields();

		if (isOpenEntityCanExtend()) {// entity支持父类时,再添加
			Class<?> superClass = null;
			Class<?> currentClass = entityClass;
			do {
				superClass = currentClass.getSuperclass(); // 拿的是父类，而不是字段的值
				if (HoneyUtil.isSuperEntity(superClass)) {
					currentClass = superClass;
					fields = mergeSuperFields(fields, currentClass);
				} else {
					break;
				}
			} while (fields != null);
		}

		if (fields == null) fields = new Field[0];

		return fields;
	}

	/**
	 * 
	 * @param <T>
	 * @param entityClass
	 * @return
	 * @since 3.0
	 */
	public static <T> Map<String, Field> getNameAndField(Class<T> entityClass) {
		Field fields[] = entityClass.getDeclaredFields();

		if (isOpenEntityCanExtend()) {// entity支持父类时,再添加
			Class<?> superClass = null;
			Class<?> currentClass = entityClass;
			do {
				superClass = currentClass.getSuperclass(); // 拿的是父类，而不是字段的值
				if (HoneyUtil.isSuperEntity(superClass)) {
					currentClass = superClass;
					fields = mergeSuperFields(fields, currentClass);
				} else {
					break;
				}
			} while (fields != null);
		}

		if (fields == null) return new LinkedHashMap<>();

		Map<String, Field> map = new LinkedHashMap<>(fields.length);
		for (Field field : fields) {
			if ("serialVersionUID".equals(field.getName())) continue;
			map.put(field.getName(), field);
		}
		return map;
	}

	private static <T> Field[] mergeSuperFields(Field oldFields[], Class<T> superClass) {
		Field superFields[] = superClass.getDeclaredFields();
		if (superFields == null || superFields.length == 0) return oldFields;

		int oldFieldsLength = oldFields.length;
		int superFieldsLength = superFields.length;
		Field[] mergedFields = Arrays.copyOf(oldFields, oldFieldsLength + superFieldsLength);
		System.arraycopy(superFields, 0, mergedFields, oldFieldsLength, superFieldsLength);

		return mergedFields;
	}

	public static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) {
			if (isOpenEntityCanExtend()) {
				Class<?> superClass = clazz.getSuperclass(); // 拿的是父类，而不是字段的值
				if (isSuperEntity(superClass)) {
					return getField(superClass, fieldName);
				}
			}
			throw e;
		}
	}

	public static boolean isSuperEntity(Class<?> superClass) {
		return superClass != null && superClass != Object.class && (!superClass.getName().startsWith("java.")
				&& !superClass.getName().startsWith("javax.") && !superClass.getName().startsWith("jakarta."));
	}

	private static boolean isOpenEntityCanExtend() {
		return HoneyConfig.getHoneyConfig().openEntityCanExtend;
	}

	static String toCloumnNameForPks(String pkName, Class entityClass) {
		return toColumnName(pkName, entityClass); // 返回id的,只支持一个主键
	}

	private static char ch[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789XY".toCharArray();// 64
	private static final String START_MARK = "[$#(";
	private static final String END_MARK = ")#$]";
	private static int index = 0;
	private static int MAX = 1 << 30;

	public static String getRandomPrefix() {
		String s = Math.random() + "";
		if (index > MAX) index = 0;
		index++;
		try {
			return START_MARK + s.substring(2, 13) + ch[index % 64] + END_MARK; // len 20
		} catch (StringIndexOutOfBoundsException e) { // Exception in thread "main" java.lang.StringIndexOutOfBoundsException:
														// String index out of range: 13
			return getRandomPrefix();
		}
	}

	@SuppressWarnings("rawtypes")
	public static String getPkName(Class c) {
		try {
			HoneyUtil.getField(c, "id"); // V1.11 因主键可以不是默认id,多了此步检测
			return "id";
		} catch (NoSuchFieldException e) {
			String pkName = HoneyUtil.getPkFieldNameByClass(c);
			if ("".equals(pkName)) throw new ObjSQLException("No primary key in " + c.getName());
			if (pkName.contains(",")) throw new ObjSQLException(
					"method of selectById just need one primary key, but more than one primary key in " + c.getName());

			return pkName;
		}
	}

	@SuppressWarnings("rawtypes")
	public static boolean isSerial(Object rs) {
		if (rs instanceof List) {
			try {
				List list = (List) rs;
				if (list != null && list.size() > 0 && list.get(0) != null) {
					return Serializable.class.isAssignableFrom(list.get(0).getClass());
				}
			} catch (Exception e) {
				Logger.debug(e.getMessage(), e);
			}
		}

		return Serializable.class.isAssignableFrom(rs.getClass());
	}

//	// 专门用于Bee框架输出SQL日志.
//	static void logSQL(String hardStr) {
////		LogSqlParse.logSQL(hardStr);
//		Logger.logSQL(hardStr);
//	}
	// 专门用于Bee框架输出SQL日志.
//	static void logSQL(String hardStr, String sql) {
////		LogSqlParse.logSQL(hardStr, sql);
//		Logger.logSQL(LogSqlParse.parseSql(hardStr, sql));
//	}

}
