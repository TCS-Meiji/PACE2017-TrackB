package fillin.branch;

public class Vertex {
	
	public int id;
	public VertexSet adj;
	public String label;
	
	public Vertex(int id, String label)
	{
		this.id = id;
		this.label = label;
		adj = new VertexSet();
	}
	
	public Vertex subgraph(VertexSet vs)
	{
		Vertex v = new Vertex(id, label);
		v.adj = adj.intersectImut( vs );
		return v;
	}
	
	public void addEdge(Vertex v)
	{
		adj.add( v );
	}
	
	public void removeEdge(Vertex v)
	{
		adj.remove( v );
	}
	
	public boolean isAdjacent(Vertex v)
	{
		return adj.contains( v );
	}
	
	@Override
	public String toString()
	{
		return id+"("+ label +")";
	}
}
