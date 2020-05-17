package test;

class Testx {
	// example of a second independent top-level class.
	// It will get its own .js file.
}

public class Test_ {

	static {
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
	}

	public Test_() {
		System.out.println("\n\n==============\nTesting " + getClass().getName());
	}
	
	public static void main(String[] args) {

//		Test_Zipout.main(args);
		System.out.println("Test_ all tests completed successfully.");
	}

	@Override
	public String toString() {
		return "testing " + this.getClass().getName();
	}

}
