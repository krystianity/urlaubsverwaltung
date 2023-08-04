package org.synyx.urlaubsverwaltung.web.thymeleaf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.hasText;

public class AssetFilenameHashMapper {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String ASSETS_MANIFEST_FILE = "classpath:assets-manifest.json";

    private final ResourceLoader resourceLoader;

    public AssetFilenameHashMapper(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String getHashedAssetFilename(String assetNameWithoutHash, String contextPath) {
        return getAsset(assetNameWithoutHash, contextPath).getUrl();
    }

    public List<String> getAssetDependencies(String assetNameWithoutHash, String contextPath) {
        return getAsset(assetNameWithoutHash, contextPath).getDependencies();
    }

    private Asset getAsset(String assetNameWithoutHash, String contextPath) {

        LOG.info("reading assets-manifest with contextPath={}", contextPath);

        final Map<String, Asset> assets = readAssetManifest();
        if (assets.containsKey(assetNameWithoutHash)) {
            return withContext(assets.get(assetNameWithoutHash), contextPath);
        }

        throw new IllegalStateException(format("could not resolve given asset name=%s", assetNameWithoutHash));
    }

    private Asset withContext(Asset asset, String contextPath) {
        final String assetUrl = withContext(asset.getUrl(), contextPath);
        final List<String> dependencies = asset.getDependencies().stream().map(url -> withContext(url, contextPath)).toList();
        return new Asset(assetUrl, dependencies);
    }

    private static String withContext(String url, String contextPath) {
        if (hasText(contextPath) && !"/".equals(contextPath)) {
            return contextPath + url;
        }
        return url;
    }

    private Map<String, Asset> readAssetManifest() {
        final InputStream manifest = getManifestFile();
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(manifest, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("could not parse manifest json file");
        }
    }

    private InputStream getManifestFile() {
        try {
            return resourceLoader.getResource(ASSETS_MANIFEST_FILE).getInputStream();
        } catch (IOException e) {
            final String message = String.format("could not read %s. please ensure 'npm run build' has been executed.", ASSETS_MANIFEST_FILE);
            throw new IllegalStateException(message);
        }
    }
}
