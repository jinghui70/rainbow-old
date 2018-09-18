package rainbow.db.incrementer;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rainbow.db.DBTest;
import rainbow.db.dao.memory.MemoryDao;

public class TestTableIncrementer {

    private static MemoryDao dao;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        dao = DBTest.createMemoryDao(TestTableIncrementer.class.getResource("test.rdm"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    	dao.destroy();
    }

    @Test
    public void testNextIntValue() {
        Incrementer incrementer = new TableIncrementer(dao, "T_INT_SEQ");
        assertEquals(1, incrementer.nextIntValue());
        assertEquals(2, incrementer.nextIntValue());
        assertEquals(3, incrementer.nextIntValue());
    }

    @Test
    public void testNextLongValue() {
        Incrementer incrementer1 = new TableIncrementer(dao, "T_LONG_SEQ", "XX");
        assertEquals(1, incrementer1.nextIntValue());
        assertEquals(2, incrementer1.nextIntValue());

        Incrementer incrementer2 = new TableIncrementer(dao, "T_LONG_SEQ", "YY");
        assertEquals(1, incrementer2.nextIntValue());
        assertEquals(2, incrementer2.nextIntValue());

        assertEquals(3, incrementer1.nextIntValue());
        assertEquals(3, incrementer2.nextIntValue());
    }

}