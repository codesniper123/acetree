package tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import tools.ACETree.Record;

public class ACETreeTest {
	public class Bucket {
		Range r;
		int count;
		
		public Bucket(int begin, int end, int count) {
			r = new Range(begin, end);
			this.count = count;
		}
		
	}
	
	public class Buckets {
		protected ArrayList<Bucket> bucketArray;
		
		public Buckets(Data data, int bucketSize, int maxNumber) {
			int numBuckets = maxNumber / bucketSize;
			
			bucketArray = new ArrayList<Bucket>();
			int start = 0;
			for( int i = 0; i < numBuckets; i++ ) {
				int end = start + bucketSize - 1;
				bucketArray.add( new Bucket(start, end, 0) );
				start = end + 1;
			}
			
			for( int i : data.d ) {
				Bucket b = getBucket(i);
				if( b == null ) {
					Util.log( Util.None, "null bucket for value = %d\n",  i );
					continue;
				}
				b.count++;
			}
		}
		
		public int getCount() {
			int count = 0;
			
			for( Bucket b : bucketArray ) {
				count += b.count;
			}
			
			return count;
		}
		
		Bucket getBucket(int value) {
			for( Bucket b : bucketArray ) {
				if( value >= b.r.begin && value <= b.r.end )
					return b;
			}
			
			return null;
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append( "Buckets:");
			for( Bucket b : bucketArray ) {
				sb.append( String.format( "[%d-%d: %d] ",  b.r.begin, b.r.end, b.count));
			}
			return sb.toString();
		}
	}
	
	public class Data {
		protected ArrayList<Integer> d;
		
		public Data(int total, int maxRandom) {
			d = new ArrayList<Integer> ();
			
			/* create some random data */
			for( int i = 0; i < total; i++ ) {
				d.add( (int)(Math.random() * maxRandom));
			}
		}
		
		public Data(Data that) {
			this.d = new ArrayList<Integer>();
			
			for( int i = 0; i < that.d.size(); i++ ) {
				this.d.add( that.d.get(i) );
			}
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			
			sb.append( "Data: [" );
			for( int i : d ) 
				sb.append( String.format( "%d ", i));
			sb.append( "]" );
			
			return sb.toString();
			
		}
	}
	
	public class Distribution {
		Data data;
		Buckets buckets;
		String name;
		
		public Distribution(String name, Data data, Buckets buckets) {
			this.data = data;
			this.buckets = buckets;
			this.name = name;
		}
		
		public void print() {
//			Util.log( Util.Minimal, "Name - %s\n", this.name);
//			Util.log( Util.Minimal, "Total points - %d\n", buckets.getCount());
//			
//			if( data.d.size() < 100) {
//				Util.log( Util.Minimal, "%s\n", data);
//			}
//			if( data.d.size() < 1000) {
//				Util.log( Util.Minimal, "%s\n", buckets);
//			}
		}
		
		public void compare(Distribution previous) {
			double ratio = (float)this.buckets.getCount() / previous.buckets.getCount();
			double diff = 0;
			for( int bucket_index = 0; bucket_index < this.buckets.bucketArray.size(); bucket_index++ ) {
				Bucket prevBucket = previous.buckets.bucketArray.get(bucket_index);
				Bucket currBucket = this.buckets.bucketArray.get(bucket_index);
				
				double desired_number = (double) (ratio * prevBucket.count);
				double thisdiff = (currBucket.count - desired_number);
				if( thisdiff < 0 ) thisdiff *= -1;
				diff += thisdiff;
				// System.out.printf( "Bucket [%d] Desired Number [%f] Actual [%d] ThisDifference [%f]\n",  bucket_index, desired_number, currBucket.count, thisdiff );
			}
			
			Util.log( Util.None, "Difference = %f\n", diff );
		}
	}
	
	// protected ArrayList<Distribution> distributions;
	protected static final int RandomSample = 1;
	protected static final int ACETreeSample = 2;
	
