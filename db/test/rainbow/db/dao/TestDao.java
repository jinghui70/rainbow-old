package rainbow.db.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rainbow.db.DBTest;
import rainbow.db.dao.memory.MemoryDao;
import rainbow.db.dao.object._Gender;
import rainbow.db.dao.object._Person;

import com.google.common.collect.Lists;

public class TestDao {

	private static MemoryDao dao;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		dao = DBTest.createMemoryDao(TestIdDao.class.getResource("object/test.rdm"));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		dao.destroy();
	}

	@Before
	public void setUp() throws Exception {
		dao.getJdbcTemplate().getTransactionManager().beginTransaction();
	}

	@After
	public void tearDown() throws Exception {
		dao.getJdbcTemplate().getTransactionManager().rollback();
	}

	private _Person createPerson(int id) {
		_Person p = new _Person();
		p.setId(id);
		p.setName("1");
		p.setBirthday(new LocalDate(1970, 1, 27));
		p.setGender(_Gender.女);
		p.getScore()[0] = 5;
		p.getScore()[1] = 4;
		p.getScore()[2] = 3;
		return p;
	}

	private void assertPerson(_Person p, int id) {
		assertEquals(Integer.valueOf(id), p.getId());
		assertEquals("1", p.getName());
		assertEquals(new LocalDate(1970, 1, 27), p.getBirthday());
		assertEquals(_Gender.女, p.getGender());
		assertEquals(Integer.valueOf(5), p.getScore()[0]);
		assertEquals(Integer.valueOf(4), p.getScore()[1]);
		assertEquals(Integer.valueOf(3), p.getScore()[2]);
	}

	@Test
	public void test() {
		_Person p = createPerson(10);
		dao.insert(p);
		assertEquals(1, dao.count("_Person"));

		dao.delete(p);
		assertEquals(0, dao.count("_Person"));

		dao.insert(p);
		p = createPerson(20);
		dao.replace(p);
		assertEquals(2, dao.count("_Person"));

		p = dao.fetch(_Person.class, 20);
		assertPerson(p, 20);
		p.getScore()[0] = 100;
		p.getScore()[1] = 100;
		p.getScore()[2] = 100;

		dao.replace(p);
		assertEquals(2, dao.count("_Person"));
		p = dao.fetch(_Person.class, 20);
		assertEquals(Integer.valueOf(100), p.getScore()[0]);
		assertEquals(Integer.valueOf(100), p.getScore()[1]);
		assertEquals(Integer.valueOf(100), p.getScore()[2]);

		p = createPerson(20);
		p.getScore()[0] = 0;
		p.getScore()[1] = 10;
		p.getScore()[2] = 20;
		dao.insertUpdate(p, false);
		p = dao.fetch(_Person.class, 20);
		assertEquals(Integer.valueOf(100), p.getScore()[0]);
		assertEquals(Integer.valueOf(90), p.getScore()[1]);
		assertEquals(Integer.valueOf(80), p.getScore()[2]);

		p.getScore()[0] = 0;
		p.getScore()[1] = 10;
		p.getScore()[2] = 20;
		dao.insertUpdate(p, true, "score.1", "score.2", "score.3");
		p = dao.fetch(_Person.class, 20);
		assertEquals(Integer.valueOf(100), p.getScore()[0]);
		assertEquals(Integer.valueOf(100), p.getScore()[1]);
		assertEquals(Integer.valueOf(100), p.getScore()[2]);
	}

	@Test
	public void testBatch() {
		List<_Person> list = Lists.newArrayList();
		for (int i = 1; i <= 10; i++) {
			list.add(createPerson(i));
		}
		dao.insert(list, 5, null);
		assertEquals(10, dao.count("_Person"));
	}
}
