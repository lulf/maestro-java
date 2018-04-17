/*
 *  Copyright 2017 Otavio R. Piske <angusyoung@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.maestro.reports;

import org.maestro.common.Constants;
import org.maestro.reports.files.HdrHistogramReportFile;
import org.maestro.reports.files.InspectorReportFile;
import org.maestro.reports.files.MptReportFile;
import org.maestro.reports.files.ReportFile;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Walks through the report directory in order to build the list of files to process
 */
final class ReportDirectoryWalker extends DirectoryWalker<ReportFile> {
    private static final Logger logger = LoggerFactory.getLogger(ReportDirectoryWalker.class);


    private final String initialPath;

    private final List<ReportFile> files = new LinkedList<>();

    public ReportDirectoryWalker(String initialPath) {
        this.initialPath = initialPath;
    }

    private void processHistogramReport(File file) {
        String normalizedName = file.getPath().replace(initialPath, "");
        files.add(new HdrHistogramReportFile(file, new File(normalizedName)));
    }

    private void processMaestroReport(File file) {
        String normalizedName = file.getPath().replace(initialPath, "");
        files.add(new MptReportFile(file, new File(normalizedName)));
    }

    private void processInspectorFile(File file) {
        String normalizedName = file.getPath().replace(initialPath, "");
        files.add(new InspectorReportFile(file, new File(normalizedName)));

    }

    @Override
    protected void handleFile(File file, int depth, Collection<ReportFile> results)

    {
        logger.debug("Processing file {}", file.getPath());
        String ext = FilenameUtils.getExtension(file.getName());

        if (Constants.FILE_EXTENSION_HDR_HISTOGRAM.equals(ext)) {
            processHistogramReport(file);

            return;
        }

        if (Constants.FILE_EXTENSION_MPT_COMPRESSED.equals(ext)) {
            if (!file.getPath().contains(Constants.FILE_HINT_INSPECTOR)) {
                processMaestroReport(file);

                return;
            }
        }


        if (Constants.FILE_EXTENSION_INSPECTOR_REPORT.equals(ext)) {
             if (file.getPath().contains(Constants.FILE_HINT_INSPECTOR)) {
                processInspectorFile(file);

                return;
             }
        }

        logger.trace("Unknown file type for {}", file.getPath());
    }

    @SuppressWarnings("unchecked")
    List<ReportFile> generate(final File reportsDir) {

        if (logger.isDebugEnabled()) {
            logger.debug("Processing downloaded reports on {}", reportsDir.getName());
        }

        try {
           if (reportsDir.exists()) {
                walk(reportsDir, new ArrayList<>());
            }
            else {
                logger.error("The reports directory does not exist: {}", reportsDir.getPath());
            }
        } catch (IOException e) {
            logger.error("Unable to walk the whole directory: " + e.getMessage(), e);
            logger.error("Returning a partial list of all the reports due to errors");
        }

        return files;
    }
}
