/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.util;

import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.exporter.spi.CharacterExporter;
import org.gephi.io.exporter.spi.Exporter;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.force.yifanHu.YifanHuProportional;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import java.util.concurrent.TimeUnit;
import java.io.StringReader;
import java.io.StringWriter;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingoldBuilder;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GephiUtil {

    public static final String YIFAN_HU_LAYOUT = "YiFan Hu";
    public static final String FRUCHTERMAN_REINGOLD_LAYOUT = "Fruchterman Reingold";

    public static String applyAlgorithmToGexf(String gexfSt,
            String algorithm,
            int algorithmTimeInMinutes) {

        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        StringReader stReader = new StringReader(gexfSt);
        Container container = importController.importFile(stReader, importController.getFileImporter(".gexf"));


        container.getLoader().setEdgeDefault(EdgeDefault.DIRECTED);   //Force DIRECTED
        container.setAllowAutoNode(false);

        //Append container to graph structure
        importController.process(container, new DefaultProcessor(), workspace);

        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();

        //Layout for 1 minute
        AutoLayout autoLayout = new AutoLayout(algorithmTimeInMinutes, TimeUnit.MINUTES);
        autoLayout.setGraphModel(graphModel);

        if (algorithm.equals(YIFAN_HU_LAYOUT)) {
            
            YifanHuLayout yifanHu = new YifanHuProportional().buildLayout();
            autoLayout.addLayout(yifanHu, 1f);
            autoLayout.execute();
            
        } else if (algorithm.equals(FRUCHTERMAN_REINGOLD_LAYOUT)) {
            
            FruchtermanReingoldBuilder fruchtermanReingoldBuilder = new FruchtermanReingoldBuilder();
            FruchtermanReingold fruchtermanReingold = fruchtermanReingoldBuilder.buildLayout();
            fruchtermanReingold.setArea(100000f);
            autoLayout.addLayout(fruchtermanReingold, 1f);
            autoLayout.execute();
            
        }
        
        PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
        model.getNodeSupervisor().setShowNodeLabels(Boolean.TRUE);


        ExportController ec = Lookup.getDefault().lookup(ExportController.class);

        Exporter exporterGexf = ec.getExporter("gexf");     //Get GraphML exporter
        exporterGexf.setWorkspace(workspace);
        StringWriter gexfStringWriter = new StringWriter();
        ec.exportWriter(gexfStringWriter, (CharacterExporter) exporterGexf);

        //--------------------------------------------------------------------------------
        //--------------------------------------------------------------------------------

        return gexfStringWriter.toString();
    }
}
