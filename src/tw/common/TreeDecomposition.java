/*
 * Copyright (c) 2016, Hisao Tamaki
 */
package tw.common;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;

/**
 * This class provides a representation of tree-decompositions of graphs.
 * It is based on the {@code Graph} class for the representation of graphs.
 * Members representing the bags and tree edges are all public.
 * Reading from and writing to files, in the .td format of PACE challeng,
 * are provided.  
 *
 * @author  Hisao Tamaki
 */

public class TreeDecomposition {
	/**
	 * number of bags
	 */
	public int nb;

	/**
	 * intended width of this decomposition
	 */
	public int width;

	/**
	 * the graph decomposed 
	 */
	public Graph g;

	/**
	 * array of bags, each of which is an int array listing vertices.
	 * The length of this array is {@code nb + 1} as the bag number (index)[
	 * starts from 1
	 */
	public int[][] bags;

	/**
	 * array of bags, each of which is an {@code XBitSet} representing
	 * the set of vertices in the bag. 
	 * The length of this array is {@code nb + 1} as the bag number (index)[
	 * starts from 1.
	 */
	public XBitSet[] bagSets;

	/**
	 * array of node degrees. {@code degree[i]} is the number of bags adjacent
	 * to the ith bag.    
	 * The length of this array is {@code nb + 1} as the bag number (index)[
	 * starts from 1.
	 */
	public int degree[];

	/**
	 * array of int arrays representing neighbor lists.
	 * {@code neighbor[i][j]} is the bag index (in {@bags} array) of 
	 * the jth bag adjacent to the ith bag.
	 * The length of this array is {@code nb + 1} as the bag number (index)[
	 * starts from 1.
	 */
	public int neighbor[][];

	private static boolean debug = false;

	/**
	 * Construct a tree decomposition with the specified number of bags,
	 * intended width, and the graph decomposed.
	 * @param nb the number of bags
	 * @param width the intended width
	 * @param g the graph decomposed
	 */
	public TreeDecomposition(int nb, int width, Graph g) {
		this.nb = nb;
		this.width = width;
		this.g = g;
		bags = new int[nb + 1][];
		degree = new int[nb + 1];
		neighbor = new int[nb + 1][];
	}

	/**
	 * Sets the ith bag to the given bag. 
	 * @param i the index of the bag. 1 <= i <= nb must hold
	 * @param bag int array representing the bag
	 */
	public void setBag(int i, int[] bag) {
		bags[i] = bag;
	}

	/**
	 * Adds the given bag. The number of bags {@code nb} is incremented.
	 * @param bag int array representing the bag to be added
	 */
	public int addBag(int[] bag) {
		nb++;
		if (debug) {
			System.out.print(nb + "th bag:");
		}
		for (int i = 0; i < bag.length; i++) {
			if (debug) {
				System.out.print(" " + bag[i]);
			}
		}
		if (debug) {
			System.out.println();
		}
		bags = Arrays.copyOf(bags, nb + 1);
		bags[nb] = bag;
		degree = Arrays.copyOf(degree, nb + 1);
		neighbor = Arrays.copyOf(neighbor, nb + 1);
		return nb;
	}

	/**
	 * Adds and edge
	 * the neighbor lists of both bags, as well as the degrees, 
	 * are updated  
	 * @param i index of one bag of the edge
	 * @param j index of the other bag of the edge
	 */
	public void addEdge(int i, int j) {
		if (debug) {
			System.out.println("add deomposition edge (" + i + "," + j + ")");
		}
		addHalfEdge(i, j);
		addHalfEdge(j, i);
	}

	/**
	 * Adds a bag to the neibhor list of another bag
	 * @param i index of the bag of which the neighbor list is updated
	 * @param j index of the bag to be added to {@code neighbor[i]}
	 */
	private void addHalfEdge(int i, int j) {
		if (neighbor[i] == null) {
			degree[i] = 1;
			neighbor[i] = new int[]{j};
		}
		else if (indexOf(j, neighbor[i]) < 0){
			degree[i]++;
			neighbor[i] = Arrays.copyOf(neighbor[i], degree[i]);
			neighbor[i][degree[i] - 1] = j;
		}
	}

