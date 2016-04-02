/*
 * Copyright 2015 Decebal Suiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.fortsoft.pf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.processor.ServiceProviderExtensionStorage;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ServiceLoader base implementation for ExtensionFinder.
 * This class lookup extensions in all extensions index files "META-INF/services".
 *
 * @author Decebal Suiu
 */
public class ServiceProviderExtensionFinder extends AbstractExtensionFinder {

    private static final Logger log = LoggerFactory.getLogger(ServiceProviderExtensionFinder.class);

    public ServiceProviderExtensionFinder(PluginManager pluginManager) {
        super(pluginManager);
    }

    @Override
    public Map<String, Set<String>> readClasspathStorages() {
        log.debug("Reading extensions storages from classpath");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        final Set<String> bucket = new HashSet<>();
        try {
            URL url = getClass().getClassLoader().getResource(getExtensionsResource());
            if (url != null) {
                Path extensionPath;
                if (url.toURI().getScheme().equals("jar")) {
                    FileSystem fileSystem = FileSystems.newFileSystem(url.toURI(), Collections.<String, Object>emptyMap());
                    extensionPath = fileSystem.getPath(getExtensionsResource());
                } else {
                    extensionPath = Paths.get(url.toURI());
                }
                Files.walkFileTree(extensionPath, Collections.<FileVisitOption>emptySet(), 1, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        log.debug("Read '{}'", file);
                        Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
                        ServiceProviderExtensionStorage.read(reader, bucket);
                        return FileVisitResult.CONTINUE;
                    }

                });
            }

            if (bucket.isEmpty()) {
                log.debug("No extensions found");
            } else {
                log.debug("Found possible {} extensions:", bucket.size());
                for (String entry : bucket) {
                    log.debug("   " + entry);
                }
            }

            result.put(null, bucket);
        } catch (IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
        }

        return result;
    }

    @Override
    public Map<String, Set<String>> readPluginsStorages() {
        log.debug("Reading extensions storages from plugins");
        Map<String, Set<String>> result = new LinkedHashMap<>();

        List<PluginWrapper> plugins = pluginManager.getPlugins();
        for (PluginWrapper plugin : plugins) {
            String pluginId = plugin.getDescriptor().getPluginId();
            log.debug("Reading extensions storages for plugin '{}'", pluginId);
            final Set<String> bucket = new HashSet<>();

            try {
                URL url = ((PluginClassLoader) plugin.getPluginClassLoader()).findResource(getExtensionsResource());
                if (url != null) {
                    Path extensionPath;
                    if (url.toURI().getScheme().equals("jar")) {
                        FileSystem fileSystem = FileSystems.newFileSystem(url.toURI(), Collections.<String, Object>emptyMap());
                        extensionPath = fileSystem.getPath(getExtensionsResource());
                    } else {
                        extensionPath = Paths.get(url.toURI());
                    }
                    Files.walkFileTree(extensionPath, Collections.<FileVisitOption>emptySet(), 1, new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            log.debug("Read '{}'", file);
                            Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
                            ServiceProviderExtensionStorage.read(reader, bucket);
                            return FileVisitResult.CONTINUE;
                        }

                    });
                } else {
                    log.debug("Cannot find '{}'", getExtensionsResource());
                }

                if (bucket.isEmpty()) {
                    log.debug("No extensions found");
                } else {
                    log.debug("Found possible {} extensions:", bucket.size());
                    for (String entry : bucket) {
                        log.debug("   " + entry);
                    }
                }

                result.put(pluginId, bucket);
            } catch (IOException | URISyntaxException e) {
                log.error(e.getMessage(), e);
            }
        }

        return result;
    }

    private static String getExtensionsResource() {
        return ServiceProviderExtensionStorage.EXTENSIONS_RESOURCE;
    }

}
