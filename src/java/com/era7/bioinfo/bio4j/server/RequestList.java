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

package com.era7.bioinfo.bio4j.server;

/**
 *
 * @author ppareja
 */
public class RequestList {

    public static final String DOWNLOAD_PROTEIN_MULTIFASTA_REQUEST = "download_protein_multifasta";
    public static final String GET_PROTEIN_MULTIFASTA_REQUEST = "get_protein_multifasta";
    public static final String GET_GENE_UNIPROT_ACCESSIONS_REQUEST = "get_gene_uniprot_accessions";

    public static final String GO_ANNOTATION_REQUEST = "go_annotation";
    public static final String GO_SLIM_REQUEST = "go_slim";
    public static final String DOWNLOAD_GO_ANNOTATION_REQUEST = "download_go_annotation";
    public static final String DOWNLOAD_GO_SLIM_REQUEST = "download_go_slim";

    public static final String GET_PROTEIN_INFO_REQUEST = "get_protein_info";

    public static final String UPLOAD_BLAST_AND_GET_COVERAGE_XML_REQUEST = "upload_blast_and_get_coverage_xml";
    public static final String LOAD_BLAST_FILE_FROM_URL_AND_GET_COVERAGE_XML_REQUEST = "load_blast_file_from_url_and_get_coverage_xml";

    public static final String GET_GO_ANNOTATION_CHART_XML_REQUEST = "get_go_annotation_chart_xml";
    public static final String GET_GO_SLIM_CHART_XML_REQUEST = "get_go_slim_chart_xml";

    public static final String GET_GO_ANNOTATION_GEXF_REQUEST = "get_go_annotation_gexf";
    public static final String GET_GO_ANNOTATION_GEXF_FROM_URL_REQUEST = "get_go_annotation_gexf_from_url";
    public static final String DOWNLOAD_GO_ANNOTATION_GEXF_FROM_URL_REQUEST = "download_go_annotation_gexf_from_url";
    
    public static final String GET_CUFF_LINKS_FULL_REPORT_REQUEST = "get_cuff_links_full_report";
    public static final String CUFFLINKS_QUALITY_CONTROL_REQUEST = "cufflinks_quality_control";

}
