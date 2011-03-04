/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.listeners;

import com.era7.bioinfo.servletlibraryneo4j.listeners.ApplicationListener;
import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import javax.servlet.ServletContext;
import org.neo4j.graphdb.Node;

/**
 *
 * @author ppareja
 */
public class MainListener extends ApplicationListener {

    @Override
    protected void contextInitializedHandler(ServletContext context) {

        System.out.println("Context initialized handler...");
        Bio4jManager manager = new Bio4jManager(CommonData.DATABASE_FOLDER);
        Node referenceNode = manager.getReferenceNode();

        if(referenceNode == null){
            System.out.println("reference node is null!!");
        }else{
            System.out.println("reference node id: " + referenceNode.getId());
        }

        System.out.println("done!");
    }

    @Override
    protected void contextDestroyedHandler(ServletContext context) {
        System.out.println("Shutting down Neo4j....");
        Bio4jManager manager = new Bio4jManager(CommonData.DATABASE_FOLDER);
        //manager.deleteAll();
        manager.shutDown();
        System.out.println("Done with shutting down! :)");
    }
}