	/*
	 * 
	 * 1. Create random data.  Create bucket for it and store it.
	 * 2. for a few iterations:
	 * 2.   Draw some random samples from data.
	 * 3.   Create another distribution.
	 * 4.   Compare the distribution with the previous one for score.
	 */	
	public void doTest( int sampleMethod ) {
		final int totalGenerated = 100000;
		final int maxRandom = 10000;
		final int bucketSize = 100;
		final int iterations = 2;
		final int samplesPerIteration = 100;
		
		ACETree aceTree;
		ACETreeSearch aceTreeSearch = null;
		
		ArrayList<Distribution> distributions = new ArrayList<Distribution>();
		
		/* Create data */
		Data data = new Data(totalGenerated, maxRandom);
		
		if( sampleMethod == ACETreeSample ) {
			int numbers[] = new int [ data.d.size() ];
			for( int i = 0; i < numbers.length; i++ ) {
				numbers[i] = data.d.get(i);
			}
			aceTree = new ACETree(numbers);
			aceTreeSearch = new ACETreeSearch(aceTree, new Range(0, maxRandom));
		}
		
		Buckets buckets = new Buckets(data, bucketSize, maxRandom);
		Distribution distribution = new Distribution(sampleMethod == RandomSample ? "Random" : "ACETree", data, buckets);
		distribution.print();
		distributions.add( distribution );
		
		Distribution dPrev = distribution;
		for( int iter = 0; iter < iterations; iter++ ) {
			/* remove a few samples */
			if( sampleMethod == RandomSample ) {
				for( int samples = 0; samples < samplesPerIteration; samples++ ) {
					int index = (int)(data.d.size() * Math.random());
					data.d.remove(index);
				}
			} else if( sampleMethod == ACETreeSample )  {
				ArrayList<Integer> result = aceTreeSearch.search();
				
				Util.log( Util.None, "[%d] Samples returned from the ACETree\n", result.size() );
				
				/* remove this from our bucket */
				for( int i : result ) {
					data.d.remove( new Integer(i) );
				}
			}
			
			/* create a new distribution */
			Buckets bNew = new Buckets(data, bucketSize, maxRandom);
			Distribution dNew = new Distribution(sampleMethod == RandomSample ? "Random" : "ACETree", data, bNew);
			dNew.print();
			distributions.add( dNew );
			
			/* compare with the previous distribution */
			dNew.compare(dPrev);
			dPrev = dNew;
		}
	}
	
	
	public void doTest2() {
		final int totalGenerated = 1000000;
		final int maxRandom = 100000;
		final int bucketSize = 100;
		final int iterations = 100;
		// final int samplesPerIteration = 100;
		
		ACETree aceTree;
		ACETreeSearch aceTreeSearch = null;
		
		ArrayList<Distribution> distributionsACETree = new ArrayList<Distribution>();
		ArrayList<Distribution> distributionsRandom = new ArrayList<Distribution>();
		
		/* Create data */
		Data dataACETree = new Data(totalGenerated, maxRandom);
		
		/* construct the ACETree */
		int numbers[] = new int [ dataACETree.d.size() ];
		for( int i = 0; i < numbers.length; i++ ) {
			numbers[i] = dataACETree.d.get(i);
		}
		aceTree = new ACETree(numbers);
		aceTreeSearch = new ACETreeSearch(aceTree, new Range(0, maxRandom));
		
		/* create distribution for ACETree */
		Buckets bucketsACETree = new Buckets(dataACETree, bucketSize, maxRandom);
		Distribution distACETree = new Distribution("ACETree", dataACETree, bucketsACETree);
		distACETree.print();
		distributionsACETree.add( distACETree );
		
		/* create distribution for RANDOM */
		Data dataRandom = new Data( dataACETree );
		Buckets bucketsRandom = new Buckets(dataRandom, bucketSize, maxRandom);
		Distribution distRandom = new Distribution("Random", dataRandom, bucketsRandom);
		distRandom.print();
		distributionsRandom.add( distACETree );
		
		Distribution dPrevACETree = distACETree;
		Distribution dPrevRandom = distRandom;
		
		for( int iter = 0; iter < iterations; iter++ ) {
			/* Remove the samples from ACETree */
			ArrayList<Integer> result = aceTreeSearch.search();
			
			Util.log( Util.None, "[%d] Samples returned from the ACETree\n", result.size() );
			
			/* remove this from our bucket */
			for( int i : result ) {
				dataACETree.d.remove( new Integer(i) );
			}

			/* Remove the same number from Random */
			for( int samples = 0; samples < result.size(); samples++ ) {
				int index = (int)(dataRandom.d.size() * Math.random());
				dataRandom.d.remove(index);
			}
			
			/* create a new distribution for ACETree: */
			Buckets bNewACETree = new Buckets(dataACETree, bucketSize, maxRandom);
			Distribution dNewACETree = new Distribution("ACETree", dataACETree, bNewACETree);
			dNewACETree.print();
			distributionsACETree.add( dNewACETree );
			
			/* compare with the previous distribution */
			// dNewACETree.compare(dPrevACETree);
			dPrevACETree = dNewACETree;

			/* create a new distribution for Random: */
			Buckets bNewRandom = new Buckets(dataRandom, bucketSize, maxRandom);
			Distribution dNewRandom = new Distribution("Random", dataRandom, bNewRandom);
			dNewRandom.print();
			distributionsRandom.add( dNewRandom );
			
			/* compare with the previous distribution */
			// dNewRandom.compare(dPrevRandom);
			dPrevRandom = dNewRandom;
		}
		
		Distribution dFirst, dLast;

		Util.log( Util.Minimal, "Size of distributions [%d]\n", distributionsACETree.size() );

		Util.log( Util.Minimal, "Comparing ACETree\n" );
		dLast = distributionsACETree.get(  distributionsACETree.size() - 1);
		dFirst = distributionsACETree.get( 0);
		dLast.compare( dFirst );
		
		Util.log( Util.Minimal, "Comparing Random\n" );
		Util.log( Util.Minimal, "Size of distributions [%d]\n", distributionsRandom.size() );
		dLast = distributionsRandom.get(  distributionsRandom.size() - 1);
		dFirst = distributionsRandom.get(0);
		dLast.compare( dFirst );
		
		
	}
	

	
	public static void main(String args[]) {
		ACETreeTest aceTreeTest = new ACETreeTest();
		//aceTreeTest.doTest( RandomSample );
		//aceTreeTest.doTest( ACETreeSample );
		aceTreeTest.doTest2();
	}
}
