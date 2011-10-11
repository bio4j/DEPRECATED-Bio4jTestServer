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

import com.amazonaws.services.s3.AmazonS3Client;
import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4jmodel.nodes.ProteinNode;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.NodeRetriever;
import com.era7.bioinfo.bioinfoaws.s3.S3FileUploader;
import com.era7.bioinfo.bioinfoaws.util.CredentialsRetriever;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

/**
 * Generates a multifasta file including the corresponding fasta format for every protein passed as a parameter. 
 * It expects a url for accessing the file including the protein accessions.
 * The result file is stored as a S3 object with the name and bucket specified as parameter.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetProteinMultifastaServlet extends BasicServletNeo4j {

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {


        Response response = new Response();

        try {

            String method = request.getMethod();


            if (method.equals(RequestList.GET_PROTEIN_MULTIFASTA_REQUEST)) {

                String urlSt = request.getParameters().getChildText("url");
                String fileName = request.getParameters().getChildText("file_name");
                String bucketName = request.getParameters().getChildText("bucket_name");
                URL url = new URL(urlSt);
                InputStream inputStream = url.openStream();

                BufferedReader inBuff = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;

                File tempFile = new File(fileName);
                BufferedWriter outBuff = new BufferedWriter(new FileWriter(tempFile));
                ArrayList<String> proteinAccessions = new ArrayList<String>();

                NodeRetriever nodeRetriever = new NodeRetriever(new Bio4jManager(CommonData.DATABASE_FOLDER));
                
                System.out.println("Storing protein accessions...");
                while ((line = inBuff.readLine()) != null) {
                    proteinAccessions.add(line.trim());
                }
                inBuff.close();
                inputStream.close();
                System.out.println("done!");

                StringBuilder resultStBuilder = new StringBuilder();
                
                System.out.println("Getting proteins...");
                int proteinsCounter = 0;
                for (String proteinAccession : proteinAccessions) {                   
                    
                    ProteinNode proteinNode = nodeRetriever.getProteinNodeByAccession(proteinAccession);                    
                    com.era7.bioinfo.bio4j.server.util.FastaUtil.getFastaFormatForProtein(proteinNode, resultStBuilder);

                    outBuff.write(resultStBuilder.toString());
                    
                    resultStBuilder.delete(0, resultStBuilder.length());
                    
                    proteinsCounter++;
                    
                    if(proteinsCounter % 1000 == 0){
                        System.out.println(proteinsCounter + " proteins retrieved...");
                        outBuff.flush();
                    }
                    
                }
                
                outBuff.close();
                
                System.out.println("done!");
                
                System.out.println("Uploading file to S3...");

                //uploading file				
                AmazonS3Client s3Client = new AmazonS3Client(CredentialsRetriever.getBasicAWSCredentialsFromOurAMI());
                S3FileUploader.uploadEveryFileToS3Bucket(tempFile, bucketName, "", s3Client, false);

                System.out.println("Deleting temporal file...");
                
                //deleting temp file
                tempFile.delete();

                response.setStatus(Response.SUCCESSFUL_RESPONSE);
                
                System.out.println("Cool! ;)");

            } else {
                response.setError("There is no such method");
            }

        } catch (Exception e) {
            response.setError(e.getMessage());
            e.printStackTrace();
        }

        return response;
    }

    @Override
    protected void logSuccessfulOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {
    }

    @Override
    protected void logErrorResponseOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {
    }

    @Override
    protected void logErrorExceptionOperation(Request rqst, Response rspns, Throwable thrwbl,
            Bio4jManager manager) {
    }

    @Override
    protected void noSession(Request request) {
    }

    @Override
    protected boolean checkPermissions(ArrayList<?> al, Request rqst) {
        return true;
    }

    @Override
    protected boolean defineCheckSessionFlag() {
        return false;
    }

    @Override
    protected boolean defineCheckPermissionsFlag() {
        return false;
    }

    @Override
    protected boolean defineLoggableFlag() {
        return false;
    }

    @Override
    protected boolean defineLoggableErrorsFlag() {
        return false;
    }

    @Override
    protected boolean defineUtf8CharacterEncodingRequest() {
        return false;
    }

    @Override
    protected void initServlet() {
    }

    @Override
    protected String defineNeo4jDatabaseFolder() {
        return CommonData.DATABASE_FOLDER;
    }
}
