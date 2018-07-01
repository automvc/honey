package org.honey.osql.core;

/**
 * @author KingStar
 * @since  1.0
 */
public final class SqlStrFilter {

	public static boolean checkFunSql(String sql, String funType) {

		sql = sql.trim().toLowerCase();
		funType = funType.trim().toLowerCase();

		if ("count".equals(funType) || funType.length() == 3) {
		} else {
			return true;// illegal
		}

		// select avg( 之间只能存在空格
		int a = sql.indexOf("select");
		int b = sql.indexOf(funType, a);

		if ("".equals(sql.substring(a + 6, b).trim())) {
			int c = sql.indexOf("(", b);
			if ("".equals(sql.substring(b + funType.length(), c).trim())) {
				return false;
			}
		}

		return true;// illegal
	}

}
