package tools;
/*
 * used in:
 * - storing the range in InternalNode.  
 * - storing the leaf ranges.
 */

public class Range {
	protected int begin;
	protected int end;
	public Range(int begin, int end ) {
		this.begin = begin;
		this.end = end;
	}
	
	public boolean overlaps2(Range that) {
		if( that.end > this.begin && that.end <= this.end ) 
			return true;
		// tbd - is Range inclusive on both ends??
		if( that.begin > this.begin && that.begin <= this.end )
			return true;
		
		return false;
	}
	
	/*
	 * 
	 * This seemingly trivial function tripped me up several times.
	 * 
	 * Need a test case for this.
	 * 
	 * Which raises the interesting question of how to test a class with several subclasses.
	 * 
	 */
	public boolean overlaps(Range that) {
		/* 
		 * three cases:  (Need to see if I am missing a simple way of checking
		 * 
		 *        |------------|
		 *   |------------|
		 *     
		 *        |------------|
		 *             | ------------|
		 *           
		 *        |------------| (that)
		 *             |---|  (this)
		 *                     
		 *        |------------| (this)
		 *             |---|  (that)        
		 */
		
		if( that.end >= this.end && that.begin <= this.end ) 
			return true;
		
		if( that.end >= this.begin && that.begin <= this.begin )
			return true;
		
		if( that.end >= this.begin && that.end <= this.end )
			return true;
		
		return false;
	}
	
	public boolean encapsulates(Range that) {
		return ( this.begin <= that.begin && this.end >= that.end ) ? true : false; 
	}
	
	public boolean includes(int i) {
		// tbd - is Range inclusive on both ends??
		return ( i >= this.begin && i <= this.end ) ? true : false;
	}
}

