package org.teasoft.honey.osql.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import org.teasoft.bee.osql.BeeSQLException;
import org.teasoft.bee.osql.transaction.Transaction;
import org.teasoft.bee.osql.transaction.TransactionIsolationLevel;
import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.SessionFactory;

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
		c = SessionFactory.getConnection();
		return c;
	}

	@Override
	public void begin() {
		try {
			this.conn = initOneConn();

			setOldAutoCommit(conn.getAutoCommit());
			conn.setAutoCommit(false);

			HoneyContext.setCurrentConnection(this.conn); //存入上下文

			isBegin = true;
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public void commit() {
		if (!isBegin) throw new BeeSQLException("The Transaction did not to begin!");
		try {
			if (conn != null && !conn.getAutoCommit()) {
				conn.commit();
				if (oldAutoCommit != conn.getAutoCommit()) conn.setAutoCommit(oldAutoCommit);
				close();
				isBegin = false;
			}
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public void rollback() {
		try {
			if (conn != null && !conn.getAutoCommit()) {
				conn.rollback();
				if (oldAutoCommit != conn.getAutoCommit()) conn.setAutoCommit(oldAutoCommit);
				close();
			}
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		try {
			conn.setReadOnly(readOnly);
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public void setTransactionIsolation(TransactionIsolationLevel level) {
		try {
			conn.setTransactionIsolation(level.getLevel());
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public boolean isReadOnly() {
		try {
			return conn.isReadOnly();
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public int getTransactionIsolation() {
		try {
			return conn.getTransactionIsolation();
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
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
				throw ExceptionHelper.convert(e);
			} finally {
				HoneyContext.removeCurrentConnection();
			}
		}
	}
}
