package tools;

import java.util.ArrayList;
import java.util.HashMap;

import tools.ACETree.InternalNode;
import tools.ACETree.LeafSection;
import tools.ACETree.LeafNode;
import tools.ACETree.Node;
import tools.ACETree.Range;

public class ACETreeSearch {
	/* 
	 * searching data structures:
	 */
	class NodeSearchInfo {
		private static final int LEFT = 1;
		private static final int RIGHT = 2;
		private int next = LEFT;;
		private boolean done = false;
		
		public NodeSearchInfo() {
			next = LEFT;
			done = false;
		}
	};
	
	private HashMap<Node, NodeSearchInfo> searchNodes;
	
	private ACETree tree;
	private Range queryRange;
	
	class BucketRange {
		protected static final int BUCKETRANGE_INVALID = -1;
		protected static final int BUCKETRANGE_UNSET = 0;
		protected static final int BUCKETRANGE_SET = 1;
		
		protected Range r;
		protected int type;
		
		public BucketRange(Range r) { this.r = r; }
		
		public String getType() {
			switch( type ) {
			case BUCKETRANGE_INVALID: return "invalid";
			case BUCKETRANGE_UNSET: return "unset";
			case BUCKETRANGE_SET: return "set";
			}
			return "unknown";
		}
	}

	/*
	 * Each bucket is very similar to the LeafSection.
	 * 
	 * We use the Bucket to combine two sections in the ACETree.  Note that we can combine only when we encapsulate the whole query range.
	 * 
	 */
	class Bucket {
		protected ArrayList<Integer> elements;
		protected ArrayList<BucketRange> bucketRanges;
		protected int totalNeeded;
		protected int totalObtained;
		int bucketID;
		
		/*
		 * bucketID is the same as the sectionID
		 */
		public Bucket(int bucketID, int height, ArrayList<LeafNode> leafNodes, Range queryRange) {
			elements = new ArrayList<Integer>();
			bucketRanges = new ArrayList<BucketRange>();
			totalNeeded = 0;
			totalObtained = 0;
			
			/* 
			 * each bucket stands for a section.
			 * Thus there are "h" buckets where h = height of the tree
			 * 
			 * the first bucket has only one range min-max. 2^0;  (it spans the entire selection)
			 * the second bucket has two 2^1 ranges
			 * the nth bucket has 2^(n-1) ranges
			 * 
			 * Also, depending on the Search Range, we may need a smaller number of "totalNeeded" before we declare success.
			 * For example, let us say the search term is 20-60, then we need the range to be including this, not min-max.
			 * 
			 * let h be the height
			 * bucket index -> 0, 1, 2, ... h - 1
			 * 
			 * #ranges for 0 -> 1
			 * #ranges for 1 -> 2
			 * and so on.
			 * 
			 * We need to index into the LeafSections to get the exact range.
			 * For bucket id = 0, we need to access ONLY 0
			 * for bucket id = 1, we need to access 0, 4
			 * for bucket id = 2, we need to access 0, 2, 4, 6
			 * and so on.
			 * 
			 * example: h = 4.  we have bucket ids = 0, 1, 2, 3;  number of leaves = 2^(h-1)
			 * 
			 * ranges for bucket[0] -> access leaf id = 0,  leafsection[0]						numRanges = 2^bucketid;  next leaf = current leaf + (2^(h-1-bucketid)
			 * ranges for bucket[1] -> access leaf id = 0, 4 leafsection[1]
			 * ranges for bucket[2] -> access leaf id = 0, 2, 4, 6 leafsection[2]
			 * ranges for bucket[3] -> access leaf id = 0, 1, 2, 3, 4, 5, 6, 7 leafsection[3]
			 * 
			 * 
			 */
			this.bucketID = bucketID; 
			int numRanges = ACETree.twoPowerN(this.bucketID);
			int incLeafID = ACETree.twoPowerN(height-1-this.bucketID);
			int leafID = 0;
			totalNeeded = 0;
			for(int i = 0; i < numRanges; i++) {
				/* get the range for this leaf section in the leaf node */
				Range sectionRange = leafNodes.get(leafID).sections.get(bucketID).r;
				
				BucketRange bucketRange = new BucketRange( sectionRange );
				leafID += incLeafID;
				if( sectionRange.overlaps(queryRange) ) {
					totalNeeded++;
					bucketRange.type = BucketRange.BUCKETRANGE_UNSET;
				} else {
					bucketRange.type = BucketRange.BUCKETRANGE_INVALID;
				}
				bucketRanges.add(bucketRange);
			}
		}
		
		
		/*
		 * 
		 * this is called to add a leaf node to the bucket's sections.
		 * 
		 * there are 2 tasks:
		 * 
		 * 1) Figure out which range to add to.
		 * 2) Figure out if this section is complete i.e does it completely overlap the search range.
		 * 
		 * Which range:
		 * 		parameters: leafIndex, sectionIndex
		 * 		output: rangeIndex
		 * 		
		 * example: h = 4 number of sections = 4 number of leaf nodes = 2^(h-1) = 8 (hence index from 0-7)
		 * 
		 * sectionIndex = 0
		 * number of ranges = 2^(sectionIndex) = 1
		 * rangeIndex = 0 // there is only one index.
		 * 
		 * sectionIndex = 1
		 * number of ranges = 2^(sectionIndex) = 2
		 * leafIndex = 0-3; rangeIndex = 0
		 * leafIndex = 4-7; rangeIndex = 1  
		 * 
		 * sectionIndex = 2
		 * number of ranges = 2^(sectionIndex) = 4
		 * leafIndex = 0-1; rangeIndex = 0
		 * leafIndex = 2-3; rangeIndex = 1
		 * leafIndex = 4-5; rangeIndex = 2
		 * leafIndex = 6-7; rangeIndex = 3
		 * 
		 *   
		 * sectionIndex = 3
		 * number of ranges = 2^(sectionIndex) = 8
		 * leafIndex = 0; rangeIndex = 0
		 * leafIndex = 1; rangeIndex = 1
		 * leafIndex = 2; rangeIndex = 2
		 * leafIndex = 3; rangeIndex = 3  
		 * leafIndex = 4; rangeIndex = 4  
		 * leafIndex = 5; rangeIndex = 5  
		 * leafIndex = 6; rangeIndex = 6  
		 * leafIndex = 7; rangeIndex = 7
		 * 
		 * Formula:
		 * leafIndex/2^(h-sectionIndex-1)
		 * e.g. leafIndex = 6 h = 4 sectionIndex = 3;  rangeIndex = 6/2^(4-3-1) = 6
		 * leafIndex = 3 h = 4 sectionIndex = 1;  rangeIndex = 3/2^(4-1-1) = 0
		 *
		 *   
		 *   PARAMETERS:
		 *   	- height (we need for the formula: can we skip this - isn't this the number of sections?)
		 *   	- leafIndex (again we need for the formula)
		 *   	- leafSection - we are processing this section
		 */
		protected boolean addLeaf(int height, int leafIndex, LeafSection leafSection) {
			/* get the range index */
			int rangeIndex = leafIndex / ACETree.twoPowerN(height-this.bucketID-1);
			assert rangeIndex < this.bucketRanges.size();
			
			/* if we have not set it before, set it and increment how much more we need to get the desired value */
			BucketRange bucketRange = bucketRanges.get(rangeIndex);
			assert bucketRange.type != BucketRange.BUCKETRANGE_INVALID;
			if( bucketRange.type == BucketRange.BUCKETRANGE_UNSET) {
				this.totalObtained++;
				bucketRange.type = BucketRange.BUCKETRANGE_SET;
			}
			
			/* regardless, add the elements to this bucket */
			for( int element : leafSection.elements ) 
				this.elements.add(element);
			
			/* we can now return the elements in this bucket */
			return this.totalObtained == this.totalNeeded ? true : false;
		}
		
