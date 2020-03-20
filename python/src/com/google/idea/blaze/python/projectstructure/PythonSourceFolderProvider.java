package com.google.idea.blaze.python.projectstructure;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import com.google.idea.blaze.base.ideinfo.PyIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetMap;
import com.google.idea.blaze.base.sync.SourceFolderProvider;
import com.google.idea.blaze.base.util.UrlUtil;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.diagnostic.Logger;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class PythonSourceFolderProvider implements SourceFolderProvider {
    private static final Logger LOG = Logger.getInstance(PythonSourceFolderProvider.class);
    private ImmutableList<String> sourceRoots;

    public PythonSourceFolderProvider(TargetMap targetMap) {
        LOG.warn("constructor");
        ImmutableList<PyIdeInfo> pythonTargets = targetMap
                .targets()
                .stream()
                .filter(t -> t.getPyIdeInfo() != null)
                .map(TargetIdeInfo::getPyIdeInfo).collect(toImmutableList());

        LOG.warn("pythonTargets" + pythonTargets);
        sourceRoots = pythonTargets
                .stream()
                .map(PyIdeInfo::getSources)
                .map(s -> s
                        .stream()
                        .filter(ArtifactLocation::isMainWorkspaceSourceArtifact)
                        .filter(a -> a
                                .getRelativePath()
                                .endsWith("/__init__.py"))
                        .min((a, b) ->
                                Integer.compare(
                                        CharMatcher.is('/').countIn(a.getRelativePath()),
                                        CharMatcher.is('/').countIn(b.getRelativePath())
                                )
                        ))
                .filter(Optional::isPresent)
                .filter(s -> (s.get().getRelativePath().indexOf("test") == -1))
                .map(a -> removeEnd(a.get().getRelativePath(), "__init__.py"))
                .collect(toImmutableList());

        LOG.info("python sources root" + sourceRoots);
    }

    @Override
    public ImmutableMap<File, SourceFolder> initializeSourceFolders(ContentEntry contentEntry) {
        String url = contentEntry.getUrl();              // file:///Users/c4urself/code/adtcode
        File contentFile = UrlUtil.urlToFile(url);       //
        String contentFileName = contentFile.getName();  // adtcode
        LOG.info("url" + url);
        LOG.info("contentFileName" + contentFileName);

        Map<File, SourceFolder> sourceFolders = sourceRoots
                .stream()
                .filter(s -> true)
                .map(s -> new File(contentFile, s))
                .collect(Collectors.toMap(
                        f -> f,
                        f -> contentEntry.addSourceFolder("file://" + f.getAbsolutePath(), false),
                        (a, b) -> a
                ));

        LOG.warn("initializeSourceFolders" + sourceFolders.toString());
        return ImmutableMap.copyOf(sourceFolders);
    }

    @Override
    public SourceFolder setSourceFolderForLocation(ContentEntry contentEntry, SourceFolder parentFolder, File file, boolean isTestSource) {
        return contentEntry.addSourceFolder(UrlUtil.fileToIdeaUrl(file), isTestSource);
    }

    private String removeEnd(String s, String remove) {
        return s.substring(0, s.length() - remove.length());
    }
}
