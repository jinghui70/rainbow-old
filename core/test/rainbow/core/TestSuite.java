package rainbow.core;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import rainbow.core.util.TestPinYin;
import rainbow.core.util.TestTemplate;
import rainbow.core.util.TestUtils;
import rainbow.core.util.ioc.TestIOC;

@RunWith(Suite.class)
@SuiteClasses({ TestUtils.class, TestIOC.class, TestPinYin.class, TestTemplate.class })
public class TestSuite {

}
