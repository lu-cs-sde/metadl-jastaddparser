import java.io.File;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestJastAddParser {
		
	private static final String TEST_ROOT = "testcases";
	private static final String TMP_ROOT = "gen";
	private String testName;
	
	public TestJastAddParser(String testDir) {
		this.testName = testDir;
	}
	
	@Test
	public void runTest() {
		TestRunner.runTest(TEST_ROOT, testName, TMP_ROOT);
	}
	
	@Parameters(name = "{0}")
	public static Collection<Object[]> getTests() {
		return TestRunner.getTests(new File(TEST_ROOT));
	}
	
}
