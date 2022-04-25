package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.token.CustomAutoSqlToken;
import org.teasoft.honey.util.StringUtils;

/**
 * Token工具类.Token Util.
 * @author Kingstar
 * @since  1.2
 */
public class TokenUtil {

	private static final String SPACE = " ";

	private TokenUtil() {}

	static SqlValueWrap process(String text, String startToken, String endToken, String replaceStr) {

		if (StringUtils.isEmpty(text)) {
			return null;
		}
		int start = text.indexOf(startToken);
		if (start < 0) return null;

		SqlValueWrap wrap = new SqlValueWrap();

		StringBuffer sbf = new StringBuffer(text);
		StringBuffer value = new StringBuffer();
		int end;
		int len1 = startToken.length();
		int len2 = endToken.length();
		int len3 = replaceStr == null ? 0 : replaceStr.length();
		while (start > -1) {
			if (start > 0 && sbf.charAt(start - 1) == '\\') {
				sbf.deleteCharAt(start - 1);
				start = sbf.indexOf(startToken, start + len1);
				continue;
			} else {
				end = sbf.indexOf(endToken, start);
				if (end > 0) {
					value.append(",");
					value.append(sbf.substring(start + len1, end));
//			        if(replaceStr!=null) sbf.replace(start, end+1,replaceStr);
					if (replaceStr != null) sbf.replace(start, end + len2, replaceStr); //v1.9 //replaceStr为null,则不替换
				}
			}
			if (replaceStr != null) {
				start = sbf.indexOf(startToken, start + len3);
			} else {
				start = sbf.indexOf(startToken, end + len2);
			}
		}

		if (value.length() > 0) value.deleteCharAt(0);

		wrap.setSql(sbf.toString());
		wrap.setValueBuffer(value); //just for map's key

		return wrap;
	}

	//@since 1.7.2 
	//v1.9
	public static String processWithMap(String text, String startToken, String endToken, Map<String, String> map) {

		if (StringUtils.isEmpty(text) || map == null) {
			return text; //return original
		}
		int start = text.indexOf(startToken);
		if (start < 0) return text; //return original

		StringBuffer sbf = new StringBuffer(text);
		int end;
		int len1 = startToken.length();
		int len2 = endToken.length();
		int len3 = 0;
		String key = "";
		String mapValue = null;
		while (start > -1) {
			if (start > 0 && sbf.charAt(start - 1) == '\\') {
				sbf.deleteCharAt(start - 1);
				start = sbf.indexOf(startToken, start + len1);
				continue;
			} else {
				end = sbf.indexOf(endToken, start);
				if (end > 0) {
					key = sbf.substring(start + len1, end);
					if (key.endsWith("?up1")) { //#{entityName?up1}  entityName upper case 1st letter 
						key = key.substring(0, key.length() - 4);
						mapValue = map.get(key);
						mapValue = mapValue.substring(0, 1).toUpperCase()
								+ mapValue.substring(1, mapValue.length());
					} else {
						mapValue = map.get(key);
					}

					if (mapValue != null) {
						//						sbf.replace(start, end + 1, mapValue);
						sbf.replace(start, end + len2, mapValue);//v1.9
						len3 = mapValue.length();
					}
				} else {
					return sbf.toString(); //can not find , return  //v1.9
				}
			}
			if (mapValue != null) {
				start = sbf.indexOf(startToken, start + len3);
			} else {
				start = sbf.indexOf(startToken, start + len1); //没找到key和没找到结束标签都应该从所找到开始标签的下一个位置开始,这样更加安全
				//				if(end>0) {
				////				   start = sbf.indexOf(startToken, end + len2);
				//				   start = sbf.indexOf(startToken, start + len1); 
				//				}else {
				//				  start = sbf.indexOf(startToken, start + len1);
				//				}
			}

			len3 = 0; //reset
			mapValue = null; //v1.9
		}
		return sbf.toString();
	}

