package demoJS;

class Testx {
	// example of a second independent top-level class.
	// It will get its own .js file.
}

public class Test_ {

	static public boolean j2sHeadless = true;

	private static int i_ = 0;

	private int t = 0;

	public void test123(int a, int b, int c) {
		
	}
	public int test_int = 3;
	
	public int showt() {
		if (true && (/**@j2sNative 1? test : */false)) {
			
		}
		System.out.println("Test_.showt() " + t);
		if (false || (/**@j2sNative test || */false)) {
			
		}
		return t;
	}
	
	public void setT(int t) {
		this.t = t;
	}

	public String t_test = "test_";

	public String showt2() {
		System.out.println("Test_.showt2() test_.t is " + t + " t_test is " + t_test);
		return t_test;
	}

	static {
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
	}

	class InnerClass {
		// A check that inner classes can access outer private methods
		// and fields
		public void testInner() {
			test3();
		}
	}

	private int test3() {
		// Some random tests

		{
			long j = 1;
			int i = 3;
			j &= ~(1L << i);
			j = 1L << i;
			j = 1L << 3;
			j &= 1L << i;
			j = ~i;
			j = ~(1L << i);
		}

		System.out.println("abcde".indexOf(99));
		assert ("test".contentEquals(new StringBuffer("test")));
		int i = "test\2ing".charAt(4);
		switch (i | 'd') {
		case 'f':
			assert (true);
			break;
		case '3':
		case 3:
		default:
			assert (false);
		}
		int y = (/** @j2sNative 1?'s': */
		'\t');
		assert (y == 9 || y == 's');
		int z = (/** @j2sNative 1?2: */
		909 + y);
		assert (z == 918 || z == 2);
		int x = (/** @j2sNative 1?3: */
		909);
		assert (x == 909 || x == 3);
		Object g = "testing";
		Object o = (/** @j2sNative g.mark$ ? g : */
		null);
		assert (o == null || o == g);
		return (/** @j2sNative 1?4: */
		4 + y);
	}

	public static void main(String[] args) {

        
		int val = new Test_().test3();
		assert (val == 13 || val == 4);

		Test_Zipout.main(args);
		System.out.println("Test_ all tests completed successfully.");
	}

	@Override
	public String toString() {
		return "testing " + this.getClass().getName();
	}

}
