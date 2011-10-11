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

package com.era7.bioinfo.bio4j.server.servlet;


import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4j.server.util.Bio4jLogger;
import com.era7.bioinfo.bio4j.server.util.FileUploadUtilities;
import com.era7.lib.bioinfoxml.go.GoAnnotationXML;
import com.era7.lib.bioinfoxml.go.GoTermXML;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Returns a XML structure intended to be used as source for a GO annotation chart.
 * It expects a GoAnnotation XML file as parameter.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetGoAnnotationChartXMLServlet extends HttpServlet {

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

            if (method.equals(RequestList.GET_GO_ANNOTATION_CHART_XML_REQUEST)) {

                boolean isMultipart = ServletFileUpload.isMultipartContent(request);

                if (!isMultipart) {
                    Response response = new Response();
                    response.setError("No file was uploaded");
                    out.write(response.toString().getBytes());
                } else {

                    FileItem fileItem = FileUploadUtilities.getFileItem(request);
                    InputStream uploadedStream = fileItem.getInputStream();
                    BufferedReader inBuff = new BufferedReader(new InputStreamReader(uploadedStream));
                    String line = null;
                    StringBuilder stBuilder = new StringBuilder();

                    while((line = inBuff.readLine()) != null){
                        stBuilder.append(line);
                    }

                    inBuff.close();
                    uploadedStream.close();

                    GoAnnotationXML goAnnotationXML = new GoAnnotationXML(stBuilder.toString());
                    double numberOfProteins = goAnnotationXML.getProteinAnnotations().getChildren(ProteinXML.TAG_NAME).size();
                    List<GoTermXML> goTerms = goAnnotationXML.getAnnotatorGoTerms();
                    
                    List<GoTermXML> bioProcTerms = new ArrayList<GoTermXML>();
                    List<GoTermXML> molFuncTerms = new ArrayList<GoTermXML>();
                    List<GoTermXML> cellCompTerms = new ArrayList<GoTermXML>();

                    for (GoTermXML goTermXML : goTerms) {

                        if(goTermXML.getAspect().equals(GoTermXML.ASPECT_COMPONENT)){
                            cellCompTerms.add(goTermXML);
                        }else if(goTermXML.getAspect().equals(GoTermXML.ASPECT_FUNCTION)){
                            molFuncTerms.add(goTermXML);
                        }else if(goTermXML.getAspect().equals(GoTermXML.ASPECT_PROCESS)){
                            bioProcTerms.add(goTermXML);
                        }

                    }

                    //Now we have to sort the lists (compare is defined in GoTermXML class)
                    Collections.sort(bioProcTerms);
                    Collections.sort(molFuncTerms);
                    Collections.sort(cellCompTerms);

                    stBuilder = new StringBuilder();
                    stBuilder.append(("<response method=\"" +
                                        RequestList.GET_GO_ANNOTATION_CHART_XML_REQUEST +
                                        "\">\n"));
                    //stBuilder.append("<")
                    stBuilder.append("<terms>\n");

                    stBuilder.append("<cellular_component>\n");
                    for (GoTermXML cellCompTerm : cellCompTerms) {
                        cellCompTerm.setFrequencyPercentage((cellCompTerm.getAnnotationsCount()/numberOfProteins) * 100.0);
                        stBuilder.append(cellCompTerm.toString());
                    }
                    stBuilder.append("</cellular_component>\n");

                    stBuilder.append("<biological_process>\n");
                    for (GoTermXML bioProcTerm : bioProcTerms) {
                        bioProcTerm.setFrequencyPercentage((bioProcTerm.getAnnotationsCount()/numberOfProteins) * 100.0);
                        stBuilder.append(bioProcTerm.toString());
                    }
                    stBuilder.append("</biological_process>\n");

                    stBuilder.append("<molecular_function>\n");
                    for (GoTermXML molFuncTerm : molFuncTerms) {
                        molFuncTerm.setFrequencyPercentage((molFuncTerm.getAnnotationsCount()/numberOfProteins) * 100.0);
                        stBuilder.append(molFuncTerm.toString());
                    }
                    stBuilder.append("</molecular_function>\n");

                    stBuilder.append("</terms>\n");
                    stBuilder.append("</response>");
                    resp.setContentType("text/html");

                    byte[] byteArray = stBuilder.toString().getBytes();
                    out.write(byteArray);
                    resp.setContentLength(byteArray.length);

                }

            } else {
                Response response = new Response();
                response.setError("There is no such method");
                out.write(response.toString().getBytes());
            }
            
            //logging request
            Bio4jLogger.log(Bio4jLogger.createLogRecord(request, myReq.toString(), RequestList.GET_GO_ANNOTATION_CHART_XML_REQUEST));


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
