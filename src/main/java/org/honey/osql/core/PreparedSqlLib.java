package org.honey.osql.core;

import java.util.ArrayList;
import java.util.List;

import org.bee.osql.ObjSQLException;
import org.bee.osql.PreparedSQL;
import org.bee.osql.SQL;

/**
 * 支持带占位符(?)的sql操作.sql语句是DB能识别的SQL,非面向对象的sql.
 * 若是简单的操作,建议用面向对象的操作方式,ObjSQL和ObjSQLRich.
 * @author Kingstar
 * @since  1.0
 */
public class PreparedSqlLib implements PreparedSQL {

	private SQL sqlLib = BeeFactory.getHoneyFactory().getSQL();

	@Override
	public <T> List<T> select(String sql, T entity, Object[] preValues) {

		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib select SQL: ", sql);
		return sqlLib.select(sql, entity);
	}

	@Override
	public <T> List<T> selectSomeField(String sql, T entity, Object[] preValues) {

		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib selectSomeField SQL: ", sql);
		return sqlLib.selectSomeField(sql, entity);
	}

	@Override
	public String selectFun(String sql, Object[] preValues) throws ObjSQLException {

		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib selectFun SQL: ", sql);
		return sqlLib.selectFun(sql);
	}

	@Override
	public List<String[]> select(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib select SQL: ", sql);
		return sqlLib.select(sql);
	}

	@Override
	public int modify(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib modify SQL: ", sql);
		return sqlLib.modify(sql);
	}

	private void initPreparedValues(String sql, Object[] preValues) {

		PreparedValue preparedValue = null;
		List<PreparedValue> list = new ArrayList<>();
		StringBuffer valueBuffer = new StringBuffer();
		for (int i = 0, k = 0; i < preValues.length; i++) {
			preparedValue = new PreparedValue();
			preparedValue.setType(preValues[i].getClass().getName());
			preparedValue.setValue(preValues[i]);
			list.add(k++, preparedValue);

			valueBuffer.append(",");
			valueBuffer.append(preValues[i]);
		}

		if (valueBuffer.length() > 0) {
			valueBuffer.deleteCharAt(0);
			HoneyContext.setPreparedValue(sql, list);
			HoneyContext.setSqlValue(sql, valueBuffer.toString());
		}
	}

	@Override
	public String selectJson(String sql, Object[] preValues) {
		initPreparedValues(sql, preValues);
		Logger.logSQL("PreparedSqlLib selectJson SQL: ", sql);
		return sqlLib.selectJson(sql);
	}
}