	static SqlValueWrap process2(String text, String startToken, String endToken, String replaceStr,
			Map map) {

		if (StringUtils.isEmpty(text)) {
			return null;
		}
		int start = text.indexOf(startToken);
		if (start < 0) return null;

		SqlValueWrap wrap = new SqlValueWrap();
		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();

		StringBuffer sbf = new StringBuffer(text.trim());

		doProcessJudgeToken(sbf, map);

		start = sbf.indexOf(startToken); //处理判断标签后,要重新计算

		int end;
		int len1 = startToken.length();
		int len2 = endToken.length();
		int len3 = replaceStr == null ? 0 : replaceStr.length();
		int len3Init = len3;
		int goBack = 0;

		String key = "";
		String newReplaceStr = "";
		boolean isInToken = false;
		boolean isToIsNULL = false;
		while (start > -1) {
			if (start > 0 && sbf.charAt(start - 1) == '\\') {
				sbf.deleteCharAt(start - 1);
				start = sbf.indexOf(startToken, start + len1);
				continue;
			} else {
				end = sbf.indexOf(endToken, start);
				if (end > 0) {
					key = sbf.substring(start + len1, end);
					len3 = len3Init;
					isInToken = false;
					isToIsNULL = false;
					goBack = 0;

					preparedValue = new PreparedValue();
					if (key.contains("%")) {
						Object v = processPecent(key, map);
						preparedValue.setValue(v);
						preparedValue.setType(v.getClass().getName());
						list.add(preparedValue);
					} else if (key.endsWith(CustomAutoSqlToken.atIn)) {
						//						System.err.println("process :  @in!");
						//						Object objIn = map.get(key.substring(0, key.length() - 3));
						String keyIn = key.replace(CustomAutoSqlToken.atIn, "").trim();
						Object objIn = map.get(keyIn);
						if (objIn != null && (List.class.isAssignableFrom(objIn.getClass())
								|| Set.class.isAssignableFrom(objIn.getClass()))) {
							Collection<?> c = (Collection<?>) objIn;
							for (Object e : c) {
								setPreValue(e, list);
							}
							isInToken = true;
							newReplaceStr = HoneyUtil.getPlaceholderValue(c.size());
							len3 = newReplaceStr.length();
						}

					} else if (key.endsWith(CustomAutoSqlToken.toIsNULL1)
							|| key.endsWith(CustomAutoSqlToken.toIsNULL2)) {
						//						int len=CustomAutoSqlToken.toIsNULL1.length();
						//						Object v = map.get(key.substring(0, key.length() - len));
						String keyIsNull = key.replace(CustomAutoSqlToken.toIsNULL1, "")
								.replace(CustomAutoSqlToken.toIsNULL2, "").trim();
						Object v = map.get(keyIsNull);
						if (v == null) {
							isToIsNULL = true;
							newReplaceStr = SPACE + K.isNull + SPACE;
						} else {
							setPreValue(v, list);
						}
						if (key.endsWith(CustomAutoSqlToken.toIsNULL1))
							goBack = -1;
						else
							goBack = -2;
					} else {
						Object value = map.get(key);
						setPreValue(value, list);
					}
					if (isInToken || isToIsNULL)
						sbf.replace(start + goBack, end + len2, newReplaceStr);
					else if (replaceStr != null) sbf.replace(start, end + len2, replaceStr); //v1.9 //replaceStr为null,则不替换
				}
			}
			if (replaceStr != null || isInToken || isToIsNULL) {
				start = sbf.indexOf(startToken, start + len3);
			} else {
				start = sbf.indexOf(startToken, end + len2);
			}

			len3 = 0; //reset

		} //end while

		wrap.setList(list);
		wrap.setSql(sbf.toString());

		return wrap;
	}

	private static void setPreValue(Object value, List<PreparedValue> list) {
		PreparedValue preparedValue = new PreparedValue();
		preparedValue.setValue(value);
		preparedValue.setType(value.getClass().getName());
		list.add(preparedValue);
	}

	private static Object processPecent(String key, Map map) {
		int len = key.length();
		Object value = "";
		if (key.startsWith("%")) {
			if (key.endsWith("%")) { //    %para%
				key = key.substring(1, len - 1);
				value = "%" + map.get(key) + "%";
			} else { //   %para
				key = key.substring(1, len);
				value = "%" + map.get(key);
			}
		} else if (key.endsWith("%")) { //  para%
			key = key.substring(0, len - 1);
			value = map.get(key) + "%";
		} else {
			value = map.get(key);
		}
		return value;
	}

