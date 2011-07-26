/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.servlet;

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4jmodel.nodes.ProteinNode;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.NodeRetriever;
import com.era7.lib.bioinfo.bioinfoutil.fasta.FastaUtil;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.communication.xml.Request;
import java.io.OutputStream;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class DownloadProteinMultifastaServlet extends HttpServlet {

    @Override
    public void init() {
    }

    @Override
    public void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        servletLogic(request, response);

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        servletLogic(request, response);


    }

    private void servletLogic(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {


        OutputStream out = response.getOutputStream();

        try {

            

            Request myReq = new Request(request.getParameter(Request.TAG_NAME));

            if (myReq.getMethod().equals(RequestList.DOWNLOAD_PROTEIN_MULTIFASTA_REQUEST)) {

                String fileName = myReq.getParameters().getChildText("file_name");

                Element proteinsXml = myReq.getParameters().getChild("proteins");

                List<Element> proteins = proteinsXml.getChildren(ProteinXML.TAG_NAME);

                int responseLength = 0;

                for (Element element : proteins) {
                    ProteinXML protein = new ProteinXML(element);

                    System.out.println("retrieving sequence for: " + protein.getId());

                    NodeRetriever nodeRetriever = new NodeRetriever(new Bio4jManager(CommonData.DATABASE_FOLDER));
                    ProteinNode proteinNode = nodeRetriever.getProteinNodeByAccession(protein.getId());

                    StringBuilder resultStBuilder = new StringBuilder();
                    com.era7.bioinfo.bio4j.server.util.FastaUtil.getFastaFormatForProtein(proteinNode, resultStBuilder);
                  
                    byte[] byteArray = resultStBuilder.toString().getBytes();
                    responseLength += byteArray.length;
                    out.write(byteArray);
                    out.flush();
                }

                response.setContentType("application/x-download");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".fasta");

                response.setContentLength(responseLength);

            } else {
                out.write("There is no such method".getBytes());
            }

        } catch (Exception e) {
            out.write("Error...".getBytes());
            out.write(e.getStackTrace()[0].toString().getBytes());
        }
        
        out.flush();
        out.close();


    }
}
