package tw.common;

public class Pair<F, S> {
	
	public F first;
	public S second;
	
	public Pair(F first, S second)
	{
		this.first = first;
		this.second = second;
	}
	@SuppressWarnings( "unchecked" )
	@Override
	public boolean equals(Object obj) {
		return ((Pair<F, S>)obj).first.equals( first ) && ((Pair<F, S>)obj).second.equals( second );
	}
	
	@Override
	public int hashCode() {
		return first.hashCode() ^ second.hashCode();
	}
}