	static String getKey(String text, String startToken, String endToken) {

		if (StringUtils.isEmpty(text)) {
			return "";
		}
		int start = text.indexOf(startToken);
		if (start < 0) return "";

		StringBuffer sbf = new StringBuffer(text);
		int end;
		int len1 = startToken.length();
		int len2 = endToken.length();
		while (start > -1) {
			if (start > 0 && sbf.charAt(start - 1) == '\\') {
				sbf.deleteCharAt(start - 1);
				start = sbf.indexOf(startToken, start + len1);
				continue;
			} else {
				end = sbf.indexOf(endToken, start);
				if (end > 0) {
					return sbf.substring(start + len1, end);
				}
			}
			start = sbf.indexOf(startToken, end + len2);
		}

		return "";
	}

	static TokenStruct getKeyStruct(StringBuffer sbf, String startToken, String endToken) {

		if (sbf == null) {
			return null;
		}
		int start = sbf.indexOf(startToken);
		if (start < 0) return null;

		TokenStruct struct = new TokenStruct();
		int end;
		int len1 = startToken.length();
		int len2 = endToken.length();
		while (start > -1) {
			if (start > 0 && sbf.charAt(start - 1) == '\\') {
				sbf.deleteCharAt(start - 1);
				start = sbf.indexOf(startToken, start + len1);
				continue;
			} else {
				end = sbf.indexOf(endToken, start);
				if (end > 0) {
					struct.key = sbf.substring(start + len1, end);
					struct.start = start;
					struct.end = end;
					return struct;
				}
			}
			start = sbf.indexOf(startToken, end + len2);
		}

		return null;
	}

	static void doProcessJudgeToken(StringBuffer sbf, Map map) {
		processJudgeToken(sbf, CustomAutoSqlToken.isNotNull, CustomAutoSqlToken.endIf, map);
		processJudgeToken(sbf, CustomAutoSqlToken.isNotBlank, CustomAutoSqlToken.endIf, map);
	}

	static StringBuffer processJudgeToken(StringBuffer sbf, String startToken, String endToken, Map map) {
		//		eg:"select * from orders where <if isNotNull> userid in #{userid@in}"
		int len1 = startToken.length();
		int len2 = endToken.length();
		TokenStruct struct = getKeyStruct(sbf, startToken, endToken);
		if (struct != null) {
			String key1 = struct.key;
			//			System.err.println(key1); //userid in #{userid@in}
			String key2 = getKey(key1, "#{", "}");
			//			System.err.println(key2);//userid @in
			//去除%,@in ...
			if (StringUtils.isNotBlank(key2)) {
				String key3 = key2.replace("%", "").replace(CustomAutoSqlToken.atIn, "")
						.replace(CustomAutoSqlToken.toIsNULL1, "")
						.replace(CustomAutoSqlToken.toIsNULL2, "");

				Object v = map.get(key3.trim());
				if ((v == null && CustomAutoSqlToken.isNotNull.equals(startToken))
						|| ((v instanceof String) && (StringUtils.isBlank((String) v))
								&& CustomAutoSqlToken.isNotBlank.equals(startToken))) {
					sbf.replace(struct.start, struct.end + len2, "");
				} else {
					sbf.replace(struct.start, struct.start + len1, "");
					sbf.replace(struct.start + key1.length(), struct.start + key1.length() + len2, "");
				}
			}

			//			System.err.println(sbf.toString());

			return processJudgeToken(sbf, startToken, endToken, map); //loop
		} else {
			String sql = sbf.toString().trim();
			String sql2 = sql.toLowerCase();
			if (sql2.endsWith("where")) { //delete the last where and empty string
				//				String sql3=sql2.substring(0,sql2.length()-5).trim();
				//				 sbf=new StringBuffer(sql3);  //不能新定义
				return sbf.replace(sql2.length() - 5, sbf.length(), "");
			} else {
				return sbf;
			}
		}

	}

	private static class TokenStruct {
		String key;
		int start;
		int end;
	}

	//	public static void main(String[] args) {
	//		//		String sql="select * from orders where <if isNotNull>userid in #{userid@in}</if>";
	//		String sql = "select * from orders where <if isNotNull> userid in #{userid@in}</if>  <if isNotNull>and name=#{name}</if>";
	//		sql = sql.trim();
	//		StringBuffer sbf = new StringBuffer(sql);
	//		Map map = new HashMap();
	//		map.put("userid", "has");
	//		//					map.put("userid0", "has");
	//		//			map.put("userid", "");
	//
	//		map.put("name", "has");
	//		//		map.put("name0", "has");
	//
	//		doProcessJudgeToken(sbf, map);
	//		System.err.println(sbf.toString());
	//	}

}
