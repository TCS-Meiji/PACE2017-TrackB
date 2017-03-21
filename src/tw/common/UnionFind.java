package tw.common;

import java.util.Arrays;

public class UnionFind {
	private final int[] tree;
	
	public UnionFind(int n) 
	{
		this.tree = new int[ n ];
		Arrays.fill(tree, -1);
	}
	public void union(int x, int y)
	{
		x = root(x);
		y = root(y);
		if (x != y) {
			if (tree[x] < tree[y]) {
				x ^= y; y ^= x; x^= y;
			}
			tree[x] += tree[y];
			tree[y] = x;
		}
	}
	
	public boolean find(int x, int y) 
	{
		return root(x) == root(y);
	}
	
	private int root(int x) 
	{
		return tree[x] < 0 ? x : (tree[x] = root(tree[x]));
	}
}
