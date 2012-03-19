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
package com.era7.bioinfo.bio4j.server.servlet.qcontrol;

import com.amazonaws.services.s3.AmazonS3Client;
import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bioinfoaws.s3.S3FileDownloader;
import com.era7.bioinfo.bioinfoaws.s3.S3FileUploader;
import com.era7.bioinfo.bioinfoaws.util.CredentialsRetriever;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.lib.bioinfoxml.cufflinks.CuffLinksElement;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Returns a XML structure including the corresponding result for the GOSlim job specified as parameter.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class CufflinksBasicQualityControlServlet extends BasicServletNeo4j {

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {

        Response response = new Response();
        String method = request.getMethod();

        if (method.equals(RequestList.CUFFLINKS_QUALITY_CONTROL_REQUEST)) {

            String diffFileName = request.getParameters().getChildText("diff_file");
            String xmlFileName = request.getParameters().getChildText("xml_file");
            String tsvFileName = request.getParameters().getChildText("tsv_file");
            String goBioProcFileName = request.getParameters().getChildText("go_bio_proc_file");
            String goCellCompProcFileName = request.getParameters().getChildText("go_cell_comp_file");
            String goMolFuncFileName = request.getParameters().getChildText("go_mol_func_file");
            String inputBucketName = request.getParameters().getChildText("input_bucket_name");
            String outputBucketName = request.getParameters().getChildText("output_bucket_name");
            String qualityResultsFileName = request.getParameters().getChildText("results_file");

            HashMap<String, String> xlocsGeneNamesMap = new HashMap<String, String>();
            HashSet<String> xlocsFoundInXML = new HashSet<String>();
            HashSet<String> xlocsFoundInTSV = new HashSet<String>();
            
            int xlocsDiffFileCounter = 0;
            int xlocsXMLFileCounter = 0;
            
            File outputFile = new File(qualityResultsFileName);
            BufferedWriter outBuff = new BufferedWriter(new FileWriter(outputFile));
            outBuff.write("Starting quality control for files:\n" + "1. " + diffFileName + "\n2. " + xmlFileName + 
                    "\n3. " + tsvFileName + "\n4. " + goBioProcFileName + "\n5. " + goCellCompProcFileName + 
                    "\n6. " + goMolFuncFileName + "\n");
            outBuff.write("All these files should be available in the bucket: " + inputBucketName + "\n");
            outBuff.write("This quality control file will be generated in the bucket: " + outputBucketName + "\n");

            AmazonS3Client s3Client = new AmazonS3Client(CredentialsRetriever.getBasicAWSCredentialsFromOurAMI());

            //---diff file---
            InputStream in = S3FileDownloader.getS3FileInputStream(diffFileName, inputBucketName, s3Client);
            BufferedReader inBuff = new BufferedReader(new InputStreamReader(in));
            String line = null;

            //reading diff header
            inBuff.readLine();

            //---getting cufflinks and gene names----------
            while ((line = inBuff.readLine()) != null) {
                String[] columns = line.split("\t");
                String geneName = columns[2];
                String cuffLinkID = columns[0];
                xlocsGeneNamesMap.put(cuffLinkID, geneName);
                xlocsDiffFileCounter++;
            }
            inBuff.close();
            in.close();

            //-----xml file----
            in = S3FileDownloader.getS3FileInputStream(xmlFileName, inputBucketName, s3Client);
            inBuff = new BufferedReader(new InputStreamReader(in));
            
            while ((line = inBuff.readLine()) != null) {
                if(line.startsWith("<cufflinks_element>")){
                    xlocsXMLFileCounter++;
                    CuffLinksElement cuffLinksElement = new CuffLinksElement(line);
                    //ProteinXML proteinXML = new ProteinXML(cuffLinksElement.asJDomElement().getChild(ProteinXML.TAG_NAME));
                    xlocsFoundInXML.add(cuffLinksElement.getId());
                }
            }
            inBuff.close();
            in.close();
            
            //-----tsv file----
            in = S3FileDownloader.getS3FileInputStream(tsvFileName, inputBucketName, s3Client);
            inBuff = new BufferedReader(new InputStreamReader(in));
            inBuff.readLine();//skipping the header
            while ((line = inBuff.readLine()) != null) {
                xlocsFoundInTSV.add(line.split("\t")[0]);
            }
            inBuff.close();
            in.close();
            
            outBuff.write("\nPerforming check: Every XLOC identifier found in the input diff file must be present in the TSV file...\n");
            boolean success = true;
            for (String xlocId : xlocsGeneNamesMap.keySet()) {
                if(!xlocsFoundInTSV.contains(xlocId)){
                    outBuff.write(xlocId + " not found :( \n");        
                    success = false;
                }
            }
            if(!success){
                outBuff.write("\nFAILED !!!!");
            }else{
                outBuff.write("\n OK !! :)");
            }    
            
            outBuff.write("\nPerforming check: Every XLOC identifier found in the input diff file must be present in the XML file...\n");
            success = true;
            for (String xlocId : xlocsGeneNamesMap.keySet()) {
                if(!xlocsFoundInXML.contains(xlocId)){
                    outBuff.write(xlocId + " not found :(");        
                    success = false;
                }
            }
            if(!success){
                outBuff.write("\nFAILED !!!!");
            }else{
                outBuff.write("\n OK !! :)");
            }      
            
            outBuff.write("\nPerforming check (Biological process GO file): There must be as many protein ids as the protein frequency listed in the GO reports, (the same for XLOCs freq and ids)...\n");
            success = true;
            //-----biological process GO file----
            in = S3FileDownloader.getS3FileInputStream(goBioProcFileName, inputBucketName, s3Client);
            inBuff = new BufferedReader(new InputStreamReader(in));
            inBuff.readLine(); //skipping header line
            while ((line = inBuff.readLine()) != null) {
                String[] columns = line.split("\t");
                
                int numberOfProteins = Integer.parseInt(columns[2]);
                int proteinsFound = columns[3].split(",").length;                
                if(numberOfProteins != proteinsFound){
                    outBuff.write("Proteins found and number of proteins expected don't coincide for GO term: " + columns[0] + "\n");
                    success = false;
                }
                
                int numberOfGenes = Integer.parseInt(columns[5]);
                int genesFound = columns[4].split(",").length;
                if(numberOfGenes != genesFound){
                    outBuff.write("Genes found and number of genes expected don't coincide for GO term: " + columns[0] + "\n");
                    success = false;
                }
            }
            inBuff.close();
            in.close();
            if(!success){
                outBuff.write("\nFAILED !!!!");
            }else{
                outBuff.write("\n OK !! :)");
            } 
            
            outBuff.write("\nPerforming check (Cellular component GO file): There must be as many protein ids as the protein frequency listed in the GO reports, (the same for XLOCs freq and ids)...\n");
            success = true;
            //-----biological process GO file----
            in = S3FileDownloader.getS3FileInputStream(goCellCompProcFileName, inputBucketName, s3Client);
            inBuff = new BufferedReader(new InputStreamReader(in));
            inBuff.readLine(); //skipping header line
            while ((line = inBuff.readLine()) != null) {
                String[] columns = line.split("\t");
                
                int numberOfProteins = Integer.parseInt(columns[2]);
                int proteinsFound = columns[3].split(",").length;
                if(numberOfProteins != proteinsFound){
                    outBuff.write("Proteins found and number of proteins expected don't coincide for GO term: " + columns[0] + "\n");
                    success = false;
                }
                
                int numberOfGenes = Integer.parseInt(columns[5]);
                int genesFound = columns[4].split(",").length;
                if(numberOfGenes != genesFound){
                    outBuff.write("Genes found and number of genes expected don't coincide for GO term: " + columns[0] + "\n");
                    success = false;
                }
            }
            inBuff.close();
            in.close();
            if(!success){
                outBuff.write("\nFAILED !!!!");
            }else{
                outBuff.write("\n OK !! :)");
            } 
            
            outBuff.write("\nPerforming check (Molecular function GO file): There must be as many protein ids as the protein frequency listed in the GO reports, (the same for XLOCs freq and ids)...\n");
            success = true;
            //-----biological process GO file----
            in = S3FileDownloader.getS3FileInputStream(goMolFuncFileName, inputBucketName, s3Client);
            inBuff = new BufferedReader(new InputStreamReader(in));
            inBuff.readLine(); //skipping header line
            while ((line = inBuff.readLine()) != null) {
                String[] columns = line.split("\t");
                
                int numberOfProteins = Integer.parseInt(columns[2]);
                int proteinsFound = columns[3].split(",").length;
                if(numberOfProteins != proteinsFound){
                    outBuff.write("Proteins found and number of proteins expected don't coincide for GO term: " + columns[0] + "\n");
                    success = false;
                }
                
                int numberOfGenes = Integer.parseInt(columns[5]);
                int genesFound = columns[4].split(",").length;
                if(numberOfGenes != genesFound){
                    outBuff.write("Genes found and number of genes expected don't coincide for GO term: " + columns[0] + "\n");
                    success = false;
                }
            }
            inBuff.close();
            in.close();
            if(!success){
                outBuff.write("\nFAILED !!!!");
            }else{
                outBuff.write("\n OK !! :)");
            } 
            
            outBuff.close();
            
            S3FileUploader.uploadEveryFileToS3Bucket(outputFile, outputBucketName, "", s3Client, false);
            
            //deleting temp file
            outputFile.delete();
            
            response.setStatus(Response.SUCCESSFUL_RESPONSE);

        } else {
            response.setError("There is no such method");
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
