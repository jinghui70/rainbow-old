package rainbow.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

public class TestUtils {

    @Test
    public void testIsNullOrEmptyCollectionOfQ() {
        assertTrue(Utils.isNullOrEmpty((Collection<?>) null));
        assertTrue(Utils.isNullOrEmpty(new ArrayList<Object>()));
    }

    @Test
    public void testHasContent() {
        assertFalse(Utils.hasContent((String) null));
        assertFalse(Utils.hasContent(""));
        assertFalse(Utils.hasContent(" "));
        assertFalse(Utils.hasContent("\t"));
        assertFalse(Utils.hasContent("\t \t"));
        assertTrue(Utils.hasContent("\ta \t"));
    }

    @Test
    public void testSplit() {
        assertEquals(0, Utils.split("", ',').length);
        assertArrayEquals(new String[] { "afff" }, Utils.split("afff", '|'));
        assertArrayEquals(new String[] { "af", "ff" }, Utils.split("af|ff", '|'));
        assertArrayEquals(new String[] { "afff", "" }, Utils.split("afff|", '|'));
        assertArrayEquals(new String[] { "", "afff", "" }, Utils.split("|afff|", '|'));
        assertArrayEquals(new String[] { "", "af", "ff", "" }, Utils.split("|af|ff|", '|'));
        assertArrayEquals(new String[] { "", "af", "", "ff", "" }, Utils.split("|af||ff|", '|'));
    }
    
    public void testTrimString() {
    	String a = " hello\t kitty\r\n";
    	assertEquals("hellokitty", Utils.trimBlank(a));
    	assertEquals(" hello kitty", Utils.trimString(a, '\t', '\r', '\n'));
    }
}
