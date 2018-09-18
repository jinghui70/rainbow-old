package rainbow.core.platform;

import java.util.Map;

import com.google.common.base.Predicate;

import rainbow.core.bundle.Bundle;
import rainbow.core.bundle.BundleData;
import rainbow.core.util.XmlBinder;

public interface BundleLoader {

    public static final XmlBinder<BundleData> binder = new XmlBinder<BundleData>(BundleData.class);

    public Map<String, Bundle> loadBundle(Predicate<String> exist);

}
