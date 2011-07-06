/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.servlet;

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4jmodel.nodes.ProteinNode;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.NodeRetriever;
import com.era7.lib.bioinfo.bioinfoutil.fasta.FastaUtil;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.OutputStream;
import java.util.ArrayList;
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

            StringBuilder resultStBuilder = new StringBuilder();

            Request myReq = new Request(request.getParameter(Request.TAG_NAME));

            if (myReq.getMethod().equals(RequestList.DOWNLOAD_PROTEIN_MULTIFASTA_REQUEST)) {

                String fileName = myReq.getParameters().getChildText("file_name");

                Element proteinsXml = myReq.getParameters().getChild("proteins");

                List<Element> proteins = proteinsXml.getChildren(ProteinXML.TAG_NAME);

                for (Element element : proteins) {
                    ProteinXML protein = new ProteinXML(element);

                    System.out.println("retrieving sequence for: " + protein.getId());
                    
                    NodeRetriever nodeRetriever = new NodeRetriever(new Bio4jManager(CommonData.DATABASE_FOLDER));
                    ProteinNode proteinNode = nodeRetriever.getProteinNodeByAccession(protein.getId());

                    if (proteinNode != null) {

                        String headerSt = ">";

                        //dataset
                        if (proteinNode.getDataset().getName().toLowerCase().startsWith("trembl")) {
                            headerSt += "tr|";
                        } else {
                            headerSt += "sp|";
                        }
                        //accession
                        headerSt += proteinNode.getAccession() + "|";
                        //name + fullname
                        headerSt += proteinNode.getName() + " " + proteinNode.getFullName() + " ";
                        //OS=
                        headerSt += "OS=" + proteinNode.getOrganism().getScientificName() + " ";
                        //GN=
                        headerSt += "GN=";
                        String[] geneNames = proteinNode.getGeneNames();
                        if (geneNames.length > 0) {
                            headerSt += geneNames[0];
                        }

                        resultStBuilder.append((headerSt + "\n"));
                        resultStBuilder.append(FastaUtil.formatSequenceWithFastaFormat(proteinNode.getSequence().replaceAll(" ", ""), 60));

                    }
                }

                response.setContentType("application/x-download");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".fasta");


                byte[] byteArray = resultStBuilder.toString().getBytes();

                out.write(byteArray);
                response.setContentLength(byteArray.length);

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