		/*
		 * we move the elements to the result after filtering.
		 * 
		 * then, reset the bucket. 
		 */
		void flushElements(Range queryRange, ArrayList<Integer> result) {
			/* add the elements */
			ACETreeSearch.filterAndAdd(queryRange, this.elements, result);
			
			/* reset the bucket */
			elements = new ArrayList<Integer>();
			this.totalObtained = 0;
			for( BucketRange bucketRange : this.bucketRanges ) {
				if( bucketRange.type == BucketRange.BUCKETRANGE_SET )
					bucketRange.type = BucketRange.BUCKETRANGE_UNSET;
			}
		}
		
		void print() {
			System.out.printf( "bucketID: %d ranges : %d: totalNeeded: %d totalObtained %d", bucketID, bucketRanges.size(), this.totalNeeded, this.totalObtained );
			for( BucketRange bucketRange  : bucketRanges ) {
				System.out.printf( "[%d-%d] (%s) ", bucketRange.r.begin, bucketRange.r.end, bucketRange.getType() );
			}
			System.out.printf( " elements: " );
			for( int elem : this.elements) System.out.printf( "[%d]", elem);
			System.out.printf("\n");
		}
		
	}
	ArrayList<Bucket> buckets;
	
	public ACETreeSearch(ACETree aceTree, ACETree.Range queryRange) {
		this.tree = aceTree;
		this.queryRange = queryRange;

		/* initialize the search nodes */
		this.searchNodes = new HashMap<Node, NodeSearchInfo> ();
		initSearchNodes2(tree.root, this.searchNodes);
		
		/* 
		 *  Initialize the buckets
		 * 
		 *  We add one bucket per each segment in the height of the tree.
		 */
		this.buckets = new ArrayList<Bucket>();
		for( int i = 0; i < this.tree.height; i++ ) {
			this.buckets.add( new Bucket(i, this.tree.height, this.tree.leafNodes, queryRange));
		}
		
		// print the buckets:
		System.out.printf( "Buckets:\n" );
		for( Bucket bucket : buckets ) 
			bucket.print();
	}

