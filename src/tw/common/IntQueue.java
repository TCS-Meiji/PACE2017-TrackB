package tw.common;

public class IntQueue {
	private final int[] queue;
	private int head, tail;
	
	public IntQueue(int size) {
		head = tail = 0;
		queue = new int[ size ];
	}
	
	public final int poll()
	{
		if (head == tail) {
			return 0;
		}
		int v = queue[ head++ ];
		if (head == queue.length) {
			head = 0;
		}
		return v;
	}
	
	public final void offer(int e)
	{
		queue[ tail++ ] = e;
		if (tail == queue.length) {
			tail = 0;
		}
	}
	
	public final boolean isEmpty()
	{
		return head == tail;
	}
}
