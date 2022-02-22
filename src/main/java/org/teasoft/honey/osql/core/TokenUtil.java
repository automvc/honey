package org.teasoft.honey.osql.core;

import java.util.Map;

import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.2
 */
public class TokenUtil {
	
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
						mapValue = mapValue.substring(0, 1).toUpperCase() + mapValue.substring(1, mapValue.length());
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

}
