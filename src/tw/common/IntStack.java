package tw.common;

public class IntStack {
	
	private int[] stack;
	private int pt;
	
	public IntStack(int n)
	{
		pt = 0;
		stack = new int[ n ];
	}
	
	public final void push(final int e)
	{
		stack[ pt++ ] = e;
	}
	
	public final int pop()
	{
		return stack[ --pt ];
	}
	
	public final int size()
	{
		return pt;
	}
	
	public final boolean isEmpty()
	{
		return size() == 0;
	}
	
	public final void clear()
	{
		pt = 0;
	}
	
}
