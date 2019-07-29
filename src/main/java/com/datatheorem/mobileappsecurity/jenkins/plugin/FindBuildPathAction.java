package com.datatheorem.mobileappsecurity.jenkins.plugin;

import groovy.lang.Tuple2;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.types.FileSet;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Find the absolute path of a build name.
 * <p>
 * This action first looks if the build is on the artifact directory.
 * Then the action searches for the build on the current job workspace directory.
 * If he doesn't find any builds the action return null
 * </p>
 */
class FindBuildPathAction {

    private final String buildName;
    private final FilePath workspace;
    private final Run<?, ?> runner;
    private final PrintStream logger;

    FindBuildPathAction(String buildName, FilePath workspace, Run<?, ?> runner, PrintStream logger)
    {

        this.buildName = buildName;
        this.workspace = workspace;
        this.runner = runner;
        this.logger = logger;
    }

    private boolean isSimilarToBuildName(String fileName) {
        /*
         * Test if the filename respects the buildName pattern
         */
        PathMatcher matcher =
                FileSystems.getDefault().getPathMatcher("glob:" + buildName);
        return matcher.matches(Paths.get(fileName));
    }

    @SuppressWarnings("deprecation")
    public Tuple2<String, Boolean> perform() {
        /*
         *  Find the absolute path of a build name
         *  @return: absolute path to the build if exist, null otherwise
         *  @return: boolean "isInArtifactDir" when the build is stored in the artifact directory
         */

        for (Run<?, ?>.Artifact artifact : runner.getArtifacts()) {
            if (isSimilarToBuildName(artifact.getFileName())) {
                return new Tuple2<>(
                        runner.getArtifactsDir().toString() + '/' + artifact.relativePath, true
                );
            }
        }

        try {
            Collection<String> files = workspace.act(new ListFiles()).values();
            for (String file : files) {
                if (isSimilarToBuildName(file)) {
                        return new Tuple2<>(file, false);
                }
            }
        } catch (IOException |
                InterruptedException e) {
            logger.println(e.toString());
        }
        return null;
    }

    private static final class ListFiles extends MasterToSlaveFileCallable<Map<String, String>> {
        /*
         * Jenkins core way to get the relative path of all files from workspace directory
         * of either jenkins local server or external secondary agent
         */

        @Override
        public Map<String, String> invoke(File basedir, VirtualChannel channel) {
            Map<String, String> r = new HashMap<>();

            FileSet fileSet = Util.createFileSet(basedir, "", "");
            fileSet.setCaseSensitive(true);

            for (String f : fileSet.getDirectoryScanner().getIncludedFiles()) {
                f = f.replace(File.separatorChar, '/');
                r.put(f, f);
            }
            return r;
        }
    }
}
