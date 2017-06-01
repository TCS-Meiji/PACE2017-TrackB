package fillin.main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import tw.common.LabeledGraph;
import tw.common.XBitSet;

public class VertexDisjointPaths {
	
	private V[] in;
	private V[] out;
	private int s;
	private int N;
	
	public VertexDisjointPaths(LabeledGraph g, int s, XBitSet T, XBitSet C)
	{
		this.s = s;
		N = g.n + 1;
		in = new V[ N ];
		out = new V[ N ];
		in[ s ] = new V();
		out[ s ] = new V();
		add(in[ s ], out[ s ], 1 << 25);
		in[ N - 1 ] = new V();
		out[ N - 1 ] = new V();
		add(in[ N - 1 ], out[ N - 1 ],  1 << 25);
		for (int v = T.nextSetBit( 0 ); v >= 0; v = T.nextSetBit( v + 1 )) {
			in[ v ] = new V();
			out[ v ] = new V();
			add(in[ v ], out[ v ], 1);
		}
		for (int v = C.nextSetBit( 0 ); v >= 0; v = C.nextSetBit( v + 1 )) {
			in[ v ] = new V();
			out[ v ] = new V();
			add(in[ v ], out[ v ], 1);
		}
		for (int v: g.neighbor[s]) {
			if (in[ v ] != null) {
				add(out[ s ], in[ v ], 1);
			}
		}
		for (int v = T.nextSetBit( 0 ); v >= 0; v = T.nextSetBit( v + 1 )) {
			for (int w: g.neighbor[ v ]) {
				if (T.get( w ) == false && in[ w ] != null) {
					add(out[ v ], in[ w ], 1);
				}
			}
			add(out[ v ], in[ N - 1 ], 1);
		}
		for (int v = C.nextSetBit( 0 ); v >= 0; v = C.nextSetBit( v + 1 )) {
			for (int w: g.neighbor[ v ]) {
				if (in[ w ] != null) {
					add(out[ v ], in[ w ], 1);
				}
			}
		}
	}
	
	private void add(V s, V t, int cap) {
		E e = new E(t, cap);
		E r = new E(s, 0);
		e.rev = r;
		r.rev = e;
		s.es.add(e);
		t.es.add(r);
	}
	
	public boolean find(int k) {
		return maxflow() >= k;
	}
	private int maxflow() {
		V s = out[ this.s ];
		V t = out[ N - 1 ];
		int flow = 0;
		for (int p = 1;; p++) {
			Queue< V > q = new LinkedList<>();
			s.p = p;
			s.level = 0;
			q.offer(s);
			while (q.isEmpty() == false) {
				V v = q.poll();
				v.i = v.es.size() - 1;
				for(E e : v.es)
					if (e.to.p < p && e.cap > 0) {
						e.to.level = v.level + 1;
						e.to.p = p;
						q.offer(e.to);
					}
			}
			if (t.p < p) {
				return flow;
			}
			for (int f; (f = dfs(s, 1 << 25)) > 0; ) {
				flow += f;
			}
		}
	}
	private int dfs(V v, int f) {
		if (v == out[ N - 1 ]) {
			return f;
		}
		for (; v.i >= 0; v.i--) {
			E e = v.es.get(v.i);
			if (e.to.level > v.level && e.cap > 0) {
				int min = dfs(e.to, Math.min(e.cap, f));
				if (min > 0) {
					e.cap -= min;
					e.rev.cap += min;
					return min;
				}
			}
		}
		return 0;
	}
	private class V {
		ArrayList<E> es = new ArrayList<>();
		int level;
		int p;
		int i;
	}
	private class E {
		E rev;
		V to;
		int cap;
		E(V to, int cap) {
			this.to = to;
			this.cap = cap;
		}
	}
}