	/**
	 * Combine the given tree-decomposition into this target tree-decomposition.
	 * The following situation is assumed. Let G be the graph for which this
	 * target tree-decomposition is being constructed. Currently, 
	 * this tree-decomposition contains bags for some subgraph of G.
	 * The tree-decomposition of some other part of G is given by the argument.
	 * The numbering of the vertices in the argument tree-decomposition differs
	 * from that in G and the conversion map is provided by another argument. 
	 * @param td tree-decomposition to be combined
	 * @param conv the conversion map, that maps the vertex number in the graph of
	 * tree-decomposition {@code td} into the vertex number of the graph of this 
	 * target tree-decomposition.
	 */
	public void combineWith(TreeDecomposition td, int conv[]) {
		this.width = Math.max(this.width, td.width);
		int nb0 = nb;
		for (int i = 1; i <= td.nb; i++) {
			addBag(convertBag(td.bags[i], conv));
		}
		for (int i = 1; i <= td.nb; i++) {
			for (int j = 0; j < td.degree[i]; j++) {
				int h = td.neighbor[i][j];
				addHalfEdge(nb0 + i, nb0 + h);
			}
		}
	}
	/**
	 * Combine the given tree-decomposition into this target tree-decomposition.
	 * The assumptions are the same as in the method with two parameters.
	 * The third parameter specifies the way in which the two parts
	 * of the decompositions are connected by a tree edge of the decomposition.
	 * 
	 * @param td tree-decomposition to be combined
	 * @param conv the conversion map, that maps the vertex number in the graph of
	 * tree-decomposition {@code td} into the vertex number of the graph of this 
	 * target tree-decomposition.
	 * @param v int array listing vertices: an existing bag containing all of
	 * these vertices and a bag in the combined part containing all of
	 * these vertices are connected by a tree edge; if {@code v} is null
	 * then first bags of the two parts are connected 
	 */
	public void combineWith(TreeDecomposition td, int conv[], int v[]) {
		this.width = Math.max(this.width, td.width);
		int nb0 = nb;
		for (int i = 1; i <= td.nb; i++) {
			addBag(convertBag(td.bags[i], conv));
		}
		for (int i = 1; i <= td.nb; i++) {
			for (int j = 0; j < td.degree[i]; j++) {
				int h = td.neighbor[i][j];
				addEdge(nb0 + i, nb0 + h);
			}
		}
		if (v == null) {
			addEdge(1, nb0 + 1);
		}
		else {
			int k = findBagWith(v, 1, nb0);
			int h = findBagWith(v, nb0 + 1, nb);
			if (k < 0) {
				System.out.println(Arrays.toString(v) + " not found in the first " + nb0 + " bags");
			}
			if (h < 0) {
				System.out.println(Arrays.toString(v) + " not found in the last " + td.nb + " bags");
			}
			addEdge(k, h);
		}
	}

	/**
	 * Converts the vetex number in the bag
	 * @param bag input bag
	 * @param conv conversion map of the vertices
	 * @return the bag resulting from the conversion,
	 * containing {@code conv[v]} for each v in the original bag
	 */

	private int[] convertBag(int bag[], int conv[]) {
		int[] result = new int[bag.length];
		for (int i = 0; i < bag.length; i++) {
			result[i] = conv[bag[i]];
		}
		return result;
	}

	/**
	 * Find a bag containing all the listed vertices,
	 * with bag index in the specified range
	 * @param v int array listing vertices
	 * @param s the starting bag index
	 * @param t the ending bag index
	 * @return index of the bag containing all the
	 * vertices listed in {@code v}; -1 if none of the
	 * bags {@code bags[i]}, s <= i <= t, satisfies this
	 * condition.
	 */
	private int findBagWith(int v[], int s, int t) {
		for (int i = s; i <= t; i++) {
			boolean all = true;
			for (int j = 0; j < v.length; j++) {
				if (indexOf(v[j], bags[i]) < 0) {
					all = false;
				}
			}
			if (all) return i;
		}
		return -1;
	}
	
//	public int[] fillEdges(LabeledGraph g)
//	{
//		for (int i = 1; i <= nb; i++) {
//			
//		}
//	}

	/**
	 * write this tree decomposition to the given print stream
	 * in the PACE .td format
	 * @param ps print stream
	 */
	public void writeTo(PrintStream ps) {
		ps.println("s td " + nb + " " + (width + 1) + " " + g.n);
		for (int i = 1; i <= nb; i++) {
			ps.print("b " + i);
			for (int j = 0; j < bags[i].length; j++) {
				ps.print(" " + (bags[i][j]));
			}
			ps.println();
		}
		for (int i = 1; i <= nb; i++) {
			for (int j = 0; j < degree[i]; j++) {
				int h = neighbor[i][j];
				if (i < h) {
					ps.println(i + " " + h);
				}
			}
		}
	}

