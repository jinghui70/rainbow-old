package rainbow.db.incrementer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rainbow.db.dao.Dao;
import rainbow.db.jdbc.JdbcUtils;
import rainbow.core.util.ioc.Inject;

import com.google.common.base.Throwables;

/**
 * 这是用一个table来维护的序列号生成器。这个table的两个字段名为FLAG, SEQ
 * 
 * FLAG用来标识一个序列号，可以是int,long,或者String类型.
 * 
 * 一般一个对象只需要生成id就够了，这时不用设置Flag，其值默认为0，即序列号表里只有一条记录。
 * 
 * 虽然一个序列号表可以支持多个对象的ID生成，比如Flag设置为对象名，即序列号表里有多条记录。但是不建议这样做，最好还是一个对象一个序列号表。
 * 
 * @author lijinghui
 * 
 */
public class TableIncrementer extends AbstractIncrementer {

    private final static Logger logger = LoggerFactory.getLogger(TableIncrementer.class);

    private Dao dao;

    private String tblName;

    private Object flag = Integer.valueOf(0);

    @Inject
    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public void setTblName(String tblName) {
        this.tblName = tblName;
    }

    public void setFlag(Object flag) {
        this.flag = flag;
    }

    /**
     * 作为Bean的构造函数
     */
    public TableIncrementer() {
    }

    /**
     * 构造函数
     * 
     * @param dao
     * @param tblName
     */
    public TableIncrementer(Dao dao, String tblName) {
        this.dao = dao;
        this.tblName = tblName;
    }

    /**
     * 构造函数
     * 
     * @param dao
     * @param tblName
     */
    public TableIncrementer(Dao dao, String tblName, Object flag) {
        this.dao = dao;
        this.tblName = tblName;
        this.flag = flag;
    }

    @Override
    protected long getNextKey() {
        long nextId = -1;
        Connection conn = null;
        try {
            conn = dao.getJdbcTemplate().getDataSource().getConnection();
        } catch (SQLException e) {
            JdbcUtils.closeConnection(conn);
            logger.error("getConnection failed when getNextId of [{}]", tblName, e);
            Throwables.propagate(e);
        }
        try {
            conn.setAutoCommit(false);
            PreparedStatement statementCount = conn.prepareStatement("select count(1) from " + tblName
                    + " where FLAG=?");
            statementCount.setObject(1, flag);
            ResultSet rs = statementCount.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            statementCount.close();

            if (count == 0) {
                PreparedStatement statementInsert = conn.prepareStatement("insert into " + tblName + " values(?, 1)");
                statementInsert.setObject(1, flag);
                statementInsert.execute();
                nextId = 1;
                statementInsert.close();
            } else {
                PreparedStatement statementUpdate = conn.prepareStatement("update " + tblName
                        + " set SEQ=SEQ+1 where FLAG=?");
                statementUpdate.setObject(1, flag);
                statementUpdate.execute();
                statementUpdate.close();

                PreparedStatement statementSelect = conn.prepareStatement("select SEQ from " + tblName
                        + " where FLAG=?");
                statementSelect.setObject(1, flag);
                rs = statementSelect.executeQuery();
                rs.next();
                nextId = rs.getLong(1);
                rs.close();
                statementSelect.close();
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
            }
            logger.error("get next id of [{}] failed", tblName, e);
            Throwables.propagate(e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("restore auto commit failed", e);
            }
            JdbcUtils.closeConnection(conn);
        }
        return nextId;
    }

}
