/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.servlet;

import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4j.server.util.FileUploadUtilities;
import com.era7.lib.bioinfo.bioinfoutil.gephi.GephiExporter;
import com.era7.lib.bioinfoxml.gexf.viz.VizColorXML;
import com.era7.lib.bioinfoxml.go.GoAnnotationXML;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingoldBuilder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetGoAnnotationGexfServlet extends HttpServlet {

    @Override
    public void init() {
    }

    @Override
    public void doPost(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        //System.out.println("doPost !");
        servletLogic(request, response);

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        //System.out.println("doGet !");
        servletLogic(request, response);


    }

    protected void servletLogic(HttpServletRequest request, HttpServletResponse resp)
            throws javax.servlet.ServletException, java.io.IOException {


        OutputStream out = resp.getOutputStream();

        String temp = request.getParameter(Request.TAG_NAME);

        try {

            Request myReq = new Request(temp);

            String method = myReq.getMethod();

            if (method.equals(RequestList.GET_GO_ANNOTATION_GEXF_REQUEST)) {

                boolean isMultipart = ServletFileUpload.isMultipartContent(request);

                if (!isMultipart) {
                    Response response = new Response();
                    response.setError("No file was uploaded");
                    out.write(response.toString().getBytes());
                } else {

                    System.out.println("all controls passed!!");

                    FileItem fileItem = FileUploadUtilities.getFileItem(request);
                    InputStream uploadedStream = fileItem.getInputStream();
                    BufferedReader inBuff = new BufferedReader(new InputStreamReader(uploadedStream));
                    String line = null;
                    StringBuilder stBuilder = new StringBuilder();
                    while ((line = inBuff.readLine()) != null) {
                        //System.out.println("line = " + line);
                        stBuilder.append(line);
                    }
                    uploadedStream.close();

                    String gexfSt = GephiExporter.exportGoAnnotationToGexf(new GoAnnotationXML(stBuilder.toString()),
                            new VizColorXML(241, 134, 21, 255),
                            new VizColorXML(21, 155, 241, 243),
                            true);

                    //System.out.println("gexfSt = " + gexfSt);                    

                    //--------------------------------------------------------------------------------
                    //----------------------------GEPHI TOOLKIT PART--------------------------------
                    //--------------------------------------------------------------------------------

                    ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
                    pc.newProject();
                    Workspace workspace = pc.getCurrentWorkspace();
                    ImportController importController = Lookup.getDefault().lookup(ImportController.class);
                    StringReader stReader = new StringReader(gexfSt);
                    Container container = importController.importFile(stReader, importController.getFileImporter("gexf"));

                    container.getLoader().setEdgeDefault(EdgeDefault.DIRECTED);   //Force DIRECTED
                    container.setAllowAutoNode(false);

                    //Append container to graph structure
                    importController.process(container, new DefaultProcessor(), workspace);

                    GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();

                    //Layout for 1 minute
                    AutoLayout autoLayout = new AutoLayout(1, TimeUnit.MINUTES);
                    autoLayout.setGraphModel(graphModel);
                    //YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
                    //FruchtermanReingoldBuilder fruchtermanReingoldBuilder = new FruchtermanReingoldBuilder();
                    //FruchtermanReingold fruchtermanReingold = fruchtermanReingoldBuilder.buildLayout();
                    //fruchtermanReingold.setArea(100000f);
                    YifanHuLayout yifanHu = new YifanHuProportional().buildLayout();

                    //yifanHu.se
                    autoLayout.addLayout(yifanHu, 1f);
                    //autoLayout.addLayout(fruchtermanReingold, 0.4f);
                    autoLayout.execute();

                    PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
                    model.getNodeSupervisor().setShowNodeLabels(Boolean.TRUE);


                    ExportController ec = Lookup.getDefault().lookup(ExportController.class);

                    Exporter exporterGexf = ec.getExporter("gexf");     //Get GraphML exporter
                    exporterGexf.setWorkspace(workspace);
                    StringWriter gexfStringWriter = new StringWriter();
                    ec.exportWriter(gexfStringWriter, (CharacterExporter) exporterGexf);

                    //--------------------------------------------------------------------------------
                    //--------------------------------------------------------------------------------

                    String gexfResponseSt = gexfStringWriter.toString();
                    gexfResponseSt = gexfResponseSt.replaceAll("<creator>Gephi 0.7</creator>", "<creator>Bio4j Go Tools</creator>");

                    String responseSt = "<response status=\"" + Response.SUCCESSFUL_RESPONSE
                            + "\" method=\"" + RequestList.GET_GO_ANNOTATION_GEXF_REQUEST
                            + "\" >\n" + gexfResponseSt + "\n</response>";

                    System.out.println("writing response");

                    resp.setContentType("text/html");

                    byte[] byteArray = responseSt.getBytes();
                    out.write(byteArray);
                    resp.setContentLength(byteArray.length);

                    //System.out.println("responseSt = " + responseSt);

                    System.out.println("doneee!!");

                }

            } else {
                Response response = new Response();
                response.setError("There is no such method");
                out.write(response.toString().getBytes());

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