	/**
	 * validates this target tree-decomposition
	 * checking the three required conditions
	 * The validation result is printed to the
	 * standard output
	 */
	public void validate() {
		System.out.println("validating nb = " + nb + ", ne = " + numberOfEdges());
		boolean error = false;
		if (!isConnected()) {
			System.out.println("is not connected ");
			error = true;
		}
		if (isCyclic()) {
			System.out.println("has a cycle ");
			error = true;
		}
		if (tooLargeBag()) {
			System.out.println("too Large bag ");
			error = true;
		}
		int v = missinVertex();
		if (v >= 0) {
			System.out.println("a vertex " + v + " missing ");
			error = true;
		}
		int edge[] = missingEdge(); 
		if (edge != null) {
			System.out.println("an edge " + Arrays.toString(edge) + " is missing ");
			error = true;
		}
		if (violatesConnectivity()) {
			System.out.println("connectivety property is violated ");
			error = true;
		}
		if (!error) {
			System.out.println("validation ok");
		}
	}

	/**
	 * Checks if this tree-decomposition is connected as
	 * a graph of bags
	 * @return {@code true} if this tree-decomposition is connected
	 * {@cdoe false} otherwise
	 */

	private boolean isConnected() {
		boolean mark[] = new boolean [nb + 1];
		depthFirst(1, mark);
		for (int i = 1; i <= nb; i++) {
			if (!mark[i]) {
				return false;
			}
		}
		return true;
	}

	private void depthFirst(int i, boolean mark[]) {
		mark[i] = true;
		for (int a = 0; a < degree[i]; a++) {
			int j = neighbor[i][a];
			if (!mark[j]) {
				depthFirst(j, mark);
			}
		}
	}

	/**
	 * Checks if this tree-decomposition is acyclic as
	 * a graph of bags
	 * @return {@code true} if this tree-decomposition is acyclic
	 * {@cdoe false} otherwise
	 */

	private boolean isCyclic() {
		boolean mark[] = new boolean [nb + 1];
		return isCyclic(1, mark, 0);
	}

