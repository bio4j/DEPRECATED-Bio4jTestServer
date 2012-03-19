/*
 * Copyright (C) 2010-2011  "Bio4j"
 *
 * This file is part of Bio4j
 *
 * Bio4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
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
