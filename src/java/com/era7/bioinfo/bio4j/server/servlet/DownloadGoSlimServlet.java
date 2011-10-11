package com.era7.bioinfo.bio4j.server.servlet;

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

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4j.server.util.Bio4jLogger;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.GoUtil;
import com.era7.lib.bioinfoxml.go.GOSlimXML;
import com.era7.lib.bioinfoxml.go.SlimSetXML;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.communication.xml.Request;
import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

/**
 * Downloads a XML file including the corresponding result for the GOSlim job specified as parameter.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class DownloadGoSlimServlet extends HttpServlet {

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
    public void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        //System.out.println("doGet !");
        servletLogic(request, response);


    }

    private void servletLogic(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {


        OutputStream out = response.getOutputStream();

        try {

            String temp = request.getParameter(Request.TAG_NAME);
            Request myReq = new Request(temp);

            //System.out.println("myReq = " + myReq);
            
            String reqSt = myReq.toString();

            if (myReq.getMethod().equals(RequestList.DOWNLOAD_GO_SLIM_REQUEST)) {

                String fileName = myReq.getParameters().getChildText("file_name");
                //boolean countGos = Boolean.parseBoolean(myReq.getParameters().getChildText("count_gos"));

                List<Element> proteinList = myReq.getParameters().getChild("proteins").getChildren(ProteinXML.TAG_NAME);
                ArrayList<ProteinXML> proteinArray = new ArrayList<ProteinXML>();
                for (Element elem : proteinList) {
                    proteinArray.add(new ProteinXML(elem));
                }

                SlimSetXML slimSet = new SlimSetXML(myReq.getParameters().getChild(SlimSetXML.TAG_NAME));
                //System.out.println("slimSet = " + slimSet);

                GOSlimXML goSlim = GoUtil.getGoSlim(proteinArray, slimSet, new Bio4jManager(CommonData.DATABASE_FOLDER), null);

                response.setContentType("application/x-download");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xml");

                //System.out.println("goSlim = " + goSlim);

                byte[] byteArray = goSlim.toString().getBytes();

                out.write(byteArray);
                response.setContentLength(byteArray.length);


            } else {
                out.write("There is no such method".getBytes());
            }
            
            //logging request
            Bio4jLogger.log(Bio4jLogger.createLogRecord(request, reqSt, RequestList.DOWNLOAD_GO_SLIM_REQUEST));



        } catch (Exception e) {
            out.write("There was an error...".getBytes());
            out.write(e.getStackTrace()[0].toString().getBytes());
        }

        out.flush();
        out.close();
    }
}


