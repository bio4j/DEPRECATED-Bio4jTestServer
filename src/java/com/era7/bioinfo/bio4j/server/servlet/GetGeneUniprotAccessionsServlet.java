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

import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * Maps gene names to Uniprot accessions. 
 * Only human protein accessions are included, otherwise the gene names provided
 * are ignored.
 * Whenever there are more than one 'Homo Sapiens' accession ocurrence for a given gene name,
 * the criteria followed is selecting randomly among the proteins belonging to Swiss-Prot 
 * dataset; if there's none from Swiss-Prot, the accession is randomly selected from those
 * belonging to Trembl.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetGeneUniprotAccessionsServlet extends BasicServletNeo4j {

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {


        Response response = new Response();

        try {

            String method = request.getMethod();


            if (method.equals(RequestList.GET_GENE_UNIPROT_ACCESSIONS_REQUEST)) {

                String urlSt = request.getParameters().getChildText("url");
                String fileName = request.getParameters().getChildText("file_name");
                String bucketName = request.getParameters().getChildText("bucket_name");
                URL url = new URL(urlSt);
                InputStream inputStream = url.openStream();

                BufferedReader inBuff = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;

                File tempFile = new File(fileName);
                BufferedWriter outBuff = new BufferedWriter(new FileWriter(tempFile));
                ArrayList<String> geneNames = new ArrayList<String>();

                NodeRetriever nodeRetriever = new NodeRetriever(manager);
                
                System.out.println("Storing gene names...");
                while ((line = inBuff.readLine()) != null) {
                    geneNames.add(line.trim());
                }
                inBuff.close();
                inputStream.close();
                System.out.println("done!");
                
                System.out.println("Getting accessions...");
                int proteinsCounter = 0;
                for (String geneName : geneNames) {                   
                    
                    List<ProteinNode> proteins = nodeRetriever.getProteinsByGeneNames(geneName);                    
                    
                    if(proteins.size() > 0){
                        if(proteins.size() == 1){
                            outBuff.write(geneName + "\t" + proteins.get(0).getAccession() + "\n");
                        }else{
                            
                            ProteinNode selectedProtein = null;
                            
                            for (int i=0;i<proteins.size();i++) {
                                
                                ProteinNode proteinNode = proteins.get(i);
                                if(proteinNode.getOrganism().getScientificName().equalsIgnoreCase("Homo Sapiens")){
                                    selectedProtein = proteinNode;
                                    if(proteinNode.getDataset().getName().equalsIgnoreCase("swiss-prot")){
                                        break;
                                        //we don't need to keep searching (any human sprot protein is ok)
                                    }
                                }
                            }
                            
                            if(selectedProtein != null){
                                outBuff.write(geneName + "\t" + selectedProtein.getAccession() + "\n");
                            }
                            
                            
                        }
                    }
                    
                    proteinsCounter++;
                    
                    if(proteinsCounter % 1000 == 0){
                        System.out.println(proteinsCounter + " genes parsed...");
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
