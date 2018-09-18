package rainbow.db;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import rainbow.db.dao.TestDao;
import rainbow.db.dao.TestIdDao;
import rainbow.db.dao.TestNeoBean;
import rainbow.db.dao.TestObjectDao;
import rainbow.db.dao.TestQuery;
import rainbow.db.incrementer.TestMaxIdIncrementer;
import rainbow.db.incrementer.TestTableIncrementer;

@RunWith(Suite.class)
@SuiteClasses({ TestNeoBean.class, //
		TestDao.class, //
		TestObjectDao.class, //
		TestIdDao.class, //
		TestMaxIdIncrementer.class, //
		TestTableIncrementer.class,
		TestQuery.class}) //
public class TestSuite {
}