	private boolean isCyclic(int i, boolean mark[], 
			int parent) {
		mark[i] = true;
		for (int a = 0; a < degree[i]; a++) {
			int j = neighbor[i][a];
			if (j == parent) {
				continue;
			}
			if (mark[j]) {
				return true;
			}
			else {
				boolean b = isCyclic(j, mark, i);
				if (b) return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the bag size is within the declared
	 * tree-width plus one
	 * @return {@code true} if there is some violating bag,
	 * {@cdoe false} otherwise
	 */
	private boolean tooLargeBag() {
		for (int i = 1; i <= nb; i++) {
			if (bags[i].length > width + 1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Finds a vertex of the graph that does not appear
	 * in any of the bags
	 * @return the missing vertex number; -1 if there is no
	 * missing vertex
	 */
	private int missinVertex() {
		for (int i = 0; i < g.n; i++) {
			if (!appears(i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Checks if the given vertex appears in some bag
	 * of this target tree-decomposition
	 * @param v vertex number
	 * @return {@cod true} if vertex {@code v} appears in 
	 * some bag
	 */
	private boolean appears(int v) {
		for (int i = 1; i <= nb; i++) {
			if (indexOf(v, bags[i]) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if there is some edge not appearing in any
	 * bag of this target tree-decomposition
	 * @return two element int array representing the
	 * missing edge; null if there is no missing edge
	 */
	private int[] missingEdge() {
		for (int i = 0; i < g.n; i++) {
			for (int j = 0; j < g.degree[i]; j++) {
				int h = g.neighbor[i][j];
				if (!appears(i, h)) {
					return new int[]{i, h};
				}
			}
		}
		return null;
	}

	/** 
	 * Checks if the edge between the two specified vertices
	 * appear in some bag of this target tree-decomposition
	 * @param u one endvertex of the edge
	 * @param v the other endvertex of the edge
	 * @return {@code true} if this edge appears in some bag; 
	 * {@code false} otherwise 
	 */
	private boolean appears(int u, int v) {
		for (int i = 1; i <= nb; i++) {
			if (indexOf(u, bags[i]) >= 0 &&
					indexOf(v, bags[i]) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if this target tree-decomposition violates
	 * the connectivity condition for some vertex of the graph
	 * @return {@code true} if the condition is violated
	 * for some vertex; {@code false} otherwise.
	 */
	private boolean violatesConnectivity() {
		for (int v = 1; v <= g.n; v++) {
			if (violatesConnectivity(v)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if this target tree-decomposition violates
	 * the connectivity condition for the given vertex {@code v}
	 * @param v vertex number
	 * @return {@code true} it the connectivity condition is violated
	 * for vertex {@code v}
	 */
	private boolean violatesConnectivity(int v) {
		boolean mark[] = new boolean[nb + 1];

		for (int i = 1; i <= nb; i++) {
			if (indexOf(v, bags[i]) >= 0) {
				markFrom(i, v, mark);
			}
		}

		for (int i = 1; i <= nb; i++) {
			if (!mark[i] && indexOf(v, bags[i]) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Mark the tree nodes (bags) containing the given vertex
	 * that are reachable from the bag numbered {@code i}, 
	 * without going through the nodes already marked 
	 * @param i bag number
	 * @param v vertex number
	 * @param mark boolean array recording the marks: 
	 * {@code mark[v]} represents if vertex {@code v} is marked
	 */
	private void markFrom(int i, int v, boolean mark[]) {
		if (mark[i]) {
			return;
		}
		mark[i] = true;

		for (int j = 0; j < degree[i]; j++) {
			int h = neighbor[i][j];
			if (indexOf(v, bags[h]) >= 0) {
				markFrom(h, v, mark);
			}
		}
	}

	/**
	 * Simplify this target tree-decomposition by
	 * forcing the intersection between each pair of
	 * adjacent bags to be a minimal separator
	 */

	public void minimalize() {
		if (bagSets == null) {
			bagSets = new XBitSet[nb + 1];
			for (int i = 1; i <= nb; i++) {
				bagSets[i] = new XBitSet(g.n);
				for (int k: bags[i]) {
					bagSets[i].set(k);
				}
			}
		}
		for (int i = 1; i <= nb; i++) {
			for (int a = 0; a < degree[i]; a++) {
				int j = neighbor[i][a];
				XBitSet separator = bagSets[i].intersectWith(bagSets[j]);
				XBitSet iSide = new XBitSet(g.n);
				collectVertices(i, j, iSide);
				iSide.andNot(separator);
				XBitSet neighbors = g.neighborSet(iSide);
				XBitSet delta = separator.subtract(neighbors);
				bagSets[i].andNot(delta);
			}
		}
		for (int i = 1; i <= nb; i++) {
			bags[i] = bagSets[i].toArray();
		}
	}

	/**
	 * Collect vertices in the bags in the specified
	 * subtree of this target tree-decomposition
	 * @param i the bag index of the root of the subtree
	 * @param exclude the index in the adjacency list
	 * the specified bag, to be excluded from the subtree 
	 * @param set the {@XBitSet} in which to collect the 
	 * vertices
	 */
	private void collectVertices(int i, int exclude, XBitSet set) {
		set.or(bagSets[i]);
		for (int a = 0; a < degree[i]; a++) {
			int j = neighbor[i][a];
			if (j != exclude) {
				collectVertices(j, i, set);
			}
		}
	}

	/**
	 * Computes the number of tree edges of this tree-decomosition,
	 * which is the sum of the node degrees divides by 2
	 * @return the number of edges
	 */
	private int numberOfEdges() {
		int count = 0;
		for (int i = 1; i <= nb; i++) {
			count += degree[i];
		}
		return count / 2;
	}
	/**
	 * Finds the index at which the given element
	 * is found in the given array.
	 * @param x int value to be searched
	 * @param a int array in which to find {@code x}
	 * @return {@code i} such that {@code a[i] == x};
	 * -1 if none such index exists
	 */

	private int indexOf(int x, int a[]) {
		return indexOf(x, a, a.length);
	}

	/**
	 * Finds the index at which the given element
	 * is found in the given array.
	 * @param x int value to be searched
	 * @param a int array in which to find {@code x}
	 * @param n the number of elements to be searched
	 * in the array
	 * @return {@code i} such that {@code a[i] == x} and
	 * 0 <= i <= n; -1 if none such index exists
	 */
	private int indexOf(int x, int a[], int n) {
		for (int i = 0; i < n; i++) {
			if (x == a[i]) {
				return i;
			}
		}
		return -1;
	}
	
	public HashSet< Pair< String, String > > computeFill(LabeledGraph g)
	{
		HashSet< Pair< String, String > > fillEdges = new HashSet<>();
		for (int i = 1; i <= nb; i++) {
			for (int j = 0; j < bags[ i ].length; j++) {
				for (int k = j + 1; k < bags[ i ].length; k++) {
					if (g.areAdjacent( bags[ i ][ j ] , bags[ i ][ k ]) == false) {
						String u = g.getLabel( bags[ i ][ j ] );
						String v = g.getLabel( bags[ i ][ k ] );
						if (u.compareTo( v ) < 0) {
							fillEdges.add( new Pair< String, String >( u, v ) );
						} else {
							fillEdges.add( new Pair< String, String >( v, u ) );
						}
					}
				}
			}
		}

		return fillEdges;
	}
}
