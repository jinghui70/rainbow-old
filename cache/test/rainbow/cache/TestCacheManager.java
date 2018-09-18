package rainbow.cache;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rainbow.cache.Cache;
import rainbow.cache.CacheLoader;
import rainbow.cache.CacheManager;
import rainbow.cache.internal.CacheManagerImpl;

public class TestCacheManager {

    public static CacheManager manager;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        manager = new CacheManagerImpl();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        final AtomicInteger time = new AtomicInteger(0);

        CacheLoader<Integer, String> loader = new CacheLoader<Integer, String>() {
            @Override
            public String load(Integer key) {
                time.incrementAndGet();
                return key.toString() + "T";
            }
        };
        Cache<Integer, String> cache = manager.createCache("test", loader);

        assertEquals("20T", cache.get(20));
        assertEquals(1, time.get());
        assertEquals("20T", cache.get(20));
        assertEquals(1, time.get());
        cache.remove(20);
        assertEquals("20T", cache.get(20));
        assertEquals(2, time.get());
        
    }
}
