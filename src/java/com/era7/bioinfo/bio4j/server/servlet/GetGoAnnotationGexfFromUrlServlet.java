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
import com.era7.bioinfo.bio4j.server.util.GephiUtil;
import com.era7.lib.bioinfo.bioinfoutil.gephi.GephiExporter;
import com.era7.lib.bioinfoxml.gexf.viz.VizColorXML;
import com.era7.lib.bioinfoxml.go.GoAnnotationXML;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Generates a Gexf XML for the corresponding GO annotation results.
 * It looks up for the GO annotation XML file in the url provided.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetGoAnnotationGexfFromUrlServlet extends HttpServlet {

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


            if (method.equals(RequestList.GET_GO_ANNOTATION_GEXF_FROM_URL_REQUEST)) {

                String requestId = myReq.getId();
                
                String subOntologySt = myReq.getParameters().getChildText("sub_ontology");
                String algorithmLayoutSt = myReq.getParameters().getChildText("layout_algorithm");
                String algorithmLayoutTimeSt = myReq.getParameters().getChildText("layout_algorithm_time");
                
                System.out.println("algorithmLayoutSt = " + algorithmLayoutSt);
                System.out.println("algorithmLayoutTimeSt = " + algorithmLayoutTimeSt);
                
                String urlSt = myReq.getParameters().getChildText("url");
                URL url = new URL(urlSt);
                InputStream inputStream = url.openStream();
                
                BufferedReader inBuff = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                StringBuilder stBuilder = new StringBuilder();
                while ((line = inBuff.readLine()) != null) {
                    stBuilder.append(line);
                    //System.out.println("line = " + line);
                }
                inBuff.close();
                inputStream.close();


                String gexfSt = GephiExporter.exportGoAnnotationToGexf(new GoAnnotationXML(stBuilder.toString()),
                        new VizColorXML(241, 134, 21, 255),
                        new VizColorXML(21, 155, 241, 243),
                        true, 
                        false, 
                        subOntologySt);
            
                
                //----------------------------GEPHI TOOLKIT PART--------------------------------

                String gexfResponseSt = GephiUtil.applyAlgorithmToGexf(gexfSt, algorithmLayoutSt, Integer.parseInt(algorithmLayoutTimeSt));                
                gexfResponseSt = gexfResponseSt.replaceAll("<creator>Gephi 0.7</creator>", "<creator>Bio4j Go Tools</creator>");                               

                String responseSt = "<response status=\"" + Response.SUCCESSFUL_RESPONSE
                        + "\" id=\"" + requestId + "\" method=\"" + RequestList.GET_GO_ANNOTATION_GEXF_FROM_URL_REQUEST
                        + "\" >\n" + gexfResponseSt + "\n</response>";
                
                System.out.println("writing response");

                resp.setContentType("text/html");

                byte[] byteArray = responseSt.getBytes();
                out.write(byteArray);
                resp.setContentLength(byteArray.length);

                //System.out.println("responseSt = " + responseSt);

                System.out.println("doneee!!");   

            } else {
                Response response = new Response();
                response.setError("There is no such method");
                out.write(response.toString().getBytes());

            }

            //logging request
            Bio4jLogger.log(Bio4jLogger.createLogRecord(request, myReq.toString(), RequestList.GET_GO_ANNOTATION_GEXF_FROM_URL_REQUEST));


        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("end reached!");
        
        out.flush();
        out.close();

    }
}
