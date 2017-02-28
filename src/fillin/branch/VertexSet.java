package fillin.branch;
import java.util.BitSet;

public class VertexSet {

	private BitSet bs;
	
	public VertexSet()
	{
		bs = new BitSet();
	}
	
	public VertexSet(Vertex v)
	{
		bs = new BitSet();
		add( v );
	}
	
	public VertexSet(Vertex u, Vertex v)
	{
		bs = new BitSet();
		add( u ).add( v );
	}
	
	private VertexSet(BitSet bs)
	{
		this.bs = bs;
	}
	
	public VertexSet add(Vertex v)
	{
		bs.set( v.id );
		return this;
	}
	
	public VertexSet addImut(Vertex v)
	{
		return copy().add( v );
	}
	
	public VertexSet union(VertexSet U)
	{
		bs.or( U.bs );
		return this;
	}
	
	public VertexSet unionImut(VertexSet U)
	{
		return copy().union( U );
	}
	
	public VertexSet intersect(VertexSet U)
	{
		bs.and( U.bs );
		return this;
	}
	
	public VertexSet intersectImut(VertexSet U)
	{
		return copy().intersect( U );
	}
	
	public VertexSet remove(Vertex v)
	{
		bs.clear( v.id );
		return this;
	}
	
	public VertexSet removeImut(Vertex v)
	{
		return copy().remove( v );
	}
	
	public VertexSet delete(VertexSet U)
	{
		bs.andNot( U.bs );
		return this;
	}
	
	public VertexSet deleteImut(VertexSet U)
	{
		return copy().delete( U );
	}
	
	public boolean contains(Vertex v)
	{
		return bs.get( v.id );
	}
	
	public int size()
	{
		return bs.cardinality();
	}
	
	public boolean isEmpty()
	{
		return bs.isEmpty();
	}
	
	public void clear()
	{
		bs.clear();
	}
	
	public VertexSet copy()
	{
		return new VertexSet( (BitSet) bs.clone() );
	}
	
	@Override
	public String toString() {
		if (size() == 0) {
			return "{}";
		}
		StringBuilder sb = new StringBuilder();
		for (int k = bs.nextSetBit( 0 ); k >= 0; k = bs.nextSetBit( k + 1 )) {
			sb.append( "," + k );
		}
		return "{" + sb.substring( 1 ) + "}";
	}
	
	public int[] indices()
	{
		int[] res = new int[ size() ];
		for (int k = bs.nextSetBit( 0 ), i = 0; k >= 0; k = bs.nextSetBit( k + 1 ), i++) {
			res[ i ] = k;
		}
		return res;
	}
	
	public int firstElement()
	{
		return bs.nextSetBit( 0 ); 
	}
	
	public int nextElement( int k )
	{
		return bs.nextSetBit( k + 1 );
	}
	
	public int lastElement()
	{
		return bs.previousSetBit( bs.length() );
	}
}
