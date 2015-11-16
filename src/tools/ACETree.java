/*
 * TODO
 * 
 * 1. Need to use generics.
 * 2. Not happy with the sorting of the arraylist parameter.
 * 3. Size of the leaf node.
 * 
 * Implementation of ACE Tree based on http://www.cise.ufl.edu/~cjermain/ace.pdf
 * 
 * some parameters:
 * 
 * 1. We can calculate the height of the tree given its size
 * 2. The number of leaf nodes = 2^n
 * 3. The number of sections in each leaf node = height - 1
 *  
 */

package tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class ACETree  {
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

	/*
	 * Base class for Internal and Leaf Nodes.
	 */
	interface Node {
		public void print();
		
		public void doDot(PrintWriter pw, int nodeNumber);
		
		/* these are the indexes of the leaf nodes */
		public Range getLeafIndexes();
		
		/* this is the range of the points stored */
		public Range getDataRange();
	}
	
	/*
	 * Does not contain data.  Contains a lot of meta data used while searching for data.
	 * 
	 * Range of values under this Leaf.
	 * Range of actual leaf indexes
	 * Points to left and right children
	 * count of left and right children
	 *  
	 */
	class InternalNode implements Node {
		protected Range r;
		protected int key_index;
		protected int key;
		protected Node left;
		protected Node right;
		protected int countLeft;
		protected int countRight;
		
		/* for internal nodes, we need to know the leaves under them */
		private int leafStartIndex;
		private int leafEndIndex;
		
		@Override
		public Range getLeafIndexes() { return new Range(leafStartIndex, leafEndIndex); }
		
		@Override
		public Range getDataRange() { return r; }
		
		public InternalNode() {
			r = new Range(-1,-1);
			key_index = key = countLeft = countRight = leafStartIndex = leafEndIndex = -1;
			left = right = null;
		}
		
		@Override
		public String toString() {
			return String.format( "%d", key, leafStartIndex, leafEndIndex);			
		}
		
		@Override
		public void print() {
			System.out.println(toString());
		}

		@Override
		public void doDot(PrintWriter pw, int nodeNumber) {
			pw.format("Node%d [width=1 height=1 label =\"%s\"]\n", nodeNumber, toString() );
		}
		

		private void setLeafIndexes(Node child) {
			Range leafIndexes = child.getLeafIndexes();
			
			if( this.leafStartIndex == -1 || this.leafStartIndex > leafIndexes.begin ) 
				this.leafStartIndex = leafIndexes.begin;
			if( this.leafEndIndex == -1 || this.leafEndIndex < leafIndexes.end )
				this.leafEndIndex = leafIndexes.end;
		}
		
	}
	
	/*
	 * Node that contains the data
	 * 
	 *  Number of sections = height.
	 *  It has the data divided into Number of Sections.
	 *  Each section has a range and the actual leaf node data.
	 */
	public class LeafSection {
		protected Range r;
		protected ArrayList<Integer> elements;
		
		public LeafSection(Range r) {
			this.r = r;
			elements = new ArrayList<Integer>();
		}
		
		public void insertRecord(int value) { 
			elements.add(value);
		}
		
		public String toString() {
			StringBuffer sb = new StringBuffer();
			String s = String.format("(%d,%d)", r.begin, r.end );
			sb.append(s);
			
			for ( int i = 0; i < elements.size(); i++) {
				if( i == 0 ) {
					sb.append(" - ");
				}
				s = String.format( "%d%s", elements.get(i), i != elements.size() - 1 ? "," : "" );
				sb.append(s);
			}
			
			return sb.toString();
		}
		
		public void print() {
			System.out.print( toString() );
		}
		
		public void doDot(PrintWriter pw, int nodeNumber) {
			pw.print(toString());
		}
	}
	

	/*
	 * This has the data about a record.
	 * 
	 * It has an index and an array of sections.  (LeafSection).
	 * The data is spread across sections.
	 */
	class LeafNode implements Node {
		protected ArrayList<LeafSection> sections;
		protected int leafIndex;
		
		public LeafNode(int leafIndex, ArrayList<Range> rangeArray) {
			this.leafIndex = leafIndex;
			
			sections = new ArrayList<LeafSection>();
			for( Range r : rangeArray ) 
				sections.add(new LeafSection(r));
		}
		
		public void insertRecord(int sectionID, int value) {
			sections.get(sectionID).insertRecord(value);
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			
			sb.append( String.format( "[%d]", leafIndex ) );
			
			for( int i = 0; i < sections.size(); i++ ) {
				LeafSection section = sections.get(i);
				
				sb.append( String.format(" S%d ", i) );
				sb.append(section.toString());
				sb.append(String.format( "%s",  i == sections.size() - 1 ? " " : "," ));
			}
			
			return sb.toString();
		}

		@Override
		public void print() {
			System.out.println(toString());
		}
		
		/*
		 *  We need the output to be in this form:
		 *  
		 *  struct1 [label="{a|{b|c}}|{d|{e|f}}"];
		 *  
		 *  for the DOT graphviz to arrange these as desired.
		 *
		 */
		@Override
		public void doDot(PrintWriter pw, int nodeNumber) {
			
			// pw.format( "Node%d [shape=rectangle width=3 height=3 label=\"%s\"]\n", nodeNumber, toString());
			pw.format( "Node%d [shape=rectangle width=0.5 height=0.5 label=\"%d\"]\n", nodeNumber, leafIndex);
			
			pw.format( "struct%d [shape=record label=\"", nodeNumber*sections.size()+1);
			
			boolean firstSection = true;
			for( LeafSection section : sections ) {
				pw.format( "%c{%d-%d|{", firstSection == true ? ' ' : '|', section.r.begin, section.r.end );
				boolean firstElement = true;
				for( int element : section.elements ) {
					pw.format( "%c%d", firstElement == true ? ' ' : '|', element);
					firstElement = false;
				}
 				pw.format("}}" );
				firstSection = false;
			}
			
			pw.format( "\"]\n" );
			
			pw.format( "Node%d -> struct%d\n", nodeNumber, nodeNumber*sections.size()+1);

		}
		
		
		
		@Override
		public Range getLeafIndexes() { return new Range(leafIndex, leafIndex); } 
		
		@Override
		public Range getDataRange() { 
			/* the range of the last section is this leaf's data range */
			assert sections != null;
			return sections.get(sections.size()-1).r;
		}

	}
	
	/* this holds all the allocated leaf nodes 
	 * 
	 * it is a little unusual for a Tree structure to hold this.  We need this to assign a record into a Leaf as we may stop the 
	 * tree navigation midstream based on the section id
	 */
	protected ArrayList<LeafNode> leafNodes;

	/*
	 * We maintain this data structure with all the Records.
	 * "value" is the record value.
	 * We uniformly assign each record to a section.
	 * We also assign a record to a leaf node randomly.
	 */
	class Record {
		private int value;
		private int section;
		private int leafID;
	}
	
	ArrayList<Record> theRecords;
	
	private static int LOG_LEAF_SIZE = 1;		// aribtrary...
	protected Node root;
	protected int height;

	/* 
	 * utility class to calculate the height of the tree given the size 
	 */
	public static int getCeilingLog2(int num) {
		int n = 0;
		for( n = 0; (num = num >> 1) > 0; n++ );
		return n+1;
	}
	
	/* returns 2 raised to N */
	public static int twoPowerN(int N) {
		assert N >= 0;
		return 1 << N;
	}
	
	/*
	 * constructor:
	 * 
	 * Takes in an array of integers
	 * 
	 * - constructs an array of Records
	 * - constructs the Phase 1 Tree where the internal nodes and the leaves' structure is created
	 * - assigns the sections to the various records
	 * - finally assigns the records to specific leaves as well.
	 */
	public ACETree(int entries[]) {
		// construct the Record object out of the integer array passed:
		theRecords = new ArrayList<Record>();
		for( int i = 0; i < entries.length; i++ ) {
			Record rec = new Record();
			rec.value = entries[i];
			theRecords.add(rec);
		}
		
		// sort the array and call the internal function:
		Collections.sort(theRecords, new Comparator<Record>() {
			public int compare(Record rec1, Record rec2) {
				return ((Integer)rec1.value).compareTo(rec2.value);
			}
		});
		
		this.height = ACETree.getCeilingLog2(theRecords.size()) - ACETree.LOG_LEAF_SIZE;
		System.out.printf( "size of arr [%d] height [%d]\n", theRecords.size(), this.height);
		
		/* this will hold the allocated leaf nodes - will be useful while assigning leaves for the records */
		this.leafNodes = new ArrayList<LeafNode>();
	
		/* 
		 * Phase1 construction:
		 * 
		 * - use the medians as the split points 
		 * - construct tree by creating the Internal and Leaf Nodes with the various properties
		 * - one interesting issue is how we calculate the leaf node range under each Internal Node.
		 * 
		 */
		Range r = new Range(theRecords.get(0).value, theRecords.get(theRecords.size()-1).value);
		ArrayList<Range> rangeArray = new ArrayList<Range>();
		rangeArray.add(r);
		this.root = constructPhase1(1, theRecords, 0, theRecords.size() - 1, rangeArray);
		
		/* 
		 * Phase 2 construction
		 * 
		 * 1. Section numbers
		 * 2. Leaf node assignments
		 */
		
		/* Assign random section numbers */
		assignSections(this.height, this.root, theRecords);
		
		/* Assign leaf nodes for each of the records */
		assignLeaves(this.root, theRecords);
		
		for( Record rec : theRecords )
			System.out.printf( "%3d", rec.section);
		System.out.println("");
		for( Record rec : theRecords )
			System.out.printf( "%3d", rec.leafID);
		System.out.println("");
		for( Record rec : theRecords )
			System.out.printf( "%3d", rec.value);
		System.out.println("");
		
		
	}
	
	/*
	 * used during phase2 construction of the tree
	 */
	private void assignLeaves(Node n, ArrayList<Record> records) {
		for( Record rec : records ) 
			assignLeafForRecord(n, rec, 0);
	}
	
	/*
	 * used during phase2 construction of the tree
	 * 
	 * recursive - does "section" comparisons before assigning a leaf.
	 */
	private void assignLeafForRecord(Node n, Record rec, int currentComparisonCount) {
		/* 
		 * we will do r.section node navigations.
		 * after this, we will choose a random leaf for this record.
		 */
		if( currentComparisonCount >= rec.section ) {
			/* we are done now - choose a leaf */
			Range r = n.getLeafIndexes();
			int rangeSize = r.end - r.begin + 1;
			rec.leafID = r.begin + (int)(rangeSize * Math.random());
			
			/* tbd: we need to have an array of leaf nodes to make the assignment of the record into the leaf easy */
			leafNodes.get(rec.leafID).insertRecord(rec.section, rec.value);
			
		} else {
			/* need to do some more comparisons - ensure that we have not hit the leaf node! */
			assert( n instanceof InternalNode );
			InternalNode in = (InternalNode)n;
			assignLeafForRecord( rec.value <= in.key ? in.left : in.right, rec, currentComparisonCount+1);
		}
	}
	
	/*
	 * We have 0, 1, .. h - 1 sections.
	 * Each record is assigned a section
	 * We need to ensure that this is uniform e.g. if h=4 and numRecords=100, we have 25 even assignments of 0, 1, 2, and 3.
	 */
	private void assignSections(int height, Node node, ArrayList<Record> records) {
		/* "height" is the number of sections we want to create */
		int sections[] = new int[height];
		for( int i = 0; i < sections.length; i++ ) 
			sections[i] = records.size() / height;

		/* now assign any reminders: */
		int remainder = records.size() % height;
		for( int i = 0; i < remainder; i++ )
			sections[i]++;
		
		for( int i = 0; i < records.size(); i++ ) {
			int randomSection = -1;
			while(true) {
				randomSection = (int)(height * Math.random());
				if( sections[randomSection] > 0 ) {
					sections[randomSection]--;
					break;
				}
			}
			
			records.get(i).section = randomSection;
		}		
	}
	
	/*
	 * phase 1 recursive function to build the nodes
	 * 
	 * 1. Find median in the list passed - note that it is a subset we need to operate on.  This is the key of the node.
	 * 2. Range is "start" and "end"
	 * 3. "left" and "right" nodes are constructed through recursion
	 * 4. count can also be calculated *before* we start the recursion.  Note that the parent node entry is included on the left tree.
	 * 
	 * See Figure 2 in the paper to understand deeper.
	 * 
	 */
	private Node constructPhase1(int currentHeight, ArrayList<Record> records, int start, int end, ArrayList<Range> rangeArray) {
		
		/* check if we have reached the limit of heights */
		if( currentHeight >= this.height ) {
			/* create leaf node */
			return createLeafNode(rangeArray);
		}
		
		InternalNode n = new InternalNode();
		
		/* Set the range */
		n.r.begin = rangeArray.get(rangeArray.size()-1).begin;
		n.r.end = rangeArray.get(rangeArray.size()-1).end;
		
		/* middle index: */
		n.key_index = start + (end-start)/2;
		n.key = records.get( n.key_index ).value;

		/* count for left and right */
		n.countLeft = n.key_index -start+1;
		n.countRight = end - n.key_index;

		/* insert current range and remove it once we are done with the call - for both left and right children */
		
		/* left child */
		/* In order for the range not to have gaps, we will use the end of the left range and add 1 to it */
		int left_begin = -1;
		if( start != 0 ) {
			left_begin = records.get(start-1).value + 1;
		} else {
			left_begin = records.get(start).value;
		}
		
		Range r = new Range(/* records.get(start).value */ left_begin, records.get(n.key_index).value);
		rangeArray.add(r);
		n.left = constructPhase1(currentHeight+1, records, start, n.key_index, rangeArray);
		rangeArray.remove(rangeArray.size()-1);
		n.setLeafIndexes(n.left);
		

		/* right child */
		/* In order for the range not to have gaps, we will use the end of the left range and add 1 to it */
		int right_begin = records.get(n.key_index).value + 1;
		
		r = new Range(/* records.get(n.key_index+1).value */ right_begin, records.get(end).value);
		rangeArray.add(r);
		n.right = constructPhase1(currentHeight+1, records, n.key_index + 1, end, rangeArray);
		rangeArray.remove(rangeArray.size()-1);		
		n.setLeafIndexes(n.right);
		
		return n;
	}
	
	/*
	 * gets the running ID from the main class.
	 * 
	 * note that we keep track of all the LeafNodes that are created
	 */
	private LeafNode createLeafNode(ArrayList<Range> rangeArray) {
		
		int leafID = leafNodes.size();
		
		LeafNode leafNode = new LeafNode(leafID, rangeArray);
		
		/* remember it */
		leafNodes.add(leafNode);
		
		return leafNode;
	}

	public void print() {
	if( root != null)
		print(root);
	else 
		System.out.printf("empty tree\n");
	}
	
	private void print(Node n) {
		assert(n != null);
		n.print();
		
		if( n instanceof InternalNode ) {
			InternalNode in = (InternalNode)n;
			if( in.left != null ) print(in.left);
			if( in.right != null ) print(in.right);
		}
	}
	
	public void doDot(PrintWriter pw) {
		pw.println( "digraph graphname{");
		pw.format( "ratio=\"fill\";margin=0;\n");
		
		if( root != null ) {
			pw.format( "Node0 [shape=diamond label=\"Start\"]\n");
			Range r = root.getDataRange();
			pw.format( "Node0 -> Node1 [label=\"%d-%d\"]\n", r.begin, r.end);
			doDot(pw, root, 1);
		}
	
		
		pw.println( "}");
	}
	
	private int doDot(PrintWriter pw, Node n, int nodeNumber) {
		int newNodeNumber = -1;
		int thisNodeNumber = nodeNumber;
		
		assert(n != null);
		n.doDot(pw, thisNodeNumber);
		
		if( n instanceof InternalNode ) {
			InternalNode in = (InternalNode)n;
			if( in.left != null ) {
				/* print the range of the child node */
				Range r = in.left.getDataRange();
				pw.format("Node%d -> Node%d [label=\"%d-%d\"]\n", thisNodeNumber, nodeNumber+1, r.begin, r.end);
				newNodeNumber = doDot(pw, in.left, nodeNumber+1);
				if( newNodeNumber > nodeNumber ) nodeNumber = newNodeNumber;
			}
			if( in.right != null ) {
				Range r = in.right.getDataRange();
				pw.format("Node%d -> Node%d [label=\"%d-%d\"]\n", thisNodeNumber, nodeNumber+1, r.begin, r.end);
				newNodeNumber = doDot(pw, in.right, nodeNumber+1);
				if( newNodeNumber > nodeNumber ) nodeNumber = newNodeNumber;
			}
		}
		
		return nodeNumber > newNodeNumber ? nodeNumber : newNodeNumber;
	}
	
	

	public static void main(String args[]) {
		System.out.println( "Hello world");
		// System.out.printf( "22 45 1023 %d %d %d \n", ACETree.getCeilingLog2(22), ACETree.getCeilingLog2(45), ACETree.getCeilingLog2(1023) );

		// test 1:
		// doTest1();
		
		doTest2();
		
	}
	
	private static void doTest1() { 		
		int MAX_NUM = 100;
		int NUM = 20;
		int[] arr = new int[NUM];
		for( int i = 0; i < NUM; i++ ) {
			arr[i] = (int)(Math.random() * MAX_NUM);
		}
	
		try {
			ACETree tree = new ACETree(arr);
			tree.print();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void doTest2() {
		int a[] = {60, 18, 25, 10, 69, 92, 41, 22, 77, 7, 50, 37, 33, 12, 29, 36, 50, 15, 88, 74, 62, 53, 58, 74, 3, 98, 75, 81, 47, 84, 89 };
		
		try {
			ACETree aceTree = new ACETree(a);
			// aceTree.print();
			
			String sDotFile = "ace.dot";
			
			try {
				PrintWriter pw = new PrintWriter(sDotFile);
				aceTree.doDot(pw);
				pw.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			System.out.printf( "searching for 20-50\n");
			ACETreeSearch treeSearch = new ACETreeSearch(aceTree, aceTree.new Range(20,50));
			
			ArrayList<Integer> finalResult = new ArrayList<Integer>();
			
			for( int i = 0; !treeSearch.done(); i++ ) {
				System.out.printf( "Doing search %d-th iteration;\n", i );;
				ArrayList<Integer> result = treeSearch.search();
				System.out.print( "Results - " + result + "\n" );
				
				for( int res : result ) 
					finalResult.add(res);
			}

			System.out.println( "Final Result: " + finalResult );
			System.out.printf( "\nDone!!\n" );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}


