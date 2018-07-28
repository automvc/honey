package org.honey.osql.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import org.bee.osql.ObjSQLException;
import org.bee.osql.transaction.Transaction;
import org.bee.osql.transaction.TransactionIsolationLevel;
import org.honey.osql.core.HoneyContext;
import org.honey.osql.core.SessionFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class JdbcTransaction implements Transaction {

	private Connection conn = null;
	private boolean oldAutoCommit;
	private boolean isBegin = false;

	private Connection initOneConn() {

		Connection c = null;
		try {
			c = SessionFactory.getConnection();
		} catch (ObjSQLException e) {
			// TODO: handle exception
			System.err.println(e.getMessage());
		}
		return c;
	}

	@Override
	public void begin() throws SQLException {

		this.conn = initOneConn();

		setOldAutoCommit(conn.getAutoCommit());
		conn.setAutoCommit(false);

		HoneyContext.setCurrentConnection(this.conn); //存入上下文

		isBegin = true;
	}

	@Override
	public void commit() throws SQLException {
		if (!isBegin) throw new SQLException("The Transaction did not to begin!");

		if (conn != null && !conn.getAutoCommit()) {
			conn.commit();
			if (oldAutoCommit != conn.getAutoCommit()) conn.setAutoCommit(oldAutoCommit);
			close();
			isBegin = false;
		}
	}

	@Override
	public void rollback() throws SQLException {
		if (conn != null && !conn.getAutoCommit()) {
			conn.rollback();
			if (oldAutoCommit != conn.getAutoCommit()) conn.setAutoCommit(oldAutoCommit);
			close();
		}
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		conn.setReadOnly(readOnly);
	}

	@Override
	public void setTransactionIsolation(TransactionIsolationLevel level) throws SQLException {
		conn.setTransactionIsolation(level.getLevel());
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return conn.isReadOnly();
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return conn.getTransactionIsolation();
	}

	@Override
	public void setTimeout(int second) {
		//TODO
	}

	private void setOldAutoCommit(boolean oldAutoCommit) {
		this.oldAutoCommit = oldAutoCommit;
	}

	private void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				System.err.println("-----------SQLException in checkClose------" + e.getMessage());
			} finally {
				HoneyContext.removeCurrentConnection();
			}
		}
	}
}
