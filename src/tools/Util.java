package tools;

public class Util {
	protected static final int None = 0;			/* I do not like this name */
	protected static final int Minimal = 1;
	protected static final int Verbose = 2;
	
	protected static int debugLevel = Util.Minimal;
	
	public static void log(int debugLevel, String formatStr, Object...args) {
		if( Util.debugLevel >= debugLevel ) {
			System.out.printf( formatStr,  args );
		}
	}
}


