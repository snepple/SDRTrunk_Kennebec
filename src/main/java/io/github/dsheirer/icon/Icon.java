package io.github.dsheirer.icon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.attributes.ViewBox;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.net.URL;
import java.util.Objects;

public class Icon implements Comparable<Icon>
{
    private final static Logger mLog = LoggerFactory.getLogger(Icon.class);

    public final static int ICON_HEIGHT_JAVAFX = 18;

    private StringProperty mName = new SimpleStringProperty();
    private StringProperty mPath = new SimpleStringProperty();

    private BooleanProperty mStandardIcon = new SimpleBooleanProperty(false);
    private BooleanProperty mDefaultIcon = new SimpleBooleanProperty(false);

    private Image mFxImage;
    private boolean mFxImageLoaded = false;

    public Icon()
    {
    }

    public Icon(String name, String path)
    {
        mName.set(name);
        mPath.set(path);
    }

    @JsonIgnore
    public StringProperty nameProperty()
    {
        return mName;
    }

    @JsonIgnore
    public StringProperty pathProperty()
    {
        return mPath;
    }

    @JsonIgnore
    public BooleanProperty standardIconProperty()
    {
        return mStandardIcon;
    }

    @JsonIgnore
    public BooleanProperty defaultIconProperty()
    {
        return mDefaultIcon;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    public String getName()
    {
        return mName.get();
    }

    public void setName(String name)
    {
        mName.set(name);
    }

    @JsonIgnore
    public boolean getStandardIcon()
    {
        return mStandardIcon.get();
    }

    void setStandardIcon(boolean standardIcon)
    {
        mStandardIcon.set(standardIcon);
    }

    @JsonIgnore
    public boolean getDefaultIcon()
    {
        return mDefaultIcon.get();
    }

    void setDefaultIcon(boolean defaultIcon)
    {
        mDefaultIcon.set(defaultIcon);
    }

    public String toString()
    {
        return getName();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "path")
    public String getPath()
    {
        return mPath.get();
    }

    public void setPath(String path)
    {
        mPath.set(path);
    }

    @JsonIgnore
    public Image getFxImage()
    {
        if(!mFxImageLoaded && getPath() != null && !getPath().isEmpty())
        {
            mFxImageLoaded = true;

            if(getPath() == null || getPath().isEmpty())
            {
                mLog.error("Error loading icon [" + getName() + "] - null or empty file path to image");
            }
            else
            {
                mFxImage = getFxImage(ICON_HEIGHT_JAVAFX * 2);
            }
        }

        return mFxImage;
    }

    @JsonIgnore
    public Image getFxImage(int renderHeight)
    {
        if (getPath() == null || getPath().isEmpty()) return null;

        try {
            URL url = null;
            if (!getPath().startsWith("images")) {
                java.io.File file = new java.io.File(getPath());
                if (file.exists()) {
                    url = file.toURI().toURL();
                } else {
                    String svgPath = getPath().replaceAll("\\.png$", ".svg");
                    java.io.File svgFile = new java.io.File(svgPath);
                    if (svgFile.exists()) {
                        url = svgFile.toURI().toURL();
                    } else {
                        url = new java.io.File(getPath()).toURI().toURL();
                    }
                }
            } else {
                String resourcePath = getPath();
                if (!resourcePath.startsWith("/")) {
                    resourcePath = "/" + resourcePath;
                }
                url = Icon.class.getResource(resourcePath);
                if (url == null) {
                    String svgResourcePath = resourcePath.replaceAll("\\.png$", ".svg");
                    url = Icon.class.getResource(svgResourcePath);
                    if (url == null) {
                        String pngResourcePath = resourcePath.replaceAll("\\.svg$", ".png");
                        url = Icon.class.getResource(pngResourcePath);
                    }
                }
            }

            if (url == null) {
                mLog.error("Could not resolve URL for icon: " + getPath());
                return null;
            }

            if (url.toString().toLowerCase().endsWith(".svg")) {
                SVGLoader loader = new SVGLoader();
                SVGDocument doc = loader.load(url);
                if (doc != null) {
                    FloatSize size = doc.size();
                    float scale = (float) renderHeight / size.height;
                    int width = Math.round(size.width * scale);

                    BufferedImage bImg = new BufferedImage(width, renderHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D cg = bImg.createGraphics();
                    cg.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    cg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    cg.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                    doc.render(null, cg, new ViewBox(0, 0, width, renderHeight));
                    cg.dispose();
                    return SwingFXUtils.toFXImage(bImg, null);
                }
            } else {
                return new javafx.scene.image.Image(url.toString(), renderHeight, renderHeight, true, true);
            }
        } catch (Exception e) {
            mLog.error("Error converting icon to FX image [" + getName() + "]", e);
        }
        return null;
    }

    @Override
    public int compareTo(Icon other)
    {
        if(other == null) return -1;
        else if(hashCode() == other.hashCode()) return 0;
        else if(getName() != null && other.getName() != null) {
            if(getName().contentEquals(other.getName())) {
                if(getPath() != null && other.getPath() != null) return getPath().compareTo(other.getPath());
                else if(getPath() != null) return -1;
                else return 1;
            } else return getName().compareTo(other.getName());
        } else if(getName() != null) return -1;
        else return 1;
   }

    @Override
    public int hashCode()
    {
        return Objects.hash(getName(), getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Icon)) return false;
        return compareTo((Icon) o) == 0;
    }

    @JsonIgnore
    public static Callback<Icon,Observable[]> extractor()
    {
        return (Icon i) -> new Observable[] {i.nameProperty(), i.pathProperty(), i.standardIconProperty(), i.defaultIconProperty()};
    }
}