	/*
	 * The main interface to do the search.
	 * 
	 * You can call this repeatedly while checking for done().
	 */
	public ArrayList<Integer> search() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		shuttle(tree.root, this.queryRange, result);
		return result;
	}
	
	/*
	 * Terminating condition for the search 
	 */
	public boolean done() { 
		return isSearchDone(this.tree.root);
	}
	
	
	/*
	 * initializing search data structures 
	 */
	private void initSearchNodes2(Node node, HashMap<Node, NodeSearchInfo> searchNodes) {
		/* end condition */
		if( node == null ) 
			return;
		
		/* recurse */
		searchNodes.put(node,  new NodeSearchInfo());
		if( node instanceof InternalNode ) {
			InternalNode in = (InternalNode)node;
			
			initSearchNodes2(in.left, searchNodes);
			initSearchNodes2(in.right, searchNodes);
		} 
	}
	
	private boolean isSearchDone(Node in) {
		NodeSearchInfo searchInfo = this.searchNodes.get(in);
		assert searchInfo != null;
		return searchInfo.done;
	}
	
	private void setSearchDone(Node in ) {
		NodeSearchInfo searchInfo = this.searchNodes.get(in);
		assert searchInfo != null;
		searchInfo.done = true;
		
	}
	
	/*
	 * The "shuttle" name is borrowed from the Algorithm described in the paper.
	 * 
	 * Please see algorithm described - we have tried to be true to it.
	 */
	private void shuttle(Node node, Range range, ArrayList<Integer> result) {
		if( node instanceof InternalNode ) {
			InternalNode in = (InternalNode)node;
			if( isSearchDone( in.left ) && isSearchDone( in.right ) ) {
				setSearchDone(node);
			} else if( !isSearchDone(in.right) && isSearchDone(in.left) ) {
				/* only right is not done */
				shuttle(in.right, range, result);
			} else if( !isSearchDone(in.left) && isSearchDone(in.right) ) {
				/* only left is not done */
				shuttle(in.left, range, result);
			} else {
				/* both the children are not done: */
				if( range.overlaps(in.left.getDataRange()) && !range.overlaps(in.right.getDataRange())) {
					/* overlaps only with left */
					shuttle(in.left, range, result);
				} else if( range.overlaps(in.right.getDataRange()) && !range.overlaps(in.left.getDataRange())) {
					/* overlaps only with right */
					shuttle(in.right, range, result);
				} else {
					/* overlaps both sides or none */
					NodeSearchInfo searchInfo = this.searchNodes.get(node);
					if( searchInfo.next == NodeSearchInfo.LEFT ) {
						shuttle(in.left, range, result);
						searchInfo.next = NodeSearchInfo.RIGHT;
					}
					if( searchInfo.next == NodeSearchInfo.RIGHT ) {
						shuttle(in.right, range, result);
						searchInfo.next = NodeSearchInfo.LEFT;
					}
				}
			}
		} else {
			combineTuples(node, range, result);
			setSearchDone(node);
		}
	}
	
	private void combineTuples(Node n, Range queryRange, ArrayList<Integer> result) {
		assert n instanceof LeafNode;
		LeafNode leaf = (LeafNode)n;
		
		
		System.out.printf( "combineTuples: leafNode index %d\n", leaf.leafIndex);
		
		/* 
		 * I have interpreted the logic in a certain way. :-)
		 * 
		 * for each section of "leaf":
		 *   if the range of the section encloses the query range, we filter these points.
		 *   if not, we add the leaf's section in our Bucket's corresponding index.
		 *   if the range in the Bucket index completely surround the range, we can filter and return the elements in this section.
		 */
		
		int sectionIndex = 0;
		for( LeafSection leafSection : leaf.sections) {
			/* check if the query range completely includes the section's range */
			if( leafSection.r.encapsulates( queryRange )) {
				/* filter and add entries */
				System.out.printf( "sectionIndex %d (%d-%d) completely encapsulates query \n", sectionIndex, leafSection.r.begin, leafSection.r.end);
				
				filterAndAdd(queryRange, leafSection.elements, result);
			} else if( queryRange.overlaps(leafSection.r )) {
				/* extend and add to the current section in the bucket */
				Bucket b = this.buckets.get(sectionIndex);
				
				System.out.printf( "sectionIndex %d (%d-%d) overlaps query \n", sectionIndex, leafSection.r.begin, leafSection.r.end);
				b.print();
				
				/* 
				 * Add this leaf to the appropriate bucket.
				 * 
				 * addLeaf returns TRUE when the section is complete.  In this case, we can merge the elements into the Result Array.
				 */
				if( b.addLeaf(this.tree.height, leaf.leafIndex, leafSection) ) {
					System.out.printf( "Completed the leaf -> Flushing the elements\n");
					b.flushElements(this.queryRange, result);
				}
				
				System.out.printf( "bucket -> after adding leaf..\n");
				b.print();
			}
			
			sectionIndex++;
		}
		
	}
	
	private static void filterAndAdd(Range queryRange, ArrayList<Integer> src, ArrayList<Integer> dest) {
		System.out.printf( "Filter and add " );
		for( Integer i : src ) {
			System.out.printf( "[%d] ", i );
			if( queryRange.includes(i) ) 
				dest.add(i);
		}
		System.out.printf("\n");
	}
}
