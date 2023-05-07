package org.teasoft.honey.util;

/**
 * 字符解析
 * @author Kingstar
 * @since 2.1
 */
public class StringParser {
	//通过这个和getKeyEndPositionByStartEnd,可以很容易改写出指定动态开始关键字的查找方法
	// 查找({开头，} )结束的结束关键字开始下标的位置。两个标识之间可以有空格。
	public static int getEndPosition(String str) {
		str = str.trim();
		char[] array = str.toCharArray();
		int len = array.length;
		boolean isStart = false;

		int single = -1;
		int dou = -1;

		boolean isComm1 = false;
		boolean isComm2 = false;

		for (int i = 0; i < len; i++) {

			if (!isComm1 && !isComm2) {
				if (array[i] == '\'') {
					single *= -1;
					continue;
				} else if (array[i] == '\"') {
					dou *= -1;
					continue;
				} else if (!(single == -1 && dou == -1)) { // 在引号内跳过
					continue;
				}
			}

			if (isComm1 && (array[i] == '\n' || array[i] == '\r'   )) { // 单行注释结束
				isComm1 = false;
			}
			if (isComm2 && array[i] == '*') {
				if (i + 1 < len && array[i + 1] == '/') {// 多行注释结束
					isComm2 = false;
				}
			}

			if (isComm1 || isComm2) continue; // 注释还没结束则跳过

			if (array[i] == '/') {
				if (i + 1 < len && array[i + 1] == '/') { // 单行注释开始
					isComm1 = true;
					i++;
					continue;
				}
			}

			if (array[i] == '/') {
				if (i + 1 < len && array[i + 1] == '*') { // 多行注释开始
					isComm2 = true;
					i++;
					continue;
				}
			}

			if (!isStart && array[i] == '(') {
				for (int k = i + 1; k < len; k++) {
					if (array[k] == '{') {
						isStart = true;
						i = k;
						break;
//					} else if (array[k] == ' ' || array[k] == '\t' || array[k] == '\n') {
					} else if (isSeprate(array[k])) {
						continue;
					} else {
						isStart = false;
						i = k;
						break;
					}
				} // end for k

				if (isStart) continue; // 首次查到后返回
			}
			if (isStart) {
				if (array[i] == '}') {
					for (int k = i + 1; k < len; k++) {
						if (array[k] == ')') {
							i = k;
							return k;
						} else if (isSeprate(array[k])) {
							continue;
						} else {
							break;
						}
					} // end for k
				}
			}
		} // end for
		return -1;
	}
   
	// 从指定位置开始，可以避开开始的引号
	public static int getKeyPosition(String str, String key, int fromIndex) {
		if (str != null && str.length() > fromIndex) str = str.substring(fromIndex);
		return getKeyPosition(str, key);
	}

	// 查找str中key的位置，但引号内的跳过; 不在双号内查找, 其它位置查找是严格匹配(目标字符有分隔符则不匹配)
	public static int getKeyPosition(String str, String key) {
		return _getKeyPosition(str, key, false);
	}

	// 返回key结束的位置； key之间可以有空陋,所以返回的下标是key首字符的位置
	public static int getKeyPosition2(String str, String key) {
		return _getKeyPosition(str, key, true);
	}
 
 //返回key结束的位置； key之间可以有空陋,所以返回的下标是key首字符的位置
	private static int _getKeyPosition(String str, String key, boolean isAllowSeprate) {
		if (StringUtils.isBlank(str) || StringUtils.isBlank(key)) return -1;

		int r = -1;
		char[] array = str.toCharArray();
		int len = array.length;
		char[] keyChar = key.toCharArray();
		int single = -1;
		int dou = -1;

		for (int i = 0; i < len; i++) {
			if (array[i] == '\'') {
				single *= -1;
				continue;
			} else if (array[i] == '\"') {
				dou *= -1;
				continue;
			} else if (!(single == -1 && dou == -1)) { // 在引号内跳过
				continue;
			}

			if (array[i] == keyChar[0]) {
				r = i;
				if (keyChar.length == 1) return r;

				for (int k = 1; k < keyChar.length; k++) {
					if (isAllowSeprate) { //允许分隔符则跳过
							while (i + k < len && isSeprate(array[i + k]))
							i++;
					}
					if (i + k < len && array[i + k] == keyChar[k]) {
						if (k + 1 == keyChar.length) return r;
						continue;
					} else {
						break;
					}
				}
			}
		}
		return -1;
	}
   
	// 找开始符号对应的结束符号的位置，支持嵌套
	// 如:开始是{, 找对应的}; 在字符串里的不算入
	public static int getKeyEndPositionByStartEnd(String str, char start, char end) {
		if (StringUtils.isBlank(str)) return -1;
		char[] array = str.toCharArray();
		int len = array.length;
		int single = -1;
		int dou = -1;
		int startCount = -1; // 计数遇到的开始标识,在引号里的不计

		for (int i = 0; i < len; i++) {
			if (array[i] == '\'') {
				single *= -1;
				continue;
			} else if (array[i] == '\"') {
				dou *= -1;
				continue;
			} else if (!(single == -1 && dou == -1)) { // 在引号内跳过
				continue;
			}

			if (array[i] == start) {
				startCount++;
				continue;
			}
			if (array[i] == end) {
				startCount--;
				if (startCount == -1) return i;
			}
		}
		return -1;
	}

	public static String removeComment(String str) {
		if (StringUtils.isBlank(str)) return str;
		
		str = str.trim();
		char[] array = str.toCharArray();
		int len = array.length;
		int single = -1;
		int dou = -1;
		boolean isComm1 = false;
		boolean isComm2 = false;
		int start = -1;
		int end = -1;

		for (int i = 0; i < len; i++) {
			if (!isComm1 && !isComm2) {
				if (array[i] == '\'') {
					single *= -1;
					continue;
				} else if (array[i] == '\"') {
					dou *= -1;
					continue;
				} else if (!(single == -1 && dou == -1)) { // 在引号内跳过
					continue;
				}
			}

			if (isComm1 && (array[i] == '\n' || array[i] == '\r'   ) ) { // 单行注释结束
				isComm1 = false;
				end = i;
				break;
			}
			if (isComm2 && array[i] == '*') {
				if (i + 1 < len && array[i + 1] == '/') {// 多行注释结束
					isComm2 = false;
					end = i + 2;
					break;
				}
			}

			if (isComm1 || isComm2) continue; // 注释还没结束则跳过

			if (array[i] == '/') {
				if (i + 1 < len && array[i + 1] == '/') { // 单行注释开始
					isComm1 = true;
					start = i;
					i++;
					continue;
				}
			}

			if (array[i] == '/') {
				if (i + 1 < len && array[i + 1] == '*') { // 多行注释开始
					isComm2 = true;
					start = i;
					i++;
					continue;
				}
			}
		} // end for

		if (start > 0 && end > 0 && end > start) {
			StringBuffer sb = new StringBuffer(str);
			sb.delete(start, end);
			return removeComment(sb.toString());
		} else {
			return str;
		}
	}
	
	private static boolean isSeprate(char ch) {
		return (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r');
	}

}
