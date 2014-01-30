package plugin;
import java.util.ArrayList;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;


public class GetAll extends ServerPlugin
{
    @Description( "return " )
    @PluginTarget( GraphDatabaseService.class )
    public Iterable<Node> getMethodsInClassNode( @Source GraphDatabaseService graphDb, @Parameter(name = "cid") String cid, @Parameter(name = "mid") String mid)
    {
        Index<Node> allMethodsIndex = graphDb.index().forNodes("allMethodsIndex");
        IndexHits<Node> methods = allMethodsIndex.get("classId", cid);
        ArrayList<Node> ret = new ArrayList<Node>(); 
        for(Node method:methods)
        {
        	if(method.getProperty("exactName").equals(mid))
        		ret.add(method);
        }
        
        return ret;
    }
}