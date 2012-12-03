package jp.seraphyware.cryptnotepad.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public final class ApplicationInfo {

    public static ApplicationInfo singleton = new ApplicationInfo();

    private String title;

    private String version;

    private ApplicationInfo() {
        try {
            load();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void load() throws IOException {
        ClassLoader cl = ApplicationInfo.class.getClassLoader();

        Manifest manifest;
        InputStream is = cl.getResourceAsStream("META-INF/MANIFEST.MF");
        try {
            manifest = new Manifest(is);
        } finally {
            is.close();
        }

        Attributes mainAttr = manifest.getMainAttributes();

        this.title = mainAttr.getValue("Implementation-Title");
        this.version = mainAttr.getValue("Implementation-Version");

        if (this.title == null || this.version == null) {
            throw new RuntimeException(
                    "manifest.mfにtitle, version情報が設定されていません.");
        }
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("(title=").append(title);
        buf.append(", version=").append(version);
        buf.append(")");
        return buf.toString();
    }

    public static ApplicationInfo getInstance() {
        return singleton;
    }
}
